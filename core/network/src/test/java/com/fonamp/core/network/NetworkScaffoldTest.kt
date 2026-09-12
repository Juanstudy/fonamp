package com.fonamp.core.network

import org.junit.Assert.assertEquals
import org.junit.Test

class NetworkScaffoldTest {
    @Test
    fun `default client uses 10s timeouts`() {
        NetworkScaffold.defaultClient().let {
            assertEquals(10_000, it.connectTimeoutMillis)
            assertEquals(10_000, it.readTimeoutMillis)
        }
    }
}
