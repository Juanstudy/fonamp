package com.fonamp.provider.api

import org.junit.Assert.assertEquals
import org.junit.Test

class ApiScaffoldTest {
    @Test
    fun `well-known source ids are stable`() {
        assertEquals("local", ApiScaffold.LOCAL_ID)
        assertEquals("radio-browser", ApiScaffold.RADIO_ID)
    }
}
