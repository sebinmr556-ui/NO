package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.NoiseGameCatalogItem
import com.example.data.PartyRoom
import com.example.data.Post
import com.example.data.Story
import com.example.data.UiState
import com.example.data.UserProfile
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
import com.example.ui.theme.ThemeManager
import com.example.ui.theme.VipGold

@Composable
fun HomeFeedScreen(
    currentUserId: String,
    userProfile: UserProfile?,
    storiesState: UiState<List<Story>>,
    partyRoomsState: UiState<List<PartyRoom>>,
    postsState: UiState<List<Post>>,
    savedPostIds: Set<String> = emptySet(),
    gameCatalog: List<NoiseGameCatalogItem> = emptyList(),
    selectedDiscoveryCategory: com.example.data.DiscoveryCategory = com.example.data.DiscoveryCategory.FOR_YOU,
    discoveryItems: List<com.example.data.DiscoveryContentItem> = emptyList(),
    disabledDiscoveryCategories: List<String> = emptyList(),
    isDiscoverySyncing: Boolean = false,
    isDiscoveryAdmin: Boolean = false,
    discoveryRefreshIntervalMinutes: Long = 20L,
    onSelectDiscoveryCategory: (com.example.data.DiscoveryCategory) -> Unit = {},
    onRefreshDiscoveryCategory: () -> Unit = {},
    onOpenDiscoveryAdminPanel: () -> Unit = {},
    onLikeDiscoveryItem: (com.example.data.DiscoveryContentItem) -> Unit = {},
    onSaveDiscoveryItem: (com.example.data.DiscoveryContentItem) -> Unit = {},
    onShareDiscoveryItem: (com.example.data.DiscoveryContentItem) -> Unit = {},
    onOpenDiscoveryItem: (com.example.data.DiscoveryContentItem) -> Unit = {},
    onAdminHideDiscoveryItem: (com.example.data.DiscoveryContentItem) -> Unit = {},
    onAdminDeleteDiscoveryItem: (com.example.data.DiscoveryContentItem) -> Unit = {},
    selectedKeralaCategory: com.example.data.KeralaNewsCategory = com.example.data.KeralaNewsCategory.KERALA_LATEST,
    selectedKeralaDistrict: String = "All Districts",
    selectedKeralaLanguage: String = "all",
    disabledKeralaCategories: List<String> = emptyList(),
    isKeralaNewsSyncing: Boolean = false,
    showKeralaNewsSection: Boolean = false,
    onToggleKeralaNewsSection: (Boolean) -> Unit = {},
    onSelectKeralaCategory: (com.example.data.KeralaNewsCategory) -> Unit = {},
    onSelectKeralaDistrict: (String) -> Unit = {},
    onSelectKeralaLanguage: (String) -> Unit = {},
    onRefreshKeralaNews: () -> Unit = {},
    onAddStoryClick: () -> Unit,
    onAddMultipleStoriesFromGallery: (List<Pair<String, String>>) -> Unit = {},
    onOpenStoryAtIndex: (Int) -> Unit = {},
    onDeleteStory: (Story) -> Unit = {},
    onGoLiveClick: () -> Unit,
    onOpenGamesPage: () -> Unit = {},
    onPlayGameNow: (NoiseGameCatalogItem) -> Unit = {},
    onOpenPartyRoom: (PartyRoom) -> Unit,
    onLikePost: (Post) -> Unit,
    onCommentPost: (Post) -> Unit,
    onToggleSavePost: (Post) -> Unit = {},
    onDeletePost: (Post) -> Unit = {},
    onInspectUserProfile: (String) -> Unit = {},
    isSuperOwner: Boolean = false,
    onOpenOwnerChatroomPanel: () -> Unit = {},
    onOwnerDeleteRoom: (PartyRoom) -> Unit = {},
    centralAppConfig: com.example.data.CentralAppConfig = com.example.data.CentralLiveUpdateRuntime.effectiveConfig,
    dynamicPages: List<com.example.data.DynamicAppPage> = com.example.data.CentralLiveUpdateRuntime.effectiveDynamicPages,
    onOpenDynamicPage: (com.example.data.DynamicAppPage) -> Unit = {},
    onOpenCentralAiAdmin: () -> Unit = {}
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val rawStories = (storiesState as? UiState.Success)?.data ?: emptyList()
    // Strictly deduplicate stories so each uploaded story appears only once
    val stories = remember(rawStories) {
        rawStories
            .filter { it.mediaKey.isNotBlank() }
            .distinctBy { "${it.userId}_${it.mediaKey}" }
    }
    // Current user's own uploaded stories update Circle #1 ("Your Story") instead of adding a duplicate circle
    val myLatestStory = remember(stories, currentUserId) {
        stories.firstOrNull { it.userId == currentUserId }
    }
    val myLatestStoryGlobalIndex = remember(stories, myLatestStory) {
        if (myLatestStory == null) -1 else stories.indexOfFirst { it.id == myLatestStory.id }
    }
    // Other users' stories: 1 circle per user who actually uploaded a story, prioritizing followed accounts first
    val followedSet = remember(userProfile?.followedUserIds) {
        userProfile?.followedUserIds?.toSet() ?: emptySet()
    }
    val otherUserStoryGroups = remember(stories, currentUserId, followedSet) {
        stories
            .filter { it.userId.isNotBlank() && it.userId != currentUserId }
            .groupBy { it.userId }
            .values
            .mapNotNull { userStories ->
                val representative = userStories.firstOrNull() ?: return@mapNotNull null
                val anyUnviewed = userStories.any { currentUserId !in it.viewedByUids }
                val firstStoryIndex = stories.indexOfFirst { it.id == representative.id }.coerceAtLeast(0)
                Triple(representative, anyUnviewed, firstStoryIndex)
            }
            .sortedWith(
                compareByDescending<Triple<Story, Boolean, Int>> { it.first.userId in followedSet }
                    .thenByDescending { it.second }
            )
    }

    val posts = (postsState as? UiState.Success)?.data ?: emptyList()
    var storyPendingDeletion by remember { mutableStateOf<Story?>(null) }

    val storyGalleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 10)
    ) { uris ->
        if (uris.isNotEmpty()) {
            val items = uris.distinct().map { uri ->
                persistPickedMediaUri(context, uri)
            }
            onAddMultipleStoriesFromGallery(items)
        }
    }

    fun launchStoryPicker() {
        try {
            storyGalleryLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
            )
        } catch (_: Exception) {
            onAddStoryClick()
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(NoiseBlack)
            .testTag("home_feed_list"),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // 0. Live Central Hero Announcement Banner & Remotely Deployed Dynamic Pages Bar
        item(key = "home_central_live_update_bar") {
            LiveCentralHeroAndDynamicPagesBar(
                config = centralAppConfig,
                dynamicPages = dynamicPages,
                isSuperOwner = isSuperOwner,
                onOpenDynamicPage = onOpenDynamicPage,
                onOpenCentralAiAdmin = onOpenCentralAiAdmin
            )
        }

        // 1. Single Story Section on Home Screen (never duplicated)
        item(key = "home_single_story_section") {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp)
                    .testTag("home_stories_row"),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Circle #1: Always "Your Story" with (+) button. Updates in-place when user uploads a story.
                item(key = "your_story_add_circle") {
                    val hasOwnStory = myLatestStory != null
                    val ownStoryViewed = myLatestStory != null && currentUserId in myLatestStory.viewedByUids
                    val ownRingBrush = when {
                        !hasOwnStory -> Brush.linearGradient(listOf(Color(0xFF282834), Color(0xFF282834)))
                        ownStoryViewed -> Brush.linearGradient(listOf(Color(0xFF3E3E4E), Color(0xFF2A2A36)))
                        else -> Brush.linearGradient(listOf(NoisePink, NoisePurple, VipGold))
                    }
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clickable {
                                if (hasOwnStory && myLatestStoryGlobalIndex >= 0) {
                                    onOpenStoryAtIndex(myLatestStoryGlobalIndex)
                                } else {
                                    launchStoryPicker()
                                }
                            }
                            .testTag("your_story_button")
                    ) {
                        Box(modifier = Modifier.size(72.dp)) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .border(if (hasOwnStory) 2.5.dp else 2.dp, ownRingBrush, CircleShape)
                                    .padding(if (hasOwnStory) 4.dp else 5.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF5B34DA)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (myLatestStory != null) {
                                    NoiseMediaImage(
                                        mediaKey = myLatestStory.effectiveMediaSource(),
                                        contentDescription = "Your story",
                                        thumbnailUrl = myLatestStory.thumbnailUrl,
                                        storagePath = myLatestStory.storagePath,
                                        cloudMediaId = myLatestStory.cloudMediaId,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    NoiseAvatar(
                                        avatarKey = userProfile?.avatarKey ?: "S",
                                        size = 62.dp,
                                        showOnlineDot = false
                                    )
                                }
                            }
                            // Plus (+) badge at bottom-right to upload a new story
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(Brush.linearGradient(listOf(NoisePink, NoisePurple)))
                                    .border(2.dp, NoiseBlack, CircleShape)
                                    .clickable { launchStoryPicker() }
                                    .testTag("your_story_plus_badge"),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Add Story",
                                    tint = Color.White,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                            // Delete own story badge at top-right when user has an active story
                            if (myLatestStory != null) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .size(22.dp)
                                        .clip(CircleShape)
                                        .background(if (ThemeManager.isLightMode) Color(0xFFFFFFFF) else Color(0xDD181822))
                                        .border(1.dp, NoisePink, CircleShape)
                                        .clickable { storyPendingDeletion = myLatestStory }
                                        .testTag("delete_story_badge_0"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "×",
                                        color = NoisePink,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Your story",
                            color = if (hasOwnStory) NoiseWhite else NoiseTextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }

                // Subsequent Circles: Other accounts that have uploaded a story (1 circle per account)
                items(
                    count = otherUserStoryGroups.size,
                    key = { idx -> otherUserStoryGroups[idx].first.userId }
                ) { idx ->
                    val (story, hasUnviewed, globalIndex) = otherUserStoryGroups[idx]
                    val ringBrush = if (!hasUnviewed) {
                        Brush.linearGradient(listOf(Color(0xFF3E3E4E), Color(0xFF2A2A36)))
                    } else {
                        Brush.linearGradient(listOf(NoisePink, NoisePurple, VipGold))
                    }
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clickable { onOpenStoryAtIndex(globalIndex) }
                            .testTag("story_circle_$idx")
                    ) {
                        Box(modifier = Modifier.size(72.dp)) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .border(2.5.dp, ringBrush, CircleShape)
                                    .padding(4.dp)
                                    .clip(CircleShape)
                            ) {
                                NoiseMediaImage(
                                    mediaKey = story.effectiveMediaSource(),
                                    contentDescription = story.username,
                                    thumbnailUrl = story.thumbnailUrl,
                                    storagePath = story.storagePath,
                                    cloudMediaId = story.cloudMediaId,
                                    modifier = Modifier.fillMaxSize()
                                )
                                if (story.mediaType == "video") {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .size(18.dp)
                                            .clip(CircleShape)
                                            .background(NoiseBlack.copy(alpha = 0.8f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Movie,
                                            contentDescription = "Video Story",
                                            tint = NoisePink,
                                            modifier = Modifier.size(11.dp)
                                        )
                                    }
                                }
                            }
                            // Small author profile avatar badge at bottom-left
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .border(1.5.dp, NoiseBlack, CircleShape)
                            ) {
                                NoiseAvatar(
                                    avatarKey = story.authorAvatarKey.ifBlank { story.username.take(1).uppercase() },
                                    size = 22.dp,
                                    showOnlineDot = false
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = story.username,
                            color = if (!hasUnviewed) NoiseTextSecondary else NoiseWhite,
                            fontSize = 12.sp
                        )
                    }
                }
            }
            HorizontalDivider(color = NoiseBorder.copy(alpha = 0.5f), thickness = 0.8.dp)
        }

        // 2. VIP PARTY ROOMS Section
        item(key = "home_single_vip_party_stage_section") {
            val isLight = ThemeManager.isLightMode
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            if (isLight) {
                                listOf(
                                    Color(0xFFFFFFFF),
                                    Color(0xFFF8F8FC),
                                    Color(0xFFFFFFFF)
                                )
                            } else {
                                listOf(
                                    Color(0xFF150D24),
                                    Color(0xFF1E0E2E),
                                    Color(0xFF0E0818)
                                )
                            }
                        )
                    )
                    .padding(vertical = 16.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(VipGold),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.WorkspacePremium,
                                    contentDescription = "VIP Crown",
                                    tint = Color.Black,
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "VIP PARTY ROOMS",
                                color = VipGold,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.sp,
                                letterSpacing = 0.6.sp
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (isSuperOwner) {
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(Color(0xFF00E676))
                                        .border(1.dp, VipGold, RoundedCornerShape(20.dp))
                                        .clickable { onOpenCentralAiAdmin() }
                                        .padding(horizontal = 10.dp, vertical = 7.dp)
                                        .testTag("open_central_ai_admin_home_button"),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = "Central AI Admin",
                                        tint = Color.Black,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "AI ADMIN",
                                        color = Color.Black,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 11.sp
                                    )
                                }

                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(Color(0xFFD50000))
                                        .border(1.dp, VipGold, RoundedCornerShape(20.dp))
                                        .clickable { onOpenOwnerChatroomPanel() }
                                        .padding(horizontal = 10.dp, vertical = 7.dp)
                                        .testTag("open_owner_chatroom_panel_button"),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteOutline,
                                        contentDescription = "Owner Chatroom Management",
                                        tint = VipGold,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "OWNER",
                                        color = Color.White,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            // GAMES button in VIP PARTY ROOMS header
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(NoisePink, NoisePurple)
                                        )
                                    )
                                    .border(1.dp, VipGold.copy(alpha = 0.7f), RoundedCornerShape(20.dp))
                                    .clickable { onOpenGamesPage() }
                                    .padding(horizontal = 12.dp, vertical = 7.dp)
                                    .testTag("home_games_button"),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SportsEsports,
                                    contentDescription = "Open Games",
                                    tint = VipGold,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = "GAMES",
                                    color = Color.White,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 12.sp
                                )
                            }

                            // + GO LIVE gold pill button with pulsing yellow dot
                            Box {
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(VipGold)
                                        .clickable { onGoLiveClick() }
                                        .padding(horizontal = 14.dp, vertical = 7.dp)
                                        .testTag("go_live_button"),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = null,
                                        tint = Color.Black,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "GO LIVE",
                                        color = Color.Black,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 12.sp
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .offset(x = 3.dp, y = (-3).dp)
                                        .size(9.dp)
                                        .clip(CircleShape)
                                        .background(VipGold)
                                        .border(1.5.dp, NoiseBlack, CircleShape)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Horizontal sliding carousel of currently active user-created chatrooms + 3D Games
                    val activePartyRooms = remember(partyRoomsState) {
                        (partyRoomsState as? UiState.Success)?.data
                            ?.filter { it.live && (it.isActive || it.active) && !it.isDummyOrSampleRoom() }
                            .orEmpty()
                    }

                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("home_active_chatrooms_carousel"),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (activePartyRooms.isEmpty()) {
                            item(key = "empty_real_chatrooms_card") {
                                Box(
                                    modifier = Modifier
                                        .width(185.dp)
                                        .height(165.dp)
                                        .clip(RoundedCornerShape(22.dp))
                                        .background(
                                            Brush.verticalGradient(
                                                if (isLight) {
                                                    listOf(Color(0xFFFFFFFF), LightLavender, Color(0xFFFFFFFF))
                                                } else {
                                                    listOf(Color(0xFF1E1030), Color(0xFF12091E))
                                                }
                                            )
                                        )
                                        .border(
                                            width = 1.2.dp,
                                            brush = Brush.linearGradient(listOf(VipGold, NoisePink)),
                                            shape = RoundedCornerShape(22.dp)
                                        )
                                        .clickable { onGoLiveClick() }
                                        .padding(14.dp)
                                        .testTag("empty_active_chatrooms_card"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(44.dp)
                                                .clip(CircleShape)
                                                .background(VipGold.copy(alpha = 0.2f))
                                                .border(1.dp, VipGold, CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Mic,
                                                contentDescription = "Create Room",
                                                tint = VipGold,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Text(
                                            text = "No Active Rooms",
                                            color = NoiseWhite,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 13.sp,
                                            textAlign = TextAlign.Center
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Tap + GO LIVE to start a public or private room",
                                            color = NoiseTextSecondary,
                                            fontSize = 10.sp,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        } else {
                            items(activePartyRooms, key = { it.id }) { activeRoom ->
                                ActiveChatroomCarouselCard(
                                    room = activeRoom,
                                    onClick = { onOpenPartyRoom(activeRoom) }
                                )
                            }
                        }

                        item(key = "home_games_stage_card") {
                            // 3D Games Stage Card right next to active chatrooms & Open Stage
                            Box(
                                modifier = Modifier
                                    .width(155.dp)
                                    .height(165.dp)
                                    .clip(RoundedCornerShape(22.dp))
                                    .background(
                                        Brush.verticalGradient(
                                            if (isLight) {
                                                listOf(
                                                    Color(0xFFFFFFFF),
                                                    LightLavender,
                                                    Color(0xFFFFFFFF)
                                                )
                                            } else {
                                                listOf(
                                                    Color(0xFF28123D),
                                                    Color(0xFF170B28),
                                                    Color(0xFF0E0819)
                                                )
                                            }
                                        )
                                    )
                                    .border(
                                        width = 1.3.dp,
                                        brush = Brush.linearGradient(listOf(NoisePink, VipGold)),
                                        shape = RoundedCornerShape(22.dp)
                                    )
                                    .clickable { onOpenGamesPage() }
                                    .testTag("home_games_stage_card")
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(vertical = 18.dp, horizontal = 12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(56.dp)
                                            .clip(CircleShape)
                                            .background(Brush.linearGradient(listOf(NoisePink, NoisePurple)))
                                            .border(1.5.dp, VipGold, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.SportsEsports,
                                            contentDescription = "3D Games",
                                            tint = VipGold,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }

                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = "3D Games",
                                            color = NoiseWhite,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            textAlign = TextAlign.Center
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Carrom • Ludo • Pool",
                                            color = VipGold,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Horizontal Multiplayer Games Quick-Launch Strip on Home Screen
                    if (gameCatalog.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.SportsEsports,
                                    contentDescription = null,
                                    tint = NoisePink,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "MULTIPLAYER GAMES",
                                    color = NoiseWhite,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 13.sp,
                                    letterSpacing = 0.5.sp
                                )
                            }
                            Text(
                                text = "OPEN GAMES MENU >",
                                color = VipGold,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 11.sp,
                                modifier = Modifier
                                    .clickable { onOpenGamesPage() }
                                    .testTag("home_see_all_games_button")
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(gameCatalog, key = { it.gameType }) { game ->
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(if (isLight) Color(0xFFFFFFFF) else Color(0xFF171128))
                                        .border(
                                            1.dp,
                                            Color(game.accentColorHex).copy(alpha = 0.65f),
                                            RoundedCornerShape(16.dp)
                                        )
                                        .clickable { onPlayGameNow(game) }
                                        .padding(horizontal = 12.dp, vertical = 10.dp)
                                        .testTag("home_quick_game_${game.gameType}"),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(
                                                Brush.linearGradient(
                                                    listOf(
                                                        Color(game.accentColorHex),
                                                        Color(game.secondaryColorHex)
                                                    )
                                                )
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.SportsEsports,
                                            contentDescription = game.title,
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = game.title,
                                            color = NoiseWhite,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 13.sp
                                        )
                                        Text(
                                            text = "${game.minPlayers}-${game.maxPlayers} Players • Play Now",
                                            color = VipGold,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(NoisePink),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = "Play Now",
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            HorizontalDivider(color = NoiseBorder.copy(alpha = 0.5f), thickness = 0.8.dp)
        }

        // 2B. AUTOMATIC DISCOVERY & CONTENT IMPORT CATEGORY BUTTONS ROW
        item(key = "home_discovery_category_bar_item") {
            DiscoveryCategoryScrollBar(
                selectedCategory = selectedDiscoveryCategory,
                disabledCategories = disabledDiscoveryCategories,
                isSyncing = isDiscoverySyncing,
                isAdmin = isDiscoveryAdmin,
                refreshIntervalMinutes = discoveryRefreshIntervalMinutes,
                onSelectCategory = { cat ->
                    onToggleKeralaNewsSection(false)
                    onSelectDiscoveryCategory(cat)
                },
                onRefreshCategory = onRefreshDiscoveryCategory,
                onOpenAdminPanel = onOpenDiscoveryAdminPanel
            )
            HorizontalDivider(color = NoiseBorder.copy(alpha = 0.45f), thickness = 0.8.dp)
        }

        // 2C. AUTOMATIC KERALA NEWS (കേരള വാർത്തകൾ) DEDICATED SECTION
        val keralaAllItems = discoveryItems.filter { item ->
            item.isKeralaNews || item.categoryKey.startsWith("kerala_")
        }.sortedByDescending { it.publishedAtMillis }

        val breakingKeralaItems = keralaAllItems.filter { it.isBreakingNews }

        item(key = "home_kerala_news_hub_item") {
            KeralaNewsHubSection(
                selectedCategory = selectedKeralaCategory,
                selectedDistrict = selectedKeralaDistrict,
                selectedLanguage = selectedKeralaLanguage,
                disabledKeralaCategories = disabledKeralaCategories,
                breakingNewsItems = breakingKeralaItems,
                isSyncing = isKeralaNewsSyncing,
                isExpanded = showKeralaNewsSection,
                onToggleExpanded = { onToggleKeralaNewsSection(!showKeralaNewsSection) },
                onSelectCategory = onSelectKeralaCategory,
                onSelectDistrict = onSelectKeralaDistrict,
                onSelectLanguage = onSelectKeralaLanguage,
                onRefreshKeralaNews = onRefreshKeralaNews,
                onOpenBreakingItem = onOpenDiscoveryItem
            )
        }

        // 3. Automatic Kerala News Feed (when expanded) OR Global Discovery + Social Feed Posts
        if (showKeralaNewsSection) {
            val filteredKeralaItems = keralaAllItems.filter { item ->
                val matchesCat = when (selectedKeralaCategory) {
                    com.example.data.KeralaNewsCategory.KERALA_LATEST -> true
                    com.example.data.KeralaNewsCategory.KERALA_BREAKING ->
                        item.isBreakingNews || item.categoryKey == com.example.data.KeralaNewsCategory.KERALA_BREAKING.key
                    com.example.data.KeralaNewsCategory.KERALA_DISTRICT ->
                        item.categoryKey == com.example.data.KeralaNewsCategory.KERALA_DISTRICT.key ||
                            item.district != "All Districts"
                    else ->
                        item.categoryKey.equals(selectedKeralaCategory.key, ignoreCase = true)
                }
                val matchesDistrict = selectedKeralaDistrict == "All Districts" ||
                    item.district.equals(selectedKeralaDistrict, ignoreCase = true) ||
                    item.title.contains(selectedKeralaDistrict, ignoreCase = true) ||
                    item.description.contains(selectedKeralaDistrict, ignoreCase = true)
                val matchesLang = selectedKeralaLanguage == "all" ||
                    item.language.equals(selectedKeralaLanguage, ignoreCase = true)
                matchesCat && matchesDistrict && matchesLang
            }.sortedByDescending { it.publishedAtMillis }

            if (filteredKeralaItems.isEmpty()) {
                item(key = "kerala_news_empty_state_${selectedKeralaCategory.key}") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 18.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(NoiseCardElevated)
                            .border(1.dp, OnlineGreen.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                            .padding(20.dp)
                            .testTag("kerala_news_empty_state"),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (isKeralaNewsSyncing) {
                            CircularProgressIndicator(
                                color = OnlineGreen,
                                strokeWidth = 2.5.dp,
                                modifier = Modifier.size(30.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Fetching latest ${selectedKeralaCategory.label} (${selectedKeralaCategory.malayalamLabel})...",
                                color = NoiseWhite,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center
                            )
                        } else {
                            Text(
                                text = "🌴 No verified ${selectedKeralaCategory.label} articles for $selectedKeralaDistrict right now.",
                                color = NoiseWhite,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = onRefreshKeralaNews,
                                colors = ButtonDefaults.buttonColors(containerColor = OnlineGreen),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Text(
                                    text = "Fetch Latest Kerala News",
                                    color = Color.Black,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            } else {
                items(
                    items = filteredKeralaItems,
                    key = { "kerala_${it.id}" }
                ) { kItem ->
                    DiscoveryContentCard(
                        item = kItem,
                        isLiked = currentUserId in kItem.likedByUids,
                        isSaved = currentUserId in kItem.savedByUids,
                        isAdmin = isDiscoveryAdmin,
                        onLikeClick = { onLikeDiscoveryItem(kItem) },
                        onSaveClick = { onSaveDiscoveryItem(kItem) },
                        onShareClick = { onShareDiscoveryItem(kItem) },
                        onOpenContent = { onOpenDiscoveryItem(kItem) },
                        onAdminHideClick = { onAdminHideDiscoveryItem(kItem) },
                        onAdminDeleteClick = { onAdminDeleteDiscoveryItem(kItem) }
                    )
                }
            }
        } else {
            val filteredDiscoveryItems = when (selectedDiscoveryCategory) {
                com.example.data.DiscoveryCategory.FOR_YOU -> {
                    discoveryItems.sortedByDescending { it.publishedAtMillis }
                }
                com.example.data.DiscoveryCategory.TRENDING -> {
                    val nowMs = System.currentTimeMillis()
                    discoveryItems.sortedByDescending { it.computeTrendingScore(nowMs) }
                }
                else -> {
                    discoveryItems
                        .filter { it.categoryKey.equals(selectedDiscoveryCategory.key, ignoreCase = true) }
                        .sortedByDescending { it.publishedAtMillis }
                }
            }

            if (selectedDiscoveryCategory != com.example.data.DiscoveryCategory.FOR_YOU) {
                if (filteredDiscoveryItems.isEmpty()) {
                    item(key = "discovery_empty_safe_state_${selectedDiscoveryCategory.key}") {
                        DiscoveryEmptySafeStateCard(
                            category = selectedDiscoveryCategory,
                            isSyncing = isDiscoverySyncing,
                            onRetryFetch = onRefreshDiscoveryCategory
                        )
                    }
                } else {
                    items(
                        items = filteredDiscoveryItems,
                        key = { "disc_${it.id}" }
                    ) { discItem ->
                        DiscoveryContentCard(
                            item = discItem,
                            isLiked = currentUserId in discItem.likedByUids,
                            isSaved = currentUserId in discItem.savedByUids,
                            isAdmin = isDiscoveryAdmin,
                            onLikeClick = { onLikeDiscoveryItem(discItem) },
                            onSaveClick = { onSaveDiscoveryItem(discItem) },
                            onShareClick = { onShareDiscoveryItem(discItem) },
                            onOpenContent = { onOpenDiscoveryItem(discItem) },
                            onAdminHideClick = { onAdminHideDiscoveryItem(discItem) },
                            onAdminDeleteClick = { onAdminDeleteDiscoveryItem(discItem) }
                        )
                    }
                }
            } else {
                // FOR YOU: Interleave verified discovery updates and community social posts
                if (filteredDiscoveryItems.isNotEmpty()) {
                    items(
                        items = filteredDiscoveryItems.take(10),
                        key = { "disc_foryou_${it.id}" }
                    ) { discItem ->
                        DiscoveryContentCard(
                            item = discItem,
                            isLiked = currentUserId in discItem.likedByUids,
                            isSaved = currentUserId in discItem.savedByUids,
                            isAdmin = isDiscoveryAdmin,
                            onLikeClick = { onLikeDiscoveryItem(discItem) },
                            onSaveClick = { onSaveDiscoveryItem(discItem) },
                            onShareClick = { onShareDiscoveryItem(discItem) },
                            onOpenContent = { onOpenDiscoveryItem(discItem) },
                            onAdminHideClick = { onAdminHideDiscoveryItem(discItem) },
                            onAdminDeleteClick = { onAdminDeleteDiscoveryItem(discItem) }
                        )
                    }
                }

            if (postsState is UiState.Loading && filteredDiscoveryItems.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 80.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(
                            color = NoisePink,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "Connecting to Noise...",
                            color = NoiseTextSecondary,
                            fontSize = 14.sp
                        )
                    }
                }
            } else if (posts.isEmpty() && filteredDiscoveryItems.isEmpty()) {
                item(key = "for_you_empty_safe_state") {
                    DiscoveryEmptySafeStateCard(
                        category = selectedDiscoveryCategory,
                        isSyncing = isDiscoverySyncing,
                        onRetryFetch = onRefreshDiscoveryCategory
                    )
                }
            } else {
                items(posts, key = { it.id }) { post ->
                    PostFeedCard(
                        post = post,
                        isLiked = currentUserId in post.likedByUids,
                        isSaved = post.id in savedPostIds,
                        isOwnPost = post.userId == currentUserId || isSuperOwner,
                        onLikeClick = { onLikePost(post) },
                        onCommentClick = { onCommentPost(post) },
                        onToggleSaveClick = { onToggleSavePost(post) },
                        onDeletePostClick = { onDeletePost(post) },
                        onAuthorClick = { if (post.userId.isNotBlank()) onInspectUserProfile(post.userId) }
                    )
                }
            }
            }
        }
    }

    val pendingDelete = storyPendingDeletion
    if (pendingDelete != null) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { storyPendingDeletion = null }
        ) {
            androidx.compose.material3.Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.2.dp, NoisePink, RoundedCornerShape(22.dp))
                    .testTag("home_story_delete_confirm_dialog"),
                shape = RoundedCornerShape(22.dp),
                color = NoiseCard
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Delete Story?",
                        color = NoiseWhite,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 17.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Are you sure you want to permanently remove this story from your Home Screen and storage?",
                        color = NoiseTextSecondary,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(18.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(NoiseCardElevated)
                                .border(1.dp, NoiseBorder, RoundedCornerShape(14.dp))
                                .clickable { storyPendingDeletion = null },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Cancel",
                                color = NoiseTextSecondary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFFDC2626))
                                .clickable {
                                    onDeleteStory(pendingDelete)
                                    storyPendingDeletion = null
                                }
                                .testTag("confirm_home_story_delete_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Delete Story",
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VipPartyRoomCard(
    room: PartyRoom,
    onClick: () -> Unit
) {
    val isLight = ThemeManager.isLightMode
    val dashStroke = remember {
        Stroke(
            width = 3f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)
        )
    }

    Box(
        modifier = Modifier
            .width(156.dp)
            .height(165.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(if (isLight) LightLavender else Color(0xFF0C0A0E))
            .clickable { onClick() }
            .testTag("party_room_${room.title}")
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRoundRect(
                color = if (isLight) VipGold else Color(0xFF6E4E10),
                cornerRadius = CornerRadius(18.dp.toPx(), 18.dp.toPx()),
                style = dashStroke
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(VipGold),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Microphone",
                        tint = Color.Black,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = "VIP Sparkle",
                    tint = VipGold,
                    modifier = Modifier.size(18.dp)
                )
            }

            Column {
                Text(
                    text = room.title,
                    color = NoiseWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = room.subtitle,
                    color = NoiseTextSecondary,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(if (isLight) VipGold else Color(0xFF8B5A00))
                )
            }
        }
    }
}

@Composable
fun ActiveChatroomCarouselCard(
    room: PartyRoom,
    onClick: () -> Unit
) {
    val isLight = ThemeManager.isLightMode
    val participantCount = room.activeParticipantCount()
    val hostAvatar = room.resolvedHostAvatarKey()
    val hasActiveYouTube = room.resolvedYouTubeVideoId().isNotBlank()
    val coverKey = room.resolvedCoverThemeKey().lowercase()
    val coverLabel = room.resolvedCoverThemeLabel()
    val darkCoverColors = when (coverKey) {
        "burj_sunset" -> listOf(Color(0xFF3E1E14), Color(0xFF8C4A27), Color(0xFF150A24))
        "royal_majlis" -> listOf(Color(0xFF2E1012), Color(0xFF6B1D1D), Color(0xFF120818))
        "cosmic_nebula" -> listOf(Color(0xFF1E0B36), Color(0xFF4C1D95), Color(0xFF0C0716))
        "imax_stage" -> listOf(Color(0xFF0F172A), Color(0xFF1E3A8A), Color(0xFF090D16))
        else -> listOf(Color(0xFF231036), Color(0xFF150A24), Color(0xFF0C0716))
    }

    Box(
        modifier = Modifier
            .width(172.dp)
            .height(165.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(
                Brush.verticalGradient(
                    if (isLight) {
                        listOf(
                            Color(0xFFFFFFFF),
                            LightLavender,
                            Color(0xFFFFFFFF)
                        )
                    } else {
                        darkCoverColors
                    }
                )
            )
            .border(
                width = 1.4.dp,
                brush = Brush.linearGradient(listOf(NoisePink, NoisePurple, VipGold)),
                shape = RoundedCornerShape(22.dp)
            )
            .clickable { onClick() }
            .testTag("active_chatroom_card_${room.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Row: LIVE badge + Participant count
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(NoisePink.copy(alpha = 0.22f))
                        .border(1.dp, NoisePink, RoundedCornerShape(10.dp))
                        .padding(horizontal = 7.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(OnlineGreen)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (room.isPublicRoom()) "LIVE" else "🔒 VIP",
                        color = NoiseWhite,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 10.sp
                    )
                }

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isLight) Color(0xFFFFF8DB) else Color.Black.copy(alpha = 0.55f))
                        .border(0.8.dp, VipGold.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 7.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Group,
                        contentDescription = "People inside",
                        tint = VipGold,
                        modifier = Modifier.size(11.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "$participantCount",
                        color = if (isLight) NoiseWhite else VipGold,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 10.sp
                    )
                }
            }

            // Center: Owner Avatar with VIP ring + Host Username + Cover Theme
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .border(
                            1.5.dp,
                            Brush.linearGradient(listOf(VipGold, NoisePink)),
                            CircleShape
                        )
                        .padding(2.dp)
                ) {
                    NoiseAvatar(
                        avatarKey = hostAvatar,
                        size = 42.dp,
                        showOnlineDot = true
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "@${room.hostUsername.ifBlank { "host" }}",
                        color = if (isLight) NoiseNormalText else VipGold,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (hasActiveYouTube) "▶️ YouTube Watch" else "${room.resolvedCategory()} • $coverLabel",
                        color = if (hasActiveYouTube) NoisePink else NoiseTextSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Bottom: Room Name + Join indicator bar
            Column {
                Text(
                    text = room.resolvedRoomName(),
                    color = NoiseWhite,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Brush.horizontalGradient(listOf(NoisePink, NoisePurple, VipGold)))
                )
            }
        }
    }
}

@Composable
fun ActiveChatroomsListScreen(
    partyRoomsState: UiState<List<PartyRoom>>,
    onBack: () -> Unit,
    onCreateChatroomClick: () -> Unit,
    onRefreshClick: () -> Unit,
    onOpenPartyRoom: (PartyRoom) -> Unit,
    isSuperOwner: Boolean = false,
    onOpenOwnerChatroomPanel: () -> Unit = {},
    onOwnerDeleteRoom: (PartyRoom) -> Unit = {}
) {
    BackHandler { onBack() }

    var searchQuery by remember { mutableStateOf("") }
    val allActiveRooms = remember(partyRoomsState) {
        (partyRoomsState as? UiState.Success)?.data
            ?.filter { it.live && (it.isActive || it.active) && it.id.isNotBlank() && !it.isDummyOrSampleRoom() }
            .orEmpty()
    }
    val filteredRooms = remember(allActiveRooms, searchQuery) {
        val q = searchQuery.trim().lowercase()
        if (q.isBlank()) {
            allActiveRooms
        } else {
            allActiveRooms.filter { room ->
                room.resolvedRoomName().lowercase().contains(q) ||
                    room.id.lowercase().contains(q) ||
                    room.hostUsername.lowercase().contains(q) ||
                    room.resolvedCategory().lowercase().contains(q) ||
                    room.resolvedDescription().lowercase().contains(q)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NoiseBlack)
            .testTag("active_chatrooms_screen")
    ) {
        // Top Header Bar with Back button, Title, and Refresh button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(NoiseCard)
                        .border(1.dp, NoiseBorder, CircleShape)
                        .clickable { onBack() }
                        .testTag("chatrooms_back_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = NoiseWhite,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Sensors,
                            contentDescription = null,
                            tint = NoisePink,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "LIVE CHATROOMS",
                            color = NoiseWhite,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 17.sp
                        )
                    }
                    Text(
                        text = "${allActiveRooms.size} active room(s) online now",
                        color = VipGold,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(NoiseCard)
                    .border(1.dp, NoiseBorder, CircleShape)
                    .clickable { onRefreshClick() }
                    .testTag("chatrooms_refresh_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh Chatrooms",
                    tint = VipGold,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        HorizontalDivider(color = NoiseBorder.copy(alpha = 0.6f), thickness = 0.8.dp)

        Spacer(modifier = Modifier.height(14.dp))

        // Prominent CREATE CHATROOM Button at the top
        val isLight = ThemeManager.isLightMode
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(52.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(Brush.horizontalGradient(listOf(NoisePink, NoisePurple, VipGold)))
                .clickable { onCreateChatroomClick() }
                .testTag("chatrooms_create_button"),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "CREATE CHATROOM",
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp,
                    letterSpacing = 0.6.sp
                )
            }
        }

        if (isSuperOwner) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(46.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color(0xFFB71C1C), Color(0xFFD50000), Color(0xFF880E4F))
                        )
                    )
                    .border(1.2.dp, VipGold, RoundedCornerShape(16.dp))
                    .clickable { onOpenOwnerChatroomPanel() }
                    .testTag("chatrooms_owner_panel_button"),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = null,
                        tint = VipGold,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "👑 OWNER CHATROOM MANAGEMENT & DELETE PANEL",
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.sp
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Search Bar to filter active chatrooms by room name or owner
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = {
                Text(
                    text = "Search active chatrooms by room name or @owner...",
                    color = NoiseTextMuted,
                    fontSize = 13.sp
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search Chatrooms",
                    tint = NoisePink
                )
            },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .testTag("chatrooms_search_input"),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = if (isLight) Color(0xFFFFFFFF) else Color(0xFF14111D),
                unfocusedContainerColor = if (isLight) Color(0xFFFFFFFF) else Color(0xFF14111D),
                focusedBorderColor = NoisePink,
                unfocusedBorderColor = NoiseBorder,
                focusedTextColor = NoiseNormalText,
                unfocusedTextColor = NoiseNormalText
            )
        )

        Spacer(modifier = Modifier.height(14.dp))

        if (filteredRooms.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(if (isLight) LightLavender else Color(0xFF1A1228))
                            .border(1.2.dp, NoisePink.copy(alpha = 0.6f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sensors,
                            contentDescription = null,
                            tint = VipGold,
                            modifier = Modifier.size(34.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = if (searchQuery.isBlank()) "No Active Chatrooms Right Now" else "No Matching Chatrooms Found",
                        color = NoiseWhite,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 17.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (searchQuery.isBlank()) {
                            "Tap CREATE CHATROOM above to launch a live voice & YouTube party stage!"
                        } else {
                            "Try another room name or @username."
                        },
                        color = NoiseTextSecondary,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .testTag("chatrooms_active_list"),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredRooms, key = { it.id }) { room ->
                    val count = room.activeParticipantCount()
                    val hostAvatar = room.resolvedHostAvatarKey()
                    val hasYouTube = room.resolvedYouTubeVideoId().isNotBlank()

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                Brush.horizontalGradient(
                                    if (isLight) {
                                        listOf(
                                            Color(0xFFFFFFFF),
                                            LightLavender
                                        )
                                    } else {
                                        listOf(
                                            Color(0xFF1D102D),
                                            Color(0xFF130B20)
                                        )
                                    }
                                )
                            )
                            .border(
                                width = 1.2.dp,
                                brush = Brush.linearGradient(listOf(NoisePink, NoisePurple, VipGold)),
                                shape = RoundedCornerShape(20.dp)
                            )
                            .clickable { onOpenPartyRoom(room) }
                            .padding(14.dp)
                            .testTag("chatroom_list_item_${room.id}"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(58.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .border(
                                        1.5.dp,
                                        Brush.linearGradient(listOf(VipGold, NoisePink)),
                                        RoundedCornerShape(16.dp)
                                    )
                                    .padding(3.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                NoiseAvatar(
                                    avatarKey = hostAvatar,
                                    size = 50.dp,
                                    showOnlineDot = true
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = room.resolvedRoomName(),
                                        color = NoiseWhite,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 16.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = "Host: @${room.hostUsername.ifBlank { "host" }} • ${room.resolvedCategory()} • ${room.resolvedCoverThemeLabel()}",
                                    color = if (isLight) NoiseNormalText else VipGold,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = if (hasYouTube) {
                                        "▶️ Watching YouTube Live / Video Together"
                                    } else {
                                        room.resolvedDescription()
                                    },
                                    color = if (hasYouTube) NoisePink else NoiseTextSecondary,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column(
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(NoisePink.copy(alpha = 0.22f))
                                    .border(1.dp, NoisePink, RoundedCornerShape(10.dp))
                                    .padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(OnlineGreen)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "LIVE",
                                    color = NoiseWhite,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 10.sp
                                )
                            }

                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isLight) Color(0xFFFFF8DB) else Color.Black.copy(alpha = 0.55f))
                                    .border(1.dp, VipGold.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                                    .padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Group,
                                    contentDescription = "Participants",
                                    tint = VipGold,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "$count inside",
                                    color = if (isLight) NoiseWhite else VipGold,
                                    fontWeight = FontWeight.Bold,
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

@Composable
fun PostFeedCard(
    post: Post,
    isLiked: Boolean,
    isSaved: Boolean = false,
    isOwnPost: Boolean = false,
    onLikeClick: () -> Unit,
    onCommentClick: () -> Unit,
    onToggleSaveClick: () -> Unit = {},
    onDeletePostClick: () -> Unit = {},
    onAuthorClick: () -> Unit = {}
) {
    val context = androidx.compose.ui.platform.LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(NoiseBlack)
            .padding(vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { onAuthorClick() }
            ) {
                NoiseAvatar(avatarKey = post.authorAvatarKey, size = 38.dp, showOnlineDot = true)
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val resolvedPostVip = post.authorVipBadgeLevel.toInt().coerceIn(0, 10)
                        VipAnimatedUsername(
                            username = post.username,
                            vipLevel = resolvedPostVip,
                            fontSize = 14.sp
                        )
                        if (resolvedPostVip > 0) {
                            Spacer(modifier = Modifier.width(5.dp))
                            VipInlineGemBadge(
                                vipLevel = resolvedPostVip,
                                gemSize = 20.dp,
                                showLabel = true
                            )
                        }
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = post.vibeEmoji,
                            fontSize = 13.sp
                        )
                    }
                    if (post.location.isNotBlank()) {
                        Text(
                            text = post.location,
                            color = NoiseTextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (ThemeManager.isLightMode) LightLavender else NoiseCard)
                        .border(
                            1.dp,
                            if (ThemeManager.isLightMode) NoisePurple.copy(alpha = 0.28f) else Color.Transparent,
                            RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = post.tag,
                        color = NoisePink,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                if (isOwnPost) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (ThemeManager.isLightMode) Color(0xFFFEF2F2) else Color(0xFF2A121C))
                            .clickable { onDeletePostClick() }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .testTag("delete_post_button_${post.id}")
                    ) {
                        Text(
                            text = "Delete",
                            color = NoisePink,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        if (post.mediaType == "video") {
            NoiseVideoPlayer(
                mediaKey = post.effectiveMediaSource(),
                contentDescription = post.caption,
                thumbnailUrl = post.thumbnailUrl,
                storagePath = post.storagePath,
                cloudMediaId = post.cloudMediaId,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .background(NoiseCard)
            )
        } else {
            NoiseMediaImage(
                mediaKey = post.effectiveMediaSource(),
                contentDescription = post.caption,
                thumbnailUrl = post.thumbnailUrl,
                storagePath = post.storagePath,
                cloudMediaId = post.cloudMediaId,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .background(NoiseCard)
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onLikeClick,
                    modifier = Modifier.testTag("like_button_${post.id}")
                ) {
                    Icon(
                        imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Like",
                        tint = if (isLiked) NoisePink else NoiseWhite,
                        modifier = Modifier.size(26.dp)
                    )
                }
                Text(
                    text = "${post.likesCount}",
                    color = NoiseWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.width(12.dp))
                IconButton(
                    onClick = onCommentClick,
                    modifier = Modifier.testTag("comment_button_${post.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.ChatBubbleOutline,
                        contentDescription = "Comments",
                        tint = NoiseWhite,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Text(
                    text = "${post.commentsCount}",
                    color = NoiseWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                        clipboard?.setPrimaryClip(
                            ClipData.newPlainText("Noise Post", "https://noise.app/p/${post.id}")
                        )
                        Toast.makeText(context, "Post link copied to clipboard!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.testTag("share_button_${post.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share",
                        tint = NoiseWhite,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            IconButton(
                onClick = onToggleSaveClick,
                modifier = Modifier.testTag("save_button_${post.id}")
            ) {
                Icon(
                    imageVector = if (isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                    contentDescription = "Save Post",
                    tint = if (isSaved) VipGold else NoiseWhite,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Text(
                text = "${post.username}  ${post.caption}",
                color = NoiseNormalText,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "View all ${post.commentsCount} comments",
                color = NoiseTextSecondary,
                fontSize = 12.sp,
                modifier = Modifier.clickable { onCommentClick() }
            )
        }

        Spacer(modifier = Modifier.height(10.dp))
        HorizontalDivider(color = NoiseBorder.copy(alpha = 0.4f), thickness = 0.8.dp)
    }
}

@Composable
fun CreatePostScreen(
    onCancel: () -> Unit,
    onSharePost: (mediaKey: String, mediaType: String, caption: String, location: String, tag: String, vibeEmoji: String) -> Unit,
    uploadState: com.example.data.MediaUploadState = com.example.data.MediaUploadState(),
    onRetryUpload: () -> Unit = {}
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var selectedMediaKey by remember { mutableStateOf("roses") }
    var hasSelectedMedia by remember { mutableStateOf(false) }
    var selectedMediaType by remember { mutableStateOf("photo") }
    var selectedEmoji by remember { mutableStateOf("✨") }
    var caption by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val (persistedKey, detectedType) = persistPickedMediaUri(
                context = context,
                uri = uri,
                fallbackMediaType = selectedMediaType
            )
            selectedMediaKey = persistedKey
            selectedMediaType = detectedType
            hasSelectedMedia = true
        }
    }

    val vibeEmojis = listOf("✨", "🔥", "📸", "☕", "🏔️", "🚀", "❤️", "🌴")
    val presetGallery = listOf(
        "roses" to "Rose Garden",
        "couple" to "Neon Couple",
        "mic" to "Studio Mic",
        "wave" to "Synth Wave",
        "lake" to "Alpine Lake",
        "canyon" to "Red Canyon"
    )
    val gradientBrush = Brush.horizontalGradient(listOf(NoisePink, NoisePurple))
    val dashStroke = remember {
        Stroke(
            width = 3f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f), 0f)
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NoiseBlack)
            .verticalScroll(rememberScrollState())
    ) {
        // Sub-header: Cancel | Create new post | Share (matching Screenshot 3)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Cancel",
                color = NoiseTextSecondary,
                fontSize = 16.sp,
                modifier = Modifier
                    .clickable { onCancel() }
                    .testTag("create_post_cancel")
            )

            Text(
                text = "Create new post",
                color = NoiseWhite,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp
            )

            Text(
                text = "Share",
                color = NoisePink.copy(alpha = 0.85f),
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                modifier = Modifier
                    .clickable {
                        onSharePost(selectedMediaKey, selectedMediaType, caption, location, "#NoiseVibes", selectedEmoji)
                    }
                    .testTag("create_post_top_share")
            )
        }
        HorizontalDivider(color = NoiseBorder.copy(alpha = 0.6f), thickness = 0.8.dp)

        Spacer(modifier = Modifier.height(14.dp))

        // Large Dashed Upload Box
        val isLight = ThemeManager.isLightMode
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(340.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(if (isLight) Color(0xFFF8F8FC) else Color(0xFF0C0C10))
                .clickable {
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawRoundRect(
                    color = if (isLight) Color(0xFFD6D6E4) else Color(0xFF282836),
                    cornerRadius = CornerRadius(22.dp.toPx(), 22.dp.toPx()),
                    style = dashStroke
                )
            }

            if (hasSelectedMedia) {
                Box(modifier = Modifier.fillMaxSize()) {
                    if (selectedMediaType == "video") {
                        NoiseVideoPlayer(
                            mediaKey = selectedMediaKey,
                            contentDescription = "Selected Video Preview",
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        NoiseMediaImage(
                            mediaKey = selectedMediaKey,
                            contentDescription = "Selected Media Preview",
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(16.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(gradientBrush)
                            .clickable {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                                )
                            }
                            .padding(horizontal = 20.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = "Change Photo or Video",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(if (isLight) LightLavender else Color(0xFF17171F))
                            .border(1.dp, if (isLight) NoisePurple.copy(alpha = 0.35f) else Color(0xFF282836), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileUpload,
                            contentDescription = "Upload",
                            tint = NoisePink,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Text(
                        text = "Select photo or video from device",
                        color = NoiseWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Drag and drop or tap to choose (MP4, MOV, JPG, PNG)",
                        color = NoiseTextSecondary,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(22.dp))

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(24.dp))
                            .background(gradientBrush)
                            .clickable {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                                )
                            }
                            .padding(horizontal = 24.dp, vertical = 12.dp)
                            .testTag("choose_photo_video_button")
                    ) {
                        Text(
                            text = "Choose Photo or Video",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Quick preset media strip + Photo/Video Clip toggle for instant testing
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (selectedMediaType == "photo") NoisePink.copy(alpha = 0.2f) else NoiseCard)
                    .border(1.dp, if (selectedMediaType == "photo") NoisePink else NoiseBorder, RoundedCornerShape(12.dp))
                    .clickable { selectedMediaType = "photo" }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text("Photo Post", color = NoiseWhite, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (selectedMediaType == "video") NoisePurple.copy(alpha = 0.25f) else NoiseCard)
                    .border(1.dp, if (selectedMediaType == "video") NoisePurple else NoiseBorder, RoundedCornerShape(12.dp))
                    .clickable { selectedMediaType = "video" }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text("Video Clip", color = NoiseWhite, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
            for ((key, label) in presetGallery) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (hasSelectedMedia && selectedMediaKey == key) NoiseCardElevated else NoiseCard)
                        .border(
                            1.dp,
                            if (hasSelectedMedia && selectedMediaKey == key) NoisePink else NoiseBorder,
                            RoundedCornerShape(12.dp)
                        )
                        .clickable {
                            selectedMediaKey = key
                            hasSelectedMedia = true
                        }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(label, color = NoiseTextSecondary, fontSize = 11.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 8 Emoji Vibe Buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            vibeEmojis.forEach { emoji ->
                val isSelected = selectedEmoji == emoji
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .background(if (isLight) Color(0xFFF8F8FC) else Color(0xFF14141B))
                        .border(
                            width = 1.dp,
                            color = if (isSelected) NoisePink else NoiseBorder,
                            shape = RoundedCornerShape(11.dp)
                        )
                        .clickable { selectedEmoji = emoji },
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = emoji, fontSize = 17.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Caption field
        OutlinedTextField(
            value = caption,
            onValueChange = { caption = it },
            placeholder = {
                Text(
                    text = "Write a caption... (e.g. Midnight city lights 🌃 #Noise)",
                    color = NoiseTextMuted,
                    fontSize = 14.sp
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(92.dp)
                .testTag("create_post_caption_input"),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = if (isLight) Color(0xFFFFFFFF) else Color(0xFF111116),
                unfocusedContainerColor = if (isLight) Color(0xFFFFFFFF) else Color(0xFF111116),
                focusedBorderColor = NoisePink,
                unfocusedBorderColor = NoiseBorder,
                focusedTextColor = NoiseNormalText,
                unfocusedTextColor = NoiseNormalText
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Location field
        OutlinedTextField(
            value = location,
            onValueChange = { location = it },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = "Location",
                    tint = NoiseTextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            },
            placeholder = {
                Text(
                    text = "Add location (e.g. Shibuya, Tokyo)",
                    color = NoiseTextMuted,
                    fontSize = 14.sp
                )
            },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .testTag("create_post_location_input"),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = if (isLight) Color(0xFFFFFFFF) else Color(0xFF111116),
                unfocusedContainerColor = if (isLight) Color(0xFFFFFFFF) else Color(0xFF111116),
                focusedBorderColor = NoisePink,
                unfocusedBorderColor = NoiseBorder,
                focusedTextColor = NoiseNormalText,
                unfocusedTextColor = NoiseNormalText
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Real-time Firebase Storage Upload Progress & Retry Banner
        if (uploadState.status in listOf("Pending", "Uploading", "Uploaded", "Failed")) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (isLight) LightLavender else Color(0xFF151320))
                    .border(
                        1.dp,
                        if (uploadState.status == "Failed") Color(0xFFFF4D6D) else NoisePink.copy(alpha = 0.6f),
                        RoundedCornerShape(14.dp)
                    )
                    .padding(12.dp)
                    .testTag("post_upload_status_card")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = when (uploadState.status) {
                            "Pending" -> "Preparing cloud upload..."
                            "Uploading" -> "Uploading to Firebase Storage (${uploadState.progressPercent}%)..."
                            "Uploaded" -> "Uploaded! Saving post to Firestore..."
                            "Failed" -> "Upload failed: ${uploadState.errorMessage ?: "Connection interrupted"}"
                            else -> uploadState.status
                        },
                        color = if (uploadState.status == "Failed") Color(0xFFFF4D6D) else NoiseWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        modifier = Modifier.weight(1f)
                    )
                    if (uploadState.status == "Failed") {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(NoisePink)
                                .clickable { onRetryUpload() }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                .testTag("retry_post_upload_button")
                        ) {
                            Text(
                                text = "Retry Upload",
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
                if (uploadState.status in listOf("Pending", "Uploading", "Uploaded")) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(if (isLight) Color(0xFFFFFFFF) else Color(0xFF282538))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth((uploadState.progressPercent.coerceIn(5, 100)) / 100f)
                                .height(6.dp)
                                .background(gradientBrush)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Full-width gradient Share Post button
        val isUploadingNow = uploadState.status in listOf("Pending", "Uploading", "Uploaded")
        Button(
            onClick = {
                if (!isUploadingNow) {
                    onSharePost(selectedMediaKey, selectedMediaType, caption, location, "#NoiseVibes", selectedEmoji)
                }
            },
            enabled = !isUploadingNow,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(52.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(gradientBrush)
                .testTag("share_post_submit_button"),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent
            )
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isUploadingNow) "Uploading ${uploadState.progressPercent}%..." else "Share Post",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun VideoClipsScreen(
    postsState: UiState<List<Post>>,
    currentUserId: String,
    followedUserIds: Set<String> = emptySet(),
    savedPostIds: Set<String> = emptySet(),
    sessionViewedReelIds: Set<String> = emptySet(),
    trendingAlgorithmMode: String = "engagement_velocity",
    isSuperOwner: Boolean = false,
    onCreatePostClick: () -> Unit,
    onLikePost: (Post) -> Unit,
    onCommentPost: (Post) -> Unit,
    onToggleSavePost: (Post) -> Unit = {},
    onShareReel: (Post) -> Unit = {},
    onRecordReelView: (Post) -> Unit = {},
    onToggleFollowCreator: (String) -> Unit = {},
    onInspectCreatorProfile: (String) -> Unit = {},
    onReportReel: (Post) -> Unit = {},
    onRefreshTrendingReels: () -> Unit = {},
    onDeletePost: (Post) -> Unit = {}
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val allPosts = (postsState as? UiState.Success)?.data ?: emptyList()
    val rawVideoClips = remember(allPosts) {
        allPosts.filter { it.mediaType == "video" && it.mediaKey.isNotBlank() }
    }
    var selectedReelsTab by remember { mutableStateOf("trending") }
    var useClassicFeedLayout by remember { mutableStateOf(false) }

    val nowMillis = remember(rawVideoClips, selectedReelsTab, trendingAlgorithmMode) {
        System.currentTimeMillis()
    }

    // Automatically rank and filter Reels using Trending Score + unviewed session priority
    val rankedVideoClips = remember(
        rawVideoClips,
        selectedReelsTab,
        trendingAlgorithmMode,
        sessionViewedReelIds,
        nowMillis
    ) {
        val tabFiltered = when (selectedReelsTab) {
            "kerala" -> {
                val regional = rawVideoClips.filter { it.isKeralaOrMalayalamReel() }
                regional.ifEmpty { rawVideoClips }
            }
            "music" -> {
                val musical = rawVideoClips.filter {
                    it.reelCategory.equals("Music", ignoreCase = true) ||
                        it.musicTrackTitle.isNotBlank() ||
                        it.tag.contains("music", ignoreCase = true) ||
                        it.caption.contains("music", ignoreCase = true) ||
                        it.caption.contains("song", ignoreCase = true) ||
                        it.caption.contains("dance", ignoreCase = true)
                }
                musical.ifEmpty { rawVideoClips }
            }
            else -> rawVideoClips
        }

        if (selectedReelsTab == "foryou") {
            // Prioritize reels not yet viewed in this session so reels don't repeat in a loop
            tabFiltered.sortedWith(
                compareByDescending<Post> { it.id !in sessionViewedReelIds }
                    .thenByDescending { it.computeReelTrendingScore(nowMillis, trendingAlgorithmMode) }
            )
        } else {
            tabFiltered.sortedByDescending {
                it.computeReelTrendingScore(nowMillis, trendingAlgorithmMode)
            }
        }
    }

    val gradientBrush = Brush.horizontalGradient(listOf(NoisePink, NoisePurple))
    val reelsTabs = listOf(
        "trending" to "🔥 Trending",
        "kerala" to "🌴 Kerala & മലയാളം",
        "music" to "🎵 Music & Dance",
        "foryou" to "✨ For You"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NoiseBlack)
            .testTag("trending_reels_screen")
    ) {
        // Top Reels Category & Controls Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0E0B18))
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            LazyRow(
                modifier = Modifier
                    .weight(1f)
                    .testTag("trending_reels_tabs_row"),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(reelsTabs, key = { it.first }) { (tabKey, tabLabel) ->
                    val active = selectedReelsTab == tabKey
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                if (active) gradientBrush
                                else Brush.horizontalGradient(listOf(Color(0xFF1B152B), Color(0xFF1B152B)))
                            )
                            .border(
                                1.dp,
                                if (active) VipGold else NoiseBorder,
                                RoundedCornerShape(16.dp)
                            )
                            .clickable { selectedReelsTab = tabKey }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                            .testTag("trending_reels_tab_$tabKey")
                    ) {
                        Text(
                            text = tabLabel,
                            color = Color.White,
                            fontWeight = if (active) FontWeight.ExtraBold else FontWeight.SemiBold,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Refresh Rankings button
                IconButton(
                    onClick = onRefreshTrendingReels,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1D172E))
                        .testTag("trending_reels_refresh_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh Trending Reels",
                        tint = VipGold,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Toggle Full-Screen Vertical Pager vs Feed List
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1D172E))
                        .border(1.dp, VipGold.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                        .clickable { useClassicFeedLayout = !useClassicFeedLayout }
                        .padding(horizontal = 8.dp, vertical = 5.dp)
                        .testTag("trending_reels_mode_toggle")
                ) {
                    Text(
                        text = if (useClassicFeedLayout) "Reels 9:16" else "Feed View",
                        color = VipGold,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 10.sp
                    )
                }
            }
        }

        if (rankedVideoClips.isEmpty()) {
            val isLight = ThemeManager.isLightMode
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(NoiseBlack)
                    .padding(horizontal = 32.dp)
                    .testTag("video_clips_empty_state"),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(84.dp)
                            .clip(CircleShape)
                            .background(if (isLight) LightLavender else Color(0xFF15151C))
                            .border(1.dp, if (isLight) NoisePurple.copy(alpha = 0.3f) else Color(0xFF262634), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Movie,
                            contentDescription = "No Video Clips",
                            tint = NoiseTextSecondary,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(22.dp))

                    Text(
                        text = "No Trending Reels Uploaded Yet",
                        color = NoiseWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Upload a real vertical video clip from your device to appear in NOICE LIVE Trending Reels!",
                        color = NoiseTextSecondary,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )

                    Spacer(modifier = Modifier.height(26.dp))

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(24.dp))
                            .background(gradientBrush)
                            .clickable { onCreatePostClick() }
                            .padding(horizontal = 28.dp, vertical = 13.dp)
                            .testTag("video_clips_create_post_button")
                    ) {
                        Text(
                            text = "Upload Trending Reel",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        } else if (useClassicFeedLayout) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(NoiseBlack),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(rankedVideoClips, key = { it.id }) { clip ->
                    PostFeedCard(
                        post = clip,
                        isLiked = currentUserId in clip.likedByUids,
                        isSaved = clip.id in savedPostIds || currentUserId in clip.savedByUids,
                        isOwnPost = clip.userId == currentUserId || isSuperOwner,
                        onLikeClick = { onLikePost(clip) },
                        onCommentClick = { onCommentPost(clip) },
                        onToggleSaveClick = { onToggleSavePost(clip) },
                        onDeletePostClick = { onDeletePost(clip) }
                    )
                }
            }
        } else {
            val pagerState = rememberPagerState(pageCount = { rankedVideoClips.size })
            val currentClip = rankedVideoClips.getOrNull(pagerState.currentPage)
            LaunchedEffect(currentClip?.id) {
                if (currentClip != null && currentClip.id.isNotBlank()) {
                    onRecordReelView(currentClip)
                }
            }

            VerticalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("trending_reels_vertical_pager")
            ) { pageIndex ->
                val clip = rankedVideoClips[pageIndex]
                val isLiked = currentUserId in clip.likedByUids
                val isSaved = clip.id in savedPostIds || currentUserId in clip.savedByUids
                val isFollowingCreator = clip.userId in followedUserIds
                val isOwnOrOwner = clip.userId == currentUserId || isSuperOwner
                val score = clip.computeReelTrendingScore(nowMillis, trendingAlgorithmMode)

                FullScreenTrendingReelPage(
                    clip = clip,
                    rankPosition = pageIndex + 1,
                    trendingScore = score,
                    isActivePage = pagerState.currentPage == pageIndex,
                    isLiked = isLiked,
                    isSaved = isSaved,
                    isFollowingCreator = isFollowingCreator,
                    showFollowButton = clip.userId.isNotBlank() && clip.userId != currentUserId,
                    canDelete = isOwnOrOwner,
                    onLikeClick = { onLikePost(clip) },
                    onCommentClick = { onCommentPost(clip) },
                    onShareClick = {
                        onShareReel(clip)
                        runCatching {
                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(
                                    Intent.EXTRA_TEXT,
                                    "Watch @${clip.username}'s Trending Reel on NOICE LIVE: ${clip.caption} ${clip.mediaKey}"
                                )
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Share Reel"))
                        }
                    },
                    onSaveClick = { onToggleSavePost(clip) },
                    onFollowCreatorClick = {
                        if (clip.userId.isNotBlank()) onToggleFollowCreator(clip.userId)
                    },
                    onCreatorClick = {
                        if (clip.userId.isNotBlank()) onInspectCreatorProfile(clip.userId)
                    },
                    onReportClick = {
                        onReportReel(clip)
                        Toast.makeText(context, "Reel reported for safety review.", Toast.LENGTH_SHORT).show()
                    },
                    onDeleteClick = { onDeletePost(clip) },
                    onUploadNewReelClick = onCreatePostClick
                )
            }
        }
    }
}

@Composable
private fun FullScreenTrendingReelPage(
    clip: Post,
    rankPosition: Int,
    trendingScore: Double,
    isActivePage: Boolean,
    isLiked: Boolean,
    isSaved: Boolean,
    isFollowingCreator: Boolean,
    showFollowButton: Boolean,
    canDelete: Boolean,
    onLikeClick: () -> Unit,
    onCommentClick: () -> Unit,
    onShareClick: () -> Unit,
    onSaveClick: () -> Unit,
    onFollowCreatorClick: () -> Unit,
    onCreatorClick: () -> Unit,
    onReportClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onUploadNewReelClick: () -> Unit
) {
    val hashtags = remember(clip.id, clip.hashtags, clip.caption, clip.tag) {
        clip.resolvedHashtags().ifEmpty {
            listOf("#NoiceLive", "#TrendingReels", "#KeralaReels")
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("trending_reel_page_${clip.id}")
    ) {
        // Full-Screen Vertical Video Player / Media Surface
        if (clip.mediaType == "video") {
            NoiseVideoPlayer(
                mediaKey = clip.effectiveMediaSource(),
                contentDescription = clip.caption.ifBlank { "Trending Reel by @${clip.username}" },
                autoPlay = isActivePage,
                thumbnailUrl = clip.thumbnailUrl,
                storagePath = clip.storagePath,
                cloudMediaId = clip.cloudMediaId,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            NoiseMediaImage(
                mediaKey = clip.effectiveMediaSource(),
                contentDescription = clip.caption.ifBlank { "Trending Reel by @${clip.username}" },
                thumbnailUrl = clip.thumbnailUrl,
                storagePath = clip.storagePath,
                cloudMediaId = clip.cloudMediaId,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Subtle top & bottom gradient scrims for crisp text readability
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp)
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Black.copy(alpha = 0.65f), Color.Transparent)
                    )
                )
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(270.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.88f))
                    )
                )
        )

        // Top-Left Trending Rank & Live Views Badge
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(Brush.horizontalGradient(listOf(NoisePink, NoisePurple)))
                    .border(1.dp, VipGold, RoundedCornerShape(14.dp))
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text(
                    text = "🔥 #$rankPosition TRENDING • ${trendingScore.toInt()} PTS",
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 11.sp
                )
            }

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.Black.copy(alpha = 0.6f))
                    .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(14.dp))
                    .padding(horizontal = 9.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Visibility,
                    contentDescription = "Views",
                    tint = VipGold,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${clip.viewsCount.coerceAtLeast(1L)}",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
            }

            if (clip.isKeralaOrMalayalamReel()) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(OnlineGreen.copy(alpha = 0.85f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "🌴 KERALA",
                        color = Color.Black,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 10.sp
                    )
                }
            }
        }

        // Top-Right Quick Upload Reel Pill
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(horizontal = 16.dp, vertical = 14.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color.Black.copy(alpha = 0.65f))
                .border(1.dp, VipGold, RoundedCornerShape(14.dp))
                .clickable { onUploadNewReelClick() }
                .padding(horizontal = 10.dp, vertical = 6.dp)
                .testTag("trending_reels_upload_button")
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Upload Reel",
                    tint = VipGold,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Reel",
                    color = VipGold,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 11.sp
                )
            }
        }

        // Right-Side Instagram-Style Vertical Action Column
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 14.dp, bottom = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Creator Avatar with + Follow badge
            Box(
                modifier = Modifier
                    .clickable { onCreatorClick() }
                    .testTag("reel_creator_avatar_${clip.id}"),
                contentAlignment = Alignment.BottomCenter
            ) {
                NoiseAvatar(
                    avatarKey = clip.authorAvatarKey.ifBlank { clip.username.take(1).uppercase() },
                    size = 48.dp,
                    showOnlineDot = true
                )
                if (showFollowButton && !isFollowingCreator) {
                    Box(
                        modifier = Modifier
                            .offset(y = 6.dp)
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(NoisePink)
                            .border(1.5.dp, Color.White, CircleShape)
                            .clickable { onFollowCreatorClick() }
                            .testTag("reel_follow_creator_${clip.id}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Follow Creator",
                            tint = Color.White,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }

            // Like Button
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clickable { onLikeClick() }
                    .testTag("reel_like_button_${clip.id}")
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Like Reel",
                        tint = if (isLiked) NoisePink else Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${clip.likesCount}",
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 12.sp
                )
            }

            // Comment Button
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clickable { onCommentClick() }
                    .testTag("reel_comment_button_${clip.id}")
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ChatBubbleOutline,
                        contentDescription = "Comment on Reel",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${clip.commentsCount}",
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 12.sp
                )
            }

            // Share Button
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clickable { onShareClick() }
                    .testTag("reel_share_button_${clip.id}")
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share Reel",
                        tint = Color.White,
                        modifier = Modifier.size(23.dp)
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${clip.sharesCount}",
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 12.sp
                )
            }

            // Save / Bookmark Button
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clickable { onSaveClick() }
                    .testTag("reel_save_button_${clip.id}")
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        contentDescription = "Save Reel",
                        tint = if (isSaved) VipGold else Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (isSaved) "Saved" else "${clip.savesCount}",
                    color = if (isSaved) VipGold else Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 11.sp
                )
            }

            // Report & Delete Moderation Controls
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                IconButton(
                    onClick = onReportClick,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.55f))
                        .testTag("reel_report_button_${clip.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Flag,
                        contentDescription = "Report Reel",
                        tint = NoiseTextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }

                if (canDelete) {
                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF7F1D1D).copy(alpha = 0.8f))
                            .testTag("reel_delete_button_${clip.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Delete Reel",
                            tint = Color.White,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }
            }
        }

        // Bottom-Left Creator Info, Follow Button, Caption, Hashtags & Music Bar
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth(0.80f)
                .padding(start = 16.dp, bottom = 28.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "@${clip.username}",
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp,
                    modifier = Modifier.clickable { onCreatorClick() }
                )

                if (clip.authorVipBadgeLevel > 0L) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(VipGold)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "VIP ${clip.authorVipBadgeLevel}",
                            color = Color.Black,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 9.sp
                        )
                    }
                }

                if (showFollowButton) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isFollowingCreator) Color.White.copy(alpha = 0.2f)
                                else NoisePink
                            )
                            .border(1.dp, Color.White.copy(alpha = 0.65f), RoundedCornerShape(12.dp))
                            .clickable { onFollowCreatorClick() }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (isFollowingCreator) "Following" else "Follow",
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            if (clip.caption.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = clip.caption,
                    color = Color.White.copy(alpha = 0.94f),
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Trending Hashtags Row
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                hashtags.take(5).forEach { tagText ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.White.copy(alpha = 0.14f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = tagText,
                            color = VipGold,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Music / Audio Attribution Pill
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.Black.copy(alpha = 0.55f))
                    .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(14.dp))
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = "Audio Track",
                    tint = NoisePink,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = clip.musicTrackTitle.ifBlank {
                        if (clip.isKeralaOrMalayalamReel()) "Malayalam Original Audio • @${clip.username}"
                        else "Original Audio • @${clip.username}"
                    },
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

