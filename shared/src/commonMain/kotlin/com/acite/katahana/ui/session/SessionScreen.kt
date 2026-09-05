package com.acite.katahana.ui.session

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.acite.katahana.domain.GameConfig
import com.acite.katahana.domain.GameTree
import com.acite.katahana.domain.SessionSnapshot
import com.acite.katahana.engine.EnginePhase
import com.acite.katahana.getPlatform
import com.acite.katahana.engine.EngineStatus
import com.acite.katahana.sgf.LocalSgfFiles
import com.acite.katahana.ui.Copy
import com.acite.katahana.ui.board.BoardCanvas
import com.acite.katahana.ui.components.EngineDot
import com.acite.katahana.ui.components.LeaveGameDialog
import com.acite.katahana.ui.components.QuietTextButton
import com.acite.katahana.ui.components.SaveNameDialog
import com.acite.katahana.ui.components.WinrateTrack
import com.acite.katahana.ui.navigation.HanaBackHandler
import com.acite.katahana.ui.settings.SettingsScreen
import com.acite.katahana.ui.theme.HanaColors
import com.acite.katahana.ui.theme.hanaTokens
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import dev.zacsweers.metrox.viewmodel.assistedMetroViewModel
import kotlin.random.Random
import kotlinx.coroutines.launch

class SessionScreen(
    private val config: GameConfig,
    private val loadedTree: GameTree? = null,
    private val recentId: String? = null,
    private val recentTitle: String? = null,
    private val instanceId: String = Random.nextLong().toULong().toString(16),
) : Screen {
    override val key: ScreenKey = "session-$instanceId"

    @Composable
    override fun Content() {
        val vm = assistedMetroViewModel<SessionViewModel, SessionViewModel.Factory>(
            key = key,
        ) {
            create(config, loadedTree, recentId, recentTitle)
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
    val showOwnership by vm.showOwnership.collectAsState()
    val showDeadStones by vm.showDeadStones.collectAsState()
    val ownershipStyle by vm.ownershipStyle.collectAsState()
    val drawerAcrylic by vm.drawerAcrylic.collectAsState()
    val navigator = LocalNavigator.currentOrThrow
    val snapshot = ui.snapshot
    val boardCandidates = if (showCandidates) ui.candidates else emptyList()
    val boardQualities = if (showQuality) ui.qualities else emptyList()
    val sgfFiles = LocalSgfFiles.current
    val scope = rememberCoroutineScope()
    var drawerOpen by remember { mutableStateOf(false) }
    var leaveAsk by remember { mutableStateOf(false) }
    var nameAsk by remember { mutableStateOf(false) }
    var nameThenLeave by remember { mutableStateOf(false) }
    var nameDraft by remember { mutableStateOf("") }
    val exportSgf: () -> Unit = {
        scope.launch {
            sgfFiles.save(vm.sgfFileName(), vm.sgfText())
        }
    }
    val actuallyLeave: () -> Unit = {
        vm.leave()
        drawerOpen = false
        leaveAsk = false
        nameAsk = false
        navigator.pop()
    }
    val requestLeave: () -> Unit = {
        drawerOpen = false
        if (!ui.dirty) actuallyLeave() else leaveAsk = true
    }
    val performSave: (Boolean) -> Unit = { thenLeave ->
        scope.launch {
            when (vm.save()) {
                SaveOutcome.NeedsName -> {
                    nameDraft = vm.suggestedTitle()
                    nameThenLeave = thenLeave
                    leaveAsk = false
                    nameAsk = true
                }
                SaveOutcome.Saved -> if (thenLeave) actuallyLeave()
                SaveOutcome.Failed -> Unit
            }
        }
    }
    val performSaveAs: (String, Boolean) -> Unit = { name, thenLeave ->
        scope.launch {
            if (vm.saveAs(name) == SaveOutcome.Saved && thenLeave) actuallyLeave()
            nameAsk = false
        }
    }
    HanaBackHandler { requestLeave() }

    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .background(HanaColors.bgApp),
    ) {
        val landscape = getPlatform().isMobile && maxWidth > maxHeight
        val chromePad = if (landscape) 4.dp else 8.dp
        val humanTurn = !ui.aiThinking && !snapshot.ended && !snapshot.aiToPlay
        val hazeState = rememberHazeState()
        Column(Modifier.fillMaxSize().padding(top = chromePad)) {
            SessionTopBar(
                status = ui.engineStatus,
                blackWinrate = ui.blackWinrate?.toFloat(),
                reviewing = snapshot.reviewing,
                compact = landscape,
                onMenu = { drawerOpen = true },
                snapshot = snapshot,
                hasSelection = ui.selected != null,
                humanTurn = humanTurn,
                onPass = vm::pass,
                onUndo = vm::undo,
                onRedo = vm::redo,
                onConfirm = vm::confirmSelected,
            )
            HanaDrawer(
                open = drawerOpen,
                onOpenChange = { drawerOpen = it },
                acrylic = drawerAcrylic,
                hazeState = hazeState,
                drawerContent = {
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
                        showOwnership = showOwnership,
                        onShowOwnershipChange = vm::setShowOwnership,
                        showDeadStones = showDeadStones,
                        onShowDeadStonesChange = vm::setShowDeadStones,
                        reviewProgress = ui.reviewProgress,
                        onAnalyzeGame = vm::analyzeGame,
                        aiThinking = ui.aiThinking,
                        aiError = ui.aiError,
                        onPass = vm::pass,
                        onUndo = vm::undo,
                        onRedo = vm::redo,
                        onConfirm = vm::confirmSelected,
                        onCycleVariation = vm::cycleVariation,
                        onGoToNode = vm::goToNode,
                        tree = ui.tree,
                        dirty = ui.dirty,
                        canSave = ui.canSave,
                        onSave = { performSave(false) },
                        onSaveAs = {
                            nameDraft = vm.suggestedTitle()
                            nameThenLeave = false
                            nameAsk = true
                        },
                        onExportSgf = exportSgf,
                        onBack = requestLeave,
                        onSettings = {
                            drawerOpen = false
                            navigator.push(SettingsScreen())
                        },
                        modifier = Modifier.fillMaxSize(),
                    )
                },
            ) {
                BoxWithConstraints(
                    Modifier
                        .fillMaxSize()
                        .hazeSource(hazeState)
                        .padding(chromePad),
                ) {
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
                            ownership = ui.ownership,
                            showOwnership = showOwnership,
                            ownershipStyle = ownershipStyle,
                            deadPoints = if (showDeadStones) ui.deadPoints else emptySet(),
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
                                    .padding(end = chromePad),
                            )
                            Box(
                                Modifier.weight(1f).fillMaxHeight(),
                                contentAlignment = Alignment.Center,
                            ) { board() }
                        }
                    } else {
                        Box(
                            Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) { board() }
                    }
                }
            }
        }
    }
    if (leaveAsk) {
        LeaveGameDialog(
            onSave = { performSave(true) },
            onDiscard = actuallyLeave,
            onCancel = { leaveAsk = false },
        )
    }
    if (nameAsk) {
        SaveNameDialog(
            initial = nameDraft,
            onConfirm = { performSaveAs(it, nameThenLeave) },
            onCancel = { nameAsk = false },
        )
    }
}

@Composable
internal fun SessionTopBar(
    status: EngineStatus,
    blackWinrate: Float?,
    onMenu: () -> Unit,
    snapshot: SessionSnapshot,
    hasSelection: Boolean,
    humanTurn: Boolean,
    onPass: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onConfirm: () -> Unit,
    reviewing: Boolean = false,
    compact: Boolean = false,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(44.dp)
            .padding(horizontal = if (compact) 4.dp else 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        EngineDot(online = status.online)
        Spacer(Modifier.width(if (compact) 6.dp else 8.dp))
        if (compact) {
            Text(
                Copy.menu,
                color = HanaColors.accentLilac,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .clip(hanaTokens.capsule)
                    .clickable(onClick = onMenu)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            )
        } else {
            QuietTextButton(Copy.menu, onClick = onMenu)
        }
        Spacer(Modifier.width(if (compact) 6.dp else 8.dp))
        WinrateTrack(
            Modifier.weight(1f).padding(end = 4.dp),
            blackWinrate = blackWinrate,
            enabled = status.online,
        )
        PlayIconCluster(
            snapshot = snapshot,
            hasSelection = hasSelection,
            humanTurn = humanTurn,
            onPass = onPass,
            onUndo = onUndo,
            onRedo = onRedo,
            onConfirm = onConfirm,
        )
        if (reviewing) {
            Box(
                Modifier
                    .clip(hanaTokens.capsule)
                    .background(HanaColors.accentPink.copy(alpha = 0.18f))
                    .clickable(onClick = onMenu)
                    .padding(
                        horizontal = if (compact) 8.dp else 10.dp,
                        vertical = if (compact) 2.dp else 5.dp,
                    ),
            ) {
                Text(
                    Copy.review,
                    color = HanaColors.accentPink,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

internal fun engineStatusLabel(status: EngineStatus): String = when (status.phase) {
    EnginePhase.Disconnected -> Copy.engineOffline
    EnginePhase.Connecting -> Copy.engineConnecting
    EnginePhase.Ready -> Copy.engineOnline
    EnginePhase.Analyzing -> Copy.engineAnalyzing
    EnginePhase.Error -> status.detail.ifBlank { Copy.engineError }
}
