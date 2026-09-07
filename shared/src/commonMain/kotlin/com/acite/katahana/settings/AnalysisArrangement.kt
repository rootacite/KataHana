package com.acite.katahana.settings

const val ANALYSIS_SIDE_WIDTH_DP_DEFAULT = 280
const val ANALYSIS_SIDE_WIDTH_DP_MIN = 168

enum class AnalysisArrangement(val id: String) {
    Auto("auto"),
    Rows("rows"),
    Columns("columns"),
    ;

    companion object {
        val Default = Rows

        fun fromId(id: String?): AnalysisArrangement =
            entries.firstOrNull { it.id == id } ?: Default
    }
}

data class AnalysisColWeights(
    val tree: Float = 1.15f,
    val graph: Float = 1f,
    val quality: Float = 0.9f,
) {
    operator fun get(index: Int): Float = when (index) {
        0 -> tree
        1 -> graph
        else -> quality
    }

    companion object {
        val Default = AnalysisColWeights()
        val DefaultRows = AnalysisColWeights(1.4f, 1f, 0.8f)

        fun parse(
            raw: String?,
            fallback: AnalysisColWeights = Default,
        ): AnalysisColWeights {
            if (raw.isNullOrBlank()) return fallback
            val parts = raw.split(',')
            if (parts.size != 3) return fallback
            val a = parts[0].trim().toFloatOrNull() ?: return fallback
            val b = parts[1].trim().toFloatOrNull() ?: return fallback
            val c = parts[2].trim().toFloatOrNull() ?: return fallback
            if (a <= 0f || b <= 0f || c <= 0f) return fallback
            return AnalysisColWeights(a, b, c)
        }

        fun format(value: AnalysisColWeights): String =
            "${value.tree},${value.graph},${value.quality}"
    }
}
