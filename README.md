<p align="center">
  <img src="katahana_clip.png" alt="KataHana logo" width="168"/>
</p>

<h1 align="center">KataHana</h1>

<p align="center">
  <strong>Local Go · pink night</strong><br/>
  A Compose Multiplatform client for playing and reading Go — with KataGo on the side, not in the skin.
</p>

<p align="center">
  Desktop JVM · Android · Chinese rules · Human vs Human · Human vs AI
</p>

---

KataHana is a local Go board that happens to speak [KataGo](https://github.com/lightvector/KataGo) Analysis JSON. The rules, the tree, and the stones live on your device. The engine is optional: Human vs Human still works when the WebSocket is down. Human vs AI does not default to a silent 9-dan — you pick a rank, or you ask for full strength.

No wood grain. No museum beige. Night purple, sakura pink, sky-blue stones.

<p align="center">
  <img src="screenshots/gaming.jpg" alt="A 19×19 game: game tree, score curve with a live tag, move-quality counts, ownership tiles, and connections under the stones"/>
</p>

---

## Why it feels different

**The board comes first.** Winrate is a slim bar. Play, save, candidates, and overlays live in a pink-tab drawer. Wide windows (and phone landscape) keep a left column: game tree, then the eval curve, then move-quality counts. Phones in portrait put that stack in the menu.

**A curve that reads like the game.** Score and winrate share one plot. Winrate runs Black 100% at the top, 50/50 in the middle, White 100% at the bottom. The stroke tints toward the leading stone; a small tag follows the current move. Tap a point to jump the tree.

**Faces, not traffic lights.** Recent moves wear little expressions for blunders through good shape — KaTrain-style score-loss bands, drawn as colored glyphs with a thin white outline so a 19×19 still reads at a glance. The quality card tallies every placed stone on the line, Black and White, Good through Blunder.

**Heat that does not hide the stones.** Ownership is optional, morphs between positions, and comes in three looks: soft tiles, drifting fog, or a constellation of stars. Pick one in Settings. Dead stones can rest with a faint yellow halo instead of leaving the board.

**Connections that know Go.** Optional overlay for 立 / 尖 / 飞 / 跳. Lines sit **under** the stones. Same-kind rings stay complete; 飞 and 跳 lean outward from the group so they do not collapse inward.

**Review is a tree, not a tape.** Undo off a leaf and you are reviewing — the AI waits. Play a new stone and you branch. Redo walks the preferred line. AI only moves at a leaf, on its color.

**Games you can come back to.** Save and Save as put a record on the home Recent list, including the evals so the curve and counts survive a reopen. Leave with unsaved moves and KataHana asks first. Export SGF is still there when you want a file.

<p align="center">
  <img src="screenshots/home.jpg" alt="Home — recent games, git-tagged changelog, and version from the latest tag"/>
</p>

The home screen is porcelain cards on a night glow. The brand version follows the latest git tag (`v1.3` today; `v0.1-alpha` if there are none). What's new lists the same history, with tag chips on tagged commits.

<p align="center">
  <img src="screenshots/p2.png" alt="13×13 board with connection lines, quality faces, and last-move ring"/>
</p>

---

## Ownership

Turn the heat on from the side menu. KataGo’s `ownership` map sits under the stones — previous heat holds until the next one lands, then it eases across. Three styles, same data:

| Blocks | Fog | Constellation |
|:---:|:---:|:---:|
| Soft tiles, one per point | Banks of mist that blend together | Stars, edges, and faint faces |
| <img src="screenshots/ownerships_1.png" alt="Ownership as Blocks — rounded tiles tinted by Black and White control"/> | <img src="screenshots/ownerships_2.png" alt="Ownership as Fog — a continuous pink-and-blue mist over the board"/> | <img src="screenshots/ownerships_3.png" alt="Ownership as Constellation — star field with faint faces and linking edges"/> |

Idle motion is quiet on purpose: tiles bounce a little, fog drifts, stars breathe. The overlay stays translucent so the stones still read.

---

## Play

Start a game the way you actually think about one: board size, komi, opponent, color.

<p align="center">
  <img src="screenshots/p5.png" alt="New game sheet — board size, komi, Human vs AI, rank slider"/>
</p>

| | |
|---|---|
| **Boards** | 9×9, 13×13, 19×19 |
| **Rules** | Chinese, positional superko, GTP coordinates (skip I) |
| **Human vs Human** | Same device, no network |
| **Human vs AI** | **Rank** — KaTrain-calibrated policy lottery (`maxVisits=1`), 15k through 3d, default 5k. **Full** — KataGo’s top move at your play visits. |
| **Review** | Undo / redo / click the tree. Non-leaf = you own the next stone. |
| **Eval** | Live Score / Winrate graph and a Good–Blunder table, bound to the saved game. |
| **Analysis** | Live top moves with PV, score loss, and visits. **Analyze game** queues the preferred line. |
| **Records** | Recent games on the home screen. **Save** / **Save as** from the drawer; asked on leave. Open and export SGF (FF[4]). |

The side menu is the rest of the table: to-play, captures, icon pass / undo / redo, candidates, the same tree + curve + stats stack, and toggles for faces, connections, coordinates, ownership, and dead stones.

<p align="center">
  <img src="screenshots/p4.png" alt="Session drawer with status, game tree, candidates, and overlay toggles"/>
</p>

---

## Looks

Four stone palettes, and the winrate bar follows them. The last-move ring and ripples pick up the stone that just landed.

- **Sky & Sakura** — clear-sky blue and cherry blossom (default)
- **Ink & Paper** — deep indigo and sakura paper
- **Midnight & Snow** — night charcoal and warm snow
- **Lilac & Peach** — soft lilac and ripe peach

Quality thresholds live next door. Defaults match KaTrain (`12 / 6 / 3 / 1.5 / 0.5`). Ownership style sits in the same Settings page.

<p align="center">
  <img src="screenshots/p1.png" alt="Settings — appearance palettes and quality thresholds"/>
</p>

---

## KataGo, on a socket

Point the client at an Analysis engine:

```
ws://127.0.0.1:2080
```

<p align="center">
  <img src="screenshots/engine.jpg" alt="Engine settings — WebSocket URL, play visits, review visits, test connection"/>
</p>

One JSON object per WebSocket text frame — KataGo’s own `rootInfo`, `moveInfos`, `ownership`, and `policy`. Winrates are stored as Black. Rank queries ask for policy at one visit; live analysis, full-strength genmove, and queued review use your play / review visits.

Engine offline? Human vs Human still plays. The analysis chrome just goes quiet.

---

## Run it

```bash
# Desktop
./gradlew :desktopApp:run
# hot reload
./gradlew :desktopApp:hotRun --auto
```

iOS: open [`iosApp/`](./iosApp) in Xcode.

### Android from the terminal

The app module is `:androidApp` (`applicationId` `com.acite.katahana`). Gradle reads the SDK from `local.properties`:

```
sdk.dir=/path/to/Android/Sdk
```

`adb` is `$sdk.dir/platform-tools/adb` (or already on `PATH`). IntelliJ’s Run button is usually `installDebug`; the same thing from a shell is `./gradlew :androidApp:installDebug`.

**Device.** USB debugging on, cable plugged in, RSA prompt accepted:

```bash
adb devices
```

You want `device`, not `unauthorized` or `offline`. Several devices? List serials with `adb devices -l`, then prefix every `adb` call with `-s <serial>` or export `ANDROID_SERIAL=<serial>` so Gradle picks the same one.

Wireless (Android 11+ Developer options → Wireless debugging):

```bash
adb pair <ip>:<pairing-port>
adb connect <ip>:<debug-port>
adb devices
```

**Release onto the phone.** Gradle builds the release APK, signs it, and talks to adb for you:

```bash
./gradlew :androidApp:installRelease
adb shell am start -n com.acite.katahana/.MainActivity
```

Same install, but you keep the APK file:

```bash
./gradlew :androidApp:assembleRelease
adb install -r androidApp/build/outputs/apk/release/androidApp-release.apk
```

`-r` replaces an existing KataHana. If adb refuses with `INSTALL_FAILED_UPDATE_INCOMPATIBLE`, the on-device copy was signed with a different key — uninstall first:

```bash
adb uninstall com.acite.katahana
adb install androidApp/build/outputs/apk/release/androidApp-release.apk
```

**Just the APK** (no device needed):

```bash
./gradlew :androidApp:assembleRelease
# androidApp/build/outputs/apk/release/androidApp-release.apk
```

Play-style App Bundle, if you need one:

```bash
./gradlew :androidApp:bundleRelease
# androidApp/build/outputs/bundle/release/androidApp-release.aab
```

Release is currently signed with the **debug** keystore (`signingConfig = signingConfigs.getByName("debug")` in `androidApp/build.gradle.kts`), minify off. That is why `installRelease` works without a store key. It is still a release variant (`debuggable=false`). A Play upload needs its own signing config later.

Debug (what the IDE run configuration typically installs):

```bash
./gradlew :androidApp:installDebug
# or
./gradlew :androidApp:assembleDebug
# androidApp/build/outputs/apk/debug/androidApp-debug.apk
```

### Tests

```bash
./gradlew :shared:jvmTest
./gradlew :shared:testAndroidHostTest
```

---

## Inside

Compose Multiplatform (`shared` + `androidApp` + `desktopApp`). Voyager for navigation, Metro for DI, DataStore for settings, a JSON file for recent games. Board, rules, and SGF are pure Kotlin. The engine client is a thin WebSocket — no second protocol. The home version and changelog chips come from git tags at compile time.

```
shared/src/commonMain/kotlin/com/acite/katahana/
  domain/     GameTree, Rules, connections, eval series
  engine/     WsClient, Analysis JSON, ownership
  ai/         RankBot, FullStrengthBot, quality bands
  recents/    Save / Save as list, persisted evals
  changelog/  git tags into the home log
  sgf/        SGF read / write
  ui/         board, session, home, theme
```

---
