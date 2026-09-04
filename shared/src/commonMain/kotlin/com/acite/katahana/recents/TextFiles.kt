package com.acite.katahana.recents

internal expect fun readUtf8(path: String): String?

internal expect fun writeUtf8(path: String, text: String)
