package com.fonamp.provider.local

import org.junit.Assert.assertEquals
import org.junit.Test

class LocalScaffoldTest {
    @Test
    fun `local source id is stable`() {
        assertEquals("local", LocalScaffold.SOURCE_ID)
    }
}
