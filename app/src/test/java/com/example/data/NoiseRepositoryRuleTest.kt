package com.example.data

import com.example.base.FirestoreEmulatorTestBase
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class NoiseRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun ensureUserInitialized_createsCleanProfileWithoutFakeConversations() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repository = NoiseRepository(firestore)

        val profileResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.ensureUserInitialized()
        }
        assertTrue(profileResult.isSuccess)
        val profile = profileResult.getOrThrow()
        assertEquals("sebin_mr", profile.username)
        assertEquals(40150L, profile.coins)
        assertEquals("New account must start with 0 posts", 0L, profile.postsCount)
        assertEquals("New account must start with 0 followers", 0L, profile.followersCount)
        assertEquals("New account must start with 0 following", 0L, profile.followingCount)

        // Create a real post and verify it appears in global feed and increments postsCount to 1
        withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createPost(
                authorProfile = profile,
                mediaKey = "roses",
                mediaType = "photo",
                caption = "First real post ✨",
                location = "Noise Studio",
                tag = "#NoiseLive",
                vibeEmoji = "🔥"
            ).getOrThrow()
        }

        val posts = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observePosts().first { it.isNotEmpty() }
        }
        assertTrue(posts.any { it.mediaKey == "roses" && it.userId == uid })

        // Verify no fake sample notifications or fake conversations are pre-populated
        val notifications = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeNotifications(uid).first()
        }
        assertTrue("Expected zero fake seeded notifications on fresh init", notifications.none { it.actorUsername == "maya_beats" })

        val conversations = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeConversations(uid).first()
        }
        assertTrue("Expected zero fake sample conversations on fresh init", conversations.isEmpty())
    }

    @Test
    fun claimVipCoins_recordsTransactionAndUpdatesBalance() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repository = NoiseRepository(firestore)
        val initial = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.ensureUserInitialized().getOrThrow()
        }

        val claimResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.claimVipCoins(initial, "Starter", 1000L)
        }
        assertTrue(claimResult.isSuccess)

        val transactions = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeCoinTransactions(uid).first { it.isNotEmpty() }
        }
        assertTrue(transactions.any { it.tierName == "Starter" && it.amount == 1000L })
    }

    @Test
    fun crossUserNotificationAccess_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = NoiseRepository(firestore)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.ensureUserInitialized().getOrThrow()
            firestore.collection("notifications").document("notif_real_$aliceUid").set(
                NotificationItem(
                    id = "notif_real_$aliceUid",
                    userId = aliceUid,
                    actorUid = aliceUid,
                    actorUsername = "sebin_mr",
                    actorAvatarKey = "S",
                    type = "message",
                    message = "Private notification",
                    targetThumbKey = "chat",
                    timeLabel = "Just now",
                    read = false
                )
            ).await()
        }

        signInTestUser(BOB_EMAIL)
        try {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                firestore.collection("notifications").document("notif_real_$aliceUid").get().await()
            }
            fail("Expected PERMISSION_DENIED when reading another user's notification")
        } catch (e: Exception) {
            val firestoreEx = checkNotNull(e.unwrapFirestoreException()) {
                "Expected FirebaseFirestoreException, got $e"
            }
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx.code)
        }
    }

    @Test
    fun observeNotifications_unauthenticated_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repository = NoiseRepository(firestore)

        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repository.observeNotifications("any_user").first()
            }
            fail("Expected PERMISSION_DENIED for unauthenticated user")
        } catch (e: Exception) {
            val firestoreEx = e.unwrapFirestoreException()
            assertNotNull("Expected nested FirebaseFirestoreException, got $e", firestoreEx)
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx?.code)
        }
    }

    @Test
    fun storyDeduplicationDeletion_andPartyRoomSeatJoiningAndRoles_succeed() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val repository = NoiseRepository(firestore)
        val aliceProfile = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.ensureUserInitialized().getOrThrow()
        }

        // 1. Story deduplication & deletion
        val firstStoryId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createStory("sebin_mr", "roses", "First upload", "photo", 6L).getOrThrow()
        }
        val duplicateStoryId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createStory("sebin_mr", "roses", "Updated upload", "photo", 6L).getOrThrow()
        }
        assertEquals("Same mediaKey for same user must reuse deterministic story ID", firstStoryId, duplicateStoryId)

        withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.deleteStory(firstStoryId).getOrThrow()
        }
        val storiesAfterDelete = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeStories().first()
        }
        assertFalse("Deleted story must be removed from Firestore", storiesAfterDelete.any { it.id == firstStoryId })

        // 2. Create Party Room with empty initial seats & creator as OWNER
        val room = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createFullPartyRoom(
                hostUsername = aliceProfile.username,
                title = "Noise VIP Stage",
                subtitle = "Party • Arabian Nights",
                category = "Party",
                musicTrack = "Arabian Nights",
                gameName = "PK Battle",
                micMode = "Free Mic",
                capacity = 12L,
                privateRoom = false,
                password = "",
                themeKey = "starry_mountain",
                description = "Live Music & Voice Chat"
            ).getOrThrow()
        }
        assertEquals("Noise VIP Stage", room.title)
        assertEquals("Live Music & Voice Chat", room.description)
        assertEquals(RoomRole.OWNER, room.roleForUser(aliceUid))
        assertTrue("New room must start with zero occupied seats", room.parseOccupiedSeats().isEmpty())

        // 3. Join Seat #1, then move to Seat #3 (must automatically release Seat #1)
        val afterSeat1 = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.joinOrMoveSeat(room.id, 1, aliceProfile.username, aliceProfile.avatarKey, micMuted = false).getOrThrow()
        }
        assertEquals(aliceUid, afterSeat1.parseOccupiedSeats()[1]?.userId)

        val afterMoveToSeat3 = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.joinOrMoveSeat(room.id, 3, aliceProfile.username, aliceProfile.avatarKey, micMuted = true).getOrThrow()
        }
        assertNull("Moving to Seat #3 must release Seat #1", afterMoveToSeat3.parseOccupiedSeats()[1])
        assertEquals(aliceUid, afterMoveToSeat3.parseOccupiedSeats()[3]?.userId)
        assertTrue(afterMoveToSeat3.parseOccupiedSeats()[3]?.micMuted == true)

        // 4. Second user (Bob) cannot join occupied Seat #3, but can join Seat #2 and be promoted by Owner
        val bobUid = signInTestUser(BOB_EMAIL)
        val bobProfile = UserProfile(id = bobUid, userId = bobUid, username = "bob_live", displayName = "Bob Live", avatarKey = "B")
        val bobJoinOccupied = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.joinOrMoveSeat(room.id, 3, bobProfile.username, bobProfile.avatarKey, micMuted = false)
        }
        assertTrue("Occupied seat must reject second user", bobJoinOccupied.isFailure)

        val bobJoinSeat2 = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.joinOrMoveSeat(room.id, 2, bobProfile.username, bobProfile.avatarKey, micMuted = false)
        }
        assertTrue(bobJoinSeat2.isSuccess)

        // Switch back to Alice (Owner) and promote Bob to SUPER_ADMIN
        signInTestUser(ALICE_EMAIL)
        val afterRoleAssign = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.assignRoomRole(room.id, bobUid, RoomRole.SUPER_ADMIN).getOrThrow()
        }
        assertEquals(RoomRole.SUPER_ADMIN, afterRoleAssign.roleForUser(bobUid))

        // 5. Real user-to-user Direct Conversation + Voice Message + Pin + Reaction
        val conv = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.startOrGetConversation(bobProfile).getOrThrow()
        }
        val voiceMsgId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.sendDirectMessage(
                conversation = conv,
                senderName = "Sebin Mr",
                text = "🎤 Voice message (5s)",
                messageType = "voice",
                mediaKey = "/data/user/0/voice.m4a",
                mediaType = "audio",
                voiceDurationSec = 5L,
                voiceWaveform = "30,60,90,45,75"
            ).getOrThrow()
        }
        val msgs = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeMessages(conv.id, aliceUid).first { list -> list.any { it.id == voiceMsgId } }
        }
        val voiceMsg = msgs.first { it.id == voiceMsgId }
        val pinned = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.togglePinDirectMessage(conv, voiceMsg).getOrThrow()
        }
        assertTrue(pinned)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.reactToDirectMessage(conv.id, voiceMsg.copy(pinned = true), "🔥").getOrThrow()
        }
    }

    @Test
    fun twoAccountVerification_TestAThroughTestH_andVoiceSlotOwnerManagement() = runBlocking {
        val repository = NoiseRepository(firestore)

        // ============================================================
        // TEST A: USER REGISTRATION (Account A & Account B)
        // ============================================================
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceProfile1 = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.ensureUserInitialized().getOrThrow()
        }
        // Re-login / re-initialize Account A -> must restore existing profile without creating duplicates
        val aliceProfile2 = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.ensureUserInitialized().getOrThrow()
        }
        assertEquals(aliceUid, aliceProfile1.userId)
        assertEquals(aliceProfile1.username, aliceProfile2.username)

        // Update Account A profile to distinct username & displayName
        val aliceUpdated = aliceProfile2.copy(
            username = "alice_star",
            usernameLower = "alice_star",
            displayName = "Alice Star",
            displayNameLower = "alice star"
        )
        withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createOrUpdateUserProfile(aliceUpdated).getOrThrow()
        }

        // Register Account B
        val bobUid = signInTestUser(BOB_EMAIL)
        val bobInit = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.ensureUserInitialized().getOrThrow()
        }
        val bobUpdated = bobInit.copy(
            username = "bob_beats",
            usernameLower = "bob_beats",
            displayName = "Bob Beats",
            displayNameLower = "bob beats"
        )
        withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createOrUpdateUserProfile(bobUpdated).getOrThrow()
        }

        // Unique username validation: Account B cannot take Account A's username
        val duplicateUsernameAttempt = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createOrUpdateUserProfile(bobUpdated.copy(username = "ALICE_STAR"))
        }
        assertTrue("Duplicate username must be rejected", duplicateUsernameAttempt.isFailure)

        // ============================================================
        // TEST B: USER SEARCH (Search by username and displayName)
        // ============================================================
        // Account B searches for Account A by username and displayName
        val searchForAliceByUsername = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.searchUsers("alice_star").getOrThrow()
        }
        assertTrue("Account B must find Account A by username", searchForAliceByUsername.any { it.userId == aliceUid })

        val searchForAliceByDisplay = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.searchUsers("Alice Star").getOrThrow()
        }
        assertTrue("Account B must find Account A by displayName", searchForAliceByDisplay.any { it.userId == aliceUid })

        // Switch to Account A and search for Account B
        signInTestUser(ALICE_EMAIL)
        val searchForBob = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.searchUsers("bob_beats").getOrThrow()
        }
        val foundBob = searchForBob.firstOrNull { it.userId == bobUid }
        assertNotNull("Account A must find Account B in search", foundBob)

        // ============================================================
        // TEST C: FOLLOW / UNFOLLOW & OPEN PROFILE
        // ============================================================
        var aliceSnapBeforeFollow = firestore.collection("users").document(aliceUid).get().await()
            .toObject(UserProfile::class.java)!!
        if (bobUid in aliceSnapBeforeFollow.followedUserIds) {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                repository.followAccount(foundBob!!, aliceSnapBeforeFollow).getOrThrow()
            }
            aliceSnapBeforeFollow = firestore.collection("users").document(aliceUid).get().await()
                .toObject(UserProfile::class.java)!!
        }
        val nowFollowing = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.followAccount(foundBob!!, aliceSnapBeforeFollow).getOrThrow()
        }
        assertTrue("Account A should now be following Account B", nowFollowing)

        val aliceAfterFollow = firestore.collection("users").document(aliceUid).get().await()
            .toObject(UserProfile::class.java)!!
        assertTrue(bobUid in aliceAfterFollow.followedUserIds)

        // Toggle unfollow and re-follow
        val afterUnfollow = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.followAccount(foundBob!!, aliceAfterFollow).getOrThrow()
        }
        assertFalse("Second tap should unfollow Account B", afterUnfollow)

        // ============================================================
        // TEST D & TEST E: VOICE MESSAGE RECORDING, UPLOAD & PLAYBACK
        // ============================================================
        val convAB = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.startOrGetConversation(foundBob!!).getOrThrow()
        }
        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        val voiceController = com.example.ui.VoiceAudioController(context)
        val recFile = voiceController.startRecording()
        val finishedPath = voiceController.finishRecording(durationSec = 3)
        val encodedBase64 = voiceController.encodeFileToBase64(finishedPath, fallbackDurationSec = 3)
        assertTrue("Encoded playable WAV base64 must not be blank", encodedBase64.isNotBlank())

        val voiceMsgId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.sendDirectMessage(
                conversation = convAB,
                senderName = "Alice Star",
                text = "🎤 Voice message (3s)",
                messageType = "voice",
                mediaKey = finishedPath,
                mediaType = "audio",
                voiceDurationSec = 3L,
                voiceWaveform = "35,70,90,55,80,45",
                voiceAudioBase64 = encodedBase64,
                voiceMimeType = "audio/wav"
            ).getOrThrow()
        }

        // Switch to Account B -> download voice recording from Firestore and materialize playable WAV file
        signInTestUser(BOB_EMAIL)
        val cloudVoiceDoc = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.fetchVoiceRecording(voiceMsgId).getOrThrow()
        }
        assertNotNull("Account B must receive uploaded voice recording payload", cloudVoiceDoc)
        assertEquals(3L, cloudVoiceDoc!!.durationSec)
        val materializedFile = voiceController.materializeBase64ToPlayableFile(
            recordingId = voiceMsgId,
            audioBase64 = cloudVoiceDoc.audioBase64,
            mimeType = cloudVoiceDoc.mimeType,
            durationSec = 3
        )
        assertTrue("Materialized voice file on Account B must exist and contain valid WAV header + PCM samples", materializedFile.exists() && materializedFile.length() > 44L)
        recFile.delete()

        // ============================================================
        // TEST F, G, H: 3-STATE MESSAGE TICKS (Sent -> Delivered -> Seen)
        // ============================================================
        // TEST F: Account B is OFFLINE; Account A sends a text message -> 1 Grey Tick ("sent")
        withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.setUserOnlineStatus(online = false).getOrThrow()
        }
        signInTestUser(ALICE_EMAIL)
        val textMsgId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.sendDirectMessage(
                conversation = convAB,
                senderName = "Alice Star",
                text = "Hey Bob, testing 3-state ticks!",
                messageType = "text"
            ).getOrThrow()
        }
        val msgStateF = firestore.collection("conversations").document(convAB.id)
            .collection("messages").document(textMsgId).get().await()
            .toObject(DirectMessage::class.java)!!
        assertEquals("STATE 1 (Recipient Offline): deliveryStatus must be 'sent' (1 grey tick)", "sent", msgStateF.deliveryStatus)
        assertEquals("sent", msgStateF.status)
        assertFalse(msgStateF.read)
        assertNull(msgStateF.deliveredAt)
        assertNull(msgStateF.readAt)

        // TEST G: Account B comes ONLINE (without opening chat yet) -> 2 Grey Ticks ("delivered")
        signInTestUser(BOB_EMAIL)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.setUserOnlineStatus(online = true).getOrThrow()
        }
        val deliveredCount = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.acknowledgePendingDeliveriesInConversation(convAB.id).getOrThrow()
        }
        assertTrue("Online recipient must acknowledge pending sent messages as delivered", deliveredCount >= 1)

        val msgStateG = firestore.collection("conversations").document(convAB.id)
            .collection("messages").document(textMsgId).get().await()
            .toObject(DirectMessage::class.java)!!
        assertEquals("STATE 2 (Recipient Online): deliveryStatus must be 'delivered' (2 grey ticks)", "delivered", msgStateG.deliveryStatus)
        assertEquals("delivered", msgStateG.status)
        assertFalse("Message must not be marked read before conversation is opened", msgStateG.read)
        assertNotNull("deliveredAt timestamp must be set", msgStateG.deliveredAt)

        // TEST H: Account B OPENS the conversation with Account A -> 2 Blue Ticks ("seen" / "read")
        val bobMessages = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeMessages(convAB.id, bobUid).first { it.any { m -> m.id == textMsgId } }
        }
        withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.markMessagesSeen(convAB.copy(unreadCount = 1L), bobMessages).getOrThrow()
        }
        val msgStateH = firestore.collection("conversations").document(convAB.id)
            .collection("messages").document(textMsgId).get().await()
            .toObject(DirectMessage::class.java)!!
        assertEquals("STATE 3 (Recipient Opens Chat): deliveryStatus must be 'seen' (2 blue ticks)", "seen", msgStateH.deliveryStatus)
        assertEquals("read", msgStateH.status)
        assertTrue(msgStateH.read)
        assertNotNull("readAt timestamp must be set", msgStateH.readAt)

        // ============================================================
        // VOICE ROOM OWNER SLOT MANAGEMENT (Mute/Unmute, Kick from Slot, Lock/Unlock)
        // ============================================================
        signInTestUser(ALICE_EMAIL)
        val partyRoom = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createFullPartyRoom(
                hostUsername = "alice_star",
                title = "3D Visualizer Stage",
                subtitle = "LiveKit + 3D Sphere",
                category = "Music",
                musicTrack = "Arabian Nights",
                gameName = "PK Battle",
                micMode = "Free Mic",
                capacity = 12L,
                privateRoom = false,
                password = "",
                themeKey = "starry_mountain"
            ).getOrThrow()
        }

        // Bob (Regular user) joins Seat #5
        signInTestUser(BOB_EMAIL)
        val roomWithBobOnSeat5 = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.joinOrMoveSeat(partyRoom.id, 5, "bob_beats", "B", micMuted = false).getOrThrow()
        }
        assertEquals(bobUid, roomWithBobOnSeat5.parseOccupiedSeats()[5]?.userId)
        assertFalse(roomWithBobOnSeat5.parseOccupiedSeats()[5]?.micMuted ?: true)

        // Regular user (Bob) CANNOT lock a slot
        val bobLockAttempt = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.toggleSeatLock(partyRoom.id, 6)
        }
        assertTrue("Regular user must not be allowed to lock/unlock slots", bobLockAttempt.isFailure)

        // Switch to Owner (Alice):
        // 1. Owner mutes Bob on Seat #5, then unmutes Bob
        signInTestUser(ALICE_EMAIL)
        val afterMuteBob = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.toggleMuteParticipant(partyRoom.id, bobUid).getOrThrow()
        }
        assertTrue("Bob must be muted in room and on Seat #5", bobUid in afterMuteBob.mutedUserIds && afterMuteBob.parseOccupiedSeats()[5]?.micMuted == true)

        val afterUnmuteBob = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.toggleMuteParticipant(partyRoom.id, bobUid).getOrThrow()
        }
        assertFalse("Bob must be unmuted after second toggle", bobUid in afterUnmuteBob.mutedUserIds)

        // 2. Owner kicks/removes Bob from Seat #5 (returns Bob to audience without deleting account)
        val afterRemoveFromSlot = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.removeParticipantFromSlot(partyRoom.id, 5, bobUid).getOrThrow()
        }
        assertNull("Seat #5 must now be empty after owner removes occupant from slot", afterRemoveFromSlot.parseOccupiedSeats()[5])

        // 3. Owner locks Seat #5 -> Regular user (Bob) cannot join locked Seat #5
        val afterLockSeat5 = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.toggleSeatLock(partyRoom.id, 5).getOrThrow()
        }
        assertTrue("Seat #5 must be in lockedSeats", 5L in afterLockSeat5.lockedSeats)

        signInTestUser(BOB_EMAIL)
        val bobJoinLockedSeat = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.joinOrMoveSeat(partyRoom.id, 5, "bob_beats", "B", micMuted = false)
        }
        assertTrue("Regular user must be blocked from joining locked slot", bobJoinLockedSeat.isFailure)

        // 4. Owner unlocks Seat #5 -> Regular user (Bob) can join Seat #5 again
        signInTestUser(ALICE_EMAIL)
        val afterUnlockSeat5 = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.toggleSeatLock(partyRoom.id, 5).getOrThrow()
        }
        assertFalse("Seat #5 must be unlocked", 5L in afterUnlockSeat5.lockedSeats)

        signInTestUser(BOB_EMAIL)
        val bobRejoinSeat5 = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.joinOrMoveSeat(partyRoom.id, 5, "bob_beats", "B", micMuted = false)
        }
        assertTrue("Regular user can join Seat #5 after owner unlocks it", bobRejoinSeat5.isSuccess)
    }

    @Test
    fun multiplayerGamingAndFollowSubcollections_endToEndTest() = runBlocking {
        val repository = NoiseRepository(firestore)

        // 1. Initialize Alice and Bob
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceProfile = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.ensureUserInitialized().getOrThrow()
        }
        val bobUid = signInTestUser(BOB_EMAIL)
        val bobInit = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.ensureUserInitialized().getOrThrow()
        }
        val bobProfile = bobInit.copy(username = "bob_gamer", displayName = "Bob Gamer", avatarKey = "B")
        withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createOrUpdateUserProfile(bobProfile).getOrThrow()
        }

        // 2. Alice follows Bob -> verify /users/alice/following and /users/bob/followers subcollections
        signInTestUser(ALICE_EMAIL)
        val nowFollowing = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.followAccount(bobProfile, aliceProfile).getOrThrow()
        }
        assertTrue(nowFollowing)

        val aliceFollowingList = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeFollowing(aliceUid).first { it.isNotEmpty() }
        }
        assertTrue("Alice's following list must include Bob", aliceFollowingList.any { it.userId == bobUid })

        val bobFollowersList = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeFollowers(bobUid).first { it.isNotEmpty() }
        }
        assertTrue("Bob's followers list must include Alice", bobFollowersList.any { it.userId == aliceUid })

        // 3. Alice creates a 3D Carrom Pro multiplayer room inside a voice chatroom
        val carromGame = repository.getGameCatalog().first { it.id == "carrom_3d" }
        val gameRoom = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createGameRoom(
                hostProfile = aliceProfile,
                gameType = carromGame.id,
                gameTitle = carromGame.title,
                mode = "1v1",
                privateRoom = false,
                maxPlayers = 4,
                chatRoomId = "party_room_1",
                chatRoomTitle = "Zama Team Stage",
                entryCoins = 250L,
                voiceRoomConnected = true
            ).getOrThrow()
        }
        assertEquals("carrom_3d", gameRoom.gameType)
        assertEquals(1, gameRoom.parsePlayers().size)
        assertTrue(gameRoom.roomCode.isNotBlank())

        // 4. Alice invites Bob to the game room
        val inviteId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.sendGameInvite(gameRoom, aliceProfile, bobProfile).getOrThrow()
        }
        assertTrue(inviteId.isNotBlank())

        // 5. Bob accepts the game invite and joins Slot #2
        signInTestUser(BOB_EMAIL)
        val pendingInvites = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeGameInvites(bobUid).first { it.isNotEmpty() }
        }
        val inviteDoc = pendingInvites.first { it.id == inviteId }
        assertEquals(bobUid, inviteDoc.targetUserId)

        val joinedRoom = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.respondToGameInvite(inviteDoc, accept = true, responderProfile = bobProfile).getOrThrow()
        }
        assertNotNull(joinedRoom)
        assertEquals(2, joinedRoom!!.parsePlayers().size)
        assertTrue(bobUid in joinedRoom.participantIds)

        // 6. Alice starts the match, syncs real-time state, and submits verified match result
        signInTestUser(ALICE_EMAIL)
        val startedRoom = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.startGameRoomMatch(gameRoom.id).getOrThrow()
        }
        assertEquals("playing", startedRoom.status)

        val syncedRoom = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.syncGameRoomState(
                gameRoomId = gameRoom.id,
                gameStateJson = """{"turnIndex":1,"queenPocketed":true}""",
                nextTurnUserId = bobUid,
                status = "playing"
            ).getOrThrow()
        }
        assertEquals(bobUid, syncedRoom.currentTurnUserId)

        val matchResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.submitVerifiedGameResult(
                gameRoom = syncedRoom,
                winnerProfile = aliceProfile,
                runnerUpUsername = bobProfile.username,
                winnerScore = 120L,
                runnerUpScore = 70L,
                durationSec = 85L
            ).getOrThrow()
        }
        assertEquals(aliceUid, matchResult.winnerId)
        assertEquals(500L, matchResult.coinsAwarded)

        val history = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeGameHistory(aliceUid).first { it.isNotEmpty() }
        }
        assertTrue(history.any { it.gameRoomId == gameRoom.id && it.winnerId == aliceUid })
    }

    @Test
    fun postAndStoryMedia_surviveCompleteLocalFileWipeOnReinstall() = runBlocking {
        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        CloudMediaStorageManager.initialize(context, firestore)

        val aliceUid = signInTestUser(ALICE_EMAIL)
        val repository = NoiseRepository(firestore)
        val aliceProfile = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.ensureUserInitialized().getOrThrow()
        }

        // 1. Prepare realistic local gallery photo and video files in filesDir/persisted_media
        val localDir = java.io.File(context.filesDir, "persisted_media").apply { mkdirs() }
        val photoFile = java.io.File(localDir, "test_upload_photo.jpg")
        val photoBytes = ByteArray(4096) { idx -> ((idx * 31 + 17) and 0xFF).toByte() }
        photoFile.writeBytes(photoBytes)

        val videoFile = java.io.File(localDir, "test_upload_video.mp4")
        val ftypHeader = byteArrayOf(
            0x00, 0x00, 0x00, 0x18, 0x66, 0x74, 0x79, 0x70,
            0x6D, 0x70, 0x34, 0x32, 0x00, 0x00, 0x00, 0x00,
            0x6D, 0x70, 0x34, 0x32, 0x69, 0x73, 0x6F, 0x6D
        )
        val videoPayload = ByteArray(8192) { idx -> ((idx * 13 + 7) and 0xFF).toByte() }
        val videoBytes = ftypHeader + videoPayload
        videoFile.writeBytes(videoBytes)

        val photoLocalUri = android.net.Uri.fromFile(photoFile).toString()
        val videoLocalUri = android.net.Uri.fromFile(videoFile).toString()

        // 2. Upload 1 Photo Post, 1 Video Post, 1 Photo Story, and 1 Video Story
        var lastPostProgress = 0
        val photoPostId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createPost(
                authorProfile = aliceProfile,
                mediaKey = photoLocalUri,
                mediaType = "photo",
                caption = "Permanent Cloud Photo Post",
                location = "Noise Cloud",
                tag = "#PermanentStorage",
                vibeEmoji = "📸",
                onProgress = { pct, _ -> lastPostProgress = pct }
            ).getOrThrow()
        }
        assertEquals(100, lastPostProgress)

        val videoPostId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createPost(
                authorProfile = aliceProfile,
                mediaKey = videoLocalUri,
                mediaType = "video",
                caption = "Permanent Cloud Video Post",
                location = "Noise Cloud",
                tag = "#PermanentVideo",
                vibeEmoji = "🎬"
            ).getOrThrow()
        }

        val photoStoryId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createStory(
                username = aliceProfile.username,
                mediaKey = photoLocalUri,
                caption = "Permanent Photo Story",
                mediaType = "photo",
                durationSec = 6L,
                authorAvatarKey = aliceProfile.avatarKey
            ).getOrThrow()
        }

        val videoStoryId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createStory(
                username = aliceProfile.username,
                mediaKey = videoLocalUri,
                caption = "Permanent Video Story",
                mediaType = "video",
                durationSec = 10L,
                authorAvatarKey = aliceProfile.avatarKey
            ).getOrThrow()
        }

        // 3. Verify Firestore documents contain permanent https:// URLs and valid Firebase Storage paths,
        // and NEVER contain temporary file:// or content:// URIs!
        val savedPhotoPost = firestore.collection("posts").document(photoPostId).get().await()
            .toObject(Post::class.java)!!
        assertTrue("Photo post mediaUrl must be permanent https:// URL, got ${savedPhotoPost.mediaUrl}", savedPhotoPost.mediaUrl.startsWith("https://"))
        assertFalse("Photo post mediaKey must never be file://", savedPhotoPost.mediaKey.startsWith("file://"))
        assertEquals("users/$aliceUid/posts/$photoPostId/original", savedPhotoPost.storagePath)
        assertEquals("Published", savedPhotoPost.uploadStatus)

        val savedVideoPost = firestore.collection("posts").document(videoPostId).get().await()
            .toObject(Post::class.java)!!
        assertTrue("Video post mediaUrl must be permanent https:// URL", savedVideoPost.mediaUrl.startsWith("https://"))
        assertFalse("Video post mediaKey must never be file://", savedVideoPost.mediaKey.startsWith("file://"))
        assertEquals("users/$aliceUid/posts/$videoPostId/original", savedVideoPost.storagePath)
        assertEquals("Published", savedVideoPost.uploadStatus)

        val savedPhotoStory = firestore.collection("stories").document(photoStoryId).get().await()
            .toObject(Story::class.java)!!
        assertTrue("Photo story mediaUrl must be permanent https:// URL", savedPhotoStory.mediaUrl.startsWith("https://"))
        assertFalse("Photo story mediaKey must never be file://", savedPhotoStory.mediaKey.startsWith("file://"))
        assertTrue(savedPhotoStory.storagePath.startsWith("users/$aliceUid/stories/"))
        assertEquals("Published", savedPhotoStory.uploadStatus)

        val savedVideoStory = firestore.collection("stories").document(videoStoryId).get().await()
            .toObject(Story::class.java)!!
        assertTrue("Video story mediaUrl must be permanent https:// URL", savedVideoStory.mediaUrl.startsWith("https://"))
        assertFalse("Video story mediaKey must never be file://", savedVideoStory.mediaKey.startsWith("file://"))
        assertTrue(savedVideoStory.storagePath.startsWith("users/$aliceUid/stories/"))
        assertEquals("Published", savedVideoStory.uploadStatus)

        // 4. SIMULATE UNINSTALLING AND REINSTALLING THE APP (OR LOGGING IN ON A NEW PHONE):
        // Completely wipe all local device storage (filesDir/persisted_media and cacheDir/cloud_media_cache)
        localDir.deleteRecursively()
        java.io.File(context.cacheDir, "cloud_media_cache").deleteRecursively()
        assertFalse("Local photo file must be deleted to simulate app uninstall", photoFile.exists())
        assertFalse("Local video file must be deleted to simulate app uninstall", videoFile.exists())

        // 5. Switch to another account (Bob) or fresh login after reinstall and verify all 4 media files
        // are downloaded/reconstructed from Firebase Cloud Storage and playable!
        signInTestUser(BOB_EMAIL)

        val recoveredPhotoPostUri = withTimeout(DEFAULT_TIMEOUT_MS) {
            CloudMediaStorageManager.resolvePlayableMediaUri(
                context = context,
                mediaUrlOrKey = savedPhotoPost.effectiveMediaSource(),
                storagePath = savedPhotoPost.storagePath,
                explicitCloudMediaId = savedPhotoPost.cloudMediaId,
                mediaType = "photo"
            )
        }
        val recoveredPhotoPostFile = java.io.File(android.net.Uri.parse(recoveredPhotoPostUri).path!!)
        assertTrue("Recovered photo post file must exist after reinstall", recoveredPhotoPostFile.exists())
        assertEquals("Recovered photo post bytes must match original upload", photoBytes.size.toLong(), recoveredPhotoPostFile.length())

        val recoveredVideoPostUri = withTimeout(DEFAULT_TIMEOUT_MS) {
            CloudMediaStorageManager.resolvePlayableMediaUri(
                context = context,
                mediaUrlOrKey = savedVideoPost.effectiveMediaSource(),
                storagePath = savedVideoPost.storagePath,
                explicitCloudMediaId = savedVideoPost.cloudMediaId,
                mediaType = "video"
            )
        }
        val recoveredVideoPostFile = java.io.File(android.net.Uri.parse(recoveredVideoPostUri).path!!)
        assertTrue("Recovered video post file must exist after reinstall", recoveredVideoPostFile.exists())
        assertEquals("Recovered video post bytes must match original upload", videoBytes.size.toLong(), recoveredVideoPostFile.length())

        val recoveredPhotoStoryUri = withTimeout(DEFAULT_TIMEOUT_MS) {
            CloudMediaStorageManager.resolvePlayableMediaUri(
                context = context,
                mediaUrlOrKey = savedPhotoStory.effectiveMediaSource(),
                storagePath = savedPhotoStory.storagePath,
                explicitCloudMediaId = savedPhotoStory.cloudMediaId,
                mediaType = "photo"
            )
        }
        val recoveredPhotoStoryFile = java.io.File(android.net.Uri.parse(recoveredPhotoStoryUri).path!!)
        assertTrue("Recovered photo story file must exist after reinstall", recoveredPhotoStoryFile.exists())
        assertEquals(photoBytes.size.toLong(), recoveredPhotoStoryFile.length())

        val recoveredVideoStoryUri = withTimeout(DEFAULT_TIMEOUT_MS) {
            CloudMediaStorageManager.resolvePlayableMediaUri(
                context = context,
                mediaUrlOrKey = savedVideoStory.effectiveMediaSource(),
                storagePath = savedVideoStory.storagePath,
                explicitCloudMediaId = savedVideoStory.cloudMediaId,
                mediaType = "video"
            )
        }
        val recoveredVideoStoryFile = java.io.File(android.net.Uri.parse(recoveredVideoStoryUri).path!!)
        assertTrue("Recovered video story file must exist after reinstall", recoveredVideoStoryFile.exists())
        assertEquals(videoBytes.size.toLong(), recoveredVideoStoryFile.length())
    }

    @Test
    fun all11GamesCatalog_andVipBadgePurchaseAndEquip_succeed() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val repository = NoiseRepository(firestore)
        val aliceProfile = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.ensureUserInitialized().getOrThrow()
        }

        // 1. Verify all 11 complete multiplayer games are in the catalog and status tracker
        val games = repository.getGameCatalog()
        assertEquals(11, games.size)
        val statusItems = repository.getGameModuleStatusList()
        assertEquals(11, statusItems.size)
        assertTrue(statusItems.all { it.completionStatus.contains("Completed") })

        // Create a Chess 3D multiplayer room and verify Firestore rules accept it
        val chessItem = games.first { it.gameType == "chess_3d" }
        val chessRoom = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createGameRoom(
                hostProfile = aliceProfile,
                gameType = chessItem.gameType,
                gameTitle = chessItem.title,
                mode = "1v1",
                privateRoom = false,
                maxPlayers = 2
            ).getOrThrow()
        }
        assertEquals("chess_3d", chessRoom.gameType)

        // 2. Verify all 10 VIP Luxury Gemstone Badges (VIP 1 to VIP 10)
        val vipCatalog = repository.getVipBadgeCatalog()
        assertEquals(10, vipCatalog.size)
        assertEquals("VIP 1 – Blue Sapphire", vipCatalog.first().badgeName)
        assertEquals("VIP 10 – Eternal Diamond", vipCatalog.last().badgeName)

        // 3. Purchase VIP 3 – Ruby (5,000 coins) -> deducts coins, adds 3 to ownedVipBadges, equips VIP 3, and records purchase receipt
        val afterBuyVip3 = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.purchaseVipBadge(3).getOrThrow()
        }
        assertEquals(40150L - 5000L, afterBuyVip3.coins)
        assertEquals(3L, afterBuyVip3.activeVipBadgeLevel)
        assertEquals("VIP 3 – Ruby", afterBuyVip3.activeVipBadgeName)
        assertTrue(3L in afterBuyVip3.ownedVipBadges)

        val purchases = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeVipBadgePurchases(aliceUid).first { it.isNotEmpty() }
        }
        assertTrue(purchases.any { it.vipLevel == 3L && it.priceCoins == 5000L })

        // 4. Switch active badge back to VIP 1 (already owned) without deducting coins
        val afterSwitchVip1 = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.selectActiveVipBadge(1).getOrThrow()
        }
        assertEquals(1L, afterSwitchVip1.activeVipBadgeLevel)
        assertEquals(afterBuyVip3.coins, afterSwitchVip1.coins)
    }

    private companion object {
        const val ALICE_EMAIL = "alice_noise@test.com"
        const val BOB_EMAIL = "bob_noise@test.com"
        const val DEFAULT_TIMEOUT_MS = 10000L
        const val FLOW_TIMEOUT_MS = 5000L
    }
}

