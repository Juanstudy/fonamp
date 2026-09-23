package com.fonamp.app.update

import android.app.DownloadManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import java.io.File

/**
 * Downloads release APKs and hands them to the system installer.
 *
 * - [enqueueUpdate] stores the system download id in private prefs so a
 *   download outliving the process is still recognized on next launch.
 * - [finishedDownload] resolves the tracked download to a file only when
 *   the system reports it successful — anything else is "not ready".
 * - [installIntent] builds the platform installer intent; [installNow]
 *   fires it for foreground flows. Background flows (the download-complete
 *   receiver) must NOT start it directly — background activity starts are
 *   blocked since Android 10 — they post it as a notification tap instead.
 */
class ApkInstaller(
    private val appContext: Context,
    private val downloads: DownloadManager =
        appContext.getSystemService(DownloadManager::class.java),
    private val prefs: SharedPreferences =
        appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE),
) {
    /** System download id we are tracking, [NO_ID] when none. */
    val trackedDownloadId: Long
        get() = prefs.getLong(KEY_DOWNLOAD_ID, NO_ID)

    /** Enqueue the apk download into our private updates dir. */
    fun enqueueUpdate(apkUrl: String, fileName: String): Long {
        val request = DownloadManager.Request(Uri.parse(apkUrl))
            .setTitle("Fonamp update")
            .setDescription(fileName)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
            .setDestinationInExternalFilesDir(appContext, Environment.DIRECTORY_DOWNLOADS, "$UPDATES_DIR/$fileName")
            .setMimeType(APK_MIME)
        val id = downloads.enqueue(request)
        prefs.edit().putLong(KEY_DOWNLOAD_ID, id).putString(KEY_FILE_NAME, fileName).apply()
        return id
    }

    /**
     * The downloaded apk file when our tracked download completed
     * successfully, null otherwise (unknown id, still running, failed,
     * or file gone).
     */
    fun finishedDownload(): File? {
        val id = trackedDownloadId
        val fileName = prefs.getString(KEY_FILE_NAME, null)
        if (id == NO_ID || fileName.isNullOrBlank()) return null
        if (downloadStatus(id) != DownloadManager.STATUS_SUCCESSFUL) return null
        return File(
            appContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
            "$UPDATES_DIR/$fileName",
        ).takeIf { it.exists() }
    }

    /**
     * Installer intent for [apkFile], null when the file is missing or the
     * FileProvider cannot serve it. Shared by [installNow] and the
     * completion notification (whose tap fires it directly — allowed even
     * from background, unlike a receiver-started activity).
     */
    fun installIntent(apkFile: File): Intent? {
        if (!apkFile.exists()) return null
        val uri = runCatching {
            FileProvider.getUriForFile(
                appContext,
                "${appContext.packageName}.fileprovider",
                apkFile,
            )
        }.getOrNull() ?: return null
        return Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, APK_MIME)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    /**
     * Open the system installer for [apkFile]. Returns false when the file
     * is missing, unservable, or nothing handles the install intent
     * (unknown sources locked down with no handler) — callers surface
     * that, never crash. Foreground flows only; Android always confirms
     * with the user, silent install is not possible by design.
     */
    fun installNow(apkFile: File): Boolean {
        val intent = installIntent(apkFile) ?: return false
        return try {
            appContext.startActivity(intent)
            true
        } catch (e: ActivityNotFoundException) {
            false
        }
    }

    /** Forget the tracked download (fresh check supersedes it). */
    fun clearTracking() {
        prefs.edit().remove(KEY_DOWNLOAD_ID).remove(KEY_FILE_NAME).apply()
    }

    private fun downloadStatus(id: Long): Int? {
        val cursor = downloads.query(DownloadManager.Query().setFilterById(id)) ?: return null
        return cursor.use {
            if (!it.moveToFirst()) return null
            it.getInt(it.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
        }
    }

    companion object {
        const val APK_MIME = "application/vnd.android.package-archive"
        const val UPDATES_DIR = "updates"
        /** Constant file name: re-downloads overwrite, never accumulate. */
        const val UPDATE_FILE_NAME = "fonamp-update.apk"
        const val NO_ID = -1L
        private const val PREFS_NAME = "in_app_updates"
        private const val KEY_DOWNLOAD_ID = "download_id"
        private const val KEY_FILE_NAME = "file_name"
    }
}
