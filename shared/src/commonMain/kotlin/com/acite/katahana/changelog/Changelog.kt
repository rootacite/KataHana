package com.acite.katahana.changelog

data class ChangelogEntry(
    val hash: String,
    val date: String,
    val subject: String,
) {
    val kind: String?
        get() = KIND_PREFIX.matchEntire(subject)?.groupValues?.get(1)

    val title: String
        get() = KIND_PREFIX.matchEntire(subject)?.groupValues?.get(2) ?: subject
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
    val hash = line.substring(0, first).trim()
    val date = line.substring(first + 1, second).trim()
    val subject = line.substring(second + 1).trim()
    if (hash.isEmpty() || subject.isEmpty()) return null
    return ChangelogEntry(hash = hash, date = date, subject = subject)
}

private val KIND_PREFIX = Regex("^\\[([^\\]]+)\\]\\s*(.*)$")
