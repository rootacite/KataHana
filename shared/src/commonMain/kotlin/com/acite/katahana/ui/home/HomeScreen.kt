package com.acite.katahana.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.acite.katahana.changelog.ChangelogEntry
import com.acite.katahana.domain.PlayMode
import com.acite.katahana.epochMillis
import com.acite.katahana.generated.AppInfo
import com.acite.katahana.recents.RecentGame
import com.acite.katahana.recents.formatSavedAt
import com.acite.katahana.sgf.LocalSgfFiles
import com.acite.katahana.sgf.parseSgf
import com.acite.katahana.ui.Copy
import com.acite.katahana.ui.LocalAppExit
import com.acite.katahana.ui.components.BrandMark
import com.acite.katahana.ui.components.HomeNavTile
import com.acite.katahana.ui.components.PorcelainCard
import com.acite.katahana.ui.components.QuietTextButton
import com.acite.katahana.ui.engine.EngineSettingsScreen
import com.acite.katahana.ui.session.SessionScreen
import com.acite.katahana.ui.settings.SettingsScreen
import com.acite.katahana.ui.theme.HanaColors
import com.acite.katahana.ui.theme.hanaTokens
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import dev.zacsweers.metrox.viewmodel.metroViewModel
import kotlinx.coroutines.launch

class HomeScreen : Screen {
    @Composable
    override fun Content() {
        HomeRoute()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeRoute() {
    val navigator = LocalNavigator.currentOrThrow
    var newMode by remember { mutableStateOf<PlayMode?>(null) }
    val tokens = hanaTokens
    val sgfFiles = LocalSgfFiles.current
    val appExit = LocalAppExit.current
    val scope = rememberCoroutineScope()
    val homeVm = metroViewModel<HomeViewModel>()
    val lastGame by homeVm.lastGame.collectAsState()
    val recents by homeVm.recentGames.collectAsState()

    val openSgf: () -> Unit = {
        scope.launch {
            val text = sgfFiles.open() ?: return@launch
            val game = runCatching { parseSgf(text) }.getOrNull() ?: return@launch
            navigator.push(SessionScreen(game.config, game.tree))
        }
    }

    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .background(HanaColors.bgApp),
    ) {
        val landscape = maxWidth > maxHeight
        val wide = maxWidth >= 840.dp
        val hazeState = rememberHazeState()
        Box(
            Modifier
                .fillMaxSize()
                .hazeSource(hazeState),
        ) {
            GlowOrbs()
        }
        if (wide) {
            Row(
                Modifier
                    .fillMaxSize()
                    .padding(28.dp),
                horizontalArrangement = Arrangement.spacedBy(22.dp),
            ) {
                HomeRail(
                    hazeState = hazeState,
                    modifier = Modifier
                        .width(260.dp)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState()),
                    onHuman = { newMode = PlayMode.HumanVsHuman },
                    onKatago = { newMode = PlayMode.HumanVsAi },
                    onLoad = openSgf,
                    onEngine = { navigator.push(EngineSettingsScreen()) },
                    onSettings = { navigator.push(SettingsScreen()) },
                    onQuit = { appExit.exit() },
                )
                Column(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    RecentSection(hazeState, recents, homeVm, navigator)
                    ChangelogSection(hazeState)
                }
            }
        } else {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                PorcelainCard(hazeState) { BrandMark(compact = true) }
                HomeActionGrid(
                    hazeState = hazeState,
                    onHuman = { newMode = PlayMode.HumanVsHuman },
                    onKatago = { newMode = PlayMode.HumanVsAi },
                    onLoad = openSgf,
                    onEngine = { navigator.push(EngineSettingsScreen()) },
                    onSettings = { navigator.push(SettingsScreen()) },
                    onQuit = { appExit.exit() },
                )
                RecentSection(hazeState, recents, homeVm, navigator)
                ChangelogSection(hazeState)
            }
        }

        if (newMode != null) {
            val start: (com.acite.katahana.domain.GameConfig) -> Unit = { config ->
                homeVm.saveLastGame(config)
                newMode = null
                navigator.push(SessionScreen(config))
            }
            if (!landscape) {
                ModalBottomSheet(
                    onDismissRequest = { newMode = null },
                    sheetState = rememberBottomSheetState(
                        initialValue = SheetValue.Hidden,
                        enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded),
                    ),
                    containerColor = HanaColors.bgPanel,
                    shape = tokens.sheet,
                ) {
                    NewGameSheet(onStart = start, initial = lastGame, lockedMode = newMode)
                }
            } else {
                Dialog(onDismissRequest = { newMode = null }) {
                    Box(
                        Modifier
                            .width(420.dp)
                            .clip(tokens.card)
                            .background(HanaColors.bgPanel)
                            .padding(8.dp),
                    ) {
                        NewGameSheet(onStart = start, initial = lastGame, lockedMode = newMode)
                    }
                }
            }
        }
    }
}

@Composable
private fun GlowOrbs() {
    Canvas(Modifier.fillMaxSize()) {
        fun orb(center: Offset, radius: Float, color: Color, core: Float, mid: Float) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        color.copy(alpha = core),
                        color.copy(alpha = mid),
                        Color.Transparent,
                    ),
                    center = center,
                    radius = radius,
                ),
                radius = radius,
                center = center,
            )
        }
        orb(
            center = Offset(80.dp.toPx(), 100.dp.toPx()),
            radius = 252.dp.toPx(),
            color = HanaColors.accentPink,
            core = 0.55f,
            mid = 0.16f,
        )
        orb(
            center = Offset(size.width - 70.dp.toPx(), 170.dp.toPx()),
            radius = 240.dp.toPx(),
            color = HanaColors.accentBlue,
            core = 0.48f,
            mid = 0.14f,
        )
        orb(
            center = Offset(110.dp.toPx(), size.height - 90.dp.toPx()),
            radius = 224.dp.toPx(),
            color = HanaColors.accentLilac,
            core = 0.42f,
            mid = 0.12f,
        )
    }
}

@Composable
private fun HomeRail(
    hazeState: HazeState,
    onHuman: () -> Unit,
    onKatago: () -> Unit,
    onLoad: () -> Unit,
    onEngine: () -> Unit,
    onSettings: () -> Unit,
    onQuit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        PorcelainCard(hazeState) { BrandMark() }
        PorcelainCard(hazeState, title = Copy.play) {
            HomeNavTile(Copy.hvh, onHuman, modifier = Modifier.fillMaxWidth(), accent = HanaColors.accentPink, emphasized = true)
            HomeNavTile(Copy.humanVsKatago, onKatago, modifier = Modifier.fillMaxWidth(), accent = HanaColors.accentBlue, emphasized = true)
            HomeNavTile(Copy.loadGame, onLoad, modifier = Modifier.fillMaxWidth(), accent = HanaColors.accentLilac)
        }
        PorcelainCard(hazeState, title = Copy.app) {
            HomeNavTile(Copy.engine, onEngine, modifier = Modifier.fillMaxWidth(), accent = HanaColors.accentBlue)
            HomeNavTile(Copy.settings, onSettings, modifier = Modifier.fillMaxWidth(), accent = HanaColors.accentLilac)
            HomeNavTile(Copy.quit, onQuit, modifier = Modifier.fillMaxWidth(), accent = HanaColors.textDim)
        }
    }
}

@Composable
private fun HomeActionGrid(
    hazeState: HazeState,
    onHuman: () -> Unit,
    onKatago: () -> Unit,
    onLoad: () -> Unit,
    onEngine: () -> Unit,
    onSettings: () -> Unit,
    onQuit: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        PorcelainCard(hazeState, title = Copy.play) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                HomeNavTile(Copy.hvh, onHuman, Modifier.weight(1f), HanaColors.accentPink, emphasized = true)
                HomeNavTile(Copy.humanVsKatago, onKatago, Modifier.weight(1f), HanaColors.accentBlue, emphasized = true)
            }
            HomeNavTile(Copy.loadGame, onLoad, Modifier.fillMaxWidth(), HanaColors.accentLilac)
        }
        PorcelainCard(hazeState, title = Copy.app) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                HomeNavTile(Copy.engine, onEngine, Modifier.weight(1f), HanaColors.accentBlue)
                HomeNavTile(Copy.settings, onSettings, Modifier.weight(1f), HanaColors.accentLilac)
            }
            HomeNavTile(Copy.quit, onQuit, Modifier.fillMaxWidth(), HanaColors.textDim)
        }
    }
}

@Composable
private fun RecentSection(
    hazeState: HazeState,
    recents: List<RecentGame>,
    homeVm: HomeViewModel,
    navigator: cafe.adriel.voyager.navigator.Navigator,
) {
    PorcelainCard(hazeState, title = Copy.recent) {
        if (recents.isEmpty()) {
            Text(Copy.noRecent, color = HanaColors.textDim, fontSize = 14.sp)
        } else {
            recents.forEach { game ->
                RecentGameCard(
                    game = game,
                    now = epochMillis(),
                    onOpen = {
                        val loaded = homeVm.openRecent(game.id) ?: return@RecentGameCard
                        navigator.push(
                            SessionScreen(
                                config = loaded.config,
                                loadedTree = loaded.tree,
                                recentId = loaded.record.id,
                                recentTitle = loaded.record.title,
                            ),
                        )
                    },
                    onRemove = { homeVm.removeRecent(game.id) },
                )
            }
        }
    }
}

@Composable
private fun ChangelogSection(hazeState: dev.chrisbanes.haze.HazeState) {
    val entries = AppInfo.changelog.take(16)
    PorcelainCard(hazeState, title = Copy.whatsNew) {
        if (entries.isEmpty()) {
            Text(Copy.noChangelog, color = HanaColors.textDim, fontSize = 14.sp)
        } else {
            entries.forEach { entry ->
                ChangelogRow(entry)
            }
        }
    }
}

@Composable
private fun ChangelogRow(entry: ChangelogEntry) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            entry.gitTags.forEach { tag ->
                ChangelogChip(tag, HanaColors.accentPink)
            }
            entry.kind?.let { kind ->
                val accent = when (kind.lowercase()) {
                    "feat" -> HanaColors.accentPink
                    "fix" -> HanaColors.accentBlue
                    "doc" -> HanaColors.qualityMint
                    else -> HanaColors.accentLilac
                }
                ChangelogChip(kind, accent)
            }
            Text(
                entry.date,
                color = HanaColors.textDim,
                fontSize = 12.sp,
                modifier = Modifier.align(Alignment.CenterVertically),
            )
        }
        Text(entry.title, color = HanaColors.text, fontSize = 14.sp)
    }
}

@Composable
private fun ChangelogChip(label: String, accent: Color) {
    Box(
        Modifier
            .clip(hanaTokens.capsule)
            .background(accent.copy(alpha = 0.18f))
            .padding(horizontal = 8.dp, vertical = 2.dp),
    ) {
        Text(label, color = accent, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun RecentGameCard(
    game: RecentGame,
    now: Long,
    onOpen: () -> Unit,
    onRemove: () -> Unit,
) {
    val tokens = hanaTokens
    Row(
        Modifier
            .fillMaxWidth()
            .clip(tokens.panel)
            .background(Color.White.copy(alpha = 0.05f))
            .clickable(onClick = onOpen)
            .padding(start = 14.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(game.title, color = HanaColors.text, fontSize = 15.sp)
            Spacer(Modifier.height(4.dp))
            Text(game.modeLine(), color = HanaColors.textDim, fontSize = 12.sp)
            Text(
                formatSavedAt(game.savedAt, now),
                color = HanaColors.accentLilac,
                fontSize = 12.sp,
            )
        }
        QuietTextButton(Copy.remove, onClick = onRemove)
    }
}
