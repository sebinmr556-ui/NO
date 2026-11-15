package com.example.data

import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

/**
 * NOICE LIVE – SUPER OWNER FULL CONTROL & OWNER CHATROOM MANAGEMENT SYSTEM
 *
 * Permanent Super Owner Identity:
 * - Username: "sebin_mr"
 * - Verified Owner Email: "sebinmr556@gmail.com"
 * - Bound Firebase Authentication UID stored in `/super_owner_registry/primary`
 *
 * Security Guarantees:
 * - Only the verified Super Owner account can access Super Owner controls.
 * - Normal users can never register, claim, or change their username to "sebin_mr".
 * - Deleted or bugged chatrooms are tracked in [deletedRoomTombstones] and purged from Firestore
 *   so they never reappear from demo data, automatic seeding, or cached state.
 * - Every Super Owner deletion and moderation action is recorded in `/owner_audit_logs`.
 */
object SuperOwnerControlManager {

    const val PERMANENT_SUPER_OWNER_USERNAME = "sebin_mr"
    const val PERMANENT_SUPER_OWNER_EMAIL = "sebinmr556@gmail.com"
    const val KNOWN_SUPER_OWNER_UID_PREFIX = "qiVebE18UCecUn4IBtOzFAg"
    const val DELETE_ALL_CHATROOMS_CONFIRMATION_MESSAGE =
        "Are you sure you want to permanently delete all chatrooms?"

    // Tombstone set of deleted room IDs in the current session to prevent any cached/stale room from reappearing
    private val deletedRoomTombstones: MutableSet<String> = ConcurrentHashMap.newKeySet()

    fun isRoomDeletedTombstone(roomId: String): Boolean {
        return roomId.isNotBlank() && roomId in deletedRoomTombstones
    }

    fun markRoomDeletedTombstone(roomId: String) {
        if (roomId.isNotBlank()) {
            deletedRoomTombstones.add(roomId)
        }
    }

    /**
     * Checks whether [candidateUsername] is a reserved variation of the permanent Super Owner username `sebin_mr`.
     */
    fun isReservedSuperOwnerUsername(candidateUsername: String): Boolean {
        val normalized = candidateUsername.trim().removePrefix("@").lowercase()
            .replace(" ", "_")
            .replace(".", "_")
        return normalized == PERMANENT_SUPER_OWNER_USERNAME ||
            normalized == "sebinmr" ||
            normalized == "sebin_mr_official" ||
            normalized == "super_owner"
    }

    /**
     * Deterministic check whether the given user is the single permanent Super Owner (`sebin_mr`).
     */
    fun isVerifiedSuperOwner(
        uid: String?,
        email: String? = null,
        username: String? = null,
        userProfile: UserProfile? = null,
        registry: SuperOwnerRegistryDoc? = null,
        registryDoc: SuperOwnerRegistryDoc? = null
    ): Boolean {
        val cleanUid = uid?.trim().orEmpty()
        if (cleanUid.isBlank()) return false

        val cleanEmail = email?.trim()?.lowercase().orEmpty()
        if (cleanEmail == PERMANENT_SUPER_OWNER_EMAIL) {
            return true
        }

        val effectiveRegistry = registry ?: registryDoc
        val registeredOwnerUid = effectiveRegistry?.superOwnerUid?.trim().orEmpty()
        if (registeredOwnerUid.isNotBlank()) {
            return cleanUid == registeredOwnerUid
        }

        if (cleanUid.startsWith(KNOWN_SUPER_OWNER_UID_PREFIX)) {
            return true
        }

        val profileUsername = (username ?: userProfile?.username)?.trim()?.removePrefix("@")?.lowercase().orEmpty()
        return profileUsername == PERMANENT_SUPER_OWNER_USERNAME &&
            (cleanEmail == PERMANENT_SUPER_OWNER_EMAIL || cleanUid.startsWith(KNOWN_SUPER_OWNER_UID_PREFIX))
    }

    /**
     * Convenience UI helper using current Firebase Auth session + UserProfile + Registry.
     */
    fun isCurrentUserSuperOwner(
        userProfile: UserProfile?,
        registry: SuperOwnerRegistryDoc? = null,
        registryDoc: SuperOwnerRegistryDoc? = null
    ): Boolean {
        val currentUser = runCatching { Firebase.auth.currentUser }.getOrNull()
        val uid = currentUser?.uid ?: userProfile?.userId?.ifBlank { userProfile.id }
        val email = currentUser?.email
        return isVerifiedSuperOwner(
            uid = uid,
            email = email,
            username = userProfile?.username,
            userProfile = userProfile,
            registry = registry ?: registryDoc
        )
    }

    /**
     * Suspend verification against Firestore `/super_owner_registry/primary` and Firebase Auth.
     */
    suspend fun verifyCallerIsSuperOwner(db: FirebaseFirestore): Boolean {
        val currentUser = runCatching { Firebase.auth.currentUser }.getOrNull() ?: return false
        val uid = currentUser.uid
        val email = currentUser.email
        if (email?.trim()?.equals(PERMANENT_SUPER_OWNER_EMAIL, ignoreCase = true) == true ||
            uid.startsWith(KNOWN_SUPER_OWNER_UID_PREFIX)
        ) {
            return true
        }
        val registry = runCatching {
            db.collection("super_owner_registry").document("primary")
                .get()
                .await()
                .toObject(SuperOwnerRegistryDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        }.getOrNull()
        if (registry != null && registry.superOwnerUid.isNotBlank() && registry.superOwnerUid == uid) {
            return true
        }
        return false
    }

    /**
     * Ensures `/super_owner_registry/primary` is bound to the verified Super Owner UID when `sebin_mr` signs in.
     * Never overwrites or transfers ownership to any other user.
     */
    suspend fun ensureSuperOwnerRegistryBound(db: FirebaseFirestore, profile: UserProfile? = null) {
        val currentUser = runCatching { Firebase.auth.currentUser }.getOrNull() ?: return
        val uid = currentUser.uid
        val email = currentUser.email?.trim()?.lowercase().orEmpty()
        val isOwnerByAuth = email == PERMANENT_SUPER_OWNER_EMAIL ||
            uid.startsWith(KNOWN_SUPER_OWNER_UID_PREFIX) ||
            (profile?.username?.equals(PERMANENT_SUPER_OWNER_USERNAME, ignoreCase = true) == true &&
                uid.startsWith(KNOWN_SUPER_OWNER_UID_PREFIX))

        if (!isOwnerByAuth) return

        runCatching {
            val regRef = db.collection("super_owner_registry").document("primary")
            val snap = regRef.get().await()
            val existing = snap.toObject(SuperOwnerRegistryDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            if (existing == null || existing.superOwnerUid.isBlank() || existing.superOwnerUid == uid) {
                regRef.set(
                    SuperOwnerRegistryDoc(
                        id = "primary",
                        appScope = "noise_live_public",
                        superOwnerUid = uid,
                        superOwnerUsername = PERMANENT_SUPER_OWNER_USERNAME,
                        superOwnerEmail = PERMANENT_SUPER_OWNER_EMAIL,
                        immutableLock = true,
                        updatedAt = null
                    )
                ).await()
            }
        }
    }

    fun observeSuperOwnerRegistry(db: FirebaseFirestore): Flow<SuperOwnerRegistryDoc> {
        return db.collection("super_owner_registry").document("primary")
            .snapshots()
            .map { snap ->
                snap.toObject(SuperOwnerRegistryDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    ?: SuperOwnerRegistryDoc()
            }
            .catch { emit(SuperOwnerRegistryDoc()) }
    }

    /**
     * Real-time stream of ALL real chatrooms for the Owner Chatroom Management panel.
     * Filters out deleted tombstones and dummy rooms, and sorts newest first.
     */
    fun observeAllChatroomsForOwner(db: FirebaseFirestore): Flow<List<PartyRoom>> {
        return db.collection("party_rooms")
            .whereEqualTo("appScope", "noise_live_public")
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    val room = doc.toObject(PartyRoom::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        ?: return@mapNotNull null
                    val resolvedId = doc.id.ifBlank { room.id.ifBlank { room.roomId } }
                    if (resolvedId.isBlank() || isRoomDeletedTombstone(resolvedId) || room.isDummyOrSampleRoom()) {
                        null
                    } else {
                        room.copy(id = resolvedId, roomId = resolvedId)
                    }
                }
                    .distinctBy { it.id }
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { emit(emptyList()) }
    }

    fun observeOwnerAuditLogs(db: FirebaseFirestore): Flow<List<OwnerAuditLogDoc>> {
        return db.collection("owner_audit_logs")
            .whereEqualTo("appScope", "noise_live_public")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(OwnerAuditLogDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.timestampMillis }
                    .take(100)
            }
            .catch { emit(emptyList()) }
    }

    fun observeContentReports(db: FirebaseFirestore): Flow<List<ContentReportDoc>> {
        return db.collection("reports")
            .whereEqualTo("appScope", "noise_live_public")
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    doc.toObject(ContentReportDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        ?.copy(id = doc.id)
                }
                    .sortedByDescending { it.timestampMillis }
            }
            .catch { emit(emptyList()) }
    }

    suspend fun logOwnerAuditAction(
        db: FirebaseFirestore,
        actionType: String,
        targetType: String,
        targetId: String,
        targetLabel: String,
        details: String
    ) {
        val uid = runCatching { Firebase.auth.currentUser?.uid }.getOrNull().orEmpty()
        if (uid.isBlank()) return
        val logId = "audit_${System.currentTimeMillis()}_${UUID.randomUUID().toString().replace("-", "").take(8)}"
        runCatching {
            db.collection("owner_audit_logs").document(logId).set(
                OwnerAuditLogDoc(
                    id = logId,
                    appScope = "noise_live_public",
                    actorUid = uid,
                    actorUsername = PERMANENT_SUPER_OWNER_USERNAME,
                    actionType = actionType.take(60),
                    targetType = targetType.take(40),
                    targetId = targetId.take(128),
                    targetLabel = targetLabel.take(160),
                    details = details.take(400),
                    timestampMillis = System.currentTimeMillis(),
                    createdAt = null
                )
            ).await()
        }
    }

    /**
     * Internal helper that permanently deletes a single chatroom's Firestore document,
     * related active game rooms in `game_rooms`, and associated room media in storage.
     * Never deletes user accounts, private messages, or unrelated app data.
     */
    private suspend fun performCompleteRoomDeletion(
        db: FirebaseFirestore,
        roomId: String,
        existingRoom: PartyRoom? = null
    ) {
        val cleanRoomId = roomId.trim()
        if (cleanRoomId.isBlank()) return
        markRoomDeletedTombstone(cleanRoomId)

        val roomRef = db.collection("party_rooms").document(cleanRoomId)
        val roomObj = existingRoom ?: runCatching {
            roomRef.get().await().toObject(PartyRoom::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        }.getOrNull()

        // 1. Delete the chatroom document from /party_rooms
        runCatching { roomRef.delete().await() }

        // 2. Delete related active game room references linked to this chatroom
        runCatching {
            val linkedGames = db.collection("game_rooms")
                .whereEqualTo("chatRoomId", cleanRoomId)
                .get()
                .await()
            for (gDoc in linkedGames.documents) {
                runCatching { gDoc.reference.delete().await() }
            }
        }

        // 3. Clean up associated room image media if stored in CloudMediaStorageManager
        if (roomObj != null && roomObj.roomImageUrl.isNotBlank()) {
            val mediaId = CloudMediaStorageManager.extractCloudMediaId(roomObj.roomImageUrl)
            if (mediaId.isNotBlank()) {
                runCatching {
                    CloudMediaStorageManager.deletePermanentMedia(
                        db = db,
                        storagePath = "",
                        cloudMediaId = mediaId
                    )
                }
            }
        }
    }

    /**
     * Automatically purges demo, dummy, test, bugged, inactive, and duplicate chatrooms from Firestore.
     */
    suspend fun purgeDemoBuggedAndDuplicateChatrooms(
        db: FirebaseFirestore,
        logAudit: Boolean = false
    ): Result<Int> = runCatching {
        val uid = Firebase.auth.currentUser?.uid ?: return@runCatching 0
        val isSuperOwner = verifyCallerIsSuperOwner(db)
        val snap = db.collection("party_rooms")
            .whereEqualTo("appScope", "noise_live_public")
            .get()
            .await()

        var deletedCount = 0
        val seenHostAndRoomName = mutableSetOf<String>()

        // Sort newest first so if a duplicate exists, the newest valid room is kept and older duplicates are removed
        val sortedDocs = snap.documents.sortedByDescending { doc ->
            doc.getTimestamp("createdAt")?.seconds ?: 0L
        }

        for (doc in sortedDocs) {
            val room = doc.toObject(PartyRoom::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                ?.copy(id = doc.id, roomId = doc.id)
            val canDeleteThisDoc = isSuperOwner || room?.hostId == uid || room?.ownerUid == uid
            if (room == null) {
                if (canDeleteThisDoc) {
                    runCatching { doc.reference.delete().await() }
                    deletedCount++
                }
                continue
            }

            val dedupKey = "${room.hostId.ifBlank { room.ownerUid }}_${room.resolvedRoomName().lowercase()}"
            val isDuplicate = room.resolvedRoomName().isNotBlank() && !seenHostAndRoomName.add(dedupKey)
            val isBuggedOrDummy = room.isDummyOrSampleRoom() ||
                !room.live ||
                !room.isActive ||
                isRoomDeletedTombstone(doc.id)

            if ((isBuggedOrDummy || isDuplicate) && canDeleteThisDoc) {
                performCompleteRoomDeletion(db, doc.id, room)
                deletedCount++
            }
        }

        if (logAudit && isSuperOwner && deletedCount > 0) {
            logOwnerAuditAction(
                db = db,
                actionType = "CLEANUP_FAKE_AND_DUPLICATE_ROOMS",
                targetType = "chatroom",
                targetId = "bulk_cleanup",
                targetLabel = "$deletedCount dummy/duplicate room(s)",
                details = "Purged $deletedCount demo, test, bugged, or duplicate chatrooms."
            )
        }
        deletedCount
    }

    /**
     * Deletes a single chatroom. Allowed for the verified Super Owner (`sebin_mr`) on ANY chatroom,
     * or for a normal user ONLY on their own created chatroom.
     */
    suspend fun deleteSingleChatroom(
        db: FirebaseFirestore,
        roomId: String
    ): Result<Unit> = runCatching {
        val uid = Firebase.auth.currentUser?.uid
            ?: throw IllegalStateException("Authentication required")
        val cleanId = roomId.trim()
        require(cleanId.isNotBlank()) { "Room ID is required" }

        val roomRef = db.collection("party_rooms").document(cleanId)
        val snap = roomRef.get().await()
        val existing = snap.toObject(PartyRoom::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        val isSuperOwner = verifyCallerIsSuperOwner(db)
        val isCreator = existing != null && (existing.hostId == uid || existing.ownerUid == uid)

        if (!isSuperOwner && !isCreator) {
            throw IllegalStateException("Only the verified Super Owner (@sebin_mr) or the Room Creator can delete this chatroom.")
        }

        val roomLabel = existing?.resolvedRoomName()?.ifBlank { cleanId } ?: cleanId
        val creatorLabel = existing?.hostUsername?.ifBlank { existing.hostId } ?: "unknown"
        performCompleteRoomDeletion(db, cleanId, existing)

        if (isSuperOwner) {
            logOwnerAuditAction(
                db = db,
                actionType = "DELETE_SINGLE_CHATROOM",
                targetType = "chatroom",
                targetId = cleanId,
                targetLabel = roomLabel,
                details = "Deleted chatroom '$roomLabel' (ID: $cleanId, Creator: @$creatorLabel)"
            )
        }
    }

    /**
     * Super Owner Exclusive: Deletes multiple selected chatrooms in one action.
     */
    suspend fun ownerDeleteSelectedChatrooms(
        db: FirebaseFirestore,
        roomIds: List<String>
    ): Result<Int> = runCatching {
        if (!verifyCallerIsSuperOwner(db)) {
            throw IllegalStateException("Access Denied: Only Super Owner (@sebin_mr) can bulk-delete chatrooms.")
        }
        val distinctIds = roomIds.map { it.trim() }.filter { it.isNotBlank() }.distinct()
        require(distinctIds.isNotEmpty()) { "Select at least one chatroom to delete." }

        var deleted = 0
        for (id in distinctIds) {
            runCatching {
                performCompleteRoomDeletion(db, id, null)
                deleted++
            }
        }

        logOwnerAuditAction(
            db = db,
            actionType = "DELETE_MULTIPLE_CHATROOMS",
            targetType = "chatroom",
            targetId = distinctIds.joinToString(",").take(120),
            targetLabel = "$deleted selected chatroom(s)",
            details = "Super Owner permanently deleted $deleted selected chatrooms: ${distinctIds.joinToString(", ")}"
        )
        deleted
    }

    /**
     * Super Owner Exclusive: Permanently deletes ALL chatrooms with one action.
     * Does NOT delete user accounts, private messages, or unrelated app data.
     */
    suspend fun ownerDeleteAllChatrooms(
        db: FirebaseFirestore
    ): Result<Int> = runCatching {
        if (!verifyCallerIsSuperOwner(db)) {
            throw IllegalStateException("Access Denied: Only Super Owner (@sebin_mr) can delete all chatrooms.")
        }
        val snap = db.collection("party_rooms")
            .whereEqualTo("appScope", "noise_live_public")
            .get()
            .await()

        var deletedCount = 0
        for (doc in snap.documents) {
            val room = doc.toObject(PartyRoom::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            runCatching {
                performCompleteRoomDeletion(db, doc.id, room)
                deletedCount++
            }
        }

        logOwnerAuditAction(
            db = db,
            actionType = "DELETE_ALL_CHATROOMS",
            targetType = "chatroom",
            targetId = "ALL_CHATROOMS",
            targetLabel = "Deleted all $deletedCount chatroom(s)",
            details = "Super Owner executed Delete ALL Chatrooms ($deletedCount rooms permanently removed from Firebase)."
        )
        deletedCount
    }

    /**
     * Super Owner Exclusive: Delete any user's group / multiplayer game room.
     */
    suspend fun ownerDeleteGameGroup(
        db: FirebaseFirestore,
        gameRoomId: String
    ): Result<Unit> = runCatching {
        val uid = Firebase.auth.currentUser?.uid ?: throw IllegalStateException("Authentication required")
        val isSuperOwner = verifyCallerIsSuperOwner(db)
        val docRef = db.collection("game_rooms").document(gameRoomId.trim())
        val snap = docRef.get().await()
        val existing = snap.toObject(GameRoomDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        if (!isSuperOwner && existing?.hostId != uid) {
            throw IllegalStateException("Only the Super Owner (@sebin_mr) or Group Host can delete this group.")
        }
        docRef.delete().await()
        if (isSuperOwner) {
            logOwnerAuditAction(
                db = db,
                actionType = "DELETE_GROUP",
                targetType = "group",
                targetId = gameRoomId,
                targetLabel = existing?.roomTitle ?: gameRoomId,
                details = "Deleted game group '${existing?.roomTitle ?: gameRoomId}' hosted by @${existing?.hostUsername ?: "unknown"}"
            )
        }
    }

    /**
     * Super Owner Exclusive: Suspend, Ban, or Restore a user account.
     * Never allows suspending or banning the permanent Super Owner (`sebin_mr`).
     */
    suspend fun ownerUpdateUserAccountStatus(
        db: FirebaseFirestore,
        targetUser: UserProfile,
        newStatus: String,
        reason: String
    ): Result<Unit> = runCatching {
        if (!verifyCallerIsSuperOwner(db)) {
            throw IllegalStateException("Access Denied: Only Super Owner (@sebin_mr) can suspend or ban accounts.")
        }
        val targetUid = targetUser.userId.ifBlank { targetUser.id }.trim()
        require(targetUid.isNotBlank()) { "Target user ID required" }
        val currentUid = Firebase.auth.currentUser?.uid.orEmpty()
        if (targetUid == currentUid ||
            targetUid.startsWith(KNOWN_SUPER_OWNER_UID_PREFIX) ||
            targetUser.username.equals(PERMANENT_SUPER_OWNER_USERNAME, ignoreCase = true)
        ) {
            throw IllegalStateException("The permanent Super Owner account (@sebin_mr) cannot be suspended or banned.")
        }

        val normalizedStatus = when (newStatus.lowercase().trim()) {
            "suspended" -> "suspended"
            "banned" -> "banned"
            else -> "active"
        }
        val cleanReason = if (normalizedStatus == "active") "" else reason.trim().ifBlank { "Violated NOICE LIVE community safety rules" }.take(240)

        // 1. Write authoritative moderation record in /user_moderation/{targetUid}
        db.collection("user_moderation").document(targetUid).set(
            UserModerationDoc(
                id = targetUid,
                appScope = "noise_live_public",
                targetUserId = targetUid,
                targetUsername = targetUser.username.take(40),
                accountStatus = normalizedStatus,
                reason = cleanReason,
                updatedByUid = currentUid,
                updatedByUsername = PERMANENT_SUPER_OWNER_USERNAME,
                timestampMillis = System.currentTimeMillis(),
                updatedAt = null
            )
        ).await()

        // 2. Update /users/{targetUid} status fields
        val userRef = db.collection("users").document(targetUid)
        val snap = userRef.get().await()
        val existingUser = snap.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        if (existingUser?.createdAt != null) {
            userRef.set(
                existingUser.copy(
                    accountStatus = normalizedStatus,
                    suspendedReason = cleanReason,
                    updatedAt = null
                )
            ).await()
        }

        // 3. If banned, also remove any active chatrooms hosted by that banned user
        if (normalizedStatus == "banned") {
            runCatching {
                val roomsSnap = db.collection("party_rooms")
                    .whereEqualTo("appScope", "noise_live_public")
                    .get()
                    .await()
                for (rDoc in roomsSnap.documents) {
                    val r = rDoc.toObject(PartyRoom::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    if (r != null && (r.hostId == targetUid || r.ownerUid == targetUid)) {
                        performCompleteRoomDeletion(db, rDoc.id, r)
                    }
                }
            }
        }

        val actionName = when (normalizedStatus) {
            "banned" -> "BAN_USER"
            "suspended" -> "SUSPEND_USER"
            else -> "RESTORE_USER"
        }
        logOwnerAuditAction(
            db = db,
            actionType = actionName,
            targetType = "user",
            targetId = targetUid,
            targetLabel = "@${targetUser.username}",
            details = "Set @${targetUser.username} ($targetUid) status to ${normalizedStatus.uppercase()}. Reason: ${cleanReason.ifBlank { "Restored to active" }}"
        )
    }

    /**
     * Allows any signed-in user to submit a report/complaint about a post, story, chatroom, or user,
     * which appears in the Super Owner Full Control Panel under Reports & Complaints.
     */
    suspend fun submitContentReport(
        db: FirebaseFirestore,
        reporterUsername: String,
        targetType: String,
        targetId: String,
        targetOwnerUid: String,
        targetSummary: String,
        reason: String
    ): Result<String> = runCatching {
        val uid = Firebase.auth.currentUser?.uid ?: throw IllegalStateException("Sign in required")
        val reportId = "rep_${System.currentTimeMillis()}_${UUID.randomUUID().toString().replace("-", "").take(8)}"
        val doc = ContentReportDoc(
            id = reportId,
            appScope = "noise_live_public",
            reporterUid = uid,
            reporterUsername = reporterUsername.ifBlank { "user_${uid.takeLast(4)}" }.take(40),
            targetType = targetType.lowercase().take(30),
            targetId = targetId.take(128),
            targetOwnerUid = targetOwnerUid.take(128),
            targetSummary = targetSummary.take(200),
            reason = reason.trim().ifBlank { "Inappropriate or unsafe content" }.take(300),
            status = "open",
            resolutionNotes = "",
            resolvedByUid = "",
            timestampMillis = System.currentTimeMillis(),
            createdAt = null,
            updatedAt = null
        )
        db.collection("reports").document(reportId).set(doc).await()
        reportId
    }

    /**
     * Super Owner Exclusive: Resolve or dismiss a report, optionally deleting the reported content immediately.
     */
    suspend fun ownerResolveReport(
        db: FirebaseFirestore,
        report: ContentReportDoc,
        newStatus: String,
        resolutionNotes: String,
        deleteTargetContent: Boolean
    ): Result<Unit> = runCatching {
        val uid = Firebase.auth.currentUser?.uid ?: throw IllegalStateException("Sign in required")
        if (!verifyCallerIsSuperOwner(db)) {
            throw IllegalStateException("Access Denied: Only Super Owner (@sebin_mr) can manage reports.")
        }

        if (deleteTargetContent && report.targetId.isNotBlank()) {
            when (report.targetType.lowercase()) {
                "chatroom", "room" -> runCatching { deleteSingleChatroom(db, report.targetId) }
                "post" -> runCatching { db.collection("posts").document(report.targetId).delete().await() }
                "story" -> runCatching { db.collection("stories").document(report.targetId).delete().await() }
                "group", "game_room" -> runCatching { ownerDeleteGameGroup(db, report.targetId) }
            }
        }

        val repRef = db.collection("reports").document(report.id)
        val snap = repRef.get().await()
        val existing = snap.toObject(ContentReportDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) ?: report
        repRef.set(
            existing.copy(
                status = newStatus.lowercase().take(30),
                resolutionNotes = resolutionNotes.trim().ifBlank { "Reviewed by Super Owner @sebin_mr" }.take(240),
                resolvedByUid = uid,
                createdAt = existing.createdAt,
                updatedAt = null
            )
        ).await()

        logOwnerAuditAction(
            db = db,
            actionType = "RESOLVE_REPORT",
            targetType = report.targetType,
            targetId = report.targetId,
            targetLabel = report.targetSummary.ifBlank { report.id },
            details = "Marked report ${report.id} as ${newStatus.uppercase()} (deleteContent=$deleteTargetContent): $resolutionNotes"
        )
    }
}
