package com.acite.katahana.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.acite.katahana.epochMillis
import com.acite.katahana.recents.RecentGame
import com.acite.katahana.recents.formatSavedAt
import com.acite.katahana.sgf.LocalSgfFiles
import com.acite.katahana.sgf.parseSgf
import com.acite.katahana.ui.Copy
import com.acite.katahana.ui.components.CapsuleButton
import com.acite.katahana.ui.components.QuietTextButton
import com.acite.katahana.ui.engine.EngineSettingsScreen
import com.acite.katahana.ui.session.SessionScreen
import com.acite.katahana.ui.settings.SettingsScreen
import com.acite.katahana.ui.theme.HanaColors
import com.acite.katahana.ui.theme.hanaTokens
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
    var showNew by remember { mutableStateOf(false) }
    val tokens = hanaTokens
    val sgfFiles = LocalSgfFiles.current
    val scope = rememberCoroutineScope()
    val homeVm = metroViewModel<HomeViewModel>()
    val lastGame by homeVm.lastGame.collectAsState()
    val recents by homeVm.recentGames.collectAsState()

    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .background(HanaColors.bgApp),
    ) {
        val landscape = maxWidth > maxHeight
        Box(Modifier.fillMaxSize()) {
            Box(
                Modifier
                    .size(220.dp)
                    .offset((-48).dp, (-24).dp)
                    .clip(CircleShape)
                    .background(HanaColors.accentPink.copy(alpha = 0.16f)),
            )
            Box(
                Modifier
                    .size(180.dp)
                    .align(Alignment.TopEnd)
                    .offset(x = 36.dp, y = 40.dp)
                    .clip(CircleShape)
                    .background(HanaColors.accentBlue.copy(alpha = 0.14f)),
            )
            Box(
                Modifier
                    .size(120.dp)
                    .align(Alignment.BottomStart)
                    .offset(x = 24.dp, y = (-80).dp)
                    .clip(CircleShape)
                    .background(HanaColors.accentLilac.copy(alpha = 0.12f)),
            )
            Column(
                Modifier
                    .widthIn(max = 520.dp)
                    .align(Alignment.Center)
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
            ) {
                Text(
                    Copy.appName,
                    color = HanaColors.text,
                    fontSize = 44.sp,
                    lineHeight = 48.sp,
                )
                Text(
                    Copy.tagline,
                    color = HanaColors.accentLilac,
                    fontSize = 16.sp,
                )
                Spacer(Modifier.height(28.dp))
                CapsuleButton(
                    Copy.newGame,
                    onClick = { showNew = true },
                    modifier = Modifier.fillMaxWidth(),
                    emphasized = true,
                )
                Spacer(Modifier.height(10.dp))
                CapsuleButton(
                    Copy.openRecord,
                    onClick = {
                        scope.launch {
                            val text = sgfFiles.open() ?: return@launch
                            val game = runCatching { parseSgf(text) }.getOrNull() ?: return@launch
                            navigator.push(SessionScreen(game.config, game.tree))
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(28.dp))
                Text(Copy.recent, color = HanaColors.text, fontSize = 16.sp)
                Spacer(Modifier.height(10.dp))
                if (recents.isEmpty()) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .clip(tokens.card)
                            .background(HanaColors.bgCard)
                            .padding(18.dp),
                    ) {
                        Text(Copy.noRecent, color = HanaColors.textDim, fontSize = 14.sp)
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                Spacer(Modifier.height(20.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    QuietTextButton(Copy.engine) { navigator.push(EngineSettingsScreen()) }
                    QuietTextButton(Copy.settings) { navigator.push(SettingsScreen()) }
                }
            }
        }

        if (showNew) {
            val start: (com.acite.katahana.domain.GameConfig) -> Unit = { config ->
                homeVm.saveLastGame(config)
                showNew = false
                navigator.push(SessionScreen(config))
            }
            if (!landscape) {
                ModalBottomSheet(
                    onDismissRequest = { showNew = false },
                    sheetState = rememberBottomSheetState(
                        initialValue = SheetValue.Hidden,
                        enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded),
                    ),
                    containerColor = HanaColors.bgPanel,
                    shape = tokens.sheet,
                ) {
                    NewGameSheet(onStart = start, initial = lastGame)
                }
            } else {
                Dialog(onDismissRequest = { showNew = false }) {
                    Box(
                        Modifier
                            .width(420.dp)
                            .clip(tokens.card)
                            .background(HanaColors.bgPanel)
                            .padding(8.dp),
                    ) {
                        NewGameSheet(onStart = start, initial = lastGame)
                    }
                }
            }
        }
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
            .clip(tokens.card)
            .background(HanaColors.bgCard)
            .clickable(onClick = onOpen)
            .padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
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
