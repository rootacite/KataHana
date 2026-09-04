package com.acite.katahana

class JVMPlatform : Platform {
    override val name: String = "Java ${System.getProperty("java.version")}"
    override val isMobile: Boolean = false
}

actual fun getPlatform(): Platform = JVMPlatform()

actual fun epochMillis(): Long = System.currentTimeMillis()