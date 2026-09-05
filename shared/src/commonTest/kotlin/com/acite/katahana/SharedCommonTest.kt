package com.acite.katahana

import com.acite.katahana.domain.AiStyle
import com.acite.katahana.domain.SeatKind
import com.acite.katahana.domain.humanSlProfile
import com.acite.katahana.domain.parseAiStyle
import com.acite.katahana.domain.parseSeatKind
import com.acite.katahana.domain.rankLabel
import com.acite.katahana.domain.toSeatKind
import com.acite.katahana.domain.toStorageId
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

    @Test
    fun humanSlProfilesMatchKataGo() {
        assertEquals("preaz_5k", humanSlProfile(5))
        assertEquals("preaz_15k", humanSlProfile(15))
        assertEquals("preaz_1d", humanSlProfile(0))
        assertEquals("preaz_3d", humanSlProfile(-2))
    }

    @Test
    fun aiStyleStorageRoundTrips() {
        assertEquals(AiStyle.Human, parseAiStyle(null))
        assertEquals(AiStyle.Human, parseAiStyle("human"))
        assertEquals(AiStyle.Rank, parseAiStyle("rank"))
        assertEquals(AiStyle.Full, parseAiStyle("full"))
        assertEquals("human", AiStyle.Human.toStorageId())
        assertEquals("rank", AiStyle.Rank.toStorageId())
        assertEquals("full", AiStyle.Full.toStorageId())
    }

    @Test
    fun seatKindStorageRoundTrips() {
        assertEquals(SeatKind.Human, parseSeatKind(null))
        assertEquals(SeatKind.Human, parseSeatKind("human"))
        assertEquals(SeatKind.Rank, parseSeatKind("rank"))
        assertEquals(SeatKind.HumanLike, parseSeatKind("humanlike"))
        assertEquals(SeatKind.Full, parseSeatKind("full"))
        assertEquals("human", SeatKind.Human.toStorageId())
        assertEquals("rank", SeatKind.Rank.toStorageId())
        assertEquals("humanlike", SeatKind.HumanLike.toStorageId())
        assertEquals("full", SeatKind.Full.toStorageId())
        assertEquals(SeatKind.HumanLike, AiStyle.Human.toSeatKind())
        assertEquals(SeatKind.Rank, AiStyle.Rank.toSeatKind())
        assertEquals(SeatKind.Full, AiStyle.Full.toSeatKind())
    }
}
