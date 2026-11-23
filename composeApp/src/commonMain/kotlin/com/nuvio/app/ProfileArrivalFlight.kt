package com.nuvio.app

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.first

private const val ProfileFlightMillis = 1600

/** Register the flight on the owner's transition so an ordinary loading overlay also
 * stays composed until its avatar has left, instead of disappearing at fade-out. */
@Composable
internal fun AnimatedVisibilityScope.profileExitFlight(): Float {
    val flight by transition.animateFloat(
        transitionSpec = { tween(ProfileFlightMillis, easing = FastOutSlowInEasing) },
        label = "profile exit",
    ) { state -> if (state == EnterExitState.PostExit) 1f else 0f }
    return flight
}

/** Readiness releases the avatar once; only the last frame releases the overlay. */
@Composable
internal fun ProfileArrivalFlight(
    ready: Boolean,
    onFinished: () -> Unit,
    content: @Composable (Float) -> Unit,
) {
    val latestReady by rememberUpdatedState(ready)
    val latestFinished by rememberUpdatedState(onFinished)
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        snapshotFlow { latestReady }.first { it }
        progress.animateTo(1f, tween(ProfileFlightMillis, easing = FastOutSlowInEasing))
        latestFinished()
    }
    content(progress.value)
}
