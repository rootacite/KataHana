package com.acite.katahana.settings

import kotlin.test.Test
import kotlin.test.assertEquals

class OwnershipStyleTest {

    @Test
    fun fromIdFallsBackToBlocks() {
        assertEquals(OwnershipStyle.Blocks, OwnershipStyle.fromId(null))
        assertEquals(OwnershipStyle.Blocks, OwnershipStyle.fromId("nope"))
        assertEquals(OwnershipStyle.Blocks, OwnershipStyle.fromId("blocks"))
        assertEquals(OwnershipStyle.Fog, OwnershipStyle.fromId("fog"))
        assertEquals(OwnershipStyle.Constellation, OwnershipStyle.fromId("constellation"))
    }
}
