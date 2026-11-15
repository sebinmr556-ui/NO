package com.example.ui

import android.net.Uri
import android.widget.VideoView
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import com.example.data.Story
import com.example.ui.theme.NoiseBlack
import com.example.ui.theme.NoiseBorder
import com.example.ui.theme.NoiseCard
import com.example.ui.theme.NoisePink
import com.example.ui.theme.NoisePurple
import com.example.ui.theme.NoiseTextMuted
import com.example.ui.theme.NoiseTextSecondary
import com.example.ui.theme.NoiseWhite
import kotlinx.coroutines.delay

@Composable
fun StoryViewerModal(
    stories: List<Story>,
    initialIndex: Int,
    currentUserId: String,
    isSuperOwner: Boolean = false,
    onStoryViewed: (Story) -> Unit,
    onAddMoreStoriesClick: () -> Unit,
    onDeleteStory: (Story) -> Unit = {},
    onReplyToStory: (Story, String) -> Unit,
    onClose: () -> Unit
) {
    BackHandler { onClose() }

    if (stories.isEmpty()) {
        onClose()
        return
    }

    val context = androidx.compose.ui.platform.LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    var currentIndex by remember(stories.size, initialIndex) {
        mutableIntStateOf(initialIndex.coerceIn(0, (stories.size - 1).coerceAtLeast(0)))
    }
    var progress by remember(currentIndex) { mutableFloatStateOf(0f) }
    var isPaused by remember { mutableStateOf(false) }
    var isMuted by remember { mutableStateOf(false) }
    var isLiked by remember(currentIndex) { mutableStateOf(false) }
    var replyText by remember(currentIndex) { mutableStateOf("") }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var verticalDragAccum by remember { mutableFloatStateOf(0f) }

    val activeStory = stories.getOrNull(currentIndex) ?: stories.first()
    val isOwnStory = activeStory.userId == currentUserId || isSuperOwner
    val storyMediaSource = activeStory.effectiveMediaSource()
    val isDeviceVideo = activeStory.mediaType == "video" &&
        !com.example.data.CloudMediaStorageManager.isPresetMediaKey(storyMediaSource) &&
        (storyMediaSource.startsWith("content://") ||
            storyMediaSource.startsWith("file://") ||
            storyMediaSource.startsWith("http") ||
            activeStory.cloudMediaId.isNotBlank() ||
            activeStory.storagePath.isNotBlank())

    var resolvedStoryUri by remember(activeStory.id, storyMediaSource) {
        mutableStateOf(
            if (storyMediaSource.startsWith("http") && storyMediaSource.contains("cloudMediaId=")) "" else storyMediaSource
        )
    }
    var isStoryResolving by remember(activeStory.id, storyMediaSource) { mutableStateOf(isDeviceVideo) }
    var isStoryVideoPrepared by remember(activeStory.id, storyMediaSource) { mutableStateOf(false) }
    var hasStoryRenderedFirstFrame by remember(activeStory.id, storyMediaSource) { mutableStateOf(false) }
    var isStoryBuffering by remember(activeStory.id, storyMediaSource) { mutableStateOf(false) }
    var hasStoryVideoError by remember(activeStory.id, storyMediaSource) { mutableStateOf(false) }
    var storyErrorDetail by remember(activeStory.id, storyMediaSource) { mutableStateOf("") }
    var storyRetryNonce by remember(activeStory.id, storyMediaSource) { mutableIntStateOf(0) }
    var actualVideoDurationMs by remember(activeStory.id, storyMediaSource) {
        mutableStateOf((activeStory.durationSec.coerceIn(5L, 3600L) * 1000L))
    }

    val advanceToNextStoryOrClose = {
        if (currentIndex < stories.lastIndex) {
            currentIndex += 1
        } else {
            onClose()
        }
    }

    LaunchedEffect(Unit) {
        NoiseVideoPlaybackCoordinator.pauseAll()
    }

    LaunchedEffect(activeStory.id) {
        onStoryViewed(activeStory)
    }

    LaunchedEffect(activeStory.id, storyMediaSource, storyRetryNonce) {
        if (!com.example.data.CloudMediaStorageManager.isPresetMediaKey(storyMediaSource) &&
            (storyMediaSource.isNotBlank() || activeStory.storagePath.isNotBlank() || activeStory.cloudMediaId.isNotBlank())
        ) {
            isStoryResolving = true
            hasStoryVideoError = false
            storyErrorDetail = ""
            val resolved = com.example.data.CloudMediaStorageManager.resolvePlayableMediaUri(
                context = context,
                mediaUrlOrKey = storyMediaSource,
                storagePath = activeStory.storagePath,
                explicitCloudMediaId = activeStory.cloudMediaId,
                mediaType = activeStory.mediaType
            )
            if (resolved.isNotBlank()) {
                resolvedStoryUri = resolved
                hasStoryVideoError = false
            } else if (isDeviceVideo) {
                hasStoryVideoError = true
                storyErrorDetail = "Story video could not be loaded from cloud storage."
                com.example.data.CloudMediaStorageManager.recordDiagnosticError(
                    stage = "StoryViewerModal.resolve",
                    detail = "Empty resolved URI for storyId=${activeStory.id} mediaSource=$storyMediaSource"
                )
            }
            isStoryResolving = false
        } else {
            isStoryResolving = false
        }
    }

    val storyExoPlayer = remember(context) {
        androidx.media3.exoplayer.ExoPlayer.Builder(context).build().apply {
            repeatMode = androidx.media3.common.Player.REPEAT_MODE_OFF
            playWhenReady = true
        }
    }

    androidx.compose.runtime.DisposableEffect(storyExoPlayer, activeStory.id, resolvedStoryUri, storyRetryNonce, isDeviceVideo) {
        val listener = object : androidx.media3.common.Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    androidx.media3.common.Player.STATE_BUFFERING -> {
                        isStoryBuffering = true
                    }
                    androidx.media3.common.Player.STATE_READY -> {
                        isStoryBuffering = false
                        isStoryVideoPrepared = true
                        hasStoryVideoError = false
                        val d = storyExoPlayer.duration
                        if (d > 0L && d != androidx.media3.common.C.TIME_UNSET) {
                            actualVideoDurationMs = d
                        }
                    }
                    androidx.media3.common.Player.STATE_ENDED -> {
                        isStoryBuffering = false
                        progress = 1f
                        advanceToNextStoryOrClose()
                    }
                    androidx.media3.common.Player.STATE_IDLE -> {
                        isStoryBuffering = false
                    }
                }
            }

            override fun onRenderedFirstFrame() {
                hasStoryRenderedFirstFrame = true
                isStoryVideoPrepared = true
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                isStoryBuffering = false
                hasStoryVideoError = true
                storyErrorDetail = "Video playback interrupted (${error.errorCodeName}). Tap Retry."
                com.example.data.CloudMediaStorageManager.recordDiagnosticError(
                    stage = "StoryViewerModal.ExoPlayer",
                    detail = "storyId=${activeStory.id} code=${error.errorCodeName} uri=$resolvedStoryUri",
                    throwable = error
                )
            }
        }

        storyExoPlayer.addListener(listener)

        if (isDeviceVideo && resolvedStoryUri.isNotBlank() && !isStoryResolving) {
            runCatching {
                isStoryVideoPrepared = false
                hasStoryRenderedFirstFrame = false
                hasStoryVideoError = false
                NoiseVideoPlaybackCoordinator.registerAndActivate(storyExoPlayer)
                storyExoPlayer.setMediaItem(androidx.media3.common.MediaItem.fromUri(Uri.parse(resolvedStoryUri)))
                storyExoPlayer.volume = if (isMuted) 0f else 1f
                storyExoPlayer.playWhenReady = !isPaused && !showDeleteConfirmation
                storyExoPlayer.prepare()
            }.onFailure { err ->
                hasStoryVideoError = true
                storyErrorDetail = err.localizedMessage ?: "Unable to start story video"
            }
        } else {
            runCatching {
                storyExoPlayer.stop()
                storyExoPlayer.clearMediaItems()
            }
        }

        onDispose {
            storyExoPlayer.removeListener(listener)
        }
    }

    androidx.compose.runtime.DisposableEffect(lifecycleOwner, storyExoPlayer) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            when (event) {
                androidx.lifecycle.Lifecycle.Event.ON_PAUSE,
                androidx.lifecycle.Lifecycle.Event.ON_STOP -> {
                    if (storyExoPlayer.isPlaying) {
                        storyExoPlayer.playWhenReady = false
                        storyExoPlayer.pause()
                    }
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            NoiseVideoPlaybackCoordinator.unregister(storyExoPlayer)
            runCatching {
                storyExoPlayer.stop()
                storyExoPlayer.clearMediaItems()
                storyExoPlayer.release()
            }
        }
    }

    LaunchedEffect(isPaused, showDeleteConfirmation, isMuted, isStoryVideoPrepared, isDeviceVideo, hasStoryVideoError) {
        if (isDeviceVideo) {
            runCatching {
                storyExoPlayer.volume = if (isMuted) 0f else 1f
                val shouldPlay = !isPaused && !showDeleteConfirmation && !hasStoryVideoError && isStoryVideoPrepared
                storyExoPlayer.playWhenReady = shouldPlay
                if (shouldPlay && !storyExoPlayer.isPlaying && storyExoPlayer.playbackState == androidx.media3.common.Player.STATE_READY) {
                    NoiseVideoPlaybackCoordinator.registerAndActivate(storyExoPlayer)
                    storyExoPlayer.play()
                } else if (!shouldPlay && storyExoPlayer.isPlaying) {
                    storyExoPlayer.pause()
                }
            }
        }
    }

    // Story Progress Indicator Engine:
    // - For Video Stories: driven directly by ExoPlayer currentPosition / actualVideoDurationMs;
    //   automatically pauses while resolving, buffering, paused, or on error.
    // - For Photo Stories: smoothly advances over story.durationSec.
    LaunchedEffect(
        currentIndex,
        activeStory.id,
        isDeviceVideo,
        isStoryResolving,
        isStoryVideoPrepared,
        isStoryBuffering,
        hasStoryVideoError,
        isPaused,
        showDeleteConfirmation,
        actualVideoDurationMs
    ) {
        if (isDeviceVideo) {
            while (progress < 1f) {
                delay(50L)
                if (!isPaused &&
                    !showDeleteConfirmation &&
                    !isStoryResolving &&
                    isStoryVideoPrepared &&
                    !isStoryBuffering &&
                    !hasStoryVideoError
                ) {
                    val pos = runCatching { storyExoPlayer.currentPosition.coerceAtLeast(0L) }.getOrDefault(0L)
                    val dur = runCatching {
                        val d = storyExoPlayer.duration
                        if (d > 0L && d != androidx.media3.common.C.TIME_UNSET) d else actualVideoDurationMs
                    }.getOrDefault(actualVideoDurationMs).coerceAtLeast(1000L)
                    actualVideoDurationMs = dur
                    progress = (pos.toFloat() / dur.toFloat()).coerceIn(0f, 0.995f)
                }
            }
        } else {
            val totalDurationMs = (activeStory.durationSec.coerceIn(4L, 30L) * 1000L)
            val stepMs = 50L
            val increment = stepMs.toFloat() / totalDurationMs.toFloat()
            while (progress < 1f) {
                delay(stepMs)
                if (!isPaused && !showDeleteConfirmation && !isStoryResolving) {
                    progress = (progress + increment).coerceAtMost(1f)
                }
            }
            advanceToNextStoryOrClose()
        }
    }

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 50, easing = LinearEasing),
        label = "story_progress"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NoiseBlack)
            .pointerInput(currentIndex, stories.size) {
                detectVerticalDragGestures(
                    onDragStart = { verticalDragAccum = 0f },
                    onVerticalDrag = { _, dragAmount ->
                        verticalDragAccum += dragAmount
                        if (verticalDragAccum > 140f) {
                            onClose()
                        }
                    }
                )
            }
            .pointerInput(currentIndex, stories.size) {
                detectTapGestures(
                    onPress = {
                        isPaused = true
                        tryAwaitRelease()
                        isPaused = false
                    },
                    onTap = { offset ->
                        val width = size.width
                        if (offset.x < width * 0.33f) {
                            if (currentIndex > 0) {
                                currentIndex -= 1
                            } else {
                                progress = 0f
                                if (isDeviceVideo) {
                                    runCatching { storyExoPlayer.seekTo(0L) }
                                }
                            }
                        } else {
                            advanceToNextStoryOrClose()
                        }
                    }
                )
            }
            .testTag("story_viewer_modal")
    ) {
        // Full-screen Story Media maintaining aspect ratio
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (isDeviceVideo && resolvedStoryUri.isNotBlank() && !hasStoryVideoError) {
                androidx.compose.runtime.key(activeStory.id, resolvedStoryUri, storyRetryNonce) {
                    AndroidView(
                        factory = { ctx ->
                            android.view.TextureView(ctx).apply {
                                storyExoPlayer.setVideoTextureView(this)
                            }
                        },
                        update = { textureView ->
                            storyExoPlayer.setVideoTextureView(textureView)
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            if (!isDeviceVideo || !hasStoryRenderedFirstFrame || hasStoryVideoError || resolvedStoryUri.isBlank()) {
                NoiseMediaImage(
                    mediaKey = if (activeStory.mediaType == "video" && activeStory.thumbnailUrl.isNotBlank()) {
                        activeStory.thumbnailUrl
                    } else {
                        storyMediaSource
                    },
                    contentDescription = activeStory.caption.ifBlank { "Story" },
                    contentScale = ContentScale.Fit,
                    thumbnailUrl = activeStory.thumbnailUrl,
                    storagePath = activeStory.storagePath,
                    cloudMediaId = activeStory.cloudMediaId,
                    modifier = Modifier.fillMaxSize()
                )
            }

            if (isDeviceVideo && (isStoryResolving || (isStoryBuffering && !hasStoryVideoError))) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.65f))
                        .border(1.5.dp, NoisePink.copy(alpha = 0.85f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.material3.CircularProgressIndicator(
                        color = NoisePink,
                        strokeWidth = 2.5.dp,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            if (isDeviceVideo && hasStoryVideoError) {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 28.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color.Black.copy(alpha = 0.85f))
                        .border(1.dp, NoisePink.copy(alpha = 0.75f), RoundedCornerShape(18.dp))
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = storyErrorDetail.ifBlank { "Unable to play story video." },
                        color = NoiseWhite,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            hasStoryVideoError = false
                            isStoryVideoPrepared = false
                            hasStoryRenderedFirstFrame = false
                            progress = 0f
                            storyRetryNonce += 1
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NoisePink),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.testTag("story_video_retry_button")
                    ) {
                        Text(
                            text = "Retry Story Video",
                            color = NoiseWhite,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            if (activeStory.mediaType == "video") {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(top = 86.dp, start = 16.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black.copy(alpha = 0.6f))
                        .border(1.dp, NoisePink.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    val durLabel = if (isDeviceVideo && actualVideoDurationMs > 0L) {
                        " • ${formatVideoDurationMs(actualVideoDurationMs)}"
                    } else {
                        ""
                    }
                    Text(
                        text = when {
                            isStoryResolving || isStoryBuffering -> "🎬 VIDEO STORY • BUFFERING..."
                            hasStoryVideoError -> "🎬 VIDEO STORY • ERROR"
                            isMuted -> "🎬 VIDEO CLIP • MUTED$durLabel"
                            else -> "🎬 VIDEO CLIP • PLAYING$durLabel"
                        },
                        color = NoiseWhite,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Top Overlay: Segmented Progress Bars + Author Header + Delete/Mute/Pause/Close Controls
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Black.copy(alpha = 0.78f), Color.Transparent)
                    )
                )
                .padding(horizontal = 12.dp, vertical = 12.dp)
        ) {
            // Segmented Progress Bars
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                stories.forEachIndexed { idx, _ ->
                    val segFill = when {
                        idx < currentIndex -> 1f
                        idx == currentIndex -> animatedProgress
                        else -> 0f
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(3.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color.White.copy(alpha = 0.3f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(segFill.coerceIn(0f, 1f))
                                .background(Brush.horizontalGradient(listOf(NoisePink, NoisePurple)))
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    NoiseAvatar(
                        avatarKey = activeStory.authorAvatarKey.ifBlank { activeStory.username.take(1).uppercase() },
                        size = 38.dp,
                        showOnlineDot = true
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = activeStory.username,
                                color = NoiseWhite,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${currentIndex + 1}/${stories.size}",
                                color = NoiseTextSecondary,
                                fontSize = 12.sp
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Visibility,
                                contentDescription = null,
                                tint = NoiseTextSecondary,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${activeStory.viewedByUids.size.coerceAtLeast(1)} viewed • Hold to pause",
                                color = NoiseTextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isPaused) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(NoisePink.copy(alpha = 0.25f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Pause,
                                    contentDescription = "Paused",
                                    tint = NoiseWhite,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("PAUSED", color = NoiseWhite, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                    }

                    // Add another story directly from viewer
                    IconButton(
                        onClick = onAddMoreStoriesClick,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("story_add_more_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Story",
                            tint = NoiseWhite
                        )
                    }

                    // Delete Story button (only shown for user's own uploaded stories)
                    if (isOwnStory) {
                        IconButton(
                            onClick = { showDeleteConfirmation = true },
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("story_delete_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Delete Story",
                                tint = NoisePink
                            )
                        }
                    }

                    // Mute / Unmute toggle
                    IconButton(
                        onClick = { isMuted = !isMuted },
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("story_mute_toggle_button")
                    ) {
                        Icon(
                            imageVector = if (isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = if (isMuted) "Unmute Story" else "Mute Story",
                            tint = NoiseWhite
                        )
                    }

                    IconButton(
                        onClick = onClose,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("story_close_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Story",
                            tint = NoiseWhite
                        )
                    }
                }
            }
        }

        // Bottom Caption + Instagram-style Story Reply & Reaction Bar
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.88f))
                    )
                )
                .padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            if (activeStory.caption.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.Black.copy(alpha = 0.55f))
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = activeStory.caption,
                        color = NoiseWhite,
                        fontSize = 14.sp
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = replyText,
                    onValueChange = {
                        replyText = it
                        isPaused = it.isNotBlank()
                    },
                    placeholder = {
                        Text(
                            text = "Send message to @${activeStory.username}...",
                            color = NoiseTextMuted,
                            fontSize = 13.sp
                        )
                    },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = NoiseCard,
                        unfocusedContainerColor = NoiseCard.copy(alpha = 0.8f),
                        focusedBorderColor = NoisePink,
                        unfocusedBorderColor = NoiseBorder,
                        focusedTextColor = NoiseWhite,
                        unfocusedTextColor = NoiseWhite
                    )
                )

                IconButton(
                    onClick = { isLiked = !isLiked }
                ) {
                    Icon(
                        imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Like Story",
                        tint = if (isLiked) NoisePink else NoiseWhite,
                        modifier = Modifier.size(26.dp)
                    )
                }

                IconButton(
                    onClick = {
                        val msg = replyText.ifBlank { "🔥 Replied to your story!" }
                        onReplyToStory(activeStory, msg)
                        replyText = ""
                        isPaused = false
                    }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send Story Reply",
                        tint = NoisePink,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        // Story Deletion Confirmation Dialog
        if (showDeleteConfirmation) {
            Dialog(onDismissRequest = { showDeleteConfirmation = false }) {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = Color(0xFF14131D),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, NoisePink.copy(alpha = 0.6f), RoundedCornerShape(22.dp))
                        .testTag("story_delete_confirm_dialog")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Text(
                            text = "Delete Story?",
                            color = NoiseWhite,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Are you sure you want to delete this story? It will be permanently removed from the Home Screen and storage.",
                            color = NoiseTextSecondary,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(18.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color(0xFF222130))
                                    .clickable { showDeleteConfirmation = false }
                                    .testTag("story_delete_cancel_button"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Cancel",
                                    color = NoiseWhite,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                            Button(
                                onClick = {
                                    showDeleteConfirmation = false
                                    val storyToDelete = activeStory
                                    onDeleteStory(storyToDelete)
                                    if (stories.size <= 1) {
                                        onClose()
                                    } else if (currentIndex >= stories.lastIndex) {
                                        currentIndex = (stories.lastIndex - 1).coerceAtLeast(0)
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .testTag("story_delete_confirm_button"),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = NoisePink)
                            ) {
                                Text(
                                    text = "Delete",
                                    color = NoiseWhite,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
