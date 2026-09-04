package com.acite.katahana.ai

import com.acite.katahana.domain.Point
import com.acite.katahana.domain.StoneColor
import com.acite.katahana.settings.QualityThresholds

const val SHALLOW_VISITS = 80
const val QUALITY_RECENT = 3

enum class QualityBand {
    Blunder,
    BigMistake,
    Mistake,
    Inaccuracy,
    Fair,
    Good,
    Shallow,
}

data class QualityMark(
    val point: Point,
    val pointsLost: Double,
    val visits: Int,
    val band: QualityBand,
    val color: StoneColor,
)

fun qualityBand(
    pointsLost: Double,
    visits: Int,
    thresholds: QualityThresholds = QualityThresholds(),
): QualityBand {
    if (visits in 1 until SHALLOW_VISITS) return QualityBand.Shallow
    return bandForLoss(pointsLost, thresholds)
}

fun bandForLoss(
    pointsLost: Double,
    thresholds: QualityThresholds = QualityThresholds(),
): QualityBand = when {
    pointsLost >= thresholds.blunder -> QualityBand.Blunder
    pointsLost >= thresholds.bigMistake -> QualityBand.BigMistake
    pointsLost >= thresholds.mistake -> QualityBand.Mistake
    pointsLost >= thresholds.inaccuracy -> QualityBand.Inaccuracy
    pointsLost >= thresholds.fair -> QualityBand.Fair
    else -> QualityBand.Good
}

fun recentQualities(marks: List<QualityMark>, limit: Int = QUALITY_RECENT): List<QualityMark> =
    marks.filter { it.color == StoneColor.Black }.takeLast(limit) +
        marks.filter { it.color == StoneColor.White }.takeLast(limit)


