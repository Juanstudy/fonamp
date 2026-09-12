package com.fonamp.core.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class UiScaffoldTest {
    @Test
    fun `ui tag is stable`() {
        assertEquals("fonamp-ui", UiScaffold.TAG)
    }
}
