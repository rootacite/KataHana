package com.acite.katahana.ui.session

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.acite.katahana.domain.PlayerSeat
import com.acite.katahana.domain.SeatKind
import com.acite.katahana.domain.StoneColor
import com.acite.katahana.ui.Copy
import com.acite.katahana.ui.components.CapsuleChoice
import com.acite.katahana.ui.components.ChoiceRow
import com.acite.katahana.ui.components.HanaDialogCard
import com.acite.katahana.ui.components.RankLadder
import com.acite.katahana.ui.theme.hanaColors
import dev.chrisbanes.haze.HazeState

@Composable
fun SeatDialog(
    color: StoneColor,
    seat: PlayerSeat,
    onChange: (PlayerSeat) -> Unit,
    onDismiss: () -> Unit,
    hazeState: HazeState,
) {
    val title = if (color == StoneColor.Black) Copy.black else Copy.white
    HanaDialogCard(onDismiss = onDismiss, hazeState = hazeState, slideFromBottom = true) {
        Text(title, color = hanaColors.text, fontSize = 18.sp)
        Spacer(Modifier.height(14.dp))
        ChoiceRow {
            SeatKindChoice(Copy.playerHuman, SeatKind.Human, seat, onChange, Modifier.weight(1f))
            SeatKindChoice(Copy.playerRank, SeatKind.Rank, seat, onChange, Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))
        ChoiceRow {
            SeatKindChoice(Copy.playerHumanLike, SeatKind.HumanLike, seat, onChange, Modifier.weight(1f))
            SeatKindChoice(Copy.playerKatago, SeatKind.Full, seat, onChange, Modifier.weight(1f))
        }
        if (seat.kind != SeatKind.Human) {
            Spacer(Modifier.height(14.dp))
            RankLadder(
                godlike = seat.kind == SeatKind.Full,
                rankKyu = seat.rankKyu,
                onRankKyu = { onChange(seat.copy(rankKyu = it)) },
            )
        }
        if (seat.kind == SeatKind.Full) {
            Spacer(Modifier.height(4.dp))
            Text(Copy.fullKatagoTease, color = hanaColors.accentPink, fontSize = 14.sp)
        }
    }
}

@Composable
private fun SeatKindChoice(
    label: String,
    kind: SeatKind,
    seat: PlayerSeat,
    onChange: (PlayerSeat) -> Unit,
    modifier: Modifier = Modifier,
) {
    CapsuleChoice(
        text = label,
        selected = seat.kind == kind,
        onClick = { onChange(seat.copy(kind = kind)) },
        modifier = modifier,
    )
}
