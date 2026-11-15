package com.example.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Post
import com.example.data.UiState
import com.example.data.UserProfile
import com.example.ui.theme.DangerRed
import com.example.ui.theme.HotOrange
import com.example.ui.theme.LightLavender
import com.example.ui.theme.NoiseBlack
import com.example.ui.theme.NoiseBorder
import com.example.ui.theme.NoiseCard
import com.example.ui.theme.NoiseCardElevated
import com.example.ui.theme.NoiseCyan
import com.example.ui.theme.NoiseLightLavender
import com.example.ui.theme.NoiseLinkColor
import com.example.ui.theme.NoiseNormalText
import com.example.ui.theme.NoisePink
import com.example.ui.theme.NoisePurple
import com.example.ui.theme.NoiseTextMuted
import com.example.ui.theme.NoiseTextSecondary
import com.example.ui.theme.NoiseThemeController
import com.example.ui.theme.NoiseWhite
import com.example.ui.theme.OnlineGreen
import com.example.ui.theme.ThemeManager
import com.example.ui.theme.VerifiedBlue
import com.example.ui.theme.VipGold
import com.example.ui.theme.noisePrimaryActionBrush
import com.example.ui.theme.noisePurplePinkGradient

data class TrendingTopicItem(
    val rank: String,
    val category: String,
    val tag: String,
    val subtitle: String,
    val countLabel: String,
    val growthLabel: String
)

@Composable
fun SearchAndAccountsScreen(
    registeredAccountsState: UiState<List<UserProfile>>,
    postsState: UiState<List<Post>>,
    followedUserIds: List<String> = emptyList(),
    onInspectUser: (UserProfile) -> Unit = {},
    onMessageUser: (UserProfile) -> Unit,
    onFollowUser: (UserProfile) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilterMode by remember { mutableIntStateOf(0) } // 0 = Top, 1 = Registered Accounts & IDs, 2 = Posts
    var selectedCategoryTag by remember { mutableStateOf("All") }

    val accounts = (registeredAccountsState as? UiState.Success)?.data ?: emptyList()
    val posts = (postsState as? UiState.Success)?.data ?: emptyList()

    val filteredAccounts = remember(accounts, searchQuery) {
        val cleanQuery = searchQuery.trim().removePrefix("@")
        if (cleanQuery.isBlank()) accounts
        else accounts.filter {
            it.username.contains(cleanQuery, ignoreCase = true) ||
                it.usernameLower.contains(cleanQuery.lowercase()) ||
                it.displayName.contains(cleanQuery, ignoreCase = true) ||
                it.displayNameLower.contains(cleanQuery.lowercase()) ||
                it.id.contains(cleanQuery, ignoreCase = true) ||
                it.userId.contains(cleanQuery, ignoreCase = true)
        }
    }

    val filteredPosts = remember(posts, searchQuery, selectedCategoryTag) {
        posts.filter { post ->
            val matchesQuery = searchQuery.isBlank() ||
                post.caption.contains(searchQuery, ignoreCase = true) ||
                post.username.contains(searchQuery, ignoreCase = true) ||
                post.tag.contains(searchQuery, ignoreCase = true)
            val matchesTag = selectedCategoryTag == "All" ||
                post.tag.equals(selectedCategoryTag, ignoreCase = true) ||
                post.caption.contains(selectedCategoryTag, ignoreCase = true)
            matchesQuery && matchesTag
        }
    }

    val categories = listOf("All", "#Photography", "#Travel", "#Art", "#Music", "#NoiseVibes")
    val trendingTopics = listOf(
        TrendingTopicItem("#1", "Community", "#NoiseVibes", "Noise Vibes", "52.4K", "+48%"),
        TrendingTopicItem("#2", "Photography", "#StreetPhotography", "Street Photography", "38.1K", "+32%"),
        TrendingTopicItem("#3", "Music", "#LofiBeats", "Lo-Fi Beats", "29.7K", "+24%")
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(NoiseBlack)
            .testTag("search_screen_list"),
        contentPadding = PaddingValues(bottom = 28.dp)
    ) {
        // 1. Search Input
        item {
            val isLight = ThemeManager.isLightMode
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = NoiseTextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                },
                placeholder = {
                    Text(
                        text = "Search by registered User ID, @username, or name...",
                        color = NoiseTextMuted,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp)
                    .testTag("search_input_field"),
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = if (isLight) Color(0xFFFFFFFF) else Color(0xFF121218),
                    unfocusedContainerColor = if (isLight) Color(0xFFFFFFFF) else Color(0xFF121218),
                    focusedBorderColor = NoisePink,
                    unfocusedBorderColor = if (isLight) NoiseBorder else Color(0xFF242432),
                    focusedTextColor = NoiseNormalText,
                    unfocusedTextColor = NoiseNormalText
                )
            )
        }

        // 2. Filter Mode Pills: [✨ Top] [Fingerprint Registered Accounts & IDs 4] [# Posts]
        item {
            val isLight = ThemeManager.isLightMode
            val unselectedTabBrush = Brush.horizontalGradient(
                listOf(
                    if (isLight) Color(0xFFFFFFFF) else Color(0xFF14141C),
                    if (isLight) Color(0xFFFFFFFF) else Color(0xFF14141C)
                )
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Top Pill
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (selectedFilterMode == 0) noisePurplePinkGradient() else unselectedTabBrush)
                        .border(
                            1.dp,
                            if (selectedFilterMode == 0) NoisePink else NoiseBorder,
                            RoundedCornerShape(20.dp)
                        )
                        .clickable { selectedFilterMode = 0 }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                        .testTag("search_tab_top"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = if (selectedFilterMode == 0) Color.White else NoiseNormalText,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Top",
                        color = if (selectedFilterMode == 0) Color.White else NoiseNormalText,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                // Registered Accounts & IDs Pill
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (selectedFilterMode == 1) noisePurplePinkGradient() else unselectedTabBrush)
                        .border(
                            1.dp,
                            if (selectedFilterMode == 1) NoisePink else NoiseBorder,
                            RoundedCornerShape(20.dp)
                        )
                        .clickable { selectedFilterMode = 1 }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                        .testTag("search_tab_accounts"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Fingerprint,
                        contentDescription = null,
                        tint = if (selectedFilterMode == 1) Color.White else NoisePurple,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Registered Accounts & IDs",
                        color = if (selectedFilterMode == 1) Color.White else NoiseNormalText,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(
                                when {
                                    selectedFilterMode == 1 -> Color.White.copy(alpha = 0.24f)
                                    isLight -> LightLavender
                                    else -> Color(0xFF252534)
                                }
                            )
                            .padding(horizontal = 6.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = "${accounts.size.coerceAtLeast(4)}",
                            color = if (selectedFilterMode == 1) Color.White else NoiseWhite,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Posts Pill
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (selectedFilterMode == 2) noisePurplePinkGradient() else unselectedTabBrush)
                        .border(
                            1.dp,
                            if (selectedFilterMode == 2) NoisePink else NoiseBorder,
                            RoundedCornerShape(20.dp)
                        )
                        .clickable { selectedFilterMode = 2 }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                        .testTag("search_tab_posts"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Tag,
                        contentDescription = null,
                        tint = if (selectedFilterMode == 2) Color.White else NoiseNormalText,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Posts",
                        color = if (selectedFilterMode == 2) Color.White else NoiseNormalText,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                }
            }
        }

        // 3. Category Hashtag Chips
        item {
            val isLight = ThemeManager.isLightMode
            val unselectedCategoryBrush = Brush.horizontalGradient(
                listOf(
                    if (isLight) Color(0xFFFFFFFF) else Color(0xFF14141C),
                    if (isLight) Color(0xFFFFFFFF) else Color(0xFF14141C)
                )
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categories.forEach { category ->
                    val isSelected = selectedCategoryTag == category
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(18.dp))
                            .background(if (isSelected) noisePurplePinkGradient() else unselectedCategoryBrush)
                            .border(
                                1.dp,
                                if (isSelected) NoisePink else NoiseBorder,
                                RoundedCornerShape(18.dp)
                            )
                            .clickable { selectedCategoryTag = category }
                            .padding(horizontal = 14.dp, vertical = 7.dp)
                    ) {
                        Text(
                            text = category,
                            color = if (isSelected) Color.White else NoiseNormalText,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        // 4. Trending Topics Section
        item {
            val isLight = ThemeManager.isLightMode
            Spacer(modifier = Modifier.height(18.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(if (isLight) Color(0xFFFFF3C4) else Color(0xFF291805))
                            .border(1.dp, if (isLight) VipGold else Color(0xFF59340B), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalFireDepartment,
                            contentDescription = "Trending",
                            tint = HotOrange,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Trending Topics",
                        color = NoiseWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isLight) LightLavender else Color(0xFF331024))
                            .border(1.dp, NoisePink.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "Popular Tags",
                            color = if (isLight) NoiseWhite else NoisePink,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(OnlineGreen)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Live Pulse",
                        color = NoiseTextSecondary,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            LazyRow(
                contentPadding = PaddingValues(horizontal = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(trendingTopics, key = { it.rank }) { topic ->
                    TrendingTopicCard(
                        topic = topic,
                        onClick = { selectedCategoryTag = topic.tag }
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // TAGS row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.LocalOffer,
                    contentDescription = null,
                    tint = NoiseTextSecondary,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = "TAGS:",
                    color = NoiseTextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                trendingTopics.forEach { topic ->
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isLight) Color(0xFFF8F8FC) else Color(0xFF14141C))
                            .border(1.dp, NoiseBorder, RoundedCornerShape(16.dp))
                            .clickable { selectedCategoryTag = topic.tag }
                            .padding(horizontal = 12.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = topic.tag,
                            color = NoiseNormalText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = topic.countLabel,
                            color = NoiseTextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
        }

        // 5. Content Area: Registered Accounts List OR 3-Column Photo Grid
        if (selectedFilterMode == 1 || searchQuery.isNotBlank()) {
            if (filteredAccounts.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No matching registered accounts found for \"$searchQuery\"",
                            color = NoiseTextSecondary,
                            fontSize = 13.sp
                        )
                    }
                }
            } else {
                items(filteredAccounts, key = { it.id }) { account ->
                    val targetUid = account.userId.ifBlank { account.id }
                    RegisteredAccountRow(
                        account = account,
                        isFollowing = targetUid in followedUserIds,
                        onInspectClick = { onInspectUser(account) },
                        onFollowClick = { onFollowUser(account) },
                        onMessageClick = { onMessageUser(account) }
                    )
                }
            }
        }

        if (selectedFilterMode != 1) {
            item {
                val rows = filteredPosts.chunked(3)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    rows.forEach { rowPosts ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            for (i in 0 until 3) {
                                val post = rowPosts.getOrNull(i)
                                if (post != null) {
                                    NoiseMediaImage(
                                        mediaKey = post.mediaKey,
                                        contentDescription = post.caption,
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(1f)
                                    )
                                } else {
                                    Spacer(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(1f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TrendingTopicCard(
    topic: TrendingTopicItem,
    onClick: () -> Unit
) {
    val isLight = ThemeManager.isLightMode
    Column(
        modifier = Modifier
            .width(196.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(if (isLight) Color(0xFFFFFFFF) else Color(0xFF111116))
            .border(1.dp, if (isLight) NoiseBorder else Color(0xFF232330), RoundedCornerShape(18.dp))
            .clickable { onClick() }
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = topic.rank,
                    color = NoiseTextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(8.dp))
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isLight) VipGold else Color(0xFF2E1B05))
                        .border(1.dp, if (isLight) VipGold else Color(0xFF6B3E0A), RoundedCornerShape(10.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🔥 HOT",
                        color = if (isLight) Color(0xFF171525) else HotOrange,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
            Text(
                text = topic.category,
                color = NoiseTextSecondary,
                fontSize = 11.sp
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = topic.tag,
            color = NoiseWhite,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = topic.subtitle,
            color = NoiseTextSecondary,
            fontSize = 12.sp
        )

        Spacer(modifier = Modifier.height(12.dp))
        HorizontalDivider(color = if (isLight) NoiseBorder else Color(0xFF20202C), thickness = 0.8.dp)
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = topic.countLabel,
                color = NoiseTextSecondary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                    contentDescription = null,
                    tint = OnlineGreen,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = topic.growthLabel,
                    color = OnlineGreen,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
fun RegisteredAccountRow(
    account: UserProfile,
    isFollowing: Boolean = false,
    onInspectClick: () -> Unit = {},
    onFollowClick: () -> Unit,
    onMessageClick: () -> Unit
) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(NoiseCard)
            .border(1.dp, NoiseBorder, RoundedCornerShape(16.dp))
            .clickable { onInspectClick() }
            .padding(12.dp)
            .testTag("search_user_row_${account.username}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            NoiseAvatar(
                avatarKey = account.avatarKey.ifBlank { account.username.take(1).uppercase() },
                size = 46.dp,
                showOnlineDot = account.online
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val acctVipLevel = account.activeVipBadgeLevel.toInt().coerceIn(0, 10)
                    VipAnimatedUsername(
                        username = account.displayName.ifBlank { account.username },
                        vipLevel = acctVipLevel,
                        fontSize = 14.sp
                    )
                    if (acctVipLevel > 0) {
                        Spacer(modifier = Modifier.width(5.dp))
                        VipInlineGemBadge(
                            vipLevel = acctVipLevel,
                            gemSize = 18.dp,
                            showLabel = true
                        )
                    }
                    if (account.verified) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.Verified,
                            contentDescription = "Verified",
                            tint = VerifiedBlue,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
                Text(
                    text = "@${account.username} • ${account.followersCount} followers",
                    color = NoiseTextSecondary,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(3.dp))
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (ThemeManager.isLightMode) LightLavender else Color(0xFF1D152B))
                        .border(
                            1.dp,
                            if (ThemeManager.isLightMode) NoisePurple.copy(alpha = 0.35f) else Color.Transparent,
                            RoundedCornerShape(8.dp)
                        )
                        .clickable {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Noise ID", account.id))
                            Toast.makeText(context, "Copied ID: ${account.id}", Toast.LENGTH_SHORT).show()
                        }
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Fingerprint,
                        contentDescription = null,
                        tint = NoisePurple,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "ID: ${account.id.take(14)}...",
                        color = if (ThemeManager.isLightMode) NoiseNormalText else Color(0xFFD6BCFA),
                        fontSize = 10.sp
                    )
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isFollowing) Brush.horizontalGradient(listOf(NoiseCardElevated, NoiseCardElevated))
                        else Brush.horizontalGradient(listOf(NoisePink, NoisePurple))
                    )
                    .border(
                        1.dp,
                        if (isFollowing) NoiseBorder else Color.Transparent,
                        RoundedCornerShape(12.dp)
                    )
                    .clickable { onFollowClick() }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
                    .testTag("follow_button_${account.username}")
            ) {
                Text(
                    text = if (isFollowing) "Following" else "Follow",
                    color = if (isFollowing) NoiseWhite else Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(NoiseCardElevated)
                    .border(1.dp, NoiseBorder, RoundedCornerShape(12.dp))
                    .clickable { onMessageClick() }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
                    .testTag("message_button_${account.username}")
            ) {
                Text(
                    text = "Message",
                    color = NoiseWhite,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
fun UserProfileScreen(
    currentUserId: String,
    userProfile: UserProfile?,
    postsState: UiState<List<Post>>,
    savedPostIds: Set<String> = emptySet(),
    followersList: List<UserProfile> = emptyList(),
    followingList: List<UserProfile> = emptyList(),
    registeredAccounts: List<UserProfile> = emptyList(),
    vipBadgeCatalog: List<com.example.data.VipBadgeCatalogItem> = emptyList(),
    onPurchaseVipBadge: (Int) -> Unit = {},
    onSelectActiveVipBadge: (Int) -> Unit = {},
    onFollowAccount: (UserProfile) -> Unit = {},
    onInspectUserProfile: (String) -> Unit = {},
    onToggleThemeMode: (String) -> Unit = {},
    isSuperOwner: Boolean = false,
    onOpenSuperOwnerPanel: (Int) -> Unit = {},
    onOpenCentralAiAdmin: () -> Unit = {},
    onOpenGeminiDeveloperStudio: () -> Unit = {},
    onSaveProfile: (username: String, displayName: String, bio: String, website: String, avatarKey: String) -> Unit,
    onDeletePost: (String) -> Unit
) {
    val context = LocalContext.current
    val profile = userProfile ?: UserProfile(id = currentUserId, userId = currentUserId)
    val allPosts = (postsState as? UiState.Success)?.data ?: emptyList()
    val userPosts = remember(allPosts, currentUserId, profile.username) {
        allPosts.filter { it.userId == currentUserId }
    }

    val resolvedFollowersList = remember(followersList, registeredAccounts, currentUserId) {
        if (followersList.isNotEmpty()) {
            followersList
        } else {
            registeredAccounts.filter { currentUserId in it.followedUserIds }
        }
    }
    val resolvedFollowingList = remember(followingList, registeredAccounts, profile.followedUserIds) {
        if (followingList.isNotEmpty()) {
            followingList
        } else {
            val followedIds = profile.followedUserIds.toSet()
            registeredAccounts.filter { (it.userId.ifBlank { it.id }) in followedIds }
        }
    }

    var selectedProfileTab by remember { mutableIntStateOf(0) } // 0 = Grid, 1 = Reels, 2 = Saved
    var showEditDialog by remember { mutableStateOf(false) }
    var showVipBadgeShopOnProfile by remember { mutableStateOf(false) }
    var followModalType by remember { mutableStateOf<String?>(null) } // "followers" or "following"
    var selectedProfilePreviewPost by remember { mutableStateOf<Post?>(null) }
    val activeVipBadgeLevel = profile.activeVipBadgeLevel.toInt().coerceIn(0, 10)
    val ownedBadgeLevels = remember(profile.ownedVipBadges) {
        profile.ownedVipBadges.map { it.toInt() }.toSet().ifEmpty { setOf(1, 4) }
    }

    val avatarRingBrush = Brush.sweepGradient(
        listOf(NoisePurple, NoiseCyan, NoisePink, NoisePurple)
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(NoiseBlack)
            .testTag("user_profile_screen"),
        contentPadding = PaddingValues(bottom = 28.dp)
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp)
            ) {
                // Avatar + Stats Row matching Screenshot 6
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Large Avatar with gradient ring + pink camera badge
                    Box(
                        modifier = Modifier
                            .size(96.dp)
                            .clickable { showEditDialog = true }
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .border(2.5.dp, avatarRingBrush, CircleShape)
                                .padding(5.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF5B34DA)),
                            contentAlignment = Alignment.Center
                        ) {
                            val isCustomAvatar = profile.avatarKey.startsWith("content://") ||
                                profile.avatarKey.startsWith("file://") ||
                                profile.avatarKey.startsWith("http") ||
                                profile.avatarKey.uppercase() in setOf("COUPLE", "ROSES", "MIC", "WAVE", "LAKE", "CANYON")
                            if (isCustomAvatar) {
                                NoiseAvatar(
                                    avatarKey = profile.avatarKey,
                                    size = 86.dp
                                )
                            } else {
                                Text(
                                    text = profile.avatarKey.take(1).uppercase().ifBlank { "S" },
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 38.sp
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Brush.linearGradient(listOf(NoisePink, NoisePurple)))
                                .border(2.dp, NoiseBlack, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = "Change Profile Photo",
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    // Stats: posts | followers (clickable) | following (clickable)
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 24.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ProfileStatColumn(
                            count = "${userPosts.size}",
                            label = "posts"
                        )
                        ProfileStatColumn(
                            count = "${profile.followersCount.coerceAtLeast(resolvedFollowersList.size.toLong())}",
                            label = "followers",
                            testTag = "profile_followers_stat",
                            onClick = { followModalType = "followers" }
                        )
                        ProfileStatColumn(
                            count = "${profile.followingCount.coerceAtLeast(resolvedFollowingList.size.toLong())}",
                            label = "following",
                            testTag = "profile_following_stat",
                            onClick = { followModalType = "following" }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Username + VIP Animated Username + Luxury Gemstone Badge + Fingerprint ID pill
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    VipAnimatedUsername(
                        username = profile.username,
                        vipLevel = activeVipBadgeLevel,
                        fontSize = 16.sp
                    )

                    if (activeVipBadgeLevel > 0) {
                        VipInlineGemBadge(
                            vipLevel = activeVipBadgeLevel,
                            gemSize = 24.dp,
                            showLabel = true
                        )
                    }

                    val displayId = currentUserId.ifBlank { "6VNAsBIMzdU6Zfr3BtmYzqP3gu22" }
                    val isLight = ThemeManager.isLightMode
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isLight) LightLavender else Color(0xFF1A1228))
                            .border(
                                1.dp,
                                if (isLight) NoisePurple.copy(alpha = 0.35f) else Color(0xFF3E246B),
                                RoundedCornerShape(14.dp)
                            )
                            .clickable {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("Noise User ID", displayId))
                                Toast.makeText(context, "Copied ID: $displayId", Toast.LENGTH_SHORT).show()
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .testTag("copy_user_id_pill"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Fingerprint,
                            contentDescription = "User ID",
                            tint = NoisePurple,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "ID: $displayId",
                            color = if (isLight) NoiseNormalText else Color(0xFFD8B4FE),
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy ID",
                            tint = NoiseTextSecondary,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "@${profile.username}",
                    color = NoiseTextSecondary,
                    fontSize = 13.sp
                )

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = profile.bio,
                    color = NoiseNormalText,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = "Website",
                        tint = NoiseLinkColor,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = profile.website,
                        color = NoiseLinkColor,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action Buttons Row: [Edit profile] [Share profile] [Trash Icon]
                val isLightProfile = ThemeManager.isLightMode
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isLightProfile) Color(0xFFF8F8FC) else Color(0xFF14141C))
                            .border(1.dp, if (isLightProfile) NoiseBorder else Color(0xFF282838), RoundedCornerShape(10.dp))
                            .clickable { showEditDialog = true }
                            .testTag("edit_profile_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Edit profile",
                            color = NoiseWhite,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isLightProfile) Color(0xFFF8F8FC) else Color(0xFF14141C))
                            .border(1.dp, if (isLightProfile) NoiseBorder else Color(0xFF282838), RoundedCornerShape(10.dp))
                            .clickable {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(
                                    ClipData.newPlainText(
                                        "Noise Profile",
                                        "https://noise.app/@${profile.username}"
                                    )
                                )
                                Toast.makeText(context, "Profile link copied!", Toast.LENGTH_SHORT).show()
                            }
                            .testTag("share_profile_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Share profile",
                            color = NoiseWhite,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isLightProfile) Color(0xFFFEF2F2) else Color(0xFF1A0F12))
                            .border(1.dp, if (isLightProfile) DangerRed.copy(alpha = 0.35f) else Color(0xFF3E1820), RoundedCornerShape(10.dp))
                            .clickable {
                                userPosts.firstOrNull()?.let { onDeletePost(it.id) }
                            }
                            .testTag("delete_latest_post_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Manage Posts",
                            tint = DangerRed,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // VIP 1-10 Luxury Gemstone Badge Shop Toggle Banner on User Profile (clean white/lavender card in Light Mode, dark luxury card in Dark Mode)
                val isProfileLight = ThemeManager.isLightMode
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (isProfileLight) {
                                Brush.horizontalGradient(
                                    listOf(Color(0xFFFFFFFF), Color(0xFFF8F8FC), Color(0xFFF3E5F8))
                                )
                            } else {
                                Brush.horizontalGradient(
                                    listOf(Color(0xFF1A102D), Color(0xFF281238), Color(0xFF1A102D))
                                )
                            }
                        )
                        .border(1.2.dp, VipGold.copy(alpha = 0.85f), RoundedCornerShape(14.dp))
                        .clickable { showVipBadgeShopOnProfile = !showVipBadgeShopOnProfile }
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                        .testTag("profile_vip_badge_shop_toggle"),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        VipGemstoneBadgeCanvas(
                            vipLevel = activeVipBadgeLevel,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "VIP 1 – VIP 10 Luxury 3D Gemstone Badges",
                                color = if (isProfileLight) Color(0xFF000000) else VipGold,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 12.sp
                            )
                            Text(
                                text = "Active: VIP $activeVipBadgeLevel • Tap to ${if (showVipBadgeShopOnProfile) "hide" else "open"} Badge Shop",
                                color = if (isProfileLight) Color(0xFF4A4A5A) else Color(0xFFE2D4F8),
                                fontSize = 10.sp
                            )
                        }
                    }
                    Text(
                        text = if (showVipBadgeShopOnProfile) "HIDE ▲" else "SHOP ▼",
                        color = NoisePink,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 11.sp
                    )
                }

                if (showVipBadgeShopOnProfile && vipBadgeCatalog.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    VipLuxuryBadgeShopSection(
                        catalog = vipBadgeCatalog,
                        userCoins = profile.coins,
                        ownedVipBadgeLevels = ownedBadgeLevels,
                        activeVipBadgeLevel = activeVipBadgeLevel,
                        onPurchaseBadge = onPurchaseVipBadge,
                        onEquipBadge = onSelectActiveVipBadge
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Light Mode / Dark Mode Theme Switch Card (Light Lavender background with dark text in Light Mode)
                val isLightMode = ThemeManager.isLightMode
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isLightMode) LightLavender else NoiseCard)
                        .border(
                            1.dp,
                            if (isLightMode) NoisePurple.copy(alpha = 0.35f) else NoiseBorder,
                            RoundedCornerShape(14.dp)
                        )
                        .clickable {
                            val nextMode = ThemeManager.toggleThemeMode(context)
                            onToggleThemeMode(nextMode)
                        }
                        .padding(horizontal = 14.dp, vertical = 11.dp)
                        .testTag("profile_theme_mode_toggle"),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(noisePrimaryActionBrush()),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isLightMode) "☀️" else "🌙",
                                fontSize = 16.sp
                            )
                        }
                        Column {
                            Text(
                                text = if (isLightMode) "Appearance: Light Mode" else "Appearance: Dark Mode",
                                color = NoiseWhite,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "Tap to switch between Dark Mode & Light Mode",
                                color = NoiseTextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isLightMode) Color(0xFFFFFFFF) else NoiseCardElevated)
                            .border(1.dp, NoiseBorder, RoundedCornerShape(20.dp))
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = if (isLightMode) "LIGHT ☀️" else "DARK 🌙",
                            color = if (isLightMode) NoisePurple else VipGold,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 11.sp
                        )
                    }
                }

                if (isSuperOwner) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color(0xFF2D0A4E), Color(0xFF7B1FA2), Color(0xFFB71C1C))
                                )
                            )
                            .border(1.4.dp, VipGold, RoundedCornerShape(14.dp))
                            .clickable { onOpenSuperOwnerPanel(0) }
                            .padding(14.dp)
                            .testTag("profile_super_owner_panel_button")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "👑 SUPER OWNER FULL CONTROL (@sebin_mr)",
                                    color = VipGold,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 13.sp
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Owner Chatroom Delete • Posts/Stories/Groups • Ban Users • Reports",
                                    color = Color.White.copy(alpha = 0.9f),
                                    fontSize = 11.sp
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(VipGold)
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = "OPEN PANEL",
                                    color = Color.Black,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color(0xFF1A0836), Color(0xFF5B21B6), Color(0xFFBE185D))
                                )
                            )
                            .border(1.4.dp, Color(0xFF00E676), RoundedCornerShape(14.dp))
                            .clickable { onOpenCentralAiAdmin() }
                            .padding(14.dp)
                            .testTag("profile_central_ai_admin_button")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "🤖 CENTRAL AI ADMIN & LIVE UPDATE",
                                    color = VipGold,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 13.sp
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "AI App Editor • Live Theme & Pages • Publish to All Users • Rollback",
                                    color = Color.White.copy(alpha = 0.92f),
                                    fontSize = 11.sp
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(Color(0xFF00E676))
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = "AI STUDIO",
                                    color = Color.Black,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color(0xFF0F172A), Color(0xFF3B0764), Color(0xFF831843))
                                )
                            )
                            .border(1.4.dp, Color(0xFF00E5FF), RoundedCornerShape(14.dp))
                            .clickable { onOpenGeminiDeveloperStudio() }
                            .padding(14.dp)
                            .testTag("profile_gemini_developer_studio_button")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "💻 GEMINI AI DEVELOPER STUDIO (@sebin_mr)",
                                    color = Color(0xFF00E5FF),
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 13.sp
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "AI Code Generator • Auto Bug Fixer • Code Editor • GitHub • Build & Test",
                                    color = Color.White.copy(alpha = 0.92f),
                                    fontSize = 11.sp
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(Color(0xFF00E5FF))
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = "DEV IDE",
                                    color = Color.Black,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // Profile Tabs: Grid | Reels | Saved matching Screenshot 6
        item {
            HorizontalDivider(color = NoiseBorder.copy(alpha = 0.6f), thickness = 0.8.dp)
            Row(modifier = Modifier.fillMaxWidth()) {
                val tabs = listOf(
                    Icons.Default.GridOn to "Grid",
                    Icons.Default.Movie to "Reels",
                    Icons.Default.BookmarkBorder to "Saved"
                )
                tabs.forEachIndexed { index, (icon, label) ->
                    val isSelected = selectedProfileTab == index
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedProfileTab = index }
                            .padding(top = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = label,
                            tint = if (isSelected) NoiseWhite else NoiseTextMuted,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(2.dp)
                                .background(if (isSelected) NoiseWhite else Color.Transparent)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(3.dp))
        }

        // 3-Column Photo & Video Grid matching Screenshot 6
        item {
            val displayGridPosts = when (selectedProfileTab) {
                1 -> userPosts.filter { it.mediaType == "video" }
                2 -> allPosts.filter { it.id in savedPostIds }
                else -> userPosts
            }
            val rows = displayGridPosts.chunked(3)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                rows.forEach { rowPosts ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        for (i in 0 until 3) {
                            val post = rowPosts.getOrNull(i)
                            if (post != null) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .clickable { selectedProfilePreviewPost = post }
                                        .testTag("profile_grid_post_${post.id}")
                                ) {
                                    NoiseMediaImage(
                                        mediaKey = post.effectiveMediaSource(),
                                        contentDescription = post.caption,
                                        thumbnailUrl = post.thumbnailUrl,
                                        storagePath = post.storagePath,
                                        cloudMediaId = post.cloudMediaId,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    if (post.mediaType == "video") {
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(6.dp)
                                                .size(22.dp)
                                                .clip(CircleShape)
                                                .background(Color.Black.copy(alpha = 0.65f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Movie,
                                                contentDescription = "Video Clip",
                                                tint = NoisePink,
                                                modifier = Modifier.size(13.dp)
                                            )
                                        }
                                    }
                                }
                            } else {
                                Spacer(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    val activePreviewPost = selectedProfilePreviewPost
    if (activePreviewPost != null) {
        AlertDialog(
            onDismissRequest = { selectedProfilePreviewPost = null },
            containerColor = NoiseCard,
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        NoiseAvatar(
                            avatarKey = activePreviewPost.profilePhoto.ifBlank { activePreviewPost.authorAvatarKey },
                            size = 32.dp,
                            showOnlineDot = false
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "@${activePreviewPost.username}",
                                color = NoiseWhite,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            if (activePreviewPost.location.isNotBlank()) {
                                Text(
                                    text = activePreviewPost.location,
                                    color = NoiseTextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                    TextButton(onClick = { selectedProfilePreviewPost = null }) {
                        Text("Close", color = NoisePink, fontWeight = FontWeight.Bold)
                    }
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    if (activePreviewPost.mediaType == "video") {
                        NoiseVideoPlayer(
                            mediaKey = activePreviewPost.effectiveMediaSource(),
                            contentDescription = activePreviewPost.caption,
                            autoPlay = true,
                            thumbnailUrl = activePreviewPost.thumbnailUrl,
                            storagePath = activePreviewPost.storagePath,
                            cloudMediaId = activePreviewPost.cloudMediaId,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(14.dp))
                        )
                    } else {
                        NoiseMediaImage(
                            mediaKey = activePreviewPost.effectiveMediaSource(),
                            contentDescription = activePreviewPost.caption,
                            thumbnailUrl = activePreviewPost.thumbnailUrl,
                            storagePath = activePreviewPost.storagePath,
                            cloudMediaId = activePreviewPost.cloudMediaId,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(14.dp))
                        )
                    }

                    if (activePreviewPost.caption.isNotBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = activePreviewPost.caption,
                            color = NoiseWhite,
                            fontSize = 13.sp
                        )
                    }
                }
            },
            confirmButton = {}
        )
    }

    if (showEditDialog) {
        EditProfileModal(
            initialProfile = profile,
            onDismiss = { showEditDialog = false },
            onSave = { username, displayName, bio, website, avatarKey ->
                onSaveProfile(username, displayName, bio, website, avatarKey)
                showEditDialog = false
            }
        )
    }

    followModalType?.let { type ->
        val listToDisplay = if (type == "followers") resolvedFollowersList else resolvedFollowingList
        FollowListModal(
            title = if (type == "followers") "Followers" else "Following",
            users = listToDisplay,
            followedUserIds = profile.followedUserIds.toSet(),
            currentUserId = currentUserId,
            onFollowToggle = onFollowAccount,
            onSelectUser = { uid ->
                followModalType = null
                if (uid != currentUserId) onInspectUserProfile(uid)
            },
            onDismiss = { followModalType = null }
        )
    }
}

@Composable
private fun ProfileStatColumn(
    count: String,
    label: String,
    testTag: String? = null,
    onClick: (() -> Unit)? = null
) {
    val mod = Modifier
        .let { if (onClick != null) it.clickable { onClick() } else it }
        .let { if (testTag != null) it.testTag(testTag) else it }
        .padding(horizontal = 4.dp, vertical = 2.dp)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = mod
    ) {
        Text(
            text = count,
            color = NoiseWhite,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
        )
        Text(
            text = label,
            color = NoiseTextSecondary,
            fontSize = 13.sp
        )
    }
}

@Composable
fun FollowListModal(
    title: String,
    users: List<UserProfile>,
    followedUserIds: Set<String>,
    currentUserId: String,
    onFollowToggle: (UserProfile) -> Unit,
    onSelectUser: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NoiseCard,
        title = {
            Text(
                text = "$title (${users.size})",
                color = NoiseWhite,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 17.sp
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("follow_list_modal_${title.lowercase()}"),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (users.isEmpty()) {
                    Text(
                        text = "No accounts in $title yet.",
                        color = NoiseTextSecondary,
                        fontSize = 13.sp
                    )
                } else {
                    users.forEach { item ->
                        val targetUid = item.userId.ifBlank { item.id }
                        val isFollowed = targetUid in followedUserIds
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(NoiseCardElevated)
                                .clickable { onSelectUser(targetUid) }
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                NoiseAvatar(
                                    avatarKey = item.avatarKey.ifBlank { item.username.take(1).uppercase() },
                                    size = 38.dp,
                                    showOnlineDot = item.online
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "@${item.username}",
                                            color = NoiseWhite,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                        Spacer(modifier = Modifier.width(5.dp))
                                        VipInlineGemBadge(
                                            vipLevel = (item.activeVipBadgeLevel.takeIf { it > 0L } ?: if (item.vip) 4L else 1L).toInt(),
                                            gemSize = 17.dp,
                                            showLabel = true
                                        )
                                    }
                                    if (item.displayName.isNotBlank()) {
                                        Text(
                                            text = item.displayName,
                                            color = NoiseTextSecondary,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }
                            if (targetUid != currentUserId) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            if (isFollowed) NoiseCard
                                            else NoisePink
                                        )
                                        .border(
                                            1.dp,
                                            if (isFollowed) NoiseBorder else Color.Transparent,
                                            RoundedCornerShape(10.dp)
                                        )
                                        .clickable { onFollowToggle(item) }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                        .testTag("follow_modal_action_${item.username}")
                                ) {
                                    Text(
                                        text = if (isFollowed) "Following" else "Follow",
                                        color = if (isFollowed) NoiseWhite else Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = NoiseTextSecondary)
            }
        }
    )
}

@Composable
fun EditProfileModal(
    initialProfile: UserProfile,
    onDismiss: () -> Unit,
    onSave: (username: String, displayName: String, bio: String, website: String, avatarKey: String) -> Unit
) {
    val context = LocalContext.current
    var username by remember { mutableStateOf(initialProfile.username) }
    var displayName by remember { mutableStateOf(initialProfile.displayName) }
    var bio by remember { mutableStateOf(initialProfile.bio) }
    var website by remember { mutableStateOf(initialProfile.website) }
    var avatarKey by remember { mutableStateOf(initialProfile.avatarKey) }

    val avatarPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val (persistedKey, _) = persistPickedMediaUri(context, uri, "photo")
            avatarKey = persistedKey
        }
    }

    val presetAvatars = listOf("S", "COUPLE", "ROSES", "MIC", "LAKE", "CANYON")

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NoiseCard,
        title = {
            Text("Edit Noise Profile", color = NoiseWhite, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    NoiseAvatar(avatarKey = avatarKey, size = 54.dp)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(Brush.horizontalGradient(listOf(NoisePink, NoisePurple)))
                                .clickable {
                                    runCatching {
                                        avatarPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    }
                                }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                .testTag("edit_profile_pick_photo_button")
                        ) {
                            Text(
                                text = "Choose from Gallery",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.horizontalScroll(rememberScrollState())
                        ) {
                            presetAvatars.forEach { preset ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (avatarKey == preset) NoisePink.copy(alpha = 0.25f) else NoiseCardElevated)
                                        .border(
                                            1.dp,
                                            if (avatarKey == preset) NoisePink else NoiseBorder,
                                            RoundedCornerShape(8.dp)
                                        )
                                        .clickable { avatarKey = preset }
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = preset,
                                        color = NoiseWhite,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Username", color = NoiseTextSecondary) },
                    singleLine = true
                )
                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = { Text("Display Name", color = NoiseTextSecondary) },
                    singleLine = true
                )
                OutlinedTextField(
                    value = bio,
                    onValueChange = { bio = it },
                    label = { Text("Bio", color = NoiseTextSecondary) }
                )
                OutlinedTextField(
                    value = website,
                    onValueChange = { website = it },
                    label = { Text("Website", color = NoiseTextSecondary) },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(username, displayName, bio, website, avatarKey) },
                colors = ButtonDefaults.buttonColors(containerColor = NoisePink)
            ) {
                Text("Save", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = NoiseTextSecondary)
            }
        }
    )
}

@Composable
fun OtherUserProfileModal(
    user: UserProfile,
    isFollowing: Boolean,
    userPosts: List<Post> = emptyList(),
    followersList: List<UserProfile> = emptyList(),
    followingList: List<UserProfile> = emptyList(),
    currentUserId: String = "",
    followedUserIds: Set<String> = emptySet(),
    isSuperOwner: Boolean = false,
    onFollowToggle: () -> Unit,
    onFollowAccountFromList: (UserProfile) -> Unit = {},
    onMessageClick: () -> Unit,
    onReportUser: (String) -> Unit = {},
    onOwnerUpdateStatus: (String, String) -> Unit = { _, _ -> },
    onOwnerDeletePost: (Post) -> Unit = {},
    onDismiss: () -> Unit
) {
    var subModalType by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NoiseCard,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                NoiseAvatar(
                    avatarKey = user.avatarKey.ifBlank { user.username.take(1).uppercase() },
                    size = 54.dp,
                    showOnlineDot = user.online
                )
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val userVip = user.activeVipBadgeLevel.toInt().coerceIn(0, 10)
                        VipAnimatedUsername(
                            username = user.displayName.ifBlank { user.username },
                            vipLevel = userVip,
                            fontSize = 17.sp
                        )
                        if (userVip > 0) {
                            Spacer(modifier = Modifier.width(5.dp))
                            VipInlineGemBadge(
                                vipLevel = userVip,
                                gemSize = 20.dp,
                                showLabel = true
                            )
                        }
                        if (user.verified) {
                            Spacer(modifier = Modifier.width(5.dp))
                            Icon(
                                imageVector = Icons.Default.Verified,
                                contentDescription = "Verified",
                                tint = VerifiedBlue,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Text(
                        text = "@${user.username}",
                        color = NoiseTextSecondary,
                        fontSize = 13.sp
                    )
                    Text(
                        text = if (user.online) "Online now" else user.lastActiveLabel.ifBlank { "Offline" },
                        color = if (user.online) OnlineGreen else NoiseTextMuted,
                        fontSize = 11.sp
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("inspected_user_profile_modal"),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (user.bio.isNotBlank()) {
                    Text(
                        text = user.bio,
                        color = NoiseWhite,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(NoiseCardElevated)
                        .border(1.dp, NoiseBorder, RoundedCornerShape(14.dp))
                        .padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ProfileStatColumn(
                        count = "${userPosts.size.toLong().coerceAtLeast(user.postsCount)}",
                        label = "posts"
                    )
                    ProfileStatColumn(
                        count = "${user.followersCount.coerceAtLeast(followersList.size.toLong())}",
                        label = "followers",
                        testTag = "inspected_followers_stat",
                        onClick = { subModalType = "followers" }
                    )
                    ProfileStatColumn(
                        count = "${user.followingCount.coerceAtLeast(followingList.size.toLong())}",
                        label = "following",
                        testTag = "inspected_following_stat",
                        onClick = { subModalType = "following" }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isFollowing) Brush.horizontalGradient(listOf(NoiseCardElevated, NoiseCardElevated))
                                else Brush.horizontalGradient(listOf(NoisePink, NoisePurple))
                            )
                            .border(
                                1.dp,
                                if (isFollowing) NoiseBorder else Color.Transparent,
                                RoundedCornerShape(12.dp)
                            )
                            .clickable { onFollowToggle() }
                            .padding(vertical = 10.dp)
                            .testTag("modal_follow_button_${user.username}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isFollowing) "Following" else "Follow",
                            color = if (isFollowing) NoiseWhite else Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(NoiseCardElevated)
                            .border(1.dp, NoisePink.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                            .clickable {
                                onDismiss()
                                onMessageClick()
                            }
                            .padding(vertical = 10.dp)
                            .testTag("modal_message_button_${user.username}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Message",
                            color = NoiseWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }

                // Inspected user's uploaded posts grid
                if (userPosts.isNotEmpty()) {
                    Text(
                        text = "Posts by @${user.username}",
                        color = NoiseTextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        userPosts.take(3).forEach { post ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(8.dp))
                            ) {
                                NoiseMediaImage(
                                    mediaKey = post.effectiveMediaSource(),
                                    contentDescription = post.caption,
                                    thumbnailUrl = post.thumbnailUrl,
                                    storagePath = post.storagePath,
                                    cloudMediaId = post.cloudMediaId,
                                    modifier = Modifier.fillMaxSize()
                                )
                                if (isSuperOwner) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(4.dp)
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFD50000))
                                            .clickable { onOwnerDeletePost(post) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteOutline,
                                            contentDescription = "Owner Delete Post",
                                            tint = Color.White,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                val isInspectedSuperOwner = com.example.data.SuperOwnerControlManager.isVerifiedSuperOwner(
                    uid = user.id,
                    username = user.username,
                    userProfile = user
                )
                if (!isInspectedSuperOwner && user.id != currentUserId) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(NoiseCardElevated)
                                .border(1.dp, NoiseBorder, RoundedCornerShape(10.dp))
                                .clickable { onReportUser("Reported account @${user.username}") }
                                .padding(vertical = 8.dp)
                                .testTag("report_user_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Report Account",
                                color = NoiseTextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        if (isSuperOwner) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFFFF6D00))
                                    .clickable { onOwnerUpdateStatus("suspended", "Suspended by Super Owner @sebin_mr") }
                                    .padding(vertical = 8.dp)
                                    .testTag("owner_suspend_user_button"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Suspend User",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFFD50000))
                                    .clickable { onOwnerUpdateStatus("banned", "Banned by Super Owner @sebin_mr") }
                                    .padding(vertical = 8.dp)
                                    .testTag("owner_ban_user_button"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Ban User",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = NoiseTextSecondary)
            }
        }
    )

    subModalType?.let { type ->
        FollowListModal(
            title = if (type == "followers") "Followers" else "Following",
            users = if (type == "followers") followersList else followingList,
            followedUserIds = followedUserIds,
            currentUserId = currentUserId,
            onFollowToggle = onFollowAccountFromList,
            onSelectUser = { subModalType = null },
            onDismiss = { subModalType = null }
        )
    }
}

