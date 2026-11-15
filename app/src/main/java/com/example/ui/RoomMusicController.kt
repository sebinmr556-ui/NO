package com.example.ui

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import kotlin.concurrent.thread
import kotlin.math.PI
import kotlin.math.sin

class RoomMusicController(private val context: Context) {

    private val audioManager: AudioManager? =
        context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

    @Volatile
    private var isPlayingLoop: Boolean = false

    @Volatile
    private var currentTrackName: String = ""

    @Volatile
    private var currentVolume: Float = 0.8f

    private var audioTrack: AudioTrack? = null
    private var mediaPlayer: MediaPlayer? = null
    private var playbackThread: Thread? = null
    private var focusRequest: AudioFocusRequest? = null

    private val focusChangeListener = AudioManager.OnAudioFocusChangeListener { change ->
        when (change) {
            AudioManager.AUDIOFOCUS_LOSS,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                pause()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                applyVolumeInternal(currentVolume * 0.35f)
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                applyVolumeInternal(currentVolume)
            }
        }
    }

    fun startOrUpdate(trackName: String, playing: Boolean, volumePercent: Int, micActive: Boolean = false) {
        val normalizedVol = (volumePercent.coerceIn(0, 100) / 100f) * (if (micActive) 0.45f else 1.0f)
        currentVolume = normalizedVol
        applyVolumeInternal(normalizedVol)

        if (!playing || trackName.isBlank() || trackName.equals("None", ignoreCase = true)) {
            stop()
            return
        }

        if (isPlayingLoop && currentTrackName == trackName) {
            return
        }

        stop()
        currentTrackName = trackName
        requestAudioFocus()

        if (trackName.startsWith("content://") || trackName.startsWith("file://")) {
            try {
                val mp = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    setDataSource(context, Uri.parse(trackName))
                    isLooping = true
                    prepare()
                    setVolume(normalizedVol, normalizedVol)
                    start()
                }
                mediaPlayer = mp
                isPlayingLoop = true
                return
            } catch (_: Exception) {
                // Fall back to synthesized ambient track
            }
        }

        isPlayingLoop = true
        playbackThread = thread(start = true, isDaemon = true, name = "NoiseRoomMusic") {
            runSynthesizedLoop(trackName)
        }
    }

    fun playTrack(trackName: String, volumePercent: Long) {
        startOrUpdate(trackName, playing = true, volumePercent = volumePercent.toInt())
    }

    fun setVolume(volumePercent: Long) {
        setVolume(volumePercent.toInt(), micActive = false)
    }

    fun setVolume(volumePercent: Int, micActive: Boolean = false) {
        val normalizedVol = (volumePercent.coerceIn(0, 100) / 100f) * (if (micActive) 0.45f else 1.0f)
        currentVolume = normalizedVol
        applyVolumeInternal(normalizedVol)
    }

    fun pause() {
        isPlayingLoop = false
        try {
            mediaPlayer?.pause()
        } catch (_: Exception) {
        }
        try {
            audioTrack?.pause()
        } catch (_: Exception) {
        }
    }

    fun release() {
        stop()
    }

    fun stop() {
        isPlayingLoop = false
        currentTrackName = ""
        try {
            playbackThread?.interrupt()
        } catch (_: Exception) {
        }
        playbackThread = null
        try {
            mediaPlayer?.stop()
        } catch (_: Exception) {
        }
        try {
            mediaPlayer?.release()
        } catch (_: Exception) {
        }
        mediaPlayer = null
        try {
            audioTrack?.stop()
        } catch (_: Exception) {
        }
        try {
            audioTrack?.release()
        } catch (_: Exception) {
        }
        audioTrack = null
        abandonAudioFocus()
    }

    private fun applyVolumeInternal(vol: Float) {
        val safe = vol.coerceIn(0f, 1f)
        try {
            mediaPlayer?.setVolume(safe, safe)
        } catch (_: Exception) {
        }
        try {
            audioTrack?.setVolume(safe)
        } catch (_: Exception) {
        }
    }

    private fun requestAudioFocus() {
        val am = audioManager ?: return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val req = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    .setOnAudioFocusChangeListener(focusChangeListener)
                    .build()
                focusRequest = req
                am.requestAudioFocus(req)
            } else {
                @Suppress("DEPRECATION")
                am.requestAudioFocus(
                    focusChangeListener,
                    AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
                )
            }
        } catch (_: Exception) {
        }
    }

    private fun abandonAudioFocus() {
        val am = audioManager ?: return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                focusRequest?.let { am.abandonAudioFocusRequest(it) }
            } else {
                @Suppress("DEPRECATION")
                am.abandonAudioFocus(focusChangeListener)
            }
        } catch (_: Exception) {
        }
    }

    private fun runSynthesizedLoop(trackName: String) {
        val notesHz: DoubleArray = when {
            trackName.contains("Arabian", ignoreCase = true) ->
                doubleArrayOf(293.66, 311.13, 369.99, 392.00, 440.00, 466.16, 554.37, 587.33)
            trackName.contains("Cyberpunk", ignoreCase = true) ->
                doubleArrayOf(220.00, 261.63, 329.63, 392.00, 440.00, 523.25)
            trackName.contains("Lo-Fi", ignoreCase = true) ->
                doubleArrayOf(261.63, 329.63, 392.00, 493.88, 587.33)
            trackName.contains("Bass", ignoreCase = true) ->
                doubleArrayOf(130.81, 164.81, 196.00, 246.94, 261.63)
            else ->
                doubleArrayOf(261.63, 293.66, 329.63, 392.00, 440.00)
        }

        val sampleRate = 16000
        val noteDurationSamples = sampleRate / 2 // 0.5s per note
        val buffer = ShortArray(noteDurationSamples)

        try {
            val minBuf = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            ).coerceAtLeast(noteDurationSamples * 2)

            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(minBuf)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            track.setVolume(currentVolume.coerceIn(0f, 1f))
            track.play()
            audioTrack = track

            var step = 0
            while (isPlayingLoop && !Thread.currentThread().isInterrupted) {
                val freq = notesHz[step % notesHz.size]
                for (i in 0 until noteDurationSamples) {
                    val env = sin(PI * i.toDouble() / noteDurationSamples.toDouble())
                    val sample = (sin(2.0 * PI * i * freq / sampleRate) * 3200 * env * currentVolume).toInt()
                    buffer[i] = sample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                }
                track.write(buffer, 0, buffer.size)
                step++
            }
        } catch (_: Exception) {
        }
    }
}
