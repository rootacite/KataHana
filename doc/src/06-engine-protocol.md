# 第 6 章 引擎网关与协议原理

## 6.1 总体架构

客户端从不直接 fork KataGo。整条链路是：

```
KataHana 客户端（shared/engine）
        │  一条 WebSocket（ws://127.0.0.1:2080，JSON 文本帧）
        ▼
engine/server.py  ← Python asyncio 网关（websockets 库）
        │  子进程 stdin/stdout，KataGo 的“每行一个 JSON 对象”analysis 协议
        ▼
KataGo analysis 子进程（主网络 + 可选人形网络）
```

网关是**透明中继**：客户端发来的 JSON 原样写进 KataGo 标准输入；KataGo 标准输出上
的每一行 JSON 按查询 `id` 送回对应的客户端。协议本体（查询/响应 JSON 的形状）是
KataGo analysis 引擎的协议，网关只是加了**多客户端路由与进程管理**。客户端侧的
协议栈见 `shared/src/commonMain/kotlin/com/acite/katahana/engine/`：

| 文件 | 职责 |
| --- | --- |
| `AnalysisDto.kt` | 查询/响应 JSON 的 kotlinx-serialization DTO 与 Json 配置 |
| `QueryBuilder.kt` | 按用途构造查询（live/review/rank/human/forecast/genmove/bench） |
| `WsClient.kt` / `EngineHttpClient.kt` | Ktor WebSocket 载体（expect/actual） |
| `AnalysisClient.kt` | 连接管理、单在飞查询纪律、waiter 路由、评测 |
| `Perspective.kt` | 胜率/目差换算、候选点、死子判定、PV 显示 |
| `EngineBenchmark.kt` | 引擎评测与报告 |
| `EngineProfile.kt` | 连接参数（URL/token/访问量） |

## 6.2 网关进程 server.py

`engine/server.py` 只有一个真正的类 `KataGoWSServer`，职责如下：

- **拉起 KataGo**：命令行大致是
  `katago analysis -model <主网络> [-human-model <人形网络>] -config analysis.cfg
  -override-config homeDataDir=…,numAnalysisThreads=2,numSearchThreads=20,
  nnMaxBatchSize=40,numNNServerThreadsPerModel=1,rocmDeviceToUse=0`。
  覆盖参数（`TUNED_*`）是针对开发机（i7-14700F + RX 9070 XT、ROCm、KataGo 1.18.1）
  调过的，**换机器/换显卡时这些是第一个要重新调的**。
- **等待就绪**：等 KataGo stderr 出现 `ready to begin handling requests`（120 秒
  超时）才对外服务。KataGo 的标准错误原样透传到网关 stderr，方便排查。
- **模型下载**：主网络 `b10c384h6nbttflrs.bin.gz`（KataGo v1.17.0）是必需的，缺失
  时从 GitHub 自动下载到 `model/`（先下 `.part`，校验 gzip 魔数与体积 >1MB 再改名）；
  人形网络 `b18c384nbt-humanv0.bin.gz`（v1.15.0）可选，下载失败只禁用 Human SL。
- **转发与路由**：客户端任意文本帧原样写进 KataGo stdin（asyncio 锁保证不交错）；
  stdout 每行 JSON 里若 `id` 是字符串，就查 `_id_to_ws` 表回给对应的连接；否则
  **广播给所有连接**（用于 KataGo 主动推送、以及没有 id 的响应）。客户端断线时
  清理它名下注册的 id。
- **关闭**：SIGINT/SIGTERM 优雅退出，先关 stdin 再 SIGTERM KataGo，5 秒不退出才 kill。

KataGo 二进制查找顺序：`--katago` 显式路径 → `engine/bin/katago` → PATH 里的
`katago`。`analysis.cfg` 里有引擎级配置（`maxVisits=500`、`reportAnalysisWinratesAs=
BLACK`、`conservativePass=true` 等），与客户端查询里的 `overrideSettings` 是两层：
引擎默认值由 cfg 给，单条查询用 override 微调。

## 6.3 查询协议

查询是一个 JSON 对象，公共字段（`AnalysisDto.kt`）：

```jsonc
{
  "id": "…",                 // 见下：路由 + 唯一性都靠它
  "rules": "chinese",
  "komi": 7.5,
  "boardXSize": 19, "boardYSize": 19,
  "moves": [["B","Q16"], ["W","C4"], …],   // 从对局开始到当前的全部着法（GTP，pass 为 "pass"）
  "analyzeTurns": [19],      // 让引擎分析第几手之后的局面（这里是“最后”）
  "maxVisits": 400,          // 本次搜索的访问量上限
  "includeOwnership": true,  // 是否返回每个点的势力图
  "includePolicy": false,    // 是否返回策略网络原始概率
  "reportDuringSearchEvery": 0.4,  // 搜索期间每 N 秒回一帧中间结果（null = 只在结束回）
  "analysisPVLen": 12,       // 每手候选的 PV 长度
  "overrideSettings": { "reportAnalysisWinratesAs": "BLACK", "humanSLProfile": null, "ignorePreRootHistory": null }
}
```

另有两条控制消息：`TerminateQuery {id, action:"terminate", terminateId}`（提前终止
另一条查询）与 `VersionQuery {id, action:"query_version"}`（连通性/版本探测，评测用）。

**查询 id 是一等公民**。约定格式是
`"<sessionId>:<nodeId>:<kind>:<nonce>"`——sessionId 与 nodeId 让引擎帧能认回
“哪个会话的哪个节点”，kind 便于排障，nonce 保证唯一。客户端侧能据此做
`parseLiveQueryId` 之类解析。

`QueryBuilder.kt` 把“用途”翻译成字段组合，一张表看懂差异：

| 用途（kind） | analyzeTurns | includePolicy | reportEvery | PV | maxVisits 来源 | 说明 |
| --- | --- | --- | --- | --- | --- | --- |
| `live` | [最后手] | 否 | 0.4s | 12 | playVisits | 局中实时分析：带进度帧 |
| `review` | [最后手] | 否 | 0.4s | 12 | reviewVisits | 整局复盘单节点 |
| `genmove` | [最后手] | 否 | 0.4s | 12 | playVisits | 最强 AI 选点 |
| `rank` | [最后手] | 是 | 0.4s | — | 1 | 段级 AI：策略探测 |
| `human` | [最后手] | 是（humanPolicy） | 0.4s | — | 1 | 拟人 AI：人形策略 + `humanSLProfile=preaz_<段级>` |
| `forecast` | [含预测首着] | 否 | **null** | 15 | reviewVisits | 预测：只要一次终答 + PV |
| `forecast-own` | 未来每一层 | 否 | **null** | 15 | reviewVisits | 预测各层的势力图（流式多帧） |
| `test` / `bench` | [0]（空盘） | 按需 | null | — | 2 / 梯子 | 连通性测试与评测 |

注意几个机制要点：

- **“当前局面”用整条 `moves` 表达**。客户端从不发“差分”，每条查询都带从空盘到
  当前的全部着法；KataGo 负责摆盘。查询的盘面因此永远与 `tree` 一致，不存在状态漂移。
- **胜负/胜率一律以黑方视角报告**：引擎配置 `reportAnalysisWinratesAs=BLACK` 全局
  兜底，查询 override 再显式要求一次（`Perspective.toBlackView` 因而近乎恒等）。
  若未来接第三方服务，注意 `toBlackViewFromSideToMove` 这条换算路径的存在意义。
- `forecast` 与 `forecast-own` 关掉进度帧（`reportDuringSearchEvery=null`），因为
  预测要的是“最终完整结论 + 长 PV”，中间帧只会浪费在飞额度。

## 6.4 响应协议

响应同样是一个 JSON 对象（`AnalysisResponse`）：

```jsonc
{
  "id": "…",
  "isDuringSearch": true,     // 是否为搜索中的进度帧
  "error": null, "field": null, "warning": null,  // 引擎错误/字段/警告
  "action": null, "version": null,   // query_version 的答复
  "turnNumber": 19,           // 本帧属于第几手后的局面（多 analyzeTurns 时区分）
  "rootInfo": { "winrate": 0.63, "scoreLead": 4.2, "visits": 400, "currentPlayer": "W" },
  "moveInfos": [ { "move": "C4", "order": 0, "visits": 210, "winrate": …, "scoreLead": …,
                   "prior": …, "pv": ["C4","Q16",…] } ],
  "policy": […],              // size²+1 个点（含 pass）的原始策略
  "humanPolicy": […],         // 人形网络策略（human 查询）
  "ownership": […]            // size² 个点（行优先，与 cells 同序）的黑方势力
}
```

客户端处理（`AnalysisClient.handleFrame`）的顺序就是协议语义：

1. 反序列化失败直接丢弃（可能是网关混入的非协议行）；
2. 若 `id` 存在且**不是**进度帧（`!isDuringSearch || error != null`），先尝试完成
   对应的 waiter——一次性查询（`OneShot`，`CompletableDeferred`）或流式
   （`Stream`，Channel）——这是“问一答一”的返回通道；
3. `action != null && rootInfo == null` 的帧（query_version 答复）到此为止；
4. 若帧不属于“当前在飞查询”（id 不匹配），丢弃；
5. 有 `error` → 状态置 `Ready` 并记录错误详情（引擎会先停掉那条查询）；
6. 属于 live 类在飞查询时（`publishLive`），把 `rootInfo` 换算成黑方视角后写入
   `_live` StateFlow——SessionViewModel 再决定如何落到界面。

**ownership 的下标约定与棋盘格子一致**（`y * size + x` 行优先），死子判定与势力图
绘制都直接复用这套下标；`Perspective.ownershipIndex(point, size)` 是唯一转换口。

## 6.5 客户端：单连接、单在飞查询

`AnalysisClient` 是 AppScope 单例，全局只有**一条 WebSocket**、同一时刻**最多一条
在飞分析查询**。为什么要单在飞？KataGo analysis 本来可以并发多条，但客户端要的是
“当前界面只看当前节点的分析”，并发只会浪费引擎资源并把结果搞混；因此用一条严格
纪律替代并发：

- 发起新查询前，若已有在飞查询，先发 `TerminateQuery` 终止它（`terminateLive()`）；
- `finishQuery` 在查询结束（无论成功/超时/取消）后也会补发一次 terminate，把引擎
  侧彻底清干净；
- 一次性查询与流式查询共用一个 `waiters` 表（id → OneShot/Stream），互不冲突；
- `analyzeLive` 的幂等保护：同 session 同节点已在飞就直接返回。

这个模型换来一个非常有用的性质：**`AnalysisClient._live` 永远只描述“用户当前正看着
的那个盘面”**，UI 层订阅它做实时刷新即可，无需自己做查询去重。

## 6.6 连接生命周期与退避重连

`AnalysisClient` 在 init 里订阅 `settings.engineProfile`（`distinctUntilChanged` +
`collectLatest`）——**改设置里的引擎地址/访问量会直接触发重连**。`runConnection`
循环：

```
Disconnected（URL 为空）
   → Connecting → Ready（onOpen）/ Error（失败，记录原因）
   → 断开 → delay(backoff) → 重试；backoff 从 1s 起倍增，封顶 15s
```

引擎状态机 `EngineStatus(phase, detail)` 的 phase 有
`Disconnected / Connecting / Ready / Analyzing / Error`，`online = Ready || Analyzing`。
URL 规范化（`normalizeEngineUrl`）：缺 scheme 补 `ws://`、补尾斜杠、`token` 非空时
拼成 `?token=` 查询参数。等待在线 `awaitOnline()` 最长 12 秒；单次查询超时 60 秒；
流式（forecast-own 多回合）超时 = 60s + 8s/回合。会话侧在 init 里收集
`analysis.status`，检测到“刚变 online”就触发 `afterPositionChange()`（补发该发的
AI 回合/实时分析）。

## 6.7 视角与候选点换算

引擎返回的数值要过一遍 `Perspective.kt` 才能上屏：

- **胜率视角**：全部按黑方处理（`toBlackView`）；白方回合的胜率也要保持“黑方胜率”
  口径，展示层负责转成“当前行棋方优势”。
- **候选点**（`selectCandidates`）：从 `moveInfos` 里挑出损失（`pointsLost`）≤
  1.5 目以内的点（`ACCEPTABLE_POINTS_LOST`），最多 10 个（`MAX_CANDIDATES`），
  除非别无选择否则不含 pass。损失 = 该手胜率对应目差与最佳着手之差
  （`pointsLost`，按行棋方换算）。
- **死子判定**（`classifyDead`）：只认 ownership，带滞回——一个点要进入“死”需要
  敌方势力 ≥ `DEAD_ENTER = 0.70`，但已经标死的点要等敌方势力跌回
  `DEAD_LEAVE = 0.45` 才取消，且要求访问量 ≥ `DEAD_MIN_VISITS = 20`（防噪声）。
  滞回是为了避免势力图微幅抖动时死子标记来回闪。
- **数据保持**（`heldScalar` / `heldOwnership` / `lerpOwnership`）：新数据没到位时
  保留旧值；势力图切换时逐点插值，形成“势力迁移”动画。

## 6.8 引擎评测 Benchmark

设置页的“评测引擎”把“引擎到底行不行”拆成**网络、硬件、手感**三个独立结论
（`EngineBenchmark.kt`），避免把“网络慢”误判成“服务器弱”。步骤：

1. Warmup：一次 9×9 双访问量空盘查询；
2. Ping ×20：`query_version` 往返 → 中位/p95/最大 RTT；ping 全失败时用 400→2000
   访问量的搜索耗时斜率反推 RTT（`rttFromSearchSlope`）；
3. Policy ×8：1 访问量 + 策略的探测 → 策略延迟；
4. Search 梯子：80 / 400 / 2000 访问量三档实测 → VPS（访问量/秒），用 RTT 校正
   （`correctedVisitsPerSec`）；
5. Human（可选）：`preaz_5k` 人形策略探测，失败只降级不禁用整体结论。

结论组装（`assembleReport`）：

- `NetworkReport`：按中位 RTT 分档 `Local(≤20ms) / Lan(≤50) / Nearby(≤120) /
  Distant(≤300) / HighDelay`；
- `HardwareReport`：VPS 从 20 到 15000 对数映射成 0–100 分与档位
  （WeakCpu→HighEnd），附带 `playVisits` 的每手等待时间估算；
- `PlayFeel`：网络 × 硬件的手感矩阵（如“GPU 有余而等网络”“网络本地但搜索受限”）。

UI 展示采用连续进度（设置页 `BenchUiState`），“测试连接”则单独发一条 2 访问量
9×9 查询并回报实际访问量或引擎错误。
