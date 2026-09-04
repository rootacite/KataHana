package com.acite.katahana.engine

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.websocket.WebSockets
import java.net.Proxy
import java.util.concurrent.TimeUnit

internal actual fun createEngineHttpClient(): HttpClient = HttpClient(OkHttp) {
    install(WebSockets)
    engine {
        config {
            proxy(Proxy.NO_PROXY)
            pingInterval(20, TimeUnit.SECONDS)
        }
    }
}
