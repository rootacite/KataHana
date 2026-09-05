package com.acite.katahana.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EngineBenchmarkTest {

    @Test
    fun percentileInterpolates() {
        val samples = listOf(10L, 20L, 30L, 40L, 50L, 60L)
        assertEquals(10L, percentile(samples, 0.0))
        assertEquals(35L, percentile(samples, 0.5))
        assertEquals(58L, percentile(samples, 0.95))
        assertEquals(60L, percentile(samples, 1.0))
        assertEquals(0L, percentile(emptyList(), 0.5))
        assertEquals(7L, percentile(listOf(7L), 0.95))
    }

    @Test
    fun concludeUsesFiveInclusiveTiers() {
        assertEquals(BenchmarkVerdict.Excellent, conclude(80, 350, 1_500))
        assertEquals(BenchmarkVerdict.Excellent, conclude(40, 200, 800))
        assertEquals(BenchmarkVerdict.Smooth, conclude(81, 350, 1_500))
        assertEquals(BenchmarkVerdict.Smooth, conclude(80, 351, 1_500))
        assertEquals(BenchmarkVerdict.Smooth, conclude(80, 350, 1_501))
        assertEquals(BenchmarkVerdict.Smooth, conclude(200, 1_000, 4_000))
        assertEquals(BenchmarkVerdict.Playable, conclude(201, 1_000, 4_000))
        assertEquals(BenchmarkVerdict.Playable, conclude(400, 2_500, 10_000))
        assertEquals(BenchmarkVerdict.Tight, conclude(401, 2_500, 10_000))
        assertEquals(BenchmarkVerdict.Tight, conclude(1_000, 6_000, 25_000))
        assertEquals(BenchmarkVerdict.Strained, conclude(1_001, 6_000, 25_000))
        assertEquals(BenchmarkVerdict.Strained, conclude(1_000, 6_001, 25_000))
        assertEquals(BenchmarkVerdict.Strained, conclude(1_000, 6_000, 25_001))
        assertEquals(BenchmarkVerdict.Strained, conclude(40, 200, 30_000))
        assertEquals(BenchmarkVerdict.Strained, conclude(2_000, 200, 800))
    }

    @Test
    fun assembleReportFillsLadderAndExcellentVerdict() {
        val policy = List(30) { 70L }
        val report = assembleReport(
            policyMs = policy,
            searches = listOf(
                SearchSample(80, 80, 50),
                SearchSample(400, 400, 300),
                SearchSample(2_000, 2_000, 1_200),
            ),
            playVisits = 400,
            humanMs = 94,
            humanPolicyPresent = true,
        )
        assertEquals(70L, report.policyMedianMs)
        assertEquals(70L, report.policyMinMs)
        assertEquals(70L, report.policyMaxMs)
        assertEquals(BenchmarkVerdict.Excellent, report.verdict)
        assertEquals(3, report.searches.size)
        assertEquals(80, report.searches[0].requestedVisits)
        assertEquals(400, report.searches[1].requestedVisits)
        assertEquals(2_000, report.searches[2].requestedVisits)
        assertTrue(report.searches[1].visitsPerSec in 1_300.0..1_400.0)
        assertEquals(300L, report.playVisitsEtaMs)
        assertEquals(94L, report.humanMs)
        assertTrue(report.humanPolicyPresent)
    }

    @Test
    fun formatDurationAndRate() {
        assertEquals("0 ms", formatDuration(0))
        assertEquals("999 ms", formatDuration(999))
        assertEquals("1.00 s", formatDuration(1_000))
        assertEquals("1.31 s", formatDuration(1_310))
        assertEquals("2.50 s", formatDuration(2_500))
        assertEquals("—", formatDuration(Long.MAX_VALUE))
        assertEquals("12.2", formatRate(12.24))
        assertEquals("1290", formatRate(1290.4))
        assertEquals("—", formatRate(0.0))
        assertEquals("—", formatRate(Double.NaN))
    }

    @Test
    fun missingHumanNetDoesNotChangeVerdict() {
        val report = assembleReport(
            policyMs = List(30) { 200L },
            searches = listOf(
                SearchSample(80, 80, 200),
                SearchSample(400, 400, 1_000),
                SearchSample(2_000, 2_000, 4_000),
            ),
            playVisits = 500,
            humanMs = null,
            humanPolicyPresent = false,
        )
        assertEquals(BenchmarkVerdict.Smooth, report.verdict)
        assertEquals(false, report.humanPolicyPresent)
        assertEquals(null, report.humanMs)
        assertTrue(report.playVisitsEtaMs in 1_200L..1_300L)
    }

    @Test
    fun searchLadderAndTimeoutsMatchFiveXStress() {
        assertEquals(listOf(80, 400, 2_000), BENCH_SEARCH_LADDER)
        assertEquals(30, BENCH_POLICY_SAMPLES)
        assertEquals(24_000L, benchSearchTimeoutMs(80))
        assertEquals(40_000L, benchSearchTimeoutMs(400))
        assertEquals(120_000L, benchSearchTimeoutMs(2_000))
    }
}
