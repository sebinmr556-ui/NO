package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.credentials.CredentialManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.NoiseRepository
import com.example.data.NotificationItem
import com.example.data.UiState
import com.example.ui.ActiveChatroomsListScreen
import com.example.ui.ActiveOneOnOneCallScreen
import com.example.ui.AuthScreen
import com.example.ui.CommentsBottomModal
import com.example.ui.CreatePartyRoomDialog
import com.example.ui.CreatePostScreen
import com.example.ui.FullScreenMultiplayerGameModal
import com.example.ui.HomeFeedScreen
import com.example.ui.IncomingCallOverlayModal
import com.example.ui.IncomingGameInviteOverlayModal
import com.example.ui.MultiplayerGamesScreen
import com.example.ui.NoiseBottomNavBar
import com.example.ui.NoiseDirectScreen
import com.example.ui.NoiseNotificationDispatcher
import com.example.ui.NoiseTab
import com.example.ui.NoiseTopHeader
import com.example.ui.NoiseViewModel
import com.example.ui.NotificationsModal
import com.example.ui.OtherUserProfileModal
import com.example.ui.PartyChatRoomScreen
import com.example.ui.RealTimeNotificationBannerOverlay
import com.example.ui.SearchAndAccountsScreen
import com.example.ui.StoryViewerModal
import com.example.ui.UserProfileScreen
import com.example.ui.VideoClipsScreen
import com.example.ui.VipUnlimitedVaultModal
import com.example.ui.signOut
import com.example.ui.theme.LightLavender
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.NoiseBlack
import com.example.ui.theme.NoisePink
import com.example.ui.theme.NoiseThemeController
import com.example.ui.theme.NoiseWhite
import com.example.ui.theme.ThemeManager
import com.example.ui.theme.ThemeViewModel
import android.content.Context
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.recaptcha.RecaptchaAppCheckProviderFactory
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow

private const val APP_CHECK_TAG = "AppCheckInit"
private const val DEBUG_TOKEN_EXTRA = "FIREBASE_APPCHECK_DEBUG_TOKEN"
private const val DEBUG_STORE_PREFIX = "com.google.firebase.appcheck.debug.store."
private const val DEBUG_SECRET_KEY = "com.google.firebase.appcheck.debug.DEBUG_SECRET"
private const val RECAPTCHA_SITE_KEY_RES = "recaptcha_site_key"

fun configureAppCheck(context: Context, intent: Intent?) {
    try {
        if (FirebaseApp.getApps(context).isEmpty()) {
            FirebaseApp.initializeApp(context)
        }

        if (BuildConfig.DEBUG) {
            val debugToken = intent?.getStringExtra(DEBUG_TOKEN_EXTRA)
            if (!debugToken.isNullOrEmpty()) {
                val persistenceKey = FirebaseApp.getInstance().persistenceKey
                val prefs = context.getSharedPreferences(
                    "$DEBUG_STORE_PREFIX$persistenceKey",
                    Context.MODE_PRIVATE
                )
                prefs.edit()
                    .putString(DEBUG_SECRET_KEY, debugToken)
                    .commit()
                Log.d(APP_CHECK_TAG, "App Check debug secret configured successfully.")
            } else {
                Log.w(APP_CHECK_TAG, "No debug token found in Intent extra.")
            }

            FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
                DebugAppCheckProviderFactory.getInstance()
            )
            Log.d(APP_CHECK_TAG, "DebugAppCheckProviderFactory installed.")
        } else {
            val resId = context.resources.getIdentifier(RECAPTCHA_SITE_KEY_RES, "string", context.packageName)
            if (resId != 0) {
                val siteKey = context.getString(resId)
                if (siteKey.isNotBlank()) {
                    FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
                        RecaptchaAppCheckProviderFactory.getInstance(siteKey)
                    )
                    Log.d(APP_CHECK_TAG, "RecaptchaAppCheckProviderFactory installed.")
                } else {
                    Log.e(APP_CHECK_TAG, "recaptcha_site_key resource string is blank.")
                }
            } else {
                Log.e(APP_CHECK_TAG, "recaptcha_site_key string resource not found in package.")
            }
        }
    } catch (e: Exception) {
        Log.e(APP_CHECK_TAG, "Failed to initialize Firebase App Check", e)
    }
}

class MainActivity : ComponentActivity() {
    private val latestDeepLinkIntent = MutableStateFlow<Intent?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureAppCheck(this, intent)
        com.example.data.CentralLiveUpdateRuntime.initOfflineCache(this)
        latestDeepLinkIntent.value = intent
        enableEdgeToEdge()
        setContent {
            val ctx = LocalContext.current
            val themeViewModel: ThemeViewModel = viewModel()
            val isLightMode by themeViewModel.isLightMode.collectAsStateWithLifecycle()
            val deepLinkIntent by latestDeepLinkIntent.collectAsStateWithLifecycle()
            LaunchedEffect(Unit) {
                themeViewModel.initTheme(ctx)
                NoiseNotificationDispatcher.ensureNotificationChannels(ctx)
            }
            MyApplicationTheme(
                darkTheme = !isLightMode,
                themeViewModel = themeViewModel
            ) {
                AppNavigation(
                    deepLinkIntent = deepLinkIntent,
                    onConsumeDeepLink = { latestDeepLinkIntent.value = null }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        latestDeepLinkIntent.value = intent
    }
}

internal fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { auth ->
        trySend(auth.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

@Composable
fun AppNavigation(
    auth: FirebaseAuth = Firebase.auth,
    deepLinkIntent: Intent? = null,
    onConsumeDeepLink: () -> Unit = {}
) {
    val context = LocalContext.current
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    var hasCompletedFirstInstallLogin by remember {
        mutableStateOf(com.example.ui.AuthSessionPrefs.hasCompletedFirstInstallLogin(context))
    }
    val user = currentUser
    if (user == null || !hasCompletedFirstInstallLogin) {
        AuthScreen(
            onAuthSuccess = {
                com.example.ui.AuthSessionPrefs.markFirstInstallLoginCompleted(context)
                hasCompletedFirstInstallLogin = true
            }
        )
    } else {
        NoiseMainApp(
            currentUserId = user.uid,
            deepLinkIntent = deepLinkIntent,
            onConsumeDeepLink = onConsumeDeepLink
        )
    }
}

@Composable
fun NoiseMainApp(
    currentUserId: String,
    deepLinkIntent: Intent? = null,
    onConsumeDeepLink: () -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    val viewModel: NoiseViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val db = NoiseRepository.resolveFirestoreInstance(app)
                com.example.data.CloudMediaStorageManager.initialize(app, db)
                NoiseViewModel(NoiseRepository(db), currentUserId)
            }
        }
    )

    // Track real-time user online/offline lifecycle status
    DisposableEffect(lifecycleOwner, currentUserId) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> viewModel.setUserOnlineStatus(true)
                Lifecycle.Event.ON_STOP -> viewModel.setUserOnlineStatus(false)
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val showNotificationsModal by viewModel.showNotificationsModal.collectAsStateWithLifecycle()
    val showVipVaultModal by viewModel.showVipVaultModal.collectAsStateWithLifecycle()
    val showDirectMessages by viewModel.showDirectMessages.collectAsStateWithLifecycle()
    val showCreatePartyRoomPopup by viewModel.showCreatePartyRoomPopup.collectAsStateWithLifecycle()
    val showChatroomsListScreen by viewModel.showChatroomsListScreen.collectAsStateWithLifecycle()
    val activeStoryViewerIndex by viewModel.activeStoryViewerIndex.collectAsStateWithLifecycle()
    val activeConversation by viewModel.activeConversation.collectAsStateWithLifecycle()
    val activeCommentPost by viewModel.activeCommentPost.collectAsStateWithLifecycle()
    val activePartyRoom by viewModel.activePartyRoom.collectAsStateWithLifecycle()
    val activeGameRoom by viewModel.activeGameRoom.collectAsStateWithLifecycle()
    val activePartyRoomGameRooms by viewModel.activePartyRoomGameRooms.collectAsStateWithLifecycle()
    val gameRoomsState by viewModel.gameRoomsState.collectAsStateWithLifecycle()
    val gameInvitesState by viewModel.gameInvitesState.collectAsStateWithLifecycle()
    val outgoingGameInvites by viewModel.outgoingGameInvitesState.collectAsStateWithLifecycle()
    val gameLeaderboardState by viewModel.gameLeaderboardState.collectAsStateWithLifecycle()
    val gameHistoryState by viewModel.gameHistoryState.collectAsStateWithLifecycle()
    val graphicsQuality by viewModel.graphicsQuality.collectAsStateWithLifecycle()
    val followersState by viewModel.followersState.collectAsStateWithLifecycle()
    val followingState by viewModel.followingState.collectAsStateWithLifecycle()
    val activeCalls by viewModel.activeCallsState.collectAsStateWithLifecycle()
    val inspectedUserProfile by viewModel.inspectedUserProfile.collectAsStateWithLifecycle()
    val savedPostIds by viewModel.savedPostIds.collectAsStateWithLifecycle()
    val statusBanner by viewModel.statusBanner.collectAsStateWithLifecycle()
    val mediaUploadState by viewModel.mediaUploadState.collectAsStateWithLifecycle()

    val userProfileState by viewModel.userProfileState.collectAsStateWithLifecycle()
    val registeredAccountsState by viewModel.registeredAccountsState.collectAsStateWithLifecycle()
    val postsState by viewModel.postsState.collectAsStateWithLifecycle()
    val storiesState by viewModel.storiesState.collectAsStateWithLifecycle()
    val partyRoomsState by viewModel.partyRoomsState.collectAsStateWithLifecycle()
    val notificationsState by viewModel.notificationsState.collectAsStateWithLifecycle()
    val conversationsState by viewModel.conversationsState.collectAsStateWithLifecycle()
    val activeMessagesState by viewModel.activeMessagesState.collectAsStateWithLifecycle()
    val activeCommentsState by viewModel.activeCommentsState.collectAsStateWithLifecycle()
    val coinTransactionsState by viewModel.coinTransactionsState.collectAsStateWithLifecycle()
    val selectedDiscoveryCategory by viewModel.selectedDiscoveryCategory.collectAsStateWithLifecycle()
    val isDiscoverySyncing by viewModel.isDiscoverySyncing.collectAsStateWithLifecycle()
    val showDiscoveryAdminPanel by viewModel.showDiscoveryAdminPanel.collectAsStateWithLifecycle()
    val activeViewingDiscoveryItem by viewModel.activeViewingDiscoveryItem.collectAsStateWithLifecycle()
    val discoveryItemsState by viewModel.discoveryItemsState.collectAsStateWithLifecycle()
    val allDiscoveryItemsForAdminState by viewModel.allDiscoveryItemsForAdminState.collectAsStateWithLifecycle()
    val discoveryGlobalConfigState by viewModel.discoveryGlobalConfigState.collectAsStateWithLifecycle()
    val discoverySourcesState by viewModel.discoverySourcesState.collectAsStateWithLifecycle()
    val discoveryImportLogsState by viewModel.discoveryImportLogsState.collectAsStateWithLifecycle()
    val showOwnerControlPanel by viewModel.showOwnerControlPanel.collectAsStateWithLifecycle()
    val ownerControlInitialTab by viewModel.ownerControlInitialTab.collectAsStateWithLifecycle()
    val superOwnerRegistryState by viewModel.superOwnerRegistryState.collectAsStateWithLifecycle()
    val allChatroomsForOwnerState by viewModel.allChatroomsForOwnerState.collectAsStateWithLifecycle()
    val ownerAuditLogsState by viewModel.ownerAuditLogsState.collectAsStateWithLifecycle()
    val contentReportsState by viewModel.contentReportsState.collectAsStateWithLifecycle()
    val isCreatingPartyRoom by viewModel.isCreatingPartyRoom.collectAsStateWithLifecycle()
    val createPartyRoomError by viewModel.createPartyRoomError.collectAsStateWithLifecycle()
    val selectedKeralaCategory by viewModel.selectedKeralaCategory.collectAsStateWithLifecycle()
    val selectedKeralaDistrict by viewModel.selectedKeralaDistrict.collectAsStateWithLifecycle()
    val selectedKeralaLanguage by viewModel.selectedKeralaLanguage.collectAsStateWithLifecycle()
    val isKeralaNewsSyncing by viewModel.isKeralaNewsSyncing.collectAsStateWithLifecycle()
    val showKeralaNewsSection by viewModel.showKeralaNewsSection.collectAsStateWithLifecycle()
    val sessionViewedReelIds by viewModel.sessionViewedReelIds.collectAsStateWithLifecycle()
    val showCentralAiAdminModal by viewModel.showCentralAiAdminModal.collectAsStateWithLifecycle()
    val activeDynamicPage by viewModel.activeDynamicPage.collectAsStateWithLifecycle()
    val currentAiProposal by viewModel.currentAiProposal.collectAsStateWithLifecycle()
    val isGeneratingAiProposal by viewModel.isGeneratingAiProposal.collectAsStateWithLifecycle()
    val isPublishingLiveUpdate by viewModel.isPublishingLiveUpdate.collectAsStateWithLifecycle()
    val aiAdminStatusMessage by viewModel.aiAdminStatusMessage.collectAsStateWithLifecycle()
    val productionAppConfig by viewModel.productionAppConfigState.collectAsStateWithLifecycle()
    val dynamicPages by viewModel.dynamicPagesState.collectAsStateWithLifecycle()
    val centralVersionHistory by viewModel.centralVersionHistoryState.collectAsStateWithLifecycle()
    val centralBackups by viewModel.centralBackupsState.collectAsStateWithLifecycle()
    val aiCommandLogs by viewModel.aiCommandLogsState.collectAsStateWithLifecycle()
    val showGeminiDeveloperStudioModal by viewModel.showGeminiDeveloperStudioModal.collectAsStateWithLifecycle()
    val currentDeveloperCodeProposal by viewModel.currentDeveloperCodeProposal.collectAsStateWithLifecycle()
    val isDeveloperStudioBusy by viewModel.isDeveloperStudioBusy.collectAsStateWithLifecycle()
    val developerStudioStatusMessage by viewModel.developerStudioStatusMessage.collectAsStateWithLifecycle()
    val developerBuildTestReport by viewModel.developerBuildTestReport.collectAsStateWithLifecycle()
    val developerWorkspaceFiles by viewModel.developerWorkspaceFilesState.collectAsStateWithLifecycle()
    val developerChatMessages by viewModel.developerChatMessagesState.collectAsStateWithLifecycle()
    val developerBugFindings by viewModel.developerBugFindingsState.collectAsStateWithLifecycle()
    val githubConfig by viewModel.githubConfigState.collectAsStateWithLifecycle()
    val githubBranches by viewModel.githubBranchesState.collectAsStateWithLifecycle()
    val githubCommits by viewModel.githubCommitsState.collectAsStateWithLifecycle()
    val developerDeployments by viewModel.developerDeploymentsState.collectAsStateWithLifecycle()

    // Request Android 13+ POST_NOTIFICATIONS runtime permission so system notifications can be displayed
    val notifPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.registerDeviceNotificationToken()
        }
    }
    LaunchedEffect(currentUserId) {
        NoiseNotificationDispatcher.ensureNotificationChannels(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPerm = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!hasPerm) {
                runCatching { notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }
            }
        }
    }

    // Handle deep-link intents from tapping Android system notifications (Messages, Game Invites, Calls)
    LaunchedEffect(deepLinkIntent, currentUserId) {
        val intent = deepLinkIntent ?: return@LaunchedEffect
        val convId = intent.getStringExtra(NoiseNotificationDispatcher.EXTRA_CONVERSATION_ID).orEmpty()
        val gameRoomId = intent.getStringExtra(NoiseNotificationDispatcher.EXTRA_GAME_ROOM_ID).orEmpty()
        val notifId = intent.getStringExtra(NoiseNotificationDispatcher.EXTRA_NOTIFICATION_ID).orEmpty()
        if (notifId.isNotBlank()) {
            viewModel.markSingleNotificationRead(notifId)
        }
        if (convId.isNotBlank()) {
            viewModel.openConversationById(convId)
            onConsumeDeepLink()
        } else if (gameRoomId.isNotBlank()) {
            viewModel.joinGameRoomByCodeOrId(gameRoomId)
            onConsumeDeepLink()
        }
    }

    val userProfile = (userProfileState as? UiState.Success)?.data
    val isSuperOwner = remember(userProfile, superOwnerRegistryState, currentUserId) {
        com.example.data.SuperOwnerControlManager.isCurrentUserSuperOwner(
            userProfile = userProfile ?: com.example.data.UserProfile(id = currentUserId, userId = currentUserId),
            registryDoc = superOwnerRegistryState
        )
    }
    LaunchedEffect(userProfile?.themeMode) {
        val cloudTheme = userProfile?.themeMode
        if (!cloudTheme.isNullOrBlank()) {
            ThemeManager.applyThemeMode(cloudTheme, context)
        }
    }
    val registeredAccounts = (registeredAccountsState as? UiState.Success)?.data ?: emptyList()
    val allPosts = (postsState as? UiState.Success)?.data ?: emptyList()
    val rawStories = (storiesState as? UiState.Success)?.data ?: emptyList()
    val stories = remember(rawStories) {
        rawStories
            .filter { it.mediaKey.isNotBlank() }
            .distinctBy { "${it.userId}_${it.mediaKey}" }
    }
    val notifications = (notificationsState as? UiState.Success)?.data ?: emptyList()
    val unreadNotificationsCount = notifications.count { !it.read }

    // Real-time in-app notification banner + Android system notification dispatcher
    var activeBannerNotification by remember { mutableStateOf<NotificationItem?>(null) }
    val seenBannerIds = remember(currentUserId) { mutableSetOf<String>() }
    val baselineLoaded = remember(currentUserId) { mutableStateOf(false) }

    LaunchedEffect(notifications, currentUserId, activeConversation?.id, showDirectMessages) {
        if (!baselineLoaded.value && notifications.isNotEmpty()) {
            // Seed existing older notifications (> 15 seconds old) so only fresh incoming notifications pop a banner
            val cutoff = System.currentTimeMillis() - 15_000L
            notifications.filter { it.createdAtMillis in 1..cutoff }.forEach { seenBannerIds.add(it.id) }
            baselineLoaded.value = true
        }
        val newestUnread = notifications
            .filter { item ->
                !item.read &&
                    item.userId == currentUserId &&
                    item.actorUid.isNotBlank() &&
                    item.actorUid != currentUserId &&
                    item.id !in seenBannerIds
            }
            .maxByOrNull { it.createdAtMillis }

        if (newestUnread != null) {
            seenBannerIds.add(newestUnread.id)
            val alreadyViewingThisChat = showDirectMessages &&
                activeConversation != null &&
                newestUnread.conversationId.isNotBlank() &&
                activeConversation?.id == newestUnread.conversationId
            if (!alreadyViewingThisChat) {
                activeBannerNotification = newestUnread
                NoiseNotificationDispatcher.dispatchNotificationItem(
                    context = context,
                    currentUserId = currentUserId,
                    item = newestUnread
                )
            } else {
                viewModel.markSingleNotificationRead(newestUnread.id)
            }
        }
    }

    // Dispatch system notification for incoming game invites as well
    LaunchedEffect(gameInvitesState, currentUserId) {
        val nowMs = System.currentTimeMillis()
        gameInvitesState
            .filter { it.status == "pending" && it.receiverUid == currentUserId && it.senderUid != currentUserId && (it.expiresAtMillis == 0L || it.expiresAtMillis > nowMs) }
            .forEach { invite ->
                NoiseNotificationDispatcher.dispatchGameInviteSystemNotification(
                    context = context,
                    currentUserId = currentUserId,
                    invite = invite
                )
            }
    }

    var inGameVoiceMuted by remember { mutableStateOf(false) }
    var inGameSpeakerOutput by remember { mutableStateOf(true) }

    BackHandler(enabled = selectedTab != NoiseTab.HOME && !showDirectMessages && !showNotificationsModal && activePartyRoom == null && activeGameRoom == null && activeStoryViewerIndex == null) {
        viewModel.selectTab(NoiseTab.HOME)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NoiseBlack)
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        val currentRoom = activePartyRoom
        if (currentRoom != null) {
            PartyChatRoomScreen(
                room = currentRoom,
                currentUserId = currentUserId,
                currentUsername = userProfile?.username ?: "sebin_mr",
                currentUserAvatarKey = userProfile?.avatarKey ?: "S",
                userCoins = userProfile?.coins ?: 40150L,
                onUpdateRoom = { viewModel.updatePartyRoom(it) },
                onJoinOrMoveSeat = { seatNum, micMuted ->
                    viewModel.joinOrMoveRoomSeat(currentRoom.id, seatNum, micMuted)
                },
                onLeaveSeat = { viewModel.leaveRoomSeat(currentRoom.id) },
                onSetMicMuted = { muted ->
                    inGameVoiceMuted = muted
                    viewModel.setRoomMicMuted(currentRoom.id, muted)
                },
                onToggleSeatLock = { seatNum -> viewModel.toggleRoomSeatLock(currentRoom.id, seatNum) },
                onSendRoomChat = { text -> viewModel.sendRoomChatMessage(currentRoom.id, text) },
                onClearRoomChat = { viewModel.clearRoomChat(currentRoom.id) },
                onAssignRole = { targetUid, role ->
                    viewModel.assignRoomRole(currentRoom.id, targetUid, role)
                },
                onTransferOwnership = { newOwnerUid, newOwnerName ->
                    viewModel.transferRoomOwnership(currentRoom.id, newOwnerUid, newOwnerName)
                },
                onToggleMuteParticipant = { targetUid ->
                    viewModel.toggleMuteParticipant(currentRoom.id, targetUid)
                },
                onRemoveParticipantFromSlot = { seatNum, targetUid ->
                    viewModel.removeParticipantFromSlot(currentRoom.id, seatNum, targetUid)
                },
                onKickParticipant = { targetUid ->
                    viewModel.kickParticipantFromRoom(currentRoom.id, targetUid)
                },
                onCloseAndDeleteRoom = {
                    viewModel.closeAndDeletePartyRoom(currentRoom.id)
                },
                onSendGift = { amount, targetSeat, _ ->
                    viewModel.sendGiftToRoom(currentRoom, amount, targetSeat)
                },
                onOpenVipVault = { viewModel.setVipVaultModalVisible(true) },
                onClose = { viewModel.openPartyRoom(null) },
                gameCatalog = viewModel.gameCatalog,
                activeChatroomGames = activePartyRoomGameRooms,
                roomParticipants = registeredAccounts,
                onStartChatroomGame = { gameItem, mode ->
                    viewModel.createAndOpenGameRoom(
                        gameItem = gameItem,
                        roomTitle = "${currentRoom.title} • ${gameItem.title}",
                        mode = mode,
                        maxPlayers = gameItem.maxPlayers,
                        privateRoom = false,
                        chatRoomId = currentRoom.id,
                        chatRoomTitle = currentRoom.title
                    )
                },
                onJoinChatroomGame = { gameRoom ->
                    viewModel.joinGameRoomByCodeOrId(gameRoom.id)
                },
                onEndChatroomGame = { gameRoom ->
                    viewModel.endSpecificGameRoom(gameRoom)
                },
                onInviteUserToChatroomGame = { gameRoom, targetUser ->
                    viewModel.sendGameInviteToUser(gameRoom, targetUser)
                },
                onSyncYouTubeState = { videoUrlOrId, videoTitle, isPlaying, positionSec ->
                    viewModel.syncPartyRoomYouTubeState(
                        roomId = currentRoom.id,
                        videoUrlOrId = videoUrlOrId,
                        videoTitle = videoTitle,
                        isPlaying = isPlaying,
                        positionSec = positionSec
                    )
                }
            )
        } else if (showChatroomsListScreen) {
            ActiveChatroomsListScreen(
                partyRoomsState = partyRoomsState,
                onBack = { viewModel.setChatroomsListScreenVisible(false) },
                onCreateChatroomClick = { viewModel.setCreatePartyRoomPopupVisible(true) },
                onRefreshClick = { viewModel.refreshActiveChatrooms() },
                onOpenPartyRoom = { room ->
                    viewModel.setChatroomsListScreenVisible(false)
                    viewModel.openPartyRoom(room)
                },
                isSuperOwner = isSuperOwner,
                onOpenOwnerChatroomPanel = { viewModel.openOwnerControlPanel(0) },
                onOwnerDeleteRoom = { room -> viewModel.ownerDeleteSingleChatroom(room.id) }
            )
        } else if (showDirectMessages) {
            NoiseDirectScreen(
                username = userProfile?.username ?: "sebin_mr",
                conversationsState = conversationsState,
                activeConversation = activeConversation,
                activeMessagesState = activeMessagesState,
                currentUserId = currentUserId,
                registeredAccounts = registeredAccounts,
                onStartNewChatWithUser = { targetUser ->
                    viewModel.startChatWithUser(targetUser)
                },
                onSelectConversation = { viewModel.openConversation(it) },
                onSendMessage = { viewModel.sendChatMessage(it) },
                onSendRichMessage = { text, msgType, mediaKey, mediaType, dur, wave, reply, voiceBase64, voiceMime ->
                    viewModel.sendRichChatMessage(
                        text = text,
                        messageType = msgType,
                        mediaKey = mediaKey,
                        mediaType = mediaType,
                        voiceDurationSec = dur,
                        voiceWaveform = wave,
                        replyToMessage = reply,
                        voiceAudioBase64 = voiceBase64,
                        voiceMimeType = voiceMime
                    )
                },
                onFetchVoiceRecording = { msgId ->
                    viewModel.fetchVoiceRecording(msgId).getOrNull()
                },
                onTypingChanged = { viewModel.setConversationTyping(it) },
                onTogglePeerOnline = { viewModel.toggleActiveConversationPeerOnline() },
                onReactMessage = { msg, emoji -> viewModel.reactToMessage(msg, emoji) },
                onTogglePinMessage = { msg -> viewModel.togglePinMessage(msg) },
                onDeleteMessage = { msg -> viewModel.deleteDirectMessage(msg) },
                onMarkVoiceSaved = { msg -> viewModel.markVoiceMessageSaved(msg) },
                onMarkMessagesSeen = { msgs ->
                    activeConversation?.let { conv ->
                        viewModel.acknowledgeMessagesDelivered(conv.id, msgs)
                    }
                    viewModel.markConversationMessagesSeen(msgs)
                },
                onStartCall = { conv, callType ->
                    viewModel.startOneOnOneCall(conv, callType)
                },
                onSelectChatTheme = { conv, themeKey ->
                    viewModel.updateConversationChatTheme(conv.id, themeKey)
                },
                onClose = { viewModel.setDirectMessagesVisible(false) },
                onRetryMessage = { msg -> viewModel.retryDirectMessage(msg) }
            )
        } else {
            Scaffold(
                containerColor = NoiseBlack,
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                topBar = {
                    NoiseTopHeader(
                        coins = userProfile?.coins ?: 40150L,
                        unreadNotificationsCount = unreadNotificationsCount,
                        onVipVaultClick = { viewModel.setVipVaultModalVisible(true) },
                        onGamesClick = { viewModel.selectTab(NoiseTab.GAMES) },
                        onLivePulseClick = { viewModel.setChatroomsListScreenVisible(true) },
                        onNotificationsClick = { viewModel.setNotificationsModalVisible(true) },
                        onDirectMessagesClick = { viewModel.setDirectMessagesVisible(true) },
                        onSignOutClick = {
                            viewModel.setUserOnlineStatus(false)
                            signOut(
                                context = context,
                                credentialManager = credentialManager,
                                onSignOutComplete = {},
                                scope = scope
                            )
                        }
                    )
                },
                bottomBar = {
                    NoiseBottomNavBar(
                        selectedTab = selectedTab,
                        userAvatarKey = userProfile?.avatarKey ?: "S",
                        onSelectTab = { viewModel.selectTab(it) }
                    )
                }
            ) { innerPadding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    if (statusBanner != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (ThemeManager.isLightMode) LightLavender else NoisePink.copy(alpha = 0.22f))
                                .clickable { viewModel.clearStatusBanner() }
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = statusBanner ?: "",
                                color = NoiseWhite,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp
                            )
                        }
                    }

                    when (selectedTab) {
                        NoiseTab.HOME -> {
                            val isDiscoveryAdmin = com.example.data.DiscoveryContentManager.isUserDiscoveryAdmin(
                                userProfile = userProfile,
                                globalConfig = discoveryGlobalConfigState
                            )
                            HomeFeedScreen(
                                currentUserId = currentUserId,
                                userProfile = userProfile,
                                storiesState = storiesState,
                                partyRoomsState = partyRoomsState,
                                postsState = postsState,
                                savedPostIds = savedPostIds,
                                gameCatalog = viewModel.gameCatalog,
                                selectedDiscoveryCategory = selectedDiscoveryCategory,
                                discoveryItems = discoveryItemsState,
                                disabledDiscoveryCategories = discoveryGlobalConfigState.disabledCategories,
                                isDiscoverySyncing = isDiscoverySyncing,
                                isDiscoveryAdmin = isDiscoveryAdmin,
                                discoveryRefreshIntervalMinutes = discoveryGlobalConfigState.refreshIntervalMinutes,
                                onSelectDiscoveryCategory = { cat -> viewModel.selectDiscoveryCategory(cat) },
                                onRefreshDiscoveryCategory = { viewModel.refreshDiscoveryCategory(forceRefresh = true) },
                                onOpenDiscoveryAdminPanel = {
                                    if (isDiscoveryAdmin) {
                                        viewModel.setDiscoveryAdminPanelVisible(true)
                                    }
                                },
                                onLikeDiscoveryItem = { item -> viewModel.toggleLikeDiscoveryItem(item) },
                                onSaveDiscoveryItem = { item -> viewModel.toggleSaveDiscoveryItem(item) },
                                onShareDiscoveryItem = { item ->
                                    viewModel.recordDiscoveryItemShare(item)
                                    runCatching {
                                        val shareText = "${item.title}\nSource: ${item.sourceName}\nRead more on NOISE LIVE: ${item.originalUrl}"
                                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_SUBJECT, item.title)
                                            putExtra(Intent.EXTRA_TEXT, shareText)
                                        }
                                        context.startActivity(Intent.createChooser(sendIntent, "Share Discovery Update"))
                                    }
                                },
                                onOpenDiscoveryItem = { item ->
                                    viewModel.setActiveViewingDiscoveryItem(item)
                                },
                                onAdminHideDiscoveryItem = { item ->
                                    viewModel.moderateDiscoveryItem(item.id, "hidden", "Hidden by Admin from Feed")
                                },
                                onAdminDeleteDiscoveryItem = { item ->
                                    viewModel.deleteDiscoveryItem(item.id)
                                },
                                selectedKeralaCategory = selectedKeralaCategory,
                                selectedKeralaDistrict = selectedKeralaDistrict,
                                selectedKeralaLanguage = selectedKeralaLanguage,
                                disabledKeralaCategories = discoveryGlobalConfigState.disabledKeralaCategories,
                                isKeralaNewsSyncing = isKeralaNewsSyncing,
                                showKeralaNewsSection = showKeralaNewsSection,
                                onToggleKeralaNewsSection = { visible -> viewModel.setShowKeralaNewsSection(visible) },
                                onSelectKeralaCategory = { cat -> viewModel.selectKeralaCategory(cat) },
                                onSelectKeralaDistrict = { dist -> viewModel.selectKeralaDistrict(dist) },
                                onSelectKeralaLanguage = { lang -> viewModel.selectKeralaLanguage(lang) },
                                onRefreshKeralaNews = { viewModel.refreshKeralaNews(forceRefresh = true) },
                                onAddStoryClick = { viewModel.addStory("roses", "Live from Noise ✨") },
                                onAddMultipleStoriesFromGallery = { items ->
                                    viewModel.addMultipleStories(items)
                                },
                                onOpenStoryAtIndex = { idx ->
                                    viewModel.openStoryViewer(idx)
                                },
                                onDeleteStory = { story ->
                                    viewModel.deleteStory(story)
                                },
                                onGoLiveClick = { viewModel.setCreatePartyRoomPopupVisible(true) },
                                onOpenGamesPage = { viewModel.selectTab(NoiseTab.GAMES) },
                                onPlayGameNow = { gameItem ->
                                    viewModel.createAndOpenGameRoom(
                                        gameItem = gameItem,
                                        roomTitle = gameItem.title,
                                        mode = "1v1",
                                        maxPlayers = gameItem.maxPlayers,
                                        privateRoom = false
                                    )
                                },
                                onOpenPartyRoom = { room -> viewModel.openPartyRoom(room) },
                                onLikePost = { viewModel.toggleLike(it) },
                                onCommentPost = { viewModel.openCommentsForPost(it) },
                                onToggleSavePost = { viewModel.toggleSavePost(it) },
                                onDeletePost = { post -> viewModel.deletePost(post.id) },
                                onInspectUserProfile = { authorUid -> viewModel.inspectUserById(authorUid) },
                                isSuperOwner = isSuperOwner,
                                onOpenOwnerChatroomPanel = { viewModel.openOwnerControlPanel(0) },
                                onOwnerDeleteRoom = { room -> viewModel.ownerDeleteSingleChatroom(room.id) },
                                centralAppConfig = com.example.data.CentralLiveUpdateRuntime.effectiveConfig,
                                dynamicPages = com.example.data.CentralLiveUpdateRuntime.effectiveDynamicPages,
                                onOpenDynamicPage = { page -> viewModel.openDynamicPage(page) },
                                onOpenCentralAiAdmin = { viewModel.setCentralAiAdminModalVisible(true) }
                            )
                        }
                        NoiseTab.SEARCH -> {
                            SearchAndAccountsScreen(
                                registeredAccountsState = registeredAccountsState,
                                postsState = postsState,
                                followedUserIds = userProfile?.followedUserIds ?: emptyList(),
                                onInspectUser = { viewModel.openInspectedUserProfile(it) },
                                onMessageUser = { viewModel.startChatWithUser(it) },
                                onFollowUser = { viewModel.followUser(it) }
                            )
                        }
                        NoiseTab.CREATE -> {
                            CreatePostScreen(
                                onCancel = { viewModel.selectTab(NoiseTab.HOME) },
                                onSharePost = { mediaKey, mediaType, caption, location, tag, vibeEmoji ->
                                    viewModel.shareNewPost(mediaKey, mediaType, caption, location, tag, vibeEmoji)
                                },
                                uploadState = mediaUploadState,
                                onRetryUpload = { viewModel.retryFailedUpload() }
                            )
                        }
                        NoiseTab.CLIPS -> {
                            VideoClipsScreen(
                                postsState = postsState,
                                currentUserId = currentUserId,
                                followedUserIds = userProfile?.followedUserIds?.toSet() ?: emptySet(),
                                savedPostIds = savedPostIds,
                                sessionViewedReelIds = sessionViewedReelIds,
                                trendingAlgorithmMode = discoveryGlobalConfigState.trendingAlgorithmMode,
                                isSuperOwner = isSuperOwner,
                                onCreatePostClick = { viewModel.selectTab(NoiseTab.CREATE) },
                                onLikePost = { viewModel.toggleLike(it) },
                                onCommentPost = { viewModel.openCommentsForPost(it) },
                                onToggleSavePost = { viewModel.toggleSavePost(it) },
                                onShareReel = { clip -> viewModel.recordReelShare(clip) },
                                onRecordReelView = { clip -> viewModel.recordReelView(clip) },
                                onToggleFollowCreator = { creatorUid ->
                                    val creatorProfile = registeredAccounts.find { it.id == creatorUid || it.userId == creatorUid }
                                    if (creatorProfile != null) {
                                        viewModel.followUser(creatorProfile)
                                    }
                                },
                                onInspectCreatorProfile = { creatorUid -> viewModel.inspectUserById(creatorUid) },
                                onReportReel = { clip ->
                                    viewModel.submitContentReport(
                                        targetType = "post",
                                        targetId = clip.id,
                                        targetOwnerUid = clip.userId,
                                        targetSummary = "Reel by @${clip.username}: ${clip.caption.take(60)}",
                                        reason = "Trending Reel flagged for safety review"
                                    )
                                },
                                onRefreshTrendingReels = { viewModel.refreshTrendingReelsRankings() },
                                onDeletePost = { post -> viewModel.deletePost(post.id) }
                            )
                        }
                        NoiseTab.GAMES -> {
                            MultiplayerGamesScreen(
                                currentUser = userProfile ?: com.example.data.UserProfile(id = currentUserId, userId = currentUserId),
                                gameCatalog = viewModel.gameCatalog,
                                activeGameRooms = gameRoomsState,
                                incomingInvites = gameInvitesState,
                                leaderboardUsers = gameLeaderboardState,
                                gameHistory = gameHistoryState,
                                registeredAccounts = registeredAccounts,
                                graphicsQuality = graphicsQuality,
                                onSelectGraphicsQuality = { viewModel.setGraphicsQuality(it) },
                                onCreateAndOpenGameRoom = { gameItem, mode, maxPlayers, isPrivate ->
                                    viewModel.createAndOpenGameRoom(
                                        gameItem = gameItem,
                                        roomTitle = gameItem.title,
                                        mode = mode,
                                        maxPlayers = maxPlayers,
                                        privateRoom = isPrivate
                                    )
                                },
                                onJoinGameRoomByCodeOrId = { codeOrId ->
                                    viewModel.joinGameRoomByCodeOrId(codeOrId)
                                },
                                onRespondToGameInvite = { invite, accept ->
                                    viewModel.respondToGameInvite(invite, accept)
                                },
                                onInviteFriendToGame = { gameRoom, friend ->
                                    viewModel.sendGameInviteToUser(gameRoom, friend)
                                },
                                onInspectUserProfile = { uid ->
                                    viewModel.inspectUserById(uid)
                                }
                            )
                        }
                        NoiseTab.PROFILE -> {
                            UserProfileScreen(
                                currentUserId = currentUserId,
                                userProfile = userProfile,
                                postsState = postsState,
                                savedPostIds = savedPostIds,
                                followersList = followersState,
                                followingList = followingState,
                                registeredAccounts = registeredAccounts,
                                vipBadgeCatalog = viewModel.vipBadgeCatalog,
                                onPurchaseVipBadge = { viewModel.purchaseVipBadge(it) },
                                onSelectActiveVipBadge = { viewModel.selectActiveVipBadge(it) },
                                onFollowAccount = { viewModel.followUser(it) },
                                onInspectUserProfile = { viewModel.inspectUserById(it) },
                                onToggleThemeMode = { nextMode ->
                                    ThemeManager.applyThemeMode(nextMode, context)
                                    viewModel.updateThemeMode(nextMode)
                                },
                                isSuperOwner = isSuperOwner,
                                onOpenSuperOwnerPanel = { tabIdx -> viewModel.openOwnerControlPanel(tabIdx) },
                                onOpenCentralAiAdmin = { viewModel.setCentralAiAdminModalVisible(true) },
                                onOpenGeminiDeveloperStudio = { viewModel.setGeminiDeveloperStudioVisible(true) },
                                onSaveProfile = { username, displayName, bio, website, avatarKey ->
                                    viewModel.saveProfileEdits(username, displayName, bio, website, avatarKey)
                                },
                                onDeletePost = { viewModel.deletePost(it) }
                            )
                        }
                    }
                }
            }
        }

        // Full-Screen 3D Multiplayer Game Modal (rendered above PartyChatRoomScreen or main Scaffold so LiveKit voice stays active!)
        val currentGameRoom = activeGameRoom
        if (currentGameRoom != null) {
            BackHandler {
                viewModel.closeActiveGameRoom(endForAll = false)
            }
            val currentUserObj = userProfile ?: com.example.data.UserProfile(id = currentUserId, userId = currentUserId)
            FullScreenMultiplayerGameModal(
                gameRoom = currentGameRoom,
                currentUser = currentUserObj,
                graphicsQuality = graphicsQuality,
                liveKitVoiceActive = currentGameRoom.voiceRoomConnected || currentRoom != null,
                micMuted = inGameVoiceMuted,
                audioOutputRouteLabel = if (inGameSpeakerOutput) "Speaker" else "Earpiece",
                activeSpeakers = if (inGameVoiceMuted) emptyList() else listOf(currentUserObj.username),
                onToggleMicMute = {
                    val nextMuted = !inGameVoiceMuted
                    inGameVoiceMuted = nextMuted
                    currentRoom?.let { room ->
                        viewModel.setRoomMicMuted(room.id, nextMuted)
                    }
                },
                onCycleAudioOutputRoute = {
                    inGameSpeakerOutput = !inGameSpeakerOutput
                },
                onOpenInviteSheet = {
                    val firstFriend = registeredAccounts.firstOrNull { it.userId != currentUserId && it.id != currentUserId }
                    if (firstFriend != null) {
                        viewModel.sendGameInviteToUser(currentGameRoom, firstFriend)
                    }
                },
                onRemovePlayer = { targetUid ->
                    viewModel.removePlayerFromActiveGame(targetUid)
                },
                onSyncGameState = { stateJson, nextTurnUid, updatedPlayers, status, winnerUid, winnerName ->
                    viewModel.syncActiveGameState(
                        gameStateJson = stateJson,
                        nextTurnUserId = nextTurnUid,
                        updatedPlayers = updatedPlayers,
                        status = status,
                        winnerUserId = winnerUid,
                        winnerUsername = winnerName
                    )
                },
                onSubmitMatchResult = { winnerProfile, runnerUpName, winScore, loseScore, durationSec ->
                    viewModel.submitActiveGameMatchResult(
                        winnerProfile = winnerProfile,
                        runnerUpUsername = runnerUpName,
                        winnerScore = winScore,
                        runnerUpScore = loseScore,
                        durationSec = durationSec
                    )
                },
                onExitToHost = {
                    viewModel.closeActiveGameRoom(endForAll = false)
                },
                registeredAccounts = registeredAccounts,
                outgoingInvites = outgoingGameInvites,
                onInviteUserToSlot = { targetUser, slotIdx ->
                    viewModel.sendGameInviteToUser(currentGameRoom, targetUser, slotIdx)
                },
                onStartMatch = {
                    viewModel.startActiveGameRoomMatch()
                },
                onCancelSession = {
                    viewModel.closeActiveGameRoom(endForAll = true)
                }
            )
        }

        // Modals & Popups
        if (activeStoryViewerIndex != null && stories.isNotEmpty()) {
            StoryViewerModal(
                stories = stories,
                initialIndex = activeStoryViewerIndex ?: 0,
                currentUserId = currentUserId,
                isSuperOwner = isSuperOwner,
                onStoryViewed = { viewModel.markStoryViewed(it) },
                onAddMoreStoriesClick = {
                    viewModel.openStoryViewer(null)
                    viewModel.addStory("couple", "Neon vibes ✨")
                },
                onDeleteStory = { story ->
                    viewModel.deleteStory(story)
                },
                onReplyToStory = { story, text -> viewModel.replyToStory(story, text) },
                onClose = { viewModel.openStoryViewer(null) }
            )
        }

        if (showCreatePartyRoomPopup) {
            CreatePartyRoomDialog(
                defaultUsername = userProfile?.username ?: "sebin_mr",
                isCreatingRoom = isCreatingPartyRoom,
                serverError = createPartyRoomError,
                onDismiss = { viewModel.setCreatePartyRoomPopupVisible(false) },
                onCreateRoom = { title, description, category, musicTrack, gameName, micMode, capacity, privateRoom, password, themeKey ->
                    viewModel.createFullPartyRoomAndOpen(
                        title = title,
                        description = description,
                        category = category,
                        musicTrack = musicTrack,
                        gameName = gameName,
                        micMode = micMode,
                        capacity = capacity,
                        privateRoom = privateRoom,
                        password = password,
                        themeKey = themeKey
                    )
                }
            )
        }

        if (showNotificationsModal) {
            NotificationsModal(
                notificationsState = notificationsState,
                onMarkAllRead = { viewModel.markAllNotificationsAsRead() },
                onClearAll = { viewModel.clearAllNotifications() },
                onClose = { viewModel.setNotificationsModalVisible(false) },
                onNotificationClick = { item -> viewModel.handleNotificationClick(item) }
            )
        }

        if (showVipVaultModal) {
            VipUnlimitedVaultModal(
                username = userProfile?.username ?: "sebin_mr",
                coins = userProfile?.coins ?: 40150L,
                onClaimTier = { tierName, amount -> viewModel.claimVipCoins(tierName, amount) },
                onClose = { viewModel.setVipVaultModalVisible(false) },
                currentUserProfile = userProfile,
                vipBadgeCatalog = viewModel.vipBadgeCatalog,
                onPurchaseVipBadge = { viewModel.purchaseVipBadge(it) },
                onSelectActiveVipBadge = { viewModel.selectActiveVipBadge(it) },
                coinTransactionsState = coinTransactionsState
            )
        }

        if (activeCommentPost != null) {
            CommentsBottomModal(
                post = activeCommentPost!!,
                commentsState = activeCommentsState,
                onSubmitComment = { text -> viewModel.submitComment(activeCommentPost!!, text) },
                onClose = { viewModel.openCommentsForPost(null) }
            )
        }

        val currentInspected = inspectedUserProfile
        if (currentInspected != null) {
            val liveInspected = registeredAccounts.find { it.id == currentInspected.id || it.userId == currentInspected.userId } ?: currentInspected
            val targetUid = liveInspected.userId.ifBlank { liveInspected.id }
            val isFollowing = targetUid in (userProfile?.followedUserIds ?: emptyList())
            val userPosts = remember(allPosts, targetUid, liveInspected.username) {
                allPosts.filter { it.userId == targetUid || it.username.equals(liveInspected.username, ignoreCase = true) }
            }
            OtherUserProfileModal(
                user = liveInspected,
                isFollowing = isFollowing,
                userPosts = userPosts,
                currentUserId = currentUserId,
                isSuperOwner = isSuperOwner,
                onFollowToggle = { viewModel.followUser(liveInspected) },
                onMessageClick = { viewModel.startChatWithUser(liveInspected) },
                onReportUser = { reason ->
                    viewModel.submitContentReport(
                        targetType = "user",
                        targetId = targetUid,
                        targetOwnerUid = targetUid,
                        targetSummary = "@${liveInspected.username}",
                        reason = reason
                    )
                },
                onOwnerUpdateStatus = { newStatus, reason ->
                    viewModel.ownerUpdateUserAccountStatus(liveInspected, newStatus, reason)
                },
                onOwnerDeletePost = { post ->
                    viewModel.deletePost(post.id)
                },
                onDismiss = { viewModel.openInspectedUserProfile(null) }
            )
        }

        if (showOwnerControlPanel && isSuperOwner) {
            val activeRoomsList = remember(allChatroomsForOwnerState, partyRoomsState) {
                val fromOwnerStream = allChatroomsForOwnerState.filter { !it.isDummyOrSampleRoom() }
                if (fromOwnerStream.isNotEmpty()) {
                    fromOwnerStream
                } else {
                    (partyRoomsState as? UiState.Success)?.data?.filter { !it.isDummyOrSampleRoom() }.orEmpty()
                }
            }
            com.example.ui.SuperOwnerControlPanelModal(
                initialTab = ownerControlInitialTab,
                currentUserProfile = userProfile ?: com.example.data.UserProfile(
                    id = currentUserId,
                    userId = currentUserId,
                    username = "sebin_mr"
                ),
                registryDoc = superOwnerRegistryState,
                allChatrooms = activeRoomsList,
                allPosts = allPosts,
                allStories = stories,
                allGameRooms = gameRoomsState,
                allUsers = registeredAccounts,
                reports = contentReportsState,
                auditLogs = ownerAuditLogsState,
                onDismiss = { viewModel.setOwnerControlPanelVisible(false) },
                onDeleteSingleChatroom = { roomId -> viewModel.ownerDeleteSingleChatroom(roomId) },
                onDeleteSelectedChatrooms = { ids -> viewModel.ownerDeleteSelectedChatrooms(ids) },
                onDeleteAllChatrooms = { viewModel.ownerDeleteAllChatrooms() },
                onPurgeDemoAndDuplicateRooms = { viewModel.ownerPurgeDemoAndDuplicateChatrooms() },
                onDeletePost = { post -> viewModel.deletePost(post.id) },
                onDeleteStory = { story -> viewModel.deleteStory(story) },
                onDeleteGameGroup = { groupId -> viewModel.ownerDeleteGameGroup(groupId) },
                onCleanupAllDemoData = { viewModel.ownerCleanupAllDemoData() },
                onUpdateUserAccountStatus = { targetUser, newStatus, reason ->
                    viewModel.ownerUpdateUserAccountStatus(targetUser, newStatus, reason)
                },
                onResolveReport = { report, newStatus, notes, deleteTarget ->
                    viewModel.ownerResolveReport(report, newStatus, notes, deleteTarget)
                },
                onOpenCentralAiAdmin = { viewModel.setCentralAiAdminModalVisible(true) }
            )
        }

        if (showCentralAiAdminModal && isSuperOwner) {
            com.example.ui.CentralAiAdminPanelModal(
                currentUserProfile = userProfile ?: com.example.data.UserProfile(
                    id = currentUserId,
                    userId = currentUserId,
                    username = "sebin_mr"
                ),
                registryDoc = superOwnerRegistryState,
                productionConfig = productionAppConfig,
                dynamicPages = dynamicPages,
                versionHistory = centralVersionHistory,
                backups = centralBackups,
                aiCommandLogs = aiCommandLogs,
                currentProposal = currentAiProposal,
                isGeneratingProposal = isGeneratingAiProposal,
                isPublishingLiveUpdate = isPublishingLiveUpdate,
                adminStatusMessage = aiAdminStatusMessage,
                onGenerateAiProposal = { prompt -> viewModel.generateAiAdminProposal(prompt) },
                onTogglePreviewProposal = { proposal, enabled ->
                    viewModel.togglePreviewAiProposal(proposal, enabled)
                },
                onPublishProposalToAllUsers = { proposal ->
                    viewModel.publishAiProposalToAllUsers(proposal)
                },
                onDirectSaveDynamicPage = { page ->
                    viewModel.saveDynamicPageAndPublish(page)
                },
                onDeleteDynamicPage = { pageKey ->
                    viewModel.deleteDynamicPageAndPublish(pageKey)
                },
                onCreateManualBackup = { reason ->
                    viewModel.createManualCentralBackup(reason)
                },
                onRollbackToBackup = { backup ->
                    viewModel.rollbackToCentralBackup(backup)
                },
                onOpenDynamicPagePreview = { page ->
                    viewModel.openDynamicPage(page)
                },
                onOpenGeminiDeveloperStudio = {
                    viewModel.setCentralAiAdminModalVisible(false)
                    viewModel.setGeminiDeveloperStudioVisible(true)
                },
                onDismiss = { viewModel.setCentralAiAdminModalVisible(false) }
            )
        }

        if (showGeminiDeveloperStudioModal && isSuperOwner) {
            com.example.ui.GeminiDeveloperStudioModal(
                currentUserProfile = userProfile ?: com.example.data.UserProfile(
                    id = currentUserId,
                    userId = currentUserId,
                    username = "sebin_mr"
                ),
                registryDoc = superOwnerRegistryState,
                productionConfig = productionAppConfig,
                workspaceFiles = developerWorkspaceFiles,
                chatMessages = developerChatMessages,
                currentCodeProposal = currentDeveloperCodeProposal,
                bugFindings = developerBugFindings,
                buildTestReport = developerBuildTestReport,
                githubConfig = githubConfig,
                githubBranches = githubBranches,
                githubCommits = githubCommits,
                deployments = developerDeployments,
                configBackups = centralBackups,
                isBusy = isDeveloperStudioBusy,
                statusMessage = developerStudioStatusMessage,
                onSendChatPrompt = { prompt -> viewModel.sendDeveloperChatPrompt(prompt) },
                onGenerateCodePatch = { prompt -> viewModel.generateDeveloperCodePatch(prompt) },
                onApproveAndDeployCodePatch = { proposal ->
                    viewModel.approveAndDeployDeveloperCodePatch(proposal)
                },
                onRunBugScanner = { viewModel.runDeveloperBugScanner() },
                onApproveAndDeployBugFix = { finding ->
                    viewModel.approveAndDeployDeveloperBugFix(finding)
                },
                onSaveEditedFileToDevBranch = { branch, path, newCode, prevCode, msg ->
                    viewModel.saveEditedFileToDevBranch(branch, path, newCode, prevCode, msg)
                },
                onRunBuildAndTests = { branch -> viewModel.runDeveloperBuildAndTests(branch) },
                onSaveGitHubConfig = { owner, repo, defBranch, devBranch, pat ->
                    viewModel.saveGitHubConfiguration(owner, repo, defBranch, devBranch, pat)
                },
                onRollbackCommit = { commit -> viewModel.rollbackDeveloperCommit(commit) },
                onRollbackConfigBackup = { backup -> viewModel.rollbackToCentralBackup(backup) },
                onOpenCentralAiLivePanel = {
                    viewModel.setGeminiDeveloperStudioVisible(false)
                    viewModel.setCentralAiAdminModalVisible(true)
                },
                onDismiss = { viewModel.setGeminiDeveloperStudioVisible(false) }
            )
        }

        val currentDynamicPage = activeDynamicPage
        if (currentDynamicPage != null) {
            com.example.ui.DynamicAppPageViewerDialog(
                page = currentDynamicPage,
                onNavigateToTab = { tab -> viewModel.selectTab(tab) },
                onDismiss = { viewModel.openDynamicPage(null) }
            )
        }

        val currentDiscoveryViewing = activeViewingDiscoveryItem
        if (currentDiscoveryViewing != null) {
            com.example.ui.DiscoveryContentViewerDialog(
                item = currentDiscoveryViewing,
                onDismiss = { viewModel.setActiveViewingDiscoveryItem(null) },
                onShare = {
                    viewModel.recordDiscoveryItemShare(currentDiscoveryViewing)
                    runCatching {
                        val shareText = "${currentDiscoveryViewing.title}\nSource: ${currentDiscoveryViewing.sourceName}\n${currentDiscoveryViewing.originalUrl}"
                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, currentDiscoveryViewing.title)
                            putExtra(Intent.EXTRA_TEXT, shareText)
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Share Discovery Update"))
                    }
                }
            )
        }

        val isCurrentUserDiscoveryAdmin = com.example.data.DiscoveryContentManager.isUserDiscoveryAdmin(
            userProfile = userProfile,
            globalConfig = discoveryGlobalConfigState
        )
        if (showDiscoveryAdminPanel && isCurrentUserDiscoveryAdmin) {
            com.example.ui.DiscoveryAdminPanelModal(
                globalConfig = discoveryGlobalConfigState,
                sources = discoverySourcesState,
                allItems = allDiscoveryItemsForAdminState,
                importLogs = discoveryImportLogsState,
                isSyncing = isDiscoverySyncing || isKeralaNewsSyncing,
                onToggleCategoryEnabled = { catKey, enabled ->
                    viewModel.setDiscoveryCategoryEnabled(catKey, enabled)
                },
                onToggleKeralaCategoryEnabled = { kCatKey, enabled ->
                    viewModel.setKeralaCategoryEnabled(kCatKey, enabled)
                },
                onUpdateKeralaAndReelsConfig = { algoMode, keralaAlerts, reelsAlerts ->
                    viewModel.updateKeralaAndReelsConfig(
                        keralaNewsNotificationsEnabled = keralaAlerts,
                        trendingReelsNotificationsEnabled = reelsAlerts,
                        trendingAlgorithmMode = algoMode
                    )
                },
                onUpdateRefreshInterval = { mins ->
                    viewModel.updateDiscoveryRefreshIntervalMinutes(mins)
                },
                onAddCustomSource = { catKey, name, feedUrl, contentType ->
                    viewModel.addCustomDiscoverySource(catKey, name, feedUrl, contentType)
                },
                onToggleSourceEnabled = { src, enabled ->
                    viewModel.toggleDiscoverySourceEnabled(src, enabled)
                },
                onDeleteSource = { srcId ->
                    viewModel.deleteDiscoverySource(srcId)
                },
                onModerateItem = { itemId, newStatus, notes ->
                    viewModel.moderateDiscoveryItem(itemId, newStatus, notes)
                },
                onDeleteItem = { itemId, blockDomain ->
                    viewModel.deleteDiscoveryItem(itemId, blockDomain)
                },
                onTriggerImmediateSync = {
                    viewModel.refreshDiscoveryCategory(forceRefresh = true)
                    viewModel.refreshKeralaNews(forceRefresh = true)
                },
                onDismiss = { viewModel.setDiscoveryAdminPanelVisible(false) }
            )
        }

        // 1-on-1 LiveKit Audio & Video Call Overlays (Incoming Ringing Overlay & Active Call Screen)
        val currentCall = activeCalls.firstOrNull { it.status in listOf("ringing", "accepted") }
        if (currentCall != null) {
            if (currentCall.status == "ringing" && currentCall.calleeId == currentUserId) {
                IncomingCallOverlayModal(
                    call = currentCall,
                    onAccept = { viewModel.respondToCall(currentCall, accept = true) },
                    onReject = { viewModel.respondToCall(currentCall, accept = false) }
                )
            } else {
                ActiveOneOnOneCallScreen(
                    call = currentCall,
                    currentUserId = currentUserId,
                    onUpdateMediaState = { micMuted, camEnabled, frontCam ->
                        viewModel.updateCallMediaState(
                            callId = currentCall.id,
                            micMuted = micMuted,
                            cameraEnabled = camEnabled,
                            frontCamera = frontCam
                        )
                    },
                    onEndCall = { durationSec ->
                        viewModel.endCall(currentCall.id, durationSec)
                    }
                )
            }
        }

        // Global Real-Time Incoming Multiplayer Game Invitation Overlay (3D Ludo, Pool, etc.)
        val nowMillis = System.currentTimeMillis()
        val pendingIncomingGameInvite = gameInvitesState.firstOrNull { invite ->
            invite.status == "pending" &&
                invite.receiverUid == currentUserId &&
                invite.senderUid != currentUserId &&
                (invite.expiresAtMillis == 0L || invite.expiresAtMillis > nowMillis) &&
                activeGameRoom?.id != invite.gameRoomId
        }
        if (pendingIncomingGameInvite != null) {
            IncomingGameInviteOverlayModal(
                invite = pendingIncomingGameInvite,
                onAccept = { invite ->
                    viewModel.respondToGameInvite(invite, accept = true)
                },
                onReject = { invite ->
                    viewModel.respondToGameInvite(invite, accept = false)
                }
            )
        }

        // Global Real-Time In-App Notification Banner (Direct Messages, Game Invites, Calls)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
        ) {
            RealTimeNotificationBannerOverlay(
                notification = activeBannerNotification,
                onOpenNotification = { item ->
                    activeBannerNotification = null
                    viewModel.handleNotificationClick(item)
                },
                onDismiss = {
                    activeBannerNotification = null
                }
            )
        }

        // OWNER-ONLY FLOATING EDIT BAR ON EVERY PAGE (Strict Firebase Auth UID + Registry Verification)
        if (isSuperOwner) {
            val activePageKey = when {
                activePartyRoom != null -> "CHATROOM"
                showChatroomsListScreen -> "CHATROOMS_LIST"
                showDirectMessages -> "DIRECT_MESSAGES"
                activeDynamicPage != null -> activeDynamicPage?.pageKey?.uppercase() ?: "DYNAMIC_PAGE"
                else -> selectedTab.name
            }
            com.example.ui.FloatingOwnerEditBarOverlay(
                viewModel = viewModel,
                currentPageKey = activePageKey,
                onOpenDeveloperStudio = { viewModel.setGeminiDeveloperStudioVisible(true) }
            )
        }
    }
}
