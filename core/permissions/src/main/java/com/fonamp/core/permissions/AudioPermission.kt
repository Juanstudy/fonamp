package com.fonamp.core.permissions

/**
 * Slice A scaffold: API-split audio permission (permissions spec Req 2).
 * READ_MEDIA_AUDIO on API 33+, READ_EXTERNAL_STORAGE on 29–32.
 * Gate UI lands in Slice I.
 */
object AudioPermission {
    const val READ_MEDIA_AUDIO = "android.permission.READ_MEDIA_AUDIO"
    const val READ_EXTERNAL_STORAGE = "android.permission.READ_EXTERNAL_STORAGE"

    fun permissionForSdk(sdkInt: Int): String =
        if (sdkInt >= 33) READ_MEDIA_AUDIO else READ_EXTERNAL_STORAGE
}
