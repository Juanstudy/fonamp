package com.fonamp.feature.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsScaffoldTest {
    @Test
    fun `settings route is stable`() {
        assertEquals("settings", SettingsScaffold.ROUTE)
    }
}
