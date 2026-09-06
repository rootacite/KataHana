package com.acite.katahana.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.acite.katahana.domain.rankLabel
import com.acite.katahana.ui.Copy
import com.acite.katahana.ui.theme.hanaColors
import kotlin.math.roundToInt

@Composable
fun RankCard(
    text: String,
    emphasized: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val colors = hanaColors
    val bg = if (emphasized) colors.accentPink.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.05f)
    val border = if (emphasized) colors.accentPink.copy(alpha = 0.40f) else Color.White.copy(alpha = 0.10f)
    val fg = if (emphasized) colors.text else colors.accentLilac
    Box(
        modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(
            text,
            color = fg,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
fun RankLadder(
    godlike: Boolean,
    rankKyu: Int,
    onRankKyu: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = hanaColors
    val label = if (godlike) Copy.katagoRankGod else rankLabel(rankKyu)
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        RankCard(label, emphasized = godlike)
        Slider(
            value = if (godlike) 17f else (15 - rankKyu).toFloat(),
            onValueChange = { value ->
                if (!godlike) onRankKyu(15 - value.roundToInt())
            },
            enabled = !godlike,
            valueRange = 0f..17f,
            steps = 16,
            colors = SliderDefaults.colors(
                thumbColor = colors.accentPink,
                activeTrackColor = colors.accentPink,
                inactiveTrackColor = colors.stroke,
                disabledThumbColor = colors.accentPink,
                disabledActiveTrackColor = colors.accentPink,
                disabledInactiveTrackColor = colors.stroke.copy(alpha = 0.55f),
            ),
        )
    }
}
