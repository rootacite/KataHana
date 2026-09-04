package com.acite.katahana

import platform.Foundation.NSDate
import platform.UIKit.UIDevice

class IOSPlatform : Platform {
    override val name: String = UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion
    override val isMobile: Boolean = true
}

actual fun getPlatform(): Platform = IOSPlatform()

actual fun epochMillis(): Long = (NSDate().timeIntervalSince1970 * 1000.0).toLong()