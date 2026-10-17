package com.androbito.tilfaz.feature.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect

/**
 * Runs [action] when the screen is no longer in front (Home pressed, an assistant or another app opened). A film must not
 * keep playing out of sight: Android mutes audio from a stopped app, so it would carry on silently.
 */
@Composable
fun OnStopEffect(action: () -> Unit) {
    val current = rememberUpdatedState(action)
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { current.value() }
}
