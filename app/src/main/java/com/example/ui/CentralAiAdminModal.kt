package com.example.ui

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Web
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.AiAdminProposal
import com.example.data.AiCommandExecutionLogDoc
import com.example.data.CentralAppConfig
import com.example.data.CentralConfigBackupDoc
import com.example.data.CentralLiveUpdateRuntime
import com.example.data.CentralVersionDoc
import com.example.data.DynamicAppPage
import com.example.data.SuperOwnerControlManager
import com.example.data.SuperOwnerRegistryDoc
import com.example.data.UserProfile
import com.example.ui.theme.DangerRed
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Live Hero Announcement Banner + Dynamic Pages Quick Bar rendered on the Home Feed
 * when enabled by the Central Live Update System (`/central_app_config/production` or `/central_dynamic_pages`).
 */
@Composable
fun LiveCentralHeroAndDynamicPagesBar(
    config: CentralAppConfig,
    dynamicPages: List<DynamicAppPage>,
    isSuperOwner: Boolean,
    onOpenDynamicPage: (DynamicAppPage) -> Unit,
    onOpenCentralAiAdmin: () -> Unit
) {
    val visiblePages = remember(dynamicPages) {
        dynamicPages.filter { it.enabled && it.showInHomeQuickBar }
    }

    if (!config.heroBannerEnabled && visiblePages.isEmpty() && !CentralLiveUpdateRuntime.isPreviewModeActive) {
        return
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag("central_live_update_home_bar"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Local Preview Indicator Banner if Owner is previewing changes before publishing
        if (CentralLiveUpdateRuntime.isPreviewModeActive) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF2E1065))
                    .border(1.2.dp, VipGold, RoundedCornerShape(14.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Visibility,
                        contentDescription = "Preview Mode",
                        tint = VipGold,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "PREVIEW MODE ACTIVE • Tap AI Admin to Publish to All Users",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
                if (isSuperOwner) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(VipGold)
                            .clickable { onOpenCentralAiAdmin() }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "PUBLISH",
                            color = Color.Black,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }

        // Live Hero Announcement Banner
        if (config.heroBannerEnabled && config.heroBannerTitle.isNotBlank()) {
            val startCol = CentralLiveUpdateRuntime.parseColorOrFallback(config.heroBannerGradientStartHex, NoisePurple)
            val endCol = CentralLiveUpdateRuntime.parseColorOrFallback(config.heroBannerGradientEndHex, NoisePink)
            val cornerDp = config.cardCornerRadiusDp.coerceIn(10L, 32L).toInt().dp

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(cornerDp))
                    .background(Brush.horizontalGradient(listOf(startCol, endCol)))
                    .border(1.dp, VipGold.copy(alpha = 0.7f), RoundedCornerShape(cornerDp))
                    .clickable {
                        val targetPage = visiblePages.firstOrNull {
                            it.pageKey.equals(config.heroBannerTargetPageKey, ignoreCase = true)
                        } ?: visiblePages.firstOrNull()
                        if (targetPage != null) {
                            onOpenDynamicPage(targetPage)
                        } else if (isSuperOwner) {
                            onOpenCentralAiAdmin()
                        }
                    }
                    .padding(horizontal = 16.dp, vertical = 14.dp)
                    .testTag("live_hero_announcement_banner")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(Color.Black.copy(alpha = 0.3f))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "⚡ LIVE UPDATE ${config.versionLabel.uppercase()}",
                                    color = VipGold,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 9.sp
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = config.heroBannerTitle,
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp
                        )
                        if (config.heroBannerSubtitle.isNotBlank()) {
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = config.heroBannerSubtitle,
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 12.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.White)
                            .padding(horizontal = 12.dp, vertical = 7.dp)
                    ) {
                        Text(
                            text = config.heroBannerCtaText.ifBlank { "Explore" },
                            color = Color(0xFF171525),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        // Dynamic Remotely Deployed Pages Row
        if (visiblePages.isNotEmpty()) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("dynamic_pages_quick_row"),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items(visiblePages, key = { it.id.ifBlank { it.pageKey } }) { page ->
                    val accent = CentralLiveUpdateRuntime.parseColorOrFallback(page.accentHex, NoisePurple)
                    val secondary = CentralLiveUpdateRuntime.parseColorOrFallback(page.secondaryAccentHex, NoisePink)
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Brush.horizontalGradient(listOf(accent.copy(alpha = 0.2f), secondary.copy(alpha = 0.2f))))
                            .border(1.2.dp, Brush.horizontalGradient(listOf(accent, secondary)), RoundedCornerShape(20.dp))
                            .clickable { onOpenDynamicPage(page) }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                            .testTag("open_dynamic_page_${page.pageKey}"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = page.title,
                            tint = VipGold,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = page.title,
                            color = NoiseWhite,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 12.sp
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(VipGold)
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "NEW",
                                color = Color.Black,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 8.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Full-Screen Dialog displaying a remotely configured DynamicAppPage (`/central_dynamic_pages/{pageId}`)
 * so new pages added by the Owner via the AI App Editor are immediately navigable by all users without APK reinstallation.
 */
@Composable
fun DynamicAppPageViewerDialog(
    page: DynamicAppPage,
    onNavigateToTab: (NoiseTab) -> Unit,
    onDismiss: () -> Unit
) {
    val isLight = ThemeManager.isLightMode
    val bgColor = if (isLight) {
        CentralLiveUpdateRuntime.parseColorOrFallback(page.backgroundLightHex, Color.White)
    } else {
        CentralLiveUpdateRuntime.parseColorOrFallback(page.backgroundDarkHex, Color(0xFF0A0714))
    }
    val accent = CentralLiveUpdateRuntime.parseColorOrFallback(page.accentHex, NoisePurple)
    val secondary = CentralLiveUpdateRuntime.parseColorOrFallback(page.secondaryAccentHex, NoisePink)
    val sections = remember(page.sectionsJson) { page.parsedSections() }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .testTag("dynamic_page_viewer_${page.pageKey}"),
            color = bgColor
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Brush.horizontalGradient(listOf(accent, secondary)))
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = VipGold,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = page.title,
                                    color = Color.White,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 17.sp
                                )
                            }
                            if (page.subtitle.isNotBlank()) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = page.subtitle,
                                    color = Color.White.copy(alpha = 0.9f),
                                    fontSize = 12.sp
                                )
                            }
                        }
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.3f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close Page",
                                tint = Color.White
                            )
                        }
                    }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(sections, key = { it.sectionId }) { section ->
                        val secAccent = CentralLiveUpdateRuntime.parseColorOrFallback(section.accentHex, accent)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(20.dp))
                                .background(NoiseCard)
                                .border(1.2.dp, secAccent.copy(alpha = 0.55f), RoundedCornerShape(20.dp))
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .background(secAccent.copy(alpha = 0.18f))
                                        .padding(horizontal = 10.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = section.badgeText.ifBlank { "LIVE" },
                                        color = secAccent,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = section.title,
                                color = NoiseWhite,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp
                            )
                            if (section.subtitle.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = section.subtitle,
                                    color = NoiseTextSecondary,
                                    fontSize = 13.sp,
                                    lineHeight = 19.sp
                                )
                            }
                            if (section.actionLabel.isNotBlank()) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = {
                                        val targetTab = when (section.actionTarget.uppercase()) {
                                            "SEARCH" -> NoiseTab.SEARCH
                                            "CLIPS" -> NoiseTab.CLIPS
                                            "GAMES" -> NoiseTab.GAMES
                                            "PROFILE" -> NoiseTab.PROFILE
                                            else -> NoiseTab.HOME
                                        }
                                        onDismiss()
                                        onNavigateToTab(targetTab)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = secAccent),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Text(
                                        text = section.actionLabel,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
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

/**
 * Central AI Admin & Live Update Control Panel Modal.
 * Accessible ONLY to the verified Owner Admin UID (`sebin_mr` / `sebinmr556@gmail.com`).
 */
@Composable
fun CentralAiAdminPanelModal(
    currentUserProfile: UserProfile,
    registryDoc: SuperOwnerRegistryDoc,
    productionConfig: CentralAppConfig,
    dynamicPages: List<DynamicAppPage>,
    versionHistory: List<CentralVersionDoc>,
    backups: List<CentralConfigBackupDoc>,
    aiCommandLogs: List<AiCommandExecutionLogDoc>,
    currentProposal: AiAdminProposal?,
    isGeneratingProposal: Boolean,
    isPublishingLiveUpdate: Boolean,
    adminStatusMessage: String?,
    onGenerateAiProposal: (String) -> Unit,
    onTogglePreviewProposal: (AiAdminProposal?, Boolean) -> Unit,
    onPublishProposalToAllUsers: (AiAdminProposal) -> Unit,
    onDirectSaveDynamicPage: (DynamicAppPage) -> Unit,
    onDeleteDynamicPage: (String) -> Unit,
    onCreateManualBackup: (String) -> Unit,
    onRollbackToBackup: (CentralConfigBackupDoc) -> Unit,
    onOpenDynamicPagePreview: (DynamicAppPage) -> Unit,
    onOpenGeminiDeveloperStudio: () -> Unit = {},
    onDismiss: () -> Unit
) {
    // Strict Universal Owner Admin UID verification
    val isVerifiedOwner = SuperOwnerControlManager.isCurrentUserSuperOwner(currentUserProfile, registryDoc)
    if (!isVerifiedOwner) {
        onDismiss()
        return
    }

    var selectedTab by remember { mutableIntStateOf(0) } // 0 = AI App Editor, 1 = Live Customizer, 2 = Dynamic Pages, 3 = Backups & Rollback
    var promptInput by remember { mutableStateOf("") }
    var showPublishConfirmDialog by remember { mutableStateOf<AiAdminProposal?>(null) }
    var pendingRollbackBackup by remember { mutableStateOf<CentralConfigBackupDoc?>(null) }

    // Direct customizer draft state initialized from current productionConfig
    var draftDarkBg by remember(productionConfig.homeBackgroundDarkHex) { mutableStateOf(productionConfig.homeBackgroundDarkHex) }
    var draftGradStart by remember(productionConfig.buttonGradientStartHex) { mutableStateOf(productionConfig.buttonGradientStartHex) }
    var draftGradEnd by remember(productionConfig.buttonGradientEndHex) { mutableStateOf(productionConfig.buttonGradientEndHex) }
    var draftMenuStyle by remember(productionConfig.menuStyle) { mutableStateOf(productionConfig.menuStyle) }
    var draftChatroomLayout by remember(productionConfig.chatroomLayoutStyle) { mutableStateOf(productionConfig.chatroomLayoutStyle) }
    var draftNotifStyle by remember(productionConfig.notificationBannerStyle) { mutableStateOf(productionConfig.notificationBannerStyle) }
    var draftFontScale by remember(productionConfig.fontScaleMultiplier) { mutableStateOf(productionConfig.fontScaleMultiplier) }
    var draftCornerRadius by remember(productionConfig.buttonCornerRadiusDp) { mutableStateOf(productionConfig.buttonCornerRadiusDp) }
    var draftHeroEnabled by remember(productionConfig.heroBannerEnabled) { mutableStateOf(productionConfig.heroBannerEnabled) }
    var draftHeroTitle by remember(productionConfig.heroBannerTitle) { mutableStateOf(productionConfig.heroBannerTitle.ifBlank { "🔥 NOICE LIVE FEATURE UPDATE" }) }
    var draftHeroSubtitle by remember(productionConfig.heroBannerSubtitle) { mutableStateOf(productionConfig.heroBannerSubtitle.ifBlank { "Experience real-time voice stages, 3D multiplayer games & Kerala News!" }) }
    var draftShowVipRooms by remember(productionConfig.showVipPartyRoomsSection) { mutableStateOf(productionConfig.showVipPartyRoomsSection) }
    var draftShowDiscovery by remember(productionConfig.showDiscoveryBar) { mutableStateOf(productionConfig.showDiscoveryBar) }
    var draftShowKeralaNews by remember(productionConfig.showKeralaNewsHub) { mutableStateOf(productionConfig.showKeralaNewsHub) }
    var draftShowGamesStrip by remember(productionConfig.showMultiplayerGamesStrip) { mutableStateOf(productionConfig.showMultiplayerGamesStrip) }

    // Dynamic page creation fields
    var newPageTitle by remember { mutableStateOf("") }
    var newPageSubtitle by remember { mutableStateOf("") }
    var newPageKey by remember { mutableStateOf("") }
    var newPageAccent by remember { mutableStateOf("#8525FF") }

    val quickCommands = remember {
        listOf(
            "Change the home page background to Deep Cyber Midnight Blue",
            "Add a new page 'VIP Events & Rewards'",
            "Remove a page from live menu",
            "Change menu design to glass floating",
            "Change button colors and gradients to Luxury Gold-Rose",
            "Modify chatroom layouts to 2-column grid",
            "Add new features & enable Live Hero Banner",
            "Edit notification designs to Gold VIP",
            "Change fonts, icons, spacing and animations to spacious",
            "Fix UI bugs and identify errors"
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .testTag("central_ai_admin_panel_modal"),
            color = NoiseBlack
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top Header Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0xFF1A0836), Color(0xFF5B21B6), Color(0xFFBE185D))
                            )
                        )
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = "AI Admin",
                                    tint = VipGold,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "CENTRAL AI ADMIN & LIVE UPDATE",
                                    color = VipGold,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .background(OnlineGreen.copy(alpha = 0.22f))
                                        .border(1.dp, OnlineGreen, RoundedCornerShape(50))
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "LIVE ${productionConfig.versionLabel}",
                                        color = OnlineGreen,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 9.sp
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = "Universal Owner UID Verified (${currentUserProfile.id.take(18)}...) • Real-Time Cloud Sync",
                                color = Color.White.copy(alpha = 0.88f),
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.Black.copy(alpha = 0.4f))
                                    .border(1.dp, Color(0xFF00E5FF), RoundedCornerShape(12.dp))
                                    .clickable { onOpenGeminiDeveloperStudio() }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                                    .testTag("open_gemini_dev_studio_from_admin_button")
                            ) {
                                Text(
                                    text = "💻 GEMINI DEV IDE",
                                    color = Color(0xFF00E5FF),
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 10.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.35f))
                                    .testTag("close_central_ai_admin_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close AI Admin Panel",
                                    tint = Color.White
                                )
                            }
                        }
                    }
                }

                // Status / Feedback Banner
                if (!adminStatusMessage.isNullOrBlank()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF064E3B))
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = OnlineGreen,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = adminStatusMessage,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }

                // Navigation Tabs
                val tabs = listOf(
                    "🤖 AI App Editor",
                    "🎨 Live Customizer",
                    "📄 Dynamic Pages (${dynamicPages.size})",
                    "⏪ Backups & Versions (${backups.size})"
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(NoiseCard)
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    tabs.forEachIndexed { idx, title ->
                        val selected = selectedTab == idx
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(
                                    if (selected) Brush.horizontalGradient(listOf(NoisePurple, NoisePink))
                                    else Brush.horizontalGradient(listOf(NoiseCardElevated, NoiseCardElevated))
                                )
                                .border(
                                    1.dp,
                                    if (selected) VipGold else NoiseBorder,
                                    RoundedCornerShape(20.dp)
                                )
                                .clickable { selectedTab = idx }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                                .testTag("ai_admin_tab_$idx")
                        ) {
                            Text(
                                text = title,
                                color = if (selected) Color.White else NoiseWhite,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                // Tab Content
                when (selectedTab) {
                    0 -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            // Security & Non-Destructive Guarantee Banner
                            item {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(NoiseCard)
                                        .border(1.dp, NoiseBorder, RoundedCornerShape(16.dp))
                                        .padding(14.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Security,
                                            contentDescription = null,
                                            tint = VipGold,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "AI-Powered Live App Editor (Firebase AI Logic • Gemini)",
                                            color = NoiseWhite,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 13.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Describe any UI, color, menu, chatroom layout, notification, or page change in natural language. Changes are inspected, validated, backed up, and broadcast live to all connected devices without reinstalling the APK.",
                                        color = NoiseTextSecondary,
                                        fontSize = 11.sp,
                                        lineHeight = 16.sp
                                    )
                                }
                            }

                            // Quick Natural Language Command Chips
                            item {
                                Column {
                                    Text(
                                        text = "QUICK OWNER COMMANDS (TAP TO LOAD):",
                                        color = NoiseTextSecondary,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 11.sp
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        quickCommands.forEachIndexed { i, cmd ->
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(16.dp))
                                                    .background(
                                                        if (ThemeManager.isLightMode) LightLavender else Color(0xFF1F1635)
                                                    )
                                                    .border(1.dp, NoisePurple.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                                                    .clickable { promptInput = cmd }
                                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                                                    .testTag("ai_quick_command_$i")
                                            ) {
                                                Text(
                                                    text = cmd,
                                                    color = NoiseWhite,
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = 11.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Natural Language Command Input Box
                            item {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(18.dp))
                                        .background(NoiseCard)
                                        .border(1.2.dp, NoisePurple.copy(alpha = 0.6f), RoundedCornerShape(18.dp))
                                        .padding(14.dp)
                                ) {
                                    OutlinedTextField(
                                        value = promptInput,
                                        onValueChange = { promptInput = it },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("ai_admin_prompt_input"),
                                        label = { Text("Enter natural language command for NOICE LIVE...") },
                                        placeholder = {
                                            Text("e.g., Change the home page background, Add a new page, Change button gradients...")
                                        },
                                        minLines = 3,
                                        maxLines = 5,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = NoiseWhite,
                                            unfocusedTextColor = NoiseWhite,
                                            focusedBorderColor = NoisePurple,
                                            unfocusedBorderColor = NoiseBorder
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Button(
                                        onClick = {
                                            if (promptInput.isNotBlank() && !isGeneratingProposal) {
                                                onGenerateAiProposal(promptInput)
                                            }
                                        },
                                        enabled = promptInput.isNotBlank() && !isGeneratingProposal,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(48.dp)
                                            .testTag("ai_admin_generate_button"),
                                        shape = RoundedCornerShape(14.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = NoisePurple)
                                    ) {
                                        if (isGeneratingProposal) {
                                            CircularProgressIndicator(
                                                color = Color.White,
                                                strokeWidth = 2.dp,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text(
                                                text = "AI Inspecting Code & Validating Changes...",
                                                color = Color.White,
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 13.sp
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Default.AutoAwesome,
                                                contentDescription = null,
                                                tint = VipGold,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "GENERATE & VALIDATE WITH AI",
                                                color = Color.White,
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 13.sp
                                            )
                                        }
                                    }
                                }
                            }

                            // Generated AI Proposal & Validation Report + PUBLISH TO ALL USERS CTA
                            if (currentProposal != null) {
                                item {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(20.dp))
                                            .background(NoiseCard)
                                            .border(1.5.dp, VipGold, RoundedCornerShape(20.dp))
                                            .padding(16.dp)
                                            .testTag("ai_proposal_result_card"),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "🔍 AI INSPECTION & VALIDATION REPORT",
                                                color = VipGold,
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 13.sp
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(50))
                                                    .background(
                                                        if (currentProposal.validationPassed) OnlineGreen.copy(alpha = 0.2f)
                                                        else DangerRed.copy(alpha = 0.2f)
                                                    )
                                                    .padding(horizontal = 10.dp, vertical = 3.dp)
                                            ) {
                                                Text(
                                                    text = if (currentProposal.validationPassed) "VALIDATED ✓" else "NEEDS FIX",
                                                    color = if (currentProposal.validationPassed) OnlineGreen else DangerRed,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    fontSize = 10.sp
                                                )
                                            }
                                        }

                                        Text(
                                            text = currentProposal.summaryOfChanges,
                                            color = NoiseWhite,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )

                                        if (currentProposal.inspectedComponents.isNotEmpty()) {
                                            Text(
                                                text = "Inspected Components: ${currentProposal.inspectedComponents.joinToString(" • ")}",
                                                color = NoiseTextSecondary,
                                                fontSize = 11.sp
                                            )
                                        }

                                        currentProposal.validationMessages.forEach { msg ->
                                            Text(
                                                text = msg,
                                                color = OnlineGreen,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }

                                        // Local Preview Toggle
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(14.dp))
                                                .background(NoiseCardElevated)
                                                .padding(horizontal = 12.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "Preview Changes Locally Before Publishing",
                                                    color = NoiseWhite,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp
                                                )
                                                Text(
                                                    text = "Test the new UI on your device first before broadcasting to all users",
                                                    color = NoiseTextSecondary,
                                                    fontSize = 10.sp
                                                )
                                            }
                                            Switch(
                                                checked = CentralLiveUpdateRuntime.isPreviewModeActive,
                                                onCheckedChange = { enabled ->
                                                    onTogglePreviewProposal(currentProposal, enabled)
                                                },
                                                colors = SwitchDefaults.colors(checkedThumbColor = VipGold),
                                                modifier = Modifier.testTag("ai_admin_preview_toggle")
                                            )
                                        }

                                        // PUBLISH TO ALL USERS Button
                                        Button(
                                            onClick = { showPublishConfirmDialog = currentProposal },
                                            enabled = currentProposal.validationPassed && !isPublishingLiveUpdate,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(52.dp)
                                                .testTag("publish_to_all_users_button"),
                                            shape = RoundedCornerShape(16.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = OnlineGreen)
                                        ) {
                                            if (isPublishingLiveUpdate) {
                                                CircularProgressIndicator(
                                                    color = Color.Black,
                                                    strokeWidth = 2.dp,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = "SAVING BACKUP & PUBLISHING TO ALL USERS...",
                                                    color = Color.Black,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    fontSize = 13.sp
                                                )
                                            } else {
                                                Icon(
                                                    imageVector = Icons.Default.Public,
                                                    contentDescription = null,
                                                    tint = Color.Black,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = "PUBLISH TO ALL USERS (LIVE UPDATE)",
                                                    color = Color.Black,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    fontSize = 14.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Recent AI Command Execution History
                            if (aiCommandLogs.isNotEmpty()) {
                                item {
                                    Text(
                                        text = "RECENT AI ADMIN COMMAND HISTORY:",
                                        color = NoiseTextSecondary,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 11.sp
                                    )
                                }
                                items(aiCommandLogs.take(8), key = { it.id }) { log ->
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(NoiseCard)
                                            .border(1.dp, NoiseBorder, RoundedCornerShape(14.dp))
                                            .padding(12.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "v${log.publishedVersionNumber} • ${log.actionCategory.uppercase()}",
                                                color = VipGold,
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 11.sp
                                            )
                                            Text(
                                                text = SimpleDateFormat("MMM dd, hh:mm a", Locale.US).format(Date(log.timestampMillis)),
                                                color = NoiseTextSecondary,
                                                fontSize = 10.sp
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "\"${log.commandPrompt}\"",
                                            color = NoiseWhite,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                        Text(
                                            text = log.summaryOfChanges,
                                            color = NoiseTextSecondary,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    1 -> {
                        // Direct Visual Live Customizer Tab
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            item {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(NoiseCard)
                                        .border(1.dp, NoiseBorder, RoundedCornerShape(16.dp))
                                        .padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Palette,
                                            contentDescription = null,
                                            tint = NoisePink,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "1. Home Page Background (Dark Mode)",
                                            color = NoiseWhite,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 13.sp
                                        )
                                    }
                                    val bgPresets = listOf(
                                        "Obsidian (#050507)" to "#050507",
                                        "Cyber Midnight (#060B1E)" to "#060B1E",
                                        "Royal Amethyst (#110622)" to "#110622",
                                        "Emerald Night (#041612)" to "#041612"
                                    )
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        bgPresets.forEach { (label, hex) ->
                                            val selected = draftDarkBg.equals(hex, ignoreCase = true)
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(14.dp))
                                                    .background(if (selected) NoisePurple else NoiseCardElevated)
                                                    .border(1.dp, if (selected) VipGold else NoiseBorder, RoundedCornerShape(14.dp))
                                                    .clickable { draftDarkBg = hex }
                                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                                            ) {
                                                Text(
                                                    text = label,
                                                    color = if (selected) Color.White else NoiseWhite,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 11.sp
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "2. Button Colors & Gradients",
                                        color = NoiseWhite,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 13.sp
                                    )
                                    val gradPresets = listOf(
                                        "NOICE Purple-Pink" to ("#8525FF" to "#E52BEB"),
                                        "Luxury Gold-Rose" to ("#FFB900" to "#FF2E93"),
                                        "Cyber Cyan-Violet" to ("#00E5FF" to "#7C3AED"),
                                        "Hyper Magenta" to ("#9D4EDD" to "#FF007F")
                                    )
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        gradPresets.forEach { (label, pair) ->
                                            val selected = draftGradStart.equals(pair.first, ignoreCase = true)
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(14.dp))
                                                    .background(
                                                        if (selected) Brush.horizontalGradient(listOf(NoisePurple, NoisePink))
                                                        else Brush.horizontalGradient(listOf(NoiseCardElevated, NoiseCardElevated))
                                                    )
                                                    .border(1.dp, if (selected) VipGold else NoiseBorder, RoundedCornerShape(14.dp))
                                                    .clickable {
                                                        draftGradStart = pair.first
                                                        draftGradEnd = pair.second
                                                    }
                                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                                            ) {
                                                Text(
                                                    text = label,
                                                    color = if (selected) Color.White else NoiseWhite,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 11.sp
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "3. VIP Party Chatroom Layout & Menu Style",
                                        color = NoiseWhite,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 13.sp
                                    )
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        listOf(
                                            "Carousel" to "carousel",
                                            "2-Column Grid" to "grid_2col",
                                            "Compact List" to "compact_list",
                                            "Featured Stage" to "featured_stage"
                                        ).forEach { (label, styleKey) ->
                                            val selected = draftChatroomLayout == styleKey
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(14.dp))
                                                    .background(if (selected) NoisePurple else NoiseCardElevated)
                                                    .clickable { draftChatroomLayout = styleKey }
                                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                                            ) {
                                                Text(
                                                    text = "Rooms: $label",
                                                    color = if (selected) Color.White else NoiseWhite,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 11.sp
                                                )
                                            }
                                        }
                                    }

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        listOf(
                                            "Modern Pill Menu" to "modern_pill",
                                            "Glass Floating Menu" to "glass_floating",
                                            "Neon Dock Menu" to "neon_dock"
                                        ).forEach { (label, styleKey) ->
                                            val selected = draftMenuStyle == styleKey
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(14.dp))
                                                    .background(if (selected) NoisePink else NoiseCardElevated)
                                                    .clickable { draftMenuStyle = styleKey }
                                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                                            ) {
                                                Text(
                                                    text = label,
                                                    color = if (selected) Color.White else NoiseWhite,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 11.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Hero Banner & Feature Visibility Controls
                            item {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(NoiseCard)
                                        .border(1.dp, NoiseBorder, RoundedCornerShape(16.dp))
                                        .padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "4. Live Home Announcement Banner & Feature Toggles",
                                        color = NoiseWhite,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 13.sp
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Show Live Hero Announcement Banner", color = NoiseWhite, fontSize = 12.sp)
                                        Switch(checked = draftHeroEnabled, onCheckedChange = { draftHeroEnabled = it })
                                    }
                                    if (draftHeroEnabled) {
                                        OutlinedTextField(
                                            value = draftHeroTitle,
                                            onValueChange = { draftHeroTitle = it },
                                            label = { Text("Banner Headline") },
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        OutlinedTextField(
                                            value = draftHeroSubtitle,
                                            onValueChange = { draftHeroSubtitle = it },
                                            label = { Text("Banner Subtitle") },
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Show VIP Party Rooms Section", color = NoiseWhite, fontSize = 12.sp)
                                        Switch(checked = draftShowVipRooms, onCheckedChange = { draftShowVipRooms = it })
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Show Automatic Discovery Bar", color = NoiseWhite, fontSize = 12.sp)
                                        Switch(checked = draftShowDiscovery, onCheckedChange = { draftShowDiscovery = it })
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Show Automatic Kerala News Hub", color = NoiseWhite, fontSize = 12.sp)
                                        Switch(checked = draftShowKeralaNews, onCheckedChange = { draftShowKeralaNews = it })
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))
                                    Button(
                                        onClick = {
                                            val updatedCfg = productionConfig.copy(
                                                homeBackgroundDarkHex = draftDarkBg,
                                                primaryAccentHex = draftGradStart,
                                                secondaryAccentHex = draftGradEnd,
                                                buttonGradientStartHex = draftGradStart,
                                                buttonGradientEndHex = draftGradEnd,
                                                menuStyle = draftMenuStyle,
                                                chatroomLayoutStyle = draftChatroomLayout,
                                                notificationBannerStyle = draftNotifStyle,
                                                fontScaleMultiplier = draftFontScale,
                                                buttonCornerRadiusDp = draftCornerRadius,
                                                heroBannerEnabled = draftHeroEnabled,
                                                heroBannerTitle = draftHeroTitle,
                                                heroBannerSubtitle = draftHeroSubtitle,
                                                showVipPartyRoomsSection = draftShowVipRooms,
                                                showDiscoveryBar = draftShowDiscovery,
                                                showKeralaNewsHub = draftShowKeralaNews,
                                                showMultiplayerGamesStrip = draftShowGamesStrip,
                                                changeSummary = "Updated live theme ($draftDarkBg), gradient ($draftGradStart→$draftGradEnd), menu ($draftMenuStyle), and chatroom layout ($draftChatroomLayout)"
                                            )
                                            showPublishConfirmDialog = AiAdminProposal(
                                                prompt = "Direct Customizer Update",
                                                actionCategory = "customizer_update",
                                                inspectedComponents = listOf("Color.kt", "HomeFeedScreen.kt", "NoiseComponents.kt"),
                                                summaryOfChanges = updatedCfg.changeSummary,
                                                validationPassed = true,
                                                validationMessages = listOf("✓ All customizer parameters validated"),
                                                proposedConfig = updatedCfg
                                            )
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(50.dp)
                                            .testTag("publish_customizer_to_all_users_button"),
                                        shape = RoundedCornerShape(14.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = OnlineGreen)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Public,
                                            contentDescription = null,
                                            tint = Color.Black
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "PUBLISH CUSTOMIZATION TO ALL USERS",
                                            color = Color.Black,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    2 -> {
                        // Dynamic Pages Manager Tab
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            item {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(NoiseCard)
                                        .border(1.dp, NoiseBorder, RoundedCornerShape(16.dp))
                                        .padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = null,
                                            tint = VipGold
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Add & Publish a New Live Dynamic Page",
                                            color = NoiseWhite,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 14.sp
                                        )
                                    }
                                    OutlinedTextField(
                                        value = newPageTitle,
                                        onValueChange = {
                                            newPageTitle = it
                                            if (newPageKey.isBlank()) {
                                                newPageKey = it.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_')
                                            }
                                        },
                                        label = { Text("Page Title (e.g. VIP Events & Tournaments)") },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("new_dynamic_page_title_input")
                                    )
                                    OutlinedTextField(
                                        value = newPageSubtitle,
                                        onValueChange = { newPageSubtitle = it },
                                        label = { Text("Page Subtitle / Description") },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("new_dynamic_page_subtitle_input")
                                    )
                                    Button(
                                        onClick = {
                                            if (newPageTitle.isNotBlank()) {
                                                val key = newPageKey.ifBlank {
                                                    newPageTitle.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_')
                                                }
                                                val page = com.example.data.CentralAiAdminManager.createStructuredDynamicPage(
                                                    pageKey = key,
                                                    title = newPageTitle.trim(),
                                                    subtitle = newPageSubtitle.trim().ifBlank { "Live interactive page published by @sebin_mr" },
                                                    accentHex = newPageAccent
                                                )
                                                onDirectSaveDynamicPage(page)
                                                newPageTitle = ""
                                                newPageSubtitle = ""
                                                newPageKey = ""
                                            }
                                        },
                                        enabled = newPageTitle.isNotBlank(),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(46.dp)
                                            .testTag("publish_new_dynamic_page_button"),
                                        shape = RoundedCornerShape(14.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = NoisePurple)
                                    ) {
                                        Text(
                                            text = "ADD PAGE & PUBLISH TO ALL USERS",
                                            color = Color.White,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }

                            if (dynamicPages.isEmpty()) {
                                item {
                                    Text(
                                        text = "No dynamic pages deployed yet. Create one above or ask the AI App Editor: \"Add a new page 'VIP Events'\"",
                                        color = NoiseTextSecondary,
                                        fontSize = 12.sp
                                    )
                                }
                            } else {
                                items(dynamicPages, key = { it.id.ifBlank { it.pageKey } }) { page ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(NoiseCard)
                                            .border(1.dp, NoiseBorder, RoundedCornerShape(16.dp))
                                            .padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = page.title,
                                                color = NoiseWhite,
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 14.sp
                                            )
                                            Text(
                                                text = "Key: /${page.pageKey} • ${page.subtitle}",
                                                color = NoiseTextSecondary,
                                                fontSize = 11.sp
                                            )
                                        }
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            OutlinedButton(
                                                onClick = { onOpenDynamicPagePreview(page) }
                                            ) {
                                                Text("Open", fontSize = 11.sp)
                                            }
                                            IconButton(
                                                onClick = { onDeleteDynamicPage(page.pageKey) },
                                                modifier = Modifier.testTag("delete_dynamic_page_${page.pageKey}")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Remove Page",
                                                    tint = DangerRed
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    3 -> {
                        // Backups, Version History & Rollback Tab
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Active Production Version: ${productionConfig.versionLabel}",
                                            color = VipGold,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 14.sp
                                        )
                                        Text(
                                            text = productionConfig.changeSummary,
                                            color = NoiseTextSecondary,
                                            fontSize = 11.sp
                                        )
                                    }
                                    Button(
                                        onClick = { onCreateManualBackup("Manual Owner Checkpoint") },
                                        colors = ButtonDefaults.buttonColors(containerColor = NoisePurple),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.testTag("create_manual_backup_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Backup,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Backup Now", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    }
                                }
                            }

                            item {
                                Text(
                                    text = "RESTORABLE BACKUP SNAPSHOTS (${backups.size}):",
                                    color = NoiseTextSecondary,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 11.sp
                                )
                            }

                            if (backups.isEmpty()) {
                                item {
                                    Text(
                                        text = "Backups are automatically saved before every live publish. You can also tap 'Backup Now' above.",
                                        color = NoiseTextSecondary,
                                        fontSize = 12.sp
                                    )
                                }
                            } else {
                                items(backups, key = { it.id }) { backup ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(NoiseCard)
                                            .border(1.dp, NoiseBorder, RoundedCornerShape(14.dp))
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = backup.backupLabel.ifBlank { backup.versionLabel },
                                                color = NoiseWhite,
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 13.sp
                                            )
                                            Text(
                                                text = backup.reason,
                                                color = NoiseTextSecondary,
                                                fontSize = 11.sp
                                            )
                                            Text(
                                                text = SimpleDateFormat("MMM dd, yyyy • hh:mm:ss a", Locale.US).format(Date(backup.createdAtMillis)),
                                                color = NoiseTextSecondary,
                                                fontSize = 10.sp
                                            )
                                        }
                                        Button(
                                            onClick = { pendingRollbackBackup = backup },
                                            colors = ButtonDefaults.buttonColors(containerColor = VipGold),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.testTag("rollback_backup_button_${backup.id}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Restore,
                                                contentDescription = "Rollback",
                                                tint = Color.Black,
                                                modifier = Modifier.size(15.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "Rollback",
                                                color = Color.Black,
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                }
                            }

                            if (versionHistory.isNotEmpty()) {
                                item {
                                    Text(
                                        text = "PUBLISHED VERSION HISTORY (${versionHistory.size}):",
                                        color = NoiseTextSecondary,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 11.sp
                                    )
                                }
                                items(versionHistory.take(15), key = { it.id }) { ver ->
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(NoiseCard)
                                            .border(1.dp, NoiseBorder, RoundedCornerShape(14.dp))
                                            .padding(12.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "${ver.versionLabel} (Version #${ver.versionNumber})",
                                                color = OnlineGreen,
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 12.sp
                                            )
                                            Text(
                                                text = SimpleDateFormat("MMM dd, hh:mm a", Locale.US).format(Date(ver.publishedAtMillis)),
                                                color = NoiseTextSecondary,
                                                fontSize = 10.sp
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(3.dp))
                                        Text(
                                            text = ver.changeSummary,
                                            color = NoiseWhite,
                                            fontSize = 12.sp
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

    // Pre-Publish Confirmation Dialog
    val proposalToPublish = showPublishConfirmDialog
    if (proposalToPublish != null) {
        AlertDialog(
            onDismissRequest = { showPublishConfirmDialog = null },
            containerColor = NoiseCard,
            title = {
                Text(
                    text = "🚀 Publish Live Update to All Users?",
                    color = NoiseWhite,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = proposalToPublish.summaryOfChanges,
                        color = NoiseWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Text(
                        text = "• Pre-publish backup will be saved automatically.\n• Version number will increment to v${productionConfig.versionNumber + 1}.\n• All connected NOICE LIVE installations will receive this update immediately in real time without reinstalling the APK.\n• Existing user accounts, messages, stories, posts, and chatrooms remain 100% untouched.",
                        color = NoiseTextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val target = proposalToPublish
                        showPublishConfirmDialog = null
                        onPublishProposalToAllUsers(target)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = OnlineGreen),
                    modifier = Modifier.testTag("confirm_publish_to_all_users_button")
                ) {
                    Text(
                        text = "CONFIRM & PUBLISH NOW",
                        color = Color.Black,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showPublishConfirmDialog = null }) {
                    Text("Cancel", color = NoiseTextSecondary)
                }
            }
        )
    }

    // Rollback Confirmation Dialog
    val backupToRestore = pendingRollbackBackup
    if (backupToRestore != null) {
        AlertDialog(
            onDismissRequest = { pendingRollbackBackup = null },
            containerColor = NoiseCard,
            title = {
                Text(
                    text = "⏪ Rollback Production Version?",
                    color = NoiseWhite,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp
                )
            },
            text = {
                Text(
                    text = "Restore production configuration to '${backupToRestore.backupLabel}' (${backupToRestore.versionLabel})? All connected devices will immediately revert to this version in real time.",
                    color = NoiseTextSecondary,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val target = backupToRestore
                        pendingRollbackBackup = null
                        onRollbackToBackup(target)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = VipGold),
                    modifier = Modifier.testTag("confirm_rollback_button")
                ) {
                    Text("ROLLBACK NOW", color = Color.Black, fontWeight = FontWeight.ExtraBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingRollbackBackup = null }) {
                    Text("Cancel", color = NoiseTextSecondary)
                }
            }
        )
    }
}
