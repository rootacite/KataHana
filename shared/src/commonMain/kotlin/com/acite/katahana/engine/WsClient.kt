package com.acite.katahana.engine

import io.ktor.client.plugins.websocket.webSocket
import io.ktor.http.takeFrom
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

internal const val ENGINE_HANDSHAKE_MS = 4_000L
internal const val ENGINE_AWAIT_ONLINE_MS = 20_000L

internal fun parseEngineUrls(raw: String): List<String> =
    raw.split(';').map { it.trim() }.filter { it.isNotEmpty() }

internal fun normalizeEngineUrl(raw: String, token: String = ""): String {
    var url = raw.trim()
    if (url.isEmpty()) return url
    if (!url.startsWith("ws://") && !url.startsWith("wss://")) {
        url = "ws://$url"
    }
    val schemeEnd = url.indexOf("://").let { if (it < 0) 0 else it + 3 }
    val rest = url.substring(schemeEnd)
    if ('/' !in rest) url = "$url/"
    val trimmedToken = token.trim()
    if (trimmedToken.isNotEmpty()) {
        val sep = if ('?' in url) '&' else '?'
        url = "${url}${sep}token=$trimmedToken"
    }
    return url
}

internal class WsClient {
    private val http = createEngineHttpClient()

    suspend fun connect(
        url: String,
        token: String,
        outgoing: ReceiveChannel<String>,
        onOpen: suspend () -> Unit = {},
        onText: suspend (String) -> Unit,
    ) {
        val target = normalizeEngineUrl(url, token)
        http.webSocket(request = { this.url.takeFrom(target) }) {
            onOpen()
            coroutineScope {
                val sender = launch {
                    for (text in outgoing) {
                        send(Frame.Text(text))
                    }
                }
                try {
                    for (frame in incoming) {
                        when (frame) {
                            is Frame.Text -> onText(frame.readText())
                            is Frame.Close -> break
                            else -> Unit
                        }
                    }
                } finally {
                    sender.cancel()
                }
            }
        }
    }
}
