package com.acite.katahana

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.acite.katahana.sgf.AndroidSgfFiles
import com.acite.katahana.sgf.LocalSgfFiles
import com.acite.katahana.ui.AppExit
import com.acite.katahana.ui.LocalAppExit

class MainActivity : ComponentActivity() {
    private lateinit var sgfFiles: AndroidSgfFiles

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        hideSystemBars()
        sgfFiles = AndroidSgfFiles(this)
        val appGraph = (application as KataHanaApp).graph
        setContent {
            CompositionLocalProvider(
                LocalSgfFiles provides sgfFiles,
                LocalAppExit provides AppExit { finishAffinity() },
            ) {
                App(appGraph.metroViewModelFactory)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        hideSystemBars()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemBars()
    }

    private fun hideSystemBars() {
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }
}
