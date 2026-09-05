package com.acite.katahana.changelog

const val DEFAULT_APP_VERSION = "v0.1-alpha"

data class ChangelogEntry(
    val hash: String,
    val date: String,
    val subject: String,
    val gitTags: List<String> = emptyList(),
) {
    val tags: List<String>
    val kind: String?
    val title: String

    init {
        val parsed = parseSubject(subject)
        tags = parsed.tags
        kind = parsed.kind
        title = parsed.title
    }
}

/** Nearest git tag for the corner label. No tags → [DEFAULT_APP_VERSION]. */
fun displayAppVersion(nearestTag: String?): String {
    val tag = nearestTag?.trim().orEmpty()
    return tag.ifEmpty { DEFAULT_APP_VERSION }
}

/** `tag: v1.0, HEAD -> main` from `git log --decorate`. */
fun parseGitDecorations(decorations: String): List<String> {
    if (decorations.isBlank()) return emptyList()
    return decorations.split(',')
        .map { it.trim() }
        .mapNotNull { token ->
            val tag = TAG_DECORATION.matchEntire(token)?.groupValues?.get(1)?.trim()
            tag?.takeIf { it.isNotEmpty() }
        }
}

/** `commitSha|v1.0` from `git for-each-ref refs/tags`. */
fun parseTagRefLine(line: String): Pair<String, String>? {
    val sep = line.indexOf('|')
    if (sep <= 0) return null
    val commit = line.substring(0, sep).trim()
    val tag = line.substring(sep + 1).trim()
    if (commit.isEmpty() || tag.isEmpty()) return null
    return commit to tag
}

internal data class ParsedSubject(
    val tags: List<String>,
    val kind: String?,
    val title: String,
)

/** `(tag) [Kind] title` — leading parenthetical tags, then an optional kind box. */
internal fun parseSubject(subject: String): ParsedSubject {
    var rest = subject.trim()
    val tags = ArrayList<String>()
    while (rest.startsWith("(")) {
        val close = rest.indexOf(')')
        if (close <= 1) break
        val tag = rest.substring(1, close).trim()
        if (tag.isEmpty()) break
        tags += tag
        rest = rest.substring(close + 1).trimStart()
    }
    val kindMatch = KIND_PREFIX.matchEntire(rest)
    return if (kindMatch != null) {
        val leftover = kindMatch.groupValues[2].trim()
        ParsedSubject(tags, kindMatch.groupValues[1], leftover.ifBlank { rest })
    } else {
        ParsedSubject(tags, null, rest.ifBlank { subject.trim() })
    }
}

fun parseChangelog(text: String): List<ChangelogEntry> {
    if (text.isBlank()) return emptyList()
    return text.lineSequence()
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .mapNotNull(::parseChangelogLine)
        .toList()
}

fun parseChangelogLine(line: String): ChangelogEntry? {
    val first = line.indexOf('|')
    if (first <= 0) return null
    val second = line.indexOf('|', first + 1)
    if (second <= first) return null
    val third = line.indexOf('|', second + 1)
    val hash = line.substring(0, first).trim()
    val date = line.substring(first + 1, second).trim()
    val subject: String
    val gitTags: List<String>
    if (third > second) {
        subject = line.substring(second + 1, third).trim()
        gitTags = line.substring(third + 1).split(',').map { it.trim() }.filter { it.isNotEmpty() }
    } else {
        subject = line.substring(second + 1).trim()
        gitTags = emptyList()
    }
    if (hash.isEmpty() || subject.isEmpty()) return null
    return ChangelogEntry(hash = hash, date = date, subject = subject, gitTags = gitTags)
}

private val KIND_PREFIX = Regex("^\\[([^\\]]+)\\]\\s*(.*)$")
private val TAG_DECORATION = Regex("^tag:\\s*(.+)$")
