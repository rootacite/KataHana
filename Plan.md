# CMP Go Client — Grok Build 实施说明

给 Grok Build 用。按文件写，别发明第二套协议，别做成 KaTrain 皮肤。

产品：Compose Multiplatform，Desktop JVM + Android。
棋盘和棋规在本地。AI 只通过 WebSocket 说 KataGo Analysis JSON。
HvH 同设备轮流，不联网。HvAI 必须能选段位，不能默认满血 9d。

---

## 工程

```
composeApp/src/commonMain/kotlin/
  domain/     GameTree, Rules, Node
  engine/     WsClient, AnalysisDto, EngineProfile
  ai/         RankBot, FullStrengthBot, MoveQuality
  sgf/        SgfIo
  ui/theme/   Color.kt Tokens.kt Type.kt
  ui/board/   BoardCanvas.kt Stones.kt Overlays.kt
  ui/session/ SessionScreen.kt SidePanel.kt
  ui/home/    HomeScreen.kt NewGameSheet.kt
  ui/engine/  EngineSettingsScreen.kt
```

导航：Voyager。状态：每屏 ViewModel + 一个 `GameSession`。
字符串英文。设置用 DataStore。SGF 走平台文件 API。

---

## 协议（不要再包一层）

传输：`ws://` / `wss://`，一帧一条 JSON。可选 `?token=`。

发出：

```json
{
  "id": "session:node:live:nonce",
  "rules": "chinese",
  "komi": 7.5,
  "boardXSize": 19,
  "boardYSize": 19,
  "moves": [["B","Q16"],["W","D4"]],
  "analyzeTurns": [2],
  "maxVisits": 400,
  "includeOwnership": true,
  "includePolicy": true
}
```

`purpose` 写在 id 里：`live` / `review` / `genmove` / `rank`。
Rank 机器人只要 policy：`maxVisits=1` + `includePolicy=true`。
满血 / 提示用正常 visits。

回包用 KataGo 原字段：`rootInfo`、`moveInfos`、`ownership`、`policy`。未知字段丢掉。
内部统一存「黑胜率」「黑目差」。行棋方损失 = 落子前最佳 scoreLead − 该点 scoreLead，再按颜色取正。

EngineProfile：`name, url, token?, playVisits, reviewVisits`。
连不上时 HvH 仍可下，分析控件灰掉。

---

## 落子质量（对标 KaTrain trainer.eval_thresholds）

KaTrain 默认阈值从差到好：`12, 6, 3, 1.5, 0.5, 0`。
颜色从差到好：紫 → 红 → 橙 → 黄 → 浅绿 → 绿。

| 损失 L（目） | 档 | 色 token |
|---|---|---|
| L ≥ 12 | 败着 | `quality.purple` `#B44AC0` |
| 6 ≤ L < 12 | 大恶手 | `quality.red` `#E85D4C` |
| 3 ≤ L < 6 | 恶手 | `quality.orange` `#F08A3A` |
| 1.5 ≤ L < 3 | 缓手 | `quality.yellow` `#F2C14E` |
| 0.5 ≤ L < 1.5 | 普通 | `quality.mint` `#7BC67E` |
| L < 0.5 | 好手 | `quality.green` `#3DAA6D` |

点画在已落子上。点的大小另表示「这手有没有被惩罚」（对局后来目差有没有真掉），第一轮可先固定大小，只上颜色。
visits < 80 不打颜色，标「浅」。
阈值放设置里，默认抄上表。

---

## Calibrated Rank AI（必须做，抄 KaTrain）

这不是把 visits 调低。满血 KataGo 就算 50 visits 也远强于人类。
KaTrain 的 Calibrated Rank 是 **policy 蒙眼抽签**：

1. 向引擎要当前局面 `policy`（一维，长度 size*size+1，最后一格是 pass）。
2. 合法非 pass 且 policy>0 的点组成候选。
3. 用 `kyu_rank` 算本手要抽多少个点 `n_moves`。
4. 按 policy 加权、无放回抽 `n_moves` 个点。
5. 在抽中的点里走 policy 最高的那个。
6. 若顶级政策极高（明显手），直接走第一，避免 20k 乱崩。

`kyu_rank` 约定（和 KaTrain 相同）：

- `15` = 15k … `1` = 1k
- `0` = 1d，`-1` = 2d，`-2` = 3d
- UI 显示「15级 … 1级 / 1段 / 2段 / 3段」，存 int。

公式（原样移植，`boardSquares = w*h`，`norm = legalNonPass / boardSquares`）：

```
orig = 0.063015 + 0.7624 * boardSquares / 10**(-0.05737 * kyu + 1.9482)
expTerm = 3.002 * norm * norm - norm - 0.034889 * kyu - 0.5097
modified = (0.3931 + 0.6559 * norm * exp(-expTerm**2) - 0.01093 * kyu) * orig
nMoves = boardSquares * norm / (1.31165 * (modified + 1) - 0.082653)
nMoves = clamp(round(nMoves), 1, legalCount)
```

明显手短路：

```
fillRatio = (boardSquares - legalCount) / boardSquares
override = 0.8 * (1 - 0.5 * fillRatio)
override2 = 0.85 + max(0, 0.02 * (kyu - 8))
if topPolicy >= override || topPolicy >= override2 → 走 policy 第一
```

加权无放回抽样：对每个候选用 `policy` 当权重，Efraimidis–Spirakis：`key = rand() ^ (1/weight)`，取 key 最大的 n 个。

文件：`ai/RankBot.kt`。单测：固定 policy 向量 + 固定种子，nMoves 在 9k/1k/1d 三档有差。

第一轮 AI 只做两档策略：

- `Rank`：上面这套。默认 5k。
- `Full`：走 analysis `moveInfos[0]`。

New Game 必须能选 Rank。不要只有 Full。

可选后做：Human SL（要 `-human-model`，网关支持再开）。

---

## UI — Pixiv 味，不要棋院风

参考：Pixiv 站本身（粉蓝点缀、圆胶囊 tab、卡片插画感、夜间模式偏蓝紫而不是木色）。
不要浅木纹棋盘、不要枯金、不要博物馆留白。

### 色

```
bg.app        #12101A
bg.panel      #1B1730
bg.card       #252042
stroke        #3A3460
text          #F4F0FF
text.dim      #A89BC8
accent.pink   #FF6BA8
accent.blue   #7AB8FF
accent.lilac  #C9B6FF

board.bg      #2A2450          // 蓝紫布，不是木头
grid          #6E64A8  0.45
star          #FF8EC8

stone.ink     #2B2148          // 「黑」→ 深靛紫
stone.ink.rim #5B4D8A
stone.ink.hi  #8E7AC8
stone.paper   #FFEAF4          // 「白」→ sakura 纸
stone.paper.rim #E8B7D2
stone.paper.hi #FFFFFF
```

棋子就是这两色，不要再画成传统黑白瓷。高光偏大、边缘软，略二次元徽章感。
当前手一圈 `accent.pink`。
推荐点用质量色的半透明圆，不要大数字墙。

### 形

- 全圆角 18–24。按钮胶囊。
- 顶栏矮：左边粉点引擎状态，中间短胜率条（粉/蓝双色），右边菜单。
- Android：棋盘尽量满，底下一张圆角 sheet，上拉出候选和树。
- Desktop：棋盘左，右栏卡片叠放，不要表格。
- Home：大插画感标题 + 两个大胶囊「新对局 / 打开棋谱」，最近对局小卡片。
- 动效 160ms spring，落子轻微 squash，不要粒子烟花。

### 禁止

- Material 默认紫 FAB
- 木纹贴图、真实棋子 PNG
- 把 8 个分析开关铺在棋盘四周
- 纯黑 `#000` 背景

### 验收

Android 竖屏棋盘 ≥ 68% 高度。Desktop 棋盘 ≥ 50% 宽。
截图一眼能看出「粉蓝紫」，看不出 KaTrain。

---

## 页面

1. Home
2. New Game：路数、贴目、HvH / HvAI、Rank 滑条（15k–3d）、谁先
3. Board：对局+复盘同一页
4. Engine：URL、token、测试连接
5. Settings：确认落子、质量阈值、坐标开关

---

## 实现顺序（一次只做一个可运行切片）

P0 两端打开，9/13/19 路 HvH，提子劫悔棋。Theme tokens 先落地。
P1 WS + DTO + 胜率条 + top3。
P2 RankBot + 质量六色 + SGF 读写 + 树前后手。
P3 ownership 层、PV 文字、整谱排队分析。

本地测试：`FakeAnalysisServer` 回固定 JSON。真机连用户已有的 analysis WS。

---

## 约束

- 棋规单测：自杀、劫、连劫、气。
- Rank 公式和阈值不许「简化成温度采样」替代。
- 分析 JSON 不改字段名。
- commonMain 不引用 Android View。
- 英文 UI。