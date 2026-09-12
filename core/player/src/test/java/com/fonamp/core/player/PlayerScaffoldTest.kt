package com.fonamp.core.player

import org.junit.Assert.assertEquals
import org.junit.Test

class PlayerScaffoldTest {
    @Test
    fun `stream timeouts are 10s per design`() {
        assertEquals(10_000, PlayerScaffold.CONNECT_TIMEOUT_MS)
        assertEquals(10_000, PlayerScaffold.READ_TIMEOUT_MS)
    }
}
