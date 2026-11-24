package com.nuvio.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import com.nuvio.app.core.ui.*
import com.nuvio.app.features.profiles.ProfilePopupContent
import com.nuvio.app.features.settings.SurfaceStyle
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import kotlin.test.*

@OptIn(ExperimentalTestApi::class)
class DesktopProfileDockTest {
    @Test fun measuredAvatarLandingAndPopupBackdropFollowTabs() {
        for ((width, height) in listOf(640 to 480, 1024 to 768, 1800 to 900)) {
            runDesktopComposeUiTest(width = width, height = height) {
                val anchor = DockAvatarAnchor()
                var moving: LayoutCoordinates? = null
                var avatar: LayoutCoordinates? = null
                var tab by mutableIntStateOf(0)
                var flight by mutableFloatStateOf(0f)
                val pages = listOf("Home", "Search", "Live TV", "Library", "Settings")
                val colors = listOf(Color.Red, Color.Green, Color.Blue, Color.Yellow, Color.Cyan)
                setContent {
                    NuvioTheme(desktopUiScale = 1f) {
                        CompositionLocalProvider(LocalDockAvatar provides anchor,
                            LocalFrostedGlassEnabled provides true, LocalSurfaceStyle provides SurfaceStyle.FrostedGlass) {
                            val haze = rememberHazeState()
                            Box(Modifier.fillMaxSize()) {
                                Box(Modifier.fillMaxSize().hazeSource(haze).background(colors[tab]))
                                DesktopNavigationBar(modifier = Modifier.align(Alignment.BottomCenter), isHeroEnabled = true,
                                    hazeState = haze, items = pages.mapIndexed { index, label ->
                                        FloatingNavigationItem(label, index == tab, { tab = index },
                                            content = if (index != pages.lastIndex) null else { _ ->
                                                Box(Modifier.size(28.dp).dockAvatarTarget().onGloballyPositioned { avatar = it })
                                            })
                                    })
                                Box(Modifier.align(Alignment.Center)) {
                                    FlyingProfileAvatar(flight, height.dp) {
                                        Box(Modifier.size(96.dp).onGloballyPositioned { moving = it })
                                    }
                                }
                                AppPopup(Alignment.Center, IntOffset.Zero, properties = PopupProperties(focusable = true)) {
                                    ProfilePopupContent(emptyList(), emptyList(), null, null, null, haze,
                                    opensBelow = true, availableHeight = 160.dp,
                                    modifier = Modifier.testTag("panel"), onBoundsChanged = { _, _ -> },
                                    onDismissRequest = {}, onProfileSelected = {}, onAddProfileRequested = {},
                                    onPinCancelled = {}, onPinVerified = {})
                                }
                            }
                        }
                    }
                }
                for (index in pages.indices) {
                    runOnIdle { tab = index; flight = 1f }
                    mainClock.advanceTimeBy(500)
                    waitForIdle()
                    runOnIdle {
                        val target = avatar!!.boundsInRoot()
                        val landed = moving!!.boundsInRoot()
                        assertTrue(target.center.x > width / 2f)
                        for ((expected, actual) in listOf(target.left to landed.left, target.top to landed.top,
                            target.right to landed.right, target.bottom to landed.bottom)) {
                            assertEquals(expected, actual, 0.01f, "$width x $height: ${pages[index]}")
                        }
                    }
                    val pixels = onNodeWithTag("panel").captureToImage().toPixelMap()
                    val sample = pixels[24, 24]
                    val base = colors[index]
                    for ((channel, backdrop) in listOf(sample.red to base.red, sample.green to base.green, sample.blue to base.blue)) {
                        // Bright artwork remains visible but dark enough for the white labels.
                        assertTrue(if (backdrop == 1f) channel in 0.24f..0.52f else channel < 0.13f,
                            "$width x $height: ${pages[index]} panel=$sample")
                    }
                }
            }
        }
    }
}
