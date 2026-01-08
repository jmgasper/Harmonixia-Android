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
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.outlined.Speaker
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextDecoration
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
import com.harmonixia.android.domain.model.Player
import com.harmonixia.android.domain.model.RepeatMode
import com.harmonixia.android.ui.components.PlaybackControls
import com.harmonixia.android.ui.components.PlayerSelectionDialog
import com.harmonixia.android.ui.components.SeekBar
import com.harmonixia.android.ui.components.TrackQualityCategory
import com.harmonixia.android.ui.components.formatTrackQualityLabel
import com.harmonixia.android.ui.components.trackQualityCategory
import com.harmonixia.android.ui.playback.NowPlayingUiState
import com.harmonixia.android.ui.playback.PlaybackInfo
import com.harmonixia.android.ui.playback.PlaybackViewModel
import com.harmonixia.android.ui.theme.CompressedQualityOnOrange
import com.harmonixia.android.ui.theme.CompressedQualityOrange
import com.harmonixia.android.ui.theme.ExternalPlaybackGreen
import com.harmonixia.android.ui.theme.ExternalPlaybackOnGreen
import com.harmonixia.android.ui.theme.LosslessQualityGreen
import com.harmonixia.android.ui.theme.LosslessQualityOnGreen
import com.harmonixia.android.util.ImageQualityManager
import com.harmonixia.android.util.PlayerSelection
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun SharedTransitionScope.NowPlayingScreen(
    onNavigateBack: () -> Unit,
    onNavigateToArtist: (String, String) -> Unit,
    viewModel: PlaybackViewModel = hiltViewModel(),
    enableSharedArtworkTransition: Boolean = true
) {
    val nowPlayingUiState by viewModel.nowPlayingUiState.collectAsStateWithLifecycle()
    val repeatMode by viewModel.repeatMode.collectAsStateWithLifecycle()
    val shuffle by viewModel.shuffle.collectAsStateWithLifecycle()
    val isRepeatModeUpdating by viewModel.isRepeatModeUpdating.collectAsStateWithLifecycle()
    val isShuffleUpdating by viewModel.isShuffleUpdating.collectAsStateWithLifecycle()
    val imageQualityManager = viewModel.imageQualityManager
    val availablePlayers by viewModel.availablePlayers.collectAsStateWithLifecycle()
    val selectedPlayer by viewModel.selectedPlayer.collectAsStateWithLifecycle()
    val localPlayerId by viewModel.localPlayerId.collectAsStateWithLifecycle()
    var showPlayerSelectionDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
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
    val isExternalPlayback = selectedPlayer?.let {
        !PlayerSelection.isLocalPlayer(it, localPlayerId)
    } == true

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
    val artistNotFoundMessage = stringResource(R.string.now_playing_artist_not_found)
    val latestOnNavigateToArtist by rememberUpdatedState(onNavigateToArtist)
    val onArtistClick = if (displayInfo.artist.isNotBlank()) {
        {
            viewModel.resolveNowPlayingArtist { artist ->
                if (artist != null) {
                    latestOnNavigateToArtist(artist.itemId, artist.provider)
                } else {
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(artistNotFoundMessage)
                    }
                }
            }
        }
    } else {
        null
    }
    val scaffoldContainerColor = if (isExternalPlayback) {
        ExternalPlaybackGreen
    } else {
        MaterialTheme.colorScheme.background
    }
    val topAppBarColors = if (isExternalPlayback) {
        TopAppBarDefaults.topAppBarColors(
            containerColor = ExternalPlaybackGreen,
            titleContentColor = ExternalPlaybackOnGreen,
            navigationIconContentColor = ExternalPlaybackOnGreen,
            actionIconContentColor = ExternalPlaybackOnGreen
        )
    } else {
        TopAppBarDefaults.topAppBarColors()
    }
    val trackIdentity = TrackIdentity(
        title = displayInfo.title,
        artist = displayInfo.artist,
        album = displayInfo.album,
        artworkUrl = displayInfo.artworkUrl,
        quality = displayInfo.quality
    )

    Scaffold(
        containerColor = scaffoldContainerColor,
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.now_playing_title)) },
                colors = topAppBarColors,
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            viewModel.refreshPlayers()
                            showPlayerSelectionDialog = true
                        }
                    ) {
                        Box(
                            modifier = Modifier.size(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Speaker,
                                contentDescription = stringResource(R.string.content_desc_speaker_icon)
                            )
                            if (isExternalPlayback) {
                                Icon(
                                    imageVector = Icons.Outlined.Wifi,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .size(12.dp)
                                )
                            }
                        }
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
            val useSideBySide = isExpanded ||
                windowSizeClass.widthSizeClass == WindowWidthSizeClass.Medium ||
                maxWidth > maxHeight
            val maxArtworkHeight = when {
                isExpanded -> maxHeight * 0.85f
                useSideBySide -> maxHeight * 0.75f
                else -> maxHeight * 0.4f
            }
            val maxArtworkWidth = when {
                isExpanded -> maxWidth * 0.5f
                useSideBySide -> maxWidth * 0.45f
                else -> maxWidth * 0.9f
            }
            val artworkSize = minOf(baseArtworkSize, maxArtworkHeight, maxArtworkWidth)

            if (isExpanded) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(32.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        PlayerProviderRow(
                            player = selectedPlayer,
                            localPlayerId = localPlayerId,
                            providerName = displayInfo.providerName,
                            providerIconSvg = displayInfo.providerIconSvg,
                            providerIconUrl = displayInfo.providerIconUrl
                        )
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
                            placeholderPainter = placeholderPainter,
                            imageQualityManager = imageQualityManager
                        )
                    }
                    ControlsPanel(
                        trackIdentity = trackIdentity,
                        titleStyle = titleStyle,
                        artistStyle = artistStyle,
                        albumStyle = albumStyle,
                        onArtistClick = onArtistClick,
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
            } else if (useSideBySide) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(24.dp)
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
                            placeholderPainter = placeholderPainter,
                            imageQualityManager = imageQualityManager
                        )
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            PlayerProviderRow(
                                player = selectedPlayer,
                                localPlayerId = localPlayerId,
                                providerName = displayInfo.providerName,
                                providerIconSvg = displayInfo.providerIconSvg,
                                providerIconUrl = displayInfo.providerIconUrl
                            )
                            TrackInfoPanel(
                                trackIdentity = trackIdentity,
                                titleStyle = titleStyle,
                                artistStyle = artistStyle,
                                albumStyle = albumStyle,
                                onArtistClick = onArtistClick
                            )
                        }
                    }
                    PlaybackControlPanel(
                        trackIdentity = trackIdentity,
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
                        modifier = Modifier.fillMaxWidth()
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
                        placeholderPainter = placeholderPainter,
                        imageQualityManager = imageQualityManager
                    )
                    PlayerProviderRow(
                        player = selectedPlayer,
                        localPlayerId = localPlayerId,
                        providerName = displayInfo.providerName,
                        providerIconSvg = displayInfo.providerIconSvg,
                        providerIconUrl = displayInfo.providerIconUrl,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    ControlsPanel(
                        trackIdentity = trackIdentity,
                        titleStyle = titleStyle,
                        artistStyle = artistStyle,
                        albumStyle = albumStyle,
                        onArtistClick = onArtistClick,
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

    if (showPlayerSelectionDialog) {
        PlayerSelectionDialog(
            players = availablePlayers,
            selectedPlayer = selectedPlayer,
            localPlayerId = localPlayerId,
            onPlayerSelected = { player ->
                viewModel.selectPlayer(player)
            },
            onPlayerVolumeChange = { player, volume ->
                viewModel.setPlayerVolume(player, volume)
            },
            onPlayerMuteChange = { player, muted ->
                viewModel.setPlayerMute(player, muted)
            },
            onDismiss = { showPlayerSelectionDialog = false }
        )
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
    placeholderPainter: ColorPainter,
    imageQualityManager: ImageQualityManager
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val optimizedSize = imageQualityManager.getOptimalImageSize(artworkSize)
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
                        .bitmapConfig(imageQualityManager.getOptimalBitmapConfig())
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
    onArtistClick: (() -> Unit)?,
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
    Column(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TrackInfoPanel(
            trackIdentity = trackIdentity,
            titleStyle = titleStyle,
            artistStyle = artistStyle,
            albumStyle = albumStyle,
            onArtistClick = onArtistClick
        )
        Spacer(modifier = Modifier.height(24.dp))
        PlaybackControlPanel(
            trackIdentity = trackIdentity,
            playbackInfo = playbackInfo,
            controlsEnabled = controlsEnabled,
            onSeek = onSeek,
            onPlayPause = onPlayPause,
            onNext = onNext,
            onPrevious = onPrevious,
            repeatMode = repeatMode,
            shuffle = shuffle,
            isRepeatModeUpdating = isRepeatModeUpdating,
            isShuffleUpdating = isShuffleUpdating,
            onRepeatToggle = onRepeatToggle,
            onShuffleToggle = onShuffleToggle,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun TrackInfoPanel(
    trackIdentity: TrackIdentity,
    titleStyle: TextStyle,
    artistStyle: TextStyle,
    albumStyle: TextStyle,
    onArtistClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    horizontalAlignment: Alignment.Horizontal = Alignment.CenterHorizontally,
    textAlign: TextAlign = TextAlign.Center
) {
    Column(
        modifier = modifier,
        horizontalAlignment = horizontalAlignment
    ) {
        AnimatedContent(
            targetState = trackIdentity,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "trackText"
        ) { identity ->
            Column(horizontalAlignment = horizontalAlignment) {
                if (identity.title.isNotBlank()) {
                    Text(
                        text = identity.title,
                        style = titleStyle,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = textAlign
                    )
                }
                if (identity.artist.isNotBlank()) {
                    val artistClick = onArtistClick
                    val artistModifier = if (artistClick != null) {
                        Modifier.clickable(role = Role.Button, onClick = artistClick)
                    } else {
                        Modifier
                    }
                    val isArtistClickable = artistClick != null
                    val artistTextStyle = if (isArtistClickable) {
                        artistStyle.copy(textDecoration = TextDecoration.Underline)
                    } else {
                        artistStyle
                    }
                    Text(
                        text = identity.artist,
                        style = artistTextStyle,
                        color = if (isArtistClickable) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = textAlign,
                        modifier = artistModifier
                    )
                }
                if (identity.album.isNotBlank()) {
                    Text(
                        text = identity.album,
                        style = albumStyle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = textAlign
                    )
                }
            }
        }
    }
}

@Composable
private fun PlayerProviderRow(
    player: Player?,
    localPlayerId: String?,
    providerName: String?,
    providerIconSvg: String?,
    providerIconUrl: String?,
    modifier: Modifier = Modifier
) {
    val hasProvider = !providerName.isNullOrBlank() ||
        !providerIconSvg.isNullOrBlank() ||
        !providerIconUrl.isNullOrBlank()
    if (player == null && !hasProvider) return
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        PlayerNamePill(
            player = player,
            localPlayerId = localPlayerId
        )
        if (hasProvider) {
            ProviderBadgeRow(
                providerName = providerName,
                providerIconSvg = providerIconSvg,
                providerIconUrl = providerIconUrl,
                textAlign = TextAlign.Start
            )
        }
    }
}

@Composable
private fun ProviderBadgeRow(
    providerName: String?,
    providerIconSvg: String?,
    providerIconUrl: String?,
    textAlign: TextAlign,
    modifier: Modifier = Modifier
) {
    val label = providerName?.trim().orEmpty()
    val iconSvg = providerIconSvg?.trim()
    val iconUrl = providerIconUrl?.trim()
    if (label.isBlank() && iconSvg.isNullOrBlank() && iconUrl.isNullOrBlank()) return
    val iconModel = remember(iconSvg, iconUrl) {
        when {
            !iconSvg.isNullOrBlank() -> iconSvg.toByteArray(Charsets.UTF_8)
            !iconUrl.isNullOrBlank() -> iconUrl
            else -> null
        }
    }
    val context = LocalContext.current
    val iconSize = 14.dp
    val sizePx = with(LocalDensity.current) { iconSize.roundToPx() }
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        shape = MaterialTheme.shapes.small,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (iconModel != null) {
                val request = remember(iconModel, sizePx) {
                    ImageRequest.Builder(context)
                        .data(iconModel)
                        .size(sizePx)
                        .build()
                }
                AsyncImage(
                    model = request,
                    contentDescription = null,
                    modifier = Modifier.size(iconSize)
                )
            }
            if (label.isNotBlank()) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = textAlign
                )
            }
        }
    }
}

@Composable
private fun PlaybackControlPanel(
    trackIdentity: TrackIdentity,
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
    val qualityLabel = formatTrackQualityLabel(
        trackIdentity.quality,
        context::getString,
        showLosslessDetail = false
    )
    val qualityCategory = trackQualityCategory(trackIdentity.quality)
    val qualityBadgeContent: (@Composable () -> Unit)? = qualityLabel?.let { label ->
        { NowPlayingQualityBadge(text = label, category = qualityCategory) }
    }
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SeekBar(
            currentPosition = playbackInfo.currentPosition,
            duration = playbackInfo.duration,
            onSeek = onSeek,
            enabled = controlsEnabled,
            centerContent = qualityBadgeContent,
            modifier = Modifier.fillMaxWidth()
        )
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

@Composable
private fun NowPlayingQualityBadge(
    text: String,
    category: TrackQualityCategory?,
    modifier: Modifier = Modifier
) {
    val (containerColor, contentColor) = when (category) {
        TrackQualityCategory.LOSSLESS -> LosslessQualityGreen to LosslessQualityOnGreen
        TrackQualityCategory.LOSSY -> CompressedQualityOrange to CompressedQualityOnOrange
        else -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
    }
    Surface(
        color = containerColor,
        contentColor = contentColor,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
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
        providerName = null,
        providerIconSvg = null,
        providerIconUrl = null,
        duration = 0L,
        currentPosition = 0L,
        isPlaying = false,
        hasNext = false,
        hasPrevious = false,
        repeatMode = RepeatMode.OFF,
        shuffle = false,
        selectedPlayer = null
    )

@Composable
private fun PlayerNamePill(
    player: Player?,
    localPlayerId: String?,
    modifier: Modifier = Modifier
) {
    val selected = player ?: return
    val playerName = if (PlayerSelection.isLocalPlayer(selected, localPlayerId)) {
        stringResource(R.string.player_selection_this_device)
    } else {
        selected.name
    }
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        shape = MaterialTheme.shapes.small,
        modifier = modifier
    ) {
        Text(
            text = playerName,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

private const val SHARED_ARTWORK_KEY = "shared_playback_artwork"
