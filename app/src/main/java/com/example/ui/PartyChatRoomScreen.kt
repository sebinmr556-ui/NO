package com.example.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import android.os.Build
import androidx.compose.material.icons.filled.BluetoothAudio
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.example.data.PartyGiftCatalogItem
import com.example.data.PartyRoom
import com.example.ui.theme.NoiseCard
import com.example.ui.theme.NoiseCardElevated
import com.example.data.RoomParticipant
import com.example.data.RoomRole
import com.example.data.RoomThemeOption
import com.example.data.SeatOccupant
import com.example.ui.theme.NoiseBlack
import com.example.ui.theme.NoiseBorder
import com.example.ui.theme.NoisePink
import com.example.ui.theme.NoisePurple
import com.example.ui.theme.NoiseTextMuted
import com.example.ui.theme.NoiseTextSecondary
import com.example.ui.theme.NoiseWhite
import com.example.ui.theme.OnlineGreen
import com.example.ui.theme.VipGold
import com.example.ui.theme.VipGoldBorder
import com.example.ui.theme.VipGoldDark

@Composable
fun CreatePartyRoomDialog(
    defaultUsername: String,
    isCreatingRoom: Boolean = false,
    serverError: String? = null,
    onDismiss: () -> Unit,
    onCreateRoom: (
        title: String,
        description: String,
        category: String,
        musicTrack: String,
        gameName: String,
        micMode: String,
        capacity: Long,
        privateRoom: Boolean,
        password: String,
        themeKey: String
    ) -> Unit
) {
    var roomName by remember { mutableStateOf("") }
    var roomDescription by remember { mutableStateOf("Welcome to our live VIP voice & video room!") }
    var selectedCategory by remember { mutableStateOf("Party") }
    var selectedMusic by remember { mutableStateOf("None") }
    var selectedGame by remember { mutableStateOf("PK Battle") }
    var selectedMicMode by remember { mutableStateOf("Free Mic") }
    var selectedCapacity by remember { mutableLongStateOf(12L) }
    var selectedThemeKey by remember { mutableStateOf("starry_mountain") }
    var isPrivate by remember { mutableStateOf(false) }
    var roomPassword by remember { mutableStateOf("") }
    var validationError by remember { mutableStateOf<String?>(null) }
    val activeError = validationError ?: serverError

    val categories = listOf("Party", "Music", "YouTube Live", "PK Battle", "Gaming", "Chat")
    val musicTracks = listOf("None", "Arabian Nights", "Cyberpunk Synth", "Midnight Lo-Fi", "Bass House", "Acoustic Chill")
    val games = listOf("PK Battle", "3D Ludo", "3D Carrom", "8 Ball Pool", "Trivia Quiz", "None")
    val micModes = listOf("Free Mic", "Host Approval", "VIP Only", "Muted on Entry")
    val capacities = listOf(8L to "8 Seats", 12L to "12 Seats", 50L to "50 Room", 100L to "100 VIP")
    val coverThemes = listOf(
        "starry_mountain" to "Starry Mountain",
        "burj_sunset" to "Sunset Tower",
        "royal_majlis" to "Royal Lounge",
        "cosmic_nebula" to "Cosmic Nebula",
        "imax_stage" to "Cinema Stage"
    )

    val gradientBrush = Brush.horizontalGradient(listOf(NoisePink, NoisePurple))

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .border(1.4.dp, VipGoldBorder, RoundedCornerShape(26.dp))
                .testTag("create_party_room_popup"),
            shape = RoundedCornerShape(26.dp),
            color = Color(0xFF0C0A12)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(VipGold),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.WorkspacePremium,
                                contentDescription = "VIP Stage",
                                tint = Color.Black,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "VIP PARTY STAGE",
                                color = VipGold,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp,
                                letterSpacing = 0.6.sp
                            )
                            Text(
                                text = "Configure & launch your live voice room",
                                color = NoiseTextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = NoiseTextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = NoiseBorder)
                Spacer(modifier = Modifier.height(14.dp))

                if (!activeError.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF3B1219))
                            .border(1.dp, NoisePink, RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                            .testTag("create_room_validation_error")
                    ) {
                        Text(
                            text = activeError,
                            color = NoisePink,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // 1. Room Name
                Text("1. Room Name *", color = NoiseWhite, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = roomName,
                    onValueChange = {
                        roomName = it
                        validationError = null
                    },
                    placeholder = { Text("Enter room name", color = NoiseTextMuted, fontSize = 13.sp) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("party_room_name_input"),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF151221),
                        unfocusedContainerColor = Color(0xFF151221),
                        focusedBorderColor = VipGold,
                        unfocusedBorderColor = NoiseBorder,
                        focusedTextColor = NoiseWhite,
                        unfocusedTextColor = NoiseWhite
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 2. Room Description
                Text("2. Room Description *", color = NoiseWhite, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = roomDescription,
                    onValueChange = {
                        roomDescription = it
                        validationError = null
                    },
                    placeholder = { Text("Describe your room topic, rules or vibe...", color = NoiseTextMuted, fontSize = 13.sp) },
                    maxLines = 2,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("party_room_description_input"),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF151221),
                        unfocusedContainerColor = Color(0xFF151221),
                        focusedBorderColor = VipGold,
                        unfocusedBorderColor = NoiseBorder,
                        focusedTextColor = NoiseWhite,
                        unfocusedTextColor = NoiseWhite
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 3. Room Category
                Text("3. Room Category", color = NoiseWhite, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(6.dp))
                OptionChipStrip(
                    options = categories,
                    selected = selectedCategory,
                    accentColor = NoisePink,
                    onSelect = { selectedCategory = it }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 4. Music Selection
                Text("4. Music Selection", color = NoiseWhite, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(6.dp))
                OptionChipStrip(
                    options = musicTracks,
                    selected = selectedMusic,
                    accentColor = NoisePurple,
                    onSelect = { selectedMusic = it }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 5. Game Selection
                Text("5. Game Selection", color = NoiseWhite, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(6.dp))
                OptionChipStrip(
                    options = games,
                    selected = selectedGame,
                    accentColor = VipGold,
                    onSelect = { selectedGame = it }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 6. Microphone Settings
                Text("6. Microphone Settings", color = NoiseWhite, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(6.dp))
                OptionChipStrip(
                    options = micModes,
                    selected = selectedMicMode,
                    accentColor = NoisePink,
                    onSelect = { selectedMicMode = it }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 7. Room Capacity
                Text("7. Room Capacity", color = NoiseWhite, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    capacities.forEach { (capVal, label) ->
                        val active = selectedCapacity == capVal
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (active) VipGoldDark else Color(0xFF151221))
                                .border(
                                    1.dp,
                                    if (active) VipGold else NoiseBorder,
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable { selectedCapacity = capVal }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                color = if (active) VipGold else NoiseTextSecondary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 7b. Room Cover / Stage Theme
                Text("Room Cover / Stage Theme", color = NoiseWhite, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    coverThemes.forEach { (themeKey, themeLabel) ->
                        val active = selectedThemeKey == themeKey
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (active) VipGoldDark else Color(0xFF151221))
                                .border(
                                    width = 1.dp,
                                    color = if (active) VipGold else NoiseBorder,
                                    shape = RoundedCornerShape(14.dp)
                                )
                                .clickable { selectedThemeKey = themeKey }
                                .padding(horizontal = 12.dp, vertical = 7.dp)
                                .testTag("create_room_theme_$themeKey")
                        ) {
                            Text(
                                text = themeLabel,
                                color = if (active) VipGold else NoiseTextSecondary,
                                fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 8. Public / Private Room Toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF151221))
                        .border(1.dp, NoiseBorder, RoundedCornerShape(14.dp))
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = if (isPrivate) "8. 🔒 Private Room" else "8. 🌐 Public Room",
                            color = NoiseWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = if (isPrivate) "Requires password to enter stage" else "Anyone on Noise Live can join",
                            color = NoiseTextSecondary,
                            fontSize = 11.sp
                        )
                    }
                    Switch(
                        checked = isPrivate,
                        onCheckedChange = {
                            isPrivate = it
                            validationError = null
                        },
                        modifier = Modifier.testTag("party_room_private_switch"),
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = VipGold,
                            checkedTrackColor = VipGoldDark
                        )
                    )
                }

                // 9. Private Room Password
                if (isPrivate) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("9. Room Password *", color = VipGold, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = roomPassword,
                        onValueChange = {
                            roomPassword = it
                            validationError = null
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = VipGold, modifier = Modifier.size(18.dp))
                        },
                        placeholder = { Text("Set 4-8 digit room password...", color = NoiseTextMuted, fontSize = 13.sp) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("party_room_password_input"),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF151221),
                            unfocusedContainerColor = Color(0xFF151221),
                            focusedBorderColor = VipGold,
                            unfocusedBorderColor = VipGoldBorder,
                            focusedTextColor = NoiseWhite,
                            unfocusedTextColor = NoiseWhite
                        )
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // 10. Cancel & CREATE ROOM Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF1A1826))
                            .border(1.dp, NoiseBorder, RoundedCornerShape(16.dp))
                            .clickable(enabled = !isCreatingRoom) { onDismiss() }
                            .testTag("create_room_cancel_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Cancel",
                            color = NoiseTextSecondary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                    Button(
                        onClick = {
                            if (isCreatingRoom) return@Button
                            val cleanTitle = roomName.trim()
                            val cleanDesc = roomDescription.trim()
                            val cleanPass = roomPassword.trim()
                            when {
                                cleanTitle.isBlank() -> {
                                    validationError = "Please enter a Room Name."
                                }
                                cleanDesc.isBlank() -> {
                                    validationError = "Please enter a Room Description."
                                }
                                isPrivate && cleanPass.length < 4 -> {
                                    validationError = "Please set a password of at least 4 characters for a Private Room."
                                }
                                else -> {
                                    validationError = null
                                    onCreateRoom(
                                        cleanTitle,
                                        cleanDesc,
                                        selectedCategory,
                                        selectedMusic,
                                        selectedGame,
                                        selectedMicMode,
                                        selectedCapacity,
                                        isPrivate,
                                        if (isPrivate) cleanPass else "",
                                        selectedThemeKey
                                    )
                                }
                            }
                        },
                        enabled = !isCreatingRoom,
                        modifier = Modifier
                            .weight(1.4f)
                            .height(48.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(gradientBrush)
                            .testTag("create_room_submit_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent
                        )
                    ) {
                        if (isCreatingRoom) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator(
                                    color = NoiseWhite,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "CREATING...",
                                    color = NoiseWhite,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 13.sp,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        } else {
                            Text(
                                text = "CREATE ROOM",
                                color = NoiseWhite,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.sp,
                                letterSpacing = 0.6.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OptionChipStrip(
    options: List<String>,
    selected: String,
    accentColor: Color,
    onSelect: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { option ->
            val active = option == selected
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (active) accentColor.copy(alpha = 0.22f) else Color(0xFF151221))
                    .border(
                        width = 1.dp,
                        color = if (active) accentColor else NoiseBorder,
                        shape = RoundedCornerShape(14.dp)
                    )
                    .clickable { onSelect(option) }
                    .padding(horizontal = 12.dp, vertical = 7.dp)
            ) {
                Text(
                    text = option,
                    color = if (active) NoiseWhite else NoiseTextSecondary,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 12.sp
                )
            }
        }
    }
}

private enum class RoomActiveSheet {
    NONE,
    TOOLS,
    GIFT_COIN,
    SELECT_THEME,
    MUSIC_CONTROL,
    PARTICIPATION_ROLES
}

@Composable
fun PartyChatRoomScreen(
    room: PartyRoom,
    currentUserId: String,
    currentUsername: String,
    currentUserAvatarKey: String,
    userCoins: Long,
    onUpdateRoom: (PartyRoom) -> Unit,
    onJoinOrMoveSeat: (seatNumber: Int, micMuted: Boolean) -> Unit,
    onLeaveSeat: () -> Unit,
    onSetMicMuted: (Boolean) -> Unit,
    onToggleSeatLock: (Int) -> Unit,
    onSendRoomChat: (String) -> Unit,
    onClearRoomChat: () -> Unit,
    onAssignRole: (targetUserId: String, newRole: RoomRole) -> Unit,
    onTransferOwnership: (newOwnerId: String, newOwnerUsername: String) -> Unit,
    onToggleMuteParticipant: (targetUserId: String) -> Unit,
    onKickParticipant: (targetUserId: String) -> Unit,
    onCloseAndDeleteRoom: () -> Unit,
    onSendGift: (amount: Long, targetSeat: Int, giftName: String) -> Unit,
    onOpenVipVault: () -> Unit,
    onClose: () -> Unit,
    onRemoveParticipantFromSlot: (seatNumber: Int, targetUserId: String) -> Unit = { _, uid -> onKickParticipant(uid) },
    gameCatalog: List<com.example.data.NoiseGameCatalogItem> = emptyList(),
    activeChatroomGames: List<com.example.data.GameRoomDoc> = emptyList(),
    roomParticipants: List<com.example.data.UserProfile> = emptyList(),
    onStartChatroomGame: (com.example.data.NoiseGameCatalogItem, String) -> Unit = { _, _ -> },
    onJoinChatroomGame: (com.example.data.GameRoomDoc) -> Unit = {},
    onEndChatroomGame: (com.example.data.GameRoomDoc) -> Unit = {},
    onInviteUserToChatroomGame: (com.example.data.GameRoomDoc, com.example.data.UserProfile) -> Unit = { _, _ -> },
    onSyncYouTubeState: (videoUrlOrId: String, videoTitle: String, isPlaying: Boolean, positionSec: Long) -> Unit = { _, _, _, _ -> }
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    // Real Room Music Player Controller — NEVER auto-starts on room entry; only plays when manually started
    val musicController = remember(context) { RoomMusicController(context) }
    var userManuallyStartedMusic by remember(room.id) { mutableStateOf(false) }

    // Official LiveKit Android SDK Session Manager + Hardware Audio Router (Speaker / Earpiece / Bluetooth)
    val liveKitSessionManager = remember(context, room.id) { LiveKitRoomSessionManager(context) }
    val liveKitAudioRouter = liveKitSessionManager.audioRouter
    val liveKitAudioState by liveKitAudioRouter.state.collectAsState()
    val liveKitSessionState by liveKitSessionManager.sessionState.collectAsState()
    var isLocalCameraOn by remember(room.id) { mutableStateOf(false) }
    var isLocalFrontCamera by remember(room.id) { mutableStateOf(true) }

    var activeSheet by remember { mutableStateOf(RoomActiveSheet.NONE) }
    var showChatroomGamesDialog by remember { mutableStateOf(false) }
    var showYouTubeControlDialog by remember { mutableStateOf(false) }
    var isSpeakerOn by remember { mutableStateOf(true) }
    var showAudioRouteDialog by remember { mutableStateOf(false) }
    var chatInput by remember { mutableStateOf("") }
    var selectedThemeKey by remember(room.themeKey) { mutableStateOf(room.themeKey.ifBlank { "starry_mountain" }) }
    var roomNoticeBanner by remember { mutableStateOf<String?>(null) }

    // Confirmation Dialog States for Tools & Owner Slot Management
    var showWipeConfirmDialog by remember { mutableStateOf(false) }
    var showDeleteChatConfirmDialog by remember { mutableStateOf(false) }
    var showCloseRoomConfirmDialog by remember { mutableStateOf(false) }
    var selectedSeatForAction by remember { mutableStateOf<Int?>(null) }
    var pendingKickSlotConfirmation by remember { mutableStateOf<Pair<Int, SeatOccupant>?>(null) }
    var pendingLockOccupiedSlotConfirmation by remember { mutableStateOf<Int?>(null) }
    var pendingSeatForMicPermission by remember { mutableStateOf<Int?>(null) }

    val occupiedSeats = remember(room.occupiedSeatsJson) { room.parseOccupiedSeats() }
    val firestoreParticipants = remember(room.participantsJson) { room.parseParticipants() }
    // Merge Firestore participants with real-time LiveKit room participants (strictly real users with Firebase UID, zero fake/demo users)
    val participants = remember(firestoreParticipants, liveKitSessionState.participants) {
        val merged = linkedMapOf<String, com.example.data.RoomParticipant>()
        firestoreParticipants.forEach { p ->
            if (p.userId.isNotBlank()) merged[p.userId] = p
        }
        liveKitSessionState.participants.forEach { lk ->
            if (lk.identity.isNotBlank() && lk.identity !in merged) {
                merged[lk.identity] = com.example.data.RoomParticipant(
                    lk.identity,
                    lk.displayName.ifBlank { lk.identity },
                    lk.displayName.take(1).uppercase().ifBlank { "U" },
                    true
                )
            }
        }
        merged.values.toList()
    }
    val chatMessages = remember(room.chatMessagesJson) { room.parseChatMessages() }
    val callerRole = remember(room, currentUserId, currentUsername) {
        if (com.example.data.SuperOwnerControlManager.isVerifiedSuperOwner(uid = currentUserId, username = currentUsername)) {
            RoomRole.OWNER
        } else {
            room.roleForUser(currentUserId)
        }
    }

    val hasMicPermission = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.RECORD_AUDIO
    ) == PackageManager.PERMISSION_GRANTED

    val mySeatEntry = remember(occupiedSeats, currentUserId) {
        occupiedSeats.entries.find { it.value.userId == currentUserId }
    }
    val isSeated = mySeatEntry != null
    val isForceMuted = currentUserId in room.mutedUserIds
    val isMicMuted = isForceMuted || (mySeatEntry?.value?.micMuted ?: (room.micMode == "Muted on Entry"))

    // Connect to the isolated LiveKit room (`room.id`) via Vercel Token Service (`https://noice-live26.vercel.app`)
    LaunchedEffect(room.id, currentUserId, room.privateRoom) {
        val isAuthorized = room.isUserAuthorizedForRoom(currentUserId) || callerRole == RoomRole.OWNER
        if (!isAuthorized) {
            roomNoticeBanner = "Access denied: You are not authorized to join this private room."
            return@LaunchedEffect
        }
        liveKitSessionManager.connectToChatroom(
            roomId = room.id,
            currentUserId = currentUserId,
            currentUsername = currentUsername,
            isPrivate = room.privateRoom,
            startMicMuted = !isSeated || isMicMuted,
            startCameraEnabled = isLocalCameraOn
        )
    }

    DisposableEffect(musicController, liveKitSessionManager, room.id) {
        musicController.stop()
        liveKitAudioRouter.enterVoiceRoomSession(room.title)
        onDispose {
            musicController.stop()
            musicController.release()
            liveKitSessionManager.leaveRoom(deleteOnServerAsHost = false)
            liveKitSessionManager.release()
        }
    }

    // Sync LiveKit microphone state & active speakers with room seat & LiveKit session
    LaunchedEffect(isSeated, isMicMuted, occupiedSeats, participants.size, liveKitSessionState.activeSpeakerNames) {
        liveKitSessionManager.setMicrophoneMuted(!isSeated || isMicMuted)
        val seatSpeakers = occupiedSeats.values
            .filter { !it.micMuted }
            .map { it.username }
        val combinedSpeakers = (liveKitSessionState.activeSpeakerNames + seatSpeakers).distinct()
        liveKitAudioRouter.updateSpeakingState(
            isLocalSpeaking = (isSeated && !isMicMuted) || liveKitSessionState.isLocalSpeaking,
            activeSpeakers = combinedSpeakers,
            participantCount = participants.size.coerceAtLeast(1)
        )
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            isLocalCameraOn = true
            liveKitSessionManager.setCameraEnabled(true)
            roomNoticeBanner = "LiveKit group video camera enabled"
        } else {
            isLocalCameraOn = false
            liveKitSessionManager.setCameraEnabled(false)
            roomNoticeBanner = "Camera permission is required to enable group video in this chatroom."
        }
    }

    val bluetoothPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        liveKitAudioRouter.refreshAvailableRoutes()
        if (granted) {
            liveKitAudioRouter.selectAudioRoute(AudioOutputRoute.BLUETOOTH)
            isSpeakerOn = true
            roomNoticeBanner = "Audio routed to Bluetooth headset"
        } else {
            roomNoticeBanner = "Bluetooth permission is required to use a Bluetooth headset."
        }
    }

    // Sync real audio playback ONLY when user/owner has explicitly started music manually in this session
    LaunchedEffect(room.musicTrack, room.musicPlaying, room.musicVolume, isSpeakerOn, userManuallyStartedMusic) {
        musicController.setVolume(room.musicVolume)
        if (userManuallyStartedMusic &&
            isSpeakerOn &&
            room.musicPlaying &&
            room.musicTrack != "None" &&
            room.musicTrack.isNotBlank()
        ) {
            musicController.playTrack(room.musicTrack, room.musicVolume)
        } else {
            musicController.pause()
        }
    }

    // Check if user was kicked from room
    LaunchedEffect(room.kickedUserIds, currentUserId) {
        if (currentUserId in room.kickedUserIds && callerRole != RoomRole.OWNER) {
            onClose()
        }
    }

    BackHandler {
        if (activeSheet != RoomActiveSheet.NONE) {
            activeSheet = RoomActiveSheet.NONE
        } else {
            musicController.stop()
            onClose()
        }
    }

    // Runtime RECORD_AUDIO permission launcher for joining microphone seats or unmuting mic
    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        val targetSeat = pendingSeatForMicPermission
        pendingSeatForMicPermission = null
        if (granted) {
            if (targetSeat != null) {
                val startMuted = isForceMuted || room.micMode == "Muted on Entry"
                onJoinOrMoveSeat(targetSeat, startMuted)
                roomNoticeBanner = "Joined Seat #$targetSeat with microphone ready"
            } else if (!isForceMuted) {
                onSetMicMuted(false)
                roomNoticeBanner = "Microphone unmuted"
            }
        } else {
            // Permission denied: do not activate microphone, show clear explanation
            if (targetSeat != null) {
                onJoinOrMoveSeat(targetSeat, true)
            } else {
                onSetMicMuted(true)
            }
            roomNoticeBanner = "Microphone permission denied. Your mic remains muted until permission is granted."
        }
    }

    fun requestSeatJoinWithMicCheck(seatNumber: Int) {
        val hasAudioPerm = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (hasAudioPerm) {
            val startMuted = isForceMuted || room.micMode == "Muted on Entry"
            onJoinOrMoveSeat(seatNumber, startMuted)
        } else {
            pendingSeatForMicPermission = seatNumber
            try {
                micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            } catch (_: Exception) {
                onJoinOrMoveSeat(seatNumber, true)
            }
        }
    }

    fun handleSeatClicked(seatNumber: Int) {
        val isLocked = seatNumber.toLong() in room.lockedSeats
        val occupant = occupiedSeats[seatNumber]

        when {
            occupant != null && occupant.userId == currentUserId -> {
                // Clicking own seat opens seat options (Mute/Unmute, Leave Seat, Lock/Unlock Slot if Owner/Admin)
                selectedSeatForAction = seatNumber
            }
            occupant != null && occupant.userId != currentUserId -> {
                // Seat is occupied by another user
                val occupantRole = room.roleForUser(occupant.userId)
                if (callerRole == RoomRole.OWNER || callerRole.canMuteOrKickParticipant(occupantRole) || callerRole.canLockOrUnlockSeat()) {
                    selectedSeatForAction = seatNumber
                } else {
                    roomNoticeBanner = "This seat is already occupied"
                }
            }
            else -> {
                // Seat is EMPTY
                if (callerRole == RoomRole.OWNER || callerRole.canLockOrUnlockSeat()) {
                    // Owner / Admin clicking an empty slot opens the Empty Slot Owner Management Menu
                    selectedSeatForAction = seatNumber
                } else if (isLocked) {
                    // Regular user clicking a locked slot sees the exact required message
                    roomNoticeBanner = "This slot is locked by the room owner."
                } else {
                    // Regular user clicking an available unlocked slot joins the slot normally
                    requestSeatJoinWithMicCheck(seatNumber)
                }
            }
        }
    }

    // 12 Room Themes matching the 3x4 grid in Right Phone of uploaded screenshot
    val roomThemes = remember {
        listOf(
            RoomThemeOption("starry_mountain", "Starry Mountain", "Gallery", 0xFF091326, 0xFF1B2942, 0xFF8B5CF6, ""),
            RoomThemeOption("burj_sunset", "Sunset Tower", "Gallery", 0xFF3E1E14, 0xFF8C4A27, 0xFFF59E0B, "canyon"),
            RoomThemeOption("desert_dunes", "Golden Dunes", "Gallery", 0xFF422813, 0xFFA16207, 0xFFFBBF24, ""),
            RoomThemeOption("royal_majlis", "Royal Lounge", "Gallery", 0xFF2E1012, 0xFF6B1D1D, 0xFFF59E0B, "roses"),
            RoomThemeOption("oasis_camp", "Oasis Camp", "Gallery", 0xFF3B2314, 0xFF78350F, 0xFFF97316, "canyon"),
            RoomThemeOption("palace_courtyard", "Palace Palms", "Gallery", 0xFF0F292A, 0xFF155E75, 0xFF38BDF8, "lake"),
            RoomThemeOption("luxury_motors", "Supercars Hall", "Premium", 0xFF111827, 0xFF1F2937, 0xFF60A5FA, "mic"),
            RoomThemeOption("imax_stage", "Cinema Stage", "Premium", 0xFF0F172A, 0xFF1E3A8A, 0xFF38BDF8, "wave"),
            RoomThemeOption("candy_clouds", "Dream Clouds", "Premium", 0xFF1E1B4B, 0xFF4338CA, 0xFFF472B6, "couple"),
            RoomThemeOption("canyon_pass", "Canyon Pass", "Premium", 0xFF271712, 0xFF573022, 0xFFFB923C, "canyon"),
            RoomThemeOption("cosmic_nebula", "Cosmic Nebula", "Premium", 0xFF1E0B36, 0xFF4C1D95, 0xFFC084FC, "wave"),
            RoomThemeOption("saturn_rings", "Ringed Planet", "Premium", 0xFF090D16, 0xFF1E293B, 0xFFA855F7, "lake")
        )
    }

    val customThemePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            selectedThemeKey = uri.toString()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NoiseBlack)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .testTag("party_chat_room_screen")
    ) {
        // 1. Dynamic Room Theme Background matching Center Phone of screenshot
        RoomThemeBackground(
            themeKey = room.themeKey,
            roomThemes = roomThemes
        )

        // 2. Main Chat Room Foreground Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            // Top Bar: [Emblem | Room Name | (+)] ... [Genuine Participant Avatars | Count 👤 | X]
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left Host Pill matching screenshot
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0xCC2C3252), Color(0xCC4A3F78))
                            )
                        )
                        .border(1.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(24.dp))
                        .clickable { activeSheet = RoomActiveSheet.PARTICIPATION_ROLES }
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(Color(0xFFFFB703), Color(0xFFFB8500), Color(0xFF9B2226))
                                )
                            )
                            .border(1.5.dp, VipGold, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.WorkspacePremium,
                            contentDescription = "Team Emblem",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Column {
                        Text(
                            text = room.title,
                            color = NoiseWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Host: @${room.hostUsername} • ${callerRole.label}",
                                color = Color.White.copy(alpha = 0.78f),
                                fontSize = 9.sp,
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            VipInlineGemBadge(
                                vipLevel = 10,
                                gemSize = 14.dp,
                                showLabel = false
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFFA855F7), Color(0xFF6366F1))
                                )
                            )
                            .clickable {
                                onSendRoomChat("Followed ${room.title} ⭐")
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Follow Room",
                            tint = NoiseWhite,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // Right Genuine Listener Avatars + Count Pill + Close X (Zero fake accounts)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    participants.take(3).forEach { participant ->
                        NoiseAvatar(
                            avatarKey = participant.avatarKey.ifBlank { participant.username.take(1).uppercase() },
                            size = 26.dp,
                            showOnlineDot = true
                        )
                    }

                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.Black.copy(alpha = 0.45f))
                            .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(14.dp))
                            .clickable { activeSheet = RoomActiveSheet.PARTICIPATION_ROLES }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .testTag("party_room_participants_pill"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${participants.size.coerceAtLeast(1)}",
                            color = Color(0xFF38BDF8),
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Participants",
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(12.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.45f))
                            .clickable {
                                musicController.stop()
                                onClose()
                            }
                            .testTag("party_room_close_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Exit Room",
                            tint = NoiseWhite,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Second Sub-header Row: [💎 Gifts] [🏆 Score] [Role Badge] ... [Music / Privacy Pill]
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Black.copy(alpha = 0.45f))
                            .border(1.dp, Color(0xFF38BDF8).copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "💎 ${formatCompactCount(room.seat1Diamonds + room.seat2Diamonds)} >",
                            color = Color(0xFFBAE6FD),
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color(0xFFD97706), Color(0xFFB45309))
                                )
                            )
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "🏆 ${formatCompactCount(room.giftsTotal)}",
                            color = NoiseWhite,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 10.sp
                        )
                    }

                    // Visible Games Pill Button in Chatroom Top Sub-Header
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Brush.horizontalGradient(listOf(NoisePink, NoisePurple)))
                            .border(1.dp, VipGold, RoundedCornerShape(12.dp))
                            .clickable { showChatroomGamesDialog = true }
                            .padding(horizontal = 9.dp, vertical = 3.dp)
                            .testTag("party_room_top_games_button"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.SportsEsports,
                            contentDescription = "Chatroom Games",
                            tint = VipGold,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (activeChatroomGames.isNotEmpty()) "Games (${activeChatroomGames.size} Live)" else "Games",
                            color = NoiseWhite,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 10.sp
                        )
                    }

                    // Visible YouTube Live / Watch Together Pill Button in Chatroom Top Sub-Header
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (room.resolvedYouTubeVideoId().isNotBlank()) {
                                    Brush.horizontalGradient(listOf(Color(0xFFFF0033), NoisePink))
                                } else {
                                    Brush.horizontalGradient(listOf(Color(0xFF281020), Color(0xFF1A0D26)))
                                }
                            )
                            .border(1.dp, NoisePink, RoundedCornerShape(12.dp))
                            .clickable { showYouTubeControlDialog = true }
                            .padding(horizontal = 9.dp, vertical = 3.dp)
                            .testTag("party_room_youtube_button"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "YouTube Watch Together",
                            tint = NoiseWhite,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = if (room.resolvedYouTubeVideoId().isNotBlank()) "YouTube LIVE" else "YouTube",
                            color = NoiseWhite,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 10.sp
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black.copy(alpha = 0.45f))
                        .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                        .clickable { activeSheet = RoomActiveSheet.MUSIC_CONTROL }
                        .padding(horizontal = 10.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = if (room.privateRoom) "🔒 Private • ${room.category}"
                        else "🎵 ${if (userManuallyStartedMusic && room.musicPlaying) room.musicTrack else "Music Off"} • ${room.category}",
                        color = NoiseWhite.copy(alpha = 0.9f),
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Room Notice / Seat Feedback Banner
            if (roomNoticeBanner != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xDD1A1428))
                        .border(1.dp, VipGoldBorder, RoundedCornerShape(12.dp))
                        .clickable { roomNoticeBanner = null }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .testTag("party_room_notice_banner"),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = roomNoticeBanner ?: "",
                        color = VipGold,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "✕",
                        color = NoiseTextSecondary,
                        fontSize = 11.sp
                    )
                }
            }

            // LiveKit Real-Time Connection & Group Video Stage Section
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.Black.copy(alpha = 0.5f))
                        .border(
                            0.8.dp,
                            when (liveKitSessionState.connectionStatus) {
                                LiveKitConnectionStatus.CONNECTED -> OnlineGreen.copy(alpha = 0.7f)
                                LiveKitConnectionStatus.CONNECTING,
                                LiveKitConnectionStatus.FETCHING_TOKEN,
                                LiveKitConnectionStatus.RECONNECTING -> VipGold.copy(alpha = 0.7f)
                                else -> NoisePink.copy(alpha = 0.7f)
                            },
                            RoundedCornerShape(10.dp)
                        )
                        .clickable {
                            if (liveKitSessionState.connectionStatus != LiveKitConnectionStatus.CONNECTED) {
                                liveKitSessionManager.reconnect()
                                roomNoticeBanner = "Reconnecting to LiveKit room ${room.id}..."
                            } else {
                                showAudioRouteDialog = true
                            }
                        }
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                        .testTag("party_room_livekit_status_pill"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(
                                when (liveKitSessionState.connectionStatus) {
                                    LiveKitConnectionStatus.CONNECTED -> OnlineGreen
                                    LiveKitConnectionStatus.CONNECTING,
                                    LiveKitConnectionStatus.FETCHING_TOKEN,
                                    LiveKitConnectionStatus.RECONNECTING -> VipGold
                                    else -> NoisePink
                                }
                            )
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = when (liveKitSessionState.connectionStatus) {
                            LiveKitConnectionStatus.CONNECTED ->
                                "LIVEKIT • ${liveKitSessionState.participants.size.coerceAtLeast(1)} Connected"
                            LiveKitConnectionStatus.RECONNECTING -> "LIVEKIT • Reconnecting..."
                            LiveKitConnectionStatus.CONNECTING,
                            LiveKitConnectionStatus.FETCHING_TOKEN -> "LIVEKIT • Connecting..."
                            LiveKitConnectionStatus.BACKEND_CONFIG_MISSING -> "LIVEKIT • Tap for Server Status"
                            else -> "LIVEKIT • Tap to Reconnect"
                        },
                        color = NoiseWhite,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (!liveKitSessionState.errorMessage.isNullOrBlank()) {
                    Text(
                        text = liveKitSessionState.errorMessage ?: "",
                        color = VipGold,
                        fontSize = 9.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 8.dp)
                            .clickable {
                                roomNoticeBanner = liveKitSessionState.errorMessage
                            }
                    )
                }
            }

            // Group Video Stage Strip (rendered when any participant enables camera in the group chatroom)
            val videoParticipants = remember(liveKitSessionState.participants) {
                liveKitSessionState.participants.filter { it.isCameraEnabled && it.videoTrack != null }
            }
            if (!room.wipedScreen && (videoParticipants.isNotEmpty() || isLocalCameraOn)) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .testTag("party_room_group_video_stage"),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    videoParticipants.forEach { vp ->
                        Box(
                            modifier = Modifier
                                .width(128.dp)
                                .height(86.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFF101322))
                                .border(
                                    1.2.dp,
                                    if (vp.isSpeaking) OnlineGreen else VipGold,
                                    RoundedCornerShape(14.dp)
                                )
                        ) {
                            LiveKitVideoTrackView(
                                room = liveKitSessionManager.currentRoom(),
                                videoTrack = vp.videoTrack,
                                mirror = vp.isLocal && isLocalFrontCamera,
                                modifier = Modifier.fillMaxSize()
                            )
                            Row(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .fillMaxWidth()
                                    .background(Color.Black.copy(alpha = 0.55f))
                                    .padding(horizontal = 6.dp, vertical = 3.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (vp.isLocal) "You" else vp.displayName,
                                    color = NoiseWhite,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (vp.isLocal) {
                                    Icon(
                                        imageVector = Icons.Default.Cameraswitch,
                                        contentDescription = "Switch Camera",
                                        tint = VipGold,
                                        modifier = Modifier
                                            .size(13.dp)
                                            .clickable {
                                                isLocalFrontCamera = !isLocalFrontCamera
                                                liveKitSessionManager.setFrontCamera(isLocalFrontCamera)
                                            }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Synchronized YouTube Live / Video Player Stage inside Chatroom
            if (!room.wipedScreen && room.resolvedYouTubeVideoId().isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                SynchronizedYouTubeRoomPlayer(
                    room = room,
                    canControlPlayback = callerRole.rank >= RoomRole.MODERATOR.rank,
                    isSpeakerOn = isSpeakerOn,
                    onSyncYouTubeState = onSyncYouTubeState,
                    onOpenChangeVideoDialog = { showYouTubeControlDialog = true }
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 3. Top 2 Royal Winged Crown Seats (Seat 1 & Seat 2) — Each has its own independent 3D Visualizer
            if (!room.wipedScreen) {
                val seat1Occupant = occupiedSeats[1]
                val seat2Occupant = occupiedSeats[2]
                val seat1Locked = 1L in room.lockedSeats
                val seat2Locked = 2L in room.lockedSeats

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RoyalCrownHostSeat(
                        seatNumber = 1,
                        occupant = seat1Occupant,
                        isLocked = seat1Locked,
                        diamondsCount = room.seat1Diamonds,
                        crownPrimary = Color(0xFFF59E0B),
                        crownSecondary = Color(0xFFA855F7),
                        currentUserId = currentUserId,
                        hasMicPermission = hasMicPermission,
                        onClick = { handleSeatClicked(1) }
                    )

                    // Center Audio Wave / PK Emblem matching screenshot
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.5f))
                                .border(
                                    1.2.dp,
                                    if (room.pkActive) VipGold else Color.White.copy(alpha = 0.25f),
                                    CircleShape
                                )
                                .clickable {
                                    if (callerRole.canManageRoomSettings()) {
                                        onUpdateRoom(room.copy(pkActive = !room.pkActive))
                                    } else {
                                        roomNoticeBanner = "Only room admins can toggle PK Battle mode"
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.GraphicEq,
                                contentDescription = "PK Wave",
                                tint = if (room.pkActive) VipGold else NoiseWhite,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        if (room.pkActive) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "PK LIVE",
                                color = VipGold,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 9.sp
                            )
                        }
                    }

                    RoyalCrownHostSeat(
                        seatNumber = 2,
                        occupant = seat2Occupant,
                        isLocked = seat2Locked,
                        diamondsCount = room.seat2Diamonds,
                        crownPrimary = Color(0xFFEC4899),
                        crownSecondary = Color(0xFFF59E0B),
                        currentUserId = currentUserId,
                        hasMicPermission = hasMicPermission,
                        onClick = { handleSeatClicked(2) }
                    )
                }

                // Live PK Progress Bar when PK Battle is active
                if (room.pkActive) {
                    Spacer(modifier = Modifier.height(8.dp))
                    val totalPk = (room.seat1Diamonds + room.seat2Diamonds).coerceAtLeast(1L)
                    val leftRatio = if (room.seat1Diamonds == 0L && room.seat2Diamonds == 0L) 0.5f
                    else (room.seat1Diamonds.toFloat() / totalPk.toFloat()).coerceIn(0.15f, 0.85f)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 28.dp)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .weight(leftRatio)
                                .background(Brush.horizontalGradient(listOf(Color(0xFF38BDF8), Color(0xFF6366F1))))
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .weight(1f - leftRatio)
                                .background(Brush.horizontalGradient(listOf(NoisePink, Color(0xFFF59E0B))))
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 4. 10 Participant Seats Grid: Row 1 (Seats 3..7), Row 2 (Seats 8..12) matching screenshot
                val row1Seats = listOf(3, 4, 5, 6, 7)
                val row2Seats = listOf(8, 9, 10, 11, 12)

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    listOf(row1Seats, row2Seats).forEach { seatRow ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            seatRow.forEach { seatNum ->
                                val isLocked = seatNum.toLong() in room.lockedSeats
                                val occupant = occupiedSeats[seatNum]
                                ParticipantSeatItem(
                                    seatNumber = seatNum,
                                    isLocked = isLocked,
                                    occupant = occupant,
                                    isCurrentUser = occupant?.userId == currentUserId,
                                    currentUserId = currentUserId,
                                    hasMicPermission = hasMicPermission,
                                    onClick = { handleSeatClicked(seatNum) }
                                )
                            }
                        }
                    }
                }
            } else {
                // Wiped screen mode indicator pill
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.Black.copy(alpha = 0.55f))
                            .border(1.dp, VipGoldBorder, RoundedCornerShape(16.dp))
                            .clickable { onUpdateRoom(room.copy(wipedScreen = false)) }
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Screen Wiped • Tap to Restore Stage UI",
                            color = VipGold,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // 5. Live Room Chat Stream (Only real Firestore messages — zero fake messages)
            if (!room.wipedScreen) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .padding(horizontal = 4.dp)
                        .testTag("party_room_chat_list"),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (chatMessages.isEmpty()) {
                        item {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color.Black.copy(alpha = 0.45f))
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = "Room Notice: ",
                                    color = VipGold,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                                Text(
                                    text = room.description.ifBlank { "Tap any open '+' seat to join the voice stage!" },
                                    color = NoiseWhite.copy(alpha = 0.85f),
                                    fontSize = 12.sp
                                )
                            }
                        }
                    } else {
                        items(chatMessages.takeLast(8), key = { it.id }) { chatMsg ->
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color.Black.copy(alpha = 0.48f))
                                    .padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (chatMsg.senderId != "system") {
                                    VipInlineGemBadge(
                                        vipLevel = ((chatMsg.senderUsername.hashCode().let { if (it < 0) -it else it } % 10) + 1),
                                        gemSize = 15.dp,
                                        showLabel = false
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                }
                                Text(
                                    text = "${chatMsg.senderUsername}: ",
                                    color = if (chatMsg.senderId == "system") VipGold else NoisePink,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                                Text(
                                    text = chatMsg.text,
                                    color = NoiseWhite,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Bottom Action Bar: Say something... | Mic | Speaker | Tools | Theme | Gift Coin
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .clip(RoundedCornerShape(21.dp))
                        .background(Color.Black.copy(alpha = 0.55f))
                        .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(21.dp))
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    androidx.compose.foundation.text.BasicTextField(
                        value = chatInput,
                        onValueChange = { chatInput = it },
                        singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(color = NoiseWhite, fontSize = 13.sp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("party_room_chat_input"),
                        decorationBox = { innerTextField ->
                            if (chatInput.isEmpty()) {
                                Text("Say something...", color = NoiseTextSecondary, fontSize = 13.sp)
                            }
                            innerTextField()
                        }
                    )
                    if (chatInput.isNotBlank()) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send in Room",
                            tint = NoisePink,
                            modifier = Modifier
                                .size(18.dp)
                                .clickable {
                                    val msg = chatInput.trim()
                                    if (msg.isNotEmpty()) {
                                        onSendRoomChat(msg)
                                        chatInput = ""
                                    }
                                }
                                .testTag("party_room_chat_send_button")
                        )
                    }
                }

                // Mic Toggle (Checks seat & permission)
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            if (!isSeated || isMicMuted) Color.Black.copy(alpha = 0.55f)
                            else OnlineGreen.copy(alpha = 0.25f)
                        )
                        .border(
                            1.dp,
                            if (!isSeated || isMicMuted) Color.White.copy(alpha = 0.2f) else OnlineGreen,
                            CircleShape
                        )
                        .clickable {
                            if (!isSeated) {
                                roomNoticeBanner = "Tap an available '+' seat first to activate your microphone"
                            } else if (isForceMuted) {
                                roomNoticeBanner = "Your microphone has been muted by a room administrator"
                            } else if (isMicMuted) {
                                // Unmuting: verify RECORD_AUDIO permission first
                                val hasPerm = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.RECORD_AUDIO
                                ) == PackageManager.PERMISSION_GRANTED
                                if (hasPerm) {
                                    onSetMicMuted(false)
                                } else {
                                    pendingSeatForMicPermission = null
                                    try {
                                        micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                    } catch (_: Exception) {
                                        onSetMicMuted(false)
                                    }
                                }
                            } else {
                                onSetMicMuted(true)
                            }
                        }
                        .testTag("party_room_mic_toggle"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (!isSeated || isMicMuted) Icons.Default.MicOff else Icons.Default.Mic,
                        contentDescription = "Toggle Mic",
                        tint = if (!isSeated || isMicMuted) NoiseTextSecondary else OnlineGreen,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Speaker & Audio Output Route Toggle (Speaker / Earpiece / Bluetooth)
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.55f))
                        .border(
                            1.dp,
                            if (isSpeakerOn) VipGold.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.2f),
                            CircleShape
                        )
                        .clickable { showAudioRouteDialog = true }
                        .testTag("party_room_speaker_toggle"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when {
                            !isSpeakerOn -> Icons.AutoMirrored.Filled.VolumeOff
                            liveKitAudioState.currentRoute == AudioOutputRoute.EARPIECE -> Icons.Default.Hearing
                            liveKitAudioState.currentRoute == AudioOutputRoute.BLUETOOTH -> Icons.Default.BluetoothAudio
                            else -> Icons.AutoMirrored.Filled.VolumeUp
                        },
                        contentDescription = "Audio Output (${liveKitAudioState.currentRoute.shortLabel})",
                        tint = if (isSpeakerOn) VipGold else NoiseWhite,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Group Video Camera On/Off Toggle (LiveKit Group Video Stage)
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            if (isLocalCameraOn) OnlineGreen.copy(alpha = 0.25f)
                            else Color.Black.copy(alpha = 0.55f)
                        )
                        .border(
                            1.dp,
                            if (isLocalCameraOn) OnlineGreen else Color.White.copy(alpha = 0.2f),
                            CircleShape
                        )
                        .clickable {
                            if (!isSeated && callerRole != RoomRole.OWNER) {
                                roomNoticeBanner = "Join a stage seat first to broadcast live video"
                            } else if (!isLocalCameraOn) {
                                val hasCamPerm = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.CAMERA
                                ) == PackageManager.PERMISSION_GRANTED
                                if (hasCamPerm) {
                                    isLocalCameraOn = true
                                    liveKitSessionManager.setCameraEnabled(true)
                                    roomNoticeBanner = "LiveKit group video camera enabled"
                                } else {
                                    try {
                                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                    } catch (_: Exception) {
                                        isLocalCameraOn = true
                                        liveKitSessionManager.setCameraEnabled(true)
                                    }
                                }
                            } else {
                                isLocalCameraOn = false
                                liveKitSessionManager.setCameraEnabled(false)
                                roomNoticeBanner = "LiveKit camera turned off"
                            }
                        }
                        .testTag("party_room_camera_toggle"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isLocalCameraOn) Icons.Default.Videocam else Icons.Default.VideocamOff,
                        contentDescription = "Toggle Group Video Camera",
                        tint = if (isLocalCameraOn) OnlineGreen else NoiseTextSecondary,
                        modifier = Modifier.size(19.dp)
                    )
                }

                // Tools Sheet Button
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.55f))
                        .border(1.dp, NoisePink.copy(alpha = 0.6f), CircleShape)
                        .clickable { activeSheet = RoomActiveSheet.TOOLS }
                        .testTag("party_room_tools_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Build,
                        contentDescription = "Tools",
                        tint = NoisePink,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Theme Sheet Button
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.55f))
                        .border(1.dp, NoisePurple.copy(alpha = 0.6f), CircleShape)
                        .clickable { activeSheet = RoomActiveSheet.SELECT_THEME }
                        .testTag("party_room_theme_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = "Select Theme",
                        tint = Color(0xFFC084FC),
                        modifier = Modifier.size(19.dp)
                    )
                }

                // Chatroom Games Button with Game Controller Icon + Label
                Row(
                    modifier = Modifier
                        .height(40.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Brush.horizontalGradient(listOf(Color(0xFF28123D), Color(0xFF190C29))))
                        .border(1.2.dp, VipGold, RoundedCornerShape(20.dp))
                        .clickable { showChatroomGamesDialog = true }
                        .padding(horizontal = 10.dp)
                        .testTag("party_room_games_button"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.SportsEsports,
                        contentDescription = "Open Chatroom Games",
                        tint = VipGold,
                        modifier = Modifier.size(19.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Games",
                        color = NoiseWhite,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 11.sp
                    )
                }

                // Gift Coin Sheet Button
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(NoisePink, NoisePurple)))
                        .clickable { activeSheet = RoomActiveSheet.GIFT_COIN }
                        .testTag("party_room_gift_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CardGiftcard,
                        contentDescription = "Gift Coin",
                        tint = NoiseWhite,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        if (showYouTubeControlDialog) {
            YouTubeWatchTogetherDialog(
                currentVideoUrl = room.youtubeVideoUrl,
                currentVideoTitle = room.youtubeVideoTitle,
                canControlPlayback = callerRole.rank >= RoomRole.MODERATOR.rank,
                onStartOrUpdateVideo = { urlOrId, title ->
                    onSyncYouTubeState(urlOrId, title, true, 0L)
                    roomNoticeBanner = "Started synchronized YouTube playback for all participants!"
                },
                onStopVideo = {
                    onSyncYouTubeState("", "", false, 0L)
                    roomNoticeBanner = "Stopped shared YouTube playback"
                },
                onDismiss = { showYouTubeControlDialog = false }
            )
        }

        if (showChatroomGamesDialog) {
            ChatroomGamesSelectorDialog(
                chatRoomId = room.id,
                chatRoomTitle = room.title,
                isRoomOwner = callerRole == RoomRole.OWNER,
                currentUserId = currentUserId,
                gameCatalog = gameCatalog,
                activeChatroomGames = activeChatroomGames,
                roomMembers = roomParticipants,
                onSelectAndStartChatroomGame = { gameItem, mode ->
                    onSendRoomChat("🎮 Started ${gameItem.title} ($mode) in room '${room.title}'! Tap Games to join with voice chat.")
                    onStartChatroomGame(gameItem, mode)
                },
                onJoinExistingChatroomGame = { liveGame ->
                    onJoinChatroomGame(liveGame)
                },
                onEndChatroomGame = { liveGame ->
                    onSendRoomChat("🏁 Ended ${liveGame.gameTitle} session.")
                    onEndChatroomGame(liveGame)
                },
                onInviteMemberToGame = { liveGame, member ->
                    onSendRoomChat("📨 Invited @${member.username} to play ${liveGame.gameTitle}!")
                    onInviteUserToChatroomGame(liveGame, member)
                },
                onDismiss = { showChatroomGamesDialog = false }
            )
        }

        // 6. Bottom Sheets & Modals matching Left, Center, and Right Phones in Uploaded Screenshot
        when (activeSheet) {
            RoomActiveSheet.TOOLS -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.35f))
                        .clickable { activeSheet = RoomActiveSheet.NONE },
                    contentAlignment = Alignment.BottomCenter
                ) {
                    ToolsBottomSheet(
                        pkActive = room.pkActive,
                        isWipedScreen = room.wipedScreen,
                        currentMusic = room.musicTrack,
                        onWipeScreen = {
                            activeSheet = RoomActiveSheet.NONE
                            showWipeConfirmDialog = true
                        },
                        onOpenBackgrounds = {
                            activeSheet = RoomActiveSheet.SELECT_THEME
                        },
                        onOpenMusic = {
                            activeSheet = RoomActiveSheet.MUSIC_CONTROL
                        },
                        onDeleteChat = {
                            activeSheet = RoomActiveSheet.NONE
                            showDeleteChatConfirmDialog = true
                        },
                        onShareRoom = {
                            val shareText = "Join my live room '${room.title}' on Noise Live! Room ID: ${room.id} • Link: https://noise-live.app/room/${room.id}"
                            clipboardManager.setText(AnnotatedString(shareText))
                            try {
                                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_SUBJECT, "Noise Live Room Invite: ${room.title}")
                                    putExtra(Intent.EXTRA_TEXT, shareText)
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "Share Room Invite"))
                            } catch (_: Exception) {
                                // Fallback when running in headless test
                            }
                            roomNoticeBanner = "Room invite link & ID copied and ready to share!"
                            activeSheet = RoomActiveSheet.NONE
                        },
                        onParticipation = {
                            activeSheet = RoomActiveSheet.PARTICIPATION_ROLES
                        },
                        onTogglePk = {
                            if (callerRole.canManageRoomSettings()) {
                                val updated = room.copy(pkActive = !room.pkActive)
                                onUpdateRoom(updated)
                                onSendRoomChat(if (updated.pkActive) "⚔️ PK Battle Started between Seat 1 & Seat 2!" else "PK Battle Ended")
                            } else {
                                roomNoticeBanner = "Only Owner, Super Admin, or Admin can toggle PK Battle"
                            }
                            activeSheet = RoomActiveSheet.NONE
                        },
                        onOpenGames = {
                            activeSheet = RoomActiveSheet.NONE
                            showChatroomGamesDialog = true
                        },
                        onClose = { activeSheet = RoomActiveSheet.NONE }
                    )
                }
            }

            RoomActiveSheet.GIFT_COIN -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.35f))
                        .clickable { activeSheet = RoomActiveSheet.NONE },
                    contentAlignment = Alignment.BottomCenter
                ) {
                    GiftCoinBottomSheet(
                        userCoins = userCoins,
                        occupiedSeats = occupiedSeats,
                        onSendGift = { gift, quantity, targetSeat ->
                            val totalCost = gift.cost * quantity
                            onSendGift(totalCost, targetSeat, "${gift.emoji} ${gift.name} x$quantity")
                        },
                        onTopUpClick = {
                            activeSheet = RoomActiveSheet.NONE
                            onOpenVipVault()
                        },
                        onClose = { activeSheet = RoomActiveSheet.NONE }
                    )
                }
            }

            RoomActiveSheet.SELECT_THEME -> {
                SelectYourThemeModal(
                    themes = roomThemes,
                    selectedThemeKey = selectedThemeKey,
                    onSelectTheme = { selectedThemeKey = it },
                    onCustomizeFromGallery = {
                        customThemePicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    onResetDefault = {
                        selectedThemeKey = "starry_mountain"
                        onUpdateRoom(room.copy(themeKey = "starry_mountain"))
                        roomNoticeBanner = "Background reset to default Starry Mountain"
                        activeSheet = RoomActiveSheet.NONE
                    },
                    onSave = {
                        if (callerRole.canManageRoomSettings()) {
                            onUpdateRoom(room.copy(themeKey = selectedThemeKey))
                            roomNoticeBanner = "Room background updated!"
                        } else {
                            roomNoticeBanner = "Only Owner, Super Admin, or Admin can change room background"
                        }
                        activeSheet = RoomActiveSheet.NONE
                    },
                    onBack = { activeSheet = RoomActiveSheet.NONE }
                )
            }

            RoomActiveSheet.MUSIC_CONTROL -> {
                RoomMusicModal(
                    currentTrack = room.musicTrack,
                    isPlaying = userManuallyStartedMusic && room.musicPlaying,
                    volume = room.musicVolume.toInt(),
                    canControl = callerRole.canManageRoomSettings(),
                    onSelectTrack = { track ->
                        userManuallyStartedMusic = track != "None"
                        if (track == "None") {
                            musicController.stop()
                        }
                        if (callerRole.canManageRoomSettings()) {
                            onUpdateRoom(room.copy(musicTrack = track, musicPlaying = track != "None"))
                        }
                    },
                    onTogglePlayPause = {
                        val currentlyActive = userManuallyStartedMusic && room.musicPlaying
                        val nextPlaying = !currentlyActive
                        userManuallyStartedMusic = nextPlaying
                        if (!nextPlaying) {
                            musicController.pause()
                        }
                        if (callerRole.canManageRoomSettings()) {
                            val nextTrack = if (room.musicTrack == "None" || room.musicTrack.isBlank()) "Arabian Nights" else room.musicTrack
                            onUpdateRoom(room.copy(musicTrack = nextTrack, musicPlaying = nextPlaying))
                        }
                    },
                    onStopMusic = {
                        userManuallyStartedMusic = false
                        musicController.stop()
                        if (callerRole.canManageRoomSettings()) {
                            onUpdateRoom(room.copy(musicPlaying = false))
                        }
                    },
                    onVolumeChange = { newVol ->
                        musicController.setVolume(newVol)
                        if (callerRole.canManageRoomSettings()) {
                            onUpdateRoom(room.copy(musicVolume = newVol.toLong()))
                        }
                    },
                    onClose = { activeSheet = RoomActiveSheet.NONE }
                )
            }

            RoomActiveSheet.PARTICIPATION_ROLES -> {
                RoomParticipationAndRolesModal(
                    room = room,
                    currentUserId = currentUserId,
                    callerRole = callerRole,
                    participants = participants,
                    occupiedSeats = occupiedSeats,
                    onAssignRole = onAssignRole,
                    onTransferOwnership = onTransferOwnership,
                    onToggleMuteParticipant = onToggleMuteParticipant,
                    onKickParticipant = onKickParticipant,
                    onToggleSeatLock = onToggleSeatLock,
                    onUpdateRoomSettings = { updated ->
                        onUpdateRoom(updated)
                        roomNoticeBanner = "Room settings saved!"
                    },
                    onRequestCloseRoom = {
                        activeSheet = RoomActiveSheet.NONE
                        showCloseRoomConfirmDialog = true
                    },
                    onClose = { activeSheet = RoomActiveSheet.NONE }
                )
            }

            RoomActiveSheet.NONE -> Unit
        }

        // Voice Slot Owner Management & Seat Action Dialog (Occupied OR Empty Slot)
        val actionSeatNum = selectedSeatForAction
        if (actionSeatNum != null) {
            val seatOccupant = occupiedSeats[actionSeatNum]
            val isSeatLocked = actionSeatNum.toLong() in room.lockedSeats
            SeatActionDialog(
                seatNumber = actionSeatNum,
                occupant = seatOccupant,
                isOwnSeat = seatOccupant?.userId == currentUserId,
                callerRole = callerRole,
                targetRole = if (seatOccupant != null) room.roleForUser(seatOccupant.userId) else RoomRole.MEMBER,
                isSeatLocked = isSeatLocked,
                onToggleMic = {
                    if (seatOccupant?.userId == currentUserId) {
                        val nextMuted = !(seatOccupant.micMuted)
                        if (!nextMuted && isForceMuted) {
                            roomNoticeBanner = "You are muted by a room administrator"
                        } else {
                            onSetMicMuted(nextMuted)
                            roomNoticeBanner = if (nextMuted) "Microphone muted" else "Microphone unmuted"
                        }
                    } else if (seatOccupant != null) {
                        onToggleMuteParticipant(seatOccupant.userId)
                        val actionLabel = if (seatOccupant.micMuted) "Unmuted" else "Muted"
                        roomNoticeBanner = "$actionLabel @${seatOccupant.username} on Slot #$actionSeatNum"
                    }
                    selectedSeatForAction = null
                },
                onLeaveOrRemoveFromSeat = {
                    if (seatOccupant?.userId == currentUserId) {
                        onLeaveSeat()
                        roomNoticeBanner = "You left Slot #$actionSeatNum"
                        selectedSeatForAction = null
                    } else if (seatOccupant != null) {
                        // Show confirmation dialog before kicking/removing user from slot
                        pendingKickSlotConfirmation = actionSeatNum to seatOccupant
                        selectedSeatForAction = null
                    }
                },
                onToggleLockSeat = {
                    if (callerRole == RoomRole.OWNER || callerRole.canLockOrUnlockSeat()) {
                        if (seatOccupant != null && !isSeatLocked) {
                            // Show confirmation dialog before locking an occupied slot
                            pendingLockOccupiedSlotConfirmation = actionSeatNum
                            selectedSeatForAction = null
                        } else {
                            onToggleSeatLock(actionSeatNum)
                            roomNoticeBanner = if (isSeatLocked) {
                                "Unlocked Slot #$actionSeatNum"
                            } else {
                                "Locked Slot #$actionSeatNum"
                            }
                            selectedSeatForAction = null
                        }
                    } else {
                        selectedSeatForAction = null
                    }
                },
                onExplicitLockEmptySlot = { shouldLock ->
                    if (callerRole == RoomRole.OWNER || callerRole.canLockOrUnlockSeat()) {
                        if (shouldLock && !isSeatLocked) {
                            onToggleSeatLock(actionSeatNum)
                            roomNoticeBanner = "Locked Slot #$actionSeatNum"
                        } else if (!shouldLock && isSeatLocked) {
                            onToggleSeatLock(actionSeatNum)
                            roomNoticeBanner = "Unlocked Slot #$actionSeatNum"
                        } else {
                            roomNoticeBanner = if (shouldLock) {
                                "Slot #$actionSeatNum is already locked"
                            } else {
                                "Slot #$actionSeatNum is already unlocked"
                            }
                        }
                    }
                    selectedSeatForAction = null
                },
                onJoinEmptySlot = {
                    selectedSeatForAction = null
                    if (isSeatLocked) {
                        roomNoticeBanner = "Unlock Slot #$actionSeatNum first to join"
                    } else {
                        requestSeatJoinWithMicCheck(actionSeatNum)
                    }
                },
                onDismiss = { selectedSeatForAction = null }
            )
        }

        // Confirmation before kicking / removing a user from an occupied voice slot
        val kickConfirmPair = pendingKickSlotConfirmation
        if (kickConfirmPair != null) {
            val (targetSeatNum, targetOccupant) = kickConfirmPair
            ConfirmToolActionDialog(
                title = "Kick / Remove User from Slot #$targetSeatNum?",
                message = "Remove @${targetOccupant.username} from voice Slot #$targetSeatNum? Their microphone transmission will stop and they will return to the audience area.",
                confirmText = "Kick / Remove User",
                onConfirm = {
                    onRemoveParticipantFromSlot(targetSeatNum, targetOccupant.userId)
                    roomNoticeBanner = "Removed @${targetOccupant.username} from Slot #$targetSeatNum to audience"
                    pendingKickSlotConfirmation = null
                },
                onDismiss = { pendingKickSlotConfirmation = null }
            )
        }

        // Confirmation before locking an occupied voice slot
        val lockOccupiedSeatNum = pendingLockOccupiedSlotConfirmation
        if (lockOccupiedSeatNum != null) {
            val occ = occupiedSeats[lockOccupiedSeatNum]
            ConfirmToolActionDialog(
                title = "Lock Occupied Slot #$lockOccupiedSeatNum?",
                message = "Lock voice Slot #$lockOccupiedSeatNum${if (occ != null) " (currently occupied by @${occ.username})" else ""}? Once locked, no other user can join this slot until unlocked.",
                confirmText = "Lock Slot",
                onConfirm = {
                    onToggleSeatLock(lockOccupiedSeatNum)
                    roomNoticeBanner = "Locked Slot #$lockOccupiedSeatNum"
                    pendingLockOccupiedSlotConfirmation = null
                },
                onDismiss = { pendingLockOccupiedSlotConfirmation = null }
            )
        }

        // Wipe Screen Confirmation Dialog
        if (showWipeConfirmDialog) {
            ConfirmToolActionDialog(
                title = if (room.wipedScreen) "Restore Screen Elements?" else "Wipe Screen Overlay?",
                message = if (room.wipedScreen) {
                    "Restore all seats and chat overlays on the room screen?"
                } else {
                    "Temporarily hide the seats and chat overlay so you can view the clean room background?"
                },
                confirmText = if (room.wipedScreen) "Restore" else "Wipe Screen",
                onConfirm = {
                    onUpdateRoom(room.copy(wipedScreen = !room.wipedScreen))
                    showWipeConfirmDialog = false
                },
                onDismiss = { showWipeConfirmDialog = false }
            )
        }

        // Delete Chat Confirmation Dialog
        if (showDeleteChatConfirmDialog) {
            ConfirmToolActionDialog(
                title = "Delete Room Chat?",
                message = "Are you sure you want to permanently clear all chat messages in this room?",
                confirmText = "Delete Chat",
                onConfirm = {
                    if (callerRole.canClearRoomChat()) {
                        onClearRoomChat()
                        roomNoticeBanner = "Room chat history cleared"
                    } else {
                        roomNoticeBanner = "Only Owner, Super Admin, Admin, or Moderator can clear chat"
                    }
                    showDeleteChatConfirmDialog = false
                },
                onDismiss = { showDeleteChatConfirmDialog = false }
            )
        }

        // Close & Delete Room Confirmation Dialog (Owner only)
        if (showCloseRoomConfirmDialog) {
            ConfirmToolActionDialog(
                title = "Close & Delete Room?",
                message = "As Room Owner, closing this room will end the live stage for all participants and delete the room.",
                confirmText = "Close Room",
                onConfirm = {
                    showCloseRoomConfirmDialog = false
                    musicController.stop()
                    liveKitSessionManager.leaveRoom(deleteOnServerAsHost = true)
                    onCloseAndDeleteRoom()
                },
                onDismiss = { showCloseRoomConfirmDialog = false }
            )
        }

        // LiveKit Audio Output Routing Modal (Speaker / Earpiece / Bluetooth)
        if (showAudioRouteDialog) {
            LiveKitAudioRouteDialog(
                currentRoute = liveKitAudioState.currentRoute,
                availableRoutes = liveKitAudioState.availableRoutes,
                isMicMuted = !isSeated || isMicMuted,
                isInVoiceRoom = true,
                isLocalSpeaking = isSeated && !isMicMuted,
                activeSpeakers = liveKitAudioState.activeSpeakerNames,
                onSelectRoute = { route ->
                    showAudioRouteDialog = false
                    if (route == AudioOutputRoute.BLUETOOTH && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        val hasBtPerm = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.BLUETOOTH_CONNECT
                        ) == PackageManager.PERMISSION_GRANTED
                        if (!hasBtPerm) {
                            try {
                                bluetoothPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
                            } catch (_: Exception) {
                                liveKitAudioRouter.selectAudioRoute(route)
                            }
                            return@LiveKitAudioRouteDialog
                        }
                    }
                    liveKitAudioRouter.selectAudioRoute(route)
                    isSpeakerOn = true
                    roomNoticeBanner = "Audio output switched to ${route.label}"
                },
                onDismiss = { showAudioRouteDialog = false }
            )
        }
    }
}

@Composable
private fun ConfirmToolActionDialog(
    title: String,
    message: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.2.dp, VipGoldBorder, RoundedCornerShape(22.dp))
                .testTag("party_tool_confirm_dialog"),
            shape = RoundedCornerShape(22.dp),
            color = Color(0xFF100E18)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = title,
                    color = NoiseWhite,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 17.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = message,
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
                            .background(Color(0xFF1C1A27))
                            .border(1.dp, NoiseBorder, RoundedCornerShape(14.dp))
                            .clickable { onDismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Cancel", color = NoiseTextSecondary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Brush.horizontalGradient(listOf(NoisePink, NoisePurple)))
                            .clickable { onConfirm() }
                            .testTag("party_tool_confirm_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(confirmText, color = NoiseWhite, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun SeatActionDialog(
    seatNumber: Int,
    occupant: SeatOccupant?,
    isOwnSeat: Boolean,
    callerRole: RoomRole,
    targetRole: RoomRole,
    isSeatLocked: Boolean,
    onToggleMic: () -> Unit,
    onLeaveOrRemoveFromSeat: () -> Unit,
    onToggleLockSeat: () -> Unit,
    onExplicitLockEmptySlot: (Boolean) -> Unit,
    onJoinEmptySlot: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.2.dp, VipGoldBorder, RoundedCornerShape(22.dp))
                .testTag("seat_action_dialog"),
            shape = RoundedCornerShape(22.dp),
            color = Color(0xFF100E18)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (occupant != null) {
                    // OCCUPIED VOICE SLOT MENU (Part 6.A)
                    Text(
                        text = if (isOwnSeat) "Voice Slot #$seatNumber (You)" else "Voice Slot #$seatNumber • @${occupant.username}",
                        color = VipGold,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (isOwnSeat) {
                            "Manage your microphone, slot lock, or leave this slot"
                        } else {
                            "Owner Slot Management • @${occupant.username} (${targetRole.label})"
                        },
                        color = NoiseTextSecondary,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // 1. Mute User / Unmute User
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF1A1829))
                            .border(1.dp, OnlineGreen.copy(alpha = 0.6f), RoundedCornerShape(14.dp))
                            .clickable { onToggleMic() }
                            .testTag("seat_action_toggle_mic"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isOwnSeat) {
                                if (occupant.micMuted) "🎙️ Unmute Microphone" else "🔇 Mute Microphone"
                            } else {
                                if (occupant.micMuted) "🎙️ Unmute User (@${occupant.username})" else "🔇 Mute User (@${occupant.username})"
                            },
                            color = NoiseWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // 2. Kick / Remove User (or Leave Seat if own seat)
                    if (isOwnSeat || callerRole == RoomRole.OWNER || callerRole.canMuteOrKickParticipant(targetRole)) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFF3A1219))
                                .border(1.dp, NoisePink, RoundedCornerShape(14.dp))
                                .clickable { onLeaveOrRemoveFromSeat() }
                                .testTag("seat_action_leave_or_kick"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isOwnSeat) {
                                    "Leave Slot #$seatNumber"
                                } else {
                                    "🚫 Kick / Remove User (@${occupant.username})"
                                },
                                color = NoisePink,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    // 3. Slot Lock / Unlock Slot
                    if (callerRole == RoomRole.OWNER || callerRole.canLockOrUnlockSeat()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFF1F1B2C))
                                .border(1.dp, VipGoldBorder, RoundedCornerShape(14.dp))
                                .clickable { onToggleLockSeat() }
                                .testTag("seat_action_lock_toggle"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isSeatLocked) "🔓 Unlock Slot #$seatNumber" else "🔒 Slot Lock (Slot #$seatNumber)",
                                color = VipGold,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    // 4. Cancel
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF161521))
                            .clickable { onDismiss() }
                            .testTag("seat_action_cancel"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Cancel", color = NoiseTextSecondary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                } else {
                    // EMPTY VOICE SLOT MENU FOR ROOM OWNER / ADMIN (Part 6.B)
                    Text(
                        text = "Voice Slot #$seatNumber (Empty)",
                        color = VipGold,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (isSeatLocked) {
                            "Status: 🔒 Locked • Users cannot join this slot"
                        } else {
                            "Status: 🔓 Unlocked • Open for participants"
                        },
                        color = NoiseTextSecondary,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // 1. Lock Slot
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (!isSeatLocked) Color(0xFF281E38) else Color(0xFF181622))
                            .border(
                                1.dp,
                                if (!isSeatLocked) VipGold else NoiseBorder,
                                RoundedCornerShape(14.dp)
                            )
                            .clickable { onExplicitLockEmptySlot(true) }
                            .testTag("seat_action_lock_slot"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "🔒 Lock Slot",
                            color = if (!isSeatLocked) VipGold else NoiseTextSecondary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // 2. Unlock Slot
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isSeatLocked) Color(0xFF192B24) else Color(0xFF181622))
                            .border(
                                1.dp,
                                if (isSeatLocked) OnlineGreen else NoiseBorder,
                                RoundedCornerShape(14.dp)
                            )
                            .clickable { onExplicitLockEmptySlot(false) }
                            .testTag("seat_action_unlock_slot"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "🔓 Unlock Slot",
                            color = if (isSeatLocked) OnlineGreen else NoiseTextSecondary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Optional Join Slot button if unlocked so Owner/Admin can also sit in an unlocked seat
                    if (!isSeatLocked) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(42.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Brush.horizontalGradient(listOf(NoisePink.copy(alpha = 0.28f), NoisePurple.copy(alpha = 0.28f))))
                                .border(1.dp, NoisePink.copy(alpha = 0.7f), RoundedCornerShape(14.dp))
                                .clickable { onJoinEmptySlot() }
                                .testTag("seat_action_join_slot"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "🎙️ Join Slot #$seatNumber",
                                color = NoiseWhite,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }

            // 3. Cancel
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(NoiseCardElevated)
                            .clickable { onDismiss() }
                            .testTag("seat_action_cancel"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Cancel", color = NoiseTextSecondary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun RoomMusicModal(
    currentTrack: String,
    isPlaying: Boolean,
    volume: Int,
    canControl: Boolean,
    onSelectTrack: (String) -> Unit,
    onTogglePlayPause: () -> Unit,
    onStopMusic: () -> Unit,
    onVolumeChange: (Int) -> Unit,
    onClose: () -> Unit
) {
    val tracks = listOf(
        "Arabian Nights" to "Oriental Synth & Desert Beats",
        "Cyberpunk Synth" to "Neon Future Arpeggio",
        "Midnight Lo-Fi" to "Chill Late Night Chords",
        "Bass House" to "VIP Club Festival Groove",
        "Acoustic Chill" to "Warm Harmonic Lounge"
    )

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .border(1.3.dp, VipGoldBorder, RoundedCornerShape(24.dp))
                .testTag("room_music_modal"),
            shape = RoundedCornerShape(24.dp),
            color = NoiseCard
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = "Music",
                            tint = VipGold,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Room Music Player",
                                color = NoiseWhite,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = if (isPlaying && currentTrack != "None") "Now Playing: $currentTrack" else "Paused / Stopped",
                                color = if (isPlaying) OnlineGreen else NoiseTextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }

                    IconButton(onClick = onClose, modifier = Modifier.size(30.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close Music", tint = NoiseTextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Play/Pause & Stop Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onTogglePlayPause,
                        enabled = canControl,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("music_play_pause_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isPlaying) NoisePurple else NoisePink
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = NoiseWhite,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isPlaying) "Pause" else "Play",
                            color = NoiseWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    Button(
                        onClick = onStopMusic,
                        enabled = canControl,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("music_stop_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF232132)),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Stop,
                            contentDescription = null,
                            tint = NoiseWhite,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Stop",
                            color = NoiseWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Volume Control Slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Volume",
                        color = NoiseWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Text(
                        text = "$volume%",
                        color = VipGold,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
                Slider(
                    value = volume.toFloat(),
                    onValueChange = { onVolumeChange(it.toInt()) },
                    valueRange = 0f..100f,
                    colors = SliderDefaults.colors(
                        thumbColor = VipGold,
                        activeTrackColor = NoisePink,
                        inactiveTrackColor = Color(0xFF252336)
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Select Background Track",
                    color = NoiseWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    tracks.forEach { (trackName, subtitle) ->
                        val selected = currentTrack == trackName
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (selected) Color(0xFF211A34) else Color(0xFF161520))
                                .border(
                                    1.dp,
                                    if (selected) VipGold else NoiseBorder,
                                    RoundedCornerShape(14.dp)
                                )
                                .clickable(enabled = canControl) { onSelectTrack(trackName) }
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = trackName,
                                    color = if (selected) VipGold else NoiseWhite,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = subtitle,
                                    color = NoiseTextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                            if (selected) {
                                Text(
                                    text = if (isPlaying) "🔊 Playing" else "Selected",
                                    color = OnlineGreen,
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
private fun RoomParticipationAndRolesModal(
    room: PartyRoom,
    currentUserId: String,
    callerRole: RoomRole,
    participants: List<RoomParticipant>,
    occupiedSeats: Map<Int, SeatOccupant>,
    onAssignRole: (targetUserId: String, newRole: RoomRole) -> Unit,
    onTransferOwnership: (newOwnerId: String, newOwnerUsername: String) -> Unit,
    onToggleMuteParticipant: (targetUserId: String) -> Unit,
    onKickParticipant: (targetUserId: String) -> Unit,
    onToggleSeatLock: (Int) -> Unit,
    onUpdateRoomSettings: (PartyRoom) -> Unit,
    onRequestCloseRoom: () -> Unit,
    onClose: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Participants & Roles, 1 = Room Settings & Seats
    var editTitle by remember(room.title) { mutableStateOf(room.title) }
    var editDesc by remember(room.description) { mutableStateOf(room.description) }
    var editPrivate by remember(room.privateRoom) { mutableStateOf(room.privateRoom) }
    var editPassword by remember(room.password) { mutableStateOf(room.password) }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp)
                .border(1.3.dp, VipGoldBorder, RoundedCornerShape(24.dp))
                .testTag("room_participation_modal"),
            shape = RoundedCornerShape(24.dp),
            color = NoiseCard
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Room Participation & Admin",
                            color = VipGold,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "Your Role: ${callerRole.label} • ${occupiedSeats.size} Seated / ${participants.size} Total",
                            color = NoiseTextSecondary,
                            fontSize = 11.sp
                        )
                    }
                    IconButton(onClick = onClose, modifier = Modifier.size(30.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = NoiseTextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Tabs
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Participants (${participants.size})", "Settings & Seats").forEachIndexed { idx, label ->
                        val active = selectedTab == idx
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (active) Brush.horizontalGradient(listOf(NoisePink, NoisePurple))
                                    else Brush.horizontalGradient(listOf(NoiseCardElevated, NoiseCardElevated))
                                )
                                .clickable { selectedTab = idx },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                color = if (active) Color.White else NoiseWhite,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (selectedTab == 0) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(340.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(participants, key = { it.userId }) { p ->
                            val pRole = room.roleForUser(p.userId)
                            val seatNum = occupiedSeats.entries.find { it.value.userId == p.userId }?.key
                            val isMuted = p.userId in room.mutedUserIds || (seatNum != null && occupiedSeats[seatNum]?.micMuted == true)
                            val canManage = p.userId != currentUserId && callerRole.canManageTarget(pRole)

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
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        NoiseAvatar(
                                            avatarKey = p.avatarKey.ifBlank { p.username.take(1).uppercase() },
                                            size = 36.dp,
                                            showOnlineDot = true
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = "@${p.username}",
                                                    color = NoiseWhite,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                VipInlineGemBadge(
                                                    vipLevel = if (pRole == RoomRole.OWNER) 10 else ((p.username.hashCode().let { if (it < 0) -it else it } % 9) + 1),
                                                    gemSize = 16.dp,
                                                    showLabel = true
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(
                                                            when (pRole) {
                                                                RoomRole.OWNER -> VipGoldDark
                                                                RoomRole.SUPER_ADMIN -> Color(0xFF3B1D5E)
                                                                RoomRole.ADMIN -> Color(0xFF1E3A8A)
                                                                RoomRole.MODERATOR -> Color(0xFF064E3B)
                                                                RoomRole.MEMBER -> Color(0xFF252436)
                                                            }
                                                        )
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        text = pRole.label,
                                                        color = if (pRole == RoomRole.OWNER) VipGold else NoiseWhite,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        fontSize = 9.sp
                                                    )
                                                }
                                            }
                                            Text(
                                                text = if (seatNum != null) {
                                                    "Seat #$seatNum • ${if (isMuted) "🔇 Muted" else "🎙️ Mic Live"}"
                                                } else {
                                                    "Listening in Room"
                                                },
                                                color = NoiseTextSecondary,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }

                                    if (p.userId != currentUserId && callerRole.canMuteOrKickParticipant(pRole)) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(Color(0xFF252338))
                                                    .clickable { onToggleMuteParticipant(p.userId) }
                                                    .padding(horizontal = 8.dp, vertical = 5.dp)
                                            ) {
                                                Text(
                                                    text = if (p.userId in room.mutedUserIds) "Unmute" else "Mute",
                                                    color = VipGold,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(Color(0xFF3B1219))
                                                    .clickable { onKickParticipant(p.userId) }
                                                    .padding(horizontal = 8.dp, vertical = 5.dp)
                                            ) {
                                                Text(
                                                    text = "Kick",
                                                    color = NoisePink,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }

                                // Role Assignment Strip for authorized admins
                                if (canManage) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        val assignableRoles = listOf(
                                            RoomRole.SUPER_ADMIN,
                                            RoomRole.ADMIN,
                                            RoomRole.MODERATOR,
                                            RoomRole.MEMBER
                                        ).filter { callerRole.canAssignRole(it) }

                                        assignableRoles.forEach { targetNewRole ->
                                            val activeRole = pRole == targetNewRole
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(if (activeRole) NoisePink.copy(alpha = 0.25f) else Color(0xFF1F1D2E))
                                                    .border(
                                                        1.dp,
                                                        if (activeRole) NoisePink else NoiseBorder,
                                                        RoundedCornerShape(8.dp)
                                                    )
                                                    .clickable { onAssignRole(p.userId, targetNewRole) }
                                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                            ) {
                                                Text(
                                                    text = targetNewRole.label,
                                                    color = if (activeRole) NoiseWhite else NoiseTextSecondary,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }

                                        if (callerRole == RoomRole.OWNER) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(VipGoldDark)
                                                    .border(1.dp, VipGold, RoundedCornerShape(8.dp))
                                                    .clickable { onTransferOwnership(p.userId, p.username) }
                                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                            ) {
                                                Text(
                                                    text = "👑 Make Owner",
                                                    color = VipGold,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.ExtraBold
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Tab 1: Room Settings & Seat Locks
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(340.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        if (callerRole.canManageRoomSettings()) {
                            Text("Edit Room Name & Description", color = NoiseWhite, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = editTitle,
                                onValueChange = { editTitle = it },
                                singleLine = true,
                                label = { Text("Room Name", fontSize = 11.sp) },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = editDesc,
                                onValueChange = { editDesc = it },
                                maxLines = 2,
                                label = { Text("Room Description", fontSize = 11.sp) },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (editPrivate) "🔒 Private Room" else "🌐 Public Room",
                                    color = NoiseWhite,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Switch(
                                    checked = editPrivate,
                                    onCheckedChange = { editPrivate = it }
                                )
                            }
                            if (editPrivate) {
                                OutlinedTextField(
                                    value = editPassword,
                                    onValueChange = { editPassword = it },
                                    singleLine = true,
                                    label = { Text("Room Password", fontSize = 11.sp) },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    onUpdateRoomSettings(
                                        room.copy(
                                            title = editTitle.trim().ifBlank { room.title },
                                            description = editDesc.trim(),
                                            privateRoom = editPrivate,
                                            password = if (editPrivate) editPassword.trim() else ""
                                        )
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = NoisePurple)
                            ) {
                                Text("Save Room Info", color = NoiseWhite, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                        }

                        if (callerRole.canLockOrUnlockSeat()) {
                            Text("Lock / Unlock Seats (1 - 12)", color = VipGold, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                (1..12).forEach { sNum ->
                                    val locked = sNum.toLong() in room.lockedSeats
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (locked) Color(0xFF3A1219) else Color(0xFF1A1829))
                                            .border(
                                                1.dp,
                                                if (locked) NoisePink else NoiseBorder,
                                                RoundedCornerShape(10.dp)
                                            )
                                            .clickable { onToggleSeatLock(sNum) }
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = if (locked) Icons.Default.Lock else Icons.Default.LockOpen,
                                                contentDescription = null,
                                                tint = if (locked) NoisePink else OnlineGreen,
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "#$sNum",
                                                color = NoiseWhite,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        if (callerRole.canDeleteOrCloseRoom()) {
                            Spacer(modifier = Modifier.height(18.dp))
                            Button(
                                onClick = onRequestCloseRoom,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("close_and_delete_room_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                            ) {
                                Text(
                                    text = "Close & Delete Room (Owner)",
                                    color = NoiseWhite,
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
}

@Composable
private fun RoomThemeBackground(
    themeKey: String,
    roomThemes: List<RoomThemeOption>
) {
    val matchedTheme = roomThemes.find { it.key == themeKey }
    Box(modifier = Modifier.fillMaxSize()) {
        if (themeKey.startsWith("content://") || themeKey.startsWith("file://")) {
            NoiseMediaImage(
                mediaKey = themeKey,
                contentDescription = "Custom Room Theme",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f))
            )
        } else {
            val topColor = Color(matchedTheme?.primaryHex ?: 0xFF091326)
            val bottomColor = Color(matchedTheme?.secondaryHex ?: 0xFF1B2942)
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                drawRect(
                    brush = Brush.verticalGradient(
                        listOf(topColor, bottomColor, Color(0xFF090A10))
                    )
                )

                val starOffsets = listOf(
                    Offset(w * 0.12f, h * 0.08f),
                    Offset(w * 0.28f, h * 0.05f),
                    Offset(w * 0.45f, h * 0.11f),
                    Offset(w * 0.62f, h * 0.06f),
                    Offset(w * 0.84f, h * 0.09f),
                    Offset(w * 0.19f, h * 0.18f),
                    Offset(w * 0.54f, h * 0.16f),
                    Offset(w * 0.76f, h * 0.21f),
                    Offset(w * 0.35f, h * 0.24f),
                    Offset(w * 0.91f, h * 0.15f)
                )
                starOffsets.forEachIndexed { idx, pos ->
                    drawCircle(
                        color = Color.White.copy(alpha = if (idx % 2 == 0) 0.8f else 0.45f),
                        radius = if (idx % 3 == 0) 3.5f else 2.2f,
                        center = pos
                    )
                }

                val moonCenter = Offset(w * 0.78f, h * 0.14f)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.55f),
                            Color(0xFFBAE6FD).copy(alpha = 0.18f),
                            Color.Transparent
                        ),
                        center = moonCenter,
                        radius = w * 0.16f
                    ),
                    radius = w * 0.16f,
                    center = moonCenter
                )
                drawCircle(
                    color = Color(0xFFE2E8F0).copy(alpha = 0.75f),
                    radius = w * 0.055f,
                    center = moonCenter
                )

                val mountainPath = Path().apply {
                    moveTo(0f, h * 0.55f)
                    lineTo(w * 0.18f, h * 0.41f)
                    lineTo(w * 0.36f, h * 0.49f)
                    lineTo(w * 0.56f, h * 0.37f)
                    lineTo(w * 0.75f, h * 0.46f)
                    lineTo(w * 0.92f, h * 0.39f)
                    lineTo(w, h * 0.48f)
                    lineTo(w, h)
                    lineTo(0f, h)
                    close()
                }
                drawPath(
                    path = mountainPath,
                    brush = Brush.verticalGradient(
                        listOf(Color(0xFF111827).copy(alpha = 0.88f), Color(0xFF06070B))
                    )
                )
            }
        }
    }
}

@Composable
private fun RoyalCrownHostSeat(
    seatNumber: Int,
    occupant: SeatOccupant?,
    isLocked: Boolean,
    diamondsCount: Long,
    crownPrimary: Color,
    crownSecondary: Color,
    currentUserId: String,
    hasMicPermission: Boolean,
    onClick: () -> Unit
) {
    val isOccupied = occupant != null
    val isMuted = occupant?.micMuted ?: true
    val slotAudioBands = rememberSlotVoiceAudioBands(
        seatNumber = seatNumber,
        occupantUserId = occupant?.userId ?: "",
        isOccupied = isOccupied,
        isMuted = isMuted,
        isLocalUserSeat = occupant?.userId == currentUserId,
        hasMicPermission = hasMicPermission,
        isSpeakingSignal = isOccupied && !isMuted
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable { onClick() }
            .testTag("party_seat_$seatNumber")
    ) {
        Box(
            modifier = Modifier.size(94.dp),
            contentAlignment = Alignment.Center
        ) {
            // Individual 3D Wireframe Sphere Visualizer with 100% TRANSPARENT background
            Sphere3DVoiceVisualizer(
                seatNumber = seatNumber,
                audioBands = slotAudioBands,
                isOccupied = isOccupied,
                isMuted = isMuted,
                detailLevel = 2,
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("slot_3d_visualizer_$seatNumber")
            )

            // Center occupant avatar or transparent '+' / Lock icon (NO solid black circle behind the sphere)
            when {
                occupant != null -> {
                    NoiseAvatar(
                        avatarKey = occupant.avatarKey.ifBlank { occupant.username.take(1).uppercase() },
                        size = 46.dp,
                        showOnlineDot = true
                    )
                }
                isLocked -> {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Locked Seat $seatNumber",
                        tint = VipGold,
                        modifier = Modifier.size(22.dp)
                    )
                }
                else -> {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Join Royal Seat $seatNumber",
                        tint = VipGold,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Top Royal Crown Emblem
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = (-4).dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        Brush.horizontalGradient(listOf(VipGold, crownPrimary))
                    )
                    .padding(horizontal = 6.dp, vertical = 1.dp)
            ) {
                Text(
                    text = "👑 VIP",
                    color = Color.Black,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 8.sp
                )
            }

            // Visible Lock Badge when an occupied slot is also locked by the owner
            if (isLocked && occupant != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(VipGoldDark)
                        .border(1.dp, VipGold, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Locked Slot $seatNumber",
                        tint = VipGold,
                        modifier = Modifier.size(10.dp)
                    )
                }
            }

            // Live Mic / Muted Indicator on occupied Royal Seat
            if (occupant != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(if (occupant.micMuted) Color(0xFF3A1219) else OnlineGreen)
                        .border(1.dp, NoiseBlack, CircleShape)
                        .testTag("seat_mic_badge_$seatNumber"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (occupant.micMuted) Icons.Default.MicOff else Icons.Default.Mic,
                        contentDescription = if (occupant.micMuted) "Muted" else "Unmuted",
                        tint = if (occupant.micMuted) NoisePink else Color.Black,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = if (occupant != null) "$seatNumber. ${occupant.username}" else "$seatNumber. Empty",
                color = NoiseWhite,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (occupant != null) {
                Spacer(modifier = Modifier.width(3.dp))
                VipInlineGemBadge(
                    vipLevel = if (seatNumber == 1) 10 else 9,
                    gemSize = 14.dp,
                    showLabel = false
                )
            }
        }

        Spacer(modifier = Modifier.height(2.dp))

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(Color.Black.copy(alpha = 0.55f))
                .border(0.8.dp, VipGold.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                .padding(horizontal = 8.dp, vertical = 2.dp)
        ) {
            Text(
                text = "💎 ${formatCompactCount(diamondsCount)}",
                color = VipGold,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp
            )
        }
    }
}

@Composable
private fun ParticipantSeatItem(
    seatNumber: Int,
    isLocked: Boolean,
    occupant: SeatOccupant?,
    isCurrentUser: Boolean,
    currentUserId: String,
    hasMicPermission: Boolean,
    onClick: () -> Unit
) {
    val isOccupied = occupant != null
    val isMuted = occupant?.micMuted ?: true
    val slotAudioBands = rememberSlotVoiceAudioBands(
        seatNumber = seatNumber,
        occupantUserId = occupant?.userId ?: "",
        isOccupied = isOccupied,
        isMuted = isMuted,
        isLocalUserSeat = occupant?.userId == currentUserId,
        hasMicPermission = hasMicPermission,
        isSpeakingSignal = isOccupied && !isMuted
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable { onClick() }
            .testTag("party_seat_$seatNumber")
    ) {
        Box(
            modifier = Modifier.size(62.dp),
            contentAlignment = Alignment.Center
        ) {
            // Individual 3D Wireframe Sphere Visualizer with 100% TRANSPARENT background
            Sphere3DVoiceVisualizer(
                seatNumber = seatNumber,
                audioBands = slotAudioBands,
                isOccupied = isOccupied,
                isMuted = isMuted,
                detailLevel = 1,
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("slot_3d_visualizer_$seatNumber")
            )

            // Center occupant avatar or transparent '+' / Lock icon (NO solid black circle behind the sphere)
            when {
                occupant != null -> {
                    NoiseAvatar(
                        avatarKey = occupant.avatarKey.ifBlank { occupant.username.take(1).uppercase() },
                        size = 34.dp,
                        showOnlineDot = true
                    )
                }
                isLocked -> {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Locked Seat $seatNumber",
                        tint = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier.size(18.dp)
                    )
                }
                else -> {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Open Seat $seatNumber",
                        tint = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Visible Lock Badge when an occupied slot is also locked by the owner
            if (isLocked && occupant != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .size(15.dp)
                        .clip(CircleShape)
                        .background(VipGoldDark)
                        .border(1.dp, VipGold, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Locked Slot $seatNumber",
                        tint = VipGold,
                        modifier = Modifier.size(9.dp)
                    )
                }
            }

            if (occupant != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(if (occupant.micMuted) Color(0xFF3A1219) else OnlineGreen)
                        .border(1.dp, NoiseBlack, CircleShape)
                        .testTag("seat_mic_badge_$seatNumber"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (occupant.micMuted) Icons.Default.MicOff else Icons.Default.Mic,
                        contentDescription = if (occupant.micMuted) "Muted" else "Unmuted",
                        tint = if (occupant.micMuted) NoisePink else Color.Black,
                        modifier = Modifier.size(10.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(3.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = occupant?.username?.take(8) ?: "$seatNumber",
                color = if (occupant != null) NoiseWhite else Color.White.copy(alpha = 0.85f),
                fontSize = 11.sp,
                fontWeight = if (occupant != null) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1
            )
            if (occupant != null) {
                Spacer(modifier = Modifier.width(2.dp))
                VipInlineGemBadge(
                    vipLevel = ((seatNumber % 10) + 1),
                    gemSize = 13.dp,
                    showLabel = false
                )
            }
        }
    }
}

@Composable
private fun ToolsBottomSheet(
    pkActive: Boolean,
    isWipedScreen: Boolean,
    currentMusic: String,
    onWipeScreen: () -> Unit,
    onOpenBackgrounds: () -> Unit,
    onOpenMusic: () -> Unit,
    onDeleteChat: () -> Unit,
    onShareRoom: () -> Unit,
    onParticipation: () -> Unit,
    onTogglePk: () -> Unit,
    onOpenGames: () -> Unit = {},
    onClose: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = false) {}
            .testTag("party_tools_sheet"),
        shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
        color = NoiseCard
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp)
        ) {
            // Top Row: [🛠️ Tools] pill | X
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(NoiseCardElevated)
                        .border(1.dp, NoisePink.copy(alpha = 0.65f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 14.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Build,
                        contentDescription = null,
                        tint = NoisePink,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Tools",
                        color = NoiseWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(NoiseCardElevated)
                        .clickable { onClose() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Tools",
                        tint = NoiseTextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Row 1: Wipe screen | Backgroun | Music | Delete Chat
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ToolCircleButton(
                    icon = Icons.Default.CleaningServices,
                    label = if (isWipedScreen) "Restore UI" else "Wipe screen",
                    testTag = "tool_wipe_screen",
                    onClick = onWipeScreen
                )
                ToolCircleButton(
                    icon = Icons.Default.Palette,
                    label = "Backgroun",
                    testTag = "tool_background",
                    onClick = onOpenBackgrounds
                )
                ToolCircleButton(
                    icon = Icons.Default.MusicNote,
                    label = "Music",
                    testTag = "tool_music",
                    onClick = onOpenMusic
                )
                ToolCircleButton(
                    icon = Icons.Default.DeleteOutline,
                    label = "Delete Chat",
                    testTag = "tool_delete_chat",
                    onClick = onDeleteChat
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Row 2: Share | Participation
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(28.dp)
            ) {
                ToolCircleButton(
                    icon = Icons.Default.Share,
                    label = "Share",
                    testTag = "tool_share",
                    onClick = onShareRoom
                )
                ToolCircleButton(
                    icon = Icons.Default.Schedule,
                    label = "Participation",
                    testTag = "tool_participation",
                    onClick = onParticipation
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Interactive Features",
                color = NoiseWhite,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Ornate PK Square Button + Chatroom 3D Games Button
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable { onTogglePk() }
                        .testTag("party_tools_pk_button")
                ) {
                    Box(
                        modifier = Modifier
                            .size(58.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFF7C2D12), Color(0xFFD97706), Color(0xFF4C1D95))
                                )
                            )
                            .border(1.5.dp, VipGold, RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "PK",
                            color = VipGold,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 20.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (pkActive) "PK (Active)" else "PK",
                        color = NoiseTextSecondary,
                        fontSize = 11.sp
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable { onOpenGames() }
                        .testTag("party_tools_games_button")
                ) {
                    Box(
                        modifier = Modifier
                            .size(58.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(NoisePink, NoisePurple)
                                )
                            )
                            .border(1.5.dp, VipGold, RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SportsEsports,
                            contentDescription = "Chatroom Games",
                            tint = VipGold,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Games",
                        color = NoiseWhite,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun ToolCircleButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    testTag: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(72.dp)
            .clickable { onClick() }
            .testTag(testTag)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(NoiseCardElevated)
                .border(1.dp, NoiseBorder, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = NoiseWhite,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            color = NoiseTextSecondary,
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

@Composable
private fun GiftCoinBottomSheet(
    userCoins: Long,
    occupiedSeats: Map<Int, SeatOccupant>,
    onSendGift: (gift: PartyGiftCatalogItem, quantity: Int, targetSeat: Int) -> Unit,
    onTopUpClick: () -> Unit,
    onClose: () -> Unit
) {
    val giftCatalog = remember {
        listOf(
            PartyGiftCatalogItem("ltl_hart", "Ltl hart", "💗", 1L, "Hot"),
            PartyGiftCatalogItem("rose", "Rose", "🌹", 9L, "Hot"),
            PartyGiftCatalogItem("star", "Star", "⭐", 99L, "Hot"),
            PartyGiftCatalogItem("kiss", "Kiss", "💋", 199L, "Hot"),
            PartyGiftCatalogItem("love_ring", "Love Ring", "💝", 520L, "Hot"),
            PartyGiftCatalogItem("zomo_chat", "Zomo chat", "🦄", 999L, "Hot"),
            PartyGiftCatalogItem("day_longing", "Day longing", "🧸", 1314L, "Hot"),
            PartyGiftCatalogItem("fligo", "Fligo", "🦩", 2999L, "Hot")
        )
    }

    var selectedCategory by remember { mutableStateOf("Hot") }
    var selectedGift by remember { mutableStateOf(giftCatalog.first()) }
    var selectedQuantity by remember { mutableIntStateOf(1) }
    var selectedTargetSeat by remember { mutableIntStateOf(0) } // 0 = All

    val categories = listOf("Hot", "Classic", "3D", "VIP", "Love", "Mood")
    val quantities = listOf(1, 5, 10, 66)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = false) {}
            .testTag("party_gift_coin_sheet"),
        shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
        color = NoiseCard
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Top Gradient Banner: 👑 Gift Coin | (?) matching Left Phone in screenshot
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color(0xFF3B82F6), Color(0xFF7C3AED), Color(0xFF9333EA))
                        )
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "👑", fontSize = 18.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Gift Coin",
                        color = NoiseWhite,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp
                    )
                }

                Icon(
                    imageVector = Icons.Default.HelpOutline,
                    contentDescription = "Gift Help",
                    tint = NoiseWhite,
                    modifier = Modifier
                        .size(20.dp)
                        .clickable { onClose() }
                )
            }

            // Recipient Avatar Row from genuinely occupied seats + [All 👤] button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (occupiedSeats.isEmpty()) {
                        Text(
                            text = "Send gift to stage",
                            color = NoiseTextSecondary,
                            fontSize = 11.sp
                        )
                    } else {
                        occupiedSeats.entries.sortedBy { it.key }.take(6).forEach { (seatIdx, occ) ->
                            val isTarget = selectedTargetSeat == seatIdx
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .border(
                                        width = if (isTarget) 2.dp else 0.dp,
                                        color = if (isTarget) NoisePink else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable { selectedTargetSeat = seatIdx }
                            ) {
                                NoiseAvatar(
                                    avatarKey = occ.avatarKey.ifBlank { occ.username.take(1).uppercase() },
                                    size = 32.dp
                                )
                            }
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (selectedTargetSeat == 0) Color(0xFF8B5CF6) else Color(0xFF232131)
                        )
                        .clickable { selectedTargetSeat = 0 }
                        .padding(horizontal = 12.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = "All 👤",
                        color = NoiseWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
            }

            // Category Tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                categories.forEach { cat ->
                    val isSelected = selectedCategory == cat
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable { selectedCategory = cat }
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = cat,
                                color = if (isSelected) NoiseWhite else NoiseTextMuted,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Box(
                                modifier = Modifier
                                    .size(4.dp)
                                    .clip(CircleShape)
                                    .background(NoisePink)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .width(20.dp)
                                .height(2.dp)
                                .background(if (isSelected) NoisePink else Color.Transparent)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 4x2 Grid of Gifts
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                giftCatalog.chunked(4).forEach { rowGifts ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowGifts.forEach { gift ->
                            val isSelected = selectedGift.id == gift.id
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(NoiseCardElevated)
                                    .border(
                                        width = if (isSelected) 1.5.dp else 1.dp,
                                        color = if (isSelected) NoisePink else NoiseBorder,
                                        shape = RoundedCornerShape(14.dp)
                                    )
                                    .clickable {
                                        selectedGift = gift
                                        onSendGift(gift, selectedQuantity, if (selectedTargetSeat == 0) 1 else selectedTargetSeat)
                                    }
                                    .padding(vertical = 10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(text = gift.emoji, fontSize = 26.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = gift.name,
                                    color = NoiseWhite,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "🪙 ${gift.cost}",
                                    color = VipGold,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Bottom Coin Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { onTopUpClick() }
                ) {
                    Text(
                        text = "🪙 ${formatCoins(userCoins)} >",
                        color = VipGold,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.sp
                    )
                }

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(NoiseCardElevated)
                        .border(1.dp, NoiseBorder, RoundedCornerShape(20.dp)),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    quantities.forEach { qty ->
                        val active = selectedQuantity == qty
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(if (active) NoisePink else Color.Transparent)
                                .clickable { selectedQuantity = qty }
                                .padding(horizontal = 9.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "$qty",
                                color = if (active) Color.White else NoiseTextSecondary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color(0xFF60A5FA), Color(0xFFA855F7))
                                )
                            )
                            .clickable {
                                val needed = selectedGift.cost * selectedQuantity
                                if (userCoins >= needed) {
                                    onSendGift(selectedGift, selectedQuantity, if (selectedTargetSeat == 0) 1 else selectedTargetSeat)
                                } else {
                                    onTopUpClick()
                                }
                            }
                            .padding(horizontal = 14.dp, vertical = 7.dp)
                    ) {
                        Text(
                            text = "Top Up",
                            color = NoiseWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectYourThemeModal(
    themes: List<RoomThemeOption>,
    selectedThemeKey: String,
    onSelectTheme: (String) -> Unit,
    onCustomizeFromGallery: () -> Unit,
    onResetDefault: () -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit
) {
    var selectedTab by remember { mutableStateOf("Gallery") }
    val tabs = listOf("Gallery", "Premium", "Customize")
    val isCustomImage = selectedThemeKey.startsWith("content://") || selectedThemeKey.startsWith("file://")

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .testTag("select_your_theme_modal"),
        color = NoiseCard
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            // Header: [<] | Select your theme | [Reset]
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(NoiseCardElevated)
                        .clickable { onBack() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = NoiseWhite,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Text(
                    text = "Select your theme",
                    color = NoiseWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(NoiseCardElevated)
                        .border(1.dp, NoiseBorder, RoundedCornerShape(14.dp))
                        .clickable { onResetDefault() }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .testTag("reset_room_theme_button"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Reset Default",
                        tint = VipGold,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Default",
                        color = VipGold,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 3 Filter Pills: Gallery | Premium | Customize matching Right Phone in screenshot
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                tabs.forEach { tab ->
                    val isSelected = selectedTab == tab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .clip(RoundedCornerShape(19.dp))
                            .background(
                                if (isSelected) {
                                    Brush.horizontalGradient(
                                        listOf(Color(0xFF60A5FA), Color(0xFFA855F7))
                                    )
                                } else {
                                    Brush.horizontalGradient(
                                        listOf(NoiseCardElevated, NoiseCardElevated)
                                    )
                                }
                            )
                            .clickable {
                                selectedTab = tab
                                if (tab == "Customize") {
                                    onCustomizeFromGallery()
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = tab,
                            color = if (isSelected) Color.White else NoiseTextSecondary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Choose from Phone Gallery button + Live Preview when custom image is selected
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(NoiseCardElevated)
                    .border(1.dp, if (isCustomImage) VipGold else NoiseBorder, RoundedCornerShape(14.dp))
                    .clickable { onCustomizeFromGallery() }
                    .padding(horizontal = 14.dp, vertical = 10.dp)
                    .testTag("choose_gallery_background_button"),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.FileUpload,
                        contentDescription = null,
                        tint = VipGold,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Choose Image from Phone Gallery",
                            color = NoiseWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                        Text(
                            text = if (isCustomImage) "Custom gallery image ready to preview & apply" else "Upload any photo from your device",
                            color = NoiseTextSecondary,
                            fontSize = 10.sp
                        )
                    }
                }
                if (isCustomImage) {
                    NoiseMediaImage(
                        mediaKey = selectedThemeKey,
                        contentDescription = "Custom Preview",
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 3x4 Grid of Theme Cards matching Right Phone in screenshot
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(themes, key = { it.key }) { theme ->
                    val isSelected = selectedThemeKey == theme.key
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(0.75f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color(theme.primaryHex), Color(theme.secondaryHex))
                                )
                            )
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) Color(0xFFA855F7) else Color(0xFF282634),
                                shape = RoundedCornerShape(14.dp)
                            )
                            .clickable { onSelectTheme(theme.key) }
                    ) {
                        if (theme.drawableKey.isNotBlank()) {
                            NoiseMediaImage(
                                mediaKey = theme.drawableKey,
                                contentDescription = theme.title,
                                modifier = Modifier.fillMaxSize()
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color(theme.primaryHex).copy(alpha = 0.35f))
                            )
                        }

                        Text(
                            text = theme.title,
                            color = NoiseWhite,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(8.dp)
                        )

                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(
                                            listOf(Color(0xFF818CF8), Color(0xFFA855F7))
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected Theme",
                                    tint = NoiseWhite,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Full-width Save Button matching bottom of Right Phone in screenshot
            Button(
                onClick = onSave,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .clip(RoundedCornerShape(25.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color(0xFF60A5FA), Color(0xFFA855F7), Color(0xFFEC4899))
                        )
                    )
                    .testTag("save_room_theme_button"),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
            ) {
                Text(
                    text = "Save",
                    color = NoiseWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
    }
}

private fun formatCompactCount(value: Long): String {
    return when {
        value >= 1_000_000L -> String.format(java.util.Locale.US, "%.1fM", value / 1_000_000.0)
        value >= 1_000L -> String.format(java.util.Locale.US, "%.1fK", value / 1_000.0)
        else -> value.toString()
    }
}
