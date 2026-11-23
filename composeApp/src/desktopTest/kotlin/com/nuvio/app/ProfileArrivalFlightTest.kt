package com.nuvio.app

import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Rule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ProfileArrivalFlightTest {
    @get:Rule val compose = createComposeRule()

    @Test fun cachedDestinationStartsImmediatelyButDoesNotSkipFlight() {
        compose.mainClock.autoAdvance = false
        var finished = false
        var progress = -1f
        compose.setContent {
            ProfileArrivalFlight(ready = true, onFinished = { finished = true }) { value ->
                SideEffect { progress = value }
            }
        }
        compose.mainClock.advanceTimeBy(150)
        compose.runOnIdle { assertTrue(progress > 0f && !finished) }
        compose.mainClock.advanceTimeBy(1_600)
        compose.runOnIdle { assertEquals(1f, progress); assertTrue(finished) }
    }

    @Test fun waitsForReadinessThenFinishesWithoutRestarting() {
        compose.mainClock.autoAdvance = false
        var ready by mutableStateOf(false)
        var showing by mutableStateOf(true)
        var progress = -1f
        var disposed = false
        compose.setContent {
            if (showing) ProfileArrivalFlight(ready, onFinished = { showing = false }) { value ->
                SideEffect { progress = value }
                DisposableEffect(Unit) { onDispose { disposed = true } }
            }
        }
        compose.mainClock.advanceTimeBy(10_000)
        compose.runOnIdle { assertEquals(0f, progress); ready = true }
        compose.mainClock.advanceTimeBy(400)
        val started = compose.runOnIdle { assertTrue(progress > 0f && progress < 1f); ready = false; progress }
        compose.mainClock.advanceTimeBy(500)
        compose.runOnIdle { assertTrue(progress > started && progress < 1f); assertTrue(!disposed) }
        compose.mainClock.advanceTimeBy(1_500)
        compose.runOnIdle { assertTrue(disposed) }
    }
}
