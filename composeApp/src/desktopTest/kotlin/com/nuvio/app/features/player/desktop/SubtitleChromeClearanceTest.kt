package com.nuvio.app.features.player.desktop

import kotlin.test.Test
import kotlin.test.assertEquals

class SubtitleChromeClearanceTest {
    @Test fun clearanceIsTransientAndRelativeToConfiguredPosition() {
        for (configured in listOf(-10, 0, 15, 60)) {
            assertEquals(configured.coerceAtLeast(0) + 25, liftedSubtitleOffset(configured, 25))
            assertEquals(configured.coerceAtLeast(0) + 5, liftedSubtitleOffset(configured, 5))
            assertEquals(configured, liftedSubtitleOffset(configured, 0))
        }
        assertEquals(100, liftedSubtitleOffset(80, 100))
    }

    @Test fun liftNeverExceedsTheCap() {
        assertEquals(100, liftedSubtitleOffset(0, 100))
        assertEquals(100, liftedSubtitleOffset(100, 100))
    }
}
