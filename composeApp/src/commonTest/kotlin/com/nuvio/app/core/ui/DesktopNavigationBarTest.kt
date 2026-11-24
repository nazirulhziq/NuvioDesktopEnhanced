package com.nuvio.app.core.ui

import com.nuvio.app.features.settings.DesktopNavigationLayout
import com.nuvio.app.features.settings.NavBarStyle
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals

class DesktopNavigationBarTest {
    @Test
    fun desktopLayoutPreferencesKeepTopBarAndSidebar() {
        assertEquals(DesktopNavigationLayout.TopBar, DesktopNavigationLayout.fromName("TopBar"))
        assertEquals(DesktopNavigationLayout.TopBar, DesktopNavigationLayout.fromName("topbar"))
        assertEquals(DesktopNavigationLayout.Sidebar, DesktopNavigationLayout.fromName("Sidebar"))
        assertEquals(DesktopNavigationLayout.Default, DesktopNavigationLayout.fromName(null))
    }

    @Test
    fun adaptiveHomeWithHeroExpandsForHoverAndProfilePopup() {
        assertEquals(0f, labels(hero = true))
        assertEquals(1f, labels(hero = true, hovered = true))
        assertEquals(1f, labels(hero = true, popup = true))
        assertEquals(0f, labels(hero = true, scrolled = true))
    }

    @Test
    fun adaptiveHomeWithoutHeroCollapsesAfterScrolling() {
        assertEquals(1f, labels())
        assertEquals(0f, labels(scrolled = true))
        assertEquals(1f, labels(scrolled = true, hovered = true))
    }

    @Test
    fun scrollCollapsedDockExpandsForHoverOrPopupOnEveryTab() {
        for (home in listOf(true, false)) {
            for (hover in listOf(true, false)) {
                for (popup in listOf(true, false)) {
                    assertEquals(if (hover || popup) 1f else 0f,
                        desktopNavigationLabelFraction(NavBarStyle.ADAPTIVE, home, false, hover, popup, false, scrollCollapsed = true))
                }
            }
        }
    }

    @Test
    fun otherTabsKeepTheirLabelsWhileScrolling() {
        assertEquals(1f, labels(home = false, scrolled = true))
    }

    @Test
    fun explicitStylesOverrideHoverAndScrolling() {
        assertEquals(0f, labels(style = NavBarStyle.COMPACT, hovered = true, popup = true))
        assertEquals(1f, labels(style = NavBarStyle.EXPANDED, hero = true, scrolled = true))
    }

    @Test
    fun adaptiveHonorsKeyboardFocusAndAvailableLabelSpace() {
        fun focused(style: NavBarStyle, width: Int) = desktopNavigationLabelFraction(
            style, true, true, false, false, true, scrollCollapsed = true, keyboardFocus = true,
            labelsFit = desktopDockLabelsFit(width.dp, 5, 46.dp))
        assertEquals(0f, focused(NavBarStyle.ADAPTIVE, 397))
        assertEquals(1f, focused(NavBarStyle.ADAPTIVE, 398))
        assertEquals(1f, focused(NavBarStyle.EXPANDED, 320))
        assertEquals(0f, focused(NavBarStyle.COMPACT, 1600))
    }

    @Test
    fun trackStaysWithinParentThroughoutWidthAnimation() {
        for ((viewport, expectedHeight) in listOf(320 to 46, 799 to 46, 800 to 48, 1600 to 48, 1601 to 50)) {
            val height = desktopTopBarHeight(viewport.dp)
            assertEquals(expectedHeight.dp, height)
            val available = (viewport - 32).dp
            for (fraction in listOf(0f, 0.5f, 1f)) {
                assertEquals(minOf(available, (expectedHeight * 5 + 8 + 320 * fraction).dp),
                    desktopDockWidth(available, 5, height, fraction))
            }
        }
    }

    private fun labels(
        style: NavBarStyle = NavBarStyle.ADAPTIVE,
        home: Boolean = true,
        hero: Boolean = false,
        hovered: Boolean = false,
        popup: Boolean = false,
        scrolled: Boolean = false,
    ): Float = desktopNavigationLabelFraction(style, home, hero, hovered, popup, scrolled)
}
