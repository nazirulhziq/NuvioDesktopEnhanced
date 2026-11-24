package com.nuvio.app.features.player.desktop

// Both native subtitle slots use half-percent units for the configured offset.
// Negative offsets are valid at rest, but must not put text under visible controls.
internal fun liftedSubtitleOffset(configured: Int, clearancePercent: Int): Int =
    if (clearancePercent <= 0) configured
    else (configured.coerceAtLeast(0) + clearancePercent.coerceAtMost(100) * 2).coerceAtMost(200)
