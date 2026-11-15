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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.ContentReportDoc
import com.example.data.GameRoomDoc
import com.example.data.OwnerAuditLogDoc
import com.example.data.PartyRoom
import com.example.data.Post
import com.example.data.Story
import com.example.data.SuperOwnerControlManager
import com.example.data.SuperOwnerRegistryDoc
import com.example.data.UserProfile
import com.example.ui.theme.NoiseBlack
import com.example.ui.theme.NoiseBorder
import com.example.ui.theme.NoiseCard
import com.example.ui.theme.NoiseCardElevated
import com.example.ui.theme.NoisePink
import com.example.ui.theme.NoisePurple
import com.example.ui.theme.NoiseSurface
import com.example.ui.theme.NoiseTextSecondary
import com.example.ui.theme.NoiseWhite
import com.example.ui.theme.VipGold
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val NoiseSurfaceColor: Color get() = NoiseBlack
private val NoiseSecondaryBg: Color get() = NoiseSurface
private val NoiseCardBg: Color get() = NoiseCard
private val NoiseGlassBorder: Color get() = NoiseBorder
private val NoisePrimaryText: Color get() = NoiseWhite
private val NoiseSecondaryText: Color get() = NoiseTextSecondary
private val NoiseNeonPink: Color get() = NoisePink
private val NoiseNeonPurple: Color get() = NoisePurple
private val NoiseCtaGradient: Brush get() = Brush.horizontalGradient(listOf(NoisePink, NoisePurple))

@Composable
fun SuperOwnerControlPanelModal(
    initialTab: Int = 0,
    currentUserProfile: UserProfile,
    registryDoc: SuperOwnerRegistryDoc,
    allChatrooms: List<PartyRoom>,
    allPosts: List<Post>,
    allStories: List<Story>,
    allGameRooms: List<GameRoomDoc>,
    allUsers: List<UserProfile>,
    reports: List<ContentReportDoc>,
    auditLogs: List<OwnerAuditLogDoc>,
    onDismiss: () -> Unit,
    onDeleteSingleChatroom: (String) -> Unit,
    onDeleteSelectedChatrooms: (List<String>) -> Unit,
    onDeleteAllChatrooms: () -> Unit,
    onPurgeDemoAndDuplicateRooms: () -> Unit,
    onDeletePost: (Post) -> Unit,
    onDeleteStory: (Story) -> Unit,
    onDeleteGameGroup: (String) -> Unit,
    onCleanupAllDemoData: () -> Unit,
    onUpdateUserAccountStatus: (UserProfile, String, String) -> Unit,
    onResolveReport: (ContentReportDoc, String, String, Boolean) -> Unit,
    onOpenCentralAiAdmin: () -> Unit = {}
) {
    val isVerifiedOwner = SuperOwnerControlManager.isCurrentUserSuperOwner(currentUserProfile, registryDoc)
    if (!isVerifiedOwner) {
        onDismiss()
        return
    }

    var selectedTab by remember(initialTab) { mutableIntStateOf(initialTab.coerceIn(0, 3)) }
    var roomSearchQuery by remember { mutableStateOf("") }
    var userSearchQuery by remember { mutableStateOf("") }
    val selectedRoomIds = remember { mutableStateListOf<String>() }

    var showDeleteAllConfirmDialog by remember { mutableStateOf(false) }
    var showDeleteSelectedConfirmDialog by remember { mutableStateOf(false) }
    var pendingSingleDeleteRoom by remember { mutableStateOf<PartyRoom?>(null) }
    var pendingModerationUser by remember { mutableStateOf<Pair<UserProfile, String>?>(null) }
    var moderationReasonInput by remember { mutableStateOf("") }

    val filteredRooms = remember(allChatrooms, roomSearchQuery) {
        val q = roomSearchQuery.trim().lowercase()
        if (q.isBlank()) {
            allChatrooms
        } else {
            allChatrooms.filter { room ->
                room.resolvedRoomName().lowercase().contains(q) ||
                    room.id.lowercase().contains(q) ||
                    room.hostUsername.lowercase().contains(q) ||
                    room.hostId.lowercase().contains(q)
            }
        }
    }

    val filteredUsers = remember(allUsers, userSearchQuery) {
        val q = userSearchQuery.trim().lowercase()
        if (q.isBlank()) {
            allUsers
        } else {
            allUsers.filter { u ->
                u.username.lowercase().contains(q) ||
                    u.displayName.lowercase().contains(q) ||
                    u.id.lowercase().contains(q)
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .testTag("super_owner_control_panel_modal"),
            color = NoiseSurfaceColor
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0xFF2D0A4E), Color(0xFF7B1FA2), Color(0xFFC2185B))
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
                                Text(
                                    text = "👑 SUPER OWNER CONTROL SYSTEM",
                                    color = Color(0xFFFFD700),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .background(Color(0xFF00E676).copy(alpha = 0.22f))
                                        .border(1.dp, Color(0xFF00E676), RoundedCornerShape(50))
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "@sebin_mr VERIFIED",
                                        color = Color(0xFF00E676),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Permanent Owner UID: ${currentUserProfile.id.take(22)}... • Full App Authority",
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(Color(0xFF00E676))
                                    .clickable {
                                        onDismiss()
                                        onOpenCentralAiAdmin()
                                    }
                                    .padding(horizontal = 10.dp, vertical = 7.dp)
                                    .testTag("open_central_ai_admin_from_owner_panel"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "🤖 AI ADMIN",
                                    color = Color.Black,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 11.sp
                                )
                            }
                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.35f))
                                    .testTag("close_owner_panel_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close Super Owner Panel",
                                    tint = Color.White
                                )
                            }
                        }
                    }
                }

                // Navigation Tabs
                val tabs = listOf(
                    "Chatroom Cleanup (${allChatrooms.size})",
                    "Posts / Stories / Groups",
                    "Users (${allUsers.size})",
                    "Reports & Audit (${reports.count { it.status == "open" }})"
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(NoiseSecondaryBg)
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    tabs.forEachIndexed { index, title ->
                        val isSelected = selectedTab == index
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(
                                    if (isSelected) NoiseCtaGradient
                                    else Brush.horizontalGradient(listOf(NoiseCardBg, NoiseCardBg))
                                )
                                .border(
                                    1.dp,
                                    if (isSelected) Color(0xFFFFD700) else NoiseGlassBorder,
                                    RoundedCornerShape(50)
                                )
                                .clickable { selectedTab = index }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                                .testTag("owner_tab_$index")
                        ) {
                            Text(
                                text = title,
                                color = if (isSelected) Color.White else NoisePrimaryText,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold
                            )
                        }
                    }
                }

                // Tab Content
                when (selectedTab) {
                    0 -> OwnerChatroomManagementTab(
                        allChatrooms = allChatrooms,
                        filteredRooms = filteredRooms,
                        searchQuery = roomSearchQuery,
                        onSearchQueryChange = { roomSearchQuery = it },
                        selectedRoomIds = selectedRoomIds,
                        onToggleSelectRoom = { roomId ->
                            if (selectedRoomIds.contains(roomId)) {
                                selectedRoomIds.remove(roomId)
                            } else {
                                selectedRoomIds.add(roomId)
                            }
                        },
                        onSelectAllVisible = {
                            val allVisibleSelected = filteredRooms.isNotEmpty() &&
                                filteredRooms.all { selectedRoomIds.contains(it.id) }
                            if (allVisibleSelected) {
                                selectedRoomIds.clear()
                            } else {
                                filteredRooms.forEach { room ->
                                    if (!selectedRoomIds.contains(room.id)) {
                                        selectedRoomIds.add(room.id)
                                    }
                                }
                            }
                        },
                        onRequestDeleteSingle = { room -> pendingSingleDeleteRoom = room },
                        onRequestDeleteSelected = {
                            if (selectedRoomIds.isNotEmpty()) {
                                showDeleteSelectedConfirmDialog = true
                            }
                        },
                        onRequestDeleteAll = { showDeleteAllConfirmDialog = true },
                        onPurgeDemoRooms = onPurgeDemoAndDuplicateRooms
                    )

                    1 -> OwnerContentModerationTab(
                        posts = allPosts,
                        stories = allStories,
                        gameRooms = allGameRooms,
                        onDeletePost = onDeletePost,
                        onDeleteStory = onDeleteStory,
                        onDeleteGameGroup = onDeleteGameGroup,
                        onCleanupAllDemoData = onCleanupAllDemoData
                    )

                    2 -> OwnerUsersControlTab(
                        users = filteredUsers,
                        searchQuery = userSearchQuery,
                        onSearchQueryChange = { userSearchQuery = it },
                        onRequestStatusChange = { user, targetStatus ->
                            moderationReasonInput = ""
                            pendingModerationUser = user to targetStatus
                        }
                    )

                    3 -> OwnerReportsAndAuditTab(
                        reports = reports,
                        auditLogs = auditLogs,
                        onResolveReport = onResolveReport
                    )
                }
            }
        }
    }

    // Confirmation Dialog: Delete ALL Chatrooms (exact required message)
    if (showDeleteAllConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAllConfirmDialog = false },
            containerColor = NoiseCardBg,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color(0xFFFF1744),
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Delete All Chatrooms",
                        color = NoisePrimaryText,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 17.sp
                    )
                }
            },
            text = {
                Text(
                    text = SuperOwnerControlManager.DELETE_ALL_CHATROOMS_CONFIRMATION_MESSAGE,
                    color = NoisePrimaryText,
                    fontSize = 14.sp,
                    modifier = Modifier.testTag("owner_delete_all_confirm_message")
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteAllConfirmDialog = false
                        selectedRoomIds.clear()
                        onDeleteAllChatrooms()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF1744)),
                    modifier = Modifier.testTag("confirm_delete_all_chatrooms_button")
                ) {
                    Text("Yes, Delete All", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteAllConfirmDialog = false }
                ) {
                    Text("Cancel", color = NoiseSecondaryText)
                }
            }
        )
    }

    // Confirmation Dialog: Delete Selected Chatrooms
    if (showDeleteSelectedConfirmDialog) {
        val count = selectedRoomIds.size
        AlertDialog(
            onDismissRequest = { showDeleteSelectedConfirmDialog = false },
            containerColor = NoiseCardBg,
            title = {
                Text(
                    text = "Delete $count Selected Chatroom(s)?",
                    color = NoisePrimaryText,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp
                )
            },
            text = {
                Text(
                    text = "This will permanently remove the $count selected chatroom(s), their Firebase records, and active room references.",
                    color = NoiseSecondaryText,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val idsToDelete = selectedRoomIds.toList()
                        showDeleteSelectedConfirmDialog = false
                        selectedRoomIds.clear()
                        onDeleteSelectedChatrooms(idsToDelete)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF1744)),
                    modifier = Modifier.testTag("confirm_delete_selected_chatrooms_button")
                ) {
                    Text("Delete Selected ($count)", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteSelectedConfirmDialog = false }) {
                    Text("Cancel", color = NoiseSecondaryText)
                }
            }
        )
    }

    // Confirmation Dialog: Single Chatroom Delete
    pendingSingleDeleteRoom?.let { targetRoom ->
        AlertDialog(
            onDismissRequest = { pendingSingleDeleteRoom = null },
            containerColor = NoiseCardBg,
            title = {
                Text(
                    text = "Delete Chatroom?",
                    color = NoisePrimaryText,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp
                )
            },
            text = {
                Text(
                    text = "Permanently delete \"${targetRoom.resolvedRoomName()}\" (ID: ${targetRoom.id}) created by @${targetRoom.hostUsername}?",
                    color = NoiseSecondaryText,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val id = targetRoom.id
                        pendingSingleDeleteRoom = null
                        selectedRoomIds.remove(id)
                        onDeleteSingleChatroom(id)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF1744)),
                    modifier = Modifier.testTag("confirm_single_room_delete_button")
                ) {
                    Text("Delete Room", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingSingleDeleteRoom = null }) {
                    Text("Cancel", color = NoiseSecondaryText)
                }
            }
        )
    }

    // Confirmation Dialog: User Suspend / Ban / Restore
    pendingModerationUser?.let { (targetUser, newStatus) ->
        AlertDialog(
            onDismissRequest = { pendingModerationUser = null },
            containerColor = NoiseCardBg,
            title = {
                Text(
                    text = "Set @${targetUser.username} to ${newStatus.uppercase()}?",
                    color = NoisePrimaryText,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Column {
                    Text(
                        text = "User UID: ${targetUser.id}",
                        color = NoiseSecondaryText,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(NoiseSecondaryBg)
                            .border(1.dp, NoiseGlassBorder, RoundedCornerShape(10.dp))
                            .padding(10.dp)
                    ) {
                        if (moderationReasonInput.isEmpty()) {
                            Text(
                                text = "Optional reason (e.g., spam, impersonation, policy violation)...",
                                color = NoiseSecondaryText,
                                fontSize = 12.sp
                            )
                        }
                        BasicTextField(
                            value = moderationReasonInput,
                            onValueChange = { moderationReasonInput = it },
                            textStyle = TextStyle(color = NoisePrimaryText, fontSize = 13.sp),
                            cursorBrush = SolidColor(NoiseNeonPink),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val reason = moderationReasonInput.trim()
                        pendingModerationUser = null
                        onUpdateUserAccountStatus(targetUser, newStatus, reason)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (newStatus == "active") Color(0xFF00C853) else Color(0xFFFF1744)
                    )
                ) {
                    Text("Confirm", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingModerationUser = null }) {
                    Text("Cancel", color = NoiseSecondaryText)
                }
            }
        )
    }
}

@Composable
private fun OwnerChatroomManagementTab(
    allChatrooms: List<PartyRoom>,
    filteredRooms: List<PartyRoom>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    selectedRoomIds: List<String>,
    onToggleSelectRoom: (String) -> Unit,
    onSelectAllVisible: () -> Unit,
    onRequestDeleteSingle: (PartyRoom) -> Unit,
    onRequestDeleteSelected: () -> Unit,
    onRequestDeleteAll: () -> Unit,
    onPurgeDemoRooms: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp)
    ) {
        // Dedicated Panel Banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(NoiseSecondaryBg)
                .border(1.dp, NoiseGlassBorder, RoundedCornerShape(14.dp))
                .padding(12.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "OWNER CHATROOM MANAGEMENT",
                        color = NoisePrimaryText,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "${allChatrooms.size} Active • ${selectedRoomIds.size} Selected",
                        color = NoiseNeonPink,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))

                // Search Bar by Room Name or Room ID
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(NoiseCardBg)
                        .border(1.dp, NoiseGlassBorder, RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search Chatrooms",
                        tint = NoiseSecondaryText,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = "Search chatrooms by Room Name, Room ID, or Creator...",
                                color = NoiseSecondaryText,
                                fontSize = 13.sp
                            )
                        }
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = onSearchQueryChange,
                            singleLine = true,
                            textStyle = TextStyle(color = NoisePrimaryText, fontSize = 13.sp),
                            cursorBrush = SolidColor(NoiseNeonPink),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("owner_chatroom_search_input")
                        )
                    }
                    if (searchQuery.isNotEmpty()) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear search",
                            tint = NoiseSecondaryText,
                            modifier = Modifier
                                .size(18.dp)
                                .clickable { onSearchQueryChange("") }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Owner Bulk Action Buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onSelectAllVisible,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("owner_select_all_rooms_button")
                    ) {
                        val allSelected = filteredRooms.isNotEmpty() &&
                            filteredRooms.all { selectedRoomIds.contains(it.id) }
                        Text(
                            text = if (allSelected) "Unselect All" else "Select All (${filteredRooms.size})",
                            color = NoisePrimaryText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Button(
                        onClick = onRequestDeleteSelected,
                        enabled = selectedRoomIds.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFF6D00),
                            disabledContainerColor = NoiseSecondaryText.copy(alpha = 0.25f)
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("owner_delete_selected_chatrooms_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Delete Selected (${selectedRoomIds.size})",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Button(
                        onClick = onRequestDeleteAll,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD50000)),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("owner_delete_all_chatrooms_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Delete ALL Chatrooms",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    OutlinedButton(
                        onClick = onPurgeDemoRooms,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("owner_purge_demo_rooms_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            tint = NoiseNeonPink,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Purge Demo/Duplicate",
                            color = NoiseNeonPink,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (filteredRooms.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(NoiseSecondaryBg)
                    .border(1.dp, NoiseGlassBorder, RoundedCornerShape(16.dp))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (searchQuery.isBlank()) "No Active Chatrooms in Firebase" else "No Matching Chatrooms Found",
                        color = NoisePrimaryText,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (searchQuery.isBlank()) {
                            "All demo, dummy, and deleted chatrooms have been permanently cleared."
                        } else {
                            "No chatroom matched \"$searchQuery\" by room name or room ID."
                        },
                        color = NoiseSecondaryText,
                        fontSize = 12.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredRooms, key = { it.id }) { room ->
                    val isSelected = selectedRoomIds.contains(room.id)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(NoiseCardBg)
                            .border(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) NoiseNeonPink else NoiseGlassBorder,
                                shape = RoundedCornerShape(14.dp)
                            )
                            .clickable { onToggleSelectRoom(room.id) }
                            .padding(12.dp)
                            .testTag("owner_room_item_${room.id}"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = { onToggleSelectRoom(room.id) },
                            colors = CheckboxDefaults.colors(
                                checkedColor = NoiseNeonPink,
                                uncheckedColor = NoiseSecondaryText
                            )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = room.resolvedRoomName(),
                                    color = NoisePrimaryText,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 14.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                if (room.privateRoom || room.visibility.equals("private", ignoreCase = true)) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = "Private Room",
                                        tint = Color(0xFFFFD700),
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = "Room ID: ${room.id}",
                                color = NoiseNeonPink,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Creator: @${room.hostUsername} (UID: ${room.hostId.take(14)}...)",
                                color = NoiseSecondaryText,
                                fontSize = 11.sp
                            )
                            Text(
                                text = "Created: ${room.formattedCreationDate()} • Listeners: ${room.activeParticipantCount()}",
                                color = NoiseSecondaryText,
                                fontSize = 11.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { onRequestDeleteSingle(room) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF1744)),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("owner_delete_room_${room.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete Room",
                                tint = Color.White,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Delete",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OwnerContentModerationTab(
    posts: List<Post>,
    stories: List<Story>,
    gameRooms: List<GameRoomDoc>,
    onDeletePost: (Post) -> Unit,
    onDeleteStory: (Story) -> Unit,
    onDeleteGameGroup: (String) -> Unit,
    onCleanupAllDemoData: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(NoiseSecondaryBg)
                    .border(1.dp, NoiseGlassBorder, RoundedCornerShape(14.dp))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "One-Tap Fake / Demo / Bugged Content Purge",
                        color = NoisePrimaryText,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.sp
                    )
                    Text(
                        text = "Remove any leftover duplicate, sample, or unowned records across Firebase.",
                        color = NoiseSecondaryText,
                        fontSize = 11.sp
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = onCleanupAllDemoData,
                    colors = ButtonDefaults.buttonColors(containerColor = NoiseNeonPurple),
                    modifier = Modifier.testTag("owner_cleanup_all_demo_content_button")
                ) {
                    Text("Clean Now", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        item {
            Text(
                text = "ALL USER POSTS (${posts.size})",
                color = NoisePrimaryText,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 13.sp
            )
        }
        items(posts, key = { "post_${it.id}" }) { post ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(NoiseCardBg)
                    .border(1.dp, NoiseGlassBorder, RoundedCornerShape(12.dp))
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "@${post.username} • ${post.mediaType.uppercase()}",
                        color = NoisePrimaryText,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Text(
                        text = post.caption.ifBlank { "(No caption)" },
                        color = NoiseSecondaryText,
                        fontSize = 12.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Post ID: ${post.id}",
                        color = NoiseNeonPink,
                        fontSize = 10.sp
                    )
                }
                IconButton(
                    onClick = { onDeletePost(post) },
                    modifier = Modifier.testTag("owner_delete_post_${post.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Super Owner Delete Post",
                        tint = Color(0xFFFF1744)
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "ALL ACTIVE STORIES (${stories.size})",
                color = NoisePrimaryText,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 13.sp
            )
        }
        items(stories, key = { "story_${it.id}" }) { story ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(NoiseCardBg)
                    .border(1.dp, NoiseGlassBorder, RoundedCornerShape(12.dp))
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "@${story.username} • Story",
                        color = NoisePrimaryText,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Text(
                        text = story.caption.ifBlank { "Story ID: ${story.id}" },
                        color = NoiseSecondaryText,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(
                    onClick = { onDeleteStory(story) },
                    modifier = Modifier.testTag("owner_delete_story_${story.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Super Owner Delete Story",
                        tint = Color(0xFFFF1744)
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "ALL GAME ROOMS / GROUPS (${gameRooms.size})",
                color = NoisePrimaryText,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 13.sp
            )
        }
        items(gameRooms, key = { "game_${it.id}" }) { group ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(NoiseCardBg)
                    .border(1.dp, NoiseGlassBorder, RoundedCornerShape(12.dp))
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${group.gameType.uppercase()} Group • Host @${group.hostUsername}",
                        color = NoisePrimaryText,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Text(
                        text = "Group ID: ${group.id} • Status: ${group.status}",
                        color = NoiseSecondaryText,
                        fontSize = 11.sp
                    )
                }
                IconButton(
                    onClick = { onDeleteGameGroup(group.id) },
                    modifier = Modifier.testTag("owner_delete_group_${group.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Super Owner Delete Group",
                        tint = Color(0xFFFF1744)
                    )
                }
            }
        }
    }
}

@Composable
private fun OwnerUsersControlTab(
    users: List<UserProfile>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onRequestStatusChange: (UserProfile, String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(NoiseCardBg)
                .border(1.dp, NoiseGlassBorder, RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search Users",
                tint = NoiseSecondaryText,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Box(modifier = Modifier.weight(1f)) {
                if (searchQuery.isEmpty()) {
                    Text(
                        text = "Search users by @username, name, or UID...",
                        color = NoiseSecondaryText,
                        fontSize = 13.sp
                    )
                }
                BasicTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    singleLine = true,
                    textStyle = TextStyle(color = NoisePrimaryText, fontSize = 13.sp),
                    cursorBrush = SolidColor(NoiseNeonPink),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(users, key = { it.id }) { user ->
                val isOwnerAccount = SuperOwnerControlManager.isVerifiedSuperOwner(
                    uid = user.id,
                    username = user.username,
                    userProfile = user
                )
                val status = user.accountStatus.ifBlank { "active" }.lowercase()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(NoiseCardBg)
                        .border(1.dp, NoiseGlassBorder, RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "@${user.username}",
                                color = NoisePrimaryText,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            if (isOwnerAccount) {
                                Text(
                                    text = "👑 SUPER OWNER",
                                    color = Color(0xFFFFD700),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            } else {
                                Text(
                                    text = status.uppercase(),
                                    color = when (status) {
                                        "banned" -> Color(0xFFFF1744)
                                        "suspended" -> Color(0xFFFF9100)
                                        else -> Color(0xFF00C853)
                                    },
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Text(
                            text = "UID: ${user.id}",
                            color = NoiseSecondaryText,
                            fontSize = 11.sp
                        )
                        if (user.suspendedReason.isNotBlank()) {
                            Text(
                                text = "Reason: ${user.suspendedReason}",
                                color = Color(0xFFFF5252),
                                fontSize = 11.sp
                            )
                        }
                    }

                    if (!isOwnerAccount) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (status != "suspended") {
                                OutlinedButton(
                                    onClick = { onRequestStatusChange(user, "suspended") },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text("Suspend", color = Color(0xFFFF9100), fontSize = 11.sp)
                                }
                            }
                            if (status != "banned") {
                                Button(
                                    onClick = { onRequestStatusChange(user, "banned") },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF1744)),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text("Ban", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            if (status != "active") {
                                Button(
                                    onClick = { onRequestStatusChange(user, "active") },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C853)),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text("Restore", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
private fun OwnerReportsAndAuditTab(
    reports: List<ContentReportDoc>,
    auditLogs: List<OwnerAuditLogDoc>,
    onResolveReport: (ContentReportDoc, String, String, Boolean) -> Unit
) {
    val sdf = remember { SimpleDateFormat("MMM d, HH:mm", Locale.US) }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text(
                text = "USER REPORTS & COMPLAINTS (${reports.size})",
                color = NoisePrimaryText,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 13.sp
            )
        }
        if (reports.isEmpty()) {
            item {
                Text(
                    text = "No open reports or complaints.",
                    color = NoiseSecondaryText,
                    fontSize = 12.sp
                )
            }
        } else {
            items(reports, key = { "report_${it.id}" }) { report ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(NoiseCardBg)
                        .border(1.dp, NoiseGlassBorder, RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = "[${report.status.uppercase()}] ${report.targetType.uppercase()} (${report.targetId})",
                        color = NoisePrimaryText,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Text(
                        text = "Reported by @${report.reporterUsername}: ${report.reason}",
                        color = NoiseSecondaryText,
                        fontSize = 12.sp
                    )
                    if (report.status == "open") {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    onResolveReport(report, "resolved", "Deleted by Super Owner", true)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF1744)),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text("Delete Content & Resolve", color = Color.White, fontSize = 11.sp)
                            }
                            OutlinedButton(
                                onClick = {
                                    onResolveReport(report, "dismissed", "Dismissed by Super Owner", false)
                                },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text("Dismiss", color = NoiseSecondaryText, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "SUPER OWNER AUDIT LOGS (${auditLogs.size})",
                color = NoisePrimaryText,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 13.sp
            )
        }
        if (auditLogs.isEmpty()) {
            item {
                Text(
                    text = "No owner moderation actions logged yet.",
                    color = NoiseSecondaryText,
                    fontSize = 12.sp
                )
            }
        } else {
            items(auditLogs, key = { "log_${it.id}" }) { log ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(NoiseSecondaryBg)
                        .border(1.dp, NoiseGlassBorder, RoundedCornerShape(10.dp))
                        .padding(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = log.actionType,
                            color = NoiseNeonPink,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 12.sp
                        )
                        Text(
                            text = sdf.format(log.createdAt?.toDate() ?: Date()),
                            color = NoiseSecondaryText,
                            fontSize = 11.sp
                        )
                    }
                    Text(
                        text = "${log.targetType}: ${log.targetId} • ${log.details}",
                        color = NoisePrimaryText,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}
