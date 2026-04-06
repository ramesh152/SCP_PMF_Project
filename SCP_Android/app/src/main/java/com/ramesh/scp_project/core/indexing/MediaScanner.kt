package com.ramesh.scp_project.core.indexing

import android.content.ContentResolver
import android.net.Uri
import android.provider.MediaStore

class MediaScanner(
    private val contentResolver: ContentResolver
) {

    fun getLatestImageUris(limit: Int = 200): List<Uri> {
        if (limit <= 0) return emptyList()

        // A LinkedHashSet keeps insertion order while preventing duplicate
        // content URIs from being indexed twice in the same pass.
        val uris = LinkedHashSet<Uri>(limit)

        val projection = arrayOf(MediaStore.Images.Media._ID)
        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

        contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            sortOrder
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)

            while (cursor.moveToNext() && uris.size < limit) {
                val id = cursor.getLong(idColumn)
                val uri = Uri.withAppendedPath(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    id.toString()
                )
                uris += uri
            }
        }

        return uris.toList()
    }
}
