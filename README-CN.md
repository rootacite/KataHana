<p align="center">
  <img src="katanhana_clip.png" alt="KataHana 图标" width="168"/>
</p>

<h1 align="center">KataHana</h1>

<p align="center">
  <strong>本地围棋 · 粉色夜色</strong><br/>
  一个用 Compose Multiplatform 编写的围棋客户端，用来下棋与复盘——KataGo 陪在棋盘旁边，而不是住在棋盘里面。
</p>

<p align="center">
  桌面 JVM · Android · 中国规则 · 人人对弈 · 人机对弈
</p>

---

KataHana 是一块留在你设备上的棋盘。规则、棋谱树、棋子、对局记录——全部都在本地，没有需要加入的服务器，也没有需要维护的账号。KataGo 是受邀的客人，而不是必备条件：客户端通过一条朴素的 WebSocket 与引擎的 Analysis JSON 对话，因此你可以搭配任何版本的 KataGo——或者干脆一个都不装。引擎不在线的时候，人人对弈玩起来一模一样。

当你把 AI 请进对局时，对面坐的是谁由你决定——而且你可以在对局中途改主意。点击侧边栏中的某一方颜色，把它设为 **人类**、**段位 AI**、**拟人 AI** 或 **KataGo 9D+**。双方彼此独立，所以 AI 对 AI 也完全允许。**拟人 AI**（新建 AI 座位时的默认选择）以 15k 到 3d 的段位采样 KataGo 的人类监督网络，因此即使在局部对杀中，它也会像这个段位的真人一样失手。**段位 AI** 是 KaTrain 更早的「走子抽奖」策略——局部的战斗中犀利，大局方向却比较松散。**KataGo 9D+** 则永远下出引擎眼中最强的一手。

至于观感，就像这盘棋终于被允许熬夜了一样。没有木纹，也没有博物馆式的米白。整个应用都为夜晚而装扮——深紫与樱花粉——默认的棋子是晴空蓝对樱花瓣，在暗色的背景下依然醒目。

<p align="center">
  <img src="screenshots/gaming.jpg" alt="一盘进行中的 19×19 对局：棋谱树、带实时数值标签的目数差曲线、着手质量统计、领地色块，以及藏在棋子下方的连络线"/>
</p>

---

## 它为什么与众不同

**棋盘永远是主角。** 棋子占据屏幕中央，其余一切都退到两侧等待。胜率缩成一条细窄的进度条，下子、保存、候选点与各种图层开关收进粉色的抽屉标签页，需要时才展开。宽屏窗口——以及横屏手机——会在棋盘旁边留出一栏安静的副栏：棋谱树、形势曲线与着手质量统计。竖屏时，同一套内容收进菜单里。

**一条读起来像棋局的曲线。** 目数差与胜率共用同一张图，而且它符合直觉。胜率轴从上到下贯穿「黑方 100% → 中央五五开 → 白方 100%」，因此黑方 70% 就落在图表 70% 的高度上，而不是伪装成某个高深莫测的「+20」。线条的颜色偏向领先的一方，最新的一点带着一个小标签，点击曲线上任意位置，棋谱树会直接跳到那一手。

**表情，而不是红绿灯。** 最近的每手棋都挂着小小的表情，而不是无名无姓的标记——从棋形良好到恶手，每个图形按 KaTrain 风格的损失分带划分，边缘描白，即使 19×19 满盘也一眼可辨。棋盘下方，质量卡片会为双方落下的每一颗棋子计数。

**懂围棋的连络线。** 可选的连络图层把 立、尖、飞、跳 画在棋子**下方**，因此永远不会盖住棋子或遮住提子。同色连络的圆环保持完整，而 飞、跳 会顺着棋形从棋团向外伸展，而不是向内缩成一团。

**让棋子透气的热力图。** 领地显示是一层覆盖物，而不是重新上色。它会在相邻两幅图之间平滑过渡，也可以完全关掉，并且有三种共享同一份数据的观感。死子甚至可以留在棋盘上，罩着一圈淡黄色的光晕，而不是被一扫而空。

**复盘是一棵树，不是一卷磁带。** 从最后一手逐级 Undo 就进入了复盘——AI 停在叶节点等你。落下一子，历史就在此处分叉成一个新的变化；Redo 沿首选变化前进。引擎永远只在树的叶节点、轮到自己颜色的时候落子，因此每一条分支都留给你探索。

**随时能回来的棋局。** Save 和 Save as 会把对局连同评估一起放上 Recent 书架，重启之后曲线与质量统计依然在。带着未保存的棋步想离开？KataHana 会先问一句。需要把棋谱交到别人手里时，还有标准 SGF（FF[4]）导出。

所有这一切同时出现在一张 13×13 棋盘上：连络线安静地躺在棋子之下，近手表情挂在新落下的棋子上，最后一手的圆环套在最新的石头上。

<p align="center">
  <img src="screenshots/p2.png" alt="一盘 13×13 对局：棋子下方的连络线、着手质量表情，以及最后一手圆环"/>
</p>

---

## 首页

打开应用，你会落在一面夜光书架上：瓷器质感的卡片浮在柔和的夜色光晕上。书架就是 Recent 列表——每盘棋都保存着自己的名字、棋盘与评估，重新捡起时，曲线也停在离开时的位置。

<p align="center">
  <img src="screenshots/home.jpg" alt="首页——最近对局、从 git 标签生成的更新日志，以及跟随最新标签的版本号"/>
</p>

另一张卡片是 **What's new**，滚动展示项目自己的 git 历史，已打标签的提交带着 tag 徽章。这里没有一行字是手写的：更新日志在构建时生成，角落里的版本号跟随最近的 git 标签（今天是 `v1.4.1`；如果仓库里一个标签都没有，则显示 `v0.1-alpha`）。

---

## 领地 (Ownership)

从侧边菜单打开热力图，KataGo 的领地图会沉降到棋子之下——每张新图都从前一张平滑过渡，而不是生硬地跳变。一份数据，三种穿法：

| Blocks 色块 | Fog 雾气 | Constellation 星图 |
|:---:|:---:|:---:|
| 圆角色块，一点一块 | 融成一片的雾气带 | 星光、连线与隐约的脸 |
| <img src="screenshots/ownerships_1.png" alt="Blocks 领地样式——被黑白双方势力染色的圆角色块"/> | <img src="screenshots/ownerships_2.png" alt="Fog 领地样式——覆盖在棋盘上的连续粉蓝色雾气"/> | <img src="screenshots/ownerships_3.png" alt="Constellation 领地样式——带隐约面孔与连线的星野"/> |

静止时的动画刻意保持安静：色块轻轻跳动，雾气缓缓飘移，星星静静呼吸。无论选哪种风格，图层始终半透明，下面的棋子依旧清晰可辨。

---

## 对局

开新局时问的还是那几个你自己本来就会问的问题：棋盘多大、贴目多少、对面是谁、你想执哪一色。

<p align="center">
  <img src="screenshots/p5.png" alt="新建对局面板——棋盘尺寸、贴目、人机对战、段位滑条"/>
</p>

| | |
|---|---|
| **棋盘** | 9×9、13×13、19×19 |
| **规则** | 中国规则，positional superko（全局同形禁着），GTP 坐标（跳过 I） |
| **人人对弈** | 同一台设备，无需网络 |
| **座位** | 每一色都可以是 **人类**、**段位 AI**、**拟人 AI** 或 **KataGo 9D+**；对局中可从侧边栏随时更换，拉开抽屉会暂停对局。段位 AI 与拟人 AI 的等级范围是 15k 到 3d。**拟人 AI** 以 `preaz_{rank}`（`maxVisits=1`）采样 KataGo 的 `humanPolicy`；**段位 AI** 是 KaTrain 的策略抽奖；**KataGo 9D+** 按你设定的 play visits 下出引擎最强手。 |
| **复盘** | Undo、Redo 或点按棋谱树。从最后一手退回去即进入复盘；落在非叶节点上时，下一手由你接管。 |
| **评估** | 实时的目数差/胜率曲线与 Good–Blunder 着手质量表，随保存的对局一并保留。 |
| **分析** | 实时候选点，含 PV、目数损失与访问量。**Analyze game** 会把首选变化排进队列。 |
| **记录** | 首页的 Recent 列表。抽屉里的 **Save** / **Save as**，以及在带着未保存棋步离开时的确认提示。可打开与导出 SGF（FF[4]）。 |

棋子落下之后，棋盘占据屏幕中央，侧栏在四周围拢成一张完整的棋桌：轮到谁、提了多少子，pass / undo / redo，候选点，宽屏布局下的棋谱树 + 曲线 + 统计，以及表情、连络、坐标、领地和死子等开关。

<p align="center">
  <img src="screenshots/p4.png" alt="对局抽屉——状态、棋谱树、候选点与图层开关"/>
</p>

---

## 外观

棋盘为夜晚而装扮，胜率条也一路相随。最后一手的圆环与它的涟漪会采用刚落下那颗棋子的颜色。

- **Sky & Sakura**——晴空蓝与樱花瓣（默认）
- **Ink & Paper**——深靛蓝与樱花纸
- **Midnight & Snow**——夜色炭黑与暖雪
- **Lilac & Peach**——淡丁香紫与熟桃

质量阈值在同一张设置页上，KaTrain 的默认值（`12 / 6 / 3 / 1.5 / 0.5`）随时等你调校；领地样式选择器就放在旁边。

<p align="center">
  <img src="screenshots/p1.png" alt="设置——外观配色与质量阈值"/>
</p>

---

## 通过 WebSocket 连接 KataGo

引擎客户端就是一条朴素的 WebSocket，所以「安装 AI」等于把客户端指向一个地址：

```
ws://127.0.0.1:2080
```

本仓库附带一个会说这个地址的网关。在项目根目录执行：

```bash
pip install -r engine/requirements.txt
python engine/server.py --katago /path/to/katago
```

首次运行会按需创建 `model/`，并从 KataGo 的 GitHub releases 下载主网络（`b10c384h6nbttflrs`）与人类监督网络（`b18c384nbt-humanv0`）。配置文件位于 `engine/analysis.cfg`；KataGo 的缓存放在 `engine/home/`。引擎二进制与 GPU 相关，因此不会被自动获取——用 `--katago` 传入路径、把二进制放到 `engine/bin/katago`，或让 `katago` 出现在 `PATH` 上即可。如果网络文件已自行放好，`--skip-download` 可以跳过下载。

<p align="center">
  <img src="screenshots/engine.jpg" alt="引擎设置——WebSocket 地址、play visits、review visits、测试连接"/>
</p>

每一帧都是一个符合 KataGo 原生格式的 JSON 对象——`rootInfo`、`moveInfos`、`ownership`、`policy`、`humanPolicy`——因此没有第二套协议需要保持同步。胜率一律以黑方视角存储。人类与段位 AI 的查询只请求引擎做单次访问的策略（人类查询还会附带设置 `humanSLProfile`）；实时分析、满强度的 genmove 与排队的复盘，则使用你在配置里设定的 play / review visits。引擎离线？人人对弈照常进行——分析层只是安静下来而已。

---

## 运行

桌面端一条命令即可启动：

```bash
./gradlew :desktopApp:run
# 热重载
./gradlew :desktopApp:hotRun --auto
```

本地 KataGo WebSocket 网关（首次运行会把网络文件放进 `model/`）：

```bash
pip install -r engine/requirements.txt
python engine/server.py --katago /path/to/katago
```

在 iOS 上，用 Xcode 打开 [`iosApp/`](./iosApp) 即可。

### 在终端里构建 Android

Android 应用位于 `:androidApp` 模块（`applicationId` 为 `com.acite.katahana`）。Gradle 需要知道 SDK 的位置，这写在 `local.properties` 里：

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

**Release 安装到手机。** Gradle 构建 release APK、签名，剩下的交给 adb：

```bash
./gradlew :androidApp:installRelease
adb shell am start -n com.acite.katahana/.MainActivity
```

同样安装，但把 APK 文件留在手边：

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

```bash
./gradlew :shared:jvmTest
./gradlew :shared:testAndroidHostTest
```

---

## 内部结构

基于 Compose Multiplatform：游戏逻辑在 `shared` 中，`androidApp` 与 `desktopApp` 只是轻量的启动器。导航用 Voyager，依赖注入用 Metro，设置存放在 DataStore，Recent 书架是一个小型 JSON 索引。棋盘、规则与 SGF 都是纯 Kotlin——没有任何平台代码挡路——引擎客户端则刻意保持轻薄：一条说着 KataGo 原生 JSON 的 WebSocket。在首页，版本号与更新日志在构建时从 git 标签中敲印出来。

```
shared/src/commonMain/kotlin/com/acite/katahana/
  domain/     棋谱树、规则、连络、评估序列
  engine/     WebSocket 客户端、Analysis JSON、领地数据
  ai/         HumanBot、RankBot、FullStrengthBot、质量分带
  settings/   偏好设置与领地样式
  recents/    Save / Save as 书架、持久化的评估
  changelog/  把 git 标签写进首页日志
  sgf/        SGF 读写
  ui/         棋盘、对局、首页、主题
```

---
