package com.fonamp.app.update

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Signals our tracked apk download completion — never installs directly.
 *
 * `startActivity` from a receiver is blocked when the app is in background
 * (Android 10+), which is exactly when downloads complete, so the old
 * direct-install path silently never opened. Instead: a ready apk posts
 * an install notification (the tap launches the installer, allowed even
 * from background); a terminal-but-unsuccessful download posts a failure
 * notification and clears tracking so the next check starts clean.
 * A completion that arrived while the app was dead still fires (manifest
 * receiver); a stranded ready apk is re-offered at next launch by the
 * shell pickup ([ApkInstaller.pendingUpdateNewerThanInstalled]).
 */
class DownloadCompleteReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != DownloadManager.ACTION_DOWNLOAD_COMPLETE) return
        val appContext = context.applicationContext
        val installer = ApkInstaller(appContext)
        val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, ApkInstaller.NO_ID)
        if (id == ApkInstaller.NO_ID || id != installer.trackedDownloadId) return
        val ready = installer.finishedDownload()
        if (ready != null) {
            val install = installer.installIntent(ready) ?: return
            UpdateNotifications.notifyDownloadComplete(appContext, install)
        } else {
            installer.clearTracking()
            UpdateNotifications.notifyDownloadFailed(appContext)
        }
    }
}
