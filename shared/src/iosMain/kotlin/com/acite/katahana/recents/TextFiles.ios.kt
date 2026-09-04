package com.acite.katahana.recents

import platform.Foundation.NSFileManager
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.stringWithContentsOfFile
import platform.Foundation.writeToFile

internal actual fun readUtf8(path: String): String? =
    NSString.stringWithContentsOfFile(path, encoding = NSUTF8StringEncoding, error = null)

@Suppress("CAST_NEVER_SUCCEEDS")
internal actual fun writeUtf8(path: String, text: String) {
    val parent = path.substringBeforeLast('/', missingDelimiterValue = "")
    if (parent.isNotEmpty()) {
        NSFileManager.defaultManager.createDirectoryAtPath(
            parent,
            withIntermediateDirectories = true,
            attributes = null,
            error = null,
        )
    }
    (text as NSString).writeToFile(
        path,
        atomically = true,
        encoding = NSUTF8StringEncoding,
        error = null,
    )
}
