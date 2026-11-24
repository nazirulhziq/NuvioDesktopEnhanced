package com.nuvio.app.features.player.desktop

import kotlin.test.Test
import kotlin.test.assertEquals

class SubtitleChromeClearanceTest {
    @Test fun clearanceIsTransientAndRelativeToConfiguredPosition() {
        for (configured in listOf(-10, 0, 15, 60)) {
            assertEquals(configured.coerceAtLeast(0) + 50, liftedSubtitleOffset(configured, 25))
            assertEquals(configured.coerceAtLeast(0) + 10, liftedSubtitleOffset(configured, 5))
            assertEquals(configured, liftedSubtitleOffset(configured, 0))
        }
        assertEquals(200, liftedSubtitleOffset(80, 100))
    }
}
