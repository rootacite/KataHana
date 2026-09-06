# 第 8 章 构建、测试与工程实践

## 8.1 构建系统

仓库根 `build.gradle.kts` 只声明插件（不发布），`settings.gradle.kts` include
`:shared`、`:androidApp`、`:desktopApp`（`iosApp/` 是 Xcode 工程，不走 Gradle）。
版本集中在 `gradle/libs.versions.toml`（version catalog），关键版本：

| 项 | 值 |
| --- | --- |
| Kotlin / AGP | 2.4.10 / 9.1.1 |
| Compose Multiplatform / material3 | 1.12.0 / 1.12.0-alpha03 |
| Ktor（websockets） | 3.5.2 |
| kotlinx-serialization / coroutines | 1.11.0 / 1.11.0 |
| Metro / metrox-viewmodel | 1.4.2 |
| Voyager | 2.2.21-1.10.3 |
| Haze | 2.0.0-beta02 |
| DataStore Preferences | 1.2.1 |
| Android SDK | compile 37 / min 24 / target 36 |
| JVM | toolchain 17（编译目标）；Gradle daemon 用 AZUL 21（`gradle/gradle-daemon-jvm.properties`） |

`shared/build.gradle.kts` 定义了 KMP 目标：`jvm`（JVM_17）、`android`（
`com.android.kotlin.multiplatform.library`，host + device 测试）、
`iosArm64` + `iosSimulatorArm64`（产出静态 framework `Shared`）。commonMain 的
源集会额外叠加 `generateHanaInfo` 生成目录（见 8.3），并在不同 sourceSet 挂对应
Ktor 引擎（okhttp/cio/darwin）。`gradle.properties` 打开 configuration-cache，
`hana.version` 是 Android `versionName` 的来源（默认 "1.0"）。**加新平台依赖先查
version catalog 有没有现成版本，不要在脚本里散落裸版本号。**

## 8.2 三端入口与打包

| 平台 | 入口 | 说明 |
| --- | --- | --- |
| 桌面 | `desktopApp/…/main.kt` | Metro `createGraph<AppGraph>()`，1280×800 窗口，注入 `LocalSgfFiles(JvmSgfFiles())` 与 `LocalAppExit`（退出应用），调公共 `App(graph.metroViewModelFactory)`。`compose.desktop` 打包 Dmg/Msi/Deb。 |
| Android | `KataHanaApp` + `MainActivity` | Application 里 `installAppContext(this)`（设置文件路径需要 context）+ 懒建图；Activity 开沉浸式（边缘到边缘、透明状态栏、可滑出系统栏），注入 `AndroidSgfFiles` 与 `finishAffinity()` 退出。 |
| iOS | `iosApp/` + `MainViewController.kt` | Xcode 工程调用 shared 的 framework；共享入口仍是公共 `App()`。 |

打包/签名注意：Android release 目前用 debug 签名配置、不混淆，正式分发前要换成
release 签名。运行命令见 AGENTS.md 或 README：桌面
`./gradlew :desktopApp:run`（热更 `:desktopApp:hotRun --auto`）。

## 8.3 AppInfo 与内建变更日志

`generateHanaInfo`（shared 构建任务）在构建期执行 git：取短哈希、最近 tag、以及最近
40 条 `log --pretty=format:%H|%h|%ad|%s`，生成 `generated/AppInfo.kt`：
`version / gitHash / changelog: List<ChangelogEntry>`。`changelog/Changelog.kt` 负责
把 `hash|date|subject|tags` 行解析成条目，从提交信息里剥出 `(tag)` 所有者标记与
`[Kind]` 前缀、识别 git decorate 的 `tag: vX.Y`。Home 的更新区渲染最近 16 条。

推论：**提交信息 = 产品更新日志的原料**。规范写提交：`[Feat]` / `[Fix]` / `[doc]`
前缀（可带 `(owner)`），主题简短清晰；不要用无法归类的随意文案。

## 8.4 测试布局与运行

测试按“能不能跑在纯 JVM/Android host 上”分层：

| 目录 | 内容 | 运行 |
| --- | --- | --- |
| `commonTest` | 绝大多数纯逻辑测试（~20 个文件，约 190 个用例） | `:shared:jvmTest` 与 `:shared:testAndroidHostTest` 都覆盖 |
| `jvmTest` | 桌面专属：`SessionScreenSerializeTest`（Voyager 屏幕 Java 序列化往返）；`RealEngineSmokeTest`（真连 `ws://127.0.0.1:2080`，连不上时**自跳**并打印 SKIP，不 fail） | `:shared:jvmTest` |
| `androidHostTest` | Android host 冒烟（带资源） | `:shared:testAndroidHostTest` |

测试约定：用 `kotlin.test`，方法名是平铺的 camelCase 描述（不用反引号长句）；
**测试文件与生产文件同名**（`GameTreeTest` 测 `GameTree`…）；随机性全部用显式种子；
跨模块的引擎会话用自造的 `FakeAnalysisServer`（内存 WS 服务），不依赖真引擎。
领域与引擎解析类的大量边界（规则、棋树、SGF、协议 JSON、评测分档）都有专门用例——
**改这些逻辑必须带测试**，跑完 `jvmTest` 再跑 `testAndroidHostTest`。

## 8.5 代码约定

- **文案**：用户可见文字一律进 `ui/Copy.kt`；屏幕不裸写字符串。
- **颜色**：原始色值只在 `ui/theme/Color.kt`/`Appearance.kt`；绘制代码用参数传入的
  颜色（composable 里先 `hanaColors` 捕获）。新增外观要保证质量语义色共享
  （`AppearancePaletteTest` 会查）。
- **可交互控件默认实心**：不改全局 `CapsuleButton` 默认样式；“瓷器”半透明只用于
  表面/卡片。改 UI 风格前先想清楚它属于哪一类。
- **不重排抽屉信息架构；不给棋盘加 GlowOrbs/背景装饰**（明确约定）。
- **结构与命名**：单文件对应一个顶层类型（组件族如 `ScreenChrome`/`Widgets` 除外）；
  注释简短、说明“为什么”与不易看出的约束，不做过程叙述；绘制/几何的魔法数用
  具名常量或就地注释；Compose 资源 import 用生成的全包名
  （`katahana.shared.generated.resources.*`）。
- **状态纪律**：领域只被 ViewModel 改；异步结果先校验 `navEpoch`/节点再写状态；
  任何树变更后走 `publish()`（第 4 章）。
- **提交**：`[Feat]/[Fix]/[doc]` 前缀；默认不 push、不自行打 tag（tag 只在被要求
  升版时打 `vX.Y`）。

## 8.6 修改 UI 的验证要求

UI 改动必须在“真实运行”层面验证，不能只看编译：

1. `./gradlew :shared:jvmTest :shared:testAndroidHostTest` 保底；
2. 桌面跑 `./gradlew :desktopApp:run`（需要时先起 `python3 engine/server.py`），
   把改动**端到端走一遍**：能点就点、能开弹窗就开弹窗、横竖屏与窄窗口各看一遍、
   空态/错误态/开关变体都过一下；
3. 改动影响“共享状态”的（如设置、主题、存档字段），要把读同一份状态的其它界面
   也检查一遍，防止一处改对、别处读挂；
4. 本仓库是 Compose 桌面/Android 应用，没有浏览器工具可用时，“运行桌面端 + 测试”
   就是最接近的验证手段；验证不了的部分（如真机 Android/iOS 触摸、后台恢复）
   要明说没验。

## 8.7 文档同步义务

`doc/` 下的中文手册是**活文档**：任何改动触及模块职责、公开 API、协议、状态流转、
UI 结构或构建方式，都要**在同一个提交里**同步手册的对应章节（改动小就在相关章节
补一句；改动大就重写该节）。构建预览：

```bash
mdbook serve doc --open     # 本地预览
mdbook build doc            # 产物在 doc/book/（git 忽略，永不提交）
```

改完 `doc/src/` 后跑一次 `mdbook build doc`，确认无链接错误再收工。

## 8.8 速查表

| 想改什么 | 源码 | 手册 |
| --- | --- | --- |
| 规则 / 提子 / 打劫 / 合法着法 | `domain/Rules.kt`、`domain/Position.kt` | 2 |
| 棋盘坐标 / GTP | `domain/Point.kt` | 2 |
| 棋树 / 撤销 / 复盘语义 / 最近叶子 | `domain/GameTree.kt` | 3 |
| 会话门面 / 快照字段 | `domain/GameSession.kt` | 3、4 |
| “AI 该不该走” | `GameSession.aiShouldMove()` | 3 |
| 会话状态 / 发布时机 | `ui/session/SessionViewModel.kt` | 4 |
| AI 选子 | `ai/*Bot.kt` + VM `playAi()` | 4 |
| 整局复盘队列 | VM `analyzeGame()` | 4 |
| 局势预测（长按） | VM `onForecast()` + `domain/Forecast.kt` | 4 |
| 发给引擎的查询 | `engine/QueryBuilder.kt` | 6 |
| 协议 DTO / 字段 | `engine/AnalysisDto.kt` | 6 |
| 连接 / 重连 | `engine/AnalysisClient.kt`、`WsClient.kt` | 6 |
| 胜率视角 / 候选 / 死子 | `engine/Perspective.kt` | 6 |
| 引擎评测 | `engine/EngineBenchmark.kt` | 6 |
| 网关 / KataGo 启动 | `engine/server.py`、`engine/analysis.cfg` | 6 |
| 设置键 | `settings/SettingsRepository.kt` | 7 |
| 最近对局存档 | `recents/*` | 7 |
| SGF 读写 | `sgf/SgfIo.kt` | 7 |
| 平台差异 / 文件位置 | 各 expect/actual + `settings/SettingsPath.kt` | 7 |
| 导航 / 返回 / 生命周期 | `ui/navigation/`、`App.kt`、各 Screen | 5 |
| 对局布局自适应 | `ui/session/SessionLayout.kt` | 5 |
| 棋盘绘制 / 手势 / 动画 | `ui/board/*` | 5 |
| 外观 / 色板 / 主题 | `ui/theme/*` | 5 |
| 用户文案 | `ui/Copy.kt` | 5 |
| 弹窗亚克力 | `ui/components/HanaDialogs.kt`、`ScreenChrome.kt` | 5 |
| 构建 / 依赖 / 入口 | 各 `build.gradle.kts`、`gradle/libs.versions.toml` | 8 |
| 测试 | `shared/src/*Test/*` | 8 |
