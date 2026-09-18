package com.example.ui

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.view.MotionEvent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Compare
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.CropRatio
import com.example.model.PhotoFilter
import com.example.viewmodel.EditorTab
import com.example.viewmodel.PhotoMasterUiState
import com.example.viewmodel.PhotoMasterViewModel

// Dark Studio & PicsArt Colors
private val PicsArtDark = Color(0xFF101216)
private val PicsArtSurface = Color(0xFF181B22)
private val PicsArtSurfaceCard = Color(0xFF222631)
private val PicsArtAccent = Color(0xFF8B5CF6)       // Vibrant Purple
private val PicsArtSecondary = Color(0xFFEC4899)    // Vibrant Pink
private val PicsArtCyan = Color(0xFF06B6D4)         // Neon Cyan

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoMasterScreen(
    viewModel: PhotoMasterViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Android Photo Picker
    val pickMediaLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.loadImageFromUri(context, uri)
        }
    }

    // Launch share sheet if shareIntent is generated
    LaunchedEffect(uiState.shareIntent) {
        uiState.shareIntent?.let { intent ->
            val chooser = Intent.createChooser(intent, "Share edited photo via")
            context.startActivity(chooser)
            viewModel.clearShareIntent()
        }
    }

    // Snackbar notifications for save feedback
    LaunchedEffect(uiState.saveMessage) {
        uiState.saveMessage?.let { msg ->
            val result = snackbarHostState.showSnackbar(
                message = msg,
                actionLabel = "View",
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed && uiState.saveSuccessUri != null) {
                try {
                    val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(uiState.saveSuccessUri, "image/*")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(viewIntent)
                } catch (_: Exception) { }
            }
            viewModel.clearSaveMessage()
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { error ->
            snackbarHostState.showSnackbar(
                message = error,
                duration = SnackbarDuration.Short
            )
            viewModel.clearErrorMessage()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = PicsArtDark,
        topBar = {
            EditorTopBar(
                hasImage = uiState.hasImage,
                isSaving = uiState.isSaving,
                onNewPhotoClick = {
                    viewModel.openGalleryPicker(context)
                },
                onClearClick = { viewModel.clearImage() },
                onCompareDown = { viewModel.setCompareOriginal(true) },
                onCompareUp = { viewModel.setCompareOriginal(false) },
                onShareClick = { viewModel.shareImage(context) },
                onSaveClick = { viewModel.saveImageToGallery(context) }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (uiState.hasImage) {
                PicsArtBottomToolsDock(
                    uiState = uiState,
                    viewModel = viewModel
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (!uiState.hasImage) {
                EmptyStateView(
                    isLoading = uiState.isLoading,
                    onOpenGalleryClick = {
                        viewModel.openGalleryPicker(context)
                    },
                    onPickPhotoClick = {
                        pickMediaLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    onDemoPhotoClick = {
                        viewModel.loadSampleImage(context, R.drawable.sample_landscape)
                    },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                ActiveEditorCanvas(
                    uiState = uiState,
                    onCompareDown = { viewModel.setCompareOriginal(true) },
                    onCompareUp = { viewModel.setCompareOriginal(false) },
                    modifier = Modifier.fillMaxSize()
                )
            }

            if (uiState.isLoading) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Black.copy(alpha = 0.65f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator(
                                color = PicsArtAccent,
                                modifier = Modifier.testTag("loading_spinner")
                            )
                            Text(
                                text = "Loading Photo...",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }

    // High performance Coil-cached thumbnail Gallery Picker Sheet
    GalleryPickerBottomSheet(
        isOpen = uiState.showGalleryPicker,
        items = uiState.galleryItems,
        isLoading = uiState.isGalleryLoading,
        onDismiss = { viewModel.closeGalleryPicker() },
        onItemSelected = { item -> viewModel.selectGalleryItem(context, item) },
        onSystemPickerClick = {
            viewModel.closeGalleryPicker()
            pickMediaLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        },
        onRefresh = { viewModel.openGalleryPicker(context) }
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
private fun EditorTopBar(
    hasImage: Boolean,
    isSaving: Boolean,
    onNewPhotoClick: () -> Unit,
    onClearClick: () -> Unit,
    onCompareDown: () -> Unit,
    onCompareUp: () -> Unit,
    onShareClick: () -> Unit,
    onSaveClick: () -> Unit
) {
    CenterAlignedTopAppBar(
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = PicsArtSurface,
            titleContentColor = Color.White
        ),
        navigationIcon = {
            if (hasImage) {
                IconButton(
                    onClick = onClearClick,
                    modifier = Modifier.testTag("clear_image_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back to Gallery Picker",
                        tint = Color.White
                    )
                }
            } else {
                IconButton(
                    onClick = onNewPhotoClick,
                    modifier = Modifier.testTag("gallery_button_top")
                ) {
                    Icon(
                        imageVector = Icons.Default.AddPhotoAlternate,
                        contentDescription = "Pick Photo",
                        tint = PicsArtAccent
                    )
                }
            }
        },
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = PicsArtSecondary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = PicsArtAccent.copy(alpha = 0.25f),
                    border = BorderStroke(1.dp, PicsArtAccent.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = "PRO",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        color = PicsArtAccent,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        },
        actions = {
            if (hasImage) {
                // Hold-to-compare button
                IconButton(
                    onClick = { },
                    modifier = Modifier
                        .testTag("topbar_compare_button")
                        .pointerInteropFilter { motionEvent ->
                            when (motionEvent.action) {
                                MotionEvent.ACTION_DOWN -> {
                                    onCompareDown()
                                    true
                                }
                                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                                    onCompareUp()
                                    true
                                }
                                else -> false
                            }
                        }
                ) {
                    Icon(
                        imageVector = Icons.Default.Compare,
                        contentDescription = "Hold to Compare Original",
                        tint = Color.White
                    )
                }

                // Share Button
                IconButton(
                    onClick = onShareClick,
                    modifier = Modifier.testTag("share_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share Photo",
                        tint = PicsArtCyan
                    )
                }

                // Save to Gallery Button
                Button(
                    onClick = onSaveClick,
                    enabled = !isSaving,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PicsArtAccent,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .testTag("save_button")
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(16.dp)
                        )
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = stringResource(R.string.btn_save),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    )
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun ActiveEditorCanvas(
    uiState: PhotoMasterUiState,
    onCompareDown: () -> Unit,
    onCompareUp: () -> Unit,
    modifier: Modifier = Modifier
) {
    val displayBitmap = uiState.activeDisplayBitmap

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PicsArtDark)
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        if (displayBitmap != null) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Black),
                border = BorderStroke(1.dp, Color(0xFF2A2E3B)),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("photo_preview_card")
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        bitmap = displayBitmap.asImageBitmap(),
                        contentDescription = "Photo Preview",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(16.dp))
                    )

                    // Top Left: Active Filter & Adjustments Tag
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(14.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color.Black.copy(alpha = 0.72f),
                            border = BorderStroke(1.dp, PicsArtAccent.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(if (uiState.showCompareOriginal) Color.LightGray else PicsArtAccent)
                                )
                                Text(
                                    text = if (uiState.showCompareOriginal) "ORIGINAL" else uiState.selectedFilter.displayName,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                if (uiState.blurBgSettings.enabled && !uiState.showCompareOriginal) {
                                    Text(
                                        text = "• BLUR BG",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = PicsArtCyan
                                    )
                                }
                            }
                        }
                    }

                    // Bottom Right: Quick "Hold to Compare" Floating Pill
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(14.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color.Black.copy(alpha = 0.75f),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
                            modifier = Modifier
                                .testTag("canvas_compare_pill")
                                .pointerInteropFilter { motionEvent ->
                                    when (motionEvent.action) {
                                        MotionEvent.ACTION_DOWN -> {
                                            onCompareDown()
                                            true
                                        }
                                        MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                                            onCompareUp()
                                            true
                                        }
                                        else -> false
                                    }
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Compare,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Hold Compare",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    // Filtering Overlay Spinner
                    androidx.compose.animation.AnimatedVisibility(
                        visible = uiState.isFiltering,
                        enter = fadeIn(tween(150)),
                        exit = fadeOut(tween(150))
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color.Black.copy(alpha = 0.65f),
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                CircularProgressIndicator(
                                    color = PicsArtAccent,
                                    strokeWidth = 2.5.dp,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Applying changes...",
                                    fontSize = 12.sp,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PicsArtBottomToolsDock(
    uiState: PhotoMasterUiState,
    viewModel: PhotoMasterViewModel,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = PicsArtSurface,
        border = BorderStroke(1.dp, Color(0xFF262A36)),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            // Contextual Control Panel for the Selected Tab
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                when (uiState.selectedTab) {
                    EditorTab.FILTERS -> FiltersControlPanel(uiState = uiState, onFilterSelect = { viewModel.selectFilter(it) })
                    EditorTab.ADJUST -> AdjustControlPanel(uiState = uiState, viewModel = viewModel)
                    EditorTab.TOOLS -> ToolsControlPanel(viewModel = viewModel)
                    EditorTab.TEXT -> TextControlPanel(uiState = uiState, viewModel = viewModel)
                    EditorTab.STICKER -> StickerControlPanel(uiState = uiState, viewModel = viewModel)
                    EditorTab.BLUR_BG -> BlurBgControlPanel(uiState = uiState, viewModel = viewModel)
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Main PicsArt Bottom Tool Bar (Horizontal scrolling if screen is narrow)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ToolTabButton(
                    title = "Filters",
                    icon = Icons.Default.AutoAwesome,
                    isSelected = uiState.selectedTab == EditorTab.FILTERS,
                    testTag = "tab_filters",
                    onClick = { viewModel.selectTab(EditorTab.FILTERS) }
                )
                ToolTabButton(
                    title = "Adjust",
                    icon = Icons.Default.Tune,
                    isSelected = uiState.selectedTab == EditorTab.ADJUST,
                    testTag = "tab_adjust",
                    onClick = { viewModel.selectTab(EditorTab.ADJUST) }
                )
                ToolTabButton(
                    title = "Tools",
                    icon = Icons.Default.Crop,
                    isSelected = uiState.selectedTab == EditorTab.TOOLS,
                    testTag = "tab_tools",
                    onClick = { viewModel.selectTab(EditorTab.TOOLS) }
                )
                ToolTabButton(
                    title = "Text",
                    icon = Icons.Default.TextFields,
                    isSelected = uiState.selectedTab == EditorTab.TEXT,
                    testTag = "tab_text",
                    onClick = { viewModel.selectTab(EditorTab.TEXT) }
                )
                ToolTabButton(
                    title = "Stickers",
                    icon = Icons.Default.EmojiEmotions,
                    isSelected = uiState.selectedTab == EditorTab.STICKER,
                    testTag = "tab_stickers",
                    onClick = { viewModel.selectTab(EditorTab.STICKER) }
                )
                ToolTabButton(
                    title = "Blur BG",
                    icon = Icons.Default.BlurOn,
                    isSelected = uiState.selectedTab == EditorTab.BLUR_BG,
                    testTag = "tab_blur_bg",
                    onClick = { viewModel.selectTab(EditorTab.BLUR_BG) }
                )
            }
        }
    }
}

@Composable
private fun ToolTabButton(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    testTag: String,
    onClick: () -> Unit
) {
    val animatedBg by animateColorAsState(
        targetValue = if (isSelected) PicsArtAccent.copy(alpha = 0.2f) else Color.Transparent,
        label = "tab_bg"
    )
    val animatedContentColor by animateColorAsState(
        targetValue = if (isSelected) PicsArtAccent else Color(0xFF94A3B8),
        label = "tab_content"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(animatedBg)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .testTag(testTag)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = animatedContentColor,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = title,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = animatedContentColor
        )
    }
}

// 1. FILTERS PANEL: 8 Filters with live thumbnails
@Composable
private fun FiltersControlPanel(
    uiState: PhotoMasterUiState,
    onFilterSelect: (PhotoFilter) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(PhotoFilter.entries.toTypedArray()) { filter ->
            val isSelected = uiState.selectedFilter == filter
            val thumbnailBitmap = uiState.filterThumbnails[filter]

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onFilterSelect(filter) }
                    .padding(4.dp)
                    .testTag("filter_option_${filter.id}")
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .border(
                            width = if (isSelected) 2.5.dp else 1.dp,
                            color = if (isSelected) PicsArtAccent else Color(0xFF333846),
                            shape = RoundedCornerShape(10.dp)
                        )
                ) {
                    if (thumbnailBitmap != null) {
                        Image(
                            bitmap = thumbnailBitmap.asImageBitmap(),
                            contentDescription = filter.displayName,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xFF252936)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = filter.displayName.take(2),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }

                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(PicsArtAccent.copy(alpha = 0.25f))
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = filter.displayName,
                    fontSize = 11.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) PicsArtAccent else Color(0xFFCBD5E1),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// 2. ADJUST PANEL: Brightness, Contrast, Saturation sliders
@Composable
private fun AdjustControlPanel(
    uiState: PhotoMasterUiState,
    viewModel: PhotoMasterViewModel
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Precision Tuning",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
            Text(
                text = "Reset All",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = PicsArtSecondary,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { viewModel.resetAdjustments() }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .testTag("reset_adjustments_button")
            )
        }

        // Brightness Slider (-100 to +100)
        AdjustmentSliderRow(
            label = "Brightness",
            icon = Icons.Default.Brightness6,
            value = uiState.adjustments.brightness,
            valueRange = -100f..100f,
            valueDisplay = "${uiState.adjustments.brightness.toInt()}",
            onValueChange = { viewModel.updateBrightness(it) },
            testTag = "slider_brightness"
        )

        // Contrast Slider (0.2x to 2.2x)
        AdjustmentSliderRow(
            label = "Contrast",
            icon = Icons.Default.Contrast,
            value = uiState.adjustments.contrast,
            valueRange = 0.2f..2.2f,
            valueDisplay = String.format("%.2fx", uiState.adjustments.contrast),
            onValueChange = { viewModel.updateContrast(it) },
            testTag = "slider_contrast"
        )

        // Saturation Slider (0.0x to 2.0x)
        AdjustmentSliderRow(
            label = "Saturation",
            icon = Icons.Default.Palette,
            value = uiState.adjustments.saturation,
            valueRange = 0.0f..2.0f,
            valueDisplay = String.format("%.2fx", uiState.adjustments.saturation),
            onValueChange = { viewModel.updateSaturation(it) },
            testTag = "slider_saturation"
        )
    }
}

@Composable
private fun AdjustmentSliderRow(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    valueDisplay: String,
    onValueChange: (Float) -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = Color(0xFF94A3B8),
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = label,
            fontSize = 11.sp,
            color = Color(0xFFE2E8F0),
            modifier = Modifier.width(68.dp)
        )
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = PicsArtAccent,
                activeTrackColor = PicsArtAccent,
                inactiveTrackColor = Color(0xFF333846)
            ),
            modifier = Modifier
                .weight(1f)
                .testTag(testTag)
        )
        Text(
            text = valueDisplay,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = PicsArtCyan,
            modifier = Modifier.width(42.dp),
            textAlign = TextAlign.End
        )
    }
}

// 3. TOOLS PANEL: Crop ratios, Rotate 90, Flip Horizontal & Vertical
@Composable
private fun ToolsControlPanel(
    viewModel: PhotoMasterViewModel
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Rotate & Flip Actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ToolActionButton(
                label = "Rotate 90°",
                icon = Icons.AutoMirrored.Filled.RotateRight,
                testTag = "tool_rotate_90",
                onClick = { viewModel.rotateImage(90f) },
                modifier = Modifier.weight(1f)
            )
            ToolActionButton(
                label = "Flip Horizontal",
                icon = Icons.Default.Flip,
                testTag = "tool_flip_h",
                onClick = { viewModel.flipImage(horizontal = true, vertical = false) },
                modifier = Modifier.weight(1f)
            )
            ToolActionButton(
                label = "Flip Vertical",
                icon = Icons.Default.Flip,
                testTag = "tool_flip_v",
                onClick = { viewModel.flipImage(horizontal = false, vertical = true) },
                modifier = Modifier.weight(1f)
            )
        }

        // Crop Ratios
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "Crop Preset Ratio",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF94A3B8)
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(CropRatio.entries.filterNot { it == CropRatio.FREE }) { ratio ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF222631),
                        border = BorderStroke(1.dp, Color(0xFF333846)),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { viewModel.cropImage(ratio) }
                            .testTag("crop_ratio_${ratio.displayName}")
                    ) {
                        Text(
                            text = ratio.displayName,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ToolActionButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFF222631),
        border = BorderStroke(1.dp, Color(0xFF333846)),
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = PicsArtAccent,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                color = Color.White,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
        }
    }
}

// 4. TEXT PANEL: Custom text, custom colors, bold/italic styles, background
@Composable
private fun TextControlPanel(
    uiState: PhotoMasterUiState,
    viewModel: PhotoMasterViewModel
) {
    var textInput by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf(Color.White) }
    var isBold by remember { mutableStateOf(true) }
    var isItalic by remember { mutableStateOf(false) }
    var hasBackground by remember { mutableStateOf(true) }

    val presetColors = listOf(
        Color.White,
        Color.Yellow,
        Color(0xFFEC4899), // Pink
        Color(0xFF8B5CF6), // Violet
        Color(0xFF06B6D4), // Cyan
        Color(0xFF10B981), // Emerald
        Color(0xFFF97316), // Orange
        Color.Black
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = textInput,
                onValueChange = { textInput = it },
                placeholder = { Text("Enter text...", fontSize = 13.sp, color = Color.Gray) },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PicsArtAccent,
                    unfocusedBorderColor = Color(0xFF333846),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedContainerColor = Color(0xFF1C202B),
                    unfocusedContainerColor = Color(0xFF1C202B)
                ),
                modifier = Modifier
                    .weight(1f)
                    .testTag("text_input_field")
            )

            Button(
                onClick = {
                    if (textInput.isNotBlank()) {
                        viewModel.addTextOverlay(
                            text = textInput,
                            color = selectedColor.toArgb(),
                            isBold = isBold,
                            isItalic = isItalic,
                            hasBackground = hasBackground
                        )
                        textInput = ""
                    }
                },
                enabled = textInput.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = PicsArtAccent),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                modifier = Modifier.testTag("add_text_button")
            ) {
                Text("Add", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Font Styles & Color Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Style Toggles
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isBold) PicsArtAccent else Color(0xFF222631),
                    border = BorderStroke(1.dp, Color(0xFF333846)),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { isBold = !isBold }
                        .testTag("text_style_bold")
                ) {
                    Icon(
                        imageVector = Icons.Default.FormatBold,
                        contentDescription = "Bold",
                        tint = Color.White,
                        modifier = Modifier
                            .padding(6.dp)
                            .size(16.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isItalic) PicsArtAccent else Color(0xFF222631),
                    border = BorderStroke(1.dp, Color(0xFF333846)),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { isItalic = !isItalic }
                        .testTag("text_style_italic")
                ) {
                    Icon(
                        imageVector = Icons.Default.FormatItalic,
                        contentDescription = "Italic",
                        tint = Color.White,
                        modifier = Modifier
                            .padding(6.dp)
                            .size(16.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (hasBackground) PicsArtAccent else Color(0xFF222631),
                    border = BorderStroke(1.dp, Color(0xFF333846)),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { hasBackground = !hasBackground }
                        .testTag("text_style_bg")
                ) {
                    Text(
                        text = "BG",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                    )
                }
            }

            // Preset Colors Palette
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                presetColors.forEach { color ->
                    val isColorSelected = selectedColor == color
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(color)
                            .border(
                                width = if (isColorSelected) 2.dp else 1.dp,
                                color = if (isColorSelected) PicsArtSecondary else Color.Gray,
                                shape = CircleShape
                            )
                            .clickable { selectedColor = color }
                    )
                }
            }
        }

        // Active texts chips with delete
        if (uiState.textOverlays.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Texts:",
                    fontSize = 10.sp,
                    color = Color.Gray
                )
                uiState.textOverlays.forEach { overlay ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF222631),
                        border = BorderStroke(1.dp, Color(0xFF333846))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = overlay.text,
                                fontSize = 11.sp,
                                color = Color.White,
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Delete text",
                                tint = Color(0xFFEF4444),
                                modifier = Modifier
                                    .size(14.dp)
                                    .clickable { viewModel.removeTextOverlay(overlay.id) }
                            )
                        }
                    }
                }
                Text(
                    text = "Clear All",
                    fontSize = 10.sp,
                    color = Color(0xFFEF4444),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clickable { viewModel.clearAllText() }
                        .padding(horizontal = 6.dp)
                )
            }
        }
    }
}

// 5. STICKERS / EMOJIS PANEL: Quick tap to add
@Composable
private fun StickerControlPanel(
    uiState: PhotoMasterUiState,
    viewModel: PhotoMasterViewModel
) {
    val stickers = listOf(
        "✨", "❤️", "🔥", "🌸", "⚡", "🕶️",
        "👑", "🦋", "🌈", "💫", "📸", "💖",
        "🌟", "🎨", "💎", "🕊️", "🎉", "🚀"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Tap Sticker to Stamp on Photo",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF94A3B8)
            )
            if (uiState.stickerOverlays.isNotEmpty()) {
                Text(
                    text = "Clear (${uiState.stickerOverlays.size})",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFEF4444),
                    modifier = Modifier
                        .clickable { viewModel.clearAllStickers() }
                        .padding(horizontal = 4.dp)
                        .testTag("clear_stickers_button")
                )
            }
        }

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(horizontal = 4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(stickers) { emoji ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF222631),
                    border = BorderStroke(1.dp, Color(0xFF333846)),
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { viewModel.addSticker(emoji) }
                        .testTag("sticker_button_$emoji")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = emoji,
                            fontSize = 24.sp
                        )
                    }
                }
            }
        }
    }
}

// 6. BLUR BACKGROUND PANEL: Toggle, blur strength, focus clear zone
@Composable
private fun BlurBgControlPanel(
    uiState: PhotoMasterUiState,
    viewModel: PhotoMasterViewModel
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.BlurOn,
                    contentDescription = null,
                    tint = PicsArtCyan,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Portrait Bokeh / Defocus BG",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }
            Switch(
                checked = uiState.blurBgSettings.enabled,
                onCheckedChange = { viewModel.toggleBlurBackground(it) },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = PicsArtCyan,
                    uncheckedThumbColor = Color.Gray,
                    uncheckedTrackColor = Color(0xFF2A2E3B)
                ),
                modifier = Modifier.testTag("blur_bg_switch")
            )
        }

        if (uiState.blurBgSettings.enabled) {
            // Blur strength slider
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Blur Strength",
                    fontSize = 11.sp,
                    color = Color(0xFFE2E8F0),
                    modifier = Modifier.width(80.dp)
                )
                Slider(
                    value = uiState.blurBgSettings.blurRadius.toFloat(),
                    onValueChange = { viewModel.updateBlurRadius(it.toInt()) },
                    valueRange = 5f..35f,
                    colors = SliderDefaults.colors(
                        thumbColor = PicsArtCyan,
                        activeTrackColor = PicsArtCyan,
                        inactiveTrackColor = Color(0xFF333846)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("slider_blur_radius")
                )
                Text(
                    text = "${uiState.blurBgSettings.blurRadius}px",
                    fontSize = 11.sp,
                    color = PicsArtCyan,
                    modifier = Modifier.width(36.dp),
                    textAlign = TextAlign.End
                )
            }

            // Focus subject clear zone slider
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Subject Focus",
                    fontSize = 11.sp,
                    color = Color(0xFFE2E8F0),
                    modifier = Modifier.width(80.dp)
                )
                Slider(
                    value = uiState.blurBgSettings.focusRadiusRatio,
                    onValueChange = { viewModel.updateBlurFocusRatio(it) },
                    valueRange = 0.15f..0.75f,
                    colors = SliderDefaults.colors(
                        thumbColor = PicsArtCyan,
                        activeTrackColor = PicsArtCyan,
                        inactiveTrackColor = Color(0xFF333846)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("slider_focus_ratio")
                )
                Text(
                    text = "${(uiState.blurBgSettings.focusRadiusRatio * 100).toInt()}%",
                    fontSize = 11.sp,
                    color = PicsArtCyan,
                    modifier = Modifier.width(36.dp),
                    textAlign = TextAlign.End
                )
            }
        }
    }
}

// EMPTY STATE HERO
@Composable
private fun EmptyStateView(
    isLoading: Boolean,
    onOpenGalleryClick: () -> Unit,
    onPickPhotoClick: () -> Unit,
    onDemoPhotoClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = PicsArtSurface),
            border = BorderStroke(1.dp, Color(0xFF262B36)),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 480.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Glow icon badge
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(PicsArtAccent, PicsArtSecondary)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Photo Master Pro",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "PicsArt-style creative studio with 8 pro filters, adjust sliders, crop & rotate, text overlays, stickers, and background blur.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF94A3B8),
                        textAlign = TextAlign.Center
                    )
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Open Gallery Picker button (with Coil L1/L2 fast caching)
                    Button(
                        onClick = onOpenGalleryClick,
                        enabled = !isLoading,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PicsArtAccent,
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("open_gallery_picker_button")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhotoLibrary,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = stringResource(R.string.btn_browse_gallery),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Pick from device file picker button
                    OutlinedButton(
                        onClick = onPickPhotoClick,
                        enabled = !isLoading,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, Color(0xFF3B4254)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .testTag("pick_photo_main_button")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddPhotoAlternate,
                                contentDescription = null,
                                tint = PicsArtCyan,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = stringResource(R.string.btn_pick_photo),
                                fontSize = 14.sp
                            )
                        }
                    }

                    // Try Demo Photo button
                    OutlinedButton(
                        onClick = onDemoPhotoClick,
                        enabled = !isLoading,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, Color(0xFF2E3444)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFCBD5E1)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("try_sample_button")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Landscape,
                                contentDescription = null,
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = stringResource(R.string.btn_sample_photo),
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
