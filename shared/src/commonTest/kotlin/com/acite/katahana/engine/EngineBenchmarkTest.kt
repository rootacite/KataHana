package com.acite.katahana.engine

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
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
    fun networkVerdictUsesInclusiveRttTiers() {
        assertEquals(NetworkVerdict.Local, networkVerdict(20))
        assertEquals(NetworkVerdict.Local, networkVerdict(8))
        assertEquals(NetworkVerdict.Lan, networkVerdict(21))
        assertEquals(NetworkVerdict.Lan, networkVerdict(50))
        assertEquals(NetworkVerdict.Nearby, networkVerdict(51))
        assertEquals(NetworkVerdict.Nearby, networkVerdict(120))
        assertEquals(NetworkVerdict.Distant, networkVerdict(121))
        assertEquals(NetworkVerdict.Distant, networkVerdict(300))
        assertEquals(NetworkVerdict.HighDelay, networkVerdict(301))
        assertEquals(NetworkVerdict.HighDelay, networkVerdict(800))
    }

    @Test
    fun hardwareScoreIsLogMappedAndClamped() {
        assertEquals(0f, hardwareScore(20.0))
        assertEquals(0f, hardwareScore(5.0))
        assertEquals(0f, hardwareScore(Double.NaN))
        assertEquals(100f, hardwareScore(15_000.0))
        assertEquals(100f, hardwareScore(40_000.0))
        assertNear(21f, hardwareScore(80.0), 2f)
        assertNear(35f, hardwareScore(200.0), 2f)
        assertNear(49f, hardwareScore(500.0), 2f)
        assertNear(65f, hardwareScore(1_500.0), 3f)
        assertNear(83f, hardwareScore(5_000.0), 3f)
    }

    @Test
    fun correctedVisitsPerSecSubtractsRtt() {
        val fastRemote = correctedVisitsPerSec(2_000, elapsedMs = 250, rttMs = 200)
        assertTrue(fastRemote in 30_000.0..45_000.0, "got $fastRemote")
        val slowLocal = correctedVisitsPerSec(2_000, elapsedMs = 25_000, rttMs = 10)
        assertTrue(slowLocal in 75.0..85.0, "got $slowLocal")
        assertEquals(2_000_000.0, correctedVisitsPerSec(2_000, elapsedMs = 200, rttMs = 200))
    }

    @Test
    fun highRttFastSearchIsDistantNetworkAndHighHardware() {
        val report = assembleReport(
            pingMs = List(20) { 220L },
            pingOk = true,
            policyMs = List(8) { 240L },
            searches = listOf(
                SearchSample(80, 80, 230),
                SearchSample(400, 400, 250),
                SearchSample(2_000, 2_000, 280),
            ),
            playVisits = 400,
            humanMs = 240,
            humanPolicyPresent = true,
        )
        assertEquals(NetworkVerdict.Distant, report.network.verdict)
        assertEquals(220L, report.network.medianMs)
        assertTrue(report.network.pingOk)
        assertTrue(report.hardware.visitsPerSec > 20_000.0, "got ${report.hardware.visitsPerSec}")
        assertTrue(report.hardware.score >= 90f, "got ${report.hardware.score}")
        assertEquals(HardwareBand.HighEnd, report.hardware.band)
        assertEquals(PlayFeel.GpuPlentyNetworkWaits, report.feel)
    }

    @Test
    fun lowRttSlowSearchIsLocalNetworkAndWeakHardware() {
        val report = assembleReport(
            pingMs = List(20) { 8L },
            pingOk = true,
            policyMs = List(8) { 90L },
            searches = listOf(
                SearchSample(80, 80, 3_000),
                SearchSample(400, 400, 12_000),
                SearchSample(2_000, 2_000, 60_000),
            ),
            playVisits = 400,
            humanMs = null,
            humanPolicyPresent = false,
        )
        assertEquals(NetworkVerdict.Local, report.network.verdict)
        assertEquals(8L, report.network.medianMs)
        assertTrue(report.hardware.visitsPerSec in 30.0..40.0, "got ${report.hardware.visitsPerSec}")
        assertTrue(report.hardware.score < 20f, "got ${report.hardware.score}")
        assertEquals(HardwareBand.WeakCpu, report.hardware.band)
        assertEquals(PlayFeel.NetworkLocalSearchLimits, report.feel)
        assertFalse(report.humanPolicyPresent)
    }

    @Test
    fun pingFailureFallsBackToSearchSlope() {
        val report = assembleReport(
            pingMs = emptyList(),
            pingOk = false,
            policyMs = List(8) { 70L },
            searches = listOf(
                SearchSample(80, 80, 50),
                SearchSample(400, 400, 220),
                SearchSample(2_000, 2_000, 1_000),
            ),
            playVisits = 400,
            humanMs = 94,
            humanPolicyPresent = true,
        )
        assertFalse(report.network.pingOk)
        assertTrue(report.network.rttMs.isEmpty())
        val slope = rttFromSearchSlope(report.hardware.searches)
        assertEquals(slope, report.network.medianMs)
        assertTrue(report.hardware.visitsPerSec > 1_500.0)
        assertEquals(PlayFeel.BothComfortable, report.feel)
    }

    @Test
    fun bothTightWhenDistantAndWeak() {
        val report = assembleReport(
            pingMs = List(20) { 400L },
            pingOk = true,
            policyMs = List(8) { 450L },
            searches = listOf(
                SearchSample(80, 80, 3_400),
                SearchSample(400, 400, 12_400),
                SearchSample(2_000, 2_000, 60_400),
            ),
            playVisits = 400,
            humanMs = null,
            humanPolicyPresent = false,
        )
        assertEquals(NetworkVerdict.HighDelay, report.network.verdict)
        assertTrue(report.hardware.score < 20f)
        assertEquals(PlayFeel.BothTight, report.feel)
    }

    @Test
    fun localFastIsComfortable() {
        val report = assembleReport(
            pingMs = List(20) { 12L },
            pingOk = true,
            policyMs = List(8) { 70L },
            searches = listOf(
                SearchSample(80, 80, 50),
                SearchSample(400, 400, 300),
                SearchSample(2_000, 2_000, 1_200),
            ),
            playVisits = 400,
            humanMs = 94,
            humanPolicyPresent = true,
        )
        assertEquals(NetworkVerdict.Local, report.network.verdict)
        assertEquals(12L, report.network.medianMs)
        assertEquals(70L, report.policyMedianMs)
        assertEquals(PlayFeel.BothComfortable, report.feel)
        assertTrue(report.hardware.score > 55f)
        assertEquals(3, report.hardware.searches.size)
        assertTrue(report.humanPolicyPresent)
        assertEquals(94L, report.humanMs)
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
        assertEquals("0", formatScore(0f))
        assertEquals("100", formatScore(100f))
        assertEquals("67", formatScore(66.6f))
    }

    @Test
    fun searchLadderAndTimeoutsMatchFiveXStress() {
        assertEquals(listOf(80, 400, 2_000), BENCH_SEARCH_LADDER)
        assertEquals(8, BENCH_POLICY_SAMPLES)
        assertEquals(20, BENCH_PING_SAMPLES)
        assertEquals(24_000L, benchSearchTimeoutMs(80))
        assertEquals(40_000L, benchSearchTimeoutMs(400))
        assertEquals(120_000L, benchSearchTimeoutMs(2_000))
    }

    private fun assertNear(expected: Float, actual: Float, delta: Float) {
        assertTrue(abs(expected - actual) <= delta, "expected $expected ± $delta, got $actual")
    }
}
