package com.acite.katahana.ui.engine

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.acite.katahana.ui.Copy
import com.acite.katahana.ui.components.CapsuleButton
import com.acite.katahana.ui.components.HanaField
import com.acite.katahana.ui.session.engineStatusLabel
import com.acite.katahana.ui.components.QuietTextButton
import com.acite.katahana.ui.settings.SettingsViewModel
import com.acite.katahana.ui.theme.HanaColors
import dev.zacsweers.metrox.viewmodel.metroViewModel

class EngineSettingsScreen : Screen {
    @Composable
    override fun Content() {
        EngineSettingsRoute(metroViewModel())
    }
}

@Composable
private fun EngineSettingsRoute(vm: SettingsViewModel) {
    val navigator = LocalNavigator.currentOrThrow
    val name by vm.engineName.collectAsState()
    val url by vm.engineUrl.collectAsState()
    val token by vm.engineToken.collectAsState()
    val play by vm.playVisits.collectAsState()
    val review by vm.reviewVisits.collectAsState()
    val engineStatus by vm.engineStatus.collectAsState()
    val testBusy by vm.testBusy.collectAsState()
    val testMessage by vm.testMessage.collectAsState()
    Column(
        Modifier
            .fillMaxSize()
            .background(HanaColors.bgApp)
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
    ) {
        QuietTextButton(Copy.back) { navigator.pop() }
        Text(Copy.engine, color = HanaColors.text, fontSize = 28.sp)
        Spacer(Modifier.height(16.dp))
        HanaField(Copy.engineName, name, vm::setEngineName)
        Spacer(Modifier.height(10.dp))
        HanaField(Copy.url, url, vm::setEngineUrl, placeholder = "ws://127.0.0.1:2080")
        Spacer(Modifier.height(10.dp))
        HanaField(Copy.token, token, vm::setEngineToken)
        Spacer(Modifier.height(10.dp))
        HanaField(
            Copy.playVisits,
            play.toString(),
            onChange = { it.toIntOrNull()?.let(vm::setPlayVisits) },
            keyboard = KeyboardType.Number,
        )
        Spacer(Modifier.height(10.dp))
        HanaField(
            Copy.reviewVisits,
            review.toString(),
            onChange = { it.toIntOrNull()?.let(vm::setReviewVisits) },
            keyboard = KeyboardType.Number,
        )
        Spacer(Modifier.height(20.dp))
        CapsuleButton(
            if (testBusy) Copy.testingConnection else Copy.testConnection,
            onClick = vm::testConnection,
            modifier = Modifier.fillMaxWidth(),
            enabled = !testBusy,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            engineStatusLabel(engineStatus),
            color = HanaColors.textDim,
            fontSize = 13.sp,
        )
        if (testMessage != null) {
            Spacer(Modifier.height(6.dp))
            Text(testMessage.orEmpty(), color = HanaColors.accentLilac, fontSize = 13.sp)
        }
        Spacer(Modifier.height(8.dp))
        Text(
            Copy.engineOfflineHint,
            color = HanaColors.textDim,
            fontSize = 13.sp,
        )
    }
}


