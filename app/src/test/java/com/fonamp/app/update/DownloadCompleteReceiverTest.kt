package com.fonamp.app.update

import android.app.DownloadManager
import android.app.Notification
import android.app.NotificationManager
import android.content.Intent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * UP-1: the receiver signals instead of installing.
 *
 * The shadow DownloadManager never completes a download, so the only
 * deterministic receiver path is tracked-but-unfinished → failure
 * notification + cleared tracking. The success branch (finished file →
 * install notification) is covered through [UpdateNotificationsTest].
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DownloadCompleteReceiverTest {

    private fun app() = RuntimeEnvironment.getApplication()

    private fun notifications() =
        shadowOf(app().getSystemService(NotificationManager::class.java))

    private fun completeIntent(id: Long) =
        Intent(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
            .putExtra(DownloadManager.EXTRA_DOWNLOAD_ID, id)

    @Test
    fun `unknown action and untracked id are ignored`() {
        val receiver = DownloadCompleteReceiver()
        receiver.onReceive(app(), Intent("com.example.NOPE"))
        receiver.onReceive(app(), completeIntent(12345L))

        assertEquals(0, notifications().size())
        assertEquals(ApkInstaller.NO_ID, ApkInstaller(app()).trackedDownloadId)
    }

    @Test
    fun `tracked id without finished file posts failure and clears tracking`() {
        val installer = ApkInstaller(app())
        val id = installer.enqueueUpdate("https://cdn.example/f.apk", "f.apk")

        DownloadCompleteReceiver().onReceive(app(), completeIntent(id))

        assertEquals(ApkInstaller.NO_ID, ApkInstaller(app()).trackedDownloadId)
        val posted = notifications().getNotification(UpdateNotifications.NOTIFY_FAILED_ID)
        assertNotNull(posted)
        assertEquals(
            "Update download failed",
            posted!!.extras.getString(Notification.EXTRA_TITLE),
        )
        assertNull(notifications().getNotification(UpdateNotifications.NOTIFY_COMPLETE_ID))
    }
}
