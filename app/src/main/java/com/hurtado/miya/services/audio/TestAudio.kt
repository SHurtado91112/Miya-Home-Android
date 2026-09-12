package com.hurtado.miya.services.audio

/**
 * Bundled stand-in audio for songs with no real `audioURL` — port of `Services/TestAudio.swift`.
 * The bundled JSON fixtures have no audio at all, so without this most taps would open a silent
 * player. Three generated-tone tracks live in `app/src/main/assets/` (copied from the iOS
 * bundle) and are addressed via Media3's `asset:///` URI scheme, which `DefaultDataSource`
 * resolves out of the box — no custom `DataSource.Factory` needed.
 */
object TestAudio {
    private val names = listOf("test-track-0", "test-track-1", "test-track-2")

    /**
     * A bundled track chosen deterministically from an item id, so a given song always sounds
     * the same across launches. FNV-1a, same as iOS, rather than Kotlin's `hashCode()`, which —
     * like Swift's `hashValue` — isn't guaranteed stable across process runs.
     */
    fun assetUri(itemId: String): String {
        var hash = FNV_OFFSET_BASIS
        for (byte in itemId.toByteArray(Charsets.UTF_8)) {
            hash = hash xor (byte.toULong() and 0xFFu)
            hash *= FNV_PRIME
        }
        val index = (hash % names.size.toULong()).toInt()
        return "asset:///${names[index]}.m4a"
    }

    private val FNV_OFFSET_BASIS = 0xcbf29ce484222325uL
    private val FNV_PRIME = 0x100000001b3uL
}
