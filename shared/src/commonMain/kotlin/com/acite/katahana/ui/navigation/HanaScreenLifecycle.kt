package com.acite.katahana.ui.navigation

import androidx.compose.runtime.Composable

/**
 * On Android, replaces Voyager 1.0.1's screen lifecycle owner. That owner crashes on
 * back: FadeTransition keeps the leaving screen composed after it is already DESTROYED,
 * then emitOnStopEvents tries DESTROYED → STARTED.
 */
@Composable
internal expect fun ProvideHanaScreenLifecycle(content: @Composable () -> Unit)
