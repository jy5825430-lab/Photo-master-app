package com.example.model

import android.net.Uri

/**
 * Represents a photo item available in the gallery or demo showcase.
 */
data class GalleryItem(
    val id: String,
    val title: String,
    val subtitle: String = "",
    val uri: Uri? = null,
    val drawableResId: Int? = null,
    val category: String = "Collection"
) {
    val model: Any
        get() = uri ?: (drawableResId ?: "")
}
