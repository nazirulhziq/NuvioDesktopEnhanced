package com.nuvio.app

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.Dp

/** Window-scoped; the destination can move as the dock expands or the window resizes. */
internal class DockAvatarAnchor {
    var boundsInWindow by mutableStateOf<Rect?>(null)
    var owner: Any? = null
}

internal val LocalDockAvatar = staticCompositionLocalOf<DockAvatarAnchor?> { null }

@Composable
internal fun Modifier.dockAvatarTarget(): Modifier {
    val anchor = LocalDockAvatar.current ?: return this
    val owner = remember(anchor) { Any() }
    DisposableEffect(owner) {
        onDispose {
            if (anchor.owner === owner) {
                anchor.owner = null
                anchor.boundsInWindow = null
            }
        }
    }
    return onGloballyPositioned {
        anchor.owner = owner
        anchor.boundsInWindow = Rect(it.localToWindow(Offset.Zero),
            it.localToWindow(Offset(it.size.width.toFloat(), it.size.height.toFloat())))
    }
}

@Composable
internal fun FlyingProfileAvatar(flight: Float, fallbackDistance: Dp, content: @Composable () -> Unit) {
    val dock = LocalDockAvatar.current
    var origin by remember { mutableStateOf<LayoutCoordinates?>(null) }
    // The outer layout never moves. Measuring the moving child would feed its own
    // translation back into the next frame and leave it short of the dock.
    Box(Modifier.onGloballyPositioned { origin = it }) {
        Box(Modifier.graphicsLayer {
            val from = origin
            val to = dock?.boundsInWindow
            val target = if (flight > 0f && from?.isAttached == true && to != null) {
                Rect(from.windowToLocal(to.topLeft), from.windowToLocal(to.bottomRight))
            } else null
            translationX = (target?.let { it.center.x - size.width / 2f } ?: 0f) * flight
            translationY = (target?.let { it.center.y - size.height / 2f } ?: fallbackDistance.toPx()) * flight
            scaleX = 1f + ((target?.let { it.width / size.width } ?: 0.38f) - 1f) * flight
            scaleY = 1f + ((target?.let { it.height / size.height } ?: 0.38f) - 1f) * flight
            alpha = 1f - 0.85f * flight
        }) { content() }
    }
}
