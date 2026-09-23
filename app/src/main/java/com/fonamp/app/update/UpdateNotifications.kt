package com.fonamp.app.update

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.fonamp.app.MainActivity

/**
 * Update completion signals (UP-1).
 *
 * The download-complete receiver must never `startActivity`: background
 * activity starts are blocked since Android 10, so the installer would
 * silently never open. Instead it posts a notification whose tap fires
 * the installer intent directly — notification taps are allowed to
 * launch even from background. If `POST_NOTIFICATIONS` was denied, the
 * system drops these silently and the cold-start pickup dialog
 * ([ApkInstaller.pendingUpdateNewerThanInstalled]) is the backstop.
 */
object UpdateNotifications {

    const val CHANNEL_ID = "fonamp_updates"
    const val NOTIFY_COMPLETE_ID = 1001
    const val NOTIFY_FAILED_ID = 1002

    fun ensureChannel(appContext: Context) {
        val manager = appContext.getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "App updates", NotificationManager.IMPORTANCE_DEFAULT),
        )
    }

    /** "Update downloaded — tap to install", firing [install] on tap. */
    fun notifyDownloadComplete(appContext: Context, install: Intent) {
        ensureChannel(appContext)
        val tap = PendingIntent.getActivity(
            appContext,
            0,
            install,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = Notification.Builder(appContext, CHANNEL_ID)
            .setContentTitle("Update downloaded")
            .setContentText("Tap to install the Fonamp update.")
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentIntent(tap)
            .setAutoCancel(true)
            .build()
        appContext.getSystemService(NotificationManager::class.java)
            ?.notify(NOTIFY_COMPLETE_ID, notification)
    }

    /** "Download failed — tap to retry", reopening the app on tap. */
    fun notifyDownloadFailed(appContext: Context) {
        ensureChannel(appContext)
        // Explicit entry point (not getLaunchIntentForPackage, which can
        // resolve to null and would silently drop this notification).
        val openApp = Intent(appContext, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val tap = PendingIntent.getActivity(
            appContext,
            0,
            openApp,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = Notification.Builder(appContext, CHANNEL_ID)
            .setContentTitle("Update download failed")
            .setContentText("Tap to check for updates again.")
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setContentIntent(tap)
            .setAutoCancel(true)
            .build()
        appContext.getSystemService(NotificationManager::class.java)
            ?.notify(NOTIFY_FAILED_ID, notification)
    }
}
