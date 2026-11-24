package com.nuvio.app.features.player.desktop

// Both native subtitle slots use half-percent units for the configured offset.
// Negative offsets are valid at rest, but must not put text under visible controls.
//
// The lift is deliberately 1:1 with the cleared control height (and capped), not
// 2:1: doubling it parked subtitles a long way above the seek bar, leaving a gap
// the eye reads as a bug. Clearance stays adaptive — it still tracks the chrome.
internal fun liftedSubtitleOffset(configured: Int, clearancePercent: Int): Int =
    if (clearancePercent <= 0) configured
    else (configured.coerceAtLeast(0) + clearancePercent.coerceAtMost(100)).coerceAtMost(100)
