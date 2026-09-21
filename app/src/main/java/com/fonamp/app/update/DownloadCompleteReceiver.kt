package com.fonamp.app.update

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Opens the system installer as soon as our tracked apk download completes.
 * Ignores every other download id; a completion that arrived while the app
 * was dead is picked up on next launch via [ApkInstaller.finishedDownload].
 */
class DownloadCompleteReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != DownloadManager.ACTION_DOWNLOAD_COMPLETE) return
        val appContext = context.applicationContext
        val installer = ApkInstaller(appContext)
        val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, ApkInstaller.NO_ID)
        if (id == ApkInstaller.NO_ID || id != installer.trackedDownloadId) return
        installer.finishedDownload()?.let { installer.installNow(it) }
    }
}
