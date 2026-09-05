package com.acite.katahana.ui.session

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.acite.katahana.domain.EvalGraphMode
import com.acite.katahana.domain.EvalSample
import com.acite.katahana.domain.QualityStats
import com.acite.katahana.domain.SeatKind
import com.acite.katahana.domain.SessionSnapshot
import com.acite.katahana.domain.StoneColor
import com.acite.katahana.domain.TreeLayout
import com.acite.katahana.recents.seatName
import com.acite.katahana.recents.seatRankChip
import com.acite.katahana.engine.Candidate
import com.acite.katahana.engine.formatPv
import com.acite.katahana.engine.formatScoreLoss
import com.acite.katahana.ui.Copy
import com.acite.katahana.ui.components.CapsuleButton
import com.acite.katahana.ui.components.QuietTextButton
import com.acite.katahana.ui.components.RankCard
import com.acite.katahana.ui.theme.HanaColors
import com.acite.katahana.ui.theme.hanaAppearance
import com.acite.katahana.ui.theme.hanaTokens

@Composable
fun SidePanel(
    snapshot: SessionSnapshot,
    hasSelection: Boolean,
    onPass: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    engineOnline: Boolean = false,
    analyzing: Boolean = false,
    candidates: List<Candidate> = emptyList(),
    showCandidates: Boolean = true,
    onShowCandidatesChange: (Boolean) -> Unit = {},
    showQuality: Boolean = true,
    onShowQualityChange: (Boolean) -> Unit = {},
    aiThinking: Boolean = false,
    aiError: String? = null,
    onCycleVariation: (Int) -> Unit = {},
    onGoToNode: (String) -> Unit = {},
    tree: TreeLayout? = null,
    evalSamples: List<EvalSample> = emptyList(),
    evalGraphMode: EvalGraphMode = EvalGraphMode.Score,
    onEvalGraphMode: (EvalGraphMode) -> Unit = {},
    qualityStats: QualityStats = QualityStats(),
    dirty: Boolean = false,
    canSave: Boolean = false,
    onSave: () -> Unit = {},
    onSaveAs: () -> Unit = {},
    onExportSgf: () -> Unit = {},
    showConnections: Boolean = false,
    onShowConnectionsChange: (Boolean) -> Unit = {},
    showCoords: Boolean = true,
    onShowCoordsChange: (Boolean) -> Unit = {},
    showOwnership: Boolean = false,
    onShowOwnershipChange: (Boolean) -> Unit = {},
    showDeadStones: Boolean = false,
    onShowDeadStonesChange: (Boolean) -> Unit = {},
    reviewProgress: ReviewProgress? = null,
    onAnalyzeGame: () -> Unit = {},
    onBack: (() -> Unit)? = null,
    onSettings: (() -> Unit)? = null,
    onEditSeat: (StoneColor) -> Unit = {},
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (onBack != null || onSettings != null) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (onBack != null) QuietTextButton(Copy.back, onClick = onBack)
                if (onSettings != null) QuietTextButton(Copy.settings, onClick = onSettings)
            }
        }
        StatusCard(snapshot, onEditSeat = onEditSeat)
        if (snapshot.aiToPlay) {
            val aiLine = when {
                snapshot.ended -> null
                snapshot.reviewing -> null
                aiThinking -> Copy.aiThinking
                !engineOnline -> Copy.aiOffline
                aiError != null -> aiError
                else -> null
            }
            if (aiLine != null) {
                Text(aiLine, color = HanaColors.accentLilac, fontSize = 13.sp)
            }
        }
        if (snapshot.ended) {
            Text(Copy.twoPasses, color = HanaColors.accentPink, fontSize = 14.sp)
        }
        val humanTurn = snapshot.humanControls && !snapshot.ended
        PlayActionsBar(
            snapshot = snapshot,
            hasSelection = hasSelection,
            humanTurn = humanTurn,
            onPass = onPass,
            onUndo = onUndo,
            onRedo = onRedo,
            onConfirm = onConfirm,
        )
        if (snapshot.variationCount > 1) {
            VariationRow(snapshot.variationIndex, snapshot.variationCount, onCycleVariation)
        }
        if (tree != null) {
            GameTreeCard(
                layout = tree,
                reviewing = snapshot.reviewing,
                onGoToNode = onGoToNode,
                compact = false,
                modifier = Modifier.fillMaxWidth(),
            )
            EvalGraphCard(
                samples = evalSamples,
                currentMoveNumber = snapshot.moveNumber,
                mode = evalGraphMode,
                onMode = onEvalGraphMode,
                onSeek = onGoToNode,
                compact = false,
                modifier = Modifier.fillMaxWidth(),
            )
            QualityStatsCard(
                stats = qualityStats,
                compact = false,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        ToggleCard(
            label = Copy.showCandidates,
            checked = showCandidates,
            onChange = onShowCandidatesChange,
        )
        ToggleCard(
            label = Copy.showDots,
            checked = showQuality,
            onChange = onShowQualityChange,
        )
        ToggleCard(
            label = Copy.connections,
            checked = showConnections,
            onChange = onShowConnectionsChange,
        )
        ToggleCard(
            label = Copy.showCoords,
            checked = showCoords,
            onChange = onShowCoordsChange,
        )
        ToggleCard(
            label = Copy.showOwnership,
            checked = showOwnership,
            onChange = onShowOwnershipChange,
        )
        ToggleCard(
            label = Copy.deadStones,
            checked = showDeadStones,
            onChange = onShowDeadStonesChange,
        )
        val reviewing = reviewProgress?.running == true
        CapsuleButton(
            if (reviewing) Copy.cancelReview else Copy.analyzeGame,
            onAnalyzeGame,
            modifier = Modifier.fillMaxWidth(),
            enabled = engineOnline || reviewing,
        )
        reviewProgress?.let { progress ->
            Text(
                Copy.reviewProgress(progress.done, progress.total),
                color = if (progress.running) HanaColors.accentLilac else HanaColors.textDim,
                fontSize = 13.sp,
            )
        }
        CandidatesCard(
            engineOnline = engineOnline,
            analyzing = analyzing,
            candidates = candidates,
            showCandidates = showCandidates,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            CapsuleButton(
                Copy.save,
                onSave,
                modifier = Modifier.weight(1f),
                emphasized = dirty,
                enabled = canSave,
            )
            CapsuleButton(
                Copy.saveAs,
                onSaveAs,
                modifier = Modifier.weight(1f),
                enabled = canSave,
            )
        }
        CapsuleButton(Copy.exportSgf, onExportSgf, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun ToggleCard(
    label: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
) {
    val tokens = hanaTokens
    Row(
        Modifier
            .fillMaxWidth()
            .clip(tokens.panel)
            .background(HanaColors.bgCard)
            .padding(horizontal = 14.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = HanaColors.text, fontSize = 14.sp, modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = HanaColors.text,
                checkedTrackColor = HanaColors.accentPink,
                uncheckedThumbColor = HanaColors.textDim,
                uncheckedTrackColor = HanaColors.stroke,
            ),
        )
    }
}

@Composable
private fun VariationRow(index: Int, count: Int, onCycle: (Int) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            "${Copy.variation} ${index + 1}/$count",
            color = HanaColors.text,
            fontSize = 13.sp,
            modifier = Modifier.weight(1f),
        )
        CapsuleButton(Copy.prevVariation, { onCycle(-1) }, modifier = Modifier.weight(1f))
        CapsuleButton(Copy.nextVariation, { onCycle(1) }, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun CandidatesCard(
    engineOnline: Boolean,
    analyzing: Boolean,
    candidates: List<Candidate>,
    showCandidates: Boolean,
) {
    if (!showCandidates) return
    val tokens = hanaTokens
    Column(
        Modifier
            .fillMaxWidth()
            .clip(tokens.panel)
            .background(HanaColors.bgCard)
            .padding(14.dp),
    ) {
        Text(Copy.candidates, color = HanaColors.text, fontSize = 14.sp)
        Spacer(Modifier.height(8.dp))
        when {
            !engineOnline -> Text(Copy.engineOfflineHint, color = HanaColors.textDim, fontSize = 13.sp)
            candidates.isEmpty() -> Text(
                if (analyzing) Copy.engineAnalyzing else Copy.waitingForAnalysis,
                color = HanaColors.textDim,
                fontSize = 13.sp,
            )
            else -> candidates.forEachIndexed { index, candidate ->
                val loss = formatScoreLoss(candidate.pointsLost)
                Text(
                    "${candidate.gtp}  ·  $loss  ·  ${candidate.visits} ${Copy.visitsLabel}",
                    color = HanaColors.text,
                    fontSize = 13.sp,
                )
                val pv = formatPv(candidate.pv)
                if (pv.isNotEmpty()) {
                    Text(pv, color = HanaColors.textDim, fontSize = 12.sp)
                }
                if (index != candidates.lastIndex) Spacer(Modifier.height(6.dp))
            }
        }
    }
}

@Composable
fun StatusCard(
    snapshot: SessionSnapshot,
    onEditSeat: (StoneColor) -> Unit = {},
) {
    val tokens = hanaTokens
    Column(
        Modifier
            .fillMaxWidth()
            .clip(tokens.panel)
            .background(HanaColors.bgCard)
            .padding(14.dp),
    ) {
        val appearance = hanaAppearance
        SeatRow(
            label = Copy.black,
            name = seatName(snapshot.black),
            rank = seatRankChip(snapshot.black),
            godlike = snapshot.black.kind == SeatKind.Full,
            stone = appearance.first.fill,
            toPlay = snapshot.toPlay == StoneColor.Black,
            onClick = { onEditSeat(StoneColor.Black) },
        )
        Spacer(Modifier.height(8.dp))
        SeatRow(
            label = Copy.white,
            name = seatName(snapshot.white),
            rank = seatRankChip(snapshot.white),
            godlike = snapshot.white.kind == SeatKind.Full,
            stone = appearance.second.fill,
            toPlay = snapshot.toPlay == StoneColor.White,
            onClick = { onEditSeat(StoneColor.White) },
        )
        Spacer(Modifier.height(10.dp))
        val toPlay = if (snapshot.toPlay == StoneColor.Black) Copy.black else Copy.white
        Text(
            "${Copy.toPlay}: $toPlay",
            color = HanaColors.text,
            fontSize = 16.sp,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "${Copy.move} ${snapshot.moveNumber}  ·  ${snapshot.size}×${snapshot.size}  ·  komi ${snapshot.komi}",
            color = HanaColors.textDim,
            fontSize = 13.sp,
        )
        if (snapshot.reviewing) {
            Spacer(Modifier.height(6.dp))
            Text(Copy.review, color = HanaColors.accentPink, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("${Copy.captured}  ", color = HanaColors.textDim, fontSize = 13.sp)
            ColorDot(appearance.first.fill, 8.dp)
            Text(" ${snapshot.capturedByBlack}   ", color = HanaColors.textDim, fontSize = 13.sp)
            ColorDot(appearance.second.fill, 8.dp)
            Text(" ${snapshot.capturedByWhite}", color = HanaColors.textDim, fontSize = 13.sp)
        }
    }
}

@Composable
private fun SeatRow(
    label: String,
    name: String,
    rank: String?,
    godlike: Boolean,
    stone: Color,
    toPlay: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(hanaTokens.capsule)
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ColorDot(stone, if (toPlay) 12.dp else 10.dp)
        Spacer(Modifier.size(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                label,
                color = if (toPlay) HanaColors.text else HanaColors.textDim,
                fontSize = 13.sp,
                fontWeight = if (toPlay) FontWeight.SemiBold else FontWeight.Normal,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(name, color = HanaColors.accentLilac, fontSize = 14.sp)
                if (rank != null) RankCard(rank, emphasized = godlike)
            }
        }
        Text("›", color = HanaColors.textDim, fontSize = 18.sp)
    }
}

@Composable
private fun ColorDot(color: Color, size: Dp = 10.dp) {
    Box(
        Modifier
            .size(size)
            .clip(CircleShape)
            .background(color),
    )
}
