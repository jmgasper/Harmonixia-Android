package com.harmonixia.android.ui.components

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.bitmapConfig
import com.harmonixia.android.R
import com.harmonixia.android.ui.playback.PlaybackInfo
import com.harmonixia.android.util.ImageQualityManager

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun SharedTransitionScope.MiniPlayer(
    playbackInfo: PlaybackInfo,
    onPlayPauseClick: () -> Unit,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
    onExpandClick: () -> Unit,
    isVisible: Boolean = true,
    isLoading: Boolean = false,
    isExpandedLayout: Boolean = false,
    enableSharedArtworkTransition: Boolean = true,
    modifier: Modifier = Modifier
) {
    val progress = if (playbackInfo.duration > 0L) {
        (playbackInfo.currentPosition.toFloat() / playbackInfo.duration.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }
    val rowHeight = MiniPlayerDefaults.rowHeight(isExpandedLayout)
    val horizontalPadding = if (isExpandedLayout) 24.dp else 16.dp
    val artworkSize = if (isExpandedLayout) 48.dp else 40.dp
    val controlSize = if (isExpandedLayout) 48.dp else 40.dp
    val controlSpacing = if (isExpandedLayout) 16.dp else 12.dp
    val sharedArtworkState = rememberSharedContentState(key = SHARED_ARTWORK_KEY)
    val placeholderPainter = ColorPainter(MaterialTheme.colorScheme.surfaceVariant)
    val title = playbackInfo.title
    val artist = playbackInfo.artist
    val album = playbackInfo.album
    val artistAlbum = when {
        artist.isNotBlank() && album.isNotBlank() -> "$artist / $album"
        artist.isNotBlank() -> artist
        album.isNotBlank() -> album
        else -> ""
    }
    val context = LocalContext.current
    val qualityLabel = formatTrackQualityLabel(playbackInfo.quality, context::getString)
    val qualityManager = remember(context) { ImageQualityManager(context) }
    val optimizedSize = qualityManager.getOptimalImageSize(artworkSize)
    val sizePx = with(LocalDensity.current) { optimizedSize.roundToPx() }
    val artworkRequest = ImageRequest.Builder(context)
        .data(playbackInfo.artworkUrl)
        .size(sizePx)
        .bitmapConfig(qualityManager.getOptimalBitmapConfig())
        .build()
    val baseArtworkModifier = Modifier
        .size(optimizedSize)
        .clip(RoundedCornerShape(8.dp))

    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically { it } + fadeIn(),
        exit = slideOutVertically { it } + fadeOut()
    ) {
        val artworkModifier = if (enableSharedArtworkTransition) {
            baseArtworkModifier.sharedElement(sharedArtworkState, this@AnimatedVisibility)
        } else {
            baseArtworkModifier
        }
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .clickable(
                    role = Role.Button,
                    onClickLabel = stringResource(R.string.content_desc_open_now_playing),
                    onClick = onExpandClick
                ),
            tonalElevation = 3.dp,
            shadowElevation = 6.dp
        ) {
            Column {
                if (isLoading) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(MiniPlayerDefaults.ProgressHeight)
                    )
                } else {
                    LinearProgressIndicator(
                        progress = progress,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(MiniPlayerDefaults.ProgressHeight)
                    )
                }
                Row(
                    modifier = Modifier
                        .height(rowHeight)
                        .padding(horizontal = horizontalPadding),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(controlSpacing)
                ) {
                    AsyncImage(
                        model = artworkRequest,
                        contentDescription = stringResource(R.string.content_desc_mini_player_artwork),
                        placeholder = placeholderPainter,
                        error = placeholderPainter,
                        modifier = artworkModifier
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.Center
                    ) {
                        if (title.isNotBlank()) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (artistAlbum.isNotBlank()) {
                            Text(
                                text = artistAlbum,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (qualityLabel != null) {
                            TrackQualityBadge(
                                text = qualityLabel,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        if (isExpandedLayout) {
                            IconButton(
                                onClick = onPreviousClick,
                                enabled = playbackInfo.hasPrevious,
                                modifier = Modifier.size(controlSize)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.SkipPrevious,
                                    contentDescription = stringResource(R.string.action_previous_track)
                                )
                            }
                        }
                        IconButton(
                            onClick = onPlayPauseClick,
                            modifier = Modifier.size(controlSize)
                        ) {
                            Icon(
                                imageVector = if (playbackInfo.isPlaying) {
                                    Icons.Filled.Pause
                                } else {
                                    Icons.Filled.PlayArrow
                                },
                                contentDescription = stringResource(
                                    if (playbackInfo.isPlaying) {
                                        R.string.action_pause
                                    } else {
                                        R.string.action_play
                                    }
                                )
                            )
                        }
                    }
                    IconButton(
                        onClick = onNextClick,
                        enabled = playbackInfo.hasNext,
                        modifier = Modifier.size(controlSize)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.SkipNext,
                            contentDescription = stringResource(R.string.action_next_track)
                        )
                    }
                }
            }
        }
    }
}

private const val SHARED_ARTWORK_KEY = "shared_playback_artwork"

internal object MiniPlayerDefaults {
    val ProgressHeight = 2.dp

    @Composable
    fun rowHeight(isExpandedLayout: Boolean): Dp {
        val configuration = LocalConfiguration.current
        val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        val baseHeight = if (isExpandedLayout) 88.dp else 72.dp
        return if (isLandscape) baseHeight * 0.8f else baseHeight
    }

    @Composable
    fun totalHeight(isExpandedLayout: Boolean): Dp = rowHeight(isExpandedLayout) + ProgressHeight
}
