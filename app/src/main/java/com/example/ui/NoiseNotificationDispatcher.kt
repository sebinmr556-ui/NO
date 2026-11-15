package com.example.ui

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.GameInviteDoc
import com.example.data.NotificationItem
import com.example.ui.theme.NoiseCard
import com.example.ui.theme.NoisePink
import com.example.ui.theme.NoisePurple
import com.example.ui.theme.NoiseTextMuted
import com.example.ui.theme.OnlineGreen
import com.example.ui.theme.VipGold

object NoiseNotificationDispatcher {
    const val CHANNEL_MESSAGES = "noicelive_messages"
    const val CHANNEL_GAMES = "noicelive_games"
    const val CHANNEL_CALLS = "noicelive_calls"

    const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
    const val EXTRA_CONVERSATION_ID = "extra_conversation_id"
    const val EXTRA_GAME_ROOM_ID = "extra_game_room_id"
    const val EXTRA_CALL_ID = "extra_call_id"

    private val dispatchedSystemIds = mutableSetOf<String>()

    fun clearCacheForTests() {
        synchronized(dispatchedSystemIds) {
            dispatchedSystemIds.clear()
        }
    }

    fun ensureNotificationChannels(context: Context) {
        ensureChannelsCreated(context)
    }

    fun ensureChannelsCreated(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
            val messagesChannel = NotificationChannel(
                CHANNEL_MESSAGES,
                "Direct Messages",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Real-time direct message alerts"
                enableVibration(true)
            }
            val gamesChannel = NotificationChannel(
                CHANNEL_GAMES,
                "Multiplayer Game Invitations",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Real-time 3D Ludo and multiplayer game invites"
            }
            val callsChannel = NotificationChannel(
                CHANNEL_CALLS,
                "Audio & Video Calls",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Incoming LiveKit audio and video calls"
                enableVibration(true)
            }
            manager.createNotificationChannel(messagesChannel)
            manager.createNotificationChannel(gamesChannel)
            manager.createNotificationChannel(callsChannel)
        }
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun dispatchNotificationItem(
        context: Context,
        currentUserId: String = "",
        item: NotificationItem
    ): Boolean {
        if (item.id.isBlank() || item.read) return false
        if (currentUserId.isNotBlank()) {
            if (item.userId.isNotBlank() && item.userId != currentUserId) return false
            if (item.actorUid.isNotBlank() && item.actorUid == currentUserId) return false
        }
        synchronized(dispatchedSystemIds) {
            if (!dispatchedSystemIds.add(item.id)) return false
        }
        ensureChannelsCreated(context)
        if (!hasNotificationPermission(context)) return true

        val channelId = when (item.type) {
            "game_invite" -> CHANNEL_GAMES
            "call" -> CHANNEL_CALLS
            else -> CHANNEL_MESSAGES
        }
        val title = when (item.type) {
            "message", "dm" -> "@${item.actorUsername.ifBlank { "New Message" }}"
            "game_invite" -> "Game Invite from @${item.actorUsername}"
            "call" -> "Call from @${item.actorUsername}"
            else -> "NOICE LIVE • @${item.actorUsername}"
        }
        val body = item.message.ifBlank { "Tap to open in NOICE LIVE" }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_NOTIFICATION_ID, item.id)
            if (item.conversationId.isNotBlank()) {
                putExtra(EXTRA_CONVERSATION_ID, item.conversationId)
            }
            if (item.gameRoomId.isNotBlank()) {
                putExtra(EXTRA_GAME_ROOM_ID, item.gameRoomId)
            }
        }
        val pendingFlags = PendingIntent.FLAG_UPDATE_CURRENT or
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        val pendingIntent = PendingIntent.getActivity(
            context,
            item.id.hashCode(),
            intent,
            pendingFlags
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(
                when (item.type) {
                    "call" -> NotificationCompat.CATEGORY_CALL
                    else -> NotificationCompat.CATEGORY_MESSAGE
                }
            )
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        runCatching {
            NotificationManagerCompat.from(context).notify(item.id.hashCode(), notification)
        }
        return true
    }

    fun dispatchGameInviteSystemNotification(
        context: Context,
        currentUserId: String = "",
        invite: GameInviteDoc
    ): Boolean {
        if (invite.id.isBlank() || invite.status != "pending") return false
        if (currentUserId.isNotBlank()) {
            if (invite.receiverUid.isNotBlank() && invite.receiverUid != currentUserId) return false
            if (invite.senderUid.isNotBlank() && invite.senderUid == currentUserId) return false
        }
        val key = "ginv_sys_${invite.id}"
        synchronized(dispatchedSystemIds) {
            if (!dispatchedSystemIds.add(key)) return false
        }
        ensureChannelsCreated(context)
        if (!hasNotificationPermission(context)) return true

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_GAME_ROOM_ID, invite.gameRoomId)
        }
        val pendingFlags = PendingIntent.FLAG_UPDATE_CURRENT or
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        val pendingIntent = PendingIntent.getActivity(
            context,
            key.hashCode(),
            intent,
            pendingFlags
        )

        val title = "🎲 ${invite.gameTitle} Invitation"
        val body = "@${invite.senderUsername} invited you to Slot ${invite.slotIndex} (Room: ${invite.roomCode})"

        val notification = NotificationCompat.Builder(context, CHANNEL_GAMES)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        runCatching {
            NotificationManagerCompat.from(context).notify(key.hashCode(), notification)
        }
        return true
    }
}

@Composable
fun RealTimeNotificationBannerOverlay(
    notification: NotificationItem?,
    onOpenNotification: (NotificationItem) -> Unit,
    onDismiss: (NotificationItem) -> Unit
) {
    AnimatedVisibility(
        visible = notification != null,
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut()
    ) {
        val item = notification ?: return@AnimatedVisibility
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .testTag("realtime_inapp_notification_banner")
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .border(
                        1.5.dp,
                        Brush.horizontalGradient(listOf(NoisePurple, NoisePink, VipGold)),
                        RoundedCornerShape(18.dp)
                    )
                    .clickable { onOpenNotification(item) },
                color = NoiseCard.copy(alpha = 0.97f),
                shadowElevation = 12.dp,
                shape = RoundedCornerShape(18.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(contentAlignment = Alignment.BottomEnd) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Brush.linearGradient(listOf(NoisePink, NoisePurple))),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = item.actorAvatarKey.ifBlank { item.actorUsername.take(1).uppercase() }.take(1).uppercase(),
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(if (item.type == "game_invite") VipGold else NoisePurple),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (item.type == "game_invite") Icons.Default.SportsEsports else Icons.Default.ChatBubble,
                                contentDescription = "Notification Type",
                                tint = Color.White,
                                modifier = Modifier.size(11.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "@${item.actorUsername}",
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "• ${item.timeLabel}",
                                color = VipGold,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = item.message,
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 13.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (item.conversationId.isNotBlank()) {
                            Text(
                                text = "Tap to open conversation",
                                color = NoisePink,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    IconButton(
                        onClick = { onDismiss(item) },
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("dismiss_inapp_notification_banner")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Dismiss notification",
                            tint = NoiseTextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun IncomingGameInviteOverlayModal(
    invite: GameInviteDoc?,
    onAccept: (GameInviteDoc) -> Unit,
    onReject: (GameInviteDoc) -> Unit
) {
    AnimatedVisibility(
        visible = invite != null,
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut()
    ) {
        val currentInvite = invite ?: return@AnimatedVisibility
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 10.dp)
                .testTag("incoming_game_invite_overlay")
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .border(
                        2.dp,
                        Brush.horizontalGradient(listOf(VipGold, NoisePink, NoisePurple)),
                        RoundedCornerShape(22.dp)
                    ),
                color = Color(0xFF0E0B1A).copy(alpha = 0.98f),
                shadowElevation = 16.dp,
                shape = RoundedCornerShape(22.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(Brush.linearGradient(listOf(NoisePink, NoisePurple))),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = currentInvite.senderAvatarKey.ifBlank {
                                    currentInvite.senderUsername.take(1).uppercase()
                                }.take(1).uppercase(),
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 18.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(VipGold.copy(alpha = 0.18f))
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "MULTIPLAYER INVITE • SLOT ${currentInvite.slotIndex}",
                                        color = VipGold,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "@${currentInvite.senderUsername} invited you to ${currentInvite.gameTitle}",
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp
                            )
                            Text(
                                text = "Room Code: ${currentInvite.roomCode} • Real-Time Synced Session",
                                color = NoiseTextMuted,
                                fontSize = 12.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onReject(currentInvite) },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("reject_global_game_invite_button"),
                            shape = RoundedCornerShape(14.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, NoisePink.copy(alpha = 0.7f))
                        ) {
                            Text(
                                text = "Reject",
                                color = NoisePink,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                        Button(
                            onClick = { onAccept(currentInvite) },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("accept_global_game_invite_button"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = OnlineGreen)
                        ) {
                            Text(
                                text = "Accept & Join",
                                color = Color.Black,
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
