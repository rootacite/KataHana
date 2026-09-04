package com.acite.katahana

import androidx.compose.ui.window.ComposeUIViewController
import dev.zacsweers.metro.createGraph

fun MainViewController() = ComposeUIViewController {
    val appGraph = createGraph<AppGraph>()
    App(appGraph.metroViewModelFactory)
}
