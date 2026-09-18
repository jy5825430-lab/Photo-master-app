package com.example.util

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.example.R
import com.example.model.GalleryItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object GalleryRepository {

    /**
     * Loads gallery items combining device images (if permission permits / queryable)
     * and built-in showcase demo collections for smooth thumbnail browsing.
     */
    suspend fun loadGalleryItems(context: Context): List<GalleryItem> = withContext(Dispatchers.IO) {
        val items = mutableListOf<GalleryItem>()

        // 1. Built-in studio showcase samples (guaranteed instant availability)
        items.add(
            GalleryItem(
                id = "sample_landscape",
                title = "Alpine Vista",
                subtitle = "Landscape Demo",
                drawableResId = R.drawable.sample_landscape,
                category = "Showcase"
            )
        )

        // 2. Query MediaStore for device photos if available
        try {
            val projection = arrayOf(
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.DATE_ADDED
            )

            val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"
            val collectionUri: Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
            } else {
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            }

            context.contentResolver.query(
                collectionUri,
                projection,
                null,
                null,
                sortOrder
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)

                var count = 0
                while (cursor.moveToNext() && count < 100) { // Fetch up to 100 items for snappy browsing
                    val id = cursor.getLong(idColumn)
                    val name = cursor.getString(nameColumn) ?: "Photo"
                    val contentUri = ContentUris.withAppendedId(collectionUri, id)

                    items.add(
                        GalleryItem(
                            id = "media_$id",
                            title = name,
                            subtitle = "Device Gallery",
                            uri = contentUri,
                            category = "Recent Photos"
                        )
                    )
                    count++
                }
            }
        } catch (_: Exception) {
            // MediaStore query error or permission denied: graceful fallback
        }

        items
    }
}
