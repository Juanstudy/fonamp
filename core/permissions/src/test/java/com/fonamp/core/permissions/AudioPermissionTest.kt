package com.fonamp.core.permissions

import org.junit.Assert.assertEquals
import org.junit.Test

class AudioPermissionTest {
    @Test
    fun `api 33 plus uses READ_MEDIA_AUDIO`() {
        assertEquals(AudioPermission.READ_MEDIA_AUDIO, AudioPermission.permissionForSdk(33))
        assertEquals(AudioPermission.READ_MEDIA_AUDIO, AudioPermission.permissionForSdk(35))
    }

    @Test
    fun `api 29 to 32 uses READ_EXTERNAL_STORAGE`() {
        assertEquals(AudioPermission.READ_EXTERNAL_STORAGE, AudioPermission.permissionForSdk(29))
        assertEquals(AudioPermission.READ_EXTERNAL_STORAGE, AudioPermission.permissionForSdk(32))
    }
}
