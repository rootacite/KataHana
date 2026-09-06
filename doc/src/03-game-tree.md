# 第 3 章 棋树与对局状态机

## 3.1 为什么需要一棵树

一局棋如果只是“手数列表”，就只能前进与后退，无法回答三个产品问题：

1. **复盘要看变着**——每一步可能有多个后续，复盘者要能跳到任意分支；
2. **悔棋要保留分支**——悔棋后走出新手，不是覆盖旧着，而是长出一根新分支；
3. **整局分析要挂数据**——每个盘面都可能有自己的胜率/目差/势力图，这些数据必须
   能跟着“某个盘面”走，而不是跟着“第几手”走。

因此 KataHana 用一棵**树**保存整局，树的每个节点 = “下到某一手之后的完整盘面”。
凡是需要记在某个盘面上的东西（评估、胜率曲线采样、质量标记）都以节点 id 为键存放。
这棵树的实现就是 `domain/GameTree.kt`，它的状态机是本项目数据管理的地基。

## 3.2 Node 的数据结构

```kotlin
class Node(
    val id: String,              // "n0", "n1", ... 全树唯一，稳定（存档、评估缓存都以它做键）
    val parent: Node?,           // 根节点为 null
    val move: Move?,             // 走到本节点的着法（根节点为 null）
    val position: Position,      // 本节点盘面（不可变）
    val children: MutableList<Node> = mutableListOf(),
) {
    var preferredChild: Int = 0  // 本节点“主变”是第几个孩子
    val moveNumber: Int          // 从根数起的有手数（含 pass）
    fun pathFromRoot(): List<Int>  // 从根到本节点的孩子下标序列
}
```

几个容易忽略的约定：

- **分支的身份是“父节点里的下标”**，而不是子节点自己的 id。同一个父节点下，
  孩子按产生顺序排在 `children` 里；`cycleVariation`、`preferredChild`、存档恢复
  用的 `pathFromRoot()`（一串下标）都建立在这个约定上。
- **每个节点都完整持有自己的 `Position`**。代价是多花点内存，换来任何导航都是
  O(1) 换指针、无需回溯重算。
- **`moveNumber` 数的是“着”**：祖先中 `move != null` 的个数。根节点无着法不算。

`GameTree` 自身的关键状态只有四个：`root`、`current`（私有 set 的游标）、自增的
`nextId`、以及 `lastLeafId`（“最近叶子”，见 3.6）。**“当前盘面”永远等于
`current.position`**，不存在第二份正在被修改的棋盘。

## 3.3 游标与“复盘”的定义

`GameTree` 提供三个派生布尔量：

```kotlin
val ended:    Boolean get() = current.position.consecutivePasses >= 2
val canUndo:  Boolean get() = current.parent != null
val canRedo:  Boolean get() = current.children.isNotEmpty()
val reviewing: Boolean get() = canRedo   // 不在叶子上，就有“可继续走的后续”
```

**“复盘”没有独立开关，它就是一个位置属性**：只要游标所在的节点还有孩子（也就是说
当前局面在历史上真的被继续走过），`reviewing` 就为真——这包括“悔棋后回到内部节点”
和“在打谱模式里走到中间某手”。反过来，站在叶子上的任何时刻都不是复盘。这一句话
派生出一整套 UI 行为：复盘时人类接管落子（AI 停手）、出现复盘按钮组、允许长按预测、
Game Tree 面板显示复盘进度条等。**不要在别处再维护一个 `isReviewing` 副本**，否则
必然和游标位置脱节。

`GameSession.aiShouldMove()` 把“复盘时人类接管”落成硬规则：

```kotlin
fun aiShouldMove(): Boolean =
    !tree.ended && !tree.reviewing && config.seat(position.toPlay).isAi
```

换句话说，AI 只在**叶子上的自己的回合**落子；一旦你悔棋或跳回内部，AI 立刻停手，
把棋盘交还人类。这是整个对局体验的纪律线。

## 3.4 导航操作族

所有导航都改变 `current`，并在最后调用私有 `rememberLeaf()`（见 3.6）：

- `undo()`：`current = current.parent`。没有父亲则返回 false。
- `redo()`：走到 `current` 的 `preferredChild` 指向的孩子（越界保护），不是“回到
  最后悔的那一手”。**这正是“重做走主变”的语义**。
- `undoTo(node)`：沿父链一直走到指定节点（带 10k 次保护），常用于“从任意深度
  一口气退回某一手”。
- `goTo(id)`：全树查找节点，然后**从根开始逐层把每个祖先的 `preferredChild` 指到
  通往目标的那个孩子**，最后把 `current` 设为目标。它同时完成两件事：跳转，以及
  让“主变/重做/最近叶子”都跟随这条新路线。
- `cycleVariation(delta)`：在**父节点**的孩子里切换到相邻分支（`(idx + delta).mod(n)`），
  并同步 `preferredChild`。这是复盘 UI 上“下一个变化图”按钮的实现。
- `applyChildPath(path)`：按一串下标从根走下来（设置 `preferredChild` + 移动
  `current`），用于从存档/棋谱恢复“当时在看哪一手”。路径非法时它会先
  `syncResumeLeaf()` 再返回 false——保证载入失败后也有一个可用的复盘出口。

配套查询：`childPath()`（`current` 的下标路径，用于存档）、`nodeAtPath(path)`、
`nodeById(id)`、`nodesFromRoot()`（根到当前）、`preferredLine()`（根 → 当前 →
沿 preferred 到叶，用于整局分析队列与曲线绘制，**不移动 current**）。

## 3.5 落子的幂等与分支复用

`play(point)` / `pass()` 先经 `Rules.tryPlay/tryPass` 校验，成功则调用私有
`append(move, position)`。`append` 有一个必须记住的幂等规则：

- 先看 `current.children` 里有没有**一模一样的着法**（同色、同点；pass 看同色）；
- 有 → 不新建节点，把 `preferredChild` 指过去、`current` 移过去；
- 没有 → 新建 `Node("n${nextId++}", ...)` 追加为孩子，并设为 preferred 与 current。

为什么这条规则重要：**它让“重新下一手已经下过的棋”不产生重复分支**。比如悔棋两步、
再原样走出同样两手，树不会长出平行复制品，而是回到原节点。复盘时如果玩家沿着主变
一路点下去，也永远不会撑大树形。只有真正不同的选择才会长出新枝。

## 3.6 最近叶子：退出复盘的锚点

复盘入口是“回到内部节点”，出口则需要一个明确目标。`GameTree` 维护
`lastLeafId`，语义是**玩家最后真实待过的那片叶子**（或“当前最合理续到的那片叶子”）：

- 私有 `rememberLeaf()`：`current` 是叶子（没有孩子）时，把 `current.id` 记成
  `lastLeafId`。每次 `undo/redo/goTo/cycleVariation/append/undoTo` 的末尾都会调用它。
  效果是：玩家在叶子上走动时锚点不断刷新；一旦走进内部（进入复盘），锚点**冻结**，
  直到再次落到某片新叶子才切换。走出几个变化图但没落到新叶，锚点仍是旧主线叶子。
- `lastLeaf(): Node`：有记录就用记录，否则（例如树上还没有任何叶子记录）从当前
  `current` 沿 preferred 一直下到叶子作为兜底。
- `syncResumeLeaf()`：把 `lastLeafId` 设为“从当前沿 preferred 下去的叶子”。
  它服务于**载入一棵“停在半途”的树**——比如读档/读 SGF 时游标落在内部节点，
  这时没有“玩家刚待过的叶子”可言，就用 preferred 续到叶作为可恢复目标。
  调用点是 `GameSession` 的 init（构造时若树已在复盘）与 `applyChildPath` 的收尾。
- `resumeLeaf(): Boolean`：跳到 `lastLeaf()`；若 `current` 已经是它则返回 false。
  **当目标是叶子时，跳过去的同时 `reviewing` 自然变 false，复盘就结束了。**

退出复盘在会话层是 `GameSession.exitReview() = tree.resumeLeaf()`；在 UI 层
`SessionViewModel.exitReview()` 会先 `bumpNav()`（令在飞的 AI 回合与局势预测任务
失效），再调用它，最后走 `publish()`。**离开复盘的瞬间，任何局势预测的虚子都会
消失**——不是预测模块专门配合，而是因为 `bumpNav()`/`publish()` 本身就会清空
forecast（第 4 章）。

## 3.7 树的布局与高亮

`GameTree.layout()`（`domain/TreeLayout.kt`）把树排成可画的网格并标注高亮，
产出 `TreeLayout(nodes, currentId, cols, rows)`，供 Game Tree 面板使用。排布算法：

- 根在 `col = 0`，每深入一层 `col + 1`；
- 每个节点的行号由递归返回：**主线上（`orderedKids` 里第一个）的孩子占据父节点
  所在行**，其余分支各自占新行并逐层占满——所以主变永远保持在同一横行上；
- `orderedKids` 把“在 active 路径上的孩子”排到第一位。active 路径来自
  **根 → `lastLeaf()`** 的整条链（不是从 current 出发的 preferred 链）。

每个 `TreeLayoutNode` 携带 UI 需要的一切：`col/row`、`moveNumber`、`color`、
`label`（GTP 点或 `P`/`·`）、`onActiveLine`（在这条根→末叶路径上）、`isCurrent`、
以及 `isFuture`（在这条路径上、但还没走到——用于在复盘时把“未来段”也画亮）。
高亮的语义是“**根到最近叶子的整条路始终高亮，无论是否在复盘**”；走到某条侧枝时，
`current` 用粉色描边单独标出，但主线依然亮着，复盘者随时知道自己离开主变有多远。
