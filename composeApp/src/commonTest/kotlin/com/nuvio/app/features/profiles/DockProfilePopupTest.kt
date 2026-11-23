package com.nuvio.app.features.profiles

import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import kotlin.test.Test
import kotlin.test.assertEquals

class DockProfilePopupTest {
    @Test
    fun popupIsCenteredAboveAvatarAndPivotTracksAvatarWhenClamped() {
        for (direction in LayoutDirection.entries) {
            for ((center, expectedLeft) in listOf(30 to 16, 800 to 670, 970 to 724)) {
                var pivot = -1f
                val provider = ProfilePopupPositionProvider(16, 18, false, centerOnAvatar = true) { _, _, x -> pivot = x }
                val offset = provider.calculatePosition(IntRect(center - 14, 700, center + 14, 728),
                    IntSize(1000, 760), direction, IntSize(260, 360))
                assertEquals(expectedLeft, offset.x)
                assertEquals(682, offset.y + 360)
                assertEquals(center.toFloat(), offset.x + pivot * 260, 0.001f)
            }
        }
    }
}
