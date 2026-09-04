package com.acite.katahana.settings

import java.io.File

actual fun appDir(): String {
    val dir = File(System.getProperty("user.home"), ".katahana")
    dir.mkdirs()
    return dir.absolutePath
}

actual fun settingsFilePath(): String = File(appDir(), SETTINGS_FILE).absolutePath
