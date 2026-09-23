package com.fonamp.core.database

import org.junit.Assert.assertEquals
import org.junit.Test

class DatabaseScaffoldTest {
    @Test
    fun `database name and v1 version are stable`() {
        assertEquals("fonamp.db", DatabaseScaffold.NAME)
        assertEquals(2, DatabaseScaffold.VERSION)
    }
}
