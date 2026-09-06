# 第 2 章 领域核心：棋盘与棋规

本章讲的是本仓库**如何实现**围棋规则，不是规则本身。气、提子、自杀、superko、
终局、真假眼、立尖飞跳虎以及棋形与行局，见手册开头的《围棋入门》。没读过那一章
请先回去读：下面的函数顺序和非法原因枚举，都是那些概念的直接翻译。

## 2.1 包结构与依赖方向

`domain/` 是整个项目唯一“只依赖 Kotlin 标准库（外加一处 Voyager 序列化接口）”的包。
它**没有任何响应式原语**——没有 `StateFlow`、没有 `mutableStateOf`、没有回调监听器，
也看不到 Compose 或 Android 的 import。原因见第 4.1 节：领域层保持“纯命令式 + 被快照”，
UI 层每次变更后重新读取状态，而不是让领域对象主动通知。

本包内的文件：

| 文件 | 内容 |
| --- | --- |
| `Point.kt` | 棋盘交点、GTP 坐标互转、星位 |
| `StoneColor.kt` | 黑白与格子常量（0/1/2） |
| `Position.kt` | 不可变盘面 |
| `Move.kt` | 着法、非法原因、落子结果 |
| `Rules.kt` | 规则引擎（提子/自杀/打劫/终局） |
| `GameTree.kt` | 棋树（第 3 章） |
| `GameSession.kt` | 对局门面与 `SessionSnapshot`（第 3、4 章） |
| `GameConfig.kt` | 对局参数：棋盘、贴目、双人座位 |
| `TreeLayout.kt` | 棋树渲染布局（第 3 章） |
| `Forecast.kt` | 局势预测的值类型与模拟（第 4 章） |
| `EvalSeries.kt` | 胜负曲线、质量统计等派生值类型（第 4 章） |

## 2.2 坐标与棋盘表示

`Point(x, y)` 是棋盘交点。坐标约定和“画布”一致：`y = 0` 在棋盘顶部、向下递增；
而 GTP 坐标（给引擎、写 SGF、显示坐标）是围棋惯例：横坐标用字母 `A–T` 跳过 `I`，
纵坐标从底部向上从 1 编号。`Point` 提供：

- `index(size)`：行优先序号 `y * size + x`，`IntArray` 的下标；
- `toGtp(size)` / `Point.fromGtp(gtp, size)`：与 GTP 字符串互转；
- `inBounds(size)`；
- `hoshiPoints(size)`：星位集合（渲染用）。

盘面 `Position` 是**不可变**值对象，核心字段：

- `cells: IntArray`：行优先的格子数组，0 = 空、1 = 黑、2 = 白（见 `StoneColor` 的
  `EMPTY_CELL / BLACK_CELL / WHITE_CELL`）；
- `toPlay: StoneColor`：该轮到谁；
- `capturedByBlack / capturedByWhite`：双方累计提子数（是“下到这一手为止”的累计值，
  不是“当前盘面差”）；
- `history: List<Long>`：**从对局开始到当前**每一手之后的盘面哈希（含当前盘面），
  供打劫判定；
- `consecutivePasses`：连续虚着次数；
- `hash: Long`：当前盘面哈希。

`Position.empty(size)` 只允许 9 / 13 / 19；`Position.of(...)` 供测试与摆子。
因为盘面不可变，每一手都产生新 `Position`，旧的可以安全保留在棋树节点上——这正是
“悔棋/复盘零成本”的基础。

## 2.3 着法与规则引擎

着法是一个密封类：

```kotlin
sealed class Move {
    data class Place(val color: StoneColor, val point: Point) : Move()
    data class Pass(val color: StoneColor) : Move()
}
```

落子结果是 `PlayResult.Ok(position, captured)` 或
`PlayResult.Illegal(reason)`，非法原因枚举 `Occupied / Suicide / Superko / GameOver / OutOfBounds`。

`Rules` 是纯函数对象，核心是 `tryPlay(position, point)`。它的判定顺序本身
就是规则语义，值得记住：

1. `consecutivePasses >= 2` → `GameOver`（终局后不能再落子）；
2. 越界 → `OutOfBounds`；已有子 → `Occupied`；
3. 在副本上落子；
4. 用栈式 BFS（`groupAndLiberties`）找出四周被围死的**对方**棋串，全部提掉，
   返回被提的点集合；
5. 若己方新棋串没有气 → `Suicide`；
6. **全历史 superko**：若新盘面哈希已经出现在 `history` 里 → `Superko`。
   注意这里不是“单劫立刻回提”那种简化判定，而是整盘历史盘面都不允许重现，
   因此长劫、多劫循环都会被拦下；
7. 生成新 `Position`：换手、累计提子、`history` 追加新哈希、虚着计数清零。

`tryPass` 只做一件事：换手并把 `consecutivePasses + 1`（棋谱、终局都靠它）。
`legalMoves` 枚举所有空点逐个试。`liberties` / `groupPoints` 供 AI、势力图、
连接形状等上游查询。

> **为什么客户端自研规则而不用引擎判棋？** 一是即时性——UI 需要在点击瞬间知道
> 落子是否合法、提了哪些子，任何一次网络往返都不可接受；二是确定性——规则是
> 硬逻辑，不应受引擎配置影响。规则与判断的边界因此非常清晰：**规则在本地、
> 判断在引擎**。

## 2.4 座位、段级与对局配置

`GameConfig` 描述一局的外部参数：`boardSize`、`komi`（贴目，Float，默认 7.5）、
`black` 与 `white` 两个 `PlayerSeat`。座位不只是“人/AI”，而是四种
`SeatKind`，这是 AI 强度的总开关：

| SeatKind | 含义 | 走子用 Bot | 引擎查询 |
| --- | --- | --- | --- |
| `Human` | 人类 | — | — |
| `HumanLike` | “拟人”AI（人类风格） | `HumanBot`（对人形网络策略采样） | `queryHuman`（`humanSLProfile=preaz_xk`） |
| `Rank` | 段级 AI（KaTrain 校准） | `RankBot` | `queryRank`（策略网络） |
| `Full` | 最强引擎 | `FullStrengthBot`（取 KataGo 首选） | `queryGenmove` |

`PlayerSeat(kind, rankKyu)` 的 `rankKyu` 是围棋段级：正数 = k 级（`5k`），0 = `1d`，
负数 = 更强（`-1` = `2d`，`-2` = `3d`），范围 −2..15。`rankLabel` / `humanSlProfile`
负责把段级显示成 `5k` 之类并映射成引擎的 `preaz_<label>` 人形策略档位。

`GameConfig` 派生出一堆便捷属性：`mode`（HvH / HvAI）、`rankKyu`、`humanPlaysBlack`、
`aiStyle`、`seat(color)`。`GameConfig` 与 `PlayerSeat` 实现了 Voyager 的
`JavaSerializable`——作为导航参数跨屏幕传递（只在桌面/Android 用，iOS 不会走到
Java 序列化）。文件里另有一组旧版构造函数与 `seatsFromLegacy` 等转换函数，用于把
早期“mode + aiStyle + rankKyu”形态的设置迁移成现在的座位模型。

## 2.5 谁在“数胜负”

客户端**没有目数/地盘计算**。凡是涉及“现在谁领先”“哪些子是死子”“哪里是谁的势力”
的问题，答案一律来自引擎查询的 `scoreLead` 与 `ownership`，经 `engine/Perspective.kt`
换算后展示（第 6、7 章）。终局判定只认“连续两虚着”——之后 UI 会把局面交给引擎
做最终的目差/势力评估，而不是本地数子。这意味着如果你想加“本地数子确认死活”，
需要新建一套完全独立于现有链路的逻辑；目前代码里不存在这条路径。
