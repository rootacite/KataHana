package com.acite.katahana.ui

import androidx.compose.runtime.staticCompositionLocalOf

fun interface AppExit {
    fun exit()
}

val LocalAppExit = staticCompositionLocalOf { AppExit { } }
