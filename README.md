<p align="center">
  <img src="katahana_clip.png" alt="KataHana logo" width="168"/>
</p>

<h1 align="center">KataHana</h1>

<p align="center">
  <strong>Local Go · pink night</strong><br/>
  A Compose Multiplatform client for playing and reviewing Go — KataGo at the side of the board, not inside it.
</p>

<p align="center">
  Desktop JVM · Android · Chinese rules · Human vs Human · Human vs AI
</p>

---

KataHana is a Go board that stays on your device. The rules, the game tree, the stones, the records — all of it is local, with no server to join and no account to keep. KataGo is a welcome guest rather than a requirement: the client speaks the engine's Analysis JSON over a plain WebSocket, so you can pair it with whichever KataGo build you like — or with none at all. When the engine is away, Human vs Human plays exactly the same game.

When you do invite the AI in, you decide who sits across from you — and you can change your mind mid-game. Tap a color in the side panel and set it to **Human**, **Rank AI**, **Human-like**, or **KataGo 9D+**. Both sides are independent, so AI vs AI is allowed. **Human-like** (the default for a new AI seat) samples KataGo's human-SL net at 15 kyu to 3 dan, so local fights can miss the way a person of that rank would. **Rank** is KaTrain's older policy lottery — sharp in tactics, looser in direction. **KataGo 9D+** plays the engine's strongest move.

And it looks like the game finally got permission to stay up late. No wood grain, no museum beige. The whole app is dressed for the night — deep purples, sakura pink — and the default stones, clear-sky blue against cherry blossom, still hold up against the dark.

<p align="center">
  <img src="screenshots/gaming.jpg" alt="A 19×19 game in progress: game tree, score curve with a live value tag, move-quality counts, ownership tiles, and connections tucked under the stones"/>
</p>

---

## Why it feels different

**The board comes first.** The stones own the middle of the screen and everything else waits in the wings. Winrate keeps to a slim bar, and play, save, candidates, and overlays tuck into a pink-tab drawer until you need them. Wide windows — and phones in landscape — keep a quiet column beside the board: the game tree, the eval curve, and the move-quality counts. In portrait, the same stack hides in the menu.

**A curve that reads like the game.** Score and winrate share one plot, and it obeys intuition. Winrate runs the full way from Black 100% at the top, through 50/50 in the middle, to White 100% at the bottom — so 70% for Black sits 70% of the way up the chart instead of posing as some cryptic “+20”. The stroke tints toward whoever is ahead, a small tag rides along with the latest point, and tapping anywhere on the curve jumps the game tree to that move.

**Faces, not traffic lights.** Recent moves carry little expressions instead of anonymous markers — one glyph each, from good shape down to blunder, scored with KaTrain-style loss bands and edged in white so even a packed 19×19 still reads at a glance. Below the board, the quality card tallies every placed stone, Black and White alike.

**Connections that know Go.** The optional connection overlay draws 立, 尖, 飞, and 跳 lines **under** the stones, so it can never cover a stone or hide a capture. Same-kind links keep their rings complete, while 飞 and 跳 lean outward from the group instead of collapsing inward.

**Heat that lets the stones breathe.** Ownership is an overlay, not a repaint. It eases in from one position to the next, it can be switched off entirely, and it comes in three looks that share the same data. Dead stones may even stay on the board under a faint yellow halo rather than being swept away.

**Review is a tree, not a tape.** Undo back off the latest move and you are reviewing — the AI parks at the leaf and waits. Play a stone and history branches into a new variation; redo walks the preferred line. The engine only ever moves at a leaf of the tree, on its own color, so every branch stays yours to explore.

**Games you can come back to.** Save and Save as drop the game onto the Recent shelf with its evaluation attached, so the curve and the quality counts are still there after a restart. Try to leave with unsaved moves and KataHana asks first. And plain SGF export (FF[4]) is there for when you want the game in someone else's hands.

All of it at once on a 13×13 board: connections resting under the stones, expressions on the recent moves, and the last-move ring on the newest stone.

<p align="center">
  <img src="screenshots/p2.png" alt="A 13×13 game: connection lines under the stones, move-quality faces, and the last-move ring"/>
</p>

---

## Home

Open the app and you land on a night-lit shelf: porcelain cards over a soft night glow. The shelf is the Recent list — each game keeps its name, its board, and its evaluation, so picking it back up restores the curve where you left it.

<p align="center">
  <img src="screenshots/home.jpg" alt="Home — recent games, git-tagged changelog, and a version label read from the latest tag"/>
</p>

The other card is **What's new**, which scrolls the project's own git history with tag chips on tagged commits. Nothing here is typed by hand: the changelog is generated at build time, and the version in the corner follows the nearest git tag (`v1.4` today; `v0.1-alpha` when the tree has no tags).

---

## Ownership

Turn the heat on from the side menu and KataGo's ownership map settles under the stones — each new map eases in from the previous one rather than snapping into place. One piece of data, three ways to wear it:

| Blocks | Fog | Constellation |
|:---:|:---:|:---:|
| Soft tiles, one per point | Banks of mist that blend together | Stars, edges, and faint faces |
| <img src="screenshots/ownerships_1.png" alt="Ownership as Blocks — rounded tiles tinted by Black and White control"/> | <img src="screenshots/ownerships_2.png" alt="Ownership as Fog — a continuous pink-and-blue mist over the board"/> | <img src="screenshots/ownerships_3.png" alt="Ownership as Constellation — a star field with faint faces and linking edges"/> |

Idle motion stays quiet on purpose: tiles bounce a little, fog drifts, stars breathe. Whichever style you pick, the layer keeps itself translucent so the stones below stay legible.

---

## Play

A new game starts with the questions you would ask yourself anyway: how big the board, what komi, who is across from you, and which color you would like.

<p align="center">
  <img src="screenshots/p5.png" alt="New game sheet — board size, komi, Human vs AI, rank slider"/>
</p>

| | |
|---|---|
| **Boards** | 9×9, 13×13, 19×19 |
| **Rules** | Chinese, positional superko, GTP coordinates (skip I) |
| **Human vs Human** | Same device, no network |
| **Seats** | Each color is **Human**, **Rank AI**, **Human-like**, or **KataGo 9D+**. Change them from the side panel during a game; opening the drawer pauses play. Rank and Human-like go from 15k to 3d. **Human-like** samples KataGo `humanPolicy` at `preaz_{rank}` (`maxVisits=1`). **Rank** is KaTrain's policy lottery. **KataGo 9D+** is the engine's top move at your play visits. |
| **Review** | Undo, redo, or tap the tree. Off the latest move you are reviewing; on a non-leaf node the next stone is yours. |
| **Eval** | Live Score / Winrate graph and a Good–Blunder table, bound to the saved game. |
| **Analysis** | Live top moves with PV, score loss, and visits. **Analyze game** queues the preferred line. |
| **Records** | Recent games on the home screen. **Save** / **Save as** from the drawer, and a prompt before you leave unsaved moves behind. Open and export SGF (FF[4]). |

Once the stones are down, the board holds the middle of the screen and a side panel rounds out the table: whose turn it is and what has been captured, pass / undo / redo, candidate moves, the same tree + curve + stats stack you get in the wide layout, and toggles for expressions, connections, coordinates, ownership, and dead stones.

<p align="center">
  <img src="screenshots/p4.png" alt="The session drawer — status, game tree, candidates, and overlay toggles"/>
</p>

---

## Looks

The board dresses for the night, and the winrate bar follows suit. The last-move ring and its ripples take the color of the stone that just landed.

- **Sky & Sakura** — clear-sky blue and cherry blossom (default)
- **Ink & Paper** — deep indigo and sakura paper
- **Midnight & Snow** — night charcoal and warm snow
- **Lilac & Peach** — soft lilac and ripe peach

Quality thresholds live on the same Settings page, with KaTrain's defaults (`12 / 6 / 3 / 1.5 / 0.5`) ready until you tune them, and the ownership-style picker sits right alongside.

<p align="center">
  <img src="screenshots/p1.png" alt="Settings — appearance palettes and quality thresholds"/>
</p>

---

## KataGo, on a socket

The engine client is a plain WebSocket, so “installing AI” means pointing it at one:

```
ws://127.0.0.1:2080
```

This repo ships a gateway that speaks that URL. From the project root:

```bash
pip install -r engine/requirements.txt
python engine/server.py --katago /path/to/katago
```

The first run creates `model/` if needed and downloads the main net (`b10c384h6nbttflrs`) plus the human-SL net (`b18c384nbt-humanv0`) from KataGo's GitHub releases. Config lives at `engine/analysis.cfg`; KataGo's cache goes in `engine/home/`. The binary itself is GPU-specific, so it is not fetched — pass `--katago`, drop a binary at `engine/bin/katago`, or have `katago` on your `PATH`. `--skip-download` skips the net fetch if you already placed the files.

<p align="center">
  <img src="screenshots/engine.jpg" alt="Engine settings — WebSocket URL, play visits, review visits, test connection"/>
</p>

Every frame is one JSON object in KataGo's own shape — `rootInfo`, `moveInfos`, `ownership`, `policy`, `humanPolicy` — so there is no second protocol to keep in sync. Winrates are stored as Black throughout. Human and Rank queries ask the engine for policy at a single visit (Human also sets `humanSLProfile` on that query only); live analysis, full-strength genmove, and queued review use the play and review visits you set for the profile. Engine offline? Human vs Human carries on regardless — the analysis chrome simply goes quiet.

---

## Run it

The desktop app is one command away:

```bash
./gradlew :desktopApp:run
# hot reload
./gradlew :desktopApp:hotRun --auto
```

The local KataGo WebSocket gateway (nets land in `model/` on first run):

```bash
pip install -r engine/requirements.txt
python engine/server.py --katago /path/to/katago
```

On iOS, open [`iosApp/`](./iosApp) in Xcode.

### Android from the terminal

The Android app lives in the `:androidApp` module (`applicationId` `com.acite.katahana`). Gradle needs to know where the SDK is, and that goes in `local.properties`:

```
sdk.dir=/path/to/Android/Sdk
```

`adb` lives at `$sdk.dir/platform-tools/adb` (or is already on your `PATH`).

**On a device.** Plug the phone in with USB debugging enabled and accept the RSA prompt:

```bash
adb devices
```

You want `device`, not `unauthorized` or `offline`. Running several devices at once? List the serials with `adb devices -l`, then prefix every `adb` call with `-s <serial>` — or export `ANDROID_SERIAL=<serial>` so Gradle picks the same one.

**Wireless** (Android 11+ Developer options → Wireless debugging):

```bash
adb pair <ip>:<pairing-port>
adb connect <ip>:<debug-port>
adb devices
```

**Release onto the phone.** Gradle builds the release APK, signs it, and lets adb do the rest:

```bash
./gradlew :androidApp:installRelease
adb shell am start -n com.acite.katahana/.MainActivity
```

Same install, but you keep the APK file around:

```bash
./gradlew :androidApp:assembleRelease
adb install -r androidApp/build/outputs/apk/release/androidApp-release.apk
```

The `-r` flag replaces an existing KataHana. If adb answers `INSTALL_FAILED_UPDATE_INCOMPATIBLE`, the copy already on the phone was signed with a different key — uninstall first and try again:

```bash
adb uninstall com.acite.katahana
adb install androidApp/build/outputs/apk/release/androidApp-release.apk
```

**Just the APK** (no phone required):

```bash
./gradlew :androidApp:assembleRelease
# androidApp/build/outputs/apk/release/androidApp-release.apk
```

Prefer a Play-style App Bundle?

```bash
./gradlew :androidApp:bundleRelease
# androidApp/build/outputs/bundle/release/androidApp-release.aab
```

A note on signing: the release build currently signs with the **debug** keystore (`signingConfig = signingConfigs.getByName("debug")` in `androidApp/build.gradle.kts`) and keeps minification off. That is why `installRelease` works without a store key of your own — it is still a real release variant (`debuggable=false`). A Play upload will want its own signing config later.

**Debug** (what the IDE run configuration typically installs):

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

## Under the hood

Compose Multiplatform, with the game logic in `shared` and thin launchers in `androidApp` and `desktopApp`. Navigation is Voyager, dependency injection is Metro, settings ride in DataStore, and the Recent shelf is a small JSON index. The board, the rules, and SGF are pure Kotlin — no platform code in the way — and the engine client is deliberately thin: one WebSocket speaking KataGo's own JSON. On the home screen, the version label and the changelog are stamped from git tags at build time.

```
shared/src/commonMain/kotlin/com/acite/katahana/
  domain/     game tree, rules, connections, eval series
  engine/     WebSocket client, Analysis JSON, ownership
  ai/         HumanBot, RankBot, FullStrengthBot, quality bands
  settings/   preferences and ownership styles
  recents/    Save / Save as shelf, persisted evals
  changelog/  git tags into the home log
  sgf/        SGF read / write
  ui/         board, session, home, theme
```

---
