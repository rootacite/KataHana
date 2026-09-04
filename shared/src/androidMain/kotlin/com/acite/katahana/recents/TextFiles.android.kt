package com.acite.katahana.recents

import java.io.File

internal actual fun readUtf8(path: String): String? {
    val file = File(path)
    if (!file.isFile) return null
    return runCatching { file.readText() }.getOrNull()
}

internal actual fun writeUtf8(path: String, text: String) {
    val file = File(path)
    file.parentFile?.mkdirs()
    file.writeText(text)
}
