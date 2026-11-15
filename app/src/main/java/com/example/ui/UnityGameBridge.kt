package com.example.ui

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import com.example.data.GameRoomDoc
import com.example.data.GraphicsQualityLevel
import com.example.data.UserProfile
import org.json.JSONArray
import org.json.JSONObject

/**
 * Hybrid Unity 6 (URP + Netcode for GameObjects + Unity Lobby/Relay) & Android Bridge.
 *
 * Passes:
 * - Current User ID, Username, Profile Photo / Avatar Key
 * - Room ID, Room Code, Selected Game Type, Unity Scene Name
 * - Player List (Slot index, UID, Username, Team, Score)
 * - Voice Channel ID & LiveKit Audio Connection State
 * - Selected Graphics Quality Level (Low / Medium / High) & Target FPS
 *
 * When the exported `unityLibrary` module from `/unity_project/NoiseLiveGamesUnity/` is linked,
 * `launchUnityActivityIfAvailable` starts `com.unity3d.player.UnityPlayerActivity` with the JSON
 * session payload; otherwise the built-in interactive 3D-projected Physics & Multiplayer Engine
 * runs directly inside the NOISE LIVE Android window while keeping LiveKit voice chat connected.
 */
object UnityGameBridge {

    const val UNITY_PLAYER_ACTIVITY_CLASS = "com.unity3d.player.UnityPlayerActivity"
    const val EXTRA_NOISE_UNITY_PAYLOAD = "NOISE_UNITY_SESSION_PAYLOAD"
    const val UNITY_GAME_OBJECT_RECEIVER = "NoiseUnityBridge"
    const val UNITY_METHOD_INITIALIZE = "InitializeFromAndroidJson"

    fun isUnityLibraryLinked(): Boolean {
        return try {
            Class.forName(UNITY_PLAYER_ACTIVITY_CLASS)
            true
        } catch (_: Throwable) {
            false
        }
    }

    fun recommendGraphicsQuality(context: Context): GraphicsQualityLevel {
        return try {
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            val memInfo = ActivityManager.MemoryInfo()
            activityManager?.getMemoryInfo(memInfo)
            val totalRamGb = memInfo.totalMem / (1024.0 * 1024.0 * 1024.0)
            when {
                totalRamGb >= 7.0 -> GraphicsQualityLevel.HIGH
                totalRamGb >= 4.0 -> GraphicsQualityLevel.MEDIUM
                else -> GraphicsQualityLevel.LOW
            }
        } catch (_: Exception) {
            GraphicsQualityLevel.HIGH
        }
    }

    fun buildSessionPayloadJson(
        currentUser: UserProfile,
        gameRoom: GameRoomDoc,
        unitySceneName: String,
        graphicsQuality: GraphicsQualityLevel,
        liveKitConnected: Boolean,
        micMuted: Boolean
    ): String {
        val root = JSONObject()
        root.put("engine", "Unity 6000.0 URP")
        root.put("networkStack", "NetcodeForGameObjects + UnityLobby + UnityRelay")
        root.put("userId", currentUser.userId.ifBlank { currentUser.id })
        root.put("username", currentUser.username)
        root.put("displayName", currentUser.displayName)
        root.put("profilePhoto", currentUser.profilePictureUrl.ifBlank { currentUser.avatarKey })
        root.put("roomId", gameRoom.id)
        root.put("roomCode", gameRoom.roomCode)
        root.put("chatRoomId", gameRoom.chatRoomId)
        root.put("selectedGame", gameRoom.gameType)
        root.put("gameTitle", gameRoom.gameTitle)
        root.put("unitySceneName", unitySceneName)
        root.put("unityLobbyId", gameRoom.unityLobbyId)
        root.put("unityRelayJoinCode", gameRoom.unityRelayJoinCode)
        root.put("voiceChannelId", gameRoom.voiceChannelId)
        root.put("liveKitVoiceConnected", liveKitConnected)
        root.put("micMuted", micMuted)
        root.put("graphicsQuality", graphicsQuality.name)
        root.put("targetFps", graphicsQuality.targetFps)
        root.put("renderScale", graphicsQuality.renderScale.toDouble())

        val playersArray = JSONArray()
        gameRoom.parsePlayers().forEach { slot ->
            val pObj = JSONObject()
            pObj.put("slotIndex", slot.slotIndex)
            pObj.put("userId", slot.userId)
            pObj.put("username", slot.username)
            pObj.put("displayName", slot.displayName)
            pObj.put("avatarKey", slot.avatarKey)
            pObj.put("team", slot.team)
            pObj.put("score", slot.score)
            playersArray.put(pObj)
        }
        root.put("playerList", playersArray)
        return root.toString()
    }

    fun launchUnityActivityIfAvailable(
        context: Context,
        payloadJson: String
    ): Boolean {
        if (!isUnityLibraryLinked()) return false
        return try {
            val clazz = Class.forName(UNITY_PLAYER_ACTIVITY_CLASS)
            val intent = Intent(context, clazz).apply {
                putExtra(EXTRA_NOISE_UNITY_PAYLOAD, payloadJson)
                addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
            context.startActivity(intent)
            true
        } catch (_: Throwable) {
            false
        }
    }
}
