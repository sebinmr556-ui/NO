package com.example.ui

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Real-time frequency band state for an individual voice slot.
 * Strictly isolated per slot so multiple speakers animate independently
 * and muted/empty slots never show voice-reactive deformation.
 */
@Stable
data class SlotVoiceAudioBands(
    val voiceIntensity: Float = 0f,
    val bassLevel: Float = 0f,
    val midLevel: Float = 0f,
    val highLevel: Float = 0f,
    val isSpeaking: Boolean = false
)

private data class SphereParticleSpec(
    val theta: Float,
    val phi: Float,
    val radialDistance: Float,
    val blinkSpeed: Float,
    val phaseOffset: Float,
    val driftSpeed: Float,
    val baseRadiusPx: Float,
    val maxBrightness: Float,
    val isOuterHalo: Boolean
)

/**
 * Real-time microphone + per-slot voice frequency band analyzer.
 * - For the local user's slot (when unmuted and mic permission is granted): captures real PCM samples
 *   from [AudioRecord] and decomposes energy into bass, mid, and high frequency bands + RMS intensity.
 * - When muted (`isMuted == true`) or empty (`!isOccupied`): all reactive bands decay smoothly to 0f.
 * - For remote unmuted speakers (`isOccupied && !isMuted && isSpeaking`): computes independent per-slot
 *   real-time voice band envelopes seeded uniquely by `seatNumber` and `occupantUserId`.
 */
@SuppressLint("MissingPermission")
@Composable
fun rememberSlotVoiceAudioBands(
    seatNumber: Int,
    occupantUserId: String,
    isOccupied: Boolean,
    isMuted: Boolean,
    isLocalUserSeat: Boolean,
    hasMicPermission: Boolean,
    isSpeakingSignal: Boolean
): SlotVoiceAudioBands {
    var rawIntensity by remember(seatNumber, occupantUserId) { mutableFloatStateOf(0f) }
    var rawBass by remember(seatNumber, occupantUserId) { mutableFloatStateOf(0f) }
    var rawMid by remember(seatNumber, occupantUserId) { mutableFloatStateOf(0f) }
    var rawHigh by remember(seatNumber, occupantUserId) { mutableFloatStateOf(0f) }

    DisposableEffect(seatNumber, occupantUserId, isOccupied, isMuted) {
        onDispose {
            rawIntensity = 0f
            rawBass = 0f
            rawMid = 0f
            rawHigh = 0f
        }
    }

    LaunchedEffect(
        seatNumber,
        occupantUserId,
        isOccupied,
        isMuted,
        isLocalUserSeat,
        hasMicPermission,
        isSpeakingSignal
    ) {
        if (!isOccupied || isMuted) {
            rawIntensity = 0f
            rawBass = 0f
            rawMid = 0f
            rawHigh = 0f
            return@LaunchedEffect
        }

        val seed = (seatNumber * 37 + occupantUserId.hashCode()).toLong()

        if (isLocalUserSeat && hasMicPermission) {
            withContext(Dispatchers.Default) {
                var audioRecord: AudioRecord? = null
                try {
                    val sampleRate = 8000
                    val minBuf = AudioRecord.getMinBufferSize(
                        sampleRate,
                        AudioFormat.CHANNEL_IN_MONO,
                        AudioFormat.ENCODING_PCM_16BIT
                    )
                    if (minBuf > 0) {
                        val candidate = AudioRecord(
                            MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                            sampleRate,
                            AudioFormat.CHANNEL_IN_MONO,
                            AudioFormat.ENCODING_PCM_16BIT,
                            max(minBuf, 512)
                        )
                        if (candidate.state == AudioRecord.STATE_INITIALIZED) {
                            audioRecord = candidate
                            audioRecord.startRecording()
                        } else {
                            candidate.release()
                        }
                    }
                } catch (_: Throwable) {
                    audioRecord = null
                }

                val pcmBuffer = ShortArray(256)
                var tick = 0L
                try {
                    while (isActive) {
                        var capturedFromHardware = false
                        val rec = audioRecord
                        if (rec != null && rec.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                            val readCount = try {
                                rec.read(pcmBuffer, 0, pcmBuffer.size)
                            } catch (_: Throwable) {
                                0
                            }
                            if (readCount > 16) {
                                var sumSq = 0.0
                                var lowDiffAcc = 0.0
                                var midDiffAcc = 0.0
                                var highDiffAcc = 0.0
                                var lpPrev = 0.0
                                for (i in 0 until readCount) {
                                    val sample = pcmBuffer[i] / 32768.0
                                    sumSq += sample * sample
                                    // Simple 1-pole low-pass for bass (~350 Hz at 8kHz)
                                    val lp = 0.82 * lpPrev + 0.18 * sample
                                    lpPrev = lp
                                    lowDiffAcc += lp * lp
                                    if (i > 0) {
                                        val prev = pcmBuffer[i - 1] / 32768.0
                                        val diff1 = sample - prev
                                        highDiffAcc += diff1 * diff1
                                        if (i > 1) {
                                            val prev2 = pcmBuffer[i - 2] / 32768.0
                                            val bp = sample - 0.5 * prev - 0.5 * prev2
                                            midDiffAcc += bp * bp
                                        }
                                    }
                                }
                                val rms = sqrt(sumSq / readCount).toFloat()
                                if (rms > 0.012f) {
                                    capturedFromHardware = true
                                    rawIntensity = (rms * 5.8f).coerceIn(0f, 1f)
                                    rawBass = (sqrt(lowDiffAcc / readCount).toFloat() * 7.2f).coerceIn(0f, 1f)
                                    rawMid = (sqrt(midDiffAcc / readCount).toFloat() * 6.4f).coerceIn(0f, 1f)
                                    rawHigh = (sqrt(highDiffAcc / readCount).toFloat() * 8.0f).coerceIn(0f, 1f)
                                }
                            }
                        }

                        if (!capturedFromHardware) {
                            if (isSpeakingSignal) {
                                val t = tick * 0.14f + (seed and 0xFF) * 0.07f
                                val envelope = (0.45f + 0.35f * sin(t * 1.9f) + 0.20f * cos(t * 3.3f)).coerceIn(0.18f, 1f)
                                rawIntensity = envelope
                                rawBass = (0.50f + 0.45f * abs(sin(t * 1.4f))).coerceIn(0.15f, 1f)
                                rawMid = (0.42f + 0.48f * abs(cos(t * 2.7f + 0.8f))).coerceIn(0.15f, 1f)
                                rawHigh = (0.38f + 0.56f * abs(sin(t * 4.3f + 1.7f))).coerceIn(0.12f, 1f)
                            } else {
                                rawIntensity = 0f
                                rawBass = 0f
                                rawMid = 0f
                                rawHigh = 0f
                            }
                        }
                        tick++
                        delay(45L)
                    }
                } finally {
                    try {
                        audioRecord?.stop()
                    } catch (_: Throwable) {
                    }
                    try {
                        audioRecord?.release()
                    } catch (_: Throwable) {
                    }
                }
            }
        } else {
            // Remote participant slot: react only when unmuted and actively speaking
            var tick = 0L
            val phaseSeed = ((seed and 0x3FFL).toFloat()) * 0.13f
            while (isActive) {
                if (isSpeakingSignal && !isMuted && isOccupied) {
                    val t = tick * 0.15f + phaseSeed
                    // Distinct per-slot cadence so two speakers never pulse identically
                    val cadenceGate = sin(t * 0.85f + seatNumber * 0.9f)
                    if (cadenceGate > -0.35f) {
                        val syllabic = (0.48f + 0.34f * sin(t * 2.3f) + 0.18f * cos(t * 3.9f)).coerceIn(0.20f, 1f)
                        rawIntensity = syllabic
                        rawBass = (0.52f + 0.44f * abs(sin(t * 1.6f + seatNumber))).coerceIn(0.18f, 1f)
                        rawMid = (0.45f + 0.46f * abs(cos(t * 2.9f + seatNumber * 0.5f))).coerceIn(0.16f, 1f)
                        rawHigh = (0.40f + 0.55f * abs(sin(t * 4.6f + seatNumber * 1.3f))).coerceIn(0.15f, 1f)
                    } else {
                        // Brief natural micro-pause between spoken phrases
                        rawIntensity = 0.14f
                        rawBass = 0.12f
                        rawMid = 0.15f
                        rawHigh = 0.10f
                    }
                } else {
                    rawIntensity = 0f
                    rawBass = 0f
                    rawMid = 0f
                    rawHigh = 0f
                }
                tick++
                delay(50L)
            }
        }
    }

    val smoothIntensity by animateFloatAsState(
        targetValue = if (isOccupied && !isMuted) rawIntensity else 0f,
        animationSpec = tween(durationMillis = 120, easing = LinearEasing),
        label = "slot_voice_intensity_$seatNumber"
    )
    val smoothBass by animateFloatAsState(
        targetValue = if (isOccupied && !isMuted) rawBass else 0f,
        animationSpec = tween(durationMillis = 135, easing = LinearEasing),
        label = "slot_voice_bass_$seatNumber"
    )
    val smoothMid by animateFloatAsState(
        targetValue = if (isOccupied && !isMuted) rawMid else 0f,
        animationSpec = tween(durationMillis = 110, easing = LinearEasing),
        label = "slot_voice_mid_$seatNumber"
    )
    val smoothHigh by animateFloatAsState(
        targetValue = if (isOccupied && !isMuted) rawHigh else 0f,
        animationSpec = tween(durationMillis = 95, easing = LinearEasing),
        label = "slot_voice_high_$seatNumber"
    )

    return SlotVoiceAudioBands(
        voiceIntensity = smoothIntensity,
        bassLevel = smoothBass,
        midLevel = smoothMid,
        highLevel = smoothHigh,
        isSpeaking = isOccupied && !isMuted && smoothIntensity > 0.08f
    )
}

/**
 * Exact 3D Wireframe Sphere Visualizer recreating the uploaded blue, cyan, purple, violet & pink
 * undulating 3D wireframe mesh sphere with independently blinking/sparkling particles and a
 * 100% transparent background (zero black background, zero rectangle, zero circle fill).
 */
@Composable
fun Sphere3DVoiceVisualizer(
    seatNumber: Int,
    audioBands: SlotVoiceAudioBands,
    isOccupied: Boolean,
    isMuted: Boolean,
    modifier: Modifier = Modifier,
    detailLevel: Int = 1 // 2 for large crown seats, 1 for standard participant seats
) {
    val infiniteTransition = rememberInfiniteTransition(label = "sphere3d_transition_$seatNumber")

    // Continuous smooth 3D rotation & wave phase so idle slots gently breathe and rotate
    val masterPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2.0 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 7200 + (seatNumber % 4) * 450, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sphere3d_master_phase_$seatNumber"
    )

    val particleClock by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (20.0 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 24000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sphere3d_particle_clock_$seatNumber"
    )

    // Pre-generate deterministic, independent particles for this slot
    val particles = remember(seatNumber, detailLevel) {
        val count = if (detailLevel >= 2) 76 else 54
        List(count) { idx ->
            val goldenAngle = PI.toFloat() * (3f - sqrt(5f))
            val yNorm = 1f - (idx.toFloat() / (count - 1).coerceAtLeast(1)) * 2f
            val theta = kotlin.math.acos(yNorm.coerceIn(-1f, 1f))
            val phi = (idx * goldenAngle + seatNumber * 0.47f) % (2f * PI.toFloat())
            val isOuter = idx % 3 != 0
            val radial = if (isOuter) {
                1.08f + ((idx * 19 + seatNumber * 7) % 23) / 100f // 1.08 .. 1.30 outer halo
            } else {
                0.96f + ((idx * 13) % 11) / 100f // surface sparkle dots
            }
            val speed = 0.65f + ((idx * 29 + seatNumber * 11) % 37) / 14f // distinct blinking speeds
            val phase = ((idx * 53 + seatNumber * 17) % 100) * 0.0628f
            val drift = 0.25f + ((idx * 17) % 19) / 30f
            val radiusPx = if (idx % 5 == 0) 2.5f else if (idx % 2 == 0) 1.85f else 1.3f
            val peakBright = 0.55f + ((idx * 41) % 45) / 100f
            SphereParticleSpec(
                theta = theta,
                phi = phi,
                radialDistance = radial,
                blinkSpeed = speed,
                phaseOffset = phase,
                driftSpeed = drift,
                baseRadiusPx = radiusPx,
                maxBrightness = peakBright,
                isOuterHalo = isOuter
            )
        }
    }

    // Reference color palette matching uploaded image:
    // Top-Left: Electric Blue (#0055FF) & Cyan (#00E5FF)
    // Center: Deep Violet (#6D28D9) & Vibrant Purple (#A855F7)
    // Bottom-Right: Hot Pink (#EC4899) & Neon Magenta (#FF1CF7)
    val colorElectricBlue = Color(0xFF0055FF)
    val colorNeonCyan = Color(0xFF00E5FF)
    val colorDeepViolet = Color(0xFF7C3AED)
    val colorVibrantPurple = Color(0xFFA855F7)
    val colorHotPink = Color(0xFFEC4899)
    val colorNeonMagenta = Color(0xFFFF1CF7)

    // Reusable path buffer to avoid per-frame allocations
    val wirePath = remember { Path() }

    // GPU layer with CompositingStrategy.Offscreen is NOT needed for background fill — we keep
    // default alpha compositing so the background is 100% transparent over the chatroom!
    Canvas(
        modifier = modifier.graphicsLayer {
            clip = false
        }
    ) {
        // IMPORTANT: Do NOT draw any background rectangle or circle here.
        // The canvas remains 100% transparent so the NOICE LIVE chatroom background shows through.

        val cx = size.width * 0.5f
        val cy = size.height * 0.5f
        val minDim = min(size.width, size.height)
        if (minDim <= 4f) return@Canvas

        val activeVoice = if (isOccupied && !isMuted) audioBands.voiceIntensity else 0f
        val bass = if (isOccupied && !isMuted) audioBands.bassLevel else 0f
        val mid = if (isOccupied && !isMuted) audioBands.midLevel else 0f
        val high = if (isOccupied && !isMuted) audioBands.highLevel else 0f

        // Base visibility scale: empty slots show a subtle idle 3D sphere, occupied slots glow brighter,
        // and speaking slots expand dynamically with voice intensity.
        val masterAlpha = when {
            !isOccupied -> 0.52f
            isMuted -> 0.68f
            else -> (0.82f + 0.18f * activeVoice).coerceIn(0f, 1f)
        }

        // Dynamic radius expansion driven by voice intensity + gentle idle breathing
        val idleBreath = 0.025f * sin(masterPhase * 2f + seatNumber)
        val voiceExpansion = 0.13f * activeVoice + 0.06f * bass
        val baseRadius = minDim * (0.415f + idleBreath + voiceExpansion)

        // 3D Sphere Grid Resolution (Latitude rings x Longitude meridians)
        val latRings = if (detailLevel >= 2) 18 else 14
        val lonSegments = if (detailLevel >= 2) 24 else 18

        // Slow 3D tilt & rotation angles
        val rotY = masterPhase + seatNumber * 0.45f + mid * 0.35f
        val rotX = 0.38f * sin(masterPhase * 0.7f + seatNumber * 0.3f) + bass * 0.22f
        val cosY = cos(rotY)
        val sinY = sin(rotY)
        val cosX = cos(rotX)
        val sinX = sin(rotX)

        // Wave amplitude: gentle organic ripples when idle; strong multi-harmonic ridges when speaking
        val idleRippleAmp = 0.085f
        val bassWaveAmp = 0.19f * bass
        val midWaveAmp = 0.14f * mid
        val highWaveAmp = 0.08f * high
        val phaseFast = masterPhase * 3.0f + activeVoice * 2.4f

        fun computeSurfacePoint3D(theta: Float, phi: Float): Triple<Float, Float, Float> {
            // Multi-harmonic spherical wave displacement matching the undulating wireframe lobes in the reference image
            val lobe1 = sin(5f * phi + phaseFast) * sin(4f * theta - masterPhase * 2f)
            val lobe2 = cos(7f * phi - masterPhase * 3f) * sin(6f * theta + phaseFast * 0.8f)
            val lobe3 = sin(10f * phi + particleClock * 0.6f) * cos(8f * theta)

            val radialMod = 1f +
                (idleRippleAmp + bassWaveAmp) * lobe1 +
                (idleRippleAmp * 0.65f + midWaveAmp) * lobe2 +
                highWaveAmp * lobe3

            val r = baseRadius * radialMod
            val sinTheta = sin(theta)
            val x0 = r * sinTheta * cos(phi)
            val y0 = r * cos(theta)
            val z0 = r * sinTheta * sin(phi)

            // Rotate around Y then X
            val x1 = x0 * cosY + z0 * sinY
            val z1 = -x0 * sinY + z0 * cosY
            val y2 = y0 * cosX - z1 * sinX
            val z2 = y0 * sinX + z1 * cosX

            // Perspective projection factor
            val cameraDist = baseRadius * 3.4f
            val perspective = cameraDist / (cameraDist - z2 * 0.55f).coerceAtLeast(baseRadius * 1.2f)
            return Triple(cx + x1 * perspective, cy + y2 * perspective, z2 / baseRadius)
        }

        fun colorForScreenPosition(px: Float, py: Float, depthZ: Float, alphaBoost: Float = 1f): Color {
            // Normalized diagonal coordinate from Top-Left (0.0 -> Blue/Cyan) to Bottom-Right (1.0 -> Pink/Magenta)
            val nx = ((px - (cx - baseRadius)) / (baseRadius * 2f)).coerceIn(0f, 1f)
            val ny = ((py - (cy - baseRadius)) / (baseRadius * 2f)).coerceIn(0f, 1f)
            val diag = (nx * 0.55f + ny * 0.45f).coerceIn(0f, 1f)

            // Secondary cross-axis blend for cyan vs electric blue & violet vs magenta highlights
            val cross = (1f - ny * 0.6f + nx * 0.4f).coerceIn(0f, 1f)

            val baseColor = when {
                diag < 0.35f -> {
                    val t = (diag / 0.35f).coerceIn(0f, 1f)
                    val topLeftMix = lerp(colorElectricBlue, colorNeonCyan, (cross * 0.7f + 0.3f * (depthZ + 1f) * 0.5f).coerceIn(0f, 1f))
                    lerp(topLeftMix, colorDeepViolet, t)
                }
                diag < 0.68f -> {
                    val t = ((diag - 0.35f) / 0.33f).coerceIn(0f, 1f)
                    val midMix = lerp(colorDeepViolet, colorVibrantPurple, ((depthZ + 1f) * 0.5f).coerceIn(0f, 1f))
                    lerp(midMix, colorHotPink, t)
                }
                else -> {
                    val t = ((diag - 0.68f) / 0.32f).coerceIn(0f, 1f)
                    lerp(colorHotPink, colorNeonMagenta, t)
                }
            }

            // Depth-based illumination: front-facing ridges glow brighter, back-facing mesh stays semi-transparent
            val depthFactor = (0.38f + 0.62f * ((depthZ + 1f) * 0.5f)).coerceIn(0.22f, 1f)
            val finalAlpha = (masterAlpha * depthFactor * alphaBoost).coerceIn(0f, 1f)
            return baseColor.copy(alpha = finalAlpha)
        }

        val strokeBasePx = if (detailLevel >= 2) 1.35f else 1.05f
        val activeStrokePx = strokeBasePx + activeVoice * 0.75f

        // 1. Draw Latitude Wireframe Rings (curved horizontal mesh lines)
        for (i in 1 until latRings) {
            val theta = (i.toFloat() / latRings) * PI.toFloat()
            wirePath.reset()
            var avgX = 0f
            var avgY = 0f
            var avgZ = 0f
            for (j in 0..lonSegments) {
                val phi = (j.toFloat() / lonSegments) * (2f * PI.toFloat())
                val (px, py, pz) = computeSurfacePoint3D(theta, phi)
                if (j == 0) {
                    wirePath.moveTo(px, py)
                } else {
                    wirePath.lineTo(px, py)
                }
                avgX += px
                avgY += py
                avgZ += pz
            }
            val count = (lonSegments + 1).toFloat()
            avgX /= count
            avgY /= count
            avgZ /= count

            val lineColor = colorForScreenPosition(avgX, avgY, avgZ, alphaBoost = 0.78f + 0.22f * activeVoice)

            // Outer soft neon glow pass for front-facing rings
            if (avgZ > -0.15f) {
                drawPath(
                    path = wirePath,
                    color = lineColor.copy(alpha = (lineColor.alpha * (0.28f + 0.25f * activeVoice)).coerceIn(0f, 0.55f)),
                    style = Stroke(width = activeStrokePx * 2.6f, cap = StrokeCap.Round, join = StrokeJoin.Round),
                    blendMode = BlendMode.Plus
                )
            }

            // Crisp vector wireframe line
            drawPath(
                path = wirePath,
                color = lineColor,
                style = Stroke(width = activeStrokePx, cap = StrokeCap.Round, join = StrokeJoin.Round),
                blendMode = BlendMode.Plus
            )
        }

        // 2. Draw Longitude Wireframe Meridians (curved vertical mesh lines + glowing mesh vertex dots)
        for (j in 0 until lonSegments) {
            val phi = (j.toFloat() / lonSegments) * (2f * PI.toFloat())
            wirePath.reset()
            var avgX = 0f
            var avgY = 0f
            var avgZ = 0f
            for (i in 0..latRings) {
                val theta = (i.toFloat() / latRings) * PI.toFloat()
                val (px, py, pz) = computeSurfacePoint3D(theta, phi)
                if (i == 0) {
                    wirePath.moveTo(px, py)
                } else {
                    wirePath.lineTo(px, py)
                }
                avgX += px
                avgY += py
                avgZ += pz

                // Draw glowing vertex dots on front-facing mesh intersections (like the sparkling dots inside the mesh in the reference image)
                if (i in 2 until latRings step 2 && j % 2 == 0 && pz > -0.1f) {
                    val vertexTwinkle = 0.45f + 0.55f * (0.5f + 0.5f * sin(particleClock * 1.4f + i * 1.7f + j * 2.3f))
                    val vertexColor = colorForScreenPosition(px, py, pz, alphaBoost = vertexTwinkle)
                    val dotRadius = (if (detailLevel >= 2) 1.55f else 1.2f) * (1f + 0.45f * high)
                    drawCircle(
                        color = vertexColor,
                        radius = dotRadius,
                        center = Offset(px, py),
                        blendMode = BlendMode.Plus
                    )
                }
            }
            val count = (latRings + 1).toFloat()
            avgX /= count
            avgY /= count
            avgZ /= count

            val meridianColor = colorForScreenPosition(avgX, avgY, avgZ, alphaBoost = 0.72f + 0.28f * activeVoice)
            if (avgZ > -0.1f) {
                drawPath(
                    path = wirePath,
                    color = meridianColor.copy(alpha = (meridianColor.alpha * 0.25f).coerceIn(0f, 0.48f)),
                    style = Stroke(width = activeStrokePx * 2.3f, cap = StrokeCap.Round, join = StrokeJoin.Round),
                    blendMode = BlendMode.Plus
                )
            }
            drawPath(
                path = wirePath,
                color = meridianColor,
                style = Stroke(width = activeStrokePx * 0.92f, cap = StrokeCap.Round, join = StrokeJoin.Round),
                blendMode = BlendMode.Plus
            )
        }

        // 3. Draw Independently Blinking, Fading, and Floating Halo & Surface Particles
        for (idx in particles.indices) {
            val p = particles[idx]
            // Each dot has its own independent blink frequency and phase offset so they NEVER blink simultaneously
            val blinkWave = sin(particleClock * p.blinkSpeed + p.phaseOffset)
            val secondarySparkle = cos(particleClock * (p.blinkSpeed * 1.63f) - p.phaseOffset * 0.7f)
            val combinedBlink = (blinkWave * 0.65f + secondarySparkle * 0.35f)

            // Some dots temporarily fade out completely (when combinedBlink < -0.25f) and gradually fade back in
            val rawVisibility = ((combinedBlink + 0.25f) / 1.25f).coerceIn(0f, 1f)
            if (rawVisibility <= 0.02f) continue

            // Smooth orbital drift + high-frequency voice energy boost
            val driftedPhi = p.phi + particleClock * 0.08f * p.driftSpeed
            val driftedTheta = (p.theta + 0.06f * sin(particleClock * 0.35f + p.phaseOffset)).coerceIn(0.05f, PI.toFloat() - 0.05f)
            val radialPulse = p.radialDistance +
                0.04f * sin(particleClock * 0.9f + p.phaseOffset) +
                (if (p.isOuterHalo) 0.11f * high + 0.06f * activeVoice else 0.05f * bass)

            val r = baseRadius * radialPulse
            val sinT = sin(driftedTheta)
            val x0 = r * sinT * cos(driftedPhi)
            val y0 = r * cos(driftedTheta)
            val z0 = r * sinT * sin(driftedPhi)

            val x1 = x0 * cosY + z0 * sinY
            val z1 = -x0 * sinY + z0 * cosY
            val y2 = y0 * cosX - z1 * sinX
            val z2 = y0 * sinX + z1 * cosX

            val px = cx + x1
            val py = cy + y2
            val depthNorm = (z2 / r).coerceIn(-1f, 1f)

            val sparkleBoost = (rawVisibility * p.maxBrightness * (1f + 0.55f * high)).coerceIn(0f, 1f)
            val particleColor = colorForScreenPosition(px, py, depthNorm, alphaBoost = sparkleBoost)
            val radius = p.baseRadiusPx * (if (detailLevel >= 2) 1.15f else 0.92f) * (0.75f + 0.55f * rawVisibility + 0.45f * high)

            // Soft outer glow halo around brighter sparkling particles
            if (rawVisibility > 0.55f) {
                drawCircle(
                    color = particleColor.copy(alpha = (particleColor.alpha * 0.36f).coerceIn(0f, 0.65f)),
                    radius = radius * 2.4f,
                    center = Offset(px, py),
                    blendMode = BlendMode.Plus
                )
            }

            // Crisp glowing particle core (with white-hot center on peak sparkle)
            val coreColor = if (rawVisibility > 0.82f) {
                lerp(particleColor, Color.White.copy(alpha = particleColor.alpha), 0.42f)
            } else {
                particleColor
            }
            drawCircle(
                color = coreColor,
                radius = radius,
                center = Offset(px, py),
                blendMode = BlendMode.Plus
            )
        }
    }
}
