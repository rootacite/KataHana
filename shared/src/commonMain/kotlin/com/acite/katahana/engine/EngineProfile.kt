package com.acite.katahana.engine

data class EngineProfile(
    val name: String = "KataGo",
    val url: String = DEFAULT_URL,
    val token: String = "",
    val playVisits: Int = 400,
    val reviewVisits: Int = 400,
) {
    companion object {
        const val DEFAULT_URL = "ws://127.0.0.1:2080"
    }
}

enum class EnginePhase {
    Disconnected,
    Connecting,
    Ready,
    Analyzing,
    Error,
}

data class EngineStatus(
    val phase: EnginePhase = EnginePhase.Disconnected,
    val detail: String = "",
) {
    val online: Boolean
        get() = phase == EnginePhase.Ready || phase == EnginePhase.Analyzing
}

sealed class TestResult {
    data class Ok(val visits: Int, val versionHint: String = "") : TestResult()
    data class Fail(val message: String) : TestResult()
}
