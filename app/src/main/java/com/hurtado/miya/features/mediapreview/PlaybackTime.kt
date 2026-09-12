package com.hurtado.miya.features.mediapreview

import kotlin.math.roundToInt

/** Port of `TimeInterval+Playback.swift`'s `m:ss` label. */
fun formatPlaybackTime(seconds: Double): String {
    val total = seconds.roundToInt().coerceAtLeast(0)
    val minutes = total / 60
    val secs = total % 60
    return "%d:%02d".format(minutes, secs)
}

/** Port of `remainingLabel(of:)`: `-m:ss`, or a placeholder until the duration is known. */
fun formatRemainingTime(currentTime: Double, duration: Double?): String {
    if (duration == null) return "--:--"
    val remaining = (duration - currentTime).coerceAtLeast(0.0)
    return "-" + formatPlaybackTime(remaining)
}
