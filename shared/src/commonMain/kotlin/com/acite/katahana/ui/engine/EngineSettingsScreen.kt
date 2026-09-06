package com.acite.katahana.ui.engine

import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.acite.katahana.engine.BenchUiState
import com.acite.katahana.engine.BenchmarkReport
import com.acite.katahana.engine.HardwareBand
import com.acite.katahana.engine.NetworkVerdict
import com.acite.katahana.engine.PlayFeel
import com.acite.katahana.engine.formatDuration
import com.acite.katahana.engine.formatRate
import com.acite.katahana.engine.formatScore
import com.acite.katahana.ui.Copy
import com.acite.katahana.ui.components.HanaBackdrop
import com.acite.katahana.ui.components.HanaField
import com.acite.katahana.ui.components.HanaSection
import com.acite.katahana.ui.components.PorcelainButton
import com.acite.katahana.ui.components.ScreenHeader
import com.acite.katahana.ui.session.engineStatusLabel
import com.acite.katahana.ui.settings.SettingsViewModel
import com.acite.katahana.ui.theme.hanaColors
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
    HanaBackdrop { hazeState ->
        Column(
            Modifier
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            ScreenHeader(Copy.engine, onBack = { navigator.pop() })
            HanaSection(
                Copy.engine,
                hazeState = hazeState,
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
                PorcelainButton(
                    if (testBusy) Copy.testingConnection else Copy.testConnection,
                    onClick = vm::testConnection,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !testBusy && !benchBusy,
                    emphasized = true,
                    accent = hanaColors.accentBlue,
                )
                Text(
                    engineStatusLabel(engineStatus),
                    color = hanaColors.textDim,
                    fontSize = 13.sp,
                )
                if (testMessage != null) {
                    Text(testMessage.orEmpty(), color = hanaColors.accentLilac, fontSize = 13.sp)
                }
            }
            HanaSection(
                Copy.benchmark,
                hazeState = hazeState,
                hint = if (engineStatus.online) Copy.benchmarkHint else Copy.benchmarkOfflineHint,
            ) {
                PorcelainButton(
                    if (benchBusy) Copy.benchmarkRunning else Copy.runBenchmark,
                    onClick = vm::runBenchmark,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = engineStatus.online && !testBusy && !benchBusy,
                    emphasized = true,
                )
                when (val state = benchState) {
                    is BenchUiState.Running -> Text(
                        state.step,
                        color = hanaColors.accentLilac,
                        fontSize = 13.sp,
                    )
                    is BenchUiState.Failed -> Text(
                        state.message,
                        color = hanaColors.qualityRed,
                        fontSize = 13.sp,
                    )
                    is BenchUiState.Done -> BenchmarkReportCard(state.report)
                    BenchUiState.Idle -> Unit
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun BenchmarkReportCard(report: BenchmarkReport) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        MetricBlock(Copy.benchmarkNetworkTitle) {
            BenchMetricRow(Copy.median, formatDuration(report.network.medianMs))
            BenchMetricRow(Copy.p95, formatDuration(report.network.p95Ms))
            Text(
                networkHeadline(report.network.verdict),
                color = networkColor(report.network.verdict),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
            )
            if (!report.network.pingOk) {
                Text(Copy.pingFallback, color = hanaColors.textDim, fontSize = 12.sp)
            }
        }
        MetricBlock(Copy.benchmarkHardwareTitle) {
            HardwareScoreBar(report.hardware.score)
            Text(
                "${formatScore(report.hardware.score)}/100  ·  ${formatRate(report.hardware.visitsPerSec)} ${Copy.visitsPerSecLabel}",
                color = hanaColors.text,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                hardwareGear(report.hardware.band),
                color = hanaColors.textDim,
                fontSize = 12.sp,
            )
        }
        MetricBlock(Copy.benchmarkLatencyTitle) {
            BenchMetricRow(Copy.median, formatDuration(report.policyMedianMs))
            BenchMetricRow(Copy.p95, formatDuration(report.policyP95Ms))
        }
        MetricBlock(Copy.benchmarkSearchTitle) {
            report.hardware.searches.forEach { sample ->
                BenchMetricRow(
                    Copy.searchRung(sample.requestedVisits),
                    Copy.searchRungValue(
                        formatDuration(sample.elapsedMs),
                        formatRate(sample.visitsPerSec),
                    ),
                )
            }
            Text(
                Copy.playVisitsEta(
                    report.hardware.playVisits,
                    formatDuration(report.hardware.playVisitsEtaMs),
                ),
                color = hanaColors.textDim,
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
                color = hanaColors.text,
                fontSize = 13.sp,
            )
        }
        Text(
            playFeelLine(report),
            color = hanaColors.accentLilac,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun HardwareScoreBar(score: Float) {
    val t = (score / 100f).coerceIn(0f, 1f)
    val colors = hanaColors
    val measurer = rememberTextMeasurer()
    val hereStyle = TextStyle(
        color = colors.accentPink,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
    )
    val tickStyle = TextStyle(
        color = colors.textDim,
        fontSize = 10.sp,
        fontWeight = FontWeight.Medium,
    )
    Canvas(
        Modifier
            .fillMaxWidth()
            .height(72.dp),
    ) {
        val pad = 8.dp.toPx()
        val left = pad
        val right = size.width - pad
        val width = (right - left).coerceAtLeast(1f)
        val trackY = size.height * 0.46f
        val trackH = 10.dp.toPx()
        drawRoundRect(
            brush = Brush.horizontalGradient(
                0f to colors.qualityRed,
                0.28f to colors.qualityOrange,
                0.52f to colors.accentLilac,
                0.74f to colors.qualityMint,
                1f to colors.accentPink,
            ),
            topLeft = Offset(left, trackY - trackH / 2f),
            size = Size(width, trackH),
            cornerRadius = CornerRadius(trackH / 2f, trackH / 2f),
        )
        val x = left + width * t
        drawCircle(colors.accentPink, radius = 7.dp.toPx(), center = Offset(x, trackY))
        drawCircle(Color.White, radius = 3.dp.toPx(), center = Offset(x, trackY))
        val here = measurer.measure(Copy.youAreHere, hereStyle)
        val hereX = (x - here.size.width / 2f).coerceIn(0f, size.width - here.size.width)
        drawText(here, topLeft = Offset(hereX, (trackY - trackH / 2f - here.size.height - 6.dp.toPx()).coerceAtLeast(0f)))

        val ticks = listOf(
            0f to "0",
            0.18f to Copy.hardwareTickCpu,
            0.42f to Copy.hardwareTickEntry,
            0.68f to Copy.hardwareTickMid,
            0.90f to Copy.hardwareTickHigh,
            1f to "100",
        )
        val tickY = trackY + trackH / 2f + 8.dp.toPx()
        ticks.forEach { (frac, label) ->
            val layout = measurer.measure(label, tickStyle)
            val tx = when (frac) {
                0f -> left
                1f -> right - layout.size.width
                else -> (left + width * frac - layout.size.width / 2f)
                    .coerceIn(left, right - layout.size.width)
            }
            drawText(layout, topLeft = Offset(tx, tickY))
        }
    }
}

@Composable
private fun MetricBlock(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            title,
            color = hanaColors.textDim,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
        )
        content()
    }
}

private fun networkHeadline(verdict: NetworkVerdict): String = when (verdict) {
    NetworkVerdict.Local -> Copy.networkLocal
    NetworkVerdict.Lan -> Copy.networkLan
    NetworkVerdict.Nearby -> Copy.networkNearby
    NetworkVerdict.Distant -> Copy.networkDistant
    NetworkVerdict.HighDelay -> Copy.networkHighDelay
}

@Composable
private fun networkColor(verdict: NetworkVerdict): Color {
    val colors = hanaColors
    return when (verdict) {
        NetworkVerdict.Local -> colors.accentPink
        NetworkVerdict.Lan -> colors.qualityMint
        NetworkVerdict.Nearby -> colors.accentLilac
        NetworkVerdict.Distant -> colors.qualityOrange
        NetworkVerdict.HighDelay -> colors.qualityRed
    }
}

private fun hardwareGear(band: HardwareBand): String = when (band) {
    HardwareBand.WeakCpu -> Copy.hardwareWeakCpu
    HardwareBand.LaptopCpu -> Copy.hardwareLaptopCpu
    HardwareBand.DesktopCpu -> Copy.hardwareDesktopCpu
    HardwareBand.EntryGpu -> Copy.hardwareEntryGpu
    HardwareBand.MidGpu -> Copy.hardwareMidGpu
    HardwareBand.HighEnd -> Copy.hardwareHighEnd
}

private fun playFeelLine(report: BenchmarkReport): String {
    val rtt = formatDuration(report.network.medianMs)
    val eta = formatDuration(report.hardware.playVisitsEtaMs)
    val visits = report.hardware.playVisits
    return when (report.feel) {
        PlayFeel.GpuPlentyNetworkWaits -> Copy.playFeelGpuPlenty(rtt)
        PlayFeel.NetworkLocalSearchLimits -> Copy.playFeelSearchLimits(visits, eta)
        PlayFeel.BothComfortable -> Copy.playFeelBothFine(rtt, visits, eta)
        PlayFeel.BothTight -> Copy.playFeelBothTight(rtt, visits, eta)
    }
}

@Composable
private fun BenchMetricRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = hanaColors.textDim, fontSize = 13.sp)
        Text(
            value,
            color = hanaColors.text,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
