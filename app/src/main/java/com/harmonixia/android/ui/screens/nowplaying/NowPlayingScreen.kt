package com.harmonixia.android.ui.screens.nowplaying

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.bitmapConfig
import com.harmonixia.android.R
import com.harmonixia.android.domain.model.RepeatMode
import com.harmonixia.android.ui.components.PlaybackControls
import com.harmonixia.android.ui.components.SeekBar
import com.harmonixia.android.ui.components.formatTrackQualityLabel
import com.harmonixia.android.ui.playback.NowPlayingUiState
import com.harmonixia.android.ui.playback.PlaybackInfo
import com.harmonixia.android.ui.playback.PlaybackViewModel
import com.harmonixia.android.util.ImageQualityManager
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun SharedTransitionScope.NowPlayingScreen(
    onNavigateBack: () -> Unit,
    viewModel: PlaybackViewModel = hiltViewModel(),
    enableSharedArtworkTransition: Boolean = true
) {
    val nowPlayingUiState by viewModel.nowPlayingUiState.collectAsStateWithLifecycle()
    val repeatMode by viewModel.repeatMode.collectAsStateWithLifecycle()
    val shuffle by viewModel.shuffle.collectAsStateWithLifecycle()
    val isRepeatModeUpdating by viewModel.isRepeatModeUpdating.collectAsStateWithLifecycle()
    val isShuffleUpdating by viewModel.isShuffleUpdating.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    BackHandler(onBack = onNavigateBack)

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is com.harmonixia.android.ui.playback.PlaybackUiEvent.Error -> {
                    snackbarHostState.showSnackbar(event.message)
                }
            }
        }
    }

    val playbackInfo = when (val state = nowPlayingUiState) {
        is NowPlayingUiState.Loading -> state.info
        is NowPlayingUiState.Playing -> state.info
        NowPlayingUiState.Idle -> null
    }
    val isIdle = nowPlayingUiState is NowPlayingUiState.Idle
    val isLoading = nowPlayingUiState is NowPlayingUiState.Loading
    val controlsEnabled = !isIdle

    val windowSizeClass = calculateWindowSizeClass(activity = context as Activity)
    val isExpanded = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Expanded
    val horizontalPadding = when (windowSizeClass.widthSizeClass) {
        WindowWidthSizeClass.Compact -> 16.dp
        WindowWidthSizeClass.Medium -> 24.dp
        WindowWidthSizeClass.Expanded -> 32.dp
        else -> 16.dp
    }
    val baseArtworkSize = when (windowSizeClass.widthSizeClass) {
        WindowWidthSizeClass.Compact -> 300.dp
        WindowWidthSizeClass.Medium -> 400.dp
        WindowWidthSizeClass.Expanded -> 500.dp
        else -> 300.dp
    }
    val titleStyle = if (isExpanded) {
        MaterialTheme.typography.headlineMedium
    } else {
        MaterialTheme.typography.headlineSmall
    }
    val artistStyle = if (isExpanded) {
        MaterialTheme.typography.titleMedium
    } else {
        MaterialTheme.typography.titleSmall
    }
    val albumStyle = if (isExpanded) {
        MaterialTheme.typography.titleSmall
    } else {
        MaterialTheme.typography.bodyMedium
    }
    val sharedArtworkState = rememberSharedContentState(key = SHARED_ARTWORK_KEY)
    val placeholderPainter = ColorPainter(MaterialTheme.colorScheme.surfaceVariant)
    val dragOffsetX = remember { Animatable(0f) }
    val swipeThreshold = with(LocalDensity.current) { 100.dp.toPx() }
    val displayInfo = playbackInfo ?: emptyPlaybackInfo()
    val trackIdentity = TrackIdentity(
        title = displayInfo.title,
        artist = displayInfo.artist,
        album = displayInfo.album,
        artworkUrl = displayInfo.artworkUrl,
        quality = displayInfo.quality
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.now_playing_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        val contentModifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(horizontal = horizontalPadding, vertical = 16.dp)

        BoxWithConstraints(modifier = contentModifier) {
            val maxArtworkHeight = if (isExpanded) {
                maxHeight * 0.85f
            } else {
                maxHeight * 0.4f
            }
            val maxArtworkWidth = if (isExpanded) {
                maxWidth * 0.5f
            } else {
                maxWidth * 0.9f
            }
            val artworkSize = minOf(baseArtworkSize, maxArtworkHeight, maxArtworkWidth)

            if (isExpanded) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(32.dp)
                ) {
                    ArtworkPanel(
                        trackIdentity = trackIdentity,
                        artworkSize = artworkSize,
                        isLoading = isLoading,
                        hasNext = displayInfo.hasNext,
                        hasPrevious = displayInfo.hasPrevious,
                        onNext = { viewModel.next() },
                        onPrevious = { viewModel.previous() },
                        haptic = haptic,
                        dragOffsetX = dragOffsetX,
                        swipeThreshold = swipeThreshold,
                        enableSharedArtworkTransition = enableSharedArtworkTransition,
                        sharedArtworkState = sharedArtworkState,
                        placeholderPainter = placeholderPainter
                    )
                    ControlsPanel(
                        trackIdentity = trackIdentity,
                        titleStyle = titleStyle,
                        artistStyle = artistStyle,
                        albumStyle = albumStyle,
                        playbackInfo = displayInfo,
                        controlsEnabled = controlsEnabled,
                        onSeek = { viewModel.seek(it) },
                        onPlayPause = { viewModel.togglePlayPause() },
                        onNext = { viewModel.next() },
                        onPrevious = { viewModel.previous() },
                        repeatMode = repeatMode,
                        shuffle = shuffle,
                        isRepeatModeUpdating = isRepeatModeUpdating,
                        isShuffleUpdating = isShuffleUpdating,
                        onRepeatToggle = { viewModel.toggleRepeatMode() },
                        onShuffleToggle = { viewModel.toggleShuffle() },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    ArtworkPanel(
                        trackIdentity = trackIdentity,
                        artworkSize = artworkSize,
                        isLoading = isLoading,
                        hasNext = displayInfo.hasNext,
                        hasPrevious = displayInfo.hasPrevious,
                        onNext = { viewModel.next() },
                        onPrevious = { viewModel.previous() },
                        haptic = haptic,
                        dragOffsetX = dragOffsetX,
                        swipeThreshold = swipeThreshold,
                        enableSharedArtworkTransition = enableSharedArtworkTransition,
                        sharedArtworkState = sharedArtworkState,
                        placeholderPainter = placeholderPainter
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    ControlsPanel(
                        trackIdentity = trackIdentity,
                        titleStyle = titleStyle,
                        artistStyle = artistStyle,
                        albumStyle = albumStyle,
                        playbackInfo = displayInfo,
                        controlsEnabled = controlsEnabled,
                        onSeek = { viewModel.seek(it) },
                        onPlayPause = { viewModel.togglePlayPause() },
                        onNext = { viewModel.next() },
                        onPrevious = { viewModel.previous() },
                        repeatMode = repeatMode,
                        shuffle = shuffle,
                        isRepeatModeUpdating = isRepeatModeUpdating,
                        isShuffleUpdating = isShuffleUpdating,
                        onRepeatToggle = { viewModel.toggleRepeatMode() },
                        onShuffleToggle = { viewModel.toggleShuffle() },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            if (isIdle) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.now_playing_empty),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }

    LaunchedEffect(displayInfo.hasNext, displayInfo.hasPrevious) {
        if (!displayInfo.hasNext && dragOffsetX.value < 0f) {
            dragOffsetX.animateTo(0f, spring())
        }
        if (!displayInfo.hasPrevious && dragOffsetX.value > 0f) {
            dragOffsetX.animateTo(0f, spring())
        }
    }

    LaunchedEffect(trackIdentity) {
        dragOffsetX.animateTo(0f, spring())
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun SharedTransitionScope.ArtworkPanel(
    trackIdentity: TrackIdentity,
    artworkSize: androidx.compose.ui.unit.Dp,
    isLoading: Boolean,
    hasNext: Boolean,
    hasPrevious: Boolean,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    haptic: androidx.compose.ui.hapticfeedback.HapticFeedback,
    dragOffsetX: Animatable<Float, *>,
    swipeThreshold: Float,
    enableSharedArtworkTransition: Boolean,
    sharedArtworkState: SharedTransitionScope.SharedContentState,
    placeholderPainter: ColorPainter
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val qualityManager = remember(context) { ImageQualityManager(context) }
    val optimizedSize = qualityManager.getOptimalImageSize(artworkSize)
    val sizePx = with(LocalDensity.current) { optimizedSize.roundToPx() }
    val baseArtworkModifier = Modifier
        .size(optimizedSize)
        .clip(RoundedCornerShape(24.dp))
    AnimatedVisibility(visible = true) {
        val artworkModifier = if (enableSharedArtworkTransition) {
            baseArtworkModifier.sharedElement(sharedArtworkState, this@AnimatedVisibility)
        } else {
            baseArtworkModifier
        }
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .offset { IntOffset(dragOffsetX.value.roundToInt(), 0) }
                .pointerInput(hasNext, hasPrevious) {
                    detectHorizontalDragGestures(
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            coroutineScope.launch {
                                dragOffsetX.snapTo(dragOffsetX.value + dragAmount)
                            }
                        },
                        onDragEnd = {
                            val offset = dragOffsetX.value
                            when {
                                offset > swipeThreshold && hasPrevious -> {
                                    onPrevious()
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                                offset < -swipeThreshold && hasNext -> {
                                    onNext()
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                            }
                            coroutineScope.launch {
                                dragOffsetX.animateTo(0f, spring())
                            }
                        },
                        onDragCancel = {
                            coroutineScope.launch {
                                dragOffsetX.animateTo(0f, spring())
                            }
                        }
                    )
                }
        ) {
            Box(
                modifier = artworkModifier
            ) {
                AnimatedContent(
                    targetState = trackIdentity.artworkUrl,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "artwork"
                ) { artworkUrl ->
                    val imageRequest = ImageRequest.Builder(context)
                        .data(artworkUrl)
                        .size(sizePx)
                        .bitmapConfig(qualityManager.getOptimalBitmapConfig())
                        .build()
                    AsyncImage(
                        model = imageRequest,
                        contentDescription = stringResource(R.string.content_desc_now_playing_artwork),
                        placeholder = placeholderPainter,
                        error = placeholderPainter,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(36.dp)
                )
            }
        }
    }
}

@Composable
private fun ControlsPanel(
    trackIdentity: TrackIdentity,
    titleStyle: TextStyle,
    artistStyle: TextStyle,
    albumStyle: TextStyle,
    playbackInfo: PlaybackInfo,
    controlsEnabled: Boolean,
    onSeek: (Long) -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    repeatMode: RepeatMode,
    shuffle: Boolean,
    isRepeatModeUpdating: Boolean,
    isShuffleUpdating: Boolean,
    onRepeatToggle: () -> Unit,
    onShuffleToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val qualityLabel = formatTrackQualityLabel(trackIdentity.quality, context::getString)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AnimatedContent(
            targetState = trackIdentity,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "trackText"
        ) { identity ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (identity.title.isNotBlank()) {
                    Text(
                        text = identity.title,
                        style = titleStyle,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                }
                if (identity.artist.isNotBlank()) {
                    Text(
                        text = identity.artist,
                        style = artistStyle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                }
                if (identity.album.isNotBlank()) {
                    Text(
                        text = identity.album,
                        style = albumStyle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        SeekBar(
            currentPosition = playbackInfo.currentPosition,
            duration = playbackInfo.duration,
            onSeek = onSeek,
            enabled = controlsEnabled,
            modifier = Modifier.fillMaxWidth()
        )
        if (qualityLabel != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = qualityLabel,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        PlaybackControls(
            isPlaying = playbackInfo.isPlaying,
            hasNext = playbackInfo.hasNext,
            hasPrevious = playbackInfo.hasPrevious,
            repeatMode = repeatMode,
            shuffle = shuffle,
            isRepeatModeUpdating = isRepeatModeUpdating,
            isShuffleUpdating = isShuffleUpdating,
            onRepeatToggle = onRepeatToggle,
            onShuffleToggle = onShuffleToggle,
            onPlayPause = onPlayPause,
            onNext = onNext,
            onPrevious = onPrevious,
            enabled = controlsEnabled
        )
    }
}

private data class TrackIdentity(
    val title: String,
    val artist: String,
    val album: String,
    val artworkUrl: String?,
    val quality: String?
)

private fun emptyPlaybackInfo(): PlaybackInfo =
    PlaybackInfo(
        title = "",
        artist = "",
        album = "",
        artworkUrl = null,
        quality = null,
        duration = 0L,
        currentPosition = 0L,
        isPlaying = false,
        hasNext = false,
        hasPrevious = false,
        repeatMode = RepeatMode.OFF,
        shuffle = false
    )

private const val SHARED_ARTWORK_KEY = "shared_playback_artwork"
