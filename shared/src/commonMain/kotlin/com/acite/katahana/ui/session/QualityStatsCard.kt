package com.acite.katahana.ui.session

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.acite.katahana.ai.QualityBand
import com.acite.katahana.domain.QualityStats
import com.acite.katahana.domain.STAT_BANDS
import com.acite.katahana.domain.StoneColor
import com.acite.katahana.ui.Copy
import com.acite.katahana.ui.board.qualityDotColor
import com.acite.katahana.ui.theme.HanaColors
import com.acite.katahana.ui.theme.hanaAppearance
import com.acite.katahana.ui.theme.hanaTokens

@Composable
fun QualityStatsCard(
    stats: QualityStats,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val tokens = hanaTokens
    val appearance = hanaAppearance
    Column(
        modifier
            .clip(tokens.card)
            .background(HanaColors.bgPanel)
            .padding(if (compact) 10.dp else 14.dp),
    ) {
        Text(
            Copy.moveQuality,
            color = HanaColors.text,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(8.dp))
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Spacer(Modifier.weight(1f))
            SideHead(appearance.first.fill, Copy.black)
            Spacer(Modifier.width(8.dp))
            SideHead(appearance.second.fill, Copy.white)
        }
        Spacer(Modifier.height(6.dp))
        STAT_BANDS.forEach { band ->
            val black = stats.count(StoneColor.Black, band)
            val white = stats.count(StoneColor.White, band)
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(qualityDotColor(band)),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    bandLabel(band),
                    color = HanaColors.text,
                    fontSize = 12.sp,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                )
                CountCell(black)
                Spacer(Modifier.width(8.dp))
                CountCell(white)
            }
        }
    }
}

@Composable
private fun SideHead(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End,
        modifier = Modifier.width(44.dp),
    ) {
        Box(
            Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color),
        )
        Spacer(Modifier.width(4.dp))
        Text(label, color = HanaColors.textDim, fontSize = 11.sp)
    }
}

@Composable
private fun CountCell(value: Int) {
    Text(
        value.toString(),
        color = if (value == 0) HanaColors.textDim else HanaColors.text,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        textAlign = TextAlign.End,
        modifier = Modifier.width(44.dp),
    )
}

private fun bandLabel(band: QualityBand): String = when (band) {
    QualityBand.Good -> "Good"
    QualityBand.Fair -> "Fair"
    QualityBand.Inaccuracy -> "Inaccuracy"
    QualityBand.Mistake -> "Mistake"
    QualityBand.BigMistake -> Copy.bigMistake
    QualityBand.Blunder -> "Blunder"
    QualityBand.Shallow -> "Shallow"
}
