# 第 1 章 项目概览

## 1.1 这是什么

KataHana 是一个**围棋客户端**：对局、复盘、棋谱都在自己的设备上完成，局势分析来自
一个独立的引擎。它把“下棋”拆成两部分：

- **规则与交互由应用自己负责**：合法着法、提子、打劫、终局判定都在客户端内完成；
- **强度与判断由 KataGo 负责**：落子建议、胜率、目差、势力分布、棋谱整局复盘，
  都通过一个 Python 网关进程向 KataGo 发起分析查询得到。

这两部分的边界要先说清楚，否则容易高估这个客户端的“独立运行”能力：**没有可达的
引擎，客户端仍然能完整进行人人对局、打谱、存档，但所有“判断类”功能——AI 对手、
胜率/目差、势力图、整局复盘、局势预测——都会保持空白或不可用**。引擎部署在哪台
机器上与客户端无关：最常见的形态是网关与 KataGo 跑在桌面电脑上，手机通过局域网连
过去；但只要对方说同一套 WebSocket 协议，客户端设置里的引擎地址可以指向任何一台
机器（甚至公网）。文档中出现的“本机/本地”指的是“由你自行部署、不属于任何云服务”，
并不承诺客户端与引擎必须同机。

客户端通过一套代码跑在三个平台上：桌面（JVM）、Android、iOS。三个平台的业务代码
全部收在同一个 `shared` 模块里，平台外壳（`desktopApp`、`androidApp`、`iosApp`）
只负责窗口、生命周期与少量系统能力。

## 1.2 技术栈与版本

| 技术 | 版本 / 说明 |
| --- | --- |
| Kotlin | 2.4.10 |
| Compose Multiplatform | 1.12.0（material3 1.12.0-alpha03） |
| Gradle / AGP | AGP 9.1.1；Kotlin Multiplatform + Android Multiplatform Library |
| 依赖注入 | Metro 1.4.2（编译期图，`AppScope` 单例 + `@Assisted` 工厂） |
| 导航 | Voyager 2.2.21-1.10.3（Navigator + FadeTransition） |
| 网络 | Ktor 3.5.2（`client-websockets`）；Android=OkHttp、JVM=CIO、iOS=Darwin |
| 序列化 | kotlinx-serialization-json 1.11.0 |
| 状态 | kotlinx-coroutines 1.11.0（StateFlow/Channel）；Compose `collectAsState` |
| 持久化 | AndroidX DataStore Preferences 1.2.1（设置）；自定义 JSON 文件（最近对局）；手写 SGF |
| 模糊亚克力 | dev.chrisbanes.haze 2.0.0-beta02（`haze` + `haze-blur`） |
| 引擎 | KataGo `analysis` 模式（网关 `engine/server.py` 拉起），WebSocket JSON 协议 |
| 文档 | mdBook（`doc/`），源文件在 `doc/src/` |

## 1.3 仓库与模块布局

```
KataHana/
├── shared/                 # 全部共享业务代码（KMP: jvm / android / ios）
│   └── src/
│       ├── commonMain/     # 平台无关：domain / ai / engine / ui / settings / sgf / recents
│       ├── androidMain/    # expect 的 Android actual（OkHttp、文件目录等）
│       ├── jvmMain/        # expect 的 JVM actual（CIO、AWT 文件对话框等）
│       ├── iosMain/        # expect 的 iOS actual（Darwin、NSFileManager 等）
│       ├── commonTest/     # 纯逻辑单元测试（绝大多数测试在这里）
│       ├── jvmTest/        # 桌面侧测试（含真引擎 smoke test、序列化测试）
│       └── androidHostTest/# Android host 测试
├── androidApp/             # Android 外壳：Application、MainActivity、清单与资源
├── desktopApp/             # 桌面外壳：main.kt、打包配置、图标
├── iosApp/                 # Xcode 工程，调用 shared 产出的 Shared.framework
├── engine/                 # Python 网关：server.py、analysis.cfg、requirements.txt
├── model/                  # KataGo 网络权重（由网关自动下载，git 忽略）
├── doc/                    # mdBook 开发手册（本手册）；doc/src/ 是源，doc/book/ 是产物
├── screenshots/            # 各版本截图
└── README.md / -CN / -JP   # 面向用户的说明（三语）
```

`shared` 的 commonMain 内部按包分工（详细地图见 `AGENTS.md` 与各章）：

| 包 | 内容 | 依赖 |
| --- | --- | --- |
| `domain` | 棋盘、规则、棋树、对局会话、评估值类型 | 纯 Kotlin |
| `ai` | 三家“棋手”与棋力评估 | → domain、engine.MoveInfo |
| `engine` | 引擎客户端、协议 DTO、候选点/死子逻辑、评测 | → domain、settings |
| `settings` | 设置仓库（DataStore） | 独立 |
| `recents` | 最近对局索引与存档 | → domain、sgf |
| `sgf` | SGF 读写与平台文件对话框 | → domain |
| `ui` | 主题、组件、棋盘、各屏幕与 ViewModel | → 上面全部 |
| `changelog` | git 历史解析（App 内“更新内容”） | 独立 |

## 1.4 三条设计决策

理解这三个决定，就理解了项目里大部分“为什么”。

**决策一：规则在客户端，判断在引擎。** 盘面（`Position`）、提子与禁着点（`Rules`）、
连续两虚着终局都在 commonMain 的纯 Kotlin 代码里，因此引擎不可达（网关没启动、
网络不通）时，规则判定与人人对弈依然完整可用。
但客户端**从不自己数地盘、算目差、算胜率**——这些全来自引擎查询。想改终局判定，
改 `Rules`；想改胜率/目差口径，改发给引擎的查询与 `Perspective`。

**决策二：一局棋就是一棵树，复盘只是移动游标。** 棋局历史用 `GameTree` 保存，
树上每个节点是一手棋之后的盘面。玩家“当前所在”由唯一游标 `current` 表示；
**“复盘”不是一个特殊模式，而是游标不在叶子上（`current.children` 非空）的状态**。
撤销、回退、换变着、点棋盘复盘其实都是同一个操作：移动 `current`。这一设计让
“打谱复盘”“悔棋”“变着分析”共享同一套机制，代价是任何操作都必须维护好
`preferredChild` 与“最近叶子”这两个辅助状态（第 3 章详述）。

**决策三：引擎是外部服务，通过网关访问。** 应用不链接 KataGo 库，而是连接一个
WebSocket 端口——应用设置里的默认引擎地址是 `ws://127.0.0.1:2080`，由仓库自带的
`engine/server.py` 提供（网关默认监听 `0.0.0.0:2080`，局域网内的手机也能直接连），
也可以指向同一网络甚至公网上的任何一台机器。网关把一个 KataGo `analysis` 子进程
变成“一问一答”的 JSON 服务，负责进程管理、模型下载与查询 id 路由。
好处是客户端不需要关心 KataGo 的命令行与并发模型，只要维护一条 WebSocket 连接；
代价是引入了“单连接 + 单在飞查询 + 终止旧查询”这一套协议纪律（第 6 章）。

## 1.5 一条落子的完整链路

以人类玩家在棋盘上点下一手为例，走一遍整条数据路径（这也是全文的“地图”）：

```
手指/鼠标点击棋盘
  → ui/board/BoardCanvas 的指针处理：命中测试（BoardHit）得到交点 Point
  → SessionScreen 回调 vm.onActivate(point, isTouch)
  → SessionViewModel.commit(point)
      · cancelReviewQueue()：中止整局复盘队列
      · snapshotLiveToCurrentNode()：把此刻实时分析结果记入节点评估缓存
      · session.play(point)（→ GameTree.play → Rules.tryPlay 校验 → append 新节点）
  → publish()：唯一状态中枢
      · cancelForecast()：清掉任何进行中的局势预测
      · _state.update { ... }：从 session.snapshot() 重新派生 SessionUiState
        （含 withAnalysis()：重算质量标记、胜负曲线、统计）
  → afterPositionChange()：若是 AI 回合则触发 AI 思考；否则发起实时分析
  → Compose 通过 collectAsState 拿到新状态，整棵 UI 重组
```

AI 落子则是同一链路的“引擎侧”变体：`afterPositionChange()` 检测到轮到一个 AI
座位，`playAi()` 按座位类型发查询（rank/human/genmove），用对应 Bot 从引擎输出里
选一手，再走 `session.play/pass → publish()`。中间任何导航都会通过 `bumpNav()` 让
旧任务失效（第 4 章）。
