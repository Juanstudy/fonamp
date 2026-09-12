package com.fonamp.feature.library

import org.junit.Assert.assertEquals
import org.junit.Test

class LibraryScaffoldTest {
    @Test
    fun `collection route is stable`() {
        assertEquals("collection", LibraryScaffold.ROUTE)
    }
}
