package com.nuvio.app.core.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class DesktopDockInteractionTest {
    @Test
    fun pointerExpandsHeroDockOnlyWhenLabelsFitAndTabsRemainClickable() {
        for (width in listOf(320, 640, 1800)) {
            runDesktopComposeUiTest(width = width, height = 400) {
                val pages = listOf("Home", "Search", "Live TV", "Library", "Settings")
                var selected by mutableIntStateOf(0)
                setContent {
                    NuvioTheme(desktopUiScale = 1f) {
                        DesktopNavigationBar(isHeroEnabled = true, items = pages.mapIndexed { index, label ->
                            FloatingNavigationItem(label, selected == index, { selected = index })
                        })
                    }
                }
                val middle = onNodeWithContentDescription("Live TV")
                fun tabWidth() = middle.fetchSemanticsNode().boundsInRoot.width
                val initial = tabWidth()
                middle.performMouseInput { moveTo(center) }
                mainClock.advanceTimeBy(500)
                waitForIdle()
                if (width == 320) assertEquals(initial, tabWidth()) else assertTrue(tabWidth() > initial)
                middle.performMouseInput { click() }
                runOnIdle { assertEquals(2, selected) }
            }
        }
    }
}
