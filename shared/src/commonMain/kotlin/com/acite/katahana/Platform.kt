package com.acite.katahana

interface Platform {
    val name: String
    val isMobile: Boolean
}

expect fun getPlatform(): Platform

expect fun epochMillis(): Long