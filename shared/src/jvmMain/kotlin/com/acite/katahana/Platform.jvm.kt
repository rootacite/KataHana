package com.acite.katahana

class JVMPlatform : Platform {
    override val name: String = "Java ${System.getProperty("java.version")}"
}

actual fun getPlatform(): Platform = JVMPlatform()

actual fun epochMillis(): Long = System.currentTimeMillis()