package com.fonamp.core.network

import java.util.concurrent.CopyOnWriteArrayList

/**
 * Ordered mirror policy (design §4): `de1 → de2 → nl1`.
 * The first healthy mirror wins per process (sticky); the order resets on
 * total failure. Every served mirror is recorded in [diagnosticsLog]
 * (diagnostics only, never UI).
 */
class MirrorPolicy(val mirrors: List<String>) {

    /** Base URL that served the last successful fetch, or null. */
    @Volatile
    var servingMirror: String? = null
        private set

    /** Append-only diagnostics log; entries look like `served-by=<mirror>`. */
    val diagnosticsLog: List<String> get() = log.toList()

    private val log = CopyOnWriteArrayList<String>()

    /** Mirrors in try-order: sticky serving mirror first, then the rest. */
    fun orderedMirrors(): List<String> {
        val serving = servingMirror
        if (serving == null || serving !in mirrors) return mirrors.toList()
        return listOf(serving) + (mirrors - serving)
    }

    fun recordServing(mirror: String) {
        servingMirror = mirror
        log.add("served-by=$mirror")
    }

    fun recordFailure(mirror: String, error: NetworkError) {
        log.add("failed-by=$mirror error=$error")
    }

    /** Clears the sticky winner after a total failure (order resets). */
    fun reset() {
        servingMirror = null
        log.add("reset")
    }

    companion object {
        const val DE1 = "https://de1.api.radio-browser.info/"
        const val DE2 = "https://de2.api.radio-browser.info/"
        const val NL1 = "https://nl1.api.radio-browser.info/"
        const val BOOTSTRAP = "https://all.api.radio-browser.info/"

        /** Default order; [BOOTSTRAP] is appended only when [withBootstrap]. */
        fun defaults(withBootstrap: Boolean = false): List<String> =
            if (withBootstrap) listOf(DE1, DE2, NL1, BOOTSTRAP) else listOf(DE1, DE2, NL1)
    }
}
