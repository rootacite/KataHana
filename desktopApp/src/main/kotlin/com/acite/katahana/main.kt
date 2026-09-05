package com.acite.katahana

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.acite.katahana.sgf.JvmSgfFiles
import com.acite.katahana.sgf.LocalSgfFiles
import com.acite.katahana.ui.AppExit
import com.acite.katahana.ui.LocalAppExit
import dev.zacsweers.metro.createGraph
import katahana.desktopapp.generated.resources.Res
import katahana.desktopapp.generated.resources.app_icon
import org.jetbrains.compose.resources.painterResource

fun main() {
    val appGraph = createGraph<AppGraph>()
    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "KataHana",
            icon = painterResource(Res.drawable.app_icon),
            state = rememberWindowState(width = 1280.dp, height = 800.dp),
        ) {
            val sgfFiles = remember { JvmSgfFiles() }
            CompositionLocalProvider(
                LocalSgfFiles provides sgfFiles,
                LocalAppExit provides AppExit { exitApplication() },
            ) {
                App(appGraph.metroViewModelFactory)
            }
        }
    }
}
