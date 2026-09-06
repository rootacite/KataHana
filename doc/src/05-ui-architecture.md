# 第 5 章 UI 架构与呈现

## 5.1 导航与屏幕组织

应用根在 `App.kt`，是一个纯 Compose 函数 `App(metroVmf)`，自外向内依次是：
Metro 的 `LocalMetroViewModelFactory` → 根 `ViewModelStore`（没有父 store 时自建，
onDispose 清空）→ `ProvideHanaScreenLifecycle` → **根级** `SettingsViewModel`（它
收集 `appearanceId` 驱动全局主题）→ `KataHanaTheme` → 一个 Voyager
`Navigator(HomeScreen())`。

导航栈只有四个“目的地”：

| 屏幕 | 入栈方式 |
| --- | --- |
| `HomeScreen` | 根（Navigator 初始页） |
| `SettingsScreen` | Home 点设置 |
| `EngineSettingsScreen` | Home/Settings 点引擎 |
| `SessionScreen(config, loadedTree?, recentId?, recentTitle?)` | 开始新对局 / 读 SGF / 打开最近对局 |

弹窗（New Game 弹层、Seat 弹窗、离开确认、起名）**不是导航目的地**，是屏幕内的
overlay（见 5.5）。返回键：App 层 `HanaBackHandler(enabled = navigator.canPop)` 负责
出栈；`SessionScreen` 自己注册返回处理——有未保存改动时弹“离开/保存”确认。

生命周期是这套导航里最特殊的一块：iOS/JVM 上 `ProvideHanaScreenLifecycle` 是空操作，
Android 上它用一个同时充当 `ScreenLifecycleOwner / LifecycleOwner /
ViewModelStoreOwner / SavedStateRegistryOwner` 的自定义 owner 把 Voyager 屏幕接进
Activity 的前后台生命周期与保存状态。这是因为 Voyager 1.0.1 在返回键配合
FadeTransition 时会把已 DESTROYED 的屏幕再试图切到 STARTED 而崩溃，自定义 owner
在进入终态后对生命周期事件做安全忽略。**改导航/生命周期时不要把屏幕的树直接放进
导航参数**：`GameTree` 不可序列化，`SessionScreen` 用 `instanceId` + 静态
`SessionTrees` 暂存树（Android 的 onSaveInstanceState 只保存屏幕外壳）。

## 5.2 对局界面的组装与自适应布局

`SessionScreen` 的 `SessionRoute` 收集 `vm.state` 与各开关，把数据“翻译”成棋盘参数
（按开关决定传不传候选点/质量点/势力图），然后交给无状态的子组件。所有面板组件都
**不持有状态**，棋盘、Game Tree、胜负图、统计、抽屉内容各是独立 composable。

布局决策收敛在一个纯函数里：

```kotlin
internal fun computeSessionLayout(
    maxWidth, maxHeight, mobile, chromePad,
    analysisMode = AnalysisLayoutMode.Auto,
): SessionLayout
```

规则（常量在 `SessionLayout.kt`）：

- **桌面**从不出现 64dp 的竖条导航（`showRail` 仅 `mobile && maxWidth > maxHeight`，
  即横屏手机）；
- 棋盘取可用区最大正方形（`min(w, h)`，下限 120dp）；有 rail 且放不下时压缩；
- **横屏/宽屏**：棋盘右侧富余宽度 ≥ 168dp 就放侧边三栏
  （`showSideTree`，宽度 clamp 在 168–280dp）；
- **纵屏**（宽 ≤ 高）：棋盘上方富余高度 ≥ 200dp 时放顶部一行
  （`showTopTree`）——从高到低依次是 Winrate 条、分析区、棋盘在底部；
- 都不满足就只有棋盘。

分析区默认是三张独立瓷器卡：`GameTreeCard` / `EvalGraphCard` / `QualityStatsCard`
（纵屏横排、横屏竖叠）。窗口太窄或太矮时，三栏会换行或把中间的 Score 图挤没，
于是 `tabbedAnalysis` 把三张卡合成**一张**瓷器，顶上三个胶囊 Tab
（Game tree / Score / Quality）切换。Auto 启发式看**当前窗口**而不是机型：

- 纵屏：`maxWidth < 560.dp`；
- 横屏：侧栏高度（≈ `boardSide`）`< 420.dp`，或侧栏宽 `< 240.dp`。

设置里 `AnalysisLayoutMode`（Auto / Compact=强制 Tab / Expanded=强制三栏）可覆盖
误判。抽屉 `SidePanel` 仍是三张独立卡，不要改它的信息架构。

纵屏顶行与横屏侧栏由 `EvalGraph.kt` / `GameTreeView.kt` 里的
`SessionTreeRow` / `SessionTreeColumn` 组织。顶栏
（纵屏 `SessionTopBar`）与侧 rail（横屏 `SessionRail`）放引擎状态点、菜单、
`WinrateTrack`（胜负条）、`PlayIconCluster`（附加按钮 Resume / EndPreview / 确认
在前，Pass/Undo/Redo 固定贴在簇的尾沿，避免 Undo 后出现的按钮把常驻三键挤开）
与复盘标记 `ReviewChip`。**横屏的 rail 与抽屉**：rail 是窄条快捷区，
抽屉 `HanaDrawer`（ModalNavigationDrawer + Haze 模糊）承担全部次级功能，内容在
`SidePanel.kt`：状态卡（座位、行棋方、手数/贴目/提子，点座位弹 Seat 弹窗）、AI
状态、走子按钮簇、变化图行、三张分析卡、六个显示开关、整局复盘按钮、候选点卡、
保存/另存/导出 SGF、返回与设置。**改动时不要重排抽屉的信息架构**（约定）。

## 5.3 棋盘绘制与指针输入

棋盘是 `ui/board/BoardCanvas.kt` 里的**一个巨型 Canvas**，按固定顺序分层绘制
（顺序即 z 序，理解“谁盖谁”看这里）：

1. 圆角棋盘底色与网格；
2. 势力图层（`Overlays.drawOwnershipLayer`：三种风格
   `Blocks`（逐点方块）/ `Fog`（8×8 双线性场雾）/ `Constellation`（星丛连线），
   各带独立无限动画与透明度）；
3. 星位 → GTP 坐标带（字号 = `gap * 0.48` 像素再 `toSp()`，粗体。从棋盘边缘起
   依次是 `edgePad`（默认 5dp）、标签、子半径 + `gridPad`（默认 3dp）才到网格；
   两项可在设置里调。字形宽超过格距 0.9 时只画偶数路与两端）；
4. **棋子下方的连接形状**（`Connections.kt`：长连/小尖/飞/跳，虚线、二次曲线带
   “外靠”偏移，随上一步的落子有生长动画）——名字就说明它是垫在棋子下面的；
5. 真实棋子（`Stones.drawStone`：径向渐变 + 高光 + 描边；落子有 squash 弹簧、
   提子有“升天”飞走动画、上一步有呼吸光晕；被标死/预测被提的棋子降低透明度）；
6. 复盘质量脸与死子脸；
7. 候选点圆圈（半径按“损失目数”分级、附损失数字）；
8. 幽灵子（`preview`，半透明）；
9. 局势预测：加载圈 + 编号虚子（透明棋子 + 手数/损失标签）。

棋子之上的交互全部经**命中测试** `BoardHit.kt` 把指针位置换算成交点：
`BoardLayout` 先算棋盘正方形、边距与格距：关坐标时 inset = 子半径 +
`edgePad`；开坐标时 inset = `edgePad` + 字高 + 子半径 + `gridPad`，标签中心在
`edgePad + 字高/2`，这样调“到边缘”不会把数字推离网格。`nearestIntersection` 找最近的格点，
距离超过阈值就视为点空（点击 `.62` 格、滑动 `.85` 格）。两条指针管线分开实现：

- **鼠标**：悬停出幽灵子（`onHover`），按下抬起 = 落子
  （`onActivate(point, isTouch=false)`），复盘时**右键抬起 = 长按预测**（`onForecast`）；
- **触摸**：一次手势里区分——快抬 = 落子；按住超过长按阈值 = 预测；拖动 = 瞄准
  （`onAim`，棋盘上划出“准备落这里”）；左缘横向滑动预留开抽屉。

落子两段式：`confirmMove` 开关或触摸时，第一次点选“选中”（画幽灵子），再点同点
确认（`confirmSelected` / `commit`）。这一切最终都汇入
`vm.onActivate → vm.commit → session.play → publish()`（第 4 章），**Canvas 从不直接
改状态**。

## 5.4 主题系统

主题由“棋子外观”驱动：`ui/theme/Appearance.kt` 定义外观 `Appearance(id, label,
blurb, tagline, first, second, palette)`，其中 `first/second` 是一对
`StoneSwatch(fill, rim, hi, light)`（黑/白棋子的配色）。每个外观带一个
`HanaPalette`。现役四个外观（`Appearance.byId` 查表，未知 id 回退默认）：

| id | 名称 |
| --- | --- |
| `sky_sakura` | 默认：天与樱 |
| `ink_paper` | 墨与纸 |
| `midnight_snow` | 夜雪 |
| `lilac_peach` | 丁香与蜜桃 |

`HanaPalette`（`Color.kt`）是整套 UI 色板：`bgApp / bgPanel / bgCard / stroke /
text / textDim / accentPink / accentBlue / accentLilac / boardBg / grid / star`，
外加六个**跨外观共享**的语义色 `qualityPurple/Red/Orange/Yellow/Mint/Green`。
`KataHanaTheme(appearance)` 用 palette 构造一个 M3 `darkColorScheme`，并通过四个
`staticCompositionLocalOf` 提供 `LocalHanaPalette`（→ `hanaColors`）、
`LocalHanaTokens`（圆角/动效 token）、`LocalAppearance`、`LocalHanaFontFamily`
（Nunito Regular/Medium/SemiBold/Bold，OFL，文件在
`composeResources/font/`）。`hanaTypography(fontFamily)` 把字族写进 M3
`Typography`；Compose `Text` 会继承。Canvas 里手写的 `TextStyle`（棋盘坐标、
胜负图轴、候选点数字）必须显式带上 `hanaFontFamily`，否则仍走平台默认。
棋盘坐标的字号按 **Canvas 像素格距** 再 `toSp()`，不要把像素当成 `sp`
（高密度窄屏上会比格距还大、字母叠在一起）。composable 里读
`hanaColors` 即可拿到“当前外观”的颜色；非 composable 语境用静态
`HanaColors`（SkySakura 默认值）。

**颜色纪律**（务必遵守）：

1. 原始十六进制只允许出现在 `Color.kt` / `Appearance.kt`；
2. composable 内读取 `hanaColors` 后，把用到的颜色**作为参数传进 DrawScope 扩展**
   ——`Canvas`/`DrawScope` 无法读 CompositionLocal；
3. 画棋子的代码需要棋子外观时读 `hanaAppearance.swatch(color)`；
4. 绘制扩展函数的默认参数可以用 `HanaColors.*`，但前提是该色在所有外观间一致
   （如质量色）——`AppearancePaletteTest` 会钉死这一点，新增外观若想改质量色会
   测试失败。

组件层有一套“瓷器（porcelain）”配方：半透明 `bgPanel` 底 + 22–24dp 模糊 +
轻噪点 + 白色 10% 细描边 + `tokens.card` 圆角，用在 Home/Settings/Engine 的背景卡
与各类弹层上。可交互按钮（`CapsuleButton` 等，`Widgets.kt`）**默认保持实心**
（`bgCard` 底），不要把全局按钮默认改成半透明——实心底是刻意保留的可读性。

## 5.5 亚克力效果的两套 HazeState

模糊亚克力（Haze 2.0.0-beta02）在本项目里有一个容易踩坑的原理，值得单独讲。

**Haze 只能在同一个窗口/同一张画面里采样。** 早期实现用 Compose `Dialog` /
`ModalBottomSheet` 弹窗，它们在 Android/桌面常是独立窗口或独立合成层，
Haze 采不到背后的 Home 页面，效果就退化成普通半透明——这就是“弹窗背后文字和弹窗
文字混在一起”的根因。修复后的约定是：

- **弹窗是屏幕布局内的 overlay，不是独立窗口**：`HanaScrimModal`（
  `ui/components/HanaDialogs.kt`）是全屏黑色半透明 scrim（点 scrim 关闭，用
  `detectTapGestures`，避免 hover 高亮导致整屏闪动）+ 居中/贴底的
  `FrostedSurface` 卡；
- 每个有弹窗的屏幕准备**两套 HazeState**：一套给页面本体（棋盘/光晕装饰的
  `hazeSource`），一套 `overlayHaze` 把**整个页面内容**注册为 source；
  弹窗画在页面之上，是页面 source 的“邻居”而不是孩子；
- `FrostedSurface` 收到 hazeState 后用
  `HazeInput.Sources(state, selection = HazeSourceSelection.All)` 采样全部 source，
  并带 fallback 色（模糊不可用时也能看清文字）；不传 hazeState 时它退化为纯
  半透明底。

所以“想给某块加亚克力”的步骤是：确认它背后有注册成 source 的内容 → 用
`hazeSource` 把页面内容包起来 → 用带 hazeState 的 `FrostedSurface`。注意别把
haze 效果放进它想模糊的 source 内部（Haze 的 `Behind` 选择器会找不到可用源，
效果静默失效）。局内棋盘背景刻意保持实心 goban，**不要给棋盘加光晕/背景装饰**。

## 5.6 文案与版本信息

所有用户可见文案集中在 `ui/Copy.kt`（一个 `object Copy` 的 const 与少量格式化函数）。
**屏幕代码里不要出现裸的英文/中文串**；加新文案先加进 `Copy`。目前只有这一层
“i18n”（无资源文件体系），`Copy` 同时充当术语表——翻译 README 也以它为准。

版本与“更新内容”：`shared/build.gradle.kts` 的 `generateHanaInfo` 任务在构建时执行
git 命令，把最近 40 条提交（`hash|date|subject|tags`）与当前版本 tag 写进
`generated/AppInfo.kt`（`version / gitHash / changelog`）。Home 的“What's new”区把
这些条目按 `[Feat]/[Fix]/[doc]` 前缀渲染成彩色标签。因此**提交信息的格式会影响
App 内展示**（见 8.5 约定）。
