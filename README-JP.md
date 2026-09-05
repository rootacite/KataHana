<p align="center">
  <img src="katanhana_clip.png" alt="KataHana ロゴ" width="168"/>
</p>

<h1 align="center">KataHana</h1>

<p align="center">
  <strong>ローカル囲碁・ピンクの夜</strong><br/>
  囲碁を打ち、棋譜を並べ直すための Compose Multiplatform クライアント。KataGo は盤の隣に寄り添う存在であり、盤の中に住み込むものではありません。
</p>

<p align="center">
  デスクトップ JVM・Android・中国ルール・対人対局・人 vs AI
</p>

---

KataHana は、あなたの端末の中にだけある碁盤です。ルールも棋譜のツリーも、石も記録も——すべてローカル。参加すべきサーバーも、作らなければならないアカウントもありません。KataGo は必須の存在ではなく、招かれた客人です。クライアントは素の WebSocket 越しに KataGo の Analysis JSON を話すだけなので、お好みの KataGo ビルドと組み合わせられますし、何も入れなくてもかまいません。エンジンがオフラインの間も、対人対局はまったく同じように進行します。

AI を招き入れるなら、向かい側に座る相手はあなたが決めます——そして対局の途中で考えを変えることもできます。サイドパネルでどちらかの色をタップして、**人間**・**段位 AI**・**人間らしい AI**・**KataGo 9D+** から選んでください。両サイドは独立しているので、AI 同士の対局も認められています。**人間らしい AI**（新しい AI 席のデフォルト）は、KataGo の人間SLネットを 15k〜3d の範囲でサンプリングするため、局部の戦いでも、その段位の人間らしい見落としを犯します。**段位 AI** は KaTrain 由来の従来型「手の抽選」——戦いの読みは鋭く、大局観はやや緩めです。**KataGo 9D+** は、エンジンが考える最強の一手を打ちます。

そして見た目は、まるでこのゲームがようやく夜更かしの許可をもらったかのようです。木目も、美術館のようなベージュもありません。アプリ全体が夜のために着飾っています——深い紫、桜ピンク。デフォルトの石は、快晴の空色と桜の花びらの取り合わせで、暗い盤面の上でもくっきりと映えます。

<p align="center">
  <img src="screenshots/gaming.jpg" alt="進行中の 19路盤の対局：棋譜ツリー、ライブの数値ラベル付き目数差カーブ、着手品質の集計、領地タイル、石の下にしまわれた連絡線"/>
</p>

---

## 何が違うのか

**盤が何より先に来る。** 石が画面の中心を占め、それ以外はすべて袖に控えます。勝率は細いバーひとつに収まり、着手・保存・候補手・オーバーレイはピンクのタブ付きドロワーにしまわれ、必要なときだけ現れます。広いウィンドウ——そして横向きのスマホ——では、盤の横に静かな列が残ります。棋譜ツリー、形勢カーブ、着手の質の集計です。縦向きでは、同じ一揃いがメニューの中に隠れます。

**そのまま棋譜として読めるカーブ。** 目数差と勝率はひとつのグラフを共有し、感覚に素直です。勝率の軸は上端の黒 100% から、中央の五分、下端の白 100% までを通るので、黒 70% ならグラフの 70% の高さに座ります。謎めいた「+20」のように偽装されることはありません。線の色はリードしている方に傾き、最新の点には小さなラベルが寄り添い、カーブ上のどこかをタップすれば棋譜ツリーがその手までジャンプします。

**信号機ではなく、表情。** 最近の着手には、匿名のマーカーではなく小さな表情が付きます。形の良い手から悪手まで、一手ひとつのグリフを KaTrain 式の損失帯で採点し、白い縁取りを付けてあるので、19路盤がびっしり埋まっていても一目で読み取れます。盤の下では、品質カードが打たれた石を黒白問わず数え上げます。

**囲碁を知っている連絡。** 任意表示の連絡レイヤーは、立・尖・飛・跳のラインを石の**下に**描くので、石を覆い隠したり、取られた石を隠したりすることは決してありません。同色どうしの連結はリングを完全な形に保ち、飛と跳はグループの外側へと伸びて、内側に縮こまることはありません。

**石に息をさせるヒート。** 領地はオーバーレイであって、塗り直しではありません。ひとつの局面から次の局面へと滑らかに移り変わり、完全にオフにすることもでき、同じデータをまとう 3 種類の見た目が用意されています。死んだ石はさらわれるのではなく、うっすらと黄色い光輪をまとって盤上に残ることもあります。

**リプレイはテープではなくツリー。** 最新の一手から一手ずつ Undo すれば、そこがリプレイです——AI は葉の上で待機します。石を打てば、歴史は新しい変化として分岐し、Redo は好みのラインを進みます。エンジンが手を下すのは常にツリーの葉の上、自分の色の番だけ。だからすべての分岐は、あなたが探検できるまま残るのです。

**また戻ってこれる棋譜たち。** Save / Save as は、評価ごと対局を Recent の棚に降ろします。再起動しても、カーブと質の集計はそこにあります。未保存の手を残したまま立ち去ろうとすると、KataHana が先に尋ねます。そして、棋譜をほかの人の手に渡したくなったら、素の SGF（FF[4]）書き出しがあります。

これらすべてが 13路盤の上で同時に現れます。石の下に連絡のラインが休み、新しい着手に表情が付き、最新の石には最終着手リングがかかります。

<p align="center">
  <img src="screenshots/p2.png" alt="13路盤の対局：石の下の連絡線、着手品質の表情、最終着手リング"/>
</p>

---

## ホーム

アプリを開くと、夜の棚の前に着きます。柔らかな夜光の上に浮かぶ、磁器のようなカード。その棚が Recent リストです——各対局は名前も、盤のサイズも、評価も覚えていて、拾い上げればカーブは去ったときのまま戻ってきます。

<p align="center">
  <img src="screenshots/home.jpg" alt="ホーム——最近の対局、git タグから生成された更新ログ、直近タグを読んだバージョン表記"/>
</p>

もう一枚のカードは **What's new**。プロジェクト自身の git 履歴を、タグ付きコミットにはチップを添えてスクロール表示します。ここに手書きの行はひとつもありません。更新ログはビルド時に生成され、隅のバージョン表記は直近の git タグに従います（今日は `v1.4.1`。タグがひとつもないツリーでは `v0.1-alpha`）。

---

## 領地（Ownership）

サイドメニューからヒートをオンにすると、KataGo の領地マップが石の下に沈んでいきます——新しいマップは、いきなり入れ替わるのではなく、前のマップから滑らかに移り変わります。ひとつのデータに、3 通りの着こなし：

| ブロック | 霧 | 星座 |
|:---:|:---:|:---:|
| ひとマスずつの柔らかいタイル | 溶け合う霧のたなびき | 星、辺、そしてほのかな顔 |
| <img src="screenshots/ownerships_1.png" alt="領地スタイル「ブロック」——黒白の勢力で染まる角丸タイル"/> | <img src="screenshots/ownerships_2.png" alt="領地スタイル「霧」——盤を覆う連続的なピンクと青の霧"/> | <img src="screenshots/ownerships_3.png" alt="領地スタイル「星座」——ほのかな顔と結ぶ辺を持つ星空"/> |

静止時のアニメーションは、意図的に静かです。ブロックはわずかに跳ね、霧は漂い、星は呼吸します。どのスタイルを選んでも、レイヤーは半透明のまま保たれるので、下の石は読みやすいままです。

---

## 対局

新しい対局は、あなたが自分に問いかけるであろう質問から始まります——盤はどのサイズか、コミはいくつか、向かいには誰がいて、どの色を持つか。

<p align="center">
  <img src="screenshots/p5.png" alt="新規対局シート——盤サイズ、コミ、人 vs AI、段位スライダー"/>
</p>

| | |
|---|---|
| **盤** | 9路・13路・19路 |
| **ルール** | 中国ルール、positional superko（全局同形禁止）、GTP 座標（I はスキップ） |
| **対人対局** | 同じ端末で、ネットワーク不要 |
| **席** | それぞれの色を **人間**・**段位 AI**・**人間らしい AI**・**KataGo 9D+** に。対局中でもサイドパネルから変更でき、ドロワーを開くと対局は一時停止します。段位 AI と人間らしい AI は 15k〜3d の範囲。**人間らしい AI** は `preaz_{rank}`（`maxVisits=1`）で KataGo の `humanPolicy` をサンプリング。**段位 AI** は KaTrain の手の抽選。**KataGo 9D+** は設定した play visits でエンジンの最善手を打ちます。 |
| **リプレイ** | Undo、Redo、またはツリーをタップ。最新手から外れたらリプレイ中です。葉でないノードにいるとき、次の石はあなたのものです。 |
| **評価** | リアルタイムの目数差・勝率グラフと Good–Blunder 着手品質表。保存済みの対局に結びついています。 |
| **解析** | PV・損失目数・訪問数付きのライブ候補手。**Analyze game** は好みのラインをキューに入れます。 |
| **記録** | ホーム画面の Recent。ドロワーから **Save** / **Save as**、未保存の手を残したまま出ようとすると確認が出ます。SGF（FF[4]）の読み込みと書き出し。 |

石が置かれてしまえば、盤が画面の中心を占め、サイドパネルが机の周りを整えます——手番と取った石、pass / undo / redo、候補手、ワイドレイアウトと同じツリー + カーブ + 統計、そして表情・連絡・座標・領地・死石のトグル。

<p align="center">
  <img src="screenshots/p4.png" alt="対局ドロワー——ステータス、棋譜ツリー、候補手、オーバーレイのトグル"/>
</p>

---

## 見た目

盤は夜のために着飾り、勝率バーもそれに続きます。最終着手のリングとその波紋は、いま置かれた石の色を取ります。

- **Sky & Sakura**——快晴の空色と桜（デフォルト）
- **Ink & Paper**——深い藍と桜和紙
- **Midnight & Snow**——夜のチャコールと温かな雪
- **Lilac & Peach**——柔らかなライラックと熟した桃

品質のしきい値は同じ設定ページにあり、KaTrain のデフォルト（`12 / 6 / 3 / 1.5 / 0.5`）があなたの調整を待って構えています。領地スタイルのピッカーもすぐ隣です。

<p align="center">
  <img src="screenshots/p1.png" alt="設定——配色パレットと品質しきい値"/>
</p>

---

## ソケット越しの KataGo

エンジンクライアントは素の WebSocket です。だから「AI のインストール」とは、クライアントをどこかの URL に向けること：

```
ws://127.0.0.1:2080
```

このリポジトリには、その URL を話すゲートウェイが同梱されています。プロジェクトルートから：

```bash
pip install -r engine/requirements.txt
python engine/server.py --katago /path/to/katago
```

初回起動時、必要なら `model/` を作成し、メインネット（`b10c384h6nbttflrs`）と人間SLネット（`b18c384nbt-humanv0`）を KataGo の GitHub releases からダウンロードします。設定は `engine/analysis.cfg`、KataGo のキャッシュは `engine/home/` に置かれます。エンジン本体は GPU 依存のため取得されません——`--katago` で渡すか、`engine/bin/katago` にバイナリを置くか、`katago` を `PATH` に通してください。ファイルを自分で用意済みなら `--skip-download` でダウンロードを省略できます。

<p align="center">
  <img src="screenshots/engine.jpg" alt="エンジン設定——WebSocket URL、play visits、review visits、接続テスト"/>
</p>

すべてのフレームは、KataGo 固有の形式を保ったひとつの JSON オブジェクトです——`rootInfo`・`moveInfos`・`ownership`・`policy`・`humanPolicy`——つまり、同期を取るべき第二のプロトコルはありません。勝率は一貫して黒番視点で保存されます。人間と段位 AI のクエリは、1 回の訪問での policy だけをエンジンに求めます（人間はそのクエリに限って `humanSLProfile` も設定します）。ライブ解析、全力の genmove、キューに入れたリプレイは、プロファイルに設定した play / review visits を使います。エンジンがオフラインでも？ 対人対局はそのまま進みます——解析の飾りが静かになるだけです。

---

## 起動

デスクトップアプリは、コマンドひとつで起きます：

```bash
./gradlew :desktopApp:run
# ホットリロード
./gradlew :desktopApp:hotRun --auto
```

ローカル KataGo WebSocket ゲートウェイ（初回起動でネットを `model/` に置きます）：

```bash
pip install -r engine/requirements.txt
python engine/server.py --katago /path/to/katago
```

iOS では、Xcode で [`iosApp/`](./iosApp) を開いてください。

### ターミナルから Android を

Android アプリは `:androidApp` モジュール（`applicationId` は `com.acite.katahana`）。Gradle に SDK の場所を教える必要があり、それは `local.properties` に書きます：

```
sdk.dir=/path/to/Android/Sdk
```

`adb` は `$sdk.dir/platform-tools/adb` にあります（またはすでに `PATH` に載っています）。

**実機へ。** USB デバッグを有効にした端末をつなぎ、RSA のプロンプトを承認します：

```bash
adb devices
```

表示されるのは `device` であるべきで、`unauthorized` や `offline` ではありません。複数の端末を同時に使っていますか？ `adb devices -l` でシリアルを確認し、すべての `adb` コマンドに `-s <serial>` を付けるか、`ANDROID_SERIAL=<serial>` をエクスポートして、Gradle にも同じ端末を選ばせてください。

**ワイヤレス**（Android 11+ の 開発者オプション → ワイヤレスデバッグ）：

```bash
adb pair <ip>:<pairing-port>
adb connect <ip>:<debug-port>
adb devices
```

**端末へ Release を。** Gradle が release APK をビルドして署名し、残りは adb が片付けます：

```bash
./gradlew :androidApp:installRelease
adb shell am start -n com.acite.katahana/.MainActivity
```

同じインストールでも、APK ファイルを手元に残したい場合：

```bash
./gradlew :androidApp:assembleRelease
adb install -r androidApp/build/outputs/apk/release/androidApp-release.apk
```

`-r` は既存の KataHana を置き換えます。adb が `INSTALL_FAILED_UPDATE_INCOMPATIBLE` と返したら、端末に残っているコピーは別の鍵で署名されています——先にアンインストールして、もう一度試してください：

```bash
adb uninstall com.acite.katahana
adb install androidApp/build/outputs/apk/release/androidApp-release.apk
```

**APK だけ欲しい**（端末不要）：

```bash
./gradlew :androidApp:assembleRelease
# androidApp/build/outputs/apk/release/androidApp-release.apk
```

Play 式の App Bundle がお好みなら：

```bash
./gradlew :androidApp:bundleRelease
# androidApp/build/outputs/bundle/release/androidApp-release.aab
```

署名についての覚え書き：現在の release ビルドは **debug** キーストアで署名し（`androidApp/build.gradle.kts` の `signingConfig = signingConfigs.getByName("debug")`）、縮小化もオフのままです。だから `installRelease` が自分のキーストアなしで通るのです——それでも本物の release バリアントです（`debuggable=false`）。Play へのアップロードでは、後日あらためて署名設定が要るでしょう。

**Debug**（IDE の実行構成がふつうインストールするもの）：

```bash
./gradlew :androidApp:installDebug
# または
./gradlew :androidApp:assembleDebug
# androidApp/build/outputs/apk/debug/androidApp-debug.apk
```

### テスト

```bash
./gradlew :shared:jvmTest
./gradlew :shared:testAndroidHostTest
```

---

## 中身の話

Compose Multiplatform。ゲームロジックは `shared` にあり、`androidApp` と `desktopApp` は薄いランチャーです。ナビゲーションは Voyager、依存性注入は Metro、設定は DataStore、Recent の棚は小さな JSON インデックス。盤・ルール・SGF は純 Kotlin——邪魔をするプラットフォームコードはありません——エンジンクライアントは意図的に薄く、KataGo 自身の JSON を話す WebSocket ひとつだけです。ホーム画面では、バージョン表記と更新ログがビルド時に git タグから刻印されます。

```
shared/src/commonMain/kotlin/com/acite/katahana/
  domain/     棋譜ツリー・ルール・連絡・評価シリーズ
  engine/     WebSocket クライアント・Analysis JSON・領地
  ai/         HumanBot・RankBot・FullStrengthBot・品質帯
  settings/   設定と領地スタイル
  recents/    Save / Save as の棚・保存された評価
  changelog/  ホームのログに git タグを
  sgf/        SGF 読み書き
  ui/         盤・対局・ホーム・テーマ
```

---
