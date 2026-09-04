package com.acite.katahana

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import com.acite.katahana.sgf.AndroidSgfFiles
import com.acite.katahana.sgf.LocalSgfFiles

class MainActivity : ComponentActivity() {
    private lateinit var sgfFiles: AndroidSgfFiles

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        sgfFiles = AndroidSgfFiles(this)
        val appGraph = (application as KataHanaApp).graph
        setContent {
            CompositionLocalProvider(LocalSgfFiles provides sgfFiles) {
                App(appGraph.metroViewModelFactory)
            }
        }
    }
}
