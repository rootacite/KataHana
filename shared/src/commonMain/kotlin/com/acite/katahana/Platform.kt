package com.acite.katahana

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform