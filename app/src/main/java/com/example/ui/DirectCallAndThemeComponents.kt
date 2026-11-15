package com.example.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMissed
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.example.data.CHAT_BACKGROUND_THEMES
import com.example.data.CallSessionDoc
import com.example.data.ChatBackgroundThemeOption
import com.example.data.DirectMessage
import com.example.ui.theme.CyanHighlight
import com.example.ui.theme.DeepRoyalBlue
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.NoiseBlack
import com.example.ui.theme.NoiseBorder
import com.example.ui.theme.NoiseCard
import com.example.ui.theme.NoiseCardElevated
import com.example.ui.theme.NoisePink
import com.example.ui.theme.NoisePurple
import com.example.ui.theme.NoiseTextMuted
import com.example.ui.theme.NoiseTextSecondary
import com.example.ui.theme.NoiseNormalText
import com.example.ui.theme.NoiseWhite
import com.example.ui.theme.OnlineGreen
import com.example.ui.theme.ThemeManager
import com.example.ui.theme.VipGold
import com.example.ui.theme.noisePrimaryActionBrush
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

fun resolveChatThemeOption(themeKey: String): ChatBackgroundThemeOption {
    val clean = themeKey.trim().lowercase().ifBlank { "default_dark" }
    return CHAT_BACKGROUND_THEMES.find { it.key == clean } ?: CHAT_BACKGROUND_THEMES.first()
}

/**
 * Renders the shared conversation background theme (Dark, Light Default, Premium, or Animated) behind the message stream.
 */
@Composable
fun ChatThemeBackgroundBox(
    themeKey: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val option = remember(themeKey) { resolveChatThemeOption(themeKey) }
    val isLightDefault = option.key == "default_dark" && ThemeManager.isLightMode
    val primaryColor = if (isLightDefault) NoiseBlack else Color(option.primaryHex)
    val secondaryColor = if (isLightDefault) NoiseCardElevated else Color(option.secondaryHex)
    val accentColor = Color(option.accentHex)

    val transition = rememberInfiniteTransition(label = "chat_theme_bg_${option.key}")
    val animProgress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "chat_bg_wave"
    )

    Box(
        modifier = modifier
            .background(
                if (option.key == "default_dark") {
                    Brush.verticalGradient(listOf(NoiseBlack, NoiseBlack))
                } else {
                    Brush.verticalGradient(
                        listOf(
                            primaryColor,
                            secondaryColor,
                            primaryColor
                        )
                    )
                }
            )
            .testTag("chat_theme_background_${option.key}")
    ) {
        if (option.key != "default_dark") {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val driftX = (animProgress - 0.5f) * w * 0.35f
                val driftY = sin(animProgress * Math.PI.toFloat()) * h * 0.12f

                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            accentColor.copy(alpha = if (option.animated) 0.24f else 0.14f),
                            Color.Transparent
                        ),
                        center = Offset(w * 0.25f + driftX, h * 0.22f + driftY),
                        radius = (w * 0.58f).coerceAtLeast(120f)
                    ),
                    center = Offset(w * 0.25f + driftX, h * 0.22f + driftY),
                    radius = (w * 0.58f).coerceAtLeast(120f)
                )

                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            secondaryColor.copy(alpha = if (option.animated) 0.30f else 0.18f),
                            Color.Transparent
                        ),
                        center = Offset(w * 0.78f - driftX, h * 0.74f - driftY),
                        radius = (w * 0.62f).coerceAtLeast(140f)
                    ),
                    center = Offset(w * 0.78f - driftX, h * 0.74f - driftY),
                    radius = (w * 0.62f).coerceAtLeast(140f)
                )

                if (option.animated) {
                    val orbCount = 10
                    for (i in 0 until orbCount) {
                        val angle = (i * 36f + animProgress * 180f) * (Math.PI.toFloat() / 180f)
                        val cx = (w * ((i + 1f) / (orbCount + 1f))) + cos(angle) * 28f
                        val cy = (h * ((i % 5 + 1f) / 6f)) + sin(angle) * 34f
                        drawCircle(
                            color = accentColor.copy(alpha = 0.20f),
                            radius = (4f + (i % 3) * 2.5f),
                            center = Offset(cx, cy)
                        )
                    }
                }
            }
        }
        content()
    }
}

/**
 * Dialog to preview and apply a customizable Chat Background Theme for both conversation participants.
 */
@Composable
fun ChatThemeSelectorDialog(
    currentThemeKey: String,
    peerDisplayName: String,
    onSelectAndApplyTheme: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var previewThemeKey by remember(currentThemeKey) {
        mutableStateOf(currentThemeKey.ifBlank { "default_dark" })
    }
    val selectedOption = remember(previewThemeKey) { resolveChatThemeOption(previewThemeKey) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .border(1.2.dp, NoiseBorder, RoundedCornerShape(24.dp))
                .testTag("chat_theme_selector_dialog"),
            shape = RoundedCornerShape(24.dp),
            color = NoiseCard
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = "Chat Theme",
                            tint = CyanHighlight,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Chat Background Theme",
                                color = NoiseWhite,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = "Synced in real time with $peerDisplayName",
                                color = NoiseTextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = NoiseTextSecondary)
                    }
                }

                // Live Preview Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .border(1.dp, Color(selectedOption.accentHex).copy(alpha = 0.7f), RoundedCornerShape(18.dp))
                        .testTag("chat_theme_live_preview_box")
                ) {
                    ChatThemeBackgroundBox(
                        themeKey = previewThemeKey,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "LIVE PREVIEW • ${selectedOption.title.uppercase()}",
                                    color = Color.White.copy(alpha = 0.9f),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    text = selectedOption.category.uppercase(),
                                    color = Color(selectedOption.accentHex),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }

                            // Sample incoming message
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color(0xFF1A1D2D).copy(alpha = 0.9f))
                                    .padding(horizontal = 12.dp, vertical = 7.dp)
                            ) {
                                Text(
                                    text = "Hey! Love this new chat theme ✨",
                                    color = Color.White,
                                    fontSize = 12.sp
                                )
                            }

                            // Sample outgoing message
                            Box(
                                modifier = Modifier
                                    .align(Alignment.End)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(noisePrimaryActionBrush())
                                    .padding(horizontal = 12.dp, vertical = 7.dp)
                            ) {
                                Text(
                                    text = "Synced for both of us in real time! 🔥 ✓✓",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                // Theme Grid
                Text(
                    text = "Select a Dark, Premium, or Animated Theme:",
                    color = NoiseTextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    CHAT_BACKGROUND_THEMES.forEach { item ->
                        val isSelected = item.key == previewThemeKey
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (isSelected) NoiseCardElevated else NoiseBlack.copy(alpha = 0.45f))
                                .border(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) Color(item.accentHex) else NoiseBorder,
                                    shape = RoundedCornerShape(14.dp)
                                )
                                .clickable { previewThemeKey = item.key }
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                                .testTag("chat_theme_option_${item.key}"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(
                                            Brush.linearGradient(
                                                listOf(
                                                    Color(item.primaryHex),
                                                    Color(item.secondaryHex),
                                                    Color(item.accentHex)
                                                )
                                            )
                                        )
                                        .border(1.dp, Color(item.accentHex), CircleShape)
                                )
                                Column {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = item.title,
                                            color = NoiseWhite,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                        if (item.animated) {
                                            Text(
                                                text = "ANIMATED",
                                                color = CyanHighlight,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.ExtraBold
                                            )
                                        }
                                    }
                                    Text(
                                        text = "${item.category} • ${item.subtitle}",
                                        color = NoiseTextSecondary,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = OnlineGreen,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = NoiseCardElevated),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel", color = NoiseTextSecondary, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = {
                            onSelectAndApplyTheme(previewThemeKey)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1.4f)
                            .testTag("chat_theme_apply_button")
                    ) {
                        Text("Apply Shared Theme", color = Color.White, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }
    }
}

/**
 * Renders a Call History or Missed Call entry inside the conversation message stream.
 */
@Composable
fun CallEventMessageBubble(
    message: DirectMessage,
    onCallBack: (String) -> Unit
) {
    val isMissedOrDeclined = message.text.contains("Missed", ignoreCase = true) ||
        message.text.contains("declined", ignoreCase = true)
    val isVideo = message.mediaType == "video" || message.text.contains("video", ignoreCase = true)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .padding(vertical = 2.dp)
            .testTag("call_message_bubble_${message.id}")
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(
                    if (isMissedOrDeclined) Color(0xFFEF4444).copy(alpha = 0.22f)
                    else OnlineGreen.copy(alpha = 0.22f)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = when {
                    isMissedOrDeclined -> Icons.AutoMirrored.Filled.CallMissed
                    isVideo -> Icons.Default.Videocam
                    else -> Icons.Default.PhoneInTalk
                },
                contentDescription = "Call Event",
                tint = if (isMissedOrDeclined) Color(0xFFF87171) else OnlineGreen,
                modifier = Modifier.size(18.dp)
            )
        }

        Column(modifier = Modifier.weight(1f, fill = false)) {
            Text(
                text = message.text,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
            Text(
                text = if (isVideo) "LiveKit 1-on-1 Video Call" else "LiveKit 1-on-1 Audio Call",
                color = Color.White.copy(alpha = 0.75f),
                fontSize = 11.sp
            )
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(Color.White.copy(alpha = 0.18f))
                .clickable { onCallBack(if (isVideo) "video" else "audio") }
                .padding(horizontal = 10.dp, vertical = 5.dp)
                .testTag("call_back_button_${message.id}")
        ) {
            Text(
                text = "Call Back",
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 11.sp
            )
        }
    }
}

/**
 * Incoming 1-on-1 Audio or Video Call Overlay shown to the receiver when `call.status == "ringing"`.
 */
@Composable
fun IncomingCallOverlayModal(
    call: CallSessionDoc,
    onAccept: () -> Unit,
    onReject: () -> Unit
) {
    val isVideo = call.callType.equals("video", ignoreCase = true)
    val pulseTransition = rememberInfiniteTransition(label = "incoming_call_pulse")
    val ringScale by pulseTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(850, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ring_scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.88f))
            .clickable(enabled = true) {}
            .testTag("incoming_call_overlay_modal"),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .border(
                    width = 1.5.dp,
                    brush = Brush.linearGradient(listOf(ElectricViolet, CyanHighlight, DeepRoyalBlue)),
                    shape = RoundedCornerShape(28.dp)
                ),
            shape = RoundedCornerShape(28.dp),
            color = NoiseCard
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(ElectricViolet.copy(alpha = 0.22f))
                        .border(1.dp, CyanHighlight.copy(alpha = 0.6f), RoundedCornerShape(50))
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = if (isVideo) "📹 INCOMING LIVEKIT VIDEO CALL" else "📞 INCOMING LIVEKIT AUDIO CALL",
                        color = CyanHighlight,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 11.sp
                    )
                }

                Spacer(modifier = Modifier.height(22.dp))

                Box(
                    modifier = Modifier
                        .size((96 * ringScale).dp)
                        .clip(CircleShape)
                        .border(2.dp, CyanHighlight.copy(alpha = 0.65f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    NoiseAvatar(
                        avatarKey = call.callerAvatarKey.ifBlank { call.callerUsername.take(1).uppercase() },
                        size = 84.dp,
                        showOnlineDot = true
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    VipAnimatedUsername(
                        username = call.callerDisplayName.ifBlank { call.callerUsername },
                        vipLevel = call.callerVipLevel.toInt(),
                        fontSize = 20.sp
                    )
                    if (call.callerVipLevel > 0L) {
                        Spacer(modifier = Modifier.width(6.dp))
                        VipInlineGemBadge(
                            vipLevel = call.callerVipLevel.toInt(),
                            gemSize = 22.dp,
                            showLabel = true
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "@${call.callerUsername} is calling you...",
                    color = Color(0xFFCBD5E1),
                    fontSize = 13.sp
                )

                Spacer(modifier = Modifier.height(28.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Reject Call Button
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFEF4444))
                                .clickable { onReject() }
                                .testTag("incoming_call_reject_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CallEnd,
                                contentDescription = "Reject Call",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Reject",
                            color = Color(0xFFF87171),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    // Accept Call Button
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(OnlineGreen)
                                .clickable { onAccept() }
                                .testTag("incoming_call_accept_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isVideo) Icons.Default.Videocam else Icons.Default.Call,
                                contentDescription = "Accept Call",
                                tint = Color.Black,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Accept",
                            color = OnlineGreen,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}

/**
 * Full-Screen 1-on-1 Audio & Video Call Screen powered by the official LiveKit Android SDK
 * (`io.livekit:livekit-android`), the deployed Vercel LiveKit Token Service (`https://noice-live26.vercel.app`),
 * and `LiveKitAudioRouter` hardware audio routing.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun ActiveOneOnOneCallScreen(
    call: CallSessionDoc,
    currentUserId: String,
    onUpdateMediaState: (micMuted: Boolean, cameraEnabled: Boolean, frontCamera: Boolean) -> Unit,
    onEndCall: (durationSec: Long) -> Unit
) {
    val context = LocalContext.current
    val isCaller = call.callerId == currentUserId
    val localUsername = if (isCaller) call.callerUsername else call.calleeUsername
    val peerUserId = if (isCaller) call.calleeId else call.callerId
    val peerDisplayName = if (isCaller) call.calleeDisplayName.ifBlank { call.calleeUsername } else call.callerDisplayName.ifBlank { call.callerUsername }
    val peerUsername = if (isCaller) call.calleeUsername else call.callerUsername
    val peerAvatarKey = if (isCaller) call.calleeAvatarKey else call.callerAvatarKey
    val peerVipLevel = (if (isCaller) call.calleeVipLevel else call.callerVipLevel).toInt()
    val peerCameraEnabledFromDoc = if (isCaller) call.calleeCameraEnabled else call.callerCameraEnabled
    val peerMicMutedFromDoc = if (isCaller) call.calleeMicMuted else call.callerMicMuted

    var localMicMuted by remember(call.id) {
        mutableStateOf(if (isCaller) call.callerMicMuted else call.calleeMicMuted)
    }
    var localCameraEnabled by remember(call.id, call.callType) {
        mutableStateOf(if (isCaller) call.callerCameraEnabled else call.calleeCameraEnabled)
    }
    var localFrontCamera by remember(call.id) {
        mutableStateOf(if (isCaller) call.callerFrontCamera else call.calleeFrontCamera)
    }
    var speakerEnabled by remember { mutableStateOf(true) }
    var elapsedSec by remember(call.id) { mutableLongStateOf(0L) }

    val liveKitSessionManager = remember(context, call.id) { LiveKitRoomSessionManager(context) }
    val audioRouter = liveKitSessionManager.audioRouter
    val liveKitState by liveKitSessionManager.sessionState.collectAsState()

    val permissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        audioRouter.enterVoiceRoomSession(call.liveKitRoomName)
        audioRouter.selectAudioRoute(if (speakerEnabled) AudioOutputRoute.SPEAKER else AudioOutputRoute.EARPIECE)
        liveKitSessionManager.connectToPrivateCall(
            callId = call.id,
            liveKitRoomName = call.liveKitRoomName,
            currentUserId = currentUserId,
            currentUsername = localUsername,
            peerUserId = peerUserId,
            isVideoCall = call.callType.equals("video", ignoreCase = true) || localCameraEnabled,
            startMicMuted = localMicMuted,
            startCameraEnabled = localCameraEnabled,
            startFrontCamera = localFrontCamera
        )
    }

    LaunchedEffect(call.id) {
        val needed = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            needed.add(Manifest.permission.RECORD_AUDIO)
        }
        if ((call.callType == "video" || localCameraEnabled) &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
        ) {
            needed.add(Manifest.permission.CAMERA)
        }
        if (needed.isNotEmpty()) {
            try {
                permissionsLauncher.launch(needed.toTypedArray())
            } catch (_: Exception) {
                audioRouter.enterVoiceRoomSession(call.liveKitRoomName)
                audioRouter.selectAudioRoute(if (speakerEnabled) AudioOutputRoute.SPEAKER else AudioOutputRoute.EARPIECE)
                liveKitSessionManager.connectToPrivateCall(
                    callId = call.id,
                    liveKitRoomName = call.liveKitRoomName,
                    currentUserId = currentUserId,
                    currentUsername = localUsername,
                    peerUserId = peerUserId,
                    isVideoCall = call.callType.equals("video", ignoreCase = true) || localCameraEnabled,
                    startMicMuted = localMicMuted,
                    startCameraEnabled = localCameraEnabled,
                    startFrontCamera = localFrontCamera
                )
            }
        } else {
            audioRouter.enterVoiceRoomSession(call.liveKitRoomName)
            audioRouter.selectAudioRoute(if (speakerEnabled) AudioOutputRoute.SPEAKER else AudioOutputRoute.EARPIECE)
            liveKitSessionManager.connectToPrivateCall(
                callId = call.id,
                liveKitRoomName = call.liveKitRoomName,
                currentUserId = currentUserId,
                currentUsername = localUsername,
                peerUserId = peerUserId,
                isVideoCall = call.callType.equals("video", ignoreCase = true) || localCameraEnabled,
                startMicMuted = localMicMuted,
                startCameraEnabled = localCameraEnabled,
                startFrontCamera = localFrontCamera
            )
        }
    }

    DisposableEffect(call.id) {
        onDispose {
            liveKitSessionManager.leaveRoom(deleteOnServerAsHost = false)
            liveKitSessionManager.release()
        }
    }

    // Live Call Timer once accepted
    LaunchedEffect(call.id, call.status) {
        while (call.status == "accepted") {
            delay(1000L)
            elapsedSec += 1L
        }
    }

    BackHandler {
        onEndCall(elapsedSec)
    }

    val mins = elapsedSec / 60L
    val secs = elapsedSec % 60L
    val formattedTimer = String.format(java.util.Locale.US, "%02d:%02d", mins, secs)

    val remoteParticipant = liveKitState.participants.firstOrNull { !it.isLocal }
    val remoteVideoTrack = remoteParticipant?.videoTrack ?: liveKitState.primaryRemoteVideoTrack
    val localVideoTrack = liveKitState.localVideoTrack
    val peerCameraEnabled = (remoteVideoTrack != null) || peerCameraEnabledFromDoc
    val peerMicMuted = if (remoteParticipant != null) !remoteParticipant.isMicEnabled else peerMicMutedFromDoc

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF070913), Color(0xFF10172A), Color(0xFF070913))
                )
            )
            .testTag("active_one_on_one_call_screen")
    ) {
        // Full-Screen Remote LiveKit Video Track when peer video is active
        if (remoteVideoTrack != null && liveKitSessionManager.currentRoom() != null) {
            LiveKitVideoTrackView(
                room = liveKitSessionManager.currentRoom(),
                videoTrack = remoteVideoTrack,
                mirror = false,
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("remote_livekit_video_view")
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Status Bar
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Color(0xFF151C30).copy(alpha = 0.88f))
                        .border(1.dp, CyanHighlight.copy(alpha = 0.5f), RoundedCornerShape(50))
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    liveKitState.connectionStatus == LiveKitConnectionStatus.CONNECTED || call.status == "accepted" -> OnlineGreen
                                    liveKitState.connectionStatus == LiveKitConnectionStatus.RECONNECTING -> VipGold
                                    else -> VipGold
                                }
                            )
                    )
                    Text(
                        text = if (call.status == "accepted") {
                            "LIVEKIT ${call.callType.uppercase()} CALL • $formattedTimer"
                        } else {
                            "CALLING @${peerUsername.uppercase()}..."
                        },
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.sp,
                        modifier = Modifier.testTag("active_call_timer_label")
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Room: ${call.liveKitRoomName} • ${liveKitState.connectionStatus.label}",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp
                    )
                    if (liveKitState.connectionStatus == LiveKitConnectionStatus.ERROR ||
                        liveKitState.connectionStatus == LiveKitConnectionStatus.DISCONNECTED ||
                        liveKitState.connectionStatus == LiveKitConnectionStatus.BACKEND_CONFIG_MISSING
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(ElectricViolet.copy(alpha = 0.3f))
                                .border(1.dp, CyanHighlight, RoundedCornerShape(10.dp))
                                .clickable { liveKitSessionManager.reconnect() }
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                                .testTag("call_livekit_reconnect_button")
                        ) {
                            Text(
                                text = "Reconnect",
                                color = CyanHighlight,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                if (!liveKitState.errorMessage.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = liveKitState.errorMessage ?: "",
                        color = VipGold,
                        fontSize = 10.sp,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .fillMaxWidth(0.92f)
                            .testTag("call_livekit_diagnostic_text")
                    )
                }
            }

            // Center Peer Visual / Video Viewport
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(136.dp)
                        .clip(CircleShape)
                        .border(
                            width = 3.dp,
                            brush = Brush.sweepGradient(
                                if (remoteParticipant?.isSpeaking == true) {
                                    listOf(OnlineGreen, CyanHighlight, OnlineGreen)
                                } else {
                                    listOf(ElectricViolet, CyanHighlight, DeepRoyalBlue, ElectricViolet)
                                }
                            ),
                            shape = CircleShape
                        )
                        .padding(6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    NoiseAvatar(
                        avatarKey = peerAvatarKey.ifBlank { peerUsername.take(1).uppercase() },
                        size = 120.dp,
                        showOnlineDot = true
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    VipAnimatedUsername(
                        username = peerDisplayName,
                        vipLevel = peerVipLevel,
                        fontSize = 22.sp
                    )
                    if (peerVipLevel > 0) {
                        Spacer(modifier = Modifier.width(6.dp))
                        VipInlineGemBadge(vipLevel = peerVipLevel, gemSize = 22.dp, showLabel = true)
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = when {
                        call.status != "accepted" -> "Ringing on @$peerUsername's device..."
                        remoteParticipant?.isSpeaking == true -> "@$peerUsername is speaking (LiveKit HD Audio)"
                        peerMicMuted -> "@$peerUsername muted their microphone"
                        localCameraEnabled -> "Camera On (${if (localFrontCamera) "Front" else "Rear"} Camera)"
                        else -> "HD Voice Connected via LiveKit (${liveKitState.participants.size.coerceAtLeast(1)} in room)"
                    },
                    color = Color(0xFFCBD5E1),
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )

                // Local Camera Preview PiP Card when Video is enabled
                if (localCameraEnabled) {
                    Spacer(modifier = Modifier.height(18.dp))
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = Color(0xFF141B2D),
                        modifier = Modifier
                            .width(170.dp)
                            .height(106.dp)
                            .border(1.5.dp, CyanHighlight, RoundedCornerShape(18.dp))
                            .testTag("local_video_pip_card")
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            if (localVideoTrack != null && liveKitSessionManager.currentRoom() != null) {
                                LiveKitVideoTrackView(
                                    room = liveKitSessionManager.currentRoom(),
                                    videoTrack = localVideoTrack,
                                    mirror = localFrontCamera,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(10.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "YOU (${if (localFrontCamera) "FRONT" else "REAR"})",
                                        color = CyanHighlight,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                    Icon(
                                        imageVector = Icons.Default.Videocam,
                                        contentDescription = null,
                                        tint = OnlineGreen,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                                Text(
                                    text = "LiveKit Video Stream Active",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }

            // Bottom Call Control Dock: Mute Mic | Speaker | Camera On/Off | Flip Camera | End Call
            Surface(
                shape = RoundedCornerShape(26.dp),
                color = Color(0xFF111827).copy(alpha = 0.94f),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFF334155), RoundedCornerShape(26.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 1. Mute / Unmute Mic
                    CallActionCircleButton(
                        icon = if (localMicMuted) Icons.Default.MicOff else Icons.Default.Mic,
                        label = if (localMicMuted) "Muted" else "Mic On",
                        active = !localMicMuted,
                        testTag = "call_toggle_mic_button",
                        onClick = {
                            localMicMuted = !localMicMuted
                            liveKitSessionManager.setMicrophoneMuted(localMicMuted)
                            onUpdateMediaState(localMicMuted, localCameraEnabled, localFrontCamera)
                        }
                    )

                    // 2. Speaker / Earpiece Audio Route
                    CallActionCircleButton(
                        icon = Icons.AutoMirrored.Filled.VolumeUp,
                        label = if (speakerEnabled) "Speaker" else "Earpiece",
                        active = speakerEnabled,
                        testTag = "call_toggle_speaker_button",
                        onClick = {
                            speakerEnabled = !speakerEnabled
                            audioRouter.selectAudioRoute(
                                if (speakerEnabled) AudioOutputRoute.SPEAKER else AudioOutputRoute.EARPIECE
                            )
                        }
                    )

                    // 3. Camera On / Off
                    CallActionCircleButton(
                        icon = if (localCameraEnabled) Icons.Default.Videocam else Icons.Default.VideocamOff,
                        label = if (localCameraEnabled) "Cam On" else "Cam Off",
                        active = localCameraEnabled,
                        testTag = "call_toggle_camera_button",
                        onClick = {
                            localCameraEnabled = !localCameraEnabled
                            liveKitSessionManager.setCameraEnabled(localCameraEnabled)
                            onUpdateMediaState(localMicMuted, localCameraEnabled, localFrontCamera)
                        }
                    )

                    // 4. Switch Front / Rear Camera
                    CallActionCircleButton(
                        icon = Icons.Default.Cameraswitch,
                        label = if (localFrontCamera) "Front" else "Rear",
                        active = localCameraEnabled,
                        testTag = "call_switch_camera_button",
                        onClick = {
                            localFrontCamera = !localFrontCamera
                            if (!localCameraEnabled) {
                                localCameraEnabled = true
                                liveKitSessionManager.setCameraEnabled(true)
                            }
                            liveKitSessionManager.setFrontCamera(localFrontCamera)
                            onUpdateMediaState(localMicMuted, localCameraEnabled, localFrontCamera)
                        }
                    )

                    // 5. End Call
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFEF4444))
                                .clickable {
                                    liveKitSessionManager.leaveRoom(deleteOnServerAsHost = false)
                                    onEndCall(elapsedSec)
                                }
                                .testTag("call_end_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CallEnd,
                                contentDescription = "End Call",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "End",
                            color = Color(0xFFF87171),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CallActionCircleButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    active: Boolean,
    testTag: String,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(if (active) ElectricViolet.copy(alpha = 0.28f) else Color(0xFF1E293B))
                .border(
                    1.dp,
                    if (active) CyanHighlight else Color(0xFF475569),
                    CircleShape
                )
                .clickable { onClick() }
                .testTag(testTag),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (active) Color.White else Color(0xFF94A3B8),
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            color = if (active) Color.White else Color(0xFF94A3B8),
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
