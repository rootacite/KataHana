package com.acite.katahana.changelog

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ChangelogParseTest {
    @Test
    fun parsesHashDateSubject() {
        val entry = parseChangelogLine("aca8624|2026-09-05|[Feat] Dead-stone overlay")
        assertEquals("aca8624", entry?.hash)
        assertEquals("2026-09-05", entry?.date)
        assertEquals("[Feat] Dead-stone overlay", entry?.subject)
        assertEquals("Feat", entry?.kind)
        assertEquals("Dead-stone overlay", entry?.title)
    }

    @Test
    fun subjectWithoutKindStaysWhole() {
        val entry = parseChangelogLine("abc1234|2026-01-01|Init the repo")
        assertEquals("Init the repo", entry?.title)
        assertNull(entry?.kind)
    }

    @Test
    fun skipsBrokenLines() {
        val parsed = parseChangelog(
            """
            aca8624|2026-09-05|[Fix] Back crash
            not-a-line
            |missing-hash|x
            def5678|2026-09-04|Hello
            """.trimIndent(),
        )
        assertEquals(2, parsed.size)
        assertEquals("Fix", parsed[0].kind)
        assertEquals("Hello", parsed[1].title)
    }
}
