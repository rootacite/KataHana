package com.acite.katahana.settings

import java.io.File

actual fun settingsFilePath(): String {
    val dir = File(System.getProperty("user.home"), ".katahana")
    dir.mkdirs()
    return File(dir, SETTINGS_FILE).absolutePath
}
