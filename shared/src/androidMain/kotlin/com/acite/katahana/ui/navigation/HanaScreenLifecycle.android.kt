package com.acite.katahana.ui.navigation

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.ProvidedValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.savedstate.compose.LocalSavedStateRegistryOwner
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.HasDefaultViewModelProviderFactory
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.SAVED_STATE_REGISTRY_OWNER_KEY
import androidx.lifecycle.SavedStateViewModelFactory
import androidx.lifecycle.VIEW_MODEL_STORE_OWNER_KEY
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.enableSavedStateHandles
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.MutableCreationExtras
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import cafe.adriel.voyager.core.lifecycle.LocalNavigatorScreenLifecycleProvider
import cafe.adriel.voyager.core.lifecycle.NavigatorScreenLifecycleProvider
import cafe.adriel.voyager.core.lifecycle.ScreenLifecycleContentProvider
import cafe.adriel.voyager.core.lifecycle.ScreenLifecycleOwner
import cafe.adriel.voyager.core.lifecycle.ScreenLifecycleStore
import cafe.adriel.voyager.core.screen.Screen
import java.util.concurrent.atomic.AtomicReference

@Composable
internal actual fun ProvideHanaScreenLifecycle(content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalNavigatorScreenLifecycleProvider provides HanaScreenLifecycleProvider,
        content = content,
    )
}

private object HanaScreenLifecycleProvider : NavigatorScreenLifecycleProvider {
    override fun provide(screen: Screen): List<ScreenLifecycleContentProvider> =
        listOf(HanaScreenLifecycleOwner.get(screen))
}

/**
 * Voyager 1.0.1's AndroidScreenLifecycleOwner without the DESTROYED → STARTED crash.
 * FadeTransition keeps a popped screen composed after StepDisposableEffect has already
 * dispatched ON_DESTROY; stop events must no-op once the owner is terminal.
 */
private class HanaScreenLifecycleOwner private constructor() :
    ScreenLifecycleOwner,
    LifecycleOwner,
    ViewModelStoreOwner,
    SavedStateRegistryOwner,
    HasDefaultViewModelProviderFactory {

    override val lifecycle: LifecycleRegistry = LifecycleRegistry(this)
    override val viewModelStore: ViewModelStore = ViewModelStore()

    private val atomicContext = AtomicReference<Context>()
    private val atomicParentLifecycleOwner = AtomicReference<LifecycleOwner>()
    private val controller = SavedStateRegistryController.create(this)
    private var isCreated: Boolean by mutableStateOf(false)

    override val savedStateRegistry: SavedStateRegistry
        get() = controller.savedStateRegistry

    override val defaultViewModelProviderFactory: ViewModelProvider.Factory
        get() = SavedStateViewModelFactory(
            application = atomicContext.get()?.applicationContext?.getApplication(),
            owner = this,
        )

    override val defaultViewModelCreationExtras: CreationExtras
        get() = MutableCreationExtras().apply {
            val application = atomicContext.get()?.applicationContext?.getApplication()
            if (application != null) {
                set(AndroidViewModelFactory.APPLICATION_KEY, application)
            }
            set(SAVED_STATE_REGISTRY_OWNER_KEY, this@HanaScreenLifecycleOwner)
            set(VIEW_MODEL_STORE_OWNER_KEY, this@HanaScreenLifecycleOwner)
        }

    init {
        controller.performAttach()
        enableSavedStateHandles()
    }

    private fun onCreate(savedState: Bundle?) {
        check(!isCreated) { "onCreate already called" }
        isCreated = true
        controller.performRestore(savedState)
        lifecycle.safeHandleLifecycleEvent(Lifecycle.Event.ON_CREATE)
    }

    private fun emitOnStartEvents() {
        lifecycle.safeHandleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycle.safeHandleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    private fun emitOnStopEvents() {
        lifecycle.safeHandleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycle.safeHandleLifecycleEvent(Lifecycle.Event.ON_STOP)
    }

    @Composable
    override fun ProvideBeforeScreenContent(
        provideSaveableState: @Composable (suffixKey: String, content: @Composable () -> Unit) -> Unit,
        content: @Composable () -> Unit,
    ) {
        provideSaveableState("lifecycle") {
            LifecycleDisposableEffect()
            val hooks = getHooks()
            CompositionLocalProvider(*hooks.toTypedArray()) {
                content()
            }
        }
    }

    override fun onDispose(screen: Screen) {
        val context = atomicContext.getAndSet(null) ?: return
        val activity = context.getActivity()
        if (activity != null && activity.isChangingConfigurations) return
        viewModelStore.clear()
        lifecycle.safeHandleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    }

    private fun performSave(outState: Bundle) {
        controller.performSave(outState)
    }

    @Composable
    private fun getHooks(): List<ProvidedValue<*>> {
        atomicContext.compareAndSet(null, LocalContext.current)
        atomicParentLifecycleOwner.compareAndSet(null, LocalLifecycleOwner.current)
        return remember(this) {
            listOf(
                LocalLifecycleOwner provides this,
                LocalViewModelStoreOwner provides this,
                LocalSavedStateRegistryOwner provides this,
            )
        }
    }

    private fun registerLifecycleListener(outState: Bundle): () -> Unit {
        val lifecycleOwner = atomicParentLifecycleOwner.get() ?: return { }
        val observer = object : DefaultLifecycleObserver {
            override fun onPause(owner: LifecycleOwner) {
                lifecycle.safeHandleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
            }

            override fun onResume(owner: LifecycleOwner) {
                lifecycle.safeHandleLifecycleEvent(Lifecycle.Event.ON_RESUME)
            }

            override fun onStart(owner: LifecycleOwner) {
                lifecycle.safeHandleLifecycleEvent(Lifecycle.Event.ON_START)
            }

            override fun onStop(owner: LifecycleOwner) {
                lifecycle.safeHandleLifecycleEvent(Lifecycle.Event.ON_STOP)
                performSave(outState)
            }
        }
        val parentLifecycle = lifecycleOwner.lifecycle
        parentLifecycle.addObserver(observer)
        return { parentLifecycle.removeObserver(observer) }
    }

    @Composable
    private fun LifecycleDisposableEffect() {
        val savedState = rememberSaveable { Bundle() }
        if (!isCreated) {
            onCreate(savedState)
        }
        DisposableEffect(this) {
            val unregisterLifecycle = registerLifecycleListener(savedState)
            emitOnStartEvents()
            onDispose {
                unregisterLifecycle()
                performSave(savedState)
                emitOnStopEvents()
            }
        }
    }

    private tailrec fun Context.getActivity(): Activity? = when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.getActivity()
        else -> null
    }

    private tailrec fun Context.getApplication(): Application? = when (this) {
        is Application -> this
        is ContextWrapper -> baseContext.getApplication()
        else -> null
    }

    companion object {
        fun get(screen: Screen): ScreenLifecycleOwner =
            ScreenLifecycleStore.get(screen) { HanaScreenLifecycleOwner() }
    }
}

private fun LifecycleRegistry.safeHandleLifecycleEvent(event: Lifecycle.Event) {
    if (currentState == Lifecycle.State.DESTROYED) return
    if (event == Lifecycle.Event.ON_DESTROY) {
        while (currentState != Lifecycle.State.DESTROYED &&
            currentState != Lifecycle.State.INITIALIZED
        ) {
            val down = Lifecycle.Event.downFrom(currentState) ?: break
            handleLifecycleEvent(down)
        }
        return
    }
    val current = currentState
    if (event != Lifecycle.Event.upFrom(current) && event != Lifecycle.Event.downFrom(current)) {
        return
    }
    handleLifecycleEvent(event)
}
