package com.fonamp.core.database

import android.content.Context
import androidx.room.Room
import app.cash.turbine.test
import java.io.File
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Slice D RED — ThemeDao contract (settings Req 1: System/Light/Dark persists
 * across restarts, applies without restart; design §5: single row id=1).
 *
 * Round-trip runs on in-memory Room; the restart test uses a temp-file database
 * (close + reopen) because an in-memory database is destroyed on close by
 * definition — the file variant proves the choice genuinely survives a restart.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ThemeDaoTest {

    private lateinit var db: FonampDatabase
    private lateinit var dao: ThemeDao

    @Before
    fun setup() {
        val context: Context = RuntimeEnvironment.getApplication()
        db = Room.inMemoryDatabaseBuilder(context, FonampDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.themeDao()
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun `observe is null before any choice`() = runTest {
        dao.observe().test {
            assertNull(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `set then observe emits the mode`() = runTest {
        dao.set(ThemePref(mode = ThemeMode.DARK))

        dao.observe().test {
            assertEquals(ThemePref(mode = ThemeMode.DARK), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `set replaces the single id-1 row`() = runTest {
        dao.set(ThemePref(mode = ThemeMode.DARK))
        dao.set(ThemePref(mode = ThemeMode.LIGHT))

        dao.observe().test {
            assertEquals(ThemePref(mode = ThemeMode.LIGHT), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `all three modes round-trip`() = runTest {
        for (mode in ThemeMode.entries) {
            dao.set(ThemePref(mode = mode))
            assertEquals(ThemePref(mode = mode), dao.observeOnceForTest())
        }
    }

    @Test
    fun `choice survives database close and reopen`() = runTest {
        val context: Context = RuntimeEnvironment.getApplication()
        val file = File(context.cacheDir, "theme-restart-test.db")
        file.delete()

        var fileDb = Room.databaseBuilder(context, FonampDatabase::class.java, file.absolutePath)
            .allowMainThreadQueries()
            .build()
        fileDb.themeDao().set(ThemePref(mode = ThemeMode.DARK))
        fileDb.close()

        fileDb = Room.databaseBuilder(context, FonampDatabase::class.java, file.absolutePath)
            .allowMainThreadQueries()
            .build()
        try {
            fileDb.themeDao().observe().test {
                assertEquals(ThemePref(mode = ThemeMode.DARK), awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        } finally {
            fileDb.close()
            file.delete()
        }
    }

    private suspend fun ThemeDao.observeOnceForTest(): ThemePref? {
        var result: ThemePref? = null
        observe().test {
            result = awaitItem()
            cancelAndIgnoreRemainingEvents()
        }
        return result
    }
}
