package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.WorkspacePremium
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.example.data.Comment
import com.example.data.Conversation
import com.example.data.DirectMessage
import com.example.data.NotificationItem
import com.example.data.Post
import com.example.data.UiState
import com.example.ui.theme.LightLavender
import com.example.ui.theme.NoiseBlack
import com.example.ui.theme.NoiseBorder
import com.example.ui.theme.NoiseCard
import com.example.ui.theme.NoiseCardElevated
import com.example.ui.theme.NoiseNormalText
import com.example.ui.theme.NoisePink
import com.example.ui.theme.NoisePurple
import com.example.ui.theme.NoiseTextMuted
import com.example.ui.theme.NoiseTextSecondary
import com.example.ui.theme.NoiseWhite
import com.example.ui.theme.OnlineGreen
import com.example.ui.theme.PacificoFontFamily
import com.example.ui.theme.ThemeManager
import com.example.ui.theme.VerifiedBlue
import com.example.ui.theme.VipGold
import com.example.ui.theme.VipGoldBorder
import com.example.ui.theme.VipGoldDark
import com.example.ui.theme.noisePurplePinkGradient
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun NotificationsModal(
    notificationsState: UiState<List<NotificationItem>>,
    onMarkAllRead: () -> Unit,
    onClearAll: () -> Unit,
    onNotificationClick: (NotificationItem) -> Unit = {},
    onClose: () -> Unit
) {
    BackHandler { onClose() }
    val notifications = (notificationsState as? UiState.Success)?.data ?: emptyList()
    val unreadNotifications = notifications.filter { !it.read }
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = All, 1 = Unread

    val displayedList = if (selectedTab == 0) notifications else unreadNotifications

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.72f))
            .clickable(enabled = false) {}
            .testTag("notifications_modal")
    ) {
        val isLight = ThemeManager.isLightMode
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 78.dp),
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            color = if (isLight) Color(0xFFFFFFFF) else Color(0xFF0B0B0F)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .width(42.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(if (isLight) NoiseBorder else Color(0xFF3A3A48))
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Notifications",
                            color = NoiseWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                        if (unreadNotifications.isNotEmpty()) {
                            Spacer(modifier = Modifier.width(10.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(if (isLight) LightLavender else Color(0xFF3B0D22))
                                    .border(1.dp, NoisePink.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                                    .padding(horizontal = 10.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = "${unreadNotifications.size} new",
                                    color = NoisePink,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onMarkAllRead,
                            modifier = Modifier.testTag("notifications_mark_read_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DoneAll,
                                contentDescription = "Mark All Read",
                                tint = OnlineGreen,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        IconButton(
                            onClick = onClearAll,
                            modifier = Modifier.testTag("notifications_clear_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Clear Notifications",
                                tint = NoiseTextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        IconButton(
                            onClick = onClose,
                            modifier = Modifier.testTag("notifications_close_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close Notifications",
                                tint = NoiseTextSecondary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }

                HorizontalDivider(color = NoiseBorder.copy(alpha = 0.6f), thickness = 0.8.dp)

                val unselectedNotifBrush = Brush.horizontalGradient(listOf(NoiseCardElevated, NoiseCardElevated))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(18.dp))
                            .background(if (selectedTab == 0) noisePurplePinkGradient() else unselectedNotifBrush)
                            .border(1.dp, if (selectedTab == 0) NoisePink else NoiseBorder, RoundedCornerShape(18.dp))
                            .clickable { selectedTab = 0 }
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "All (${notifications.size})",
                            color = if (selectedTab == 0) Color.White else NoiseNormalText,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(18.dp))
                            .background(if (selectedTab == 1) noisePurplePinkGradient() else unselectedNotifBrush)
                            .border(1.dp, if (selectedTab == 1) NoisePink else NoiseBorder, RoundedCornerShape(18.dp))
                            .clickable { selectedTab = 1 }
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Unread (${unreadNotifications.size})",
                            color = if (selectedTab == 1) Color.White else NoiseNormalText,
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp
                        )
                    }
                }

                HorizontalDivider(color = NoiseBorder.copy(alpha = 0.4f), thickness = 0.8.dp)

                if (displayedList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.ChatBubble,
                                contentDescription = null,
                                tint = NoiseTextMuted,
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "No notifications yet",
                                color = NoiseWhite,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Real-time messages, calls, game invitations, and activity alerts will appear here.",
                                color = NoiseTextSecondary,
                                fontSize = 13.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(displayedList, key = { it.id }) { item ->
                            NotificationCardRow(
                                item = item,
                                onClick = { onNotificationClick(item) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationCardRow(
    item: NotificationItem,
    onClick: () -> Unit = {}
) {
    val isLight = ThemeManager.isLightMode
    val borderColor = if (!item.read) NoisePink.copy(alpha = 0.75f) else NoiseBorder
    val bgColor = if (isLight) {
        if (!item.read) LightLavender else Color(0xFFFFFFFF)
    } else {
        if (!item.read) Color(0xFF15121B) else Color(0xFF101015)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(bgColor)
            .border(1.2.dp, borderColor, RoundedCornerShape(18.dp))
            .clickable { onClick() }
            .padding(12.dp)
            .testTag("notification_row_${item.id}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(modifier = Modifier.size(48.dp)) {
                NoiseAvatar(avatarKey = item.actorAvatarKey, size = 46.dp)
                val (badgeIcon, badgeColor) = when (item.type) {
                    "like", "reel_like" -> Icons.Default.Favorite to NoisePink
                    "comment", "message", "dm" -> Icons.Default.ChatBubble to Color(0xFF0EA5E9)
                    "call" -> Icons.Default.Call to OnlineGreen
                    "game_invite" -> Icons.Default.AutoAwesome to VipGold
                    "mention" -> Icons.Default.AutoAwesome to VipGold
                    else -> Icons.Default.PersonAdd to NoisePurple
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = 2.dp, y = 2.dp)
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(if (isLight) Color(0xFFFFFFFF) else Color(0xFF14141D))
                        .border(1.dp, badgeColor.copy(alpha = 0.6f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = badgeIcon,
                        contentDescription = null,
                        tint = badgeColor,
                        modifier = Modifier.size(11.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                val titleLine = when (item.type) {
                    "message", "dm" -> "@${item.actorUsername}: ${item.message}"
                    else -> "${item.actorUsername} ${item.message}"
                }
                Text(
                    text = titleLine,
                    color = NoiseWhite,
                    fontSize = 14.sp,
                    lineHeight = 19.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = NoiseTextMuted,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = item.timeLabel,
                        color = NoiseTextSecondary,
                        fontSize = 12.sp
                    )
                    if (!item.read) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(NoisePink)
                        )
                    }
                    if (item.conversationId.isNotBlank() || item.type in listOf("message", "dm")) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Open Chat →",
                            color = NoisePink,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    } else if (item.gameRoomId.isNotBlank() || item.type == "game_invite") {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Join Game →",
                            color = VipGold,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        if (item.targetThumbKey.isNotBlank() && item.type !in listOf("message", "dm", "game_invite", "call")) {
            Spacer(modifier = Modifier.width(10.dp))
            NoiseMediaImage(
                mediaKey = item.targetThumbKey,
                contentDescription = "Post Thumbnail",
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(10.dp))
            )
        }
    }
}

@Composable
fun VipUnlimitedVaultModal(
    username: String,
    coins: Long,
    onClaimTier: (tierName: String, amount: Long) -> Unit,
    onClose: () -> Unit,
    currentUserProfile: com.example.data.UserProfile? = null,
    vipBadgeCatalog: List<com.example.data.VipBadgeCatalogItem> = emptyList(),
    onPurchaseVipBadge: (Int) -> Unit = {},
    onSelectActiveVipBadge: (Int) -> Unit = {},
    coinTransactionsState: UiState<List<com.example.data.CoinTransaction>> = UiState.Success(emptyList())
) {
    val tiers = listOf(
        Triple("Starter", 1000L, "1,000"),
        Triple("Popular", 5000L, "5,000"),
        Triple("Super VIP", 20000L, "20,000"),
        Triple("Hyper King", 100000L, "100,000")
    )

    val activeVipLevel = (currentUserProfile?.activeVipBadgeLevel?.takeIf { it > 0L } ?: 1L).toInt()
    val ownedLevels = remember(currentUserProfile?.ownedVipBadges) {
        currentUserProfile?.ownedVipBadges?.map { it.toInt() }?.toSet() ?: setOf(1)
    }
    val recentTransactions = (coinTransactionsState as? UiState.Success)?.data ?: emptyList()

    val isLight = ThemeManager.isLightMode
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 20.dp)
                .border(1.2.dp, VipGoldBorder, RoundedCornerShape(26.dp))
                .testTag("vip_unlimited_vault_modal"),
            shape = RoundedCornerShape(26.dp),
            color = if (isLight) Color(0xFFFFFFFF) else Color(0xFF0E0E12)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.WorkspacePremium,
                            contentDescription = "VIP Crown",
                            tint = VipGold,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "VIP COIN VAULT & LUXURY BADGE SHOP",
                            color = VipGold,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp,
                            letterSpacing = 0.5.sp
                        )
                    }

                    IconButton(
                        onClick = onClose,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("vip_vault_close_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Vault",
                            tint = NoiseTextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Current Balance Hero Card
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(
                            Brush.horizontalGradient(
                                if (isLight) {
                                    listOf(LightLavender, Color(0xFFFFF8DB))
                                } else {
                                    listOf(Color(0xFF1A1528), Color(0xFF12101B))
                                }
                            )
                        )
                        .border(1.dp, VipGoldBorder, RoundedCornerShape(18.dp))
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                        .testTag("vip_vault_current_balance"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "CURRENT VIP COIN BALANCE",
                            color = NoiseTextSecondary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "🪙 ${formatCoins(coins)} Coins",
                            color = VipGold,
                            fontWeight = FontWeight.Black,
                            fontSize = 22.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "@$username",
                                color = NoiseWhite,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            VipInlineGemBadge(
                                vipLevel = activeVipLevel,
                                gemSize = 20.dp,
                                showLabel = true
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(VipGoldDark)
                            .border(1.dp, VipGoldBorder, RoundedCornerShape(14.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "CLOUD SYNCED",
                                color = OnlineGreen,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 9.sp
                            )
                            Text(
                                text = "${recentTransactions.size} Txns",
                                color = VipGold,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // VIP FREE COIN REFILL VAULT TIERS
                Text(
                    text = "VIP FREE COIN REFILL TIERS",
                    color = VipGold,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 13.sp,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    tiers.forEach { (badge, amount, formatted) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isLight) Color(0xFFF8F8FC) else Color(0xFF14141A))
                                .border(1.dp, NoiseBorder, RoundedCornerShape(16.dp))
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(22.dp)
                                            .clip(CircleShape)
                                            .background(VipGold),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AccountBalance,
                                            contentDescription = null,
                                            tint = Color.Black,
                                            modifier = Modifier.size(13.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "+$formatted",
                                        color = VipGold,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 18.sp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(VipGoldDark)
                                            .padding(horizontal = 7.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = badge,
                                            color = VipGold,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Instant Firestore Vault Deposit",
                                    color = NoiseTextSecondary,
                                    fontSize = 12.sp
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(VipGold)
                                    .clickable { onClaimTier(badge, amount) }
                                    .padding(horizontal = 18.dp, vertical = 10.dp)
                                    .testTag("claim_vip_${badge.lowercase().replace(" ", "_")}")
                            ) {
                                Text(
                                    text = "CLAIM FREE",
                                    color = Color.Black,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = NoiseBorder, thickness = 0.8.dp)
                Spacer(modifier = Modifier.height(14.dp))

                // RECENT COIN TRANSACTIONS SECTION (Firestore /coin_transactions)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = "Recent Transactions",
                            tint = VipGold,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "RECENT COIN TRANSACTIONS",
                            color = VipGold,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp,
                            letterSpacing = 0.5.sp
                        )
                    }
                    Text(
                        text = "${recentTransactions.size} recorded",
                        color = NoiseTextSecondary,
                        fontSize = 11.sp
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("vip_coin_transactions_list"),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    when (coinTransactionsState) {
                        is UiState.Loading -> {
                            Text(
                                text = "Loading coin transactions from Firestore...",
                                color = NoiseTextSecondary,
                                fontSize = 12.sp
                            )
                        }
                        is UiState.Error -> {
                            Text(
                                text = coinTransactionsState.message,
                                color = NoisePink,
                                fontSize = 12.sp
                            )
                        }
                        is UiState.Success -> {
                            if (recentTransactions.isEmpty()) {
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (isLight) Color(0xFFF8F8FC) else Color(0xFF14141C),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, NoiseBorder, RoundedCornerShape(14.dp))
                                ) {
                                    Text(
                                        text = "No coin transactions yet. Claim a VIP tier above or win a 3D game match to record your first transaction!",
                                        color = NoiseTextSecondary,
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(14.dp)
                                    )
                                }
                            } else {
                                recentTransactions.take(12).forEach { tx ->
                                    val isCredit = tx.amount >= 0L
                                    val amountText = if (isCredit) "+${formatCoins(tx.amount)}" else "-${formatCoins(kotlin.math.abs(tx.amount))}"
                                    val amountColor = if (isCredit) OnlineGreen else NoisePink
                                    val timeFormatted = remember(tx.createdAt) {
                                        val millis = tx.createdAt?.toDate()?.time
                                        if (millis != null) {
                                            java.text.SimpleDateFormat("MMM d, h:mm a", java.util.Locale.US).format(java.util.Date(millis))
                                        } else {
                                            "Just now"
                                        }
                                    }

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(if (isLight) Color(0xFFF8F8FC) else Color(0xFF14141C))
                                            .border(1.dp, NoiseBorder, RoundedCornerShape(14.dp))
                                            .padding(horizontal = 12.dp, vertical = 10.dp)
                                            .testTag("coin_tx_row_${tx.id}"),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(34.dp)
                                                    .clip(CircleShape)
                                                    .background(
                                                        if (isCredit) OnlineGreen.copy(alpha = 0.16f)
                                                        else NoisePink.copy(alpha = 0.16f)
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = if (isCredit) "⬆️" else "💎",
                                                    fontSize = 14.sp
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text(
                                                    text = tx.tierName.ifBlank { "VIP Vault Refill" },
                                                    color = NoiseWhite,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = "$timeFormatted • Balance: ${formatCoins(tx.balanceAfter)}",
                                                    color = NoiseTextSecondary,
                                                    fontSize = 11.sp
                                                )
                                            }
                                        }

                                        Text(
                                            text = "$amountText 🪙",
                                            color = amountColor,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // VIP 1 TO VIP 10 LUXURY GEMSTONE BADGE COLLECTION SHOP
                if (vipBadgeCatalog.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = NoiseBorder, thickness = 0.8.dp)
                    Spacer(modifier = Modifier.height(14.dp))
                    VipLuxuryBadgeShopSection(
                        catalog = vipBadgeCatalog,
                        userCoins = coins,
                        ownedVipBadgeLevels = ownedLevels,
                        activeVipBadgeLevel = activeVipLevel,
                        onPurchaseBadge = onPurchaseVipBadge,
                        onEquipBadge = onSelectActiveVipBadge
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = NoiseBorder, thickness = 0.8.dp)
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Current Coins Balance: ",
                        color = NoiseTextSecondary,
                        fontSize = 13.sp
                    )
                    Text(
                        text = formatCoins(coins),
                        color = VipGold,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
fun NoiseDirectScreen(
    username: String,
    conversationsState: UiState<List<Conversation>>,
    activeConversation: Conversation?,
    activeMessagesState: UiState<List<DirectMessage>>,
    currentUserId: String,
    registeredAccounts: List<com.example.data.UserProfile> = emptyList(),
    onStartNewChatWithUser: (com.example.data.UserProfile) -> Unit = {},
    onSelectConversation: (Conversation?) -> Unit,
    onSendMessage: (text: String) -> Unit,
    onSendRichMessage: (
        text: String,
        messageType: String,
        mediaKey: String,
        mediaType: String,
        voiceDurationSec: Long,
        voiceWaveform: String,
        replyToMessage: DirectMessage?,
        voiceAudioBase64: String,
        voiceMimeType: String
    ) -> Unit = { text, _, _, _, _, _, _, _, _ -> onSendMessage(text) },
    onFetchVoiceRecording: suspend (String) -> com.example.data.VoiceRecordingDoc? = { null },
    onTypingChanged: (Boolean) -> Unit = {},
    onTogglePeerOnline: () -> Unit = {},
    onReactMessage: (DirectMessage, String) -> Unit = { _, _ -> },
    onTogglePinMessage: (DirectMessage) -> Unit = {},
    onDeleteMessage: (DirectMessage) -> Unit = {},
    onMarkVoiceSaved: (DirectMessage) -> Unit = {},
    onMarkMessagesSeen: (List<DirectMessage>) -> Unit = {},
    onRetryMessage: (DirectMessage) -> Unit = {},
    onStartCall: (Conversation, String) -> Unit = { _, _ -> },
    onSelectChatTheme: (Conversation, String) -> Unit = { _, _ -> },
    onClose: () -> Unit
) {
    BackHandler {
        if (activeConversation != null) {
            onSelectConversation(null)
        } else {
            onClose()
        }
    }

    var searchQuery by remember { mutableStateOf("") }
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Primary, 1 = General
    var showNewChatPicker by remember { mutableStateOf(false) }
    val conversations = (conversationsState as? UiState.Success)?.data ?: emptyList()

    // Keep activeConversation synced with latest Firestore snapshot
    val liveActiveConversation = remember(conversations, activeConversation) {
        if (activeConversation == null) null
        else conversations.find { it.id == activeConversation.id } ?: activeConversation
    }

    val filteredConversations = remember(conversations, searchQuery, selectedTab) {
        val tabName = if (selectedTab == 0) "Primary" else "General"
        conversations.filter { conv ->
            val matchesTab = selectedTab == 0 || conv.tabCategory.equals(tabName, ignoreCase = true)
            val matchesQuery = searchQuery.isBlank() ||
                conv.peerDisplayName.contains(searchQuery, ignoreCase = true) ||
                conv.peerUsername.contains(searchQuery, ignoreCase = true) ||
                conv.lastMessage.contains(searchQuery, ignoreCase = true)
            matchesTab && matchesQuery
        }
    }

    val logoGradient = Brush.horizontalGradient(listOf(NoisePink, NoisePurple))

    if (liveActiveConversation != null) {
        DirectChatConversationView(
            conversation = liveActiveConversation,
            messagesState = activeMessagesState,
            currentUserId = currentUserId,
            onBack = { onSelectConversation(null) },
            onSendRichMessage = onSendRichMessage,
            onFetchVoiceRecording = onFetchVoiceRecording,
            onTypingChanged = onTypingChanged,
            onTogglePeerOnline = onTogglePeerOnline,
            onReactMessage = onReactMessage,
            onTogglePinMessage = onTogglePinMessage,
            onDeleteMessage = onDeleteMessage,
            onMarkVoiceSaved = onMarkVoiceSaved,
            onMarkMessagesSeen = onMarkMessagesSeen,
            onRetryMessage = onRetryMessage,
            onStartCall = { callType -> onStartCall(liveActiveConversation, callType) },
            onSelectChatTheme = { themeKey -> onSelectChatTheme(liveActiveConversation, themeKey) }
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NoiseBlack)
            .testTag("noise_direct_screen")
    ) {
        // Header: <- | Noise Direct (Pacifico cursive) | @sebin_mr pill | + | X matching Screenshot 4
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.testTag("direct_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = NoiseTextSecondary
                    )
                }

                Text(
                    text = "Noise Direct",
                    style = TextStyle(
                        fontFamily = PacificoFontFamily,
                        brush = logoGradient,
                        fontSize = 23.sp
                    )
                )

                Spacer(modifier = Modifier.width(10.dp))

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(NoiseCardElevated)
                        .border(1.dp, NoiseBorder, RoundedCornerShape(16.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "@$username",
                        color = NoiseTextSecondary,
                        fontSize = 12.sp
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { showNewChatPicker = !showNewChatPicker },
                    modifier = Modifier.testTag("direct_new_chat_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "New Chat",
                        tint = NoiseWhite
                    )
                }
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.testTag("direct_close_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Direct",
                        tint = NoiseTextSecondary
                    )
                }
            }
        }

        HorizontalDivider(color = NoiseBorder.copy(alpha = 0.6f), thickness = 0.8.dp)

        val isLight = ThemeManager.isLightMode
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search Messages",
                    tint = NoiseTextSecondary,
                    modifier = Modifier.size(19.dp)
                )
            },
            placeholder = {
                Text(
                    text = "Search direct messages...",
                    color = NoiseTextMuted,
                    fontSize = 14.sp
                )
            },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp)
                .testTag("direct_search_input"),
            shape = RoundedCornerShape(22.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = if (isLight) Color(0xFFFFFFFF) else Color(0xFF121217),
                unfocusedContainerColor = if (isLight) Color(0xFFFFFFFF) else Color(0xFF121217),
                focusedBorderColor = NoisePink,
                unfocusedBorderColor = NoiseBorder,
                focusedTextColor = NoiseNormalText,
                unfocusedTextColor = NoiseNormalText
            )
        )

        Row(modifier = Modifier.fillMaxWidth()) {
            val tabs = listOf("Primary", "General")
            tabs.forEachIndexed { index, title ->
                val isSelected = selectedTab == index
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { selectedTab = index }
                        .padding(top = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = title,
                        color = if (isSelected) NoiseWhite else NoiseTextMuted,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.75f)
                            .height(2.5.dp)
                            .background(if (isSelected) NoisePink else Color.Transparent)
                    )
                }
            }
        }
        HorizontalDivider(color = NoiseBorder.copy(alpha = 0.6f), thickness = 0.8.dp)

        if (showNewChatPicker && registeredAccounts.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (isLight) Color(0xFFF8F8FC) else Color(0xFF12111A))
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Text(
                    text = "Start a conversation with a registered account:",
                    color = VipGold,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                registeredAccounts.take(6).forEach { acct ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                showNewChatPicker = false
                                onStartNewChatWithUser(acct)
                            }
                            .padding(vertical = 8.dp, horizontal = 6.dp)
                            .testTag("start_dm_${acct.username}"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            NoiseAvatar(
                                avatarKey = acct.avatarKey.ifBlank { acct.username.take(1).uppercase() },
                                size = 36.dp,
                                showOnlineDot = acct.online
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = acct.displayName.ifBlank { acct.username },
                                    color = NoiseWhite,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = "@${acct.username}",
                                    color = NoiseTextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }
                        Text(
                            text = "Message",
                            color = NoisePink,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
            HorizontalDivider(color = NoiseBorder.copy(alpha = 0.6f), thickness = 0.8.dp)
        }

        if (filteredConversations.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 28.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "No Direct Messages Yet",
                    color = NoiseWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Only real user-to-user conversations appear here. Tap '+' above or select a registered account to start chatting.",
                    color = NoiseTextSecondary,
                    fontSize = 13.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                if (registeredAccounts.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(logoGradient)
                            .clickable {
                                onStartNewChatWithUser(registeredAccounts.first())
                            }
                            .padding(horizontal = 20.dp, vertical = 10.dp)
                            .testTag("direct_start_first_chat_button")
                    ) {
                        Text(
                            text = "Message @${registeredAccounts.first().username}",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(filteredConversations, key = { it.id }) { conv ->
                    ConversationListRow(
                        conversation = conv,
                        onClick = { onSelectConversation(conv) }
                    )
                    HorizontalDivider(color = NoiseBorder.copy(alpha = 0.35f), thickness = 0.6.dp)
                }
            }
        }
    }
}

@Composable
private fun ConversationListRow(
    conversation: Conversation,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .testTag("conversation_row_${conversation.peerUsername}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            NoiseAvatar(
                avatarKey = conversation.peerAvatarKey,
                size = 54.dp,
                showOnlineDot = conversation.peerOnline
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val peerVip = conversation.peerVipBadgeLevel.toInt().coerceIn(0, 10)
                    VipAnimatedUsername(
                        username = conversation.peerDisplayName.ifBlank { conversation.peerUsername },
                        vipLevel = peerVip,
                        fontSize = 15.sp
                    )
                    if (peerVip > 0) {
                        Spacer(modifier = Modifier.width(5.dp))
                        VipInlineGemBadge(
                            vipLevel = peerVip,
                            gemSize = 18.dp,
                            showLabel = true
                        )
                    }
                    if (conversation.peerVerified) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.Verified,
                            contentDescription = "Verified",
                            tint = VerifiedBlue,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                if (conversation.peerTyping) {
                    Text(
                        text = "Typing...",
                        color = NoisePink,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (conversation.lastMessage.startsWith("✓ ")) {
                            Icon(
                                imageVector = Icons.Default.DoneAll,
                                contentDescription = "Seen",
                                tint = if (conversation.peerOnline) OnlineGreen else NoiseTextSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = conversation.lastMessage.removePrefix("✓ "),
                                color = NoiseTextSecondary,
                                fontSize = 14.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        } else {
                            Text(
                                text = conversation.lastMessage,
                                color = NoiseTextSecondary,
                                fontSize = 14.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = conversation.lastTimeLabel,
                color = NoiseTextMuted,
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (conversation.peerOnline) "Online" else conversation.peerLastSeen.ifBlank { "Offline" },
                color = if (conversation.peerOnline) OnlineGreen else NoiseTextMuted,
                fontSize = 10.sp
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DirectChatConversationView(
    conversation: Conversation,
    messagesState: UiState<List<DirectMessage>>,
    currentUserId: String,
    onBack: () -> Unit,
    onSendRichMessage: (
        text: String,
        messageType: String,
        mediaKey: String,
        mediaType: String,
        voiceDurationSec: Long,
        voiceWaveform: String,
        replyToMessage: DirectMessage?,
        voiceAudioBase64: String,
        voiceMimeType: String
    ) -> Unit,
    onFetchVoiceRecording: suspend (String) -> com.example.data.VoiceRecordingDoc? = { null },
    onTypingChanged: (Boolean) -> Unit,
    onTogglePeerOnline: () -> Unit,
    onReactMessage: (DirectMessage, String) -> Unit,
    onTogglePinMessage: (DirectMessage) -> Unit,
    onDeleteMessage: (DirectMessage) -> Unit,
    onMarkVoiceSaved: (DirectMessage) -> Unit,
    onMarkMessagesSeen: (List<DirectMessage>) -> Unit,
    onRetryMessage: (DirectMessage) -> Unit = {},
    onStartCall: (String) -> Unit = {},
    onSelectChatTheme: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val voiceController = remember { VoiceAudioController(context) }

    DisposableEffect(Unit) {
        onDispose {
            voiceController.stopPlayback()
            voiceController.cancelRecording()
        }
    }

    var messageInput by remember { mutableStateOf("") }
    var replyingToMessage by remember { mutableStateOf<DirectMessage?>(null) }
    var selectedActionMessage by remember { mutableStateOf<DirectMessage?>(null) }
    var localFeedbackToast by remember { mutableStateOf<String?>(null) }
    var highlightedMessageId by remember { mutableStateOf<String?>(null) }
    var activePinnedCursor by remember { mutableIntStateOf(0) }
    var showChatThemeDialog by remember { mutableStateOf(false) }

    // Voice Recording State
    var isRecordingVoice by remember { mutableStateOf(false) }
    var isRecordingPaused by remember { mutableStateOf(false) }
    var isPreviewingVoice by remember { mutableStateOf(false) }
    var isUploadingVoice by remember { mutableStateOf(false) }
    var recordedDurationSec by remember { mutableIntStateOf(0) }
    var recordedFilePath by remember { mutableStateOf("") }
    val recordedAmplitudes = remember { mutableStateListOf<Int>() }

    // Voice Playback State
    var playingMessageId by remember { mutableStateOf<String?>(null) }
    var isPlaybackPaused by remember { mutableStateOf(false) }
    var playbackSpeed by remember { mutableFloatStateOf(1.0f) }
    var playbackProgress by remember { mutableFloatStateOf(0f) }

    val messages = (messagesState as? UiState.Success)?.data ?: emptyList()
    val pinnedMessages = remember(messages, conversation.pinnedMessageIds) {
        messages.filter { it.pinned || it.id in conversation.pinnedMessageIds }
    }

    // Mark incoming messages as seen when viewing conversation
    LaunchedEffect(messages.size, conversation.id) {
        if (messages.isNotEmpty()) {
            onMarkMessagesSeen(messages)
            listState.animateScrollToItem((messages.size - 1).coerceAtLeast(0))
        }
    }

    // Automatic typing indicator debounce
    LaunchedEffect(messageInput) {
        if (messageInput.isNotBlank()) {
            onTypingChanged(true)
            delay(2200)
            onTypingChanged(false)
        } else {
            onTypingChanged(false)
        }
    }

    // Live Voice Recording Timer & Waveform Sampler
    LaunchedEffect(isRecordingVoice, isRecordingPaused) {
        var tick = 0
        while (isRecordingVoice) {
            delay(250)
            if (!isRecordingPaused) {
                tick += 1
                if (tick % 4 == 0) {
                    recordedDurationSec += 1
                }
                val amp = voiceController.sampleAmplitude(tick)
                if (recordedAmplitudes.size >= 28) {
                    recordedAmplitudes.removeAt(0)
                }
                recordedAmplitudes.add(amp)
            }
        }
    }

    // Auto-clear feedback toast
    LaunchedEffect(localFeedbackToast) {
        if (localFeedbackToast != null) {
            delay(2500)
            localFeedbackToast = null
        }
    }

    val recordAudioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        recordedDurationSec = 0
        recordedAmplitudes.clear()
        isRecordingPaused = false
        isPreviewingVoice = false
        val file = voiceController.startRecording()
        recordedFilePath = file.absolutePath
        isRecordingVoice = true
    }

    val chatMediaPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val (persistedKey, detectedType) = persistPickedMediaUri(context, uri)
            val isVideo = detectedType == "video"
            onSendRichMessage(
                if (isVideo) "🎬 Shared a video clip" else "📷 Shared a photo",
                if (isVideo) "video" else "image",
                persistedKey,
                if (isVideo) "video" else "photo",
                0L,
                "",
                replyingToMessage,
                "",
                ""
            )
            replyingToMessage = null
        }
    }

    val gradientBrush = Brush.horizontalGradient(listOf(NoisePink, NoisePurple))
    val reactionEmojis = listOf("❤️", "😂", "😮", "😢", "🔥", "👏")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NoiseBlack)
            .testTag("direct_chat_view")
    ) {
        // 1. Top Conversation Bar with Online/Offline status, Last Seen, and Typing controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back to Inbox",
                        tint = NoiseWhite
                    )
                }
                NoiseAvatar(
                    avatarKey = conversation.peerAvatarKey,
                    size = 40.dp,
                    showOnlineDot = conversation.peerOnline
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val peerVip = conversation.peerVipBadgeLevel.toInt().coerceIn(0, 10)
                        VipAnimatedUsername(
                            username = conversation.peerDisplayName.ifBlank { conversation.peerUsername },
                            vipLevel = peerVip,
                            fontSize = 15.sp
                        )
                        if (peerVip > 0) {
                            Spacer(modifier = Modifier.width(5.dp))
                            VipInlineGemBadge(
                                vipLevel = peerVip,
                                gemSize = 18.dp,
                                showLabel = true
                            )
                        }
                        if (conversation.peerVerified) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.Verified,
                                contentDescription = "Verified",
                                tint = VerifiedBlue,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                    val statusSubtitle = when {
                        conversation.peerTyping -> "Typing..."
                        conversation.peerOnline -> "Online"
                        else -> conversation.peerLastSeen.ifBlank { "Last seen recently" }
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        modifier = Modifier.testTag("chat_automatic_presence_subtitle")
                    ) {
                        if (conversation.peerOnline && !conversation.peerTyping) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(OnlineGreen)
                            )
                        }
                        Text(
                            text = statusSubtitle,
                            color = when {
                                conversation.peerTyping -> NoisePink
                                conversation.peerOnline -> OnlineGreen
                                else -> NoiseTextSecondary
                            },
                            fontSize = 11.sp,
                            fontWeight = if (conversation.peerTyping) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            // Right Header Actions: Audio Call | Video Call | Chat Background Theme
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(NoiseCard)
                        .border(1.dp, OnlineGreen.copy(alpha = 0.65f), CircleShape)
                        .clickable { onStartCall("audio") }
                        .testTag("dm_audio_call_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Call,
                        contentDescription = "Audio Call",
                        tint = OnlineGreen,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(NoiseCard)
                        .border(1.dp, NoisePurple.copy(alpha = 0.75f), CircleShape)
                        .clickable { onStartCall("video") }
                        .testTag("dm_video_call_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Videocam,
                        contentDescription = "Video Call",
                        tint = NoisePurple,
                        modifier = Modifier.size(19.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(NoiseCard)
                        .border(1.dp, NoisePink.copy(alpha = 0.7f), CircleShape)
                        .clickable { showChatThemeDialog = true }
                        .testTag("dm_chat_theme_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = "Chat Theme",
                        tint = NoisePink,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
        HorizontalDivider(color = NoiseBorder.copy(alpha = 0.6f), thickness = 0.8.dp)

        // 2. Pinned Messages Banner at Top of Conversation (supports multiple pinned messages & jump-to-message)
        if (pinnedMessages.isNotEmpty()) {
            val safeIdx = activePinnedCursor % pinnedMessages.size
            val pinnedMsg = pinnedMessages[safeIdx]
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (ThemeManager.isLightMode) LightLavender else Color(0xFF14121D))
                    .clickable {
                        val targetIndex = messages.indexOfFirst { it.id == pinnedMsg.id }
                        if (targetIndex >= 0) {
                            highlightedMessageId = pinnedMsg.id
                            scope.launch {
                                listState.animateScrollToItem(targetIndex)
                            }
                        }
                        activePinnedCursor = (activePinnedCursor + 1) % pinnedMessages.size
                    }
                    .padding(horizontal = 14.dp, vertical = 9.dp)
                    .testTag("pinned_messages_banner"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.PushPin,
                        contentDescription = "Pinned",
                        tint = VipGold,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Pinned Message (${safeIdx + 1}/${pinnedMessages.size}) • Tap to locate",
                            color = VipGold,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                        Text(
                            text = pinnedMsg.text,
                            color = NoiseWhite,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                IconButton(
                    onClick = { onTogglePinMessage(pinnedMsg) },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Unpin Message",
                        tint = NoiseTextSecondary,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
            HorizontalDivider(color = NoiseBorder.copy(alpha = 0.5f), thickness = 0.6.dp)
        }

        // Local Toast Banner (e.g. Copied to clipboard / Saved voice recording locally)
        if (localFeedbackToast != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(NoisePink.copy(alpha = 0.2f))
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = localFeedbackToast ?: "",
                    color = NoiseWhite,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // 3. Chat History Message Stream with Shared Customizable Background Theme
        ChatThemeBackgroundBox(
            themeKey = conversation.chatThemeKey,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                itemsIndexed(messages, key = { _, msg -> msg.id }) { index, msg ->
                val isMine = msg.senderId == currentUserId
                val isLastMessage = index == messages.lastIndex
                val isHighlighted = highlightedMessageId == msg.id

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (isHighlighted) {
                                Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(VipGold.copy(alpha = 0.12f))
                                    .padding(4.dp)
                            } else Modifier
                        ),
                    horizontalAlignment = if (isMine) Alignment.End else Alignment.Start
                ) {
                    // Quoted Reply Preview Header if this message replies to an earlier message
                    if (msg.replyToText.isNotBlank()) {
                        Column(
                            modifier = Modifier
                                .padding(bottom = 4.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (ThemeManager.isLightMode) Color(0xFFF8F8FC) else Color(0xFF191824))
                                .border(1.dp, NoiseBorder, RoundedCornerShape(12.dp))
                                .clickable {
                                    val replyIdx = messages.indexOfFirst { it.id == msg.replyToMessageId }
                                    if (replyIdx >= 0) {
                                        highlightedMessageId = msg.replyToMessageId
                                        scope.launch { listState.animateScrollToItem(replyIdx) }
                                    }
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "↩ Replied to ${msg.replyToSenderName.ifBlank { "message" }}",
                                color = NoisePink,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = msg.replyToText,
                                color = NoiseTextSecondary,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start
                    ) {
                        if (!isMine) {
                            NoiseAvatar(
                                avatarKey = conversation.peerAvatarKey,
                                size = 28.dp,
                                showOnlineDot = conversation.peerOnline
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }

                        Box {
                            Column(
                                modifier = Modifier
                                    .widthIn(max = 285.dp)
                                    .clip(
                                        RoundedCornerShape(
                                            topStart = 18.dp,
                                            topEnd = 18.dp,
                                            bottomStart = if (isMine) 18.dp else 4.dp,
                                            bottomEnd = if (isMine) 4.dp else 18.dp
                                        )
                                    )
                                    .background(
                                        if (isMine) gradientBrush
                                        else Brush.horizontalGradient(listOf(NoiseCardElevated, NoiseCardElevated))
                                    )
                                    .combinedClickable(
                                        onClick = {
                                            selectedActionMessage = if (selectedActionMessage?.id == msg.id) null else msg
                                        },
                                        onLongClick = {
                                            selectedActionMessage = msg
                                        }
                                    )
                                    .padding(horizontal = 14.dp, vertical = 10.dp)
                            ) {
                                // Pinned Indicator inside bubble
                                if (msg.pinned) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(bottom = 4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PushPin,
                                            contentDescription = "Pinned",
                                            tint = VipGold,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Pinned",
                                            color = VipGold,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                val bubbleTextColor = if (isMine) Color.White else NoiseNormalText
                                when (msg.messageType) {
                                    "voice" -> {
                                        VoiceMessageBubbleContent(
                                            message = msg,
                                            isPlaying = playingMessageId == msg.id && !isPlaybackPaused,
                                            isPaused = playingMessageId == msg.id && isPlaybackPaused,
                                            playbackProgress = if (playingMessageId == msg.id) playbackProgress else 0f,
                                            playbackSpeed = playbackSpeed,
                                            onPlayToggle = {
                                                if (playingMessageId == msg.id && !isPlaybackPaused) {
                                                    voiceController.pausePlayback()
                                                    isPlaybackPaused = true
                                                } else if (playingMessageId == msg.id && isPlaybackPaused) {
                                                    voiceController.resumePlayback(playbackSpeed)
                                                    isPlaybackPaused = false
                                                } else {
                                                    scope.launch {
                                                        playingMessageId = msg.id
                                                        isPlaybackPaused = false
                                                        playbackProgress = 0f
                                                        var resolvedPath = msg.mediaKey
                                                        val localExists = resolvedPath.isNotBlank() &&
                                                            !resolvedPath.startsWith("firestore://") &&
                                                            java.io.File(resolvedPath).let { it.exists() && it.length() > 44L }
                                                        if (!localExists) {
                                                            val cloudRec = onFetchVoiceRecording(msg.id)
                                                            if (cloudRec != null && cloudRec.audioBase64.isNotBlank()) {
                                                                val file = voiceController.materializeBase64ToPlayableFile(
                                                                    recordingId = msg.id,
                                                                    audioBase64 = cloudRec.audioBase64,
                                                                    mimeType = cloudRec.mimeType,
                                                                    durationSec = msg.voiceDurationSec.coerceAtLeast(1L).toInt()
                                                                )
                                                                resolvedPath = file.absolutePath
                                                            }
                                                        }
                                                        voiceController.playAudioFile(
                                                            path = resolvedPath,
                                                            speed = playbackSpeed,
                                                            durationHintSec = msg.voiceDurationSec.coerceAtLeast(1L).toInt(),
                                                            onProgress = { progress ->
                                                                if (playingMessageId == msg.id) {
                                                                    playbackProgress = progress
                                                                }
                                                            },
                                                            onCompletion = {
                                                                playingMessageId = null
                                                                isPlaybackPaused = false
                                                                playbackProgress = 0f
                                                            }
                                                        )
                                                    }
                                                }
                                            },
                                            onCycleSpeed = {
                                                playbackSpeed = when (playbackSpeed) {
                                                    1.0f -> 1.5f
                                                    1.5f -> 2.0f
                                                    else -> 1.0f
                                                }
                                                if (playingMessageId == msg.id && !isPlaybackPaused) {
                                                    voiceController.resumePlayback(playbackSpeed)
                                                }
                                            },
                                            onSaveLocally = {
                                                scope.launch {
                                                    var resolvedPath = msg.mediaKey
                                                    val localExists = resolvedPath.isNotBlank() &&
                                                        !resolvedPath.startsWith("firestore://") &&
                                                        java.io.File(resolvedPath).let { it.exists() && it.length() > 44L }
                                                    if (!localExists) {
                                                        val cloudRec = onFetchVoiceRecording(msg.id)
                                                        if (cloudRec != null && cloudRec.audioBase64.isNotBlank()) {
                                                            val file = voiceController.materializeBase64ToPlayableFile(
                                                                recordingId = msg.id,
                                                                audioBase64 = cloudRec.audioBase64,
                                                                mimeType = cloudRec.mimeType,
                                                                durationSec = msg.voiceDurationSec.coerceAtLeast(1L).toInt()
                                                            )
                                                            resolvedPath = file.absolutePath
                                                        }
                                                    }
                                                    val savedFile = voiceController.saveRecordingToLocalVault(
                                                        sourcePath = resolvedPath,
                                                        messageId = msg.id,
                                                        durationSec = msg.voiceDurationSec
                                                    )
                                                    onMarkVoiceSaved(msg)
                                                    localFeedbackToast = "Saved voice recording: ${savedFile.name}"
                                                }
                                            }
                                        )
                                    }

                                    "image", "video" -> {
                                        val effectiveMedia = msg.mediaUrl.ifBlank { msg.mediaKey }
                                        if (effectiveMedia.isNotBlank()) {
                                            if (msg.messageType == "video" || msg.mediaType == "video") {
                                                NoiseVideoPlayer(
                                                    mediaKey = effectiveMedia,
                                                    contentDescription = msg.text,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(175.dp)
                                                        .clip(RoundedCornerShape(12.dp))
                                                )
                                            } else {
                                                NoiseMediaImage(
                                                    mediaKey = effectiveMedia,
                                                    contentDescription = msg.text,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(175.dp)
                                                        .clip(RoundedCornerShape(12.dp))
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(6.dp))
                                        }
                                        Text(
                                            text = msg.text,
                                            color = bubbleTextColor,
                                            fontSize = 14.sp
                                        )
                                    }

                                    "call" -> {
                                        CallEventMessageBubble(
                                            message = msg,
                                            onCallBack = { callType -> onStartCall(callType) }
                                        )
                                    }

                                    else -> {
                                        Text(
                                            text = msg.text,
                                            color = bubbleTextColor,
                                            fontSize = 14.sp,
                                            lineHeight = 19.sp
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                // Timestamp & Exact 4-State Tick System (Sending = clock, Sent = 1 grey tick, Delivered = 2 grey ticks, Seen = 2 blue ticks)
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.End,
                                    modifier = Modifier.align(Alignment.End)
                                ) {
                                    Text(
                                        text = msg.timeLabel.ifBlank { "Just now" },
                                        color = bubbleTextColor.copy(alpha = 0.72f),
                                        fontSize = 10.sp
                                    )
                                    if (isMine) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        val isFailed = msg.deliveryStatus == "failed" || msg.status == "failed"
                                        val isSending = !isFailed && (msg.deliveryStatus == "sending" || msg.status == "sending")
                                        val isSeen = !isFailed && !isSending && (msg.read || msg.deliveryStatus == "seen" || msg.status == "read")
                                        val isDelivered = !isFailed && !isSending && !isSeen && (msg.deliveryStatus == "delivered" || msg.status == "delivered")
                                        val tickIcon = when {
                                            isSending -> Icons.Default.Schedule
                                            isSeen || isDelivered -> Icons.Default.DoneAll
                                            else -> Icons.Default.Check
                                        }
                                        val tickColor = when {
                                            isFailed -> Color(0xFFFF5252)
                                            isSending -> VipGold
                                            isSeen -> Color(0xFF38BDF8) // Bright Blue ticks for Seen/Read
                                            isDelivered -> Color(0xFFD1D5DB) // Light Grey double ticks for Delivered
                                            else -> Color(0xFF9CA3AF) // Single Grey tick for Sent
                                        }
                                        val tickDesc = when {
                                            isFailed -> "failed"
                                            isSending -> "sending"
                                            isSeen -> "seen"
                                            isDelivered -> "delivered"
                                            else -> "sent"
                                        }
                                        Icon(
                                            imageVector = tickIcon,
                                            contentDescription = tickDesc,
                                            tint = tickColor,
                                            modifier = Modifier
                                                .size(14.dp)
                                                .testTag("message_tick_${msg.id}_$tickDesc")
                                        )
                                    }
                                }
                            }

                            // Emoji Reaction Badge on corner of message bubble
                            if (msg.reactionEmoji.isNotBlank()) {
                                Box(
                                    modifier = Modifier
                                        .align(if (isMine) Alignment.BottomStart else Alignment.BottomEnd)
                                        .offset(x = if (isMine) (-8).dp else 8.dp, y = 8.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(NoiseCard)
                                        .border(1.dp, NoiseBorder, RoundedCornerShape(12.dp))
                                        .clickable { onReactMessage(msg, msg.reactionEmoji) }
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(text = msg.reactionEmoji, fontSize = 12.sp)
                                }
                            }
                        }
                    }

                    // Failed Message Retry Button
                    if (isMine && (msg.deliveryStatus == "failed" || msg.status == "failed")) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (ThemeManager.isLightMode) Color(0xFFFEF2F2) else Color(0xFF3B0D1E))
                                .border(1.dp, Color(0xFFFF5252), RoundedCornerShape(12.dp))
                                .clickable { onRetryMessage(msg) }
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                                .testTag("retry_message_${msg.id}"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "⚠️ Failed to send • Tap to Retry",
                                color = if (ThemeManager.isLightMode) Color(0xFFDC2626) else Color(0xFFFF8A80),
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    } else if (isMine && isLastMessage) {
                        // 4-State Status Label below latest outgoing message
                        Spacer(modifier = Modifier.height(4.dp))
                        val isSending = msg.deliveryStatus == "sending" || msg.status == "sending"
                        val isSeen = !isSending && (msg.read || msg.deliveryStatus == "seen" || msg.status == "read")
                        val isDelivered = !isSending && !isSeen && (msg.deliveryStatus == "delivered" || msg.status == "delivered")
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            when {
                                isSending -> {
                                    Text(
                                        text = "Sending...",
                                        color = VipGold,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                isSeen -> {
                                    NoiseAvatar(
                                        avatarKey = conversation.peerAvatarKey,
                                        size = 15.dp
                                    )
                                    Text(
                                        text = "Seen ✓✓",
                                        color = Color(0xFF38BDF8),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                isDelivered -> {
                                    Text(
                                        text = "Delivered ✓✓",
                                        color = NoiseTextSecondary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                else -> {
                                    Text(
                                        text = "Sent ✓",
                                        color = NoiseTextMuted,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }

                    // Interactive Message Context Action Bar (Reactions, Reply, Pin/Unpin, Copy, Delete)
                    if (selectedActionMessage?.id == msg.id) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Column(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(NoiseCard)
                                .border(1.dp, NoiseBorder, RoundedCornerShape(16.dp))
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Emoji Reactions Strip
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                reactionEmojis.forEach { emoji ->
                                    Text(
                                        text = emoji,
                                        fontSize = 18.sp,
                                        modifier = Modifier.clickable {
                                            onReactMessage(msg, emoji)
                                            selectedActionMessage = null
                                        }
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            HorizontalDivider(color = NoiseBorder)
                            Spacer(modifier = Modifier.height(6.dp))
                            // Action Buttons: Reply | Pin/Unpin | Copy | Delete
                            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.clickable {
                                        replyingToMessage = msg
                                        selectedActionMessage = null
                                    }
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.Reply, contentDescription = "Reply", tint = NoiseWhite, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text("Reply", color = NoiseWhite, fontSize = 11.sp)
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.clickable {
                                        onTogglePinMessage(msg)
                                        selectedActionMessage = null
                                    }
                                ) {
                                    Icon(Icons.Default.PushPin, contentDescription = "Pin", tint = VipGold, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(if (msg.pinned) "Unpin" else "Pin", color = VipGold, fontSize = 11.sp)
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.clickable {
                                        clipboardManager.setText(AnnotatedString(msg.text))
                                        localFeedbackToast = "Message copied to clipboard"
                                        selectedActionMessage = null
                                    }
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = NoiseTextSecondary, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text("Copy", color = NoiseTextSecondary, fontSize = 11.sp)
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.clickable {
                                        onDeleteMessage(msg)
                                        selectedActionMessage = null
                                    }
                                ) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = NoisePink, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text("Delete", color = NoisePink, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }

            // Live Peer Typing Bubble inside Chat Stream
            if (conversation.peerTyping) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        NoiseAvatar(avatarKey = conversation.peerAvatarKey, size = 26.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(NoiseCardElevated)
                                .border(1.dp, NoisePink.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "••• ${conversation.peerUsername} is typing...",
                                color = NoisePink,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
        }

        if (showChatThemeDialog) {
            ChatThemeSelectorDialog(
                currentThemeKey = conversation.chatThemeKey,
                peerDisplayName = conversation.peerDisplayName.ifBlank { conversation.peerUsername },
                onSelectAndApplyTheme = { themeKey ->
                    onSelectChatTheme(themeKey)
                },
                onDismiss = { showChatThemeDialog = false }
            )
        }

        // 4. Reply Preview Banner above Input Bar
        if (replyingToMessage != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (ThemeManager.isLightMode) LightLavender else Color(0xFF15141F))
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Replying to ${replyingToMessage?.senderName}",
                        color = NoisePink,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                    Text(
                        text = replyingToMessage?.text ?: "",
                        color = NoiseTextSecondary,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(
                    onClick = { replyingToMessage = null },
                    modifier = Modifier.size(26.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Cancel Reply", tint = NoiseTextSecondary, modifier = Modifier.size(16.dp))
                }
            }
        }

        // 5. Voice Recording & Preview Panel (when recording or previewing a voice note)
        if (isRecordingVoice || isPreviewingVoice || isUploadingVoice) {
            VoiceRecorderControlBar(
                isRecording = isRecordingVoice,
                isPaused = isRecordingPaused,
                isPreviewing = isPreviewingVoice,
                isUploading = isUploadingVoice,
                durationSec = recordedDurationSec.coerceAtLeast(1),
                amplitudes = recordedAmplitudes,
                onPauseResume = {
                    if (isRecordingPaused) {
                        voiceController.resumeRecording()
                        isRecordingPaused = false
                    } else {
                        voiceController.pauseRecording()
                        isRecordingPaused = true
                    }
                },
                onPreview = {
                    if (isRecordingVoice) {
                        recordedFilePath = voiceController.finishRecording(recordedDurationSec.coerceAtLeast(1))
                        isRecordingVoice = false
                    }
                    isPreviewingVoice = true
                    voiceController.playAudioFile(
                        path = recordedFilePath,
                        speed = 1.0f,
                        durationHintSec = recordedDurationSec.coerceAtLeast(1),
                        onCompletion = {}
                    )
                },
                onCancel = {
                    voiceController.cancelRecording()
                    voiceController.stopPlayback()
                    isRecordingVoice = false
                    isRecordingPaused = false
                    isPreviewingVoice = false
                    isUploadingVoice = false
                    recordedDurationSec = 0
                    recordedAmplitudes.clear()
                },
                onSendVoice = {
                    scope.launch {
                        isUploadingVoice = true
                        val finalDur = recordedDurationSec.coerceAtLeast(1).toLong()
                        val finalPath = if (isRecordingVoice) {
                            voiceController.finishRecording(finalDur.toInt())
                        } else {
                            recordedFilePath
                        }
                        val waveformCsv = if (recordedAmplitudes.isEmpty()) {
                            "30,55,80,45,70,90,60,40,75,50,85,65"
                        } else {
                            recordedAmplitudes.joinToString(",")
                        }
                        val audioBase64 = voiceController.encodeFileToBase64(finalPath, finalDur.toInt())
                        val mimeType = if (finalPath.endsWith(".m4a", ignoreCase = true)) "audio/mp4" else "audio/wav"

                        isRecordingVoice = false
                        isRecordingPaused = false
                        isPreviewingVoice = false
                        isUploadingVoice = false
                        recordedDurationSec = 0
                        recordedAmplitudes.clear()

                        onSendRichMessage(
                            "🎤 Voice message (${finalDur}s)",
                            "voice",
                            finalPath,
                            "audio",
                            finalDur,
                            waveformCsv,
                            replyingToMessage,
                            audioBase64,
                            mimeType
                        )
                        replyingToMessage = null
                    }
                }
            )
        } else {
            // Quick Emoji Strip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 14.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                listOf("❤️", "🔥", "😂", "😍", "🙌", "✨", "🎉", "🌹").forEach { quickEmoji ->
                    Text(
                        text = quickEmoji,
                        fontSize = 18.sp,
                        modifier = Modifier.clickable {
                            messageInput += quickEmoji
                        }
                    )
                }
            }

            // 6. Main Message Input Bar with Gallery Picker, Mic Recorder, Text Field, and Send
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Gallery Photo / Video Picker Button
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(NoiseCard)
                        .border(1.dp, NoiseBorder, CircleShape)
                        .clickable {
                            try {
                                chatMediaPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                                )
                            } catch (_: Exception) {
                                onSendRichMessage(
                                    "📷 Shared a photo",
                                    "image",
                                    "roses",
                                    "photo",
                                    0L,
                                    "",
                                    replyingToMessage,
                                    "",
                                    ""
                                )
                                replyingToMessage = null
                            }
                        }
                        .testTag("chat_media_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = "Send Photo or Video",
                        tint = NoisePink,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Voice Recording Mic Button
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(NoiseCard)
                        .border(1.dp, NoisePurple.copy(alpha = 0.7f), CircleShape)
                        .clickable {
                            val hasPerm = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.RECORD_AUDIO
                            ) == PackageManager.PERMISSION_GRANTED
                            if (hasPerm) {
                                recordedDurationSec = 0
                                recordedAmplitudes.clear()
                                isRecordingPaused = false
                                isPreviewingVoice = false
                                val file = voiceController.startRecording()
                                recordedFilePath = file.absolutePath
                                isRecordingVoice = true
                            } else {
                                recordAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        }
                        .testTag("chat_mic_record_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Record Voice Message",
                        tint = NoisePurple,
                        modifier = Modifier.size(20.dp)
                    )
                }

                OutlinedTextField(
                    value = messageInput,
                    onValueChange = { messageInput = it },
                    placeholder = {
                        Text(
                            text = "Message @${conversation.peerUsername}...",
                            color = NoiseTextMuted,
                            fontSize = 14.sp
                        )
                    },
                    maxLines = 4,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("chat_message_input"),
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = NoiseCard,
                        unfocusedContainerColor = NoiseCard,
                        focusedBorderColor = NoisePink,
                        unfocusedBorderColor = NoiseBorder,
                        focusedTextColor = NoiseNormalText,
                        unfocusedTextColor = NoiseNormalText
                    )
                )

                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(gradientBrush)
                        .clickable {
                            val textToSend = messageInput.trim().ifBlank { "❤️" }
                            onSendRichMessage(
                                textToSend,
                                "text",
                                "",
                                "photo",
                                0L,
                                "",
                                replyingToMessage,
                                "",
                                ""
                            )
                            messageInput = ""
                            replyingToMessage = null
                        }
                        .testTag("chat_send_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send Message",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun VoiceRecorderControlBar(
    isRecording: Boolean,
    isPaused: Boolean,
    isPreviewing: Boolean,
    isUploading: Boolean = false,
    durationSec: Int,
    amplitudes: List<Int>,
    onPauseResume: () -> Unit,
    onPreview: () -> Unit,
    onCancel: () -> Unit,
    onSendVoice: () -> Unit
) {
    val isLight = ThemeManager.isLightMode
    val mins = durationSec / 60
    val secs = durationSec % 60
    val formattedDuration = String.format(java.util.Locale.US, "%02d:%02d", mins, secs)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isLight) Color(0xFFF8F8FC) else Color(0xFF14111E))
            .border(1.dp, NoisePink.copy(alpha = 0.5f), RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .testTag("voice_recorder_bar")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(if (isPaused || isPreviewing) VipGold else NoisePink)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = when {
                        isUploading -> "Uploading Voice..."
                        isPreviewing -> "Previewing Voice ($formattedDuration)"
                        isPaused -> "Paused ($formattedDuration)"
                        else -> "Recording $formattedDuration"
                    },
                    color = NoiseWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }

            // Live Waveform Visualization
            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp)
                    .height(30.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                val displayBars = if (amplitudes.isEmpty()) {
                    listOf(25, 50, 75, 40, 85, 60, 35, 70, 90, 50, 65, 45)
                } else {
                    amplitudes.takeLast(18)
                }
                displayBars.forEach { amp ->
                    val barHeight = (amp * 0.26f).dp.coerceIn(5.dp, 28.dp)
                    Box(
                        modifier = Modifier
                            .width(3.dp)
                            .height(barHeight)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Brush.verticalGradient(listOf(NoisePink, NoisePurple)))
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Controls: Cancel | Pause/Resume | Preview | Send Voice
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Cancel button
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (isLight) Color(0xFFFEF2F2) else Color(0xFF231824))
                    .clickable { onCancel() }
                    .padding(horizontal = 12.dp, vertical = 7.dp)
                    .testTag("voice_record_cancel_button"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.DeleteOutline, contentDescription = "Cancel", tint = NoisePink, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Cancel", color = NoisePink, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            // Pause / Resume button
            if (isRecording) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isLight) Color(0xFFFFFFFF) else Color(0xFF1D1C2B))
                        .border(1.dp, NoiseBorder, RoundedCornerShape(14.dp))
                        .clickable { onPauseResume() }
                        .padding(horizontal = 12.dp, vertical = 7.dp)
                        .testTag("voice_record_pause_button"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                        contentDescription = if (isPaused) "Resume" else "Pause",
                        tint = VipGold,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isPaused) "Resume" else "Pause", color = VipGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Preview before sending
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (isLight) Color(0xFFFFFFFF) else Color(0xFF1D1C2B))
                    .border(1.dp, NoiseBorder, RoundedCornerShape(14.dp))
                    .clickable { onPreview() }
                    .padding(horizontal = 12.dp, vertical = 7.dp)
                    .testTag("voice_record_preview_button"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = "Preview", tint = NoiseWhite, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Preview", color = NoiseWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            // Send Voice Note
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(Brush.horizontalGradient(listOf(NoisePink, NoisePurple)))
                    .clickable { onSendVoice() }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("voice_record_send_button"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send Voice", tint = Color.White, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (isUploading) "Sending..." else "Send", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}

@Composable
private fun VoiceMessageBubbleContent(
    message: DirectMessage,
    isPlaying: Boolean,
    isPaused: Boolean,
    playbackProgress: Float = 0f,
    playbackSpeed: Float,
    onPlayToggle: () -> Unit,
    onCycleSpeed: () -> Unit,
    onSaveLocally: () -> Unit
) {
    val waveformBars = remember(message.voiceWaveform) {
        message.voiceWaveform.split(",")
            .mapNotNull { it.trim().toIntOrNull() }
            .ifEmpty { listOf(30, 55, 80, 45, 70, 90, 60, 40, 75, 50, 85, 65) }
    }
    val displayedBars = waveformBars.take(16)
    val activeBarThreshold = (displayedBars.size * playbackProgress.coerceIn(0f, 1f)).toInt()

    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Play / Pause Button
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.35f))
                    .clickable { onPlayToggle() }
                    .testTag("voice_msg_play_${message.id}"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause Voice" else "Play Voice",
                    tint = NoiseWhite,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Audio Waveform Bars with live progress highlighting
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(26.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.5.dp)
            ) {
                displayedBars.forEachIndexed { idx, amp ->
                    val h = (amp * 0.22f).dp.coerceIn(4.dp, 24.dp)
                    val isBarPlayed = (isPlaying || isPaused) && idx <= activeBarThreshold
                    Box(
                        modifier = Modifier
                            .width(3.dp)
                            .height(h)
                            .clip(RoundedCornerShape(1.5.dp))
                            .background(
                                if (isBarPlayed) VipGold
                                else Color.White.copy(alpha = 0.85f)
                            )
                    )
                }
            }

            // Playback Speed Control Pill (1x / 1.5x / 2x)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.Black.copy(alpha = 0.35f))
                    .clickable { onCycleSpeed() }
                    .padding(horizontal = 7.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "${playbackSpeed}x",
                    color = NoiseWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        val totalSec = message.voiceDurationSec.coerceAtLeast(1L)
        val elapsedSec = (totalSec * playbackProgress).toLong().coerceIn(0L, totalSec)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = when {
                    isPlaying -> "Playing • ${elapsedSec}s / ${totalSec}s"
                    isPaused -> "Paused • ${elapsedSec}s / ${totalSec}s"
                    else -> "Voice • ${totalSec}s"
                },
                color = NoiseWhite.copy(alpha = 0.85f),
                fontSize = 11.sp
            )

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = 0.28f))
                    .clickable { onSaveLocally() }
                    .padding(horizontal = 7.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (message.savedLocally) Icons.Default.Check else Icons.Default.Download,
                    contentDescription = "Save Voice Recording",
                    tint = if (message.savedLocally) OnlineGreen else NoiseWhite,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = if (message.savedLocally) "Saved" else "Save",
                    color = if (message.savedLocally) OnlineGreen else NoiseWhite,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
fun CommentsBottomModal(
    post: Post,
    commentsState: UiState<List<Comment>>,
    onSubmitComment: (String) -> Unit,
    onClose: () -> Unit
) {
    var commentText by remember { mutableStateOf("") }
    val comments = (commentsState as? UiState.Success)?.data ?: emptyList()

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(24.dp),
            color = NoiseCard
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Comments (${comments.size})",
                        color = NoiseWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = NoiseTextSecondary)
                    }
                }
                HorizontalDivider(color = NoiseBorder)
                Spacer(modifier = Modifier.height(10.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (comments.isEmpty()) {
                        item {
                            Text(
                                text = "Be the first to comment on @${post.username}'s post!",
                                color = NoiseTextSecondary,
                                fontSize = 13.sp
                            )
                        }
                    } else {
                        items(comments, key = { it.id }) { cmt ->
                            Column {
                                Text(
                                    text = "@${cmt.username}",
                                    color = NoisePink,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                                Text(
                                    text = cmt.text,
                                    color = NoiseWhite,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = commentText,
                        onValueChange = { commentText = it },
                        placeholder = { Text("Add a comment...", color = NoiseTextMuted) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = {
                            if (commentText.isNotBlank()) {
                                onSubmitComment(commentText)
                                commentText = ""
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Post Comment",
                            tint = NoisePink
                        )
                    }
                }
            }
        }
    }
}
