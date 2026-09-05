package com.acite.katahana.engine

import kotlin.math.roundToInt

const val BENCH_POLICY_SAMPLES = 30
const val BENCH_LIGHT_VISITS = 80
const val BENCH_PLAY_VISITS = 400
const val BENCH_STRESS_VISITS = 2_000
const val BENCH_POLICY_TIMEOUT_MS = 20_000L

val BENCH_SEARCH_LADDER = listOf(BENCH_LIGHT_VISITS, BENCH_PLAY_VISITS, BENCH_STRESS_VISITS)

internal const val EXCELLENT_MEDIAN_MS = 80L
internal const val EXCELLENT_PLAY_MS = 350L
internal const val EXCELLENT_STRESS_MS = 1_500L
internal const val SMOOTH_MEDIAN_MS = 200L
internal const val SMOOTH_PLAY_MS = 1_000L
internal const val SMOOTH_STRESS_MS = 4_000L
internal const val PLAYABLE_MEDIAN_MS = 400L
internal const val PLAYABLE_PLAY_MS = 2_500L
internal const val PLAYABLE_STRESS_MS = 10_000L
internal const val TIGHT_MEDIAN_MS = 1_000L
internal const val TIGHT_PLAY_MS = 6_000L
internal const val TIGHT_STRESS_MS = 25_000L

enum class BenchmarkVerdict {
    Excellent,
    Smooth,
    Playable,
    Tight,
    Strained,
}

sealed class BenchmarkStep {
    data object Warmup : BenchmarkStep()
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

data class BenchmarkReport(
    val policyMs: List<Long>,
    val policyMedianMs: Long,
    val policyP95Ms: Long,
    val policyMinMs: Long,
    val policyMaxMs: Long,
    val policyQueriesPerSec: Double,
    val searches: List<SearchSample>,
    val playVisits: Int,
    val playVisitsEtaMs: Long,
    val humanMs: Long?,
    val humanPolicyPresent: Boolean,
    val verdict: BenchmarkVerdict,
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

fun conclude(medianMs: Long, playMs: Long, stressMs: Long): BenchmarkVerdict {
    fun within(med: Long, play: Long, stress: Long): Boolean =
        medianMs <= med && playMs <= play && stressMs <= stress
    return when {
        within(EXCELLENT_MEDIAN_MS, EXCELLENT_PLAY_MS, EXCELLENT_STRESS_MS) ->
            BenchmarkVerdict.Excellent
        within(SMOOTH_MEDIAN_MS, SMOOTH_PLAY_MS, SMOOTH_STRESS_MS) ->
            BenchmarkVerdict.Smooth
        within(PLAYABLE_MEDIAN_MS, PLAYABLE_PLAY_MS, PLAYABLE_STRESS_MS) ->
            BenchmarkVerdict.Playable
        within(TIGHT_MEDIAN_MS, TIGHT_PLAY_MS, TIGHT_STRESS_MS) ->
            BenchmarkVerdict.Tight
        else -> BenchmarkVerdict.Strained
    }
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

fun queriesPerSec(samplesMs: List<Long>): Double {
    val total = samplesMs.sum()
    if (total <= 0L || samplesMs.isEmpty()) return 0.0
    return samplesMs.size * 1000.0 / total
}

fun visitsPerSec(visits: Int, elapsedMs: Long): Double {
    if (visits <= 0 || elapsedMs <= 0L) return 0.0
    return visits * 1000.0 / elapsedMs
}

fun playVisitsEtaMs(playVisits: Int, vps: Double): Long {
    if (vps <= 0.0 || !vps.isFinite()) return Long.MAX_VALUE
    return (playVisits * 1000.0 / vps).roundToInt().toLong()
}

fun assembleReport(
    policyMs: List<Long>,
    searches: List<SearchSample>,
    playVisits: Int,
    humanMs: Long?,
    humanPolicyPresent: Boolean,
): BenchmarkReport {
    val median = percentile(policyMs, 0.5)
    val play = searches.firstOrNull { it.requestedVisits == BENCH_PLAY_VISITS }
    val stress = searches.firstOrNull { it.requestedVisits == BENCH_STRESS_VISITS }
    val etaSource = play ?: searches.maxByOrNull { it.requestedVisits }
    return BenchmarkReport(
        policyMs = policyMs,
        policyMedianMs = median,
        policyP95Ms = percentile(policyMs, 0.95),
        policyMinMs = policyMs.minOrNull() ?: 0L,
        policyMaxMs = policyMs.maxOrNull() ?: 0L,
        policyQueriesPerSec = queriesPerSec(policyMs),
        searches = searches,
        playVisits = playVisits,
        playVisitsEtaMs = playVisitsEtaMs(playVisits, etaSource?.visitsPerSec ?: 0.0),
        humanMs = humanMs,
        humanPolicyPresent = humanPolicyPresent,
        verdict = conclude(
            medianMs = median,
            playMs = play?.elapsedMs ?: Long.MAX_VALUE,
            stressMs = stress?.elapsedMs ?: Long.MAX_VALUE,
        ),
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
