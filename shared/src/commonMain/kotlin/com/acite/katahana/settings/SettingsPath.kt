package com.acite.katahana.settings

const val SETTINGS_FILE = "katahana.preferences_pb"

expect fun appDir(): String

expect fun settingsFilePath(): String
