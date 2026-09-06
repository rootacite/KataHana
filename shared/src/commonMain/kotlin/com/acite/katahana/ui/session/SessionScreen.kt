package com.acite.katahana.ui.session

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.acite.katahana.domain.GameConfig
import com.acite.katahana.domain.GameTree
import com.acite.katahana.domain.SessionSnapshot
import com.acite.katahana.domain.StoneColor
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
import com.acite.katahana.ui.theme.hanaColors
import com.acite.katahana.ui.theme.hanaTokens
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import dev.zacsweers.metrox.viewmodel.assistedMetroViewModel
import kotlin.random.Random
import kotlinx.coroutines.launch

class SessionScreen(
    private val config: GameConfig,
    loadedTree: GameTree? = null,
    private val recentId: String? = null,
    private val recentTitle: String? = null,
    private val instanceId: String = Random.nextLong().toULong().toString(16),
) : Screen {
    override val key: ScreenKey = "session-$instanceId"

    init {
        if (loadedTree != null) SessionTrees.put(instanceId, loadedTree)
    }

    @Composable
    override fun Content() {
        val tree = remember(instanceId) { SessionTrees.take(instanceId) }
        val vm = assistedMetroViewModel<SessionViewModel, SessionViewModel.Factory>(
            key = key,
        ) {
            create(config, tree, recentId, recentTitle)
        }
        SessionRoute(vm)
    }
}

/** GameTree is not Java-serializable; keep it off the Screen so Android stop/save cannot crash. */
private object SessionTrees {
    private val pending = mutableMapOf<String, GameTree>()

    fun put(id: String, tree: GameTree) {
        pending[id] = tree
    }

    fun take(id: String): GameTree? = pending.remove(id)
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
    val analysisLayoutMode by vm.analysisLayoutMode.collectAsState()
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
    var editColor by remember { mutableStateOf<StoneColor?>(null) }
    LaunchedEffect(drawerOpen, editColor) {
        vm.setPaused(drawerOpen || editColor != null)
    }
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
        editColor = null
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
            .background(hanaColors.bgApp),
    ) {
        val landscape = getPlatform().isMobile && maxWidth > maxHeight
        val chromePad = if (landscape) 4.dp else 8.dp
        val humanTurn = snapshot.humanControls && !snapshot.ended
        val hazeState = rememberHazeState()
        val overlayHaze = rememberHazeState()
        Column(
            Modifier
                .fillMaxSize()
                .padding(top = chromePad)
                .hazeSource(overlayHaze),
        ) {
            if (!landscape) {
                SessionTopBar(
                    status = ui.engineStatus,
                    blackWinrate = ui.blackWinrate?.toFloat(),
                    reviewing = snapshot.reviewing,
                    compact = false,
                    onMenu = { drawerOpen = true },
                    snapshot = snapshot,
                    hasSelection = ui.selected != null,
                    humanTurn = humanTurn,
                    onPass = vm::pass,
                    onUndo = vm::undo,
                    onRedo = vm::redo,
                    onConfirm = vm::confirmSelected,
                    forecastActive = ui.forecast != null,
                    onEndForecast = vm::endForecast,
                    onExitReview = vm::exitReview,
                )
            }
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
                        forecastActive = ui.forecast != null,
                        onEndForecast = vm::endForecast,
                        onExitReview = vm::exitReview,
                        onCycleVariation = vm::cycleVariation,
                        onGoToNode = vm::goToNode,
                        tree = ui.tree,
                        evalSamples = ui.evalSamples,
                        evalGraphMode = ui.evalGraphMode,
                        onEvalGraphMode = vm::setEvalGraphMode,
                        qualityStats = ui.qualityStats,
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
                        onEditSeat = { editColor = it },
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
                    val layout = computeSessionLayout(
                        maxWidth = maxWidth,
                        maxHeight = maxHeight,
                        mobile = getPlatform().isMobile,
                        chromePad = chromePad,
                        analysisMode = analysisLayoutMode,
                    )
                    val board: @Composable () -> Unit = {
                        BoardCanvas(
                            snapshot = snapshot,
                            showCoords = coords,
                            preview = ui.preview,
                            onHover = vm::onHover,
                            onActivate = vm::onActivate,
                            onAim = vm::onAim,
                            onForecast = vm::onForecast,
                            modifier = Modifier.size(layout.boardSide),
                            candidates = boardCandidates,
                            qualities = boardQualities,
                            showConnections = showConnections,
                            ownership = ui.ownership,
                            showOwnership = showOwnership || ui.forecastRevealed > 0,
                            ownershipStyle = ownershipStyle,
                            deadPoints = if (showDeadStones) ui.deadPoints else emptySet(),
                            forecast = ui.forecast,
                            forecastRevealed = ui.forecastRevealed,
                        )
                    }
                    when {
                        layout.showTopTree -> {
                            Column(Modifier.fillMaxSize()) {
                                SessionTreeRow(
                                    layout = ui.tree,
                                    reviewing = snapshot.reviewing,
                                    onGoToNode = vm::goToNode,
                                    samples = ui.evalSamples,
                                    currentMoveNumber = snapshot.moveNumber,
                                    graphMode = ui.evalGraphMode,
                                    onGraphMode = vm::setEvalGraphMode,
                                    stats = ui.qualityStats,
                                    tabbed = layout.tabbedAnalysis,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(layout.treeH),
                                )
                                Spacer(Modifier.height(chromePad))
                                Box(
                                    Modifier
                                        .fillMaxWidth()
                                        .weight(1f),
                                    contentAlignment = Alignment.BottomCenter,
                                ) { board() }
                            }
                        }
                        layout.showSideTree || layout.showRail -> {
                            Row(
                                Modifier.fillMaxSize(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                if (layout.showSideTree) {
                                    SessionTreeColumn(
                                        layout = ui.tree,
                                        reviewing = snapshot.reviewing,
                                        onGoToNode = vm::goToNode,
                                        samples = ui.evalSamples,
                                        currentMoveNumber = snapshot.moveNumber,
                                        graphMode = ui.evalGraphMode,
                                        onGraphMode = vm::setEvalGraphMode,
                                        stats = ui.qualityStats,
                                        tabbed = layout.tabbedAnalysis,
                                        modifier = Modifier
                                            .width(layout.treeW)
                                            .fillMaxHeight(),
                                    )
                                    Spacer(Modifier.width(chromePad))
                                }
                                if (layout.showRail) {
                                    SessionRail(
                                        status = ui.engineStatus,
                                        blackWinrate = ui.blackWinrate?.toFloat(),
                                        reviewing = snapshot.reviewing,
                                        onMenu = { drawerOpen = true },
                                        snapshot = snapshot,
                                        hasSelection = ui.selected != null,
                                        humanTurn = humanTurn,
                                        onPass = vm::pass,
                                        onUndo = vm::undo,
                                        onRedo = vm::redo,
                                        onConfirm = vm::confirmSelected,
                                        forecastActive = ui.forecast != null,
                                        onEndForecast = vm::endForecast,
                                        onExitReview = vm::exitReview,
                                        modifier = Modifier
                                            .width(SessionRailWidth)
                                            .fillMaxHeight(),
                                    )
                                    Spacer(Modifier.width(chromePad))
                                }
                                Box(
                                    Modifier.weight(1f).fillMaxHeight(),
                                    contentAlignment = Alignment.Center,
                                ) { board() }
                            }
                        }
                        else -> {
                            Box(
                                Modifier.fillMaxSize(),
                                contentAlignment = if (maxHeight >= maxWidth) {
                                    Alignment.BottomCenter
                                } else {
                                    Alignment.Center
                                },
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
                hazeState = overlayHaze,
            )
        }
        if (nameAsk) {
            SaveNameDialog(
                initial = nameDraft,
                onConfirm = { performSaveAs(it, nameThenLeave) },
                onCancel = { nameAsk = false },
                hazeState = overlayHaze,
            )
        }
        editColor?.let { color ->
            SeatDialog(
                color = color,
                seat = snapshot.seat(color),
                onChange = { vm.setSeat(color, it) },
                onDismiss = { editColor = null },
                hazeState = overlayHaze,
            )
        }
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
    forecastActive: Boolean = false,
    onEndForecast: () -> Unit = {},
    onExitReview: () -> Unit = {},
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
        MenuChip(compact = compact, onClick = onMenu)
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
            forecastActive = forecastActive,
            onEndForecast = onEndForecast,
            onExitReview = onExitReview,
        )
        if (reviewing) {
            ReviewChip(compact = compact, onClick = onExitReview)
        }
    }
}

@Composable
private fun SessionRail(
    status: EngineStatus,
    blackWinrate: Float?,
    reviewing: Boolean,
    onMenu: () -> Unit,
    snapshot: SessionSnapshot,
    hasSelection: Boolean,
    humanTurn: Boolean,
    onPass: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onConfirm: () -> Unit,
    forecastActive: Boolean = false,
    onEndForecast: () -> Unit = {},
    onExitReview: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        EngineDot(online = status.online)
        MenuChip(compact = true, onClick = onMenu)
        if (reviewing) {
            ReviewChip(compact = true, onClick = onExitReview)
        }
        WinrateTrack(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            blackWinrate = blackWinrate,
            enabled = status.online,
            vertical = true,
        )
        PlayIconCluster(
            snapshot = snapshot,
            hasSelection = hasSelection,
            humanTurn = humanTurn,
            onPass = onPass,
            onUndo = onUndo,
            onRedo = onRedo,
            onConfirm = onConfirm,
            forecastActive = forecastActive,
            onEndForecast = onEndForecast,
            onExitReview = onExitReview,
            vertical = true,
        )
    }
}

@Composable
private fun MenuChip(compact: Boolean, onClick: () -> Unit) {
    if (compact) {
        Text(
            Copy.menu,
            color = hanaColors.accentLilac,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            maxLines = 1,
            modifier = Modifier
                .clip(hanaTokens.capsule)
                .clickable(onClick = onClick)
                .padding(horizontal = 6.dp, vertical = 4.dp),
        )
    } else {
        QuietTextButton(Copy.menu, onClick = onClick)
    }
}

@Composable
private fun ReviewChip(compact: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(hanaTokens.capsule)
            .background(hanaColors.accentPink.copy(alpha = 0.18f))
            .clickable(onClick = onClick)
            .padding(
                horizontal = if (compact) 4.dp else 10.dp,
                vertical = if (compact) 2.dp else 5.dp,
            ),
    ) {
        Text(
            Copy.exitReview,
            color = hanaColors.accentPink,
            fontSize = if (compact) 11.sp else 12.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            maxLines = 1,
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
