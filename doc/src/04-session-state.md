# 第 4 章 局内会话状态与数据流

这一章回答用户最关心的问题：**一局棋进行中，状态到底存在哪、怎么流、怎么保证不错**。
它是全项目最重要的一章——大部分“改坏了”都发生在对这套约定的破坏上。

## 4.1 三层状态哲学

整个局内状态被刻意分成三层，每一层只做一件事：

```
┌────────────────────────────────────────────────────────────┐
│ 第 1 层  领域层 domain（GameTree / GameSession / Position）  │
│          纯命令式 Kotlin，无响应式原语，无监听器，无回调      │
│          状态：树节点、current 游标、lastLeafId、座位配置      │
├────────────────────────────────────────────────────────────┤
│ 第 2 层  会话层 SessionViewModel                              │
│          唯一的领域变更入口 + 唯一的快照发布中枢              │
│          状态：MutableStateFlow<SessionUiState>、评估缓存      │
├────────────────────────────────────────────────────────────┤
│ 第 3 层  呈现层 Compose（SessionScreen 及子组件）             │
│          只 collectAsState + 回调 vm::方法，无领域状态         │
└────────────────────────────────────────────────────────────┘
```

为什么领域层故意“不响应式”？三个原因：

1. **规则与状态机可以被纯单元测试**——`GameTreeTest`、`GameSessionTest` 直接构造、
   调用、断言，不需要任何协程或 Compose 环境；
2. **杜绝并发撕裂**——棋树被协程里的 AI 查询回调直接改写时，如果没有明确的“谁在
   何时写”纪律，几乎必然出现状态错乱。领域层是普通对象，只有一个写入口（VM），
   写与写之间天然串行；
3. **快照让 UI 永远拿到一致视图**——UI 不读“树当前字段”，而读一次 `snapshot()`
   冻结出来的不可变对象。一次重组里不可能看到“盘面已经换了、但提子数还是旧的”
   这种中间态。

代价由第 2 层承担：**每次领域变更后必须手动把状态“发布”出去**（`publish()`），
否则 UI 不会知道。这条纪律由 `SessionViewModel` 强制执行——它是唯一会调用
`session.*` 变更方法的地方。

## 4.2 GameSession：对局的门面

`GameSession(config, tree?)` 把“一局的配置 + 一棵树”包成一个门面，暴露给 VM 的是一组
面向对局的动词，而不是裸的树操作：

- `play(point)` / `pass()` / `undo()` / `redo()` / `cycleVariation(delta)` /
  `goTo(id)` / `exitReview()`：直接转发给 `tree`；
- `setSeat(color, seat)`：改某一方的座位（VM 会先 `bumpNav` 再调用）；
- `position`、`lastPlace`、`reviewing`、`ended`：便捷读取；
- `aiShouldMove()`：3.3 的“AI 只在活棋的叶子回合落子”策略；
- `snapshot()`：产出给 UI 的 `SessionSnapshot`。

构造时如果传入的树“停在半途”（读档/读 SGF 后游标在内部节点），init 会调用
`tree.syncResumeLeaf()`，为“退出复盘”预置一个可恢复目标（3.6）。**VM 不会直接
new 一棵树放进 SessionScreen**——`SessionScreen` 是导航目标，树太大不适合放进
导航参数，因此通过 `SessionTrees` 对象按 instanceId 暂存、由 VM 构造时取回
（见第 5 章）。

## 4.3 SessionSnapshot：给 UI 的不可变快照

`SessionSnapshot` 是 `session.snapshot()` 返回的不可变视图，携带 UI 画一帧棋盘所需的
一切“事实”：`size`、`cells`（拷贝的 IntArray）、`toPlay`、`lastMove`、
`lastWasPass`、双方提子数、`canUndo/canRedo/ended`、`moveNumber`、`komi`、
`black/white` 座位、`variationIndex/variationCount`、`aiToPlay`。

它在 `equals`/`hashCode` 上按**内容**比较（含 cells 内容），而不是按引用。这很关键：
`SessionUiState` 是 data class，StateFlow 只在值变化时通知订阅者；若快照按引用比较，
每次 `snapshot()` 都会产生新对象，导致整屏无谓重组。按内容比较后，撤销又撤销回去、
或 AI 快速连续发布时，语义相同的状态不会触发重组。

快照上还有一组派生属性，UI 直接消费：

- `reviewing`（= canRedo）、`ended`；
- `mode` / `rankKyu` / `humanPlaysBlack` / `aiStyle`：从座位反推（向后兼容旧存档）；
- `humanControls`：**“这手该不该由人类控制”** = `ended || reviewing || !seat(toPlay).isAi`。
  复盘和终局都属于人类（复盘时人可以自由导航、预测）；
- `seat(color)`、`stoneAt(x, y)`。

## 4.4 SessionViewModel 与 SessionUiState

`SessionViewModel` 由 Metro 用 `@Assisted` 工厂创建，参数是 `GameConfig`、可选的
`loadedTree`、`recentId`、`recentTitle`（从 Home 打开最近对局时传入）。它注入
`SettingsRepository`、`AnalysisClient`、`RecentGamesRepository`。

对外状态只有**一个**主 StateFlow：

```kotlin
private val _state = MutableStateFlow(SessionUiState(snapshot = ..., engineStatus = ..., tree = ...))
val state: StateFlow<SessionUiState> = _state.asStateFlow()
```

`SessionUiState` 字段（一张表看懂“一帧 UI 需要什么”）：

| 字段 | 来源 |
| --- | --- |
| `snapshot: SessionSnapshot` | `session.snapshot()`：棋盘事实 |
| `selected` / `hover: Point?` | 本地：选中待确认的点 / 悬停幽灵子 |
| `engineStatus`、`analyzing` | `analysis.status`（在线/分析中/错误） |
| `blackWinrate` / `blackScoreLead` / `visits` | 当前节点的实时或缓存评估 |
| `candidates: List<Candidate>` | 当前节点候选点（含损失） |
| `aiThinking`、`aiError` | AI 回合瞬态 |
| `qualities: List<QualityMark>` | 复盘质量标记（当前行） |
| `evalSamples` / `qualityStats` | 胜负曲线采样 / 质量统计 |
| `evalGraphMode` | 曲线模式（Score/Winrate） |
| `tree: TreeLayout` | 棋树面板布局 |
| `ownership: List<Double>`、`deadPoints` | 势力图与死子标记 |
| `reviewProgress: ReviewProgress?` | 整局复盘进度（瞬态） |
| `dirty`、`canSave` | 保存状态 |
| `forecast: Forecast?`、`forecastRevealed` | 局势预测（4.9） |

`preview` 是派生值：终局或 AI 回合时为空，否则 `selected ?: hover`——棋盘用它画
“幽灵落子”。

除了主状态，VM 还有一组**显示开关** StateFlow（`showCoords`、`showCandidates`、
`showQuality`、`showConnections`、`showOwnership`、`showDeadStones`、
`ownershipStyle`、`drawerAcrylic`、`quality` 阈值、`confirmMove`）——它们直接来自
`settings` 仓库，用 `stateIn(WhileSubscribed(1_000))` 挂起；`playVisits` 用
`Eagerly`（AI 回合需要无条件可读）。设置项走设置页，也走侧栏开关，两者最终写的是
同一批 DataStore 键。

UI 侧的一切用户操作都只是：**读 state → 决定要不要做 → 调 vm::方法**。屏幕不持有
棋局状态；本地 `remember` 只放抽屉开关、弹窗开关这类纯展示状态。

## 4.5 publish()：唯一的状态中枢

`publish()` 是“树变了之后”的唯一收口。它的调用点覆盖：`commit`、`pass`、`undo`、
`redo`、`cycleVariation`、`goToNode`、`exitReview`、`setSeat`、AI 落子完成后。
其内部顺序是约定，不要打乱：

```kotlin
private fun publish() {
    cancelForecast()                        // 1. 任何树变化都终止局势预测
    val eval = nodeEvals[session.tree.current.id]  // 2. 取当前节点已缓存评估
    _state.update { it.copy(
        snapshot = session.snapshot(),      // 3. 以领域事实重建状态
        selected = null, hover = null,
        candidates / blackWinrate / blackScoreLead / visits / ownership / deadPoints
            = 从缓存评估推算（没有则保持/清空）,
        aiThinking = false, aiError = null,
        tree = session.tree.layout(),       // 4. 重建棋树布局
        dirty/canSave = computeDirty()/...,
        forecast = null, forecastRevealed = 0,
    ).withAnalysis() }                       // 5. 重派生分析覆盖层
    afterPositionChange()                    // 6. 决定下一步：AI 回合 / 实时分析
}
```

其中 `withAnalysis()` 是一次派生，把所有“由树行 + 节点评估缓存算得出来”的东西
重算一遍并写进同一个 copy：`qualities`（复盘质量点）、`evalSamples`（胜负曲线）、
`qualityStats`。**它不查询引擎，只查缓存**。

`afterPositionChange()` 是“接下来该发生什么”的决策点：

- 若当前轮到 AI 且引擎在线 → `cancelReviewQueue()` + `playAi()`；
- 否则若是人类回合且没有整局复盘在跑 → `requestLive()`（发起实时分析）。

实时分析结果回流时（VM init 里收集 `analysis.live`），只在该 live 属于本会话、
且节点就是当前节点、且没有 forecast 挡路时才覆盖主状态的胜率/势力/候选；否则只做
`withAnalysis()` 或直接忽略——**防止过期的引擎帧把别的节点/别的会话的数据写进来**。

## 4.6 bumpNav() 与 epoch 取消契约

异步是状态管理最容易出错的地方。规则是：

```kotlin
private fun bumpNav() {
    navEpoch++          // 使旧 epoch 失效
    aiJob?.cancel()
    cancelForecast()
}
```

**任何“改变玩家所在位置或对局性质”的操作必须先 `bumpNav()`**：
`undo / redo / cycleVariation / goToNode / exitReview / setSeat / setPaused(true) /
leave`。`commit`（向前走一手）不需要 bump，因为当前节点本身没被导航改变，但它会
`cancelReviewQueue()` 并把实时分析先“记进”缓存（`snapshotLiveToCurrentNode`）。

所有会跨协程延迟后写状态的作业（AI 回合 `playAi`、局势预测 `onForecast`、整局复盘
`analyzeGame`）在动手前都记录 `val epoch = navEpoch`，每次 await 回来**重新检查**
`epoch != navEpoch || session.tree.current.id != nodeId` 才继续，否则直接放弃。
`navEpoch` 是普通 Int，不是 Flow——检查发生在协程内部，够用且简单。

配套的取消粒度：`cancelForecast()` 只杀预测；`cancelReviewQueue()` 杀整局复盘队列
并 `analysis.cancelLive`；`leave()`（退出对局）做全套：bump、取消 AI/预测/复盘、
`cancelLive`、并把评估缓存刷回存档（`flushBoundEvals`）。`onCleared` 会调用
`leave()`。暂停（开抽屉/座位弹窗）用 `setPaused`：暂停 = bump + 停 AI；恢复 =
`afterPositionChange()` 重新评估。

## 4.7 AI 回合

`playAi()` 启动一个 `aiJob`。它的骨架体现了“引擎查询是异步的、但落子必须同步校验”：

1. 记 `epoch`，置 `aiThinking = true`；
2. 读取当前行棋方与其 `SeatKind`，按类型走三分支：

   | 座位 | 查询 | 选子 |
   | --- | --- | --- |
   | `HumanLike` | `queryHuman` | `HumanBot.choose(humanPolicy, position, rng)` |
   | `Rank` | `queryRank` | `RankBot.choose(policy, position, rankKyu, rng).move` |
   | `Full` | `queryGenmove` | `FullStrengthBot.choose(moveInfos, size, toPlay)` |

   每个分支都会先把响应**存进节点评估缓存**（`storeEval`），这样引擎算过的数据
   不浪费，复盘时立刻可用；
3. **三重校验后才落子**：`epoch != navEpoch`（用户可能已经导航）、树没终局、
   `current.id` 没变、且仍然是 AI 回合；随后 `session.play/pass`；
4. 再校验一次 epoch 后 `publish()`。异常落入 `aiError`（长度截断），UI 展示；
   被取消则原样重抛（`CancellationException`）。

“校验在落子前、而不在查询前”是刻意的：查询期间用户完全可以悔棋/跳走/暂停，回来时
必须放弃这手而不是强行落下。

## 4.8 实时分析与逐节点评估缓存

**引擎评估是按节点缓存的**：`nodeEvals: MutableMap<String, StoredEval>`，键是节点
id。`StoredEval` 保存 `blackWinrate / blackScoreLead / visits / moveInfos /
toPlay / ownership / pointsLost / hasView`。它是 UI 层持有的“分析记忆”：胜负曲线、
候选点、质量标记、势力图全部从这里派生，而不是每次重新问引擎。

写入路径有 4 条：

- 实时分析帧落地：VM init 里收集 `analysis.live`，当帧属于当前节点且无预测挡路，
  直接进 `_state`；`rememberLive` 把它同时写进缓存（保留更高 visits 的结果）；
- 落子前 `snapshotLiveToCurrentNode()`：把“最后时刻的实时结果”固定到当前节点，
  免得刚走出的一手丢了上一手的评估；
- AI 查询完成 `storeEval`（4.7）；
- 整局复盘/预测返回（4.9）；以及读档时 `restoreEvals`（从存档的
  `PersistedEval` 还原，节点位置用 `pathFromRoot()` 定位）。

`rememberEval` 的保留策略是“留更好的”：已有更高 visits 的旧值不会被低 visits 覆盖；
同 visits 时有 ownership 的胜于没 ownership 的。`lossInto`/`refreshChildLosses`
负责推导每个节点的 `pointsLost`（相对父节点最佳着法的目差损失），供候选点标签与
质量分使用。

## 4.9 整局复盘与局势预测

**整局复盘（Analyze game）**：`analyzeGame()` 沿着 `preferredLine()` 逐节点补齐
评估。判据 `needsReview(node)` = 缓存缺失、或 `visits < reviewVisits`、或 ownership
不全。它对每个待查节点发一次 `queryReview`（按该节点自己的盘面发查询）、写缓存、
推进 `reviewProgress`；若节点正好是 `current`，同时刷新主状态的胜率/候选/势力。
任务可被 `cancelReviewQueue()` 随时打断。跑完或打断后若轮到人且无任务在跑，会
`requestLive()` 恢复实时分析。

**局势预测（Forecast）**：复盘时**长按/右键**某个合法落子点，引擎会模拟一条
“如果下在这里”的最佳应接线并逐层展示（最多 16 层）。它的生命周期是最复杂的一块：

- 触发条件：`paused` 之外、不在终局、**在复盘**（`session.reviewing`）、引擎在线、
  且 `Rules.tryPlay` 判定该点合法；
- 流程：先放入占位 `Forecast(origin, nodeId, 空)`（棋盘画加载圈）→ `queryForecast`
  得到首选应接（PV）→ `buildForecastLine` 用**纯规则模拟**把它变成 `ForecastLine`
  （连续虚着、非法手、超过 16 层都会截断；模拟完全不碰棋树）→ 立刻揭示第 1 层；
- 第 2..N 层的逐层揭示依赖**每个未来盘面的 ownership**：另发
  `collectForecastOwnership` 流式查询，各层 ownership 经无限 Channel 送回；每层
  等数据就绪（`canRevealForecastPly`，超时 45 秒放弃），再按
  `settings.forecastDropMs`（默认 400ms，设置页可调，上限 3000ms）的节奏逐层放下，
  形成“一颗颗落下”的动画；
- 每揭示一层，用该层 ownership 覆盖势力图/死子标记（`revealForecastPly`）；
- **清除条件**：任何导航/落子/`publish()`（内部先 `cancelForecast`）、`endForecast()`
  （“结束预览”按钮：只清虚子、留在复盘）、退出复盘（`bumpNav` + publish）。清除后
  `restoreAfterForecast()` 把当前节点缓存的 ownership/死子恢复、并 `requestLive()`。
- 预测的数据**永远只活在 `forecast` 字段与棋盘渲染里，不写棋树**。这是它和“变着”
  的本质区别：预测是“给你看的假设”，变着是“真的能走的历史分支”。

## 4.10 数据方向总览

一句话总结：**UI 只发事件、领域只存事实、ViewModel 负责“事件 → 事实 → 快照”并把
快照推给 UI；引擎是唯一的“外部事实来源”，结果永远先落缓存再落状态。**

```
       用户操作 / 引擎帧 / 设置变更
                │
                ▼
   ┌──────────────────────────┐
   │  SessionViewModel         │  ← 唯一允许触碰领域与写主状态的地方
   │  事件方法（onActivate、    │
   │  undo、goToNode、…）       │
   └───────┬──────────────────┘
           │ 变更（同步、带前置校验）
           ▼
   ┌──────────────────────────┐
   │  GameSession / GameTree   │  领域事实：节点、current、盘面
   └───────┬──────────────────┘
           │ publish()：cancelForecast →
           │ snapshot()+layout()+缓存评估 → 单次 _state.update
           ▼
   ┌──────────────────────────┐
   │  MutableStateFlow<SessionUiState>
   └───────┬──────────────────┘
           │ collectAsState
           ▼
   Compose 重组 → Canvas/组件画当前帧
           │
           └──(afterPositionChange)──► AnalysisClient ──► 网关 ──► KataGo
                                            │ 响应/流式帧
                                            ▼
                                 回到 SessionViewModel（缓存 + 状态）
```

状态流转的两个不变式：

1. **领域只被 VM 改**，且每次改完必须 `publish()`；
2. **异步结果不能绕过 VM 写状态**——引擎回调、协程都先进缓存、再经 `_state.update`，
   并且受 `navEpoch` / 节点 id 双重校验，过期结果一律丢弃。

遵守这两条，就遵守了本项目数据管理的全部核心纪律。
