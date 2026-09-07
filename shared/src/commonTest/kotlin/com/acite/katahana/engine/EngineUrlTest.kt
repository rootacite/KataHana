package com.acite.katahana.engine

import kotlin.test.Test
import kotlin.test.assertEquals

class EngineUrlTest {

    @Test
    fun emptyAndWhitespaceYieldNothing() {
        assertEquals(emptyList(), parseEngineUrls(""))
        assertEquals(emptyList(), parseEngineUrls("   "))
        assertEquals(emptyList(), parseEngineUrls(" ; ; "))
    }

    @Test
    fun singleAddressIsUnchanged() {
        assertEquals(listOf("ws://127.0.0.1:2080"), parseEngineUrls("ws://127.0.0.1:2080"))
        assertEquals(listOf("ws://127.0.0.1:2080"), parseEngineUrls("  ws://127.0.0.1:2080  "))
    }

    @Test
    fun splitsOnSemicolonAndDropsEmptySegments() {
        assertEquals(
            listOf("ws://127.0.0.1:2080", "ws://192.168.1.10:2080"),
            parseEngineUrls("ws://127.0.0.1:2080; ws://192.168.1.10:2080"),
        )
        assertEquals(
            listOf("a", "b", "c"),
            parseEngineUrls("a; b ; ;c"),
        )
        assertEquals(
            listOf("ws://host:1", "ws://host:2"),
            parseEngineUrls(";ws://host:1;ws://host:2;"),
        )
    }
}
