package com.acite.katahana.settings

enum class AnalysisLayoutMode(val id: String) {
    Auto("auto"),
    Compact("compact"),
    Expanded("expanded"),
    ;

    companion object {
        val Default = Auto

        fun fromId(id: String?): AnalysisLayoutMode =
            entries.firstOrNull { it.id == id } ?: Default
    }
}
