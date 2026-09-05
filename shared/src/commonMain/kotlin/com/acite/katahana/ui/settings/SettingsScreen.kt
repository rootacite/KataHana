package com.acite.katahana.ui.settings

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.acite.katahana.settings.OwnershipStyle
import com.acite.katahana.ui.Copy
import com.acite.katahana.ui.board.OwnershipMotion
import com.acite.katahana.ui.board.drawOwnershipLayer
import com.acite.katahana.ui.board.drawStoneSwatch
import com.acite.katahana.domain.Point
import com.acite.katahana.ui.components.HanaField
import com.acite.katahana.ui.components.HanaSection
import com.acite.katahana.ui.components.ScreenHeader
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
    val ownershipStyle by vm.ownershipStyle.collectAsState()
    val quality by vm.quality.collectAsState()
    val acrylic by vm.drawerAcrylic.collectAsState()
    val tokens = hanaTokens

    Column(
        Modifier
            .fillMaxSize()
            .background(HanaColors.bgApp)
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp),
    ) {
        ScreenHeader(Copy.settings, onBack = { navigator.pop() })
        HanaSection(Copy.appearance, hint = Copy.appearanceHint) {
            Appearance.all.forEach { appearance ->
                val selected = appearance.id == appearanceId
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(tokens.panel)
                        .background(HanaColors.bgPanel)
                        .then(
                            if (selected) {
                                Modifier.border(1.5.dp, HanaColors.accentPink, tokens.panel)
                            } else {
                                Modifier.border(1.dp, HanaColors.stroke, tokens.panel)
                            },
                        )
                        .clickable { vm.setAppearanceId(appearance.id) }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
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
        HanaSection(Copy.drawerFrost, hint = Copy.drawerFrostHint) {
            Text(
                "${(acrylic * 100f).toInt()}%",
                color = HanaColors.accentLilac,
                fontSize = 13.sp,
            )
            Slider(
                value = acrylic,
                onValueChange = vm::setDrawerAcrylic,
                valueRange = 0f..1f,
                colors = SliderDefaults.colors(
                    thumbColor = HanaColors.accentPink,
                    activeTrackColor = HanaColors.accentPink,
                    inactiveTrackColor = HanaColors.stroke,
                ),
            )
        }
        HanaSection(Copy.boardSection) {
            ToggleRow(Copy.confirmMove, confirm, vm::setConfirmMove)
            ToggleRow(Copy.showCoords, coords, vm::setShowCoords)
        }
        HanaSection(Copy.ownershipStyle, hint = Copy.ownershipStyleHint) {
            OwnershipStyle.entries.forEach { style ->
                val selected = style == ownershipStyle
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(tokens.panel)
                        .background(HanaColors.bgPanel)
                        .then(
                            if (selected) {
                                Modifier.border(1.5.dp, HanaColors.accentPink, tokens.panel)
                            } else {
                                Modifier.border(1.dp, HanaColors.stroke, tokens.panel)
                            },
                        )
                        .clickable { vm.setOwnershipStyle(style) }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OwnershipStylePreview(style)
                    Spacer(Modifier.size(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(styleTitle(style), color = HanaColors.text, fontSize = 15.sp)
                        Text(styleBlurb(style), color = HanaColors.textDim, fontSize = 12.sp)
                    }
                }
            }
        }
        HanaSection(Copy.quality, hint = Copy.qualityHint) {
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
        Spacer(Modifier.height(8.dp))
    }
}

private fun styleTitle(style: OwnershipStyle): String = when (style) {
    OwnershipStyle.Blocks -> Copy.ownershipBlocks
    OwnershipStyle.Fog -> Copy.ownershipFog
    OwnershipStyle.Constellation -> Copy.ownershipStars
}

private fun styleBlurb(style: OwnershipStyle): String = when (style) {
    OwnershipStyle.Blocks -> Copy.ownershipBlocksHint
    OwnershipStyle.Fog -> Copy.ownershipFogHint
    OwnershipStyle.Constellation -> Copy.ownershipStarsHint
}

@Composable
private fun OwnershipStylePreview(style: OwnershipStyle) {
    val pulse = rememberInfiniteTransition(label = "ownershipPreview")
    val bounce by pulse.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "previewBounce",
    )
    val drift by pulse.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(9000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "previewDrift",
    )
    val breath by pulse.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "previewBreath",
    )
    val twinkle by pulse.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(14000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "previewTwinkle",
    )
    Canvas(
        Modifier
            .size(64.dp)
            .clip(hanaTokens.panel),
    ) {
        drawRoundRect(
            color = HanaColors.boardBg,
            topLeft = Offset.Zero,
            size = Size(this.size.width, this.size.height),
            cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx()),
        )
        val n = 5
        val inset = this.size.minDimension * 0.16f
        val span = this.size.minDimension - inset * 2f
        val gap = span / (n - 1).toFloat()
        val origin = Offset(
            (this.size.width - span) / 2f,
            (this.size.height - span) / 2f,
        )
        val values = FloatArray(n * n) { i ->
            val x = i % n
            val y = i / n
            val across = 1f - 2f * x / (n - 1).toFloat()
            val fade = 1f - 0.35f * kotlin.math.abs(y - 2) / 2f
            across * fade
        }
        drawOwnershipLayer(
            values = values,
            boardSize = n,
            gap = gap,
            centerOf = { p: Point -> Offset(origin.x + p.x * gap, origin.y + p.y * gap) },
            style = style,
            motion = OwnershipMotion(
                bounce = bounce,
                drift = drift,
                breath = breath,
                twinkle = twinkle,
            ),
        )
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
}
