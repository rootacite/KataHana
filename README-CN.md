<p align="center">
  <img src="katahana_clip.png" alt="KataHana 图标" width="168"/>
</p>

<h1 align="center">KataHana</h1>

<p align="center">
  <strong>本地围棋 · 粉色夜色</strong><br/>
  一个以本地优先的围棋客户端，用于下棋与复盘；需要时可通过 WebSocket 接入可选的 KataGo 分析。
</p>

<p align="center">
  桌面 JVM · Android · 中国规则 · 人人对弈 · 人机对弈
</p>

---

## KataHana 是什么

KataHana 是一个围棋客户端，用来做两件事：下棋，以及事后复盘。它用 Compose Multiplatform 编写，因此游戏逻辑与绝大部分界面都集中在同一个共享模块里，由桌面（JVM）版与 Android 版共用；仓库里还包含一个 iOS 应用壳。应用完全本地化：棋盘、规则、棋谱树与保存的对局都存放在你的设备上，这意味着没有需要连接的服务器、没有需要注册的账号，也不会在离线时出现任何不可用的功能。

这个项目之所以存在，是因为围棋软件里有一个常见的两难取舍。许多工具都默认你具备两样东西之一：要么有一个在线服务可以下棋，要么本地已经装好了引擎。两种假设都有代价。服务器会把你的棋谱记录保存在别人的平台上，没有连接就用不了。引擎则通常需要针对某一种硬件打包与配置，这对 KataGo 来说尤其麻烦，因为它的二进制是跟随 GPU 分发的，无法做成一个通用的安装文件。KataHana 试图同时避开这两个假设：棋谱数据归你自己所有，引擎只是一个外部进程，仅在它可用时才与之通信。结果就是：人人对弈任何时候都能进行，而 AI 支持是加分项，而不是前提条件。

应用的其余部分建立在三个决定之上，后面各小节描述的绝大多数内容都是这三个决定的推论。

- **规则实现在应用内部。** KataHana 在本地执行围棋规则：禁止自杀落子、通过 positional superko 拒绝全局同形再现、连续两次虚着（pass）即终局。这意味着不需要任何引擎也能从头到尾下完一整盘棋，引擎只被用来做它真正擅长的事——评估与选点。
- **引擎被当作服务，而不是依赖。** 客户端通过一条朴素的 WebSocket 连接任何 KataGo 兼容的进程，逐帧交换 KataGo 自身的 Analysis JSON。胜率一律以黑方视角处理，因此数值在一局棋里不会中途变换含义。由于连接是可选的，依赖引擎的那部分界面在没有引擎可连时，就保持空白或安静。
- **AI 对手是“按颜色设置的座位”（seat），带可选定的强度，而不是一个固定设置。** 每种颜色都能独立设为人类（Human）、段位 AI（Rank AI）、拟人 AI（Human-like）或 KataGo 9D+，而且对局中途可以更换。特别地，引擎并不总被要求以全力行棋：拟人 AI 与段位 AI 会在你选定的等级（15k 到 3d，即 15 级到 3 段）上从引擎的策略输出中抽取着手，因此它们会以真实可信、接近人类的方式输棋，也正因如此适合拿来和较弱的对手练棋。
- **棋谱树就是复盘的模型。** 每一手都是分支树上的一个节点，Undo 与 Redo 就是在这棵树上行走，从较早的某一手继续落子则会分出变化。由于引擎只会在树的叶节点、且轮到自己颜色时行棋，你探索某条分支时绝不会被一个“凭空应答”的 AI 打断。

文档的其余部分会按照你实际接触的顺序介绍应用的各个部分：先是主屏，再是棋盘与对局流程，然后是分析与复盘功能，最后是引擎连接、外观设置与构建说明。

<p align="center">
  <img src="screenshots/gaming.jpg" alt="一盘进行中的 19×19 对局：右侧的棋谱树、带实时数值标签的形势曲线、着手质量统计、领地色块，以及藏在棋子下方的连络线"/>
</p>

## 功能一览

下面这份清单概括了实际使用中最突出的特性。每一项都会在后面的小节里展开说明。

- **棋盘优先的布局。** 棋盘是屏幕上最大的元素，辅助操作在需要之前都待在旁边。pass / undo / redo 紧挨着棋盘——竖屏时在顶部细栏里，横屏手机则放在侧栏——胜率用一条细窄的进度条表示。保存、候选点、棋谱树与图层开关收在抽屉里，抽屉由一枚粉色小标签打开；在宽窗口上，其中一部分内容会改为常驻在棋盘旁的侧栏里。
- **纵向坐标有意义的形势图。** 这张图同时展示目数差与胜率，可以用一个小切换器在两种模式之间切换。胜率模式下，轴是固定的：黑方 100% 在顶端、白方 100% 在底端、50% 在正中间。因此，显示的数值直接对应到一个纵向位置，而不是一个需要另行解读的偏移量。曲线的颜色偏向领先的一方，最新样本带一个小数值标签，点击图上任意位置，棋谱树就会跳到那一手。
- **以最佳着手为尺度的着手质量。** 每一手棋都会与引擎找到的最佳着手比较，损失的目数决定它的等级。阈值采用 KaTrain 的默认值（12、6、3、1.5、0.5 目），并且可以在设置中修改。双方最近几手的等级会以一张小脸画在棋盘上，棋盘下方的表格则分别统计黑、白双方各等级出现的次数。
- **画在棋子下方的连络线。** 可选的连络图层会画出棋手们真正会命名的四种连络形态：立、尖、飞、跳。因为线段渲染在棋子之下，它们永远不会盖住棋子，也不会落在提子的上方。
- **叠加而非重绘的领地。** KataGo 返回的领地图作为一个独立图层绘制，因此棋子与颜色始终清晰可见。新图会从前一张图淡入切换，而不是生硬地替换；整个图层也可以关掉。
- **以树为基础的复盘模型。** 从最新一手往前 Undo，就进入了复盘：AI 在叶节点等待；若从某个中间节点落子，棋局就在那里分叉成一个新变化。Redo 沿首选变化前进，引擎从不进入非叶节点的局面行棋。
- **重启之后仍在的对局。** 保存会把对局连同逐手评估写入最近列表，因此重新打开时会恢复局面、形势曲线与质量统计。带着未保存的着手退出对局时，应用会先询问是否保留；需要把记录交给别的软件时，也提供标准 SGF（FF[4]）导出。

以上这些特性可以同时出现在一张 13×13 棋盘上：连络线躺在棋子之下，近几手带着表情，最新一手套着圆环。

<p align="center">
  <img src="screenshots/p2.png" alt="一盘 13×13 对局：棋子下方的连络线、着手质量表情，以及最后一手的圆环"/>
</p>

---

## 主屏

打开应用，首先看到的就是主屏：两张卡片浮在柔和的深色背景上。第一张卡片是最近对局列表。每条目显示对局的标题与一行摘要（棋盘尺寸、手数与对弈双方），整局存档就藏在这一行的背后。因此，打开某一条记录，就会恢复到保存时的局面、棋谱树与形势曲线。候选着手与领地图这类实时数据并不存在存档里，所以会在你回到棋盘后向引擎重新请求。

<p align="center">
  <img src="screenshots/home.jpg" alt="主屏——最近对局、从 git 标签生成的更新日志，以及跟随最新标签的版本号"/>
</p>

第二张卡片是 **What's new**：一份由项目自身 git 历史生成的更新日志。打过标签的提交会带彩色徽章，没有一行是手写的——列表在构建时由 `git log` 生成，角落里的版本号取自最近的 git 标签（当前仓库为 `v1.4.1`；仓库没有任何标签时显示 `v0.1-alpha`）。这个机制在“代码结构”一节还会再提到，因为它实现在 Gradle 构建里，而不是应用代码里。

---

## 领地（Ownership）

领地叠加层使用的是 KataGo 分析时返回的领地图。你可以从对局中的抽屉打开它；由于每落一子地图都会变化，每一张新图都会从前一张图短暂淡入，而不是生硬地跳变。底层数据始终相同，不同的只是画法——共有三种样式可选：

| Blocks 色块 | Fog 雾气 | Constellation 星图 |
|:---:|:---:|:---:|
| 每点一枚圆角色块 | 在棋盘上化开的连续雾气 | 带着隐约面孔与连线的星点 |
| <img src="screenshots/ownerships_1.png" alt="Blocks 领地样式——被黑白双方势力染色的圆角色块"/> | <img src="screenshots/ownerships_2.png" alt="Fog 领地样式——覆盖在棋盘上的连续粉蓝色雾气"/> | <img src="screenshots/ownerships_3.png" alt="Constellation 领地样式——带隐约面孔与连线的星野"/> |

三种样式的空闲动画都刻意保持克制：色块微微跳动、雾气缓缓飘移、星座轻轻呼吸并闪烁。图层始终保持半透明，让下方的棋子清晰可读。这里有一个重要的区分：**死子（Dead stones）**开关不是某种样式，而是一项独立的功能。开启后，KataGo 判定为死的棋子会就地压暗，并罩上一圈淡黄色光晕，这样你无需移走任何棋子就能看清终局形势。该标记需要当前局面有实时的领地评估，因为死子正是从领地图上判读出来的。

---

## 对局

新建对局先要回答与真实棋桌相同的问题：棋盘多大、贴目多少、每种颜色由谁执掌。支持的棋盘为 9×9、13×13 与 19×19；贴目可选 6.5、7.5 或 0，默认 7.5；规则为中国规则，并采用 positional superko；坐标使用 GTP 坐标，也就是跳过字母 I。新建面板还会记住你上一次的选择，因此开一盘相似的对局往往只需一次点击。

<p align="center">
  <img src="screenshots/p5.png" alt="新建对局面板——棋盘尺寸、贴目、人机对战、段位滑条"/>
</p>

对局开始后，棋盘占据窗口中央，其余一切都围绕它排布。在窄屏/竖屏布局中，顶部细栏放置引擎状态、胜率条与 pass / undo / redo 操作；左侧抽屉（点击粉色标签或滑动打开）则容纳其余内容：轮到谁、提了多少子、候选点、棋谱树、形势图与质量表，以及表情、连络线、坐标、领地与死子等开关。在宽窗口与横屏手机上，棋谱树、形势图与质量表会改为常驻在棋盘旁的侧栏中，这样对局过程中它们始终可见。

<p align="center">
  <img src="screenshots/p4.png" alt="对局抽屉——状态、棋谱树、候选点与图层开关"/>
</p>

### 座位

每种颜色的对手是分别选择的，这正是即使新建面板只提供“人人对弈”与“人机对弈”，AI 对 AI 的组合依然可行的原因。四种座位如下：

- **人类（Human）**——这个颜色由同一台设备上的人来下。
- **段位 AI（Rank AI）**——引擎被请求一次单访问（one-visit）的策略，随后该座位按 KaTrain 针对所选等级校准的公式，从这份策略中抽取一手。这样得到的对手在局部战斗中反应很快，却会犯下该等级棋手会犯的弱手。
- **拟人 AI（Human-like）**——使用同样的单访问查询，但额外请求引擎的人类网络（通过 `humanSLProfile`，例如 `preaz_5k`）。随后从这份人类策略中采样一手，因此它下得更像一个选定等级的真人，而不是一台被削弱的引擎。
- **KataGo 9D+**——在配置的 play visits 预算内，下出引擎认为最强的一手。

段位 AI 与拟人 AI 都接受 15k 到 3d（15 级到 3 段）之间的等级，默认 5 级。对局中你随时可以从侧栏更换某个座位；由于打开抽屉会暂停 AI，你可以从容思考如何调整，而不必担心棋盘在眼皮底下继续变化。

### 复盘与分析

复盘是应用的另一项主要用途，而且直接构建在棋谱树之上。从最新一手往前 Undo，你就进入了复盘状态：当前节点带有子节点——这正是这里“复盘”的含义。处于这种中间节点时，下一手由人类负责，落子会从这里分叉出一个新变化。Redo 沿首选变化前进；引擎只会在树的叶节点、且轮到自己颜色时行棋。这样就保证了：你正在探索的某条变化绝不会被 AI 的着手打断。

形势图与质量表里的评估是逐手收集的，这也是为什么这些功能只有在连接引擎之后才会出现。当前局面会显示带变化（PV）、损失目数与访问数的候选着手；**Analyze game** 会在后台沿整条首选变化逐节点请求完整分析。

### 保存与记录

保存通过对局抽屉完成：**Save** 更新当前记录，**Save as** 以新名字另存一份。两者都会把对局连同逐手评估写入最近列表，这正是重启之后形势曲线与质量统计依然存在的原因。由于应用会跟踪保存之后局面是否发生变化，带着未保存的着手退出对局画面时，会弹出对话框，让你选择保存、放弃或取消。此外也可以打开与导出 SGF 文件，格式为 FF[4]——这也是绝大多数围棋工具使用的交换格式。

---

## 外观与阈值

外观主题与分析阈值都在设置页上配置。共有四套棋子/外观组合，每套定义两种座位的颜色，因此也决定了胜率条与曲线的颜色：

- **Sky & Sakura**——晴空蓝与樱花（默认）
- **Ink & Paper**——深靛蓝与樱花纸
- **Midnight & Snow**——夜色炭黑与暖雪
- **Lilac & Peach**——淡丁香紫与熟桃

胜率条用两种棋子的颜色绘制，两色交界处即当前的胜率分割；最后一手的圆环取刚落下那颗棋子的颜色。“功能一览”一节提到的质量阈值也在这同一页编辑，紧挨着领地样式的选择器，因此分级与外观放在同一处。

<p align="center">
  <img src="screenshots/p1.png" alt="设置——外观配色与质量阈值"/>
</p>

---

## 引擎与网关

本节说明引擎如何连接，以及仓库附带的小型网关如何工作。引擎设置页包含连接配置（WebSocket 地址、可选 token，以及 play 与 review 两套独立的访问量预算），以及“测试连接”和“运行基准测试”两个操作。基准测试会测一次单访问策略查询、80 / 400 / 2000 访问量的搜索档位与一次人类网络探测，并给出该服务器是否足以流畅行棋的结论，大约耗时 20–90 秒。

所有引擎流量都是 WebSocket 上的 KataGo Analysis JSON，因此没有第二套协议需要保持同步。查询会请求 `rootInfo`、`moveInfos`、`ownership`、`policy`，需要时还有 `humanPolicy`。胜率一律以黑方视角存储与报告。拟人 AI 与段位 AI 使用单访问并请求策略的查询——它们只需要从中抽一手——拟人 AI 还会在同一查询上附带 `humanSLProfile`（例如 `preaz_5k`）。实时分析与 KataGo 9D+ 的着手消耗 play visits 预算，排队的 **Analyze game** 复盘则消耗 review visits 预算。

<p align="center">
  <img src="screenshots/engine.jpg" alt="引擎设置——WebSocket 地址、play visits、review visits、测试连接"/>
</p>

引擎离线时，依赖引擎的界面部分（形势图、质量标记、领地叠加层、AI 着手）要么什么都不做，要么保持空白；人人对弈则一如既往地正常工作。

### 附带的网关

由于引擎客户端期望诸如 `ws://127.0.0.1:2080` 这样的地址，仓库内置了一个恰好提供该地址的小型 Python 网关。在项目根目录执行：

```bash
pip install -r engine/requirements.txt
python engine/server.py --katago /path/to/katago
```

`requirements.txt` 里唯一的依赖是 `websockets` 库。首次运行时，网关会在 `model/` 目录缺失时创建它，并从 KataGo 的 GitHub releases 下载两个网络：主网络 `b10c384h6nbttflrs`，以及拟人 AI 座位需要的人类监督网络 `b18c384nbt-humanv0`。如果文件已经自行放好，可以用 `--skip-download` 跳过下载。引擎调优参数位于 `engine/analysis.cfg`，KataGo 的缓存与日志放在 `engine/home/`。KataGo 二进制本身不会被下载，因为它跟随 GPU 分发；网关按以下顺序查找它：`--katago` 传入的路径、`engine/bin/katago`、最后是 `PATH` 上的 `katago` 可执行文件。

---

## 构建与运行

桌面应用可以直接从仓库启动：

```bash
./gradlew :desktopApp:run
# 开发时热重载
./gradlew :desktopApp:hotRun --auto
```

想要有引擎可用，就在启动应用前先运行上一节的网关（首次运行会把网络文件放进 `model/`）：

```bash
pip install -r engine/requirements.txt
python engine/server.py --katago /path/to/katago
```

iOS 上请用 Xcode 打开 [`iosApp/`](./iosApp)。需要说明一点：SGF 的打开/导出对话框目前只在桌面与 Android 上实现，因此这两项操作在 iOS 壳里目前不会有任何效果。

### 在终端里构建 Android

Android 应用位于 `:androidApp` 模块（`applicationId` 为 `com.acite.katahana`）。Gradle 需要知道 Android SDK 的位置，这写在 `local.properties` 里：

```
sdk.dir=/path/to/Android/Sdk
```

`adb` 位于 `$sdk.dir/platform-tools/adb`（或已经在你 `PATH` 上）。

**真机安装。** 开启 USB 调试并插上手机，接受 RSA 提示：

```bash
adb devices
```

你希望看到 `device`，而不是 `unauthorized` 或 `offline`。同时连着多台设备？先用 `adb devices -l` 列出序列号，再给每条 `adb` 命令加上 `-s <serial>` 前缀——或者导出 `ANDROID_SERIAL=<serial>`，让 Gradle 也使用同一台。

**无线**（Android 11+ 开发者选项 → 无线调试）：

```bash
adb pair <ip>:<pairing-port>
adb connect <ip>:<debug-port>
adb devices
```

**Release 安装到手机。** Gradle 构建 release APK 并签名，剩下的交给 adb：

```bash
./gradlew :androidApp:installRelease
adb shell am start -n com.acite.katahana/.MainActivity
```

同样的安装，但把 APK 文件留在手边：

```bash
./gradlew :androidApp:assembleRelease
adb install -r androidApp/build/outputs/apk/release/androidApp-release.apk
```

`-r` 会替换已存在的 KataHana。如果 adb 回答 `INSTALL_FAILED_UPDATE_INCOMPATIBLE`，说明手机上的版本是用不同的密钥签名的——先卸载再重试：

```bash
adb uninstall com.acite.katahana
adb install androidApp/build/outputs/apk/release/androidApp-release.apk
```

**只要 APK**（不需要手机）：

```bash
./gradlew :androidApp:assembleRelease
# androidApp/build/outputs/apk/release/androidApp-release.apk
```

想要 Play 商店风格的 App Bundle？

```bash
./gradlew :androidApp:bundleRelease
# androidApp/build/outputs/bundle/release/androidApp-release.aab
```

关于签名的一点说明：release 构建目前使用 **debug** 密钥库签名（`androidApp/build.gradle.kts` 中的 `signingConfig = signingConfigs.getByName("debug")`），并保持关闭混淆。这正是 `installRelease` 无需你自备密钥库也能工作的原因——它仍然是真正的 release 变体（`debuggable=false`）。今后若要上架 Play，需要另配自己的签名配置。

**Debug**（IDE 的运行配置通常安装的就是它）：

```bash
./gradlew :androidApp:installDebug
# 或
./gradlew :androidApp:assembleDebug
# androidApp/build/outputs/apk/debug/androidApp-debug.apk
```

### 测试

共享逻辑由单元测试覆盖，分别在 JVM 与 Android host 上运行：

```bash
./gradlew :shared:jvmTest
./gradlew :shared:testAndroidHostTest
```

---

## 代码结构

项目是一个 Kotlin Multiplatform 工程，包含三个 Gradle 模块：`shared` 承载游戏逻辑与 Compose 界面，`androidApp` 与 `desktopApp` 只是轻量启动器；`iosApp/` 中的 iOS 应用包裹同一套共享界面。在 `shared` 内部，所有与操作系统无关的代码都放在 `commonMain`；`jvmMain`、`androidMain` 与 `iosMain` 只提供各平台必须提供的部分，例如文件访问、设置路径、HTTP/WebSocket 客户端引擎与屏幕生命周期钩子。

引擎客户端是 `engine/` 包里的一条轻量 WebSocket 连接。主屏上的版本号与更新日志并非写死：`shared/build.gradle.kts` 中的一个 Gradle 任务会在构建时执行若干 `git` 命令，并生成一个由界面读取的小型 Kotlin 文件。导航使用 Voyager，依赖注入使用 Metro，偏好设置存放在 DataStore。

```
shared/src/commonMain/kotlin/com/acite/katahana/
  domain/     棋谱树、规则、连络、评估序列
  engine/     WebSocket 客户端、Analysis JSON、领地数据
  ai/         HumanBot、RankBot、FullStrengthBot、质量分带
  settings/   偏好设置与领地样式
  recents/    Save / Save as 书架、持久化的评估
  changelog/  把 git 标签写进主屏日志
  sgf/        SGF 读写
  ui/         棋盘、对局、主屏、主题
```
