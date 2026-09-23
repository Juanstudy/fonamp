package com.fonamp.app.update

import android.app.Notification
import android.app.NotificationManager
import android.content.Intent
import android.net.Uri
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * UP-1: notification content for both terminal download states.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class UpdateNotificationsTest {

    private fun app() = RuntimeEnvironment.getApplication()

    private fun notifications() =
        shadowOf(app().getSystemService(NotificationManager::class.java))

    @Test
    fun `complete posts install notification with view tap`() {
        val install = Intent(Intent.ACTION_VIEW)
            .setDataAndType(Uri.parse("content://com.fonamp.app.fileprovider/x.apk"), ApkInstaller.APK_MIME)

        UpdateNotifications.notifyDownloadComplete(app(), install)

        val posted = notifications().getNotification(UpdateNotifications.NOTIFY_COMPLETE_ID)
        assertNotNull(posted)
        assertEquals(
            "Update downloaded",
            posted!!.extras.getString(Notification.EXTRA_TITLE),
        )
        assertTrue((posted.flags and Notification.FLAG_AUTO_CANCEL) != 0)
        val tap = shadowOf(posted.contentIntent)
        assertNotNull(posted.contentIntent)
        assertTrue(tap.isActivityIntent)
        assertEquals(Intent.ACTION_VIEW, tap.savedIntent.action)
    }

    @Test
    fun `failed posts retry notification opening the app`() {
        UpdateNotifications.notifyDownloadFailed(app())

        val posted = notifications().getNotification(UpdateNotifications.NOTIFY_FAILED_ID)
        assertNotNull(posted)
        assertEquals(
            "Update download failed",
            posted!!.extras.getString(Notification.EXTRA_TITLE),
        )
        assertNotNull(posted.contentIntent)
    }
}
