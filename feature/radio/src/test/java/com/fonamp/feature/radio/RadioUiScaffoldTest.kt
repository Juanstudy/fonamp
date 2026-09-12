package com.fonamp.feature.radio

import org.junit.Assert.assertEquals
import org.junit.Test

class RadioUiScaffoldTest {
    @Test
    fun `discover route is stable`() {
        assertEquals("radio/discover", RadioUiScaffold.DISCOVER_ROUTE)
    }
}
