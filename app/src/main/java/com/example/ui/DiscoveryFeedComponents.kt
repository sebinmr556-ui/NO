package com.example.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
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
import com.example.data.DiscoveryCategory
import com.example.data.DiscoveryContentItem
import com.example.data.DiscoveryGlobalConfig
import com.example.data.DiscoveryImportLog
import com.example.data.DiscoverySourceConfig
import com.example.data.KERALA_DISTRICTS
import com.example.data.KeralaNewsCategory
import com.example.ui.theme.LightLavender
import com.example.ui.theme.NoiseBlack
import com.example.ui.theme.NoiseBorder
import com.example.ui.theme.NoiseCard
import com.example.ui.theme.NoiseCardElevated
import com.example.ui.theme.NoisePink
import com.example.ui.theme.NoisePurple
import com.example.ui.theme.NoiseTextSecondary
import com.example.ui.theme.NoiseWhite
import com.example.ui.theme.OnlineGreen
import com.example.ui.theme.ThemeManager
import com.example.ui.theme.VipGold
import com.example.ui.theme.VipGoldBorder

@Composable
fun DiscoveryCategoryScrollBar(
    selectedCategory: DiscoveryCategory,
    disabledCategories: List<String>,
    isSyncing: Boolean,
    isAdmin: Boolean,
    refreshIntervalMinutes: Long,
    onSelectCategory: (DiscoveryCategory) -> Unit,
    onRefreshCategory: () -> Unit,
    onOpenAdminPanel: () -> Unit
) {
    val isLight = ThemeManager.isLightMode
    val availableCategories = remember(disabledCategories) {
        DiscoveryCategory.entries.filter { it.key !in disabledCategories }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isLight) Color(0xFFFFFFFF) else NoiseBlack)
            .padding(top = 10.dp, bottom = 8.dp)
            .testTag("home_discovery_category_section")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Verified,
                    contentDescription = "Safe Discovery",
                    tint = OnlineGreen,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "DISCOVER • SAFE & VERIFIED",
                    color = NoiseWhite,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 12.sp,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "(Auto ${refreshIntervalMinutes.coerceIn(15L, 30L)}m)",
                    color = NoiseTextSecondary,
                    fontSize = 10.sp
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Pull / Manual Refresh Pill
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isLight) LightLavender else Color(0xFF191229))
                        .border(1.dp, NoisePink.copy(alpha = 0.55f), RoundedCornerShape(14.dp))
                        .clickable(enabled = !isSyncing) { onRefreshCategory() }
                        .padding(horizontal = 9.dp, vertical = 4.dp)
                        .testTag("discovery_refresh_button"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isSyncing) {
                        CircularProgressIndicator(
                            color = NoisePink,
                            strokeWidth = 1.8.dp,
                            modifier = Modifier.size(12.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh Discovery Feed",
                            tint = NoisePink,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isSyncing) "Syncing..." else "Refresh",
                        color = NoiseWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                }

                // Admin-Only Control Panel Button
                if (isAdmin) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(Brush.horizontalGradient(listOf(Color(0xFF281A08), Color(0xFF3D260A))))
                            .border(1.dp, VipGold, RoundedCornerShape(14.dp))
                            .clickable { onOpenAdminPanel() }
                            .padding(horizontal = 9.dp, vertical = 4.dp)
                            .testTag("discovery_admin_panel_button"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AdminPanelSettings,
                            contentDescription = "Discovery Admin Panel",
                            tint = VipGold,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Admin",
                            color = VipGold,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Horizontally scrollable row of small, attractive category buttons
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("home_discovery_categories_row"),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items(availableCategories, key = { it.key }) { category ->
                val isSelected = category == selectedCategory
                val accentColor = Color(category.badgeColorHex)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(18.dp))
                        .background(
                            if (isSelected) {
                                Brush.horizontalGradient(listOf(NoisePink, NoisePurple))
                            } else if (isLight) {
                                Brush.horizontalGradient(listOf(Color(0xFFF8F8FC), Color(0xFFF1EFFA)))
                            } else {
                                Brush.horizontalGradient(listOf(Color(0xFF1A1228), Color(0xFF130D1E)))
                            }
                        )
                        .border(
                            width = if (isSelected) 1.3.dp else 1.dp,
                            brush = if (isSelected) {
                                Brush.linearGradient(listOf(VipGold, NoisePink))
                            } else {
                                Brush.linearGradient(
                                    listOf(
                                        accentColor.copy(alpha = 0.45f),
                                        NoiseBorder.copy(alpha = 0.7f)
                                    )
                                )
                            },
                            shape = RoundedCornerShape(18.dp)
                        )
                        .clickable { onSelectCategory(category) }
                        .padding(horizontal = 13.dp, vertical = 7.dp)
                        .testTag("discovery_category_chip_${category.key}"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = category.emoji,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = category.label,
                            color = if (isSelected) Color.White else NoiseWhite,
                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DiscoveryContentCard(
    item: DiscoveryContentItem,
    isLiked: Boolean,
    isSaved: Boolean,
    isAdmin: Boolean = false,
    onLikeClick: () -> Unit,
    onSaveClick: () -> Unit,
    onShareClick: () -> Unit,
    onOpenContent: () -> Unit,
    onAdminHideClick: () -> Unit = {},
    onAdminDeleteClick: () -> Unit = {}
) {
    val isLight = ThemeManager.isLightMode
    val isKeralaItem = item.isKeralaNews || item.categoryKey.startsWith("kerala_")
    val keralaEnum = if (isKeralaItem) {
        KeralaNewsCategory.fromKey(item.categoryKey)
    } else null
    val catEnum = DiscoveryCategory.fromKey(item.categoryKey)
    val badgeColor = Color(keralaEnum?.badgeColorHex ?: catEnum.badgeColorHex)
    val isVideo = item.isVideoContent()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 7.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(if (isLight) Color(0xFFFFFFFF) else Color(0xFF140F21))
            .border(
                width = if (item.isBreakingNews) 1.5.dp else 1.dp,
                brush = Brush.linearGradient(
                    if (item.isBreakingNews) {
                        listOf(Color(0xFFEF4444), VipGold, NoisePink)
                    } else {
                        listOf(
                            badgeColor.copy(alpha = 0.55f),
                            NoisePink.copy(alpha = 0.35f),
                            VipGold.copy(alpha = 0.35f)
                        )
                    }
                ),
                shape = RoundedCornerShape(20.dp)
            )
            .clickable { onOpenContent() }
            .padding(14.dp)
            .testTag("discovery_content_card_${item.id}")
    ) {
        // Header Row: Category Pill + Breaking/District/Language Badges + Trusted Source Name + Date
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                if (item.isBreakingNews) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFDC2626))
                            .padding(horizontal = 7.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "🚨 BREAKING",
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 9.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(badgeColor.copy(alpha = 0.18f))
                        .border(1.dp, badgeColor.copy(alpha = 0.65f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    val emojiLabel = keralaEnum?.emoji ?: catEnum.emoji
                    val textLabel = item.categoryLabel.ifBlank { keralaEnum?.label ?: catEnum.label }
                    Text(
                        text = "$emojiLabel $textLabel",
                        color = if (isLight) NoiseWhite else badgeColor,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 10.sp
                    )
                }

                if (item.district.isNotBlank() && item.district != "All Districts") {
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF065F46).copy(alpha = 0.25f))
                            .border(1.dp, OnlineGreen.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                            .padding(horizontal = 7.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "📍 ${item.district}",
                            color = OnlineGreen,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp
                        )
                    }
                }

                if (isKeralaItem) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(VipGold.copy(alpha = 0.16f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (item.language == "ml") "മലയാളം" else "EN",
                            color = VipGold,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 9.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Icon(
                    imageVector = Icons.Default.Verified,
                    contentDescription = "Trusted Publisher",
                    tint = OnlineGreen,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = item.sourceName.ifBlank { item.sourceDomain },
                    color = NoiseWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = item.publishedDateLabel.ifBlank { "Recent" },
                color = NoiseTextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Thumbnail / Video Preview Banner
        if (item.thumbnailUrl.isNotBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(188.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF1D1530))
            ) {
                NoiseMediaImage(
                    mediaKey = item.thumbnailUrl,
                    contentDescription = item.title,
                    thumbnailUrl = item.thumbnailUrl,
                    modifier = Modifier.fillMaxSize()
                )

                if (isVideo) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.68f))
                            .border(1.5.dp, VipGold, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Play Authorized Video",
                            tint = VipGold,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }

                // Domain attribution badge at bottom-right of thumbnail
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(alpha = 0.72f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = item.sourceDomain.ifBlank { "Verified Source" },
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        // Original Title
        Text(
            text = item.title,
            color = NoiseWhite,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 16.sp,
            lineHeight = 21.sp,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )

        if (item.description.isNotBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = item.description,
                color = NoiseTextSecondary,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Action Bar: Like | Save | Share | Watch / Read More
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Like Button
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onLikeClick() }
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                        .testTag("discovery_like_${item.id}"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Like",
                        tint = if (isLiked) NoisePink else NoiseTextSecondary,
                        modifier = Modifier.size(19.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${item.likesCount}",
                        color = if (isLiked) NoisePink else NoiseTextSecondary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }

                // Save Button
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onSaveClick() }
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                        .testTag("discovery_save_${item.id}"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        contentDescription = "Save",
                        tint = if (isSaved) VipGold else NoiseTextSecondary,
                        modifier = Modifier.size(19.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isSaved) "Saved" else "Save",
                        color = if (isSaved) VipGold else NoiseTextSecondary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }

                // Share Button
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onShareClick() }
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                        .testTag("discovery_share_${item.id}"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share",
                        tint = NoiseTextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Share",
                        color = NoiseTextSecondary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }

            // Watch or Read More Primary Action Pill
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(Brush.horizontalGradient(listOf(NoisePink, NoisePurple)))
                    .border(1.dp, VipGold.copy(alpha = 0.65f), RoundedCornerShape(16.dp))
                    .clickable { onOpenContent() }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .testTag("discovery_open_${item.id}"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isVideo) Icons.Default.PlayArrow else Icons.AutoMirrored.Filled.OpenInNew,
                    contentDescription = if (isVideo) "Watch Video" else "Read More",
                    tint = Color.White,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (isVideo) "Watch" else "Read More",
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 12.sp
                )
            }
        }

        // Quick Admin Moderation Strip when user is Admin
        if (isAdmin) {
            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = NoiseBorder.copy(alpha = 0.4f), thickness = 0.6.dp)
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🛡️ Safety Score: ${item.safetyScore}% • ${item.moderationStatus.uppercase()}",
                    color = OnlineGreen,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Hide",
                        color = VipGold,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { onAdminHideClick() }
                    )
                    Text(
                        text = "Delete",
                        color = Color(0xFFEF4444),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { onAdminDeleteClick() }
                    )
                }
            }
        }
    }
}

@Composable
fun DiscoveryEmptySafeStateCard(
    category: DiscoveryCategory,
    isSyncing: Boolean,
    onRetryFetch: () -> Unit
) {
    val isLight = ThemeManager.isLightMode
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 20.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(if (isLight) Color(0xFFF8F8FC) else Color(0xFF150E24))
            .border(1.dp, NoiseBorder, RoundedCornerShape(22.dp))
            .padding(24.dp)
            .testTag("discovery_empty_safe_state"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (isSyncing) {
            CircularProgressIndicator(
                color = NoisePink,
                strokeWidth = 3.dp,
                modifier = Modifier.size(34.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Fetching verified ${category.label} updates from trusted sources...",
                color = NoiseWhite,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Running automatic safety & copyright checks before publishing.",
                color = NoiseTextSecondary,
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
        } else {
            Icon(
                imageVector = Icons.Default.Security,
                contentDescription = "Safe Content Policy",
                tint = VipGold,
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "No safe and verified content available right now.",
                color = NoiseWhite,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 15.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Category: ${category.emoji} ${category.label} • Only verified, family-safe, and legally compliant sources are displayed.",
                color = NoiseTextSecondary,
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(14.dp))
            Button(
                onClick = onRetryFetch,
                colors = ButtonDefaults.buttonColors(containerColor = NoisePink),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.testTag("discovery_retry_fetch_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Check Trusted Sources Now",
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
fun KeralaNewsHubSection(
    selectedCategory: KeralaNewsCategory,
    selectedDistrict: String,
    selectedLanguage: String,
    disabledKeralaCategories: List<String>,
    breakingNewsItems: List<DiscoveryContentItem>,
    isSyncing: Boolean,
    isExpanded: Boolean,
    onToggleExpanded: () -> Unit,
    onSelectCategory: (KeralaNewsCategory) -> Unit,
    onSelectDistrict: (String) -> Unit,
    onSelectLanguage: (String) -> Unit,
    onRefreshKeralaNews: () -> Unit,
    onOpenBreakingItem: (DiscoveryContentItem) -> Unit
) {
    val isLight = ThemeManager.isLightMode
    val availableCategories = remember(disabledKeralaCategories) {
        KeralaNewsCategory.entries.filter { it.key !in disabledKeralaCategories }
    }
    val languageOptions = listOf(
        "all" to "All • മലയാളം + EN",
        "ml" to "മലയാളം",
        "en" to "English"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(
                if (isLight) {
                    Brush.verticalGradient(listOf(Color(0xFFF3FDF8), Color(0xFFFFFFFF)))
                } else {
                    Brush.verticalGradient(listOf(Color(0xFF0A221A), Color(0xFF120E1E)))
                }
            )
            .border(
                width = 1.2.dp,
                brush = Brush.horizontalGradient(listOf(OnlineGreen, VipGold, NoisePink)),
                shape = RoundedCornerShape(22.dp)
            )
            .padding(14.dp)
            .testTag("kerala_news_hub_section")
    ) {
        // Top Header Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .weight(1f)
                    .clickable { onToggleExpanded() }
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(OnlineGreen, Color(0xFF047857)))),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "🌴", fontSize = 17.sp)
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "KERALA NEWS • കേരള വാർത്തകൾ",
                            color = NoiseWhite,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(OnlineGreen.copy(alpha = 0.2f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "LIVE 24×7",
                                color = OnlineGreen,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 9.sp
                            )
                        }
                    }
                    Text(
                        text = "Automated Malayalam & English news • 14 Districts • Trusted Publishers",
                        color = NoiseTextSecondary,
                        fontSize = 10.sp
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Refresh Kerala News button
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isLight) Color(0xFFE6F9F0) else Color(0xFF112E24))
                        .border(1.dp, OnlineGreen.copy(alpha = 0.65f), RoundedCornerShape(14.dp))
                        .clickable(enabled = !isSyncing) { onRefreshKeralaNews() }
                        .padding(horizontal = 9.dp, vertical = 5.dp)
                        .testTag("kerala_news_refresh_button"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isSyncing) {
                        CircularProgressIndicator(
                            color = OnlineGreen,
                            strokeWidth = 1.8.dp,
                            modifier = Modifier.size(12.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh Kerala News",
                            tint = OnlineGreen,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isSyncing) "Updating..." else "Refresh",
                        color = NoiseWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                }

                // Expand/Collapse toggle pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isExpanded) VipGold else NoiseCardElevated)
                        .clickable { onToggleExpanded() }
                        .padding(horizontal = 9.dp, vertical = 5.dp)
                        .testTag("kerala_news_toggle_button")
                ) {
                    Text(
                        text = if (isExpanded) "Active" else "Open",
                        color = if (isExpanded) Color.Black else NoiseWhite,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 10.sp
                    )
                }
            }
        }

        // Breaking News Live Alert Strip if any breaking news exists
        val topBreaking = breakingNewsItems.firstOrNull()
        if (topBreaking != null) {
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Brush.horizontalGradient(listOf(Color(0xFF991B1B), Color(0xFF450A0A))))
                    .border(1.dp, VipGold, RoundedCornerShape(14.dp))
                    .clickable { onOpenBreakingItem(topBreaking) }
                    .padding(horizontal = 10.dp, vertical = 7.dp)
                    .testTag("kerala_breaking_news_banner"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(VipGold)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "🚨 BREAKING",
                        color = Color.Black,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 9.sp
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = topBreaking.title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        if (isExpanded) {
            Spacer(modifier = Modifier.height(10.dp))

            // Language Filter Pills (All / Malayalam / English)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                languageOptions.forEach { (langKey, langLabel) ->
                    val active = selectedLanguage == langKey
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                if (active) Brush.horizontalGradient(listOf(OnlineGreen, Color(0xFF059669)))
                                else Brush.horizontalGradient(listOf(NoiseCardElevated, NoiseCardElevated))
                            )
                            .border(
                                1.dp,
                                if (active) VipGold else NoiseBorder,
                                RoundedCornerShape(14.dp)
                            )
                            .clickable { onSelectLanguage(langKey) }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                            .testTag("kerala_lang_chip_$langKey")
                    ) {
                        Text(
                            text = langLabel,
                            color = if (active) Color.White else NoiseWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 10 Kerala News Category Pills
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("kerala_news_categories_row"),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(availableCategories, key = { it.key }) { cat ->
                    val isSelected = cat == selectedCategory
                    val accent = Color(cat.badgeColorHex)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                if (isSelected) {
                                    Brush.horizontalGradient(listOf(NoisePink, NoisePurple))
                                } else if (isLight) {
                                    Brush.horizontalGradient(listOf(Color(0xFFFFFFFF), Color(0xFFF3FDF8)))
                                } else {
                                    Brush.horizontalGradient(listOf(Color(0xFF171326), Color(0xFF101F1A)))
                                }
                            )
                            .border(
                                width = if (isSelected) 1.3.dp else 1.dp,
                                color = if (isSelected) VipGold else accent.copy(alpha = 0.55f),
                                shape = RoundedCornerShape(16.dp)
                            )
                            .clickable { onSelectCategory(cat) }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                            .testTag("kerala_category_chip_${cat.key}")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = cat.emoji, fontSize = 12.sp)
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "${cat.label} • ${cat.malayalamLabel}",
                                color = if (isSelected) Color.White else NoiseWhite,
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 14 Kerala Districts Scrollable Filter Strip
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("kerala_districts_row"),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(KERALA_DISTRICTS, key = { it }) { district ->
                    val isSelected = district.equals(selectedDistrict, ignoreCase = true)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isSelected) VipGold
                                else if (isLight) Color(0xFFEFF6FF)
                                else Color(0xFF161324)
                            )
                            .border(
                                1.dp,
                                if (isSelected) VipGold else NoiseBorder,
                                RoundedCornerShape(12.dp)
                            )
                            .clickable { onSelectDistrict(district) }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                            .testTag("kerala_district_chip_${district.lowercase().replace(" ", "_")}")
                    ) {
                        Text(
                            text = if (district == "All Districts") "📍 All 14 Districts" else "📍 $district",
                            color = if (isSelected) Color.Black else NoiseTextSecondary,
                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun DiscoveryContentViewerDialog(
    item: DiscoveryContentItem,
    onDismiss: () -> Unit,
    onShare: () -> Unit
) {
    val context = LocalContext.current
    val isVideo = item.isVideoContent()
    val targetUrl = if (isVideo && item.videoEmbedUrl.isNotBlank()) {
        item.videoEmbedUrl
    } else {
        item.originalUrl
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .testTag("discovery_content_viewer_dialog"),
            color = NoiseBlack
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top Attribution & Control Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(NoiseCard)
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Verified,
                            contentDescription = null,
                            tint = OnlineGreen,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = item.sourceName.ifBlank { item.sourceDomain },
                                color = NoiseWhite,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = item.originalUrl,
                                color = NoiseTextSecondary,
                                fontSize = 10.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onShare) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share Original Link",
                                tint = NoiseWhite
                            )
                        }
                        IconButton(
                            onClick = {
                                runCatching {
                                    val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(item.originalUrl))
                                    context.startActivity(browserIntent)
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = "Open in External Browser",
                                tint = VipGold
                            )
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close Viewer",
                                tint = NoiseWhite
                            )
                        }
                    }
                }

                HorizontalDivider(color = NoiseBorder, thickness = 0.8.dp)

                // Authorized Original Source WebView / Video Embed
                AndroidView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    factory = { ctx ->
                        WebView(ctx).apply {
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.mediaPlaybackRequiresUserGesture = false
                            settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                            webChromeClient = WebChromeClient()
                            webViewClient = object : WebViewClient() {
                                override fun shouldOverrideUrlLoading(
                                    view: WebView?,
                                    request: WebResourceRequest?
                                ): Boolean {
                                    val nextUri = request?.url ?: return false
                                    if (nextUri.scheme != "https") return true
                                    return false
                                }
                            }
                            loadUrl(targetUrl)
                        }
                    },
                    onRelease = { webView ->
                        runCatching {
                            webView.stopLoading()
                            webView.destroy()
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun DiscoveryAdminPanelModal(
    globalConfig: DiscoveryGlobalConfig,
    sources: List<DiscoverySourceConfig>,
    allItems: List<DiscoveryContentItem>,
    importLogs: List<DiscoveryImportLog>,
    isSyncing: Boolean,
    onToggleCategoryEnabled: (String, Boolean) -> Unit,
    onToggleKeralaCategoryEnabled: (String, Boolean) -> Unit = { _, _ -> },
    onUpdateKeralaAndReelsConfig: (
        trendingAlgorithmMode: String,
        keralaNewsNotificationsEnabled: Boolean,
        trendingReelsNotificationsEnabled: Boolean
    ) -> Unit = { _, _, _ -> },
    onUpdateRefreshInterval: (Long) -> Unit,
    onAddCustomSource: (categoryKey: String, name: String, feedUrl: String, contentType: String) -> Unit,
    onToggleSourceEnabled: (DiscoverySourceConfig, Boolean) -> Unit,
    onDeleteSource: (String) -> Unit,
    onModerateItem: (itemId: String, newStatus: String, notes: String) -> Unit,
    onDeleteItem: (itemId: String, blockDomain: String) -> Unit,
    onTriggerImmediateSync: () -> Unit,
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    var newSourceCategoryKey by remember { mutableStateOf(DiscoveryCategory.NEWS.key) }
    var newSourceName by remember { mutableStateOf("") }
    var newSourceUrl by remember { mutableStateOf("") }
    var newSourceType by remember { mutableStateOf("article") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .padding(horizontal = 12.dp)
                .border(1.3.dp, VipGoldBorder, RoundedCornerShape(24.dp))
                .testTag("discovery_admin_panel_modal"),
            shape = RoundedCornerShape(24.dp),
            color = NoiseCard
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AdminPanelSettings,
                            contentDescription = null,
                            tint = VipGold,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Discovery, Kerala News & Reels Admin",
                                color = NoiseWhite,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = "Safe & Positive Content Policy • Trusted Sources Only",
                                color = OnlineGreen,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close Admin Panel", tint = NoiseWhite)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Admin Sub-Tabs
                val tabTitles = listOf(
                    "Categories",
                    "🌴 Kerala & Reels",
                    "Sources (${sources.size})",
                    "Moderation (${allItems.size})",
                    "Logs (${importLogs.size})"
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(tabTitles.indices.toList()) { idx ->
                        val active = selectedTab == idx
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    if (active) Brush.horizontalGradient(listOf(NoisePink, NoisePurple))
                                    else Brush.horizontalGradient(listOf(NoiseCardElevated, NoiseCardElevated))
                                )
                                .border(1.dp, if (active) VipGold else NoiseBorder, RoundedCornerShape(14.dp))
                                .clickable { selectedTab = idx }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = tabTitles[idx],
                                color = if (active) Color.White else NoiseWhite,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = NoiseBorder)
                Spacer(modifier = Modifier.height(10.dp))

                when (selectedTab) {
                    0 -> {
                        // TAB 0: Categories & Auto-Refresh Interval Control
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            item {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(NoiseCardElevated)
                                        .border(1.dp, VipGoldBorder, RoundedCornerShape(16.dp))
                                        .padding(12.dp)
                                ) {
                                    Text(
                                        text = "Automatic Fetching Interval (15–30 Minutes)",
                                        color = VipGold,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 13.sp
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        listOf(15L, 20L, 25L, 30L).forEach { mins ->
                                            val active = globalConfig.refreshIntervalMinutes == mins
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(if (active) NoisePink else NoiseBlack)
                                                    .border(1.dp, if (active) VipGold else NoiseBorder, RoundedCornerShape(12.dp))
                                                    .clickable { onUpdateRefreshInterval(mins) }
                                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                                            ) {
                                                Text(
                                                    text = "${mins}m",
                                                    color = if (active) Color.White else NoiseWhite,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.weight(1f))
                                        Button(
                                            onClick = onTriggerImmediateSync,
                                            enabled = !isSyncing,
                                            colors = ButtonDefaults.buttonColors(containerColor = OnlineGreen),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = if (isSyncing) "Syncing..." else "Sync Now",
                                                color = Color.Black,
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                }
                            }

                            items(DiscoveryCategory.entries, key = { it.key }) { cat ->
                                val enabled = cat.key !in globalConfig.disabledCategories
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(NoiseCardElevated)
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(text = cat.emoji, fontSize = 16.sp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = cat.label,
                                                color = NoiseWhite,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                            )
                                            Text(
                                                text = "Key: ${cat.key}",
                                                color = NoiseTextSecondary,
                                                fontSize = 10.sp
                                            )
                                        }
                                    }
                                    Switch(
                                        checked = enabled,
                                        onCheckedChange = { onToggleCategoryEnabled(cat.key, it) },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color.White,
                                            checkedTrackColor = OnlineGreen
                                        )
                                    )
                                }
                            }
                        }
                    }

                    1 -> {
                        // TAB 1: Kerala News Categories & Trending Reels Algorithm Settings
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            item {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(NoiseCardElevated)
                                        .border(1.dp, VipGoldBorder, RoundedCornerShape(16.dp))
                                        .padding(12.dp)
                                ) {
                                    Text(
                                        text = "🔥 Trending Reels Ranking Algorithm",
                                        color = VipGold,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 13.sp
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    val modes = listOf(
                                        "engagement_velocity" to "Engagement + Velocity",
                                        "views_first" to "Most Viewed First",
                                        "recent_first" to "Newest First"
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        modes.forEach { (modeKey, modeLabel) ->
                                            val active = globalConfig.trendingAlgorithmMode == modeKey
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(if (active) NoisePink else NoiseBlack)
                                                    .border(1.dp, if (active) VipGold else NoiseBorder, RoundedCornerShape(10.dp))
                                                    .clickable {
                                                        onUpdateKeralaAndReelsConfig(
                                                            modeKey,
                                                            globalConfig.keralaNewsNotificationsEnabled,
                                                            globalConfig.trendingReelsNotificationsEnabled
                                                        )
                                                    }
                                                    .padding(horizontal = 8.dp, vertical = 5.dp)
                                            ) {
                                                Text(
                                                    text = modeLabel,
                                                    color = if (active) Color.White else NoiseWhite,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 10.sp
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "🚨 Breaking Kerala News Push Alerts",
                                            color = NoiseWhite,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                        Switch(
                                            checked = globalConfig.keralaNewsNotificationsEnabled,
                                            onCheckedChange = { enabled ->
                                                onUpdateKeralaAndReelsConfig(
                                                    globalConfig.trendingAlgorithmMode,
                                                    enabled,
                                                    globalConfig.trendingReelsNotificationsEnabled
                                                )
                                            }
                                        )
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "🎬 New Trending Reels Notifications",
                                            color = NoiseWhite,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                        Switch(
                                            checked = globalConfig.trendingReelsNotificationsEnabled,
                                            onCheckedChange = { enabled ->
                                                onUpdateKeralaAndReelsConfig(
                                                    globalConfig.trendingAlgorithmMode,
                                                    globalConfig.keralaNewsNotificationsEnabled,
                                                    enabled
                                                )
                                            }
                                        )
                                    }
                                }
                            }

                            items(KeralaNewsCategory.entries, key = { it.key }) { kCat ->
                                val enabled = kCat.key !in globalConfig.disabledKeralaCategories
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(NoiseCardElevated)
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(text = kCat.emoji, fontSize = 16.sp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = "${kCat.label} • ${kCat.malayalamLabel}",
                                                color = NoiseWhite,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                            )
                                            Text(
                                                text = "Key: ${kCat.key}",
                                                color = NoiseTextSecondary,
                                                fontSize = 10.sp
                                            )
                                        }
                                    }
                                    Switch(
                                        checked = enabled,
                                        onCheckedChange = { onToggleKeralaCategoryEnabled(kCat.key, it) },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color.White,
                                            checkedTrackColor = OnlineGreen
                                        )
                                    )
                                }
                            }
                        }
                    }

                    2 -> {
                        // TAB 2: Add / Remove / Toggle Trusted Content Sources
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            item {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(NoiseCardElevated)
                                        .border(1.dp, NoiseBorder, RoundedCornerShape(16.dp))
                                        .padding(12.dp)
                                ) {
                                    Text(
                                        text = "Add Trusted HTTPS RSS / Video Source",
                                        color = VipGold,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 13.sp
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        items(DiscoveryCategory.entries.filter { it != DiscoveryCategory.FOR_YOU }) { cat ->
                                            val sel = newSourceCategoryKey == cat.key
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(if (sel) NoisePink else NoiseBlack)
                                                    .clickable { newSourceCategoryKey = cat.key }
                                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                            ) {
                                                Text(
                                                    text = "${cat.emoji} ${cat.label}",
                                                    color = if (sel) Color.White else NoiseWhite,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    OutlinedTextField(
                                        value = newSourceName,
                                        onValueChange = { newSourceName = it },
                                        label = { Text("Publisher Name (e.g. Reuters Tech)", fontSize = 11.sp) },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    OutlinedTextField(
                                        value = newSourceUrl,
                                        onValueChange = { newSourceUrl = it },
                                        label = { Text("HTTPS Feed URL (https://...)", fontSize = 11.sp) },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            listOf("article" to "Article Feed", "video" to "Video Feed").forEach { (typeKey, label) ->
                                                val sel = newSourceType == typeKey
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(10.dp))
                                                        .background(if (sel) NoisePurple else NoiseBlack)
                                                        .clickable { newSourceType = typeKey }
                                                        .padding(horizontal = 10.dp, vertical = 5.dp)
                                                ) {
                                                    Text(
                                                        text = label,
                                                        color = if (sel) Color.White else NoiseTextSecondary,
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        }
                                        Button(
                                            onClick = {
                                                if (newSourceUrl.startsWith("https://", ignoreCase = true)) {
                                                    onAddCustomSource(
                                                        newSourceCategoryKey,
                                                        newSourceName,
                                                        newSourceUrl,
                                                        newSourceType
                                                    )
                                                    newSourceName = ""
                                                    newSourceUrl = ""
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = NoisePink)
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Add Source", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                        }
                                    }
                                }
                            }

                            items(sources, key = { it.id }) { src ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(NoiseCardElevated)
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${src.sourceName} (${src.categoryKey})",
                                            color = NoiseWhite,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                        Text(
                                            text = src.feedUrl,
                                            color = NoiseTextSecondary,
                                            fontSize = 10.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Switch(
                                            checked = src.enabled,
                                            onCheckedChange = { onToggleSourceEnabled(src, it) }
                                        )
                                        IconButton(onClick = { onDeleteSource(src.id) }) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Delete Source",
                                                tint = Color(0xFFEF4444)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    3 -> {
                        // TAB 3: Approve / Hide / Delete Imported Content
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (allItems.isEmpty()) {
                                item {
                                    Text(
                                        text = "No imported items in database yet.",
                                        color = NoiseTextSecondary,
                                        fontSize = 13.sp,
                                        modifier = Modifier.padding(16.dp)
                                    )
                                }
                            } else {
                                items(allItems, key = { it.id }) { item ->
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(NoiseCardElevated)
                                            .border(1.dp, NoiseBorder, RoundedCornerShape(14.dp))
                                            .padding(12.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "[${item.moderationStatus.uppercase()}] ${item.categoryLabel} • ${item.sourceName}",
                                                color = if (item.moderationStatus == "approved") OnlineGreen else VipGold,
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 11.sp
                                            )
                                            Text(
                                                text = "Safety: ${item.safetyScore}%",
                                                color = NoiseTextSecondary,
                                                fontSize = 10.sp
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = item.title,
                                            color = NoiseWhite,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            maxLines = 2
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Button(
                                                onClick = {
                                                    onModerateItem(item.id, "approved", "Approved by Admin")
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = OnlineGreen),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                            ) {
                                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.Black, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Approve", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                            }
                                            Button(
                                                onClick = {
                                                    onModerateItem(item.id, "hidden", "Hidden by Admin")
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = NoisePurple),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                            ) {
                                                Icon(Icons.Default.VisibilityOff, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Hide", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                            }
                                            Button(
                                                onClick = {
                                                    onDeleteItem(item.id, "")
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                            ) {
                                                Icon(Icons.Default.Delete, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Delete", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    4 -> {
                        // TAB 4: View Import Logs & Errors
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (importLogs.isEmpty()) {
                                item {
                                    Text(
                                        text = "No import logs recorded yet.",
                                        color = NoiseTextSecondary,
                                        fontSize = 13.sp,
                                        modifier = Modifier.padding(16.dp)
                                    )
                                }
                            } else {
                                items(importLogs, key = { it.id }) { log ->
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(NoiseCardElevated)
                                            .padding(10.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "${log.sourceName} (${log.categoryKey})",
                                                color = NoiseWhite,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp
                                            )
                                            Text(
                                                text = log.status.uppercase(),
                                                color = if (log.status == "success") OnlineGreen else Color(0xFFEF4444),
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 10.sp
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(3.dp))
                                        Text(
                                            text = "Fetched: ${log.itemsFetched} • Saved: ${log.itemsSaved} • Duplicates: ${log.duplicatesSkipped} • Safety Rejected: ${log.safetyRejected}",
                                            color = NoiseTextSecondary,
                                            fontSize = 11.sp
                                        )
                                        if (log.errorMessage.isNotBlank()) {
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "Error: ${log.errorMessage}",
                                                color = Color(0xFFEF4444),
                                                fontSize = 11.sp
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
    }
}
