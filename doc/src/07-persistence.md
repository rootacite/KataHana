# 第 7 章 持久化与平台层

## 7.1 三套持久化

客户端落盘三类数据，技术各不同、边界很清晰：

| 数据 | 存储 | 位置 | 说明 |
| --- | --- | --- | --- |
| 设置 | AndroidX DataStore Preferences | `<appDir>/katahana.preferences_pb` | 键值对，类型化流 |
| 最近对局 | JSON 文件 | `<appDir>/recent-games.json` | 列表，上限 40 条，含整局 SGF 文本与评估缓存 |
| 棋谱文件 | SGF v4 文本 | 用户选择的文件（桌面/Android 文件对话框） | 导入/导出 |

`appDir` 三端不同（见 7.5）。文件读写抽象为 `internal expect fun readUtf8(path) /
writeUtf8(path, text)`，最近对局仓库再包一层 `TextStore`（测试用
`MemoryTextStore`，生产用 `FileTextStore`）。

## 7.2 设置：DataStore

`settings/SettingsRepository.kt` 是 AppScope 单例，用
`PreferenceDataStoreFactory.createWithPath(settingsFilePath())` 建一个 Preferences
DataStore。**所有设置都以“Flow + setter”成对出现**，UI/VM 用
`stateIn(...)` 订阅、用 setter 写。键分五组：

| 组 | 键（示例） | 说明 |
| --- | --- | --- |
| 显示开关 | `show_coords / show_candidates / show_quality / show_connections / show_ownership / show_dead_stones`、`confirm_move` | 棋盘显示与“确认落子” |
| 外观 | `appearance`（默认 `sky_sakura`）、`drawer_acrylic`（浮点）、`ownership_style` | 主题与抽屉模糊度 |
| 引擎 | `engine_name / engine_url / engine_token`、`play_visits`(400)、`review_visits`(400)、`forecast_drop_ms`(400，上限 3000) | 连接与访问量预算 |
| 质量阈值 | `quality_blunder/big/mistake/inacc/fair`（12/6/3/1.5/0.5） | 复盘“损失目数→档位”分界，封装成 `QualityThresholds` |
| 上次对局 | `last_*`（尺寸/贴目/模式/段级/执黑/风格 + 双座位 kind 与段级） | Home 新对局弹层默认值 |

`engineProfile` 是引擎设置合流成的 `EngineProfile{name,url,token,playVisits,
reviewVisits}`（`AnalysisClient` 订阅它决定何时重连）。`lastGame` 会把旧版
“mode + aiStyle”形态通过 `seatsFromLegacy` 迁移成座位形态。**加新设置的标准动作**：
在 `Keys` 里加键、加 Flow、加 setter——三件套缺一不可；数值型记得 `coerce` 边界。

## 7.3 最近对局：recent-games.json

文件内容是一个 JSON 列表（`RecentGamesIndex` 负责编解码），单条 `RecentGame`：

```kotlin
data class RecentGame(
    id, title, savedAt, createdAt, moveNumber,
    boardSize, komi, mode, rankKyu, humanPlaysBlack, aiStyle,
    blackKind, whiteKind, blackRankKyu, whiteRankKyu,   // 座位（空则走旧字段兼容）
    sgf: String,          // 整局棋谱（SGF 文本），存档的真正载体
    currentPath: List<Int>, // “当时看到第几手”——树里的下标路径
    evals: List<PersistedEval>, // 逐节点评估缓存（path + winrate/score/visits/pointsLost）
)
```

设计要点：

- **存档是“SGF 文本 + 游标路径 + 评估缓存”三件套**。SGF 是树的完整内容、可被任何
  围棋软件读取；`currentPath` 是“恢复时把游标放回哪”；`evals` 让重新打开一局复盘时
  胜率曲线/质量点立刻可用，不必重新问引擎。三者分开存，也意味着**座位不写进 SGF**
  （SGF 标准没有座位概念），只存在 RecentGame 字段里。
- 仓库（`RecentGamesRepository`）内部持有 `Mutex` + `StateFlow<List<RecentGame>>`；
  `upsert` 头插并裁剪到 40 条、`remove` 删除、每次变更同步写盘并更新内存。
  `upsertSync` 是“最后抢救”用的无锁版本——VM 在 `leave()`/`onCleared()` 时把评估
  缓存刷回存档（`flushBoundEvals`）就走它。
- **打开一条存档**（`open(id)`）是理解“存档如何变回一局”的关键：`parseSgf(sgf)`
  得到 `GameTree` → `applyChildPath(currentPath)` 把游标放回当时的手数（若停在内部
  节点，`syncResumeLeaf` 会顺带修好“退出复盘”的目标）→ `record.toConfig()` 还原
  座位与对局参数 → 返回 `LoadedRecent(config, tree)` 交给 `SessionScreen`。

## 7.4 棋谱：SGF

`sgf/SgfIo.kt` 是一个**手写的 SGF v4 读写器**（无第三方库）。为什么手写？SGF 是
几十年历史的简单文本格式，引入一个完整解析库对三个平台（尤其 iOS）都更重；项目
实际用到的特性很有限。

- `writeSgf(tree, config, blackName, whiteName)`：从根 DFS 整棵树。主变写成
  `;B[…];W[…];…` 的线性序列，**变着写成嵌套括号的分支**；点坐标是标准
  SGF 的 `a–t`（与 GTP 同构：横 a–t，纵从底部）。文件头带 `FF[4]GM[1]CA[UTF-8]
  AP[KataHana]SZ[]KM[]RU[]PB[]PW[]GN[]`。转义处理 `\` 与 `]`。
- `parseSgf(text)` → `SgfGame{tree, config, blackName, whiteName, warnings}`：读
  `SZ/KM/PB/PW/AB/AW/PL`，沿主变摆子；遇到分支时“撤销回父节点再走新枝”，把
  SGF 的并行分支还原成树的兄弟节点。对非法/轮次错误的着手不崩溃，而是记进
  `warnings`（界面可提示）。读完后游标复位到根，交给上层决定跳到哪。
- 平台文件对话框抽象为 `SgfFiles` 接口，通过 `LocalSgfFiles`
  （`staticCompositionLocalOf`，默认 `NoOpSgfFiles`）注入：桌面入口提供
  `JvmSgfFiles`（AWT `FileDialog`，强制 `.sgf` 后缀）；Android 入口提供
  `AndroidSgfFiles`（SAF 的 Open/CreateDocument）；**iOS 目前没有文件对话框**，
  保持 NoOp（读 SGF 只能走“最近对局”里的存档）。

## 7.5 expect/actual 与平台差异

三端差异收敛成几条 expect/actual：

| expect（commonMain） | Android | JVM | iOS |
| --- | --- | --- | --- |
| `Platform`（name/isMobile）+ `epochMillis()` | Android SDK 名 | Java 版本 | UIDevice / NSDate |
| `readUtf8/writeUtf8(path)` | `java.io.File(filesDir, …)` | `java.io.File(~/.katahana, …)` | `NSFileManager`，iOS Documents 目录 |
| `appDir()` / `settingsFilePath()` | `filesDir`（需 `installAppContext` 在 Application 里先注入） | `~/.katahana` | `NSDocumentDirectory` |
| `createEngineHttpClient()` | OkHttp（`NO_PROXY` + 20s ping 保活） | CIO（无请求超时） | Darwin（NSURLSession） |
| `HanaScreenLifecycle` | 真前后台 owner（第 5 章） | 空操作 | 空操作 |
| SGF 对话框 | `AndroidSgfFiles`（SAF） | `JvmSgfFiles`（AWT） | NoOp |

改动跨平台行为时，先在 commonMain 加 expect，再给三个 sourceSet 补 actual——
**不要在一个平台 actual 里塞平台判断**。测试侧的对应物是 `jvmTest`（桌面）与
`androidHostTest`（Android host，带资源）两套宿主测试，见第 8 章。
