package com.acite.katahana.settings

enum class OwnershipStyle(val id: String) {
    Blocks("blocks"),
    Fog("fog"),
    Constellation("constellation"),
    ;

    companion object {
        val Default = Blocks

        fun fromId(id: String?): OwnershipStyle =
            entries.firstOrNull { it.id == id } ?: Default
    }
}
