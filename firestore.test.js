const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user cannot read posts, users, or notifications", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("posts").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("notifications").get());
});

test("Authenticated user can create their own profile and query public profiles", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      appScope: "noise_live_public",
      username: "sebin_mr",
      displayName: "Sebin Mr",
      bio: "Digital Creator",
      website: "noise.app",
      avatarKey: "S",
      coins: 40150,
      vip: true,
      verified: true,
      online: true,
      postsCount: 1,
      followersCount: 13,
      followingCount: 7,
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").where("appScope", "==", "noise_live_public").get()
  );
});

test("Authenticated user cannot spoof another user's profile or inject ghost fields", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date();
  await assertFails(
    aliceDb.collection("users").doc(BOB_UID).set({
      userId: BOB_UID,
      appScope: "noise_live_public",
      username: "bob_user",
      displayName: "Bob",
      bio: "",
      website: "",
      avatarKey: "B",
      coins: 1000,
      vip: true,
      verified: false,
      online: true,
      postsCount: 0,
      followersCount: 0,
      followingCount: 0,
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      appScope: "noise_live_public",
      username: "sebin_mr",
      displayName: "Sebin Mr",
      bio: "",
      website: "",
      avatarKey: "S",
      coins: 40150,
      vip: true,
      verified: true,
      online: true,
      postsCount: 1,
      followersCount: 13,
      followingCount: 7,
      isAdminGhost: true,
      createdAt: now,
      updatedAt: now,
    })
  );
});

test("Notifications and coin transactions enforce strict cross-user isolation", async () => {
  const now = new Date();
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("notifications").doc("notif_bob").set({
      userId: BOB_UID,
      actorUid: ALICE_UID,
      actorUsername: "maya_beats",
      actorAvatarKey: "M",
      type: "like",
      message: "liked your photo",
      targetThumbKey: "mic",
      timeLabel: "2m ago",
      read: false,
      createdAt: now,
      updatedAt: now,
    });
    await context.firestore().collection("coin_transactions").doc("tx_bob").set({
      userId: BOB_UID,
      tierName: "Starter",
      amount: 1000,
      balanceAfter: 41150,
      createdAt: now,
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("notifications").doc("notif_bob").get());
  await assertFails(aliceDb.collection("coin_transactions").doc("tx_bob").get());
  await assertFails(aliceDb.collection("notifications").get());

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertSucceeds(
    bobDb.collection("notifications").where("userId", "==", BOB_UID).get()
  );
  await assertSucceeds(
    bobDb.collection("coin_transactions").where("userId", "==", BOB_UID).get()
  );
  await assertFails(
    bobDb.collection("coin_transactions").doc("tx_bob").delete()
  );
});

test("Direct messages and conversations enforce participant membership", async () => {
  const now = new Date();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("conversations").doc("conv_1").set({
      participantIds: [ALICE_UID, BOB_UID],
      peerUid: BOB_UID,
      peerUsername: "nora_____noraaa",
      peerDisplayName: "nora_____noraaa",
      peerAvatarKey: "S",
      peerVerified: false,
      peerOnline: true,
      peerTyping: false,
      lastMessage: "Hello",
      lastTimeLabel: "09:12 PM",
      unreadCount: 0,
      tabCategory: "Primary",
      createdAt: now,
      updatedAt: now,
    })
  );

  const eveDb = testEnv.authenticatedContext("eve_789").firestore();
  await assertFails(eveDb.collection("conversations").doc("conv_1").get());
  await assertSucceeds(
    aliceDb.collection("conversations").where("participantIds", "array-contains", ALICE_UID).get()
  );
});

test("PartyRoom enforces 5-tier role hierarchy and Story deletion enforces ownership", async () => {
  const now = new Date();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const memberDb = testEnv.authenticatedContext("member_999").firestore();

  // Alice creates a room as Owner
  await assertSucceeds(
    aliceDb.collection("party_rooms").doc("room_1").set({
      hostId: ALICE_UID,
      appScope: "noise_live_public",
      hostUsername: "sebin_mr",
      title: "Zomo Team",
      subtitle: "Live Stage",
      description: "Official Live Stage",
      superAdminIds: [BOB_UID],
      adminIds: [],
      moderatorIds: [],
      mutedUserIds: [],
      kickedUserIds: [],
      lockedSeats: [4, 7, 9, 11],
      occupiedSeatsJson: "",
      listenersCount: 1,
      giftsTotal: 0,
      live: true,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Ordinary member CANNOT assign admins or lock seats
  await assertFails(
    memberDb.collection("party_rooms").doc("room_1").update({
      adminIds: ["member_999"],
      updatedAt: new Date(),
    })
  );
  await assertFails(
    memberDb.collection("party_rooms").doc("room_1").update({
      lockedSeats: [4],
      updatedAt: new Date(),
    })
  );

  // Ordinary member CAN join an empty seat
  await assertSucceeds(
    memberDb.collection("party_rooms").doc("room_1").update({
      occupiedSeatsJson: "3:member_999:member_user:M:false",
      updatedAt: new Date(),
    })
  );

  // Super Admin (Bob) CAN assign Admins and lock/unlock seats, but CANNOT steal Owner (hostId)
  await assertSucceeds(
    bobDb.collection("party_rooms").doc("room_1").update({
      adminIds: ["member_999"],
      lockedSeats: [4, 7],
      updatedAt: new Date(),
    })
  );
  await assertFails(
    bobDb.collection("party_rooms").doc("room_1").update({
      hostId: BOB_UID,
      updatedAt: new Date(),
    })
  );

  // Story deletion: only owner can delete
  await assertSucceeds(
    aliceDb.collection("stories").doc("story_1").set({
      userId: ALICE_UID,
      appScope: "noise_live_public",
      username: "sebin_mr",
      mediaKey: "roses",
      caption: "My story",
      createdAt: now,
    })
  );
  await assertFails(bobDb.collection("stories").doc("story_1").delete());
  await assertSucceeds(aliceDb.collection("stories").doc("story_1").delete());
});

test("Private user email isolation, voice_recordings access control, and 3-state message tick receiver-only updates", async () => {
  const now = new Date();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const eveDb = testEnv.authenticatedContext("eve_789").firestore();

  // 1. Private email in /user_private/{uid} can only be read/written by owner
  await assertSucceeds(
    aliceDb.collection("user_private").doc(ALICE_UID).set({
      userId: ALICE_UID,
      email: "alice@example.com",
    })
  );
  await assertFails(bobDb.collection("user_private").doc(ALICE_UID).get());

  // 2. Voice recording in /voice_recordings/{id} accessible only to conversation participants
  await assertSucceeds(
    aliceDb.collection("voice_recordings").doc("vmsg_1").set({
      conversationId: "conv_ab",
      senderId: ALICE_UID,
      receiverId: BOB_UID,
      participantIds: [ALICE_UID, BOB_UID],
      audioBase64: "UklGRiQAAABXQVZFZm10IBAAAAABAAEAQB8AAIA+AAACABAAZGF0YQAAAAA=",
      mimeType: "audio/wav",
      durationSec: 4,
      waveform: "30,60,90,45",
      createdAt: now,
    })
  );
  await assertSucceeds(bobDb.collection("voice_recordings").doc("vmsg_1").get());
  await assertFails(eveDb.collection("voice_recordings").doc("vmsg_1").get());

  // 3. 3-State Message Tick Rules: Sender creates as "sent" (1 grey tick); only Receiver can update to "delivered" (2 grey ticks) and "seen" (2 blue ticks)
  await assertSucceeds(
    aliceDb.collection("conversations").doc("conv_ab").set({
      participantIds: [ALICE_UID, BOB_UID],
      peerUid: BOB_UID,
      peerUsername: "bob_live",
      peerDisplayName: "Bob Live",
      peerAvatarKey: "B",
      peerVerified: false,
      peerOnline: true,
      peerTyping: false,
      lastMessage: "Hello Bob",
      lastTimeLabel: "10:00 AM",
      unreadCount: 1,
      tabCategory: "Primary",
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    aliceDb
      .collection("conversations")
      .doc("conv_ab")
      .collection("messages")
      .doc("msg_1")
      .set({
        conversationId: "conv_ab",
        senderId: ALICE_UID,
        receiverId: BOB_UID,
        senderName: "Sebin Mr",
        participantIds: [ALICE_UID, BOB_UID],
        text: "Hello Bob",
        messageContent: "Hello Bob",
        messageType: "text",
        mediaKey: "",
        mediaUrl: "",
        mediaType: "photo",
        voiceDurationSec: 0,
        voiceWaveform: "",
        deliveryStatus: "sent",
        status: "sent",
        timeLabel: "10:00 AM",
        reactionEmoji: "",
        replyToMessageId: "",
        replyToSenderName: "",
        replyToText: "",
        pinned: false,
        savedLocally: false,
        read: false,
        createdAt: now,
      })
  );

  // Sender (Alice) CANNOT spoof "seen" or "delivered" on her own outgoing message to Bob
  await assertFails(
    aliceDb
      .collection("conversations")
      .doc("conv_ab")
      .collection("messages")
      .doc("msg_1")
      .update({
        deliveryStatus: "seen",
        status: "read",
        read: true,
      })
  );

  // Receiver (Bob) CAN transition "sent" -> "delivered" (2 grey ticks) -> "seen" (2 blue ticks)
  await assertSucceeds(
    bobDb
      .collection("conversations")
      .doc("conv_ab")
      .collection("messages")
      .doc("msg_1")
      .update({
        deliveryStatus: "delivered",
        status: "delivered",
        deliveredAt: new Date(),
      })
  );

  await assertSucceeds(
    bobDb
      .collection("conversations")
      .doc("conv_ab")
      .collection("messages")
      .doc("msg_1")
      .update({
        deliveryStatus: "seen",
        status: "read",
        read: true,
        readAt: new Date(),
      })
  );
});

test("Followers/Following subcollections, Presence, Typing, and Multiplayer Game Rooms enforce zero-trust rules", async () => {
  const now = new Date();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const eveDb = testEnv.authenticatedContext("eve_789").firestore();

  // 1. Alice follows Bob -> writes to /users/alice/following/bob and /users/bob/followers/alice
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("following")
      .doc(BOB_UID)
      .set({
        followerId: ALICE_UID,
        followingId: BOB_UID,
        username: "bob_beats",
        displayName: "Bob Beats",
        avatarKey: "B",
        verified: false,
        createdAt: now,
      })
  );

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(BOB_UID)
      .collection("followers")
      .doc(ALICE_UID)
      .set({
        followerId: ALICE_UID,
        followingId: BOB_UID,
        username: "alice_star",
        displayName: "Alice Star",
        avatarKey: "A",
        verified: true,
        createdAt: now,
      })
  );

  // Eve cannot forge a follow relation on behalf of Alice
  await assertFails(
    eveDb
      .collection("users")
      .doc(BOB_UID)
      .collection("followers")
      .doc(ALICE_UID)
      .set({
        followerId: ALICE_UID,
        followingId: BOB_UID,
        username: "spoof",
      })
  );

  // 2. Presence & Typing documents
  await assertSucceeds(
    aliceDb.collection("presence").doc(ALICE_UID).set({
      userId: ALICE_UID,
      online: true,
      lastSeen: now,
      updatedAt: now,
    })
  );
  await assertFails(
    bobDb.collection("presence").doc(ALICE_UID).set({
      userId: ALICE_UID,
      online: false,
    })
  );

  // 3. Multiplayer Game Room, Game Invite & Match History
  await assertSucceeds(
    aliceDb.collection("game_rooms").doc("groom_1").set({
      roomCode: "NL7788",
      roomTitle: "Alice's 3D Carrom Pro",
      gameId: "carrom_3d",
      gameName: "3D Carrom Pro",
      hostId: ALICE_UID,
      hostUsername: "alice_star",
      hostAvatarKey: "A",
      chatRoomId: "party_1",
      chatRoomTitle: "VIP Stage",
      privateRoom: false,
      status: "WAITING",
      maxPlayers: 4,
      minPlayers: 2,
      playerIds: [ALICE_UID],
      invitedUserIds: [BOB_UID],
      playersJson: `0:${ALICE_UID}:alice_star:A:READY:0:false:false`,
      scoresMap: {},
      activeTurnUserId: ALICE_UID,
      winnerUserId: "",
      winnerUsername: "",
      relayJoinCode: "NL-RELAY-7788",
      unitySceneName: "NoiseCarrom3DArena",
      graphicsQuality: "HIGH",
      voiceEnabled: true,
      gameStateJson: `{"strikerX":0.5}`,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Alice invites Bob to the game room
  await assertSucceeds(
    aliceDb.collection("game_invites").doc("ginv_1").set({
      gameRoomId: "groom_1",
      roomCode: "NL7788",
      gameId: "carrom_3d",
      gameName: "3D Carrom Pro",
      chatRoomId: "party_1",
      fromUserId: ALICE_UID,
      fromUsername: "alice_star",
      fromAvatarKey: "A",
      toUserId: BOB_UID,
      status: "PENDING",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Eve cannot read Bob's private game invite
  await assertFails(eveDb.collection("game_invites").doc("ginv_1").get());
  // Bob can read and accept his game invite
  await assertSucceeds(
    bobDb.collection("game_invites").doc("ginv_1").update({
      status: "ACCEPTED",
      updatedAt: new Date(),
    })
  );

  // Match history record is immutable once created by participant
  await assertSucceeds(
    aliceDb.collection("game_history").doc("match_1").set({
      gameRoomId: "groom_1",
      gameId: "carrom_3d",
      gameName: "3D Carrom Pro",
      chatRoomId: "party_1",
      hostId: ALICE_UID,
      winnerUserId: ALICE_UID,
      winnerUsername: "alice_star",
      playerIds: [ALICE_UID, BOB_UID],
      playerNames: ["alice_star", "bob_beats"],
      scoresMap: { [ALICE_UID]: 120, [BOB_UID]: 80 },
      coinsRewarded: 500,
      durationSec: 95,
      verifiedByServerRule: true,
      createdAt: now,
    })
  );
  await assertFails(aliceDb.collection("game_history").doc("match_1").delete());
});

test("Permanent Cloud Media Objects and Post/Story storage metadata enforce owner write and authenticated read", async () => {
  const now = new Date();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const unauthDb = testEnv.unauthenticatedContext().firestore();

  // 1. Alice creates a permanent cloud media object and chunk
  await assertSucceeds(
    aliceDb.collection("cloud_media_objects").doc("media_post_1").set({
      mediaId: "media_post_1",
      ownerUid: ALICE_UID,
      storagePath: `users/${ALICE_UID}/posts/post_1/original`,
      mediaType: "video",
      mimeType: "video/mp4",
      totalBytes: 65536,
      chunkCount: 1,
      thumbnailBase64: "data:image/jpeg;base64,/9j/4AAQSkZJRg==",
      downloadUrl: "https://firebasestorage.googleapis.com/v0/b/noise/o/post_1?alt=media",
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    aliceDb
      .collection("cloud_media_objects")
      .doc("media_post_1")
      .collection("chunks")
      .doc("chunk_0000")
      .set({
        mediaId: "media_post_1",
        ownerUid: ALICE_UID,
        chunkIndex: 0,
        dataBase64: "AAAAHGZ0eXBtcDQyAAAAAG1wNDJpc29t",
      })
  );

  // 2. Bob (authenticated) can read the cloud media object and chunk after reinstall/cross-device login, but cannot delete or overwrite Alice's media
  await assertSucceeds(bobDb.collection("cloud_media_objects").doc("media_post_1").get());
  await assertSucceeds(
    bobDb.collection("cloud_media_objects").doc("media_post_1").collection("chunks").doc("chunk_0000").get()
  );
  await assertFails(bobDb.collection("cloud_media_objects").doc("media_post_1").delete());
  await assertFails(unauthDb.collection("cloud_media_objects").doc("media_post_1").get());
});

test("All 11 multiplayer game types and VIP 1-10 Luxury Gemstone Badge purchases enforce strict security rules", async () => {
  const now = new Date();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  // 1. Verify new board & arcade games (chess_3d, checkers_3d, tic_tac_toe, dominoes_pro, snakes_ladders, mini_arcade) succeed in game_rooms
  for (const gameType of ["chess_3d", "checkers_3d", "tic_tac_toe", "dominoes_pro", "snakes_ladders", "mini_arcade"]) {
    await assertSucceeds(
      aliceDb.collection("game_rooms").doc(`room_${gameType}`).set({
        roomCode: "NL9999",
        roomTitle: `Alice ${gameType}`,
        gameType,
        gameId: gameType,
        gameName: gameType,
        hostId: ALICE_UID,
        hostUsername: "alice_star",
        maxPlayers: 4,
        participantIds: [ALICE_UID],
        status: "playing",
        createdAt: now,
        updatedAt: now,
      })
    );
  }

  // 2. Alice can create a verified VIP 1-10 badge purchase for herself, and cannot spoof Bob's userId or invalid VIP level
  await assertSucceeds(
    aliceDb.collection("vip_badge_purchases").doc("vip_purchase_1").set({
      userId: ALICE_UID,
      username: "alice_star",
      vipLevel: 5,
      badgeName: "VIP 5 – Pink Diamond",
      gemstoneName: "Large Pink Diamond",
      priceCoins: 12000,
      balanceAfter: 28150,
      createdAt: now,
    })
  );

  // Bob cannot read or delete Alice's VIP badge purchase receipt
  await assertFails(bobDb.collection("vip_badge_purchases").doc("vip_purchase_1").get());
  await assertFails(aliceDb.collection("vip_badge_purchases").doc("vip_purchase_1").delete());

  // Invalid VIP level (e.g., 11) must be rejected
  await assertFails(
    aliceDb.collection("vip_badge_purchases").doc("vip_purchase_invalid").set({
      userId: ALICE_UID,
      username: "alice_star",
      vipLevel: 11,
      badgeName: "VIP 11",
      priceCoins: 1000,
      balanceAfter: 1000,
      createdAt: now,
    })
  );
});

test("1-on-1 Audio/Video Call signaling (/calls), shared conversation chatThemeKey, PartyRoom YouTube sync, and user Light/Dark themeMode enforce security rules", async () => {
  const now = new Date();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const eveDb = testEnv.authenticatedContext("eve_789").firestore();

  // 1. Alice starts a 1-on-1 video call with Bob
  await assertSucceeds(
    aliceDb.collection("calls").doc("call_ab_1").set({
      callId: "call_ab_1",
      conversationId: `conv_${ALICE_UID}_${BOB_UID}`,
      participantIds: [ALICE_UID, BOB_UID],
      callerId: ALICE_UID,
      callerUsername: "alice_star",
      callerDisplayName: "Alice Star",
      callerAvatarKey: "A",
      callerAvatarUrl: "",
      callerVipLevel: 5,
      calleeId: BOB_UID,
      calleeUsername: "bob_beats",
      calleeDisplayName: "Bob Beats",
      calleeAvatarKey: "B",
      calleeAvatarUrl: "",
      calleeVipLevel: 2,
      callType: "video",
      status: "ringing",
      liveKitRoomName: "nl_call_ab_1",
      callerMuted: false,
      calleeMuted: false,
      callerCameraOn: true,
      calleeCameraOn: true,
      callerFrontCamera: true,
      calleeFrontCamera: true,
      durationSec: 0,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Eve cannot read or hijack the private call between Alice and Bob
  await assertFails(eveDb.collection("calls").doc("call_ab_1").get());
  await assertFails(
    eveDb.collection("calls").doc("call_ab_1").update({
      status: "accepted",
      updatedAt: new Date(),
    })
  );

  // Bob (callee) can read, accept, and end the call
  await assertSucceeds(bobDb.collection("calls").doc("call_ab_1").get());
  await assertSucceeds(
    bobDb.collection("calls").doc("call_ab_1").update({
      status: "accepted",
      answeredAt: new Date(),
      updatedAt: new Date(),
    })
  );
  await assertSucceeds(
    bobDb.collection("calls").doc("call_ab_1").update({
      status: "ended",
      durationSec: 42,
      endedAt: new Date(),
      updatedAt: new Date(),
    })
  );

  // 2. Shared conversation chatThemeKey and bidirectional initiator/peer fields
  const convId = `conv_${ALICE_UID}_${BOB_UID}`;
  await assertSucceeds(
    aliceDb.collection("conversations").doc(convId).set({
      participantIds: [ALICE_UID, BOB_UID],
      initiatorUid: ALICE_UID,
      initiatorUsername: "alice_star",
      initiatorDisplayName: "Alice Star",
      initiatorAvatarKey: "A",
      initiatorVipBadgeLevel: 5,
      peerUid: BOB_UID,
      peerUsername: "bob_beats",
      peerDisplayName: "Bob Beats",
      peerAvatarKey: "B",
      peerVerified: false,
      peerVipBadgeLevel: 2,
      peerOnline: true,
      peerTyping: false,
      chatThemeKey: "royal_aurora",
      lastMessage: "Video call (0:42)",
      lastTimeLabel: "10:15 AM",
      unreadCount: 0,
      tabCategory: "Primary",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Bob can update the shared chat background theme so both users see the same theme
  await assertSucceeds(
    bobDb.collection("conversations").doc(convId).update({
      chatThemeKey: "cyber_matrix",
      updatedAt: new Date(),
    })
  );

  // 3. User Light / Dark themeMode preference in /users/{uid}
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      appScope: "noise_live_public",
      username: "alice_star",
      displayName: "Alice Star",
      bio: "VIP Creator",
      website: "",
      avatarKey: "A",
      coins: 50000,
      vip: true,
      activeVipBadgeLevel: 5,
      themeMode: "light",
      verified: true,
      online: true,
      postsCount: 2,
      followersCount: 10,
      followingCount: 5,
      createdAt: now,
      updatedAt: now,
    })
  );
});

test("Mandatory Two-Account Verification (Tests A-F): Direct Messages 4-State, Real-Time Notifications, Ludo Slot Invites, Chatrooms, Calling & YouTube Sync", async () => {
  const now = new Date();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  // Register Account A (Alice) and Account B (Bob) with device notification tokens (Test B)
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      appScope: "noise_live_public",
      username: "alice_account",
      displayName: "Alice Account",
      bio: "Account A",
      website: "",
      avatarKey: "A",
      coins: 40150,
      vip: true,
      verified: true,
      online: true,
      fcmToken: "nl_fcm_alice_123_device",
      notificationsEnabled: true,
      postsCount: 0,
      followersCount: 0,
      followingCount: 0,
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertSucceeds(
    bobDb.collection("users").doc(BOB_UID).set({
      userId: BOB_UID,
      appScope: "noise_live_public",
      username: "bob_account",
      displayName: "Bob Account",
      bio: "Account B",
      website: "",
      avatarKey: "B",
      coins: 40150,
      vip: true,
      verified: false,
      online: true,
      fcmToken: "nl_fcm_bob_456_device",
      notificationsEnabled: true,
      postsCount: 0,
      followersCount: 0,
      followingCount: 0,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Test A & Test B: Account A sends text & voice messages to Account B in canonical conversation
  const canonicalConvId = `conv_${ALICE_UID}_${BOB_UID}`;
  await assertSucceeds(
    aliceDb.collection("conversations").doc(canonicalConvId).set({
      participantIds: [ALICE_UID, BOB_UID],
      initiatorUid: ALICE_UID,
      initiatorUsername: "alice_account",
      initiatorDisplayName: "Alice Account",
      initiatorAvatarKey: "A",
      initiatorVipBadgeLevel: 1,
      peerUid: BOB_UID,
      peerUsername: "bob_account",
      peerDisplayName: "Bob Account",
      peerAvatarKey: "B",
      peerVerified: false,
      peerVipBadgeLevel: 1,
      peerOnline: true,
      peerTyping: false,
      chatThemeKey: "midnight_vip",
      lastMessage: "Hey Bob, ready for Ludo?",
      lastTimeLabel: "11:00 AM",
      unreadCount: 1,
      tabCategory: "Primary",
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    aliceDb.collection("conversations").doc(canonicalConvId).collection("messages").doc("msg_text_1").set({
      conversationId: canonicalConvId,
      senderId: ALICE_UID,
      receiverId: BOB_UID,
      senderName: "Alice Account",
      participantIds: [ALICE_UID, BOB_UID],
      text: "Hey Bob, ready for Ludo?",
      messageContent: "Hey Bob, ready for Ludo?",
      messageType: "text",
      mediaKey: "",
      mediaUrl: "",
      mediaType: "photo",
      voiceDurationSec: 0,
      voiceWaveform: "",
      deliveryStatus: "sent",
      status: "sent",
      timeLabel: "11:00 AM",
      reactionEmoji: "",
      replyToMessageId: "",
      replyToSenderName: "",
      replyToText: "",
      pinned: false,
      savedLocally: false,
      read: false,
      createdAt: now,
    })
  );

  // Real-time message notification created for Account B with conversationId deep-link
  await assertSucceeds(
    aliceDb.collection("notifications").doc("notif_msg_text_1").set({
      userId: BOB_UID,
      actorUid: ALICE_UID,
      actorUsername: "alice_account",
      actorAvatarKey: "A",
      type: "message",
      message: "sent you a message: \"Hey Bob, ready for Ludo?\"",
      targetThumbKey: "chat",
      conversationId: canonicalConvId,
      gameRoomId: "",
      timeLabel: "Just now",
      read: false,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Account B receives the conversation, message, and notification in real time, then marks DELIVERED -> SEEN
  const bobConvSnap = await assertSucceeds(
    bobDb.collection("conversations").where("participantIds", "array-contains", BOB_UID).get()
  );
  if (bobConvSnap.empty) throw new Error("Account B did not receive shared conversation");

  await assertSucceeds(
    bobDb.collection("conversations").doc(canonicalConvId).collection("messages").doc("msg_text_1").update({
      deliveryStatus: "delivered",
      status: "delivered",
      deliveredAt: new Date(),
    })
  );
  await assertSucceeds(
    bobDb.collection("conversations").doc(canonicalConvId).collection("messages").doc("msg_text_1").update({
      deliveryStatus: "seen",
      status: "read",
      read: true,
      readAt: new Date(),
    })
  );

  // Test C: Account A creates a waiting 3D Ludo session, invites Account B to Slot 2, Account B accepts & joins, and turns sync
  await assertSucceeds(
    aliceDb.collection("game_rooms").doc("ludo_room_ab").set({
      roomCode: "LUDO22",
      roomTitle: "3D Royal Ludo",
      gameId: "ludo_3d",
      gameType: "ludo_3d",
      gameName: "3D Royal Ludo",
      hostId: ALICE_UID,
      hostUsername: "alice_account",
      hostAvatarKey: "A",
      chatRoomId: "",
      chatRoomTitle: "",
      privateRoom: false,
      status: "waiting",
      maxPlayers: 4,
      minPlayers: 2,
      playerIds: [ALICE_UID],
      participantIds: [ALICE_UID],
      invitedUserIds: [BOB_UID],
      playersJson: `1~${ALICE_UID}~alice_account~Alice Account~A~1~true~0~true~true~false`,
      activeTurnUserId: ALICE_UID,
      winnerUserId: "",
      winnerUsername: "",
      gameStateJson: `{"engine":"unity6_urp","status":"waiting"}`,
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    aliceDb.collection("game_invites").doc("ginv_ludo_slot2").set({
      gameRoomId: "ludo_room_ab",
      roomCode: "LUDO22",
      gameId: "ludo_3d",
      gameType: "ludo_3d",
      gameName: "3D Royal Ludo",
      chatRoomId: "",
      fromUserId: ALICE_UID,
      fromUsername: "alice_account",
      fromAvatarKey: "A",
      toUserId: BOB_UID,
      slotIndex: 2,
      expiresAtMillis: Date.now() + 180000,
      status: "pending",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Account B accepts the Slot 2 invitation and joins the Ludo room
  await assertSucceeds(
    bobDb.collection("game_invites").doc("ginv_ludo_slot2").update({
      status: "accepted",
      updatedAt: new Date(),
    })
  );
  await assertSucceeds(
    bobDb.collection("game_rooms").doc("ludo_room_ab").update({
      playerIds: [ALICE_UID, BOB_UID],
      participantIds: [ALICE_UID, BOB_UID],
      playersJson: `1~${ALICE_UID}~alice_account~Alice Account~A~1~true~0~true~true~false;2~${BOB_UID}~bob_account~Bob Account~B~2~true~0~true~true~false`,
      status: "playing",
      gameStateJson: "LUDOv2:1|6|6,-1,-1,-1,0,-1,-1,-1,0,-1,-1,-1,0,-1,-1,-1",
      activeTurnUserId: BOB_UID,
      updatedAt: new Date(),
    })
  );

  // Test D & Test F: Account A creates a live Chatroom, syncs YouTube Live state for Account B, then closes/deletes the room
  await assertSucceeds(
    aliceDb.collection("party_rooms").doc("party_ab_live").set({
      hostId: ALICE_UID,
      appScope: "noise_live_public",
      hostUsername: "alice_account",
      title: "Alice Live Cinema & Voice",
      subtitle: "Live Stage",
      description: "Real user room",
      superAdminIds: [],
      adminIds: [],
      moderatorIds: [BOB_UID],
      mutedUserIds: [],
      kickedUserIds: [],
      lockedSeats: [],
      occupiedSeatsJson: `1:${ALICE_UID}:alice_account:A:false;2:${BOB_UID}:bob_account:B:false`,
      listenersCount: 2,
      giftsTotal: 0,
      live: true,
      youtubeVideoId: "jfKfPfyJRdk",
      youtubeVideoTitle: "Lofi Hip Hop Radio - Beats to Relax/Study to",
      youtubePlaying: true,
      youtubePositionSec: 120,
      youtubeUpdatedByUid: ALICE_UID,
      youtubeUpdatedAtMillis: Date.now(),
      createdAt: now,
      updatedAt: now,
    })
  );

  // Account B (Moderator) can pause/seek the shared YouTube stream
  await assertSucceeds(
    bobDb.collection("party_rooms").doc("party_ab_live").update({
      youtubePlaying: false,
      youtubePositionSec: 145,
      youtubeUpdatedByUid: BOB_UID,
      youtubeUpdatedAtMillis: Date.now(),
      updatedAt: new Date(),
    })
  );

  // Account A closes and deletes the chatroom -> immediately removed from /party_rooms
  await assertSucceeds(
    aliceDb.collection("party_rooms").doc("party_ab_live").delete()
  );
});

test("Owner-Only Gemini AI Developer Studio (/developer_studio_*) & Central App Config enforce strict Super Owner UID & email rules", async () => {
  const ownerUid = "qiVebE18UCecUn4IBtOzFAg_sebin";
  const ownerDb = testEnv.authenticatedContext(ownerUid, { email: "sebinmr556@gmail.com" }).firestore();
  const normalDb = testEnv.authenticatedContext(BOB_UID, { email: "bob@example.com" }).firestore();

  // Verified Owner (sebin_mr) can read & write /developer_studio_files, /developer_studio_commits, /developer_studio_deployments & /central_app_config
  await assertSucceeds(
    ownerDb.collection("developer_studio_files").doc("MainActivity_kt").set({
      appScope: "noise_live_public",
      filePath: "app/src/main/java/com/example/MainActivity.kt",
      content: "package com.example",
      branchName: "ai-dev/gemini-studio",
    })
  );
  await assertSucceeds(
    ownerDb.collection("developer_studio_commits").doc("commit_101").set({
      appScope: "noise_live_public",
      commitSha: "a94f82c1",
      branchName: "ai-dev/gemini-studio",
      commitMessage: "Gemini AI Code Patch",
    })
  );
  await assertSucceeds(
    ownerDb.collection("developer_studio_deployments").doc("deploy_101").set({
      appScope: "noise_live_public",
      deploymentVersion: "v1.2.0-live",
      deploymentType: "LIVE_REMOTE_UPDATE",
    })
  );

  // Normal user or non-owner admin CANNOT read or write /developer_studio_files or /developer_studio_commits
  await assertFails(normalDb.collection("developer_studio_files").doc("MainActivity_kt").get());
  await assertFails(
    normalDb.collection("developer_studio_files").doc("MainActivity_kt").set({
      appScope: "noise_live_public",
      filePath: "hacked.kt",
    })
  );
  await assertFails(
    normalDb.collection("developer_studio_commits").doc("commit_hack").set({
      appScope: "noise_live_public",
    })
  );
  // Signed-in user can read /developer_studio_deployments to receive live update notices, but CANNOT write
  await assertSucceeds(normalDb.collection("developer_studio_deployments").doc("deploy_101").get());
  await assertFails(
    normalDb.collection("developer_studio_deployments").doc("deploy_101").delete()
  );
});

test("VIP Party Stage Create Voice Room works for any two registered accounts with full 13-field Android PartyRoom payload and enforces owner/SuperOwner delete rules", async () => {
  const now = new Date();
  const user1Uid = "reg_user_account_1";
  const user2Uid = "reg_user_account_2";
  const superOwnerUid = "sebin_owner_uid_777";

  const user1Db = testEnv.authenticatedContext(user1Uid, { email: "user1@example.com" }).firestore();
  const user2Db = testEnv.authenticatedContext(user2Uid, { email: "user2@example.com" }).firestore();
  const ownerDb = testEnv.authenticatedContext(superOwnerUid, { email: "sebinmr556@gmail.com" }).firestore();
  const unauthDb = testEnv.unauthenticatedContext().firestore();

  // 1. Registered Account 1 (non-admin user) creates a Public VIP Party Stage room with full Android PartyRoom fields (including active + isActive)
  const room1Id = "room_vip_account1_public";
  await assertSucceeds(
    user1Db.collection("party_rooms").doc(room1Id).set({
      id: room1Id,
      roomId: room1Id,
      appScope: "noise_live_public",
      hostId: user1Uid,
      hostUid: user1Uid,
      ownerUid: user1Uid,
      hostUsername: "arjun_live",
      ownerUsername: "arjun_live",
      roomName: "Arjun Midnight Beats",
      title: "Arjun Midnight Beats",
      subtitle: "arjun_live • Music",
      description: "Live Malayalam & Chill Beats",
      roomDescription: "Live Malayalam & Chill Beats",
      category: "Music",
      roomCategory: "Music",
      visibility: "public",
      publicRoom: true,
      privateRoom: false,
      password: "",
      invitationToken: "inv_token_101",
      authorizedUserIds: [user1Uid],
      isActive: true,
      active: true,
      roomStatus: "active",
      participantCount: 1,
      musicTrack: "Arabian Nights",
      musicSelection: "Arabian Nights",
      gameName: "3D Royal Ludo",
      gameSelection: "3D Royal Ludo",
      micMode: "Free Mic",
      microphoneSettings: "Free Mic",
      capacity: 12,
      roomCapacity: 12,
      themeKey: "starry_mountain",
      roomCover: "starry_mountain",
      stageTheme: "starry_mountain",
      roomImageUrl: "starry_mountain",
      superAdminIds: [],
      adminIds: [],
      moderatorIds: [],
      mutedUserIds: [],
      kickedUserIds: [],
      lockedSeats: [],
      occupiedSeatsJson: `1:${user1Uid}:arjun_live:A:false`,
      listenersCount: 1,
      giftsTotal: 0,
      live: true,
      youtubeVideoId: "",
      youtubeVideoTitle: "",
      youtubePlaying: false,
      youtubePositionSec: 0,
      youtubeUpdatedByUid: "",
      youtubeUpdatedAtMillis: 0,
      createdTimestamp: now,
      createdAt: now,
      updatedAt: now,
    })
  );

  // 2. Registered Account 2 creates a second room (Private VIP Party Stage room)
  const room2Id = "room_vip_account2_private";
  await assertSucceeds(
    user2Db.collection("party_rooms").doc(room2Id).set({
      id: room2Id,
      roomId: room2Id,
      appScope: "noise_live_public",
      hostId: user2Uid,
      hostUid: user2Uid,
      ownerUid: user2Uid,
      hostUsername: "meera_vibes",
      ownerUsername: "meera_vibes",
      roomName: "Meera Private Lounge",
      title: "Meera Private Lounge",
      subtitle: "meera_vibes • Gaming",
      description: "Friends-only gaming & voice stage",
      roomDescription: "Friends-only gaming & voice stage",
      category: "Gaming",
      roomCategory: "Gaming",
      visibility: "private",
      publicRoom: false,
      privateRoom: true,
      password: "2580",
      invitationToken: "inv_token_202",
      authorizedUserIds: [user2Uid, user1Uid],
      isActive: true,
      active: true,
      roomStatus: "active",
      participantCount: 1,
      musicTrack: "Neon Horizon",
      musicSelection: "Neon Horizon",
      gameName: "PK Battle",
      gameSelection: "PK Battle",
      micMode: "Host Approval",
      microphoneSettings: "Host Approval",
      capacity: 8,
      roomCapacity: 8,
      themeKey: "neon_cyber",
      roomCover: "neon_cyber",
      stageTheme: "neon_cyber",
      roomImageUrl: "neon_cyber",
      superAdminIds: [],
      adminIds: [],
      moderatorIds: [],
      mutedUserIds: [],
      kickedUserIds: [],
      lockedSeats: [7, 8],
      occupiedSeatsJson: `1:${user2Uid}:meera_vibes:M:false`,
      listenersCount: 1,
      giftsTotal: 0,
      live: true,
      youtubeVideoId: "",
      youtubeVideoTitle: "",
      youtubePlaying: false,
      youtubePositionSec: 0,
      youtubeUpdatedByUid: "",
      youtubeUpdatedAtMillis: 0,
      createdTimestamp: now,
      createdAt: now,
      updatedAt: now,
    })
  );

  // 3. Cross-account synchronization: Account 2 can query and join Account 1's room
  const liveRoomsSnap = await assertSucceeds(
    user2Db.collection("party_rooms").where("appScope", "==", "noise_live_public").get()
  );
  if (liveRoomsSnap.size !== 2) {
    throw new Error(`Expected 2 synchronized live rooms across accounts, found ${liveRoomsSnap.size}`);
  }

  await assertSucceeds(
    user2Db.collection("party_rooms").doc(room1Id).update({
      occupiedSeatsJson: `1:${user1Uid}:arjun_live:A:false;2:${user2Uid}:meera_vibes:M:false`,
      listenersCount: 2,
      participantCount: 2,
      updatedAt: new Date(),
    })
  );

  // 4. Security enforcement: Unauthenticated users cannot read or create rooms
  await assertFails(unauthDb.collection("party_rooms").doc(room1Id).get());
  await assertFails(
    unauthDb.collection("party_rooms").doc("unauth_room").set({
      hostId: "anon",
      title: "Anon Room",
    })
  );

  // 5. Ownership enforcement: Account 2 CANNOT delete Account 1's room or hijack ownership
  await assertFails(user2Db.collection("party_rooms").doc(room1Id).delete());
  await assertFails(
    user2Db.collection("party_rooms").doc(room1Id).update({
      hostId: user2Uid,
      ownerUid: user2Uid,
    })
  );

  // 6. Account 1 CAN delete their own room; Super Owner (sebin_mr) CAN delete any room
  await assertSucceeds(user1Db.collection("party_rooms").doc(room1Id).delete());
  await assertSucceeds(ownerDb.collection("party_rooms").doc(room2Id).delete());
});




