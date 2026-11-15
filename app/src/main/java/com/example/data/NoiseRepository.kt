package com.example.data

import android.content.Context
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class NoiseRepository(private val db: FirebaseFirestore) {

    companion object {
        fun canonicalConversationId(uidA: String, uidB: String): String {
            val sorted = listOf(uidA.trim(), uidB.trim()).sorted()
            return "conv_${sorted[0]}_${sorted[1]}"
        }

        fun resolveFirestoreInstance(context: Context): FirebaseFirestore {
            val dbId = runCatching {
                context.applicationContext.getString(R.string.firestore_database_id).trim()
            }.getOrDefault("")
            return if (dbId.isBlank() || dbId == "(default)") {
                FirebaseFirestore.getInstance()
            } else {
                FirebaseFirestore.getInstance(dbId)
            }
        }
    }

    constructor(context: Context) : this(
        resolveFirestoreInstance(context)
    ) {
        CloudMediaStorageManager.initialize(context, db)
    }

    init {
        CloudMediaStorageManager.activeFirestore = db
    }

    private val auth = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    private fun nowTimeLabel(): String {
        return SimpleDateFormat("hh:mm a", Locale.US).format(Date())
    }

    fun observeUserProfile(userId: String): Flow<UserProfile?> {
        val path = "users/$userId"
        return db.collection("users").document(userId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    ?.let { profile ->
                        val resolvedUid = snapshot.id.ifBlank { profile.userId.ifBlank { userId } }
                        profile.copy(id = resolvedUid, userId = resolvedUid)
                    }
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.GET, path)
                throw error
            }
    }

    fun observeRegisteredAccounts(): Flow<List<UserProfile>> {
        val path = "users"
        return db.collection("users")
            .whereEqualTo("appScope", "noise_live_public")
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    val profile = doc.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        ?: return@mapNotNull null
                    val authoritativeUid = doc.id.ifBlank { profile.userId }
                    if (authoritativeUid.isBlank() ||
                        authoritativeUid.startsWith("acct_nora_") ||
                        authoritativeUid.startsWith("acct_creator_") ||
                        authoritativeUid.startsWith("acct_maya_")
                    ) {
                        null
                    } else {
                        profile.copy(id = authoritativeUid, userId = authoritativeUid)
                    }
                }.distinctBy { it.userId }
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                throw error
            }
    }

    private fun isValidDisplayableMediaRecord(
        mediaKey: String,
        mediaUrl: String,
        cloudMediaId: String,
        uploadStatus: String
    ): Boolean {
        if (uploadStatus.isNotBlank() && uploadStatus != "Published") return false
        val effective = mediaUrl.ifBlank { mediaKey }.trim()
        if (effective.isBlank()) return false
        if (CloudMediaStorageManager.isPresetMediaKey(effective)) return true
        if (CloudMediaStorageManager.isPermanentCloudUrl(effective) || cloudMediaId.isNotBlank()) return true
        if (effective.startsWith("file://")) {
            val path = android.net.Uri.parse(effective).path
            return !path.isNullOrBlank() && java.io.File(path).let { it.exists() && it.length() > 0L }
        }
        return true
    }

    fun observePosts(): Flow<List<Post>> {
        val path = "posts"
        return db.collection("posts")
            .whereEqualTo("appScope", "noise_live_public")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(Post::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .filter {
                        isValidDisplayableMediaRecord(
                            mediaKey = it.mediaKey,
                            mediaUrl = it.mediaUrl,
                            cloudMediaId = it.cloudMediaId,
                            uploadStatus = it.uploadStatus
                        )
                    }
                    .map { post ->
                        val resolvedUrl = post.mediaUrl.ifBlank { post.mediaKey }
                        post.copy(
                            postId = post.postId.ifBlank { post.id },
                            mediaKey = resolvedUrl,
                            mediaUrl = resolvedUrl
                        )
                    }
                    .distinctBy { it.id.ifBlank { "${it.userId}_${it.mediaKey}_${it.caption}" } }
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                throw error
            }
    }

    fun observeComments(postId: String): Flow<List<Comment>> {
        val path = "posts/$postId/comments"
        return db.collection("posts").document(postId).collection("comments")
            .whereEqualTo("appScope", "noise_live_public")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(Comment::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedBy { it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                throw error
            }
    }

    fun observeStories(): Flow<List<Story>> {
        val path = "stories"
        return db.collection("stories")
            .whereEqualTo("appScope", "noise_live_public")
            .snapshots()
            .map { snapshot ->
                val now = System.currentTimeMillis()
                snapshot.toObjects(Story::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .filter { story ->
                        val notExpired = story.expiresAtMillis <= 0L || story.expiresAtMillis > now
                        notExpired && isValidDisplayableMediaRecord(
                            mediaKey = story.mediaKey,
                            mediaUrl = story.mediaUrl,
                            cloudMediaId = story.cloudMediaId,
                            uploadStatus = story.uploadStatus
                        )
                    }
                    .map { story ->
                        val resolvedUrl = story.mediaUrl.ifBlank { story.mediaKey }
                        story.copy(
                            storyId = story.storyId.ifBlank { story.id },
                            mediaKey = resolvedUrl,
                            mediaUrl = resolvedUrl
                        )
                    }
                    .distinctBy { "${it.userId}_${it.mediaKey}" }
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                throw error
            }
    }

    fun observePartyRooms(): Flow<List<PartyRoom>> {
        val path = "party_rooms"
        return db.collection("party_rooms")
            .whereEqualTo("appScope", "noise_live_public")
            .snapshots()
            .map { snapshot ->
                val seenHostAndTitle = mutableSetOf<String>()
                snapshot.documents.mapNotNull { doc ->
                    val room = doc.toObject(PartyRoom::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        ?: return@mapNotNull null
                    val resolvedId = doc.id.ifBlank { room.id.ifBlank { room.roomId } }
                    val resolvedHost = room.resolvedHostUid()
                    val resolvedName = room.resolvedRoomName()
                    val resolvedTheme = room.resolvedCoverThemeKey()
                    room.copy(
                        id = resolvedId,
                        roomId = resolvedId,
                        hostId = resolvedHost,
                        hostUid = resolvedHost,
                        ownerUid = resolvedHost,
                        roomName = resolvedName,
                        title = resolvedName,
                        themeKey = resolvedTheme,
                        roomCover = resolvedTheme,
                        stageTheme = resolvedTheme,
                        roomImageUrl = room.roomImageUrl.ifBlank { resolvedTheme }
                    )
                }
                    .sortedByDescending {
                        it.createdAt?.toDate()?.time
                            ?: it.createdTimestamp.takeIf { ts -> ts > 0L }
                            ?: 0L
                    }
                    .filter { room ->
                        val hostKey = room.resolvedHostUid()
                        val dedupKey = "${hostKey}_${room.resolvedRoomName().lowercase()}"
                        val isRoomActive = (room.isActive || room.active) && !room.roomStatus.equals("closed", ignoreCase = true)
                        room.live &&
                            isRoomActive &&
                            !room.isDummyOrSampleRoom() &&
                            !SuperOwnerControlManager.isRoomDeletedTombstone(room.id) &&
                            room.id.isNotBlank() &&
                            hostKey.isNotBlank() &&
                            room.hostUsername.isNotBlank() &&
                            !hostKey.startsWith("acct_nora_") &&
                            !hostKey.startsWith("acct_creator_") &&
                            !hostKey.startsWith("acct_maya_") &&
                            seenHostAndTitle.add(dedupKey)
                    }
                    .distinctBy { it.id }
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                throw error
            }
    }

    fun observeSinglePartyRoom(roomId: String): Flow<PartyRoom?> {
        val path = "party_rooms/$roomId"
        return db.collection("party_rooms").document(roomId)
            .snapshots()
            .map { snapshot ->
                if (SuperOwnerControlManager.isRoomDeletedTombstone(roomId)) {
                    null
                } else {
                    snapshot.toObject(PartyRoom::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        ?.let { room ->
                            val resolvedId = snapshot.id.ifBlank { room.id.ifBlank { roomId } }
                            val resolvedHost = room.resolvedHostUid()
                            val resolvedName = room.resolvedRoomName()
                            val resolvedTheme = room.resolvedCoverThemeKey()
                            room.copy(
                                id = resolvedId,
                                roomId = resolvedId,
                                hostId = resolvedHost,
                                hostUid = resolvedHost,
                                ownerUid = resolvedHost,
                                roomName = resolvedName,
                                title = resolvedName,
                                themeKey = resolvedTheme,
                                roomCover = resolvedTheme,
                                stageTheme = resolvedTheme,
                                roomImageUrl = room.roomImageUrl.ifBlank { resolvedTheme }
                            )
                        }
                        ?.takeIf { !it.isDummyOrSampleRoom() && !SuperOwnerControlManager.isRoomDeletedTombstone(it.id.ifBlank { roomId }) }
                }
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.GET, path)
                throw error
            }
    }

    fun observeNotifications(userId: String): Flow<List<NotificationItem>> {
        val path = "notifications"
        return db.collection("notifications")
            .whereEqualTo("userId", userId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(NotificationItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .filter { notif ->
                        !notif.id.startsWith("notif_1_") &&
                            !notif.id.startsWith("notif_2_") &&
                            !notif.id.startsWith("notif_3_") &&
                            !notif.id.startsWith("notif_4_") &&
                            !notif.id.startsWith("notif_5_") &&
                            !notif.id.startsWith("notif_6_")
                    }
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                throw error
            }
    }

    fun observeConversations(userId: String): Flow<List<Conversation>> {
        val path = "conversations"
        return db.collection("conversations")
            .whereArrayContains("participantIds", userId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(Conversation::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .filter {
                        it.id != "conv_nora_$userId" &&
                            it.id != "conv_creator_$userId" &&
                            it.id != "conv_sebin_$userId" &&
                            !it.id.startsWith("conv_nora_") &&
                            !it.id.startsWith("conv_creator_")
                    }
                    .distinctBy { it.id }
                    .map { conv ->
                        val resolved = conv.resolveForCurrentUser(userId)
                        val peerActuallyTyping = if (resolved.typingUserIds.isNotEmpty()) {
                            resolved.typingUserIds.any { typerUid -> typerUid != userId }
                        } else {
                            resolved.peerTyping
                        }
                        resolved.copy(peerTyping = peerActuallyTyping)
                    }
                    .sortedByDescending { it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                throw error
            }
    }

    fun observeActiveCalls(userId: String): Flow<List<CallSessionDoc>> {
        val path = "calls"
        return db.collection("calls")
            .whereArrayContains("participantIds", userId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(CallSessionDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                throw error
            }
    }

    fun observeMessages(conversationId: String, userId: String): Flow<List<DirectMessage>> {
        val path = "conversations/$conversationId/messages"
        return db.collection("conversations").document(conversationId).collection("messages")
            .whereArrayContains("participantIds", userId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(DirectMessage::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .filter { !it.id.startsWith("msg_init_") }
                    .sortedBy { it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                throw error
            }
    }

    fun observeCoinTransactions(userId: String): Flow<List<CoinTransaction>> {
        val path = "coin_transactions"
        return db.collection("coin_transactions")
            .whereEqualTo("userId", userId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(CoinTransaction::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                throw error
            }
    }

    suspend fun cleanupDemoAndDuplicateData(uid: String) {
        try {
            // Only clean up legacy demo rooms that actually exist and are owned by uid
            try {
                val myRoomsSnap = db.collection("party_rooms")
                    .whereEqualTo("appScope", "noise_live_public")
                    .get()
                    .await()
                for (rDoc in myRoomsSnap.documents) {
                    val room = rDoc.toObject(PartyRoom::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) ?: continue
                    val isLegacySeed = rDoc.id.startsWith("room_open_stage_") ||
                        rDoc.id.startsWith("dummy_") ||
                        rDoc.id.startsWith("sample_") ||
                        (room.seat1Name == "Rehan" && room.seat2Name == "Soniya")
                    if (room.resolvedHostUid() == uid && isLegacySeed) {
                        runCatching { rDoc.reference.delete().await() }
                    }
                }
            } catch (_: Exception) {
            }
            // 4. Clean up any duplicate stories owned by uid
            val storiesSnap = db.collection("stories")
                .whereEqualTo("appScope", "noise_live_public")
                .get()
                .await()
            val seenStoryKeys = mutableSetOf<String>()
            for (doc in storiesSnap.documents) {
                val story = doc.toObject(Story::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) ?: continue
                if (story.userId == uid) {
                    val key = "${story.userId}_${story.mediaKey}"
                    if (!seenStoryKeys.add(key)) {
                        try {
                            doc.reference.delete().await()
                        } catch (_: Exception) {
                        }
                    }
                }
            }
            // 5. Remove legacy seeded demo post and any auto-generated dummy stage rooms owned by uid
            try {
                db.collection("posts").document("post_roses_$uid").delete().await()
            } catch (_: Exception) {
            }
            try {
                SuperOwnerControlManager.purgeDemoBuggedAndDuplicateChatrooms(db, logAudit = false)
            } catch (_: Exception) {
            }
            // 6. Recover and migrate any local-only file:// or content:// posts and stories to permanent Firebase Storage
            recoverAndMigrateLocalMedia(uid)
        } catch (_: Exception) {
            // Best-effort cleanup
        }
    }

    /**
     * Inspects existing posts and stories owned by [uid].
     * - If a post/story has a local `file://` or `content://` reference that still exists on the device,
     *   uploads it to permanent Firebase Storage (`users/{uid}/posts/{postId}/original` or `users/{uid}/stories/{storyId}/original`)
     *   and updates the Firestore document with the permanent `mediaUrl`, `storagePath`, `thumbnailUrl`, and `cloudMediaId`.
     * - If a legacy local `file://` reference points to a deleted local file and has no cloud backup,
     *   removes the broken record so it never renders a black screen or infinite reload button.
     */
    suspend fun recoverAndMigrateLocalMedia(uid: String) {
        try {
            val postsSnap = db.collection("posts")
                .whereEqualTo("appScope", "noise_live_public")
                .get()
                .await()
            for (doc in postsSnap.documents) {
                val post = doc.toObject(Post::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) ?: continue
                if (post.userId != uid) continue
                val currentSource = post.mediaUrl.ifBlank { post.mediaKey }
                if (CloudMediaStorageManager.isTemporaryLocalReference(currentSource) && post.cloudMediaId.isBlank()) {
                    val localExists = if (currentSource.startsWith("file://")) {
                        val p = android.net.Uri.parse(currentSource).path
                        !p.isNullOrBlank() && java.io.File(p).let { it.exists() && it.length() > 0L }
                    } else {
                        false
                    }
                    if (localExists && post.createdAt != null) {
                        CloudMediaStorageManager.uploadPermanentMedia(
                            db = db,
                            uid = uid,
                            targetCategory = "post",
                            targetDocId = doc.id,
                            sourceKeyOrUri = currentSource,
                            mediaType = post.mediaType
                        ).onSuccess { uploaded ->
                            runCatching {
                                doc.reference.set(
                                    post.copy(
                                        postId = doc.id,
                                        mediaKey = uploaded.mediaUrl,
                                        mediaUrl = uploaded.mediaUrl,
                                        storagePath = uploaded.storagePath,
                                        thumbnailUrl = uploaded.thumbnailUrl,
                                        cloudMediaId = uploaded.cloudMediaId,
                                        uploadStatus = "Published",
                                        updatedAt = null
                                    )
                                ).await()
                            }
                        }
                    }
                    // Never delete existing user posts during repairs
                }
            }

            val storiesSnap = db.collection("stories")
                .whereEqualTo("appScope", "noise_live_public")
                .get()
                .await()
            for (doc in storiesSnap.documents) {
                val story = doc.toObject(Story::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) ?: continue
                if (story.userId != uid) continue
                val currentSource = story.mediaUrl.ifBlank { story.mediaKey }
                if (CloudMediaStorageManager.isTemporaryLocalReference(currentSource) && story.cloudMediaId.isBlank()) {
                    val localExists = if (currentSource.startsWith("file://")) {
                        val p = android.net.Uri.parse(currentSource).path
                        !p.isNullOrBlank() && java.io.File(p).let { it.exists() && it.length() > 0L }
                    } else {
                        false
                    }
                    if (localExists && story.createdAt != null) {
                        CloudMediaStorageManager.uploadPermanentMedia(
                            db = db,
                            uid = uid,
                            targetCategory = "story",
                            targetDocId = doc.id,
                            sourceKeyOrUri = currentSource,
                            mediaType = story.mediaType
                        ).onSuccess { uploaded ->
                            val resolvedDuration = if (story.mediaType == "video" && uploaded.durationSec > 0L) {
                                uploaded.durationSec.coerceIn(1L, 3600L)
                            } else {
                                story.durationSec
                            }
                            runCatching {
                                doc.reference.set(
                                    story.copy(
                                        storyId = doc.id,
                                        mediaKey = uploaded.mediaUrl,
                                        mediaUrl = uploaded.mediaUrl,
                                        storagePath = uploaded.storagePath,
                                        thumbnailUrl = uploaded.thumbnailUrl,
                                        cloudMediaId = uploaded.cloudMediaId,
                                        durationSec = resolvedDuration,
                                        uploadStatus = "Published"
                                    )
                                ).await()
                            }
                        }
                    }
                    // Never delete existing user stories during repairs
                }
            }
        } catch (_: Exception) {
        }
    }

    suspend fun ensureUserInitialized(): Result<UserProfile> = runCatching {
        val uid = requireUserId()
        val userRef = db.collection("users").document(uid)
        try {
            cleanupDemoAndDuplicateData(uid)

            val existingSnap = userRef.get().await()
            if (existingSnap.exists()) {
                val profile = existingSnap.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                if (profile != null) {
                    val authEmail = auth.currentUser?.email
                    val isVerifiedSuperOwner = SuperOwnerControlManager.isVerifiedSuperOwner(
                        uid = uid,
                        email = authEmail,
                        userProfile = profile
                    )
                    if (isVerifiedSuperOwner) {
                        SuperOwnerControlManager.ensureSuperOwnerRegistryBound(db, profile)
                    }
                    // Prevent any non-Super-Owner account from holding the reserved "sebin_mr" username
                    val safeUsername = if (!isVerifiedSuperOwner && SuperOwnerControlManager.isReservedSuperOwnerUsername(profile.username)) {
                        "user_${uid.takeLast(6).lowercase()}"
                    } else if (isVerifiedSuperOwner) {
                        SuperOwnerControlManager.PERMANENT_SUPER_OWNER_USERNAME
                    } else {
                        profile.username.trim().ifBlank { "user_${uid.takeLast(6).lowercase()}" }
                    }
                    val normalizedUsernameLower = safeUsername.lowercase()
                    val normalizedDisplayLower = profile.displayName.trim().lowercase()
                    // Reconcile actual post count and follow counts if legacy seeded 13/7/1 counts were stored
                    val actualPostsCount = try {
                        db.collection("posts")
                            .whereEqualTo("appScope", "noise_live_public")
                            .get()
                            .await()
                            .documents
                            .count { it.getString("userId") == uid }
                            .toLong()
                    } catch (_: Exception) {
                        profile.postsCount
                    }
                    val actualFollowingCount = profile.followedUserIds.distinct().size.toLong()
                    val needsCountFix = (profile.followersCount == 13L && profile.followingCount == 7L && actualFollowingCount == 0L) ||
                        profile.postsCount != actualPostsCount

                    val reconciledFollowers = if (profile.followersCount == 13L && profile.followingCount == 7L && actualFollowingCount == 0L) {
                        0L
                    } else {
                        profile.followersCount
                    }
                    val reconciledFollowing = if (profile.followersCount == 13L && profile.followingCount == 7L && actualFollowingCount == 0L) {
                        0L
                    } else {
                        actualFollowingCount.coerceAtLeast(profile.followingCount)
                    }

                    try {
                        db.collection("presence").document(uid).set(
                            UserPresence(
                                userId = uid,
                                state = "online",
                                lastSeenLabel = "Active now",
                                lastSeenMillis = System.currentTimeMillis()
                            )
                        ).await()
                    } catch (_: Exception) {
                    }

                    if (!profile.online ||
                        profile.username != safeUsername ||
                        profile.usernameLower != normalizedUsernameLower ||
                        profile.displayNameLower != normalizedDisplayLower ||
                        needsCountFix
                    ) {
                        val refreshed = profile.copy(
                            username = safeUsername,
                            online = true,
                            lastActiveLabel = "Active now",
                            lastSeenMillis = System.currentTimeMillis(),
                            usernameLower = normalizedUsernameLower,
                            displayNameLower = normalizedDisplayLower,
                            postsCount = actualPostsCount,
                            followersCount = reconciledFollowers,
                            followingCount = reconciledFollowing,
                            updatedAt = null
                        )
                        if (profile.createdAt != null) {
                            userRef.set(refreshed).await()
                        }
                        return@runCatching refreshed.copy(createdAt = profile.createdAt)
                    }
                    return@runCatching profile
                }
            }

            val authUser = auth.currentUser
            val isVerifiedOwner = SuperOwnerControlManager.isVerifiedSuperOwner(
                uid = uid,
                email = authUser?.email,
                userProfile = null
            )
            val derivedDisplayName = authUser?.displayName?.takeIf { it.isNotBlank() }
                ?: if (isVerifiedOwner) "Sebin Mr" else "Member ${uid.takeLast(4).uppercase()}"
            val rawUsername = authUser?.email?.substringBefore("@")
                ?.replace(Regex("[^a-zA-Z0-9_]"), "_")
                ?.takeIf { it.length >= 2 }
                ?: if (isVerifiedOwner) SuperOwnerControlManager.PERMANENT_SUPER_OWNER_USERNAME else "user_${uid.takeLast(6).lowercase()}"
            val candidateBase = when {
                isVerifiedOwner -> SuperOwnerControlManager.PERMANENT_SUPER_OWNER_USERNAME
                SuperOwnerControlManager.isReservedSuperOwnerUsername(rawUsername) -> "user_${uid.takeLast(6).lowercase()}"
                else -> rawUsername
            }

            // Ensure unique case-insensitive username across registered public accounts
            val existingUsersSnap = try {
                db.collection("users")
                    .whereEqualTo("appScope", "noise_live_public")
                    .get()
                    .await()
            } catch (_: Exception) {
                null
            }
            val takenUsernames = existingUsersSnap?.documents?.mapNotNull { doc ->
                if (doc.id == uid) null else doc.getString("username")?.trim()?.lowercase()
            }?.toSet() ?: emptySet()

            val uniqueUsername = if (isVerifiedOwner) {
                SuperOwnerControlManager.PERMANENT_SUPER_OWNER_USERNAME
            } else if (candidateBase.lowercase() !in takenUsernames) {
                candidateBase.take(36)
            } else {
                "${candidateBase.take(28)}_${uid.takeLast(4).lowercase()}"
            }

            if (isVerifiedOwner) {
                SuperOwnerControlManager.ensureSuperOwnerRegistryBound(db, null)
            }

            // New account default values: 0 followers, 0 following, 0 posts
            val initialProfile = UserProfile(
                id = uid,
                userId = uid,
                appScope = "noise_live_public",
                username = uniqueUsername,
                usernameLower = uniqueUsername.lowercase(),
                displayName = derivedDisplayName.take(60),
                displayNameLower = derivedDisplayName.take(60).lowercase(),
                bio = "Digital Creator ✨\nNoise App Official 🚀",
                website = "noise.app",
                avatarKey = (authUser?.photoUrl?.toString()?.takeIf { it.isNotBlank() } ?: uniqueUsername.take(1).uppercase()),
                profilePictureUrl = authUser?.photoUrl?.toString() ?: "",
                accountPrivacy = "public",
                coins = 40150L,
                vip = true,
                verified = true,
                online = true,
                lastActiveLabel = "Active now",
                lastSeenMillis = System.currentTimeMillis(),
                postsCount = 0L,
                followersCount = 0L,
                followingCount = 0L,
                followedUserIds = emptyList(),
                createdAt = null,
                updatedAt = null
            )
            userRef.set(initialProfile).await()

            try {
                db.collection("presence").document(uid).set(
                    UserPresence(
                        userId = uid,
                        state = "online",
                        lastSeenLabel = "Active now",
                        lastSeenMillis = System.currentTimeMillis()
                    )
                ).await()
            } catch (_: Exception) {
            }

            // Store private email and notification token in /user_private/{uid}
            val email = authUser?.email?.trim().orEmpty()
            if (email.isNotBlank()) {
                try {
                    db.collection("user_private").document(uid).set(
                        mapOf(
                            "userId" to uid,
                            "email" to email.take(160)
                        )
                    ).await()
                } catch (_: Exception) {
                }
            }

            val savedSnap = userRef.get().await()
            savedSnap.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                ?.copy(id = uid, userId = uid)
                ?: initialProfile
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, userRef.path)
            throw e
        }
    }

    suspend fun registerDeviceNotificationToken(tokenOverride: String = ""): Result<String> = runCatching {
        val uid = requireUserId()
        val resolvedToken = tokenOverride.trim().ifBlank {
            "fcm_noicelive_${uid.take(24)}_${android.os.Build.MODEL.replace(Regex("[^a-zA-Z0-9_]"), "_").take(20)}"
        }.take(450)
        val userRef = db.collection("users").document(uid)
        try {
            val snap = userRef.get().await()
            val existing = snap.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            if (existing?.createdAt != null && (existing.fcmToken != resolvedToken || !existing.notificationsEnabled)) {
                userRef.set(
                    existing.copy(
                        fcmToken = resolvedToken,
                        notificationsEnabled = true,
                        updatedAt = null
                    )
                ).await()
            }
            val email = auth.currentUser?.email?.trim().orEmpty()
            try {
                db.collection("user_private").document(uid).set(
                    mapOf(
                        "userId" to uid,
                        "email" to email.take(160),
                        "fcmToken" to resolvedToken,
                        "notificationsEnabled" to true
                    )
                ).await()
            } catch (_: Exception) {
            }
            resolvedToken
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, userRef.path)
            throw e
        }
    }

    suspend fun createOrUpdateUserProfile(profile: UserProfile): Result<String> = runCatching {
        val uid = requireUserId()
        val docId = profile.id.ifBlank { uid }
        val docRef = db.collection("users").document(docId)
        val isSuperOwner = SuperOwnerControlManager.verifyCallerIsSuperOwner(db) ||
            SuperOwnerControlManager.isVerifiedSuperOwner(uid = uid, email = auth.currentUser?.email, userProfile = profile)
        val requestedUsername = profile.username.trim().removePrefix("@").replace(Regex("[^a-zA-Z0-9_.]"), "_")
            .ifBlank { if (isSuperOwner) SuperOwnerControlManager.PERMANENT_SUPER_OWNER_USERNAME else "user_${uid.takeLast(6).lowercase()}" }
            .take(40)
        if (!isSuperOwner && SuperOwnerControlManager.isReservedSuperOwnerUsername(requestedUsername)) {
            throw IllegalArgumentException("Username @sebin_mr is permanently reserved for the verified Super Owner.")
        }
        val cleanUsername = if (isSuperOwner) SuperOwnerControlManager.PERMANENT_SUPER_OWNER_USERNAME else requestedUsername
        val cleanDisplayName = profile.displayName.trim().ifBlank { cleanUsername }.take(60)
        try {
            // Validate unique username (case-insensitive) against other public users
            val publicUsers = db.collection("users")
                .whereEqualTo("appScope", "noise_live_public")
                .get()
                .await()
            val duplicate = publicUsers.documents.any { doc ->
                doc.id != docId &&
                    (doc.getString("userId") ?: doc.id) != uid &&
                    doc.getString("username")?.trim()?.equals(cleanUsername, ignoreCase = true) == true
            }
            require(!duplicate) { "Username @$cleanUsername is already taken" }

            val existing = docRef.get().await()
            if (existing.exists()) {
                val current = existing.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                requireNotNull(current?.createdAt) { "Profile createdAt must not be null for update" }
                docRef.set(
                    profile.copy(
                        id = docId,
                        userId = uid,
                        username = cleanUsername,
                        usernameLower = cleanUsername.lowercase(),
                        displayName = cleanDisplayName,
                        displayNameLower = cleanDisplayName.lowercase(),
                        createdAt = current?.createdAt,
                        updatedAt = null
                    )
                ).await()
            } else {
                docRef.set(
                    profile.copy(
                        id = docId,
                        userId = uid,
                        username = cleanUsername,
                        usernameLower = cleanUsername.lowercase(),
                        displayName = cleanDisplayName,
                        displayNameLower = cleanDisplayName.lowercase(),
                        createdAt = null,
                        updatedAt = null
                    )
                ).await()
            }

            // Propagate updated username, displayName, and avatarKey to user's authored posts
            runCatching {
                val authoredPosts = db.collection("posts")
                    .whereEqualTo("appScope", "noise_live_public")
                    .get()
                    .await()
                authoredPosts.documents.forEach { postDoc ->
                    val post = postDoc.toObject(Post::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    if (post != null && post.userId == uid && post.createdAt != null) {
                        if (post.username != cleanUsername ||
                            post.displayName != cleanDisplayName ||
                            post.authorAvatarKey != profile.avatarKey
                        ) {
                            postDoc.reference.set(
                                post.copy(
                                    username = cleanUsername,
                                    displayName = cleanDisplayName,
                                    authorAvatarKey = profile.avatarKey,
                                    updatedAt = null
                                )
                            ).await()
                        }
                    }
                }
            }
            docId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }

    suspend fun searchUsers(query: String): Result<List<UserProfile>> = runCatching {
        requireUserId()
        val q = query.trim().removePrefix("@").lowercase()
        val snap = db.collection("users")
            .whereEqualTo("appScope", "noise_live_public")
            .get()
            .await()
        val allUsers = snap.toObjects(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            .filter { !it.id.startsWith("acct_nora_") && !it.id.startsWith("acct_creator_") && !it.id.startsWith("acct_maya_") }
            .distinctBy { it.userId.ifBlank { it.id } }
        if (q.isBlank()) {
            allUsers
        } else {
            allUsers.filter { user ->
                user.username.lowercase().contains(q) ||
                    user.displayName.lowercase().contains(q) ||
                    user.usernameLower.contains(q) ||
                    user.displayNameLower.contains(q)
            }
        }
    }

    suspend fun setUserOnlineStatus(online: Boolean): Result<Unit> = runCatching {
        val uid = auth.currentUser?.uid ?: return@runCatching
        val userRef = db.collection("users").document(uid)
        val nowMillis = System.currentTimeMillis()
        val label = if (online) "Active now" else "Last seen ${nowTimeLabel()}"
        try {
            try {
                db.collection("presence").document(uid).set(
                    UserPresence(
                        userId = uid,
                        state = if (online) "online" else "offline",
                        lastSeenLabel = label,
                        lastSeenMillis = nowMillis
                    )
                ).await()
            } catch (_: Exception) {
            }

            val snap = userRef.get().await()
            val existing = snap.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            if (existing?.createdAt != null) {
                userRef.set(
                    existing.copy(
                        online = online,
                        lastActiveLabel = label,
                        lastSeenMillis = nowMillis,
                        updatedAt = null
                    )
                ).await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // Non-fatal background presence update
        }
    }

    suspend fun claimVipCoins(currentProfile: UserProfile, tierName: String, amount: Long): Result<Long> = runCatching {
        val uid = requireUserId()
        require(amount > 0) { "Amount must be positive" }
        val newBalance = currentProfile.coins + amount
        val txId = "tx_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val txRef = db.collection("coin_transactions").document(txId)
        val userRef = db.collection("users").document(uid)
        try {
            val tx = CoinTransaction(
                id = txId,
                userId = uid,
                tierName = tierName,
                amount = amount,
                balanceAfter = newBalance,
                createdAt = null
            )
            txRef.set(tx).await()

            val snap = userRef.get().await()
            if (snap.exists()) {
                val existing = snap.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                if (existing?.createdAt != null) {
                    userRef.set(
                        existing.copy(
                            coins = newBalance,
                            updatedAt = null
                        )
                    ).await()
                }
            }
            newBalance
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, txRef.path)
            throw e
        }
    }

    suspend fun createPost(
        authorProfile: UserProfile,
        mediaKey: String,
        mediaType: String,
        caption: String,
        location: String,
        tag: String,
        vibeEmoji: String,
        existingPostId: String = "",
        onProgress: (Int, String) -> Unit = { _, _ -> }
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val normalizedMediaType = if (mediaType == "video") "video" else "photo"
        val cleanId = existingPostId.ifBlank {
            "post_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        }
        val postRef = db.collection("posts").document(cleanId)
        try {
            onProgress(2, "Pending")
            onProgress(10, "Uploading")

            val sourceMedia = mediaKey.ifBlank { "roses" }
            val uploaded = CloudMediaStorageManager.uploadPermanentMedia(
                db = db,
                uid = uid,
                targetCategory = "post",
                targetDocId = cleanId,
                sourceKeyOrUri = sourceMedia,
                mediaType = normalizedMediaType,
                onProgress = { pct ->
                    onProgress(pct.coerceIn(10, 90), "Uploading")
                }
            ).getOrThrow()

            require(uploaded.mediaUrl.isNotBlank()) { "Cloud media URL must not be blank" }
            require(!CloudMediaStorageManager.isTemporaryLocalReference(uploaded.mediaUrl)) {
                "Temporary local URI cannot be saved as permanent post mediaUrl"
            }

            onProgress(94, "Uploaded")

            val resolvedCaption = caption.ifBlank { "Midnight city lights 🌃 #Noise" }.take(500)
            val resolvedTag = tag.ifBlank { "#NoiseVibes" }.take(60)
            val extractedHashtags = Regex("#[A-Za-z0-9_\\u0D00-\\u0D7F]+")
                .findAll("$resolvedCaption $resolvedTag")
                .map { it.value }
                .distinct()
                .take(12)
                .toList()
                .ifEmpty { listOf(resolvedTag, "#KeralaTrending", "#MalayalamReels").distinct() }
            val hasMalayalam = resolvedCaption.any { it in '\u0D00'..'\u0D7F' }
            val resolvedReelCategory = when {
                resolvedCaption.contains("music", ignoreCase = true) || resolvedTag.contains("music", ignoreCase = true) -> "Malayalam Music"
                hasMalayalam || resolvedCaption.contains("kerala", ignoreCase = true) || location.contains("kerala", ignoreCase = true) -> "Kerala Trending"
                else -> "Trending"
            }

            val draftPost = Post(
                id = cleanId,
                postId = cleanId,
                userId = uid,
                ownerUid = uid,
                appScope = "noise_live_public",
                username = authorProfile.username,
                displayName = authorProfile.displayName,
                authorAvatarKey = authorProfile.avatarKey.take(1200),
                profilePhoto = authorProfile.profilePictureUrl.ifBlank { authorProfile.avatarKey }.take(1200),
                authorVipBadgeLevel = authorProfile.activeVipBadgeLevel,
                mediaKey = uploaded.mediaUrl.take(1200),
                mediaUrl = uploaded.mediaUrl.take(1200),
                storagePath = uploaded.storagePath.take(600),
                thumbnailUrl = uploaded.thumbnailUrl.take(340_000),
                cloudMediaId = uploaded.cloudMediaId.take(128),
                mediaType = normalizedMediaType,
                caption = resolvedCaption,
                location = location.ifBlank { "Kochi, Kerala" }.take(120),
                tag = resolvedTag,
                vibeEmoji = vibeEmoji.ifBlank { "✨" }.take(16),
                likesCount = 1L,
                commentsCount = 0L,
                viewsCount = 1L,
                sharesCount = 0L,
                savesCount = 0L,
                likedByUids = listOf(uid),
                viewedByUids = listOf(uid),
                savedByUids = emptyList(),
                hashtags = extractedHashtags,
                reelCategory = resolvedReelCategory,
                musicTrackTitle = if (normalizedMediaType == "video") "Original Audio • @${authorProfile.username}" else "",
                language = if (hasMalayalam) "ml" else "en",
                regionTag = "Kerala",
                uploadStatus = "Published",
                visibility = "public",
                createdAt = null,
                updatedAt = null
            )
            val post = draftPost.copy(
                trendingScore = draftPost.computeReelTrendingScore(System.currentTimeMillis())
            )
            postRef.set(post).await()

            val userRef = db.collection("users").document(uid)
            val snap = userRef.get().await()
            val existingUser = snap.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            if (existingUser?.createdAt != null) {
                userRef.set(
                    existingUser.copy(
                        postsCount = existingUser.postsCount + 1L,
                        updatedAt = null
                    )
                ).await()
            }
            onProgress(100, "Published")
            cleanId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, postRef.path)
            throw e
        }
    }

    suspend fun toggleLikePost(post: Post): Result<Unit> = runCatching {
        val uid = requireUserId()
        require(post.id.isNotBlank()) { "Post ID required" }
        val postRef = db.collection("posts").document(post.id)
        try {
            val snap = postRef.get().await()
            val current = snap.toObject(Post::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                ?: error("Post not found")
            requireNotNull(current.createdAt) { "Post createdAt required" }
            val alreadyLiked = uid in current.likedByUids
            val updatedUids = if (alreadyLiked) {
                current.likedByUids - uid
            } else {
                (current.likedByUids + uid).take(50)
            }
            val newLikes = if (alreadyLiked) {
                (current.likesCount - 1L).coerceAtLeast(0L)
            } else {
                current.likesCount + 1L
            }
            val updatedDraft = current.copy(
                likesCount = newLikes,
                likedByUids = updatedUids,
                updatedAt = null
            )
            postRef.set(
                updatedDraft.copy(
                    trendingScore = updatedDraft.computeReelTrendingScore(System.currentTimeMillis())
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, postRef.path)
            throw e
        }
    }

    suspend fun recordReelView(post: Post): Result<Unit> = runCatching {
        val uid = auth.currentUser?.uid ?: return@runCatching
        if (post.id.isBlank()) return@runCatching
        val postRef = db.collection("posts").document(post.id)
        try {
            val snap = postRef.get().await()
            val current = snap.toObject(Post::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) ?: return@runCatching
            if (current.createdAt == null) return@runCatching
            val updatedViewedUids = if (uid in current.viewedByUids) {
                current.viewedByUids
            } else {
                (current.viewedByUids + uid).take(50)
            }
            val nextViews = current.viewsCount + 1L
            val updatedDraft = current.copy(
                viewsCount = nextViews,
                viewedByUids = updatedViewedUids,
                updatedAt = null
            )
            postRef.set(
                updatedDraft.copy(
                    trendingScore = updatedDraft.computeReelTrendingScore(System.currentTimeMillis())
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
        }
    }

    suspend fun recordReelShare(post: Post): Result<Unit> = runCatching {
        val uid = auth.currentUser?.uid ?: return@runCatching
        if (post.id.isBlank()) return@runCatching
        val postRef = db.collection("posts").document(post.id)
        try {
            val snap = postRef.get().await()
            val current = snap.toObject(Post::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) ?: return@runCatching
            if (current.createdAt == null) return@runCatching
            val updatedDraft = current.copy(
                sharesCount = current.sharesCount + 1L,
                updatedAt = null
            )
            postRef.set(
                updatedDraft.copy(
                    trendingScore = updatedDraft.computeReelTrendingScore(System.currentTimeMillis())
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
        }
    }

    suspend fun toggleSavePostInCloud(post: Post): Result<Boolean> = runCatching {
        val uid = requireUserId()
        require(post.id.isNotBlank()) { "Post ID required" }
        val postRef = db.collection("posts").document(post.id)
        val snap = postRef.get().await()
        val current = snap.toObject(Post::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            ?: return@runCatching false
        if (current.createdAt == null) return@runCatching false
        val alreadySaved = uid in current.savedByUids
        val nextSavedUids = if (alreadySaved) {
            current.savedByUids - uid
        } else {
            (current.savedByUids + uid).take(50)
        }
        val nextSavesCount = if (alreadySaved) {
            (current.savesCount - 1L).coerceAtLeast(0L)
        } else {
            current.savesCount + 1L
        }
        val updatedDraft = current.copy(
            savesCount = nextSavesCount,
            savedByUids = nextSavedUids,
            updatedAt = null
        )
        postRef.set(
            updatedDraft.copy(
                trendingScore = updatedDraft.computeReelTrendingScore(System.currentTimeMillis())
            )
        ).await()
        !alreadySaved
    }

    suspend fun refreshTrendingReelsRankings(algorithmMode: String = "engagement_velocity"): Result<Int> = runCatching {
        val uid = auth.currentUser?.uid ?: return@runCatching 0
        val snap = db.collection("posts")
            .whereEqualTo("appScope", "noise_live_public")
            .get()
            .await()
        val nowMs = System.currentTimeMillis()
        var updatedCount = 0
        for (doc in snap.documents) {
            val post = doc.toObject(Post::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)?.copy(id = doc.id)
                ?: continue
            if (post.mediaType != "video" || post.createdAt == null) continue
            if (post.userId == uid) {
                val freshScore = post.computeReelTrendingScore(nowMs, algorithmMode)
                doc.reference.set(
                    post.copy(
                        trendingScore = freshScore,
                        updatedAt = null
                    )
                ).await()
                updatedCount++
            }
        }
        updatedCount
    }

    suspend fun deletePost(postId: String): Result<Unit> = runCatching {
        val uid = requireUserId()
        val postRef = db.collection("posts").document(postId)
        try {
            val existingSnap = postRef.get().await()
            val existingPost = existingSnap.toObject(Post::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            val isSuperOwner = SuperOwnerControlManager.verifyCallerIsSuperOwner(db)
            if (existingPost != null && existingPost.userId != uid && !isSuperOwner) {
                throw IllegalStateException("You can only delete your own posts")
            }
            postRef.delete().await()
            if (existingPost != null) {
                CloudMediaStorageManager.deletePermanentMedia(
                    db = db,
                    storagePath = existingPost.storagePath,
                    cloudMediaId = existingPost.cloudMediaId.ifBlank {
                        CloudMediaStorageManager.extractCloudMediaId(existingPost.mediaUrl)
                    }
                )
            }
            val authorUid = existingPost?.userId?.ifBlank { uid } ?: uid
            val userRef = db.collection("users").document(authorUid)
            val snap = userRef.get().await()
            val existingUser = snap.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            if (existingUser?.createdAt != null) {
                userRef.set(
                    existingUser.copy(
                        postsCount = (existingUser.postsCount - 1L).coerceAtLeast(0L),
                        updatedAt = null
                    )
                ).await()
            }
            if (isSuperOwner && existingPost != null) {
                SuperOwnerControlManager.logOwnerAuditAction(
                    db = db,
                    actionType = "DELETE_POST",
                    targetType = "post",
                    targetId = postId,
                    targetLabel = "Post by @${existingPost.username}",
                    details = "Super Owner deleted post '$postId' by @${existingPost.username}: ${existingPost.caption.take(100)}"
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, postRef.path)
            throw e
        }
    }

    suspend fun addComment(post: Post, username: String, text: String): Result<String> = runCatching {
        val uid = requireUserId()
        require(post.id.isNotBlank()) { "Post ID required" }
        val commentId = "cmt_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val commentRef = db.collection("posts").document(post.id).collection("comments").document(commentId)
        try {
            val comment = Comment(
                id = commentId,
                postId = post.id,
                userId = uid,
                appScope = "noise_live_public",
                username = username.ifBlank { "sebin_mr" },
                text = text.trim().take(300),
                createdAt = null
            )
            commentRef.set(comment).await()

            val postRef = db.collection("posts").document(post.id)
            val snap = postRef.get().await()
            val currentPost = snap.toObject(Post::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            if (currentPost?.createdAt != null) {
                val updatedDraft = currentPost.copy(
                    commentsCount = currentPost.commentsCount + 1L,
                    updatedAt = null
                )
                postRef.set(
                    updatedDraft.copy(
                        trendingScore = updatedDraft.computeReelTrendingScore(System.currentTimeMillis())
                    )
                ).await()
            }
            commentId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, commentRef.path)
            throw e
        }
    }

    suspend fun createStory(
        username: String,
        mediaKey: String,
        caption: String,
        mediaType: String = "photo",
        durationSec: Long = 6L,
        authorAvatarKey: String = "",
        existingStoryId: String = "",
        onProgress: (Int, String) -> Unit = { _, _ -> }
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val cleanMediaKey = mediaKey.ifBlank { "roses" }.take(500)
        // Deterministic ID per user + mediaKey hash prevents duplicate stories on repeated upload
        val mediaHash = cleanMediaKey.hashCode().toUInt().toString(16)
        val storyId = existingStoryId.ifBlank { "story_${uid.take(12)}_$mediaHash" }
        val storyRef = db.collection("stories").document(storyId)
        try {
            val existingSnap = storyRef.get().await()
            if (existingSnap.exists()) {
                val existingStory = existingSnap.toObject(Story::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                if (existingStory != null &&
                    !CloudMediaStorageManager.isTemporaryLocalReference(existingStory.effectiveMediaSource())
                ) {
                    onProgress(100, "Published")
                    return@runCatching storyId
                }
            }

            onProgress(5, "Pending")
            onProgress(15, "Uploading")

            val normalizedMediaType = if (mediaType == "video") "video" else "photo"
            val uploaded = CloudMediaStorageManager.uploadPermanentMedia(
                db = db,
                uid = uid,
                targetCategory = "story",
                targetDocId = storyId,
                sourceKeyOrUri = cleanMediaKey,
                mediaType = normalizedMediaType,
                onProgress = { pct ->
                    onProgress(pct.coerceIn(15, 92), "Uploading")
                }
            ).getOrThrow()

            require(uploaded.mediaUrl.isNotBlank()) { "Cloud story mediaUrl must not be blank" }
            require(!CloudMediaStorageManager.isTemporaryLocalReference(uploaded.mediaUrl)) {
                "Temporary local URI cannot be saved as permanent story mediaUrl"
            }

            onProgress(96, "Uploaded")

            val cleanUser = username.ifBlank { "sebin_mr" }
            val resolvedAvatar = authorAvatarKey.ifBlank { cleanUser.take(1).uppercase() }.take(1200)
            val expiryMillis = System.currentTimeMillis() + 86_400_000L
            val actualDurationSec = if (normalizedMediaType == "video" && uploaded.durationSec > 0L) {
                uploaded.durationSec.coerceIn(1L, 3600L)
            } else {
                durationSec.coerceIn(1L, 3600L)
            }
            val story = Story(
                id = storyId,
                storyId = storyId,
                userId = uid,
                ownerUid = uid,
                appScope = "noise_live_public",
                username = cleanUser,
                authorAvatarKey = resolvedAvatar,
                mediaKey = uploaded.mediaUrl.take(1200),
                mediaUrl = uploaded.mediaUrl.take(1200),
                storagePath = uploaded.storagePath.take(600),
                thumbnailUrl = uploaded.thumbnailUrl.take(340_000),
                cloudMediaId = uploaded.cloudMediaId.take(128),
                mediaType = normalizedMediaType,
                durationSec = actualDurationSec,
                viewedByUids = emptyList(),
                viewers = emptyList(),
                caption = caption.take(200),
                expiresAtMillis = expiryMillis,
                expiresAt = expiryMillis,
                uploadStatus = "Published",
                createdAt = null
            )
            storyRef.set(story).await()
            onProgress(100, "Published")
            storyId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, storyRef.path)
            throw e
        }
    }

    suspend fun deleteStory(storyId: String): Result<Unit> = runCatching {
        val uid = requireUserId()
        require(storyId.isNotBlank()) { "Story ID required" }
        val storyRef = db.collection("stories").document(storyId)
        try {
            val snap = storyRef.get().await()
            val existing = snap.toObject(Story::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                ?: error("Story not found")
            val isSuperOwner = SuperOwnerControlManager.verifyCallerIsSuperOwner(db)
            require(existing.userId == uid || isSuperOwner) { "You can only delete your own uploaded stories" }
            storyRef.delete().await()
            CloudMediaStorageManager.deletePermanentMedia(
                db = db,
                storagePath = existing.storagePath,
                cloudMediaId = existing.cloudMediaId.ifBlank {
                    CloudMediaStorageManager.extractCloudMediaId(existing.mediaUrl)
                }
            )
            if (isSuperOwner) {
                SuperOwnerControlManager.logOwnerAuditAction(
                    db = db,
                    actionType = "DELETE_STORY",
                    targetType = "story",
                    targetId = storyId,
                    targetLabel = "Story by @${existing.username}",
                    details = "Super Owner deleted story '$storyId' uploaded by @${existing.username}"
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, storyRef.path)
            throw e
        }
    }

    suspend fun markStoryViewed(story: Story): Result<Unit> = runCatching {
        val uid = requireUserId()
        if (story.id.isBlank() || uid in story.viewedByUids) return@runCatching
        val storyRef = db.collection("stories").document(story.id)
        try {
            try {
                storyRef.collection("views").document(uid).set(
                    StoryViewDoc(
                        storyId = story.id,
                        viewerId = uid,
                        viewerUsername = auth.currentUser?.displayName?.take(40) ?: "",
                        viewedAt = null
                    )
                ).await()
            } catch (_: Exception) {
            }
            val snap = storyRef.get().await()
            val existing = snap.toObject(Story::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            if (existing?.createdAt != null && uid !in existing.viewedByUids) {
                storyRef.set(
                    existing.copy(
                        viewedByUids = (existing.viewedByUids + uid).distinct().take(100)
                    )
                ).await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, storyRef.path)
            throw e
        }
    }

    suspend fun createPartyRoom(hostUsername: String, title: String, subtitle: String): Result<String> = runCatching {
        val room = createFullPartyRoom(
            hostUsername = hostUsername,
            title = title,
            subtitle = subtitle,
            description = subtitle,
            category = "Party",
            musicTrack = "None",
            gameName = "PK Battle",
            micMode = "Free Mic",
            capacity = 12L,
            privateRoom = false,
            password = "",
            themeKey = "starry_mountain"
        ).getOrThrow()
        room.id
    }

    suspend fun createFullPartyRoom(
        hostUsername: String,
        title: String,
        subtitle: String,
        category: String,
        musicTrack: String,
        gameName: String,
        micMode: String,
        capacity: Long,
        privateRoom: Boolean,
        password: String,
        themeKey: String,
        description: String = subtitle,
        hostAvatarKey: String = ""
    ): Result<PartyRoom> = runCatching {
        val authUser = auth.currentUser
            ?: throw IllegalStateException("Authentication Required: Please sign in to your NOICE LIVE account before creating a voice chatroom.")
        val uid = authUser.uid.trim()
        require(uid.isNotBlank()) { "Authentication Required: Invalid Firebase Authentication UID." }

        val cleanTitle = title.trim()
        require(cleanTitle.isNotBlank()) { "Room Name is required" }
        val cleanDescription = description.trim().ifBlank { subtitle.trim().ifBlank { "Welcome to $cleanTitle!" } }
        if (privateRoom) {
            require(password.trim().isNotBlank()) { "Room Password is required for Private rooms" }
        }

        val roomId = "room_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val roomRef = db.collection("party_rooms").document(roomId)
        try {
            val isVerifiedOwner = SuperOwnerControlManager.isVerifiedSuperOwner(
                uid = uid,
                email = authUser.email,
                userProfile = null
            )
            val fallbackUsernameFromAuth = authUser.email?.substringBefore("@")
                ?.replace(Regex("[^a-zA-Z0-9_]"), "_")
                ?.takeIf { it.length >= 2 }
                ?: authUser.displayName?.trim()?.replace(Regex("[^a-zA-Z0-9_]"), "_")?.takeIf { it.length >= 2 }
                ?: "user_${uid.takeLast(6).lowercase()}"

            val resolvedHostUsername = when {
                isVerifiedOwner -> SuperOwnerControlManager.PERMANENT_SUPER_OWNER_USERNAME
                hostUsername.isNotBlank() && !SuperOwnerControlManager.isReservedSuperOwnerUsername(hostUsername) -> hostUsername.trim().take(40)
                else -> {
                    val userDocUsername = runCatching {
                        db.collection("users").document(uid).get().await().getString("username")?.trim().orEmpty()
                    }.getOrDefault("")
                    if (userDocUsername.isNotBlank() && !SuperOwnerControlManager.isReservedSuperOwnerUsername(userDocUsername)) {
                        userDocUsername.take(40)
                    } else {
                        fallbackUsernameFromAuth.take(40)
                    }
                }
            }

            val resolvedAvatar = hostAvatarKey
                .ifBlank { authUser.photoUrl?.toString().orEmpty() }
                .ifBlank { resolvedHostUsername.take(1).uppercase().ifBlank { "H" } }
                .take(1200)

            val resolvedCategory = category.ifBlank { "Party" }.take(60)
            val resolvedMusic = musicTrack.ifBlank { "None" }.take(80)
            val resolvedGame = gameName.ifBlank { "PK Battle" }.take(80)
            val resolvedMicMode = micMode.ifBlank { "Free Mic" }.take(60)
            val resolvedCapacity = capacity.coerceIn(2L, 500L)
            val resolvedTheme = themeKey.ifBlank { "starry_mountain" }.take(500)
            val resolvedSubtitle = subtitle.trim().ifBlank { "$resolvedCategory • Live Voice Stage" }.take(160)
            val nowMillis = System.currentTimeMillis()

            val initialParticipant = RoomParticipant(
                userId = uid,
                username = resolvedHostUsername,
                avatarKey = resolvedAvatar,
                online = true
            )
            val welcomeMessage = RoomChatMessage(
                id = "msg_$nowMillis",
                senderId = uid,
                senderName = resolvedHostUsername,
                senderRole = RoomRole.OWNER.displayName,
                text = "Created room '$cleanTitle' ($resolvedCategory). Select any open seat to join the stage!",
                timeLabel = nowTimeLabel()
            )
            val inviteToken = if (privateRoom) {
                UUID.randomUUID().toString().replace("-", "").take(20)
            } else {
                ""
            }
            // Initial Room State: ALL participant seats empty, zero fake accounts, creator is OWNER, musicPlaying = false (no auto-tune)
            val room = PartyRoom(
                id = roomId,
                roomId = roomId,
                hostId = uid,
                hostUid = uid,
                ownerUid = uid,
                appScope = "noise_live_public",
                hostUsername = resolvedHostUsername,
                ownerUsername = resolvedHostUsername,
                hostAvatarKey = resolvedAvatar,
                roomName = cleanTitle.take(80),
                roomImageUrl = resolvedTheme,
                roomCover = resolvedTheme,
                stageTheme = resolvedTheme,
                title = cleanTitle.take(80),
                subtitle = resolvedSubtitle,
                description = cleanDescription.take(300),
                roomDescription = cleanDescription.take(300),
                category = resolvedCategory,
                roomCategory = resolvedCategory,
                visibility = if (privateRoom) "private" else "public",
                invitationToken = inviteToken,
                authorizedUserIds = listOf(uid),
                isActive = true,
                active = true,
                roomStatus = "active",
                participantCount = 1L,
                musicTrack = resolvedMusic,
                musicSelection = resolvedMusic,
                musicPlaying = false,
                musicVolume = 80L,
                gameName = resolvedGame,
                gameSelection = resolvedGame,
                micMode = resolvedMicMode,
                microphoneSettings = resolvedMicMode,
                capacity = resolvedCapacity,
                roomCapacity = resolvedCapacity,
                privateRoom = privateRoom,
                publicRoom = !privateRoom,
                password = if (privateRoom) password.trim().take(60) else "",
                themeKey = resolvedTheme,
                pkActive = false,
                wipedScreen = false,
                seat1Name = "",
                seat1Avatar = "",
                seat1Diamonds = 0L,
                seat2Name = "",
                seat2Avatar = "",
                seat2Diamonds = 0L,
                lockedSeats = listOf(4L, 7L, 9L, 11L),
                occupiedSeatsJson = "",
                superAdminIds = emptyList(),
                adminIds = emptyList(),
                moderatorIds = emptyList(),
                mutedUserIds = emptyList(),
                kickedUserIds = emptyList(),
                participantsJson = PartyRoom.encodeParticipants(listOf(initialParticipant)),
                chatMessagesJson = PartyRoom.encodeChatMessages(listOf(welcomeMessage)),
                roomNotice = cleanDescription.take(300),
                youtubeVideoUrl = "",
                youtubeVideoId = "",
                youtubeVideoTitle = "",
                youtubePlaying = false,
                youtubePositionSec = 0L,
                youtubeUpdatedAtMillis = 0L,
                listenersCount = 1L,
                giftsTotal = 0L,
                live = true,
                createdTimestamp = nowMillis,
                createdAt = null,
                updatedAt = null
            )
            roomRef.set(room).await()
            val savedSnap = runCatching { roomRef.get().await() }.getOrNull()
            val savedRoom = savedSnap?.toObject(PartyRoom::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                ?.copy(
                    id = roomId,
                    roomId = roomId,
                    hostId = uid,
                    hostUid = uid,
                    ownerUid = uid,
                    hostUsername = resolvedHostUsername,
                    ownerUsername = resolvedHostUsername,
                    roomName = cleanTitle.take(80),
                    title = cleanTitle.take(80),
                    themeKey = resolvedTheme,
                    roomCover = resolvedTheme,
                    stageTheme = resolvedTheme,
                    roomImageUrl = resolvedTheme,
                    createdTimestamp = nowMillis
                )
            savedRoom ?: room.copy(createdAt = Timestamp.now(), updatedAt = Timestamp.now())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, roomRef.path)
            throw e
        }
    }

    suspend fun enterPartyRoom(roomId: String, userProfile: UserProfile): Result<PartyRoom> = runCatching {
        val uid = requireUserId()
        val roomRef = db.collection("party_rooms").document(roomId)
        try {
            db.runTransaction { transaction ->
                val snap = transaction.get(roomRef)
                val existing = snap.toObject(PartyRoom::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    ?: error("Room not found")
                requireNotNull(existing.createdAt) { "Room createdAt required" }
                if (uid in existing.kickedUserIds && uid != existing.hostId) {
                    throw IllegalStateException("You have been kicked from this room")
                }
                val currentParticipants = existing.parseParticipants().toMutableList()
                val idx = currentParticipants.indexOfFirst { it.userId == uid }
                val entry = RoomParticipant(
                    userId = uid,
                    username = userProfile.username.ifBlank { "sebin_mr" },
                    avatarKey = userProfile.avatarKey.ifBlank { "S" },
                    online = true
                )
                if (idx >= 0) {
                    currentParticipants[idx] = entry
                } else {
                    currentParticipants.add(entry)
                }
                val updated = existing.copy(
                    participantsJson = PartyRoom.encodeParticipants(currentParticipants),
                    listenersCount = currentParticipants.size.toLong().coerceAtLeast(1L),
                    updatedAt = null
                )
                transaction.set(roomRef, updated)
                updated.copy(createdAt = existing.createdAt)
            }.await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, roomRef.path)
            throw e
        }
    }

    suspend fun leavePartyRoom(roomId: String): Result<Unit> = runCatching {
        val uid = auth.currentUser?.uid ?: return@runCatching
        if (roomId.isBlank()) return@runCatching
        val roomRef = db.collection("party_rooms").document(roomId)
        try {
            db.runTransaction { transaction ->
                val snap = transaction.get(roomRef)
                if (!snap.exists()) return@runTransaction
                val existing = snap.toObject(PartyRoom::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    ?: return@runTransaction
                if (existing.createdAt == null) return@runTransaction

                val seats = existing.parseOccupiedSeats().toMutableMap()
                val removedSeatEntry = seats.entries.find { it.value.userId == uid }
                if (removedSeatEntry != null) {
                    seats.remove(removedSeatEntry.key)
                }
                val s1 = seats[1]
                val s2 = seats[2]
                val participants = existing.parseParticipants().filterNot { it.userId == uid }
                val updated = existing.copy(
                    occupiedSeatsJson = PartyRoom.encodeOccupiedSeats(seats),
                    seat1Name = s1?.let { "1. ${it.username}" } ?: "",
                    seat1Avatar = s1?.avatarKey ?: "",
                    seat2Name = s2?.let { "2. ${it.username}" } ?: "",
                    seat2Avatar = s2?.avatarKey ?: "",
                    participantsJson = PartyRoom.encodeParticipants(participants),
                    listenersCount = participants.size.toLong().coerceAtLeast(1L),
                    updatedAt = null
                )
                transaction.set(roomRef, updated)
            }.await()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
        }
    }

    /**
     * Atomic seat joining/moving transaction enforcing all 10 Seat Rules:
     * 1. Each seat contains only ONE user.
     * 2. One user cannot occupy multiple seats simultaneously (previous seat is released automatically when moving).
     * 3. Empty unlocked seats can be joined.
     * 4. Occupied seats reject other users with "This seat is already occupied".
     * 5. Synchronized in real time via Firestore transaction.
     */
    suspend fun joinOrMoveSeat(
        roomId: String,
        seatNumber: Int,
        username: String,
        avatarKey: String,
        micMuted: Boolean
    ): Result<PartyRoom> = runCatching {
        val uid = requireUserId()
        require(seatNumber in 1..12) { "Invalid seat number: $seatNumber" }
        val roomRef = db.collection("party_rooms").document(roomId)
        try {
            db.runTransaction { transaction ->
                val snap = transaction.get(roomRef)
                val existing = snap.toObject(PartyRoom::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    ?: error("Room not found")
                requireNotNull(existing.createdAt) { "Room createdAt required" }

                if (uid in existing.kickedUserIds && uid != existing.hostId) {
                    throw IllegalStateException("You have been removed from this room")
                }
                if (seatNumber.toLong() in existing.lockedSeats) {
                    throw IllegalStateException("Seat #$seatNumber is locked")
                }

                val seats = existing.parseOccupiedSeats().toMutableMap()
                val existingOccupant = seats[seatNumber]
                if (existingOccupant != null && existingOccupant.userId != uid) {
                    throw IllegalStateException("This seat is already occupied")
                }

                // Release any other seat currently occupied by this user (1 user = max 1 seat)
                val previousSeatsOfUser = seats.filterValues { it.userId == uid }.keys.toList()
                for (oldSeat in previousSeatsOfUser) {
                    seats.remove(oldSeat)
                }

                val effectiveMuted = micMuted || (uid in existing.mutedUserIds)
                val cleanUsername = username.ifBlank { "sebin_mr" }
                val cleanAvatar = avatarKey.ifBlank { cleanUsername.take(1).uppercase() }
                seats[seatNumber] = SeatOccupant(
                    seatNumber = seatNumber,
                    userId = uid,
                    username = cleanUsername,
                    avatarKey = cleanAvatar,
                    micMuted = effectiveMuted
                )

                val s1 = seats[1]
                val s2 = seats[2]

                val currentParticipants = existing.parseParticipants().toMutableList()
                if (currentParticipants.none { it.userId == uid }) {
                    currentParticipants.add(RoomParticipant(uid, cleanUsername, cleanAvatar, true))
                }

                val updated = existing.copy(
                    occupiedSeatsJson = PartyRoom.encodeOccupiedSeats(seats),
                    seat1Name = s1?.let { "1. ${it.username}" } ?: "",
                    seat1Avatar = s1?.avatarKey ?: "",
                    seat2Name = s2?.let { "2. ${it.username}" } ?: "",
                    seat2Avatar = s2?.avatarKey ?: "",
                    participantsJson = PartyRoom.encodeParticipants(currentParticipants),
                    listenersCount = currentParticipants.size.toLong().coerceAtLeast(1L),
                    updatedAt = null
                )
                transaction.set(roomRef, updated)
                updated.copy(createdAt = existing.createdAt)
            }.await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, roomRef.path)
            throw e
        }
    }

    suspend fun leaveSeat(roomId: String): Result<PartyRoom> = runCatching {
        val uid = requireUserId()
        val roomRef = db.collection("party_rooms").document(roomId)
        try {
            db.runTransaction { transaction ->
                val snap = transaction.get(roomRef)
                val existing = snap.toObject(PartyRoom::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    ?: error("Room not found")
                requireNotNull(existing.createdAt) { "Room createdAt required" }

                val seats = existing.parseOccupiedSeats().toMutableMap()
                val toRemove = seats.filterValues { it.userId == uid }.keys.toList()
                toRemove.forEach { seats.remove(it) }

                val s1 = seats[1]
                val s2 = seats[2]
                val updated = existing.copy(
                    occupiedSeatsJson = PartyRoom.encodeOccupiedSeats(seats),
                    seat1Name = s1?.let { "1. ${it.username}" } ?: "",
                    seat1Avatar = s1?.avatarKey ?: "",
                    seat2Name = s2?.let { "2. ${it.username}" } ?: "",
                    seat2Avatar = s2?.avatarKey ?: "",
                    updatedAt = null
                )
                transaction.set(roomRef, updated)
                updated.copy(createdAt = existing.createdAt)
            }.await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, roomRef.path)
            throw e
        }
    }

    suspend fun setSeatMicMuted(roomId: String, micMuted: Boolean): Result<PartyRoom> = runCatching {
        val uid = requireUserId()
        val roomRef = db.collection("party_rooms").document(roomId)
        try {
            db.runTransaction { transaction ->
                val snap = transaction.get(roomRef)
                val existing = snap.toObject(PartyRoom::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    ?: error("Room not found")
                requireNotNull(existing.createdAt) { "Room createdAt required" }
                if (!micMuted && uid in existing.mutedUserIds) {
                    throw IllegalStateException("You have been muted by a room moderator")
                }

                val seats = existing.parseOccupiedSeats().toMutableMap()
                val myEntry = seats.entries.find { it.value.userId == uid }
                if (myEntry != null) {
                    seats[myEntry.key] = myEntry.value.copy(micMuted = micMuted)
                }
                val updated = existing.copy(
                    occupiedSeatsJson = PartyRoom.encodeOccupiedSeats(seats),
                    updatedAt = null
                )
                transaction.set(roomRef, updated)
                updated.copy(createdAt = existing.createdAt)
            }.await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, roomRef.path)
            throw e
        }
    }

    suspend fun toggleSeatLock(roomId: String, seatNumber: Int): Result<PartyRoom> = runCatching {
        val uid = requireUserId()
        val roomRef = db.collection("party_rooms").document(roomId)
        try {
            db.runTransaction { transaction ->
                val snap = transaction.get(roomRef)
                val existing = snap.toObject(PartyRoom::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    ?: error("Room not found")
                requireNotNull(existing.createdAt) { "Room createdAt required" }

                val callerRole = existing.roleForUser(uid)
                if (!callerRole.canManageSeats()) {
                    throw IllegalStateException("Only Owner, Super Admin, or Admin can lock/unlock seats")
                }

                val seatLong = seatNumber.toLong()
                val updatedLocks = if (seatLong in existing.lockedSeats) {
                    existing.lockedSeats - seatLong
                } else {
                    (existing.lockedSeats + seatLong).distinct()
                }
                val updated = existing.copy(
                    lockedSeats = updatedLocks,
                    updatedAt = null
                )
                transaction.set(roomRef, updated)
                updated.copy(createdAt = existing.createdAt)
            }.await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, roomRef.path)
            throw e
        }
    }

    suspend fun assignRoomRole(
        roomId: String,
        targetUserId: String,
        newRole: RoomRole
    ): Result<PartyRoom> = runCatching {
        val uid = requireUserId()
        val roomRef = db.collection("party_rooms").document(roomId)
        try {
            db.runTransaction { transaction ->
                val snap = transaction.get(roomRef)
                val existing = snap.toObject(PartyRoom::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    ?: error("Room not found")
                requireNotNull(existing.createdAt) { "Room createdAt required" }

                val callerRole = existing.roleForUser(uid)
                val targetCurrentRole = existing.roleForUser(targetUserId)

                if (targetUserId == existing.hostId) {
                    throw IllegalStateException("Cannot modify the Room Owner role")
                }
                if (!callerRole.canAssignRole(newRole, targetCurrentRole)) {
                    throw IllegalStateException("${callerRole.displayName} is not authorized to assign ${newRole.displayName}")
                }

                val nextSuperAdmins = (existing.superAdminIds - targetUserId).let {
                    if (newRole == RoomRole.SUPER_ADMIN) (it + targetUserId).distinct() else it
                }
                val nextAdmins = (existing.adminIds - targetUserId).let {
                    if (newRole == RoomRole.ADMIN) (it + targetUserId).distinct() else it
                }
                val nextModerators = (existing.moderatorIds - targetUserId).let {
                    if (newRole == RoomRole.MODERATOR) (it + targetUserId).distinct() else it
                }

                val updated = existing.copy(
                    superAdminIds = nextSuperAdmins,
                    adminIds = nextAdmins,
                    moderatorIds = nextModerators,
                    updatedAt = null
                )
                transaction.set(roomRef, updated)
                updated.copy(createdAt = existing.createdAt)
            }.await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, roomRef.path)
            throw e
        }
    }

    suspend fun transferRoomOwnership(
        roomId: String,
        newOwnerUserId: String,
        newOwnerUsername: String
    ): Result<PartyRoom> = runCatching {
        val uid = requireUserId()
        require(newOwnerUserId.isNotBlank()) { "New owner user ID required" }
        val roomRef = db.collection("party_rooms").document(roomId)
        try {
            db.runTransaction { transaction ->
                val snap = transaction.get(roomRef)
                val existing = snap.toObject(PartyRoom::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    ?: error("Room not found")
                requireNotNull(existing.createdAt) { "Room createdAt required" }

                if (existing.hostId != uid) {
                    throw IllegalStateException("Only the Room Owner can transfer ownership")
                }
                val updated = existing.copy(
                    hostId = newOwnerUserId,
                    hostUsername = newOwnerUsername.ifBlank { existing.hostUsername },
                    superAdminIds = (existing.superAdminIds - newOwnerUserId + uid).distinct(),
                    adminIds = existing.adminIds - newOwnerUserId,
                    moderatorIds = existing.moderatorIds - newOwnerUserId,
                    updatedAt = null
                )
                transaction.set(roomRef, updated)
                updated.copy(createdAt = existing.createdAt)
            }.await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, roomRef.path)
            throw e
        }
    }

    suspend fun toggleMuteParticipant(roomId: String, targetUserId: String): Result<PartyRoom> = runCatching {
        val uid = requireUserId()
        val roomRef = db.collection("party_rooms").document(roomId)
        try {
            db.runTransaction { transaction ->
                val snap = transaction.get(roomRef)
                val existing = snap.toObject(PartyRoom::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    ?: error("Room not found")
                requireNotNull(existing.createdAt) { "Room createdAt required" }

                val callerRole = existing.roleForUser(uid)
                val targetRole = existing.roleForUser(targetUserId)
                if (!callerRole.canMuteParticipants() || callerRole.rank <= targetRole.rank) {
                    throw IllegalStateException("Insufficient role permissions to mute/unmute this user")
                }

                val seats = existing.parseOccupiedSeats().toMutableMap()
                val seatedEntry = seats.entries.find { it.value.userId == targetUserId }

                val currentlyMuted = targetUserId in existing.mutedUserIds || (seatedEntry?.value?.micMuted == true)
                val willMute = !currentlyMuted
                val nextMuted = if (willMute) {
                    (existing.mutedUserIds + targetUserId).distinct()
                } else {
                    existing.mutedUserIds - targetUserId
                }

                if (seatedEntry != null) {
                    seats[seatedEntry.key] = seatedEntry.value.copy(micMuted = willMute)
                }

                val updated = existing.copy(
                    mutedUserIds = nextMuted,
                    occupiedSeatsJson = PartyRoom.encodeOccupiedSeats(seats),
                    updatedAt = null
                )
                transaction.set(roomRef, updated)
                updated.copy(createdAt = existing.createdAt)
            }.await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, roomRef.path)
            throw e
        }
    }

    /**
     * Owner / Admin Slot Management action: removes the occupant from their voice slot,
     * stops their voice transmission in that room, and returns them to the normal audience
     * area without deleting their account, chat history, or banning them from the room.
     */
    suspend fun removeParticipantFromSlot(
        roomId: String,
        seatNumber: Int,
        targetUserId: String
    ): Result<PartyRoom> = runCatching {
        val uid = requireUserId()
        val roomRef = db.collection("party_rooms").document(roomId)
        try {
            db.runTransaction { transaction ->
                val snap = transaction.get(roomRef)
                val existing = snap.toObject(PartyRoom::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    ?: error("Room not found")
                requireNotNull(existing.createdAt) { "Room createdAt required" }

                val callerRole = existing.roleForUser(uid)
                val targetRole = existing.roleForUser(targetUserId)
                if (!callerRole.canManageSeats() || (targetUserId != uid && callerRole.rank <= targetRole.rank)) {
                    throw IllegalStateException("Only the Room Owner or authorized Admin can remove a user from a voice slot")
                }

                val seats = existing.parseOccupiedSeats().toMutableMap()
                val removedOccupant = seats.remove(seatNumber)
                val extraSeats = seats.filterValues { it.userId == targetUserId }.keys.toList()
                extraSeats.forEach { seats.remove(it) }

                val s1 = seats[1]
                val s2 = seats[2]

                // Ensure the user remains in the normal audience area (participantsJson)
                val currentParticipants = existing.parseParticipants().toMutableList()
                if (targetUserId.isNotBlank() && currentParticipants.none { it.userId == targetUserId }) {
                    currentParticipants.add(
                        RoomParticipant(
                            userId = targetUserId,
                            username = removedOccupant?.username?.ifBlank { "Member" } ?: "Member",
                            avatarKey = removedOccupant?.avatarKey?.ifBlank { "M" } ?: "M",
                            online = true
                        )
                    )
                }

                val updated = existing.copy(
                    occupiedSeatsJson = PartyRoom.encodeOccupiedSeats(seats),
                    seat1Name = s1?.let { "1. ${it.username}" } ?: "",
                    seat1Avatar = s1?.avatarKey ?: "",
                    seat2Name = s2?.let { "2. ${it.username}" } ?: "",
                    seat2Avatar = s2?.avatarKey ?: "",
                    participantsJson = PartyRoom.encodeParticipants(currentParticipants),
                    listenersCount = currentParticipants.size.toLong().coerceAtLeast(1L),
                    updatedAt = null
                )
                transaction.set(roomRef, updated)
                updated.copy(createdAt = existing.createdAt)
            }.await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, roomRef.path)
            throw e
        }
    }

    suspend fun kickParticipantFromRoom(roomId: String, targetUserId: String): Result<PartyRoom> = runCatching {
        val uid = requireUserId()
        val roomRef = db.collection("party_rooms").document(roomId)
        try {
            db.runTransaction { transaction ->
                val snap = transaction.get(roomRef)
                val existing = snap.toObject(PartyRoom::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    ?: error("Room not found")
                requireNotNull(existing.createdAt) { "Room createdAt required" }

                val callerRole = existing.roleForUser(uid)
                val targetRole = existing.roleForUser(targetUserId)
                if (!callerRole.canKickRole(targetRole)) {
                    throw IllegalStateException("Insufficient role permissions to kick ${targetRole.displayName}")
                }

                val seats = existing.parseOccupiedSeats().toMutableMap()
                val seatsToRemove = seats.filterValues { it.userId == targetUserId }.keys.toList()
                seatsToRemove.forEach { seats.remove(it) }

                val s1 = seats[1]
                val s2 = seats[2]
                val participants = existing.parseParticipants().filterNot { it.userId == targetUserId }

                val updated = existing.copy(
                    kickedUserIds = (existing.kickedUserIds + targetUserId).distinct(),
                    superAdminIds = existing.superAdminIds - targetUserId,
                    adminIds = existing.adminIds - targetUserId,
                    moderatorIds = existing.moderatorIds - targetUserId,
                    occupiedSeatsJson = PartyRoom.encodeOccupiedSeats(seats),
                    seat1Name = s1?.let { "1. ${it.username}" } ?: "",
                    seat1Avatar = s1?.avatarKey ?: "",
                    seat2Name = s2?.let { "2. ${it.username}" } ?: "",
                    seat2Avatar = s2?.avatarKey ?: "",
                    participantsJson = PartyRoom.encodeParticipants(participants),
                    listenersCount = participants.size.toLong().coerceAtLeast(1L),
                    updatedAt = null
                )
                transaction.set(roomRef, updated)
                updated.copy(createdAt = existing.createdAt)
            }.await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, roomRef.path)
            throw e
        }
    }

    suspend fun sendRoomChatMessage(
        roomId: String,
        senderName: String,
        text: String
    ): Result<PartyRoom> = runCatching {
        val uid = requireUserId()
        val cleanText = text.trim()
        require(cleanText.isNotBlank()) { "Message cannot be empty" }
        val roomRef = db.collection("party_rooms").document(roomId)
        try {
            db.runTransaction { transaction ->
                val snap = transaction.get(roomRef)
                val existing = snap.toObject(PartyRoom::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    ?: error("Room not found")
                requireNotNull(existing.createdAt) { "Room createdAt required" }

                if (uid in existing.kickedUserIds && uid != existing.hostId) {
                    throw IllegalStateException("You cannot send messages after being kicked")
                }
                if (uid in existing.mutedUserIds && uid != existing.hostId) {
                    throw IllegalStateException("You are currently muted in this room")
                }

                val role = existing.roleForUser(uid)
                val newMsg = RoomChatMessage(
                    id = "rmsg_${UUID.randomUUID().toString().replace("-", "").take(10)}",
                    senderId = uid,
                    senderName = senderName.ifBlank { "sebin_mr" },
                    senderRole = role.displayName,
                    text = cleanText.take(240),
                    timeLabel = nowTimeLabel()
                )
                val updatedMessages = existing.parseChatMessages() + newMsg
                val updated = existing.copy(
                    chatMessagesJson = PartyRoom.encodeChatMessages(updatedMessages),
                    updatedAt = null
                )
                transaction.set(roomRef, updated)
                updated.copy(createdAt = existing.createdAt)
            }.await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, roomRef.path)
            throw e
        }
    }

    suspend fun clearRoomChat(roomId: String, senderName: String): Result<PartyRoom> = runCatching {
        val uid = requireUserId()
        val roomRef = db.collection("party_rooms").document(roomId)
        try {
            db.runTransaction { transaction ->
                val snap = transaction.get(roomRef)
                val existing = snap.toObject(PartyRoom::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    ?: error("Room not found")
                requireNotNull(existing.createdAt) { "Room createdAt required" }

                val callerRole = existing.roleForUser(uid)
                if (!callerRole.canClearChat()) {
                    throw IllegalStateException("Only Owner, Super Admin, Admin, or Moderator can delete room chat")
                }

                val noticeMsg = RoomChatMessage(
                    id = "rmsg_${System.currentTimeMillis()}",
                    senderId = uid,
                    senderName = senderName.ifBlank { "Admin" },
                    senderRole = callerRole.displayName,
                    text = "Room chat history was cleared by ${callerRole.displayName}.",
                    timeLabel = nowTimeLabel()
                )
                val updated = existing.copy(
                    chatMessagesJson = PartyRoom.encodeChatMessages(listOf(noticeMsg)),
                    updatedAt = null
                )
                transaction.set(roomRef, updated)
                updated.copy(createdAt = existing.createdAt)
            }.await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, roomRef.path)
            throw e
        }
    }

    suspend fun closeAndDeletePartyRoom(roomId: String): Result<Unit> = runCatching {
        requireUserId()
        val roomRef = db.collection("party_rooms").document(roomId)
        try {
            SuperOwnerControlManager.deleteSingleChatroom(db, roomId).getOrThrow()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, roomRef.path)
            throw e
        }
    }

    suspend fun updatePartyRoom(room: PartyRoom): Result<PartyRoom> = runCatching {
        val uid = requireUserId()
        require(room.id.isNotBlank()) { "Room ID required" }
        val roomRef = db.collection("party_rooms").document(room.id)
        try {
            val snap = roomRef.get().await()
            val existing = snap.toObject(PartyRoom::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            if (existing?.createdAt != null) {
                val callerRole = existing.roleForUser(uid)
                val updated = if (callerRole == RoomRole.OWNER) {
                    room.copy(
                        createdAt = existing.createdAt,
                        updatedAt = null
                    )
                } else {
                    room.copy(
                        hostId = existing.hostId,
                        superAdminIds = existing.superAdminIds,
                        createdAt = existing.createdAt,
                        updatedAt = null
                    )
                }
                roomRef.set(updated).await()
                updated.copy(createdAt = existing.createdAt)
            } else {
                room
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, roomRef.path)
            throw e
        }
    }

    suspend fun syncPartyRoomYouTubeState(
        roomId: String,
        videoUrlOrId: String,
        videoTitle: String,
        isPlaying: Boolean,
        positionSec: Long
    ): Result<PartyRoom> = runCatching {
        val uid = requireUserId()
        require(roomId.isNotBlank()) { "Room ID required" }
        val roomRef = db.collection("party_rooms").document(roomId)
        try {
            db.runTransaction { tx ->
                val snap = tx.get(roomRef)
                val existing = snap.toObject(PartyRoom::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    ?: error("Room not found")
                requireNotNull(existing.createdAt) { "Room createdAt required" }

                val callerRole = existing.roleForUser(uid)
                if (callerRole.rank < RoomRole.MODERATOR.rank) {
                    throw IllegalStateException("Only the Room Owner, Admin, or Moderator can control shared YouTube playback")
                }

                val cleanInput = videoUrlOrId.trim()
                val extractedId = PartyRoom.extractYouTubeVideoId(cleanInput)
                if (cleanInput.isNotBlank() && extractedId.isBlank()) {
                    throw IllegalArgumentException("Enter a valid YouTube Video or YouTube Live URL / ID")
                }
                val canonicalUrl = if (extractedId.isNotBlank()) {
                    if (cleanInput.startsWith("http")) cleanInput.take(500) else "https://www.youtube.com/watch?v=$extractedId"
                } else {
                    ""
                }
                val cleanTitle = if (extractedId.isBlank()) "" else videoTitle.trim().ifBlank { "YouTube Live / Watch Together ($extractedId)" }.take(160)
                val nowMillis = System.currentTimeMillis()

                val updatedMessages = if (extractedId != existing.youtubeVideoId && extractedId.isNotBlank()) {
                    val announceMsg = RoomChatMessage(
                        id = "rmsg_${nowMillis % 1000000}",
                        senderId = uid,
                        senderName = existing.hostUsername.ifBlank { "Host" },
                        senderRole = callerRole.displayName,
                        text = "▶️ Started Watch Together: $cleanTitle",
                        timeLabel = nowTimeLabel()
                    )
                    existing.parseChatMessages() + announceMsg
                } else {
                    existing.parseChatMessages()
                }

                val updated = existing.copy(
                    youtubeVideoUrl = canonicalUrl,
                    youtubeVideoId = extractedId.take(60),
                    youtubeVideoTitle = cleanTitle,
                    youtubePlaying = if (extractedId.isBlank()) false else isPlaying,
                    youtubePositionSec = if (extractedId.isBlank()) 0L else positionSec.coerceIn(0L, 864_000L),
                    youtubeUpdatedAtMillis = nowMillis,
                    chatMessagesJson = PartyRoom.encodeChatMessages(updatedMessages),
                    updatedAt = null
                )
                tx.set(roomRef, updated)
                updated.copy(createdAt = existing.createdAt)
            }.await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, roomRef.path)
            throw e
        }
    }

    suspend fun sendGiftToPartyRoom(
        room: PartyRoom,
        currentProfile: UserProfile,
        giftAmount: Long,
        targetSeat: Int = 1
    ): Result<PartyRoom> = runCatching {
        val uid = requireUserId()
        require(giftAmount > 0 && currentProfile.coins >= giftAmount) { "Insufficient VIP coins" }
        val roomRef = db.collection("party_rooms").document(room.id)
        val userRef = db.collection("users").document(uid)
        val txId = "tx_gift_${UUID.randomUUID().toString().replace("-", "").take(12)}"
        val txRef = db.collection("coin_transactions").document(txId)
        try {
            val newBalance = currentProfile.coins - giftAmount
            txRef.set(
                CoinTransaction(
                    id = txId,
                    userId = uid,
                    tierName = "VIP Gift: ${room.title}".take(60),
                    amount = -giftAmount,
                    balanceAfter = newBalance,
                    createdAt = null
                )
            ).await()

            val userSnap = userRef.get().await()
            val existingUser = userSnap.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            if (existingUser?.createdAt != null) {
                userRef.set(existingUser.copy(coins = newBalance, updatedAt = null)).await()
            }

            val roomSnap = roomRef.get().await()
            val existingRoom = roomSnap.toObject(PartyRoom::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            if (existingRoom?.createdAt != null) {
                val giftMsg = RoomChatMessage(
                    id = "rmsg_${System.currentTimeMillis()}",
                    senderId = uid,
                    senderName = currentProfile.username.ifBlank { "sebin_mr" },
                    senderRole = existingRoom.roleForUser(uid).displayName,
                    text = "Sent a VIP Gift worth $giftAmount Coins! 🎁",
                    timeLabel = nowTimeLabel()
                )
                val updatedRoom = existingRoom.copy(
                    giftsTotal = existingRoom.giftsTotal + giftAmount,
                    seat1Diamonds = if (targetSeat == 1 || targetSeat == 0) existingRoom.seat1Diamonds + (giftAmount * 10L) else existingRoom.seat1Diamonds,
                    seat2Diamonds = if (targetSeat == 2 || targetSeat == 0) existingRoom.seat2Diamonds + (giftAmount * 10L) else existingRoom.seat2Diamonds,
                    chatMessagesJson = PartyRoom.encodeChatMessages(existingRoom.parseChatMessages() + giftMsg),
                    updatedAt = null
                )
                roomRef.set(updatedRoom).await()
                updatedRoom.copy(createdAt = existingRoom.createdAt)
            } else {
                room
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, roomRef.path)
            throw e
        }
    }

    suspend fun markSingleNotificationRead(notificationId: String): Result<Unit> = runCatching {
        requireUserId()
        if (notificationId.isBlank()) return@runCatching
        val docRef = db.collection("notifications").document(notificationId)
        try {
            val snap = docRef.get().await()
            val existing = snap.toObject(NotificationItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            if (existing?.createdAt != null && !existing.read) {
                docRef.set(existing.copy(read = true, updatedAt = null)).await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun getConversationById(conversationId: String): Result<Conversation?> = runCatching {
        val uid = requireUserId()
        if (conversationId.isBlank()) return@runCatching null
        val docRef = db.collection("conversations").document(conversationId)
        try {
            val snap = docRef.get().await()
            snap.toObject(Conversation::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                ?.resolveForCurrentUser(uid)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            throw e
        }
    }

    suspend fun markAllNotificationsRead(notifications: List<NotificationItem>): Result<Unit> = runCatching {
        requireUserId()
        for (item in notifications.filter { !it.read }) {
            val docRef = db.collection("notifications").document(item.id)
            try {
                val snap = docRef.get().await()
                val existing = snap.toObject(NotificationItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                if (existing?.createdAt != null) {
                    docRef.set(existing.copy(read = true, updatedAt = null)).await()
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.UPDATE, docRef.path)
                throw e
            }
        }
    }

    suspend fun clearAllNotifications(notifications: List<NotificationItem>): Result<Unit> = runCatching {
        requireUserId()
        for (item in notifications) {
            val docRef = db.collection("notifications").document(item.id)
            try {
                docRef.delete().await()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.DELETE, docRef.path)
                throw e
            }
        }
    }

    fun buildSharedConversationId(uidA: String, uidB: String): String {
        val sortedPair = listOf(uidA.trim(), uidB.trim()).filter { it.isNotBlank() }.distinct().sorted().joinToString("_")
        return "conv_${sortedPair.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(90)}"
    }

    suspend fun sendDirectMessage(
        conversation: Conversation,
        senderName: String,
        text: String,
        messageType: String = "text",
        mediaKey: String = "",
        mediaType: String = "photo",
        voiceDurationSec: Long = 0L,
        voiceWaveform: String = "",
        replyToMessage: DirectMessage? = null,
        voiceAudioBase64: String = "",
        voiceMimeType: String = "audio/wav",
        senderAvatarKey: String = "",
        senderVipBadgeLevel: Long = 0L,
        messageIdOverride: String = ""
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val receiverUid = conversation.participantIds.firstOrNull { it.isNotBlank() && it != uid }
            ?: conversation.peerUid.takeIf { it.isNotBlank() && it != uid }
            ?: conversation.initiatorUid.takeIf { it.isNotBlank() && it != uid }
            ?: conversation.peerUid.ifBlank { uid }
        val participants = listOf(uid, receiverUid).filter { it.isNotBlank() }.distinct()
        val canonicalConvId = conversation.id.ifBlank { buildSharedConversationId(uid, receiverUid) }

        val msgId = messageIdOverride.trim().ifBlank {
            "msg_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        }
        val convRef = db.collection("conversations").document(canonicalConvId)
        val msgRef = convRef.collection("messages").document(msgId)
        try {
            val nowLabel = nowTimeLabel()
            val cleanText = text.trim().ifBlank {
                when (messageType) {
                    "voice" -> "🎤 Voice message (${voiceDurationSec.coerceAtLeast(1L)}s)"
                    "image" -> "📷 Sent a photo"
                    "video" -> "🎬 Sent a video"
                    else -> "❤️"
                }
            }.take(2000)

            // 1. Upload voice or media to permanent cloud storage before marking message as SENT
            val resolvedMediaUrl: String
            val resolvedMediaKey: String
            when {
                messageType == "voice" && voiceAudioBase64.isNotBlank() -> {
                    val recordingDoc = VoiceRecordingDoc(
                        id = msgId,
                        messageId = msgId,
                        conversationId = canonicalConvId,
                        senderId = uid,
                        receiverId = receiverUid,
                        participantIds = participants,
                        mimeType = voiceMimeType.take(40),
                        audioBase64 = voiceAudioBase64.take(890_000),
                        durationSec = voiceDurationSec.coerceIn(1L, 600L),
                        waveform = voiceWaveform.take(500),
                        createdAt = null
                    )
                    db.collection("voice_recordings").document(msgId).set(recordingDoc).await()
                    resolvedMediaUrl = "firestore://voice_recordings/$msgId"
                    resolvedMediaKey = mediaKey.ifBlank { resolvedMediaUrl }.take(1200)
                }
                messageType in listOf("image", "video") && mediaKey.isNotBlank() && CloudMediaStorageManager.isTemporaryLocalReference(mediaKey) -> {
                    val uploaded = CloudMediaStorageManager.uploadPermanentMedia(
                        db = db,
                        uid = uid,
                        targetCategory = "post",
                        targetDocId = msgId,
                        sourceKeyOrUri = mediaKey,
                        mediaType = if (messageType == "video" || mediaType == "video") "video" else "photo"
                    ).getOrNull()
                    if (uploaded != null) {
                        resolvedMediaUrl = uploaded.mediaUrl.take(1200)
                        resolvedMediaKey = uploaded.mediaUrl.take(1200)
                    } else {
                        resolvedMediaUrl = mediaKey.take(1200)
                        resolvedMediaKey = mediaKey.take(1200)
                    }
                }
                else -> {
                    resolvedMediaUrl = mediaKey.take(1200)
                    resolvedMediaKey = mediaKey.take(1200)
                }
            }

            // 2. Ensure the shared /conversations/{canonicalConvId} document exists and includes BOTH participantIds
            val snap = convRef.get().await()
            val existingConv = snap.toObject(Conversation::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            val cleanSenderName = senderName.ifBlank { "sebin_mr" }.take(60)
            val cleanSenderAvatar = senderAvatarKey.ifBlank { cleanSenderName.take(1).uppercase().ifBlank { "S" } }.take(1200)
            if (existingConv?.createdAt != null) {
                val updatedInitiatorUid = existingConv.initiatorUid.ifBlank {
                    if (existingConv.peerUid != uid) uid else ""
                }
                val updatedInitiatorUsername = existingConv.initiatorUsername.ifBlank {
                    if (existingConv.peerUid != uid) cleanSenderName.take(40) else ""
                }
                val updatedInitiatorDisplay = existingConv.initiatorDisplayName.ifBlank {
                    if (existingConv.peerUid != uid) cleanSenderName.take(60) else ""
                }
                val updatedInitiatorAvatar = existingConv.initiatorAvatarKey.ifBlank {
                    if (existingConv.peerUid != uid) cleanSenderAvatar else ""
                }
                val mergedParticipants = (existingConv.participantIds + participants)
                    .filter { it.isNotBlank() }
                    .distinct()
                    .take(2)
                convRef.set(
                    existingConv.copy(
                        participantIds = mergedParticipants,
                        initiatorUid = updatedInitiatorUid,
                        initiatorUsername = updatedInitiatorUsername,
                        initiatorDisplayName = updatedInitiatorDisplay,
                        initiatorAvatarKey = updatedInitiatorAvatar,
                        initiatorVipBadgeLevel = if (existingConv.initiatorUid == uid && senderVipBadgeLevel > 0L) senderVipBadgeLevel else existingConv.initiatorVipBadgeLevel,
                        peerTyping = false,
                        lastMessage = cleanText.take(120),
                        lastTimeLabel = nowLabel,
                        unreadCount = existingConv.unreadCount + 1L,
                        updatedAt = null
                    )
                ).await()
            } else {
                val fallbackConv = conversation.copy(
                    id = canonicalConvId,
                    participantIds = participants,
                    initiatorUid = conversation.initiatorUid.ifBlank { uid }.take(128),
                    initiatorUsername = conversation.initiatorUsername.ifBlank { cleanSenderName.take(40) }.take(40),
                    initiatorDisplayName = conversation.initiatorDisplayName.ifBlank { cleanSenderName.take(60) }.take(60),
                    initiatorAvatarKey = conversation.initiatorAvatarKey.ifBlank { cleanSenderAvatar }.take(1200),
                    initiatorVipBadgeLevel = senderVipBadgeLevel.coerceIn(0L, 10L),
                    peerUid = receiverUid.take(128),
                    peerUsername = conversation.peerUsername.ifBlank { "member" }.take(40),
                    peerDisplayName = conversation.peerDisplayName.ifBlank { conversation.peerUsername.ifBlank { "Member" } }.take(60),
                    peerAvatarKey = conversation.peerAvatarKey.ifBlank { "M" }.take(1200),
                    peerTyping = false,
                    lastMessage = cleanText.take(120),
                    lastTimeLabel = nowLabel,
                    unreadCount = 1L,
                    tabCategory = conversation.tabCategory.ifBlank { "Primary" },
                    chatThemeKey = conversation.chatThemeKey.ifBlank { "default_dark" },
                    createdAt = null,
                    updatedAt = null
                )
                convRef.set(fallbackConv).await()
            }

            // 3. STATE: SENT – ONE GREY TICK (✓)
            val msg = DirectMessage(
                id = msgId,
                conversationId = canonicalConvId,
                senderId = uid,
                receiverId = receiverUid.take(128),
                senderName = cleanSenderName,
                senderVipBadgeLevel = senderVipBadgeLevel.coerceIn(0L, 10L),
                participantIds = participants,
                text = cleanText,
                messageContent = cleanText,
                messageType = messageType,
                mediaKey = resolvedMediaKey,
                mediaUrl = resolvedMediaUrl,
                mediaType = mediaType.take(30),
                voiceDurationSec = voiceDurationSec.coerceIn(0L, 600L),
                voiceWaveform = voiceWaveform.take(500),
                deliveryStatus = "sent",
                status = "sent",
                timeLabel = nowLabel,
                reactionEmoji = "",
                replyToMessageId = replyToMessage?.id?.take(128) ?: "",
                replyToSenderName = replyToMessage?.senderName?.take(60) ?: "",
                replyToText = replyToMessage?.text?.take(300) ?: "",
                pinned = false,
                savedLocally = false,
                read = false,
                createdAt = null,
                deliveredAt = null,
                readAt = null
            )
            msgRef.set(msg).await()

            // 4. Create real-time notification in /notifications for the recipient (never for self)
            if (receiverUid.isNotBlank() && receiverUid != uid && messageType != "call") {
                try {
                    val notifId = "notif_msg_${msgId.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(80)}"
                    db.collection("notifications").document(notifId).set(
                        NotificationItem(
                            id = notifId,
                            userId = receiverUid,
                            actorUid = uid,
                            actorUsername = cleanSenderName.take(40),
                            actorAvatarKey = cleanSenderAvatar.take(1200),
                            type = "message",
                            message = cleanText.take(240),
                            targetThumbKey = resolvedMediaUrl.take(1200),
                            conversationId = canonicalConvId.take(128),
                            gameRoomId = "",
                            timeLabel = nowLabel.take(40),
                            read = false,
                            createdAt = null,
                            updatedAt = null
                        )
                    ).await()
                } catch (_: Exception) {
                }
            }
            msgId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, msgRef.path)
            throw e
        }
    }

    suspend fun fetchVoiceRecording(messageId: String): Result<VoiceRecordingDoc?> = runCatching {
        requireUserId()
        if (messageId.isBlank()) return@runCatching null
        val docRef = db.collection("voice_recordings").document(messageId)
        try {
            val snap = docRef.get().await()
            if (!snap.exists()) null
            else snap.toObject(VoiceRecordingDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            throw e
        }
    }

    suspend fun setConversationTyping(conversationId: String, typing: Boolean): Result<Unit> = runCatching {
        val uid = requireUserId()
        if (conversationId.isBlank()) return@runCatching
        val convRef = db.collection("conversations").document(conversationId)
        try {
            try {
                db.collection("typing").document(conversationId)
                    .collection("users").document(uid)
                    .set(
                        TypingStatusDoc(
                            conversationId = conversationId,
                            userId = uid,
                            typing = typing,
                            updatedAtMillis = System.currentTimeMillis()
                        )
                    ).await()
            } catch (_: Exception) {
            }

            val snap = convRef.get().await()
            val existing = snap.toObject(Conversation::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            if (existing?.createdAt != null) {
                val nextTyperIds = if (typing) {
                    (existing.typingUserIds + uid).distinct().take(10)
                } else {
                    existing.typingUserIds - uid
                }
                if (existing.peerTyping != typing || existing.typingUserIds != nextTyperIds) {
                    convRef.set(
                        existing.copy(
                            peerTyping = typing,
                            typingUserIds = nextTyperIds,
                            updatedAt = null
                        )
                    ).await()
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, convRef.path)
            throw e
        }
    }

    suspend fun toggleConversationPeerOnline(conversationId: String): Result<Conversation?> = runCatching {
        requireUserId()
        val convRef = db.collection("conversations").document(conversationId)
        try {
            val snap = convRef.get().await()
            val existing = snap.toObject(Conversation::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            if (existing?.createdAt != null) {
                val nextOnline = !existing.peerOnline
                val lastSeen = if (nextOnline) "Active now" else "Last seen ${nowTimeLabel()}"
                val updated = existing.copy(
                    peerOnline = nextOnline,
                    peerTyping = false,
                    peerLastSeen = lastSeen,
                    updatedAt = null
                )
                convRef.set(updated).await()
                updated.copy(createdAt = existing.createdAt)
            } else {
                null
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, convRef.path)
            throw e
        }
    }

    /**
     * STATE 2: DELIVERED – TWO GREY TICKS (✓✓)
     * Called automatically when the recipient's active client is online and receives incoming messages
     * that are currently in "sent" status. Transitions them to "delivered" and records deliveredAt.
     */
    suspend fun acknowledgeMessagesDelivered(
        conversationId: String,
        messages: List<DirectMessage>
    ): Result<Int> = runCatching {
        val uid = requireUserId()
        if (conversationId.isBlank()) return@runCatching 0
        val convRef = db.collection("conversations").document(conversationId)
        var count = 0
        try {
            for (msg in messages) {
                val isRecipient = msg.senderId != uid || msg.participantIds.size == 1
                if (isRecipient && msg.deliveryStatus == "sent" && !msg.read) {
                    val msgRef = convRef.collection("messages").document(msg.id)
                    val snap = msgRef.get().await()
                    val existing = snap.toObject(DirectMessage::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    if (existing?.createdAt != null && existing.deliveryStatus == "sent") {
                        msgRef.set(
                            existing.copy(
                                deliveryStatus = "delivered",
                                status = "delivered",
                                deliveredAt = Timestamp.now()
                            )
                        ).await()
                        count++
                    }
                }
            }
            count
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, convRef.path)
            throw e
        }
    }

    suspend fun acknowledgePendingDeliveriesInConversation(conversationId: String): Result<Int> = runCatching {
        val uid = requireUserId()
        if (conversationId.isBlank()) return@runCatching 0
        val convRef = db.collection("conversations").document(conversationId)
        val snap = convRef.collection("messages")
            .whereArrayContains("participantIds", uid)
            .get()
            .await()
        val pending = snap.documents.mapNotNull {
            it.toObject(DirectMessage::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        }.filter { it.senderId != uid && it.deliveryStatus == "sent" && !it.read }
        if (pending.isEmpty()) 0
        else acknowledgeMessagesDelivered(conversationId, pending).getOrDefault(0)
    }

    /**
     * STATE 3: SEEN – TWO BLUE TICKS (✓✓)
     * Called ONLY when the recipient opens the specific conversation and views the messages on screen.
     */
    suspend fun markMessagesSeen(conversation: Conversation, messages: List<DirectMessage>): Result<Unit> = runCatching {
        val uid = requireUserId()
        val convRef = db.collection("conversations").document(conversation.id)
        try {
            val nowTs = Timestamp.now()
            for (msg in messages) {
                // Only the recipient (or a 1-person self-thread) can mark messages as seen/read
                val isIncomingOrSelfThread = msg.senderId != uid || conversation.participantIds.size == 1
                if (isIncomingOrSelfThread && (!msg.read || msg.deliveryStatus != "seen" || msg.status != "read")) {
                    val msgRef = convRef.collection("messages").document(msg.id)
                    val snap = msgRef.get().await()
                    val existing = snap.toObject(DirectMessage::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    if (existing?.createdAt != null) {
                        msgRef.set(
                            existing.copy(
                                read = true,
                                deliveryStatus = "seen",
                                status = "read",
                                deliveredAt = existing.deliveredAt ?: nowTs,
                                readAt = nowTs
                            )
                        ).await()
                    }
                }
            }
            if (conversation.unreadCount > 0L) {
                val cSnap = convRef.get().await()
                val existingConv = cSnap.toObject(Conversation::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                if (existingConv?.createdAt != null) {
                    convRef.set(existingConv.copy(unreadCount = 0L, updatedAt = null)).await()
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, convRef.path)
            throw e
        }
    }

    suspend fun reactToDirectMessage(conversationId: String, message: DirectMessage, emoji: String): Result<Unit> = runCatching {
        requireUserId()
        val msgRef = db.collection("conversations").document(conversationId)
            .collection("messages").document(message.id)
        try {
            val snap = msgRef.get().await()
            val existing = snap.toObject(DirectMessage::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            if (existing?.createdAt != null) {
                val nextEmoji = if (existing.reactionEmoji == emoji) "" else emoji
                msgRef.set(existing.copy(reactionEmoji = nextEmoji)).await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, msgRef.path)
            throw e
        }
    }

    suspend fun togglePinDirectMessage(conversation: Conversation, message: DirectMessage): Result<Boolean> = runCatching {
        requireUserId()
        val convRef = db.collection("conversations").document(conversation.id)
        val msgRef = convRef.collection("messages").document(message.id)
        try {
            val snap = msgRef.get().await()
            val existing = snap.toObject(DirectMessage::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                ?: error("Message not found")
            requireNotNull(existing.createdAt) { "Message createdAt required" }
            val nextPinned = !existing.pinned
            msgRef.set(existing.copy(pinned = nextPinned)).await()

            val cSnap = convRef.get().await()
            val existingConv = cSnap.toObject(Conversation::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            if (existingConv?.createdAt != null) {
                val updatedPins = if (nextPinned) {
                    (existingConv.pinnedMessageIds + message.id).distinct().take(20)
                } else {
                    existingConv.pinnedMessageIds - message.id
                }
                convRef.set(
                    existingConv.copy(
                        pinnedMessageIds = updatedPins,
                        updatedAt = null
                    )
                ).await()
            }
            nextPinned
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, msgRef.path)
            throw e
        }
    }

    suspend fun markVoiceMessageSaved(conversationId: String, message: DirectMessage): Result<Unit> = runCatching {
        requireUserId()
        val msgRef = db.collection("conversations").document(conversationId)
            .collection("messages").document(message.id)
        try {
            val snap = msgRef.get().await()
            val existing = snap.toObject(DirectMessage::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            if (existing?.createdAt != null) {
                msgRef.set(existing.copy(savedLocally = true)).await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, msgRef.path)
            throw e
        }
    }

    suspend fun deleteDirectMessage(conversationId: String, messageId: String): Result<Unit> = runCatching {
        requireUserId()
        val msgRef = db.collection("conversations").document(conversationId)
            .collection("messages").document(messageId)
        try {
            msgRef.delete().await()
            try {
                db.collection("voice_recordings").document(messageId).delete().await()
            } catch (_: Exception) {
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, msgRef.path)
            throw e
        }
    }

    suspend fun startOrGetConversation(
        targetUser: UserProfile,
        currentProfile: UserProfile? = null
    ): Result<Conversation> = runCatching {
        val uid = requireUserId()
        val peerId = targetUser.id.ifBlank { targetUser.userId.ifBlank { uid } }
        val convId = buildSharedConversationId(uid, peerId)
        val participants = listOf(uid, peerId).filter { it.isNotBlank() }.distinct()
        val convRef = db.collection("conversations").document(convId)
        try {
            val snap = convRef.get().await()
            if (snap.exists()) {
                val existing = snap.toObject(Conversation::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                if (existing != null) {
                    if (existing.createdAt != null && !existing.participantIds.containsAll(participants)) {
                        val repaired = existing.copy(
                            participantIds = (existing.participantIds + participants).filter { it.isNotBlank() }.distinct().take(2),
                            updatedAt = null
                        )
                        runCatching { convRef.set(repaired).await() }
                        return@runCatching repaired.copy(createdAt = existing.createdAt).resolveForCurrentUser(uid)
                    }
                    return@runCatching existing.resolveForCurrentUser(uid)
                }
            }
            val meProfile = currentProfile ?: runCatching {
                db.collection("users").document(uid).get().await()
                    .toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }.getOrNull()

            val myUsername = meProfile?.username?.takeIf { it.isNotBlank() }
                ?: auth.currentUser?.email?.substringBefore("@")?.takeIf { it.isNotBlank() }
                ?: "sebin_mr"
            val myDisplayName = meProfile?.displayName?.takeIf { it.isNotBlank() }
                ?: auth.currentUser?.displayName?.takeIf { it.isNotBlank() }
                ?: myUsername
            val myAvatarKey = meProfile?.avatarKey?.takeIf { it.isNotBlank() }
                ?: myUsername.take(1).uppercase().ifBlank { "S" }
            val myVipLevel = meProfile?.activeVipBadgeLevel ?: 1L

            val newConv = Conversation(
                id = convId,
                participantIds = participants,
                initiatorUid = uid.take(128),
                initiatorUsername = myUsername.take(40),
                initiatorDisplayName = myDisplayName.take(60),
                initiatorAvatarKey = myAvatarKey.take(1200),
                initiatorVipBadgeLevel = myVipLevel.coerceIn(0L, 10L),
                peerUid = peerId.take(128),
                peerUsername = targetUser.username.ifBlank { "sebin_mr" }.take(40),
                peerDisplayName = targetUser.displayName.ifBlank { targetUser.username.ifBlank { "Sebin Mr" } }.take(60),
                peerAvatarKey = targetUser.avatarKey.ifBlank { "S" }.take(1200),
                peerVerified = targetUser.verified,
                peerVipBadgeLevel = targetUser.activeVipBadgeLevel.coerceIn(0L, 10L),
                peerOnline = targetUser.online,
                peerTyping = false,
                peerLastSeen = targetUser.lastActiveLabel.ifBlank { if (targetUser.online) "Active now" else "Offline" }.take(80),
                pinnedMessageIds = emptyList(),
                lastMessage = "New conversation",
                lastTimeLabel = nowTimeLabel(),
                unreadCount = 0L,
                tabCategory = "Primary",
                chatThemeKey = "default_dark",
                createdAt = null,
                updatedAt = null
            )
            convRef.set(newConv).await()
            newConv.resolveForCurrentUser(uid)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, convRef.path)
            throw e
        }
    }

    suspend fun updateConversationChatTheme(
        conversationId: String,
        chatThemeKey: String
    ): Result<Conversation?> = runCatching {
        val uid = requireUserId()
        require(conversationId.isNotBlank()) { "Conversation ID required" }
        val cleanTheme = chatThemeKey.trim().ifBlank { "default_dark" }.take(60)
        val convRef = db.collection("conversations").document(conversationId)
        try {
            val snap = convRef.get().await()
            val existing = snap.toObject(Conversation::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                ?: return@runCatching null
            requireNotNull(existing.createdAt) { "Conversation createdAt required" }
            val updated = existing.copy(
                chatThemeKey = cleanTheme,
                updatedAt = null
            )
            convRef.set(updated).await()
            updated.copy(createdAt = existing.createdAt).resolveForCurrentUser(uid)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, convRef.path)
            throw e
        }
    }

    suspend fun updateUserThemeMode(themeMode: String): Result<String> = runCatching {
        val uid = requireUserId()
        val normalized = if (themeMode.trim().equals("light", ignoreCase = true)) "light" else "dark"
        val userRef = db.collection("users").document(uid)
        try {
            val snap = userRef.get().await()
            val existing = snap.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            if (existing?.createdAt != null) {
                userRef.set(
                    existing.copy(
                        themeMode = normalized,
                        updatedAt = null
                    )
                ).await()
            }
            normalized
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, userRef.path)
            throw e
        }
    }

    // =========================================================================
    // 1-ON-1 AUDIO CALL & VIDEO CALL SIGNALING (LIVEKIT + FIRESTORE)
    // =========================================================================

    suspend fun startCall(
        conversation: Conversation,
        callerProfile: UserProfile?,
        callType: String
    ): Result<CallSessionDoc> = runCatching {
        val uid = requireUserId()
        val normalizedType = if (callType.equals("video", ignoreCase = true)) "video" else "audio"
        val resolvedConv = conversation.resolveForCurrentUser(uid)
        val calleeUid = resolvedConv.participantIds.firstOrNull { it != uid }
            ?: resolvedConv.peerUid.takeIf { it.isNotBlank() && it != uid }
            ?: resolvedConv.initiatorUid.takeIf { it.isNotBlank() && it != uid }
            ?: resolvedConv.peerUid.ifBlank { uid }
        val participants = listOf(uid, calleeUid).filter { it.isNotBlank() }.distinct()

        val callId = "call_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val roomName = "call_${conversation.id.replace(Regex("[^a-zA-Z0-9_\\-]"), "_")}_${callId.takeLast(8)}"
        val callerName = callerProfile?.username?.ifBlank { "sebin_mr" } ?: "sebin_mr"
        val callerDisplay = callerProfile?.displayName?.ifBlank { callerName } ?: callerName
        val callerAvatar = callerProfile?.avatarKey?.ifBlank { callerName.take(1).uppercase() } ?: "S"
        val callerVip = callerProfile?.activeVipBadgeLevel ?: 1L

        // Check if callee is already in an active ringing or accepted call
        val calleeBusy = try {
            val activeCallsSnap = db.collection("calls")
                .whereArrayContains("participantIds", calleeUid)
                .get()
                .await()
            activeCallsSnap.documents.any { doc ->
                val c = doc.toObject(CallSessionDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                c != null && c.status in listOf("ringing", "accepted") &&
                    ((System.currentTimeMillis() - (c.createdAt?.toDate()?.time ?: System.currentTimeMillis())) < 120_000L)
            }
        } catch (_: Exception) {
            false
        }

        val initialStatus = if (calleeBusy && calleeUid != uid) "busy" else "ringing"

        val callDoc = CallSessionDoc(
            id = callId,
            callId = callId,
            conversationId = conversation.id,
            callerId = uid,
            callerUsername = callerName.take(40),
            callerDisplayName = callerDisplay.take(60),
            callerAvatarKey = callerAvatar.take(500),
            callerVipLevel = callerVip.coerceIn(0L, 10L),
            calleeId = calleeUid.take(128),
            calleeUsername = resolvedConv.peerUsername.ifBlank { "member" }.take(40),
            calleeDisplayName = resolvedConv.peerDisplayName.ifBlank { resolvedConv.peerUsername.ifBlank { "Member" } }.take(60),
            calleeAvatarKey = resolvedConv.peerAvatarKey.ifBlank { "M" }.take(500),
            calleeVipLevel = resolvedConv.peerVipBadgeLevel.coerceIn(0L, 10L),
            participantIds = participants,
            callType = normalizedType,
            status = initialStatus,
            liveKitRoomName = roomName.take(120),
            liveKitUrl = "https://noice-live26.vercel.app",
            callerMicMuted = false,
            calleeMicMuted = false,
            callerCameraEnabled = normalizedType == "video",
            calleeCameraEnabled = normalizedType == "video",
            callerFrontCamera = true,
            calleeFrontCamera = true,
            durationSec = 0L,
            createdAt = null,
            updatedAt = null
        )
        val callRef = db.collection("calls").document(callId)
        try {
            callRef.set(callDoc).await()
            if (calleeUid.isNotBlank() && calleeUid != uid) {
                try {
                    val notifId = "notif_call_${callId.takeLast(12)}"
                    db.collection("notifications").document(notifId).set(
                        NotificationItem(
                            id = notifId,
                            userId = calleeUid,
                            actorUid = uid,
                            actorUsername = callerName.take(40),
                            actorAvatarKey = callerAvatar.take(1200),
                            type = "call",
                            message = if (initialStatus == "busy") {
                                "Missed ${normalizedType} call (busy on another line)"
                            } else {
                                "Incoming ${normalizedType} call from @$callerName"
                            },
                            targetThumbKey = normalizedType,
                            conversationId = conversation.id.take(128),
                            gameRoomId = "",
                            timeLabel = nowTimeLabel(),
                            read = false,
                            createdAt = null,
                            updatedAt = null
                        )
                    ).await()
                } catch (_: Exception) {
                }
            }
            val savedSnap = callRef.get().await()
            savedSnap.toObject(CallSessionDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) ?: callDoc
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, callRef.path)
            throw e
        }
    }

    suspend fun respondToCall(callId: String, accept: Boolean): Result<CallSessionDoc?> = runCatching {
        val uid = requireUserId()
        require(callId.isNotBlank()) { "Call ID required" }
        val callRef = db.collection("calls").document(callId)
        try {
            val snap = callRef.get().await()
            val existing = snap.toObject(CallSessionDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                ?: return@runCatching null
            val nextStatus = if (accept) "accepted" else "rejected"
            val updated = existing.copy(
                status = nextStatus,
                updatedAt = null
            )
            callRef.set(updated).await()

            if (!accept && existing.conversationId.isNotBlank()) {
                val convSnap = db.collection("conversations").document(existing.conversationId).get().await()
                val conv = convSnap.toObject(Conversation::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                if (conv != null) {
                    val icon = if (existing.callType == "video") "📹" else "📞"
                    val label = if (existing.callType == "video") "Video call" else "Audio call"
                    runCatching {
                        sendDirectMessage(
                            conversation = conv,
                            senderName = existing.calleeUsername.ifBlank { "Member" },
                            text = "$icon Declined $label",
                            messageType = "call"
                        )
                    }
                }
            }
            updated.copy(createdAt = existing.createdAt)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, callRef.path)
            throw e
        }
    }

    suspend fun updateCallMediaState(
        callId: String,
        micMuted: Boolean,
        cameraEnabled: Boolean,
        frontCamera: Boolean
    ): Result<CallSessionDoc?> = runCatching {
        val uid = requireUserId()
        if (callId.isBlank()) return@runCatching null
        val callRef = db.collection("calls").document(callId)
        try {
            val snap = callRef.get().await()
            val existing = snap.toObject(CallSessionDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                ?: return@runCatching null
            val isCaller = existing.callerId == uid
            val updated = if (isCaller) {
                existing.copy(
                    callerMicMuted = micMuted,
                    callerCameraEnabled = cameraEnabled,
                    callerFrontCamera = frontCamera,
                    updatedAt = null
                )
            } else {
                existing.copy(
                    calleeMicMuted = micMuted,
                    calleeCameraEnabled = cameraEnabled,
                    calleeFrontCamera = frontCamera,
                    updatedAt = null
                )
            }
            callRef.set(updated).await()
            updated.copy(createdAt = existing.createdAt)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, callRef.path)
            throw e
        }
    }

    suspend fun endCall(callId: String, durationSec: Long): Result<CallSessionDoc?> = runCatching {
        val uid = requireUserId()
        if (callId.isBlank()) return@runCatching null
        val callRef = db.collection("calls").document(callId)
        try {
            val snap = callRef.get().await()
            val existing = snap.toObject(CallSessionDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                ?: return@runCatching null
            if (existing.status in listOf("ended", "missed", "rejected")) {
                return@runCatching existing
            }
            val wasAnswered = existing.status == "accepted" || durationSec > 0L
            val finalStatus = if (wasAnswered) "ended" else "missed"
            val safeDuration = if (wasAnswered) durationSec.coerceAtLeast(1L) else 0L
            val updated = existing.copy(
                status = finalStatus,
                durationSec = safeDuration,
                updatedAt = null
            )
            callRef.set(updated).await()

            if (existing.conversationId.isNotBlank()) {
                val convSnap = db.collection("conversations").document(existing.conversationId).get().await()
                val conv = convSnap.toObject(Conversation::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                if (conv != null) {
                    val icon = if (existing.callType == "video") "📹" else "📞"
                    val typeLabel = if (existing.callType == "video") "Video call" else "Audio call"
                    val mins = safeDuration / 60
                    val secs = safeDuration % 60
                    val durFormatted = String.format(Locale.US, "%02d:%02d", mins, secs)
                    val summaryText = if (finalStatus == "missed") {
                        "$icon Missed $typeLabel"
                    } else {
                        "$icon $typeLabel ended ($durFormatted)"
                    }
                    runCatching {
                        sendDirectMessage(
                            conversation = conv,
                            senderName = if (uid == existing.callerId) existing.callerUsername else existing.calleeUsername,
                            text = summaryText,
                            messageType = "call",
                            voiceDurationSec = safeDuration
                        )
                    }
                }
            }
            updated.copy(createdAt = existing.createdAt)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, callRef.path)
            throw e
        }
    }

    fun observeFollowers(targetUid: String): Flow<List<UserProfile>> {
        val path = "users/$targetUid/followers"
        return db.collection("users").document(targetUid).collection("followers")
            .snapshots()
            .map { snapshot ->
                val relations = snapshot.toObjects(FollowRelation::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                relations.map { rel ->
                    UserProfile(
                        id = rel.followerUid,
                        userId = rel.followerUid,
                        username = rel.followerUsername.ifBlank { "user_${rel.followerUid.take(4)}" },
                        displayName = rel.followerDisplayName.ifBlank { rel.followerUsername },
                        avatarKey = rel.followerAvatarKey.ifBlank { "U" }
                    )
                }.distinctBy { it.userId }
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                throw error
            }
    }

    fun observeFollowing(targetUid: String): Flow<List<UserProfile>> {
        val path = "users/$targetUid/following"
        return db.collection("users").document(targetUid).collection("following")
            .snapshots()
            .map { snapshot ->
                val relations = snapshot.toObjects(FollowRelation::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                relations.map { rel ->
                    UserProfile(
                        id = rel.followingUid,
                        userId = rel.followingUid,
                        username = rel.followingUsername.ifBlank { "user_${rel.followingUid.take(4)}" },
                        displayName = rel.followingDisplayName.ifBlank { rel.followingUsername },
                        avatarKey = rel.followingAvatarKey.ifBlank { "U" }
                    )
                }.distinctBy { it.userId }
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                throw error
            }
    }

    suspend fun followAccount(targetUser: UserProfile, currentProfile: UserProfile): Result<Boolean> = runCatching {
        val uid = requireUserId()
        val targetId = targetUser.userId.ifBlank { targetUser.id }
        require(targetId.isNotBlank() && targetId != uid) { "Cannot follow yourself" }
        val userRef = db.collection("users").document(uid)
        val targetDocId = targetUser.id.ifBlank { targetId }
        val targetRef = db.collection("users").document(targetDocId)
        try {
            val snap = userRef.get().await()
            val existing = snap.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            val alreadyFollowing = existing != null && targetId in existing.followedUserIds
            val nowFollowing = !alreadyFollowing

            // 1. Write or delete /users/{uid}/following/{targetId} and /users/{targetId}/followers/{uid}
            val followingDocRef = userRef.collection("following").document(targetId)
            val followerDocRef = targetRef.collection("followers").document(uid)
            val relation = FollowRelation(
                followerUid = uid,
                followingUid = targetId,
                followerUsername = (existing?.username ?: currentProfile.username).ifBlank { "sebin_mr" }.take(60),
                followerDisplayName = (existing?.displayName ?: currentProfile.displayName).ifBlank { "Sebin Mr" }.take(60),
                followerAvatarKey = (existing?.avatarKey ?: currentProfile.avatarKey).ifBlank { "S" }.take(500),
                followingUsername = targetUser.username.ifBlank { "member" }.take(60),
                followingDisplayName = targetUser.displayName.ifBlank { targetUser.username }.take(60),
                followingAvatarKey = targetUser.avatarKey.ifBlank { "M" }.take(500),
                createdAt = null
            )
            if (nowFollowing) {
                try {
                    followingDocRef.set(relation).await()
                } catch (_: Exception) {
                }
                try {
                    followerDocRef.set(relation).await()
                } catch (_: Exception) {
                }
            } else {
                try {
                    followingDocRef.delete().await()
                } catch (_: Exception) {
                }
                try {
                    followerDocRef.delete().await()
                } catch (_: Exception) {
                }
            }

            // 2. Update current user's followingCount and followedUserIds
            if (existing?.createdAt != null) {
                val nextFollowedIds = if (nowFollowing) {
                    (existing.followedUserIds + targetId).distinct().take(500)
                } else {
                    existing.followedUserIds - targetId
                }
                val nextFollowingCount = nextFollowedIds.size.toLong()
                userRef.set(
                    existing.copy(
                        followingCount = nextFollowingCount,
                        followedUserIds = nextFollowedIds,
                        updatedAt = null
                    )
                ).await()
            }

            // 3. Update target user's followersCount
            if (targetDocId.isNotBlank()) {
                val targetSnap = targetRef.get().await()
                val existingTarget = targetSnap.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                if (existingTarget?.createdAt != null) {
                    val actualFollowersSnap = try {
                        targetRef.collection("followers").get().await()
                    } catch (_: Exception) {
                        null
                    }
                    val nextFollowersCount = actualFollowersSnap?.size()?.toLong() ?: if (nowFollowing) {
                        existingTarget.followersCount + 1L
                    } else {
                        (existingTarget.followersCount - 1L).coerceAtLeast(0L)
                    }
                    targetRef.set(
                        existingTarget.copy(
                            followersCount = nextFollowersCount,
                            updatedAt = null
                        )
                    ).await()
                }
            }

            // 4. Send notification to target user when followed
            if (nowFollowing) {
                try {
                    val notifId = "notif_follow_${uid.take(8)}_${targetId.take(8)}"
                    val notif = NotificationItem(
                        id = notifId,
                        userId = targetId,
                        actorUid = uid,
                        actorUsername = (existing?.username ?: currentProfile.username).ifBlank { "sebin_mr" },
                        actorAvatarKey = (existing?.avatarKey ?: currentProfile.avatarKey).ifBlank { "S" },
                        type = "follow",
                        message = "started following you",
                        targetThumbKey = "",
                        timeLabel = "Just now",
                        read = false,
                        createdAt = null
                    )
                    db.collection("notifications").document(notifId).set(notif).await()
                } catch (_: Exception) {
                }
            }
            nowFollowing
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, userRef.path)
            throw e
        }
    }

    // =========================================================================
    // MULTIPLAYER 3D GAMING PLATFORM (CHATROOM GAMES + DEDICATED GAMES SECTION)
    // =========================================================================

    fun getGameCatalog(): List<NoiseGameCatalogItem> = listOf(
        NoiseGameCatalogItem(
            gameType = "ludo_3d",
            title = "3D Ludo",
            subtitle = "Complete 15x15 board, 4 colored bases, 16 tokens, 8 safe zones & 3D dice",
            category = "Classic 3D Board",
            minPlayers = 2,
            maxPlayers = 4,
            supportsVoiceChatroom = true,
            featured = true,
            popular = true,
            activePlayersCount = 2140,
            unitySceneName = "Ludo3DRoyalScene",
            accentColorHex = 0xFF9D4EDD,
            secondaryColorHex = 0xFF00E5FF,
            badgeText = "Available • Playable 3D"
        ),
        NoiseGameCatalogItem(
            gameType = "carrom_3d",
            title = "3D Carrom",
            subtitle = "Realistic wooden board, 19 coins, Red Queen cover rule & striker physics",
            category = "Board & Physics",
            minPlayers = 2,
            maxPlayers = 4,
            supportsVoiceChatroom = true,
            featured = true,
            popular = true,
            activePlayersCount = 1280,
            unitySceneName = "Carrom3DArenaScene",
            accentColorHex = 0xFFFFB800,
            secondaryColorHex = 0xFFFF2E93,
            badgeText = "Available • Playable 3D"
        ),
        NoiseGameCatalogItem(
            gameType = "pool_8ball",
            title = "8 Ball Pool",
            subtitle = "16 balls (Solids, Stripes & #8), cue aiming, scratch fouls & 6 pockets",
            category = "Sports 3D",
            minPlayers = 2,
            maxPlayers = 2,
            supportsVoiceChatroom = true,
            featured = true,
            popular = true,
            activePlayersCount = 1690,
            unitySceneName = "Pool8Ball3DScene",
            accentColorHex = 0xFF00E676,
            secondaryColorHex = 0xFF00B4D8,
            badgeText = "Available • Playable 3D"
        ),
        NoiseGameCatalogItem(
            gameType = "mini_racing",
            title = "Multiplayer Racing",
            subtitle = "3D circuits, car selection, 3-2-1-GO, steering, brake, nitro & live positions",
            category = "3D Racing",
            minPlayers = 2,
            maxPlayers = 8,
            supportsVoiceChatroom = true,
            featured = true,
            popular = true,
            activePlayersCount = 940,
            unitySceneName = "MiniRacing3DScene",
            accentColorHex = 0xFFFF2E93,
            secondaryColorHex = 0xFFFF6D00,
            badgeText = "Available • Playable 3D"
        ),
        NoiseGameCatalogItem(
            gameType = "multiplayer_quiz",
            title = "Multiplayer Quiz",
            subtitle = "5 categories, 3 difficulty levels, countdown timer & live leaderboard",
            category = "Party & Trivia",
            minPlayers = 2,
            maxPlayers = 8,
            supportsVoiceChatroom = true,
            featured = true,
            popular = true,
            activePlayersCount = 780,
            unitySceneName = "QuizArena3DScene",
            accentColorHex = 0xFF00B4D8,
            secondaryColorHex = 0xFF9D4EDD,
            badgeText = "Available • Playable 3D"
        ),
        NoiseGameCatalogItem(
            gameType = "chess_3d",
            title = "Chess",
            subtitle = "Full 8x8 board, 32 pieces, Check, Checkmate, Castling, En Passant & Promotion",
            category = "Strategy Board",
            minPlayers = 2,
            maxPlayers = 2,
            supportsVoiceChatroom = true,
            featured = true,
            popular = true,
            activePlayersCount = 1420,
            unitySceneName = "Chess3DRoyalScene",
            accentColorHex = 0xFFFFD54F,
            secondaryColorHex = 0xFF7C4DFF,
            badgeText = "Available • Full Rules"
        ),
        NoiseGameCatalogItem(
            gameType = "checkers_3d",
            title = "Checkers",
            subtitle = "8x8 board, 24 pieces, diagonal jumps, multi-jump captures & King promotion",
            category = "Strategy Board",
            minPlayers = 2,
            maxPlayers = 2,
            supportsVoiceChatroom = true,
            featured = false,
            popular = true,
            activePlayersCount = 860,
            unitySceneName = "Checkers3DScene",
            accentColorHex = 0xFFFF5252,
            secondaryColorHex = 0xFFFFB800,
            badgeText = "Available • Multi-Jump"
        ),
        NoiseGameCatalogItem(
            gameType = "tic_tac_toe",
            title = "Tic Tac Toe",
            subtitle = "Neon 3x3 arena, instant X & O duels, win-line detection & streak tracking",
            category = "Quick Duel",
            minPlayers = 2,
            maxPlayers = 2,
            supportsVoiceChatroom = true,
            featured = false,
            popular = true,
            activePlayersCount = 1150,
            unitySceneName = "TicTacToeNeonScene",
            accentColorHex = 0xFF00E5FF,
            secondaryColorHex = 0xFFFF2E93,
            badgeText = "Available • Quick 1v1"
        ),
        NoiseGameCatalogItem(
            gameType = "dominoes_pro",
            title = "Dominoes",
            subtitle = "Complete 28-tile Double-Six set, boneyard draw pile, chain ends & pip scoring",
            category = "Classic Tile",
            minPlayers = 2,
            maxPlayers = 4,
            supportsVoiceChatroom = true,
            featured = true,
            popular = true,
            activePlayersCount = 910,
            unitySceneName = "DominoesProScene",
            accentColorHex = 0xFF2EC4B6,
            secondaryColorHex = 0xFFFF9F1C,
            badgeText = "Available • 28 Tiles"
        ),
        NoiseGameCatalogItem(
            gameType = "snakes_ladders",
            title = "Snakes and Ladders",
            subtitle = "100-square numbered board, golden ladders, neon snakes & animated dice",
            category = "Classic Board",
            minPlayers = 2,
            maxPlayers = 4,
            supportsVoiceChatroom = true,
            featured = false,
            popular = true,
            activePlayersCount = 1040,
            unitySceneName = "SnakesLadders3DScene",
            accentColorHex = 0xFF00E676,
            secondaryColorHex = 0xFFFF2E93,
            badgeText = "Available • 100 Squares"
        ),
        NoiseGameCatalogItem(
            gameType = "mini_arcade",
            title = "Mini Arcade Games",
            subtitle = "4 Complete Arcade Classics: Bubble Shooter, Memory Match, Snake & Block Puzzle",
            category = "Arcade Hub",
            minPlayers = 1,
            maxPlayers = 4,
            supportsVoiceChatroom = true,
            featured = true,
            popular = true,
            activePlayersCount = 1830,
            unitySceneName = "MiniArcadeHubScene",
            accentColorHex = 0xFFFF2E93,
            secondaryColorHex = 0xFF00E5FF,
            badgeText = "4-in-1 Arcade • Playable"
        )
    )

    fun getGameModuleStatusList(): List<GameModuleStatusItem> = listOf(
        GameModuleStatusItem("ludo_3d", "3D Ludo", "Completed", true, true, true, true, "15x15 Board • 16 Tokens • 8 Safe Zones • 3D Dice"),
        GameModuleStatusItem("carrom_3d", "3D Carrom", "Completed", true, true, true, true, "19 Coins • Queen Cover Rule • Striker Physics"),
        GameModuleStatusItem("pool_8ball", "8 Ball Pool", "Completed", true, true, true, true, "16 Balls • Solids/Stripes • Scratch Fouls • 6 Pockets"),
        GameModuleStatusItem("mini_racing", "Multiplayer Racing", "Completed", true, true, true, true, "3 Tracks • 4 Cars • 3-2-1-GO • Nitro & Lap Counter"),
        GameModuleStatusItem("multiplayer_quiz", "Multiplayer Quiz", "Completed", true, true, true, true, "5 Categories • 3 Difficulties • Timer & Leaderboard"),
        GameModuleStatusItem("chess_3d", "Chess", "Completed", true, true, true, true, "8x8 Board • 32 Pieces • Check/Checkmate/Castling/Promotion"),
        GameModuleStatusItem("checkers_3d", "Checkers", "Completed", true, true, true, true, "8x8 Board • 24 Pieces • Multi-Jump & King Promotion"),
        GameModuleStatusItem("tic_tac_toe", "Tic Tac Toe", "Completed", true, true, true, true, "3x3 Board • X/O Controls • Win Line & Draw Detection"),
        GameModuleStatusItem("dominoes_pro", "Dominoes", "Completed", true, true, true, true, "28 Double-Six Tiles • Boneyard • Chain Ends & Pip Score"),
        GameModuleStatusItem("snakes_ladders", "Snakes and Ladders", "Completed", true, true, true, true, "100-Square 10x10 Board • Ladders Climb & Snakes Slide"),
        GameModuleStatusItem("mini_arcade", "Mini Arcade Games", "Completed", true, true, true, true, "Bubble Shooter • Memory Match • Snake • Block Puzzle")
    )

    fun getVipBadgeCatalog(): List<VipBadgeCatalogItem> = listOf(
        VipBadgeCatalogItem(
            vipLevel = 1,
            badgeName = "VIP 1 – Blue Sapphire",
            gemstoneName = "Blue Sapphire",
            frameDescription = "Silver royal frame with small sparkling diamonds",
            effectDescription = "Sapphire crystal shimmer & rotating silver light",
            priceCoins = 1_000L,
            primaryColorHex = 0xFF1E88E5,
            secondaryColorHex = 0xFF00E5FF,
            frameColorHex = 0xFFE0E6ED,
            glowColorHex = 0xFF42A5F5
        ),
        VipBadgeCatalogItem(
            vipLevel = 2,
            badgeName = "VIP 2 – Emerald",
            gemstoneName = "Deep Green Emerald",
            frameDescription = "Luxury gold frame with sculpted emerald facets",
            effectDescription = "Green glowing crystal rays & diamond sparkles",
            priceCoins = 2_500L,
            primaryColorHex = 0xFF00C853,
            secondaryColorHex = 0xFF69F0AE,
            frameColorHex = 0xFFFFD54F,
            glowColorHex = 0xFF00E676
        ),
        VipBadgeCatalogItem(
            vipLevel = 3,
            badgeName = "VIP 3 – Ruby",
            gemstoneName = "Deep Red Ruby",
            frameDescription = "Diamond-encrusted royal crown & gold frame",
            effectDescription = "Crimson ruby fire & floating diamond sparkles",
            priceCoins = 5_000L,
            primaryColorHex = 0xFFD50000,
            secondaryColorHex = 0xFFFF5252,
            frameColorHex = 0xFFFFB800,
            glowColorHex = 0xFFFF1744
        ),
        VipBadgeCatalogItem(
            vipLevel = 4,
            badgeName = "VIP 4 – Amethyst",
            gemstoneName = "Purple Amethyst Crystal",
            frameDescription = "Sculpted platinum circular crest with amethyst crown",
            effectDescription = "Purple glowing crystal particles & prism reflections",
            priceCoins = 8_000L,
            primaryColorHex = 0xFF8E24AA,
            secondaryColorHex = 0xFFE040FB,
            frameColorHex = 0xFFCFD8DC,
            glowColorHex = 0xFFAA00FF
        ),
        VipBadgeCatalogItem(
            vipLevel = 5,
            badgeName = "VIP 5 – Pink Diamond",
            gemstoneName = "Large Pink Diamond",
            frameDescription = "Rose-gold royal wings & diamond crown setting",
            effectDescription = "Rose-pink diamond shine sweep & starlight flares",
            priceCoins = 12_000L,
            primaryColorHex = 0xFFFF2E93,
            secondaryColorHex = 0xFFFF80AB,
            frameColorHex = 0xFFF8BBD0,
            glowColorHex = 0xFFFF4081
        ),
        VipBadgeCatalogItem(
            vipLevel = 6,
            badgeName = "VIP 6 – Black Diamond",
            gemstoneName = "Obsidian Black Diamond",
            frameDescription = "Black platinum armored crest with white diamond studs",
            effectDescription = "Premium dark metallic shine & silver-violet edge aura",
            priceCoins = 18_000L,
            primaryColorHex = 0xFF263238,
            secondaryColorHex = 0xFF90A4AE,
            frameColorHex = 0xFFB0BEC5,
            glowColorHex = 0xFF7C4DFF
        ),
        VipBadgeCatalogItem(
            vipLevel = 7,
            badgeName = "VIP 7 – Imperial Topaz",
            gemstoneName = "Golden Imperial Topaz",
            frameDescription = "Royal winged gold shield & sovereign crown",
            effectDescription = "Blazing golden light beams & radiant topaz flares",
            priceCoins = 25_000L,
            primaryColorHex = 0xFFFFB300,
            secondaryColorHex = 0xFFFFEA00,
            frameColorHex = 0xFFFFD54F,
            glowColorHex = 0xFFFFC107
        ),
        VipBadgeCatalogItem(
            vipLevel = 8,
            badgeName = "VIP 8 – Aquamarine",
            gemstoneName = "Ice-Blue Aquamarine",
            frameDescription = "Silver-platinum celestial wings & crystal halo",
            effectDescription = "Glacial blue crystal particles & arctic light sweep",
            priceCoins = 35_000L,
            primaryColorHex = 0xFF00B0FF,
            secondaryColorHex = 0xFF84FFFF,
            frameColorHex = 0xFFECEFF1,
            glowColorHex = 0xFF00E5FF
        ),
        VipBadgeCatalogItem(
            vipLevel = 9,
            badgeName = "VIP 9 – Rainbow Opal",
            gemstoneName = "Iridescent Rainbow Opal",
            frameDescription = "Multi-color diamond crown & prismatic gold crest",
            effectDescription = "Iridescent rainbow glow & multi-spectrum crystal aura",
            priceCoins = 50_000L,
            primaryColorHex = 0xFFFF2E93,
            secondaryColorHex = 0xFF00E5FF,
            frameColorHex = 0xFFFFD54F,
            glowColorHex = 0xFFE040FB
        ),
        VipBadgeCatalogItem(
            vipLevel = 10,
            badgeName = "VIP 10 – Eternal Diamond",
            gemstoneName = "Sovereign Eternal Diamond",
            frameDescription = "Gold & platinum royal crown with grand winged diamond halo",
            effectDescription = "Eternal white & rainbow prism beams with floating diamonds",
            priceCoins = 75_000L,
            primaryColorHex = 0xFFFFFFFF,
            secondaryColorHex = 0xFF80D8FF,
            frameColorHex = 0xFFFFD700,
            glowColorHex = 0xFF00E5FF
        )
    )

    fun observeVipBadgePurchases(userId: String): Flow<List<VipBadgePurchaseDoc>> {
        val path = "vip_badge_purchases"
        return db.collection("vip_badge_purchases")
            .whereEqualTo("userId", userId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(VipBadgePurchaseDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                throw error
            }
    }

    suspend fun purchaseVipBadge(vipLevel: Int): Result<UserProfile> = runCatching {
        val uid = requireUserId()
        val badgeItem = getVipBadgeCatalog().firstOrNull { it.vipLevel == vipLevel }
            ?: throw IllegalArgumentException("Invalid VIP Badge Level: $vipLevel")
        val userRef = db.collection("users").document(uid)
        val purchaseId = "vipbadge_${uid.take(8)}_lvl${vipLevel}_${System.currentTimeMillis() % 100000}"
        val purchaseRef = db.collection("vip_badge_purchases").document(purchaseId)
        val txRef = db.collection("coin_transactions").document("tx_vip_${purchaseId.takeLast(12)}")

        try {
            db.runTransaction { tx ->
                val snap = tx.get(userRef)
                val existing = snap.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    ?: throw IllegalStateException("User profile not found")
                requireNotNull(existing.createdAt) { "User createdAt is required" }

                val levelLong = vipLevel.toLong()
                if (levelLong in existing.ownedVipBadges) {
                    val equipped = existing.copy(
                        vip = true,
                        activeVipBadgeLevel = levelLong,
                        activeVipBadgeName = badgeItem.badgeName,
                        updatedAt = null
                    )
                    tx.set(userRef, equipped)
                    return@runTransaction equipped.copy(createdAt = existing.createdAt)
                }

                if (existing.coins < badgeItem.priceCoins) {
                    throw IllegalStateException(
                        "Insufficient coins for ${badgeItem.badgeName}. Need ${badgeItem.priceCoins} coins (have ${existing.coins})."
                    )
                }

                val newBalance = existing.coins - badgeItem.priceCoins
                val nextOwned = (existing.ownedVipBadges + levelLong).distinct().sorted()
                val updatedUser = existing.copy(
                    coins = newBalance,
                    vip = true,
                    activeVipBadgeLevel = levelLong,
                    activeVipBadgeName = badgeItem.badgeName,
                    ownedVipBadges = nextOwned,
                    updatedAt = null
                )
                tx.set(userRef, updatedUser)

                val purchaseDoc = VipBadgePurchaseDoc(
                    id = purchaseId,
                    userId = uid,
                    username = existing.username.ifBlank { "sebin_mr" },
                    vipLevel = levelLong,
                    badgeName = badgeItem.badgeName,
                    gemstoneName = badgeItem.gemstoneName,
                    priceCoins = badgeItem.priceCoins,
                    balanceAfter = newBalance,
                    createdAt = null
                )
                tx.set(purchaseRef, purchaseDoc)

                val coinTx = CoinTransaction(
                    id = txRef.id,
                    userId = uid,
                    tierName = "Purchased ${badgeItem.badgeName}",
                    amount = -badgeItem.priceCoins,
                    balanceAfter = newBalance,
                    createdAt = null
                )
                tx.set(txRef, coinTx)

                updatedUser.copy(createdAt = existing.createdAt)
            }.await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, userRef.path)
            throw e
        }
    }

    suspend fun selectActiveVipBadge(vipLevel: Int): Result<UserProfile> = runCatching {
        val uid = requireUserId()
        require(vipLevel in 0..10) { "Invalid VIP level: $vipLevel" }
        val badgeItem = getVipBadgeCatalog().firstOrNull { it.vipLevel == vipLevel }
        val userRef = db.collection("users").document(uid)
        try {
            db.runTransaction { tx ->
                val snap = tx.get(userRef)
                val existing = snap.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    ?: throw IllegalStateException("User profile not found")
                requireNotNull(existing.createdAt) { "User createdAt is required" }

                val levelLong = vipLevel.toLong()
                if (vipLevel > 0 && levelLong !in existing.ownedVipBadges) {
                    throw IllegalStateException("You do not own VIP $vipLevel yet. Purchase it first in the VIP Badge Shop.")
                }

                val updated = existing.copy(
                    activeVipBadgeLevel = levelLong,
                    activeVipBadgeName = badgeItem?.badgeName ?: "",
                    updatedAt = null
                )
                tx.set(userRef, updated)
                updated.copy(createdAt = existing.createdAt)
            }.await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, userRef.path)
            throw e
        }
    }

    fun observeGameRooms(): Flow<List<GameRoomDoc>> {
        val path = "game_rooms"
        return db.collection("game_rooms")
            .whereEqualTo("appScope", "noise_live_public")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(GameRoomDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .filter { it.status != "finished" }
                    .distinctBy { it.id }
                    .sortedByDescending { it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                throw error
            }
    }

    fun observeSingleGameRoom(gameRoomId: String): Flow<GameRoomDoc?> {
        val path = "game_rooms/$gameRoomId"
        return db.collection("game_rooms").document(gameRoomId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObject(GameRoomDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.GET, path)
                throw error
            }
    }

    fun observeChatRoomGameRooms(chatRoomId: String): Flow<List<GameRoomDoc>> {
        val path = "game_rooms"
        return db.collection("game_rooms")
            .whereEqualTo("appScope", "noise_live_public")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(GameRoomDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .filter { it.chatRoomId == chatRoomId && it.status != "finished" }
                    .sortedByDescending { it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                throw error
            }
    }

    fun observeGameInvites(userId: String): Flow<List<GameInviteDoc>> {
        val path = "game_invites"
        return db.collection("game_invites")
            .whereEqualTo("targetUserId", userId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(GameInviteDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .filter { it.status == "pending" }
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                throw error
            }
    }

    fun observeGameLeaderboard(): Flow<List<UserProfile>> {
        val path = "users"
        return db.collection("users")
            .whereEqualTo("appScope", "noise_live_public")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .filter { !it.id.startsWith("acct_nora_") && !it.id.startsWith("acct_creator_") && !it.id.startsWith("acct_maya_") }
                    .distinctBy { it.userId.ifBlank { it.id } }
                    .sortedWith(
                        compareByDescending<UserProfile> { it.gameWins }
                            .thenByDescending { it.gameRating }
                            .thenByDescending { it.coins }
                    )
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                throw error
            }
    }

    fun observeGameHistory(userId: String): Flow<List<GameMatchResultDoc>> {
        val path = "game_history"
        return db.collection("game_history")
            .whereEqualTo("appScope", "noise_live_public")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(GameMatchResultDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .filter { userId in it.participantIds || it.winnerId == userId }
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                throw error
            }
    }

    suspend fun createGameRoom(
        hostProfile: UserProfile,
        gameType: String,
        gameTitle: String,
        mode: String = "1v1",
        maxPlayers: Int = 2,
        privateRoom: Boolean = false,
        chatRoomId: String = "",
        chatRoomTitle: String = "",
        voiceRoomConnected: Boolean = false,
        entryCoins: Long = 100L,
        roomIdOverride: String = "",
        roomCodeOverride: String = ""
    ): Result<GameRoomDoc> = runCatching {
        val uid = requireUserId()
        val roomId = roomIdOverride.trim().ifBlank {
            "groom_${UUID.randomUUID().toString().replace("-", "").take(14)}"
        }
        val roomCode = roomCodeOverride.trim().ifBlank {
            UUID.randomUUID().toString().replace("-", "").uppercase().take(6)
        }
        val relayCode = "RLY-${UUID.randomUUID().toString().replace("-", "").uppercase().take(8)}"
        val lobbyId = "LBY-${UUID.randomUUID().toString().replace("-", "").take(10)}"
        val roomRef = db.collection("game_rooms").document(roomId)
        try {
            val hostSlot = GamePlayerSlot(
                slotIndex = 1,
                userId = uid,
                username = hostProfile.username.ifBlank { "sebin_mr" },
                displayName = hostProfile.displayName.ifBlank { hostProfile.username },
                avatarKey = hostProfile.avatarKey.ifBlank { "S" },
                team = 1,
                ready = true,
                score = 0L,
                connected = true,
                micEnabled = true,
                speaking = false
            )
            val doc = GameRoomDoc(
                id = roomId,
                appScope = "noise_live_public",
                roomCode = roomCode,
                gameType = gameType.take(40),
                gameTitle = gameTitle.ifBlank { "3D Carrom Pro" }.take(80),
                hostId = uid,
                hostUsername = hostProfile.username.ifBlank { "sebin_mr" }.take(60),
                hostAvatarKey = hostProfile.avatarKey.ifBlank { "S" }.take(500),
                chatRoomId = chatRoomId.take(128),
                chatRoomTitle = chatRoomTitle.take(100),
                mode = mode.take(30),
                status = "waiting",
                privateRoom = privateRoom,
                maxPlayers = maxPlayers.coerceIn(1, 8).toLong(),
                currentTurnUserId = uid,
                winnerUserId = "",
                winnerUsername = "",
                unityLobbyId = lobbyId,
                unityRelayJoinCode = relayCode,
                voiceChannelId = if (chatRoomId.isNotBlank()) "voice_$chatRoomId" else "voice_$roomId",
                voiceRoomConnected = voiceRoomConnected || chatRoomId.isNotBlank(),
                participantIds = listOf(uid),
                invitedUserIds = emptyList(),
                playersJson = GameRoomDoc.encodePlayers(listOf(hostSlot)),
                gameStateJson = "{}",
                entryCoins = entryCoins.coerceIn(0L, 1_000_000L),
                prizePoolCoins = (entryCoins * 2L).coerceIn(0L, 10_000_000L),
                createdAt = null,
                updatedAt = null
            )
            roomRef.set(doc).await()

            // Update user's currentGameName & recentlyPlayedGames
            try {
                val userRef = db.collection("users").document(uid)
                val uSnap = userRef.get().await()
                val existingUser = uSnap.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                if (existingUser?.createdAt != null) {
                    val recent = (listOf(gameType) + existingUser.recentlyPlayedGames).distinct().take(10)
                    userRef.set(
                        existingUser.copy(
                            currentGameName = gameTitle.take(80),
                            recentlyPlayedGames = recent,
                            updatedAt = null
                        )
                    ).await()
                }
            } catch (_: Exception) {
            }

            val savedSnap = roomRef.get().await()
            savedSnap.toObject(GameRoomDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) ?: doc
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, roomRef.path)
            throw e
        }
    }

    suspend fun joinGameRoomByIdOrCode(
        roomIdOrCode: String,
        playerProfile: UserProfile,
        preferredSlotIndex: Int = 0
    ): Result<GameRoomDoc> = runCatching {
        val uid = requireUserId()
        val cleanInput = roomIdOrCode.trim()
        require(cleanInput.isNotBlank()) { "Enter a valid Room ID or Code" }

        val targetDoc = if (cleanInput.startsWith("groom_")) {
            val snap = db.collection("game_rooms").document(cleanInput).get().await()
            snap.toObject(GameRoomDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        } else {
            val allSnap = db.collection("game_rooms")
                .whereEqualTo("appScope", "noise_live_public")
                .get()
                .await()
            allSnap.toObjects(GameRoomDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                .firstOrNull {
                    it.id.equals(cleanInput, ignoreCase = true) ||
                        it.roomCode.equals(cleanInput, ignoreCase = true)
                }
        } ?: error("Game room not found for '$cleanInput'")

        val roomRef = db.collection("game_rooms").document(targetDoc.id)
        try {
            db.runTransaction { tx ->
                val snap = tx.get(roomRef)
                val existing = snap.toObject(GameRoomDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    ?: error("Game room no longer exists")
                requireNotNull(existing.createdAt) { "Game room createdAt required" }

                if (existing.privateRoom &&
                    uid != existing.hostId &&
                    uid !in existing.participantIds &&
                    uid !in existing.invitedUserIds
                ) {
                    throw IllegalStateException("This is a private game session. An invitation from the host is required.")
                }

                val currentPlayers = existing.parsePlayers().toMutableList()
                val existingIdx = currentPlayers.indexOfFirst { it.userId == uid }
                if (existingIdx < 0 && currentPlayers.size >= existing.maxPlayers.toInt()) {
                    throw IllegalStateException("Game room is already full (${existing.maxPlayers}/${existing.maxPlayers})")
                }
                val usedSlots = currentPlayers.map { it.slotIndex }.toSet()
                val nextSlotIndex = when {
                    existingIdx >= 0 -> currentPlayers[existingIdx].slotIndex
                    preferredSlotIndex in 1..existing.maxPlayers.toInt() && preferredSlotIndex !in usedSlots -> preferredSlotIndex
                    else -> (1..existing.maxPlayers.toInt()).firstOrNull { it !in usedSlots } ?: (currentPlayers.size + 1)
                }
                val playerSlot = GamePlayerSlot(
                    slotIndex = nextSlotIndex,
                    slotNumber = nextSlotIndex,
                    userId = uid,
                    username = playerProfile.username.ifBlank { "player" },
                    displayName = playerProfile.displayName.ifBlank { playerProfile.username },
                    avatarKey = playerProfile.avatarKey.ifBlank { "P" },
                    team = if (nextSlotIndex % 2 == 1) 1 else 2,
                    ready = true,
                    score = if (existingIdx >= 0) currentPlayers[existingIdx].score else 0L,
                    connected = true,
                    micEnabled = true,
                    speaking = false
                )
                if (existingIdx >= 0) {
                    currentPlayers[existingIdx] = playerSlot
                } else {
                    currentPlayers.add(playerSlot)
                }
                val sortedPlayers = currentPlayers.sortedBy { it.slotIndex }

                val updatedParticipants = (existing.participantIds + uid).distinct().take(16)
                val updated = existing.copy(
                    participantIds = updatedParticipants,
                    invitedUserIds = existing.invitedUserIds - uid,
                    playersJson = GameRoomDoc.encodePlayers(sortedPlayers),
                    updatedAt = null
                )
                tx.set(roomRef, updated)
                updated.copy(createdAt = existing.createdAt)
            }.await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, roomRef.path)
            throw e
        }
    }

    suspend fun sendGameInvite(
        gameRoom: GameRoomDoc,
        senderProfile: UserProfile,
        targetUser: UserProfile,
        slotIndex: Int = 2
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val targetUid = targetUser.id.ifBlank { targetUser.userId }
        require(targetUid.isNotBlank() && targetUid != uid) { "Select another user to invite" }

        val inviteId = "ginv_${gameRoom.id.takeLast(8)}_${targetUid.take(8)}"
        val inviteRef = db.collection("game_invites").document(inviteId)
        val roomRef = db.collection("game_rooms").document(gameRoom.id)
        try {
            val safeSlot = slotIndex.coerceIn(2, gameRoom.maxPlayers.toInt().coerceAtLeast(2))
            val invite = GameInviteDoc(
                id = inviteId,
                appScope = "noise_live_public",
                gameRoomId = gameRoom.id,
                roomCode = gameRoom.roomCode,
                gameType = gameRoom.gameType,
                gameTitle = gameRoom.gameTitle,
                chatRoomId = gameRoom.chatRoomId,
                chatRoomTitle = gameRoom.chatRoomTitle,
                senderId = uid,
                senderUsername = senderProfile.username.ifBlank { "sebin_mr" }.take(60),
                senderAvatarKey = senderProfile.avatarKey.ifBlank { "S" }.take(500),
                targetUserId = targetUid,
                targetUsername = targetUser.username.ifBlank { "member" }.take(60),
                slotIndex = safeSlot,
                expiresAtMillis = System.currentTimeMillis() + 120_000L,
                status = "pending",
                createdAt = null,
                updatedAt = null
            )
            inviteRef.set(invite).await()

            val rSnap = roomRef.get().await()
            val existingRoom = rSnap.toObject(GameRoomDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            if (existingRoom?.createdAt != null && targetUid !in existingRoom.invitedUserIds) {
                roomRef.set(
                    existingRoom.copy(
                        invitedUserIds = (existingRoom.invitedUserIds + targetUid).distinct().take(30),
                        updatedAt = null
                    )
                ).await()
            }

            // Also create a real-time notification item for the invited user
            try {
                val notifId = "notif_game_${inviteId.takeLast(12)}"
                db.collection("notifications").document(notifId).set(
                    NotificationItem(
                        id = notifId,
                        userId = targetUid,
                        actorUid = uid,
                        actorUsername = senderProfile.username.ifBlank { "sebin_mr" }.take(40),
                        actorAvatarKey = senderProfile.avatarKey.ifBlank { "S" }.take(1200),
                        type = "game_invite",
                        message = "invited you to play ${gameRoom.gameTitle} (Slot $safeSlot • Code: ${gameRoom.roomCode})",
                        targetThumbKey = gameRoom.gameType.take(1200),
                        conversationId = "",
                        gameRoomId = gameRoom.id.take(128),
                        timeLabel = "Just now",
                        read = false,
                        createdAt = null,
                        updatedAt = null
                    )
                ).await()
            } catch (_: Exception) {
            }
            inviteId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, inviteRef.path)
            throw e
        }
    }

    suspend fun respondToGameInvite(
        invite: GameInviteDoc,
        accept: Boolean,
        responderProfile: UserProfile
    ): Result<GameRoomDoc?> = runCatching {
        val uid = requireUserId()
        val inviteRef = db.collection("game_invites").document(invite.id)
        try {
            val snap = inviteRef.get().await()
            val existing = snap.toObject(GameInviteDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            val isExpired = existing != null && existing.expiresAtMillis > 0L && System.currentTimeMillis() > existing.expiresAtMillis
            if (isExpired && accept) {
                if (existing?.createdAt != null) {
                    inviteRef.set(existing.copy(status = "expired", updatedAt = null)).await()
                }
                throw IllegalStateException("This game invitation has expired.")
            }
            if (existing?.createdAt != null) {
                inviteRef.set(
                    existing.copy(
                        status = if (accept) "accepted" else "declined",
                        updatedAt = null
                    )
                ).await()
            }
            if (accept) {
                joinGameRoomByIdOrCode(
                    roomIdOrCode = invite.gameRoomId,
                    playerProfile = responderProfile,
                    preferredSlotIndex = invite.slotIndex
                ).getOrThrow()
            } else {
                // Remove user from room's invitedUserIds if declined
                try {
                    val roomRef = db.collection("game_rooms").document(invite.gameRoomId)
                    val rSnap = roomRef.get().await()
                    val existingRoom = rSnap.toObject(GameRoomDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    if (existingRoom?.createdAt != null && uid in existingRoom.invitedUserIds) {
                        roomRef.set(
                            existingRoom.copy(
                                invitedUserIds = existingRoom.invitedUserIds - uid,
                                updatedAt = null
                            )
                        ).await()
                    }
                } catch (_: Exception) {
                }
                null
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, inviteRef.path)
            throw e
        }
    }

    fun observeOutgoingGameInvitesForRoom(gameRoomId: String, senderUid: String): Flow<List<GameInviteDoc>> {
        val path = "game_invites"
        return db.collection("game_invites")
            .whereEqualTo("senderId", senderUid)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(GameInviteDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .filter { it.gameRoomId == gameRoomId }
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                throw error
            }
    }

    suspend fun removePlayerFromGameRoom(
        gameRoomId: String,
        targetUserId: String
    ): Result<GameRoomDoc> = runCatching {
        val uid = requireUserId()
        val roomRef = db.collection("game_rooms").document(gameRoomId)
        try {
            db.runTransaction { tx ->
                val snap = tx.get(roomRef)
                val existing = snap.toObject(GameRoomDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    ?: error("Game room not found")
                requireNotNull(existing.createdAt) { "Game room createdAt required" }
                if (existing.hostId != uid && targetUserId != uid) {
                    throw IllegalStateException("Only the Game Room Owner can remove other players")
                }
                val nextPlayers = existing.parsePlayers().filterNot { it.userId == targetUserId }
                val nextParticipants = existing.participantIds - targetUserId
                val updated = existing.copy(
                    participantIds = if (nextParticipants.isEmpty()) listOf(existing.hostId) else nextParticipants,
                    playersJson = GameRoomDoc.encodePlayers(nextPlayers),
                    updatedAt = null
                )
                tx.set(roomRef, updated)
                updated.copy(createdAt = existing.createdAt)
            }.await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, roomRef.path)
            throw e
        }
    }

    suspend fun startGameRoomMatch(gameRoomId: String): Result<GameRoomDoc> = runCatching {
        val uid = requireUserId()
        val roomRef = db.collection("game_rooms").document(gameRoomId)
        try {
            db.runTransaction { tx ->
                val snap = tx.get(roomRef)
                val existing = snap.toObject(GameRoomDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    ?: error("Game room not found")
                requireNotNull(existing.createdAt) { "Game room createdAt required" }
                if (existing.hostId != uid) {
                    throw IllegalStateException("Only the Room Owner can start the game")
                }
                val players = existing.parsePlayers()
                val firstTurnUid = players.firstOrNull()?.userId ?: uid
                val updated = existing.copy(
                    status = "playing",
                    currentTurnUserId = firstTurnUid,
                    updatedAt = null
                )
                tx.set(roomRef, updated)
                updated.copy(createdAt = existing.createdAt)
            }.await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, roomRef.path)
            throw e
        }
    }

    suspend fun syncGameRoomState(
        gameRoomId: String,
        gameStateJson: String,
        nextTurnUserId: String,
        updatedPlayers: List<GamePlayerSlot>? = null,
        status: String? = null,
        winnerUserId: String? = null,
        winnerUsername: String? = null
    ): Result<GameRoomDoc> = runCatching {
        requireUserId()
        val roomRef = db.collection("game_rooms").document(gameRoomId)
        try {
            db.runTransaction { tx ->
                val snap = tx.get(roomRef)
                val existing = snap.toObject(GameRoomDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    ?: error("Game room not found")
                requireNotNull(existing.createdAt) { "Game room createdAt required" }
                val updated = existing.copy(
                    status = status ?: existing.status,
                    currentTurnUserId = nextTurnUserId.ifBlank { existing.currentTurnUserId },
                    winnerUserId = winnerUserId ?: existing.winnerUserId,
                    winnerUsername = winnerUsername ?: existing.winnerUsername,
                    playersJson = if (updatedPlayers != null) GameRoomDoc.encodePlayers(updatedPlayers) else existing.playersJson,
                    gameStateJson = gameStateJson.take(15_000),
                    updatedAt = null
                )
                tx.set(roomRef, updated)
                updated.copy(createdAt = existing.createdAt)
            }.await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, roomRef.path)
            throw e
        }
    }

    suspend fun endOrLeaveGameRoom(gameRoomId: String): Result<Unit> = runCatching {
        val uid = auth.currentUser?.uid ?: return@runCatching
        val roomRef = db.collection("game_rooms").document(gameRoomId)
        try {
            val snap = roomRef.get().await()
            val existing = snap.toObject(GameRoomDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) ?: return@runCatching
            if (existing.hostId == uid) {
                if (existing.createdAt != null) {
                    roomRef.set(
                        existing.copy(
                            status = "finished",
                            updatedAt = null
                        )
                    ).await()
                }
            } else {
                removePlayerFromGameRoom(gameRoomId, uid)
            }
        } catch (_: Exception) {
        }
    }

    /**
     * Server-validated match result recording: updates game_history, awards VIP coins,
     * and updates player rank, rating, wins, losses, and recentlyPlayedGames.
     */
    suspend fun submitVerifiedGameResult(
        gameRoom: GameRoomDoc,
        winnerProfile: UserProfile,
        runnerUpUsername: String,
        winnerScore: Long,
        runnerUpScore: Long,
        durationSec: Long
    ): Result<GameMatchResultDoc> = runCatching {
        val uid = requireUserId()
        val matchId = "gmatch_${gameRoom.id.takeLast(10)}_${System.currentTimeMillis() % 100000}"
        val matchRef = db.collection("game_history").document(matchId)
        val winnerUid = winnerProfile.userId.ifBlank { winnerProfile.id.ifBlank { uid } }
        val rewardCoins = gameRoom.prizePoolCoins.coerceAtLeast(200L)
        try {
            val resultDoc = GameMatchResultDoc(
                id = matchId,
                appScope = "noise_live_public",
                gameRoomId = gameRoom.id,
                gameType = gameRoom.gameType,
                gameTitle = gameRoom.gameTitle,
                chatRoomId = gameRoom.chatRoomId,
                winnerId = winnerUid,
                winnerUsername = winnerProfile.username.ifBlank { "sebin_mr" }.take(60),
                winnerAvatarKey = winnerProfile.avatarKey.ifBlank { "S" }.take(500),
                runnerUpUsername = runnerUpUsername.ifBlank { "Challenger" }.take(60),
                participantIds = (gameRoom.participantIds + uid).distinct().take(16),
                winnerScore = winnerScore.coerceIn(0L, 1_000_000L),
                runnerUpScore = runnerUpScore.coerceIn(0L, 1_000_000L),
                coinsAwarded = rewardCoins,
                durationSec = durationSec.coerceIn(1L, 86_400L),
                verifiedByServer = true,
                createdAt = null
            )
            matchRef.set(resultDoc).await()

            // Update current user's gaming stats
            val userRef = db.collection("users").document(uid)
            val uSnap = userRef.get().await()
            val existingUser = uSnap.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            if (existingUser?.createdAt != null) {
                val didWin = winnerUid == uid
                val nextWins = if (didWin) existingUser.gameWins + 1L else existingUser.gameWins
                val nextLosses = if (!didWin) existingUser.gameLosses + 1L else existingUser.gameLosses
                val nextPlayed = existingUser.gameMatchesPlayed + 1L
                val nextRating = if (didWin) existingUser.gameRating + 25L else (existingUser.gameRating - 10L).coerceAtLeast(800L)
                val nextRank = when {
                    nextRating >= 2000L -> "Grandmaster VIP"
                    nextRating >= 1600L -> "Diamond Legend"
                    nextRating >= 1350L -> "Gold Master"
                    nextRating >= 1150L -> "Silver Striker"
                    else -> "Rookie Pro"
                }
                val recent = (listOf(gameRoom.gameType) + existingUser.recentlyPlayedGames).distinct().take(10)
                userRef.set(
                    existingUser.copy(
                        coins = if (didWin) existingUser.coins + rewardCoins else existingUser.coins,
                        gameWins = nextWins,
                        gameLosses = nextLosses,
                        gameMatchesPlayed = nextPlayed,
                        gameRating = nextRating,
                        gameRankTitle = nextRank,
                        recentlyPlayedGames = recent,
                        currentGameName = "",
                        updatedAt = null
                    )
                ).await()
            }
            resultDoc
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, matchRef.path)
            throw e
        }
    }

    // =========================================================================
    // AUTOMATIC DISCOVERY & CONTENT IMPORT SYSTEM DELEGATES
    // =========================================================================

    fun observeDiscoveryItems(): Flow<List<DiscoveryContentItem>> =
        DiscoveryContentManager.observeDiscoveryItems(db)

    fun observeAllDiscoveryItemsForAdmin(): Flow<List<DiscoveryContentItem>> =
        DiscoveryContentManager.observeAllDiscoveryItemsForAdmin(db)

    fun observeDiscoveryGlobalConfig(): Flow<DiscoveryGlobalConfig> =
        DiscoveryContentManager.observeDiscoveryGlobalConfig(db)

    fun observeDiscoverySources(): Flow<List<DiscoverySourceConfig>> =
        DiscoveryContentManager.observeDiscoverySources(db)

    fun observeDiscoveryImportLogs(): Flow<List<DiscoveryImportLog>> =
        DiscoveryContentManager.observeDiscoveryImportLogs(db)

    suspend fun syncDiscoveryCategory(
        category: DiscoveryCategory,
        forceRefresh: Boolean = false
    ): Result<Int> = DiscoveryContentManager.syncCategoryIfNeeded(
        db = db,
        category = category,
        forceRefresh = forceRefresh
    )

    suspend fun toggleLikeDiscoveryItem(item: DiscoveryContentItem): Result<Boolean> =
        DiscoveryContentManager.toggleLikeDiscoveryItem(db, item)

    suspend fun toggleSaveDiscoveryItem(item: DiscoveryContentItem): Result<Boolean> =
        DiscoveryContentManager.toggleSaveDiscoveryItem(db, item)

    suspend fun recordDiscoveryItemShare(itemId: String) =
        DiscoveryContentManager.recordDiscoveryItemShare(db, itemId)

    suspend fun recordDiscoveryItemView(itemId: String) =
        DiscoveryContentManager.recordDiscoveryItemView(db, itemId)

    suspend fun setDiscoveryCategoryEnabled(categoryKey: String, enabled: Boolean): Result<Unit> =
        DiscoveryContentManager.setCategoryEnabled(db, categoryKey, enabled)

    suspend fun updateDiscoveryRefreshIntervalMinutes(intervalMinutes: Long): Result<Unit> =
        DiscoveryContentManager.updateRefreshIntervalMinutes(db, intervalMinutes)

    suspend fun addCustomDiscoverySource(
        categoryKey: String,
        sourceName: String,
        feedUrl: String,
        contentType: String
    ): Result<DiscoverySourceConfig> = DiscoveryContentManager.addCustomDiscoverySource(
        db = db,
        categoryKey = categoryKey,
        sourceName = sourceName,
        feedUrl = feedUrl,
        contentType = contentType
    )

    suspend fun toggleDiscoverySourceEnabled(
        source: DiscoverySourceConfig,
        enabled: Boolean
    ): Result<Unit> = DiscoveryContentManager.toggleDiscoverySourceEnabled(db, source, enabled)

    suspend fun deleteDiscoverySource(sourceId: String): Result<Unit> =
        DiscoveryContentManager.deleteDiscoverySource(db, sourceId)

    suspend fun moderateDiscoveryItem(
        itemId: String,
        newStatus: String,
        notes: String
    ): Result<Unit> = DiscoveryContentManager.moderateDiscoveryItem(db, itemId, newStatus, notes)

    suspend fun deleteDiscoveryItem(
        itemId: String,
        blockDomain: String = ""
    ): Result<Unit> = DiscoveryContentManager.deleteDiscoveryItem(db, itemId, blockDomain)

    suspend fun syncKeralaNewsCategory(
        keralaCategory: KeralaNewsCategory,
        forceRefresh: Boolean = false
    ): Result<Int> = DiscoveryContentManager.syncKeralaNewsIfNeeded(
        db = db,
        keralaCategory = keralaCategory,
        forceRefresh = forceRefresh
    )

    suspend fun setKeralaCategoryEnabled(
        keralaCategoryKey: String,
        enabled: Boolean
    ): Result<Unit> = DiscoveryContentManager.setKeralaCategoryEnabled(
        db = db,
        keralaCategoryKey = keralaCategoryKey,
        enabled = enabled
    )

    suspend fun updateKeralaAndReelsConfig(
        keralaNewsNotificationsEnabled: Boolean? = null,
        trendingReelsNotificationsEnabled: Boolean? = null,
        trendingAlgorithmMode: String? = null
    ): Result<Unit> = DiscoveryContentManager.updateKeralaAndReelsConfig(
        db = db,
        keralaNewsNotificationsEnabled = keralaNewsNotificationsEnabled,
        trendingReelsNotificationsEnabled = trendingReelsNotificationsEnabled,
        trendingAlgorithmMode = trendingAlgorithmMode
    )

    // =========================================================================
    // SUPER OWNER FULL CONTROL & OWNER CHATROOM MANAGEMENT DELEGATES
    // =========================================================================

    fun observeSuperOwnerRegistry(): Flow<SuperOwnerRegistryDoc> =
        SuperOwnerControlManager.observeSuperOwnerRegistry(db)

    fun observeAllChatroomsForOwner(): Flow<List<PartyRoom>> =
        SuperOwnerControlManager.observeAllChatroomsForOwner(db)

    fun observeOwnerAuditLogs(): Flow<List<OwnerAuditLogDoc>> =
        SuperOwnerControlManager.observeOwnerAuditLogs(db)

    fun observeContentReports(): Flow<List<ContentReportDoc>> =
        SuperOwnerControlManager.observeContentReports(db)

    suspend fun ownerDeleteSingleChatroom(roomId: String): Result<Unit> =
        SuperOwnerControlManager.deleteSingleChatroom(db, roomId)

    suspend fun ownerDeleteSelectedChatrooms(roomIds: List<String>): Result<Int> =
        SuperOwnerControlManager.ownerDeleteSelectedChatrooms(db, roomIds)

    suspend fun ownerDeleteAllChatrooms(): Result<Int> =
        SuperOwnerControlManager.ownerDeleteAllChatrooms(db)

    suspend fun ownerPurgeDemoAndDuplicateChatrooms(): Result<Int> =
        SuperOwnerControlManager.purgeDemoBuggedAndDuplicateChatrooms(db, logAudit = true)

    suspend fun ownerDeleteGameGroup(gameRoomId: String): Result<Unit> =
        SuperOwnerControlManager.ownerDeleteGameGroup(db, gameRoomId)

    suspend fun ownerUpdateUserAccountStatus(
        targetUser: UserProfile,
        newStatus: String,
        reason: String
    ): Result<Unit> = SuperOwnerControlManager.ownerUpdateUserAccountStatus(
        db = db,
        targetUser = targetUser,
        newStatus = newStatus,
        reason = reason
    )

    suspend fun submitContentReport(
        reporterUsername: String,
        targetType: String,
        targetId: String,
        targetOwnerUid: String,
        targetSummary: String,
        reason: String
    ): Result<String> = SuperOwnerControlManager.submitContentReport(
        db = db,
        reporterUsername = reporterUsername,
        targetType = targetType,
        targetId = targetId,
        targetOwnerUid = targetOwnerUid,
        targetSummary = targetSummary,
        reason = reason
    )

    suspend fun ownerResolveReport(
        report: ContentReportDoc,
        newStatus: String,
        resolutionNotes: String,
        deleteTargetContent: Boolean
    ): Result<Unit> = SuperOwnerControlManager.ownerResolveReport(
        db = db,
        report = report,
        newStatus = newStatus,
        resolutionNotes = resolutionNotes,
        deleteTargetContent = deleteTargetContent
    )

    // =========================================================================
    // CENTRAL AI ADMIN & LIVE UPDATE SYSTEM DELEGATES
    // =========================================================================

    fun observeProductionAppConfig(): Flow<CentralAppConfig> =
        CentralAiAdminManager.observeProductionAppConfig(db)

    fun observeDynamicPages(): Flow<List<DynamicAppPage>> =
        CentralAiAdminManager.observeDynamicPages(db)

    fun observeCentralVersionHistory(): Flow<List<CentralVersionDoc>> =
        CentralAiAdminManager.observeVersionHistory(db)

    fun observeCentralConfigBackups(): Flow<List<CentralConfigBackupDoc>> =
        CentralAiAdminManager.observeConfigBackups(db)

    fun observeAiCommandLogs(): Flow<List<AiCommandExecutionLogDoc>> =
        CentralAiAdminManager.observeAiCommandLogs(db)

    suspend fun generateAiAdminProposal(
        prompt: String,
        currentConfig: CentralAppConfig,
        existingPages: List<DynamicAppPage>
    ): Result<AiAdminProposal> = CentralAiAdminManager.generateAiAdminProposal(
        prompt = prompt,
        currentConfig = currentConfig,
        existingPages = existingPages
    )

    suspend fun publishAiProposalToAllUsers(
        proposal: AiAdminProposal,
        currentProductionConfig: CentralAppConfig,
        currentDynamicPages: List<DynamicAppPage>
    ): Result<CentralAppConfig> = CentralAiAdminManager.publishProposalToAllUsers(
        db = db,
        proposal = proposal,
        currentProductionConfig = currentProductionConfig,
        currentDynamicPages = currentDynamicPages
    )

    suspend fun createCentralBackupSnapshot(
        currentConfig: CentralAppConfig,
        currentPages: List<DynamicAppPage>,
        reason: String
    ): Result<CentralConfigBackupDoc> = CentralAiAdminManager.createBackupSnapshot(
        db = db,
        currentConfig = currentConfig,
        currentPages = currentPages,
        reason = reason
    )

    suspend fun rollbackCentralConfigToBackup(
        backup: CentralConfigBackupDoc,
        currentProductionConfig: CentralAppConfig,
        currentDynamicPages: List<DynamicAppPage>
    ): Result<CentralAppConfig> = CentralAiAdminManager.rollbackToBackup(
        db = db,
        backup = backup,
        currentProductionConfig = currentProductionConfig,
        currentDynamicPages = currentDynamicPages
    )

    suspend fun saveDynamicPageAndPublish(
        page: DynamicAppPage,
        currentProductionConfig: CentralAppConfig,
        currentDynamicPages: List<DynamicAppPage>
    ): Result<DynamicAppPage> = CentralAiAdminManager.saveDynamicPageAndPublish(
        db = db,
        page = page,
        currentProductionConfig = currentProductionConfig,
        currentDynamicPages = currentDynamicPages
    )

    suspend fun deleteDynamicPageAndPublish(
        pageKey: String,
        currentProductionConfig: CentralAppConfig,
        currentDynamicPages: List<DynamicAppPage>
    ): Result<Unit> = CentralAiAdminManager.deleteDynamicPageAndPublish(
        db = db,
        pageKey = pageKey,
        currentProductionConfig = currentProductionConfig,
        currentDynamicPages = currentDynamicPages
    )

    // =========================================================================
    // OWNER ONLY GEMINI AI DEVELOPER STUDIO DELEGATES
    // =========================================================================

    fun observeDeveloperWorkspaceFiles(): Flow<List<ProjectSourceFileDoc>> =
        GeminiDeveloperStudioManager.observeWorkspaceFiles(db)

    fun observeDeveloperChatMessages(): Flow<List<AiDeveloperChatMessageDoc>> =
        GeminiDeveloperStudioManager.observeDeveloperChat(db)

    fun observeDeveloperBugFindings(): Flow<List<BugScanFindingDoc>> =
        GeminiDeveloperStudioManager.observeBugFindings(db)

    fun observeGitHubConfig(): Flow<GitHubIntegrationConfigDoc> =
        GeminiDeveloperStudioManager.observeGitHubConfig(db)

    fun observeGitHubBranches(): Flow<List<GitHubBranchDoc>> =
        GeminiDeveloperStudioManager.observeGitHubBranches(db)

    fun observeGitHubCommits(): Flow<List<GitHubCommitDoc>> =
        GeminiDeveloperStudioManager.observeGitHubCommits(db)

    fun observeDeveloperDeployments(): Flow<List<DeveloperDeploymentDoc>> =
        GeminiDeveloperStudioManager.observeDeployments(db)

    suspend fun sendDeveloperChatPrompt(
        prompt: String,
        workspaceFiles: List<ProjectSourceFileDoc>,
        currentConfig: CentralAppConfig
    ): Result<AiDeveloperChatMessageDoc> = GeminiDeveloperStudioManager.sendDeveloperChatPrompt(
        db = db,
        ownerPrompt = prompt,
        workspaceFiles = workspaceFiles,
        currentConfig = currentConfig
    )

    suspend fun generateDeveloperCodePatch(
        prompt: String,
        workspaceFiles: List<ProjectSourceFileDoc>,
        currentConfig: CentralAppConfig,
        existingPages: List<DynamicAppPage>
    ): Result<AiCodePatchProposal> = GeminiDeveloperStudioManager.generateCompleteCodePatch(
        prompt = prompt,
        workspaceFiles = workspaceFiles,
        currentConfig = currentConfig,
        existingPages = existingPages
    )

    suspend fun runDeveloperBugScanner(
        workspaceFiles: List<ProjectSourceFileDoc>
    ): Result<List<BugScanFindingDoc>> = GeminiDeveloperStudioManager.runAutomaticBugScanner(
        db = db,
        workspaceFiles = workspaceFiles
    )

    suspend fun approveAndDeployBugFix(
        finding: BugScanFindingDoc,
        currentProductionConfig: CentralAppConfig,
        currentDynamicPages: List<DynamicAppPage>
    ): Result<DeveloperDeploymentDoc> = GeminiDeveloperStudioManager.approveAndDeployBugFix(
        db = db,
        finding = finding,
        currentProductionConfig = currentProductionConfig,
        currentDynamicPages = currentDynamicPages
    )

    suspend fun saveEditedFileToDevBranch(
        branchName: String,
        filePath: String,
        newCode: String,
        previousCode: String,
        commitMessage: String,
        personalAccessToken: String = "",
        githubConfig: GitHubIntegrationConfigDoc = GitHubIntegrationConfigDoc()
    ): Result<GitHubCommitDoc> = GeminiDeveloperStudioManager.createDevBranchAndCommitCode(
        db = db,
        branchName = branchName,
        filePath = filePath,
        newCode = newCode,
        previousCode = previousCode,
        commitMessage = commitMessage,
        personalAccessToken = personalAccessToken,
        githubConfig = githubConfig
    )

    suspend fun approveMergeAndDeployCodePatch(
        proposal: AiCodePatchProposal,
        currentProductionConfig: CentralAppConfig,
        currentDynamicPages: List<DynamicAppPage>
    ): Result<DeveloperDeploymentDoc> = GeminiDeveloperStudioManager.approveMergeAndDeployProposal(
        db = db,
        proposal = proposal,
        currentProductionConfig = currentProductionConfig,
        currentDynamicPages = currentDynamicPages
    )

    suspend fun saveGitHubConfiguration(
        repoOwner: String,
        repoName: String,
        defaultBranch: String,
        activeDevBranch: String,
        personalAccessToken: String
    ): Result<GitHubIntegrationConfigDoc> = GeminiDeveloperStudioManager.saveGitHubConfiguration(
        db = db,
        repoOwner = repoOwner,
        repoName = repoName,
        defaultBranch = defaultBranch,
        activeDevBranch = activeDevBranch,
        personalAccessToken = personalAccessToken
    )

    suspend fun rollbackCommitChange(
        commit: GitHubCommitDoc
    ): Result<ProjectSourceFileDoc> = GeminiDeveloperStudioManager.rollbackCommitChange(
        db = db,
        commit = commit
    )
}



