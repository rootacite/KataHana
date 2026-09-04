package com.acite.katahana.ui.navigation

import androidx.compose.runtime.Composable

@Composable
internal actual fun ProvideHanaScreenLifecycle(content: @Composable () -> Unit) = content()
