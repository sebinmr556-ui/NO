package com.example.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.AudioAttributes
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.BluetoothAudio
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import com.example.ui.theme.NoiseBlack
import com.example.ui.theme.NoiseBorder
import com.example.ui.theme.NoisePink
import com.example.ui.theme.NoisePurple
import com.example.ui.theme.NoiseTextSecondary
import com.example.ui.theme.NoiseWhite
import com.example.ui.theme.OnlineGreen
import com.example.ui.theme.VipGold
import com.example.ui.theme.VipGoldBorder
import com.google.firebase.auth.FirebaseAuth
import io.livekit.android.ConnectOptions
import io.livekit.android.LiveKit
import io.livekit.android.events.RoomEvent
import io.livekit.android.events.collect
import io.livekit.android.renderer.TextureViewRenderer
import io.livekit.android.room.Room
import io.livekit.android.room.participant.Participant
import io.livekit.android.room.track.CameraPosition
import io.livekit.android.room.track.LocalVideoTrack
import io.livekit.android.room.track.Track
import io.livekit.android.room.track.VideoTrack
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import livekit.org.webrtc.RendererCommon
import org.json.JSONArray
import org.json.JSONObject

/**
 * Production Vercel LiveKit Token Service URL for NOICE LIVE.
 * Note: LIVEKIT_API_KEY and LIVEKIT_API_SECRET remain exclusively on the Vercel server
 * and are NEVER stored or embedded in the Android application.
 */
const val VERCEL_LIVE_URL = "https://noice-live26.vercel.app"

/**
 * Verified Vercel API endpoints on `https://noice-live26.vercel.app`.
 */
const val VERCEL_HEALTH_ENDPOINT = "$VERCEL_LIVE_URL/api/health"
const val VERCEL_TOKEN_ENDPOINT = "$VERCEL_LIVE_URL/api/livekit/token"
const val VERCEL_ROOMS_ENDPOINT = "$VERCEL_LIVE_URL/api/livekit/rooms"
const val VERCEL_CALLS_ENDPOINT = "$VERCEL_LIVE_URL/api/livekit/calls"

private val TRUSTED_VERCEL_HOSTS = setOf(
    "noice-live26.vercel.app",
    "noiselive-b1kzur2k4-no-team-188e.vercel.app",
    "noise-new-style.vercel.app"
)

/**
 * Strictly validates that a WebView permission or bridge origin belongs to the trusted
 * Vercel HTTPS deployment before granting sensitive hardware access (e.g. microphone or camera).
 */
fun isTrustedVercelOrigin(originUri: Uri?): Boolean {
    if (originUri == null) return false
    val scheme = originUri.scheme?.lowercase() ?: return false
    if (scheme != "https") return false
    val host = originUri.host?.lowercase() ?: return false
    return host in TRUSTED_VERCEL_HOSTS ||
        (host.endsWith(".vercel.app") &&
            (host.startsWith("noice-live") || host.startsWith("noiselive") || host.startsWith("noise-")))
}

/**
 * Health check snapshot returned by `GET https://noice-live26.vercel.app/api/health`.
 */
data class VercelBackendHealthStatus(
    val ok: Boolean,
    val service: String = "noice-live-livekit",
    val httpCode: Int = 200,
    val livekitUrlConfigured: Boolean = false,
    val livekitApiKeyConfigured: Boolean = false,
    val livekitApiSecretConfigured: Boolean = false,
    val firebaseProjectIdConfigured: Boolean = false,
    val firebaseClientEmailConfigured: Boolean = false,
    val firebasePrivateKeyConfigured: Boolean = false,
    val diagnosticMessage: String = ""
) {
    val missingEnvVars: List<String>
        get() = buildList {
            if (!livekitUrlConfigured) add("LIVEKIT_URL")
            if (!livekitApiKeyConfigured) add("LIVEKIT_API_KEY")
            if (!livekitApiSecretConfigured) add("LIVEKIT_API_SECRET")
            if (!firebaseProjectIdConfigured) add("FIREBASE_PROJECT_ID")
            if (!firebaseClientEmailConfigured) add("FIREBASE_CLIENT_EMAIL")
            if (!firebasePrivateKeyConfigured) add("FIREBASE_PRIVATE_KEY")
        }
}

/**
 * Result of requesting a room-scoped LiveKit access token from the Vercel backend.
 */
sealed class VercelLiveKitTokenResult {
    data class Success(
        val wsUrl: String,
        val token: String,
        val roomName: String,
        val participantIdentity: String,
        val endpointUsed: String
    ) : VercelLiveKitTokenResult()

    data class Failure(
        val httpCode: Int,
        val errorTitle: String,
        val errorMessage: String,
        val isAuthError: Boolean = false,
        val isServerConfigError: Boolean = false,
        val missingServerEnvVars: List<String> = emptyList()
    ) : VercelLiveKitTokenResult()
}

/**
 * Secure HTTP client connecting Android to the deployed Vercel LiveKit Token Service
 * (`https://noice-live26.vercel.app`).
 *
 * Security guarantees:
 * - Authenticates every request with the user's Firebase ID token (`Authorization: Bearer <idToken>`).
 * - Automatically retries once with `getIdToken(true)` if the token has expired (`HTTP 401`).
 * - Sends the authenticated Firebase UID (`uid`, `identity`) and requested `roomId` / `roomName`.
 * - Never embeds `LIVEKIT_API_KEY` or `LIVEKIT_API_SECRET` in the Android client.
 */
object VercelLiveKitTokenClient {

    private fun encodePathSegmentSafe(segment: String): String {
        val trimmed = segment.trim()
        val androidEncoded = try {
            Uri.encode(trimmed)
        } catch (_: Throwable) {
            null
        }
        return if (!androidEncoded.isNullOrBlank()) {
            androidEncoded
        } else {
            try {
                java.net.URLEncoder.encode(trimmed, "UTF-8").replace("+", "%20")
            } catch (_: Throwable) {
                trimmed
            }
        }
    }

    fun buildRoomTokenUrl(roomId: String, baseUrl: String = VERCEL_LIVE_URL): String {
        val cleanBase = baseUrl.trimEnd('/')
        val encodedRoom = encodePathSegmentSafe(roomId)
        return "$cleanBase/api/livekit/rooms/$encodedRoom/token"
    }

    fun buildRoomDetailUrl(roomId: String, baseUrl: String = VERCEL_LIVE_URL): String {
        val cleanBase = baseUrl.trimEnd('/')
        val encodedRoom = encodePathSegmentSafe(roomId)
        return "$cleanBase/api/livekit/rooms/$encodedRoom"
    }

    fun buildDirectTokenUrl(baseUrl: String = VERCEL_LIVE_URL): String {
        return "${baseUrl.trimEnd('/')}/api/livekit/token"
    }

    fun buildRoomsCreateUrl(baseUrl: String = VERCEL_LIVE_URL): String {
        return "${baseUrl.trimEnd('/')}/api/livekit/rooms"
    }

    fun buildCallsUrl(baseUrl: String = VERCEL_LIVE_URL): String {
        return "${baseUrl.trimEnd('/')}/api/livekit/calls"
    }

    private fun regexExtractJsonString(rawJson: String, key: String): String {
        val pattern = Regex("\"${Regex.escape(key)}\"\\s*:\\s*\"([^\"]*)\"")
        return pattern.find(rawJson)?.groupValues?.getOrNull(1)?.trim().orEmpty()
    }

    private fun regexExtractJsonBoolean(rawJson: String, key: String, defaultVal: Boolean): Boolean {
        val pattern = Regex("\"${Regex.escape(key)}\"\\s*:\\s*(true|false)", RegexOption.IGNORE_CASE)
        val matched = pattern.find(rawJson)?.groupValues?.getOrNull(1)?.lowercase() ?: return defaultVal
        return matched == "true"
    }

    private fun safeOptObj(obj: JSONObject?, key: String): JSONObject? {
        if (obj == null) return null
        return try {
            obj.optJSONObject(key)
        } catch (_: Throwable) {
            null
        }
    }

    private fun safeOptStr(obj: JSONObject?, key: String): String {
        if (obj == null) return ""
        return try {
            obj.optString(key).orEmpty()
        } catch (_: Throwable) {
            ""
        }
    }

    private fun safeOptBool(obj: JSONObject?, key: String): Boolean? {
        if (obj == null) return null
        return try {
            if (obj.has(key)) obj.optBoolean(key) else null
        } catch (_: Throwable) {
            null
        }
    }

    /**
     * Parses a JSON response from `/api/livekit/rooms/:roomName/token`, `/api/livekit/token`,
     * `/api/livekit/rooms`, or `/api/livekit/calls`.
     */
    fun parseTokenResponseJson(
        rawJson: String,
        fallbackRoomName: String,
        fallbackIdentity: String,
        endpointUsed: String
    ): VercelLiveKitTokenResult.Success? {
        if (rawJson.isBlank()) return null
        val root = try {
            JSONObject(rawJson)
        } catch (_: Throwable) {
            null
        }
        val dataObj = safeOptObj(root, "data") ?: safeOptObj(root, "result") ?: root

        val wsUrl = sequenceOf(
            safeOptStr(dataObj, "wsUrl"),
            safeOptStr(dataObj, "url"),
            safeOptStr(dataObj, "serverUrl"),
            safeOptStr(dataObj, "livekitUrl"),
            safeOptStr(root, "wsUrl"),
            safeOptStr(root, "url"),
            safeOptStr(root, "serverUrl"),
            safeOptStr(root, "livekitUrl"),
            regexExtractJsonString(rawJson, "wsUrl"),
            regexExtractJsonString(rawJson, "url"),
            regexExtractJsonString(rawJson, "serverUrl"),
            regexExtractJsonString(rawJson, "livekitUrl")
        ).map { it.trim() }.firstOrNull { it.isNotEmpty() && !it.equals("null", ignoreCase = true) }.orEmpty()

        val token = sequenceOf(
            safeOptStr(dataObj, "token"),
            safeOptStr(dataObj, "accessToken"),
            safeOptStr(dataObj, "jwt"),
            safeOptStr(root, "token"),
            safeOptStr(root, "accessToken"),
            safeOptStr(root, "jwt"),
            regexExtractJsonString(rawJson, "token"),
            regexExtractJsonString(rawJson, "accessToken"),
            regexExtractJsonString(rawJson, "jwt")
        ).map { it.trim() }.firstOrNull { it.isNotEmpty() && !it.equals("null", ignoreCase = true) }.orEmpty()

        if (wsUrl.isBlank() || token.isBlank()) {
            return null
        }

        val roomObj = safeOptObj(dataObj, "room")
        val resolvedRoom = sequenceOf(
            safeOptStr(dataObj, "roomName"),
            safeOptStr(dataObj, "roomId"),
            safeOptStr(roomObj, "name"),
            safeOptStr(root, "roomName"),
            regexExtractJsonString(rawJson, "roomName"),
            regexExtractJsonString(rawJson, "roomId"),
            fallbackRoomName
        ).map { it.trim() }.firstOrNull { it.isNotEmpty() && !it.equals("null", ignoreCase = true) }
            ?: fallbackRoomName

        val resolvedIdentity = sequenceOf(
            safeOptStr(dataObj, "identity"),
            safeOptStr(dataObj, "participantIdentity"),
            safeOptStr(dataObj, "uid"),
            safeOptStr(root, "identity"),
            regexExtractJsonString(rawJson, "identity"),
            regexExtractJsonString(rawJson, "participantIdentity"),
            regexExtractJsonString(rawJson, "uid"),
            fallbackIdentity
        ).map { it.trim() }.firstOrNull { it.isNotEmpty() && !it.equals("null", ignoreCase = true) }
            ?: fallbackIdentity

        return VercelLiveKitTokenResult.Success(
            wsUrl = wsUrl,
            token = token,
            roomName = resolvedRoom,
            participantIdentity = resolvedIdentity,
            endpointUsed = endpointUsed
        )
    }

    /**
     * Parses `GET /api/health` JSON payload from `https://noice-live26.vercel.app/api/health`.
     */
    fun parseHealthResponseJson(rawJson: String, httpCode: Int): VercelBackendHealthStatus {
        val root = try {
            JSONObject(rawJson)
        } catch (_: Throwable) {
            null
        }
        val cfg = safeOptObj(root, "config")

        val defaultOk = httpCode in 200..299
        val ok = safeOptBool(root, "ok") ?: regexExtractJsonBoolean(rawJson, "ok", defaultOk)
        val service = safeOptStr(root, "service").takeIf { it.isNotBlank() }
            ?: regexExtractJsonString(rawJson, "service").ifBlank { "noice-live-livekit" }

        val lkUrl = safeOptBool(cfg, "LIVEKIT_URL") ?: regexExtractJsonBoolean(rawJson, "LIVEKIT_URL", true)
        val lkKey = safeOptBool(cfg, "LIVEKIT_API_KEY") ?: regexExtractJsonBoolean(rawJson, "LIVEKIT_API_KEY", true)
        val lkSecret = safeOptBool(cfg, "LIVEKIT_API_SECRET") ?: regexExtractJsonBoolean(rawJson, "LIVEKIT_API_SECRET", true)
        val fbProject = safeOptBool(cfg, "FIREBASE_PROJECT_ID") ?: regexExtractJsonBoolean(rawJson, "FIREBASE_PROJECT_ID", ok)
        val fbEmail = safeOptBool(cfg, "FIREBASE_CLIENT_EMAIL") ?: regexExtractJsonBoolean(rawJson, "FIREBASE_CLIENT_EMAIL", ok)
        val fbKey = safeOptBool(cfg, "FIREBASE_PRIVATE_KEY") ?: regexExtractJsonBoolean(rawJson, "FIREBASE_PRIVATE_KEY", ok)

        val status = VercelBackendHealthStatus(
            ok = ok,
            service = service,
            httpCode = httpCode,
            livekitUrlConfigured = lkUrl,
            livekitApiKeyConfigured = lkKey,
            livekitApiSecretConfigured = lkSecret,
            firebaseProjectIdConfigured = fbProject,
            firebaseClientEmailConfigured = fbEmail,
            firebasePrivateKeyConfigured = fbKey
        )
        val missing = status.missingEnvVars
        val msg = if (ok && missing.isEmpty()) {
            "Vercel LiveKit Token Service ($service) is healthy and ready."
        } else {
            "Vercel server ($VERCEL_LIVE_URL) is missing environment variables: ${missing.joinToString(", ")}. " +
                "Set FIREBASE_PROJECT_ID=project-7eddf9da-6480-4ac9-a2d, FIREBASE_CLIENT_EMAIL, and FIREBASE_PRIVATE_KEY in Vercel Project Settings."
        }
        return status.copy(diagnosticMessage = msg)
    }

    /**
     * Checks `GET https://noice-live26.vercel.app/api/health`.
     */
    suspend fun checkBackendHealth(baseUrl: String = VERCEL_LIVE_URL): VercelBackendHealthStatus =
        withContext(Dispatchers.IO) {
            val healthUrl = "${baseUrl.trimEnd('/')}/api/health"
            var conn: HttpURLConnection? = null
            try {
                conn = (URL(healthUrl).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 10_000
                    readTimeout = 10_000
                    setRequestProperty("Accept", "application/json")
                }
                val code = conn.responseCode
                val body = readStreamSafe(conn, code)
                parseHealthResponseJson(body, code)
            } catch (e: Exception) {
                VercelBackendHealthStatus(
                    ok = false,
                    httpCode = -1,
                    diagnosticMessage = "Network error reaching $healthUrl: ${e.localizedMessage ?: "check internet connection"}"
                )
            } finally {
                conn?.disconnect()
            }
        }

    /**
     * Fetches a fresh Firebase Authentication ID token for the currently signed-in user.
     */
    suspend fun getFirebaseIdToken(forceRefresh: Boolean = false): Result<Pair<String, String>> {
        return try {
            val currentUser = FirebaseAuth.getInstance().currentUser
                ?: return Result.failure(IllegalStateException("User is not signed in to Firebase Authentication."))
            val tokenResult = currentUser.getIdToken(forceRefresh).await()
            val idToken = tokenResult?.token?.trim().orEmpty()
            if (idToken.isBlank()) {
                Result.failure(IllegalStateException("Firebase ID token was empty."))
            } else {
                Result.success(currentUser.uid to idToken)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Requests a LiveKit room token for a group voice or video chatroom from the production Vercel service.
     *
     * Verifies and tries:
     * 1. `POST https://noice-live26.vercel.app/api/livekit/rooms/{roomId}/token`
     * 2. `POST https://noice-live26.vercel.app/api/livekit/token`
     * 3. `POST https://noice-live26.vercel.app/api/livekit/rooms` (if room needs initial creation on LiveKit)
     */
    suspend fun fetchChatroomToken(
        roomId: String,
        currentUserId: String,
        currentUsername: String,
        isPrivate: Boolean = false,
        baseUrl: String = VERCEL_LIVE_URL
    ): VercelLiveKitTokenResult = withContext(Dispatchers.IO) {
        val cleanRoomId = roomId.trim()
        if (cleanRoomId.isBlank()) {
            return@withContext VercelLiveKitTokenResult.Failure(
                httpCode = 400,
                errorTitle = "Invalid Room ID",
                errorMessage = "Cannot request LiveKit token for an empty room ID."
            )
        }

        val authPair = getFirebaseIdToken(forceRefresh = false).getOrNull()
        val effectiveUid = authPair?.first?.ifBlank { currentUserId } ?: currentUserId
        val idToken = authPair?.second.orEmpty()

        if (effectiveUid.isBlank() || idToken.isBlank()) {
            return@withContext VercelLiveKitTokenResult.Failure(
                httpCode = 401,
                errorTitle = "Authentication Required",
                errorMessage = "Please sign in with Firebase Authentication to join LiveKit voice chatrooms.",
                isAuthError = true
            )
        }

        val payloadJson = JSONObject().apply {
            put("roomId", cleanRoomId)
            put("roomName", cleanRoomId)
            put("room", cleanRoomId)
            put("uid", effectiveUid)
            put("userId", effectiveUid)
            put("identity", effectiveUid)
            put("username", currentUsername.ifBlank { effectiveUid })
            put("name", currentUsername.ifBlank { effectiveUid })
            put("participantName", currentUsername.ifBlank { effectiveUid })
            put("isPrivate", isPrivate)
            put("canPublish", true)
            put("canSubscribe", true)
            put("canPublishData", true)
        }.toString()

        val candidateEndpoints = listOf(
            buildRoomTokenUrl(cleanRoomId, baseUrl),
            buildDirectTokenUrl(baseUrl),
            buildRoomsCreateUrl(baseUrl)
        )

        executeTokenRequestWithFallback(
            candidateEndpoints = candidateEndpoints,
            payloadJson = payloadJson,
            idToken = idToken,
            effectiveUid = effectiveUid,
            roomName = cleanRoomId,
            baseUrl = baseUrl
        )
    }

    /**
     * Requests a LiveKit token for a 1-on-1 private audio or video call.
     *
     * Tries:
     * 1. `POST https://noice-live26.vercel.app/api/livekit/calls`
     * 2. `POST https://noice-live26.vercel.app/api/livekit/rooms/{liveKitRoomName}/token`
     * 3. `POST https://noice-live26.vercel.app/api/livekit/token`
     */
    suspend fun fetchPrivateCallToken(
        callId: String,
        liveKitRoomName: String,
        currentUserId: String,
        currentUsername: String,
        peerUserId: String,
        callType: String,
        baseUrl: String = VERCEL_LIVE_URL
    ): VercelLiveKitTokenResult = withContext(Dispatchers.IO) {
        val cleanRoom = liveKitRoomName.trim().ifBlank { "call_$callId" }
        val authPair = getFirebaseIdToken(forceRefresh = false).getOrNull()
        val effectiveUid = authPair?.first?.ifBlank { currentUserId } ?: currentUserId
        val idToken = authPair?.second.orEmpty()

        if (effectiveUid.isBlank() || idToken.isBlank()) {
            return@withContext VercelLiveKitTokenResult.Failure(
                httpCode = 401,
                errorTitle = "Authentication Required",
                errorMessage = "Please sign in with Firebase Authentication to place or answer LiveKit calls.",
                isAuthError = true
            )
        }

        val normalizedCallType = if (callType.equals("video", ignoreCase = true)) "video" else "voice"
        val payloadJson = JSONObject().apply {
            put("callId", callId)
            put("roomName", cleanRoom)
            put("roomId", cleanRoom)
            put("room", cleanRoom)
            put("uid", effectiveUid)
            put("userId", effectiveUid)
            put("identity", effectiveUid)
            put("callerUid", effectiveUid)
            put("calleeUid", peerUserId)
            put("targetUserId", peerUserId)
            put("username", currentUsername.ifBlank { effectiveUid })
            put("name", currentUsername.ifBlank { effectiveUid })
            put("participantName", currentUsername.ifBlank { effectiveUid })
            put("callType", normalizedCallType)
            put("type", normalizedCallType)
            put("canPublish", true)
            put("canSubscribe", true)
            put("canPublishData", true)
        }.toString()

        val candidateEndpoints = listOf(
            buildCallsUrl(baseUrl),
            buildRoomTokenUrl(cleanRoom, baseUrl),
            buildDirectTokenUrl(baseUrl)
        )

        executeTokenRequestWithFallback(
            candidateEndpoints = candidateEndpoints,
            payloadJson = payloadJson,
            idToken = idToken,
            effectiveUid = effectiveUid,
            roomName = cleanRoom,
            baseUrl = baseUrl
        )
    }

    /**
     * Notifies `DELETE https://noice-live26.vercel.app/api/livekit/rooms/{roomId}` when the room host
     * closes and deletes a room so all LiveKit participants are disconnected server-side.
     */
    suspend fun deleteRoomOnBackend(
        roomId: String,
        baseUrl: String = VERCEL_LIVE_URL
    ): Boolean = withContext(Dispatchers.IO) {
        val cleanRoomId = roomId.trim()
        if (cleanRoomId.isBlank()) return@withContext false
        val authPair = getFirebaseIdToken(forceRefresh = false).getOrNull() ?: return@withContext false
        val endpoint = buildRoomDetailUrl(cleanRoomId, baseUrl)
        var conn: HttpURLConnection? = null
        try {
            conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                requestMethod = "DELETE"
                connectTimeout = 10_000
                readTimeout = 10_000
                setRequestProperty("Authorization", "Bearer ${authPair.second}")
                setRequestProperty("X-Firebase-Uid", authPair.first)
                setRequestProperty("Accept", "application/json")
            }
            conn.responseCode in 200..299
        } catch (_: Exception) {
            false
        } finally {
            conn?.disconnect()
        }
    }

    private suspend fun executeTokenRequestWithFallback(
        candidateEndpoints: List<String>,
        payloadJson: String,
        idToken: String,
        effectiveUid: String,
        roomName: String,
        baseUrl: String
    ): VercelLiveKitTokenResult {
        var currentToken = idToken
        var retriedAuth = false
        var lastHttpCode = -1
        var lastErrorBody = ""

        for (endpoint in candidateEndpoints) {
            val (code, body) = postJson(endpoint, payloadJson, currentToken, effectiveUid)
            lastHttpCode = code
            lastErrorBody = body

            if (code in 200..299) {
                val parsed = parseTokenResponseJson(
                    rawJson = body,
                    fallbackRoomName = roomName,
                    fallbackIdentity = effectiveUid,
                    endpointUsed = endpoint
                )
                if (parsed != null) {
                    return parsed
                }
            }

            // If token expired (HTTP 401), force-refresh Firebase ID token once and retry this endpoint
            if (code == 401 && !retriedAuth) {
                retriedAuth = true
                val refreshed = getFirebaseIdToken(forceRefresh = true).getOrNull()
                if (refreshed != null && refreshed.second.isNotBlank()) {
                    currentToken = refreshed.second
                    val (retryCode, retryBody) = postJson(endpoint, payloadJson, currentToken, effectiveUid)
                    lastHttpCode = retryCode
                    lastErrorBody = retryBody
                    if (retryCode in 200..299) {
                        val parsed = parseTokenResponseJson(
                            rawJson = retryBody,
                            fallbackRoomName = roomName,
                            fallbackIdentity = effectiveUid,
                            endpointUsed = endpoint
                        )
                        if (parsed != null) {
                            return parsed
                        }
                    }
                }
            }

            // If 403 Forbidden (unauthorized for private room), stop immediately and report
            if (code == 403) {
                val serverMsg = extractServerErrorMessage(body)
                return VercelLiveKitTokenResult.Failure(
                    httpCode = 403,
                    errorTitle = "Room Access Denied",
                    errorMessage = serverMsg.ifBlank { "You do not have permission to join this private LiveKit room." },
                    isAuthError = true
                )
            }
        }

        // If 500 or 503 occurred, inspect /api/health to provide exact server diagnostic details
        if (lastHttpCode == 500 || lastHttpCode == 503) {
            val health = checkBackendHealth(baseUrl)
            if (!health.ok && health.missingEnvVars.isNotEmpty()) {
                return VercelLiveKitTokenResult.Failure(
                    httpCode = lastHttpCode,
                    errorTitle = "Vercel Server Environment Incomplete",
                    errorMessage = health.diagnosticMessage,
                    isServerConfigError = true,
                    missingServerEnvVars = health.missingEnvVars
                )
            }
        }

        val serverMsg = extractServerErrorMessage(lastErrorBody)
        return VercelLiveKitTokenResult.Failure(
            httpCode = lastHttpCode,
            errorTitle = if (lastHttpCode == 401) "Authentication Error" else "LiveKit Token Service Error",
            errorMessage = serverMsg.ifBlank {
                if (lastHttpCode > 0) {
                    "Vercel token service returned HTTP $lastHttpCode from $baseUrl."
                } else {
                    "Could not reach $baseUrl. Check your internet connection."
                }
            },
            isAuthError = lastHttpCode == 401
        )
    }

    private fun postJson(
        endpoint: String,
        payloadJson: String,
        bearerToken: String,
        firebaseUid: String
    ): Pair<Int, String> {
        var conn: HttpURLConnection? = null
        return try {
            conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = 12_000
                readTimeout = 12_000
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                setRequestProperty("Accept", "application/json")
                setRequestProperty("Authorization", "Bearer $bearerToken")
                setRequestProperty("X-Firebase-Uid", firebaseUid)
            }
            OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { writer ->
                writer.write(payloadJson)
                writer.flush()
            }
            val code = conn.responseCode
            val body = readStreamSafe(conn, code)
            code to body
        } catch (e: Exception) {
            -1 to (e.localizedMessage ?: "Network exception")
        } finally {
            conn?.disconnect()
        }
    }

    private fun readStreamSafe(conn: HttpURLConnection, code: Int): String {
        val stream = try {
            if (code in 200..299) conn.inputStream else conn.errorStream
        } catch (_: Exception) {
            null
        } ?: return ""
        return try {
            BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { it.readText() }
        } catch (_: Exception) {
            ""
        }
    }

    private fun extractServerErrorMessage(rawBody: String): String {
        if (rawBody.isBlank()) return ""
        return try {
            val json = JSONObject(rawBody)
            sequenceOf(
                json.optString("error"),
                json.optString("message"),
                json.optString("detail")
            ).map { it.trim() }.firstOrNull { it.isNotEmpty() && !it.equals("null", ignoreCase = true) }.orEmpty()
        } catch (_: Exception) {
            if (rawBody.trimStart().startsWith("<")) "" else rawBody.take(180)
        }
    }
}

/**
 * Connection lifecycle states for the real LiveKit Android SDK (`io.livekit.android.room.Room`).
 */
enum class LiveKitConnectionStatus(val label: String) {
    IDLE("Idle"),
    FETCHING_TOKEN("Requesting Token..."),
    CONNECTING("Connecting to LiveKit..."),
    CONNECTED("LiveKit Connected"),
    RECONNECTING("Reconnecting..."),
    DISCONNECTED("Disconnected"),
    AUTH_ERROR("Auth Required"),
    BACKEND_CONFIG_MISSING("Server Config Needed"),
    ERROR("Connection Error")
}

/**
 * Real-time participant snapshot from the LiveKit Room (`LocalParticipant` or `RemoteParticipant`).
 * Strictly represents real authenticated users connected to the room — never demo or fake users.
 */
data class LiveKitParticipantSnapshot(
    val identity: String,
    val displayName: String,
    val isLocal: Boolean,
    val isSpeaking: Boolean,
    val isMicEnabled: Boolean,
    val isCameraEnabled: Boolean,
    val audioLevel: Float = 0f,
    val videoTrack: VideoTrack? = null
)

/**
 * Reactive UI state exposed by [LiveKitRoomSessionManager] for both Group Voice/Video Chatrooms
 * and 1-on-1 Private Audio/Video Calls.
 */
data class LiveKitSessionUiState(
    val connectionStatus: LiveKitConnectionStatus = LiveKitConnectionStatus.IDLE,
    val roomId: String = "",
    val wsUrl: String = "",
    val endpointUsed: String = "",
    val localIdentity: String = "",
    val isMicEnabled: Boolean = false,
    val isCameraEnabled: Boolean = false,
    val isFrontCamera: Boolean = true,
    val isLocalSpeaking: Boolean = false,
    val participants: List<LiveKitParticipantSnapshot> = emptyList(),
    val activeSpeakerIds: Set<String> = emptySet(),
    val activeSpeakerNames: List<String> = emptyList(),
    val localVideoTrack: VideoTrack? = null,
    val primaryRemoteVideoTrack: VideoTrack? = null,
    val statusMessage: String = "Ready",
    val errorMessage: String? = null,
    val missingServerEnvVars: List<String> = emptyList()
)

/**
 * Manages a real-time LiveKit WebRTC Room using the official LiveKit Android SDK (`io.livekit:livekit-android`)
 * and synchronizes hardware audio routing via [LiveKitAudioRouter].
 */
class LiveKitRoomSessionManager(
    context: Context,
    val audioRouter: LiveKitAudioRouter = LiveKitAudioRouter(context)
) {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private var room: Room? = null
    private var roomEventsJob: Job? = null
    private var connectJob: Job? = null

    // Saved connection parameters for automatic or manual reconnect
    private var lastRoomId: String = ""
    private var lastUserId: String = ""
    private var lastUsername: String = ""
    private var lastPeerUserId: String? = null
    private var lastCallId: String? = null
    private var lastIsPrivateRoom: Boolean = false
    private var lastIsVideoCall: Boolean = false

    private val _sessionState = MutableStateFlow(LiveKitSessionUiState())
    val sessionState: StateFlow<LiveKitSessionUiState> = _sessionState.asStateFlow()

    fun currentRoom(): Room? = room

    /**
     * Connects to an isolated LiveKit room for a NOICE LIVE group voice or video chatroom.
     * Each `PartyRoom` uses its unique `room.id` so rooms are strictly isolated.
     */
    fun connectToChatroom(
        roomId: String,
        currentUserId: String,
        currentUsername: String,
        isPrivate: Boolean = false,
        startMicMuted: Boolean = true,
        startCameraEnabled: Boolean = false
    ) {
        val cleanRoomId = roomId.trim()
        if (cleanRoomId.isBlank() || currentUserId.isBlank()) return

        // If already connected or connecting to the exact same room, just sync media states
        if (lastRoomId == cleanRoomId &&
            (_sessionState.value.connectionStatus == LiveKitConnectionStatus.CONNECTED ||
                _sessionState.value.connectionStatus == LiveKitConnectionStatus.CONNECTING ||
                _sessionState.value.connectionStatus == LiveKitConnectionStatus.FETCHING_TOKEN)
        ) {
            setMicrophoneMuted(startMicMuted)
            if (startCameraEnabled != _sessionState.value.isCameraEnabled) {
                setCameraEnabled(startCameraEnabled)
            }
            return
        }

        lastRoomId = cleanRoomId
        lastUserId = currentUserId
        lastUsername = currentUsername
        lastPeerUserId = null
        lastCallId = null
        lastIsPrivateRoom = isPrivate
        lastIsVideoCall = startCameraEnabled

        connectJob?.cancel()
        connectJob = scope.launch {
            _sessionState.update {
                it.copy(
                    connectionStatus = LiveKitConnectionStatus.FETCHING_TOKEN,
                    roomId = cleanRoomId,
                    localIdentity = currentUserId,
                    statusMessage = "Authenticating with Vercel LiveKit service...",
                    errorMessage = null,
                    missingServerEnvVars = emptyList()
                )
            }

            audioRouter.enterVoiceRoomSession(cleanRoomId)
            audioRouter.setMicrophoneMuted(startMicMuted)

            val tokenResult = VercelLiveKitTokenClient.fetchChatroomToken(
                roomId = cleanRoomId,
                currentUserId = currentUserId,
                currentUsername = currentUsername,
                isPrivate = isPrivate
            )

            when (tokenResult) {
                is VercelLiveKitTokenResult.Success -> {
                    establishLiveKitRoomConnection(
                        wsUrl = tokenResult.wsUrl,
                        token = tokenResult.token,
                        roomId = cleanRoomId,
                        localIdentity = tokenResult.participantIdentity.ifBlank { currentUserId },
                        endpointUsed = tokenResult.endpointUsed,
                        startMicMuted = startMicMuted,
                        startCameraEnabled = startCameraEnabled,
                        startFrontCamera = true
                    )
                }
                is VercelLiveKitTokenResult.Failure -> {
                    val status = when {
                        tokenResult.isAuthError -> LiveKitConnectionStatus.AUTH_ERROR
                        tokenResult.isServerConfigError -> LiveKitConnectionStatus.BACKEND_CONFIG_MISSING
                        else -> LiveKitConnectionStatus.ERROR
                    }
                    _sessionState.update {
                        it.copy(
                            connectionStatus = status,
                            statusMessage = tokenResult.errorTitle,
                            errorMessage = tokenResult.errorMessage,
                            missingServerEnvVars = tokenResult.missingServerEnvVars
                        )
                    }
                }
            }
        }
    }

    /**
     * Connects to an isolated LiveKit room for a 1-on-1 private audio or video call.
     */
    fun connectToPrivateCall(
        callId: String,
        liveKitRoomName: String,
        currentUserId: String,
        currentUsername: String,
        peerUserId: String,
        isVideoCall: Boolean,
        startMicMuted: Boolean = false,
        startCameraEnabled: Boolean = isVideoCall,
        startFrontCamera: Boolean = true
    ) {
        val cleanRoomName = liveKitRoomName.trim().ifBlank { "call_$callId" }
        if (cleanRoomName.isBlank() || currentUserId.isBlank()) return

        lastRoomId = cleanRoomName
        lastUserId = currentUserId
        lastUsername = currentUsername
        lastPeerUserId = peerUserId
        lastCallId = callId
        lastIsPrivateRoom = true
        lastIsVideoCall = isVideoCall

        connectJob?.cancel()
        connectJob = scope.launch {
            _sessionState.update {
                it.copy(
                    connectionStatus = LiveKitConnectionStatus.FETCHING_TOKEN,
                    roomId = cleanRoomName,
                    localIdentity = currentUserId,
                    isFrontCamera = startFrontCamera,
                    statusMessage = "Requesting LiveKit call token...",
                    errorMessage = null,
                    missingServerEnvVars = emptyList()
                )
            }

            audioRouter.enterVoiceRoomSession(cleanRoomName)
            audioRouter.setMicrophoneMuted(startMicMuted)

            val tokenResult = VercelLiveKitTokenClient.fetchPrivateCallToken(
                callId = callId,
                liveKitRoomName = cleanRoomName,
                currentUserId = currentUserId,
                currentUsername = currentUsername,
                peerUserId = peerUserId,
                callType = if (isVideoCall) "video" else "voice"
            )

            when (tokenResult) {
                is VercelLiveKitTokenResult.Success -> {
                    establishLiveKitRoomConnection(
                        wsUrl = tokenResult.wsUrl,
                        token = tokenResult.token,
                        roomId = cleanRoomName,
                        localIdentity = tokenResult.participantIdentity.ifBlank { currentUserId },
                        endpointUsed = tokenResult.endpointUsed,
                        startMicMuted = startMicMuted,
                        startCameraEnabled = startCameraEnabled,
                        startFrontCamera = startFrontCamera
                    )
                }
                is VercelLiveKitTokenResult.Failure -> {
                    val status = when {
                        tokenResult.isAuthError -> LiveKitConnectionStatus.AUTH_ERROR
                        tokenResult.isServerConfigError -> LiveKitConnectionStatus.BACKEND_CONFIG_MISSING
                        else -> LiveKitConnectionStatus.ERROR
                    }
                    _sessionState.update {
                        it.copy(
                            connectionStatus = status,
                            statusMessage = tokenResult.errorTitle,
                            errorMessage = tokenResult.errorMessage,
                            missingServerEnvVars = tokenResult.missingServerEnvVars
                        )
                    }
                }
            }
        }
    }

    private suspend fun establishLiveKitRoomConnection(
        wsUrl: String,
        token: String,
        roomId: String,
        localIdentity: String,
        endpointUsed: String,
        startMicMuted: Boolean,
        startCameraEnabled: Boolean,
        startFrontCamera: Boolean
    ) {
        disconnectInternal(clearParams = false)

        _sessionState.update {
            it.copy(
                connectionStatus = LiveKitConnectionStatus.CONNECTING,
                roomId = roomId,
                wsUrl = wsUrl,
                endpointUsed = endpointUsed,
                localIdentity = localIdentity,
                isFrontCamera = startFrontCamera,
                statusMessage = "Connecting to LiveKit room $roomId...",
                errorMessage = null
            )
        }

        try {
            val newRoom = LiveKit.create(appContext = appContext)
            room = newRoom

            roomEventsJob = scope.launch {
                newRoom.events.collect { event ->
                    handleRoomEvent(event)
                }
            }

            val connectOptions = ConnectOptions(autoSubscribe = true)
            newRoom.connect(
                url = wsUrl,
                token = token,
                options = connectOptions
            )

            // Apply initial microphone & camera state if permissions are granted
            val hasAudioPerm = ContextCompat.checkSelfPermission(
                appContext,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
            val shouldEnableMic = hasAudioPerm && !startMicMuted
            runCatching {
                newRoom.localParticipant.setMicrophoneEnabled(shouldEnableMic)
            }
            audioRouter.setMicrophoneMuted(!shouldEnableMic)

            val hasCameraPerm = ContextCompat.checkSelfPermission(
                appContext,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
            val shouldEnableCam = hasCameraPerm && startCameraEnabled
            if (shouldEnableCam) {
                runCatching {
                    newRoom.localParticipant.setCameraEnabled(true)
                    if (!startFrontCamera) {
                        switchLocalVideoTrackCamera(newRoom, frontCamera = false)
                    }
                }
            }

            refreshParticipantsSnapshot()
            _sessionState.update {
                it.copy(
                    connectionStatus = LiveKitConnectionStatus.CONNECTED,
                    isMicEnabled = shouldEnableMic,
                    isCameraEnabled = shouldEnableCam,
                    statusMessage = "Connected to LiveKit ($roomId)",
                    errorMessage = null
                )
            }
        } catch (e: Exception) {
            _sessionState.update {
                it.copy(
                    connectionStatus = LiveKitConnectionStatus.ERROR,
                    statusMessage = "LiveKit Connection Failed",
                    errorMessage = e.localizedMessage ?: "Unable to connect to LiveKit WebRTC server."
                )
            }
        }
    }

    private fun handleRoomEvent(event: RoomEvent) {
        when (event) {
            is RoomEvent.Connected -> {
                refreshParticipantsSnapshot()
                _sessionState.update {
                    it.copy(
                        connectionStatus = LiveKitConnectionStatus.CONNECTED,
                        statusMessage = "Connected to LiveKit (${it.roomId})",
                        errorMessage = null
                    )
                }
            }
            is RoomEvent.Reconnecting -> {
                _sessionState.update {
                    it.copy(
                        connectionStatus = LiveKitConnectionStatus.RECONNECTING,
                        statusMessage = "Reconnecting to LiveKit..."
                    )
                }
            }
            is RoomEvent.Reconnected -> {
                refreshParticipantsSnapshot()
                _sessionState.update {
                    it.copy(
                        connectionStatus = LiveKitConnectionStatus.CONNECTED,
                        statusMessage = "Reconnected to LiveKit",
                        errorMessage = null
                    )
                }
            }
            is RoomEvent.Disconnected -> {
                refreshParticipantsSnapshot()
                _sessionState.update {
                    it.copy(
                        connectionStatus = LiveKitConnectionStatus.DISCONNECTED,
                        statusMessage = "Disconnected from LiveKit"
                    )
                }
            }
            is RoomEvent.FailedToConnect -> {
                _sessionState.update {
                    it.copy(
                        connectionStatus = LiveKitConnectionStatus.ERROR,
                        statusMessage = "Failed to connect to LiveKit",
                        errorMessage = event.error.localizedMessage ?: "LiveKit signaling failure"
                    )
                }
            }
            is RoomEvent.ParticipantConnected,
            is RoomEvent.ParticipantDisconnected,
            is RoomEvent.ParticipantMetadataChanged,
            is RoomEvent.ParticipantNameChanged,
            is RoomEvent.ParticipantPermissionsChanged,
            is RoomEvent.TrackSubscribed,
            is RoomEvent.TrackUnsubscribed,
            is RoomEvent.TrackPublished,
            is RoomEvent.TrackUnpublished,
            is RoomEvent.TrackMuted,
            is RoomEvent.TrackUnmuted,
            is RoomEvent.LocalTrackSubscribed -> {
                refreshParticipantsSnapshot()
            }
            is RoomEvent.ActiveSpeakersChanged -> {
                val speakerIds = event.speakers.mapNotNull { it.identity?.value }.toSet()
                val speakerNames = event.speakers.map { p ->
                    p.name?.takeIf { it.isNotBlank() } ?: p.identity?.value.orEmpty()
                }.filter { it.isNotBlank() }
                val localId = room?.localParticipant?.identity?.value ?: _sessionState.value.localIdentity
                val isLocalSpeakingNow = localId.isNotBlank() && localId in speakerIds
                audioRouter.updateSpeakingState(
                    isLocalSpeaking = isLocalSpeakingNow,
                    activeSpeakers = speakerNames,
                    participantCount = _sessionState.value.participants.size.coerceAtLeast(1)
                )
                refreshParticipantsSnapshot(overrideSpeakerIds = speakerIds, overrideSpeakerNames = speakerNames)
            }
            else -> {}
        }
    }

    private fun refreshParticipantsSnapshot(
        overrideSpeakerIds: Set<String>? = null,
        overrideSpeakerNames: List<String>? = null
    ) {
        val activeRoom = room ?: return
        val speakerIds = overrideSpeakerIds
            ?: activeRoom.activeSpeakers.mapNotNull { it.identity?.value }.toSet()
        val speakerNames = overrideSpeakerNames
            ?: activeRoom.activeSpeakers.map { p ->
                p.name?.takeIf { it.isNotBlank() } ?: p.identity?.value.orEmpty()
            }.filter { it.isNotBlank() }

        val snapshots = mutableListOf<LiveKitParticipantSnapshot>()

        // 1. Local Participant
        val local = activeRoom.localParticipant
        val localIdentity = local.identity?.value?.ifBlank { lastUserId } ?: lastUserId
        val localName = local.name?.takeIf { it.isNotBlank() } ?: lastUsername.ifBlank { localIdentity }
        val localVideo = extractParticipantVideoTrack(local)
        val localSpeaking = local.isSpeaking || (localIdentity.isNotBlank() && localIdentity in speakerIds)
        if (localIdentity.isNotBlank()) {
            snapshots.add(
                LiveKitParticipantSnapshot(
                    identity = localIdentity,
                    displayName = localName,
                    isLocal = true,
                    isSpeaking = localSpeaking,
                    isMicEnabled = local.isMicrophoneEnabled,
                    isCameraEnabled = local.isCameraEnabled,
                    audioLevel = local.audioLevel,
                    videoTrack = localVideo
                )
            )
        }

        // 2. Remote Participants (only real authenticated LiveKit participants)
        var firstRemoteVideo: VideoTrack? = null
        activeRoom.remoteParticipants.values.forEach { remote ->
            val remoteId = remote.identity?.value.orEmpty()
            if (remoteId.isNotBlank()) {
                val remoteName = remote.name?.takeIf { it.isNotBlank() } ?: remoteId
                val remoteVideo = extractParticipantVideoTrack(remote)
                if (firstRemoteVideo == null && remoteVideo != null) {
                    firstRemoteVideo = remoteVideo
                }
                snapshots.add(
                    LiveKitParticipantSnapshot(
                        identity = remoteId,
                        displayName = remoteName,
                        isLocal = false,
                        isSpeaking = remote.isSpeaking || remoteId in speakerIds,
                        isMicEnabled = remote.isMicrophoneEnabled,
                        isCameraEnabled = remote.isCameraEnabled,
                        audioLevel = remote.audioLevel,
                        videoTrack = remoteVideo
                    )
                )
            }
        }

        audioRouter.updateSpeakingState(
            isLocalSpeaking = localSpeaking,
            activeSpeakers = speakerNames,
            participantCount = snapshots.size.coerceAtLeast(1)
        )

        _sessionState.update {
            it.copy(
                participants = snapshots,
                activeSpeakerIds = speakerIds,
                activeSpeakerNames = speakerNames,
                isLocalSpeaking = localSpeaking,
                isMicEnabled = local.isMicrophoneEnabled,
                isCameraEnabled = local.isCameraEnabled,
                localVideoTrack = localVideo,
                primaryRemoteVideoTrack = firstRemoteVideo
            )
        }
    }

    private fun extractParticipantVideoTrack(participant: Participant): VideoTrack? {
        val cameraPub = participant.getTrackPublication(Track.Source.CAMERA)
        val cameraTrack = cameraPub?.track as? VideoTrack
        if (cameraTrack != null && !cameraPub.muted) {
            return cameraTrack
        }
        return participant.videoTrackPublications
            .firstOrNull { (pub, track) -> !pub.muted && track is VideoTrack }
            ?.second as? VideoTrack
    }

    /**
     * Enables or mutes the local participant's microphone in both the LiveKit Room and [LiveKitAudioRouter].
     */
    fun setMicrophoneMuted(muted: Boolean) {
        audioRouter.setMicrophoneMuted(muted)
        val hasPerm = ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        val enableMic = hasPerm && !muted
        scope.launch {
            val activeRoom = room
            if (activeRoom != null && _sessionState.value.connectionStatus == LiveKitConnectionStatus.CONNECTED) {
                runCatching {
                    activeRoom.localParticipant.setMicrophoneEnabled(enableMic)
                }
                refreshParticipantsSnapshot()
            } else {
                _sessionState.update { it.copy(isMicEnabled = enableMic) }
            }
        }
    }

    /**
     * Enables or disables the local participant's camera for 1-on-1 video calls or group video chatrooms.
     */
    fun setCameraEnabled(enabled: Boolean) {
        val hasCamPerm = ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        val targetEnabled = hasCamPerm && enabled
        scope.launch {
            val activeRoom = room
            if (activeRoom != null && _sessionState.value.connectionStatus == LiveKitConnectionStatus.CONNECTED) {
                runCatching {
                    activeRoom.localParticipant.setCameraEnabled(targetEnabled)
                }
                refreshParticipantsSnapshot()
            } else {
                _sessionState.update { it.copy(isCameraEnabled = targetEnabled) }
            }
        }
    }

    /**
     * Switches between front and rear cameras on the active [LocalVideoTrack].
     */
    fun setFrontCamera(frontCamera: Boolean) {
        _sessionState.update { it.copy(isFrontCamera = frontCamera) }
        val activeRoom = room ?: return
        scope.launch {
            runCatching {
                switchLocalVideoTrackCamera(activeRoom, frontCamera)
            }
            refreshParticipantsSnapshot()
        }
    }

    private fun switchLocalVideoTrackCamera(activeRoom: Room, frontCamera: Boolean) {
        val pub = activeRoom.localParticipant.getTrackPublication(Track.Source.CAMERA)
        val localVideoTrack = pub?.track as? LocalVideoTrack
        val targetPosition = if (frontCamera) CameraPosition.FRONT else CameraPosition.BACK
        localVideoTrack?.switchCamera(position = targetPosition)
    }

    /**
     * Retries connecting to the current LiveKit room (re-fetching a fresh Firebase-authenticated token).
     */
    fun reconnect() {
        val roomId = lastRoomId
        val userId = lastUserId
        if (roomId.isBlank() || userId.isBlank()) return
        val callId = lastCallId
        val peerId = lastPeerUserId
        if (callId != null && peerId != null) {
            connectToPrivateCall(
                callId = callId,
                liveKitRoomName = roomId,
                currentUserId = userId,
                currentUsername = lastUsername,
                peerUserId = peerId,
                isVideoCall = lastIsVideoCall,
                startMicMuted = !_sessionState.value.isMicEnabled,
                startCameraEnabled = _sessionState.value.isCameraEnabled,
                startFrontCamera = _sessionState.value.isFrontCamera
            )
        } else {
            connectToChatroom(
                roomId = roomId,
                currentUserId = userId,
                currentUsername = lastUsername,
                isPrivate = lastIsPrivateRoom,
                startMicMuted = !_sessionState.value.isMicEnabled,
                startCameraEnabled = _sessionState.value.isCameraEnabled
            )
        }
    }

    /**
     * Leaves the LiveKit room and optionally calls `DELETE /api/livekit/rooms/{roomId}` if the host
     * is closing and deleting the room.
     */
    fun leaveRoom(deleteOnServerAsHost: Boolean = false) {
        val roomToDelete = lastRoomId
        if (deleteOnServerAsHost && roomToDelete.isNotBlank()) {
            scope.launch {
                VercelLiveKitTokenClient.deleteRoomOnBackend(roomToDelete)
            }
        }
        disconnectInternal(clearParams = true)
        audioRouter.leaveVoiceRoomSession()
    }

    private fun disconnectInternal(clearParams: Boolean) {
        roomEventsJob?.cancel()
        roomEventsJob = null
        val oldRoom = room
        room = null
        if (oldRoom != null) {
            runCatching { oldRoom.disconnect() }
            runCatching { oldRoom.release() }
        }
        if (clearParams) {
            lastRoomId = ""
            lastCallId = null
            lastPeerUserId = null
            _sessionState.value = LiveKitSessionUiState()
        }
    }

    fun release() {
        connectJob?.cancel()
        disconnectInternal(clearParams = true)
        audioRouter.release()
        scope.cancel()
    }
}

/**
 * Jetpack Compose renderer for a LiveKit [VideoTrack] (local camera or remote participant video)
 * using the official LiveKit Android SDK's [TextureViewRenderer].
 */
@Composable
fun LiveKitVideoTrackView(
    room: Room?,
    videoTrack: VideoTrack?,
    mirror: Boolean = false,
    modifier: Modifier = Modifier
) {
    if (room == null || videoTrack == null) return

    var initializedRenderer by remember(room) { mutableStateOf<TextureViewRenderer?>(null) }

    DisposableEffect(room, videoTrack, mirror) {
        val renderer = initializedRenderer
        if (renderer != null) {
            runCatching {
                renderer.setMirror(mirror)
                videoTrack.addRenderer(renderer)
            }
        }
        onDispose {
            if (renderer != null) {
                runCatching { videoTrack.removeRenderer(renderer) }
            }
        }
    }

    DisposableEffect(room) {
        onDispose {
            initializedRenderer?.let { renderer ->
                runCatching { videoTrack.removeRenderer(renderer) }
                runCatching { renderer.release() }
            }
            initializedRenderer = null
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            TextureViewRenderer(ctx).apply {
                runCatching {
                    room.initVideoRenderer(this)
                    setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL)
                    setEnableHardwareScaler(true)
                    setMirror(mirror)
                    videoTrack.addRenderer(this)
                }
                initializedRenderer = this
            }
        },
        update = { renderer ->
            runCatching {
                renderer.setMirror(mirror)
            }
        }
    )
}

/**
 * Supported Android audio output routes for LiveKit voice chatrooms.
 */
enum class AudioOutputRoute(val label: String, val shortLabel: String) {
    SPEAKER("Speaker Output", "Speaker"),
    EARPIECE("Earpiece Output", "Earpiece"),
    BLUETOOTH("Bluetooth Headset", "Bluetooth")
}

/**
 * Snapshot of the LiveKit voice & audio routing state.
 */
data class LiveKitAudioUiState(
    val isInVoiceRoom: Boolean = false,
    val activeRoomName: String = "",
    val currentRoute: AudioOutputRoute = AudioOutputRoute.SPEAKER,
    val availableRoutes: Set<AudioOutputRoute> = setOf(AudioOutputRoute.SPEAKER, AudioOutputRoute.EARPIECE),
    val isMicMuted: Boolean = false,
    val isSpeakerMuted: Boolean = false,
    val hasAudioFocus: Boolean = false,
    val isLocalSpeaking: Boolean = false,
    val activeSpeakerNames: List<String> = emptyList(),
    val participantCount: Int = 0
)

/**
 * Manages Android Audio Focus, Communication Mode (`MODE_IN_COMMUNICATION`),
 * Earpiece / Speaker / Bluetooth Headset routing, and Microphone Mute synchronization
 * for both the native LiveKit SDK sessions and the Vercel LiveKit WebView.
 */
class LiveKitAudioRouter(context: Context) {
    private val appContext = context.applicationContext
    private val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val mainHandler = Handler(Looper.getMainLooper())

    private val _state = MutableStateFlow(LiveKitAudioUiState())
    val state: StateFlow<LiveKitAudioUiState> = _state.asStateFlow()

    private var audioFocusRequest: AudioFocusRequest? = null

    private val focusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_GAIN,
            AudioManager.AUDIOFOCUS_GAIN_TRANSIENT,
            AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK -> {
                _state.update { it.copy(hasAudioFocus = true) }
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                // Keep session alive while ducking
                _state.update { it.copy(hasAudioFocus = true) }
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                _state.update { it.copy(hasAudioFocus = false) }
            }
            AudioManager.AUDIOFOCUS_LOSS -> {
                _state.update { it.copy(hasAudioFocus = false) }
            }
        }
    }

    private val deviceCallback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) {
            refreshAvailableRoutes()
        }

        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) {
            refreshAvailableRoutes()
            val available = _state.value.availableRoutes
            if (_state.value.currentRoute == AudioOutputRoute.BLUETOOTH &&
                AudioOutputRoute.BLUETOOTH !in available
            ) {
                selectAudioRoute(AudioOutputRoute.SPEAKER)
            }
        }
    }

    init {
        refreshAvailableRoutes()
        try {
            audioManager.registerAudioDeviceCallback(deviceCallback, mainHandler)
        } catch (_: Exception) {
            // Safe fallback on Robolectric or restricted environments
        }
    }

    fun refreshAvailableRoutes() {
        val routes = linkedSetOf(AudioOutputRoute.SPEAKER, AudioOutputRoute.EARPIECE)
        try {
            val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            val hasBt = devices.any { info ->
                info.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
                    info.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                    (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                        (info.type == AudioDeviceInfo.TYPE_BLE_HEADSET ||
                            info.type == AudioDeviceInfo.TYPE_BLE_SPEAKER))
            }
            if (hasBt) {
                routes.add(AudioOutputRoute.BLUETOOTH)
            }
        } catch (_: Exception) {
            // Ignore in test environments
        }
        _state.update { it.copy(availableRoutes = routes) }
    }

    /**
     * Requests voice communication audio focus and configures `MODE_IN_COMMUNICATION`
     * for low-latency full-duplex LiveKit WebRTC audio.
     */
    fun enterVoiceRoomSession(roomName: String = "LiveKit Stage") {
        val granted = requestVoiceAudioFocus()
        try {
            audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
        } catch (_: Exception) {
        }
        refreshAvailableRoutes()
        applyRouteToSystem(_state.value.currentRoute)
        _state.update {
            it.copy(
                isInVoiceRoom = true,
                activeRoomName = roomName.ifBlank { "LiveKit Stage" },
                hasAudioFocus = granted
            )
        }
    }

    /**
     * Leaves the LiveKit voice room, restores `MODE_NORMAL`, clears communication device overrides,
     * and abandons transient voice audio focus.
     */
    fun leaveVoiceRoomSession() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                audioManager.clearCommunicationDevice()
            } else {
                @Suppress("DEPRECATION")
                audioManager.stopBluetoothSco()
                @Suppress("DEPRECATION")
                audioManager.isBluetoothScoOn = false
                @Suppress("DEPRECATION")
                audioManager.isSpeakerphoneOn = false
            }
            audioManager.isMicrophoneMute = false
            audioManager.mode = AudioManager.MODE_NORMAL
        } catch (_: Exception) {
        }
        abandonVoiceAudioFocus()
        _state.update {
            it.copy(
                isInVoiceRoom = false,
                activeRoomName = "",
                hasAudioFocus = false,
                isLocalSpeaking = false,
                activeSpeakerNames = emptyList(),
                participantCount = 0
            )
        }
    }

    /**
     * Switches between Speaker, Earpiece, and Bluetooth Headset output.
     */
    fun selectAudioRoute(route: AudioOutputRoute): Boolean {
        if (!_state.value.isInVoiceRoom) {
            requestVoiceAudioFocus()
            try {
                audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
            } catch (_: Exception) {
            }
        }
        val applied = applyRouteToSystem(route)
        _state.update {
            it.copy(
                currentRoute = route,
                isSpeakerMuted = false
            )
        }
        return applied
    }

    /**
     * Cycles to the next audio output route (Speaker -> Earpiece -> Bluetooth if available).
     */
    fun cycleAudioRoute(): AudioOutputRoute {
        refreshAvailableRoutes()
        val ordered = buildList {
            add(AudioOutputRoute.SPEAKER)
            add(AudioOutputRoute.EARPIECE)
            if (AudioOutputRoute.BLUETOOTH in _state.value.availableRoutes) {
                add(AudioOutputRoute.BLUETOOTH)
            }
        }
        val currentIdx = ordered.indexOf(_state.value.currentRoute).coerceAtLeast(0)
        val nextRoute = ordered[(currentIdx + 1) % ordered.size]
        selectAudioRoute(nextRoute)
        return nextRoute
    }

    fun setMicrophoneMuted(muted: Boolean) {
        try {
            audioManager.isMicrophoneMute = muted
        } catch (_: Exception) {
        }
        _state.update {
            it.copy(
                isMicMuted = muted,
                isLocalSpeaking = if (muted) false else it.isLocalSpeaking
            )
        }
    }

    fun toggleMicrophoneMuted(): Boolean {
        val next = !_state.value.isMicMuted
        setMicrophoneMuted(next)
        return next
    }

    fun setSpeakerMuted(muted: Boolean) {
        _state.update { it.copy(isSpeakerMuted = muted) }
    }

    fun updateSpeakingState(
        isLocalSpeaking: Boolean,
        activeSpeakers: List<String> = _state.value.activeSpeakerNames,
        participantCount: Int = _state.value.participantCount
    ) {
        _state.update {
            it.copy(
                isLocalSpeaking = if (it.isMicMuted) false else isLocalSpeaking,
                activeSpeakerNames = activeSpeakers,
                participantCount = participantCount.coerceAtLeast(0)
            )
        }
    }

    private fun applyRouteToSystem(route: AudioOutputRoute): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val outputDevices = audioManager.availableCommunicationDevices
                val targetDevice = when (route) {
                    AudioOutputRoute.BLUETOOTH -> outputDevices.firstOrNull {
                        it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
                            it.type == AudioDeviceInfo.TYPE_BLE_HEADSET ||
                            it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP
                    }
                    AudioOutputRoute.EARPIECE -> outputDevices.firstOrNull {
                        it.type == AudioDeviceInfo.TYPE_BUILTIN_EARPIECE
                    }
                    AudioOutputRoute.SPEAKER -> outputDevices.firstOrNull {
                        it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
                    }
                }
                if (targetDevice != null) {
                    audioManager.setCommunicationDevice(targetDevice)
                } else {
                    legacyApplyRoute(route)
                }
            } else {
                legacyApplyRoute(route)
            }
        } catch (_: Exception) {
            false
        }
    }

    @Suppress("DEPRECATION")
    private fun legacyApplyRoute(route: AudioOutputRoute): Boolean {
        return try {
            when (route) {
                AudioOutputRoute.BLUETOOTH -> {
                    audioManager.isSpeakerphoneOn = false
                    audioManager.startBluetoothSco()
                    audioManager.isBluetoothScoOn = true
                    true
                }
                AudioOutputRoute.EARPIECE -> {
                    audioManager.stopBluetoothSco()
                    audioManager.isBluetoothScoOn = false
                    audioManager.isSpeakerphoneOn = false
                    true
                }
                AudioOutputRoute.SPEAKER -> {
                    audioManager.stopBluetoothSco()
                    audioManager.isBluetoothScoOn = false
                    audioManager.isSpeakerphoneOn = true
                    true
                }
            }
        } catch (_: Exception) {
            false
        }
    }

    private fun requestVoiceAudioFocus(): Boolean {
        return try {
            val attrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()
            val req = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                .setAudioAttributes(attrs)
                .setAcceptsDelayedFocusGain(true)
                .setOnAudioFocusChangeListener(focusChangeListener, mainHandler)
                .build()
            audioFocusRequest = req
            val res = audioManager.requestAudioFocus(req)
            res == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        } catch (_: Exception) {
            false
        }
    }

    private fun abandonVoiceAudioFocus() {
        try {
            audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
            audioFocusRequest = null
        } catch (_: Exception) {
        }
    }

    fun release() {
        leaveVoiceRoomSession()
        try {
            audioManager.unregisterAudioDeviceCallback(deviceCallback)
        } catch (_: Exception) {
        }
    }
}

/**
 * JavaScript-to-Android bridge injected only on trusted Vercel origins.
 * Synchronizes WebRTC / LiveKit voice room status, microphone mute state, and speaking indicators
 * with [LiveKitAudioRouter].
 */
class NoiseLiveKitJsBridge(
    private val audioRouter: LiveKitAudioRouter,
    private val onStatusMessage: (String) -> Unit
) {
    private val mainHandler = Handler(Looper.getMainLooper())

    @JavascriptInterface
    fun onVoiceRoomJoined(roomName: String, participantCount: Int) {
        mainHandler.post {
            audioRouter.enterVoiceRoomSession(roomName.ifBlank { "LiveKit Voice Stage" })
            audioRouter.updateSpeakingState(
                isLocalSpeaking = false,
                participantCount = participantCount.coerceAtLeast(1)
            )
        }
    }

    @JavascriptInterface
    fun onVoiceRoomLeft() {
        mainHandler.post {
            audioRouter.leaveVoiceRoomSession()
        }
    }

    @JavascriptInterface
    fun onMicMuteChanged(muted: Boolean) {
        mainHandler.post {
            audioRouter.setMicrophoneMuted(muted)
        }
    }

    @JavascriptInterface
    fun onSpeakingStateChanged(isLocalSpeaking: Boolean, speakersCsv: String, participantCount: Int) {
        mainHandler.post {
            val speakers = speakersCsv
                .split(",")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
            audioRouter.updateSpeakingState(
                isLocalSpeaking = isLocalSpeaking,
                activeSpeakers = speakers,
                participantCount = participantCount
            )
        }
    }

    @JavascriptInterface
    fun onAudioNotice(message: String) {
        if (message.isNotBlank()) {
            mainHandler.post { onStatusMessage(message) }
        }
    }
}

/**
 * Non-intrusive script injected into the Vercel web app after page load to hook WebRTC/LiveKit
 * audio tracks into Android's native [LiveKitAudioRouter].
 */
internal val LIVEKIT_WEBRTC_BRIDGE_JS = """
(function() {
  if (window.__noiseLiveKitBridgeInstalled) return;
  window.__noiseLiveKitBridgeInstalled = true;
  window.__noiseActiveAudioTracks = [];

  window.__noiseSetWebRtcMicMuted = function(muted) {
    try {
      window.__noiseActiveAudioTracks.forEach(function(track) {
        if (track && track.readyState === 'live') {
          track.enabled = !muted;
        }
      });
      if (window.NoiseAndroidLiveKit && window.NoiseAndroidLiveKit.onMicMuteChanged) {
        window.NoiseAndroidLiveKit.onMicMuteChanged(!!muted);
      }
    } catch (e) {}
  };

  if (navigator.mediaDevices && navigator.mediaDevices.getUserMedia) {
    const origGetUserMedia = navigator.mediaDevices.getUserMedia.bind(navigator.mediaDevices);
    navigator.mediaDevices.getUserMedia = async function(constraints) {
      const stream = await origGetUserMedia(constraints);
      try {
        if (constraints && constraints.audio) {
          const audioTracks = stream.getAudioTracks();
          audioTracks.forEach(function(track) {
            window.__noiseActiveAudioTracks.push(track);
            track.addEventListener('ended', function() {
              window.__noiseActiveAudioTracks = window.__noiseActiveAudioTracks.filter(function(t) {
                return t !== track && t.readyState === 'live';
              });
              if (window.__noiseActiveAudioTracks.length === 0 && window.NoiseAndroidLiveKit) {
                window.NoiseAndroidLiveKit.onVoiceRoomLeft();
              }
            });
          });
          if (window.NoiseAndroidLiveKit) {
            window.NoiseAndroidLiveKit.onVoiceRoomJoined(document.title || 'LiveKit Voice Room', 1);
          }
          const AudioCtx = window.AudioContext || window.webkitAudioContext;
          if (AudioCtx && audioTracks.length > 0) {
            const ctx = new AudioCtx();
            const source = ctx.createMediaStreamSource(stream);
            const analyser = ctx.createAnalyser();
            analyser.fftSize = 256;
            source.connect(analyser);
            const data = new Uint8Array(analyser.frequencyBinCount);
            let lastSpeaking = false;
            const intervalId = setInterval(function() {
              if (audioTracks[0].readyState !== 'live') {
                clearInterval(intervalId);
                try { ctx.close(); } catch (e) {}
                return;
              }
              if (!audioTracks[0].enabled) {
                if (lastSpeaking) {
                  lastSpeaking = false;
                  if (window.NoiseAndroidLiveKit) {
                    window.NoiseAndroidLiveKit.onSpeakingStateChanged(false, '', 1);
                  }
                }
                return;
              }
              analyser.getByteFrequencyData(data);
              let sum = 0;
              for (let i = 0; i < data.length; i++) sum += data[i];
              const avg = sum / data.length;
              const speaking = avg > 18;
              if (speaking !== lastSpeaking) {
                lastSpeaking = speaking;
                if (window.NoiseAndroidLiveKit) {
                  window.NoiseAndroidLiveKit.onSpeakingStateChanged(speaking, speaking ? 'You' : '', 1);
                }
              }
            }, 350);
          }
        }
      } catch (err) {}
      return stream;
    };
  }
})();
""".trimIndent()

/**
 * Full-screen Vercel WebView + LiveKit Voice Room Integration screen.
 * Connects `https://noice-live26.vercel.app` directly to Android WebView
 * with WebRTC microphone permission security, cookies/DOM storage persistence, and hardware
 * audio routing (Speaker / Earpiece / Bluetooth headset).
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun VercelLiveKitWebViewScreen(
    url: String = VERCEL_LIVE_URL,
    audioRouter: LiveKitAudioRouter,
    onSwitchToNativeApp: () -> Unit
) {
    val context = LocalContext.current
    val audioState by audioRouter.state.collectAsState()

    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var loadProgress by remember { mutableIntStateOf(0) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var noticeBanner by remember { mutableStateOf<String?>(null) }
    var showAudioRouteSheet by remember { mutableStateOf(false) }
    var pendingWebPermissionRequest by remember { mutableStateOf<PermissionRequest?>(null) }

    // Runtime RECORD_AUDIO permission launcher for WebView LiveKit getUserMedia requests
    val recordAudioLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        val pending = pendingWebPermissionRequest
        pendingWebPermissionRequest = null
        if (isGranted) {
            if (pending != null) {
                try {
                    val grantable = pending.resources.filter {
                        it == PermissionRequest.RESOURCE_AUDIO_CAPTURE ||
                            it == PermissionRequest.RESOURCE_PROTECTED_MEDIA_ID
                    }.toTypedArray()
                    if (grantable.isNotEmpty()) {
                        pending.grant(grantable)
                    } else {
                        pending.grant(arrayOf(PermissionRequest.RESOURCE_AUDIO_CAPTURE))
                    }
                } catch (_: Exception) {
                }
            }
            audioRouter.enterVoiceRoomSession("LiveKit Voice Room")
            noticeBanner = "LiveKit microphone active (${audioState.currentRoute.shortLabel})"
        } else {
            try {
                pending?.deny()
            } catch (_: Exception) {
            }
            audioRouter.setMicrophoneMuted(true)
            noticeBanner = "Microphone permission denied. Voice room audio works in listen-only mode."
        }
    }

    // Runtime BLUETOOTH_CONNECT permission launcher for Android 12+ (API 31+)
    val bluetoothPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        audioRouter.refreshAvailableRoutes()
        if (isGranted) {
            audioRouter.selectAudioRoute(AudioOutputRoute.BLUETOOTH)
            noticeBanner = "Switched LiveKit audio output to Bluetooth headset"
        } else {
            noticeBanner = "Bluetooth permission is required to route LiveKit voice to a Bluetooth headset."
        }
    }

    fun requestAudioRouteChange(route: AudioOutputRoute) {
        if (route == AudioOutputRoute.BLUETOOTH && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val hasBtPerm = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
            if (!hasBtPerm) {
                try {
                    bluetoothPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
                } catch (_: Exception) {
                    audioRouter.selectAudioRoute(route)
                }
                return
            }
        }
        audioRouter.selectAudioRoute(route)
        noticeBanner = "Audio output: ${route.label}"
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                pendingWebPermissionRequest?.deny()
            } catch (_: Exception) {
            }
            pendingWebPermissionRequest = null
            runCatching {
                audioRouter.leaveVoiceRoomSession()
                webViewRef?.stopLoading()
                webViewRef?.removeJavascriptInterface("NoiseAndroidLiveKit")
                webViewRef?.destroy()
                webViewRef = null
            }
        }
    }

    BackHandler(enabled = webViewRef?.canGoBack() == true) {
        webViewRef?.goBack()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NoiseBlack)
            .testTag("vercel_livekit_webview_screen")
    ) {
        // Compact Top Control Bar: Preserves LiveKit Voice Routing + Mode Toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0B0A10))
                .border(0.8.dp, NoiseBorder, RoundedCornerShape(0.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: LiveKit & Vercel Connection Status
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                audioState.isLocalSpeaking -> VipGold
                                audioState.isInVoiceRoom -> OnlineGreen
                                else -> NoisePink
                            }
                        )
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (audioState.isInVoiceRoom) {
                        if (audioState.isLocalSpeaking) "LIVEKIT • Speaking"
                        else "LIVEKIT • Voice Connected"
                    } else {
                        "NOICE LIVE • Web + LiveKit"
                    },
                    color = NoiseWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Right: Audio Output Route + Mic Mute + Reload + Native Switch
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Audio Output Route Selector Pill (Speaker / Earpiece / Bluetooth)
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF171522))
                        .border(1.dp, VipGoldBorder, RoundedCornerShape(14.dp))
                        .clickable { showAudioRouteSheet = true }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .testTag("livekit_audio_route_button"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = when (audioState.currentRoute) {
                            AudioOutputRoute.SPEAKER -> Icons.AutoMirrored.Filled.VolumeUp
                            AudioOutputRoute.EARPIECE -> Icons.Default.Hearing
                            AudioOutputRoute.BLUETOOTH -> Icons.Default.BluetoothAudio
                        },
                        contentDescription = "Audio Output Route",
                        tint = VipGold,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = audioState.currentRoute.shortLabel,
                        color = VipGold,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                }

                // LiveKit Mic Mute/Unmute Pill
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(
                            if (audioState.isMicMuted) Color(0xFF3A1219)
                            else OnlineGreen.copy(alpha = 0.22f)
                        )
                        .border(
                            1.dp,
                            if (audioState.isMicMuted) NoisePink else OnlineGreen,
                            CircleShape
                        )
                        .clickable {
                            val hasMicPerm = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.RECORD_AUDIO
                            ) == PackageManager.PERMISSION_GRANTED
                            if (!hasMicPerm) {
                                recordAudioLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            } else {
                                val muted = audioRouter.toggleMicrophoneMuted()
                                webViewRef?.evaluateJavascript(
                                    "window.__noiseSetWebRtcMicMuted && window.__noiseSetWebRtcMicMuted($muted);",
                                    null
                                )
                                noticeBanner = if (muted) "LiveKit microphone muted" else "LiveKit microphone unmuted"
                            }
                        }
                        .testTag("livekit_webview_mic_toggle"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (audioState.isMicMuted) Icons.Default.MicOff else Icons.Default.Mic,
                        contentDescription = "Toggle LiveKit Mic",
                        tint = if (audioState.isMicMuted) NoisePink else OnlineGreen,
                        modifier = Modifier.size(14.dp)
                    )
                }

                // Reload Latest Vercel Deployment
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF171522))
                        .border(1.dp, NoiseBorder, CircleShape)
                        .clickable {
                            loadError = null
                            webViewRef?.reload()
                        }
                        .testTag("vercel_webview_reload_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Reload Vercel Deployment",
                        tint = NoiseWhite,
                        modifier = Modifier.size(14.dp)
                    )
                }

                // Switch to Native App Mode
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(Brush.horizontalGradient(listOf(NoisePink, NoisePurple)))
                        .clickable { onSwitchToNativeApp() }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .testTag("switch_to_native_app_button"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.PhoneAndroid,
                        contentDescription = "Native Mode",
                        tint = NoiseWhite,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "Native UI",
                        color = NoiseWhite,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 10.sp
                    )
                }
            }
        }

        // Loading progress bar
        if (isLoading && loadProgress in 1..99) {
            LinearProgressIndicator(
                progress = { loadProgress / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp),
                color = NoisePink,
                trackColor = Color(0xFF191724)
            )
        }

        // Permission or Audio Route Notice Banner
        AnimatedVisibility(visible = noticeBanner != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1B1528))
                    .border(0.8.dp, VipGoldBorder, RoundedCornerShape(0.dp))
                    .clickable { noticeBanner = null }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .testTag("livekit_webview_notice_banner"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = noticeBanner ?: "",
                    color = VipGold,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Dismiss Notice",
                    tint = NoiseTextSecondary,
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        // Main Vercel WebView Container
        Box(modifier = Modifier.fillMaxSize()) {
            AndroidView(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("vercel_webview_container"),
                factory = { ctx ->
                    WebView(ctx).apply {
                        webViewRef = this

                        // Enable Cookies & Third-Party Cookies for Auth & LiveKit session persistence
                        val cookieManager = CookieManager.getInstance()
                        cookieManager.setAcceptCookie(true)
                        cookieManager.setAcceptThirdPartyCookies(this, true)

                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            mediaPlaybackRequiresUserGesture = false
                            cacheMode = WebSettings.LOAD_DEFAULT
                            loadsImagesAutomatically = true
                            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                            allowFileAccess = false
                            allowContentAccess = true
                            setSupportMultipleWindows(false)
                            javaScriptCanOpenWindowsAutomatically = false
                            userAgentString = settings.userAgentString + " NoiseLiveAndroid/1.0"
                        }

                        addJavascriptInterface(
                            NoiseLiveKitJsBridge(
                                audioRouter = audioRouter,
                                onStatusMessage = { msg -> noticeBanner = msg }
                            ),
                            "NoiseAndroidLiveKit"
                        )

                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView?, pageUrl: String?, favicon: Bitmap?) {
                                super.onPageStarted(view, pageUrl, favicon)
                                isLoading = true
                                loadError = null
                            }

                            override fun onPageFinished(view: WebView?, pageUrl: String?) {
                                super.onPageFinished(view, pageUrl)
                                isLoading = false
                                CookieManager.getInstance().flush()
                                val parsed = pageUrl?.let { Uri.parse(it) }
                                if (isTrustedVercelOrigin(parsed)) {
                                    view?.evaluateJavascript(LIVEKIT_WEBRTC_BRIDGE_JS, null)
                                }
                            }

                            override fun onReceivedError(
                                view: WebView?,
                                request: WebResourceRequest?,
                                error: WebResourceError?
                            ) {
                                super.onReceivedError(view, request, error)
                                if (request?.isForMainFrame == true) {
                                    isLoading = false
                                    loadError = error?.description?.toString()
                                        ?: "Unable to reach Vercel deployment. Check your connection."
                                }
                            }
                        }

                        webChromeClient = object : WebChromeClient() {
                            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                super.onProgressChanged(view, newProgress)
                                loadProgress = newProgress
                                if (newProgress >= 100) {
                                    isLoading = false
                                }
                            }

                            override fun onPermissionRequest(request: PermissionRequest?) {
                                if (request == null) return
                                val origin = request.origin
                                if (!isTrustedVercelOrigin(origin)) {
                                    request.deny()
                                    noticeBanner = "Blocked microphone request from untrusted origin: ${origin?.host ?: "unknown"}"
                                    return
                                }

                                val wantsAudio = request.resources.contains(PermissionRequest.RESOURCE_AUDIO_CAPTURE)
                                if (wantsAudio) {
                                    val hasRecordPermission = ContextCompat.checkSelfPermission(
                                        ctx,
                                        Manifest.permission.RECORD_AUDIO
                                    ) == PackageManager.PERMISSION_GRANTED

                                    if (hasRecordPermission) {
                                        audioRouter.enterVoiceRoomSession("LiveKit Voice Room")
                                        val allowedResources = request.resources.filter {
                                            it == PermissionRequest.RESOURCE_AUDIO_CAPTURE ||
                                                it == PermissionRequest.RESOURCE_PROTECTED_MEDIA_ID
                                        }.toTypedArray()
                                        request.grant(
                                            if (allowedResources.isNotEmpty()) allowedResources
                                            else arrayOf(PermissionRequest.RESOURCE_AUDIO_CAPTURE)
                                        )
                                    } else {
                                        pendingWebPermissionRequest = request
                                        try {
                                            recordAudioLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                        } catch (_: Exception) {
                                            request.deny()
                                            pendingWebPermissionRequest = null
                                        }
                                    }
                                } else {
                                    request.deny()
                                }
                            }

                            override fun onPermissionRequestCanceled(request: PermissionRequest?) {
                                super.onPermissionRequestCanceled(request)
                                if (pendingWebPermissionRequest == request) {
                                    pendingWebPermissionRequest = null
                                }
                            }
                        }

                        loadUrl(url)
                    }
                }
            )

            // Initial loading overlay matching Noise Live black/pink theme
            if (isLoading && loadProgress < 35 && loadError == null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(NoiseBlack.copy(alpha = 0.85f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(
                            color = NoisePink,
                            strokeWidth = 2.5.dp,
                            modifier = Modifier.size(34.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Connecting to Noise...",
                            color = NoiseTextSecondary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Network Error Recovery Card
            if (loadError != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(NoiseBlack)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(22.dp))
                            .background(Color(0xFF12101B))
                            .border(1.dp, NoiseBorder, RoundedCornerShape(22.dp))
                            .padding(22.dp)
                    ) {
                        Text(
                            text = "Connection Issue",
                            color = NoiseWhite,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 17.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = loadError ?: "Could not connect to NOICE LIVE on Vercel.",
                            color = NoiseTextSecondary,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Brush.horizontalGradient(listOf(NoisePink, NoisePurple)))
                                    .clickable {
                                        loadError = null
                                        isLoading = true
                                        webViewRef?.loadUrl(url)
                                    }
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Retry Vercel", color = NoiseWhite, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color(0xFF1F1D2B))
                                    .border(1.dp, VipGoldBorder, RoundedCornerShape(14.dp))
                                    .clickable { onSwitchToNativeApp() }
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Open Native UI", color = VipGold, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAudioRouteSheet) {
        LiveKitAudioRouteDialog(
            currentRoute = audioState.currentRoute,
            availableRoutes = audioState.availableRoutes,
            isMicMuted = audioState.isMicMuted,
            isInVoiceRoom = audioState.isInVoiceRoom,
            isLocalSpeaking = audioState.isLocalSpeaking,
            activeSpeakers = audioState.activeSpeakerNames,
            onSelectRoute = { route ->
                requestAudioRouteChange(route)
                showAudioRouteSheet = false
            },
            onDismiss = { showAudioRouteSheet = false }
        )
    }
}

/**
 * Reusable LiveKit Audio Output Routing & Voice Status Modal used in both
 * the Vercel WebView bar and the native `PartyChatRoomScreen`.
 */
@Composable
fun LiveKitAudioRouteDialog(
    currentRoute: AudioOutputRoute,
    availableRoutes: Set<AudioOutputRoute>,
    isMicMuted: Boolean,
    isInVoiceRoom: Boolean,
    isLocalSpeaking: Boolean,
    activeSpeakers: List<String>,
    onSelectRoute: (AudioOutputRoute) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.2.dp, VipGoldBorder, RoundedCornerShape(24.dp))
                .testTag("livekit_audio_route_dialog"),
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF0E0C16)
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
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(VipGold.copy(alpha = 0.2f))
                                .border(1.dp, VipGold, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.GraphicEq,
                                contentDescription = "LiveKit Audio",
                                tint = VipGold,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "LiveKit Voice & Audio Output",
                                color = NoiseWhite,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp
                            )
                            Text(
                                text = when {
                                    isLocalSpeaking -> "Speaking • Mic Active"
                                    isMicMuted -> "Microphone Muted"
                                    isInVoiceRoom -> "Connected • Real-Time Voice"
                                    else -> "Select preferred audio routing"
                                },
                                color = if (isLocalSpeaking) OnlineGreen else NoiseTextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = NoiseTextSecondary,
                        modifier = Modifier
                            .size(20.dp)
                            .clickable { onDismiss() }
                    )
                }

                if (activeSpeakers.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(OnlineGreen.copy(alpha = 0.14f))
                            .border(1.dp, OnlineGreen.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = null,
                            tint = OnlineGreen,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Speaking now: ${activeSpeakers.joinToString(", ")}",
                            color = NoiseWhite,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                AudioOutputRoute.entries.forEach { route ->
                    val isSelected = route == currentRoute
                    val isDetected = route in availableRoutes
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isSelected) Color(0xFF1F1930) else Color(0xFF151320))
                            .border(
                                width = 1.dp,
                                color = if (isSelected) VipGold else NoiseBorder,
                                shape = RoundedCornerShape(14.dp)
                            )
                            .clickable { onSelectRoute(route) }
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                            .testTag("audio_route_option_${route.name.lowercase()}"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = when (route) {
                                    AudioOutputRoute.SPEAKER -> Icons.AutoMirrored.Filled.VolumeUp
                                    AudioOutputRoute.EARPIECE -> Icons.Default.Hearing
                                    AudioOutputRoute.BLUETOOTH -> Icons.Default.BluetoothAudio
                                },
                                contentDescription = route.label,
                                tint = if (isSelected) VipGold else NoiseWhite,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = route.label,
                                    color = if (isSelected) VipGold else NoiseWhite,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = when (route) {
                                        AudioOutputRoute.SPEAKER -> "Loudspeaker for party stage & music"
                                        AudioOutputRoute.EARPIECE -> "Private handset earpiece audio"
                                        AudioOutputRoute.BLUETOOTH -> if (isDetected) {
                                            "Bluetooth headset connected & ready"
                                        } else {
                                            "Connect Bluetooth headset for wireless voice"
                                        }
                                    },
                                    color = NoiseTextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Selected",
                                tint = VipGold,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
