# AGENTS.md — KataHana development guide

KataHana is a **self-hosted Go client** (Compose Multiplatform / Kotlin Multiplatform)
that plays, reviews, and analyzes Go with a **KataGo engine behind a small Python
WebSocket gateway**. The gateway is usually run on the same machine as the desktop app, or
on a PC that a phone reaches over the LAN, but the client itself only requires a reachable
engine speaking the protocol — it makes no assumption that engine and client share a
machine. It ships as a desktop app (JVM), an Android app, and an iOS app, all sharing one
`shared` module of Kotlin code.

This file is a compact orientation for anyone editing the repository. The authoritative,
much deeper development handbook lives in **`doc/`** (an mdBook in Chinese); read the
relevant chapter before changing any core mechanism, and keep it in sync when you do.

**Prerequisite: understand the rules of Go before touching core features.** You do not
need to be a strong player, but you must know liberties, capture, suicide, ko / superko,
true vs false eyes (corner / edge / center), two-pass game end, the basic shapes 立 / 尖 /
飞 / 跳 / 虎 / 拆 / 双关 (nobi, kosumi, keima, tobi, tiger's mouth, extension, double
one-point jump), the opening / middle / endgame, and the proverbs 金角银边草肚皮 and
高低搭配. Do not implement or change `domain/`, `engine/`, `ui/board/`, or `ui/session/`
without that. The handbook chapter `doc/src/00-go.md` (*围棋入门：规则与基本手法*)
is the expected baseline.

---

## Repository layout

| Path | What it is |
| --- | --- |
| `shared/` | The whole product: KMP module targeting JVM, Android, iOS (`iosArm64`/`iosSimulatorArm64`). All game logic, engine client, persistence, and Compose UI live here. |
| `androidApp/` | Thin Android host: `KataHanaApp`, `MainActivity`, manifest/resources. |
| `desktopApp/` | Thin desktop host: `main.kt`, packaging config, app icon. |
| `iosApp/` | Xcode shell around the shared iOS framework (`Shared`), Swift entry point. |
| `engine/` | Python gateway `server.py` + `analysis.cfg`: starts KataGo `analysis` and exposes it as one WebSocket JSON service on `ws://127.0.0.1:2080`. Not shipped inside the apps; the app expects it running. |
| `model/` | KataGo network weights, auto-downloaded by the gateway (git-ignored). |
| `doc/` | mdBook handbook (Chinese). **Sources in `doc/src/`; build output `doc/book/` is git-ignored — never commit it.** |
| `screenshots/`, `README*.md` | Marketing/usage material, mirrored in EN/CN/JP. |

## Shared source tree map

All paths relative to `shared/src/commonMain/kotlin/com/acite/katahana/` unless noted.

| Package / file | Responsibility |
| --- | --- |
| `domain/` | **Pure game logic, zero UI/reactive imports.** Board & rules (`Position`, `Rules`, `Move`, `Point`, `StoneColor`), the game tree (`GameTree`, `Node`, `TreeLayout`), the session facade (`GameSession`, `SessionSnapshot`), config/seats (`GameConfig`, `PlayerSeat`), analysis value types (`EvalSeries`, `Forecast`). |
| `ai/` | Pure move choosers over engine outputs: `FullStrengthBot` (KataGo best), `RankBot` (KaTrain-calibrated rank AI), `HumanBot` (human-style sampling of the SL policy), `MoveQuality` (points-lost bands). |
| `engine/` | The engine client: `AnalysisClient` (one WebSocket, query orchestration), `WsClient` + `EngineHttpClient` (expect/actual), `QueryBuilder` (query JSON per use), `AnalysisDto` (JSON schema), `Perspective` (winrate/score view + candidate + dead-stone logic), `EngineBenchmark`. |
| `settings/` | `SettingsRepository`: single DataStore-preferences file, typed flows for every toggle/threshold/profile; `OwnershipStyle`; `AnalysisLayoutMode` (Auto / Compact tabs / Expanded three cards); coordinate edge/grid padding in dp. |
| `recents/` | Recent-game index + persistence (`RecentGame`, `RecentGamesRepository`, `RecentGamesIndex`, `TextFiles` expect/actual). |
| `sgf/` | Hand-rolled SGF v4 reader/writer (`SgfIo`) + platform file dialogs (`SgfFiles`). |
| `changelog/` | Parsing of `git log` output for the in-app "What's new" list. |
| `ui/theme/` | Appearance model (`Appearance`, stone swatches), palettes (`HanaPalette`/`hanaColors`), Compose theme, tokens, Nunito typography. |
| `ui/components/` | Shared chrome: `ScreenChrome` (frosted surfaces, backdrop, glow orbs), `Widgets` (buttons/controls), `HanaDialogs` (scrim modals), `RankLadder`. |
| `ui/board/` | Board rendering: `BoardCanvas` (single Canvas, all layers), `BoardHit` (geometry + hit test), `Stones`, `Connections` (under-stone shapes), `Overlays` (ownership styles), `QualityFaces`. |
| `ui/home/` `ui/session/` `ui/settings/` `ui/engine/` | Screens + their ViewModels. Session is the largest: `SessionScreen`, `SessionViewModel`, `SessionLayout`, `SidePanel` (drawer content), `EvalGraph`, `GameTreeView`, `PlayActionsBar`, `SeatDialog`. |
| `ui/navigation/` | Back handling and the Android lifecycle owner used by screens. |
| `ui/Copy.kt` | **Every user-facing string in the app** (single copy layer, no resource files). |
| `App.kt` | Common app root: DI plumbing, theme, Voyager `Navigator`. |
| `Factory.kt` / `Graphs.kt` | Metro compile-time DI graph. |
| `Platform.kt` | `expect` platform name/isMobile/epochMillis. |
| `generated/` | `AppInfo.kt` produced at build time by `generateHanaInfo` (git version/hash/changelog). Not hand-edited. |

Platform `actual`s live in the same package under `shared/src/{androidMain,jvmMain,iosMain}/kotlin/...`:
`EngineHttpClient.*.kt`, `Platform.*.kt`, `recents/TextFiles.*.kt`, `settings/SettingsPath.*.kt`
(+ `SettingsRepository.android` app-dir install), `ui/navigation/HanaScreenLifecycle.*.kt`, and
`sgf/JvmSgfFiles.kt` (desktop only).

## Core architecture — read before editing

- **Domain is plain imperative Kotlin.** `GameTree`, `GameSession`, `Position` have **no
  StateFlow/mutableStateOf/callbacks**. The UI polls by *snapshotting* after each mutation.
- **One-directional data flow.** UI event → `SessionViewModel` method → domain mutation
  (`GameSession`/`GameTree`) → single `publish()` re-derives a fresh immutable
  `SessionUiState` from `session.snapshot()` + a per-node eval cache + `tree.layout()` →
  `_state: MutableStateFlow<SessionUiState>.update` → screens `collectAsState`.
  Never mutate domain state from a composable or bypass `publish()`.
- **`publish()` is the single choke point** after every tree change (play/pass/undo/redo/
  jump/setSeat/exitReview). It also cancels forecast. Derived overlays (quality marks, eval
  series, stats) are re-synthesized by `withAnalysis()` inside the same update.
- **`bumpNav()` + `navEpoch` cancel stale async work.** Undo/redo/jump/setSeat/pause/exit/
  leave all bump the epoch and cancel the AI/review/forecast jobs; those jobs re-check
  `epoch != navEpoch` before ever mutating again. Never let a coroutine that captured an old
  node write state after the user navigated.
- **`reviewing == canRedo`** (current node has children). Review is *positional*: stepping
  back into the interior is review; the AI parks at the last leaf. `lastLeaf()`/`resumeLeaf()`
  /`exitReview()` let the player jump straight back to their last leaf and clear the review
  state.
- **Forecast never mutates the tree.** Review-mode move forecasting simulates an alternate
  line in `Forecast` (up to 16 virtual plies with ownership-driven reveal pacing) purely for
  rendering; any navigation or `publish()` cancels it and clears virtual stones.
- **The engine is a judge, not a library.** Territory/score/ownership/winrate are never
  computed in-app; every answer comes from one WebSocket query to the local gateway. Rules
  (legal moves, captures, ko) are computed in-app.
- **Protocol model:** one `WebSocket` connection per engine profile; the gateway relays
  JSON to one KataGo process. Queries carry an `id = "<session>:<node>:<kind>:<nonce>"`;
  responses with a matching non-search id complete a one-shot waiter, in-search frames feed
  the live StateFlow. **At most one analysis query is in flight**; starting another first
  sends a `terminate` for the previous one.
- **Theme follows the stones.** `Appearance` (selected stone color pair) drives a
  `HanaPalette`; composables read `hanaColors` (CompositionLocal). `DrawScope`/Canvas code
  cannot read composition locals, so composables **capture** colors and pass them into draw
  helpers. Raw hex belongs only in `ui/theme/Color.kt`/`Appearance.kt`. Never hard-code a
  color inside drawing code.
- **Acrylic needs the same window.** Frosted surfaces blur with Haze only when given a
  `HazeState` whose source is a sibling drawn *behind* them (`HazeSourceSelection.All`).
  Dialogs are in-layout overlays (`HanaScrimModal`), never separate `Dialog` windows, so the
  page behind them can be sampled.

## Where a feature lives (cheat sheet)

| I want to change… | Start here |
| --- | --- |
| Rules / captures / ko / legality | `domain/Rules.kt`, `domain/Position.kt` |
| Board coordinate math, GTP labels | `domain/Point.kt` |
| Tree navigation, undo/redo, review semantics | `domain/GameTree.kt` |
| Snapshot fields exposed to UI | `domain/GameSession.kt` (`SessionSnapshot`) |
| What "an AI seat should move" means | `GameSession.aiShouldMove()` |
| Session UI state / when state is published | `ui/session/SessionViewModel.kt` (`publish`, `SessionUiState`) |
| AI move selection | `ai/RankBot.kt`, `ai/HumanBot.kt`, `ai/FullStrengthBot.kt` + VM `playAi()` |
| Whole-game review queue | VM `analyzeGame()` + `engine/AnalysisClient.queryReview` |
| Move forecasting (long-press) | VM `onForecast()` + `domain/Forecast.kt` |
| Query JSON sent to KataGo | `engine/QueryBuilder.kt` |
| JSON schema / DTO fields | `engine/AnalysisDto.kt` |
| Connection lifecycle / reconnects | `engine/AnalysisClient.runConnection`, `WsClient`, `EngineHttpClient` |
| Settings keys | `settings/SettingsRepository.kt` (Keys object + flows) |
| Recent-game save/open | `recents/RecentGamesRepository.kt`, `RecentGame` |
| SGF export/import | `sgf/SgfIo.kt` |
| Screen navigation / back | `ui/navigation/`, `App.kt`, per-screen composables |
| Session layout (portrait/landscape/desktop) | `ui/session/SessionLayout.kt` (`computeSessionLayout`) |
| Board paint order / animations | `ui/board/BoardCanvas.kt`, `Overlays.kt`, `Stones.kt` |
| User-facing wording | `ui/Copy.kt` |
| New palette / stone appearance | `ui/theme/Appearance.kt` + `Color.kt` (+ `AppearancePaletteTest`) |
| Gateway / KataGo launch args | `engine/server.py`, `engine/analysis.cfg` |

## Conventions & workflow rules

- **All user-facing text goes in `ui/Copy.kt`.** No inline English strings in screens.
- **Colors are defined only in theme files**; drawing helpers take colors as parameters.
- **Keep the global `CapsuleButton` default solid.** The in-game action cluster and most
  interactive controls are intentionally opaque; only surfaces/cards use translucent
  "porcelain" styling. Do not silently restyle global components.
- **Do not change the drawer's information architecture** without an explicit request.
- **Do not add glow orbs / backdrop on the goban** (board area stays a solid goban).
- **Commit subjects use `[Feat] / [Fix] / [doc]` prefixes** (and optional `(owner)` tags) —
  the app renders this history as its in-app changelog. Convention: `[Fix]`, `[Feat]`,
  `[doc]` as seen in `git log`.
- **Do not push or tag releases unless asked.** Version tags are `v1.x`; bump only on request.
- **Tests accompany logic changes.** Pure logic is directly unit-tested from
  `shared/src/commonTest` (deterministic seeds; no mocking frameworks).
- **`doc/` must be kept in sync.** The Chinese handbook documents every module and the
  principles behind the core mechanisms. When you change a module's API, behavior, protocol,
  or UI structure, update the matching `doc/src/` chapter **in the same commit**. `doc/book/`
  (mdbook output) is never committed.

## Building, running, testing

```bash
# Desktop app (hot reload available)
./gradlew :desktopApp:run
./gradlew :desktopApp:hotRun --auto

# Shared JVM unit tests (most logic lives here)
./gradlew :shared:jvmTest

# Android host tests
./gradlew :shared:testAndroidHostTest

# Compile checks
./gradlew :desktopApp:compileKotlin :androidApp:assembleDebug

# Docs (mdBook, sources in doc/src/)
mdbook build doc        # or: mdbook serve doc --open
```

Note: the game needs a running engine gateway for analysis/AI. Start it separately with
`python3 engine/server.py` (it downloads nets into `model/` and launches KataGo).
The engine smoke test self-skips when the gateway is offline.
