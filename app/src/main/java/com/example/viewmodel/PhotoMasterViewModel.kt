package com.example.viewmodel

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.Adjustments
import com.example.model.BlurBgSettings
import com.example.model.CropRatio
import com.example.model.FilterProcessor
import com.example.model.PhotoFilter
import com.example.model.GalleryItem
import com.example.model.StickerOverlay
import com.example.model.TextOverlay
import com.example.util.GalleryRepository
import com.example.util.ImageUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class EditorTab(val displayName: String) {
    FILTERS("Filters"),
    ADJUST("Adjust"),
    TOOLS("Tools"),
    TEXT("Text"),
    STICKER("Stickers"),
    BLUR_BG("Blur BG")
}

data class PhotoMasterUiState(
    val originalBitmap: Bitmap? = null,
    val displayBitmap: Bitmap? = null,
    val selectedTab: EditorTab = EditorTab.FILTERS,
    val selectedFilter: PhotoFilter = PhotoFilter.ORIGINAL,
    val adjustments: Adjustments = Adjustments(),
    val blurBgSettings: BlurBgSettings = BlurBgSettings(),
    val textOverlays: List<TextOverlay> = emptyList(),
    val stickerOverlays: List<StickerOverlay> = emptyList(),
    val filterThumbnails: Map<PhotoFilter, Bitmap> = emptyMap(),
    val isLoading: Boolean = false,
    val isFiltering: Boolean = false,
    val isSaving: Boolean = false,
    val saveSuccessUri: Uri? = null,
    val saveMessage: String? = null,
    val shareIntent: Intent? = null,
    val errorMessage: String? = null,
    val showCompareOriginal: Boolean = false,
    val showGalleryPicker: Boolean = false,
    val galleryItems: List<GalleryItem> = emptyList(),
    val isGalleryLoading: Boolean = false
) {
    val hasImage: Boolean get() = originalBitmap != null

    val activeDisplayBitmap: Bitmap?
        get() = if (showCompareOriginal) originalBitmap else (displayBitmap ?: originalBitmap)
}

class PhotoMasterViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(PhotoMasterUiState())
    val uiState: StateFlow<PhotoMasterUiState> = _uiState.asStateFlow()

    private var renderJob: Job? = null

    fun selectTab(tab: EditorTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun loadImageFromUri(context: Context, uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, saveMessage = null) }
            try {
                val loadedBitmap = withContext(Dispatchers.IO) {
                    ImageUtils.loadBitmapFromUri(context.applicationContext, uri)
                }

                if (loadedBitmap != null) {
                    onNewBaseBitmap(loadedBitmap)
                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "Unable to load selected image."
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Error loading image: ${e.localizedMessage ?: "Unknown error"}"
                    )
                }
            }
        }
    }

    fun loadSampleImage(context: Context, resId: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, saveMessage = null) }
            try {
                val sampleBitmap = withContext(Dispatchers.IO) {
                    ImageUtils.loadBitmapFromResource(context.applicationContext, resId)
                }
                if (sampleBitmap != null) {
                    onNewBaseBitmap(sampleBitmap)
                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "Unable to load sample image."
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Error loading sample image: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    private suspend fun onNewBaseBitmap(bitmap: Bitmap) {
        withContext(Dispatchers.Default) {
            val baseThumb = ImageUtils.createThumbnail(bitmap, 110)
            val thumbMap = mutableMapOf<PhotoFilter, Bitmap>()
            for (filter in PhotoFilter.entries) {
                thumbMap[filter] = FilterProcessor.applyFilter(baseThumb, filter)
            }

            _uiState.update {
                it.copy(
                    originalBitmap = bitmap,
                    displayBitmap = bitmap,
                    selectedFilter = PhotoFilter.ORIGINAL,
                    adjustments = Adjustments(),
                    blurBgSettings = BlurBgSettings(),
                    textOverlays = emptyList(),
                    stickerOverlays = emptyList(),
                    filterThumbnails = thumbMap,
                    isLoading = false,
                    isFiltering = false,
                    saveMessage = null,
                    errorMessage = null
                )
            }
        }
    }

    /**
     * Re-renders the full pipeline when filter, adjustments, blur, or overlays change
     */
    private fun scheduleRender() {
        val original = _uiState.value.originalBitmap ?: return
        renderJob?.cancel()
        renderJob = viewModelScope.launch {
            _uiState.update { it.copy(isFiltering = true) }
            val state = _uiState.value
            val rendered = withContext(Dispatchers.Default) {
                FilterProcessor.processFullImage(
                    baseBitmap = original,
                    filter = state.selectedFilter,
                    adjustments = state.adjustments,
                    blurBg = state.blurBgSettings,
                    textOverlays = state.textOverlays,
                    stickerOverlays = state.stickerOverlays
                )
            }
            _uiState.update { it.copy(displayBitmap = rendered, isFiltering = false) }
        }
    }

    // 1. Filters
    fun selectFilter(filter: PhotoFilter) {
        if (_uiState.value.selectedFilter == filter) return
        _uiState.update { it.copy(selectedFilter = filter) }
        scheduleRender()
    }

    // 2. Adjustments
    fun updateBrightness(brightness: Float) {
        _uiState.update {
            it.copy(adjustments = it.adjustments.copy(brightness = brightness))
        }
        scheduleRender()
    }

    fun updateContrast(contrast: Float) {
        _uiState.update {
            it.copy(adjustments = it.adjustments.copy(contrast = contrast))
        }
        scheduleRender()
    }

    fun updateSaturation(saturation: Float) {
        _uiState.update {
            it.copy(adjustments = it.adjustments.copy(saturation = saturation))
        }
        scheduleRender()
    }

    fun resetAdjustments() {
        _uiState.update { it.copy(adjustments = Adjustments()) }
        scheduleRender()
    }

    // 3. Transform Tools: Rotate, Flip, Crop
    fun rotateImage(degrees: Float = 90f) {
        val original = _uiState.value.originalBitmap ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isFiltering = true) }
            val rotated = withContext(Dispatchers.Default) {
                ImageUtils.rotateImage(original, degrees)
            }
            onNewBaseBitmap(rotated)
        }
    }

    fun flipImage(horizontal: Boolean, vertical: Boolean) {
        val original = _uiState.value.originalBitmap ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isFiltering = true) }
            val flipped = withContext(Dispatchers.Default) {
                ImageUtils.flipImage(original, horizontal, vertical)
            }
            onNewBaseBitmap(flipped)
        }
    }

    fun cropImage(ratio: CropRatio) {
        val original = _uiState.value.originalBitmap ?: return
        if (ratio == CropRatio.FREE) return
        viewModelScope.launch {
            _uiState.update { it.copy(isFiltering = true) }
            val cropped = withContext(Dispatchers.Default) {
                ImageUtils.cropToRatio(original, ratio)
            }
            onNewBaseBitmap(cropped)
        }
    }

    // 4. Text Overlays
    fun addTextOverlay(
        text: String,
        color: Int = android.graphics.Color.WHITE,
        fontSizeSp: Float = 32f,
        isBold: Boolean = true,
        isItalic: Boolean = false,
        hasBackground: Boolean = true
    ) {
        if (text.isBlank()) return
        val newOverlay = TextOverlay(
            text = text,
            color = color,
            fontSizeSp = fontSizeSp,
            isBold = isBold,
            isItalic = isItalic,
            hasBackground = hasBackground,
            normalizedX = 0.5f,
            normalizedY = 0.5f
        )
        _uiState.update {
            it.copy(textOverlays = it.textOverlays + newOverlay)
        }
        scheduleRender()
    }

    fun removeTextOverlay(id: Long) {
        _uiState.update {
            it.copy(textOverlays = it.textOverlays.filterNot { item -> item.id == id })
        }
        scheduleRender()
    }

    fun clearAllText() {
        _uiState.update { it.copy(textOverlays = emptyList()) }
        scheduleRender()
    }

    // 5. Stickers / Emojis
    fun addSticker(emoji: String, sizeSp: Float = 48f) {
        val newSticker = StickerOverlay(
            emoji = emoji,
            sizeSp = sizeSp,
            normalizedX = 0.5f,
            normalizedY = 0.5f
        )
        _uiState.update {
            it.copy(stickerOverlays = it.stickerOverlays + newSticker)
        }
        scheduleRender()
    }

    fun removeSticker(id: Long) {
        _uiState.update {
            it.copy(stickerOverlays = it.stickerOverlays.filterNot { item -> item.id == id })
        }
        scheduleRender()
    }

    fun clearAllStickers() {
        _uiState.update { it.copy(stickerOverlays = emptyList()) }
        scheduleRender()
    }

    // 6. Blur Background
    fun toggleBlurBackground(enabled: Boolean) {
        _uiState.update {
            it.copy(blurBgSettings = it.blurBgSettings.copy(enabled = enabled))
        }
        scheduleRender()
    }

    fun updateBlurRadius(radius: Int) {
        _uiState.update {
            it.copy(blurBgSettings = it.blurBgSettings.copy(blurRadius = radius))
        }
        scheduleRender()
    }

    fun updateBlurFocusRatio(ratio: Float) {
        _uiState.update {
            it.copy(blurBgSettings = it.blurBgSettings.copy(focusRadiusRatio = ratio))
        }
        scheduleRender()
    }

    // Compare original
    fun setCompareOriginal(show: Boolean) {
        _uiState.update { it.copy(showCompareOriginal = show) }
    }

    // 7. Save to Gallery
    fun saveImageToGallery(context: Context) {
        val state = _uiState.value
        val bitmapToSave = state.displayBitmap ?: state.originalBitmap
        if (bitmapToSave == null) {
            _uiState.update { it.copy(errorMessage = "No photo to save.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null, saveMessage = null) }
            val result = withContext(Dispatchers.IO) {
                ImageUtils.saveBitmapToGallery(context.applicationContext, bitmapToSave)
            }

            result.onSuccess { uri ->
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        saveSuccessUri = uri,
                        saveMessage = "Photo saved to Gallery (Pictures/PhotoMaster)!"
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = "Failed to save photo: ${error.localizedMessage ?: "Unknown error"}"
                    )
                }
            }
        }
    }

    // Share photo
    fun shareImage(context: Context) {
        val state = _uiState.value
        val bitmapToShare = state.displayBitmap ?: state.originalBitmap
        if (bitmapToShare == null) {
            _uiState.update { it.copy(errorMessage = "No photo to share.") }
            return
        }

        viewModelScope.launch {
            val intent = withContext(Dispatchers.IO) {
                ImageUtils.shareBitmap(context.applicationContext, bitmapToShare)
            }
            if (intent != null) {
                _uiState.update { it.copy(shareIntent = intent) }
            } else {
                _uiState.update { it.copy(errorMessage = "Could not prepare image for sharing.") }
            }
        }
    }

    fun clearShareIntent() {
        _uiState.update { it.copy(shareIntent = null) }
    }

    fun clearSaveMessage() {
        _uiState.update { it.copy(saveMessage = null, saveSuccessUri = null) }
    }

    fun clearErrorMessage() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun clearImage() {
        _uiState.update {
            PhotoMasterUiState()
        }
    }

    // Gallery Picker with Coil Caching Controls
    fun openGalleryPicker(context: Context) {
        _uiState.update { it.copy(showGalleryPicker = true, isGalleryLoading = true) }
        viewModelScope.launch {
            val items = GalleryRepository.loadGalleryItems(context.applicationContext)
            _uiState.update {
                it.copy(
                    galleryItems = items,
                    isGalleryLoading = false
                )
            }
        }
    }

    fun closeGalleryPicker() {
        _uiState.update { it.copy(showGalleryPicker = false) }
    }

    fun selectGalleryItem(context: Context, item: GalleryItem) {
        closeGalleryPicker()
        if (item.uri != null) {
            loadImageFromUri(context, item.uri)
        } else if (item.drawableResId != null) {
            loadSampleImage(context, item.drawableResId)
        }
    }
}
