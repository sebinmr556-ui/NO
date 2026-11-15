package com.example.ui

import android.app.Activity
import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.VideoView
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.R
import com.example.ui.theme.NoisePink
import com.example.ui.theme.NoisePurple
import com.example.ui.theme.VipGold
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Tracks whether the user has completed the initial Login / Sign Up screen on first installation.
 * Ensures a brand-new installation always displays the premium DJ Party Login / Sign Up screen first.
 */
object AuthSessionPrefs {
    private const val PREFS_NAME = "noice_live_auth_onboarding_prefs"
    private const val KEY_FIRST_INSTALL_LOGIN_DONE = "first_install_login_completed"

    fun hasCompletedFirstInstallLogin(context: Context): Boolean {
        return runCatching {
            context.applicationContext
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_FIRST_INSTALL_LOGIN_DONE, false)
        }.getOrDefault(false)
    }

    fun markFirstInstallLoginCompleted(context: Context) {
        runCatching {
            context.applicationContext
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_FIRST_INSTALL_LOGIN_DONE, true)
                .apply()
        }
    }

    fun resetFirstInstallLogin(context: Context) {
        runCatching {
            context.applicationContext
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_FIRST_INSTALL_LOGIN_DONE, false)
                .apply()
        }
    }
}

fun attemptAutoSignIn(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: () -> Unit,
    onUnauthenticated: () -> Unit,
    scope: CoroutineScope
) {
    // Only auto-sign-in if the user has already completed the first-installation login screen
    if (!AuthSessionPrefs.hasCompletedFirstInstallLogin(context)) {
        onUnauthenticated()
        return
    }
    if (Firebase.auth.currentUser != null) {
        onAuthSuccess()
        return
    }
    val clientId = try {
        context.getString(R.string.default_web_client_id)
    } catch (e: Exception) {
        onUnauthenticated()
        return
    }

    val googleIdOption = GetGoogleIdOption.Builder()
        .setFilterByAuthorizedAccounts(true)
        .setServerClientId(clientId)
        .setAutoSelectEnabled(true)
        .build()

    val request = GetCredentialRequest.Builder().addCredentialOption(googleIdOption).build()

    scope.launch {
        try {
            val result = credentialManager.getCredential(context, request)
            val credential = result.credential
            if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                Firebase.auth.signInWithCredential(authCredential).await()
                AuthSessionPrefs.markFirstInstallLoginCompleted(context)
                onAuthSuccess()
            } else {
                onUnauthenticated()
            }
        } catch (e: Exception) {
            onUnauthenticated()
        }
    }
}

fun onGoogleSignInClicked(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: () -> Unit,
    onAuthError: (String) -> Unit,
    scope: CoroutineScope,
    onAuthCancelled: () -> Unit = {}
) {
    val clientId = try {
        context.getString(R.string.default_web_client_id)
    } catch (e: Exception) {
        onAuthError("Google Sign-In configuration missing: default_web_client_id not found")
        return
    }

    val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
    val request = GetCredentialRequest.Builder().addCredentialOption(signInOption).build()

    scope.launch {
        try {
            val result = credentialManager.getCredential(context as Activity, request)
            val credential = result.credential
            if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                Firebase.auth.signInWithCredential(authCredential).await()
                AuthSessionPrefs.markFirstInstallLoginCompleted(context)
                onAuthSuccess()
            } else {
                onAuthError("Unexpected credential type")
            }
        } catch (e: GetCredentialCancellationException) {
            Log.w("Auth", "Google Sign-In flow cancelled or dismissed: ${e.message}", e)
            onAuthCancelled()
        } catch (e: Exception) {
            Log.e("Auth", "Google Sign-In failed", e)
            onAuthError(e.localizedMessage ?: "Sign in failed")
        }
    }
}

fun signOut(
    context: Context,
    credentialManager: CredentialManager,
    onSignOutComplete: () -> Unit,
    scope: CoroutineScope
) {
    AuthSessionPrefs.resetFirstInstallLogin(context)
    Firebase.auth.signOut()
    scope.launch {
        try {
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
        } catch (e: Exception) {
            Log.e("Auth", "Failed to clear credential state", e)
        } finally {
            onSignOutComplete()
        }
    }
}

/**
 * Full-Screen Seamless Looping DJ Music Party Stage Video Background.
 * - Plays a bundled raw video in a seamless loop if present (`R.raw.dj_party_bg` / `R.raw.login_dj_video`),
 *   AND renders a high-definition DJ concert stage (`R.drawable.img_dj_stage_party`) with real-time 60fps
 *   volumetric stage lasers, moving spotlights, animated DJ turntables/mixer EQ bars, and party lighting.
 * - Lifecycle-aware (`ON_PAUSE` / `ON_RESUME`) so it consumes zero CPU/GPU when backgrounded.
 */
@Composable
fun DjPartyStageVideoBackground(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var isLifecycleActive by remember { mutableStateOf(true) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME, Lifecycle.Event.ON_START -> isLifecycleActive = true
                Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP -> isLifecycleActive = false
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Check if an optional raw MP4 video file is bundled in res/raw
    val rawVideoResId = remember(context) {
        val candidates = listOf("dj_party_bg", "login_dj_video", "dj_stage_loop", "noice_login_bg")
        candidates.firstNotNullOfOrNull { name ->
            val id = context.resources.getIdentifier(name, "raw", context.packageName)
            if (id != 0) id else null
        } ?: 0
    }

    val transition = rememberInfiniteTransition(label = "dj_party_stage_video_loop")
    val cameraPan by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 11000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dj_camera_pan"
    )
    val laserPhase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2.0 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 5200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "dj_laser_phase"
    )
    val beatPulse by transition.animateFloat(
        initialValue = 0.72f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 480, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dj_beat_pulse"
    )

    Box(modifier = modifier.fillMaxSize()) {
        // Layer 1: High-definition DJ Stage Concert visual with smooth Ken Burns camera motion
        Image(
            painter = painterResource(id = R.drawable.img_dj_stage_party),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val scale = if (isLifecycleActive) 1.06f + 0.06f * cameraPan else 1.04f
                    scaleX = scale
                    scaleY = scale
                    translationX = if (isLifecycleActive) (cameraPan - 0.5f) * 26f else 0f
                    translationY = if (isLifecycleActive) (0.5f - cameraPan) * 16f else 0f
                }
        )

        // Optional Layer 1B: Seamless hardware-decoded VideoView loop if raw video resource exists
        if (rawVideoResId != 0 && isLifecycleActive) {
            AndroidView(
                factory = { ctx ->
                    VideoView(ctx).apply {
                        val uri = Uri.parse("android.resource://${ctx.packageName}/$rawVideoResId")
                        setVideoURI(uri)
                        setOnPreparedListener { mp ->
                            mp.isLooping = true
                            mp.setVolume(0f, 0f)
                            start()
                        }
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // Layer 2: Real-time 60fps Volumetric DJ Stage Lasers, Moving Spotlights, EQ LEDs & Party Bokeh
        if (isLifecycleActive) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                // Sweeping Stage Spotlights & Laser Beams from Top Truss Rig
                val beamOrigins = listOf(
                    Offset(w * 0.15f, -h * 0.04f),
                    Offset(w * 0.50f, -h * 0.05f),
                    Offset(w * 0.85f, -h * 0.04f)
                )
                val beamColors = listOf(
                    Color(0xFF9D4EDD), // Electric Purple
                    Color(0xFFFF2E93), // Neon Pink
                    Color(0xFF00E5FF), // Cyan Laser
                    Color(0xFFFFB900)  // Golden Stage Beam
                )

                beamOrigins.forEachIndexed { idx, origin ->
                    val sweepAngle = sin(laserPhase + idx * 1.4f) * 0.42f
                    val targetX = (origin.x + sweepAngle * w * 0.78f).coerceIn(-w * 0.2f, w * 1.2f)
                    val spread = w * (0.20f + 0.05f * beatPulse)

                    val conePath = Path().apply {
                        moveTo(origin.x, origin.y)
                        lineTo(targetX - spread, h * 0.92f)
                        lineTo(targetX + spread, h * 0.92f)
                        close()
                    }
                    val beamColor = beamColors[idx % beamColors.size]
                    drawPath(
                        path = conePath,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                beamColor.copy(alpha = 0.34f * beatPulse),
                                beamColor.copy(alpha = 0.14f),
                                Color.Transparent
                            ),
                            startY = 0f,
                            endY = h * 0.92f
                        )
                    )

                    // Crisp Laser Core Line
                    drawLine(
                        color = beamColor.copy(alpha = 0.55f * beatPulse),
                        start = origin,
                        end = Offset(targetX, h * 0.78f),
                        strokeWidth = 2.5f,
                        cap = StrokeCap.Round
                    )
                }

                // Animated DJ Booth Equalizer Spectrum LED Bars along the lower stage horizon
                val barCount = 28
                val totalEqWidth = w * 0.76f
                val startX = (w - totalEqWidth) * 0.5f
                val stepX = totalEqWidth / barCount
                val baseStageY = h * 0.64f

                for (i in 0 until barCount) {
                    val wave = 0.35f + 0.65f * kotlin.math.abs(sin(laserPhase * 2.2f + i * 0.45f))
                    val barHeight = h * 0.048f * wave * beatPulse
                    val bx = startX + i * stepX
                    val barColor = when {
                        i % 3 == 0 -> Color(0xFFFF2E93).copy(alpha = 0.38f)
                        i % 3 == 1 -> Color(0xFF8525FF).copy(alpha = 0.38f)
                        else -> Color(0xFF00E5FF).copy(alpha = 0.38f)
                    }
                    drawLine(
                        color = barColor,
                        start = Offset(bx, baseStageY),
                        end = Offset(bx, baseStageY - barHeight),
                        strokeWidth = (stepX * 0.52f).coerceAtLeast(3f),
                        cap = StrokeCap.Round
                    )
                }

                // Floating Party Bokeh & Gold/Magenta Stage Confetti Lights
                for (p in 0 until 18) {
                    val px = ((p * 97 + (laserPhase * 40f).toInt()) % 1000) / 1000f * w
                    val py = h * (0.12f + 0.75f * (((p * 173) % 100) / 100f) - 0.04f * sin(laserPhase + p))
                    val radius = (3.5f + (p % 4) * 2.8f) * beatPulse
                    val bokehColor = when (p % 4) {
                        0 -> Color(0xFFFFB900).copy(alpha = 0.45f)
                        1 -> Color(0xFFFF2E93).copy(alpha = 0.42f)
                        2 -> Color(0xFF00E5FF).copy(alpha = 0.40f)
                        else -> Color.White.copy(alpha = 0.50f)
                    }
                    drawCircle(
                        color = bokehColor,
                        radius = radius,
                        center = Offset(px, py)
                    )
                }
            }
        }

        // Layer 3: Cinematic vignette overlay so the centered NOICE LIVE logo & buttons pop clearly
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF060310).copy(alpha = 0.48f),
                            Color(0xFF0A051A).copy(alpha = 0.35f),
                            Color(0xFF060310).copy(alpha = 0.84f)
                        )
                    )
                )
        )
    }
}

/**
 * Resolves the official NOICE LIVE logo drawable resource without distorting or replacing it.
 * Checks for any uploaded `noice_logo` / `ic_noice_logo` drawable first, and falls back to
 * the official launcher icon (`R.mipmap.ic_launcher_round` / `R.drawable.ic_launcher_foreground`).
 */
@Composable
fun OfficialNoiceLiveLogoBadge(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val customLogoResId = remember(context) {
        val names = listOf("noice_logo", "ic_noice_logo", "noice_live_logo", "official_noice_logo")
        names.firstNotNullOfOrNull { name ->
            val id = context.resources.getIdentifier(name, "drawable", context.packageName)
            if (id != 0) id else null
        } ?: 0
    }

    val transition = rememberInfiniteTransition(label = "noice_logo_aura_anim")
    val auraScale by transition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "logo_aura_scale"
    )

    Box(
        modifier = modifier.testTag("official_noice_live_logo_container"),
        contentAlignment = Alignment.Center
    ) {
        // Outer luminous neon ring
        Box(
            modifier = Modifier
                .size(116.dp)
                .graphicsLayer {
                    scaleX = auraScale
                    scaleY = auraScale
                }
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            NoisePink.copy(alpha = 0.45f),
                            NoisePurple.copy(alpha = 0.28f),
                            Color.Transparent
                        )
                    )
                )
        )

        // Official Logo Frame — preserves exact aspect ratio (ContentScale.Fit) without distortion
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = Color(0xFF100A22).copy(alpha = 0.88f),
            modifier = Modifier
                .size(94.dp)
                .border(
                    width = 2.dp,
                    brush = Brush.linearGradient(
                        listOf(VipGold, NoisePink, Color(0xFF00E5FF), NoisePurple)
                    ),
                    shape = RoundedCornerShape(26.dp)
                )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(6.dp),
                contentAlignment = Alignment.Center
            ) {
                if (customLogoResId != 0) {
                    Image(
                        painter = painterResource(id = customLogoResId),
                        contentDescription = stringResource(R.string.app_name),
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Image(
                        painter = painterResource(id = R.drawable.ic_launcher_foreground),
                        contentDescription = stringResource(R.string.app_name),
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    // 0 = Login tab, 1 = Sign Up tab
    var selectedAuthTab by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        attemptAutoSignIn(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = onAuthSuccess,
            onUnauthenticated = {},
            scope = coroutineScope
        )
    }

    val gradientBrush = Brush.horizontalGradient(listOf(NoisePurple, NoisePink))

    val triggerGoogleAuth: () -> Unit = {
        errorMessage = null
        isLoading = true
        onGoogleSignInClicked(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = {
                isLoading = false
                AuthSessionPrefs.markFirstInstallLoginCompleted(context)
                onAuthSuccess()
            },
            onAuthError = { msg ->
                isLoading = false
                errorMessage = msg
            },
            scope = coroutineScope,
            onAuthCancelled = {
                isLoading = false
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("auth_screen_root"),
        contentAlignment = Alignment.Center
    ) {
        // 1. Full-screen DJ Music Party Stage Video Background (seamless loop)
        DjPartyStageVideoBackground(
            modifier = Modifier.fillMaxSize()
        )

        // 2. Centered Login / Sign Up Content Overlay
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 22.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Live DJ Stage Status Pill
            Surface(
                shape = RoundedCornerShape(50),
                color = Color(0xFF120B24).copy(alpha = 0.78f),
                modifier = Modifier
                    .border(
                        1.dp,
                        Brush.horizontalGradient(listOf(VipGold.copy(alpha = 0.7f), NoisePink.copy(alpha = 0.7f))),
                        RoundedCornerShape(50)
                    )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = VipGold,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = "NOICE LIVE • GLOBAL DJ PARTY & VIP ROOMS",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            // Center Official NOICE LIVE Logo & Brand Title
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                OfficialNoiceLiveLogoBadge()

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Realistic3DDiamondIcon(vipLevel = 10, size = 24.dp)
                    Text(
                        text = "NOICE LIVE",
                        style = TextStyle(
                            brush = Brush.horizontalGradient(
                                listOf(Color.White, VipGold, NoisePink, Color(0xFF00E5FF))
                            ),
                            fontSize = 34.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.5.sp
                        )
                    )
                    Realistic3DDiamondIcon(vipLevel = 5, size = 24.dp)
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "3D Party Rooms • Live Voice • 3D Ludo • Luxury VIP Gems",
                    color = Color(0xFFE6E1FA),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
            }

            // Bottom Glassmorphic Login / Sign Up Card
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.2.dp,
                        brush = Brush.linearGradient(
                            listOf(
                                VipGold.copy(alpha = 0.65f),
                                NoisePink.copy(alpha = 0.65f),
                                Color(0xFF00E5FF).copy(alpha = 0.55f)
                            )
                        ),
                        shape = RoundedCornerShape(28.dp)
                    ),
                shape = RoundedCornerShape(28.dp),
                color = Color(0xFF100B20).copy(alpha = 0.86f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Login / Sign Up Mode Switcher Tabs
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(50))
                            .background(Color(0xFF1D1536))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val tabs = listOf("Login", "Sign Up")
                        tabs.forEachIndexed { index, title ->
                            val selected = selectedAuthTab == index
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(50))
                                    .background(
                                        if (selected) gradientBrush
                                        else Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))
                                    )
                                    .clickable { selectedAuthTab = index }
                                    .padding(vertical = 10.dp)
                                    .testTag(if (index == 0) "auth_tab_login" else "auth_tab_signup"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = title,
                                    color = if (selected) Color.White else Color(0xFFB8B2D1),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    AnimatedContent(
                        targetState = selectedAuthTab,
                        transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(180)) },
                        label = "auth_mode_description"
                    ) { tab ->
                        Text(
                            text = if (tab == 0) {
                                "Welcome back to NOICE LIVE! Sign in to sync your profile, VIP 1–10 3D Diamonds, Direct Messages, and Live Party Rooms."
                            } else {
                                "Create your official NOICE LIVE account in one tap to unlock 3D Ludo, Voice Party Rooms, Reels, and VIP Gemstone Badges."
                            },
                            color = Color(0xFFDAD5F2),
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Primary Google Login / Sign Up Button (preserves testTag("google_sign_in_button"))
                    Button(
                        onClick = triggerGoogleAuth,
                        enabled = !isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .clip(RoundedCornerShape(27.dp))
                            .background(gradientBrush)
                            .testTag("google_sign_in_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent
                        )
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                                                        contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = if (selectedAuthTab == 0) "Sign in with Google" else "Sign up with Google",
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Secondary Login / Sign Up Quick Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                selectedAuthTab = 0
                                triggerGoogleAuth()
                            },
                            enabled = !isLoading,
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("quick_login_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountCircle,
                                contentDescription = null,
                                tint = VipGold,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Account Login",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                selectedAuthTab = 1
                                triggerGoogleAuth()
                            },
                            enabled = !isLoading,
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("quick_signup_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.PersonAdd,
                                contentDescription = null,
                                tint = Color(0xFF00E5FF),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "New Sign Up",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Existing Firebase session fast-resume option if a user is already signed in on first install
                    val existingFirebaseUser = remember { Firebase.auth.currentUser }
                    if (existingFirebaseUser != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFF1F1638),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, VipGold.copy(alpha = 0.55f), RoundedCornerShape(16.dp))
                                .clickable {
                                    AuthSessionPrefs.markFirstInstallLoginCompleted(context)
                                    onAuthSuccess()
                                }
                                .testTag("continue_existing_session_button")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VerifiedUser,
                                    contentDescription = null,
                                    tint = VipGold,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Continue as ${existingFirebaseUser.displayName ?: existingFirebaseUser.email ?: "VIP Member"}",
                                    color = VipGold,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }

                    if (errorMessage != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = errorMessage ?: "",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}
