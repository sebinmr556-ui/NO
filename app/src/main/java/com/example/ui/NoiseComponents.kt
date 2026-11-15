package com.example.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.VideoView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AddBox
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Podcasts
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material.icons.outlined.AddBox
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.example.R
import com.example.ui.theme.NoiseBlack
import com.example.ui.theme.NoiseBorder
import com.example.ui.theme.NoiseCard
import com.example.ui.theme.NoiseCyan
import com.example.ui.theme.NoisePink as ThemeNoisePink
import com.example.ui.theme.NoisePurple as ThemeNoisePurple
import com.example.ui.theme.NoiseTextSecondary as ThemeNoiseTextSecondary
import com.example.ui.theme.NoiseViolet
import com.example.ui.theme.NoiseWhite
import com.example.ui.theme.OnlineGreen
import com.example.ui.theme.VipGold
import com.example.ui.theme.VipGoldBorder
import com.example.ui.theme.VipGoldDark
import java.io.File
import java.text.NumberFormat
import java.util.Locale

val NoiseGold: Color
    get() = VipGold
val NoiseBgBlack: Color
    get() = NoiseBlack
val NoisePink: Color
    get() = ThemeNoisePink
val NoisePurple: Color
    get() = ThemeNoisePurple
val NoiseTextSecondary: Color
    get() = ThemeNoiseTextSecondary

fun formatCoins(coins: Long): String {
    return NumberFormat.getNumberInstance(Locale.US).format(coins)
}

/**
 * Persists a gallery-picked [Uri] into durable app-internal storage (`filesDir/persisted_media`)
 * and attempts `takePersistableUriPermission` so that uploaded photos, videos, stories, and avatars
 * remain readable and playable after the app is closed and restarted.
 *
 * @return Pair(durableMediaKey, detectedMediaType) where `detectedMediaType` is `"video"` or `"photo"`.
 */
fun persistPickedMediaUri(
    context: Context,
    uri: Uri,
    fallbackMediaType: String = "photo"
): Pair<String, String> {
    val resolver = context.contentResolver
    val mimeType = runCatching { resolver.getType(uri) }.getOrNull()?.lowercase().orEmpty()
    val uriString = uri.toString()
    val isVideo = mimeType.startsWith("video/") ||
        uriString.contains("/video", ignoreCase = true) ||
        uriString.endsWith(".mp4", ignoreCase = true) ||
        uriString.endsWith(".mov", ignoreCase = true) ||
        uriString.endsWith(".mkv", ignoreCase = true) ||
        uriString.endsWith(".webm", ignoreCase = true) ||
        fallbackMediaType == "video"

    val detectedType = if (isVideo) "video" else "photo"

    runCatching {
        resolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    val ext = when {
        mimeType.contains("mp4") -> "mp4"
        mimeType.contains("webm") -> "webm"
        mimeType.contains("quicktime") || mimeType.contains("mov") -> "mov"
        isVideo -> "mp4"
        mimeType.contains("png") -> "png"
        mimeType.contains("webp") -> "webp"
        else -> "jpg"
    }

    val persistedUri = runCatching {
        val dir = File(context.filesDir, "persisted_media").apply { mkdirs() }
        val hash = uriString.hashCode().toUInt().toString(16)
        val destFile = File(dir, "media_${hash}.$ext")
        if (!destFile.exists() || destFile.length() == 0L) {
            resolver.openInputStream(uri)?.use { input ->
                destFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        }
        if (destFile.exists() && destFile.length() > 0L) {
            Uri.fromFile(destFile).toString()
        } else {
            uriString
        }
    }.getOrDefault(uriString)

    return persistedUri to detectedType
}

@Composable
fun NoiseTopHeader(
    coins: Long,
    unreadNotificationsCount: Int,
    onVipVaultClick: () -> Unit,
    onLivePulseClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    onDirectMessagesClick: () -> Unit,
    onSignOutClick: () -> Unit,
    onGamesClick: () -> Unit = {}
) {
    var showMenu by remember { mutableStateOf(false) }
    val logoGradient = Brush.horizontalGradient(listOf(NoisePink, NoisePurple))
    val logoOverride = com.example.data.CentralLiveUpdateRuntime.getComponentOverride("home_header_logo")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(NoiseBlack)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = logoOverride.customText.ifBlank { "Noise" },
                    style = TextStyle(
                        brush = logoGradient,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold
                    ),
                    modifier = Modifier
                        .ownerEditableComponent("home_header_logo", "NOICE Brand Logo")
                        .testTag("noise_logo_text")
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Gold VIP Pill with 3D Diamond Icon
                val isHeaderLight = com.example.ui.theme.ThemeManager.isLightMode
                Row(
                    modifier = Modifier
                        .ownerEditableComponent("header_vip_pill", "Top Bar VIP Crown Pill")
                        .clip(RoundedCornerShape(22.dp))
                        .background(VipGoldDark)
                        .border(1.2.dp, VipGoldBorder, RoundedCornerShape(22.dp))
                        .clickable { onVipVaultClick() }
                        .padding(horizontal = 9.dp, vertical = 5.dp)
                        .testTag("vip_vault_header_button"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Realistic3DDiamondIcon(
                        vipLevel = 10,
                        size = 16.dp
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = formatCoins(coins),
                        color = if (isHeaderLight) Color(0xFF6E4E00) else VipGold,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(VipGold)
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = "VIP",
                            color = Color.Black,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 9.sp
                        )
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Prominent Games Pill Button in Top Header
                Row(
                    modifier = Modifier
                        .ownerEditableComponent("header_games_button", "Top Bar 3D Games Button")
                        .clip(RoundedCornerShape(18.dp))
                        .background(Brush.horizontalGradient(listOf(NoisePink.copy(alpha = 0.25f), NoisePurple.copy(alpha = 0.32f))))
                        .border(1.dp, VipGold, RoundedCornerShape(18.dp))
                        .clickable { onGamesClick() }
                        .padding(horizontal = 9.dp, vertical = 5.dp)
                        .testTag("header_games_button"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.SportsEsports,
                        contentDescription = "Games",
                        tint = VipGold,
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "GAMES",
                        color = NoiseWhite,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 10.sp
                    )
                }

                Spacer(modifier = Modifier.width(2.dp))

                // Broadcast / Live icon with pink dot
                Box(
                    modifier = Modifier
                        .ownerEditableComponent("header_live_button", "Top Bar Live Button")
                        .size(36.dp)
                        .clickable { onLivePulseClick() }
                        .testTag("header_live_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Podcasts,
                        contentDescription = "Live Party Rooms",
                        tint = NoisePink,
                        modifier = Modifier.size(21.dp)
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = (-5).dp, y = 5.dp)
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(NoisePink)
                    )
                }

                // Heart / Notifications icon with pink dot
                Box(
                    modifier = Modifier
                        .ownerEditableComponent("header_notification_icon", "Notification Bell Icon")
                        .size(36.dp)
                        .clickable { onNotificationsClick() }
                        .testTag("header_notifications_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.FavoriteBorder,
                        contentDescription = "Notifications",
                        tint = NoiseWhite,
                        modifier = Modifier.size(21.dp)
                    )
                    if (unreadNotificationsCount > 0) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = (-5).dp, y = 6.dp)
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(NoisePink)
                        )
                    }
                }

                // Direct Messages chat bubble icon
                IconButton(
                    onClick = onDirectMessagesClick,
                    modifier = Modifier
                        .ownerEditableComponent("header_dm_icon", "Direct Messages Icon")
                        .size(36.dp)
                        .testTag("header_direct_messages_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.ChatBubbleOutline,
                        contentDescription = "Noise Direct Messages",
                        tint = NoiseWhite,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Hamburger menu icon
                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("header_menu_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Menu",
                            tint = NoiseWhite,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        modifier = Modifier.background(NoiseCard)
                    ) {
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.SportsEsports,
                                        contentDescription = null,
                                        tint = VipGold,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Multiplayer Games", color = VipGold, fontWeight = FontWeight.ExtraBold)
                                }
                            },
                            onClick = {
                                showMenu = false
                                onGamesClick()
                            },
                            modifier = Modifier.testTag("menu_games_option")
                        )
                        DropdownMenuItem(
                            text = { Text("VIP Unlimited Vault", color = VipGold) },
                            onClick = {
                                showMenu = false
                                onVipVaultClick()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Noise Direct", color = NoiseWhite) },
                            onClick = {
                                showMenu = false
                                onDirectMessagesClick()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Sign Out", color = NoisePink) },
                            onClick = {
                                showMenu = false
                                onSignOutClick()
                            }
                        )
                    }
                }
            }
        }
        OwnerCustomPageElementsSection(
            pageKey = com.example.data.CentralLiveUpdateRuntime.activePageContextKey
        )
        HorizontalDivider(color = NoiseBorder.copy(alpha = 0.6f), thickness = 0.8.dp)
    }
}

@Composable
fun NoiseBottomNavBar(
    selectedTab: NoiseTab,
    userAvatarKey: String,
    onSelectTab: (NoiseTab) -> Unit
) {
    val activeGradient = Brush.linearGradient(listOf(NoisePink, NoisePurple))
    val profileRingGradient = Brush.linearGradient(listOf(NoisePurple, NoiseCyan, NoisePink))

    Column(
        modifier = Modifier
            .ownerEditableComponent("bottom_nav_bar", "Bottom Navigation Dock")
            .fillMaxWidth()
            .background(NoiseBlack)
            .windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        HorizontalDivider(color = NoiseBorder.copy(alpha = 0.7f), thickness = 0.8.dp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(66.dp)
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // 1. Home Tab
            Column(
                modifier = Modifier
                    .weight(1f)
                    .height(54.dp)
                    .clickable { onSelectTab(NoiseTab.HOME) }
                    .testTag("nav_home"),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = if (selectedTab == NoiseTab.HOME) Icons.Filled.Home else Icons.Outlined.Home,
                    contentDescription = "Home Feed",
                    tint = if (selectedTab == NoiseTab.HOME) NoiseWhite else NoiseTextSecondary,
                    modifier = Modifier.size(23.dp)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Home",
                    color = if (selectedTab == NoiseTab.HOME) NoiseWhite else NoiseTextSecondary,
                    fontSize = 10.sp,
                    fontWeight = if (selectedTab == NoiseTab.HOME) FontWeight.Bold else FontWeight.Medium
                )
            }

            // 2. Search Tab
            Column(
                modifier = Modifier
                    .weight(1f)
                    .height(54.dp)
                    .clickable { onSelectTab(NoiseTab.SEARCH) }
                    .testTag("nav_search"),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search & Registered Accounts",
                    tint = if (selectedTab == NoiseTab.SEARCH) NoiseWhite else NoiseTextSecondary,
                    modifier = Modifier.size(23.dp)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Search",
                    color = if (selectedTab == NoiseTab.SEARCH) NoiseWhite else NoiseTextSecondary,
                    fontSize = 10.sp,
                    fontWeight = if (selectedTab == NoiseTab.SEARCH) FontWeight.Bold else FontWeight.Medium
                )
            }

            // 3. Create Post Center Button
            Column(
                modifier = Modifier
                    .weight(1f)
                    .height(54.dp)
                    .clickable { onSelectTab(NoiseTab.CREATE) }
                    .testTag("nav_create"),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(
                            if (selectedTab == NoiseTab.CREATE) activeGradient
                            else Brush.linearGradient(listOf(NoiseCard, NoiseCard))
                        )
                        .border(
                            width = 1.dp,
                            color = if (selectedTab == NoiseTab.CREATE) NoisePink else NoiseBorder,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (selectedTab == NoiseTab.CREATE) Icons.Filled.AddBox else Icons.Outlined.AddBox,
                        contentDescription = "Create New Post",
                        tint = if (selectedTab == NoiseTab.CREATE) Color.White else NoiseWhite,
                        modifier = Modifier.size(19.dp)
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Create",
                    color = if (selectedTab == NoiseTab.CREATE) NoiseWhite else NoiseTextSecondary,
                    fontSize = 10.sp,
                    fontWeight = if (selectedTab == NoiseTab.CREATE) FontWeight.Bold else FontWeight.Medium
                )
            }

            // 4. Video Clips / Reels Tab
            Column(
                modifier = Modifier
                    .weight(1f)
                    .height(54.dp)
                    .clickable { onSelectTab(NoiseTab.CLIPS) }
                    .testTag("nav_clips"),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = if (selectedTab == NoiseTab.CLIPS) Icons.Filled.Movie else Icons.Outlined.Movie,
                    contentDescription = "Video Clips",
                    tint = if (selectedTab == NoiseTab.CLIPS) NoiseWhite else NoiseTextSecondary,
                    modifier = Modifier.size(23.dp)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Clips",
                    color = if (selectedTab == NoiseTab.CLIPS) NoiseWhite else NoiseTextSecondary,
                    fontSize = 10.sp,
                    fontWeight = if (selectedTab == NoiseTab.CLIPS) FontWeight.Bold else FontWeight.Medium
                )
            }

            // 5. Profile Tab
            Column(
                modifier = Modifier
                    .weight(1f)
                    .height(54.dp)
                    .clickable { onSelectTab(NoiseTab.PROFILE) }
                    .testTag("nav_profile"),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .then(
                            if (selectedTab == NoiseTab.PROFILE) {
                                Modifier
                                    .border(1.8.dp, profileRingGradient, CircleShape)
                                    .padding(2.dp)
                            } else {
                                Modifier
                            }
                        )
                        .clip(CircleShape)
                        .background(NoiseViolet),
                    contentAlignment = Alignment.Center
                ) {
                    if (userAvatarKey.startsWith("content://") ||
                        userAvatarKey.startsWith("file://") ||
                        userAvatarKey.startsWith("http")
                    ) {
                        AsyncImage(
                            model = userAvatarKey,
                            contentDescription = "Profile Avatar",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                        )
                    } else {
                        Text(
                            text = userAvatarKey.take(1).uppercase().ifBlank { "S" },
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Profile",
                    color = if (selectedTab == NoiseTab.PROFILE) NoiseWhite else NoiseTextSecondary,
                    fontSize = 10.sp,
                    fontWeight = if (selectedTab == NoiseTab.PROFILE) FontWeight.Bold else FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun NoiseAvatar(
    avatarKey: String,
    size: Dp = 48.dp,
    showOnlineDot: Boolean = false,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.size(size)) {
        val isUri = avatarKey.startsWith("content://") ||
            avatarKey.startsWith("file://") ||
            avatarKey.startsWith("http")
        val presetDrawable = when (avatarKey.uppercase()) {
            "COUPLE" -> R.drawable.img_post_couple
            "ROSES" -> R.drawable.img_post_roses
            "MIC" -> R.drawable.img_thumb_mic
            "WAVE" -> R.drawable.img_thumb_wave
            "LAKE" -> R.drawable.img_thumb_lake
            "CANYON" -> R.drawable.img_thumb_canyon
            else -> null
        }

        if (isUri) {
            AsyncImage(
                model = avatarKey,
                contentDescription = "Creator Avatar",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
            )
        } else if (presetDrawable != null) {
            Image(
                painter = painterResource(id = presetDrawable),
                contentDescription = "Creator Avatar",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
            )
        } else if (avatarKey == "S_BLUE") {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(Color(0xFF155E95)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "S",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = (size.value * 0.44f).sp
                )
            }
        } else {
            val bgColor = when (avatarKey.uppercase()) {
                "M" -> Color(0xFF3B4CCA)
                "A" -> Color(0xFF0F766E)
                "K" -> Color(0xFF6D28D9)
                "SL" -> Color(0xFF0284C7)
                "ST" -> Color(0xFFB45309)
                "E" -> Color(0xFF9D174D)
                else -> Color(0xFF5B34DA)
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(bgColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = avatarKey.take(1).uppercase().ifBlank { "S" },
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = (size.value * 0.44f).sp
                )
            }
        }

        if (showOnlineDot) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size((size.value * 0.26f).dp.coerceAtLeast(10.dp))
                    .clip(CircleShape)
                    .background(OnlineGreen)
                    .border(2.dp, NoiseBlack, CircleShape)
            )
        }
    }
}

@Composable
fun NoiseMediaImage(
    mediaKey: String,
    contentDescription: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    thumbnailUrl: String = "",
    storagePath: String = "",
    cloudMediaId: String = ""
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val drawableRes = when (mediaKey.lowercase()) {
        "roses" -> R.drawable.img_post_roses
        "couple" -> R.drawable.img_post_couple
        "mic" -> R.drawable.img_thumb_mic
        "wave" -> R.drawable.img_thumb_wave
        "lake" -> R.drawable.img_thumb_lake
        "canyon" -> R.drawable.img_thumb_canyon
        else -> null
    }

    val looksLikeVideoSource = mediaKey.endsWith(".mp4", ignoreCase = true) ||
        mediaKey.contains(".mp4?", ignoreCase = true) ||
        storagePath.endsWith(".mp4", ignoreCase = true)

    var resolvedUri by remember(mediaKey, storagePath, cloudMediaId) {
        mutableStateOf(
            if ((mediaKey.startsWith("http") && mediaKey.contains("cloudMediaId=")) || looksLikeVideoSource) {
                ""
            } else {
                mediaKey
            }
        )
    }
    var extractedVideoFrame by remember(mediaKey, storagePath, cloudMediaId) {
        mutableStateOf<android.graphics.Bitmap?>(null)
    }

    val inlineBitmap = remember(mediaKey, thumbnailUrl) {
        val dataSource = when {
            thumbnailUrl.startsWith("data:image/") -> thumbnailUrl
            mediaKey.startsWith("data:image/") -> mediaKey
            else -> ""
        }
        if (dataSource.isNotBlank()) {
            runCatching {
                val b64 = dataSource.substringAfter("base64,")
                val bytes = android.util.Base64.decode(b64, android.util.Base64.DEFAULT)
                android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            }.getOrNull()
        } else null
    }

    androidx.compose.runtime.LaunchedEffect(mediaKey, thumbnailUrl, storagePath, cloudMediaId) {
        if (drawableRes == null && inlineBitmap == null && mediaKey.isNotBlank()) {
            if (thumbnailUrl.startsWith("http") || thumbnailUrl.startsWith("file://") || thumbnailUrl.startsWith("content://")) {
                resolvedUri = thumbnailUrl
                return@LaunchedEffect
            }
            val resolved = com.example.data.CloudMediaStorageManager.resolvePlayableMediaUri(
                context = context,
                mediaUrlOrKey = mediaKey,
                storagePath = storagePath,
                explicitCloudMediaId = cloudMediaId,
                mediaType = if (looksLikeVideoSource) "video" else "photo"
            )
            if (resolved.isNotBlank()) {
                if (looksLikeVideoSource || resolved.endsWith(".mp4", ignoreCase = true)) {
                    val bmp = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                        runCatching {
                            val retriever = android.media.MediaMetadataRetriever()
                            try {
                                val uri = Uri.parse(resolved)
                                if (resolved.startsWith("file://")) {
                                    retriever.setDataSource(uri.path)
                                } else {
                                    retriever.setDataSource(context, uri)
                                }
                                retriever.getFrameAtTime(100_000L, android.media.MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                                    ?: retriever.frameAtTime
                            } finally {
                                runCatching { retriever.release() }
                            }
                        }.getOrNull()
                    }
                    if (bmp != null) {
                        extractedVideoFrame = bmp
                    }
                } else {
                    resolvedUri = resolved
                }
            }
        }
    }

    if (drawableRes != null) {
        Image(
            painter = painterResource(id = drawableRes),
            contentDescription = contentDescription,
            contentScale = contentScale,
            modifier = modifier
        )
    } else if (inlineBitmap != null) {
        Image(
            bitmap = inlineBitmap.asImageBitmap(),
            contentDescription = contentDescription,
            contentScale = contentScale,
            modifier = modifier
        )
    } else if (extractedVideoFrame != null) {
        Image(
            bitmap = extractedVideoFrame!!.asImageBitmap(),
            contentDescription = contentDescription,
            contentScale = contentScale,
            modifier = modifier
        )
    } else if (resolvedUri.startsWith("content://") ||
        resolvedUri.startsWith("file://") ||
        resolvedUri.startsWith("http")
    ) {
        AsyncImage(
            model = resolvedUri,
            contentDescription = contentDescription,
            contentScale = contentScale,
            modifier = modifier
        )
    } else {
        Image(
            painter = painterResource(id = R.drawable.img_post_roses),
            contentDescription = contentDescription,
            contentScale = contentScale,
            modifier = modifier
        )
    }
}

/**
 * Global playback coordinator ensuring that only a single video player plays audio/video
 * at any given time across Feed Posts, Trending Reels, Stories, Direct Messages, and Modals.
 */
object NoiseVideoPlaybackCoordinator {
    private val activePlayers = java.util.Collections.synchronizedSet(
         linkedSetOf<androidx.media3.exoplayer.ExoPlayer>()
    )

    fun registerAndActivate(player: androidx.media3.exoplayer.ExoPlayer) {
        val snapshot = synchronized(activePlayers) {
            activePlayers.add(player)
            activePlayers.filter { it !== player }
        }
        snapshot.forEach { other ->
            runCatching {
                if (other.isPlaying) {
                    other.playWhenReady = false
                    other.pause()
                }
            }
        }
    }

    fun unregister(player: androidx.media3.exoplayer.ExoPlayer) {
        activePlayers.remove(player)
    }

    fun pauseAll(except: androidx.media3.exoplayer.ExoPlayer? = null) {
        val snapshot = synchronized(activePlayers) {
            activePlayers.filter { it !== except }
        }
        snapshot.forEach { player ->
            runCatching {
                player.playWhenReady = false
                player.pause()
            }
        }
    }
}

fun formatVideoDurationMs(durationMs: Long): String {
    val totalSeconds = (durationMs.coerceAtLeast(0L) / 1000L).toInt()
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(java.util.Locale.US, "%02d:%02d", minutes, seconds)
}

/**
 * Production AndroidX Media3 ExoPlayer video player used in Home Feed, Trending Reels,
 * Create Post preview, Profile video posts, Chatrooms, and Direct Messages.
 *
 * Features:
 * - Hardware-accelerated H.264/AAC MP4 playback via `ExoPlayer` + `TextureView`
 * - Supports Firebase Storage URLs (`https://...`), cloud-hydrated durable cache (`file://`), and `content://`
 * - Play, Pause, interactive Seek Bar, Replay, and Mute/Unmute controls
 * - Thumbnail preview layer maintained until first video frame renders (eliminates black screen flash)
 * - Single-active-player coordination via [NoiseVideoPlaybackCoordinator]
 * - Automatic lifecycle pause/resume and deterministic `exoPlayer.release()` on disposal
 * - Explicit error state with diagnostic logging and Retry button
 */
@Composable
fun NoiseVideoPlayer(
    mediaKey: String,
    contentDescription: String,
    modifier: Modifier = Modifier,
    autoPlay: Boolean = false,
    thumbnailUrl: String = "",
    storagePath: String = "",
    cloudMediaId: String = ""
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    var isPlaying by remember(mediaKey, cloudMediaId) { mutableStateOf(autoPlay) }
    var isMuted by remember(mediaKey, cloudMediaId) { mutableStateOf(false) }
    var isPrepared by remember(mediaKey, cloudMediaId) { mutableStateOf(false) }
    var hasRenderedFirstFrame by remember(mediaKey, cloudMediaId) { mutableStateOf(false) }
    var isBuffering by remember(mediaKey, cloudMediaId) { mutableStateOf(false) }
    var isResolvingCloud by remember(mediaKey, cloudMediaId) { mutableStateOf(false) }
    var hasError by remember(mediaKey, cloudMediaId) { mutableStateOf(false) }
    var errorDetail by remember(mediaKey, cloudMediaId) { mutableStateOf("") }
    var isEnded by remember(mediaKey, cloudMediaId) { mutableStateOf(false) }
    var retryNonce by remember(mediaKey, cloudMediaId) { mutableStateOf(0) }
    var currentPositionMs by remember(mediaKey, cloudMediaId) { mutableStateOf(0L) }
    var durationMs by remember(mediaKey, cloudMediaId) { mutableStateOf(0L) }
    var isUserSeeking by remember(mediaKey, cloudMediaId) { mutableStateOf(false) }
    var seekSliderFraction by remember(mediaKey, cloudMediaId) { mutableStateOf(0f) }

    var resolvedPlayableUri by remember(mediaKey, storagePath, cloudMediaId) {
        mutableStateOf(
            if (mediaKey.startsWith("http") && mediaKey.contains("cloudMediaId=")) "" else mediaKey
        )
    }

    // Sync with external autoPlay changes (e.g., swiping vertical pager in Trending Reels)
    androidx.compose.runtime.LaunchedEffect(autoPlay) {
        isPlaying = autoPlay
    }

    androidx.compose.runtime.LaunchedEffect(mediaKey, storagePath, cloudMediaId, retryNonce) {
        if (!com.example.data.CloudMediaStorageManager.isPresetMediaKey(mediaKey) &&
            (mediaKey.isNotBlank() || storagePath.isNotBlank() || cloudMediaId.isNotBlank())
        ) {
            isResolvingCloud = true
            hasError = false
            errorDetail = ""
            val resolved = com.example.data.CloudMediaStorageManager.resolvePlayableMediaUri(
                context = context,
                mediaUrlOrKey = mediaKey,
                storagePath = storagePath,
                explicitCloudMediaId = cloudMediaId,
                mediaType = "video"
            )
            if (resolved.isNotBlank()) {
                resolvedPlayableUri = resolved
                hasError = false
            } else {
                hasError = true
                errorDetail = "Video could not be loaded from cloud storage. Check connection and tap Retry."
                com.example.data.CloudMediaStorageManager.recordDiagnosticError(
                    stage = "NoiseVideoPlayer.resolve",
                    detail = "Empty resolved URI for mediaKey=$mediaKey cloudMediaId=$cloudMediaId"
                )
            }
            isResolvingCloud = false
        }
    }

    val isPlayableUri = resolvedPlayableUri.startsWith("content://") ||
        resolvedPlayableUri.startsWith("file://") ||
        resolvedPlayableUri.startsWith("http")

    val exoPlayer = remember(context) {
        androidx.media3.exoplayer.ExoPlayer.Builder(context).build().apply {
            repeatMode = androidx.media3.common.Player.REPEAT_MODE_ONE
            playWhenReady = autoPlay
        }
    }

    // Bind player listener and MediaItem whenever resolvedPlayableUri or retryNonce updates
    DisposableEffect(exoPlayer, resolvedPlayableUri, retryNonce) {
        val listener = object : androidx.media3.common.Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    androidx.media3.common.Player.STATE_BUFFERING -> {
                        isBuffering = true
                    }
                    androidx.media3.common.Player.STATE_READY -> {
                        isBuffering = false
                        isPrepared = true
                        hasError = false
                        val d = exoPlayer.duration
                        if (d > 0L && d != androidx.media3.common.C.TIME_UNSET) {
                            durationMs = d
                        }
                    }
                    androidx.media3.common.Player.STATE_ENDED -> {
                        isBuffering = false
                        isEnded = true
                        isPlaying = false
                    }
                    androidx.media3.common.Player.STATE_IDLE -> {
                        isBuffering = false
                    }
                }
            }

            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
                if (playing) {
                    isEnded = false
                    NoiseVideoPlaybackCoordinator.registerAndActivate(exoPlayer)
                }
            }

            override fun onRenderedFirstFrame() {
                hasRenderedFirstFrame = true
                isPrepared = true
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                isBuffering = false
                isPlaying = false
                hasError = true
                errorDetail = "Playback interrupted (${error.errorCodeName}). Tap Retry to reload."
                com.example.data.CloudMediaStorageManager.recordDiagnosticError(
                    stage = "NoiseVideoPlayer.ExoPlayer",
                    detail = "code=${error.errorCodeName} uri=$resolvedPlayableUri",
                    throwable = error
                )
            }
        }

        exoPlayer.addListener(listener)

        if (isPlayableUri && !isResolvingCloud) {
            runCatching {
                hasRenderedFirstFrame = false
                isPrepared = false
                hasError = false
                val mediaItem = androidx.media3.common.MediaItem.fromUri(Uri.parse(resolvedPlayableUri))
                exoPlayer.setMediaItem(mediaItem)
                exoPlayer.volume = if (isMuted) 0f else 1f
                exoPlayer.playWhenReady = isPlaying
                if (isPlaying) {
                    NoiseVideoPlaybackCoordinator.registerAndActivate(exoPlayer)
                }
                exoPlayer.prepare()
            }.onFailure { err ->
                hasError = true
                errorDetail = err.localizedMessage ?: "Unable to initialize video player"
            }
        }

        onDispose {
            exoPlayer.removeListener(listener)
        }
    }

    // Lifecycle cleanup & background pause
    DisposableEffect(lifecycleOwner, exoPlayer) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            when (event) {
                androidx.lifecycle.Lifecycle.Event.ON_PAUSE,
                androidx.lifecycle.Lifecycle.Event.ON_STOP -> {
                    if (exoPlayer.isPlaying) {
                        exoPlayer.playWhenReady = false
                        exoPlayer.pause()
                    }
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            NoiseVideoPlaybackCoordinator.unregister(exoPlayer)
            runCatching {
                exoPlayer.stop()
                exoPlayer.clearMediaItems()
                exoPlayer.release()
            }
        }
    }

    // Sync play/pause & mute state changes to ExoPlayer
    androidx.compose.runtime.LaunchedEffect(isPlaying, isMuted, isPrepared) {
        runCatching {
            exoPlayer.volume = if (isMuted) 0f else 1f
            if (isPlaying && isPlayableUri && !hasError) {
                NoiseVideoPlaybackCoordinator.registerAndActivate(exoPlayer)
                if (exoPlayer.playbackState == androidx.media3.common.Player.STATE_IDLE) {
                    exoPlayer.prepare()
                } else if (exoPlayer.playbackState == androidx.media3.common.Player.STATE_ENDED) {
                    exoPlayer.seekTo(0L)
                }
                exoPlayer.playWhenReady = true
                exoPlayer.play()
            } else if (!isPlaying && exoPlayer.isPlaying) {
                exoPlayer.playWhenReady = false
                exoPlayer.pause()
            }
        }
    }

    // Poll current playback position for smooth seek bar & timestamp updates while playing
    androidx.compose.runtime.LaunchedEffect(isPlaying, isPrepared, isUserSeeking) {
        while (isPrepared) {
            if (!isUserSeeking) {
                val pos = runCatching { exoPlayer.currentPosition.coerceAtLeast(0L) }.getOrDefault(0L)
                val dur = runCatching {
                    val d = exoPlayer.duration
                    if (d > 0L && d != androidx.media3.common.C.TIME_UNSET) d else durationMs
                }.getOrDefault(durationMs)
                currentPositionMs = pos
                if (dur > 0L) {
                    durationMs = dur
                    seekSliderFraction = (pos.toFloat() / dur.toFloat()).coerceIn(0f, 1f)
                }
            }
            if (!isPlaying) break
            kotlinx.coroutines.delay(200L)
        }
    }

    Box(
        modifier = modifier
            .background(NoiseBlack)
            .clickable {
                if (hasError) {
                    hasError = false
                    isPrepared = false
                    hasRenderedFirstFrame = false
                    retryNonce += 1
                    isPlaying = true
                } else {
                    isPlaying = !isPlaying
                }
            }
            .testTag("noise_video_player"),
        contentAlignment = Alignment.Center
    ) {
        if (isPlayableUri && !hasError) {
            androidx.compose.runtime.key(resolvedPlayableUri, retryNonce) {
                AndroidView(
                    factory = { ctx ->
                        android.view.TextureView(ctx).apply {
                            exoPlayer.setVideoTextureView(this)
                        }
                    },
                    update = { textureView ->
                        exoPlayer.setVideoTextureView(textureView)
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // Always render video thumbnail overlay until first frame renders so there is NEVER a black screen
        if (!hasRenderedFirstFrame || hasError || !isPlayableUri) {
            NoiseMediaImage(
                mediaKey = thumbnailUrl.ifBlank { mediaKey },
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                thumbnailUrl = thumbnailUrl,
                storagePath = storagePath,
                cloudMediaId = cloudMediaId,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Top-left VIDEO status badge
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color.Black.copy(alpha = 0.65f))
                .border(1.dp, NoisePink.copy(alpha = 0.65f), RoundedCornerShape(10.dp))
                .padding(horizontal = 9.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Movie,
                contentDescription = null,
                tint = NoisePink,
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = when {
                    isResolvingCloud -> "VIDEO • SYNCING CLOUD..."
                    isBuffering -> "VIDEO • BUFFERING..."
                    hasError -> "VIDEO • TAP RETRY"
                    isPlaying -> "VIDEO • PLAYING"
                    else -> "VIDEO CLIP"
                },
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Top-right Replay + Mute/Unmute controls
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isPrepared && isPlayableUri && !hasError) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.65f))
                        .border(1.dp, Color.White.copy(alpha = 0.25f), CircleShape)
                        .clickable {
                            runCatching {
                                exoPlayer.seekTo(0L)
                                currentPositionMs = 0L
                                seekSliderFraction = 0f
                                isEnded = false
                                isPlaying = true
                            }
                        }
                        .testTag("video_player_replay_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Replay Video",
                        tint = Color.White,
                        modifier = Modifier.size(17.dp)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.65f))
                    .border(1.dp, Color.White.copy(alpha = 0.25f), CircleShape)
                    .clickable { isMuted = !isMuted }
                    .testTag("video_player_mute_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = if (isMuted) "Unmute Video" else "Mute Video",
                    tint = Color.White,
                    modifier = Modifier.size(17.dp)
                )
            }
        }

        // Center Loading / Error / Play overlay
        if (isResolvingCloud || (isBuffering && isPlaying && !hasError)) {
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
        } else if (hasError) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black.copy(alpha = 0.82f))
                    .border(1.dp, NoisePink.copy(alpha = 0.7f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = errorDetail.ifBlank { "Unable to load video clip." },
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Brush.linearGradient(listOf(NoisePink, NoisePurple)))
                        .clickable {
                            hasError = false
                            isPrepared = false
                            hasRenderedFirstFrame = false
                            retryNonce += 1
                            isPlaying = true
                        }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .testTag("video_player_retry_button"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Retry Video",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Retry Video",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        } else if (!isPlaying) {
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(NoisePink.copy(alpha = 0.9f), NoisePurple.copy(alpha = 0.9f))
                        )
                    )
                    .border(2.dp, Color.White.copy(alpha = 0.85f), CircleShape)
                    .testTag("video_player_play_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isEnded) Icons.Default.Refresh else Icons.Default.PlayArrow,
                    contentDescription = if (isEnded) "Replay Video" else "Play Video",
                    tint = Color.White,
                    modifier = Modifier.size(30.dp)
                )
            }
        } else if (!isPlayableUri) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.Black.copy(alpha = 0.65f))
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Pause,
                        contentDescription = "Pause Video",
                        tint = NoisePink,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Playing Clip • Tap to pause",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Bottom Interactive Seek Bar & Duration Indicator
        if (isPrepared && isPlayableUri && durationMs > 0L && !hasError) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.78f))
                        )
                    )
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = Color.White,
                    modifier = Modifier
                        .size(20.dp)
                        .clickable { isPlaying = !isPlaying }
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = formatVideoDurationMs(currentPositionMs),
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold
                )
                androidx.compose.material3.Slider(
                    value = seekSliderFraction.coerceIn(0f, 1f),
                    onValueChange = { fraction ->
                        isUserSeeking = true
                        seekSliderFraction = fraction
                        currentPositionMs = (fraction * durationMs).toLong()
                    },
                    onValueChangeFinished = {
                        val targetMs = (seekSliderFraction * durationMs).toLong().coerceIn(0L, durationMs)
                        runCatching { exoPlayer.seekTo(targetMs) }
                        isUserSeeking = false
                    },
                    colors = androidx.compose.material3.SliderDefaults.colors(
                        thumbColor = NoisePink,
                        activeTrackColor = NoisePink,
                        inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(24.dp)
                        .padding(horizontal = 6.dp)
                        .testTag("video_player_seek_slider")
                )
                Text(
                    text = formatVideoDurationMs(durationMs),
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}



