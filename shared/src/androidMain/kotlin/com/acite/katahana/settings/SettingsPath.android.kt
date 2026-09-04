package com.acite.katahana.settings

import android.annotation.SuppressLint
import android.content.Context
import java.io.File

@SuppressLint("StaticFieldLeak")
private var androidAppContext: Context? = null

fun installAppContext(context: Context) {
    androidAppContext = context.applicationContext
}

private fun requireAppContext(): Context =
    androidAppContext
        ?: Class.forName("android.app.ActivityThread")
            .getMethod("currentApplication")
            .invoke(null) as Context

actual fun settingsFilePath(): String =
    File(requireAppContext().filesDir, SETTINGS_FILE).absolutePath
