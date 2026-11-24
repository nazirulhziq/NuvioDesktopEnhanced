package com.nuvio.app.features.player

import com.nuvio.app.core.ui.frostedGlassSecondaryContentColor

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderState
import androidx.compose.material3.Text
import androidx.compose.runtime.rememberCoroutineScope
import com.nuvio.app.features.player.seekpreview.LocalSeekPreviewInteraction
import com.nuvio.app.features.player.seekpreview.seekPreviewInput
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nuvio.app.core.ui.accentBrush
import com.nuvio.app.core.ui.themePalette
import com.nuvio.app.core.ui.nuvioTypeScale
import com.nuvio.app.features.streams.StreamBadge
import nuvio.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import com.nuvio.app.core.i18n.localizedEpisodeTitle

internal val PlayerTimelineContentInset = 2.dp

@Composable
internal fun PlayerTimelineDetails(
    title: String,
    seasonNumber: Int?,
    episodeNumber: Int?,
    episodeTitle: String?,
    releaseInfo: String?,
    metrics: PlayerLayoutMetrics,
    badges: List<StreamBadge> = emptyList(),
) {
    val typeScale = MaterialTheme.nuvioTypeScale
    Column(Modifier.padding(horizontal = PlayerTimelineContentInset)) {
        Text(
            text = title,
            style = typeScale.titleLg.copy(
                fontSize = metrics.titleSize,
                lineHeight = metrics.titleSize * 1.16f,
                fontWeight = FontWeight.SemiBold,
            ),
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (seasonNumber != null && episodeNumber != null) {
            val episodeCode = stringResource(Res.string.compose_player_episode_code_full, seasonNumber, episodeNumber)
            val displayEpisodeTitle = localizedEpisodeTitle(episodeTitle, episodeNumber)
            Text(
                text = if (displayEpisodeTitle.isNullOrBlank()) episodeCode else "$episodeCode • $displayEpisodeTitle",
                style = typeScale.bodyMd.copy(fontSize = metrics.episodeInfoSize),
                color = Color.White.copy(alpha = 0.9f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (badges.isNotEmpty()) {
            PlayerBadges(badges = badges, modifier = Modifier.padding(vertical = 4.dp))
        }
        if (!releaseInfo.isNullOrBlank()) {
            Text(
                text = releaseInfo,
                style = typeScale.labelSm.copy(fontSize = metrics.metadataSize),
                color = frostedGlassSecondaryContentColor(Color.White.copy(alpha = 0.68f)),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PlayerTimeline(
    snapshot: PlayerPlaybackSnapshot,
    displayedPositionMs: Long,
    onScrubChange: (Long) -> Unit,
    onScrubFinished: (Long) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    /** Each chapter after the first gets a mark where it starts. */
    chapters: List<PlayerChapter> = emptyList(),
    /** Layout metrics used to size the end-time label; null keeps the label off. */
    metrics: PlayerLayoutMetrics? = null,
    /** Swaps the end-time label between total duration and time remaining. */
    showRemainingTime: Boolean = false,
    /** Non-null shows the end-time label, which toggles elapsed/remaining on click. */
    onRuntimeClick: (() -> Unit)? = null,
) {
    val previewInteraction = LocalSeekPreviewInteraction.current
    val previewScope = rememberCoroutineScope()
    val durationMs = snapshot.durationMs.coerceAtLeast(0L)
    val rangeEnd = durationMs.coerceAtLeast(1L).toFloat()
    val rawBufferedFraction = (snapshot.bufferedPositionMs.toFloat() / rangeEnd).coerceIn(0f, 1f)
    val bufferedFraction = if (durationMs > 2000L && (snapshot.bufferedPositionMs >= durationMs - 2000L || snapshot.positionMs >= durationMs - 1500L || rawBufferedFraction >= 0.985f)) {
        1f
    } else {
        rawBufferedFraction
    }
    val accent = MaterialTheme.colorScheme.primary
    val accentBrush = MaterialTheme.themePalette.accentBrush()
    val description = stringResource(Res.string.player_seek_position)
    var scrubPosition by remember { mutableStateOf<Long?>(null) }
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val isDragged by interactionSource.collectIsDraggedAsState()
    val isInteracting = enabled && durationMs > 0L && (isPressed || isDragged)
    val trackThickness by animateDpAsState(
        targetValue = if (isInteracting) 10.dp else 6.dp,
        animationSpec = tween(durationMillis = if (isInteracting) 140 else 180),
        label = "playerTimelineThickness",
    )

    val sliderState = remember(rangeEnd) { SliderState(trackRange = 0f..rangeEnd) }
    sliderState.value = displayedPositionMs.coerceIn(0L, durationMs).toFloat()

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Row(
            modifier = modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
        ) {
            Slider(
                state = sliderState,
                onValueChange = { value ->
                    val position = value.toLong().coerceIn(0L, durationMs)
                    scrubPosition = position
                    if (previewInteraction?.dragging == false) previewInteraction.keyboard(position, previewScope)
                    onScrubChange(position)
                },
                onValueChangeFinished = {
                    val rawTarget = (scrubPosition ?: displayedPositionMs).coerceIn(0L, durationMs)
                    val target = if (durationMs > 2000L) rawTarget.coerceAtMost(durationMs - 1500L) else rawTarget
                    onScrubFinished(target)
                    scrubPosition = null
                },
                enabled = enabled && durationMs > 0L,
                interactionSource = interactionSource,
                thumb = {},
                track = { state ->
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .drawBehind {
                                val trackHeight = trackThickness.toPx()
                                val trackOrigin = Offset(0f, 35.dp.toPx() - trackHeight / 2)
                                val radius = CornerRadius(trackHeight / 2)
                                drawRoundRect(
                                    color = Color.White.copy(alpha = 0.3f),
                                    topLeft = trackOrigin,
                                    size = Size(size.width, trackHeight),
                                    cornerRadius = radius,
                                )
                                drawRoundRect(
                                    color = accent.copy(alpha = 0.35f),
                                    topLeft = trackOrigin,
                                    size = Size(size.width * bufferedFraction, trackHeight),
                                    cornerRadius = radius,
                                )
                                drawRoundRect(
                                    brush = accentBrush,
                                    topLeft = trackOrigin,
                                    size = Size(size.width * (state.value / rangeEnd).coerceIn(0f, 1f), trackHeight),
                                    cornerRadius = radius,
                                )
                                if (durationMs > 0L) {
                                    val markWidth = 2.dp.toPx()
                                    chapters.forEach { chapter ->
                                        val fraction = chapter.startMs / rangeEnd
                                        if (fraction <= 0f || fraction >= 1f) return@forEach
                                        drawRect(
                                            color = Color.Black.copy(alpha = 0.6f),
                                            topLeft = Offset(size.width * fraction - markWidth / 2f, trackOrigin.y),
                                            size = Size(markWidth, trackHeight),
                                        )
                                    }
                                }
                            },
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .seekPreviewInput(durationMs)
                    .fillMaxWidth()
                    .height(24.dp)
                    .wrapContentHeight(Alignment.Bottom, unbounded = true)
                    .requiredHeight(48.dp)
                    .semantics { contentDescription = description },
            )
            val runtimeClick = onRuntimeClick
            if (runtimeClick != null && durationMs > 0L) {
                TimelineEndTimeLabel(
                    text = if (showRemainingTime) {
                        formatPlaybackRuntime(displayedPositionMs, durationMs, true)
                    } else {
                        formatPlaybackTime(durationMs)
                    },
                    fontSize = metrics?.timeSize ?: 12.sp,
                    onClickLabel = stringResource(
                        if (showRemainingTime) Res.string.compose_player_show_elapsed_time else Res.string.compose_player_show_remaining_time,
                    ),
                    onClick = runtimeClick,
                )
            }
        }
    }
}

/**
 * Total duration (or time remaining) parked at the right end of the seek bar. Tapping it flips the
 * player between elapsed and remaining time; it is deliberately as compact as a small caption so the
 * track keeps its width.
 */
@Composable
private fun TimelineEndTimeLabel(
    text: String,
    fontSize: TextUnit,
    onClickLabel: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .height(28.dp)
            .widthIn(min = 52.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(role = Role.Button, onClickLabel = onClickLabel, onClick = onClick)
            .padding(start = 6.dp, end = 2.dp),
        contentAlignment = Alignment.CenterEnd,
    ) {
        Text(
            text = text,
            style = MaterialTheme.nuvioTypeScale.labelSm.copy(
                fontSize = fontSize,
                lineHeight = fontSize * 1.25f,
                fontWeight = FontWeight.Medium,
            ),
            color = Color.White.copy(alpha = 0.9f),
            maxLines = 1,
            textAlign = TextAlign.End,
        )
    }
}

/**
 * The name of the chapter at [positionMs], or "Chapter N" when the file gives it none. Hidden when
 * "Show Chapter Title" is off.
 */
@Composable
internal fun PlayerChapterTitle(
    chapters: List<PlayerChapter>,
    positionMs: Long,
    modifier: Modifier = Modifier,
) {
    val settings by PlayerSettingsRepository.uiState.collectAsState()
    if (!settings.chapterTitleEnabled) return
    val index = PlayerChapters.indexAt(chapters, positionMs)
    if (index < 0) return
    Text(
        text = chapters[index].title ?: stringResource(Res.string.compose_player_chapter_number, index + 1),
        style = MaterialTheme.nuvioTypeScale.bodyMd,
        color = Color.White.copy(alpha = 0.9f),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier.padding(horizontal = PlayerTimelineContentInset),
    )
}