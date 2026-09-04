package com.acite.katahana.ui.session

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.acite.katahana.domain.GameConfig
import com.acite.katahana.domain.GameTree
import com.acite.katahana.engine.EnginePhase
import com.acite.katahana.engine.EngineStatus
import com.acite.katahana.sgf.LocalSgfFiles
import com.acite.katahana.ui.Copy
import com.acite.katahana.ui.board.BoardCanvas
import com.acite.katahana.ui.components.EngineDot
import com.acite.katahana.ui.components.QuietTextButton
import com.acite.katahana.ui.components.WinrateTrack
import com.acite.katahana.ui.settings.SettingsScreen
import com.acite.katahana.ui.theme.HanaColors
import com.acite.katahana.ui.theme.hanaTokens
import dev.zacsweers.metrox.viewmodel.assistedMetroViewModel
import kotlin.random.Random
import kotlinx.coroutines.launch

class SessionScreen(
    private val config: GameConfig,
    private val loadedTree: GameTree? = null,
    private val instanceId: String = Random.nextLong().toULong().toString(16),
) : Screen {
    override val key: ScreenKey = "session-$instanceId"

    @Composable
    override fun Content() {
        val vm = assistedMetroViewModel<SessionViewModel, SessionViewModel.Factory>(
            key = key,
        ) {
            create(config, loadedTree)
        }
        SessionRoute(vm)
    }
}

@Composable
private fun SessionRoute(vm: SessionViewModel) {
    val ui by vm.state.collectAsState()
    val coords by vm.showCoords.collectAsState()
    val showCandidates by vm.showCandidates.collectAsState()
    val showQuality by vm.showQuality.collectAsState()
    val showConnections by vm.showConnections.collectAsState()
    val navigator = LocalNavigator.currentOrThrow
    val snapshot = ui.snapshot
    val boardCandidates = if (showCandidates) ui.candidates else emptyList()
    val boardQualities = if (showQuality) ui.qualities else emptyList()
    val sgfFiles = LocalSgfFiles.current
    val scope = rememberCoroutineScope()
    var drawerOpen by remember { mutableStateOf(false) }
    val saveSgf: () -> Unit = {
        scope.launch {
            sgfFiles.save(vm.sgfFileName(), vm.sgfText())
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(HanaColors.bgApp)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        SessionTopBar(
            status = ui.engineStatus,
            blackWinrate = ui.blackWinrate?.toFloat(),
            reviewing = snapshot.reviewing,
            onMenu = { drawerOpen = true },
        )
        Box(Modifier.weight(1f).fillMaxWidth()) {
            BoxWithConstraints(Modifier.fillMaxSize().padding(8.dp)) {
                val boardSide = minOf(maxWidth, maxHeight).coerceAtLeast(120.dp)
                val leftover = maxWidth - boardSide
                val showSideTree = leftover >= 168.dp
                val board: @Composable () -> Unit = {
                    BoardCanvas(
                        snapshot = snapshot,
                        showCoords = coords,
                        preview = ui.preview,
                        onHover = vm::onHover,
                        onActivate = vm::onActivate,
                        modifier = Modifier.size(boardSide),
                        candidates = boardCandidates,
                        qualities = boardQualities,
                        showConnections = showConnections,
                    )
                }
                if (showSideTree) {
                    Row(
                        Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        GameTreeCard(
                            layout = ui.tree,
                            reviewing = snapshot.reviewing,
                            onGoToNode = vm::goToNode,
                            compact = true,
                            modifier = Modifier
                                .width(leftover.coerceAtMost(280.dp))
                                .fillMaxHeight()
                                .padding(end = 8.dp),
                        )
                        Box(
                            Modifier.weight(1f).fillMaxHeight(),
                            contentAlignment = Alignment.Center,
                        ) { board() }
                    }
                } else {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { board() }
                }
            }
            SessionDrawerOverlay(
                open = drawerOpen,
                onOpen = { drawerOpen = true },
                onClose = { drawerOpen = false },
            ) {
                SidePanel(
                    snapshot = snapshot,
                    hasSelection = ui.selected != null,
                    engineOnline = ui.engineStatus.online,
                    analyzing = ui.analyzing,
                    candidates = ui.candidates,
                    showCandidates = showCandidates,
                    onShowCandidatesChange = vm::setShowCandidates,
                    showQuality = showQuality,
                    onShowQualityChange = vm::setShowQuality,
                    showConnections = showConnections,
                    onShowConnectionsChange = vm::setShowConnections,
                    showCoords = coords,
                    onShowCoordsChange = vm::setShowCoords,
                    aiThinking = ui.aiThinking,
                    aiError = ui.aiError,
                    onPass = vm::pass,
                    onUndo = vm::undo,
                    onRedo = vm::redo,
                    onConfirm = vm::confirmSelected,
                    onCycleVariation = vm::cycleVariation,
                    onGoToNode = vm::goToNode,
                    tree = ui.tree,
                    onSaveSgf = saveSgf,
                    onBack = {
                        vm.leave()
                        drawerOpen = false
                        navigator.pop()
                    },
                    onSettings = {
                        drawerOpen = false
                        navigator.push(SettingsScreen())
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

@Composable
internal fun SessionTopBar(
    status: EngineStatus,
    blackWinrate: Float?,
    onMenu: () -> Unit,
    reviewing: Boolean = false,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(44.dp)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        EngineDot(online = status.online)
        Spacer(Modifier.width(10.dp))
        WinrateTrack(
            Modifier.weight(1f).padding(end = 4.dp),
            blackWinrate = blackWinrate,
            enabled = status.online,
        )
        if (reviewing) {
            Box(
                Modifier
                    .clip(hanaTokens.capsule)
                    .background(HanaColors.accentPink.copy(alpha = 0.18f))
                    .clickable(onClick = onMenu)
                    .padding(horizontal = 10.dp, vertical = 5.dp),
            ) {
                Text(
                    Copy.review,
                    color = HanaColors.accentPink,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Spacer(Modifier.width(4.dp))
        }
        QuietTextButton(Copy.menu, onClick = onMenu)
    }
}

@Composable
private fun BoxScope.SessionDrawerOverlay(
    open: Boolean,
    onOpen: () -> Unit,
    onClose: () -> Unit,
    content: @Composable () -> Unit,
) {
    if (!open) {
        DrawerHandle(
            modifier = Modifier.align(Alignment.CenterEnd),
            onClick = onOpen,
        )
    }
    androidx.compose.animation.AnimatedVisibility(
        visible = open,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.45f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClose,
                ),
        )
    }
    androidx.compose.animation.AnimatedVisibility(
        visible = open,
        modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight().width(300.dp),
        enter = slideInHorizontally { it },
        exit = slideOutHorizontally { it },
    ) {
        content()
    }
}

@Composable
private fun DrawerHandle(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val tokens = hanaTokens
    Box(
        modifier
            .padding(end = 4.dp)
            .width(18.dp)
            .height(72.dp)
            .clip(RoundedCornerShape(topStart = 14.dp, bottomStart = 14.dp))
            .background(HanaColors.bgPanel)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .width(3.dp)
                .height(28.dp)
                .clip(tokens.capsule)
                .background(HanaColors.accentPink.copy(alpha = 0.85f)),
        )
    }
}

internal fun engineStatusLabel(status: EngineStatus): String = when (status.phase) {
    EnginePhase.Disconnected -> Copy.engineOffline
    EnginePhase.Connecting -> Copy.engineConnecting
    EnginePhase.Ready -> Copy.engineOnline
    EnginePhase.Analyzing -> Copy.engineAnalyzing
    EnginePhase.Error -> status.detail.ifBlank { Copy.engineError }
}
