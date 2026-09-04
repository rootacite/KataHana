package com.acite.katahana

import com.acite.katahana.domain.rankLabel
import kotlin.test.Test
import kotlin.test.assertEquals

class SharedCommonTest {

    @Test
    fun rankLabelsMatchKyuDanConvention() {
        assertEquals("15k", rankLabel(15))
        assertEquals("5k", rankLabel(5))
        assertEquals("1k", rankLabel(1))
        assertEquals("1d", rankLabel(0))
        assertEquals("2d", rankLabel(-1))
        assertEquals("3d", rankLabel(-2))
    }
}
