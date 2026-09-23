package com.fonamp.app.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File

/**
 * T2: [ApkInstaller] tracking — enqueue persists and replaces the tracked
 * id, nothing tracked resolves to nothing, missing files never install.
 * The DownloadManager/installer handoff itself is device-verified.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ApkInstallerTest {

    private fun installer() = ApkInstaller(RuntimeEnvironment.getApplication())

    @Test
    fun `enqueue tracks id and replaces previous`() {
        val installer = installer()
        assertEquals(ApkInstaller.NO_ID, installer.trackedDownloadId)

        val first = installer.enqueueUpdate("https://cdn.example/f1.apk", "f1.apk")
        assertEquals(first, installer.trackedDownloadId)

        val second = installer.enqueueUpdate("https://cdn.example/f2.apk", "f2.apk")
        assertEquals(second, installer.trackedDownloadId)
    }

    @Test
    fun `finished download is null when untracked or incomplete`() {
        val installer = installer()
        assertNull(installer.finishedDownload())

        installer.enqueueUpdate("https://cdn.example/f.apk", "f.apk")
        // Enqueued but never completed in the shadow manager.
        assertNull(installer.finishedDownload())
    }

    @Test
    fun `install missing file returns false`() {
        val installer = installer()
        assertNull(installer.installIntent(File("/nonexistent/fonamp.apk")))
        assertFalse(installer.installNow(File("/nonexistent/fonamp.apk")))
    }

    @Test
    fun `clear tracking resets`() {
        val installer = installer()
        installer.enqueueUpdate("https://cdn.example/f.apk", "f.apk")
        installer.clearTracking()

        assertEquals(ApkInstaller.NO_ID, installer.trackedDownloadId)
        assertNull(installer.finishedDownload())
    }

    @Test
    fun `pending update offered only when archive is newer`() {
        val installer = installer()
        val apk = File.createTempFile("fonamp-update", ".apk")
        try {
            assertEquals(apk, installer.pendingUpdateNewerThanInstalled(11L, finished = apk) { 12L })
            assertNull(installer.pendingUpdateNewerThanInstalled(12L, finished = apk) { 12L })
            assertNull(installer.pendingUpdateNewerThanInstalled(13L, finished = apk) { 12L })
            assertNull(installer.pendingUpdateNewerThanInstalled(11L, finished = apk) { null })
            assertNull(installer.pendingUpdateNewerThanInstalled(11L, finished = null) { 12L })
        } finally {
            apk.delete()
        }
    }

    @Test
    fun `archive newness predicate`() {
        assertTrue(ApkInstaller.isArchiveNewer(12L, 11L))
        assertFalse(ApkInstaller.isArchiveNewer(11L, 11L))
        assertFalse(ApkInstaller.isArchiveNewer(10L, 11L))
    }
}
