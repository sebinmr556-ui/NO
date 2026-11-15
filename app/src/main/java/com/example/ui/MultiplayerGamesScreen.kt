package com.example.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.GameInviteDoc
import com.example.data.GameMatchResultDoc
import com.example.data.GameRoomDoc
import com.example.data.GraphicsQualityLevel
import com.example.data.NoiseGameCatalogItem
import com.example.data.UserProfile
import com.example.ui.theme.LightLavender
import com.example.ui.theme.NoiseBorder
import com.example.ui.theme.NoiseCard
import com.example.ui.theme.NoiseCardElevated
import com.example.ui.theme.NoiseNormalText
import com.example.ui.theme.NoiseSurface
import com.example.ui.theme.NoiseWhite
import com.example.ui.theme.ThemeManager
import com.example.ui.theme.noisePrimaryActionBrush

private data class ComingSoonGameItem(
    val id: String,
    val title: String,
    val supportedPlayersLabel: String,
    val subtitle: String,
    val accentColor: Color,
    val secondaryColor: Color
)

@Composable
fun MultiplayerGamesScreen(
    currentUser: UserProfile,
    gameCatalog: List<NoiseGameCatalogItem>,
    activeGameRooms: List<GameRoomDoc>,
    incomingInvites: List<GameInviteDoc>,
    leaderboardUsers: List<UserProfile>,
    gameHistory: List<GameMatchResultDoc>,
    registeredAccounts: List<UserProfile>,
    graphicsQuality: GraphicsQualityLevel,
    onSelectGraphicsQuality: (GraphicsQualityLevel) -> Unit,
    onCreateAndOpenGameRoom: (NoiseGameCatalogItem, String, Int, Boolean) -> Unit,
    onJoinGameRoomByCodeOrId: (String) -> Unit,
    onRespondToGameInvite: (GameInviteDoc, Boolean) -> Unit,
    onInviteFriendToGame: (GameRoomDoc, UserProfile) -> Unit,
    onInspectUserProfile: (String) -> Unit
) {
    var selectedSectionTab by remember { mutableStateOf("Lobby") } // "Lobby", "Leaderboard", "History"
    var showCreateRoomDialogFor by remember { mutableStateOf<NoiseGameCatalogItem?>(null) }
    var showJoinByCodeDialog by remember { mutableStateOf(false) }
    var showInviteFriendsDialog by remember { mutableStateOf(false) }
    var showGraphicsModal by remember { mutableStateOf(false) }

    val featuredGames = remember(gameCatalog) { gameCatalog.filter { it.featured } }
    val recentlyPlayedItems = remember(currentUser.recentlyPlayedGames, gameCatalog) {
        val mapped = currentUser.recentlyPlayedGames.mapNotNull { gType ->
            gameCatalog.firstOrNull { it.gameType == gType }
        }
        mapped.ifEmpty { gameCatalog.take(3) }
    }
    val currentUid = currentUser.userId.ifBlank { currentUser.id }
    val friendsPlaying = remember(registeredAccounts, currentUser.followedUserIds) {
        registeredAccounts.filter { acc ->
            val uid = acc.userId.ifBlank { acc.id }
            uid != currentUid && (uid in currentUser.followedUserIds || acc.online)
        }
    }

    val comingSoonGames = remember {
        listOf(
            ComingSoonGameItem(
                id = "chess_3d",
                title = "3D Chess Arena",
                supportedPlayersLabel = "2 Players (1v1)",
                subtitle = "Grandmaster 3D tournament chess with voice chat",
                accentColor = Color(0xFF8B5CF6),
                secondaryColor = Color(0xFF31105A)
            ),
            ComingSoonGameItem(
                id = "bowling_3d",
                title = "3D Bowling Pro",
                supportedPlayersLabel = "2 - 4 Players",
                subtitle = "Neon alley physics & curved spin bowling",
                accentColor = Color(0xFF00E5FF),
                secondaryColor = Color(0xFF09364F)
            )
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(NoiseBgBlack)
            .statusBarsPadding()
            .testTag("multiplayer_games_screen"),
        contentPadding = PaddingValues(bottom = 110.dp)
    ) {
        // 1. HEADER + GRAPHICS QUALITY + PLAYER RANK BADGE
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.SportsEsports,
                                contentDescription = null,
                                tint = NoisePink,
                                modifier = Modifier.size(26.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "NOISE 3D GAMES",
                                color = NoiseWhite,
                                fontWeight = FontWeight.Black,
                                fontSize = 21.sp
                            )
                        }
                        Text(
                            text = "11 Complete Games: Ludo • Carrom • Pool • Racing • Quiz • Chess • Checkers • Dominoes & More",
                            color = NoiseTextSecondary,
                            fontSize = 11.sp
                        )
                    }

                    // Graphics Quality Chip (Low / Medium / High)
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (ThemeManager.isLightMode) LightLavender else Color(0xFF1A152C),
                        modifier = Modifier
                            .border(1.dp, NoiseGold.copy(alpha = 0.6f), RoundedCornerShape(14.dp))
                            .clickable { showGraphicsModal = true }
                            .testTag("games_graphics_quality_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = "Graphics Quality",
                                tint = NoiseGold,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${graphicsQuality.label} • ${graphicsQuality.targetFps}FPS",
                                color = if (ThemeManager.isLightMode) NoiseWhite else NoiseGold,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Player Gaming Stats & VIP Rank Banner
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (ThemeManager.isLightMode) LightLavender else Color(0xFF161128),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            1.dp,
                            Brush.horizontalGradient(listOf(NoisePink, NoisePurple, NoiseGold)),
                            RoundedCornerShape(20.dp)
                        )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = currentUser.gameRankTitle.ifBlank { "Rookie Pro" }.uppercase(),
                                color = NoiseGold,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 11.sp
                            )
                            Text(
                                text = "@${currentUser.username}",
                                color = NoiseWhite,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = "Wins: ${currentUser.gameWins} • Rating: ${currentUser.gameRating} MMR • Coins: ${currentUser.coins}",
                                color = NoiseTextSecondary,
                                fontSize = 11.sp
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    showCreateRoomDialogFor = gameCatalog.firstOrNull()
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = NoisePink,
                                    contentColor = Color.White
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                modifier = Modifier.testTag("create_game_room_button")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Create Room", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = { showJoinByCodeDialog = true },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = NoisePurple,
                                    contentColor = Color.White
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                modifier = Modifier.testTag("join_game_room_button")
                            ) {
                                Text("Join Code", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // 2. INCOMING GAME INVITATIONS (Accept / Decline)
        if (incomingInvites.isNotEmpty()) {
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                    Text(
                        text = "PENDING GAME INVITATIONS (${incomingInvites.size})",
                        color = NoiseGold,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    incomingInvites.forEach { invite ->
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (ThemeManager.isLightMode) LightLavender else Color(0xFF1F1638),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .border(1.dp, NoiseGold, RoundedCornerShape(16.dp))
                                .testTag("game_invite_card_${invite.id}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "@${invite.senderUsername} invited you to ${invite.gameTitle}",
                                        color = NoiseWhite,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = if (invite.chatRoomTitle.isNotBlank()) {
                                            "Chatroom: ${invite.chatRoomTitle} • Code: ${invite.roomCode}"
                                        } else {
                                            "Room Code: ${invite.roomCode}"
                                        },
                                        color = NoiseTextSecondary,
                                        fontSize = 11.sp
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    IconButton(
                                        onClick = { onRespondToGameInvite(invite, true) },
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF00E676).copy(alpha = 0.25f))
                                            .testTag("accept_game_invite_${invite.id}")
                                    ) {
                                        Icon(Icons.Default.Check, contentDescription = "Accept", tint = Color(0xFF00E676))
                                    }
                                    IconButton(
                                        onClick = { onRespondToGameInvite(invite, false) },
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFFF1744).copy(alpha = 0.25f))
                                            .testTag("decline_game_invite_${invite.id}")
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "Decline", tint = Color(0xFFFF5252))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. SECTION NAVIGATION PILLS (Lobby / Leaderboard / My Game History / Invite Friends)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    "Lobby" to Icons.Default.SportsEsports,
                    "Leaderboard" to Icons.Default.EmojiEvents,
                    "History" to Icons.Default.History
                ).forEach { (tabName, icon) ->
                    val selected = selectedSectionTab == tabName
                    val tabTextColor = if (selected) Color.White else NoiseNormalText
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (selected) Color.Transparent else (if (ThemeManager.isLightMode) NoiseCard else Color(0xFF171329)),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .then(
                                if (selected) {
                                    Modifier.background(noisePrimaryActionBrush())
                                } else {
                                    Modifier.border(1.dp, NoiseBorder, RoundedCornerShape(14.dp))
                                }
                            )
                            .clickable { selectedSectionTab = tabName }
                            .testTag("games_tab_${tabName.lowercase()}")
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(icon, contentDescription = null, tint = tabTextColor, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(tabName, color = tabTextColor, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (ThemeManager.isLightMode) LightLavender else Color(0xFF171329),
                    modifier = Modifier
                        .clickable { showInviteFriendsDialog = true }
                        .border(1.dp, NoisePurple, RoundedCornerShape(14.dp))
                        .testTag("games_invite_friends_pill")
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.PersonAdd, contentDescription = "Invite Friends", tint = NoisePurple, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Invite", color = if (ThemeManager.isLightMode) NoiseWhite else NoiseGold, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }

        if (selectedSectionTab == "Leaderboard") {
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Text(
                        text = "GLOBAL 3D GAME LEADERBOARD",
                        color = NoiseGold,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
            items(leaderboardUsers.take(25), key = { it.userId.ifBlank { it.id } }) { player ->
                val rankIndex = leaderboardUsers.indexOf(player) + 1
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (ThemeManager.isLightMode) NoiseCard else Color(0xFF151126),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .border(1.dp, NoiseBorder, RoundedCornerShape(16.dp))
                        .clickable { onInspectUserProfile(player.userId.ifBlank { player.id }) }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "#$rankIndex",
                                color = if (rankIndex <= 3) NoiseGold else NoiseTextSecondary,
                                fontWeight = FontWeight.Black,
                                fontSize = 15.sp,
                                modifier = Modifier.width(34.dp)
                            )
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(Brush.linearGradient(listOf(NoisePink, NoisePurple))),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = player.avatarKey.ifBlank { player.username.take(1) }.take(1).uppercase(),
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "@${player.username}",
                                    color = NoiseWhite,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "${player.gameRankTitle.ifBlank { "Rookie Pro" }} • ${player.gameMatchesPlayed} matches",
                                    color = NoiseTextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "${player.gameWins} Wins",
                                color = Color(0xFF00C985),
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "${player.gameRating} MMR",
                                color = NoiseGold,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        } else if (selectedSectionTab == "History") {
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Text(
                        text = "MY VERIFIED GAME HISTORY",
                        color = NoiseGold,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp
                    )
                    if (gameHistory.isEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No completed matches yet. Start any 3D game to record verified match results!",
                            color = NoiseTextSecondary,
                            fontSize = 13.sp
                        )
                    }
                }
            }
            items(gameHistory, key = { it.id }) { match ->
                val won = match.winnerId == currentUid
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (ThemeManager.isLightMode) NoiseCard else Color(0xFF151126),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .border(1.dp, NoiseBorder, RoundedCornerShape(16.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = match.gameTitle,
                                color = NoiseWhite,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Winner: @${match.winnerUsername} vs @${match.runnerUpUsername} • ${match.durationSec}s",
                                color = NoiseTextSecondary,
                                fontSize = 11.sp
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = if (won) "VICTORY (+${match.coinsAwarded})" else "COMPLETED",
                                color = if (won) Color(0xFF00C985) else NoiseGold,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 12.sp
                            )
                            Text(
                                text = "${match.winnerScore} - ${match.runnerUpScore}",
                                color = NoiseWhite,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        } else {
            // LOBBY VIEW: All 5 Required Game Cards + Featured Carousel + Multiplayer Rooms + Coming Soon

            // 4. ALL 5 DEDICATED MULTIPLAYER GAME CARDS (3D Carrom, 3D Ludo, 8 Ball Pool, Multiplayer Racing, Multiplayer Quiz)
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                    SectionHeaderRow(
                        title = "All Multiplayer Games (${gameCatalog.size})",
                        subtitle = "Select any game below and tap Play Now to launch",
                        noHorizontalPadding = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    gameCatalog.forEach { game ->
                        DedicatedGameCatalogCard(
                            game = game,
                            onPlayNow = { onCreateAndOpenGameRoom(game, "1v1", game.maxPlayers, false) },
                            onCustomRoom = { showCreateRoomDialogFor = game }
                        )
                    }
                }
            }

            // 5. FEATURED 3D GAMES CAROUSEL
            item {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    SectionHeaderRow(title = "Featured 3D Games", subtitle = "Unity 6 URP • Instant Play")
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(featuredGames, key = { it.gameType }) { game ->
                            Featured3DGameCard(
                                game = game,
                                onPlayNow = { onCreateAndOpenGameRoom(game, "1v1", game.maxPlayers, false) },
                                onCustomRoom = { showCreateRoomDialogFor = game }
                            )
                        }
                    }
                }
            }

            // 6. ACTIVE MULTIPLAYER ROOMS
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                    SectionHeaderRow(
                        title = "Multiplayer Rooms (${activeGameRooms.size})",
                        subtitle = "Join live public lobbies or enter a room code",
                        noHorizontalPadding = true
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    if (activeGameRooms.isEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (ThemeManager.isLightMode) NoiseCard else Color(0xFF151126),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, NoiseBorder, RoundedCornerShape(16.dp))
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "No open public lobbies right now",
                                        color = NoiseWhite,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = "Tap 'Play Now' on any game card above to start a match!",
                                        color = NoiseTextSecondary,
                                        fontSize = 11.sp
                                    )
                                }
                                Button(
                                    onClick = { showCreateRoomDialogFor = gameCatalog.firstOrNull() },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = NoisePink,
                                        contentColor = Color.White
                                    )
                                ) {
                                    Text("Host Now", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }
                    } else {
                        activeGameRooms.take(8).forEach { room ->
                            val slots = room.parsePlayers()
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = if (ThemeManager.isLightMode) NoiseCard else Color(0xFF161128),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .border(1.dp, NoiseBorder, RoundedCornerShape(16.dp))
                                    .testTag("active_game_room_${room.id}")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = if (room.privateRoom) Icons.Default.Lock else Icons.Default.Public,
                                                contentDescription = null,
                                                tint = NoiseGold,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(5.dp))
                                            Text(
                                                text = "${room.gameTitle} • ${room.mode}",
                                                color = NoiseWhite,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp
                                            )
                                        }
                                        Text(
                                            text = "Host: @${room.hostUsername} • Players: ${slots.size}/${room.maxPlayers} • Code: ${room.roomCode}",
                                            color = NoiseTextSecondary,
                                            fontSize = 11.sp
                                        )
                                    }
                                    Button(
                                        onClick = { onJoinGameRoomByCodeOrId(room.id) },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = NoisePink,
                                            contentColor = Color.White
                                        ),
                                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                    ) {
                                        Text("Join", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 7. GAME DEVELOPMENT STATUS TRACKER (ALL 11 MODULES COMPLETED)
            item {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .testTag("game_development_status_tracker")
                ) {
                    SectionHeaderRow(
                        title = "Game Development Status (${gameCatalog.size}/11 Completed)",
                        subtitle = "Graphics • Gameplay • Controls • Rules • Sound • Multiplayer Sync",
                        noHorizontalPadding = true
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    gameCatalog.forEach { game ->
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = if (ThemeManager.isLightMode) NoiseCard else Color(0xFF130F22),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .border(1.dp, Color(0xFF00C985).copy(alpha = 0.45f), RoundedCornerShape(18.dp))
                                .clickable { onCreateAndOpenGameRoom(game, "1v1", game.maxPlayers, false) }
                                .testTag("game_status_tracker_${game.gameType}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    GameCardThumbnail(
                                        gameType = game.gameType,
                                        accentColor = Color(game.accentColorHex),
                                        secondaryColor = Color(game.secondaryColorHex),
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = game.title,
                                            color = NoiseWhite,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 14.sp
                                        )
                                        Text(
                                            text = "Graphics ✓ • Gameplay ✓ • Controls ✓ • Multiplayer ✓",
                                            color = Color(0xFF00C985),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFF00C985).copy(alpha = 0.16f),
                                    modifier = Modifier.border(1.dp, Color(0xFF00C985), RoundedCornerShape(10.dp))
                                ) {
                                    Text(
                                        text = "Completed",
                                        color = Color(0xFF00C985),
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 10.sp,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 8. RECENTLY PLAYED & FRIENDS PLAYING
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    SectionHeaderRow(
                        title = "Recently Played",
                        subtitle = "Jump right back into your favorite 3D arenas",
                        noHorizontalPadding = true
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        recentlyPlayedItems.take(3).forEach { game ->
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = if (ThemeManager.isLightMode) NoiseCard else Color(0xFF18122B),
                                modifier = Modifier
                                    .weight(1f)
                                    .border(1.dp, NoiseBorder, RoundedCornerShape(16.dp))
                                    .clickable { onCreateAndOpenGameRoom(game, "1v1", game.maxPlayers, false) }
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = game.title,
                                        color = NoiseWhite,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${game.minPlayers}-${game.maxPlayers} Players",
                                        color = NoiseGold,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    SectionHeaderRow(
                        title = "Friends Playing (${friendsPlaying.size})",
                        subtitle = "Invite online friends into your 3D room",
                        noHorizontalPadding = true
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    if (friendsPlaying.isEmpty()) {
                        Text(
                            text = "Follow users on NOISE LIVE to see when they are online and playing!",
                            color = NoiseTextSecondary,
                            fontSize = 12.sp
                        )
                    } else {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(friendsPlaying, key = { it.userId.ifBlank { it.id } }) { friend ->
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = if (ThemeManager.isLightMode) NoiseCard else Color(0xFF151126),
                                    modifier = Modifier
                                        .width(155.dp)
                                        .border(1.dp, NoiseBorder, RoundedCornerShape(16.dp))
                                        .clickable { showInviteFriendsDialog = true }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(34.dp)
                                                .clip(CircleShape)
                                                .background(Brush.linearGradient(listOf(NoisePink, NoisePurple))),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = friend.avatarKey.ifBlank { friend.username.take(1) }.take(1).uppercase(),
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = "@${friend.username}",
                                                color = NoiseWhite,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = friend.currentGameName.ifBlank { "Online • Invite" },
                                                color = Color(0xFF00C985),
                                                fontSize = 10.sp,
                                                maxLines = 1
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

    // CREATE GAME ROOM MODAL
    showCreateRoomDialogFor?.let { selectedGame ->
        var selectedMode by remember { mutableStateOf("1v1") }
        var maxPlayers by remember { mutableIntStateOf(selectedGame.maxPlayers) }
        var isPrivate by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showCreateRoomDialogFor = null },
            containerColor = NoiseCard,
            title = {
                Text("Create ${selectedGame.title} Room", color = NoiseWhite, fontWeight = FontWeight.ExtraBold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Configure your Unity 6 Multiplayer Lobby & Relay room:",
                        color = NoiseTextSecondary,
                        fontSize = 12.sp
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("1v1" to 2, "2v2" to 4, "4-Player" to 4).forEach { (modeLabel, playersCnt) ->
                            val isSelected = selectedMode == modeLabel
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) NoisePink else NoiseSurface,
                                modifier = Modifier
                                    .border(1.dp, if (isSelected) NoisePink else NoiseBorder, RoundedCornerShape(10.dp))
                                    .clickable {
                                        selectedMode = modeLabel
                                        maxPlayers = playersCnt
                                    }
                            ) {
                                Text(
                                    text = modeLabel,
                                    color = if (isSelected) Color.White else NoiseWhite,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Private Room (Join by Code/Invite)", color = NoiseWhite, fontSize = 13.sp)
                        Switch(checked = isPrivate, onCheckedChange = { isPrivate = it })
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val game = selectedGame
                        showCreateRoomDialogFor = null
                        onCreateAndOpenGameRoom(game, selectedMode, maxPlayers, isPrivate)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NoisePink),
                    modifier = Modifier.testTag("confirm_create_game_room_button")
                ) {
                    Text("Launch 3D Room", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateRoomDialogFor = null }) {
                    Text("Cancel", color = NoiseTextSecondary)
                }
            }
        )
    }

    // JOIN GAME ROOM BY CODE DIALOG
    if (showJoinByCodeDialog) {
        var codeInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showJoinByCodeDialog = false },
            containerColor = NoiseCard,
            title = { Text("Join Game Room", color = NoiseWhite, fontWeight = FontWeight.ExtraBold) },
            text = {
                OutlinedTextField(
                    value = codeInput,
                    onValueChange = { codeInput = it.uppercase() },
                    label = { Text("Enter 6-Digit Room Code or ID") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = NoiseWhite,
                        unfocusedTextColor = NoiseWhite,
                        focusedBorderColor = NoisePink,
                        unfocusedBorderColor = NoisePurple
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("join_game_code_input")
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (codeInput.isNotBlank()) {
                            onJoinGameRoomByCodeOrId(codeInput.trim())
                            showJoinByCodeDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NoisePink)
                ) {
                    Text("Join Match", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showJoinByCodeDialog = false }) {
                    Text("Cancel", color = NoiseTextSecondary)
                }
            }
        )
    }

    // GRAPHICS QUALITY SETTINGS MODAL (Low / Medium / High)
    if (showGraphicsModal) {
        AlertDialog(
            onDismissRequest = { showGraphicsModal = false },
            containerColor = NoiseCard,
            title = { Text("3D Graphics Quality & FPS", color = NoiseWhite, fontWeight = FontWeight.ExtraBold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Adjust Universal Render Pipeline (URP) quality, shadows, and target frame rate:",
                        color = NoiseTextSecondary,
                        fontSize = 12.sp
                    )
                    GraphicsQualityLevel.entries.forEach { level ->
                        val selected = level == graphicsQuality
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (selected) NoisePink.copy(alpha = 0.16f) else NoiseSurface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    1.dp,
                                    if (selected) NoisePink else NoiseBorder,
                                    RoundedCornerShape(14.dp)
                                )
                                .clickable {
                                    onSelectGraphicsQuality(level)
                                    showGraphicsModal = false
                                }
                                .testTag("graphics_quality_${level.name.lowercase()}")
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(level.label, color = NoiseWhite, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text(
                                        text = "Target: ${level.targetFps} FPS • Render Scale: ${(level.renderScale * 100).toInt()}%",
                                        color = NoiseTextSecondary,
                                        fontSize = 11.sp
                                    )
                                }
                                if (selected) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = NoiseGold)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showGraphicsModal = false }) {
                    Text("Done", color = NoiseGold, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // INVITE FRIENDS MODAL
    if (showInviteFriendsDialog) {
        val targetRoom = activeGameRooms.firstOrNull { it.hostId == currentUid } ?: activeGameRooms.firstOrNull()
        AlertDialog(
            onDismissRequest = { showInviteFriendsDialog = false },
            containerColor = NoiseCard,
            title = { Text("Invite Friends to Play", color = NoiseWhite, fontWeight = FontWeight.ExtraBold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (registeredAccounts.none { it.userId.ifBlank { it.id } != currentUid }) {
                        Text("No other registered accounts found yet.", color = NoiseTextSecondary, fontSize = 12.sp)
                    } else {
                        registeredAccounts
                            .filter { it.userId.ifBlank { it.id } != currentUid }
                            .take(6)
                            .forEach { user ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("@${user.username}", color = NoiseWhite, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Button(
                                        onClick = {
                                            if (targetRoom != null) {
                                                onInviteFriendToGame(targetRoom, user)
                                            } else {
                                                gameCatalog.firstOrNull()?.let { firstGame ->
                                                    onCreateAndOpenGameRoom(firstGame, "1v1", 2, false)
                                                }
                                            }
                                            showInviteFriendsDialog = false
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = NoisePink),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                    ) {
                                        Text("Invite", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showInviteFriendsDialog = false }) {
                    Text("Close", color = NoiseTextSecondary)
                }
            }
        )
    }
}

/**
 * Dedicated Game Card with:
 * - Game image or thumbnail (`GameCardThumbnail`)
 * - Game name (`3D Carrom`, `3D Ludo`, `8 Ball Pool`, `Multiplayer Racing`, `Multiplayer Quiz`)
 * - Number of supported players (`2 - 4 Players`, `2 Players (1v1)`, `2 - 8 Players`)
 * - Game availability status (`🟢 Available • Playable Now`)
 * - Clickable `PLAY NOW` button
 */
@Composable
private fun DedicatedGameCatalogCard(
    game: NoiseGameCatalogItem,
    onPlayNow: () -> Unit,
    onCustomRoom: () -> Unit
) {
    val supportedPlayersLabel = if (game.minPlayers == game.maxPlayers) {
        "${game.maxPlayers} Players (1v1)"
    } else {
        "${game.minPlayers} - ${game.maxPlayers} Players"
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (ThemeManager.isLightMode) NoiseCard else Color(0xFF161129),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .border(
                1.2.dp,
                Brush.horizontalGradient(
                    listOf(
                        Color(game.accentColorHex).copy(alpha = 0.85f),
                        Color(game.secondaryColorHex).copy(alpha = 0.7f)
                    )
                ),
                RoundedCornerShape(20.dp)
            )
            .clickable { onPlayNow() }
            .testTag("popular_game_row_${game.gameType}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Game Image / 3D Thumbnail
            GameCardThumbnail(
                gameType = game.gameType,
                accentColor = Color(game.accentColorHex),
                secondaryColor = Color(game.secondaryColorHex),
                modifier = Modifier
                    .size(74.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, NoiseBorder, RoundedCornerShape(16.dp))
            )

            Spacer(modifier = Modifier.width(14.dp))

            // Game Name, Supported Players, Availability Status
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = game.title,
                    color = NoiseWhite,
                    fontWeight = FontWeight.Black,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(3.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Group,
                        contentDescription = null,
                        tint = NoiseGold,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = supportedPlayersLabel,
                        color = NoiseGold,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "🟢 Available • ${game.activePlayersCount} Online",
                    color = Color(0xFF00C985),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Text(
                    text = game.subtitle,
                    color = NoiseTextSecondary,
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Play Now Button + Room Options
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Button(
                    onClick = onPlayNow,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NoisePink,
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    modifier = Modifier.testTag("play_button_${game.gameType}")
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Play Now", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (ThemeManager.isLightMode) NoiseCardElevated else Color.White.copy(alpha = 0.08f),
                    modifier = Modifier
                        .border(1.dp, NoiseBorder, RoundedCornerShape(10.dp))
                        .clickable { onCustomRoom() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Lobby Settings",
                            tint = NoiseTextSecondary,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Lobby",
                            color = NoiseTextSecondary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

/**
 * Custom 3D-styled Canvas Game Thumbnail for:
 * - `carrom_3d` (3D Carrom board, pockets, red queen, striker)
 * - `ludo_3d` (3D Ludo 4-color board, star safe zones, dice)
 * - `pool_8ball` (8 Ball Pool table, #8 black ball, cue stick)
 * - `mini_racing` (Multiplayer Racing neon track, curbs, nitro cars)
 * - `multiplayer_quiz` (Multiplayer Quiz neon stage, trophy, buzzer)
 */
@Composable
fun GameCardThumbnail(
    gameType: String,
    accentColor: Color,
    secondaryColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.background(
            Brush.linearGradient(listOf(accentColor, secondaryColor))
        ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val cx = w / 2f
            val cy = h / 2f

            when (gameType) {
                "carrom_3d" -> {
                    // Wooden 3D Carrom board with corner pockets & red queen
                    drawRoundRect(
                        color = Color(0xFF4E2A14),
                        topLeft = Offset(w * 0.1f, h * 0.1f),
                        size = Size(w * 0.8f, h * 0.8f),
                        cornerRadius = CornerRadius(12f, 12f)
                    )
                    drawRoundRect(
                        color = Color(0xFFF3D5A3),
                        topLeft = Offset(w * 0.17f, h * 0.17f),
                        size = Size(w * 0.66f, h * 0.66f),
                        cornerRadius = CornerRadius(8f, 8f)
                    )
                    // 4 pockets
                    val pRadius = w * 0.06f
                    drawCircle(Color(0xFF1A0F08), pRadius, Offset(w * 0.22f, h * 0.22f))
                    drawCircle(Color(0xFF1A0F08), pRadius, Offset(w * 0.78f, h * 0.22f))
                    drawCircle(Color(0xFF1A0F08), pRadius, Offset(w * 0.22f, h * 0.78f))
                    drawCircle(Color(0xFF1A0F08), pRadius, Offset(w * 0.78f, h * 0.78f))
                    // Center circle & Red Queen
                    drawCircle(Color(0xFFD97706), w * 0.13f, Offset(cx, cy), style = Stroke(width = 2.5f))
                    drawCircle(Color(0xFFFF1744), w * 0.07f, Offset(cx, cy))
                    // Striker
                    drawCircle(Color(0xFF00E5FF), w * 0.085f, Offset(cx, h * 0.7f))
                }
                "ludo_3d" -> {
                    // Gold-framed 3D Isometric Ludo Board (Red, Green, Blue, Yellow) + Floating 3D White Dice
                    val boardTop = h * 0.34f
                    val boardBot = h * 0.88f
                    val framePath = Path().apply {
                        moveTo(w * 0.16f, boardTop)
                        lineTo(w * 0.84f, boardTop)
                        lineTo(w * 0.94f, boardBot)
                        lineTo(w * 0.06f, boardBot)
                        close()
                    }
                    drawPath(framePath, Brush.linearGradient(listOf(Color(0xFFFFF59D), Color(0xFFFFB300), Color(0xFF8D5000))))
                    // 2x2 Home Zones: Red (TL), Green (TR), Blue (BL), Yellow (BR)
                    drawRoundRect(Color(0xFFFF1744), Offset(w * 0.16f, h * 0.38f), Size(w * 0.28f, h * 0.21f), CornerRadius(6f, 6f))
                    drawRoundRect(Color(0xFF00E676), Offset(w * 0.56f, h * 0.38f), Size(w * 0.28f, h * 0.21f), CornerRadius(6f, 6f))
                    drawRoundRect(Color(0xFF0088FF), Offset(w * 0.12f, h * 0.63f), Size(w * 0.32f, h * 0.22f), CornerRadius(6f, 6f))
                    drawRoundRect(Color(0xFFFFD000), Offset(w * 0.56f, h * 0.63f), Size(w * 0.32f, h * 0.22f), CornerRadius(6f, 6f))
                    // Golden orbital glow + Floating 3D White Dice above board
                    drawOval(
                        color = Color(0xFFFFD700),
                        topLeft = Offset(cx - w * 0.34f, h * 0.12f),
                        size = Size(w * 0.68f, h * 0.18f),
                        style = Stroke(width = 2.5f)
                    )
                    drawRoundRect(
                        color = Color.White,
                        topLeft = Offset(cx - w * 0.15f, h * 0.08f),
                        size = Size(w * 0.30f, h * 0.26f),
                        cornerRadius = CornerRadius(7f, 7f)
                    )
                    drawCircle(Color.Black, w * 0.03f, Offset(cx - w * 0.06f, h * 0.15f))
                    drawCircle(Color.Black, w * 0.03f, Offset(cx + w * 0.06f, h * 0.15f))
                    drawCircle(Color.Black, w * 0.03f, Offset(cx - w * 0.06f, h * 0.26f))
                    drawCircle(Color.Black, w * 0.03f, Offset(cx + w * 0.06f, h * 0.26f))
                }
                "pool_8ball" -> {
                    // Emerald Pool Felt + Cue Stick + 8 Ball
                    drawRoundRect(
                        color = Color(0xFF065F46),
                        topLeft = Offset(w * 0.08f, h * 0.14f),
                        size = Size(w * 0.84f, h * 0.72f),
                        cornerRadius = CornerRadius(12f, 12f)
                    )
                    // Cue line
                    drawLine(
                        color = Color(0xFFFDE68A),
                        start = Offset(w * 0.15f, h * 0.78f),
                        end = Offset(cx - w * 0.08f, cy + h * 0.06f),
                        strokeWidth = 5f
                    )
                    // 8 Ball
                    drawCircle(Color.Black, w * 0.22f, Offset(cx + w * 0.06f, cy - h * 0.03f))
                    drawCircle(Color.White, w * 0.095f, Offset(cx + w * 0.06f, cy - h * 0.03f))
                    drawCircle(Color.Black, w * 0.04f, Offset(cx + w * 0.06f, cy - h * 0.03f))
                }
                "mini_racing" -> {
                    // Asphalt Track + Neon Curbs + 2 Race Cars
                    val trackPath = Path().apply {
                        moveTo(w * 0.32f, h * 0.1f)
                        lineTo(w * 0.68f, h * 0.1f)
                        lineTo(w * 0.88f, h * 0.9f)
                        lineTo(w * 0.12f, h * 0.9f)
                        close()
                    }
                    drawPath(trackPath, Color(0xFF1E293B))
                    drawLine(
                        color = Color(0xFFFFEA00),
                        start = Offset(cx, h * 0.15f),
                        end = Offset(cx, h * 0.85f),
                        strokeWidth = 3f
                    )
                    // Red Car & Cyan Car
                    drawRoundRect(
                        color = Color(0xFFFF1744),
                        topLeft = Offset(cx - w * 0.2f, h * 0.45f),
                        size = Size(w * 0.15f, h * 0.26f),
                        cornerRadius = CornerRadius(6f, 6f)
                    )
                    drawRoundRect(
                        color = Color(0xFF00E5FF),
                        topLeft = Offset(cx + w * 0.05f, h * 0.32f),
                        size = Size(w * 0.15f, h * 0.26f),
                        cornerRadius = CornerRadius(6f, 6f)
                    )
                }
                "chess_3d" -> {
                    // 8x8 Chessboard pattern + Golden King Crown
                    val pad = w * 0.14f
                    val sq = (w - pad * 2f) / 4f
                    for (r in 0..3) {
                        for (c in 0..3) {
                            drawRect(
                                color = if ((r + c) % 2 == 0) Color(0xFFF3E5F5) else Color(0xFF4A148C),
                                topLeft = Offset(pad + c * sq, pad + r * sq),
                                size = Size(sq, sq)
                            )
                        }
                    }
                    drawCircle(Color(0xFFFFD700), w * 0.16f, Offset(cx, cy))
                }
                "checkers_3d" -> {
                    // Checkers board + Red & Gold King discs
                    drawRoundRect(
                        color = Color(0xFF281E46),
                        topLeft = Offset(w * 0.12f, h * 0.12f),
                        size = Size(w * 0.76f, h * 0.76f),
                        cornerRadius = CornerRadius(10f, 10f)
                    )
                    drawCircle(Color(0xFFFF1744), w * 0.18f, Offset(cx - w * 0.14f, cy - h * 0.10f))
                    drawCircle(Color(0xFFFFD700), w * 0.18f, Offset(cx + w * 0.14f, cy + h * 0.12f))
                }
                "tic_tac_toe" -> {
                    // Neon 3x3 grid + X and O
                    val g1 = w * 0.36f
                    val g2 = w * 0.64f
                    drawLine(Color.White.copy(alpha = 0.6f), Offset(g1, h * 0.16f), Offset(g1, h * 0.84f), strokeWidth = 3f)
                    drawLine(Color.White.copy(alpha = 0.6f), Offset(g2, h * 0.16f), Offset(g2, h * 0.84f), strokeWidth = 3f)
                    drawLine(Color.White.copy(alpha = 0.6f), Offset(w * 0.16f, g1), Offset(w * 0.84f, g1), strokeWidth = 3f)
                    drawLine(Color.White.copy(alpha = 0.6f), Offset(w * 0.16f, g2), Offset(w * 0.84f, g2), strokeWidth = 3f)
                    drawCircle(Color(0xFFFF2E93), w * 0.10f, Offset(cx, cy), style = Stroke(width = 4f))
                }
                "dominoes_pro" -> {
                    // Double-Six Ivory Domino Tile
                    drawRoundRect(
                        color = Color.White,
                        topLeft = Offset(w * 0.26f, h * 0.14f),
                        size = Size(w * 0.48f, h * 0.72f),
                        cornerRadius = CornerRadius(10f, 10f)
                    )
                    drawLine(Color(0xFF141026), Offset(w * 0.30f, cy), Offset(w * 0.70f, cy), strokeWidth = 3f)
                    drawCircle(Color(0xFF141026), w * 0.045f, Offset(cx - w * 0.09f, cy - h * 0.16f))
                    drawCircle(Color(0xFF141026), w * 0.045f, Offset(cx + w * 0.09f, cy - h * 0.16f))
                    drawCircle(Color(0xFFFF1744), w * 0.055f, Offset(cx, cy + h * 0.16f))
                }
                "snakes_ladders" -> {
                    // Golden Ladder + Neon Serpent
                    drawLine(Color(0xFFFFD700), Offset(w * 0.28f, h * 0.82f), Offset(w * 0.48f, h * 0.18f), strokeWidth = 4f)
                    drawLine(Color(0xFFFFD700), Offset(w * 0.44f, h * 0.82f), Offset(w * 0.64f, h * 0.18f), strokeWidth = 4f)
                    drawCircle(Color(0xFF00E676), w * 0.14f, Offset(w * 0.70f, h * 0.62f))
                }
                "mini_arcade" -> {
                    // 4 Colorful Arcade Bubbles
                    drawCircle(Color(0xFFFF2E93), w * 0.15f, Offset(cx - w * 0.16f, cy - h * 0.14f))
                    drawCircle(Color(0xFF00E5FF), w * 0.15f, Offset(cx + w * 0.16f, cy - h * 0.14f))
                    drawCircle(Color(0xFFFFEA00), w * 0.15f, Offset(cx - w * 0.16f, cy + h * 0.16f))
                    drawCircle(Color(0xFF00E676), w * 0.15f, Offset(cx + w * 0.16f, cy + h * 0.16f))
                }
                else -> {
                    // Multiplayer Quiz Stage Rings & Golden Buzzers
                    drawCircle(
                        color = Color(0xFFFFD54F).copy(alpha = 0.3f),
                        radius = w * 0.36f,
                        center = Offset(cx, cy),
                        style = Stroke(width = 4f)
                    )
                    drawCircle(
                        color = Color(0xFFFFD54F),
                        radius = w * 0.2f,
                        center = Offset(cx, cy)
                    )
                    drawCircle(
                        color = Color(0xFF31105A),
                        radius = w * 0.08f,
                        center = Offset(cx, cy)
                    )
                }
            }
        }
    }
}

@Composable
private fun Featured3DGameCard(
    game: NoiseGameCatalogItem,
    onPlayNow: () -> Unit,
    onCustomRoom: () -> Unit
) {
    val supportedPlayersLabel = if (game.minPlayers == game.maxPlayers) {
        "${game.maxPlayers} Players"
    } else {
        "${game.minPlayers}-${game.maxPlayers} Players"
    }
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = if (ThemeManager.isLightMode) NoiseCard else Color(0xFF18122C),
        modifier = Modifier
            .width(248.dp)
            .border(
                1.5.dp,
                Brush.linearGradient(listOf(Color(game.accentColorHex), Color(game.secondaryColorHex))),
                RoundedCornerShape(22.dp)
            )
            .clickable { onPlayNow() }
            .testTag("featured_game_card_${game.gameType}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                GameCardThumbnail(
                    gameType = game.gameType,
                    accentColor = Color(game.accentColorHex),
                    secondaryColor = Color(game.secondaryColorHex),
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "🟢 Available",
                        color = Color(0xFF00C985),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = supportedPlayersLabel,
                        color = NoiseGold,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = game.title,
                color = NoiseWhite,
                fontWeight = FontWeight.Black,
                fontSize = 17.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = game.subtitle,
                color = NoiseTextSecondary,
                fontSize = 11.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.height(32.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onPlayNow,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NoisePink,
                        contentColor = Color.White
                    ),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    Text("Play Now", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                }
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (ThemeManager.isLightMode) NoiseCardElevated else Color.White.copy(alpha = 0.08f),
                    modifier = Modifier
                        .border(1.dp, NoiseBorder, RoundedCornerShape(12.dp))
                        .clickable { onCustomRoom() }
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Default.Settings, contentDescription = "Room Options", tint = NoiseWhite, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
private fun SectionHeaderRow(
    title: String,
    subtitle: String,
    noHorizontalPadding: Boolean = false
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = if (noHorizontalPadding) 0.dp else 16.dp, vertical = 4.dp)
    ) {
        Text(text = title, color = NoiseWhite, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
        Text(text = subtitle, color = NoiseTextSecondary, fontSize = 11.sp)
    }
}

/**
 * Chatroom Games Selection Panel for `PartyChatRoomScreen`.
 * Shows:
 * - Carrom (`3D Carrom`)
 * - Ludo (`3D Ludo`)
 * - Pool (`8 Ball Pool`)
 * - Racing (`Multiplayer Racing`)
 * - Quiz (`Multiplayer Quiz`)
 * Allows the room owner to select a game, start it inside the chatroom, and invite room members.
 */
@Composable
fun ChatroomGamesSelectorDialog(
    chatRoomId: String,
    chatRoomTitle: String,
    isRoomOwner: Boolean,
    currentUserId: String = "",
    gameCatalog: List<NoiseGameCatalogItem>,
    activeChatroomGames: List<GameRoomDoc>,
    roomMembers: List<UserProfile> = emptyList(),
    onSelectAndStartChatroomGame: (NoiseGameCatalogItem, String) -> Unit,
    onJoinExistingChatroomGame: (GameRoomDoc) -> Unit,
    onEndChatroomGame: (GameRoomDoc) -> Unit,
    onInviteMemberToGame: (GameRoomDoc, UserProfile) -> Unit = { _, _ -> },
    onDismiss: () -> Unit
) {
    var selectedMode by remember { mutableStateOf("1v1") }
    var invitedUserIds by remember { mutableStateOf(setOf<String>()) }

    // Map short Chatroom game labels alongside full titles so both "Carrom" and "3D Carrom" are crystal clear
    fun shortLabelFor(gameType: String, defaultTitle: String): String = when (gameType) {
        "carrom_3d" -> "Carrom • 3D Carrom"
        "ludo_3d" -> "Ludo • 3D Ludo"
        "pool_8ball" -> "Pool • 8 Ball Pool"
        "mini_racing" -> "Racing • Multiplayer Racing"
        "multiplayer_quiz" -> "Quiz • Multiplayer Quiz"
        "chess_3d" -> "Chess • 3D Royal Chess"
        "checkers_3d" -> "Checkers • Multi-Jump Checkers"
        "tic_tac_toe" -> "Tic Tac Toe • Neon 3x3 Duel"
        "dominoes_pro" -> "Dominoes • Double-Six 28 Tiles"
        "snakes_ladders" -> "Snakes & Ladders • 100 Squares"
        "mini_arcade" -> "Mini Arcade • 4-in-1 Hub"
        else -> defaultTitle
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NoiseCard,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.SportsEsports, contentDescription = null, tint = NoiseGold)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Chatroom Games Selection",
                            color = NoiseWhite,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 17.sp
                        )
                        Text(
                            text = if (isRoomOwner) "Room Owner: Select a game & invite members" else "Select a game to play with voice chat",
                            color = NoiseGold,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Mode selector (1v1 / 2v2 / 4-Player)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Mode:", color = NoiseTextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    listOf("1v1", "2v2", "4-Player").forEach { mode ->
                        val isSelected = selectedMode == mode
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) NoisePink else NoiseSurface,
                            modifier = Modifier
                                .border(1.dp, if (isSelected) NoisePink else NoiseBorder, RoundedCornerShape(10.dp))
                                .clickable { selectedMode = mode }
                        ) {
                            Text(
                                text = mode,
                                color = if (isSelected) Color.White else NoiseWhite,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }

                // Active game in this chatroom if any
                activeChatroomGames.firstOrNull()?.let { liveGame ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = NoiseSurface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, NoiseGold, RoundedCornerShape(14.dp))
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "LIVE NOW: ${liveGame.gameTitle}",
                                        color = NoiseGold,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 12.sp
                                    )
                                    Text(
                                        text = "Host: @${liveGame.hostUsername} • ${liveGame.parsePlayers().size}/${liveGame.maxPlayers} players",
                                        color = NoiseWhite,
                                        fontSize = 11.sp
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Button(
                                        onClick = {
                                            onDismiss()
                                            onJoinExistingChatroomGame(liveGame)
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676)),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text("Join", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    }
                                    if (isRoomOwner) {
                                        Button(
                                            onClick = { onEndChatroomGame(liveGame) },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF1744)),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                        ) {
                                            Text("End", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 5 Chatroom Games: Carrom, Ludo, Pool, Racing, Quiz
                gameCatalog.forEach { game ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = NoiseSurface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                1.dp,
                                Color(game.accentColorHex).copy(alpha = 0.55f),
                                RoundedCornerShape(14.dp)
                            )
                            .clickable {
                                onDismiss()
                                onSelectAndStartChatroomGame(game, selectedMode)
                            }
                            .testTag("chatroom_game_option_${game.gameType}")
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            GameCardThumbnail(
                                gameType = game.gameType,
                                accentColor = Color(game.accentColorHex),
                                secondaryColor = Color(game.secondaryColorHex),
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(10.dp))
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = shortLabelFor(game.gameType, game.title),
                                    color = NoiseWhite,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = "👥 ${game.minPlayers}-${game.maxPlayers} Players • 🟢 Available",
                                    color = NoiseGold,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Button(
                                onClick = {
                                    onDismiss()
                                    onSelectAndStartChatroomGame(game, selectedMode)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = NoisePink),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text("Play Now", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 11.sp)
                            }
                        }
                    }
                }

                // Room Owner Member Invitation Section
                val invitableMembers = roomMembers.filter {
                    val uid = it.userId.ifBlank { it.id }
                    uid.isNotBlank() && uid != currentUserId
                }
                if (invitableMembers.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "INVITE ROOM MEMBERS",
                        color = NoiseGold,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 11.sp
                    )
                    invitableMembers.take(5).forEach { member ->
                        val memberUid = member.userId.ifBlank { member.id }
                        val alreadyInvited = memberUid in invitedUserIds
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.White.copy(alpha = 0.05f))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "@${member.username}",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                            Button(
                                onClick = {
                                    invitedUserIds = invitedUserIds + memberUid
                                    val liveGame = activeChatroomGames.firstOrNull()
                                    if (liveGame != null) {
                                        onInviteMemberToGame(liveGame, member)
                                    } else {
                                        val firstGame = gameCatalog.firstOrNull()
                                        if (firstGame != null) {
                                            onSelectAndStartChatroomGame(firstGame, selectedMode)
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (alreadyInvited) Color(0xFF00E676) else NoisePurple
                                ),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = if (alreadyInvited) "Invited ✓" else "Invite",
                                    color = if (alreadyInvited) Color.Black else Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
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
}
