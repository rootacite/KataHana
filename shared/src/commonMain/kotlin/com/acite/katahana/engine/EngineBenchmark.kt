package com.acite.katahana.engine

import kotlin.math.ln
import kotlin.math.roundToInt

const val BENCH_PING_SAMPLES = 20
const val BENCH_POLICY_SAMPLES = 8
const val BENCH_LIGHT_VISITS = 80
const val BENCH_PLAY_VISITS = 400
const val BENCH_STRESS_VISITS = 2_000
const val BENCH_POLICY_TIMEOUT_MS = 20_000L

val BENCH_SEARCH_LADDER = listOf(BENCH_LIGHT_VISITS, BENCH_PLAY_VISITS, BENCH_STRESS_VISITS)

internal const val HARDWARE_VPS_FLOOR = 20.0
internal const val HARDWARE_VPS_CEIL = 15_000.0

internal const val NET_LOCAL_MS = 20L
internal const val NET_LAN_MS = 50L
internal const val NET_NEARBY_MS = 120L
internal const val NET_DISTANT_MS = 300L

internal const val GPU_PLENTY_SCORE = 55f
internal const val GPU_WEAK_SCORE = 40f

enum class NetworkVerdict {
    Local,
    Lan,
    Nearby,
    Distant,
    HighDelay,
}

enum class HardwareBand {
    WeakCpu,
    LaptopCpu,
    DesktopCpu,
    EntryGpu,
    MidGpu,
    HighEnd,
}

enum class PlayFeel {
    GpuPlentyNetworkWaits,
    NetworkLocalSearchLimits,
    BothComfortable,
    BothTight,
}

sealed class BenchmarkStep {
    data object Warmup : BenchmarkStep()
    data class Ping(val done: Int, val total: Int) : BenchmarkStep()
    data class Latency(val done: Int, val total: Int) : BenchmarkStep()
    data class Search(val visits: Int) : BenchmarkStep()
    data object Human : BenchmarkStep()
}

data class SearchSample(
    val requestedVisits: Int,
    val actualVisits: Int,
    val elapsedMs: Long,
) {
    val visitsPerSec: Double
        get() = if (actualVisits <= 0 || elapsedMs <= 0L) 0.0 else actualVisits * 1000.0 / elapsedMs
}

data class NetworkReport(
    val rttMs: List<Long>,
    val medianMs: Long,
    val p95Ms: Long,
    val minMs: Long,
    val maxMs: Long,
    val verdict: NetworkVerdict,
    val pingOk: Boolean,
)

data class HardwareReport(
    val searches: List<SearchSample>,
    val visitsPerSec: Double,
    val score: Float,
    val playVisits: Int,
    val playVisitsEtaMs: Long,
    val band: HardwareBand,
)

data class BenchmarkReport(
    val network: NetworkReport,
    val hardware: HardwareReport,
    val policyMs: List<Long>,
    val policyMedianMs: Long,
    val policyP95Ms: Long,
    val humanMs: Long?,
    val humanPolicyPresent: Boolean,
    val feel: PlayFeel,
)

sealed class BenchmarkResult {
    data class Ok(val report: BenchmarkReport) : BenchmarkResult()
    data class Fail(val message: String) : BenchmarkResult()
}

sealed class BenchUiState {
    data object Idle : BenchUiState()
    data class Running(val step: String) : BenchUiState()
    data class Done(val report: BenchmarkReport) : BenchUiState()
    data class Failed(val message: String) : BenchUiState()
}

fun benchSearchTimeoutMs(visits: Int): Long =
    (20_000L + visits.toLong() * 50L).coerceAtMost(180_000L)

fun networkVerdict(medianMs: Long): NetworkVerdict = when {
    medianMs <= NET_LOCAL_MS -> NetworkVerdict.Local
    medianMs <= NET_LAN_MS -> NetworkVerdict.Lan
    medianMs <= NET_NEARBY_MS -> NetworkVerdict.Nearby
    medianMs <= NET_DISTANT_MS -> NetworkVerdict.Distant
    else -> NetworkVerdict.HighDelay
}

fun correctedVisitsPerSec(visits: Int, elapsedMs: Long, rttMs: Long): Double {
    if (visits <= 0) return 0.0
    val compute = (elapsedMs - rttMs).coerceAtLeast(1L)
    return visits * 1000.0 / compute
}

fun hardwareScore(vps: Double): Float {
    if (!vps.isFinite() || vps <= HARDWARE_VPS_FLOOR) return 0f
    if (vps >= HARDWARE_VPS_CEIL) return 100f
    val t = ln(vps / HARDWARE_VPS_FLOOR) / ln(HARDWARE_VPS_CEIL / HARDWARE_VPS_FLOOR)
    return (t.toFloat() * 100f).coerceIn(0f, 100f)
}

fun hardwareBand(score: Float): HardwareBand = when {
    score < 20f -> HardwareBand.WeakCpu
    score < 35f -> HardwareBand.LaptopCpu
    score < 50f -> HardwareBand.DesktopCpu
    score < 70f -> HardwareBand.EntryGpu
    score < 85f -> HardwareBand.MidGpu
    else -> HardwareBand.HighEnd
}

fun playFeel(network: NetworkReport, hardware: HardwareReport): PlayFeel {
    val netSlow = network.verdict == NetworkVerdict.Distant ||
        network.verdict == NetworkVerdict.HighDelay
    val netSnappy = network.verdict == NetworkVerdict.Local ||
        network.verdict == NetworkVerdict.Lan
    val gpuPlenty = hardware.score >= GPU_PLENTY_SCORE
    val gpuWeak = hardware.score < GPU_WEAK_SCORE
    return when {
        gpuPlenty && netSlow -> PlayFeel.GpuPlentyNetworkWaits
        netSnappy && gpuWeak -> PlayFeel.NetworkLocalSearchLimits
        netSlow && gpuWeak -> PlayFeel.BothTight
        else -> PlayFeel.BothComfortable
    }
}

fun rttFromSearchSlope(searches: List<SearchSample>): Long? {
    val play = searches.firstOrNull { it.requestedVisits == BENCH_PLAY_VISITS } ?: return null
    val stress = searches.firstOrNull { it.requestedVisits == BENCH_STRESS_VISITS } ?: return null
    val dv = (stress.actualVisits - play.actualVisits).coerceAtLeast(1)
    val dt = (stress.elapsedMs - play.elapsedMs).coerceAtLeast(1L)
    val vps = dv * 1000.0 / dt
    if (!vps.isFinite() || vps <= 0.0) return null
    val rtt = play.elapsedMs - (play.actualVisits * 1000.0 / vps)
    if (!rtt.isFinite()) return null
    return rtt.roundToInt().toLong().coerceAtLeast(1L)
}

fun percentile(values: List<Long>, p: Double): Long {
    if (values.isEmpty()) return 0L
    val sorted = values.sorted()
    if (sorted.size == 1) return sorted[0]
    val pos = p.coerceIn(0.0, 1.0) * (sorted.size - 1)
    val lo = pos.toInt().coerceIn(0, sorted.lastIndex)
    val hi = (lo + 1).coerceAtMost(sorted.lastIndex)
    val frac = pos - lo
    return (sorted[lo] * (1.0 - frac) + sorted[hi] * frac).roundToInt().toLong()
}

fun playVisitsEtaMs(playVisits: Int, vps: Double): Long {
    if (vps <= 0.0 || !vps.isFinite()) return Long.MAX_VALUE
    return (playVisits * 1000.0 / vps).roundToInt().toLong()
}

fun assembleReport(
    pingMs: List<Long>,
    pingOk: Boolean,
    policyMs: List<Long>,
    searches: List<SearchSample>,
    playVisits: Int,
    humanMs: Long?,
    humanPolicyPresent: Boolean,
): BenchmarkReport {
    val usedPing = pingOk && pingMs.isNotEmpty()
    val slopeRtt = if (usedPing) null else rttFromSearchSlope(searches)
    val rttMedian = when {
        usedPing -> percentile(pingMs, 0.5)
        slopeRtt != null -> slopeRtt
        else -> 1L
    }
    val rttP95 = if (usedPing) percentile(pingMs, 0.95) else rttMedian
    val rttMin = if (usedPing) pingMs.minOrNull() ?: rttMedian else rttMedian
    val rttMax = if (usedPing) pingMs.maxOrNull() ?: rttMedian else rttMedian
    val stress = searches.firstOrNull { it.requestedVisits == BENCH_STRESS_VISITS }
        ?: searches.maxByOrNull { it.requestedVisits }
    val vps = if (stress != null) {
        correctedVisitsPerSec(stress.actualVisits, stress.elapsedMs, rttMedian)
    } else {
        0.0
    }
    val score = hardwareScore(vps)
    val network = NetworkReport(
        rttMs = if (usedPing) pingMs else emptyList(),
        medianMs = rttMedian,
        p95Ms = rttP95,
        minMs = rttMin,
        maxMs = rttMax,
        verdict = networkVerdict(rttMedian),
        pingOk = usedPing,
    )
    val hardware = HardwareReport(
        searches = searches,
        visitsPerSec = vps,
        score = score,
        playVisits = playVisits.coerceAtLeast(1),
        playVisitsEtaMs = playVisitsEtaMs(playVisits.coerceAtLeast(1), vps),
        band = hardwareBand(score),
    )
    return BenchmarkReport(
        network = network,
        hardware = hardware,
        policyMs = policyMs,
        policyMedianMs = percentile(policyMs, 0.5),
        policyP95Ms = percentile(policyMs, 0.95),
        humanMs = humanMs,
        humanPolicyPresent = humanPolicyPresent,
        feel = playFeel(network, hardware),
    )
}

fun formatDuration(ms: Long): String {
    if (ms < 0L) return "—"
    if (ms == Long.MAX_VALUE) return "—"
    if (ms < 1000L) return "$ms ms"
    val whole = ms / 1000
    val hundredths = (ms % 1000) / 10
    return "$whole.${hundredths.toString().padStart(2, '0')} s"
}

fun formatRate(rate: Double): String {
    if (!rate.isFinite() || rate <= 0.0) return "—"
    if (rate >= 100.0) return rate.roundToInt().toString()
    val tenths = (rate * 10.0).roundToInt()
    return "${tenths / 10}.${tenths % 10}"
}

fun formatScore(score: Float): String {
    val rounded = score.roundToInt().coerceIn(0, 100)
    return "$rounded"
}
