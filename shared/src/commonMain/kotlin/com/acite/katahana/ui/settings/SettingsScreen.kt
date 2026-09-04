package com.acite.katahana.ui.settings

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.acite.katahana.ui.Copy
import com.acite.katahana.ui.board.drawStoneSwatch
import com.acite.katahana.ui.components.QuietTextButton
import com.acite.katahana.ui.engine.HanaField
import com.acite.katahana.ui.theme.Appearance
import com.acite.katahana.ui.theme.HanaColors
import com.acite.katahana.ui.theme.StoneSwatch
import com.acite.katahana.ui.theme.hanaTokens
import dev.zacsweers.metrox.viewmodel.metroViewModel

class SettingsScreen : Screen {
    @Composable
    override fun Content() {
        SettingsRoute(metroViewModel())
    }
}

@Composable
private fun SettingsRoute(vm: SettingsViewModel) {
    val navigator = LocalNavigator.currentOrThrow
    val confirm by vm.confirmMove.collectAsState()
    val coords by vm.showCoords.collectAsState()
    val appearanceId by vm.appearanceId.collectAsState()
    val quality by vm.quality.collectAsState()
    val tokens = hanaTokens

    Column(
        Modifier
            .fillMaxSize()
            .background(HanaColors.bgApp)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
    ) {
        QuietTextButton(Copy.back) { navigator.pop() }
        Text(Copy.settings, color = HanaColors.text, fontSize = 28.sp)
        Spacer(Modifier.height(20.dp))
        Text(Copy.appearance, color = HanaColors.text, fontSize = 18.sp)
        Spacer(Modifier.height(4.dp))
        Text(Copy.appearanceHint, color = HanaColors.textDim, fontSize = 13.sp)
        Spacer(Modifier.height(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Appearance.all.forEach { appearance ->
                val selected = appearance.id == appearanceId
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(tokens.card)
                        .background(HanaColors.bgCard)
                        .then(
                            if (selected) {
                                Modifier.border(1.5.dp, HanaColors.accentPink, tokens.card)
                            } else {
                                Modifier.border(1.dp, HanaColors.stroke, tokens.card)
                            },
                        )
                        .clickable { vm.setAppearanceId(appearance.id) }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    StonePairPreview(appearance)
                    Spacer(Modifier.size(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(appearance.label, color = HanaColors.text, fontSize = 15.sp)
                        Text(appearance.blurb, color = HanaColors.textDim, fontSize = 12.sp)
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
        ToggleRow(Copy.confirmMove, confirm, vm::setConfirmMove)
        Spacer(Modifier.height(8.dp))
        ToggleRow(Copy.showCoords, coords, vm::setShowCoords)
        Spacer(Modifier.height(24.dp))
        Text(Copy.quality, color = HanaColors.text, fontSize = 18.sp)
        Spacer(Modifier.height(4.dp))
        Text(Copy.qualityHint, color = HanaColors.textDim, fontSize = 13.sp)
        Spacer(Modifier.height(12.dp))
        ThresholdField("Blunder (≥ purple)", quality.blunder) {
            vm.setQuality(quality.copy(blunder = it))
        }
        ThresholdField("Big mistake", quality.bigMistake) {
            vm.setQuality(quality.copy(bigMistake = it))
        }
        ThresholdField("Mistake", quality.mistake) {
            vm.setQuality(quality.copy(mistake = it))
        }
        ThresholdField("Inaccuracy", quality.inaccuracy) {
            vm.setQuality(quality.copy(inaccuracy = it))
        }
        ThresholdField("Fair", quality.fair) {
            vm.setQuality(quality.copy(fair = it))
        }
    }
}

@Composable
private fun StonePairPreview(appearance: Appearance) {
    Row(horizontalArrangement = Arrangement.spacedBy((-6).dp), verticalAlignment = Alignment.CenterVertically) {
        StonePreview(appearance.first)
        StonePreview(appearance.second)
    }
}

@Composable
private fun StonePreview(swatch: StoneSwatch) {
    Canvas(Modifier.size(28.dp)) {
        val r = size.minDimension / 2f * 0.92f
        drawStoneSwatch(swatch, Offset(size.width / 2f, size.height / 2f), r)
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = HanaColors.text, modifier = Modifier.weight(1f), fontSize = 16.sp)
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = HanaColors.text,
                checkedTrackColor = HanaColors.accentPink,
                uncheckedThumbColor = HanaColors.textDim,
                uncheckedTrackColor = HanaColors.stroke,
            ),
        )
    }
}

@Composable
private fun ThresholdField(label: String, value: Float, onChange: (Float) -> Unit) {
    HanaField(
        label = label,
        value = value.toString(),
        onChange = { it.toFloatOrNull()?.let(onChange) },
        keyboard = KeyboardType.Decimal,
    )
    Spacer(Modifier.height(8.dp))
}
