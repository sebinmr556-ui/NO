package com.example.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val appScope: String = "noise_live_public",
    val username: String = "sebin_mr",
    val usernameLower: String = "sebin_mr",
    val displayName: String = "Sebin Mr",
    val displayNameLower: String = "sebin mr",
    val bio: String = "Digital Creator ✨\nNoise App Official 🚀",
    val location: String = "",
    val website: String = "noise.app",
    val avatarKey: String = "S",
    val profilePictureUrl: String = "",
    val accountPrivacy: String = "public",
    val coins: Long = 40150L,
    val vip: Boolean = true,
    val verified: Boolean = true,
    val online: Boolean = true,
    val lastActiveLabel: String = "Active now",
    val lastSeenMillis: Long = 0L,
    val showOnlineStatus: Boolean = true,
    val showLastSeen: Boolean = true,
    val showTypingIndicator: Boolean = true,
    val postsCount: Long = 0L,
    val followersCount: Long = 0L,
    val followingCount: Long = 0L,
    val followedUserIds: List<String> = emptyList(),
    // Multiplayer Gaming Stats linked to Firebase UID
    val gameWins: Long = 0L,
    val gameLosses: Long = 0L,
    val gameMatchesPlayed: Long = 0L,
    val gameRating: Long = 1000L,
    val gameRankTitle: String = "Bronze Contender",
    val recentlyPlayedGames: List<String> = emptyList(),
    val currentGameName: String = "",
    // VIP 1 to VIP 10 Luxury Gemstone Badge System
    val activeVipBadgeLevel: Long = 1L,
    val activeVipBadgeName: String = "VIP 1 – Blue Sapphire",
    val ownedVipBadges: List<Long> = listOf(1L),
    val themeMode: String = "dark", // "dark" or "light"
    val fcmToken: String = "",
    val notificationsEnabled: Boolean = true,
    val accountStatus: String = "active", // "active", "suspended", "banned"
    val suspendedReason: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class FollowRelation(
    @DocumentId val id: String = "",
    val followerUid: String = "",
    val followingUid: String = "",
    val followerUsername: String = "",
    val followerDisplayName: String = "",
    val followerAvatarKey: String = "S",
    val followingUsername: String = "",
    val followingDisplayName: String = "",
    val followingAvatarKey: String = "S",
    val username: String = "",
    val displayName: String = "",
    val avatarKey: String = "S",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class UserPresence(
    @DocumentId val id: String = "",
    val userId: String = "",
    val state: String = "online", // "online" or "offline"
    val lastActiveLabel: String = "Active now",
    val lastSeenLabel: String = "Active now",
    val lastSeenMillis: Long = 0L,
    val showOnlineStatus: Boolean = true,
    val showLastSeen: Boolean = true,
    @ServerTimestamp val lastChanged: Timestamp? = null
)

@IgnoreExtraProperties
data class TypingStatusDoc(
    @DocumentId val id: String = "",
    val conversationId: String = "",
    val userId: String = "",
    val username: String = "",
    val isTyping: Boolean = false,
    val typing: Boolean = false,
    val updatedAtMillis: Long = 0L,
    @ServerTimestamp val lastUpdated: Timestamp? = null
)

@IgnoreExtraProperties
data class Post(
    @DocumentId val id: String = "",
    val postId: String = "",
    val userId: String = "",
    val ownerUid: String = "",
    val appScope: String = "noise_live_public",
    val username: String = "sebin_mr",
    val displayName: String = "Sebin Mr",
    val authorAvatarKey: String = "S",
    val profilePhoto: String = "",
    val mediaKey: String = "roses",
    val mediaUrl: String = "",
    val storagePath: String = "",
    val thumbnailUrl: String = "",
    val cloudMediaId: String = "",
    val mediaType: String = "photo",
    val caption: String = "Red roses in full bloom 🌹✨ #NoiseVibes #Photography",
    val location: String = "Shibuya, Tokyo",
    val tag: String = "#Photography",
    val vibeEmoji: String = "✨",
    val likesCount: Long = 0L,
    val commentsCount: Long = 0L,
    val viewsCount: Long = 0L,
    val sharesCount: Long = 0L,
    val savesCount: Long = 0L,
    val likedByUids: List<String> = emptyList(),
    val viewedByUids: List<String> = emptyList(),
    val savedByUids: List<String> = emptyList(),
    val hashtags: List<String> = emptyList(),
    val reelCategory: String = "Trending", // "Trending", "Kerala Trending", "Malayalam Music", "Regional"
    val musicTrackTitle: String = "",
    val language: String = "ml", // "ml" or "en"
    val regionTag: String = "Kerala",
    val trendingScore: Double = 0.0,
    val uploadStatus: String = "Published",
    val visibility: String = "public",
    val authorVipBadgeLevel: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    @Exclude
    fun effectiveMediaSource(): String = mediaUrl.ifBlank { mediaKey }

    @Exclude
    fun resolvedHashtags(): List<String> {
        if (hashtags.isNotEmpty()) return hashtags.distinct()
        val fromCaption = Regex("#[A-Za-z0-9_\\u0D00-\\u0D7F]+")
            .findAll("$caption $tag")
            .map { it.value }
            .distinct()
            .toList()
        return if (fromCaption.isNotEmpty()) {
            fromCaption
        } else {
            listOf(tag.ifBlank { "#NoiseLive" }, "#KeralaTrending", "#MalayalamReels").distinct()
        }
    }

    @Exclude
    fun isKeralaOrMalayalamReel(): Boolean {
        val text = "$caption $location $tag $reelCategory $musicTrackTitle $regionTag ${hashtags.joinToString(" ")}".lowercase()
        val hasMalayalamScript = caption.any { it in '\u0D00'..'\u0D7F' } || musicTrackTitle.any { it in '\u0D00'..'\u0D7F' }
        return hasMalayalamScript ||
            language.equals("ml", ignoreCase = true) ||
            regionTag.equals("Kerala", ignoreCase = true) ||
            reelCategory.contains("kerala", ignoreCase = true) ||
            reelCategory.contains("malayalam", ignoreCase = true) ||
            text.contains("kerala") ||
            text.contains("malayalam") ||
            text.contains("kochi") ||
            text.contains("trivandrum") ||
            text.contains("kozhikode") ||
            text.contains("thrissur") ||
            text.contains("munnar") ||
            text.contains("alleppey") ||
            text.contains("wayanad") ||
            text.contains("kannur")
    }

    @Exclude
    fun computeReelTrendingScore(
        nowMillis: Long = System.currentTimeMillis(),
        algorithmMode: String = "engagement_velocity"
    ): Double {
        val createdMs = createdAt?.toDate()?.time ?: nowMillis
        val ageHours = ((nowMillis - createdMs).coerceAtLeast(0L).toDouble() / 3_600_000.0)
        val effectiveViews = maxOf(viewsCount, viewedByUids.size.toLong(), 1L)
        val effectiveSaves = maxOf(savesCount, savedByUids.size.toLong())
        val effectiveLikes = maxOf(likesCount, likedByUids.size.toLong())
        return when (algorithmMode.lowercase()) {
            "views_first" -> {
                (effectiveViews * 10.0) + (effectiveLikes * 4.0) + (sharesCount * 6.0) + (commentsCount * 5.0)
            }
            "recent_first" -> {
                val recency = 1000.0 / (1.0 + (ageHours / 2.0))
                recency + (effectiveLikes * 2.0) + (effectiveViews * 0.5)
            }
            else -> {
                // Default "engagement_velocity": balances views, likes, shares, comments, saves, and recency
                val recencyBoost = 140.0 / (1.0 + (ageHours / 8.0))
                val engagementScore = (effectiveViews * 1.8) +
                    (effectiveLikes * 6.5) +
                    (commentsCount * 8.0) +
                    (sharesCount * 11.0) +
                    (effectiveSaves * 7.5)
                val regionalBoost = if (isKeralaOrMalayalamReel()) 15.0 else 0.0
                recencyBoost + engagementScore + regionalBoost
            }
        }
    }
}

@IgnoreExtraProperties
data class Comment(
    @DocumentId val id: String = "",
    val postId: String = "",
    val userId: String = "",
    val appScope: String = "noise_live_public",
    val username: String = "",
    val text: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Story(
    @DocumentId val id: String = "",
    val storyId: String = "",
    val userId: String = "",
    val ownerUid: String = "",
    val appScope: String = "noise_live_public",
    val username: String = "",
    val authorAvatarKey: String = "S",
    val mediaKey: String = "roses",
    val mediaUrl: String = "",
    val storagePath: String = "",
    val thumbnailUrl: String = "",
    val cloudMediaId: String = "",
    val mediaType: String = "photo",
    val durationSec: Long = 6L,
    val expiresAtMillis: Long = 0L,
    val expiresAt: Long = 0L,
    val viewedByUids: List<String> = emptyList(),
    val viewers: List<String> = emptyList(),
    val caption: String = "",
    val uploadStatus: String = "Published",
    @ServerTimestamp val createdAt: Timestamp? = null
) {
    fun effectiveMediaSource(): String = mediaUrl.ifBlank { mediaKey }
}

@IgnoreExtraProperties
data class CloudMediaObjectDoc(
    @DocumentId val id: String = "",
    val mediaId: String = "",
    val ownerUid: String = "",
    val appScope: String = "noise_live_public",
    val targetCategory: String = "post", // "post", "story", "avatar"
    val targetDocId: String = "",
    val storagePath: String = "",
    val downloadUrl: String = "",
    val mediaType: String = "photo", // "photo" or "video"
    val mimeType: String = "image/jpeg",
    val fileExtension: String = "jpg",
    val totalBytes: Long = 0L,
    val chunkCount: Int = 0,
    val inlineDataBase64: String = "",
    val thumbnailBase64: String = "",
    val uploadStatus: String = "Published",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class CloudMediaChunkDoc(
    @DocumentId val id: String = "",
    val mediaId: String = "",
    val ownerUid: String = "",
    val chunkIndex: Int = 0,
    val dataBase64: String = "",
    val byteLength: Int = 0,
    @ServerTimestamp val createdAt: Timestamp? = null
)

data class MediaUploadState(
    val status: String = "Idle", // "Idle", "Pending", "Uploading", "Uploaded", "Published", "Failed"
    val progressPercent: Int = 0,
    val targetType: String = "post", // "post" or "story"
    val pendingDocId: String = "",
    val sourceUri: String = "",
    val mediaType: String = "photo",
    val caption: String = "",
    val location: String = "",
    val tag: String = "",
    val vibeEmoji: String = "✨",
    val errorMessage: String? = null
)

@IgnoreExtraProperties
data class StoryViewDoc(
    @DocumentId val id: String = "",
    val storyId: String = "",
    val viewerUid: String = "",
    val viewerId: String = "",
    val viewerUsername: String = "",
    @ServerTimestamp val viewedAt: Timestamp? = null
)

enum class RoomRole(val displayName: String, val rank: Int) {
    OWNER("OWNER", 5),
    SUPER_ADMIN("SUPER ADMIN", 4),
    ADMIN("ADMIN", 3),
    MODERATOR("MODERATOR", 2),
    MEMBER("MEMBER", 1);

    val label: String get() = displayName

    fun canManageSeats(): Boolean = rank >= ADMIN.rank
    fun canLockOrUnlockSeat(): Boolean = canManageSeats()
    fun canManageMusicAndTheme(): Boolean = rank >= ADMIN.rank
    fun canManageRoomSettings(): Boolean = canManageMusicAndTheme()
    fun canMuteParticipants(): Boolean = rank >= MODERATOR.rank
    fun canClearChat(): Boolean = rank >= MODERATOR.rank
    fun canClearRoomChat(): Boolean = canClearChat()
    fun canKickRole(targetRole: RoomRole): Boolean = rank >= ADMIN.rank && rank > targetRole.rank
    fun canMuteOrKickParticipant(targetRole: RoomRole): Boolean = canMuteParticipants() && rank > targetRole.rank
    fun canManageTarget(targetRole: RoomRole): Boolean = rank >= SUPER_ADMIN.rank && rank > targetRole.rank
    fun canDeleteOrCloseRoom(): Boolean = this == OWNER
    fun canAssignRole(newRole: RoomRole, targetCurrentRole: RoomRole = MEMBER): Boolean {
        if (this == OWNER) return newRole != OWNER
        if (this == SUPER_ADMIN) {
            return targetCurrentRole.rank < SUPER_ADMIN.rank &&
                (newRole == ADMIN || newRole == MODERATOR || newRole == MEMBER)
        }
        return false
    }
}

data class SeatOccupant(
    val seatNumber: Int,
    val userId: String,
    val username: String,
    val avatarKey: String,
    val micMuted: Boolean
)

data class RoomParticipant(
    val userId: String,
    val username: String,
    val avatarKey: String,
    val online: Boolean = true
)

data class RoomChatMessage(
    val id: String,
    val senderId: String,
    val senderName: String,
    val senderRole: String,
    val text: String,
    val timeLabel: String
) {
    val senderUsername: String get() = senderName
}

@IgnoreExtraProperties
data class PartyRoom(
    @DocumentId val id: String = "",
    val roomId: String = "",
    val hostId: String = "",
    val hostUid: String = "",
    val ownerUid: String = "",
    val appScope: String = "noise_live_public",
    val hostUsername: String = "",
    val ownerUsername: String = "",
    val hostAvatarKey: String = "",
    val roomName: String = "",
    val roomImageUrl: String = "",
    val roomCover: String = "starry_mountain",
    val stageTheme: String = "starry_mountain",
    val title: String = "",
    val subtitle: String = "Earn VIP Gifts",
    val description: String = "Welcome to VIP Party Stage!",
    val roomDescription: String = "Welcome to VIP Party Stage!",
    val category: String = "Party",
    val roomCategory: String = "Party",
    val visibility: String = "public", // "public" or "private"
    val invitationToken: String = "",
    val authorizedUserIds: List<String> = emptyList(),
    @get:PropertyName("isActive")
    val isActive: Boolean = true,
    val active: Boolean = true,
    val roomStatus: String = "active", // "active", "live", or "closed"
    val participantCount: Long = 1L,
    val musicTrack: String = "None",
    val musicSelection: String = "None",
    val musicPlaying: Boolean = false,
    val musicVolume: Long = 80L,
    val gameName: String = "PK Battle",
    val gameSelection: String = "PK Battle",
    val micMode: String = "Free Mic",
    val microphoneSettings: String = "Free Mic",
    val capacity: Long = 12L,
    val roomCapacity: Long = 12L,
    val privateRoom: Boolean = false,
    val publicRoom: Boolean = true,
    val password: String = "",
    val themeKey: String = "starry_mountain",
    val pkActive: Boolean = false,
    val wipedScreen: Boolean = false,
    val seat1Name: String = "",
    val seat1Avatar: String = "",
    val seat1Diamonds: Long = 0L,
    val seat2Name: String = "",
    val seat2Avatar: String = "",
    val seat2Diamonds: Long = 0L,
    val lockedSeats: List<Long> = listOf(4L, 7L, 9L, 11L),
    val occupiedSeatsJson: String = "",
    val superAdminIds: List<String> = emptyList(),
    val adminIds: List<String> = emptyList(),
    val moderatorIds: List<String> = emptyList(),
    val mutedUserIds: List<String> = emptyList(),
    val kickedUserIds: List<String> = emptyList(),
    val participantsJson: String = "",
    val chatMessagesJson: String = "",
    val roomNotice: String = "Welcome to VIP Party Stage!",
    val youtubeVideoUrl: String = "",
    val youtubeVideoId: String = "",
    val youtubeVideoTitle: String = "",
    val youtubePlaying: Boolean = false,
    val youtubePositionSec: Long = 0L,
    val youtubeUpdatedAtMillis: Long = 0L,
    val listenersCount: Long = 1L,
    val giftsTotal: Long = 0L,
    val live: Boolean = true,
    val createdTimestamp: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    @Exclude
    fun resolvedRoomName(): String = roomName.ifBlank { title }.trim()

    @Exclude
    fun resolvedHostUid(): String = hostId.ifBlank { hostUid.ifBlank { ownerUid } }.trim()

    @Exclude
    fun resolvedDescription(): String = roomDescription.ifBlank { description.ifBlank { subtitle } }.trim()

    @Exclude
    fun resolvedCategory(): String = roomCategory.ifBlank { category.ifBlank { "Party" } }.trim()

    @Exclude
    fun resolvedCoverThemeKey(): String =
        themeKey.ifBlank { stageTheme.ifBlank { roomCover.ifBlank { roomImageUrl.ifBlank { "starry_mountain" } } } }.trim()

    @Exclude
    fun resolvedCoverThemeLabel(): String {
        return when (resolvedCoverThemeKey().lowercase()) {
            "starry_mountain" -> "Starry Mountain"
            "burj_sunset" -> "Sunset Tower"
            "royal_majlis" -> "Royal Lounge"
            "cosmic_nebula" -> "Cosmic Nebula"
            "imax_stage" -> "Cinema Stage"
            "desert_dunes" -> "Golden Dunes"
            "oasis_camp" -> "Oasis Camp"
            "palace_courtyard" -> "Palace Palms"
            "luxury_motors" -> "Supercars Hall"
            "candy_clouds" -> "Dream Clouds"
            "canyon_pass" -> "Canyon Pass"
            "saturn_rings" -> "Ringed Planet"
            else -> "Starry Mountain"
        }
    }

    @Exclude
    fun isPublicRoom(): Boolean {
        return !privateRoom && !visibility.equals("private", ignoreCase = true)
    }

    @Exclude
    fun isDummyOrSampleRoom(): Boolean {
        val checkTitle = resolvedRoomName().lowercase()
        val checkId = id.ifBlank { roomId }.lowercase()
        val checkHost = resolvedHostUid().lowercase()
        val checkHostUsername = hostUsername.ifBlank { ownerUsername }.lowercase()
        if (checkId.isBlank()) return true
        if (checkId == "vip_party_stage_card" ||
            checkId.startsWith("room_open_stage_") ||
            checkId.startsWith("dummy_") ||
            checkId.startsWith("sample_") ||
            checkId.startsWith("seed_") ||
            checkId.startsWith("demo_") ||
            checkId.startsWith("test_") ||
            checkId.startsWith("bug_")
        ) return true
        if (checkTitle.isBlank() ||
            checkTitle == "open stage" ||
            checkTitle == "mic lem886's stage" ||
            checkTitle == "sebin_mr's stage" ||
            checkTitle == "noise's stage" ||
            checkTitle.contains("dummy room") ||
            checkTitle.contains("demo room") ||
            checkTitle.contains("sample room") ||
            checkTitle.contains("test room")
        ) return true
        if (checkHost.isBlank() ||
            checkHost.startsWith("acct_nora_") ||
            checkHost.startsWith("acct_creator_") ||
            checkHost.startsWith("acct_maya_") ||
            checkHost.startsWith("dummy_") ||
            checkHost.startsWith("demo_")
        ) return true
        if (checkHostUsername == "mic lem886" || checkHostUsername == "mic_lem886") return true
        if (seat1Name == "Rehan" && seat2Name == "Soniya") return true
        return false
    }

    @Exclude
    fun formattedCreationDate(): String {
        val date = createdAt?.toDate()
            ?: (if (createdTimestamp > 0L) java.util.Date(createdTimestamp) else null)
            ?: updatedAt?.toDate()
            ?: return "Just now"
        return java.text.SimpleDateFormat("MMM dd, yyyy • hh:mm a", java.util.Locale.US).format(date)
    }

    @Exclude
    fun isUserAuthorizedForRoom(
        uid: String,
        suppliedToken: String? = null,
        suppliedPassword: String? = null
    ): Boolean {
        if (isPublicRoom()) return true
        if (uid.isNotBlank() && (uid == hostId || uid == hostUid || uid == ownerUid || uid in authorizedUserIds || uid in superAdminIds || uid in adminIds)) {
            return true
        }
        if (!suppliedToken.isNullOrBlank() && invitationToken.isNotBlank() && suppliedToken.trim() == invitationToken.trim()) {
            return true
        }
        if (!suppliedPassword.isNullOrBlank() && password.isNotBlank() && suppliedPassword.trim() == password.trim()) {
            return true
        }
        return false
    }

    @Exclude
    fun buildShareInviteUrl(includeToken: Boolean = true): String {
        val resolvedId = id.ifBlank { roomId }
        val tokenParam = if (includeToken && invitationToken.isNotBlank()) "?token=$invitationToken" else ""
        return "https://noiselive.app/room/$resolvedId$tokenParam"
    }

    @Exclude
    fun buildShareMessage(includeToken: Boolean = true): String {
        val name = resolvedRoomName().ifBlank { "NOISE LIVE Room" }
        val link = buildShareInviteUrl(includeToken)
        return "Join my NOISE LIVE room!\nRoom: $name\nJoin here: $link"
    }

    @Exclude
    fun roleForUser(userId: String): RoomRole {
        if (userId.isBlank()) return RoomRole.MEMBER
        return when {
            userId == hostId || (hostUid.isNotBlank() && userId == hostUid) || (ownerUid.isNotBlank() && userId == ownerUid) -> RoomRole.OWNER
            userId in superAdminIds -> RoomRole.SUPER_ADMIN
            userId in adminIds -> RoomRole.ADMIN
            userId in moderatorIds -> RoomRole.MODERATOR
            else -> RoomRole.MEMBER
        }
    }

    @Exclude
    fun resolvedHostAvatarKey(): String {
        if (hostAvatarKey.isNotBlank()) return hostAvatarKey
        val hostFromParticipants = parseParticipants().firstOrNull { it.userId == hostId || it.userId == hostUid || it.userId == ownerUid }?.avatarKey
        if (!hostFromParticipants.isNullOrBlank()) return hostFromParticipants
        if (seat1Avatar.isNotBlank()) return seat1Avatar
        return hostUsername.ifBlank { ownerUsername }.take(1).uppercase().ifBlank { "H" }
    }

    fun activeParticipantCount(): Int {
        val parsedCount = parseParticipants().size
        return maxOf(parsedCount, participantCount.toInt(), listenersCount.toInt(), 1)
    }

    fun resolvedYouTubeVideoId(): String {
        return youtubeVideoId.ifBlank { extractYouTubeVideoId(youtubeVideoUrl) }
    }

    fun estimatedYouTubePositionSec(nowMillis: Long = System.currentTimeMillis()): Long {
        if (!youtubePlaying || youtubeUpdatedAtMillis <= 0L) {
            return youtubePositionSec.coerceAtLeast(0L)
        }
        val elapsedSec = ((nowMillis - youtubeUpdatedAtMillis) / 1000L).coerceAtLeast(0L)
        return (youtubePositionSec + elapsedSec).coerceAtLeast(0L)
    }

    fun parseOccupiedSeats(): Map<Int, SeatOccupant> {
        if (occupiedSeatsJson.isBlank()) return emptyMap()
        val result = mutableMapOf<Int, SeatOccupant>()
        val seenUsers = mutableSetOf<String>()
        occupiedSeatsJson.split(";").filter { it.isNotBlank() }.forEach { entry ->
            val parts = entry.split(":")
            when {
                parts.size >= 5 -> {
                    val seatNum = parts[0].toIntOrNull()
                    val uid = parts[1]
                    val uname = parts[2]
                    val avatar = parts[3].replace("%3A", ":").ifBlank { uname.take(1).uppercase() }
                    val muted = parts[4].toBooleanStrictOrNull() ?: false
                    if (seatNum != null && seatNum in 1..12 && uid.isNotBlank() && uid !in seenUsers && seatNum !in result) {
                        seenUsers.add(uid)
                        result[seatNum] = SeatOccupant(seatNum, uid, uname, avatar, muted)
                    }
                }
                parts.size == 2 -> {
                    // Legacy "3:username" fallback
                    val seatNum = parts[0].toIntOrNull()
                    val uname = parts[1]
                    if (seatNum != null && seatNum in 1..12 && uname.isNotBlank() && uname !in seenUsers && seatNum !in result) {
                        seenUsers.add(uname)
                        result[seatNum] = SeatOccupant(
                            seatNumber = seatNum,
                            userId = uname,
                            username = uname,
                            avatarKey = uname.take(1).uppercase(),
                            micMuted = false
                        )
                    }
                }
            }
        }
        return result
    }

    fun parseParticipants(): List<RoomParticipant> {
        val map = linkedMapOf<String, RoomParticipant>()
        if (participantsJson.isNotBlank()) {
            participantsJson.split(";").filter { it.isNotBlank() }.forEach { item ->
                val parts = item.split(":")
                if (parts.size >= 3) {
                    val uid = parts[0]
                    val uname = parts[1]
                    val avatar = parts[2].replace("%3A", ":").ifBlank { uname.take(1).uppercase() }
                    val online = parts.getOrNull(3)?.toBooleanStrictOrNull() ?: true
                    if (uid.isNotBlank() && uname.isNotBlank() && uid !in kickedUserIds) {
                        map[uid] = RoomParticipant(uid, uname, avatar, online)
                    }
                }
            }
        }
        // Also ensure any user currently in a seat is represented in participants
        parseOccupiedSeats().values.forEach { occ ->
            if (occ.userId !in map && occ.userId !in kickedUserIds) {
                map[occ.userId] = RoomParticipant(occ.userId, occ.username, occ.avatarKey, true)
            }
        }
        return map.values.toList()
    }

    fun parseChatMessages(): List<RoomChatMessage> {
        if (chatMessagesJson.isBlank()) return emptyList()
        return chatMessagesJson.split("\n").filter { it.isNotBlank() }.mapNotNull { line ->
            val parts = line.split("|", limit = 6)
            if (parts.size >= 6) {
                RoomChatMessage(
                    id = parts[0],
                    senderId = parts[1],
                    senderName = parts[2],
                    senderRole = parts[3],
                    text = parts[4],
                    timeLabel = parts[5]
                )
            } else null
        }
    }

    companion object {
        fun extractYouTubeVideoId(input: String): String {
            val clean = input.trim()
            if (clean.isBlank()) return ""
            // Direct 11-character YouTube video ID
            if (clean.matches(Regex("^[a-zA-Z0-9_-]{11}$"))) {
                return clean
            }
            val patterns = listOf(
                Regex("(?:youtube\\.com/watch\\?.*v=|youtu\\.be/|youtube\\.com/embed/|youtube\\.com/live/|youtube\\.com/shorts/|youtube\\.com/v/)([a-zA-Z0-9_-]{11})"),
                Regex("[?&]v=([a-zA-Z0-9_-]{11})")
            )
            for (pattern in patterns) {
                val match = pattern.find(clean)
                if (match != null && match.groupValues.size > 1) {
                    return match.groupValues[1]
                }
            }
            return ""
        }

        fun encodeOccupiedSeats(seats: Map<Int, SeatOccupant>): String {
            val deduplicatedByUser = linkedMapOf<String, SeatOccupant>()
            seats.entries.sortedBy { it.key }.forEach { (_, occ) ->
                if (occ.userId.isNotBlank() && occ.userId !in deduplicatedByUser) {
                    deduplicatedByUser[occ.userId] = occ
                }
            }
            return deduplicatedByUser.values.joinToString(";") { occ ->
                val safeName = occ.username.replace(":", "").replace(";", "")
                val safeAvatar = occ.avatarKey.replace(":", "%3A").replace(";", "")
                "${occ.seatNumber}:${occ.userId}:$safeName:$safeAvatar:${occ.micMuted}"
            }
        }

        fun encodeParticipants(participants: List<RoomParticipant>): String {
            return participants.distinctBy { it.userId }.take(50).joinToString(";") { p ->
                val safeName = p.username.replace(":", "").replace(";", "")
                val safeAvatar = p.avatarKey.replace(":", "%3A").replace(";", "")
                "${p.userId}:$safeName:$safeAvatar:${p.online}"
            }
        }

        fun encodeChatMessages(messages: List<RoomChatMessage>): String {
            return messages.takeLast(40).joinToString("\n") { m ->
                val safeName = m.senderName.replace("|", " ").replace("\n", " ")
                val safeRole = m.senderRole.replace("|", " ").replace("\n", " ")
                val safeText = m.text.replace("|", " ").replace("\n", " ").take(240)
                val safeTime = m.timeLabel.replace("|", " ").replace("\n", " ")
                "${m.id}|${m.senderId}|$safeName|$safeRole|$safeText|$safeTime"
            }
        }
    }
}

@IgnoreExtraProperties
data class NotificationItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val actorUid: String = "",
    val actorUsername: String = "",
    val actorAvatarKey: String = "",
    val type: String = "like", // "like", "comment", "follow", "mention", "reel_like", "game_invite", "message", "call"
    val message: String = "",
    val targetThumbKey: String = "",
    val conversationId: String = "",
    val gameRoomId: String = "",
    val timeLabel: String = "Just now",
    val read: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    @get:Exclude
    val createdAtMillis: Long
        get() = createdAt?.toDate()?.time ?: 0L
}

@IgnoreExtraProperties
data class Conversation(
    @DocumentId val id: String = "",
    val participantIds: List<String> = emptyList(),
    val initiatorUid: String = "",
    val initiatorUsername: String = "",
    val initiatorDisplayName: String = "",
    val initiatorAvatarKey: String = "S",
    val initiatorVipBadgeLevel: Long = 0L,
    val peerUid: String = "",
    val peerUsername: String = "",
    val peerDisplayName: String = "",
    val peerAvatarKey: String = "S",
    val peerVerified: Boolean = false,
    val peerOnline: Boolean = true,
    val peerTyping: Boolean = false,
    val typingUserIds: List<String> = emptyList(),
    val peerLastSeen: String = "Active now",
    val pinnedMessageIds: List<String> = emptyList(),
    val lastMessage: String = "",
    val lastTimeLabel: String = "09:12 PM",
    val unreadCount: Long = 0L,
    val tabCategory: String = "Primary",
    val peerVipBadgeLevel: Long = 0L,
    val chatThemeKey: String = "default_dark",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    /**
     * Resolves peer metadata for the current logged-in user so BOTH User A (initiator)
     * and User B (recipient) see the other user's display name, username, avatar, and VIP level.
     */
    fun resolveForCurrentUser(currentUserId: String): Conversation {
        if (currentUserId.isBlank()) return this
        val otherParticipantUid = participantIds.firstOrNull { it.isNotBlank() && it != currentUserId }.orEmpty()
        return if (peerUid == currentUserId && initiatorUid.isNotBlank() && initiatorUid != currentUserId) {
            copy(
                peerUid = initiatorUid,
                peerUsername = initiatorUsername.ifBlank { peerUsername },
                peerDisplayName = initiatorDisplayName.ifBlank { initiatorUsername.ifBlank { peerDisplayName } },
                peerAvatarKey = initiatorAvatarKey.ifBlank { peerAvatarKey },
                peerVipBadgeLevel = if (initiatorVipBadgeLevel > 0L) initiatorVipBadgeLevel else peerVipBadgeLevel,
                peerTyping = typingUserIds.any { it != currentUserId }
            )
        } else {
            copy(
                peerUid = peerUid.ifBlank { otherParticipantUid },
                peerTyping = typingUserIds.any { it != currentUserId } || (peerTyping && peerUid != currentUserId)
            )
        }
    }
}

data class ChatBackgroundThemeOption(
    val key: String,
    val title: String,
    val category: String, // "Dark", "Premium", "Animated"
    val primaryHex: Long,
    val secondaryHex: Long,
    val accentHex: Long,
    val animated: Boolean = false,
    val subtitle: String = ""
)

val CHAT_BACKGROUND_THEMES: List<ChatBackgroundThemeOption> = listOf(
    ChatBackgroundThemeOption(
        key = "default_dark",
        title = "NOICE Obsidian",
        category = "Dark",
        primaryHex = 0xFF050508,
        secondaryHex = 0xFF0E101A,
        accentHex = 0xFF6B2CF5,
        animated = false,
        subtitle = "Original NOICE dark obsidian theme"
    ),
    ChatBackgroundThemeOption(
        key = "electric_violet",
        title = "Electric Violet Royal",
        category = "Premium",
        primaryHex = 0xFF0D0822,
        secondaryHex = 0xFF19123B,
        accentHex = 0xFF6B2CF5,
        animated = false,
        subtitle = "Electric violet & deep royal blue luxury"
    ),
    ChatBackgroundThemeOption(
        key = "royal_cyber_blue",
        title = "Cyber Sapphire",
        category = "Premium",
        primaryHex = 0xFF061024,
        secondaryHex = 0xFF0D234A,
        accentHex = 0xFF00E5FF,
        animated = false,
        subtitle = "Deep royal blue with cyan highlights"
    ),
    ChatBackgroundThemeOption(
        key = "aurora_pulse",
        title = "Aurora Borealis",
        category = "Animated",
        primaryHex = 0xFF071324,
        secondaryHex = 0xFF142A3B,
        accentHex = 0xFF00E5FF,
        animated = true,
        subtitle = "Animated cyan & violet northern lights"
    ),
    ChatBackgroundThemeOption(
        key = "vip_gold_dynasty",
        title = "VIP Gold Dynasty",
        category = "Animated",
        primaryHex = 0xFF171105,
        secondaryHex = 0xFF2B1E08,
        accentHex = 0xFFFFB800,
        animated = true,
        subtitle = "Animated sovereign gold & ember shimmer"
    ),
    ChatBackgroundThemeOption(
        key = "cosmic_nebula",
        title = "Cosmic Nebula",
        category = "Animated",
        primaryHex = 0xFF140726,
        secondaryHex = 0xFF280E42,
        accentHex = 0xFFD946EF,
        animated = true,
        subtitle = "Animated starlight & magenta-violet nebula"
    )
)

@IgnoreExtraProperties
data class CallSessionDoc(
    @DocumentId val id: String = "",
    val callId: String = id,
    val appScope: String = "noise_live_public",
    val conversationId: String = "",
    val liveKitRoomName: String = "",
    val liveKitUrl: String = "https://noice-live26.vercel.app",
    val callerId: String = "",
    val callerUsername: String = "",
    val callerDisplayName: String = "",
    val callerAvatarKey: String = "S",
    val callerVipLevel: Long = 0L,
    val calleeId: String = "",
    val calleeUsername: String = "",
    val calleeDisplayName: String = "",
    val calleeAvatarKey: String = "S",
    val calleeVipLevel: Long = 0L,
    val participantIds: List<String> = emptyList(),
    val callType: String = "audio", // "audio" or "video"
    val status: String = "ringing", // "ringing", "accepted", "rejected", "busy", "ended", "missed"
    val callerMicMuted: Boolean = false,
    val calleeMicMuted: Boolean = false,
    val callerCameraEnabled: Boolean = false,
    val calleeCameraEnabled: Boolean = false,
    val callerFrontCamera: Boolean = true,
    val calleeFrontCamera: Boolean = true,
    val durationSec: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class DirectMessage(
    @DocumentId val id: String = "",
    val conversationId: String = "",
    val senderId: String = "",
    val receiverId: String = "",
    val senderName: String = "",
    val participantIds: List<String> = emptyList(),
    val text: String = "",
    val messageContent: String = "",
    val messageType: String = "text", // "text", "image", "video", "voice", "call"
    val mediaKey: String = "",
    val mediaUrl: String = "",
    val mediaType: String = "photo",
    val voiceDurationSec: Long = 0L,
    val voiceWaveform: String = "", // comma-separated amplitudes 0..100
    val deliveryStatus: String = "sent", // "sent" (1 grey tick), "delivered" (2 grey ticks), "seen" (2 blue ticks)
    val status: String = "sent", // "sent", "delivered", "read"
    val timeLabel: String = "09:12 PM",
    val reactionEmoji: String = "",
    val replyToMessageId: String = "",
    val replyToSenderName: String = "",
    val replyToText: String = "",
    val pinned: Boolean = false,
    val savedLocally: Boolean = false,
    val read: Boolean = false,
    val senderVipBadgeLevel: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    val deliveredAt: Timestamp? = null,
    val readAt: Timestamp? = null
)

@IgnoreExtraProperties
data class VoiceRecordingDoc(
    @DocumentId val id: String = "",
    val messageId: String = "",
    val conversationId: String = "",
    val senderId: String = "",
    val receiverId: String = "",
    val participantIds: List<String> = emptyList(),
    val mimeType: String = "audio/wav",
    val audioBase64: String = "",
    val durationSec: Long = 0L,
    val waveform: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class CoinTransaction(
    @DocumentId val id: String = "",
    val userId: String = "",
    val tierName: String = "",
    val amount: Long = 0L,
    val balanceAfter: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null
)

data class PartyGiftCatalogItem(
    val id: String,
    val name: String,
    val emoji: String,
    val cost: Long,
    val category: String
)

data class RoomThemeOption(
    val key: String,
    val title: String,
    val category: String, // "Gallery", "Premium", "Customize"
    val primaryHex: Long,
    val secondaryHex: Long,
    val accentHex: Long,
    val drawableKey: String = ""
)

enum class GraphicsQualityLevel(
    val label: String,
    val targetFps: Int,
    val shadowQuality: String,
    val antiAliasingSamples: Int,
    val particleMultiplier: Float,
    val renderScale: Float
) {
    LOW("Low", 30, "Off", 0, 0.4f, 0.7f),
    MEDIUM("Medium", 45, "Soft Shadows", 2, 0.75f, 0.85f),
    HIGH("High (URP Ultra)", 60, "Real-Time Cascaded", 4, 1.0f, 1.0f)
}

data class NoiseGameCatalogItem(
    val gameType: String = "carrom_3d",
    val id: String = gameType,
    val title: String = "3D Carrom Pro",
    val subtitle: String = "",
    val category: String = "Board & Physics",
    val minPlayers: Int = 2,
    val maxPlayers: Int = 4,
    val supportsVoiceChatroom: Boolean = true,
    val featured: Boolean = true,
    val popular: Boolean = true,
    val activePlayersCount: Int = 1000,
    val unitySceneName: String = "Carrom3DArenaScene",
    val accentColorHex: Long = 0xFFFFB800,
    val secondaryColorHex: Long = 0xFFFF2E93,
    val badgeText: String = "3D URP",
    val supportedModes: List<String> = listOf("1v1", "2v2"),
    val netcodeMode: String = "Unity Netcode + Relay",
    val primaryHex: Long = accentColorHex,
    val secondaryHex: Long = secondaryColorHex,
    val accentHex: Long = accentColorHex,
    val iconEmoji: String = "🎮"
)

data class GamePlayerSlot(
    val slotIndex: Int = 1,
    val slotNumber: Int = slotIndex,
    val userId: String = "",
    val username: String = "",
    val displayName: String = username,
    val avatarKey: String = "S",
    val team: Int = 1,
    val ready: Boolean = true,
    val score: Long = 0L,
    val connected: Boolean = true,
    val micEnabled: Boolean = true,
    val speaking: Boolean = false
)

@IgnoreExtraProperties
data class GameRoomDoc(
    @DocumentId val id: String = "",
    val appScope: String = "noise_live_public",
    val roomCode: String = "",
    val joinCode: String = roomCode,
    val gameType: String = "carrom_3d",
    val gameId: String = gameType,
    val gameTitle: String = "3D Carrom Pro",
    val gameName: String = gameTitle,
    val roomTitle: String = gameTitle,
    val hostId: String = "",
    val hostUsername: String = "",
    val hostAvatarKey: String = "S",
    val chatRoomId: String = "",
    val chatRoomTitle: String = "",
    val mode: String = "1v1",
    val matchMode: String = mode,
    val status: String = "waiting",
    val privateRoom: Boolean = false,
    val maxPlayers: Long = 2L,
    val currentTurnUserId: String = "",
    val currentTurnUid: String = currentTurnUserId,
    val winnerUserId: String = "",
    val winnerUid: String = winnerUserId,
    val winnerUsername: String = "",
    val unityLobbyId: String = "",
    val unityRelayJoinCode: String = "",
    val voiceChannelId: String = "",
    val voiceRoomConnected: Boolean = true,
    val participantIds: List<String> = emptyList(),
    val invitedUserIds: List<String> = emptyList(),
    val playersJson: String = "",
    val gameStateJson: String = "{}",
    val entryCoins: Long = 100L,
    val prizePoolCoins: Long = 200L,
    val scoresMap: Map<String, Long> = emptyMap(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val playerIds: List<String> get() = participantIds

    fun parsePlayers(): List<GamePlayerSlot> {
        if (playersJson.isBlank()) return emptyList()
        return playersJson.split(";").filter { it.isNotBlank() }.mapNotNull { entry ->
            val parts = entry.split(":")
            if (parts.size >= 6) {
                val slotIdx = parts[0].toIntOrNull() ?: 1
                val uid = parts[1]
                val uname = parts[2]
                val avatar = parts[3].ifBlank { uname.take(1).uppercase() }
                val ready = parts[4].toBooleanStrictOrNull() ?: true
                val score = parts[5].toLongOrNull() ?: 0L
                val connected = parts.getOrNull(6)?.toBooleanStrictOrNull() ?: true
                val teamNum = parts.getOrNull(7)?.toIntOrNull() ?: if (slotIdx % 2 == 1) 1 else 2
                val micOn = parts.getOrNull(8)?.toBooleanStrictOrNull() ?: true
                val isSpeaking = parts.getOrNull(9)?.toBooleanStrictOrNull() ?: false
                if (uid.isNotBlank()) {
                    GamePlayerSlot(
                        slotIndex = slotIdx,
                        slotNumber = slotIdx,
                        userId = uid,
                        username = uname,
                        displayName = uname,
                        avatarKey = avatar,
                        team = teamNum,
                        ready = ready,
                        score = score,
                        connected = connected,
                        micEnabled = micOn,
                        speaking = isSpeaking
                    )
                } else null
            } else null
        }
    }

    companion object {
        fun encodePlayers(players: List<GamePlayerSlot>): String {
            return players.distinctBy { it.userId }.joinToString(";") { p ->
                val safeName = p.username.replace(":", "").replace(";", "")
                val safeAvatar = p.avatarKey.replace(":", "").replace(";", "")
                "${p.slotIndex}:${p.userId}:$safeName:$safeAvatar:${p.ready}:${p.score}:${p.connected}:${p.team}:${p.micEnabled}:${p.speaking}"
            }
        }
    }
}

fun encodeGamePlayers(players: List<GamePlayerSlot>): String = GameRoomDoc.encodePlayers(players)

@IgnoreExtraProperties
data class GameInviteDoc(
    @DocumentId val id: String = "",
    val appScope: String = "noise_live_public",
    val gameRoomId: String = "",
    val roomCode: String = "",
    val gameType: String = "carrom_3d",
    val gameId: String = gameType,
    val gameTitle: String = "3D Carrom Pro",
    val gameName: String = gameTitle,
    val chatRoomId: String = "",
    val chatRoomTitle: String = "",
    val senderId: String = "",
    val senderUsername: String = "",
    val senderAvatarKey: String = "S",
    val targetUserId: String = "",
    val targetUsername: String = "",
    val slotIndex: Int = 2,
    val expiresAtMillis: Long = 0L,
    val status: String = "pending",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val toUserId: String get() = targetUserId
    @get:Exclude
    val senderUid: String get() = senderId
    @get:Exclude
    val receiverUid: String get() = targetUserId
    @get:Exclude
    val receiverUsername: String get() = targetUsername
    @get:Exclude
    val createdAtMillis: Long get() = createdAt?.toDate()?.time ?: 0L
}

@IgnoreExtraProperties
data class GameMatchResultDoc(
    @DocumentId val id: String = "",
    val appScope: String = "noise_live_public",
    val gameRoomId: String = "",
    val gameType: String = "carrom_3d",
    val gameId: String = gameType,
    val gameTitle: String = "3D Carrom Pro",
    val gameName: String = gameTitle,
    val chatRoomId: String = "",
    val winnerId: String = "",
    val winnerUsername: String = "",
    val winnerAvatarKey: String = "S",
    val runnerUpUsername: String = "",
    val participantIds: List<String> = emptyList(),
    val winnerScore: Long = 0L,
    val runnerUpScore: Long = 0L,
    val coinsAwarded: Long = 200L,
    val durationSec: Long = 60L,
    val verifiedByServer: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null
) {
    val winnerUserId: String get() = winnerId
    val coinsRewarded: Long get() = coinsAwarded
}

data class VipBadgeCatalogItem(
    val vipLevel: Int,
    val badgeId: String = "vip_$vipLevel",
    val title: String, // e.g., "VIP 1 – Blue Sapphire"
    val gemstoneName: String, // e.g., "Blue Sapphire"
    val frameMaterial: String, // e.g., "Silver Royal Frame"
    val effectDescription: String,
    val priceCoins: Long,
    val primaryGemHex: Long,
    val secondaryGemHex: Long,
    val highlightGemHex: Long = secondaryGemHex,
    val framePrimaryHex: Long = 0xFFFFD54F,
    val frameSecondaryHex: Long = framePrimaryHex,
    val particleHex: Long = secondaryGemHex,
    val hasRoyalCrown: Boolean = vipLevel >= 3,
    val hasSculptedWings: Boolean = vipLevel >= 5
) {
    val badgeName: String get() = title
    val frameDescription: String get() = frameMaterial

    constructor(
        vipLevel: Int,
        badgeName: String,
        gemstoneName: String,
        frameDescription: String,
        effectDescription: String,
        priceCoins: Long,
        primaryColorHex: Long,
        secondaryColorHex: Long,
        frameColorHex: Long,
        glowColorHex: Long
    ) : this(
        vipLevel = vipLevel,
        badgeId = "vip_$vipLevel",
        title = badgeName,
        gemstoneName = gemstoneName,
        frameMaterial = frameDescription,
        effectDescription = effectDescription,
        priceCoins = priceCoins,
        primaryGemHex = primaryColorHex,
        secondaryGemHex = secondaryColorHex,
        highlightGemHex = glowColorHex,
        framePrimaryHex = frameColorHex,
        frameSecondaryHex = frameColorHex,
        particleHex = glowColorHex,
        hasRoyalCrown = vipLevel >= 3,
        hasSculptedWings = vipLevel >= 5
    )
}

@IgnoreExtraProperties
data class VipBadgePurchaseDoc(
    @DocumentId val id: String = "",
    val appScope: String = "noise_live_public",
    val userId: String = "",
    val username: String = "",
    val vipLevel: Long = 1L,
    val badgeId: String = "vip_1",
    val badgeName: String = "Blue Sapphire",
    val gemstoneName: String = "Blue Sapphire",
    val priceCoins: Long = 2000L,
    val balanceBefore: Long = 0L,
    val balanceAfter: Long = 0L,
    val verifiedByBackend: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null
)

data class GameModuleStatusItem(
    val gameType: String,
    val title: String,
    val engineType: String,
    val multiplayerSupport: String,
    val rulesStatus: String,
    val audioStatus: String,
    val completionStatus: String = "Completed • Verified"
) {
    constructor(
        gameType: String,
        title: String,
        completionStatus: String,
        uiDone: Boolean,
        rulesDone: Boolean,
        multiplayerDone: Boolean,
        audioDone: Boolean,
        summaryText: String
    ) : this(
        gameType = gameType,
        title = title,
        engineType = summaryText,
        multiplayerSupport = if (multiplayerDone) "Multiplayer Synced" else "Single",
        rulesStatus = if (rulesDone && uiDone) "Complete Rules" else "Partial",
        audioStatus = if (audioDone) "SFX + Music" else "Muted",
        completionStatus = completionStatus
    )
}

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

enum class DiscoveryCategory(
    val key: String,
    val label: String,
    val emoji: String,
    val badgeColorHex: Long
) {
    FOR_YOU("for_you", "For You", "✨", 0xFFFF2E93),
    TRENDING("trending", "Trending", "🔥", 0xFFFF6B00),
    NEWS("news", "News", "📰", 0xFF38BDF8),
    WORLD_NEWS("world_news", "World News", "🌍", 0xFF0EA5E9),
    ENTERTAINMENT("entertainment", "Entertainment", "🎭", 0xFFD946EF),
    MUSIC("music", "Music", "🎵", 0xFFA855F7),
    MOVIES("movies", "Movies", "🎬", 0xFFF59E0B),
    HORROR("horror", "Horror", "👻", 0xFFEF4444),
    GADGETS("gadgets", "Gadgets", "📱", 0xFF06B6D4),
    TECHNOLOGY("technology", "Technology", "💻", 0xFF3B82F6),
    SPORTS("sports", "Sports", "⚽", 0xFF10B981),
    GAMING("gaming", "Gaming", "🎮", 0xFF8B5CF6),
    COMEDY("comedy", "Comedy", "😂", 0xFFFACC15),
    VIRAL_VIDEOS("viral_videos", "Viral Videos", "🚀", 0xFFEC4899),
    FASHION("fashion", "Fashion", "👗", 0xFFF472B6),
    EDUCATION("education", "Education", "🎓", 0xFF14B8A6),
    SCIENCE("science", "Science", "🔬", 0xFF6366F1),
    LIFESTYLE("lifestyle", "Lifestyle", "🌿", 0xFF22C55E);

    companion object {
        fun fromKey(key: String): DiscoveryCategory {
            return entries.firstOrNull { it.key.equals(key, ignoreCase = true) } ?: FOR_YOU
        }
    }
}

enum class KeralaNewsCategory(
    val key: String,
    val label: String,
    val malayalamLabel: String,
    val emoji: String,
    val badgeColorHex: Long
) {
    KERALA_LATEST("kerala_latest", "Kerala Latest News", "കേരളം പുതിയ വാർത്തകൾ", "🌴", 0xFF10B981),
    KERALA_BREAKING("kerala_breaking", "Kerala Breaking News", "ബ്രേക്കിംഗ് ന്യൂസ്", "🚨", 0xFFEF4444),
    KERALA_POLITICS("kerala_politics", "Politics", "രാഷ്ട്രീയം", "🏛️", 0xFFF59E0B),
    KERALA_EDUCATION("kerala_education", "Education", "വിദ്യാഭ്യാസം", "🎓", 0xFF14B8A6),
    KERALA_TECH("kerala_technology", "Technology", "സാങ്കേതികവിദ്യ", "💻", 0xFF3B82F6),
    KERALA_SPORTS("kerala_sports", "Sports", "കായികം", "⚽", 0xFF22C55E),
    KERALA_ENTERTAINMENT("kerala_entertainment", "Entertainment", "സിനിമ & വിനോദം", "🎬", 0xFFD946EF),
    KERALA_BUSINESS("kerala_business", "Business", "ബിസിനസ്", "📈", 0xFF06B6D4),
    KERALA_DISTRICT("kerala_district", "District News", "ജില്ലാ വാർത്തകൾ", "📍", 0xFFA855F7),
    KERALA_WEATHER("kerala_weather", "Weather & Public Info", "കാലാവസ്ഥ & അറിയിപ്പുകൾ", "🌧️", 0xFF38BDF8);

    companion object {
        fun fromKey(key: String): KeralaNewsCategory {
            return entries.firstOrNull { it.key.equals(key, ignoreCase = true) } ?: KERALA_LATEST
        }
    }
}

val KERALA_DISTRICTS: List<String> = listOf(
    "All Districts",
    "Thiruvananthapuram",
    "Kollam",
    "Pathanamthitta",
    "Alappuzha",
    "Kottayam",
    "Idukki",
    "Ernakulam",
    "Thrissur",
    "Palakkad",
    "Malappuram",
    "Kozhikode",
    "Wayanad",
    "Kannur",
    "Kasaragod"
)

@IgnoreExtraProperties
data class DiscoveryContentItem(
    @DocumentId val id: String = "",
    val appScope: String = "noise_live_public",
    val categoryKey: String = "news",
    val categoryLabel: String = "News",
    val title: String = "",
    val description: String = "",
    val thumbnailUrl: String = "",
    val sourceName: String = "",
    val sourceDomain: String = "",
    val originalUrl: String = "",
    val contentType: String = "article", // "article" or "video"
    val videoEmbedUrl: String = "",
    val youtubeVideoId: String = "",
    val publishedAtMillis: Long = 0L,
    val publishedDateLabel: String = "",
    val importedAtMillis: Long = 0L,
    val moderationStatus: String = "approved", // "approved", "pending_review", "rejected", "hidden"
    val moderationNotes: String = "Verified Safe & Positive Content",
    val safetyScore: Long = 100L,
    val trustedSourceVerified: Boolean = true,
    val copyrightVerified: Boolean = true,
    val allowedCountries: List<String> = listOf("ALL"),
    val blockedCountries: List<String> = emptyList(),
    val language: String = "en", // "en" or "ml"
    val district: String = "All Districts",
    val isKeralaNews: Boolean = false,
    val isBreakingNews: Boolean = false,
    val likesCount: Long = 0L,
    val savesCount: Long = 0L,
    val sharesCount: Long = 0L,
    val viewsCount: Long = 0L,
    val likedByUids: List<String> = emptyList(),
    val savedByUids: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    @Exclude
    fun isVideoContent(): Boolean {
        return contentType.equals("video", ignoreCase = true) ||
            youtubeVideoId.isNotBlank() ||
            videoEmbedUrl.isNotBlank()
    }

    @Exclude
    fun computeTrendingScore(nowMillis: Long = System.currentTimeMillis()): Double {
        val ageHours = ((nowMillis - publishedAtMillis).coerceAtLeast(0L).toDouble() / 3_600_000.0)
        val recencyBoost = 100.0 / (1.0 + (ageHours / 6.0))
        val engagementBoost = (likesCount * 5.0) + (savesCount * 6.0) + (sharesCount * 8.0) + (viewsCount * 1.5)
        val categoryBoost = if (categoryKey == "trending" || categoryKey == "viral_videos" || isBreakingNews) 18.0 else 0.0
        return recencyBoost + engagementBoost + categoryBoost
    }
}

@IgnoreExtraProperties
data class DiscoverySourceConfig(
    @DocumentId val id: String = "",
    val appScope: String = "noise_live_public",
    val categoryKey: String = "news",
    val sourceName: String = "",
    val sourceDomain: String = "",
    val feedUrl: String = "",
    val feedFormat: String = "rss", // "rss", "atom", "youtube_rss"
    val contentType: String = "article", // "article" or "video"
    val language: String = "en", // "en" or "ml"
    val isKeralaSource: Boolean = false,
    val enabled: Boolean = true,
    val trustedVerified: Boolean = true,
    val lastFetchedAtMillis: Long = 0L,
    val lastStatus: String = "Ready",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class DiscoveryGlobalConfig(
    @DocumentId val id: String = "",
    val appScope: String = "noise_live_public",
    val enabledCategories: List<String> = DiscoveryCategory.entries.map { it.key },
    val disabledCategories: List<String> = emptyList(),
    val enabledKeralaCategories: List<String> = KeralaNewsCategory.entries.map { it.key },
    val disabledKeralaCategories: List<String> = emptyList(),
    val blockedDomains: List<String> = emptyList(),
    val restrictedCountriesMap: Map<String, List<String>> = emptyMap(),
    val refreshIntervalMinutes: Long = 20L, // Configurable 15-30 minutes
    val strictSafetyPolicyEnabled: Boolean = true,
    val requireManualAdminApprovalForUncertain: Boolean = true,
    val keralaNewsNotificationsEnabled: Boolean = true,
    val trendingReelsNotificationsEnabled: Boolean = true,
    val trendingAlgorithmMode: String = "engagement_velocity", // "engagement_velocity", "views_first", "recent_first"
    val customNewsApiEndpoint: String = "",
    val adminEmails: List<String> = listOf("sebinmr556@gmail.com"),
    val adminUids: List<String> = emptyList(),
    val lastGlobalSyncMillis: Long = 0L,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class DiscoveryImportLog(
    @DocumentId val id: String = "",
    val appScope: String = "noise_live_public",
    val categoryKey: String = "",
    val sourceName: String = "",
    val feedUrl: String = "",
    val status: String = "success", // "success", "error", "filtered"
    val itemsFetched: Long = 0L,
    val itemsSaved: Long = 0L,
    val duplicatesSkipped: Long = 0L,
    val safetyRejected: Long = 0L,
    val errorMessage: String = "",
    val timestampMillis: Long = System.currentTimeMillis(),
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class SuperOwnerRegistryDoc(
    @DocumentId val id: String = "primary",
    val appScope: String = "noise_live_public",
    val superOwnerUid: String = "",
    val superOwnerUsername: String = "sebin_mr",
    val superOwnerEmail: String = "sebinmr556@gmail.com",
    val immutableLock: Boolean = true,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class OwnerAuditLogDoc(
    @DocumentId val id: String = "",
    val appScope: String = "noise_live_public",
    val actorUid: String = "",
    val actorUsername: String = "sebin_mr",
    val actionType: String = "", // e.g. "DELETE_SINGLE_CHATROOM", "DELETE_MULTIPLE_CHATROOMS", "DELETE_ALL_CHATROOMS", "DELETE_POST", "DELETE_STORY", "DELETE_GROUP", "SUSPEND_USER", "BAN_USER", "RESTORE_USER", "RESOLVE_REPORT", "CLEANUP_FAKE_CONTENT"
    val targetType: String = "", // "chatroom", "post", "story", "group", "user", "report", "system"
    val targetId: String = "",
    val targetLabel: String = "",
    val details: String = "",
    val timestampMillis: Long = System.currentTimeMillis(),
    @ServerTimestamp val createdAt: Timestamp? = null
) {
    fun formattedTimestamp(): String {
        val date = createdAt?.toDate() ?: java.util.Date(timestampMillis)
        return java.text.SimpleDateFormat("MMM dd, yyyy • hh:mm:ss a", java.util.Locale.US).format(date)
    }
}

@IgnoreExtraProperties
data class UserModerationDoc(
    @DocumentId val id: String = "",
    val appScope: String = "noise_live_public",
    val targetUserId: String = "",
    val targetUsername: String = "",
    val accountStatus: String = "active", // "active", "suspended", "banned"
    val reason: String = "",
    val updatedByUid: String = "",
    val updatedByUsername: String = "sebin_mr",
    val timestampMillis: Long = System.currentTimeMillis(),
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class ContentReportDoc(
    @DocumentId val id: String = "",
    val appScope: String = "noise_live_public",
    val reporterUid: String = "",
    val reporterUsername: String = "",
    val targetType: String = "post", // "post", "story", "chatroom", "group", "user"
    val targetId: String = "",
    val targetOwnerUid: String = "",
    val targetSummary: String = "",
    val reason: String = "",
    val status: String = "open", // "open", "resolved", "dismissed"
    val resolutionNotes: String = "",
    val resolvedByUid: String = "",
    val timestampMillis: Long = System.currentTimeMillis(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)


