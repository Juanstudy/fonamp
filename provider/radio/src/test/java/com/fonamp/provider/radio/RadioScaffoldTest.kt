package com.fonamp.provider.radio

import org.junit.Assert.assertEquals
import org.junit.Test

class RadioScaffoldTest {
    @Test
    fun `radio source id is stable`() {
        assertEquals("radio-browser", RadioScaffold.SOURCE_ID)
    }
}
