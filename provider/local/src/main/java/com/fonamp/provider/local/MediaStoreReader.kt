package com.fonamp.provider.local

import android.content.ContentResolver
import android.database.Cursor
import android.net.Uri

/**
 * Slice F: test seam over [ContentResolver] (design §8).
 *
 * Production uses [ContentResolverReader]; tests inject a fake returning a
 * cursor built in-memory, so no device MediaStore is needed.
 */
interface MediaStoreReader {
    fun query(
        uri: Uri,
        projection: Array<String>?,
        selection: String?,
        selectionArgs: Array<String>?,
        sortOrder: String?,
    ): Cursor?
}

/** Production [MediaStoreReader] delegating to the real [ContentResolver]. */
class ContentResolverReader(private val resolver: ContentResolver) : MediaStoreReader {
    override fun query(
        uri: Uri,
        projection: Array<String>?,
        selection: String?,
        selectionArgs: Array<String>?,
        sortOrder: String?,
    ): Cursor? = resolver.query(uri, projection, selection, selectionArgs, sortOrder)
}
