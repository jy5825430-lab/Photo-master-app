package com.example.util

import android.content.Context
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.size.Precision
import coil.size.Scale
import okhttp3.OkHttpClient
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Optimized ImageLoader singleton using Coil with an aggressive, dual-layer caching strategy
 * (MemoryCache + DiskCache) specifically designed for smooth thumbnail browsing
 * when handling large image collections in the gallery picker.
 */
object CoilThumbnailCache {

    @Volatile
    private var imageLoaderInstance: ImageLoader? = null

    fun getImageLoader(context: Context): ImageLoader {
        return imageLoaderInstance ?: synchronized(this) {
            imageLoaderInstance ?: buildImageLoader(context.applicationContext).also {
                imageLoaderInstance = it
            }
        }
    }

    private fun buildImageLoader(appContext: Context): ImageLoader {
        return ImageLoader.Builder(appContext)
            // 1. Layer 1: Memory Cache with 25% max memory allocation for instantaneous UI response
            .memoryCache {
                MemoryCache.Builder(appContext)
                    .maxSizePercent(0.25)
                    .strongReferencesEnabled(true)
                    .build()
            }
            // 2. Layer 2: Dedicated Disk Cache for persistent gallery thumbnails across sessions
            .diskCache {
                DiskCache.Builder()
                    .directory(File(appContext.cacheDir, "coil_thumbnail_cache"))
                    .maxSizeBytes(100L * 1024 * 1024) // 100 MB dedicated thumbnail disk cache
                    .build()
            }
            // 3. Cache policies ensuring fast thumbnail retrieval
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .networkCachePolicy(CachePolicy.DISABLED) // Local gallery optimization
            .respectCacheHeaders(false) // Maximize cache longevity for local photos
            .okHttpClient {
                OkHttpClient.Builder()
                    .connectTimeout(15, TimeUnit.SECONDS)
                    .readTimeout(15, TimeUnit.SECONDS)
                    .build()
            }
            .build()
    }

    /**
     * Constructs an optimized ImageRequest configured for thumbnail loading:
     * - Fixed 256x256 pixel dimension downsampling (reduces RAM usage drastically)
     * - Inexact precision allows fast downsampling
     * - Crossfade transition for smooth rendering
     */
    fun createThumbnailRequest(
        context: Context,
        data: Any?,
        targetWidthPx: Int = 256,
        targetHeightPx: Int = 256
    ): ImageRequest {
        return ImageRequest.Builder(context)
            .data(data)
            .size(targetWidthPx, targetHeightPx)
            .scale(Scale.FIT)
            .precision(Precision.INEXACT)
            .crossfade(true)
            .crossfade(200)
            .memoryCacheKey("thumb_${data}_${targetWidthPx}x${targetHeightPx}")
            .diskCacheKey("thumb_${data}_${targetWidthPx}x${targetHeightPx}")
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .build()
    }
}
