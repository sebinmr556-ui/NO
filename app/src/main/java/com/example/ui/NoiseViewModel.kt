package com.example.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.CallSessionDoc
import com.example.data.CoinTransaction
import com.example.data.Comment
import com.example.data.ContentReportDoc
import com.example.data.Conversation
import com.example.data.DirectMessage
import com.example.data.DiscoveryCategory
import com.example.data.DiscoveryContentItem
import com.example.data.DiscoveryGlobalConfig
import com.example.data.DiscoveryImportLog
import com.example.data.DiscoverySourceConfig
import com.example.data.GameInviteDoc
import com.example.data.GameMatchResultDoc
import com.example.data.GameRoomDoc
import com.example.data.GraphicsQualityLevel
import com.example.data.KeralaNewsCategory
import com.example.data.NoiseGameCatalogItem
import com.example.data.NoiseRepository
import com.example.data.NotificationItem
import com.example.data.OwnerAuditLogDoc
import com.example.data.PartyRoom
import com.example.data.Post
import com.example.data.RoomRole
import com.example.data.Story
import com.example.data.SuperOwnerControlManager
import com.example.data.SuperOwnerRegistryDoc
import com.example.data.UiState
import com.example.data.UserProfile
import com.example.data.VipBadgeCatalogItem
import kotlinx.coroutines.delay
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class FailedDirectMessagePayload(
    val messageId: String,
    val conversation: Conversation,
    val text: String,
    val messageType: String,
    val mediaKey: String,
    val mediaType: String,
    val voiceDurationSec: Long,
    val voiceWaveform: String,
    val replyToMessage: DirectMessage?,
    val voiceAudioBase64: String,
    val voiceMimeType: String
)

enum class NoiseTab {
    HOME,
    SEARCH,
    CREATE,
    CLIPS,
    GAMES,
    PROFILE
}

class NoiseViewModel(
    private val repository: NoiseRepository,
    val currentUserId: String
) : ViewModel() {

    private val _selectedTab = MutableStateFlow(NoiseTab.HOME)
    val selectedTab: StateFlow<NoiseTab> = _selectedTab.asStateFlow()

    private val _showNotificationsModal = MutableStateFlow(false)
    val showNotificationsModal: StateFlow<Boolean> = _showNotificationsModal.asStateFlow()

    private val _showVipVaultModal = MutableStateFlow(false)
    val showVipVaultModal: StateFlow<Boolean> = _showVipVaultModal.asStateFlow()

    private val _showDirectMessages = MutableStateFlow(false)
    val showDirectMessages: StateFlow<Boolean> = _showDirectMessages.asStateFlow()

    private val _showCreatePartyRoomPopup = MutableStateFlow(false)
    val showCreatePartyRoomPopup: StateFlow<Boolean> = _showCreatePartyRoomPopup.asStateFlow()

    private val _isCreatingPartyRoom = MutableStateFlow(false)
    val isCreatingPartyRoom: StateFlow<Boolean> = _isCreatingPartyRoom.asStateFlow()

    private val _createPartyRoomError = MutableStateFlow<String?>(null)
    val createPartyRoomError: StateFlow<String?> = _createPartyRoomError.asStateFlow()

    private val _showChatroomsListScreen = MutableStateFlow(false)
    val showChatroomsListScreen: StateFlow<Boolean> = _showChatroomsListScreen.asStateFlow()

    private val _activeStoryViewerIndex = MutableStateFlow<Int?>(null)
    val activeStoryViewerIndex: StateFlow<Int?> = _activeStoryViewerIndex.asStateFlow()

    private val _activeConversation = MutableStateFlow<Conversation?>(null)
    val activeConversation: StateFlow<Conversation?> = _activeConversation.asStateFlow()

    private val _activeCommentPost = MutableStateFlow<Post?>(null)
    val activeCommentPost: StateFlow<Post?> = _activeCommentPost.asStateFlow()

    private val _inspectedUserProfile = MutableStateFlow<UserProfile?>(null)
    val inspectedUserProfile: StateFlow<UserProfile?> = _inspectedUserProfile.asStateFlow()

    private val _savedPostIds = MutableStateFlow<Set<String>>(emptySet())
    val savedPostIds: StateFlow<Set<String>> = _savedPostIds.asStateFlow()

    private val acknowledgedDeliveryKeys = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()

    private val _activePartyRoomId = MutableStateFlow<String?>(null)
    private val _fallbackPartyRoom = MutableStateFlow<PartyRoom?>(null)

    private val _showOwnerControlPanel = MutableStateFlow(false)
    val showOwnerControlPanel: StateFlow<Boolean> = _showOwnerControlPanel.asStateFlow()

    private val _ownerControlInitialTab = MutableStateFlow(0)
    val ownerControlInitialTab: StateFlow<Int> = _ownerControlInitialTab.asStateFlow()

    private val _showCentralAiAdminModal = MutableStateFlow(false)
    val showCentralAiAdminModal: StateFlow<Boolean> = _showCentralAiAdminModal.asStateFlow()

    private val _activeDynamicPage = MutableStateFlow<com.example.data.DynamicAppPage?>(null)
    val activeDynamicPage: StateFlow<com.example.data.DynamicAppPage?> = _activeDynamicPage.asStateFlow()

    private val _currentAiProposal = MutableStateFlow<com.example.data.AiAdminProposal?>(null)
    val currentAiProposal: StateFlow<com.example.data.AiAdminProposal?> = _currentAiProposal.asStateFlow()

    private val _isGeneratingAiProposal = MutableStateFlow(false)
    val isGeneratingAiProposal: StateFlow<Boolean> = _isGeneratingAiProposal.asStateFlow()

    private val _isPublishingLiveUpdate = MutableStateFlow(false)
    val isPublishingLiveUpdate: StateFlow<Boolean> = _isPublishingLiveUpdate.asStateFlow()

    private val _aiAdminStatusMessage = MutableStateFlow<String?>(null)
    val aiAdminStatusMessage: StateFlow<String?> = _aiAdminStatusMessage.asStateFlow()

    val productionAppConfigState: StateFlow<com.example.data.CentralAppConfig> = repository.observeProductionAppConfig()
        .catch { emit(com.example.data.CentralAppConfig()) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = com.example.data.CentralAppConfig()
        )

    val dynamicPagesState: StateFlow<List<com.example.data.DynamicAppPage>> = repository.observeDynamicPages()
        .catch { emit(emptyList()) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = emptyList()
        )

    val centralVersionHistoryState: StateFlow<List<com.example.data.CentralVersionDoc>> = repository.observeCentralVersionHistory()
        .catch { emit(emptyList()) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = emptyList()
        )

    val centralBackupsState: StateFlow<List<com.example.data.CentralConfigBackupDoc>> = repository.observeCentralConfigBackups()
        .catch { emit(emptyList()) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = emptyList()
        )

    val aiCommandLogsState: StateFlow<List<com.example.data.AiCommandExecutionLogDoc>> = repository.observeAiCommandLogs()
        .catch { emit(emptyList()) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = emptyList()
        )

    // Owner Only Gemini AI Developer Studio state flows
    private val _showGeminiDeveloperStudioModal = MutableStateFlow(false)
    val showGeminiDeveloperStudioModal: StateFlow<Boolean> = _showGeminiDeveloperStudioModal.asStateFlow()

    private val _currentDeveloperCodeProposal = MutableStateFlow<com.example.data.AiCodePatchProposal?>(null)
    val currentDeveloperCodeProposal: StateFlow<com.example.data.AiCodePatchProposal?> = _currentDeveloperCodeProposal.asStateFlow()

    private val _isDeveloperStudioBusy = MutableStateFlow(false)
    val isDeveloperStudioBusy: StateFlow<Boolean> = _isDeveloperStudioBusy.asStateFlow()

    private val _developerStudioStatusMessage = MutableStateFlow<String?>(null)
    val developerStudioStatusMessage: StateFlow<String?> = _developerStudioStatusMessage.asStateFlow()

    private val _developerBuildTestReport = MutableStateFlow(
        com.example.data.GeminiDeveloperStudioManager.runBuildAndTestVerificationSuite(
            com.example.data.GeminiDeveloperStudioManager.defaultProjectSourceCatalog()
        )
    )
    val developerBuildTestReport: StateFlow<com.example.data.DeveloperBuildTestReport> = _developerBuildTestReport.asStateFlow()

    val developerWorkspaceFilesState: StateFlow<List<com.example.data.ProjectSourceFileDoc>> =
        repository.observeDeveloperWorkspaceFiles()
            .catch { emit(com.example.data.GeminiDeveloperStudioManager.defaultProjectSourceCatalog()) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = com.example.data.GeminiDeveloperStudioManager.defaultProjectSourceCatalog()
            )

    val developerChatMessagesState: StateFlow<List<com.example.data.AiDeveloperChatMessageDoc>> =
        repository.observeDeveloperChatMessages()
            .catch { emit(emptyList()) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = emptyList()
            )

    val developerBugFindingsState: StateFlow<List<com.example.data.BugScanFindingDoc>> =
        repository.observeDeveloperBugFindings()
            .catch { emit(emptyList()) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = emptyList()
            )

    val githubConfigState: StateFlow<com.example.data.GitHubIntegrationConfigDoc> =
        repository.observeGitHubConfig()
            .catch { emit(com.example.data.GitHubIntegrationConfigDoc()) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = com.example.data.GitHubIntegrationConfigDoc()
            )

    val githubBranchesState: StateFlow<List<com.example.data.GitHubBranchDoc>> =
        repository.observeGitHubBranches()
            .catch { emit(emptyList()) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = emptyList()
            )

    val githubCommitsState: StateFlow<List<com.example.data.GitHubCommitDoc>> =
        repository.observeGitHubCommits()
            .catch { emit(emptyList()) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = emptyList()
            )

    val developerDeploymentsState: StateFlow<List<com.example.data.DeveloperDeploymentDoc>> =
        repository.observeDeveloperDeployments()
            .catch { emit(emptyList()) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = emptyList()
            )

    val superOwnerRegistryState: StateFlow<SuperOwnerRegistryDoc> = repository.observeSuperOwnerRegistry()
        .catch { emit(SuperOwnerRegistryDoc()) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = SuperOwnerRegistryDoc()
        )

    val allChatroomsForOwnerState: StateFlow<List<PartyRoom>> = repository.observeAllChatroomsForOwner()
        .catch { emit(emptyList()) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = emptyList()
        )

    val ownerAuditLogsState: StateFlow<List<OwnerAuditLogDoc>> = repository.observeOwnerAuditLogs()
        .catch { emit(emptyList()) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = emptyList()
        )

    val contentReportsState: StateFlow<List<ContentReportDoc>> = repository.observeContentReports()
        .catch { emit(emptyList()) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = emptyList()
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val activePartyRoom: StateFlow<PartyRoom?> = _activePartyRoomId
        .flatMapLatest { roomId ->
            if (roomId == null || SuperOwnerControlManager.isRoomDeletedTombstone(roomId)) {
                flowOf(null)
            } else {
                repository.observeSinglePartyRoom(roomId)
                    .map { liveRoom ->
                        if (SuperOwnerControlManager.isRoomDeletedTombstone(roomId)) {
                            null
                        } else {
                            liveRoom ?: _fallbackPartyRoom.value
                        }
                    }
                    .catch {
                        emit(
                            if (SuperOwnerControlManager.isRoomDeletedTombstone(roomId)) null
                            else _fallbackPartyRoom.value
                        )
                    }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = null
        )

    // Multiplayer Gaming & VIP Gemstone Badge Catalog state
    val gameCatalog: List<NoiseGameCatalogItem> = repository.getGameCatalog()
    val vipBadgeCatalog: List<VipBadgeCatalogItem> = repository.getVipBadgeCatalog()

    private val _graphicsQuality = MutableStateFlow(GraphicsQualityLevel.HIGH)
    val graphicsQuality: StateFlow<GraphicsQualityLevel> = _graphicsQuality.asStateFlow()

    private val _activeGameRoomId = MutableStateFlow<String?>(null)
    private val _fallbackGameRoom = MutableStateFlow<GameRoomDoc?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val activeGameRoom: StateFlow<GameRoomDoc?> = _activeGameRoomId
        .flatMapLatest { gameRoomId ->
            if (gameRoomId == null) {
                flowOf(null)
            } else {
                repository.observeSingleGameRoom(gameRoomId)
                    .map { liveRoom -> liveRoom ?: _fallbackGameRoom.value }
                    .catch { emit(_fallbackGameRoom.value) }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = null
        )

    val gameRoomsState: StateFlow<List<GameRoomDoc>> = repository.observeGameRooms()
        .catch { e ->
            Log.w(TAG, "Error observing game rooms", e)
            emit(emptyList())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = emptyList()
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val activePartyRoomGameRooms: StateFlow<List<GameRoomDoc>> = _activePartyRoomId
        .flatMapLatest { partyRoomId ->
            if (partyRoomId.isNullOrBlank()) {
                flowOf(emptyList())
            } else {
                repository.observeChatRoomGameRooms(partyRoomId)
                    .catch { emit(emptyList()) }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = emptyList()
        )

    val gameInvitesState: StateFlow<List<GameInviteDoc>> = repository.observeGameInvites(currentUserId)
        .catch { e ->
            Log.w(TAG, "Error observing game invites", e)
            emit(emptyList())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = emptyList()
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val outgoingGameInvitesState: StateFlow<List<GameInviteDoc>> = _activeGameRoomId
        .flatMapLatest { gameRoomId ->
            if (gameRoomId.isNullOrBlank()) {
                flowOf(emptyList())
            } else {
                repository.observeOutgoingGameInvitesForRoom(gameRoomId, currentUserId)
                    .catch { emit(emptyList()) }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = emptyList()
        )

    val gameLeaderboardState: StateFlow<List<UserProfile>> = repository.observeGameLeaderboard()
        .catch { e ->
            Log.w(TAG, "Error observing game leaderboard", e)
            emit(emptyList())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = emptyList()
        )

    val gameHistoryState: StateFlow<List<GameMatchResultDoc>> = repository.observeGameHistory(currentUserId)
        .catch { e ->
            Log.w(TAG, "Error observing game history", e)
            emit(emptyList())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = emptyList()
        )

    val followersState: StateFlow<List<UserProfile>> = repository.observeFollowers(currentUserId)
        .catch { emit(emptyList()) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = emptyList()
        )

    val followingState: StateFlow<List<UserProfile>> = repository.observeFollowing(currentUserId)
        .catch { emit(emptyList()) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = emptyList()
        )

    private val _fallbackCallSession = MutableStateFlow<CallSessionDoc?>(null)

    val activeCallsState: StateFlow<List<CallSessionDoc>> = repository.observeActiveCalls(currentUserId)
        .map { calls ->
            val fallback = _fallbackCallSession.value
            if (fallback != null && calls.none { it.id == fallback.id }) {
                listOf(fallback) + calls
            } else {
                if (fallback != null) {
                    val liveMatch = calls.find { it.id == fallback.id }
                    if (liveMatch != null && liveMatch.status in listOf("ended", "rejected", "missed", "busy")) {
                        _fallbackCallSession.value = null
                    }
                }
                calls
            }
        }
        .catch { emit(listOfNotNull(_fallbackCallSession.value)) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = emptyList()
        )

    private val _statusBanner = MutableStateFlow<String?>(null)
    val statusBanner: StateFlow<String?> = _statusBanner.asStateFlow()

    val userProfileState: StateFlow<UiState<UserProfile?>> = repository.observeUserProfile(currentUserId)
        .map<UserProfile?, UiState<UserProfile?>> { UiState.Success(it) }
        .catch { e ->
            Log.w(TAG, "Error observing user profile", e)
            emit(UiState.Error(e.message ?: "Failed to load profile"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val registeredAccountsState: StateFlow<UiState<List<UserProfile>>> = repository.observeRegisteredAccounts()
        .map<List<UserProfile>, UiState<List<UserProfile>>> { UiState.Success(it) }
        .catch { e ->
            Log.w(TAG, "Error observing registered accounts", e)
            emit(UiState.Error(e.message ?: "Failed to load registered accounts"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val postsState: StateFlow<UiState<List<Post>>> = repository.observePosts()
        .map<List<Post>, UiState<List<Post>>> { UiState.Success(it) }
        .catch { e ->
            Log.w(TAG, "Error observing posts", e)
            emit(UiState.Error(e.message ?: "Failed to load posts"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val storiesState: StateFlow<UiState<List<Story>>> = repository.observeStories()
        .map<List<Story>, UiState<List<Story>>> { UiState.Success(it) }
        .catch { e ->
            Log.w(TAG, "Error observing stories", e)
            emit(UiState.Error(e.message ?: "Failed to load stories"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val partyRoomsState: StateFlow<UiState<List<PartyRoom>>> = combine(
        repository.observePartyRooms(),
        _fallbackPartyRoom
    ) { remoteRooms, fallback ->
        val merged = if (fallback != null &&
            !fallback.isDummyOrSampleRoom() &&
            !SuperOwnerControlManager.isRoomDeletedTombstone(fallback.id) &&
            remoteRooms.none { it.id == fallback.id }
        ) {
            listOf(fallback) + remoteRooms
        } else {
            remoteRooms
        }
        val state: UiState<List<PartyRoom>> = UiState.Success(merged)
        state
    }
        .catch { e ->
            Log.w(TAG, "Error observing party rooms", e)
            val fallbackList = listOfNotNull(_fallbackPartyRoom.value?.takeIf { !it.isDummyOrSampleRoom() })
            if (fallbackList.isNotEmpty()) {
                emit(UiState.Success(fallbackList))
            } else {
                emit(UiState.Error(e.message ?: "Failed to load party rooms"))
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val notificationsState: StateFlow<UiState<List<NotificationItem>>> = repository.observeNotifications(currentUserId)
        .map<List<NotificationItem>, UiState<List<NotificationItem>>> { UiState.Success(it) }
        .catch { e ->
            Log.w(TAG, "Error observing notifications", e)
            emit(UiState.Error(e.message ?: "Failed to load notifications"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val conversationsState: StateFlow<UiState<List<Conversation>>> = repository.observeConversations(currentUserId)
        .map<List<Conversation>, UiState<List<Conversation>>> { list ->
            val currentOnline = (userProfileState.value as? UiState.Success)?.data?.online != false
            if (currentOnline) {
                list.forEach { conv ->
                    val ackKey = "${conv.id}_${conv.lastMessage}_${conv.unreadCount}_${conv.updatedAt?.seconds}_${conv.updatedAt?.nanoseconds}"
                    if (acknowledgedDeliveryKeys.add(ackKey)) {
                        viewModelScope.launch {
                            repository.acknowledgePendingDeliveriesInConversation(conv.id)
                        }
                    }
                }
            }
            // Keep activeConversation metadata fresh if it is currently open
            val active = _activeConversation.value
            if (active != null) {
                val updatedActive = list.find { it.id == active.id }
                if (updatedActive != null && updatedActive != active) {
                    _activeConversation.value = updatedActive
                }
            }
            UiState.Success(list)
        }
        .catch { e ->
            Log.w(TAG, "Error observing conversations", e)
            emit(UiState.Error(e.message ?: "Failed to load conversations"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val coinTransactionsState: StateFlow<UiState<List<CoinTransaction>>> = repository.observeCoinTransactions(currentUserId)
        .map<List<CoinTransaction>, UiState<List<CoinTransaction>>> { UiState.Success(it) }
        .catch { e ->
            Log.w(TAG, "Error observing coin transactions", e)
            emit(UiState.Error(e.message ?: "Failed to load coin transactions"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    private val _pendingDirectMessages = MutableStateFlow<Map<String, List<DirectMessage>>>(emptyMap())
    private val _failedDirectPayloads = MutableStateFlow<Map<String, FailedDirectMessagePayload>>(emptyMap())

    @OptIn(ExperimentalCoroutinesApi::class)
    val activeMessagesState: StateFlow<UiState<List<DirectMessage>>> = _activeConversation
        .flatMapLatest { conv ->
            if (conv == null || conv.id.isBlank()) {
                flowOf(UiState.Success(emptyList()))
            } else {
                combine(
                    repository.observeMessages(conv.id, currentUserId),
                    _pendingDirectMessages
                ) { remoteMessages, pendingByConv ->
                    val localPending = pendingByConv[conv.id].orEmpty()
                    val remoteIds = remoteMessages.map { it.id }.toSet()
                    val remainingPending = localPending.filter { it.id !in remoteIds }
                    val state: UiState<List<DirectMessage>> = UiState.Success(remoteMessages + remainingPending)
                    state
                }.catch { e ->
                    Log.w(TAG, "Error observing messages", e)
                    emit(UiState.Error(e.message ?: "Failed to load messages"))
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val activeCommentsState: StateFlow<UiState<List<Comment>>> = _activeCommentPost
        .flatMapLatest { post ->
            if (post == null || post.id.isBlank()) {
                flowOf(UiState.Success(emptyList()))
            } else {
                repository.observeComments(post.id)
                    .map<List<Comment>, UiState<List<Comment>>> { UiState.Success(it) }
                    .catch { e ->
                        Log.w(TAG, "Error observing comments", e)
                        emit(UiState.Error(e.message ?: "Failed to load comments"))
                    }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    private val _selectedDiscoveryCategory = MutableStateFlow(DiscoveryCategory.FOR_YOU)
    val selectedDiscoveryCategory: StateFlow<DiscoveryCategory> = _selectedDiscoveryCategory.asStateFlow()

    private val _isDiscoverySyncing = MutableStateFlow(false)
    val isDiscoverySyncing: StateFlow<Boolean> = _isDiscoverySyncing.asStateFlow()

    private val _showDiscoveryAdminPanel = MutableStateFlow(false)
    val showDiscoveryAdminPanel: StateFlow<Boolean> = _showDiscoveryAdminPanel.asStateFlow()

    private val _activeViewingDiscoveryItem = MutableStateFlow<DiscoveryContentItem?>(null)
    val activeViewingDiscoveryItem: StateFlow<DiscoveryContentItem?> = _activeViewingDiscoveryItem.asStateFlow()

    // Automatic Kerala News Hub state
    private val _selectedKeralaCategory = MutableStateFlow(KeralaNewsCategory.KERALA_LATEST)
    val selectedKeralaCategory: StateFlow<KeralaNewsCategory> = _selectedKeralaCategory.asStateFlow()

    private val _selectedKeralaDistrict = MutableStateFlow("All Districts")
    val selectedKeralaDistrict: StateFlow<String> = _selectedKeralaDistrict.asStateFlow()

    private val _selectedKeralaLanguage = MutableStateFlow("all") // "all", "ml", "en"
    val selectedKeralaLanguage: StateFlow<String> = _selectedKeralaLanguage.asStateFlow()

    private val _isKeralaNewsSyncing = MutableStateFlow(false)
    val isKeralaNewsSyncing: StateFlow<Boolean> = _isKeralaNewsSyncing.asStateFlow()

    private val _showKeralaNewsSection = MutableStateFlow(true)
    val showKeralaNewsSection: StateFlow<Boolean> = _showKeralaNewsSection.asStateFlow()

    // Session-viewed Reel IDs to prevent repeat loops in Trending Reels
    private val _sessionViewedReelIds = MutableStateFlow<Set<String>>(emptySet())
    val sessionViewedReelIds: StateFlow<Set<String>> = _sessionViewedReelIds.asStateFlow()

    val discoveryItemsState: StateFlow<List<DiscoveryContentItem>> = repository.observeDiscoveryItems()
        .catch { emit(emptyList()) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = emptyList()
        )

    val allDiscoveryItemsForAdminState: StateFlow<List<DiscoveryContentItem>> = repository.observeAllDiscoveryItemsForAdmin()
        .catch { emit(emptyList()) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = emptyList()
        )

    val discoveryGlobalConfigState: StateFlow<DiscoveryGlobalConfig> = repository.observeDiscoveryGlobalConfig()
        .catch { emit(DiscoveryGlobalConfig()) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = DiscoveryGlobalConfig()
        )

    val discoverySourcesState: StateFlow<List<DiscoverySourceConfig>> = repository.observeDiscoverySources()
        .catch { emit(emptyList()) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = emptyList()
        )

    val discoveryImportLogsState: StateFlow<List<DiscoveryImportLog>> = repository.observeDiscoveryImportLogs()
        .catch { emit(emptyList()) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = emptyList()
        )

    init {
        viewModelScope.launch {
            repository.ensureUserInitialized()
            repository.registerDeviceNotificationToken()
            // Initial automatic content discovery + Kerala News check on launch
            triggerDiscoverySync(_selectedDiscoveryCategory.value, forceRefresh = false)
            triggerKeralaNewsSync(_selectedKeralaCategory.value, forceRefresh = false)
            triggerKeralaNewsSync(KeralaNewsCategory.KERALA_BREAKING, forceRefresh = false)
        }
        // Scheduled periodic 15-30 minute automatic discovery + Kerala News + Trending Reels check
        viewModelScope.launch {
            while (true) {
                val intervalMinutes = discoveryGlobalConfigState.value.refreshIntervalMinutes.coerceIn(15L, 30L)
                delay(intervalMinutes * 60_000L)
                triggerDiscoverySync(_selectedDiscoveryCategory.value, forceRefresh = false)
                triggerKeralaNewsSync(_selectedKeralaCategory.value, forceRefresh = false)
                triggerKeralaNewsSync(KeralaNewsCategory.KERALA_BREAKING, forceRefresh = false)
                repository.refreshTrendingReelsRankings(discoveryGlobalConfigState.value.trendingAlgorithmMode)
            }
        }
    }

    fun registerDeviceNotificationToken(token: String = "") {
        viewModelScope.launch {
            repository.registerDeviceNotificationToken(token)
        }
    }

    fun selectTab(tab: NoiseTab) {
        _selectedTab.value = tab
    }

    fun setNotificationsModalVisible(visible: Boolean) {
        _showNotificationsModal.value = visible
    }

    fun setVipVaultModalVisible(visible: Boolean) {
        _showVipVaultModal.value = visible
    }

    fun setDirectMessagesVisible(visible: Boolean) {
        _showDirectMessages.value = visible
        if (!visible) {
            _activeConversation.value = null
        }
    }

    fun setCreatePartyRoomPopupVisible(visible: Boolean) {
        if (!visible) {
            _createPartyRoomError.value = null
            _isCreatingPartyRoom.value = false
        }
        _showCreatePartyRoomPopup.value = visible
    }

    fun setChatroomsListScreenVisible(visible: Boolean) {
        _showChatroomsListScreen.value = visible
    }

    fun refreshActiveChatrooms() {
        _statusBanner.value = "Active chatrooms refreshed in real time"
    }

    fun openStoryViewer(index: Int?) {
        _activeStoryViewerIndex.value = index
    }

    fun openConversation(conversation: Conversation?) {
        _activeConversation.value = conversation
    }

    fun openConversationById(conversationId: String) {
        val cleanId = conversationId.trim()
        if (cleanId.isBlank()) return
        _showNotificationsModal.value = false
        _inspectedUserProfile.value = null
        val existingList = (conversationsState.value as? UiState.Success)?.data ?: emptyList()
        val match = existingList.find { it.id == cleanId }
        if (match != null) {
            _showDirectMessages.value = true
            _activeConversation.value = match
        } else {
            viewModelScope.launch {
                repository.getConversationById(cleanId).onSuccess { fetched ->
                    if (fetched != null) {
                        _showDirectMessages.value = true
                        _activeConversation.value = fetched
                    }
                }
            }
        }
    }

    fun handleNotificationClick(item: NotificationItem) {
        viewModelScope.launch {
            repository.markSingleNotificationRead(item.id)
        }
        when {
            item.conversationId.isNotBlank() || item.type in listOf("message", "dm", "call") -> {
                val convId = item.conversationId.ifBlank {
                    if (item.actorUid.isNotBlank()) repository.buildSharedConversationId(currentUserId, item.actorUid) else ""
                }
                if (convId.isNotBlank()) {
                    openConversationById(convId)
                }
            }
            item.gameRoomId.isNotBlank() || item.type == "game_invite" -> {
                _showNotificationsModal.value = false
                if (item.gameRoomId.isNotBlank()) {
                    joinGameRoomByCodeOrId(item.gameRoomId)
                }
            }
            item.actorUid.isNotBlank() -> {
                _showNotificationsModal.value = false
                inspectUserById(item.actorUid)
            }
        }
    }

    fun markSingleNotificationRead(notificationId: String) {
        viewModelScope.launch {
            repository.markSingleNotificationRead(notificationId)
        }
    }

    fun openCommentsForPost(post: Post?) {
        _activeCommentPost.value = post
    }

    fun openInspectedUserProfile(user: UserProfile?) {
        _inspectedUserProfile.value = user
    }

    fun inspectUserById(userId: String) {
        if (userId.isBlank() || userId == currentUserId) {
            _selectedTab.value = NoiseTab.PROFILE
            return
        }
        val accounts = (registeredAccountsState.value as? UiState.Success)?.data ?: emptyList()
        val found = accounts.find { it.userId == userId || it.id == userId }
        if (found != null) {
            _inspectedUserProfile.value = found
        }
    }

    fun setGraphicsQuality(level: GraphicsQualityLevel) {
        _graphicsQuality.value = level
        _statusBanner.value = "3D Graphics Quality set to ${level.label} (${level.targetFps} FPS)"
    }

    private val _mediaUploadState = MutableStateFlow(com.example.data.MediaUploadState())
    val mediaUploadState: StateFlow<com.example.data.MediaUploadState> = _mediaUploadState.asStateFlow()

    fun createAndOpenGameRoom(
        gameItem: NoiseGameCatalogItem,
        roomTitle: String = "",
        mode: String = "1v1",
        maxPlayers: Int = gameItem.maxPlayers,
        privateRoom: Boolean = false,
        chatRoomId: String = "",
        chatRoomTitle: String = "",
        invitedUserIds: List<String> = emptyList()
    ) {
        val profile = currentProfileOrDefault()
        val optimisticRoomId = "groom_${java.util.UUID.randomUUID().toString().replace("-", "").take(14)}"
        val optimisticCode = java.util.UUID.randomUUID().toString().replace("-", "").uppercase().take(6)
        val hostSlot = com.example.data.GamePlayerSlot(
            slotIndex = 1,
            slotNumber = 1,
            userId = currentUserId,
            username = profile.username.ifBlank { "sebin_mr" },
            displayName = profile.displayName.ifBlank { profile.username },
            avatarKey = profile.avatarKey.ifBlank { "S" },
            team = 1,
            ready = true,
            score = 0L,
            connected = true,
            micEnabled = true,
            speaking = false
        )
        val optimisticRoom = GameRoomDoc(
            id = optimisticRoomId,
            appScope = "noise_live_public",
            roomCode = optimisticCode,
            joinCode = optimisticCode,
            gameType = gameItem.gameType,
            gameId = gameItem.gameType,
            gameTitle = roomTitle.ifBlank { gameItem.title },
            gameName = gameItem.title,
            roomTitle = roomTitle.ifBlank { gameItem.title },
            hostId = currentUserId,
            hostUsername = profile.username.ifBlank { "sebin_mr" },
            hostAvatarKey = profile.avatarKey.ifBlank { "S" },
            chatRoomId = chatRoomId,
            chatRoomTitle = chatRoomTitle,
            mode = mode,
            matchMode = mode,
            status = "waiting",
            privateRoom = privateRoom,
            maxPlayers = maxPlayers.coerceIn(2, 8).toLong(),
            currentTurnUserId = currentUserId,
            currentTurnUid = currentUserId,
            unityLobbyId = "LBY-$optimisticCode",
            unityRelayJoinCode = "RLY-$optimisticCode",
            voiceChannelId = if (chatRoomId.isNotBlank()) "voice_$chatRoomId" else "voice_$optimisticRoomId",
            voiceRoomConnected = chatRoomId.isNotBlank(),
            participantIds = listOf(currentUserId),
            invitedUserIds = invitedUserIds,
            playersJson = GameRoomDoc.encodePlayers(listOf(hostSlot)),
            gameStateJson = "{}"
        )
        // Immediately open the playable 3D game modal in waiting lobby state!
        _fallbackGameRoom.value = optimisticRoom
        _activeGameRoomId.value = optimisticRoom.id
        _statusBanner.value = "Created ${gameItem.title} Session • Room Code: $optimisticCode"

        viewModelScope.launch {
            repository.createGameRoom(
                hostProfile = profile,
                gameType = gameItem.gameType,
                gameTitle = roomTitle.ifBlank { gameItem.title },
                mode = mode,
                maxPlayers = maxPlayers,
                privateRoom = privateRoom,
                chatRoomId = chatRoomId,
                chatRoomTitle = chatRoomTitle,
                voiceRoomConnected = chatRoomId.isNotBlank(),
                roomIdOverride = optimisticRoomId,
                roomCodeOverride = optimisticCode
            ).onSuccess { room ->
                _fallbackGameRoom.value = room
                _activeGameRoomId.value = room.id
                val accounts = (registeredAccountsState.value as? UiState.Success)?.data ?: emptyList()
                invitedUserIds.forEachIndexed { idx, targetUid ->
                    val targetProfile = accounts.find { it.userId == targetUid || it.id == targetUid }
                    if (targetProfile != null) {
                        repository.sendGameInvite(room, profile, targetProfile, slotIndex = (idx + 2).coerceIn(2, maxPlayers))
                    }
                }
                _statusBanner.value = "Launched ${room.gameName} • Room Code: ${room.roomCode}"
            }
        }
    }

    fun joinGameRoomByCodeOrId(codeOrId: String) {
        viewModelScope.launch {
            val profile = currentProfileOrDefault()
            repository.joinGameRoomByIdOrCode(codeOrId, profile)
                .onSuccess { room ->
                    _fallbackGameRoom.value = room
                    _activeGameRoomId.value = room.id
                    _statusBanner.value = "Joined ${room.gameName} (${room.roomTitle})!"
                }
                .onFailure { e ->
                    _statusBanner.value = e.localizedMessage ?: "Could not join game room"
                }
        }
    }

    fun respondToGameInvite(invite: GameInviteDoc, accept: Boolean) {
        viewModelScope.launch {
            val profile = currentProfileOrDefault()
            repository.respondToGameInvite(invite, accept, profile)
                .onSuccess { joinedRoom ->
                    if (accept && joinedRoom != null) {
                        _fallbackGameRoom.value = joinedRoom
                        _activeGameRoomId.value = joinedRoom.id
                        _statusBanner.value = "Accepted invite • Joined ${joinedRoom.gameName} (Slot ${invite.slotIndex})!"
                    } else {
                        _statusBanner.value = "Declined game invite"
                    }
                }
                .onFailure { e ->
                    _statusBanner.value = e.localizedMessage ?: "Failed to respond to invite"
                }
        }
    }

    fun sendGameInviteToUser(gameRoom: GameRoomDoc, targetUser: UserProfile, slotIndex: Int = 2) {
        viewModelScope.launch {
            val profile = currentProfileOrDefault()
            repository.sendGameInvite(gameRoom, profile, targetUser, slotIndex = slotIndex)
                .onSuccess {
                    _statusBanner.value = "Invited @${targetUser.username} to Slot $slotIndex!"
                }
                .onFailure { e ->
                    _statusBanner.value = e.localizedMessage ?: "Failed to send invite"
                }
        }
    }

    fun removePlayerFromActiveGame(targetUserId: String) {
        val currentRoom = activeGameRoom.value ?: return
        viewModelScope.launch {
            repository.removePlayerFromGameRoom(currentRoom.id, targetUserId)
                .onSuccess { updated ->
                    _fallbackGameRoom.value = updated
                    _statusBanner.value = "Removed player from game slot"
                }
                .onFailure { e ->
                    _statusBanner.value = e.localizedMessage ?: "Could not remove player"
                }
        }
    }

    fun startActiveGameRoomMatch() {
        val currentRoom = activeGameRoom.value ?: return
        viewModelScope.launch {
            repository.startGameRoomMatch(currentRoom.id)
                .onSuccess { updated ->
                    _fallbackGameRoom.value = updated
                }
        }
    }

    fun syncActiveGameState(
        gameStateJson: String,
        nextTurnUserId: String,
        updatedPlayers: List<com.example.data.GamePlayerSlot>? = null,
        status: String? = null,
        winnerUserId: String? = null,
        winnerUsername: String? = null
    ) {
        val currentRoom = activeGameRoom.value ?: return
        viewModelScope.launch {
            repository.syncGameRoomState(
                gameRoomId = currentRoom.id,
                gameStateJson = gameStateJson,
                nextTurnUserId = nextTurnUserId,
                updatedPlayers = updatedPlayers,
                status = status,
                winnerUserId = winnerUserId,
                winnerUsername = winnerUsername
            ).onSuccess { updated ->
                _fallbackGameRoom.value = updated
            }
        }
    }

    fun submitActiveGameMatchResult(
        winnerProfile: UserProfile,
        runnerUpUsername: String,
        winnerScore: Long,
        runnerUpScore: Long,
        durationSec: Long
    ) {
        val currentRoom = activeGameRoom.value ?: return
        viewModelScope.launch {
            repository.submitVerifiedGameResult(
                gameRoom = currentRoom,
                winnerProfile = winnerProfile,
                runnerUpUsername = runnerUpUsername,
                winnerScore = winnerScore,
                runnerUpScore = runnerUpScore,
                durationSec = durationSec
            ).onSuccess { result ->
                _statusBanner.value = "🏆 Match Complete! Winner: @${result.winnerUsername} (+${result.coinsRewarded} VIP Coins)"
            }
        }
    }

    fun closeActiveGameRoom(endForAll: Boolean = false) {
        val currentRoom = activeGameRoom.value
        _activeGameRoomId.value = null
        _fallbackGameRoom.value = null
        if (currentRoom != null) {
            viewModelScope.launch {
                repository.endOrLeaveGameRoom(currentRoom.id)
            }
        }
    }

    fun endSpecificGameRoom(gameRoom: GameRoomDoc) {
        if (_activeGameRoomId.value == gameRoom.id) {
            _activeGameRoomId.value = null
            _fallbackGameRoom.value = null
        }
        viewModelScope.launch {
            repository.endOrLeaveGameRoom(gameRoom.id)
        }
    }

    fun openPartyRoom(room: PartyRoom?) {
        val prevId = _activePartyRoomId.value
        if (room == null) {
            _activePartyRoomId.value = null
            _fallbackPartyRoom.value = null
            if (prevId != null) {
                viewModelScope.launch {
                    repository.leavePartyRoom(prevId)
                }
            }
        } else {
            _fallbackPartyRoom.value = room
            _activePartyRoomId.value = room.id
            viewModelScope.launch {
                val profile = currentProfileOrDefault()
                repository.enterPartyRoom(room.id, profile)
            }
        }
    }

    fun clearStatusBanner() {
        _statusBanner.value = null
    }

    fun setUserOnlineStatus(online: Boolean) {
        viewModelScope.launch {
            repository.setUserOnlineStatus(online)
        }
    }

    private fun currentProfileOrDefault(): UserProfile {
        val loaded = (userProfileState.value as? UiState.Success)?.data
        if (loaded != null) return loaded
        val authUser = runCatching { com.google.firebase.auth.FirebaseAuth.getInstance().currentUser }.getOrNull()
        val isOwner = SuperOwnerControlManager.isVerifiedSuperOwner(
            uid = currentUserId,
            email = authUser?.email,
            userProfile = null
        )
        val fallbackName = if (isOwner) {
            SuperOwnerControlManager.PERMANENT_SUPER_OWNER_USERNAME
        } else {
            authUser?.email?.substringBefore("@")?.replace(Regex("[^a-zA-Z0-9_]"), "_")?.takeIf { it.length >= 2 }
                ?: "user_${currentUserId.takeLast(6).lowercase().ifBlank { "member" }}"
        }
        return UserProfile(
            id = currentUserId,
            userId = currentUserId,
            username = fallbackName,
            usernameLower = fallbackName.lowercase(),
            displayName = authUser?.displayName?.ifBlank { fallbackName } ?: fallbackName,
            avatarKey = authUser?.photoUrl?.toString()?.ifBlank { fallbackName.take(1).uppercase() } ?: fallbackName.take(1).uppercase(),
            profilePictureUrl = authUser?.photoUrl?.toString().orEmpty()
        )
    }

    fun claimVipCoins(tierName: String, amount: Long) {
        viewModelScope.launch {
            val profile = currentProfileOrDefault()
            val res = repository.claimVipCoins(profile, tierName, amount)
            res.onSuccess { newBalance ->
                _statusBanner.value = "Claimed +$amount VIP Coins ($tierName)! Balance: $newBalance"
            }.onFailure {
                _statusBanner.value = it.localizedMessage ?: "Failed to claim VIP coins"
            }
        }
    }

    fun shareNewPost(
        mediaKey: String,
        mediaType: String,
        caption: String,
        location: String,
        tag: String,
        vibeEmoji: String,
        existingPostId: String = ""
    ) {
        val deterministicPostId = existingPostId.ifBlank {
            "post_${java.util.UUID.randomUUID().toString().replace("-", "").take(16)}"
        }
        _mediaUploadState.value = com.example.data.MediaUploadState(
            status = "Pending",
            progressPercent = 2,
            targetType = "post",
            pendingDocId = deterministicPostId,
            sourceUri = mediaKey,
            mediaType = mediaType,
            caption = caption,
            location = location,
            tag = tag,
            vibeEmoji = vibeEmoji,
            errorMessage = null
        )
        viewModelScope.launch {
            val profile = currentProfileOrDefault()
            repository.createPost(
                authorProfile = profile,
                mediaKey = mediaKey,
                mediaType = mediaType,
                caption = caption,
                location = location,
                tag = tag,
                vibeEmoji = vibeEmoji,
                existingPostId = deterministicPostId,
                onProgress = { pct, statusLabel ->
                    _mediaUploadState.value = _mediaUploadState.value.copy(
                        status = statusLabel,
                        progressPercent = pct
                    )
                }
            )
                .onSuccess {
                    _mediaUploadState.value = com.example.data.MediaUploadState(
                        status = "Published",
                        progressPercent = 100,
                        targetType = "post"
                    )
                    _statusBanner.value = if (mediaType == "video") "Video clip permanently saved & shared to Noise!" else "Post permanently saved & shared to Noise!"
                    _selectedTab.value = if (mediaType == "video") NoiseTab.CLIPS else NoiseTab.HOME
                }
                .onFailure { e ->
                    _mediaUploadState.value = _mediaUploadState.value.copy(
                        status = "Failed",
                        errorMessage = e.localizedMessage ?: "Upload interrupted"
                    )
                    _statusBanner.value = e.localizedMessage ?: "Failed to upload post — tap Retry Upload"
                }
        }
    }

    fun retryFailedUpload() {
        val current = _mediaUploadState.value
        if (current.status != "Failed") return
        if (current.targetType == "story") {
            addStory(
                mediaKey = current.sourceUri,
                caption = current.caption,
                mediaType = current.mediaType,
                existingStoryId = current.pendingDocId
            )
        } else {
            shareNewPost(
                mediaKey = current.sourceUri,
                mediaType = current.mediaType,
                caption = current.caption,
                location = current.location,
                tag = current.tag,
                vibeEmoji = current.vibeEmoji,
                existingPostId = current.pendingDocId
            )
        }
    }

    fun toggleLike(post: Post) {
        viewModelScope.launch {
            repository.toggleLikePost(post)
        }
    }

    fun toggleSavePost(post: Post) {
        if (post.id.isBlank()) return
        val current = _savedPostIds.value
        val next = if (post.id in current) {
            _statusBanner.value = "Removed from Saved"
            current - post.id
        } else {
            _statusBanner.value = "Post saved to your profile!"
            current + post.id
        }
        _savedPostIds.value = next
        viewModelScope.launch {
            repository.toggleSavePostInCloud(post)
        }
    }

    fun recordReelView(post: Post) {
        if (post.id.isBlank()) return
        _sessionViewedReelIds.value = _sessionViewedReelIds.value + post.id
        viewModelScope.launch {
            repository.recordReelView(post)
        }
    }

    fun recordReelShare(post: Post) {
        if (post.id.isBlank()) return
        viewModelScope.launch {
            repository.recordReelShare(post)
        }
    }

    fun refreshTrendingReelsRankings() {
        viewModelScope.launch {
            val mode = discoveryGlobalConfigState.value.trendingAlgorithmMode
            repository.refreshTrendingReelsRankings(mode)
            _statusBanner.value = "Trending Reels rankings refreshed ✨"
        }
    }

    fun deletePost(postId: String) {
        viewModelScope.launch {
            repository.deletePost(postId).onSuccess {
                _savedPostIds.value = _savedPostIds.value - postId
                _statusBanner.value = "Post removed"
            }
        }
    }

    fun submitComment(post: Post, text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            val profile = currentProfileOrDefault()
            repository.addComment(post, profile.username, text)
        }
    }

    fun addStory(
        mediaKey: String,
        caption: String,
        mediaType: String = "photo",
        existingStoryId: String = ""
    ) {
        val mediaHash = mediaKey.ifBlank { "roses" }.hashCode().toUInt().toString(16)
        val deterministicStoryId = existingStoryId.ifBlank {
            "story_${currentUserId.take(12)}_$mediaHash"
        }
        _mediaUploadState.value = com.example.data.MediaUploadState(
            status = "Pending",
            progressPercent = 5,
            targetType = "story",
            pendingDocId = deterministicStoryId,
            sourceUri = mediaKey,
            mediaType = mediaType,
            caption = caption
        )
        viewModelScope.launch {
            val profile = currentProfileOrDefault()
            repository.createStory(
                username = profile.username,
                mediaKey = mediaKey,
                caption = caption,
                mediaType = mediaType,
                durationSec = if (mediaType == "video") 10L else 6L,
                authorAvatarKey = profile.avatarKey,
                existingStoryId = deterministicStoryId,
                onProgress = { pct, statusLabel ->
                    _mediaUploadState.value = _mediaUploadState.value.copy(
                        status = statusLabel,
                        progressPercent = pct
                    )
                }
            ).onSuccess {
                _mediaUploadState.value = com.example.data.MediaUploadState(
                    status = "Published",
                    progressPercent = 100,
                    targetType = "story"
                )
                _statusBanner.value = "Story uploaded to permanent cloud storage!"
            }.onFailure { e ->
                _mediaUploadState.value = _mediaUploadState.value.copy(
                    status = "Failed",
                    errorMessage = e.localizedMessage ?: "Story upload failed"
                )
                _statusBanner.value = e.localizedMessage ?: "Story upload failed"
            }
        }
    }

    fun addMultipleStories(mediaItems: List<Pair<String, String>>) {
        if (mediaItems.isEmpty()) return
        viewModelScope.launch {
            val profile = currentProfileOrDefault()
            val distinctItems = mediaItems.distinctBy { it.first }
            for ((uriStr, type) in distinctItems) {
                repository.createStory(
                    username = profile.username,
                    mediaKey = uriStr,
                    caption = "Shared from Gallery ✨",
                    mediaType = type,
                    durationSec = if (type == "video") 12L else 6L,
                    authorAvatarKey = profile.avatarKey,
                    onProgress = { pct, statusLabel ->
                        _mediaUploadState.value = com.example.data.MediaUploadState(
                            status = statusLabel,
                            progressPercent = pct,
                            targetType = "story",
                            sourceUri = uriStr,
                            mediaType = type,
                            caption = "Shared from Gallery ✨"
                        )
                    }
                )
            }
            _mediaUploadState.value = com.example.data.MediaUploadState(
                status = "Published",
                progressPercent = 100,
                targetType = "story"
            )
            _statusBanner.value = "Uploaded ${distinctItems.size} story item(s) to cloud storage!"
        }
    }

    fun deleteStory(story: Story) {
        viewModelScope.launch {
            repository.deleteStory(story.id)
                .onSuccess {
                    _statusBanner.value = "Story deleted"
                }
                .onFailure {
                    _statusBanner.value = it.localizedMessage ?: "Could not delete story"
                }
        }
    }

    fun markStoryViewed(story: Story) {
        viewModelScope.launch {
            repository.markStoryViewed(story)
        }
    }

    fun replyToStory(story: Story, text: String) {
        viewModelScope.launch {
            val profile = currentProfileOrDefault()
            val targetAccount = UserProfile(
                id = story.userId.ifBlank { currentUserId },
                userId = story.userId.ifBlank { currentUserId },
                username = story.username,
                displayName = story.username
            )
            repository.startOrGetConversation(targetAccount, profile).onSuccess { conv ->
                repository.sendDirectMessage(
                    conversation = conv,
                    senderName = profile.displayName,
                    text = "Replied to story: $text"
                )
                _statusBanner.value = "Sent story reply to @${story.username}"
            }
        }
    }

    fun startNewPartyRoom(title: String, subtitle: String) {
        _createPartyRoomError.value = null
        _isCreatingPartyRoom.value = false
        _showCreatePartyRoomPopup.value = true
    }

    fun createFullPartyRoomAndOpen(
        title: String,
        description: String,
        category: String,
        musicTrack: String,
        gameName: String,
        micMode: String,
        capacity: Long,
        privateRoom: Boolean,
        password: String,
        themeKey: String = "starry_mountain"
    ) {
        if (_isCreatingPartyRoom.value) return

        // Check Firebase Authentication state before every room creation request
        val authUser = runCatching { com.google.firebase.auth.FirebaseAuth.getInstance().currentUser }.getOrNull()
        if (authUser == null || authUser.uid.isBlank()) {
            val authMsg = "Authentication Required: Please sign in to your NOICE LIVE account before creating a voice room."
            _createPartyRoomError.value = authMsg
            _statusBanner.value = authMsg
            return
        }

        val cleanTitle = title.trim()
        val cleanDesc = description.trim()
        if (cleanTitle.isBlank()) {
            _createPartyRoomError.value = "Room Name is required"
            return
        }
        if (cleanDesc.isBlank()) {
            _createPartyRoomError.value = "Room Description is required"
            return
        }
        if (privateRoom && password.trim().isBlank()) {
            _createPartyRoomError.value = "Room Password is required for Private rooms"
            return
        }

        _isCreatingPartyRoom.value = true
        _createPartyRoomError.value = null
        _statusBanner.value = "Creating voice room '$cleanTitle'..."

        viewModelScope.launch {
            try {
                val profile = currentProfileOrDefault()
                val resolvedSubtitle = if (musicTrack.isBlank() || musicTrack.equals("None", ignoreCase = true)) {
                    "$category • Live Chatroom"
                } else {
                    "$category • $musicTrack"
                }
                repository.createFullPartyRoom(
                    hostUsername = profile.username,
                    title = cleanTitle,
                    subtitle = resolvedSubtitle,
                    category = category,
                    musicTrack = musicTrack,
                    gameName = gameName,
                    micMode = micMode,
                    capacity = capacity,
                    privateRoom = privateRoom,
                    password = password,
                    themeKey = themeKey.ifBlank { "starry_mountain" },
                    description = cleanDesc,
                    hostAvatarKey = profile.profilePictureUrl.ifBlank { profile.avatarKey }
                ).onSuccess { room ->
                    _isCreatingPartyRoom.value = false
                    _createPartyRoomError.value = null
                    _showCreatePartyRoomPopup.value = false
                    _showChatroomsListScreen.value = false
                    _fallbackPartyRoom.value = room
                    _activePartyRoomId.value = room.id
                    _statusBanner.value = "✓ Room '${room.title}' created! Host: @${room.hostUsername} (${room.resolvedCoverThemeLabel()})"
                }.onFailure { err ->
                    _isCreatingPartyRoom.value = false
                    val raw = err.localizedMessage ?: "Failed to create party room"
                    val msg = if (raw.contains("PERMISSION_DENIED", ignoreCase = true)) {
                        "Permission error while creating room: $raw. Please verify you are signed in."
                    } else {
                        raw
                    }
                    _createPartyRoomError.value = msg
                    _statusBanner.value = msg
                }
            } catch (e: Exception) {
                _isCreatingPartyRoom.value = false
                val msg = e.localizedMessage ?: "Failed to create party room"
                _createPartyRoomError.value = msg
                _statusBanner.value = msg
            }
        }
    }

    fun updatePartyRoom(room: PartyRoom) {
        _fallbackPartyRoom.value = room
        viewModelScope.launch {
            repository.updatePartyRoom(room).onSuccess { saved ->
                _fallbackPartyRoom.value = saved
            }
        }
    }

    fun syncPartyRoomYouTubeState(
        roomId: String,
        videoUrlOrId: String,
        videoTitle: String = "",
        isPlaying: Boolean,
        positionSec: Long
    ) {
        viewModelScope.launch {
            repository.syncPartyRoomYouTubeState(
                roomId = roomId,
                videoUrlOrId = videoUrlOrId,
                videoTitle = videoTitle,
                isPlaying = isPlaying,
                positionSec = positionSec
            ).onSuccess { updated ->
                _fallbackPartyRoom.value = updated
            }.onFailure { e ->
                _statusBanner.value = e.localizedMessage ?: "Could not update YouTube Watch Together"
            }
        }
    }

    fun joinOrMoveRoomSeat(roomId: String, seatNumber: Int, micMuted: Boolean) {
        viewModelScope.launch {
            val profile = currentProfileOrDefault()
            repository.joinOrMoveSeat(
                roomId = roomId,
                seatNumber = seatNumber,
                username = profile.username,
                avatarKey = profile.avatarKey,
                micMuted = micMuted
            )
                .onSuccess { updated ->
                    _fallbackPartyRoom.value = updated
                }
                .onFailure { e ->
                    _statusBanner.value = e.localizedMessage ?: "Could not join seat"
                }
        }
    }

    fun leaveRoomSeat(roomId: String) {
        viewModelScope.launch {
            repository.leaveSeat(roomId).onSuccess { updated ->
                _fallbackPartyRoom.value = updated
            }
        }
    }

    fun setRoomMicMuted(roomId: String, muted: Boolean) {
        viewModelScope.launch {
            repository.setSeatMicMuted(roomId, muted).onSuccess { updated ->
                _fallbackPartyRoom.value = updated
            }
        }
    }

    fun toggleRoomSeatLock(roomId: String, seatNumber: Int) {
        viewModelScope.launch {
            repository.toggleSeatLock(roomId, seatNumber).onSuccess { updated ->
                _fallbackPartyRoom.value = updated
            }
        }
    }

    fun sendRoomChatMessage(roomId: String, text: String) {
        viewModelScope.launch {
            val profile = currentProfileOrDefault()
            repository.sendRoomChatMessage(roomId, profile.username, text).onSuccess { updated ->
                _fallbackPartyRoom.value = updated
            }
        }
    }

    fun clearRoomChat(roomId: String) {
        viewModelScope.launch {
            val profile = currentProfileOrDefault()
            repository.clearRoomChat(roomId, profile.username).onSuccess { updated ->
                _fallbackPartyRoom.value = updated
            }
        }
    }

    fun assignRoomRole(roomId: String, targetUserId: String, newRole: RoomRole) {
        viewModelScope.launch {
            repository.assignRoomRole(roomId, targetUserId, newRole).onSuccess { updated ->
                _fallbackPartyRoom.value = updated
            }
        }
    }

    fun transferRoomOwnership(roomId: String, newOwnerId: String, newOwnerUsername: String) {
        viewModelScope.launch {
            repository.transferRoomOwnership(roomId, newOwnerId, newOwnerUsername).onSuccess { updated ->
                _fallbackPartyRoom.value = updated
            }
        }
    }

    fun toggleMuteParticipant(roomId: String, targetUserId: String) {
        viewModelScope.launch {
            repository.toggleMuteParticipant(roomId, targetUserId).onSuccess { updated ->
                _fallbackPartyRoom.value = updated
            }
        }
    }

    fun removeParticipantFromSlot(roomId: String, seatNumber: Int, targetUserId: String) {
        viewModelScope.launch {
            repository.removeParticipantFromSlot(roomId, seatNumber, targetUserId).onSuccess { updated ->
                _fallbackPartyRoom.value = updated
            }.onFailure { e ->
                _statusBanner.value = e.localizedMessage ?: "Could not remove user from slot"
            }
        }
    }

    fun kickParticipantFromRoom(roomId: String, targetUserId: String) {
        viewModelScope.launch {
            repository.kickParticipantFromRoom(roomId, targetUserId).onSuccess { updated ->
                _fallbackPartyRoom.value = updated
            }
        }
    }

    fun closeAndDeletePartyRoom(roomId: String) {
        viewModelScope.launch {
            repository.closeAndDeletePartyRoom(roomId).onSuccess {
                _activePartyRoomId.value = null
                _fallbackPartyRoom.value = null
                _statusBanner.value = "Room closed and deleted"
            }
        }
    }

    fun sendGiftToRoom(room: PartyRoom, amount: Long, targetSeat: Int = 1) {
        viewModelScope.launch {
            val profile = currentProfileOrDefault()
            repository.sendGiftToPartyRoom(room, profile, amount, targetSeat)
                .onSuccess { updatedRoom ->
                    _fallbackPartyRoom.value = updatedRoom
                }
                .onFailure {
                    _statusBanner.value = it.localizedMessage ?: "Not enough VIP coins"
                }
        }
    }

    fun markAllNotificationsAsRead() {
        val currentList = (notificationsState.value as? UiState.Success)?.data ?: return
        viewModelScope.launch {
            repository.markAllNotificationsRead(currentList)
        }
    }

    fun clearAllNotifications() {
        val currentList = (notificationsState.value as? UiState.Success)?.data ?: return
        viewModelScope.launch {
            repository.clearAllNotifications(currentList)
        }
    }

    fun sendChatMessage(text: String) {
        sendRichChatMessage(
            text = text,
            messageType = "text",
            mediaKey = "",
            mediaType = "photo",
            voiceDurationSec = 0L,
            voiceWaveform = "",
            replyToMessage = null
        )
    }

    fun sendRichChatMessage(
        text: String,
        messageType: String,
        mediaKey: String,
        mediaType: String,
        voiceDurationSec: Long,
        voiceWaveform: String,
        replyToMessage: DirectMessage?,
        voiceAudioBase64: String = "",
        voiceMimeType: String = "audio/wav",
        existingMessageId: String = "",
        onComplete: (Boolean) -> Unit = {}
    ) {
        val conv = _activeConversation.value ?: return
        val profile = currentProfileOrDefault()
        val msgId = existingMessageId.ifBlank {
            "msg_${java.util.UUID.randomUUID().toString().replace("-", "").take(16)}"
        }
        val cleanText = text.trim().ifBlank {
            when (messageType) {
                "voice" -> "🎤 Voice message (${voiceDurationSec.coerceAtLeast(1L)}s)"
                "image" -> "📷 Sent a photo"
                "video" -> "🎬 Sent a video"
                else -> "❤️"
            }
        }
        val receiverUid = conv.participantIds.firstOrNull { it.isNotBlank() && it != currentUserId }
            ?: conv.peerUid.ifBlank { currentUserId }
        val participants = listOf(currentUserId, receiverUid).filter { it.isNotBlank() }.distinct()
        val optimisticSendingMsg = DirectMessage(
            id = msgId,
            conversationId = conv.id,
            senderId = currentUserId,
            receiverId = receiverUid,
            senderName = profile.displayName.ifBlank { profile.username },
            senderVipBadgeLevel = profile.activeVipBadgeLevel,
            participantIds = participants,
            text = cleanText,
            messageContent = cleanText,
            messageType = messageType,
            mediaKey = mediaKey,
            mediaUrl = mediaKey,
            mediaType = mediaType,
            voiceDurationSec = voiceDurationSec,
            voiceWaveform = voiceWaveform,
            deliveryStatus = "sending",
            status = "sending",
            timeLabel = "Sending...",
            replyToMessageId = replyToMessage?.id ?: "",
            replyToSenderName = replyToMessage?.senderName ?: "",
            replyToText = replyToMessage?.text ?: "",
            read = false
        )
        // STATE 1: SENDING (Optimistic local queue while uploading/saving)
        val currentConvPending = _pendingDirectMessages.value[conv.id].orEmpty().filterNot { it.id == msgId }
        _pendingDirectMessages.value = _pendingDirectMessages.value + (conv.id to (currentConvPending + optimisticSendingMsg))

        viewModelScope.launch {
            repository.sendDirectMessage(
                conversation = conv,
                senderName = profile.username.ifBlank { profile.displayName },
                text = text,
                messageType = messageType,
                mediaKey = mediaKey,
                mediaType = mediaType,
                voiceDurationSec = voiceDurationSec,
                voiceWaveform = voiceWaveform,
                replyToMessage = replyToMessage,
                voiceAudioBase64 = voiceAudioBase64,
                voiceMimeType = voiceMimeType,
                senderAvatarKey = profile.profilePictureUrl.ifBlank { profile.avatarKey },
                senderVipBadgeLevel = profile.activeVipBadgeLevel,
                messageIdOverride = msgId
            ).onSuccess {
                val afterPending = _pendingDirectMessages.value[conv.id].orEmpty().filterNot { it.id == msgId }
                _pendingDirectMessages.value = _pendingDirectMessages.value + (conv.id to afterPending)
                _failedDirectPayloads.value = _failedDirectPayloads.value - msgId
                onComplete(true)
            }.onFailure { e ->
                val failedMsg = optimisticSendingMsg.copy(
                    deliveryStatus = "failed",
                    status = "failed",
                    timeLabel = "Failed • Tap to retry"
                )
                val afterPending = _pendingDirectMessages.value[conv.id].orEmpty().filterNot { it.id == msgId } + failedMsg
                _pendingDirectMessages.value = _pendingDirectMessages.value + (conv.id to afterPending)
                _failedDirectPayloads.value = _failedDirectPayloads.value + (
                    msgId to FailedDirectMessagePayload(
                        messageId = msgId,
                        conversation = conv,
                        text = text,
                        messageType = messageType,
                        mediaKey = mediaKey,
                        mediaType = mediaType,
                        voiceDurationSec = voiceDurationSec,
                        voiceWaveform = voiceWaveform,
                        replyToMessage = replyToMessage,
                        voiceAudioBase64 = voiceAudioBase64,
                        voiceMimeType = voiceMimeType
                    )
                )
                _statusBanner.value = e.localizedMessage ?: "Failed to send message — tap message to retry"
                onComplete(false)
            }
        }
    }

    fun retryDirectMessage(message: DirectMessage) {
        val payload = _failedDirectPayloads.value[message.id]
        if (payload != null) {
            if (_activeConversation.value?.id != payload.conversation.id) {
                _activeConversation.value = payload.conversation
            }
            sendRichChatMessage(
                text = payload.text,
                messageType = payload.messageType,
                mediaKey = payload.mediaKey,
                mediaType = payload.mediaType,
                voiceDurationSec = payload.voiceDurationSec,
                voiceWaveform = payload.voiceWaveform,
                replyToMessage = payload.replyToMessage,
                voiceAudioBase64 = payload.voiceAudioBase64,
                voiceMimeType = payload.voiceMimeType,
                existingMessageId = payload.messageId
            )
        } else {
            sendRichChatMessage(
                text = message.text,
                messageType = message.messageType,
                mediaKey = message.mediaKey,
                mediaType = message.mediaType,
                voiceDurationSec = message.voiceDurationSec,
                voiceWaveform = message.voiceWaveform,
                replyToMessage = null,
                existingMessageId = message.id
            )
        }
    }

    suspend fun fetchVoiceRecording(messageId: String) = repository.fetchVoiceRecording(messageId)

    fun setConversationTyping(typing: Boolean) {
        val conv = _activeConversation.value ?: return
        viewModelScope.launch {
            repository.setConversationTyping(conv.id, typing)
        }
    }

    fun toggleActiveConversationPeerOnline() {
        val conv = _activeConversation.value ?: return
        viewModelScope.launch {
            repository.toggleConversationPeerOnline(conv.id).onSuccess { updated ->
                if (updated != null) {
                    _activeConversation.value = updated
                }
            }
        }
    }

    fun acknowledgeMessagesDelivered(conversationId: String, messages: List<DirectMessage>) {
        viewModelScope.launch {
            repository.acknowledgeMessagesDelivered(conversationId, messages)
        }
    }

    fun markConversationMessagesSeen(messages: List<DirectMessage>) {
        val conv = _activeConversation.value ?: return
        viewModelScope.launch {
            repository.markMessagesSeen(conv, messages)
        }
    }

    fun reactToMessage(message: DirectMessage, emoji: String) {
        val conv = _activeConversation.value ?: return
        viewModelScope.launch {
            repository.reactToDirectMessage(conv.id, message, emoji)
        }
    }

    fun togglePinMessage(message: DirectMessage) {
        val conv = _activeConversation.value ?: return
        viewModelScope.launch {
            repository.togglePinDirectMessage(conv, message)
        }
    }

    fun deleteDirectMessage(message: DirectMessage) {
        val conv = _activeConversation.value ?: return
        viewModelScope.launch {
            repository.deleteDirectMessage(conv.id, message.id)
        }
    }

    fun markVoiceMessageSaved(message: DirectMessage) {
        val conv = _activeConversation.value ?: return
        viewModelScope.launch {
            repository.markVoiceMessageSaved(conv.id, message)
        }
    }

    fun startChatWithUser(targetUser: UserProfile) {
        viewModelScope.launch {
            val currentProfile = (userProfileState.value as? UiState.Success)?.data
            repository.startOrGetConversation(targetUser, currentProfile).onSuccess { conv ->
                _inspectedUserProfile.value = null
                _showDirectMessages.value = true
                _activeConversation.value = conv
            }
        }
    }

    fun updateConversationChatTheme(conversationId: String, chatThemeKey: String) {
        val currentConv = _activeConversation.value
        if (currentConv != null && currentConv.id == conversationId) {
            _activeConversation.value = currentConv.copy(chatThemeKey = chatThemeKey)
        }
        viewModelScope.launch {
            repository.updateConversationChatTheme(conversationId, chatThemeKey).onSuccess {
                _statusBanner.value = "Chat theme updated for both participants ✨"
            }.onFailure { e ->
                _statusBanner.value = e.localizedMessage ?: "Could not update chat theme"
            }
        }
    }

    fun updateThemeMode(themeMode: String) {
        viewModelScope.launch {
            repository.updateUserThemeMode(themeMode).onSuccess { normalized ->
                _statusBanner.value = if (normalized == "light") "Switched to Light Mode ☀️" else "Switched to Dark Mode 🌙"
            }
        }
    }

    fun startOneOnOneCall(conversation: Conversation, callType: String) {
        val callerProfile = currentProfileOrDefault()
        viewModelScope.launch {
            repository.startCall(
                conversation = conversation,
                callerProfile = callerProfile,
                callType = callType
            ).onSuccess { callDoc ->
                _fallbackCallSession.value = callDoc
            }.onFailure { e ->
                _statusBanner.value = e.localizedMessage ?: "Could not start call"
            }
        }
    }

    fun respondToCall(call: CallSessionDoc, accept: Boolean) {
        if (!accept) {
            if (_fallbackCallSession.value?.id == call.id) {
                _fallbackCallSession.value = null
            }
        } else {
            _fallbackCallSession.value = call.copy(status = "accepted")
        }
        viewModelScope.launch {
            repository.respondToCall(call.id, accept).onSuccess { updated ->
                _fallbackCallSession.value = if (accept) updated else null
            }.onFailure {
                if (!accept) _fallbackCallSession.value = null
            }
        }
    }

    fun updateCallMediaState(
        callId: String,
        micMuted: Boolean,
        cameraEnabled: Boolean,
        frontCamera: Boolean
    ) {
        val currentFallback = _fallbackCallSession.value
        if (currentFallback != null && currentFallback.id == callId) {
            val isCaller = currentFallback.callerId == currentUserId
            _fallbackCallSession.value = if (isCaller) {
                currentFallback.copy(
                    callerMicMuted = micMuted,
                    callerCameraEnabled = cameraEnabled,
                    callerFrontCamera = frontCamera
                )
            } else {
                currentFallback.copy(
                    calleeMicMuted = micMuted,
                    calleeCameraEnabled = cameraEnabled,
                    calleeFrontCamera = frontCamera
                )
            }
        }
        viewModelScope.launch {
            repository.updateCallMediaState(
                callId = callId,
                micMuted = micMuted,
                cameraEnabled = cameraEnabled,
                frontCamera = frontCamera
            )
        }
    }

    fun endCall(callId: String, durationSec: Long) {
        _fallbackCallSession.value = null
        viewModelScope.launch {
            repository.endCall(callId, durationSec)
        }
    }

    fun followUser(targetUser: UserProfile) {
        viewModelScope.launch {
            val profile = currentProfileOrDefault()
            repository.followAccount(targetUser, profile).onSuccess { nowFollowing ->
                _statusBanner.value = if (nowFollowing) {
                    "Following @${targetUser.username}"
                } else {
                    "Unfollowed @${targetUser.username}"
                }
                val currentInspected = _inspectedUserProfile.value
                if (currentInspected != null && (currentInspected.userId == targetUser.userId || currentInspected.id == targetUser.id)) {
                    val delta = if (nowFollowing) 1L else -1L
                    _inspectedUserProfile.value = currentInspected.copy(
                        followersCount = (currentInspected.followersCount + delta).coerceAtLeast(0L)
                    )
                }
            }.onFailure { e ->
                _statusBanner.value = e.localizedMessage ?: "Could not update follow status"
            }
        }
    }

    fun saveProfileEdits(username: String, displayName: String, bio: String, website: String, avatarKey: String) {
        viewModelScope.launch {
            val current = currentProfileOrDefault()
            val updated = current.copy(
                username = username.trim().removePrefix("@").ifBlank { current.username },
                displayName = displayName.trim().ifBlank { current.displayName },
                bio = bio.trim(),
                website = website.trim().ifBlank { "noise.app" },
                avatarKey = avatarKey.ifBlank { current.avatarKey }
            )
            repository.createOrUpdateUserProfile(updated).onSuccess {
                _statusBanner.value = "Profile updated!"
            }.onFailure { e ->
                _statusBanner.value = e.localizedMessage ?: "Could not update profile"
            }
        }
    }

    fun purchaseVipBadge(vipLevel: Int) {
        viewModelScope.launch {
            repository.purchaseVipBadge(vipLevel)
                .onSuccess { updated ->
                    _statusBanner.value = "Unlocked & Equipped ${updated.activeVipBadgeName}!"
                }
                .onFailure { e ->
                    _statusBanner.value = e.localizedMessage ?: "Failed to purchase VIP badge"
                }
        }
    }

    fun selectActiveVipBadge(vipLevel: Int) {
        viewModelScope.launch {
            repository.selectActiveVipBadge(vipLevel)
                .onSuccess { updated ->
                    _statusBanner.value = if (vipLevel > 0) {
                        "Equipped ${updated.activeVipBadgeName}!"
                    } else {
                        "VIP Badge hidden"
                    }
                }
                .onFailure { e ->
                    _statusBanner.value = e.localizedMessage ?: "Could not equip VIP badge"
                }
        }
    }

    // =========================================================================
    // AUTOMATIC DISCOVERY & CONTENT IMPORT ACTIONS
    // =========================================================================

    fun selectDiscoveryCategory(category: DiscoveryCategory) {
        _selectedDiscoveryCategory.value = category
        triggerDiscoverySync(category, forceRefresh = false)
    }

    fun refreshDiscoveryCategory(forceRefresh: Boolean = true) {
        triggerDiscoverySync(_selectedDiscoveryCategory.value, forceRefresh = forceRefresh)
    }

    private fun triggerDiscoverySync(category: DiscoveryCategory, forceRefresh: Boolean) {
        viewModelScope.launch {
            _isDiscoverySyncing.value = true
            try {
                repository.syncDiscoveryCategory(category, forceRefresh)
                    .onSuccess { savedCount ->
                        if (forceRefresh && savedCount > 0) {
                            _statusBanner.value = "Imported $savedCount verified ${category.label} updates ✨"
                        }
                    }
            } finally {
                _isDiscoverySyncing.value = false
            }
        }
    }

    fun setDiscoveryAdminPanelVisible(visible: Boolean) {
        _showDiscoveryAdminPanel.value = visible
    }

    fun setActiveViewingDiscoveryItem(item: DiscoveryContentItem?) {
        _activeViewingDiscoveryItem.value = item
        if (item != null) {
            viewModelScope.launch {
                repository.recordDiscoveryItemView(item.id)
            }
        }
    }

    fun toggleLikeDiscoveryItem(item: DiscoveryContentItem) {
        viewModelScope.launch {
            repository.toggleLikeDiscoveryItem(item)
        }
    }

    fun toggleSaveDiscoveryItem(item: DiscoveryContentItem) {
        viewModelScope.launch {
            repository.toggleSaveDiscoveryItem(item).onSuccess { saved ->
                _statusBanner.value = if (saved) "Saved to your bookmarks" else "Removed from bookmarks"
            }
        }
    }

    fun recordDiscoveryItemShare(item: DiscoveryContentItem) {
        viewModelScope.launch {
            repository.recordDiscoveryItemShare(item.id)
        }
    }

    fun setDiscoveryCategoryEnabled(categoryKey: String, enabled: Boolean) {
        viewModelScope.launch {
            repository.setDiscoveryCategoryEnabled(categoryKey, enabled).onSuccess {
                _statusBanner.value = if (enabled) "Category '$categoryKey' enabled" else "Category '$categoryKey' disabled"
            }
        }
    }

    fun updateDiscoveryRefreshIntervalMinutes(intervalMinutes: Long) {
        viewModelScope.launch {
            repository.updateDiscoveryRefreshIntervalMinutes(intervalMinutes).onSuccess {
                _statusBanner.value = "Auto-refresh interval set to ${intervalMinutes.coerceIn(15L, 30L)} minutes"
            }
        }
    }

    fun addCustomDiscoverySource(
        categoryKey: String,
        sourceName: String,
        feedUrl: String,
        contentType: String
    ) {
        viewModelScope.launch {
            repository.addCustomDiscoverySource(categoryKey, sourceName, feedUrl, contentType)
                .onSuccess { src ->
                    _statusBanner.value = "Added trusted source: ${src.sourceName}"
                    triggerDiscoverySync(DiscoveryCategory.fromKey(categoryKey), forceRefresh = true)
                }
                .onFailure { e ->
                    _statusBanner.value = e.localizedMessage ?: "Failed to add source"
                }
        }
    }

    fun toggleDiscoverySourceEnabled(source: DiscoverySourceConfig, enabled: Boolean) {
        viewModelScope.launch {
            repository.toggleDiscoverySourceEnabled(source, enabled)
        }
    }

    fun deleteDiscoverySource(sourceId: String) {
        viewModelScope.launch {
            repository.deleteDiscoverySource(sourceId).onSuccess {
                _statusBanner.value = "Removed content source"
            }
        }
    }

    fun moderateDiscoveryItem(itemId: String, newStatus: String, notes: String) {
        viewModelScope.launch {
            repository.moderateDiscoveryItem(itemId, newStatus, notes).onSuccess {
                _statusBanner.value = "Item marked as ${newStatus.uppercase()}"
            }
        }
    }

    fun deleteDiscoveryItem(itemId: String, blockDomain: String = "") {
        viewModelScope.launch {
            repository.deleteDiscoveryItem(itemId, blockDomain).onSuccess {
                _statusBanner.value = "Deleted discovery item"
            }
        }
    }

    // =========================================================================
    // AUTOMATIC KERALA NEWS ACTIONS
    // =========================================================================

    fun setShowKeralaNewsSection(visible: Boolean) {
        _showKeralaNewsSection.value = visible
        if (visible) {
            triggerKeralaNewsSync(_selectedKeralaCategory.value, forceRefresh = false)
        }
    }

    fun selectKeralaCategory(category: KeralaNewsCategory) {
        _selectedKeralaCategory.value = category
        triggerKeralaNewsSync(category, forceRefresh = false)
    }

    fun selectKeralaDistrict(district: String) {
        _selectedKeralaDistrict.value = district
        if (district != "All Districts" && _selectedKeralaCategory.value != KeralaNewsCategory.KERALA_DISTRICT) {
            triggerKeralaNewsSync(KeralaNewsCategory.KERALA_DISTRICT, forceRefresh = false)
        }
    }

    fun selectKeralaLanguage(language: String) {
        _selectedKeralaLanguage.value = language
    }

    fun refreshKeralaNews(forceRefresh: Boolean = true) {
        triggerKeralaNewsSync(_selectedKeralaCategory.value, forceRefresh = forceRefresh)
    }

    private fun triggerKeralaNewsSync(category: KeralaNewsCategory, forceRefresh: Boolean) {
        viewModelScope.launch {
            _isKeralaNewsSyncing.value = true
            try {
                repository.syncKeralaNewsCategory(category, forceRefresh)
                    .onSuccess { savedCount ->
                        if (savedCount > 0) {
                            if (category == KeralaNewsCategory.KERALA_BREAKING &&
                                discoveryGlobalConfigState.value.keralaNewsNotificationsEnabled
                            ) {
                                _statusBanner.value = "🚨 Kerala Breaking News: $savedCount new verified alert(s)!"
                            } else if (forceRefresh) {
                                _statusBanner.value = "🌴 Updated $savedCount verified ${category.label} article(s)"
                            }
                        }
                    }
            } finally {
                _isKeralaNewsSyncing.value = false
            }
        }
    }

    fun setKeralaCategoryEnabled(keralaCategoryKey: String, enabled: Boolean) {
        viewModelScope.launch {
            repository.setKeralaCategoryEnabled(keralaCategoryKey, enabled).onSuccess {
                _statusBanner.value = if (enabled) {
                    "Kerala category '$keralaCategoryKey' enabled"
                } else {
                    "Kerala category '$keralaCategoryKey' disabled"
                }
            }
        }
    }

    fun updateKeralaAndReelsConfig(
        keralaNewsNotificationsEnabled: Boolean? = null,
        trendingReelsNotificationsEnabled: Boolean? = null,
        trendingAlgorithmMode: String? = null
    ) {
        viewModelScope.launch {
            repository.updateKeralaAndReelsConfig(
                keralaNewsNotificationsEnabled = keralaNewsNotificationsEnabled,
                trendingReelsNotificationsEnabled = trendingReelsNotificationsEnabled,
                trendingAlgorithmMode = trendingAlgorithmMode
            ).onSuccess {
                _statusBanner.value = "Updated Kerala News & Trending Reels settings ✨"
            }
        }
    }

    // =========================================================================
    // SUPER OWNER FULL CONTROL & CHATROOM CLEANUP ACTIONS
    // =========================================================================

    fun openOwnerControlPanel(initialTab: Int = 0) {
        _ownerControlInitialTab.value = initialTab.coerceIn(0, 3)
        _showOwnerControlPanel.value = true
    }

    fun setOwnerControlPanelVisible(visible: Boolean) {
        _showOwnerControlPanel.value = visible
    }

    fun ownerDeleteSingleChatroom(roomId: String) {
        if (roomId.isBlank()) return
        if (_activePartyRoomId.value == roomId) {
            _activePartyRoomId.value = null
            _fallbackPartyRoom.value = null
        }
        viewModelScope.launch {
            repository.ownerDeleteSingleChatroom(roomId).onSuccess {
                _statusBanner.value = "Chatroom ($roomId) permanently deleted"
            }.onFailure { e ->
                _statusBanner.value = e.localizedMessage ?: "Failed to delete chatroom"
            }
        }
    }

    fun ownerDeleteSelectedChatrooms(roomIds: List<String>) {
        if (roomIds.isEmpty()) return
        if (_activePartyRoomId.value in roomIds) {
            _activePartyRoomId.value = null
            _fallbackPartyRoom.value = null
        }
        viewModelScope.launch {
            repository.ownerDeleteSelectedChatrooms(roomIds).onSuccess { count ->
                _statusBanner.value = "Permanently deleted $count selected chatroom(s)"
            }.onFailure { e ->
                _statusBanner.value = e.localizedMessage ?: "Failed to delete selected chatrooms"
            }
        }
    }

    fun ownerDeleteAllChatrooms() {
        _activePartyRoomId.value = null
        _fallbackPartyRoom.value = null
        viewModelScope.launch {
            repository.ownerDeleteAllChatrooms().onSuccess { count ->
                _statusBanner.value = "All $count chatroom(s) permanently deleted from Firebase"
            }.onFailure { e ->
                _statusBanner.value = e.localizedMessage ?: "Failed to delete all chatrooms"
            }
        }
    }

    fun ownerPurgeDemoAndDuplicateChatrooms() {
        viewModelScope.launch {
            repository.ownerPurgeDemoAndDuplicateChatrooms().onSuccess { count ->
                _statusBanner.value = if (count > 0) {
                    "Purged $count demo/duplicate/bugged chatroom(s)"
                } else {
                    "Verified: 0 demo or duplicate chatrooms found"
                }
            }.onFailure { e ->
                _statusBanner.value = e.localizedMessage ?: "Failed to purge demo chatrooms"
            }
        }
    }

    fun ownerCleanupAllDemoData() {
        viewModelScope.launch {
            repository.cleanupDemoAndDuplicateData(currentUserId)
            _statusBanner.value = "Fake, demo, and duplicate records cleaned across Firebase"
        }
    }

    fun ownerDeleteGameGroup(gameRoomId: String) {
        if (_activeGameRoomId.value == gameRoomId) {
            _activeGameRoomId.value = null
            _fallbackGameRoom.value = null
        }
        viewModelScope.launch {
            repository.ownerDeleteGameGroup(gameRoomId).onSuccess {
                _statusBanner.value = "Game group ($gameRoomId) deleted by Super Owner"
            }.onFailure { e ->
                _statusBanner.value = e.localizedMessage ?: "Could not delete group"
            }
        }
    }

    fun ownerUpdateUserAccountStatus(targetUser: UserProfile, newStatus: String, reason: String) {
        viewModelScope.launch {
            repository.ownerUpdateUserAccountStatus(
                targetUser = targetUser,
                newStatus = newStatus,
                reason = reason
            ).onSuccess {
                _statusBanner.value = "Updated @${targetUser.username} status to ${newStatus.uppercase()}"
                val currentInspected = _inspectedUserProfile.value
                if (currentInspected != null && currentInspected.id == targetUser.id) {
                    _inspectedUserProfile.value = currentInspected.copy(
                        accountStatus = newStatus,
                        suspendedReason = reason
                    )
                }
            }.onFailure { e ->
                _statusBanner.value = e.localizedMessage ?: "Could not update user status"
            }
        }
    }

    fun submitContentReport(
        targetType: String,
        targetId: String,
        targetOwnerUid: String,
        targetSummary: String,
        reason: String
    ) {
        viewModelScope.launch {
            val profile = currentProfileOrDefault()
            repository.submitContentReport(
                reporterUsername = profile.username,
                targetType = targetType,
                targetId = targetId,
                targetOwnerUid = targetOwnerUid,
                targetSummary = targetSummary,
                reason = reason
            ).onSuccess {
                _statusBanner.value = "Report submitted to Super Owner for review"
            }.onFailure { e ->
                _statusBanner.value = e.localizedMessage ?: "Could not submit report"
            }
        }
    }

    fun ownerResolveReport(
        report: ContentReportDoc,
        newStatus: String,
        resolutionNotes: String,
        deleteTargetContent: Boolean
    ) {
        viewModelScope.launch {
            repository.ownerResolveReport(
                report = report,
                newStatus = newStatus,
                resolutionNotes = resolutionNotes,
                deleteTargetContent = deleteTargetContent
            ).onSuccess {
                _statusBanner.value = "Report marked as ${newStatus.uppercase()}"
            }.onFailure { e ->
                _statusBanner.value = e.localizedMessage ?: "Could not resolve report"
            }
        }
    }

    // =========================================================================
    // CENTRAL AI ADMIN & LIVE UPDATE ACTIONS
    // =========================================================================

    fun setCentralAiAdminModalVisible(visible: Boolean) {
        _showCentralAiAdminModal.value = visible
        if (!visible) {
            _aiAdminStatusMessage.value = null
        }
    }

    fun openDynamicPage(page: com.example.data.DynamicAppPage?) {
        _activeDynamicPage.value = page
    }

    fun generateAiAdminProposal(prompt: String) {
        if (_isGeneratingAiProposal.value) return
        _isGeneratingAiProposal.value = true
        _aiAdminStatusMessage.value = null
        viewModelScope.launch {
            val currentCfg = productionAppConfigState.value
            val currentPages = dynamicPagesState.value
            repository.generateAiAdminProposal(
                prompt = prompt,
                currentConfig = currentCfg,
                existingPages = currentPages
            ).onSuccess { proposal ->
                _currentAiProposal.value = proposal
                _aiAdminStatusMessage.value = "AI inspected ${proposal.inspectedComponents.size} component(s) & validated changes."
            }.onFailure { e ->
                _aiAdminStatusMessage.value = e.localizedMessage ?: "Could not generate AI proposal"
            }
            _isGeneratingAiProposal.value = false
        }
    }

    fun togglePreviewAiProposal(proposal: com.example.data.AiAdminProposal?, enabled: Boolean) {
        if (!enabled || proposal == null) {
            com.example.data.CentralLiveUpdateRuntime.clearLocalPreview()
            _aiAdminStatusMessage.value = "Returned to live production configuration."
            return
        }
        val previewPages = when (proposal.dynamicPageOperation) {
            "add_or_update" -> {
                val newPage = proposal.proposedDynamicPage
                if (newPage != null) {
                    dynamicPagesState.value.filter { it.pageKey != newPage.pageKey } + newPage
                } else {
                    dynamicPagesState.value
                }
            }
            "remove" -> dynamicPagesState.value.filter { it.pageKey != proposal.removeDynamicPageKey }
            else -> dynamicPagesState.value
        }
        com.example.data.CentralLiveUpdateRuntime.enableLocalPreview(
            config = proposal.proposedConfig,
            previewPages = previewPages
        )
        _aiAdminStatusMessage.value = "Local Preview Active — Inspect changes before publishing to all users."
    }

    fun publishAiProposalToAllUsers(proposal: com.example.data.AiAdminProposal) {
        if (_isPublishingLiveUpdate.value) return
        _isPublishingLiveUpdate.value = true
        viewModelScope.launch {
            repository.publishAiProposalToAllUsers(
                proposal = proposal,
                currentProductionConfig = productionAppConfigState.value,
                currentDynamicPages = dynamicPagesState.value
            ).onSuccess { updatedCfg ->
                _currentAiProposal.value = null
                _aiAdminStatusMessage.value = "✅ Published ${updatedCfg.versionLabel} live to all connected users!"
                _statusBanner.value = "⚡ Live Update ${updatedCfg.versionLabel} Published to All Users!"
            }.onFailure { e ->
                _aiAdminStatusMessage.value = e.localizedMessage ?: "Failed to publish live update"
            }
            _isPublishingLiveUpdate.value = false
        }
    }

    fun saveDynamicPageAndPublish(page: com.example.data.DynamicAppPage) {
        viewModelScope.launch {
            repository.saveDynamicPageAndPublish(
                page = page,
                currentProductionConfig = productionAppConfigState.value,
                currentDynamicPages = dynamicPagesState.value
            ).onSuccess { saved ->
                _aiAdminStatusMessage.value = "✅ Published dynamic page '${saved.title}' to all users!"
                _statusBanner.value = "✨ New Page '${saved.title}' is now live for all users!"
            }.onFailure { e ->
                _aiAdminStatusMessage.value = e.localizedMessage ?: "Failed to publish dynamic page"
            }
        }
    }

    fun deleteDynamicPageAndPublish(pageKey: String) {
        viewModelScope.launch {
            repository.deleteDynamicPageAndPublish(
                pageKey = pageKey,
                currentProductionConfig = productionAppConfigState.value,
                currentDynamicPages = dynamicPagesState.value
            ).onSuccess {
                _aiAdminStatusMessage.value = "✅ Removed dynamic page '/$pageKey' from live navigation."
            }.onFailure { e ->
                _aiAdminStatusMessage.value = e.localizedMessage ?: "Failed to remove dynamic page"
            }
        }
    }

    fun createManualCentralBackup(reason: String) {
        viewModelScope.launch {
            repository.createCentralBackupSnapshot(
                currentConfig = productionAppConfigState.value,
                currentPages = dynamicPagesState.value,
                reason = reason
            ).onSuccess { backup ->
                _aiAdminStatusMessage.value = "💾 Saved backup snapshot '${backup.backupLabel}'"
            }.onFailure { e ->
                _aiAdminStatusMessage.value = e.localizedMessage ?: "Failed to create backup"
            }
        }
    }

    val isSuperOwner: Boolean
        get() {
            val profile = (userProfileState.value as? UiState.Success)?.data
                ?: UserProfile(id = currentUserId, userId = currentUserId)
            return SuperOwnerControlManager.isCurrentUserSuperOwner(
                userProfile = profile,
                registryDoc = superOwnerRegistryState.value
            )
        }

    val centralDynamicPages: StateFlow<List<com.example.data.DynamicAppPage>>
        get() = dynamicPagesState

    val centralVersionHistory: StateFlow<List<com.example.data.CentralVersionDoc>>
        get() = centralVersionHistoryState

    val centralBackupsList: StateFlow<List<com.example.data.CentralConfigBackupDoc>>
        get() = centralBackupsState

    val centralAiIsGenerating: Boolean
        get() = _isGeneratingAiProposal.value

    val centralAiIsPublishing: Boolean
        get() = _isPublishingLiveUpdate.value

    val centralAiCurrentProposal: com.example.data.AiAdminProposal?
        get() = _currentAiProposal.value

    val centralAiStatusMessage: String?
        get() = _aiAdminStatusMessage.value

    fun runCentralAiAdminCommand(prompt: String) {
        if (_isGeneratingAiProposal.value) return
        _isGeneratingAiProposal.value = true
        _aiAdminStatusMessage.value = null
        viewModelScope.launch {
            val currentCfg = com.example.data.CentralLiveUpdateRuntime.effectiveConfig
            val currentPages = dynamicPagesState.value
            repository.generateAiAdminProposal(
                prompt = prompt,
                currentConfig = currentCfg,
                existingPages = currentPages
            ).onSuccess { proposal ->
                _currentAiProposal.value = proposal
                togglePreviewAiProposal(proposal, enabled = true)
                _aiAdminStatusMessage.value = "Gemini AI applied live preview: ${proposal.summaryOfChanges}"
            }.onFailure { e ->
                _aiAdminStatusMessage.value = e.localizedMessage ?: "Could not generate AI proposal"
            }
            _isGeneratingAiProposal.value = false
        }
    }

    fun publishCentralAiProposalToAllUsers() {
        val proposal = _currentAiProposal.value ?: return
        publishAiProposalToAllUsers(proposal)
    }

    fun cancelCentralAiPreview() {
        _currentAiProposal.value = null
        togglePreviewAiProposal(null, enabled = false)
    }

    fun publishDirectOwnerVisualProposal(
        proposal: com.example.data.AiAdminProposal,
        onSuccessDone: () -> Unit = {}
    ) {
        if (_isPublishingLiveUpdate.value) return
        _isPublishingLiveUpdate.value = true
        viewModelScope.launch {
            repository.publishAiProposalToAllUsers(
                proposal = proposal,
                currentProductionConfig = productionAppConfigState.value,
                currentDynamicPages = dynamicPagesState.value
            ).onSuccess { updatedCfg ->
                _currentAiProposal.value = null
                _aiAdminStatusMessage.value = "✅ Published ${updatedCfg.versionLabel} live to all connected devices!"
                _statusBanner.value = "⚡ Live Update ${updatedCfg.versionLabel} Published to All Devices!"
                onSuccessDone()
            }.onFailure { e ->
                _aiAdminStatusMessage.value = e.localizedMessage ?: "Failed to publish live update"
                _statusBanner.value = e.localizedMessage ?: "Failed to publish live update"
            }
            _isPublishingLiveUpdate.value = false
        }
    }

    fun saveAndPublishDynamicPage(
        title: String,
        subtitle: String,
        accentHex: String = "#8525FF",
        secondaryHex: String = "#E52BEB"
    ) {
        val pageKey = title.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_').ifBlank { "custom_page" }
        val page = com.example.data.CentralAiAdminManager.createStructuredDynamicPage(
            pageKey = pageKey,
            title = title,
            subtitle = subtitle,
            accentHex = accentHex,
            secondaryHex = secondaryHex
        )
        saveDynamicPageAndPublish(page)
    }

    fun deleteAndPublishDynamicPage(pageKey: String) {
        deleteDynamicPageAndPublish(pageKey)
    }

    fun rollbackCentralConfigToBackup(backup: com.example.data.CentralConfigBackupDoc) {
        rollbackToCentralBackup(backup)
    }

    fun rollbackToCentralBackup(backup: com.example.data.CentralConfigBackupDoc) {
        viewModelScope.launch {
            repository.rollbackCentralConfigToBackup(
                backup = backup,
                currentProductionConfig = productionAppConfigState.value,
                currentDynamicPages = dynamicPagesState.value
            ).onSuccess { restored ->
                _aiAdminStatusMessage.value = "⏪ Rolled back production to ${backup.versionLabel} (${restored.versionLabel})"
                _developerStudioStatusMessage.value = "⏪ Rolled back production config to ${backup.versionLabel}"
                _statusBanner.value = "⏪ Rolled back live configuration to ${backup.versionLabel}"
            }.onFailure { e ->
                _aiAdminStatusMessage.value = e.localizedMessage ?: "Rollback failed"
            }
        }
    }

    // =========================================================================
    // OWNER ONLY GEMINI AI DEVELOPER STUDIO ACTIONS
    // =========================================================================

    fun setGeminiDeveloperStudioVisible(visible: Boolean) {
        _showGeminiDeveloperStudioModal.value = visible
        if (visible && developerBugFindingsState.value.isEmpty()) {
            runDeveloperBugScanner()
        }
    }

    fun sendDeveloperChatPrompt(prompt: String) {
        if (_isDeveloperStudioBusy.value) return
        _isDeveloperStudioBusy.value = true
        _developerStudioStatusMessage.value = null
        viewModelScope.launch {
            repository.sendDeveloperChatPrompt(
                prompt = prompt,
                workspaceFiles = developerWorkspaceFilesState.value,
                currentConfig = productionAppConfigState.value
            ).onSuccess { msg ->
                _developerStudioStatusMessage.value = "✅ Gemini generated complete code for ${msg.generatedCodeFilePath}"
                // Also populate currentDeveloperCodeProposal so the Owner can immediately inspect diff & deploy
                repository.generateDeveloperCodePatch(
                    prompt = prompt,
                    workspaceFiles = developerWorkspaceFilesState.value,
                    currentConfig = productionAppConfigState.value,
                    existingPages = dynamicPagesState.value
                ).onSuccess { patch ->
                    _currentDeveloperCodeProposal.value = patch
                }
            }.onFailure { e ->
                _developerStudioStatusMessage.value = e.localizedMessage ?: "Gemini chat request failed"
            }
            _isDeveloperStudioBusy.value = false
        }
    }

    fun generateDeveloperCodePatch(prompt: String) {
        if (_isDeveloperStudioBusy.value) return
        _isDeveloperStudioBusy.value = true
        _developerStudioStatusMessage.value = null
        viewModelScope.launch {
            repository.generateDeveloperCodePatch(
                prompt = prompt,
                workspaceFiles = developerWorkspaceFilesState.value,
                currentConfig = productionAppConfigState.value,
                existingPages = dynamicPagesState.value
            ).onSuccess { patch ->
                _currentDeveloperCodeProposal.value = patch
                _developerStudioStatusMessage.value = "✅ Generated complete code & unified diff for ${patch.targetFilePath} (Build & Tests Verified)"
            }.onFailure { e ->
                _developerStudioStatusMessage.value = e.localizedMessage ?: "Code generation failed"
            }
            _isDeveloperStudioBusy.value = false
        }
    }

    fun approveAndDeployDeveloperCodePatch(proposal: com.example.data.AiCodePatchProposal) {
        if (_isDeveloperStudioBusy.value) return
        _isDeveloperStudioBusy.value = true
        viewModelScope.launch {
            repository.approveMergeAndDeployCodePatch(
                proposal = proposal,
                currentProductionConfig = productionAppConfigState.value,
                currentDynamicPages = dynamicPagesState.value
            ).onSuccess { deployDoc ->
                _currentDeveloperCodeProposal.value = null
                _developerStudioStatusMessage.value = "🚀 Approved, merged (${deployDoc.commitSha}) & deployed ${deployDoc.deploymentVersion}!"
                _statusBanner.value = "🚀 Gemini AI Update ${deployDoc.deploymentVersion} Deployed!"
                _developerBuildTestReport.value = com.example.data.GeminiDeveloperStudioManager.runBuildAndTestVerificationSuite(
                    developerWorkspaceFilesState.value,
                    deployDoc.branchName
                )
            }.onFailure { e ->
                _developerStudioStatusMessage.value = e.localizedMessage ?: "Deployment failed"
            }
            _isDeveloperStudioBusy.value = false
        }
    }

    fun runDeveloperBugScanner() {
        viewModelScope.launch {
            _isDeveloperStudioBusy.value = true
            repository.runDeveloperBugScanner(
                workspaceFiles = developerWorkspaceFilesState.value
            ).onSuccess { findings ->
                _developerStudioStatusMessage.value = "🔍 Scanned project source code: ${findings.size} verified fix(es) ready for review."
            }.onFailure { e ->
                _developerStudioStatusMessage.value = e.localizedMessage ?: "Bug scan failed"
            }
            _isDeveloperStudioBusy.value = false
        }
    }

    fun approveAndDeployDeveloperBugFix(finding: com.example.data.BugScanFindingDoc) {
        if (_isDeveloperStudioBusy.value) return
        _isDeveloperStudioBusy.value = true
        viewModelScope.launch {
            repository.approveAndDeployBugFix(
                finding = finding,
                currentProductionConfig = productionAppConfigState.value,
                currentDynamicPages = dynamicPagesState.value
            ).onSuccess { deployDoc ->
                _developerStudioStatusMessage.value = "✅ Bug fix approved & deployed (${deployDoc.commitSha})!"
                _statusBanner.value = "✅ Deployed verified bug fix: ${finding.title}"
            }.onFailure { e ->
                _developerStudioStatusMessage.value = e.localizedMessage ?: "Failed to deploy bug fix"
            }
            _isDeveloperStudioBusy.value = false
        }
    }

    fun saveEditedFileToDevBranch(
        branchName: String,
        filePath: String,
        newCode: String,
        previousCode: String,
        commitMessage: String
    ) {
        viewModelScope.launch {
            _isDeveloperStudioBusy.value = true
            repository.saveEditedFileToDevBranch(
                branchName = branchName,
                filePath = filePath,
                newCode = newCode,
                previousCode = previousCode,
                commitMessage = commitMessage,
                githubConfig = githubConfigState.value
            ).onSuccess { commit ->
                _developerStudioStatusMessage.value = "✅ Committed [${commit.commitSha}] to branch '${commit.branchName}' (Build & Security Verified)"
                _developerBuildTestReport.value = com.example.data.GeminiDeveloperStudioManager.runBuildAndTestVerificationSuite(
                    developerWorkspaceFilesState.value,
                    commit.branchName
                )
            }.onFailure { e ->
                _developerStudioStatusMessage.value = e.localizedMessage ?: "Commit failed"
            }
            _isDeveloperStudioBusy.value = false
        }
    }

    fun runDeveloperBuildAndTests(branchName: String) {
        val report = com.example.data.GeminiDeveloperStudioManager.runBuildAndTestVerificationSuite(
            workspaceFiles = developerWorkspaceFilesState.value,
            branchName = branchName
        )
        _developerBuildTestReport.value = report
        _developerStudioStatusMessage.value = if (report.compileCheckPassed && report.firestoreRulesCheckPassed) {
            "🧪 Build & Tests Passed (${report.unitTestPassedCount}/${report.unitTestTotalCount} suites in ${report.durationMs}ms)"
        } else {
            "⚠️ Build check reported issues — inspect Build & Test console"
        }
    }

    fun saveGitHubConfiguration(
        repoOwner: String,
        repoName: String,
        defaultBranch: String,
        activeDevBranch: String,
        personalAccessToken: String
    ) {
        viewModelScope.launch {
            _isDeveloperStudioBusy.value = true
            repository.saveGitHubConfiguration(
                repoOwner = repoOwner,
                repoName = repoName,
                defaultBranch = defaultBranch,
                activeDevBranch = activeDevBranch,
                personalAccessToken = personalAccessToken
            ).onSuccess { cfg ->
                _developerStudioStatusMessage.value = "🐙 ${cfg.lastSyncStatus}"
            }.onFailure { e ->
                _developerStudioStatusMessage.value = e.localizedMessage ?: "Failed to save GitHub configuration"
            }
            _isDeveloperStudioBusy.value = false
        }
    }

    fun rollbackDeveloperCommit(commit: com.example.data.GitHubCommitDoc) {
        viewModelScope.launch {
            _isDeveloperStudioBusy.value = true
            repository.rollbackCommitChange(commit).onSuccess { restoredFile ->
                _developerStudioStatusMessage.value = "⏪ Rolled back ${restoredFile.filePath} from commit [${commit.commitSha}]"
                _statusBanner.value = "⏪ Rolled back commit [${commit.commitSha}]"
            }.onFailure { e ->
                _developerStudioStatusMessage.value = e.localizedMessage ?: "Commit rollback failed"
            }
            _isDeveloperStudioBusy.value = false
        }
    }

    private companion object {
        const val TAG = "NoiseViewModel"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}


