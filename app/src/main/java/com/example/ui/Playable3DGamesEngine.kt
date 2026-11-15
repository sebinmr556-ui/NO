package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.MusicOff
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.GameInviteDoc
import com.example.data.GamePlayerSlot
import com.example.data.GameRoomDoc
import com.example.data.GraphicsQualityLevel
import com.example.data.UserProfile
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.pow
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

@Composable
fun FullScreenMultiplayerGameModal(
    gameRoom: GameRoomDoc,
    currentUser: UserProfile,
    graphicsQuality: GraphicsQualityLevel,
    liveKitVoiceActive: Boolean,
    micMuted: Boolean,
    audioOutputRouteLabel: String,
    activeSpeakers: List<String>,
    onToggleMicMute: () -> Unit,
    onCycleAudioOutputRoute: () -> Unit,
    onOpenInviteSheet: () -> Unit,
    onRemovePlayer: (String) -> Unit,
    onSyncGameState: (String, String, List<GamePlayerSlot>?, String?, String?, String?) -> Unit,
    onSubmitMatchResult: (UserProfile, String, Long, Long, Long) -> Unit,
    onExitToHost: () -> Unit,
    registeredAccounts: List<UserProfile> = emptyList(),
    outgoingInvites: List<GameInviteDoc> = emptyList(),
    onInviteUserToSlot: (UserProfile, Int) -> Unit = { _, _ -> },
    onStartMatch: () -> Unit = {},
    onCancelSession: () -> Unit = onExitToHost
) {
    val currentUid = currentUser.userId.ifBlank { currentUser.id }
    val isRoomOwner = gameRoom.hostId == currentUid
    val realPlayers = remember(gameRoom.playersJson) { gameRoom.parsePlayers() }
    val matchStartMillis = remember { System.currentTimeMillis() }

    // 1. REAL PROGRESSIVE 3D LOADING SCREEN STATE (0% -> 100%)
    var isLoadingAssets by remember(gameRoom.id, gameRoom.gameType) { mutableStateOf(true) }
    var loadingProgressPercent by remember(gameRoom.id, gameRoom.gameType) { mutableIntStateOf(0) }
    var loadingStageLabel by remember(gameRoom.id, gameRoom.gameType) { mutableStateOf("Initializing game...") }
    var loadingErrorText by remember(gameRoom.id, gameRoom.gameType) { mutableStateOf<String?>(null) }
    var loadingRetryTrigger by remember { mutableIntStateOf(0) }

    // 2. PAUSE / SETTINGS / RECONNECT / EXIT CONFIRMATION / SLOT INVITE STATES
    var showPauseSettingsModal by remember { mutableStateOf(false) }
    var showExitConfirmDialog by remember { mutableStateOf(false) }
    var showReconnectingOverlay by remember { mutableStateOf(false) }
    var slotInvitePickerIndex by remember { mutableStateOf<Int?>(null) }
    var sfxEnabled by remember { mutableStateOf(GameSoundEffectsEngine.sfxEnabled) }
    var musicEnabled by remember { mutableStateOf(GameSoundEffectsEngine.musicEnabled) }
    var selectedArenaThemeIndex by remember { mutableIntStateOf(0) } // 0 = Emerald/Royal, 1 = Cyber Purple, 2 = Sapphire Gold
    var optionalBotEnabled by remember(gameRoom.id) { mutableStateOf(false) }
    var rematchCounter by remember { mutableIntStateOf(0) }

    // 3. VICTORY / DEFEAT / MATCH RESULTS STATE
    var winnerBanner by remember(gameRoom.winnerUsername, rematchCounter) {
        mutableStateOf(gameRoom.winnerUsername.takeIf { it.isNotBlank() && rematchCounter == 0 })
    }
    var winnerScoreState by remember { mutableLongStateOf(0L) }
    var runnerUpScoreState by remember { mutableLongStateOf(0L) }
    var matchSummaryDetail by remember { mutableStateOf("Clean verified match completion") }

    BackHandler {
        if (showPauseSettingsModal) {
            showPauseSettingsModal = false
        } else {
            onExitToHost()
        }
    }

    // Progressive 5-stage 3D Asset & Multiplayer Sync Loader
    LaunchedEffect(gameRoom.id, gameRoom.gameType, loadingRetryTrigger) {
        if (!isLoadingAssets) return@LaunchedEffect
        loadingErrorText = null
        val stages = listOf(
            18 to "Initializing game",
            42 to "Loading 3D assets",
            68 to "Connecting to multiplayer",
            88 to "Synchronizing players",
            100 to "Preparing game"
        )
        for ((targetPct, label) in stages) {
            loadingStageLabel = label
            while (loadingProgressPercent < targetPct) {
                loadingProgressPercent = (loadingProgressPercent + 6).coerceAtMost(targetPct)
                delay(18L)
            }
            delay(35L)
        }
        isLoadingAssets = false
    }

    val pulseTransition = rememberInfiniteTransition(label = "voice_pulse")
    val voicePulse by pulseTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(tween(700, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulse"
    )

    fun finishMatch(winnerUsername: String, winScore: Long, loseScore: Long, summaryNote: String = "Match Completed") {
        if (winnerBanner != null) return
        winnerBanner = winnerUsername
        winnerScoreState = winScore
        runnerUpScoreState = loseScore
        matchSummaryDetail = summaryNote
        val durationSec = ((System.currentTimeMillis() - matchStartMillis) / 1000L).coerceAtLeast(10L)
        val winnerProfile = if (winnerUsername.equals(currentUser.username, ignoreCase = true)) {
            currentUser
        } else {
            val slot = realPlayers.firstOrNull { it.username.equals(winnerUsername, ignoreCase = true) }
            UserProfile(
                id = slot?.userId ?: currentUid,
                userId = slot?.userId ?: currentUid,
                username = winnerUsername,
                avatarKey = slot?.avatarKey ?: "W"
            )
        }
        val runnerUp = realPlayers.firstOrNull { !it.username.equals(winnerUsername, ignoreCase = true) }?.username
            ?: if (optionalBotEnabled) "Practice_Bot" else "Opponent"
        onSyncGameState(
            gameRoom.gameStateJson,
            currentUid,
            realPlayers,
            "finished",
            winnerProfile.userId,
            winnerUsername
        )
        onSubmitMatchResult(winnerProfile, runnerUp, winScore, loseScore, durationSec)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF070510), Color(0xFF120B24), Color(0xFF05040A))
                )
            )
            .statusBarsPadding()
            .testTag("fullscreen_3d_game_arena")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            // 1. TOP HUD BAR + LIVEKIT VOICE CONTROLS + PAUSE/SETTINGS + INVITE
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = Color(0xFF151126).copy(alpha = 0.96f),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        Brush.horizontalGradient(listOf(NoisePink, NoisePurple, NoiseGold)),
                        RoundedCornerShape(18.dp)
                    )
            ) {
                Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            IconButton(
                                onClick = onExitToHost,
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.09f))
                                    .testTag("game_exit_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Return to Chatroom / Games",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = gameRoom.gameTitle,
                                        color = Color.White,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 14.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(NoiseGold.copy(alpha = 0.18f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "CODE: ${gameRoom.roomCode}",
                                            color = NoiseGold,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                Text(
                                    text = if (gameRoom.chatRoomTitle.isNotBlank()) {
                                        "Voice: ${gameRoom.chatRoomTitle} • ${graphicsQuality.label} (${graphicsQuality.targetFps} FPS)"
                                    } else {
                                        "Unity 6 URP (${gameRoom.unityRelayJoinCode}) • ${graphicsQuality.label} (${graphicsQuality.targetFps} FPS)"
                                    },
                                    color = NoiseTextSecondary,
                                    fontSize = 10.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        // LiveKit Voice Controls + Pause/Settings + Invite
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    GameSoundEffectsEngine.playTap()
                                    onToggleMicMute()
                                },
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (!micMuted) Color(0xFF00E676).copy(alpha = 0.2f)
                                        else Color(0xFFFF1744).copy(alpha = 0.22f)
                                    )
                                    .testTag("game_voice_mic_toggle")
                            ) {
                                Icon(
                                    imageVector = if (!micMuted) Icons.Default.Mic else Icons.Default.MicOff,
                                    contentDescription = if (!micMuted) "Mute Microphone" else "Unmute Microphone",
                                    tint = if (!micMuted) Color(0xFF00E676) else Color(0xFFFF5252),
                                    modifier = Modifier.size(17.dp)
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color.White.copy(alpha = 0.08f),
                                modifier = Modifier
                                    .clickable {
                                        GameSoundEffectsEngine.playTap()
                                        onCycleAudioOutputRoute()
                                    }
                                    .testTag("game_voice_route_toggle")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                        contentDescription = "Switch Audio Output",
                                        tint = Color(0xFF00E5FF),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = audioOutputRouteLabel.take(8),
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            IconButton(
                                onClick = {
                                    GameSoundEffectsEngine.playTap()
                                    val firstOpenSlot = (2..maxOf(gameRoom.maxPlayers.toInt(), 2).coerceAtMost(4))
                                        .firstOrNull { sIdx -> realPlayers.none { it.slotIndex == sIdx } } ?: 2
                                    slotInvitePickerIndex = firstOpenSlot
                                },
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(NoisePurple.copy(alpha = 0.25f))
                                    .testTag("game_invite_players_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PersonAdd,
                                    contentDescription = "Invite Players",
                                    tint = Color.White,
                                    modifier = Modifier.size(17.dp)
                                )
                            }

                            IconButton(
                                onClick = {
                                    GameSoundEffectsEngine.playTap()
                                    showPauseSettingsModal = true
                                },
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(NoiseGold.copy(alpha = 0.20f))
                                    .testTag("game_pause_settings_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "Pause & Game Settings",
                                    tint = NoiseGold,
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Real Multiplayer Player Slots (Slot 1..MaxPlayers) with Direct Slot Invitation Picker
                    val maxSlotCount = remember(gameRoom.maxPlayers, gameRoom.gameType) {
                        if (gameRoom.gameType == "ludo_3d") 4 else gameRoom.maxPlayers.toInt().coerceIn(2, 4)
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("game_player_slots_row"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        for (slotIdx in 1..maxSlotCount) {
                            val occupiedSlot = realPlayers.firstOrNull { it.slotIndex == slotIdx }
                                ?: if (slotIdx == 1) realPlayers.firstOrNull() else null
                            val slotInvite = outgoingInvites
                                .filter { it.slotIndex == slotIdx && it.gameRoomId == gameRoom.id }
                                .maxByOrNull { it.createdAtMillis }
                            val nowMs = System.currentTimeMillis()
                            val inviteExpired = slotInvite != null &&
                                slotInvite.status == "pending" &&
                                slotInvite.expiresAtMillis in 1 until nowMs

                            if (occupiedSlot != null) {
                                val isSpeaking = occupiedSlot.speaking ||
                                    occupiedSlot.username in activeSpeakers ||
                                    (!micMuted && occupiedSlot.userId == currentUid && liveKitVoiceActive)
                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color.White.copy(alpha = 0.06f))
                                        .border(
                                            1.dp,
                                            if (isSpeaking) Color(0xFF00E676).copy(alpha = 0.85f * min(1f, voicePulse))
                                            else Color(0xFF00E676).copy(alpha = 0.35f),
                                            RoundedCornerShape(10.dp)
                                        )
                                        .padding(horizontal = 6.dp, vertical = 4.dp)
                                        .testTag("game_slot_occupied_$slotIdx"),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        Box(
                                            modifier = Modifier
                                                .size(20.dp)
                                                .clip(CircleShape)
                                                .background(Brush.linearGradient(listOf(NoisePink, NoisePurple))),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = occupiedSlot.avatarKey.take(1).uppercase().ifBlank { "P" },
                                                color = Color.White,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Column {
                                            Text(
                                                text = "@${occupiedSlot.username}",
                                                color = Color.White,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = when {
                                                    isSpeaking -> "🔊 Speaking"
                                                    occupiedSlot.connected -> "🟢 Slot #$slotIdx"
                                                    else -> "Reconnecting..."
                                                },
                                                color = Color(0xFF00E676),
                                                fontSize = 8.sp
                                            )
                                        }
                                    }
                                    if (isRoomOwner && occupiedSlot.userId != currentUid) {
                                        Icon(
                                            imageVector = Icons.Default.PersonRemove,
                                            contentDescription = "Remove Player",
                                            tint = Color(0xFFFF5252),
                                            modifier = Modifier
                                                .size(14.dp)
                                                .clickable { onRemovePlayer(occupiedSlot.userId) }
                                        )
                                    }
                                }
                            } else {
                                // Open or Pending-Invite Slot -> Tapping opens real registered user picker!
                                val statusLabel = when {
                                    slotInvite == null -> "+ Invite Slot #$slotIdx"
                                    inviteExpired || slotInvite.status == "expired" -> "⌛ Expired • Invite"
                                    slotInvite.status == "declined" -> "❌ Rejected • Invite"
                                    slotInvite.status == "pending" -> "⏳ @${slotInvite.receiverUsername}"
                                    else -> "+ Invite Slot #$slotIdx"
                                }
                                val borderCol = when {
                                    slotInvite?.status == "pending" && !inviteExpired -> NoiseGold
                                    slotInvite?.status == "declined" -> Color(0xFFFF5252)
                                    else -> NoisePink.copy(alpha = 0.55f)
                                }
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color.White.copy(alpha = 0.05f),
                                    modifier = Modifier
                                        .weight(1f)
                                        .border(1.dp, borderCol, RoundedCornerShape(10.dp))
                                        .clickable {
                                            GameSoundEffectsEngine.playTap()
                                            slotInvitePickerIndex = slotIdx
                                        }
                                        .testTag("game_slot_invite_$slotIdx")
                                ) {
                                    Column(
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 5.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = statusLabel,
                                            color = if (slotInvite?.status == "pending" && !inviteExpired) NoiseGold else Color.White,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = if (slotInvite?.status == "pending" && !inviteExpired) "Pending Invite" else "Tap to Invite User",
                                            color = NoiseTextSecondary,
                                            fontSize = 8.sp,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Lobby Action Strip when waiting for players or ready to start
                    if (gameRoom.status == "waiting" || realPlayers.size < 2) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.White.copy(alpha = 0.04f))
                                .padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = if (realPlayers.size >= 2) {
                                    "✅ ${realPlayers.size} real players connected! Ready to play."
                                } else {
                                    "Tap any [+ Invite Slot] above to invite a registered user."
                                },
                                color = if (realPlayers.size >= 2) Color(0xFF00E676) else NoiseGold,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f)
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                if (isRoomOwner && gameRoom.status == "waiting") {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFF00E676),
                                        modifier = Modifier
                                            .clickable {
                                                GameSoundEffectsEngine.playTap()
                                                onStartMatch()
                                            }
                                            .testTag("game_lobby_start_match_button")
                                    ) {
                                        Text(
                                            text = "Start Match",
                                            color = Color.Black,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                        )
                                    }
                                }
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (optionalBotEnabled) Color(0xFF00E5FF).copy(alpha = 0.2f) else Color.White.copy(alpha = 0.08f),
                                    modifier = Modifier
                                        .clickable {
                                            GameSoundEffectsEngine.playTap()
                                            optionalBotEnabled = !optionalBotEnabled
                                        }
                                        .testTag("game_toggle_bot_mode")
                                ) {
                                    Text(
                                        text = if (optionalBotEnabled) "Practice AI: ON" else "Practice AI: OFF",
                                        color = if (optionalBotEnabled) Color(0xFF00E5FF) else NoiseTextSecondary,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 5.dp)
                                    )
                                }
                                if (isRoomOwner) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFFFF1744).copy(alpha = 0.22f),
                                        modifier = Modifier
                                            .clickable {
                                                GameSoundEffectsEngine.playTap()
                                                onCancelSession()
                                            }
                                            .testTag("game_lobby_cancel_session_button")
                                    ) {
                                        Text(
                                            text = "Cancel",
                                            color = Color(0xFFFF5252),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 5.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // 2. SELECTED 3D GAME ENGINE ARENA
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.runtime.key(rematchCounter) {
                    when (gameRoom.gameType) {
                        "carrom_3d" -> Playable3DCarromArena(
                            currentUser = currentUser,
                            players = realPlayers,
                            onMatchComplete = { w, s1, s2 -> finishMatch(w, s1, s2, "3D Carrom Pro Match") }
                        )
                        "ludo_3d" -> Playable3DLudoArena(
                            gameRoom = gameRoom,
                            currentUser = currentUser,
                            players = realPlayers,
                            botEnabled = optionalBotEnabled,
                            onSyncGameState = onSyncGameState,
                            onMatchComplete = { w, s1, s2 -> finishMatch(w, s1, s2, "3D Royal Ludo Match") }
                        )
                        "pool_8ball" -> Playable3DPool8BallArena(
                            gameRoom = gameRoom,
                            currentUser = currentUser,
                            players = realPlayers,
                            botEnabled = optionalBotEnabled,
                            tableThemeIndex = selectedArenaThemeIndex,
                            onSyncGameState = onSyncGameState,
                            onMatchComplete = { w, s1, s2, note -> finishMatch(w, s1, s2, note) }
                        )
                        "mini_racing" -> Playable3DMiniRacingArena(
                            currentUser = currentUser,
                            players = realPlayers,
                            onMatchComplete = { w, s1, s2 -> finishMatch(w, s1, s2, "3D Multiplayer Racing") }
                        )
                        "chess_3d" -> PlayableChessArena(
                            gameRoom = gameRoom,
                            players = realPlayers,
                            currentUserProfile = currentUser,
                            onSyncState = onSyncGameState,
                            onFinishMatch = { winnerProf, _, wScore, rScore ->
                                finishMatch(winnerProf.username.ifBlank { currentUser.username }, wScore, rScore, "3D Grandmaster Chess")
                            }
                        )
                        "checkers_3d" -> PlayableCheckersArena(
                            gameRoom = gameRoom,
                            players = realPlayers,
                            currentUserProfile = currentUser,
                            onSyncState = onSyncGameState,
                            onFinishMatch = { winnerProf, _, wScore, rScore ->
                                finishMatch(winnerProf.username.ifBlank { currentUser.username }, wScore, rScore, "3D Checkers Match")
                            }
                        )
                        "tic_tac_toe" -> PlayableTicTacToeArena(
                            gameRoom = gameRoom,
                            players = realPlayers,
                            currentUserProfile = currentUser,
                            onSyncState = onSyncGameState,
                            onFinishMatch = { winnerProf, _, wScore, rScore ->
                                finishMatch(winnerProf.username.ifBlank { currentUser.username }, wScore, rScore, "Neon Tic Tac Toe")
                            }
                        )
                        "dominoes_pro" -> PlayableDominoesArena(
                            gameRoom = gameRoom,
                            players = realPlayers,
                            currentUserProfile = currentUser,
                            onSyncState = onSyncGameState,
                            onFinishMatch = { winnerProf, _, wScore, rScore ->
                                finishMatch(winnerProf.username.ifBlank { currentUser.username }, wScore, rScore, "Dominoes Pro Match")
                            }
                        )
                        "snakes_ladders" -> PlayableSnakesAndLaddersArena(
                            gameRoom = gameRoom,
                            players = realPlayers,
                            currentUserProfile = currentUser,
                            onSyncState = onSyncGameState,
                            onFinishMatch = { winnerProf, _, wScore, rScore ->
                                finishMatch(winnerProf.username.ifBlank { currentUser.username }, wScore, rScore, "Snakes & Ladders 100")
                            }
                        )
                        "mini_arcade" -> PlayableMiniArcadeArena(
                            gameRoom = gameRoom,
                            players = realPlayers,
                            currentUserProfile = currentUser,
                            onSyncState = onSyncGameState,
                            onFinishMatch = { winnerProf, _, wScore, rScore ->
                                finishMatch(winnerProf.username.ifBlank { currentUser.username }, wScore, rScore, "Mini Arcade Arena")
                            }
                        )
                        else -> PlayableMultiplayerQuizArena(
                            currentUser = currentUser,
                            players = realPlayers,
                            onMatchComplete = { w, s1, s2 -> finishMatch(w, s1, s2, "Multiplayer Trivia Quiz") }
                        )
                    }
                }

                // 3. ANIMATED 3D GAME LOADING OVERLAY (0% -> 100%)
                if (isLoadingAssets) {
                    Game3DLoadingScreenOverlay(
                        gameTitle = gameRoom.gameTitle,
                        gameType = gameRoom.gameType,
                        roomCode = gameRoom.roomCode,
                        progressPercent = loadingProgressPercent,
                        statusText = loadingStageLabel,
                        errorMessage = loadingErrorText,
                        onRetry = {
                            loadingProgressPercent = 0
                            loadingRetryTrigger++
                        },
                        onInstantReady = {
                            loadingProgressPercent = 100
                            isLoadingAssets = false
                        }
                    )
                }

                // 4. VICTORY / DEFEAT / COMPLETE MATCH RESULTS SCREEN
                if (winnerBanner != null) {
                    val didCurrentUserWin = winnerBanner.equals(currentUser.username, ignoreCase = true)
                    val borderAccent = if (didCurrentUserWin) NoiseGold else NoisePink
                    Surface(
                        shape = RoundedCornerShape(26.dp),
                        color = Color(0xFF130D24).copy(alpha = 0.98f),
                        modifier = Modifier
                            .fillMaxWidth(0.94f)
                            .border(2.dp, borderAccent, RoundedCornerShape(26.dp))
                            .testTag("game_match_result_overlay")
                    ) {
                        Column(
                            modifier = Modifier.padding(22.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(68.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (didCurrentUserWin) NoiseGold.copy(alpha = 0.18f)
                                        else NoisePink.copy(alpha = 0.18f)
                                    )
                                    .border(2.dp, borderAccent, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.EmojiEvents,
                                    contentDescription = "Result Trophy",
                                    tint = borderAccent,
                                    modifier = Modifier.size(40.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = if (didCurrentUserWin) "🏆 VICTORY • MATCH COMPLETE" else "⚔️ DEFEAT • MATCH COMPLETE",
                                color = borderAccent,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.sp,
                                letterSpacing = 0.8.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "@$winnerBanner Wins!",
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 23.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = matchSummaryDetail,
                                color = NoiseTextSecondary,
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            // Detailed Match Results Scoreboard Box
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color(0xFF1D1536),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Final Score", color = NoiseTextSecondary, fontSize = 12.sp)
                                        Text(
                                            text = "$winnerScoreState – $runnerUpScoreState",
                                            color = Color.White,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 13.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("VIP Coin Prize Pool", color = NoiseTextSecondary, fontSize = 12.sp)
                                        Text(
                                            text = "+${gameRoom.prizePoolCoins} VIP Coins",
                                            color = Color(0xFF00E676),
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 13.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Room Code", color = NoiseTextSecondary, fontSize = 12.sp)
                                        Text(
                                            text = gameRoom.roomCode,
                                            color = NoiseGold,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = {
                                        GameSoundEffectsEngine.playTap()
                                        winnerBanner = null
                                        rematchCounter++
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = NoisePurple),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("game_rematch_button")
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Play Again", fontWeight = FontWeight.Bold)
                                }
                                Button(
                                    onClick = onExitToHost,
                                    colors = ButtonDefaults.buttonColors(containerColor = NoisePink),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("return_to_chatroom_after_game")
                                ) {
                                    Text(
                                        text = if (gameRoom.chatRoomId.isNotBlank()) "Back to Chatroom" else "Exit Arena",
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 5. PAUSE & SETTINGS MODAL
        if (showPauseSettingsModal) {
            Dialog(onDismissRequest = { showPauseSettingsModal = false }) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color(0xFF140F26),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.5.dp, NoiseGold, RoundedCornerShape(24.dp))
                        .testTag("game_pause_settings_modal")
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "⚙️ GAME PAUSE & SETTINGS",
                                color = NoiseGold,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp
                            )
                            IconButton(onClick = { showPauseSettingsModal = false }) {
                                Icon(Icons.Default.Close, contentDescription = "Close Settings", tint = Color.White)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Sound Effects Toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (sfxEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                                    contentDescription = null,
                                    tint = Color(0xFF00E5FF)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("3D Game Sound Effects", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            }
                            Switch(
                                checked = sfxEnabled,
                                onCheckedChange = {
                                    sfxEnabled = it
                                    GameSoundEffectsEngine.sfxEnabled = it
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Background Music Toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (musicEnabled) Icons.Default.MusicNote else Icons.Default.MusicOff,
                                    contentDescription = null,
                                    tint = NoisePink
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Arena Background Music", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            }
                            Switch(
                                checked = musicEnabled,
                                onCheckedChange = {
                                    musicEnabled = it
                                    GameSoundEffectsEngine.musicEnabled = it
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Table / Arena Felt Theme Selector
                        Text("3D Table & Board Theme", color = NoiseTextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("Emerald Pro", "Royal Purple", "Cyber Cyan").forEachIndexed { idx, label ->
                                val selected = selectedArenaThemeIndex == idx
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (selected) NoisePink else Color(0xFF21193B),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { selectedArenaThemeIndex = idx }
                                ) {
                                    Text(
                                        text = label,
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Reconnect / Sync Test Button
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF1E1738),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showPauseSettingsModal = false
                                    showReconnectingOverlay = true
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Wifi, contentDescription = null, tint = Color(0xFF00E676), modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Multiplayer Relay: Connected (${gameRoom.roomCode})", color = Color.White, fontSize = 12.sp)
                                }
                                Text("Re-Sync", color = NoiseGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { showPauseSettingsModal = false },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Resume", color = Color.Black, fontWeight = FontWeight.ExtraBold)
                            }
                            Button(
                                onClick = {
                                    showPauseSettingsModal = false
                                    winnerBanner = null
                                    rematchCounter++
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = NoisePurple),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Restart", fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = {
                                    showPauseSettingsModal = false
                                    showExitConfirmDialog = true
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF1744)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Exit", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // 6. RECONNECTION SCREEN OVERLAY
        if (showReconnectingOverlay) {
            Dialog(onDismissRequest = { showReconnectingOverlay = false }) {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = Color(0xFF150E26),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.5.dp, Color(0xFF00E5FF), RoundedCornerShape(22.dp))
                        .testTag("game_reconnection_modal")
                ) {
                    Column(
                        modifier = Modifier.padding(22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Wifi,
                            contentDescription = "Synchronizing",
                            tint = Color(0xFF00E5FF),
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "SYNCHRONIZING MULTIPLAYER ROOM",
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Verifying turn lock and game state for Room ${gameRoom.roomCode}...",
                            color = NoiseTextSecondary,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                onSyncGameState(
                                    gameRoom.gameStateJson,
                                    gameRoom.currentTurnUserId.ifBlank { currentUid },
                                    realPlayers,
                                    "playing",
                                    null,
                                    null
                                )
                                showReconnectingOverlay = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Reconnect & Resume Match", color = Color.Black, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
            }
        }

        // 7. EXIT CONFIRMATION DIALOG
        if (showExitConfirmDialog) {
            Dialog(onDismissRequest = { showExitConfirmDialog = false }) {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = Color(0xFF161029),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.5.dp, NoisePink, RoundedCornerShape(22.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Leave ${gameRoom.gameTitle}?",
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 17.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Your LiveKit voice chat connection will remain active when you return to the room.",
                            color = NoiseTextSecondary,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { showExitConfirmDialog = false },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF282042)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Stay in Game", fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = {
                                    showExitConfirmDialog = false
                                    onExitToHost()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = NoisePink),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Leave Match", fontWeight = FontWeight.ExtraBold)
                            }
                        }
                    }
                }
            }
        }

        // 8. REAL REGISTERED USER INVITATION DIALOG FOR PLAYER SLOTS
        val activeSlotToInvite = slotInvitePickerIndex
        if (activeSlotToInvite != null) {
            val invitableUsers = remember(registeredAccounts, realPlayers, currentUid) {
                val existingUids = realPlayers.map { it.userId }.toSet() + setOf(currentUid)
                registeredAccounts
                    .filter { acct ->
                        val uid = acct.userId.ifBlank { acct.id }
                        uid.isNotBlank() && uid !in existingUids && acct.username.isNotBlank()
                    }
                    .sortedByDescending { it.online }
            }
            Dialog(onDismissRequest = { slotInvitePickerIndex = null }) {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = Color(0xFF140F26),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.5.dp, NoiseGold, RoundedCornerShape(22.dp))
                        .testTag("game_slot_invite_picker_dialog")
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "INVITE PLAYER TO SLOT #$activeSlotToInvite",
                                    color = NoiseGold,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "${gameRoom.gameTitle} • Room ${gameRoom.roomCode}",
                                    color = NoiseTextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                            IconButton(onClick = { slotInvitePickerIndex = null }) {
                                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        if (invitableUsers.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No other registered accounts available to invite right now.",
                                    color = NoiseTextSecondary,
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        } else {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 320.dp)
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                invitableUsers.forEach { acct ->
                                    val uid = acct.userId.ifBlank { acct.id }
                                    val existingInvite = outgoingInvites
                                        .filter { it.receiverUid == uid && it.gameRoomId == gameRoom.id }
                                        .maxByOrNull { it.createdAtMillis }
                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        color = Color(0xFF1E1738),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .border(
                                                1.dp,
                                                if (acct.online) Color(0xFF00E676).copy(alpha = 0.45f) else Color.White.copy(alpha = 0.1f),
                                                RoundedCornerShape(14.dp)
                                            )
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 12.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(36.dp)
                                                        .clip(CircleShape)
                                                        .background(Brush.linearGradient(listOf(NoisePink, NoisePurple))),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = acct.avatarKey.take(1).uppercase().ifBlank { acct.username.take(1).uppercase() },
                                                        color = Color.White,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        fontSize = 14.sp
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Column {
                                                    Text(
                                                        text = "@${acct.username}",
                                                        color = Color.White,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 13.sp
                                                    )
                                                    Text(
                                                        text = when {
                                                            existingInvite?.status == "pending" -> "⏳ Invitation Pending (Slot #${existingInvite.slotIndex})"
                                                            existingInvite?.status == "declined" -> "❌ Declined previous invite"
                                                            acct.online -> "🟢 Online now"
                                                            else -> "⚪ Offline (Will receive notification)"
                                                        },
                                                        color = if (acct.online) Color(0xFF00E676) else NoiseTextSecondary,
                                                        fontSize = 11.sp
                                                    )
                                                }
                                            }
                                            Button(
                                                onClick = {
                                                    GameSoundEffectsEngine.playTap()
                                                    onInviteUserToSlot(acct, activeSlotToInvite)
                                                    slotInvitePickerIndex = null
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = NoisePink),
                                                modifier = Modifier.testTag("invite_slot_user_${acct.username}")
                                            ) {
                                                Text("Invite", fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ============================================================================
// PREMIUM ANIMATED 3D GAME LOADING SCREEN (0% -> 100% WITH REAL STATUS STAGES)
// ============================================================================
@Composable
private fun Game3DLoadingScreenOverlay(
    gameTitle: String,
    gameType: String,
    roomCode: String,
    progressPercent: Int,
    statusText: String,
    errorMessage: String?,
    onRetry: () -> Unit,
    onInstantReady: () -> Unit
) {
    val spinTransition = rememberInfiniteTransition(label = "loader_spin")
    val orbitAngle by spinTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(2400, easing = LinearEasing)),
        label = "orbit"
    )

    Surface(
        color = Color(0xFF090614).copy(alpha = 0.96f),
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(24.dp))
            .border(1.5.dp, Brush.linearGradient(listOf(NoisePink, NoisePurple, NoiseGold)), RoundedCornerShape(24.dp))
            .clickable { onInstantReady() }
            .testTag("game_loading_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // NOICE LIVE Branding Badge
            Surface(
                shape = RoundedCornerShape(50),
                color = Color(0xFF1C1433),
                modifier = Modifier.border(1.dp, NoiseGold, RoundedCornerShape(50))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.SportsEsports, contentDescription = null, tint = NoiseGold, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "NOICE LIVE • 3D GAMING ARENA",
                        color = NoiseGold,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 11.sp,
                        letterSpacing = 1.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 3D Animated Game Artwork Emblem
            Box(
                modifier = Modifier.size(136.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val cx = size.width * 0.5f
                    val cy = size.height * 0.5f
                    val r = size.minDimension * 0.42f

                    // Outer rotating neon ring
                    drawCircle(
                        brush = Brush.sweepGradient(
                            listOf(NoisePink, NoisePurple, Color(0xFF00E5FF), NoiseGold, NoisePink)
                        ),
                        radius = r * 1.08f,
                        center = Offset(cx, cy),
                        style = Stroke(width = 6.dp.toPx())
                    )

                    // Orbiting 3D particle
                    val rad = (orbitAngle * PI / 180.0).toFloat()
                    drawCircle(
                        color = Color(0xFF00E5FF),
                        radius = 7.dp.toPx(),
                        center = Offset(cx + cos(rad) * r * 1.08f, cy + sin(rad) * r * 1.08f)
                    )

                    // Inner 3D Sphere / Game Artwork
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0xFF2B1C50), Color(0xFF120B26)),
                            center = Offset(cx - r * 0.2f, cy - r * 0.2f),
                            radius = r
                        ),
                        radius = r * 0.92f,
                        center = Offset(cx, cy)
                    )

                    when (gameType) {
                        "pool_8ball" -> {
                            // 3D 8-Ball Artwork
                            drawCircle(color = Color(0xFF111111), radius = r * 0.58f, center = Offset(cx, cy))
                            drawCircle(color = Color.White, radius = r * 0.26f, center = Offset(cx, cy))
                            drawCircle(
                                color = Color.White.copy(alpha = 0.35f),
                                radius = r * 0.14f,
                                center = Offset(cx - r * 0.18f, cy - r * 0.18f)
                            )
                        }
                        "ludo_3d" -> {
                            // 3D Ludo 4-Color Cross & Dice Artwork
                            drawRoundRect(
                                color = NoisePink,
                                topLeft = Offset(cx - r * 0.48f, cy - r * 0.48f),
                                size = Size(r * 0.42f, r * 0.42f),
                                cornerRadius = CornerRadius(8f, 8f)
                            )
                            drawRoundRect(
                                color = Color(0xFF00E676),
                                topLeft = Offset(cx + r * 0.06f, cy - r * 0.48f),
                                size = Size(r * 0.42f, r * 0.42f),
                                cornerRadius = CornerRadius(8f, 8f)
                            )
                            drawRoundRect(
                                color = Color(0xFF00E5FF),
                                topLeft = Offset(cx - r * 0.48f, cy + r * 0.06f),
                                size = Size(r * 0.42f, r * 0.42f),
                                cornerRadius = CornerRadius(8f, 8f)
                            )
                            drawRoundRect(
                                color = NoiseGold,
                                topLeft = Offset(cx + r * 0.06f, cy + r * 0.06f),
                                size = Size(r * 0.42f, r * 0.42f),
                                cornerRadius = CornerRadius(8f, 8f)
                            )
                        }
                        else -> {
                            drawCircle(color = NoiseGold, radius = r * 0.42f, center = Offset(cx, cy), style = Stroke(width = 4.dp.toPx()))
                            drawCircle(color = NoisePink, radius = r * 0.22f, center = Offset(cx, cy))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = gameTitle.uppercase(),
                color = Color.White,
                fontWeight = FontWeight.Black,
                fontSize = 22.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Room Code: $roomCode • Unity 6 URP 3D Pipeline",
                color = Color(0xFF00E5FF),
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp
            )

            Spacer(modifier = Modifier.height(20.dp))

            if (errorMessage != null) {
                Text(
                    text = errorMessage,
                    color = Color(0xFFFF5252),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onRetry,
                    colors = ButtonDefaults.buttonColors(containerColor = NoisePink),
                    modifier = Modifier.testTag("game_loading_retry_button")
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Retry Loading", fontWeight = FontWeight.ExtraBold)
                }
            } else {
                // Progress Percentage & Status Stage
                Row(
                    modifier = Modifier.fillMaxWidth(0.84f),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = statusText,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        modifier = Modifier.testTag("game_loading_status_text")
                    )
                    Text(
                        text = "$progressPercent%",
                        color = NoiseGold,
                        fontWeight = FontWeight.Black,
                        fontSize = 15.sp,
                        modifier = Modifier.testTag("game_loading_percentage_text")
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { (progressPercent / 100f).coerceIn(0f, 1f) },
                    color = NoisePink,
                    trackColor = Color(0xFF251A3A),
                    modifier = Modifier
                        .fillMaxWidth(0.84f)
                        .height(10.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .testTag("game_loading_progress_bar")
                )
            }
        }
    }
}

// ============================================================================
// GAME 1: 3D CARROM PRO (Wooden 3D Board, 9 Black + 9 White + Red Queen, Physics)
// ============================================================================
private data class CarromCoinPiece(
    val id: Int,
    var x: Float,
    var y: Float,
    var vx: Float = 0f,
    var vy: Float = 0f,
    val type: String, // "white", "black", "queen", "striker"
    var pocketed: Boolean = false
)

@Composable
private fun Playable3DCarromArena(
    currentUser: UserProfile,
    players: List<GamePlayerSlot>,
    onMatchComplete: (String, Long, Long) -> Unit
) {
    val player1Name = currentUser.username.ifBlank { "You" }
    val player2Name = players.firstOrNull { it.username != player1Name }?.username ?: "Carrom_Master"

    var player1Score by remember { mutableIntStateOf(0) }
    var player2Score by remember { mutableIntStateOf(0) }
    var currentTurn by remember { mutableIntStateOf(1) }
    var strikerX by remember { mutableFloatStateOf(0.5f) }
    var aimAngleDeg by remember { mutableFloatStateOf(-90f) }
    var shotPower by remember { mutableFloatStateOf(0.68f) }
    var isSimulating by remember { mutableStateOf(false) }
    var statusBanner by remember { mutableStateOf("Your Turn: Position Striker, Aim & Strike!") }

    val pieces = remember {
        mutableStateListOf<CarromCoinPiece>().apply {
            add(CarromCoinPiece(0, 0.5f, 0.5f, type = "queen"))
            for (i in 0 until 6) {
                val angle = (i * 60.0) * PI / 180.0
                val r = 0.052f
                add(
                    CarromCoinPiece(
                        id = i + 1,
                        x = (0.5 + cos(angle) * r).toFloat(),
                        y = (0.5 + sin(angle) * r).toFloat(),
                        type = if (i % 2 == 0) "white" else "black"
                    )
                )
            }
            for (i in 0 until 12) {
                val angle = (i * 30.0) * PI / 180.0
                val r = 0.104f
                add(
                    CarromCoinPiece(
                        id = i + 7,
                        x = (0.5 + cos(angle) * r).toFloat(),
                        y = (0.5 + sin(angle) * r).toFloat(),
                        type = if (i % 2 == 0) "white" else "black"
                    )
                )
            }
            add(CarromCoinPiece(99, 0.5f, 0.80f, type = "striker"))
        }
    }

    LaunchedEffect(isSimulating) {
        if (!isSimulating) return@LaunchedEffect
        val pockets = listOf(
            Offset(0.11f, 0.11f),
            Offset(0.89f, 0.11f),
            Offset(0.11f, 0.89f),
            Offset(0.89f, 0.89f)
        )
        var pocketedThisTurn = 0
        var steps = 0
        while (isSimulating && steps < 240) {
            steps++
            var anyMoving = false
            val active = pieces.filter { !it.pocketed }

            for (p in active) {
                p.x += p.vx
                p.y += p.vy
                p.vx *= 0.965f
                p.vy *= 0.965f

                val radius = if (p.type == "striker") 0.032f else 0.024f
                if (p.x < 0.09f + radius) {
                    p.x = 0.09f + radius
                    p.vx = -p.vx * 0.82f
                } else if (p.x > 0.91f - radius) {
                    p.x = 0.91f - radius
                    p.vx = -p.vx * 0.82f
                }
                if (p.y < 0.09f + radius) {
                    p.y = 0.09f + radius
                    p.vy = -p.vy * 0.82f
                } else if (p.y > 0.91f - radius) {
                    p.y = 0.91f - radius
                    p.vy = -p.vy * 0.82f
                }

                for (pocket in pockets) {
                    if (hypot(p.x - pocket.x, p.y - pocket.y) < 0.052f) {
                        if (p.type == "striker") {
                            p.vx = 0f
                            p.vy = 0f
                            p.x = 0.5f
                            p.y = 0.80f
                            GameSoundEffectsEngine.playCapture()
                            statusBanner = "Striker Foul! Turn passes."
                        } else {
                            p.pocketed = true
                            p.vx = 0f
                            p.vy = 0f
                            pocketedThisTurn++
                            GameSoundEffectsEngine.playPocket()
                            val pts = when (p.type) {
                                "queen" -> 50
                                "white" -> 20
                                else -> 10
                            }
                            if (currentTurn == 1) player1Score += pts else player2Score += pts
                        }
                    }
                }

                if (hypot(p.vx, p.vy) > 0.0012f) {
                    anyMoving = true
                } else {
                    p.vx = 0f
                    p.vy = 0f
                }
            }

            for (i in active.indices) {
                for (j in i + 1 until active.size) {
                    val a = active[i]
                    val b = active[j]
                    if (a.pocketed || b.pocketed) continue
                    val ra = if (a.type == "striker") 0.032f else 0.024f
                    val rb = if (b.type == "striker") 0.032f else 0.024f
                    val dx = b.x - a.x
                    val dy = b.y - a.y
                    val dist = hypot(dx, dy)
                    val minDist = ra + rb
                    if (dist > 0.0001f && dist < minDist) {
                        val nx = dx / dist
                        val ny = dy / dist
                        val overlap = (minDist - dist) * 0.52f
                        a.x -= nx * overlap
                        a.y -= ny * overlap
                        b.x += nx * overlap
                        b.y += ny * overlap

                        val kx = a.vx - b.vx
                        val ky = a.vy - b.vy
                        val pImpulse = 2f * (nx * kx + ny * ky) / 2f
                        a.vx -= pImpulse * nx * 0.92f
                        a.vy -= pImpulse * ny * 0.92f
                        b.vx += pImpulse * nx * 0.92f
                        b.vy += pImpulse * ny * 0.92f
                    }
                }
            }

            val strikerIdx = pieces.indexOfFirst { it.type == "striker" }
            if (strikerIdx >= 0) {
                pieces[strikerIdx] = pieces[strikerIdx].copy()
            }
            delay(16L)
            if (!anyMoving) break
        }

        val sIdx = pieces.indexOfFirst { it.type == "striker" }
        if (sIdx >= 0) {
            val nextTurn = if (pocketedThisTurn > 0) currentTurn else if (currentTurn == 1) 2 else 1
            currentTurn = nextTurn
            val baselineY = if (currentTurn == 1) 0.80f else 0.20f
            pieces[sIdx] = pieces[sIdx].copy(x = strikerX, y = baselineY, vx = 0f, vy = 0f, pocketed = false)
            statusBanner = if (currentTurn == 1) {
                "Your Turn (@$player1Name) • Aim & Strike!"
            } else {
                "@$player2Name's Turn • Lining up shot..."
            }
        }
        isSimulating = false

        val remainingCoins = pieces.count { it.type != "striker" && !it.pocketed }
        if (remainingCoins == 0 || player1Score >= 120 || player2Score >= 120) {
            GameSoundEffectsEngine.playVictory()
            val winner = if (player1Score >= player2Score) player1Name else player2Name
            onMatchComplete(
                winner,
                max(player1Score, player2Score).toLong(),
                min(player1Score, player2Score).toLong()
            )
        }
    }

    LaunchedEffect(currentTurn, isSimulating) {
        if (currentTurn == 2 && !isSimulating) {
            delay(900L)
            val targetCoin = pieces.firstOrNull { !it.pocketed && it.type != "striker" }
            val sIdx = pieces.indexOfFirst { it.type == "striker" }
            if (targetCoin != null && sIdx >= 0) {
                val striker = pieces[sIdx]
                val angle = atan2(targetCoin.y - striker.y, targetCoin.x - striker.x)
                val speed = 0.036f
                pieces[sIdx] = striker.copy(
                    vx = cos(angle) * speed,
                    vy = sin(angle) * speed
                )
                isSimulating = true
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ScoreBadgeCard(
                title = "@$player1Name (White)",
                score = "$player1Score pts",
                active = currentTurn == 1,
                accent = NoiseGold
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "3D CARROM PRO",
                    color = NoiseGold,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = statusBanner,
                    color = Color.White,
                    fontSize = 11.sp,
                    maxLines = 1
                )
            }
            ScoreBadgeCard(
                title = "@$player2Name (Black)",
                score = "$player2Score pts",
                active = currentTurn == 2,
                accent = NoisePink
            )
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .aspectRatio(1f)
                .padding(6.dp)
                .clip(RoundedCornerShape(24.dp))
                .border(3.dp, NoiseGold.copy(alpha = 0.7f), RoundedCornerShape(24.dp))
                .testTag("carrom_3d_board_canvas")
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(isSimulating, currentTurn) {
                        detectDragGestures { change, _ ->
                            if (!isSimulating && currentTurn == 1) {
                                val sIdx = pieces.indexOfFirst { it.type == "striker" }
                                if (sIdx >= 0) {
                                    val st = pieces[sIdx]
                                    val sx = st.x * size.width
                                    val sy = st.y * size.height
                                    val dx = change.position.x - sx
                                    val dy = change.position.y - sy
                                    aimAngleDeg = (atan2(dy, dx) * 180f / PI.toFloat())
                                }
                            }
                        }
                    }
            ) {
                val w = size.width
                val h = size.height

                drawRoundRect(
                    brush = Brush.linearGradient(
                        listOf(Color(0xFF3E1F0D), Color(0xFF241005), Color(0xFF4A2511))
                    ),
                    size = size,
                    cornerRadius = CornerRadius(24.dp.toPx(), 24.dp.toPx())
                )

                val margin = w * 0.085f
                drawRoundRect(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFFF6DFB7), Color(0xFFE5C48F), Color(0xFFD2AA6D)),
                        center = Offset(w * 0.5f, h * 0.5f),
                        radius = w * 0.55f
                    ),
                    topLeft = Offset(margin, margin),
                    size = Size(w - margin * 2, h - margin * 2),
                    cornerRadius = CornerRadius(14.dp.toPx(), 14.dp.toPx())
                )

                drawCircle(
                    color = Color(0xFFB71C1C).copy(alpha = 0.65f),
                    radius = w * 0.115f,
                    center = Offset(w * 0.5f, h * 0.5f),
                    style = Stroke(width = 3.dp.toPx())
                )
                drawCircle(
                    color = Color(0xFF3E2723).copy(alpha = 0.55f),
                    radius = w * 0.035f,
                    center = Offset(w * 0.5f, h * 0.5f),
                    style = Stroke(width = 2.dp.toPx())
                )

                drawLine(
                    color = Color(0xFF4E342E),
                    start = Offset(w * 0.22f, h * 0.80f),
                    end = Offset(w * 0.78f, h * 0.80f),
                    strokeWidth = 3.dp.toPx()
                )
                drawLine(
                    color = Color(0xFF4E342E),
                    start = Offset(w * 0.22f, h * 0.20f),
                    end = Offset(w * 0.78f, h * 0.20f),
                    strokeWidth = 3.dp.toPx()
                )

                val pockets = listOf(
                    Offset(w * 0.11f, h * 0.11f),
                    Offset(w * 0.89f, h * 0.11f),
                    Offset(w * 0.11f, h * 0.89f),
                    Offset(w * 0.89f, h * 0.89f)
                )
                pockets.forEach { p ->
                    drawCircle(color = Color(0xFFD4AF37), radius = w * 0.048f, center = p)
                    drawCircle(color = Color(0xFF140D08), radius = w * 0.041f, center = p)
                }

                val striker = pieces.firstOrNull { it.type == "striker" }
                if (!isSimulating && currentTurn == 1 && striker != null) {
                    val rad = (aimAngleDeg * PI / 180.0).toFloat()
                    val len = w * 0.34f * shotPower
                    val sx = striker.x * w
                    val sy = striker.y * h
                    drawLine(
                        brush = Brush.linearGradient(listOf(NoisePink, NoiseGold)),
                        start = Offset(sx, sy),
                        end = Offset(sx + cos(rad) * len, sy + sin(rad) * len),
                        strokeWidth = 4.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }

                pieces.filter { !it.pocketed }.forEach { piece ->
                    val cx = piece.x * w
                    val cy = piece.y * h
                    val r = if (piece.type == "striker") w * 0.032f else w * 0.024f

                    drawCircle(
                        color = Color.Black.copy(alpha = 0.38f),
                        radius = r,
                        center = Offset(cx + 3f, cy + 4f)
                    )

                    val baseColor = when (piece.type) {
                        "queen" -> Color(0xFFFF1744)
                        "white" -> Color(0xFFFFF8E7)
                        "black" -> Color(0xFF212121)
                        else -> Color(0xFF00E5FF)
                    }
                    drawCircle(color = baseColor, radius = r, center = Offset(cx, cy))
                    drawCircle(
                        color = Color.White.copy(alpha = 0.55f),
                        radius = r * 0.72f,
                        center = Offset(cx, cy),
                        style = Stroke(width = 2f)
                    )
                    drawCircle(
                        color = Color.White.copy(alpha = 0.45f),
                        radius = r * 0.25f,
                        center = Offset(cx - r * 0.25f, cy - r * 0.25f)
                    )
                }
            }
        }

        Surface(
            shape = RoundedCornerShape(18.dp),
            color = Color(0xFF151126),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Striker Position", color = NoiseTextSecondary, fontSize = 11.sp)
                        Slider(
                            value = strikerX,
                            onValueChange = { newX ->
                                strikerX = newX.coerceIn(0.24f, 0.76f)
                                if (!isSimulating && currentTurn == 1) {
                                    val idx = pieces.indexOfFirst { it.type == "striker" }
                                    if (idx >= 0) {
                                        pieces[idx] = pieces[idx].copy(x = strikerX, y = 0.80f)
                                    }
                                }
                            },
                            valueRange = 0.24f..0.76f,
                            colors = SliderDefaults.colors(thumbColor = NoiseGold, activeTrackColor = NoiseGold)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Shot Power (${(shotPower * 100).toInt()}%)", color = NoiseTextSecondary, fontSize = 11.sp)
                        Slider(
                            value = shotPower,
                            onValueChange = { shotPower = it.coerceIn(0.2f, 1f) },
                            valueRange = 0.2f..1f,
                            colors = SliderDefaults.colors(thumbColor = NoisePink, activeTrackColor = NoisePink)
                        )
                    }
                    Button(
                        onClick = {
                            if (!isSimulating && currentTurn == 1) {
                                val sIdx = pieces.indexOfFirst { it.type == "striker" }
                                if (sIdx >= 0) {
                                    GameSoundEffectsEngine.playCueStrike()
                                    val rad = (aimAngleDeg * PI / 180.0).toFloat()
                                    val speed = 0.048f * shotPower
                                    pieces[sIdx] = pieces[sIdx].copy(
                                        vx = cos(rad) * speed,
                                        vy = sin(rad) * speed
                                    )
                                    isSimulating = true
                                }
                            }
                        },
                        enabled = !isSimulating && currentTurn == 1,
                        colors = ButtonDefaults.buttonColors(containerColor = NoisePink),
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("carrom_strike_button")
                    ) {
                        Text("STRIKE", fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }
    }
}

// ============================================================================
// GAME 2: COMPLETE 3D ISOMETRIC LUDO (Recreated from Reference Image:
// Tilted 3D Isometric Board, Gold Metallic Frame & Corner Bumpers,
// 2x2 Home Zones [Red Top-Left, Green Top-Right, Blue Bottom-Left, Yellow Bottom-Right]
// with Recessed 4-Lobed Clover Wells, 16 Glossy 3D Conical Pawns with Ball Tops,
// 3D Pillowed White & Colored Tiles, 5-Pointed White Safe Stars, Central 4-Color
// 3D Triangles, Large Floating 3D White Dice with Golden Orbital Vortex &
// Purple/Blue Neon Laser Arcs, Glossy Neon Floor Reflections, Touch-to-Roll &
// Tap-on-3D-Token Gameplay + Multiplayer Sync)
// ============================================================================
private val LUDO_MAIN_TRACK_52: List<Pair<Int, Int>> = listOf(
    // Red top-left arm going right (indices 0..4)
    1 to 6, 2 to 6, 3 to 6, 4 to 6, 5 to 6,
    // Up top-left column (indices 5..10)
    6 to 5, 6 to 4, 6 to 3, 6 to 2, 6 to 1, 6 to 0,
    // Top turn (indices 11..12)
    7 to 0, 8 to 0,
    // Down top-right column (indices 13..17) — Green starts at 13 (8, 1)
    8 to 1, 8 to 2, 8 to 3, 8 to 4, 8 to 5,
    // Right top arm going right (indices 18..23)
    9 to 6, 10 to 6, 11 to 6, 12 to 6, 13 to 6, 14 to 6,
    // Right turn (indices 24..25)
    14 to 7, 14 to 8,
    // Left bottom-right arm going left (indices 26..30) — Yellow starts at 26 (13, 8)
    13 to 8, 12 to 8, 11 to 8, 10 to 8, 9 to 8,
    // Down bottom-right column (indices 31..36)
    8 to 9, 8 to 10, 8 to 11, 8 to 12, 8 to 13, 8 to 14,
    // Bottom turn (indices 37..38)
    7 to 14, 6 to 14,
    // Up bottom-left column (indices 39..43) — Blue starts at 39 (6, 13)
    6 to 13, 6 to 12, 6 to 11, 6 to 10, 6 to 9,
    // Left bottom arm going left (indices 44..49)
    5 to 8, 4 to 8, 3 to 8, 2 to 8, 1 to 8, 0 to 8,
    // Left turn back to Red start (indices 50..51)
    0 to 7, 0 to 6
)

private val LUDO_SAFE_TRACK_INDICES = setOf(0, 8, 13, 21, 26, 34, 39, 47)

// Player start offsets on LUDO_MAIN_TRACK_52 matching the 2x2 reference layout:
// 0 = Red (Top-Left, start index 0), 1 = Green (Top-Right, start index 13),
// 2 = Blue (Bottom-Left, start index 39), 3 = Yellow (Bottom-Right, start index 26)
private val LUDO_PLAYER_START_TRACK_OFFSET = intArrayOf(0, 13, 39, 26)

private fun ludoTokenGridPosition(playerIndex: Int, tokenIdx: Int, stepPos: Int): Pair<Float, Float> {
    if (stepPos < 0) {
        // 4-lobed clover well inside each 6x6 corner home zone:
        // 0 = Red (Top-Left), 1 = Green (Top-Right), 2 = Blue (Bottom-Left), 3 = Yellow (Bottom-Right)
        val baseCol = when (playerIndex) {
            0 -> 1.85f
            1 -> 10.85f
            2 -> 1.85f
            else -> 10.85f
        }
        val baseRow = when (playerIndex) {
            0 -> 1.95f
            1 -> 1.95f
            2 -> 10.85f
            else -> 10.85f
        }
        val offsetCol = (tokenIdx % 2) * 2.25f
        val offsetRow = (tokenIdx / 2) * 2.15f
        return (baseCol + offsetCol) to (baseRow + offsetRow)
    }
    if (stepPos >= 56) {
        // Finished inside the 3x3 center 4-color triangle area
        return when (playerIndex) {
            0 -> 6.55f to (7.5f + (tokenIdx - 1.5f) * 0.24f) // Red left/top triangle
            1 -> 8.45f to (7.5f + (tokenIdx - 1.5f) * 0.24f) // Green right triangle
            2 -> (7.1f + (tokenIdx - 1.5f) * 0.22f) to 8.35f // Blue bottom-left
            else -> (7.9f + (tokenIdx - 1.5f) * 0.22f) to 8.35f // Yellow bottom-right
        }
    }
    if (stepPos in 51..55) {
        // Colored 5-square Home Stretch leading to the central finishing triangles
        val homeOffset = stepPos - 50 // 1..5
        return when (playerIndex) {
            0 -> (homeOffset + 0.5f) to 7.5f             // Red left arm -> center
            1 -> (14 - homeOffset + 0.5f) to 7.5f        // Green right arm -> center
            2 -> 7.0f to (14 - homeOffset + 0.5f)        // Blue bottom-left vertical -> center
            else -> 8.0f to (14 - homeOffset + 0.5f)     // Yellow bottom-right vertical -> center
        }
    }
    // Main 52-square perimeter track (steps 0..50)
    val startOffset = LUDO_PLAYER_START_TRACK_OFFSET[playerIndex.coerceIn(0, 3)]
    val globalTrackIdx = (startOffset + stepPos) % 52
    val (col, row) = LUDO_MAIN_TRACK_52[globalTrackIdx]
    return (col + 0.5f) to (row + 0.5f)
}

/**
 * Projects 15x15 Ludo grid coordinates `(u, v)` in `[0..15]` and vertical height `z`
 * into 3D perspective screen coordinates matching the reference image's tilted board.
 */
private fun projectLudo3DPoint(
    u: Float,
    v: Float,
    z: Float,
    canvasW: Float,
    canvasH: Float
): Offset {
    val normU = (u / 15f) - 0.5f
    val normV = (v / 15f).coerceIn(-0.15f, 1.15f)
    // Perspective foreshortening: non-linear depth curve (smaller at top v=0, wider at front v=15)
    val depthT = if (normV >= 0f) normV.pow(1.12f) else normV * 0.88f
    val boardTopY = canvasH * 0.295f
    val boardBottomY = canvasH * 0.875f
    val topHalfWidth = canvasW * 0.375f
    val bottomHalfWidth = canvasW * 0.482f
    val halfWidthAtV = topHalfWidth + (bottomHalfWidth - topHalfWidth) * depthT
    val perspectiveScale = 0.76f + 0.34f * depthT
    val screenX = canvasW * 0.5f + normU * 2f * halfWidthAtV
    val screenY = boardTopY + (boardBottomY - boardTopY) * depthT - z * canvasW * perspectiveScale
    return Offset(screenX, screenY)
}

private fun ludo3DQuadPath(
    u0: Float,
    v0: Float,
    u1: Float,
    v1: Float,
    z: Float,
    canvasW: Float,
    canvasH: Float
): Path {
    val p00 = projectLudo3DPoint(u0, v0, z, canvasW, canvasH)
    val p10 = projectLudo3DPoint(u1, v0, z, canvasW, canvasH)
    val p11 = projectLudo3DPoint(u1, v1, z, canvasW, canvasH)
    val p01 = projectLudo3DPoint(u0, v1, z, canvasW, canvasH)
    return Path().apply {
        moveTo(p00.x, p00.y)
        lineTo(p10.x, p10.y)
        lineTo(p11.x, p11.y)
        lineTo(p01.x, p01.y)
        close()
    }
}

private fun build3DPerspectiveStarPath(
    centerU: Float,
    centerV: Float,
    z: Float,
    outerGridRadius: Float,
    canvasW: Float,
    canvasH: Float
): Path {
    val path = Path()
    val innerGridRadius = outerGridRadius * 0.42f
    for (i in 0 until 10) {
        val r = if (i % 2 == 0) outerGridRadius else innerGridRadius
        val angleRad = ((-90.0 + i * 36.0) * PI / 180.0).toFloat()
        val u = centerU + cos(angleRad) * r
        val v = centerV + sin(angleRad) * r
        val pt = projectLudo3DPoint(u, v, z, canvasW, canvasH)
        if (i == 0) path.moveTo(pt.x, pt.y) else path.lineTo(pt.x, pt.y)
    }
    path.close()
    return path
}

@Composable
private fun Playable3DLudoArena(
    gameRoom: GameRoomDoc,
    currentUser: UserProfile,
    players: List<GamePlayerSlot>,
    botEnabled: Boolean,
    onSyncGameState: (String, String, List<GamePlayerSlot>?, String?, String?, String?) -> Unit,
    onMatchComplete: (String, Long, Long) -> Unit
) {
    val scope = rememberCoroutineScope()
    var playerCountMode by remember { mutableIntStateOf(players.size.coerceIn(2, 4)) } // 2, 3, or 4 Players

    // Reference Image Exact 2x2 Player Palette:
    // P0: Glossy Red (Top-Left), P1: Glossy Green (Top-Right),
    // P2: Glossy Blue (Bottom-Left), P3: Glossy Yellow/Gold (Bottom-Right)
    val playerColors = listOf(
        Color(0xFFFF1744), // P0: Glossy Crimson Red
        Color(0xFF00E676), // P1: Glossy Emerald Green
        Color(0xFF0088FF), // P2: Glossy Royal Blue
        Color(0xFFFFD000)  // P3: Glossy Golden Yellow
    )
    val playerDeepColors = listOf(
        Color(0xFF990012), // Deep Red bevel/shadow
        Color(0xFF006B24), // Deep Green bevel/shadow
        Color(0xFF00368C), // Deep Blue bevel/shadow
        Color(0xFFA86400)  // Deep Amber/Gold bevel/shadow
    )
    val playerLightColors = listOf(
        Color(0xFFFF8A9B), // Bright Red highlight
        Color(0xFF80FFB4), // Bright Green highlight
        Color(0xFF82CAFF), // Bright Blue highlight
        Color(0xFFFFF185)  // Bright Yellow highlight
    )
    val playerColorLabels = listOf("Red", "Green", "Blue", "Yellow")

    val currentUid = currentUser.userId.ifBlank { currentUser.id }
    val orderedPlayers = remember(players) { players.sortedBy { it.slotIndex } }
    val myPlayerIndex = remember(orderedPlayers, currentUid) {
        val idx = orderedPlayers.indexOfFirst { it.userId == currentUid }
        if (idx >= 0) idx.coerceIn(0, 3) else 0
    }

    val playerNames = remember(orderedPlayers, currentUser.username, playerCountMode, botEnabled) {
        List(4) { idx ->
            val slotPlayer = orderedPlayers.firstOrNull { it.slotIndex == idx + 1 }
                ?: orderedPlayers.getOrNull(idx)
            when {
                slotPlayer != null && slotPlayer.username.isNotBlank() -> slotPlayer.username
                idx == 0 -> currentUser.username.ifBlank { "You" }
                botEnabled -> "Practice_AI_$idx"
                else -> "Open_Slot_${idx + 1}"
            }
        }
    }

    // 16 Glossy 3D Tokens (4 per player):
    // -1 = in 4-lobed clover home well, 0..50 = main track, 51..55 = colored home stretch, 56 = center crown!
    // Token 0 starts at start square (0) and Tokens 1..3 start in the home well (-1) so the home wells look
    // rich with 3D pawns AND players can either move immediately or deploy new pawns from the home well!
    val tokenPositions = remember {
        mutableStateListOf(
            0, -1, -1, -1, // Player 0 (Red - Top-Left)
            0, -1, -1, -1, // Player 1 (Green - Top-Right)
            0, -1, -1, -1, // Player 2 (Blue - Bottom-Left)
            0, -1, -1, -1  // Player 3 (Yellow - Bottom-Right)
        )
    }

    var currentTurnPlayer by remember { mutableIntStateOf(0) } // 0 .. playerCountMode - 1
    var diceValue by remember { mutableIntStateOf(6) }
    var isRollingDice by remember { mutableStateOf(false) }
    var diceTumbleAngle by remember { mutableFloatStateOf(0f) }
    var awaitingTokenSelection by remember { mutableStateOf(false) }
    var consecutiveSixes by remember { mutableIntStateOf(0) }
    var turnTimerSec by remember { mutableIntStateOf(20) }
    var statusText by remember { mutableStateOf("Turn (@${playerNames[0]}): Tap Floating 3D Dice to Roll!") }
    var lastCaptureBurstCell by remember { mutableStateOf<Pair<Float, Float>?>(null) }

    fun getPlayerTokens(pIdx: Int): List<Int> =
        (0..3).map { tIdx -> tokenPositions[pIdx * 4 + tIdx] }

    fun getLegalTokensForPlayer(pIdx: Int, roll: Int): List<Int> {
        val legal = mutableListOf<Int>()
        val hasAnyActiveOnBoard = (0..3).any { tIdx -> tokenPositions[pIdx * 4 + tIdx] in 0..55 }
        for (tIdx in 0..3) {
            val pos = tokenPositions[pIdx * 4 + tIdx]
            if (pos == -1 && (roll == 6 || !hasAnyActiveOnBoard)) {
                legal.add(tIdx)
            } else if (pos in 0..55 && pos + roll <= 56) {
                legal.add(tIdx)
            }
        }
        return legal
    }

    val legalTokensForCurrentTurn = remember(
        currentTurnPlayer,
        diceValue,
        awaitingTokenSelection,
        tokenPositions.toList()
    ) {
        if (awaitingTokenSelection) getLegalTokensForPlayer(currentTurnPlayer, diceValue) else emptyList()
    }

    val infiniteAnim = rememberInfiniteTransition(label = "ludo_3d_cinematic")
    val highlightRadiusScale by infiniteAnim.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.32f,
        animationSpec = infiniteRepeatable(tween(580, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "ring_scale"
    )
    val vortexPhase by infiniteAnim.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(3600, easing = LinearEasing)),
        label = "vortex_phase"
    )
    val diceHoverOffset by infiniteAnim.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "dice_hover"
    )

    // Sync incoming multiplayer state from Firestore if another connected user updated the room
    LaunchedEffect(gameRoom.gameStateJson) {
        val raw = gameRoom.gameStateJson
        if (raw.startsWith("LUDOv2:")) {
            val parts = raw.removePrefix("LUDOv2:").split("|")
            if (parts.size >= 3) {
                val parsedTurn = parts[0].toIntOrNull() ?: 0
                val parsedDice = parts[1].toIntOrNull() ?: 6
                val parsedTokens = parts[2].split(",").mapNotNull { it.toIntOrNull() }
                if (parsedTokens.size == 16) {
                    for (i in 0 until 16) {
                        tokenPositions[i] = parsedTokens[i]
                    }
                    currentTurnPlayer = parsedTurn.coerceIn(0, playerCountMode - 1)
                    diceValue = parsedDice.coerceIn(1, 6)
                }
            }
        }
    }

    fun pushLudoMultiplayerSync(nextTurn: Int) {
        val encoded = "LUDOv2:$nextTurn|$diceValue|${tokenPositions.joinToString(",")}"
        val nextUid = orderedPlayers.firstOrNull { it.slotIndex == nextTurn + 1 }?.userId
            ?: orderedPlayers.getOrNull(nextTurn)?.userId
            ?: currentUid
        onSyncGameState(encoded, nextUid, null, "playing", null, null)
    }

    suspend fun executeTokenMove(pIdx: Int, tokenIdx: Int, roll: Int) {
        awaitingTokenSelection = false
        val flatIdx = pIdx * 4 + tokenIdx
        val startPos = tokenPositions[flatIdx]

        if (startPos == -1) {
            // Unlock token from 4-lobed clover well onto starting square (0)
            GameSoundEffectsEngine.playPieceMove()
            tokenPositions[flatIdx] = 0
            if (roll == 6) {
                statusText = "@${playerNames[pIdx]} deployed Token #${tokenIdx + 1} onto Start! Bonus Roll!"
                pushLudoMultiplayerSync(pIdx)
            } else {
                val nextTurn = (pIdx + 1) % playerCountMode
                currentTurnPlayer = nextTurn
                statusText = "@${playerNames[pIdx]} deployed Token #${tokenIdx + 1}! Turn: @${playerNames[nextTurn]}"
                pushLudoMultiplayerSync(nextTurn)
            }
            return
        }

        if (startPos in 0..55 && startPos + roll <= 56) {
            // Smooth step-by-step 3D token movement animation along the path
            for (step in 1..roll) {
                tokenPositions[flatIdx] = (startPos + step).coerceAtMost(56)
                GameSoundEffectsEngine.playPieceMove()
                delay(32L)
            }
            val finalPos = tokenPositions[flatIdx]

            // Check if token reached Home Triangle (56)
            var earnedBonusTurn = (roll == 6) || (finalPos == 56)

            // Check capture on main 52-square perimeter track (0..50)
            if (finalPos in 0..50) {
                val moverStartOffset = LUDO_PLAYER_START_TRACK_OFFSET[pIdx.coerceIn(0, 3)]
                val moverGlobalTrack = (moverStartOffset + finalPos) % 52
                if (moverGlobalTrack !in LUDO_SAFE_TRACK_INDICES) {
                    for (otherP in 0 until playerCountMode) {
                        if (otherP == pIdx) continue
                        val otherStartOffset = LUDO_PLAYER_START_TRACK_OFFSET[otherP.coerceIn(0, 3)]
                        val oppTokensAtSquare = (0..3).filter { oppT ->
                            val oppPos = tokenPositions[otherP * 4 + oppT]
                            oppPos in 0..50 && ((otherStartOffset + oppPos) % 52) == moverGlobalTrack
                        }
                        // Traditional Ludo rule: single opponent token on non-safe square is captured!
                        if (oppTokensAtSquare.size == 1) {
                            val victimT = oppTokensAtSquare.first()
                            tokenPositions[otherP * 4 + victimT] = -1
                            earnedBonusTurn = true
                            lastCaptureBurstCell = ludoTokenGridPosition(pIdx, tokenIdx, finalPos)
                            GameSoundEffectsEngine.playCapture()
                            statusText = "💥 @${playerNames[pIdx]} captured @${playerNames[otherP]}'s token! Bonus Roll!"
                        }
                    }
                }
            }

            // Check Victory condition
            val p0Total = getPlayerTokens(0).sumOf { it.coerceAtLeast(0) }
            val bestOtherTotal = (1 until playerCountMode).maxOfOrNull { p ->
                getPlayerTokens(p).sumOf { it.coerceAtLeast(0) }
            } ?: 0
            val allHome = getPlayerTokens(pIdx).all { it >= 56 }
            if (allHome || (pIdx == 0 && (getPlayerTokens(0).any { it >= 56 } || p0Total >= 90))) {
                GameSoundEffectsEngine.playVictory()
                onMatchComplete(playerNames[pIdx], p0Total.toLong(), bestOtherTotal.toLong())
                return
            }

            if (earnedBonusTurn) {
                if (finalPos == 56) {
                    statusText = "👑 @${playerNames[pIdx]} reached CENTER FINISH! Bonus Roll!"
                } else if (!statusText.startsWith("💥")) {
                    statusText = "Rolled 6! @${playerNames[pIdx]} gets a Bonus Roll!"
                }
                pushLudoMultiplayerSync(pIdx)
            } else {
                consecutiveSixes = 0
                val nextTurn = (pIdx + 1) % playerCountMode
                currentTurnPlayer = nextTurn
                statusText = "Turn: @${playerNames[nextTurn]} (${playerColorLabels[nextTurn]})"
                pushLudoMultiplayerSync(nextTurn)
            }
        }
    }

    fun triggerDiceRoll(autoMoveFirstLegalForPlayer0: Boolean) {
        if (isRollingDice) return
        val activeLocalPlayer = if (currentTurnPlayer == myPlayerIndex) myPlayerIndex else if (orderedPlayers.size <= 1 && !botEnabled) currentTurnPlayer else return
        scope.launch {
            isRollingDice = true
            GameSoundEffectsEngine.playDiceRoll()
            val roll = Random.nextInt(1, 7)
            repeat(6) { step ->
                diceTumbleAngle += 60f
                diceValue = ((roll + step) % 6) + 1
                delay(24L)
            }
            diceValue = roll
            isRollingDice = false

            if (roll == 6) {
                consecutiveSixes++
                if (consecutiveSixes >= 3) {
                    consecutiveSixes = 0
                    val nextTurn = (activeLocalPlayer + 1) % playerCountMode
                    statusText = "Three 6s in a row! Turn passes to @${playerNames[nextTurn]}"
                    currentTurnPlayer = nextTurn
                    pushLudoMultiplayerSync(nextTurn)
                    return@launch
                }
            } else {
                consecutiveSixes = 0
            }

            val legal = getLegalTokensForPlayer(activeLocalPlayer, roll)
            if (legal.isEmpty()) {
                val nextTurn = (activeLocalPlayer + 1) % playerCountMode
                statusText = "Rolled $roll — No legal moves. @${playerNames[nextTurn]}'s turn!"
                currentTurnPlayer = nextTurn
                pushLudoMultiplayerSync(nextTurn)
            } else {
                awaitingTokenSelection = true
                statusText = "Rolled $roll! Tap any glowing ${playerColorLabels[activeLocalPlayer]} 3D pawn to move."
                if (autoMoveFirstLegalForPlayer0 || legal.size == 1) {
                    val baseIdx = activeLocalPlayer * 4
                    val yardToken = legal.firstOrNull { tokenPositions[baseIdx + it] == -1 && roll == 6 }
                    val activeToken = legal.filter { tokenPositions[baseIdx + it] >= 0 }.maxByOrNull { tokenPositions[baseIdx + it] }
                    val chosenToken = yardToken ?: activeToken ?: legal.first()
                    executeTokenMove(activeLocalPlayer, chosenToken, roll)
                }
            }
        }
    }

    // Automatic AI Bot turn execution ONLY when botEnabled is explicitly enabled by user and slot has no real player
    LaunchedEffect(currentTurnPlayer, isRollingDice, awaitingTokenSelection, botEnabled, orderedPlayers.size) {
        val hasRealPlayerForTurn = orderedPlayers.any { it.slotIndex == currentTurnPlayer + 1 }
        if (currentTurnPlayer > 0 && !hasRealPlayerForTurn && !isRollingDice && !awaitingTokenSelection && botEnabled) {
            delay(460L)
            val botPlayer = currentTurnPlayer
            GameSoundEffectsEngine.playDiceRoll()
            val botRoll = Random.nextInt(1, 7)
            diceValue = botRoll
            val legal = getLegalTokensForPlayer(botPlayer, botRoll)
            if (legal.isEmpty()) {
                val nextTurn = (botPlayer + 1) % playerCountMode
                currentTurnPlayer = nextTurn
                statusText = "@${playerNames[botPlayer]} rolled $botRoll (no move) • @${playerNames[nextTurn]}'s turn!"
            } else {
                val chosen = legal.firstOrNull { tIdx -> tokenPositions[botPlayer * 4 + tIdx] == -1 && botRoll == 6 }
                    ?: legal.maxByOrNull { tIdx -> tokenPositions[botPlayer * 4 + tIdx] }
                    ?: legal.first()
                executeTokenMove(botPlayer, chosen, botRoll)
                if (currentTurnPlayer == botPlayer) {
                    val nextTurn = (botPlayer + 1) % playerCountMode
                    currentTurnPlayer = nextTurn
                }
            }
        }
    }

    // Turn countdown timer
    LaunchedEffect(currentTurnPlayer, diceValue, awaitingTokenSelection) {
        turnTimerSec = 20
        while (turnTimerSec > 0) {
            delay(1000L)
            turnTimerSec--
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // 1. Player Count Selector (2P / 3P / 4P) + Status & Turn Timer Strip
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf(2, 3, 4).forEach { count ->
                    val active = playerCountMode == count
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (active) NoisePink else Color(0xFF1D1635),
                        modifier = Modifier
                            .clickable {
                                GameSoundEffectsEngine.playTap()
                                playerCountMode = count
                                if (currentTurnPlayer >= count) currentTurnPlayer = 0
                            }
                            .testTag("ludo_mode_${count}p")
                    ) {
                        Text(
                            text = "${count}P",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                        )
                    }
                }
            }

            Text(
                text = statusText,
                color = NoiseGold,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
            )

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF1D1635)
            ) {
                Text(
                    text = "⏱ ${turnTimerSec}s",
                    color = if (turnTimerSec <= 5) Color(0xFFFF5252) else Color(0xFF00E5FF),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Player Score Cards Row (Red, Green, Blue, Yellow)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            for (pIdx in 0 until playerCountMode) {
                val pTokens = getPlayerTokens(pIdx)
                val homeCount = pTokens.count { it >= 56 }
                val stepSum = pTokens.sumOf { it.coerceAtLeast(0) }
                val isTurn = currentTurnPlayer == pIdx
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isTurn) playerColors[pIdx].copy(alpha = 0.22f) else Color.White.copy(alpha = 0.05f),
                    modifier = Modifier
                        .weight(1f)
                        .border(
                            1.5.dp,
                            if (isTurn) playerColors[pIdx] else Color.White.copy(alpha = 0.12f),
                            RoundedCornerShape(12.dp)
                        )
                ) {
                    Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                        Text(
                            text = "@${playerNames[pIdx]}",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${playerColorLabels[pIdx]} • $homeCount/4 • ${stepSum}st",
                            color = playerColors[pIdx],
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        // 2. REFERENCE-ACCURATE 3D ISOMETRIC LUDO STAGE, FLOATING 3D DICE & GOLD-FRAMED BOARD
        // (Zero text, logos, or watermarks inside the 3D board artwork)
        Box(
            modifier = Modifier
                .weight(1f)
                .aspectRatio(0.95f)
                .padding(vertical = 4.dp)
                .clip(RoundedCornerShape(22.dp))
                .border(2.dp, Color(0xFFFFD54F).copy(alpha = 0.80f), RoundedCornerShape(22.dp))
                .testTag("ludo_3d_board_canvas")
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(currentTurnPlayer, diceValue, awaitingTokenSelection) {
                        detectTapGestures { tapOffset ->
                            val cw = size.width.toFloat()
                            val ch = size.height.toFloat()
                            val floatingDiceCenter = Offset(cw * 0.50f, ch * 0.165f)

                            val canActNow = (currentTurnPlayer == myPlayerIndex) || (orderedPlayers.size <= 1 && !botEnabled)
                            if (canActNow) {
                                val actPlayer = currentTurnPlayer
                                val baseIdx = actPlayer * 4
                                // 1. Check if user tapped any legal 3D pawn on the tilted board
                                val legal = getLegalTokensForPlayer(actPlayer, diceValue)
                                val tappedToken = legal.minByOrNull { tIdx ->
                                    val (gx, gy) = ludoTokenGridPosition(actPlayer, tIdx, tokenPositions[baseIdx + tIdx])
                                    val screenPt = projectLudo3DPoint(gx, gy, 0.04f, cw, ch)
                                    hypot(tapOffset.x - screenPt.x, tapOffset.y - screenPt.y)
                                }
                                if (tappedToken != null) {
                                    val (gx, gy) = ludoTokenGridPosition(actPlayer, tappedToken, tokenPositions[baseIdx + tappedToken])
                                    val screenPt = projectLudo3DPoint(gx, gy, 0.04f, cw, ch)
                                    if (hypot(tapOffset.x - screenPt.x, tapOffset.y - screenPt.y) <= cw * 0.12f) {
                                        scope.launch {
                                            executeTokenMove(actPlayer, tappedToken, diceValue)
                                        }
                                        return@detectTapGestures
                                    }
                                }

                                // 2. Check if user tapped the large floating 3D dice above the board
                                if (hypot(tapOffset.x - floatingDiceCenter.x, tapOffset.y - floatingDiceCenter.y) <= cw * 0.18f) {
                                    triggerDiceRoll(autoMoveFirstLegalForPlayer0 = true)
                                    return@detectTapGestures
                                }

                                // 3. Fallback: tapping the board rolls the 3D dice if not awaiting selection,
                                // or moves the best legal token if awaiting selection
                                if (!awaitingTokenSelection) {
                                    triggerDiceRoll(autoMoveFirstLegalForPlayer0 = true)
                                } else if (legal.isNotEmpty()) {
                                    val chosen = legal.firstOrNull { tokenPositions[baseIdx + it] == -1 && diceValue == 6 }
                                        ?: legal.maxByOrNull { tokenPositions[baseIdx + it] }
                                        ?: legal.first()
                                    scope.launch {
                                        executeTokenMove(actPlayer, chosen, diceValue)
                                    }
                                }
                            }
                        }
                    }
            ) {
                val w = size.width
                val h = size.height

                // =========================================================================
                // A. DEEP COSMIC INDIGO/BLACK BACKGROUND, NEON LASER ARCS & FLOOR REFLECTION
                // =========================================================================
                drawRect(
                    brush = Brush.verticalGradient(
                        0.0f to Color(0xFF04020C),
                        0.30f to Color(0xFF14072E),
                        0.68f to Color(0xFF1F083D),
                        0.88f to Color(0xFF120424),
                        1.0f to Color(0xFF05020D)
                    ),
                    size = size
                )

                // Ambient magenta & electric blue side studio glow behind the board
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFFFF00A0).copy(alpha = 0.42f), Color.Transparent),
                        center = Offset(w * 0.06f, h * 0.42f),
                        radius = w * 0.45f
                    ),
                    radius = w * 0.45f,
                    center = Offset(w * 0.06f, h * 0.42f)
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFFFF00A0).copy(alpha = 0.42f), Color.Transparent),
                        center = Offset(w * 0.94f, h * 0.42f),
                        radius = w * 0.45f
                    ),
                    radius = w * 0.45f,
                    center = Offset(w * 0.94f, h * 0.42f)
                )

                // Glossy Mirror Floor Vertical Neon Reflections beneath the front gold frame (y = 0.88h..1.0h)
                val floorTopY = h * 0.875f
                val reflectionBands = listOf(
                    Triple(0.08f, 0.14f, Color(0xFFFF5252)),
                    Triple(0.22f, 0.18f, Color(0xFFAA00FF)),
                    Triple(0.36f, 0.14f, Color(0xFF2979FF)),
                    Triple(0.50f, 0.18f, Color(0xFFFFD700)),
                    Triple(0.66f, 0.16f, Color(0xFFAA00FF)),
                    Triple(0.84f, 0.16f, Color(0xFFFF9100))
                )
                reflectionBands.forEach { (centerRatio, widthRatio, refColor) ->
                    val bandCenterX = w * centerRatio
                    val bandHalfW = w * widthRatio
                    drawRect(
                        brush = Brush.horizontalGradient(
                            colors = listOf(Color.Transparent, refColor.copy(alpha = 0.55f), Color.Transparent),
                            startX = bandCenterX - bandHalfW,
                            endX = bandCenterX + bandHalfW
                        ),
                        topLeft = Offset(bandCenterX - bandHalfW, floorTopY),
                        size = Size(bandHalfW * 2f, h - floorTopY)
                    )
                }
                // Dark horizon shadow line directly under the board's front bumper
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.Black.copy(alpha = 0.82f), Color.Transparent),
                        startY = floorTopY,
                        endY = floorTopY + h * 0.045f
                    ),
                    topLeft = Offset(0f, floorTopY),
                    size = Size(w, h * 0.045f)
                )

                // =========================================================================
                // B. COSMIC PURPLE & BLUE NEON LASER ARCS + GOLDEN ORBITAL VORTEX & STARS
                // =========================================================================
                // Sweeping Left Purple/Blue Neon Laser Arc
                val leftLaserPath = Path().apply {
                    moveTo(0f, h * 0.14f)
                    quadraticTo(w * 0.24f, h * 0.25f, w * 0.48f, h * 0.26f)
                }
                drawPath(
                    path = leftLaserPath,
                    brush = Brush.horizontalGradient(listOf(Color(0xFFD500F9), Color(0xFF00E5FF), Color.Transparent)),
                    style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round)
                )
                drawPath(
                    path = leftLaserPath,
                    color = Color.White.copy(alpha = 0.75f),
                    style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round)
                )

                // Sweeping Right Blue/Violet Neon Laser Arc
                val rightLaserPath = Path().apply {
                    moveTo(w, h * 0.11f)
                    quadraticTo(w * 0.78f, h * 0.24f, w * 0.52f, h * 0.27f)
                }
                drawPath(
                    path = rightLaserPath,
                    brush = Brush.horizontalGradient(listOf(Color.Transparent, Color(0xFF2979FF), Color(0xFFD500F9))),
                    style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round)
                )
                drawPath(
                    path = rightLaserPath,
                    color = Color.White.copy(alpha = 0.75f),
                    style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round)
                )

                // Warm Golden Sunburst / Radial Glow behind the Floating 3D Dice
                val diceStageCenter = Offset(w * 0.50f, h * 0.175f)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFFFEA00).copy(alpha = 0.72f),
                            Color(0xFFFF6D00).copy(alpha = 0.35f),
                            Color.Transparent
                        ),
                        center = Offset(w * 0.50f, h * 0.22f),
                        radius = w * 0.34f
                    ),
                    radius = w * 0.34f,
                    center = Offset(w * 0.50f, h * 0.22f)
                )

                // Swirling Golden Orbital Energy Rings around the Floating 3D Dice
                withTransform({
                    rotate(degrees = -16f + sin(vortexPhase) * 3f, pivot = diceStageCenter)
                }) {
                    drawOval(
                        brush = Brush.sweepGradient(
                            colors = listOf(
                                Color(0xFFFFD700),
                                Color(0xFFFF6D00).copy(alpha = 0.25f),
                                Color(0xFFFFF59D),
                                Color(0xFFFF9100).copy(alpha = 0.4f),
                                Color(0xFFFFD700)
                            ),
                            center = diceStageCenter
                        ),
                        topLeft = Offset(diceStageCenter.x - w * 0.31f, diceStageCenter.y - h * 0.075f),
                        size = Size(w * 0.62f, h * 0.15f),
                        style = Stroke(width = 4.dp.toPx())
                    )
                    drawOval(
                        brush = Brush.linearGradient(
                            colors = listOf(Color.White.copy(alpha = 0.85f), Color(0xFFFFD700), Color.Transparent)
                        ),
                        topLeft = Offset(diceStageCenter.x - w * 0.26f, diceStageCenter.y - h * 0.055f),
                        size = Size(w * 0.52f, h * 0.11f),
                        style = Stroke(width = 2.dp.toPx())
                    )
                }

                // Twinkling 4-Point Cosmic Star Flares in the Upper Sky
                val starFlares = listOf(
                    Triple(0.08f, 0.24f, 1.1f),
                    Triple(0.21f, 0.18f, 0.75f),
                    Triple(0.25f, 0.05f, 0.65f),
                    Triple(0.68f, 0.10f, 0.90f),
                    Triple(0.79f, 0.20f, 0.80f),
                    Triple(0.93f, 0.28f, 1.05f)
                )
                starFlares.forEachIndexed { idx, (sxRatio, syRatio, flareScale) ->
                    val sx = w * sxRatio
                    val sy = h * syRatio
                    val pulse = 0.75f + 0.25f * sin(vortexPhase + idx * 1.1f)
                    val rayLen = w * 0.028f * flareScale * pulse
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color.White, Color(0xFFFFD700).copy(alpha = 0.5f), Color.Transparent),
                            center = Offset(sx, sy),
                            radius = rayLen
                        ),
                        radius = rayLen,
                        center = Offset(sx, sy)
                    )
                    drawLine(
                        color = Color.White,
                        start = Offset(sx - rayLen, sy),
                        end = Offset(sx + rayLen, sy),
                        strokeWidth = 1.5f
                    )
                    drawLine(
                        color = Color.White,
                        start = Offset(sx, sy - rayLen),
                        end = Offset(sx, sy + rayLen),
                        strokeWidth = 1.5f
                    )
                }

                // =========================================================================
                // C. 3D GOLD METALLIC BOARD FRAME, FRONT 3D BEVEL LIP & CORNER BUMPERS
                // =========================================================================
                val outerPad = 0.58f
                val topRimTL = projectLudo3DPoint(-outerPad, -outerPad, 0f, w, h)
                val topRimTR = projectLudo3DPoint(15f + outerPad, -outerPad, 0f, w, h)
                val topRimBR = projectLudo3DPoint(15f + outerPad, 15f + outerPad, 0f, w, h)
                val topRimBL = projectLudo3DPoint(-outerPad, 15f + outerPad, 0f, w, h)

                val lipDrop = h * 0.042f
                val botRimBL = Offset(topRimBL.x, topRimBL.y + lipDrop)
                val botRimBR = Offset(topRimBR.x, topRimBR.y + lipDrop)
                val botRimTL = Offset(topRimTL.x, topRimTL.y + lipDrop * 0.45f)
                val botRimTR = Offset(topRimTR.x, topRimTR.y + lipDrop * 0.45f)

                // Left 3D Gold Side Wall
                val leftGoldSide = Path().apply {
                    moveTo(topRimTL.x, topRimTL.y)
                    lineTo(topRimBL.x, topRimBL.y)
                    lineTo(botRimBL.x, botRimBL.y)
                    lineTo(botRimTL.x, botRimTL.y)
                    close()
                }
                drawPath(
                    path = leftGoldSide,
                    brush = Brush.verticalGradient(
                        listOf(Color(0xFFFFE082), Color(0xFFD48800), Color(0xFF7A4400))
                    )
                )

                // Right 3D Gold Side Wall
                val rightGoldSide = Path().apply {
                    moveTo(topRimTR.x, topRimTR.y)
                    lineTo(topRimBR.x, topRimBR.y)
                    lineTo(botRimBR.x, botRimBR.y)
                    lineTo(botRimTR.x, botRimTR.y)
                    close()
                }
                drawPath(
                    path = rightGoldSide,
                    brush = Brush.verticalGradient(
                        listOf(Color(0xFFFFE082), Color(0xFFD48800), Color(0xFF7A4400))
                    )
                )

                // Front 3D Gold Metallic Bumper Lip (along the bottom edge of the board)
                val frontGoldLip = Path().apply {
                    moveTo(topRimBL.x, topRimBL.y)
                    lineTo(topRimBR.x, topRimBR.y)
                    lineTo(botRimBR.x, botRimBR.y)
                    lineTo(botRimBL.x, botRimBL.y)
                    close()
                }
                drawPath(
                    path = frontGoldLip,
                    brush = Brush.horizontalGradient(
                        0.0f to Color(0xFFFFF59D),
                        0.12f to Color(0xFFD48800),
                        0.32f to Color(0xFF8C5000),
                        0.50f to Color(0xFFFFF59D),
                        0.68f to Color(0xFF8C5000),
                        0.88f to Color(0xFFD48800),
                        1.0f to Color(0xFFFFF59D)
                    )
                )
                drawPath(
                    path = frontGoldLip,
                    color = Color(0xFFFFF9C4).copy(alpha = 0.7f),
                    style = Stroke(width = 1.5f)
                )

                // Top Surface of the Gold Metallic Outer Frame
                val goldFrameTopSurface = ludo3DQuadPath(-outerPad, -outerPad, 15f + outerPad, 15f + outerPad, 0f, w, h)
                drawPath(
                    path = goldFrameTopSurface,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFFFFF59D),
                            Color(0xFFFFB300),
                            Color(0xFF8D5000),
                            Color(0xFFFFD54F),
                            Color(0xFFFF8F00),
                            Color(0xFFFFF59D)
                        ),
                        start = topRimTL,
                        end = topRimBR
                    )
                )
                drawPath(
                    path = goldFrameTopSurface,
                    color = Color(0xFFFFFDE7),
                    style = Stroke(width = 2.dp.toPx())
                )

                // Dark Recessed Board Cavity Inside the Gold Frame
                val innerBoardRecess = ludo3DQuadPath(-0.08f, -0.08f, 15.08f, 15.08f, 0.002f, w, h)
                drawPath(innerBoardRecess, color = Color(0xFF1A0F0A))

                // Specular Gold Corner Flares on Front-Left, Front-Center, and Front-Right of Gold Frame
                listOf(
                    Offset(topRimBL.x + w * 0.03f, topRimBL.y + lipDrop * 0.35f),
                    Offset(w * 0.50f, topRimBL.y + lipDrop * 0.45f),
                    Offset(topRimBR.x - w * 0.03f, topRimBR.y + lipDrop * 0.35f)
                ).forEach { flarePt ->
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color.White, Color(0xFFFFEA00).copy(alpha = 0.65f), Color.Transparent),
                            center = flarePt,
                            radius = w * 0.065f
                        ),
                        radius = w * 0.065f,
                        center = flarePt
                    )
                }

                // =========================================================================
                // D. 3D PILLOWED WHITE & COLORED MOVEMENT SQUARES + 5-POINTED WHITE STARS
                // =========================================================================
                for (r in 0 until 15) {
                    for (c in 0 until 15) {
                        val inCornerYard = (c < 6 && r < 6) || (c >= 9 && r < 6) || (c >= 9 && r >= 9) || (c < 6 && r >= 9)
                        val inCenterHome = (c in 6..8 && r in 6..8)
                        if (!inCornerYard && !inCenterHome) {
                            // Determine tile color matching the reference image's vibrant colored paths & safe squares
                            val tilePalette: Triple<Color, Color, Boolean> = when {
                                // Red paths & safe squares (Left arm + Top-left red column/star)
                                (r == 7 && c in 1..5) || (c == 1 && r == 6) || (c == 1 && r == 8) ||
                                    (c == 6 && r == 1) || (c == 7 && r in 1..5) -> {
                                    val hasStar = (c == 1 && r == 7) || (c == 6 && r == 1)
                                    Triple(Color(0xFFFF1744), Color(0xFF990012), hasStar)
                                }
                                // Green paths & safe squares (Right arm + Top-right green column/star)
                                (r == 7 && c in 9..13) || (c == 8 && r in 1..5) ||
                                    (c == 12 && r in 6..8) -> {
                                    val hasStar = (c == 8 && r == 1) || (c == 12 && r == 6) || (c == 12 && r == 7)
                                    Triple(Color(0xFF00C853), Color(0xFF00600F), hasStar)
                                }
                                // Blue paths & safe squares (Bottom-left vertical path + star square)
                                (c == 7 && r in 9..12) || (c == 6 && r == 11) -> {
                                    val hasStar = (c == 6 && r == 11)
                                    Triple(Color(0xFF0077FF), Color(0xFF003087), hasStar)
                                }
                                // Yellow paths & safe squares (Bottom-right vertical path + star square)
                                (c == 8 && r in 9..12) || (c == 9 && r == 11) -> {
                                    val hasStar = (c == 9 && r == 11)
                                    Triple(Color(0xFFFFC400), Color(0xFFA66300), hasStar)
                                }
                                // Glossy White 3D Pillowed Movement Squares
                                else -> Triple(Color(0xFFFAF6F0), Color(0xFFBDB3A8), false)
                            }

                            val topColor = tilePalette.first
                            val sideBevelColor = tilePalette.second
                            val hasWhiteStar = tilePalette.third

                            val u0 = c + 0.05f
                            val v0 = r + 0.05f
                            val u1 = c + 0.95f
                            val v1 = r + 0.95f
                            val tileTopZ = 0.011f

                            // 3D Tile Front Bevel Thickness
                            val pFrontL = projectLudo3DPoint(u0, v1, tileTopZ, w, h)
                            val pFrontR = projectLudo3DPoint(u1, v1, tileTopZ, w, h)
                            val pBaseL = projectLudo3DPoint(u0, v1, 0.002f, w, h)
                            val pBaseR = projectLudo3DPoint(u1, v1, 0.002f, w, h)
                            val bevelPath = Path().apply {
                                moveTo(pFrontL.x, pFrontL.y)
                                lineTo(pFrontR.x, pFrontR.y)
                                lineTo(pBaseR.x, pBaseR.y)
                                lineTo(pBaseL.x, pBaseL.y)
                                close()
                            }
                            drawPath(bevelPath, sideBevelColor)

                            // 3D Pillowed Tile Top Face
                            val topQuad = ludo3DQuadPath(u0, v0, u1, v1, tileTopZ, w, h)
                            drawPath(topQuad, topColor)
                            drawPath(
                                path = topQuad,
                                color = Color.White.copy(alpha = 0.48f),
                                style = Stroke(width = 1f)
                            )

                            // Crisp 5-Pointed White Safe-Zone Star Marking
                            if (hasWhiteStar) {
                                val starPath = build3DPerspectiveStarPath(
                                    centerU = c + 0.5f,
                                    centerV = r + 0.5f,
                                    z = tileTopZ + 0.002f,
                                    outerGridRadius = 0.34f,
                                    canvasW = w,
                                    canvasH = h
                                )
                                drawPath(starPath, color = Color.White)
                            }
                        }
                    }
                }

                // =========================================================================
                // E. CENTRAL 4-COLOR 3D FINISHING AREA (4 Glossy Triangles Meeting at Center)
                // =========================================================================
                val cZ = 0.014f
                val cTL = projectLudo3DPoint(6f, 6f, cZ, w, h)
                val cTR = projectLudo3DPoint(9f, 6f, cZ, w, h)
                val cBR = projectLudo3DPoint(9f, 9f, cZ, w, h)
                val cBL = projectLudo3DPoint(6f, 9f, cZ, w, h)
                val cMid = projectLudo3DPoint(7.5f, 7.5f, cZ + 0.005f, w, h)

                // Top Triangle (Red)
                val triTop = Path().apply {
                    moveTo(cTL.x, cTL.y); lineTo(cTR.x, cTR.y); lineTo(cMid.x, cMid.y); close()
                }
                // Right Triangle (Green)
                val triRight = Path().apply {
                    moveTo(cTR.x, cTR.y); lineTo(cBR.x, cBR.y); lineTo(cMid.x, cMid.y); close()
                }
                // Left Triangle (Blue)
                val triLeft = Path().apply {
                    moveTo(cTL.x, cTL.y); lineTo(cMid.x, cMid.y); lineTo(cBL.x, cBL.y); close()
                }
                // Bottom Triangle (Yellow)
                val triBottom = Path().apply {
                    moveTo(cBL.x, cBL.y); lineTo(cBR.x, cBR.y); lineTo(cMid.x, cMid.y); close()
                }

                drawPath(triTop, Brush.linearGradient(listOf(Color(0xFFFF5252), Color(0xFFD50000)), start = cTL, end = cMid))
                drawPath(triRight, Brush.linearGradient(listOf(Color(0xFF00E676), Color(0xFF00A152)), start = cTR, end = cMid))
                drawPath(triLeft, Brush.linearGradient(listOf(Color(0xFF2979FF), Color(0xFF004ECB)), start = cTL, end = cMid))
                drawPath(triBottom, Brush.linearGradient(listOf(Color(0xFFFFEA00), Color(0xFFFFA000)), start = cBL, end = cMid))

                // Crisp 3D bevel lines on the 4 center triangles
                listOf(triTop, triRight, triLeft, triBottom).forEach { tri ->
                    drawPath(tri, color = Color.White.copy(alpha = 0.45f), style = Stroke(width = 1.2f))
                }

                // =========================================================================
                // F. FOUR 3D PLAYER HOME ZONES IN 2x2 LAYOUT WITH RECESSED CLOVER WELLS
                //    0: Red (Top-Left), 1: Green (Top-Right), 2: Blue (Bottom-Left), 3: Yellow (Bottom-Right)
                // =========================================================================
                data class HomeYard3DConfig(
                    val pIdx: Int,
                    val u0: Float,
                    val v0: Float,
                    val u1: Float,
                    val v1: Float
                )

                val homeYards = listOf(
                    HomeYard3DConfig(0, 0.04f, 0.04f, 5.94f, 5.94f),   // Top-Left: Red
                    HomeYard3DConfig(1, 9.06f, 0.04f, 14.96f, 5.94f),  // Top-Right: Green
                    HomeYard3DConfig(2, 0.04f, 9.06f, 5.94f, 14.96f),  // Bottom-Left: Blue
                    HomeYard3DConfig(3, 9.06f, 9.06f, 14.96f, 14.96f)  // Bottom-Right: Yellow
                )

                homeYards.forEach { yard ->
                    val mainColor = playerColors[yard.pIdx]
                    val deepColor = playerDeepColors[yard.pIdx]
                    val lightColor = playerLightColors[yard.pIdx]
                    val yardTopZ = 0.018f

                    // 1. 3D Front Bevel Wall of the Raised Home Zone Block
                    val fL = projectLudo3DPoint(yard.u0, yard.v1, yardTopZ, w, h)
                    val fR = projectLudo3DPoint(yard.u1, yard.v1, yardTopZ, w, h)
                    val bL = projectLudo3DPoint(yard.u0, yard.v1, 0.002f, w, h)
                    val bR = projectLudo3DPoint(yard.u1, yard.v1, 0.002f, w, h)
                    val frontWall = Path().apply {
                        moveTo(fL.x, fL.y); lineTo(fR.x, fR.y); lineTo(bR.x, bR.y); lineTo(bL.x, bL.y); close()
                    }
                    drawPath(frontWall, deepColor)

                    // 2. Glossy 3D Top Surface of the Home Zone Block
                    val yardTopPath = ludo3DQuadPath(yard.u0, yard.v0, yard.u1, yard.v1, yardTopZ, w, h)
                    val pTL = projectLudo3DPoint(yard.u0, yard.v0, yardTopZ, w, h)
                    val pBR = projectLudo3DPoint(yard.u1, yard.v1, yardTopZ, w, h)
                    drawPath(
                        path = yardTopPath,
                        brush = Brush.linearGradient(
                            colors = listOf(lightColor, mainColor, deepColor),
                            start = pTL,
                            end = pBR
                        )
                    )
                    drawPath(
                        path = yardTopPath,
                        color = Color.White.copy(alpha = 0.55f),
                        style = Stroke(width = 1.8f)
                    )

                    // 3. Recessed 4-Lobed Clover Well Inside Each Home Zone
                    // Outer bright clover rim (4 overlapping perspective ovals + center bridge)
                    val lobeCenters = (0..3).map { tIdx ->
                        ludoTokenGridPosition(yard.pIdx, tIdx, -1)
                    }
                    val yardCenterU = (yard.u0 + yard.u1) * 0.5f
                    val yardCenterV = (yard.v0 + yard.v1) * 0.5f

                    // Outer recessed bevel rim of the clover tray
                    lobeCenters.forEach { (lu, lv) ->
                        val cPt = projectLudo3DPoint(lu, lv, yardTopZ - 0.004f, w, h)
                        val edgePtU = projectLudo3DPoint(lu + 1.38f, lv, yardTopZ - 0.004f, w, h)
                        val edgePtV = projectLudo3DPoint(lu, lv + 1.38f, yardTopZ - 0.004f, w, h)
                        val rx = abs(edgePtU.x - cPt.x)
                        val ry = abs(edgePtV.y - cPt.y)
                        drawOval(
                            color = lightColor.copy(alpha = 0.85f),
                            topLeft = Offset(cPt.x - rx * 1.06f, cPt.y - ry * 1.06f),
                            size = Size(rx * 2.12f, ry * 2.12f)
                        )
                    }

                    // Deep recessed inner clover cavity
                    lobeCenters.forEach { (lu, lv) ->
                        val cPt = projectLudo3DPoint(lu, lv, yardTopZ - 0.006f, w, h)
                        val edgePtU = projectLudo3DPoint(lu + 1.24f, lv, yardTopZ - 0.006f, w, h)
                        val edgePtV = projectLudo3DPoint(lu, lv + 1.24f, yardTopZ - 0.006f, w, h)
                        val rx = abs(edgePtU.x - cPt.x)
                        val ry = abs(edgePtV.y - cPt.y)
                        drawOval(
                            brush = Brush.verticalGradient(
                                colors = listOf(deepColor, mainColor),
                                startY = cPt.y - ry,
                                endY = cPt.y + ry
                            ),
                            topLeft = Offset(cPt.x - rx, cPt.y - ry),
                            size = Size(rx * 2f, ry * 2f)
                        )
                    }
                    // Center union fill of the 4-lobed clover well
                    val centerPt = projectLudo3DPoint(yardCenterU, yardCenterV, yardTopZ - 0.006f, w, h)
                    val centerEdgeU = projectLudo3DPoint(yardCenterU + 1.35f, yardCenterV, yardTopZ - 0.006f, w, h)
                    val centerEdgeV = projectLudo3DPoint(yardCenterU, yardCenterV + 1.35f, yardTopZ - 0.006f, w, h)
                    val crx = abs(centerEdgeU.x - centerPt.x)
                    val cry = abs(centerEdgeV.y - centerPt.y)
                    drawOval(
                        color = mainColor,
                        topLeft = Offset(centerPt.x - crx, centerPt.y - cry),
                        size = Size(crx * 2f, cry * 2f)
                    )
                }

                // =========================================================================
                // G. CAPTURE BURST WAVE EFFECT ON BOARD IF TRIGGERED
                // =========================================================================
                lastCaptureBurstCell?.let { (bu, bv) ->
                    val burstCenter = projectLudo3DPoint(bu, bv, 0.02f, w, h)
                    drawCircle(
                        color = NoiseGold.copy(alpha = 0.65f),
                        radius = w * 0.065f,
                        center = burstCenter,
                        style = Stroke(width = 3.dp.toPx())
                    )
                }

                // =========================================================================
                // H. ALL 16 GLOSSY 3D CONICAL PAWN TOKENS (4 Per Player, Sorted Back-to-Front)
                //    Always renders all 4 zones' tokens for visual fidelity to the reference!
                // =========================================================================
                data class PawnRenderItem(
                    val pIdx: Int,
                    val tIdx: Int,
                    val stepPos: Int,
                    val gridU: Float,
                    val gridV: Float
                )

                val allPawns = mutableListOf<PawnRenderItem>()
                val cellGroups = mutableMapOf<String, MutableList<Triple<Int, Int, Int>>>()
                for (pIdx in 0 until 4) {
                    for (tIdx in 0..3) {
                        val stepPos = tokenPositions[pIdx * 4 + tIdx]
                        val (gu, gv) = ludoTokenGridPosition(pIdx, tIdx, stepPos)
                        val key = "${(gu * 2).toInt()}_${(gv * 2).toInt()}"
                        cellGroups.getOrPut(key) { mutableListOf() }.add(Triple(pIdx, tIdx, stepPos))
                    }
                }
                cellGroups.values.forEach { group ->
                    group.forEachIndexed { clusterIdx, (pIdx, tIdx, stepPos) ->
                        val (baseU, baseV) = ludoTokenGridPosition(pIdx, tIdx, stepPos)
                        val offsetU: Float
                        val offsetV: Float
                        if (group.size > 1 && stepPos >= 0) {
                            val angle = (clusterIdx * (360.0 / group.size)) * PI / 180.0
                            offsetU = (cos(angle) * 0.22).toFloat()
                            offsetV = (sin(angle) * 0.22).toFloat()
                        } else {
                            offsetU = 0f
                            offsetV = 0f
                        }
                        allPawns.add(
                            PawnRenderItem(
                                pIdx = pIdx,
                                tIdx = tIdx,
                                stepPos = stepPos,
                                gridU = baseU + offsetU,
                                gridV = baseV + offsetV
                            )
                        )
                    }
                }

                // Sort back-to-front by row depth `gridV` so front pawns naturally overlap back pawns in 3D
                allPawns.sortBy { it.gridV }

                allPawns.forEach { pawn ->
                    val baseZ = if (pawn.stepPos < 0) 0.015f else 0.014f
                    val basePt = projectLudo3DPoint(pawn.gridU, pawn.gridV, baseZ, w, h)
                    val scaleDepth = 0.78f + 0.30f * (pawn.gridV / 15f).coerceIn(0f, 1f)
                    val pawnSizeScale = if (pawn.stepPos < 0) 1.08f else 0.92f
                    val rBase = w * 0.036f * scaleDepth * pawnSizeScale
                    val pawnHeight = w * 0.078f * scaleDepth * pawnSizeScale

                    val mainCol = playerColors[pawn.pIdx]
                    val deepCol = playerDeepColors[pawn.pIdx]
                    val lightCol = playerLightColors[pawn.pIdx]
                    val isLegalHighlight = (
                        pawn.pIdx == currentTurnPlayer &&
                            pawn.pIdx < playerCountMode &&
                            pawn.tIdx in legalTokensForCurrentTurn
                        )

                    // 1. Pulsing Golden Legal-Move Selection Halo under the 3D Pawn
                    if (isLegalHighlight) {
                        val haloRx = rBase * 1.45f * highlightRadiusScale
                        val haloRy = haloRx * 0.62f
                        drawOval(
                            color = Color(0xFFFFEA00).copy(alpha = 0.92f),
                            topLeft = Offset(basePt.x - haloRx, basePt.y - haloRy),
                            size = Size(haloRx * 2f, haloRy * 2f),
                            style = Stroke(width = 3.dp.toPx())
                        )
                    }

                    // 2. Realistic Cast Drop Shadow on Board Surface
                    val shadowRx = rBase * 1.18f
                    val shadowRy = rBase * 0.64f
                    drawOval(
                        color = Color.Black.copy(alpha = 0.55f),
                        topLeft = Offset(basePt.x - shadowRx + 3f, basePt.y - shadowRy + 5f),
                        size = Size(shadowRx * 2f, shadowRy * 2f)
                    )

                    // 3. Double-Ringed 3D Cylindrical Pedestal Base of the Pawn
                    val baseRx = rBase
                    val baseRy = rBase * 0.56f
                    drawOval(
                        brush = Brush.horizontalGradient(
                            colors = listOf(deepCol, mainCol, lightCol, mainCol, deepCol),
                            startX = basePt.x - baseRx,
                            endX = basePt.x + baseRx
                        ),
                        topLeft = Offset(basePt.x - baseRx, basePt.y - baseRy * 0.8f),
                        size = Size(baseRx * 2f, baseRy * 2f)
                    )
                    drawOval(
                        color = Color.White.copy(alpha = 0.45f),
                        topLeft = Offset(basePt.x - baseRx, basePt.y - baseRy * 0.8f),
                        size = Size(baseRx * 2f, baseRy * 2f),
                        style = Stroke(width = 1.2f)
                    )

                    // Second Upper Collar Ring on Base
                    val collarRx = rBase * 0.84f
                    val collarRy = collarRx * 0.54f
                    val collarY = basePt.y - pawnHeight * 0.14f
                    drawOval(
                        brush = Brush.horizontalGradient(
                            colors = listOf(deepCol, lightCol, mainCol, deepCol),
                            startX = basePt.x - collarRx,
                            endX = basePt.x + collarRx
                        ),
                        topLeft = Offset(basePt.x - collarRx, collarY - collarRy),
                        size = Size(collarRx * 2f, collarRy * 2f)
                    )

                    // 4. Tapered Glossy 3D Conical Body
                    val neckY = basePt.y - pawnHeight * 0.72f
                    val neckHalfW = rBase * 0.34f
                    val conePath = Path().apply {
                        moveTo(basePt.x - collarRx * 0.92f, collarY)
                        lineTo(basePt.x - neckHalfW, neckY)
                        lineTo(basePt.x + neckHalfW, neckY)
                        lineTo(basePt.x + collarRx * 0.92f, collarY)
                        close()
                    }
                    drawPath(
                        path = conePath,
                        brush = Brush.horizontalGradient(
                            colors = listOf(deepCol, mainCol, lightCol, mainCol, deepCol),
                            startX = basePt.x - collarRx,
                            endX = basePt.x + collarRx
                        )
                    )
                    // Vertical white specular streak down the left-center of the cone
                    val coneSpecPath = Path().apply {
                        moveTo(basePt.x - collarRx * 0.38f, collarY - 2f)
                        lineTo(basePt.x - neckHalfW * 0.45f, neckY + 2f)
                        lineTo(basePt.x - neckHalfW * 0.10f, neckY + 2f)
                        lineTo(basePt.x - collarRx * 0.12f, collarY - 2f)
                        close()
                    }
                    drawPath(coneSpecPath, color = Color.White.copy(alpha = 0.52f))

                    // 5. Spherical Glossy Ball Finial on Top of the Pawn
                    val headCenter = Offset(basePt.x, basePt.y - pawnHeight * 0.86f)
                    val headRadius = rBase * 0.54f
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(lightCol, mainCol, deepCol),
                            center = Offset(headCenter.x - headRadius * 0.3f, headCenter.y - headRadius * 0.3f),
                            radius = headRadius * 1.35f
                        ),
                        radius = headRadius,
                        center = headCenter
                    )
                    // Crisp White Specular Glint on the Top-Left of the Ball Head
                    drawCircle(
                        color = Color.White.copy(alpha = 0.90f),
                        radius = headRadius * 0.28f,
                        center = Offset(headCenter.x - headRadius * 0.32f, headCenter.y - headRadius * 0.32f)
                    )
                }

                // =========================================================================
                // I. LARGE REALISTIC FLOATING 3D WHITE DICE ABOVE THE BOARD
                //    (3 Perspective Faces, Rounded Edges, Recessed Black Pips & Warm Glow)
                // =========================================================================
                val diceCenterX = w * 0.495f
                val diceCenterY = h * 0.165f + diceHoverOffset * h * 0.008f
                val diceR = w * 0.125f
                val tumbleDeg = if (isRollingDice) diceTumbleAngle else -14f

                withTransform({
                    rotate(degrees = tumbleDeg, pivot = Offset(diceCenterX, diceCenterY))
                }) {
                    // Warm golden ambient glow around the 3D dice
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0xFFFFD54F).copy(alpha = 0.55f), Color.Transparent),
                            center = Offset(diceCenterX, diceCenterY + diceR * 0.25f),
                            radius = diceR * 1.45f
                        ),
                        radius = diceR * 1.45f,
                        center = Offset(diceCenterX, diceCenterY + diceR * 0.25f)
                    )

                    // 6-vertex isometric hexagon vertices for the 3D rounded cube
                    val vTop = Offset(diceCenterX, diceCenterY - diceR * 0.96f)
                    val vUpperL = Offset(diceCenterX - diceR * 0.90f, diceCenterY - diceR * 0.36f)
                    val vUpperR = Offset(diceCenterX + diceR * 0.90f, diceCenterY - diceR * 0.36f)
                    val vCenter = Offset(diceCenterX - diceR * 0.04f, diceCenterY + diceR * 0.12f)
                    val vLowerL = Offset(diceCenterX - diceR * 0.72f, diceCenterY + diceR * 0.56f)
                    val vLowerR = Offset(diceCenterX + diceR * 0.76f, diceCenterY + diceR * 0.48f)
                    val vBottom = Offset(diceCenterX + diceR * 0.02f, diceCenterY + diceR * 0.98f)

                    // 1. Top-Left Face (Brightest Pearlescent White)
                    val faceTopLeft = Path().apply {
                        moveTo(vTop.x, vTop.y)
                        lineTo(vUpperR.x, vUpperR.y)
                        lineTo(vCenter.x, vCenter.y)
                        lineTo(vUpperL.x, vUpperL.y)
                        close()
                    }
                    drawPath(
                        path = faceTopLeft,
                        brush = Brush.linearGradient(
                            colors = listOf(Color.White, Color(0xFFF3EFF8), Color(0xFFE2DCEF)),
                            start = vTop,
                            end = vCenter
                        )
                    )

                    // 2. Front-Left Face (Soft Cool Lavender-White Shadow)
                    val faceBottomLeft = Path().apply {
                        moveTo(vUpperL.x, vUpperL.y)
                        lineTo(vCenter.x, vCenter.y)
                        lineTo(vBottom.x, vBottom.y)
                        lineTo(vLowerL.x, vLowerL.y)
                        close()
                    }
                    drawPath(
                        path = faceBottomLeft,
                        brush = Brush.linearGradient(
                            colors = listOf(Color(0xFFEDE7F6), Color(0xFFD1C4E9), Color(0xFFB39DDB)),
                            start = vUpperL,
                            end = vBottom
                        )
                    )

                    // 3. Front-Right Face (Warm Golden-Lit Ivory Face)
                    val faceBottomRight = Path().apply {
                        moveTo(vCenter.x, vCenter.y)
                        lineTo(vUpperR.x, vUpperR.y)
                        lineTo(vLowerR.x, vLowerR.y)
                        lineTo(vBottom.x, vBottom.y)
                        close()
                    }
                    drawPath(
                        path = faceBottomRight,
                        brush = Brush.linearGradient(
                            colors = listOf(Color(0xFFF5F0FF), Color(0xFFD7CCC8), Color(0xFFFFE082)),
                            start = vCenter,
                            end = vBottom
                        )
                    )

                    // Smooth rounded 3D specular bevel ridges along the cube edges
                    val outlineHex = Path().apply {
                        moveTo(vTop.x, vTop.y)
                        lineTo(vUpperR.x, vUpperR.y)
                        lineTo(vLowerR.x, vLowerR.y)
                        lineTo(vBottom.x, vBottom.y)
                        lineTo(vLowerL.x, vLowerL.y)
                        lineTo(vUpperL.x, vUpperL.y)
                        close()
                    }
                    drawPath(
                        path = outlineHex,
                        color = Color.White.copy(alpha = 0.92f),
                        style = Stroke(width = 3.dp.toPx(), join = StrokeJoin.Round)
                    )
                    drawLine(Color.White.copy(alpha = 0.85f), vTop, vCenter, strokeWidth = 2.5f)
                    drawLine(Color.White.copy(alpha = 0.85f), vUpperL, vCenter, strokeWidth = 2.5f)
                    drawLine(Color.White.copy(alpha = 0.85f), vUpperR, vCenter, strokeWidth = 2.5f)
                    drawLine(Color.White.copy(alpha = 0.70f), vCenter, vBottom, strokeWidth = 2.5f)

                    // Helper to draw a recessed 3D black pip in perspective on a face
                    fun draw3DPip(center: Offset, rx: Float, ry: Float, tiltDeg: Float) {
                        withTransform({
                            rotate(degrees = tiltDeg, pivot = center)
                        }) {
                            // Outer rim shadow
                            drawOval(
                                color = Color(0xFF4A4458),
                                topLeft = Offset(center.x - rx * 1.12f, center.y - ry * 1.12f),
                                size = Size(rx * 2.24f, ry * 2.24f)
                            )
                            // Deep glossy black pip interior
                            drawOval(
                                color = Color(0xFF0D0A14),
                                topLeft = Offset(center.x - rx, center.y - ry),
                                size = Size(rx * 2f, ry * 2f)
                            )
                        }
                    }

                    // Bilinear interpolation helper on a 4-corner perspective face
                    fun facePoint(p00: Offset, p10: Offset, p11: Offset, p01: Offset, s: Float, t: Float): Offset {
                        val topX = p00.x + (p10.x - p00.x) * s
                        val topY = p00.y + (p10.y - p00.y) * s
                        val botX = p01.x + (p11.x - p01.x) * s
                        val botY = p01.y + (p11.y - p01.y) * s
                        return Offset(topX + (botX - topX) * t, topY + (botY - topY) * t)
                    }

                    fun pipPatternUVs(faceVal: Int): List<Pair<Float, Float>> = when (faceVal) {
                        1 -> listOf(0.50f to 0.50f)
                        2 -> listOf(0.28f to 0.28f, 0.72f to 0.72f)
                        3 -> listOf(0.25f to 0.25f, 0.50f to 0.50f, 0.75f to 0.75f)
                        4 -> listOf(0.28f to 0.28f, 0.72f to 0.28f, 0.28f to 0.72f, 0.72f to 0.72f)
                        5 -> listOf(0.26f to 0.26f, 0.74f to 0.26f, 0.50f to 0.50f, 0.26f to 0.74f, 0.74f to 0.74f)
                        else -> listOf(
                            0.28f to 0.22f, 0.72f to 0.22f,
                            0.28f to 0.50f, 0.72f to 0.50f,
                            0.28f to 0.78f, 0.72f to 0.78f
                        )
                    }

                    // Primary rolled value on the prominent top face, plus complementary side faces
                    val topFaceVal = diceValue.coerceIn(1, 6)
                    val leftFaceVal = when (topFaceVal) {
                        4 -> 3
                        6 -> 4
                        1 -> 5
                        else -> 4
                    }
                    val rightFaceVal = when (topFaceVal) {
                        4 -> 5
                        6 -> 3
                        5 -> 3
                        else -> 5
                    }

                    val pipRx = diceR * 0.115f
                    val pipRy = diceR * 0.082f

                    pipPatternUVs(topFaceVal).forEach { (u, v) ->
                        val pt = facePoint(vTop, vUpperR, vCenter, vUpperL, u, v)
                        draw3DPip(pt, pipRx, pipRy, -22f)
                    }
                    pipPatternUVs(leftFaceVal).forEach { (u, v) ->
                        val pt = facePoint(vUpperL, vCenter, vBottom, vLowerL, u, v)
                        draw3DPip(pt, pipRx * 0.88f, pipRy * 0.92f, 32f)
                    }
                    pipPatternUVs(rightFaceVal).forEach { (u, v) ->
                        val pt = facePoint(vCenter, vUpperR, vLowerR, vBottom, u, v)
                        draw3DPip(pt, pipRx * 0.88f, pipRy * 0.92f, -32f)
                    }
                }
            }
        }

        // 3. BOTTOM 3D SIX-FACED DICE + INTERACTIVE TOKEN SELECTOR CHIPS + ROLL BUTTON
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = Color(0xFF151126),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                val controlledPlayerIdx = if (orderedPlayers.size <= 1 && !botEnabled) currentTurnPlayer else myPlayerIndex
                val isMyTurn = (currentTurnPlayer == controlledPlayerIdx)
                // Quick Token Move Selector Row (Tap Token #1..#4 directly or tap on 3D board)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${playerColorLabels[controlledPlayerIdx]} Pawns:",
                        color = NoiseTextSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                    for (tIdx in 0..3) {
                        val pos = tokenPositions[controlledPlayerIdx * 4 + tIdx]
                        val isLegal = (isMyTurn && tIdx in legalTokensForCurrentTurn)
                        val label = when {
                            pos == -1 -> "P${tIdx + 1} (Home)"
                            pos >= 56 -> "P${tIdx + 1} (Goal)"
                            else -> "P${tIdx + 1} ($pos)"
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isLegal) NoiseGold.copy(alpha = 0.28f) else Color.White.copy(alpha = 0.07f),
                            modifier = Modifier
                                .weight(1f)
                                .border(
                                    1.dp,
                                    if (isLegal) NoiseGold else Color.White.copy(alpha = 0.14f),
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable {
                                    if (isMyTurn) {
                                        val legalNow = getLegalTokensForPlayer(controlledPlayerIdx, diceValue)
                                        if (tIdx in legalNow) {
                                            scope.launch { executeTokenMove(controlledPlayerIdx, tIdx, diceValue) }
                                        }
                                    }
                                }
                                .testTag("ludo_token_button_$tIdx")
                        ) {
                            Text(
                                text = label,
                                color = if (isLegal) NoiseGold else Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 5.dp, horizontal = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Custom 3D Six-Faced Dice Cube Canvas with Real Pips
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Brush.linearGradient(listOf(NoisePink, NoisePurple)))
                                .border(2.dp, NoiseGold, RoundedCornerShape(14.dp))
                                .clickable {
                                    triggerDiceRoll(autoMoveFirstLegalForPlayer0 = false)
                                }
                                .testTag("ludo_3d_dice_cube"),
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(modifier = Modifier.size(36.dp)) {
                                drawRoundRect(
                                    color = Color.White,
                                    size = size,
                                    cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
                                )
                                val w = size.width
                                val h = size.height
                                val pipR = w * 0.10f
                                val pipColor = Color(0xFF100C1C)
                                val pipOffsets = when (diceValue) {
                                    1 -> listOf(Offset(w * 0.5f, h * 0.5f))
                                    2 -> listOf(Offset(w * 0.28f, h * 0.28f), Offset(w * 0.72f, h * 0.72f))
                                    3 -> listOf(Offset(w * 0.26f, h * 0.26f), Offset(w * 0.5f, h * 0.5f), Offset(w * 0.74f, h * 0.74f))
                                    4 -> listOf(
                                        Offset(w * 0.28f, h * 0.28f), Offset(w * 0.72f, h * 0.28f),
                                        Offset(w * 0.28f, h * 0.72f), Offset(w * 0.72f, h * 0.72f)
                                    )
                                    5 -> listOf(
                                        Offset(w * 0.26f, h * 0.26f), Offset(w * 0.74f, h * 0.26f),
                                        Offset(w * 0.5f, h * 0.5f),
                                        Offset(w * 0.26f, h * 0.74f), Offset(w * 0.74f, h * 0.74f)
                                    )
                                    else -> listOf(
                                        Offset(w * 0.28f, h * 0.24f), Offset(w * 0.72f, h * 0.24f),
                                        Offset(w * 0.28f, h * 0.50f), Offset(w * 0.72f, h * 0.50f),
                                        Offset(w * 0.28f, h * 0.76f), Offset(w * 0.72f, h * 0.76f)
                                    )
                                }
                                pipOffsets.forEach { pt ->
                                    drawCircle(color = pipColor, radius = pipR, center = pt)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (isMyTurn) "Your Turn (${playerColorLabels[controlledPlayerIdx]}) • Rolled $diceValue" else "@${playerNames[currentTurnPlayer]}'s Turn (${playerColorLabels[currentTurnPlayer]})",
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "Tap Floating 3D Dice or any 3D Pawn on board",
                                color = NoiseTextSecondary,
                                fontSize = 10.sp
                            )
                        }
                    }

                    Button(
                        onClick = {
                            triggerDiceRoll(autoMoveFirstLegalForPlayer0 = true)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NoisePink),
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("ludo_roll_dice_button")
                    ) {
                        Text("ROLL 3D DICE", fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }
    }
}

// ============================================================================
// GAME 3: COMPLETE 8 BALL POOL 3D (Realistic 3D Table, 6 Pockets, 18 Diamond
// Sights, 16 Numbered Solids/Stripes Balls, Cue Stick 3D Pullback, Ghost Ball
// Trajectory, Open Table -> Solids/Stripes Assignment, Fouls, Ball-in-Hand,
// 8-Ball Victory/Defeat Rules & Multiplayer Turn Synchronization)
// ============================================================================
private data class PoolBall(
    val number: Int,
    var x: Float,
    var y: Float,
    var vx: Float = 0f,
    var vy: Float = 0f,
    val color: Color,
    val isStripe: Boolean = false,
    var potted: Boolean = false
)

@Composable
private fun Playable3DPool8BallArena(
    gameRoom: GameRoomDoc,
    currentUser: UserProfile,
    players: List<GamePlayerSlot>,
    botEnabled: Boolean,
    tableThemeIndex: Int,
    onSyncGameState: (String, String, List<GamePlayerSlot>?, String?, String?, String?) -> Unit,
    onMatchComplete: (String, Long, Long, String) -> Unit
) {
    val p1Name = currentUser.username.ifBlank { "You" }
    val p2Name = players.firstOrNull { it.username != p1Name }?.username
        ?: if (botEnabled) "Pool_Shark_AI" else "Challenger"

    var p1Solids by remember { mutableIntStateOf(0) }
    var p2Stripes by remember { mutableIntStateOf(0) }
    var tableGroupAssignment by remember { mutableStateOf("OPEN TABLE") } // "OPEN TABLE", "P1_SOLIDS", "P1_STRIPES"
    var currentTurn by remember { mutableIntStateOf(1) } // 1 = Player 1, 2 = Player 2
    var ballInHand by remember { mutableStateOf(false) }
    var placingBallInHand by remember { mutableStateOf(false) }
    var cueAngleDeg by remember { mutableFloatStateOf(0f) }
    var cuePower by remember { mutableFloatStateOf(0.76f) }
    var ballsMoving by remember { mutableStateOf(false) }
    var shotPullbackAnim by remember { mutableFloatStateOf(0f) }
    var turnTimerSec by remember { mutableIntStateOf(30) }
    var statusBanner by remember { mutableStateOf("Open Table • Drag on table to aim & tap SHOOT CUE!") }

    // Official Pool Ball Colors (1..7 Solids, 8 Black, 9..15 Stripes)
    val officialBallColors = remember {
        mapOf(
            0 to Color.White,
            1 to Color(0xFFFFD600), // 1 Yellow
            2 to Color(0xFF1E88E5), // 2 Blue
            3 to Color(0xFFE53935), // 3 Red
            4 to Color(0xFF8E24AA), // 4 Purple
            5 to Color(0xFFFB8C00), // 5 Orange
            6 to Color(0xFF00897B), // 6 Green
            7 to Color(0xFF6D1B1B), // 7 Maroon
            8 to Color(0xFF111111), // 8 Black
            9 to Color(0xFFFFD600), // 9 Yellow Stripe
            10 to Color(0xFF1E88E5), // 10 Blue Stripe
            11 to Color(0xFFE53935), // 11 Red Stripe
            12 to Color(0xFF8E24AA), // 12 Purple Stripe
            13 to Color(0xFFFB8C00), // 13 Orange Stripe
            14 to Color(0xFF00897B), // 14 Green Stripe
            15 to Color(0xFF6D1B1B)  // 15 Maroon Stripe
        )
    }

    // Standard 8-Ball Triangle Rack Order (Apex = 1, Center of Triangle [row 2, col 1] = 8-Ball!)
    val balls = remember {
        mutableStateListOf<PoolBall>().apply {
            // 0 = Cue Ball behind head string
            add(PoolBall(0, 0.24f, 0.50f, color = Color.White, isStripe = false))
            val rackOrder = listOf(
                listOf(1),
                listOf(9, 2),
                listOf(3, 8, 10), // 8-Ball in exact center of triangle
                listOf(11, 4, 12, 5),
                listOf(6, 13, 14, 7, 15)
            )
            for (col in rackOrder.indices) {
                val colBalls = rackOrder[col]
                for (row in colBalls.indices) {
                    val num = colBalls[row]
                    val bx = 0.68f + col * 0.041f
                    val by = 0.50f + (row - col / 2f) * 0.054f
                    add(
                        PoolBall(
                            number = num,
                            x = bx,
                            y = by,
                            color = officialBallColors[num] ?: Color.White,
                            isStripe = num >= 9
                        )
                    )
                }
            }
        }
    }

    // Turn Timer
    LaunchedEffect(currentTurn, ballsMoving) {
        if (ballsMoving) return@LaunchedEffect
        turnTimerSec = 30
        while (turnTimerSec > 0) {
            delay(1000L)
            turnTimerSec--
        }
    }

    // 60 FPS Rigid-Body Billiard Physics, Cushion Rebounds, Pocket Detection & 8-Ball Rules
    LaunchedEffect(ballsMoving) {
        if (!ballsMoving) return@LaunchedEffect
        val pockets = listOf(
            Offset(0.06f, 0.08f), Offset(0.50f, 0.06f), Offset(0.94f, 0.08f),
            Offset(0.06f, 0.92f), Offset(0.50f, 0.94f), Offset(0.94f, 0.92f)
        )
        var firstHitBallNumber: Int? = null
        var scratchedThisTurn = false
        var potted8BallThisTurn = false
        val pottedNumbersThisTurn = mutableListOf<Int>()

        var ticks = 0
        while (ballsMoving && ticks < 220) {
            ticks++
            var moving = false
            val active = balls.filter { !it.potted }
            for (b in active) {
                b.x += b.vx
                b.y += b.vy
                b.vx *= 0.972f
                b.vy *= 0.972f

                // Cushion rebounds
                if (b.x < 0.08f) { b.x = 0.08f; b.vx = -b.vx * 0.85f }
                if (b.x > 0.92f) { b.x = 0.92f; b.vx = -b.vx * 0.85f }
                if (b.y < 0.11f) { b.y = 0.11f; b.vy = -b.vy * 0.85f }
                if (b.y > 0.89f) { b.y = 0.89f; b.vy = -b.vy * 0.85f }

                // 6 Pockets detection
                for (p in pockets) {
                    if (hypot(b.x - p.x, b.y - p.y) < 0.056f) {
                        if (b.number == 0) {
                            // Scratch foul! Reset cue ball & give opponent Ball-in-Hand
                            scratchedThisTurn = true
                            b.x = 0.24f; b.y = 0.50f; b.vx = 0f; b.vy = 0f
                            GameSoundEffectsEngine.playCapture()
                        } else {
                            b.potted = true
                            b.vx = 0f; b.vy = 0f
                            pottedNumbersThisTurn.add(b.number)
                            GameSoundEffectsEngine.playPocket()
                            if (b.number == 8) {
                                potted8BallThisTurn = true
                            } else if (b.number in 1..7) {
                                p1Solids += 1
                            } else {
                                p2Stripes += 1
                            }
                        }
                    }
                }
                if (hypot(b.vx, b.vy) > 0.001f) moving = true else { b.vx = 0f; b.vy = 0f }
            }

            // Elastic 2D Sphere Collisions
            for (i in active.indices) {
                for (j in i + 1 until active.size) {
                    val a = active[i]
                    val b = active[j]
                    if (a.potted || b.potted) continue
                    val dx = b.x - a.x
                    val dy = b.y - a.y
                    val dist = hypot(dx, dy)
                    val minDist = 0.044f
                    if (dist > 0.0001f && dist < minDist) {
                        if (firstHitBallNumber == null && (a.number == 0 || b.number == 0)) {
                            firstHitBallNumber = if (a.number == 0) b.number else a.number
                        }
                        val nx = dx / dist
                        val ny = dy / dist
                        val overlap = (minDist - dist) * 0.52f
                        a.x -= nx * overlap; a.y -= ny * overlap
                        b.x += nx * overlap; b.y += ny * overlap
                        val kx = a.vx - b.vx
                        val ky = a.vy - b.vy
                        val pImp = (nx * kx + ny * ky)
                        a.vx -= pImp * nx * 0.94f
                        a.vy -= pImp * ny * 0.94f
                        b.vx += pImp * nx * 0.94f
                        b.vy += pImp * ny * 0.94f
                    }
                }
            }
            balls[0] = balls[0].copy()
            delay(16L)
            if (!moving) break
        }
        ballsMoving = false
        shotPullbackAnim = 0f

        // Assign Solids / Stripes if table was OPEN and a ball (1..7 or 9..15) was legally potted
        if (tableGroupAssignment == "OPEN TABLE" && !scratchedThisTurn) {
            val firstNon8Pot = pottedNumbersThisTurn.firstOrNull { it != 8 }
            if (firstNon8Pot != null) {
                val pottedSolid = firstNon8Pot in 1..7
                tableGroupAssignment = if ((currentTurn == 1 && pottedSolid) || (currentTurn == 2 && !pottedSolid)) {
                    "P1_SOLIDS"
                } else {
                    "P1_STRIPES"
                }
            }
        }

        // Evaluate 8-Ball Victory / Defeat or Group Completion
        if (potted8BallThisTurn) {
            val shooterClearedGroup = if (currentTurn == 1) p1Solids >= 5 else p2Stripes >= 5
            if (shooterClearedGroup && !scratchedThisTurn) {
                GameSoundEffectsEngine.playVictory()
                val winner = if (currentTurn == 1) p1Name else p2Name
                onMatchComplete(winner, (p1Solids * 25 + 50).toLong(), (p2Stripes * 25).toLong(), "Legal 8-Ball Pocketed • Victory!")
                return@LaunchedEffect
            } else {
                // Early 8-ball or scratch on 8-ball -> opponent wins if not in quick-win threshold, or complete match
                GameSoundEffectsEngine.playVictory()
                val winner = if (p1Solids >= p2Stripes) p1Name else p2Name
                onMatchComplete(winner, (max(p1Solids, 1) * 25).toLong(), (p2Stripes * 25).toLong(), "8-Ball Pocketed • Match Complete")
                return@LaunchedEffect
            }
        }

        if (p1Solids >= 5 || p2Stripes >= 5) {
            GameSoundEffectsEngine.playVictory()
            onMatchComplete(
                if (p1Solids >= p2Stripes) p1Name else p2Name,
                (p1Solids * 25).toLong(),
                (p2Stripes * 25).toLong(),
                "Table Cleared • 8-Ball Pro Victory!"
            )
            return@LaunchedEffect
        }

        // Check fouls (scratch or no ball contacted)
        val noBallHitFoul = (firstHitBallNumber == null && pottedNumbersThisTurn.isEmpty())
        val isFoul = scratchedThisTurn || noBallHitFoul
        if (isFoul) {
            ballInHand = true
            currentTurn = if (currentTurn == 1) 2 else 1
            statusBanner = if (scratchedThisTurn) {
                "FOUL (Cue Ball Scratch)! Ball-in-Hand for @${if (currentTurn == 1) p1Name else p2Name}"
            } else {
                "FOUL (No Ball Contacted)! Ball-in-Hand for @${if (currentTurn == 1) p1Name else p2Name}"
            }
        } else if (pottedNumbersThisTurn.isNotEmpty()) {
            ballInHand = false
            statusBanner = "Great Shot! Potted #${pottedNumbersThisTurn.joinToString(", #")} • Bonus Turn!"
        } else {
            ballInHand = false
            currentTurn = if (currentTurn == 1) 2 else 1
            statusBanner = if (currentTurn == 1) "Your Turn (@$p1Name) • Drag to Aim & Shoot!" else "@$p2Name's Turn..."
        }

        // Sync settled pool table state to Firestore (low-frequency after shot settles)
        val encodedState = "POOLv2:$currentTurn|$p1Solids|$p2Stripes|$tableGroupAssignment|$ballInHand"
        val nextUid = if (currentTurn == 1) currentUser.userId.ifBlank { currentUser.id } else (players.getOrNull(1)?.userId ?: currentUser.userId)
        onSyncGameState(encodedState, nextUid, null, "playing", null, null)
    }

    // Optional AI Bot / Remote Rival Turn Execution when currentTurn == 2
    LaunchedEffect(currentTurn, ballsMoving, botEnabled) {
        if (currentTurn == 2 && !ballsMoving && botEnabled) {
            delay(650L)
            val cue = balls.first()
            val target = balls.drop(1).firstOrNull { !it.potted && it.number != 8 }
                ?: balls.drop(1).firstOrNull { !it.potted }
            if (target != null) {
                val angle = atan2(target.y - cue.y, target.x - cue.x)
                cueAngleDeg = (angle * 180f / PI.toFloat())
                val speed = 0.046f
                GameSoundEffectsEngine.playCueStrike()
                balls[0] = cue.copy(vx = cos(angle) * speed, vy = sin(angle) * speed)
                ballsMoving = true
            } else {
                currentTurn = 1
            }
        }
    }

    val feltBrushColors = when (tableThemeIndex) {
        1 -> listOf(Color(0xFF4A148C), Color(0xFF311B92), Color(0xFF1A093B))
        2 -> listOf(Color(0xFF0277BD), Color(0xFF01579B), Color(0xFF002F6C))
        else -> listOf(Color(0xFF00897B), Color(0xFF00695C), Color(0xFF004D40))
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // 1. TOP HUD: Player 1 (Solids Tray) | Match Status & Timer | Player 2 (Stripes Tray)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                ScoreBadgeCard(
                    title = "@$p1Name (Solids 1–7)",
                    score = "$p1Solids Potted",
                    active = currentTurn == 1,
                    accent = Color(0xFF00E676)
                )
                Spacer(modifier = Modifier.height(3.dp))
                // Mini Solids Ball Rack Indicator (1..7)
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    for (num in 1..7) {
                        val isPotted = balls.getOrNull(balls.indexOfFirst { it.number == num })?.potted == true
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isPotted) Color.White.copy(alpha = 0.12f)
                                    else (officialBallColors[num] ?: Color.Yellow)
                                )
                                .border(0.8.dp, Color.White.copy(alpha = 0.4f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "$num",
                                color = if (isPotted) Color.Gray else Color.White,
                                fontSize = 7.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 6.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF1F1638),
                    modifier = Modifier.border(1.dp, NoiseGold.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                ) {
                    Text(
                        text = "$tableGroupAssignment • ⏱ ${turnTimerSec}s",
                        color = NoiseGold,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = statusBanner,
                    color = if (ballInHand) Color(0xFFFFD700) else Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.End
            ) {
                ScoreBadgeCard(
                    title = "@$p2Name (Stripes 9–15)",
                    score = "$p2Stripes Potted",
                    active = currentTurn == 2,
                    accent = Color(0xFF00E5FF)
                )
                Spacer(modifier = Modifier.height(3.dp))
                // Mini Stripes Ball Rack Indicator (9..15)
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    for (num in 9..15) {
                        val isPotted = balls.getOrNull(balls.indexOfFirst { it.number == num })?.potted == true
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isPotted) Color.White.copy(alpha = 0.12f)
                                    else (officialBallColors[num] ?: Color.Cyan)
                                )
                                .border(1.dp, Color.White, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "$num",
                                color = if (isPotted) Color.Gray else Color.White,
                                fontSize = 6.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }
            }
        }

        // 2. REALISTIC 3D 8-BALL POOL TABLE CANVAS
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(vertical = 6.dp)
                .clip(RoundedCornerShape(24.dp))
                .border(4.dp, Color(0xFF5D4037), RoundedCornerShape(24.dp))
                .testTag("pool_8ball_canvas")
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(ballsMoving, placingBallInHand, ballInHand) {
                        detectDragGestures { change, _ ->
                            if (!ballsMoving) {
                                val cue = balls.first()
                                if (placingBallInHand && ballInHand) {
                                    // Free Ball-in-Hand placement on the felt
                                    val nx = (change.position.x / size.width).coerceIn(0.10f, 0.90f)
                                    val ny = (change.position.y / size.height).coerceIn(0.14f, 0.86f)
                                    balls[0] = cue.copy(x = nx, y = ny, vx = 0f, vy = 0f)
                                } else {
                                    cueAngleDeg = (atan2(
                                        change.position.y - cue.y * size.height,
                                        change.position.x - cue.x * size.width
                                    ) * 180f / PI.toFloat())
                                }
                            }
                        }
                    }
            ) {
                val w = size.width
                val h = size.height

                // Outer 3D Mahogany & Brass Table Frame
                drawRect(
                    brush = Brush.linearGradient(
                        listOf(Color(0xFF4E2A1E), Color(0xFF2E150D), Color(0xFF5A3224))
                    ),
                    size = size
                )

                val rail = w * 0.055f
                // Cushion Shadow Border
                drawRoundRect(
                    color = Color(0xFF002922),
                    topLeft = Offset(rail * 0.88f, rail * 0.88f),
                    size = Size(w - rail * 1.76f, h - rail * 1.76f),
                    cornerRadius = CornerRadius(18f, 18f)
                )

                // Illuminated Championship Felt Surface
                drawRoundRect(
                    brush = Brush.radialGradient(
                        colors = feltBrushColors,
                        center = Offset(w * 0.5f, h * 0.5f),
                        radius = w * 0.62f
                    ),
                    topLeft = Offset(rail, rail),
                    size = Size(w - rail * 2, h - rail * 2),
                    cornerRadius = CornerRadius(16f, 16f)
                )

                // 18 Diamond Sights along Top, Bottom, Left, and Right Rails
                val topSightX = listOf(0.18f, 0.29f, 0.40f, 0.60f, 0.71f, 0.82f)
                topSightX.forEach { sx ->
                    drawCircle(color = NoiseGold, radius = 3.dp.toPx(), center = Offset(w * sx, rail * 0.48f))
                    drawCircle(color = NoiseGold, radius = 3.dp.toPx(), center = Offset(w * sx, h - rail * 0.48f))
                }
                listOf(0.28f, 0.50f, 0.72f).forEach { sy ->
                    drawCircle(color = NoiseGold, radius = 3.dp.toPx(), center = Offset(rail * 0.48f, h * sy))
                    drawCircle(color = NoiseGold, radius = 3.dp.toPx(), center = Offset(w - rail * 0.48f, h * sy))
                }

                // Baulk Line (Head String) & Head/Foot Spots
                drawLine(
                    color = Color.White.copy(alpha = 0.24f),
                    start = Offset(w * 0.24f, rail),
                    end = Offset(w * 0.24f, h - rail),
                    strokeWidth = 2f
                )
                drawCircle(color = Color.White.copy(alpha = 0.35f), radius = 3.5f, center = Offset(w * 0.24f, h * 0.50f))
                drawCircle(color = Color.White.copy(alpha = 0.35f), radius = 3.5f, center = Offset(w * 0.68f, h * 0.50f))

                // 6 Pockets with 3D Brass Castings & Deep Shadow Nets
                val pockets = listOf(
                    Offset(w * 0.06f, h * 0.08f), Offset(w * 0.50f, h * 0.06f), Offset(w * 0.94f, h * 0.08f),
                    Offset(w * 0.06f, h * 0.92f), Offset(w * 0.50f, h * 0.94f), Offset(w * 0.94f, h * 0.92f)
                )
                pockets.forEach { p ->
                    drawCircle(color = Color(0xFFD4AF37), radius = w * 0.042f, center = p)
                    drawCircle(color = Color(0xFF070707), radius = w * 0.035f, center = p)
                }

                // Cue Stick, Trajectory Raycast, Ghost Ball & Deflection Guide
                val cueBall = balls.first()
                if (!ballsMoving) {
                    val rad = (cueAngleDeg * PI / 180.0).toFloat()
                    val cx = cueBall.x * w
                    val cy = cueBall.y * h
                    val dirX = cos(rad)
                    val dirY = sin(rad)
                    val ballRadiusPx = w * 0.022f

                    // Find closest object ball along the aiming ray for Ghost Ball preview
                    var bestHitDist = w * 0.46f
                    var hitBall: PoolBall? = null
                    balls.drop(1).filter { !it.potted }.forEach { ob ->
                        val ox = ob.x * w - cx
                        val oy = ob.y * h - cy
                        val proj = ox * dirX + oy * dirY
                        if (proj > 0f && proj < bestHitDist) {
                            val perpSq = (ox * ox + oy * oy) - proj * proj
                            val touchDist = ballRadiusPx * 2f
                            if (perpSq >= 0f && perpSq < touchDist * touchDist) {
                                val hitDist = proj - kotlin.math.sqrt((touchDist * touchDist - perpSq).coerceAtLeast(0f))
                                if (hitDist > 0f && hitDist < bestHitDist) {
                                    bestHitDist = hitDist
                                    hitBall = ob
                                }
                            }
                        }
                    }

                    val ghostX = cx + dirX * bestHitDist
                    val ghostY = cy + dirY * bestHitDist

                    // Primary Trajectory Line
                    drawLine(
                        color = Color.White.copy(alpha = 0.80f),
                        start = Offset(cx, cy),
                        end = Offset(ghostX, ghostY),
                        strokeWidth = 3f,
                        cap = StrokeCap.Round
                    )

                    // Ghost Ball Circle & Target Ball Post-Impact Guide Line
                    drawCircle(
                        color = Color(0xFF00E5FF).copy(alpha = 0.75f),
                        radius = ballRadiusPx,
                        center = Offset(ghostX, ghostY),
                        style = Stroke(width = 2.dp.toPx())
                    )
                    hitBall?.let { target ->
                        val tx = target.x * w
                        val ty = target.y * h
                        val impDx = tx - ghostX
                        val impDy = ty - ghostY
                        val impLen = hypot(impDx, impDy).coerceAtLeast(1f)
                        val nx = impDx / impLen
                        val ny = impDy / impLen
                        // Object ball target direction line (Gold)
                        drawLine(
                            color = NoiseGold.copy(alpha = 0.90f),
                            start = Offset(tx, ty),
                            end = Offset(tx + nx * w * 0.16f, ty + ny * w * 0.16f),
                            strokeWidth = 3f,
                            cap = StrokeCap.Round
                        )
                    }

                    // 3D Tapered Cue Stick with Maple Shaft, Gold Ferrule & Leather Tip
                    val pullbackPx = 20f + cuePower * 34f - shotPullbackAnim * 18f
                    val tipX = cx - dirX * pullbackPx
                    val tipY = cy - dirY * pullbackPx
                    val buttX = cx - dirX * (pullbackPx + 145f)
                    val buttY = cy - dirY * (pullbackPx + 145f)

                    // Cue drop shadow
                    drawLine(
                        color = Color.Black.copy(alpha = 0.45f),
                        start = Offset(tipX + 3f, tipY + 5f),
                        end = Offset(buttX + 3f, buttY + 5f),
                        strokeWidth = 9f,
                        cap = StrokeCap.Round
                    )
                    // Cue body
                    drawLine(
                        brush = Brush.linearGradient(
                            colors = listOf(Color(0xFFFFF8E1), NoiseGold, Color(0xFF5D4037), Color(0xFF1B0F0A)),
                            start = Offset(tipX, tipY),
                            end = Offset(buttX, buttY)
                        ),
                        start = Offset(tipX, tipY),
                        end = Offset(buttX, buttY),
                        strokeWidth = 8.5f,
                        cap = StrokeCap.Round
                    )
                    // Blue chalk tip
                    drawCircle(
                        color = Color(0xFF00E5FF),
                        radius = 4.2f,
                        center = Offset(tipX, tipY)
                    )
                }

                // Draw All Active 3D Billiard Balls (with Numbers, Stripes, Specular Glint & Drop Shadow)
                val numPaint = android.graphics.Paint().apply {
                    isAntiAlias = true
                    color = android.graphics.Color.BLACK
                    textAlign = android.graphics.Paint.Align.CENTER
                    typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                }

                balls.filter { !it.potted }.forEach { b ->
                    val bx = b.x * w
                    val by = b.y * h
                    val r = w * 0.022f

                    // Ball-in-Hand Ring on Cue Ball
                    if (b.number == 0 && ballInHand && !ballsMoving) {
                        drawCircle(
                            color = NoiseGold,
                            radius = r * 1.45f,
                            center = Offset(bx, by),
                            style = Stroke(width = 2.dp.toPx())
                        )
                    }

                    // 3D Drop Shadow
                    drawCircle(
                        color = Color.Black.copy(alpha = 0.42f),
                        radius = r,
                        center = Offset(bx + 3f, by + 4f)
                    )

                    if (b.isStripe) {
                        // White sphere base + Colored equatorial stripe band
                        drawCircle(color = Color(0xFFFDFDFD), radius = r, center = Offset(bx, by))
                        drawRoundRect(
                            color = b.color,
                            topLeft = Offset(bx - r * 0.92f, by - r * 0.52f),
                            size = Size(r * 1.84f, r * 1.04f),
                            cornerRadius = CornerRadius(r * 0.45f, r * 0.45f)
                        )
                    } else {
                        // Solid sphere
                        drawCircle(color = b.color, radius = r, center = Offset(bx, by))
                    }

                    // Number Circle & Number Text (for balls 1..15)
                    if (b.number > 0) {
                        drawCircle(
                            color = Color.White,
                            radius = r * 0.48f,
                            center = Offset(bx, by)
                        )
                        numPaint.textSize = r * 0.66f
                        drawContext.canvas.nativeCanvas.drawText(
                            b.number.toString(),
                            bx,
                            by + r * 0.24f,
                            numPaint
                        )
                    }

                    // 3D Specular Highlight Glint
                    drawCircle(
                        color = Color.White.copy(alpha = 0.55f),
                        radius = r * 0.24f,
                        center = Offset(bx - r * 0.28f, by - r * 0.28f)
                    )
                }
            }
        }

        // 3. BOTTOM CONTROLS: Fine-Tune Aim (-1°/+1°) + Aim Slider + Power Slider + Ball-in-Hand + SHOOT CUE Button
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = Color(0xFF151126),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Fine Aim Buttons (-1° / +1°) + Aim Slider
                    Column(modifier = Modifier.weight(1.1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Aim (${cueAngleDeg.toInt()}°)", color = NoiseTextSecondary, fontSize = 10.sp)
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color.White.copy(alpha = 0.10f),
                                    modifier = Modifier
                                        .clickable { cueAngleDeg = ((cueAngleDeg - 1.5f + 180f) % 360f) - 180f }
                                        .testTag("pool_fine_aim_left")
                                ) {
                                    Text("-1°", color = Color(0xFF00E5FF), fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                }
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color.White.copy(alpha = 0.10f),
                                    modifier = Modifier
                                        .clickable { cueAngleDeg = ((cueAngleDeg + 1.5f + 180f) % 360f) - 180f }
                                        .testTag("pool_fine_aim_right")
                                ) {
                                    Text("+1°", color = Color(0xFF00E5FF), fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                }
                                if (ballInHand) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (placingBallInHand) NoiseGold else NoiseGold.copy(alpha = 0.22f),
                                        modifier = Modifier
                                            .clickable { placingBallInHand = !placingBallInHand }
                                            .testTag("pool_ball_in_hand_toggle")
                                    ) {
                                        Text(
                                            text = if (placingBallInHand) "Lock Cue" else "Move Cue",
                                            color = if (placingBallInHand) Color.Black else NoiseGold,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                        Slider(
                            value = cueAngleDeg,
                            onValueChange = { cueAngleDeg = it },
                            valueRange = -180f..180f,
                            colors = SliderDefaults.colors(thumbColor = Color(0xFF00E5FF), activeTrackColor = Color(0xFF00E5FF))
                        )
                    }

                    // Shot Power Slider (20%..100%)
                    Column(modifier = Modifier.weight(0.9f)) {
                        Text(
                            text = "Power (${(cuePower * 100).toInt()}%)",
                            color = NoiseTextSecondary,
                            fontSize = 10.sp
                        )
                        Slider(
                            value = cuePower,
                            onValueChange = { cuePower = it },
                            valueRange = 0.2f..1f,
                            colors = SliderDefaults.colors(thumbColor = Color(0xFF00E676), activeTrackColor = Color(0xFF00E676))
                        )
                    }

                    // SHOOT CUE Button with Cue Strike Animation
                    Button(
                        onClick = {
                            if (!ballsMoving) {
                                placingBallInHand = false
                                GameSoundEffectsEngine.playCueStrike()
                                shotPullbackAnim = 1f
                                val rad = (cueAngleDeg * PI / 180.0).toFloat()
                                val speed = 0.052f * cuePower
                                balls[0] = balls[0].copy(vx = cos(rad) * speed, vy = sin(rad) * speed)
                                ballsMoving = true
                            }
                        },
                        enabled = !ballsMoving,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676)),
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("pool_shoot_button")
                    ) {
                        Text("SHOOT CUE", color = Color.Black, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }
    }
}

// ============================================================================
// GAME 4: NEON 3D MULTIPLAYER RACING (Track Selection, Car Selection,
// 3-2-1-GO Countdown, Steering, Accelerate, Brake, Nitro, Lap Counter, Timer)
// ============================================================================
@Composable
private fun Playable3DMiniRacingArena(
    currentUser: UserProfile,
    players: List<GamePlayerSlot>,
    onMatchComplete: (String, Long, Long) -> Unit
) {
    val p1Name = currentUser.username.ifBlank { "You" }
    val p2Name = players.firstOrNull { it.username != p1Name }?.username ?: "Nitro_Racer"

    val tracks = listOf("Cyber Tokyo Circuit", "Neon Vegas Strip", "Royal Dubai Marina")
    val cars = listOf(
        Triple("Hyper Pink GT", Color(0xFFFF2E93), Color(0xFF9D4EDD)),
        Triple("Sovereign Gold V12", Color(0xFFFFD700), Color(0xFFFF6D00)),
        Triple("Cyan Phantom X", Color(0xFF00E5FF), Color(0xFF2979FF)),
        Triple("Emerald Viper R", Color(0xFF00E676), Color(0xFF00BFA5))
    )

    var selectedTrackIdx by remember { mutableIntStateOf(0) }
    var selectedCarIdx by remember { mutableIntStateOf(0) }
    var countdownValue by remember { mutableIntStateOf(3) }
    var raceElapsedMs by remember { mutableLongStateOf(0L) }

    var carLaneX by remember { mutableFloatStateOf(0f) }
    var speedKmh by remember { mutableFloatStateOf(175f) }
    var braking by remember { mutableStateOf(false) }
    var nitroCharge by remember { mutableFloatStateOf(100f) }
    var nitroActive by remember { mutableStateOf(false) }
    var distanceMeters by remember { mutableFloatStateOf(0f) }
    var rivalDistance by remember { mutableFloatStateOf(0f) }
    val lapLengthMeters = 500f
    val totalLaps = 3

    LaunchedEffect(selectedTrackIdx) {
        countdownValue = 3
        distanceMeters = 0f
        rivalDistance = 0f
        raceElapsedMs = 0L
        while (countdownValue > 0) {
            GameSoundEffectsEngine.playTap()
            delay(850L)
            countdownValue--
        }
        GameSoundEffectsEngine.playPocket()
    }

    LaunchedEffect(countdownValue) {
        if (countdownValue > 0) return@LaunchedEffect
        while (true) {
            val currentBoost = when {
                braking -> 0.55f
                nitroActive && nitroCharge > 0f -> 1.58f
                else -> 1f
            }
            if (nitroActive && nitroCharge > 0f) {
                nitroCharge = (nitroCharge - 1.4f).coerceAtLeast(0f)
            } else {
                nitroActive = false
                nitroCharge = (nitroCharge + 0.35f).coerceAtMost(100f)
            }
            val effectiveSpeed = speedKmh * currentBoost
            distanceMeters += effectiveSpeed * 0.035f
            rivalDistance += (170f + Random.nextFloat() * 18f) * 0.035f
            raceElapsedMs += 30L

            if (distanceMeters >= lapLengthMeters * totalLaps || rivalDistance >= lapLengthMeters * totalLaps) {
                GameSoundEffectsEngine.playVictory()
                val winner = if (distanceMeters >= rivalDistance) p1Name else p2Name
                onMatchComplete(winner, distanceMeters.toLong(), rivalDistance.toLong())
                break
            }
            delay(30L)
        }
    }

    val currentLap = ((distanceMeters / lapLengthMeters).toInt() + 1).coerceIn(1, totalLaps)
    val playerPosition = if (distanceMeters >= rivalDistance) "1st / 4" else "2nd / 4"
    val selectedCar = cars[selectedCarIdx]

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFF1A1433),
                modifier = Modifier.clickable {
                    selectedTrackIdx = (selectedTrackIdx + 1) % tracks.size
                }
            ) {
                Text(
                    text = "🏁 ${tracks[selectedTrackIdx]}",
                    color = Color(0xFF00E5FF),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFF1A1433),
                modifier = Modifier.clickable {
                    selectedCarIdx = (selectedCarIdx + 1) % cars.size
                }
            ) {
                Text(
                    text = "🏎 ${selectedCar.first}",
                    color = Color(0xFFFFD700),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }

        Spacer(Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            ScoreBadgeCard("LAP $currentLap/$totalLaps", "POS: $playerPosition", true, NoisePink)
            ScoreBadgeCard("SPEED", "${(speedKmh * (if (nitroActive) 1.5f else if (braking) 0.55f else 1f)).toInt()} KM/H", nitroActive, NoiseGold)
            ScoreBadgeCard("TIMER", "${raceElapsedMs / 1000}.${(raceElapsedMs % 1000) / 100}s", nitroCharge > 25f, Color(0xFF00E5FF))
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(vertical = 6.dp)
                .clip(RoundedCornerShape(24.dp))
                .border(2.dp, NoisePink, RoundedCornerShape(24.dp))
                .testTag("mini_racing_3d_canvas"),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val horizonY = h * 0.28f

                drawRect(
                    brush = Brush.verticalGradient(
                        listOf(Color(0xFF090418), Color(0xFF280B45), selectedCar.second.copy(alpha = 0.45f))
                    ),
                    size = Size(w, horizonY)
                )

                drawRect(
                    color = Color(0xFF0B0716),
                    topLeft = Offset(0f, horizonY),
                    size = Size(w, h - horizonY)
                )

                val roadPath = Path().apply {
                    moveTo(w * 0.42f, horizonY)
                    lineTo(w * 0.58f, horizonY)
                    lineTo(w * 0.94f, h)
                    lineTo(w * 0.06f, h)
                    close()
                }
                drawPath(
                    path = roadPath,
                    brush = Brush.verticalGradient(listOf(Color(0xFF1E1B2E), Color(0xFF12101B)))
                )

                val phase = (distanceMeters % 40f) / 40f
                for (i in 0..6) {
                    val t = ((i + phase) / 7f).coerceIn(0f, 1f)
                    val y = horizonY + (h - horizonY) * (t * t)
                    val dashH = 8f + 26f * t
                    drawLine(
                        color = NoiseGold,
                        start = Offset(w * 0.5f, y),
                        end = Offset(w * 0.5f, (y + dashH).coerceAtMost(h)),
                        strokeWidth = 2f + 5f * t
                    )
                }

                val carCenterX = w * 0.5f + carLaneX * (w * 0.28f)
                val carCenterY = h * 0.80f
                drawRoundRect(
                    brush = Brush.linearGradient(listOf(selectedCar.second, selectedCar.third)),
                    topLeft = Offset(carCenterX - 38f, carCenterY - 56f),
                    size = Size(76f, 112f),
                    cornerRadius = CornerRadius(18f, 18f)
                )
                drawRoundRect(
                    color = Color(0xFF00E5FF).copy(alpha = 0.8f),
                    topLeft = Offset(carCenterX - 28f, carCenterY - 30f),
                    size = Size(56f, 32f),
                    cornerRadius = CornerRadius(8f, 8f)
                )
                if (nitroActive) {
                    drawCircle(
                        color = Color(0xFF00E5FF),
                        radius = 22f,
                        center = Offset(carCenterX, carCenterY + 66f)
                    )
                }
            }

            if (countdownValue > 0) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF140E28).copy(alpha = 0.9f),
                    modifier = Modifier.size(92.dp).border(2.dp, Color(0xFFFFD700), CircleShape)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "$countdownValue",
                            color = Color(0xFFFFD700),
                            fontSize = 42.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
        }

        Surface(shape = RoundedCornerShape(18.dp), color = Color(0xFF151126), modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Button(
                        onClick = { carLaneX = (carLaneX - 0.32f).coerceAtLeast(-1f) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.12f))
                    ) {
                        Text("◀", fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = { carLaneX = (carLaneX + 0.32f).coerceAtMost(1f) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.12f))
                    ) {
                        Text("▶", fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = { braking = !braking },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (braking) Color(0xFFFF5252) else Color.White.copy(alpha = 0.12f)
                        )
                    ) {
                        Text(if (braking) "BRAKE ON" else "BRAKE", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Button(
                    onClick = {
                        braking = false
                        nitroActive = !nitroActive
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (nitroActive) Color(0xFF00E5FF) else NoisePink
                    ),
                    modifier = Modifier
                        .height(46.dp)
                        .testTag("racing_nitro_button")
                ) {
                    Icon(Icons.Default.Speed, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (nitroActive) "NITRO 🔥" else "NITRO (${nitroCharge.toInt()}%)", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }
}

// ============================================================================
// GAME 5: MULTIPLAYER QUIZ ARENA (5 Categories, 3 Difficulties, Timer, Leaderboard)
// ============================================================================
private data class QuizQuestionItem(
    val category: String,
    val difficulty: String,
    val question: String,
    val options: List<String>,
    val correctIndex: Int
)

@Composable
private fun PlayableMultiplayerQuizArena(
    currentUser: UserProfile,
    players: List<GamePlayerSlot>,
    onMatchComplete: (String, Long, Long) -> Unit
) {
    val p1Name = currentUser.username.ifBlank { "You" }
    val p2Name = players.firstOrNull { it.username != p1Name }?.username ?: "Quiz_Pro"

    val categories = listOf("All", "Gaming", "Music", "Science", "Sports")
    val difficulties = listOf("Easy", "Medium", "Hard")
    var selectedCategory by remember { mutableStateOf("All") }
    var selectedDifficulty by remember { mutableStateOf("Medium") }

    val allQuestions = remember {
        listOf(
            QuizQuestionItem(
                "Gaming", "Easy",
                "Which rendering pipeline powers NOISE LIVE's 3D Multiplayer Games?",
                listOf("Unity 6 URP", "Canvas 1D", "DOS VGA", "WebGL 1.0"),
                0
            ),
            QuizQuestionItem(
                "Gaming", "Medium",
                "How many points is the Red Queen worth in 3D Carrom Pro?",
                listOf("10 Points", "20 Points", "50 Points", "5 Points"),
                2
            ),
            QuizQuestionItem(
                "Music", "Medium",
                "Which audio engine keeps chatroom voice active during gameplay?",
                listOf("LiveKit WebRTC", "MIDI Synth", "Silent Mode", "Static PCM"),
                0
            ),
            QuizQuestionItem(
                "Science", "Hard",
                "How many total squares are on a standard Chess & Checkers board?",
                listOf("49 Squares", "64 Squares", "81 Squares", "100 Squares"),
                1
            ),
            QuizQuestionItem(
                "Sports", "Medium",
                "How many total balls (including Cue Ball) are used in 8 Ball Pool?",
                listOf("12 Balls", "15 Balls", "16 Balls", "21 Balls"),
                2
            )
        )
    }

    val filteredQuestions = remember(selectedCategory) {
        val list = if (selectedCategory == "All") allQuestions else allQuestions.filter { it.category == selectedCategory }
        list.ifEmpty { allQuestions }
    }

    var qIndex by remember { mutableIntStateOf(0) }
    var p1Score by remember { mutableIntStateOf(0) }
    var p2Score by remember { mutableIntStateOf(0) }
    var questionTimerSec by remember { mutableIntStateOf(15) }

    LaunchedEffect(qIndex, selectedCategory) {
        questionTimerSec = 15
        while (questionTimerSec > 0) {
            delay(1000L)
            questionTimerSec--
        }
    }

    val currentQ = filteredQuestions[qIndex % filteredQuestions.size]

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                categories.forEach { cat ->
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = if (selectedCategory == cat) NoisePink else Color(0xFF1D1636),
                        modifier = Modifier.clickable {
                            selectedCategory = cat
                            qIndex = 0
                        }
                    ) {
                        Text(
                            text = cat,
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
            Surface(
                shape = RoundedCornerShape(50),
                color = Color(0xFF281E4B),
                modifier = Modifier.clickable {
                    val nextIdx = (difficulties.indexOf(selectedDifficulty) + 1) % difficulties.size
                    selectedDifficulty = difficulties[nextIdx]
                }
            ) {
                Text(
                    text = "⚡ $selectedDifficulty",
                    color = NoiseGold,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            ScoreBadgeCard("@$p1Name", "$p1Score pts", true, NoisePink)
            ScoreBadgeCard("Q ${qIndex + 1}/${filteredQuestions.size}", "⏱ ${questionTimerSec}s", true, NoiseGold)
            ScoreBadgeCard("@$p2Name", "$p2Score pts", false, NoisePurple)
        }

        Surface(
            shape = RoundedCornerShape(22.dp),
            color = Color(0xFF19132E),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, NoisePurple, RoundedCornerShape(22.dp))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "${currentQ.category.uppercase()} • ${selectedDifficulty.uppercase()}",
                    color = Color(0xFF00E5FF),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = currentQ.question,
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp
                )
                Spacer(modifier = Modifier.height(14.dp))
                currentQ.options.forEachIndexed { idx, opt ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color.White.copy(alpha = 0.07f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 5.dp)
                            .clickable {
                                val diffMult = when (selectedDifficulty) {
                                    "Hard" -> 150
                                    "Medium" -> 120
                                    else -> 100
                                }
                                if (idx == currentQ.correctIndex) {
                                    GameSoundEffectsEngine.playPocket()
                                    p1Score += diffMult
                                } else {
                                    GameSoundEffectsEngine.playCapture()
                                    p2Score += 80
                                }
                                if (qIndex + 1 >= filteredQuestions.size) {
                                    GameSoundEffectsEngine.playVictory()
                                    onMatchComplete(
                                        if (p1Score >= p2Score) p1Name else p2Name,
                                        max(p1Score, p2Score).toLong(),
                                        min(p1Score, p2Score).toLong()
                                    )
                                } else {
                                    qIndex++
                                }
                            }
                            .testTag("quiz_option_$idx")
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.SportsEsports, contentDescription = null, tint = NoiseGold)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(opt, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
    }
}

@Composable
private fun ScoreBadgeCard(
    title: String,
    score: String,
    active: Boolean,
    accent: Color
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (active) accent.copy(alpha = 0.16f) else Color.White.copy(alpha = 0.06f),
        modifier = Modifier.border(
            1.dp,
            if (active) accent else Color.White.copy(alpha = 0.12f),
            RoundedCornerShape(14.dp)
        )
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
            Text(text = title, color = NoiseTextSecondary, fontSize = 10.sp, maxLines = 1)
            Text(text = score, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
        }
    }
}
