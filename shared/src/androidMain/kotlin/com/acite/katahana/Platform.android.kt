package com.acite.katahana

import android.os.Build

class AndroidPlatform : Platform {
    override val name: String = "Android ${Build.VERSION.SDK_INT}"
    override val isMobile: Boolean = true
}

actual fun getPlatform(): Platform = AndroidPlatform()

actual fun epochMillis(): Long = System.currentTimeMillis()