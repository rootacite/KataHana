package com.acite.katahana

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.transitions.FadeTransition
import com.acite.katahana.ui.home.HomeScreen
import com.acite.katahana.ui.navigation.HanaBackHandler
import com.acite.katahana.ui.navigation.ProvideHanaScreenLifecycle
import com.acite.katahana.ui.settings.SettingsViewModel
import com.acite.katahana.ui.theme.Appearance
import com.acite.katahana.ui.theme.HanaColors
import com.acite.katahana.ui.theme.KataHanaTheme
import dev.zacsweers.metrox.viewmodel.LocalMetroViewModelFactory
import dev.zacsweers.metrox.viewmodel.MetroViewModelFactory
import dev.zacsweers.metrox.viewmodel.metroViewModel

@Composable
fun App(metroVmf: MetroViewModelFactory) {
    CompositionLocalProvider(LocalMetroViewModelFactory provides metroVmf) {
        ProvideViewModelStore {
            ProvideHanaScreenLifecycle {
                val settings = metroViewModel<SettingsViewModel>()
                val appearanceId by settings.appearanceId.collectAsState()
                KataHanaTheme(appearance = Appearance.byId(appearanceId)) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(HanaColors.bgApp)
                            .windowInsetsPadding(WindowInsets.displayCutout),
                    ) {
                        Navigator(HomeScreen(), onBackPressed = null) { navigator ->
                            HanaBackHandler(enabled = navigator.canPop) {
                                navigator.pop()
                            }
                            FadeTransition(navigator)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProvideViewModelStore(content: @Composable () -> Unit) {
    val parent = LocalViewModelStoreOwner.current
    if (parent != null) {
        content()
        return
    }
    val store = remember { ViewModelStore() }
    val owner = remember {
        object : ViewModelStoreOwner {
            override val viewModelStore: ViewModelStore = store
        }
    }
    DisposableEffect(Unit) {
        onDispose { store.clear() }
    }
    CompositionLocalProvider(LocalViewModelStoreOwner provides owner, content = content)
}
