package com.acite.katahana.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.acite.katahana.domain.AiStyle
import com.acite.katahana.domain.GameConfig
import com.acite.katahana.domain.PlayMode
import com.acite.katahana.domain.rankLabel
import com.acite.katahana.domain.rankLongLabel
import com.acite.katahana.ui.Copy
import com.acite.katahana.ui.components.CapsuleButton
import com.acite.katahana.ui.components.CapsuleChoice
import com.acite.katahana.ui.components.ChoiceRow
import com.acite.katahana.ui.theme.HanaColors
import kotlin.math.roundToInt

@Composable
fun NewGameSheet(
    onStart: (GameConfig) -> Unit,
    modifier: Modifier = Modifier,
    initial: GameConfig = GameConfig(),
) {
    var size by remember(initial) { mutableIntStateOf(initial.boardSize) }
    var komi by remember(initial) { mutableStateOf(initial.komi) }
    var mode by remember(initial) { mutableStateOf(initial.mode) }
    var rankKyu by remember(initial) { mutableIntStateOf(initial.rankKyu) }
    var humanPlaysBlack by remember(initial) { mutableStateOf(initial.humanPlaysBlack) }
    var aiStyle by remember(initial) { mutableStateOf(initial.aiStyle) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(Copy.newGame, color = HanaColors.text, fontSize = 22.sp)
        Label(Copy.boardSize)
        ChoiceRow {
            for (n in listOf(9, 13, 19)) {
                CapsuleChoice(
                    text = "${n}×${n}",
                    selected = size == n,
                    onClick = { size = n },
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Label(Copy.komi)
        ChoiceRow {
            for (k in listOf(6.5f, 7.5f, 0f)) {
                CapsuleChoice(
                    text = k.toString(),
                    selected = komi == k,
                    onClick = { komi = k },
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Label(Copy.match)
        ChoiceRow {
            CapsuleChoice(Copy.hvh, mode == PlayMode.HumanVsHuman, { mode = PlayMode.HumanVsHuman }, Modifier.weight(1f))
            CapsuleChoice(Copy.hvai, mode == PlayMode.HumanVsAi, { mode = PlayMode.HumanVsAi }, Modifier.weight(1f))
        }
        if (mode == PlayMode.HumanVsAi) {
            Label(Copy.aiStyle)
            ChoiceRow {
                CapsuleChoice(Copy.rank, aiStyle == AiStyle.Rank, { aiStyle = AiStyle.Rank }, Modifier.weight(1f))
                CapsuleChoice(Copy.fullStrength, aiStyle == AiStyle.Full, { aiStyle = AiStyle.Full }, Modifier.weight(1f))
            }
            if (aiStyle == AiStyle.Rank) {
            Label("${Copy.rank}  ${rankLongLabel(rankKyu)}  (${rankLabel(rankKyu)})")
            Slider(
                value = (15 - rankKyu).toFloat(),
                onValueChange = { rankKyu = 15 - it.roundToInt() },
                valueRange = 0f..17f,
                steps = 16,
                colors = SliderDefaults.colors(
                    thumbColor = HanaColors.accentPink,
                    activeTrackColor = HanaColors.accentPink,
                    inactiveTrackColor = HanaColors.stroke,
                ),
            )
            }
            Label(Copy.youPlay)
            ChoiceRow {
                CapsuleChoice(Copy.black, humanPlaysBlack, { humanPlaysBlack = true }, Modifier.weight(1f))
                CapsuleChoice(Copy.white, !humanPlaysBlack, { humanPlaysBlack = false }, Modifier.weight(1f))
            }
        }
        Spacer(Modifier.height(4.dp))
        CapsuleButton(
            text = Copy.start,
            onClick = {
                onStart(
                    GameConfig(
                        boardSize = size,
                        komi = komi,
                        mode = mode,
                        rankKyu = rankKyu,
                        humanPlaysBlack = humanPlaysBlack,
                        aiStyle = aiStyle,
                    ),
                )
            },
            modifier = Modifier.fillMaxWidth(),
            emphasized = true,
        )
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun Label(text: String) {
    Text(text, color = HanaColors.textDim, fontSize = 13.sp)
}
