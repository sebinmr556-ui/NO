package com.example

import android.content.Context
import android.media.AudioManager
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.example.ui.AudioOutputRoute
import com.example.ui.LiveKitAudioRouter
import com.example.ui.VERCEL_LIVE_URL
import com.example.ui.isTrustedVercelOrigin
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], manifest = Config.NONE)
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Noise Live", appName)
    }

    @Test
    fun `isTrustedVercelOrigin validates trusted HTTPS Vercel origin and blocks untrusted origins`() {
        assertTrue(isTrustedVercelOrigin(Uri.parse(VERCEL_LIVE_URL)))
        assertTrue(isTrustedVercelOrigin(Uri.parse("https://noiselive-b1kzur2k4-no-team-188e.vercel.app/room/123")))
        assertTrue(isTrustedVercelOrigin(Uri.parse("https://noise-new-style.vercel.app")))

        // Reject non-HTTPS or untrusted domains
        assertFalse(isTrustedVercelOrigin(Uri.parse("http://noiselive-b1kzur2k4-no-team-188e.vercel.app")))
        assertFalse(isTrustedVercelOrigin(Uri.parse("https://malicious-site.example.com")))
        assertFalse(isTrustedVercelOrigin(Uri.parse("https://otherapp.vercel.app")))
        assertFalse(isTrustedVercelOrigin(null))
    }

    @Test
    fun `LiveKitAudioRouter manages voice room session, audio output routes, and mic mute state`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val router = LiveKitAudioRouter(context)

        // Enter LiveKit voice room
        router.enterVoiceRoomSession("Zama Team VIP Stage")
        assertTrue(router.state.value.isInVoiceRoom)
        assertEquals("Zama Team VIP Stage", router.state.value.activeRoomName)
        assertEquals(AudioManager.MODE_IN_COMMUNICATION, audioManager.mode)

        // Switch between Earpiece, Bluetooth, and Speaker
        router.selectAudioRoute(AudioOutputRoute.EARPIECE)
        assertEquals(AudioOutputRoute.EARPIECE, router.state.value.currentRoute)

        router.selectAudioRoute(AudioOutputRoute.BLUETOOTH)
        assertEquals(AudioOutputRoute.BLUETOOTH, router.state.value.currentRoute)

        router.selectAudioRoute(AudioOutputRoute.SPEAKER)
        assertEquals(AudioOutputRoute.SPEAKER, router.state.value.currentRoute)

        // Microphone mute/unmute & speaking indicator sync
        router.updateSpeakingState(isLocalSpeaking = true, activeSpeakers = listOf("sebin_mr"), participantCount = 3)
        assertTrue(router.state.value.isLocalSpeaking)
        assertEquals(listOf("sebin_mr"), router.state.value.activeSpeakerNames)

        router.setMicrophoneMuted(true)
        assertTrue(router.state.value.isMicMuted)
        assertFalse("Muted mic must not report local speaking", router.state.value.isLocalSpeaking)

        router.setMicrophoneMuted(false)
        assertFalse(router.state.value.isMicMuted)

        // Leave voice room & restore normal mode
        router.leaveVoiceRoomSession()
        assertFalse(router.state.value.isInVoiceRoom)
        assertEquals(AudioManager.MODE_NORMAL, audioManager.mode)
        router.release()
    }

    @Test
    fun `persistPickedMediaUri copies picked media to durable internal storage and detects video vs photo`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val tempSourceVideo = java.io.File(context.cacheDir, "sample_clip.mp4").apply {
            writeBytes(ByteArray(256) { (it % 127).toByte() })
        }
        val videoUri = Uri.fromFile(tempSourceVideo)

        val (persistedVideoKey, detectedVideoType) = com.example.ui.persistPickedMediaUri(
            context = context,
            uri = videoUri,
            fallbackMediaType = "photo"
        )
        assertEquals("video", detectedVideoType)
        assertTrue(persistedVideoKey.startsWith("file://"))
        val persistedVideoFile = java.io.File(Uri.parse(persistedVideoKey).path!!)
        assertTrue("Persisted video file must exist in durable filesDir/persisted_media", persistedVideoFile.exists())
        assertEquals(256L, persistedVideoFile.length())

        val tempSourcePhoto = java.io.File(context.cacheDir, "sample_photo.jpg").apply {
            writeBytes(ByteArray(128) { 42 })
        }
        val photoUri = Uri.fromFile(tempSourcePhoto)
        val (persistedPhotoKey, detectedPhotoType) = com.example.ui.persistPickedMediaUri(
            context = context,
            uri = photoUri,
            fallbackMediaType = "photo"
        )
        assertEquals("photo", detectedPhotoType)
        assertTrue(persistedPhotoKey.startsWith("file://"))

        tempSourceVideo.delete()
        tempSourcePhoto.delete()
        persistedVideoFile.delete()
        java.io.File(Uri.parse(persistedPhotoKey).path!!).delete()
    }

    @Test
    fun `canonicalConversationId produces identical shared conversation ID for Account A and Account B`() {
        val accountA = "uid_alice_001"
        val accountB = "uid_bob_002"
        val fromA = com.example.data.NoiseRepository.canonicalConversationId(accountA, accountB)
        val fromB = com.example.data.NoiseRepository.canonicalConversationId(accountB, accountA)
        assertEquals(fromA, fromB)
        assertEquals("conv_uid_alice_001_uid_bob_002", fromA)
    }

    @Test
    fun `NoiseNotificationDispatcher dispatches incoming Account B notifications and blocks self notifications`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        com.example.ui.NoiseNotificationDispatcher.clearCacheForTests()
        val accountA = "uid_alice_001"
        val accountB = "uid_bob_002"

        val selfNotif = com.example.data.NotificationItem(
            id = "notif_self_1",
            userId = accountA,
            actorUid = accountA,
            actorUsername = "alice",
            type = "message",
            message = "sent you a message: Hello",
            conversationId = "conv_uid_alice_001_uid_bob_002",
            read = false
        )
        assertFalse(
            "Must never trigger notification for user's own outgoing message",
            com.example.ui.NoiseNotificationDispatcher.dispatchNotificationItem(context, accountA, selfNotif)
        )

        val incomingMsgNotif = com.example.data.NotificationItem(
            id = "notif_incoming_ab_1",
            userId = accountB,
            actorUid = accountA,
            actorUsername = "alice",
            type = "message",
            message = "sent you a message: Ready for Ludo?",
            conversationId = "conv_uid_alice_001_uid_bob_002",
            read = false
        )
        assertTrue(
            "Must dispatch notification for Account B when Account A sends a message",
            com.example.ui.NoiseNotificationDispatcher.dispatchNotificationItem(context, accountB, incomingMsgNotif)
        )
        assertFalse(
            "Must deduplicate already-dispatched notification ID",
            com.example.ui.NoiseNotificationDispatcher.dispatchNotificationItem(context, accountB, incomingMsgNotif)
        )
    }

    @Test
    fun `Ludo multiplayer player slots preserve slotIndex 1 to 4 and real user identities`() {
        val slot1 = com.example.data.GamePlayerSlot(
            slotIndex = 1,
            userId = "uid_alice_001",
            username = "alice",
            displayName = "Alice",
            avatarKey = "A",
            team = 1,
            ready = true
        )
        val slot2 = com.example.data.GamePlayerSlot(
            slotIndex = 2,
            userId = "uid_bob_002",
            username = "bob",
            displayName = "Bob",
            avatarKey = "B",
            team = 2,
            ready = true
        )
        val encoded = com.example.data.encodeGamePlayers(listOf(slot1, slot2))
        val room = com.example.data.GameRoomDoc(
            id = "ludo_room_1",
            gameType = "ludo_3d",
            status = "waiting",
            playersJson = encoded
        )
        val parsed = room.parsePlayers()
        assertEquals(2, parsed.size)
        assertEquals(1, parsed[0].slotIndex)
        assertEquals("uid_alice_001", parsed[0].userId)
        assertEquals(2, parsed[1].slotIndex)
        assertEquals("uid_bob_002", parsed[1].userId)
    }
}

