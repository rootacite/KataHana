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
        assertEquals(emptyList(), entry?.tags)
    }

    @Test
    fun leadingParentheticalTagsSitBesideKind() {
        val entry = parseChangelogLine("fff6758|2026-09-05|(acite) [doc] issue1")
        assertEquals(listOf("acite"), entry?.tags)
        assertEquals("doc", entry?.kind)
        assertEquals("issue1", entry?.title)
    }

    @Test
    fun multipleTagsBeforeTitle() {
        val entry = parseChangelogLine("abc1234|2026-01-02|(alice) (bob) Hello there")
        assertEquals(listOf("alice", "bob"), entry?.tags)
        assertNull(entry?.kind)
        assertEquals("Hello there", entry?.title)
    }

    @Test
    fun gitTagsRideTheFourthField() {
        val entry = parseChangelogLine("aca8624|2026-09-05|[Feat] Overlay|v1.0,v1.0.0")
        assertEquals(listOf("v1.0", "v1.0.0"), entry?.gitTags)
        assertEquals("Feat", entry?.kind)
        assertEquals("Overlay", entry?.title)
    }

    @Test
    fun decorationsExtractOnlyGitTags() {
        assertEquals(
            listOf("v1.0"),
            parseGitDecorations("HEAD -> main, tag: v1.0, origin/main"),
        )
        assertEquals(
            listOf("v1.0", "release"),
            parseGitDecorations("tag: v1.0, tag: release"),
        )
        assertEquals(emptyList(), parseGitDecorations("HEAD -> main"))
    }

    @Test
    fun displayVersionFallsBackWhenUntagged() {
        assertEquals("v0.1-alpha", displayAppVersion(null))
        assertEquals("v0.1-alpha", displayAppVersion("  "))
        assertEquals("v1.0", displayAppVersion("v1.0"))
        assertEquals("1.0", displayAppVersion("1.0"))
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
