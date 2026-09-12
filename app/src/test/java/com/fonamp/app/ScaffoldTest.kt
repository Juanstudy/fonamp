package com.fonamp.app

import org.junit.Assert.assertEquals
import org.junit.Test

class ScaffoldTest {
    @Test
    fun `scaffold module tag is stable`() {
        assertEquals("fonamp-slice-a", ScaffoldModule.provideAppTag())
    }
}
