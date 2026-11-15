package com.example.ui

import android.annotation.SuppressLint
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SmartDisplay
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.PartyRoom
import com.example.ui.theme.NoiseBlack
import com.example.ui.theme.NoiseBorder
import com.example.ui.theme.NoiseCard
import com.example.ui.theme.NoisePink
import com.example.ui.theme.NoisePurple
import com.example.ui.theme.NoiseTextMuted
import com.example.ui.theme.NoiseTextSecondary
import com.example.ui.theme.NoiseWhite
import com.example.ui.theme.OnlineGreen
import com.example.ui.theme.VipGold
import com.example.ui.theme.VipGoldBorder
import kotlinx.coroutines.delay

private class YouTubeJsBridge(
    private val onPlayerReadyCallback: () -> Unit,
    private val onCurrentTimeReported: (Long) -> Unit
) {
    @JavascriptInterface
    fun onPlayerReady() {
        onPlayerReadyCallback()
    }

    @JavascriptInterface
    fun onTimeUpdate(seconds: Double) {
        if (!seconds.isNaN() && seconds >= 0.0) {
            onCurrentTimeReported(seconds.toLong())
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun SynchronizedYouTubeRoomPlayer(
    room: PartyRoom,
    canControlPlayback: Boolean,
    isSpeakerOn: Boolean,
    onSyncYouTubeState: (videoUrlOrId: String, videoTitle: String, isPlaying: Boolean, positionSec: Long) -> Unit,
    onOpenChangeVideoDialog: () -> Unit,
    modifier: Modifier = Modifier
) {
    val rawVideoUrl = room.youtubeVideoUrl.trim()
    val isDirectVideoUrl = rawVideoUrl.contains(".mp4", ignoreCase = true) ||
        rawVideoUrl.contains("cloudMediaId=") ||
        rawVideoUrl.startsWith("file://") ||
        rawVideoUrl.startsWith("content://")
    val videoId = room.resolvedYouTubeVideoId()
    if (videoId.isBlank() && !isDirectVideoUrl) return

    var isCollapsed by remember { mutableStateOf(false) }
    // Start muted by default to comply with WebView / browser autoplay policies; user taps once to unmute
    var isLocalAudioMuted by remember { mutableStateOf(false) }
    var isPlayerReady by remember { mutableStateOf(false) }
    var localReportedSec by remember { mutableLongStateOf(room.estimatedYouTubePositionSec()) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    // Keep local reported time updated every second for smooth UI & drift checks
    LaunchedEffect(videoId, room.youtubePlaying, room.youtubePositionSec, room.youtubeUpdatedAtMillis, isPlayerReady) {
        while (true) {
            val authoritativeSec = room.estimatedYouTubePositionSec(System.currentTimeMillis())
            localReportedSec = authoritativeSec
            val wv = webViewRef
            if (wv != null && isPlayerReady) {
                val safeId = videoId.replace("'", "")
                val playFlag = if (room.youtubePlaying) "true" else "false"
                wv.post {
                    wv.evaluateJavascript(
                        "window.syncFromRoomState && window.syncFromRoomState('$safeId', $playFlag, $authoritativeSec);",
                        null
                    )
                }
            }
            delay(2500L)
        }
    }

    // Immediate sync whenever room's YouTube state changes in Firestore
    LaunchedEffect(videoId, room.youtubePlaying, room.youtubePositionSec, room.youtubeUpdatedAtMillis, isPlayerReady) {
        val wv = webViewRef ?: return@LaunchedEffect
        if (!isPlayerReady) return@LaunchedEffect
        val authoritativeSec = room.estimatedYouTubePositionSec(System.currentTimeMillis())
        val safeId = videoId.replace("'", "")
        val playFlag = if (room.youtubePlaying) "true" else "false"
        wv.post {
            wv.evaluateJavascript(
                "window.syncFromRoomState && window.syncFromRoomState('$safeId', $playFlag, $authoritativeSec);",
                null
            )
        }
    }

    // Sync local mute/unmute with room speaker toggle
    LaunchedEffect(isLocalAudioMuted, isSpeakerOn, isPlayerReady) {
        val wv = webViewRef ?: return@LaunchedEffect
        if (!isPlayerReady) return@LaunchedEffect
        val effectiveMuted = isLocalAudioMuted || !isSpeakerOn
        wv.post {
            wv.evaluateJavascript(
                "window.setPlayerMuted && window.setPlayerMuted($effectiveMuted);",
                null
            )
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            webViewRef?.let { wv ->
                runCatching {
                    wv.loadUrl("about:blank")
                    wv.stopLoading()
                    wv.destroy()
                }
            }
            webViewRef = null
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xE6100B1C))
            .border(
                width = 1.3.dp,
                brush = Brush.linearGradient(listOf(NoisePink, NoisePurple, VipGold)),
                shape = RoundedCornerShape(18.dp)
            )
            .padding(10.dp)
            .testTag("party_room_youtube_player_card")
    ) {
        // Top Header Bar of the Synchronized YouTube Player
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFFF0033)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SmartDisplay,
                        contentDescription = "YouTube Live",
                        tint = NoiseWhite,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = room.youtubeVideoTitle.ifBlank { "YouTube Watch Together" },
                            color = NoiseWhite,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = if (room.youtubePlaying) {
                            "🔴 LIVE SYNC • ${formatYouTubeSeconds(localReportedSec)} • Host @${room.hostUsername}"
                        } else {
                            "⏸️ PAUSED AT ${formatYouTubeSeconds(localReportedSec)} • Host @${room.hostUsername}"
                        },
                        color = if (room.youtubePlaying) VipGold else NoiseTextSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Local Audio Mute / Unmute Pill
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (isLocalAudioMuted || !isSpeakerOn) Color(0xFF2B1422)
                            else OnlineGreen.copy(alpha = 0.22f)
                        )
                        .border(
                            1.dp,
                            if (isLocalAudioMuted || !isSpeakerOn) NoisePink else OnlineGreen,
                            RoundedCornerShape(10.dp)
                        )
                        .clickable {
                            isLocalAudioMuted = !isLocalAudioMuted
                        }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .testTag("youtube_player_mute_toggle"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isLocalAudioMuted || !isSpeakerOn) {
                            Icons.AutoMirrored.Filled.VolumeOff
                        } else {
                            Icons.AutoMirrored.Filled.VolumeUp
                        },
                        contentDescription = "Toggle YouTube Audio",
                        tint = if (isLocalAudioMuted || !isSpeakerOn) NoisePink else OnlineGreen,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isLocalAudioMuted || !isSpeakerOn) "Unmute" else "Audio On",
                        color = NoiseWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                }

                // Minimize / Expand Video Stage Button
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.1f))
                        .clickable { isCollapsed = !isCollapsed }
                        .testTag("youtube_player_collapse_toggle"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isCollapsed) Icons.Default.ExpandMore else Icons.Default.ExpandLess,
                        contentDescription = if (isCollapsed) "Expand Video" else "Minimize Video",
                        tint = NoiseWhite,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        if (!isCollapsed) {
            Spacer(modifier = Modifier.height(8.dp))

            // Official YouTube IFrame Player API WebView
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(168.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.Black)
                    .border(1.dp, NoiseBorder, RoundedCornerShape(14.dp))
                    .testTag("youtube_iframe_webview_container")
            ) {
                if (isDirectVideoUrl) {
                    NoiseVideoPlayer(
                        mediaKey = rawVideoUrl,
                        contentDescription = room.youtubeVideoTitle.ifBlank { "Shared Chatroom Video" },
                        autoPlay = room.youtubePlaying,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    val initialStartSec = remember(videoId) { room.estimatedYouTubePositionSec() }
                    val initialAutoplay = remember(videoId) { room.youtubePlaying }

                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { ctx ->
                            WebView(ctx).apply {
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                settings.mediaPlaybackRequiresUserGesture = false
                                settings.loadsImagesAutomatically = true
                                settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                                webChromeClient = WebChromeClient()
                                webViewClient = WebViewClient()

                                addJavascriptInterface(
                                    YouTubeJsBridge(
                                        onPlayerReadyCallback = {
                                            isPlayerReady = true
                                        },
                                        onCurrentTimeReported = { sec ->
                                            localReportedSec = sec
                                        }
                                    ),
                                    "NoiseYTBridge"
                                )

                                val html = buildYouTubeIFrameHtml(
                                    videoId = videoId,
                                    startSeconds = initialStartSec,
                                    isPlaying = initialAutoplay,
                                    isMuted = isLocalAudioMuted || !isSpeakerOn,
                                    allowControls = canControlPlayback
                                )
                                loadDataWithBaseURL(
                                    "https://www.youtube-nocookie.com",
                                    html,
                                    "text/html",
                                    "utf-8",
                                    null
                                )
                                webViewRef = this
                            }
                        },
                        update = { wv ->
                            webViewRef = wv
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Synchronized Playback Controls Bar (Owner/Moderator Controls vs Participant Read-Only Sync Badge)
        if (canControlPlayback) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Rewind -15s
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF1F1930))
                            .border(1.dp, NoiseBorder, RoundedCornerShape(10.dp))
                            .clickable {
                                val nextPos = (room.estimatedYouTubePositionSec() - 15L).coerceAtLeast(0L)
                                onSyncYouTubeState(
                                    room.youtubeVideoUrl.ifBlank { videoId },
                                    room.youtubeVideoTitle,
                                    room.youtubePlaying,
                                    nextPos
                                )
                            }
                            .padding(horizontal = 8.dp, vertical = 5.dp)
                            .testTag("youtube_sync_rewind_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.FastRewind,
                                contentDescription = "Rewind 15s",
                                tint = NoiseWhite,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text("-15s", color = NoiseWhite, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Play / Pause Shared Playback
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(Brush.horizontalGradient(listOf(NoisePink, NoisePurple)))
                            .clickable {
                                val currentSec = room.estimatedYouTubePositionSec()
                                onSyncYouTubeState(
                                    room.youtubeVideoUrl.ifBlank { videoId },
                                    room.youtubeVideoTitle,
                                    !room.youtubePlaying,
                                    currentSec
                                )
                            }
                            .padding(horizontal = 12.dp, vertical = 5.dp)
                            .testTag("youtube_sync_play_pause_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (room.youtubePlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (room.youtubePlaying) "Pause for All" else "Play for All",
                                tint = NoiseWhite,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (room.youtubePlaying) "Pause All" else "Play All",
                                color = NoiseWhite,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }

                    // Forward +15s
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF1F1930))
                            .border(1.dp, NoiseBorder, RoundedCornerShape(10.dp))
                            .clickable {
                                val nextPos = room.estimatedYouTubePositionSec() + 15L
                                onSyncYouTubeState(
                                    room.youtubeVideoUrl.ifBlank { videoId },
                                    room.youtubeVideoTitle,
                                    room.youtubePlaying,
                                    nextPos
                                )
                            }
                            .padding(horizontal = 8.dp, vertical = 5.dp)
                            .testTag("youtube_sync_forward_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("+15s", color = NoiseWhite, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(2.dp))
                            Icon(
                                imageVector = Icons.Default.FastForward,
                                contentDescription = "Forward 15s",
                                tint = NoiseWhite,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Change Video URL
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF1F1930))
                            .border(1.dp, VipGold.copy(alpha = 0.7f), RoundedCornerShape(10.dp))
                            .clickable { onOpenChangeVideoDialog() }
                            .padding(horizontal = 8.dp, vertical = 5.dp)
                            .testTag("youtube_change_video_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Change URL", color = VipGold, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }

                    // Stop & Remove Shared Video
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF3A121E))
                            .border(1.dp, NoisePink, RoundedCornerShape(10.dp))
                            .clickable {
                                onSyncYouTubeState("", "", false, 0L)
                            }
                            .padding(horizontal = 8.dp, vertical = 5.dp)
                            .testTag("youtube_stop_video_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Stop,
                            contentDescription = "Stop Shared Video",
                            tint = NoisePink,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = VipGold,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Playback synced in real time by Room Owner @${room.hostUsername}",
                        color = NoiseTextSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                Text(
                    text = formatYouTubeSeconds(localReportedSec),
                    color = VipGold,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun YouTubeWatchTogetherDialog(
    currentVideoUrl: String,
    currentVideoTitle: String,
    canControlPlayback: Boolean,
    onStartOrUpdateVideo: (videoUrlOrId: String, videoTitle: String) -> Unit,
    onStopVideo: () -> Unit,
    onDismiss: () -> Unit
) {
    var videoInput by remember { mutableStateOf(currentVideoUrl) }
    var titleInput by remember { mutableStateOf(currentVideoTitle) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val presetStreams = remember {
        listOf(
            Triple("jfKfPfyJRdk", "Lofi Hip Hop Radio – Beats to Relax/Study Live", "https://www.youtube.com/watch?v=jfKfPfyJRdk"),
            Triple("4xDzrJKXOOY", "Synthwave Radio – Chill / Retro Beats Live", "https://www.youtube.com/watch?v=4xDzrJKXOOY"),
            Triple("21X5lGlDOfg", "NASA Live – Official Stream from Space", "https://www.youtube.com/watch?v=21X5lGlDOfg"),
            Triple("5qap5aO4i9A", "VIP Party Chill Mix – Live Stream", "https://www.youtube.com/watch?v=5qap5aO4i9A")
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
                .border(1.4.dp, VipGoldBorder, RoundedCornerShape(24.dp))
                .testTag("youtube_watch_together_dialog"),
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF0E0B16)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFFF0033)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.SmartDisplay,
                                contentDescription = "YouTube Live",
                                tint = NoiseWhite,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "YOUTUBE LIVE / WATCH TOGETHER",
                                color = NoiseWhite,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp
                            )
                            Text(
                                text = "Synchronized video & live stream for everyone in room",
                                color = VipGold,
                                fontSize = 11.sp
                            )
                        }
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(30.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = NoiseTextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = NoiseBorder)
                Spacer(modifier = Modifier.height(12.dp))

                if (!canControlPlayback) {
                    Text(
                        text = "Only the Room Owner, Admin, or Moderator can change or control the shared YouTube video.",
                        color = NoisePink,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                } else {
                    if (errorMessage != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF3B1219))
                                .border(1.dp, NoisePink, RoundedCornerShape(10.dp))
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = errorMessage ?: "",
                                color = NoisePink,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    Text(
                        text = "YouTube Video or YouTube Live URL / ID *",
                        color = NoiseWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = videoInput,
                        onValueChange = {
                            videoInput = it
                            errorMessage = null
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Link, contentDescription = null, tint = NoisePink)
                        },
                        placeholder = {
                            Text(
                                text = "https://www.youtube.com/watch?v=... or Live URL",
                                color = NoiseTextMuted,
                                fontSize = 12.sp
                            )
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("youtube_url_input"),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF161224),
                            unfocusedContainerColor = Color(0xFF161224),
                            focusedBorderColor = NoisePink,
                            unfocusedBorderColor = NoiseBorder,
                            focusedTextColor = NoiseWhite,
                            unfocusedTextColor = NoiseWhite
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Stream / Video Title (Optional)",
                        color = NoiseWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = titleInput,
                        onValueChange = { titleInput = it },
                        placeholder = {
                            Text(
                                text = "e.g. Friday Night Live Stream 🎬",
                                color = NoiseTextMuted,
                                fontSize = 12.sp
                            )
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("youtube_title_input"),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF161224),
                            unfocusedContainerColor = Color(0xFF161224),
                            focusedBorderColor = VipGold,
                            unfocusedBorderColor = NoiseBorder,
                            focusedTextColor = NoiseWhite,
                            unfocusedTextColor = NoiseWhite
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Quick Select Live Stream / Video:",
                        color = VipGold,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        presetStreams.forEach { (id, label, url) ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF1A142B))
                                    .border(1.dp, NoisePurple.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
                                    .clickable {
                                        videoInput = url
                                        titleInput = label
                                        errorMessage = null
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                                    .testTag("youtube_preset_$id")
                            ) {
                                Text(
                                    text = label,
                                    color = NoiseWhite,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (currentVideoUrl.isNotBlank()) {
                            Box(
                                modifier = Modifier
                                    .weight(0.9f)
                                    .height(44.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color(0xFF3A121E))
                                    .border(1.dp, NoisePink, RoundedCornerShape(14.dp))
                                    .clickable {
                                        onStopVideo()
                                        onDismiss()
                                    }
                                    .testTag("youtube_dialog_stop_button"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Stop Video",
                                    color = NoisePink,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }

                        Button(
                            onClick = {
                                val extractedId = PartyRoom.extractYouTubeVideoId(videoInput)
                                if (extractedId.isBlank()) {
                                    errorMessage = "Please enter a valid YouTube Video or YouTube Live URL / 11-char ID."
                                } else {
                                    onStartOrUpdateVideo(videoInput.trim(), titleInput.trim())
                                    onDismiss()
                                }
                            },
                            modifier = Modifier
                                .weight(1.4f)
                                .height(44.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Brush.horizontalGradient(listOf(NoisePink, NoisePurple)))
                                .testTag("youtube_start_sync_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
                        ) {
                            Text(
                                text = "START WATCH TOGETHER",
                                color = NoiseWhite,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatYouTubeSeconds(totalSeconds: Long): String {
    val safe = totalSeconds.coerceAtLeast(0L)
    val hrs = safe / 3600L
    val mins = (safe % 3600L) / 60L
    val secs = safe % 60L
    return if (hrs > 0L) {
        String.format("%d:%02d:%02d", hrs, mins, secs)
    } else {
        String.format("%02d:%02d", mins, secs)
    }
}

private fun buildYouTubeIFrameHtml(
    videoId: String,
    startSeconds: Long,
    isPlaying: Boolean,
    isMuted: Boolean,
    allowControls: Boolean
): String {
    val safeVideoId = videoId.replace(Regex("[^a-zA-Z0-9_-]"), "")
    val controlsFlag = if (allowControls) 1 else 0
    val autoplayFlag = if (isPlaying) 1 else 0
    val muteFlag = if (isMuted) 1 else 0
    return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
            <style>
                html, body {
                    margin: 0;
                    padding: 0;
                    width: 100%;
                    height: 100%;
                    background-color: #000000;
                    overflow: hidden;
                }
                #player {
                    width: 100%;
                    height: 100%;
                }
            </style>
        </head>
        <body>
            <div id="player"></div>
            <script src="https://www.youtube.com/iframe_api"></script>
            <script>
                var player = null;
                var currentVideoId = '$safeVideoId';
                var desiredPlaying = ${if (isPlaying) "true" else "false"};
                var desiredSeconds = $startSeconds;
                var desiredMuted = ${if (isMuted) "true" else "false"};
                var isReady = false;

                function onYouTubeIframeAPIReady() {
                    player = new YT.Player('player', {
                        width: '100%',
                        height: '100%',
                        videoId: currentVideoId,
                        playerVars: {
                            'playsinline': 1,
                            'autoplay': $autoplayFlag,
                            'start': $startSeconds,
                            'controls': $controlsFlag,
                            'rel': 0,
                            'modestbranding': 1,
                            'enablejsapi': 1,
                            'origin': 'https://www.youtube-nocookie.com'
                        },
                        events: {
                            'onReady': onPlayerReady,
                            'onStateChange': onPlayerStateChange
                        }
                    });
                }

                function onPlayerReady(event) {
                    isReady = true;
                    if (desiredMuted) {
                        player.mute();
                    } else {
                        player.unMute();
                        player.setVolume(100);
                    }
                    if (desiredSeconds > 0) {
                        player.seekTo(desiredSeconds, true);
                    }
                    if (desiredPlaying) {
                        player.playVideo();
                    } else {
                        player.pauseVideo();
                    }
                    if (window.NoiseYTBridge && window.NoiseYTBridge.onPlayerReady) {
                        window.NoiseYTBridge.onPlayerReady();
                    }
                    setInterval(function() {
                        if (isReady && player && player.getCurrentTime) {
                            var t = player.getCurrentTime();
                            if (window.NoiseYTBridge && window.NoiseYTBridge.onTimeUpdate) {
                                window.NoiseYTBridge.onTimeUpdate(t);
                            }
                        }
                    }, 1000);
                }

                function onPlayerStateChange(event) {
                    if (!isReady || !player) return;
                }

                window.syncFromRoomState = function(videoId, shouldPlay, targetSec) {
                    desiredPlaying = shouldPlay;
                    desiredSeconds = targetSec;
                    if (!isReady || !player) return;

                    if (videoId && videoId !== currentVideoId) {
                        currentVideoId = videoId;
                        if (shouldPlay) {
                            player.loadVideoById({'videoId': videoId, 'startSeconds': targetSec});
                        } else {
                            player.cueVideoById({'videoId': videoId, 'startSeconds': targetSec});
                        }
                        return;
                    }

                    var currentSec = player.getCurrentTime ? player.getCurrentTime() : 0;
                    var drift = Math.abs(currentSec - targetSec);
                    if (drift > 2.8) {
                        player.seekTo(targetSec, true);
                    }

                    var state = player.getPlayerState ? player.getPlayerState() : -1;
                    // YT.PlayerState.PLAYING == 1, PAUSED == 2, BUFFERING == 3
                    if (shouldPlay && state !== 1 && state !== 3) {
                        player.playVideo();
                    } else if (!shouldPlay && state === 1) {
                        player.pauseVideo();
                    }
                };

                window.setPlayerMuted = function(muted) {
                    desiredMuted = muted;
                    if (!isReady || !player) return;
                    if (muted) {
                        player.mute();
                    } else {
                        player.unMute();
                        player.setVolume(100);
                    }
                };
            </script>
        </body>
        </html>
    """.trimIndent()
}
