package com.acite.katahana.ui.engine

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.acite.katahana.engine.BenchUiState
import com.acite.katahana.engine.BenchmarkReport
import com.acite.katahana.engine.BenchmarkVerdict
import com.acite.katahana.engine.formatDuration
import com.acite.katahana.engine.formatRate
import com.acite.katahana.ui.Copy
import com.acite.katahana.ui.components.CapsuleButton
import com.acite.katahana.ui.components.HanaField
import com.acite.katahana.ui.components.HanaSection
import com.acite.katahana.ui.components.ScreenHeader
import com.acite.katahana.ui.session.engineStatusLabel
import com.acite.katahana.ui.settings.SettingsViewModel
import com.acite.katahana.ui.theme.HanaColors
import dev.zacsweers.metrox.viewmodel.metroViewModel

class EngineSettingsScreen : Screen {
    @Composable
    override fun Content() {
        EngineSettingsRoute(metroViewModel())
    }
}

@Composable
private fun EngineSettingsRoute(vm: SettingsViewModel) {
    val navigator = LocalNavigator.currentOrThrow
    val name by vm.engineName.collectAsState()
    val url by vm.engineUrl.collectAsState()
    val token by vm.engineToken.collectAsState()
    val play by vm.playVisits.collectAsState()
    val review by vm.reviewVisits.collectAsState()
    val engineStatus by vm.engineStatus.collectAsState()
    val testBusy by vm.testBusy.collectAsState()
    val testMessage by vm.testMessage.collectAsState()
    val benchState by vm.benchState.collectAsState()
    val benchBusy = benchState is BenchUiState.Running
    Column(
        Modifier
            .fillMaxSize()
            .background(HanaColors.bgApp)
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp),
    ) {
        ScreenHeader(Copy.engine, onBack = { navigator.pop() })
        HanaSection(
            Copy.engine,
            hint = if (engineStatus.online) null else Copy.engineOfflineHint,
        ) {
            HanaField(Copy.engineName, name, vm::setEngineName)
            HanaField(Copy.url, url, vm::setEngineUrl, placeholder = "ws://127.0.0.1:2080")
            HanaField(Copy.token, token, vm::setEngineToken)
            HanaField(
                Copy.playVisits,
                play.toString(),
                onChange = { it.toIntOrNull()?.let(vm::setPlayVisits) },
                keyboard = KeyboardType.Number,
            )
            HanaField(
                Copy.reviewVisits,
                review.toString(),
                onChange = { it.toIntOrNull()?.let(vm::setReviewVisits) },
                keyboard = KeyboardType.Number,
            )
            CapsuleButton(
                if (testBusy) Copy.testingConnection else Copy.testConnection,
                onClick = vm::testConnection,
                modifier = Modifier.fillMaxWidth(),
                enabled = !testBusy && !benchBusy,
            )
            Text(
                engineStatusLabel(engineStatus),
                color = HanaColors.textDim,
                fontSize = 13.sp,
            )
            if (testMessage != null) {
                Text(testMessage.orEmpty(), color = HanaColors.accentLilac, fontSize = 13.sp)
            }
        }
        HanaSection(
            Copy.benchmark,
            hint = if (engineStatus.online) Copy.benchmarkHint else Copy.benchmarkOfflineHint,
        ) {
            CapsuleButton(
                if (benchBusy) Copy.benchmarkRunning else Copy.runBenchmark,
                onClick = vm::runBenchmark,
                modifier = Modifier.fillMaxWidth(),
                enabled = engineStatus.online && !testBusy && !benchBusy,
            )
            when (val state = benchState) {
                is BenchUiState.Running -> Text(
                    state.step,
                    color = HanaColors.accentLilac,
                    fontSize = 13.sp,
                )
                is BenchUiState.Failed -> Text(
                    state.message,
                    color = HanaColors.qualityRed,
                    fontSize = 13.sp,
                )
                is BenchUiState.Done -> BenchmarkReportCard(state.report)
                BenchUiState.Idle -> Unit
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun BenchmarkReportCard(report: BenchmarkReport) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        MetricBlock(Copy.benchmarkLatencyTitle) {
            BenchMetricRow(Copy.median, formatDuration(report.policyMedianMs))
            BenchMetricRow(Copy.p95, formatDuration(report.policyP95Ms))
            BenchMetricRow(
                Copy.queriesPerSec,
                formatRate(report.policyQueriesPerSec),
            )
        }
        MetricBlock(Copy.benchmarkSearchTitle) {
            report.searches.forEach { sample ->
                BenchMetricRow(
                    Copy.searchRung(sample.requestedVisits),
                    Copy.searchRungValue(
                        formatDuration(sample.elapsedMs),
                        formatRate(sample.visitsPerSec),
                    ),
                )
            }
            Text(
                Copy.playVisitsEta(report.playVisits, formatDuration(report.playVisitsEtaMs)),
                color = HanaColors.textDim,
                fontSize = 13.sp,
            )
        }
        MetricBlock(Copy.benchmarkHumanTitle) {
            Text(
                if (report.humanPolicyPresent && report.humanMs != null) {
                    "${formatDuration(report.humanMs)}  ·  ${Copy.humanPolicyPresent}"
                } else {
                    Copy.humanNetMissing
                },
                color = HanaColors.text,
                fontSize = 13.sp,
            )
        }
        Text(
            verdictHeadline(report.verdict),
            color = verdictColor(report.verdict),
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            verdictGear(report.verdict),
            color = HanaColors.textDim,
            fontSize = 12.sp,
        )
    }
}

@Composable
private fun MetricBlock(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            title,
            color = HanaColors.textDim,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
        )
        content()
    }
}

private fun verdictHeadline(verdict: BenchmarkVerdict): String = when (verdict) {
    BenchmarkVerdict.Excellent -> Copy.benchmarkExcellent
    BenchmarkVerdict.Smooth -> Copy.benchmarkSmooth
    BenchmarkVerdict.Playable -> Copy.benchmarkPlayable
    BenchmarkVerdict.Tight -> Copy.benchmarkTight
    BenchmarkVerdict.Strained -> Copy.benchmarkStrained
}

private fun verdictGear(verdict: BenchmarkVerdict): String = when (verdict) {
    BenchmarkVerdict.Excellent -> Copy.benchmarkExcellentGear
    BenchmarkVerdict.Smooth -> Copy.benchmarkSmoothGear
    BenchmarkVerdict.Playable -> Copy.benchmarkPlayableGear
    BenchmarkVerdict.Tight -> Copy.benchmarkTightGear
    BenchmarkVerdict.Strained -> Copy.benchmarkStrainedGear
}

private fun verdictColor(verdict: BenchmarkVerdict): Color = when (verdict) {
    BenchmarkVerdict.Excellent -> HanaColors.accentPink
    BenchmarkVerdict.Smooth -> HanaColors.qualityMint
    BenchmarkVerdict.Playable -> HanaColors.accentLilac
    BenchmarkVerdict.Tight -> HanaColors.qualityOrange
    BenchmarkVerdict.Strained -> HanaColors.qualityRed
}

@Composable
private fun BenchMetricRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = HanaColors.textDim, fontSize = 13.sp)
        Text(
            value,
            color = HanaColors.text,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}


