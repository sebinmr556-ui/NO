package com.example.ui.theme

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Centralized ThemeManager for NOICE LIVE.
 * Manages switching between Light Mode ("light" / Day Mode) and Dark Mode ("dark"),
 * keeping Compose snapshot state, StateFlows for ThemeViewModel, and SharedPreferences in sync.
 */
object ThemeManager {
    private const val PREFS_NAME = "noise_live_theme_prefs"
    private const val KEY_THEME_MODE = "theme_mode"

    var isLightMode by mutableStateOf(false)
        private set

    private val _themeModeFlow = MutableStateFlow("dark")
    val themeModeFlow: StateFlow<String> = _themeModeFlow.asStateFlow()

    private val _isLightModeFlow = MutableStateFlow(false)
    val isLightModeFlow: StateFlow<Boolean> = _isLightModeFlow.asStateFlow()

    val currentModeKey: String
        get() = if (isLightMode) "light" else "dark"

    fun initFromPrefs(context: Context) {
        runCatching {
            val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val saved = prefs.getString(KEY_THEME_MODE, "dark")?.lowercase() ?: "dark"
            val light = saved == "light"
            isLightMode = light
            _isLightModeFlow.value = light
            _themeModeFlow.value = if (light) "light" else "dark"
        }
    }

    fun applyThemeMode(mode: String, context: Context? = null) {
        val normalized = if (mode.trim().equals("light", ignoreCase = true)) "light" else "dark"
        val light = normalized == "light"
        isLightMode = light
        _isLightModeFlow.value = light
        _themeModeFlow.value = normalized
        if (context != null) {
            runCatching {
                context.applicationContext
                    .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    .edit()
                    .putString(KEY_THEME_MODE, normalized)
                    .apply()
            }
        }
    }

    fun toggleThemeMode(context: Context? = null): String {
        val next = if (isLightMode) "dark" else "light"
        applyThemeMode(next, context)
        return next
    }
}

/**
 * Backwards-compatible delegate to [ThemeManager] so all existing callers continue working seamlessly.
 */
object NoiseThemeController {
    val isLightMode: Boolean
        get() = ThemeManager.isLightMode

    val currentModeKey: String
        get() = ThemeManager.currentModeKey

    fun initFromPrefs(context: Context) = ThemeManager.initFromPrefs(context)

    fun applyThemeMode(mode: String, context: Context? = null) =
        ThemeManager.applyThemeMode(mode, context)

    fun toggleThemeMode(context: Context? = null): String =
        ThemeManager.toggleThemeMode(context)
}

// ============================================================================
// DYNAMIC TWO-MODE SURFACE & TYPOGRAPHY TOKENS (DARK MODE + DAY/LIGHT MODE)
// ============================================================================

/**
 * Main page and main content background:
 * - Light Mode (Day Mode): Pure White (#FFFFFF)
 * - Dark Mode: Original Black (#050507)
 */
val NoiseBlack: Color
    get() {
        val cfg = com.example.data.CentralLiveUpdateRuntime.effectiveConfig
        return if (ThemeManager.isLightMode) {
            com.example.data.CentralLiveUpdateRuntime.parseColorOrFallback(cfg.homeBackgroundLightHex, Color(0xFFFFFFFF))
        } else {
            com.example.data.CentralLiveUpdateRuntime.parseColorOrFallback(cfg.homeBackgroundDarkHex, Color(0xFF050507))
        }
    }

/**
 * Secondary section background:
 * - Light Mode (Day Mode): Soft Lavender-Grey (#F8F8FC)
 * - Dark Mode: Deep Dark Surface (#0D0D12)
 */
val NoiseSurface: Color
    get() {
        val cfg = com.example.data.CentralLiveUpdateRuntime.effectiveConfig
        return if (ThemeManager.isLightMode) {
            com.example.data.CentralLiveUpdateRuntime.parseColorOrFallback(cfg.surfaceBackgroundLightHex, Color(0xFFF8F8FC))
        } else {
            com.example.data.CentralLiveUpdateRuntime.parseColorOrFallback(cfg.surfaceBackgroundDarkHex, Color(0xFF0D0D12))
        }
    }

/**
 * Cards and containers background:
 * - Light Mode (Day Mode): Pure White (#FFFFFF)
 * - Dark Mode: Dark Container (#14141C)
 */
val NoiseCard: Color
    get() {
        val cfg = com.example.data.CentralLiveUpdateRuntime.effectiveConfig
        return if (ThemeManager.isLightMode) {
            com.example.data.CentralLiveUpdateRuntime.parseColorOrFallback(cfg.cardBackgroundLightHex, Color(0xFFFFFFFF))
        } else {
            com.example.data.CentralLiveUpdateRuntime.parseColorOrFallback(cfg.cardBackgroundDarkHex, Color(0xFF14141C))
        }
    }

/**
 * Secondary pills / elevated chip containers:
 * - Light Mode (Day Mode): Secondary Section Tint (#F8F8FC)
 * - Dark Mode: Elevated Dark Container (#1C1C27)
 */
val NoiseCardElevated: Color
    get() = if (ThemeManager.isLightMode) Color(0xFFF8F8FC) else Color(0xFF1C1C27)

/**
 * Light Lavender accent surface:
 * - Light Mode (Day Mode): #F3E5F8
 * - Dark Mode: #1E1430
 */
val LightLavender = Color(0xFFF3E5F8)

val NoiseLightLavender: Color
    get() = if (ThemeManager.isLightMode) Color(0xFFF3E5F8) else Color(0xFF1E1430)

/**
 * Subtle border color:
 * - Light Mode (Day Mode): Clean Light Grey Border (#E0E0EA)
 * - Dark Mode: Dark Border (#262636)
 */
val NoiseBorder: Color
    get() {
        val cfg = com.example.data.CentralLiveUpdateRuntime.effectiveConfig
        return if (ThemeManager.isLightMode) {
            com.example.data.CentralLiveUpdateRuntime.parseColorOrFallback(cfg.borderColorLightHex, Color(0xFFE0E0EA))
        } else {
            com.example.data.CentralLiveUpdateRuntime.parseColorOrFallback(cfg.borderColorDarkHex, Color(0xFF262636))
        }
    }

/**
 * Main headings & primary text color:
 * - Automatically adapts if the Owner sets a light background in Dark Mode (e.g., #FFFFFF)
 *   or uses `textPrimaryDarkHex` / `textPrimaryLightHex` from MagicPicker.
 */
val NoiseHeadingText: Color
    get() {
        val cfg = com.example.data.CentralLiveUpdateRuntime.effectiveConfig
        val bg = NoiseBlack
        val bgLum = 0.2126f * bg.red + 0.7152f * bg.green + 0.0722f * bg.blue
        return if (ThemeManager.isLightMode || bgLum > 0.65f) {
            com.example.data.CentralLiveUpdateRuntime.parseColorOrFallback(cfg.textPrimaryLightHex, Color(0xFF000000))
        } else {
            com.example.data.CentralLiveUpdateRuntime.parseColorOrFallback(cfg.textPrimaryDarkHex, Color(0xFFF9F9FB))
        }
    }

val NoiseWhite: Color
    get() = NoiseHeadingText

/**
 * Normal body text color:
 * - Light Mode (Day Mode): Pure Black (#000000)
 * - Dark Mode: #F2F2F7
 */
val NoiseNormalText: Color
    get() = NoiseHeadingText

/**
 * Secondary text color:
 * - Light Mode (Day Mode): Dark Grey (#4A4A5A)
 * - Dark Mode: #9E9EB0
 */
val NoiseTextSecondary: Color
    get() {
        val bg = NoiseBlack
        val bgLum = 0.2126f * bg.red + 0.7152f * bg.green + 0.0722f * bg.blue
        return if (ThemeManager.isLightMode || bgLum > 0.65f) Color(0xFF4A4A5A) else Color(0xFF9E9EB0)
    }

/**
 * Placeholder / muted text color:
 * - Light Mode (Day Mode): #7A7A8C
 * - Dark Mode: #68687A
 */
val NoisePlaceholderText: Color
    get() = if (ThemeManager.isLightMode) Color(0xFF7A7A8C) else Color(0xFF68687A)

val NoiseTextMuted: Color
    get() = NoisePlaceholderText

/**
 * Disabled text color:
 * - Light Mode (Day Mode): #9E9EAE
 * - Dark Mode: #4E4E5E
 */
val NoiseDisabledText: Color
    get() = if (ThemeManager.isLightMode) Color(0xFF9E9EAE) else Color(0xFF4E4E5E)

/**
 * Links text color:
 * - Light Mode (Day Mode): #C72DDC
 * - Dark Mode: #E52BEB
 */
val NoiseLinkColor: Color
    get() = if (ThemeManager.isLightMode) Color(0xFFC72DDC) else Color(0xFFE52BEB)

/**
 * Automatic contrast helpers:
 * Elements with dark backgrounds automatically use white/light text & icons;
 * elements with light backgrounds automatically use black/dark text & icons.
 */
fun autoContrastTextOn(backgroundColor: Color): Color {
    val lum = 0.2126f * backgroundColor.red + 0.7152f * backgroundColor.green + 0.0722f * backgroundColor.blue
    return if (lum > 0.48f) Color(0xFF000000) else Color(0xFFFFFFFF)
}

fun autoContrastSecondaryTextOn(backgroundColor: Color): Color {
    val lum = 0.2126f * backgroundColor.red + 0.7152f * backgroundColor.green + 0.0722f * backgroundColor.blue
    return if (lum > 0.48f) Color(0xFF4A4A5A) else Color(0xFFB8B8C8)
}

fun autoContrastIconOn(backgroundColor: Color): Color = autoContrastTextOn(backgroundColor)

// ============================================================================
// PRESERVED NOICE LIVE BRAND COLORS & GRADIENTS
// Purple: #8525FF | Pink: #E52BEB | Gold: #FFB900 | Green: #00C985 | Lavender: #F3E5F8
// ============================================================================

val NoiseBrandPurple: Color
    get() = com.example.data.CentralLiveUpdateRuntime.parseColorOrFallback(
        com.example.data.CentralLiveUpdateRuntime.effectiveConfig.primaryAccentHex,
        Color(0xFF8525FF)
    )

val NoiseBrandPink: Color
    get() = com.example.data.CentralLiveUpdateRuntime.parseColorOrFallback(
        com.example.data.CentralLiveUpdateRuntime.effectiveConfig.secondaryAccentHex,
        Color(0xFFE52BEB)
    )

val ElectricViolet: Color
    get() = NoiseBrandPurple
val DeepRoyalBlue = Color(0xFF2563EB)
val CyanHighlight = Color(0xFF00E5FF)
val ControlledMagenta: Color
    get() = NoiseBrandPink

val NoisePink: Color
    get() = NoiseBrandPink
val NoisePurple: Color
    get() = NoiseBrandPurple
val NoiseViolet = DeepRoyalBlue
val NoiseCyan = CyanHighlight

fun noisePurplePinkGradient(): Brush {
    return com.example.data.CentralLiveUpdateRuntime.primaryActionGradientBrush(
        defaultStart = Color(0xFF8525FF),
        defaultEnd = Color(0xFFE52BEB)
    )
}

fun noiseStoryRingGradient(): Brush {
    val cfg = com.example.data.CentralLiveUpdateRuntime.effectiveConfig
    val start = com.example.data.CentralLiveUpdateRuntime.parseColorOrFallback(cfg.storyRingStartHex, Color(0xFF8525FF))
    val end = com.example.data.CentralLiveUpdateRuntime.parseColorOrFallback(cfg.storyRingEndHex, Color(0xFFE52BEB))
    return Brush.linearGradient(colors = listOf(start, end))
}

fun noiseChatroomButtonGradient(): Brush {
    return com.example.data.CentralLiveUpdateRuntime.chatroomActionGradientBrush(
        defaultStart = Color(0xFF8525FF),
        defaultEnd = Color(0xFFE52BEB)
    )
}

fun noiseBrandBrush(): Brush = Brush.horizontalGradient(
    colors = listOf(NoiseBrandPurple, DeepRoyalBlue, CyanHighlight, NoiseBrandPink)
)

fun noisePrimaryActionBrush(): Brush = noisePurplePinkGradient()

// ============================================================================
// VIP & STATUS ACCENTS
// ============================================================================

val VipGold: Color
    get() = com.example.data.CentralLiveUpdateRuntime.parseColorOrFallback(
        com.example.data.CentralLiveUpdateRuntime.effectiveConfig.vipAccentHex,
        Color(0xFFFFB900)
    )
val VipGoldLight = Color(0xFFFFDF70)
val VipGoldDark: Color
    get() = if (ThemeManager.isLightMode) Color(0xFFFFF8DB) else Color(0xFF3A2800)
val VipGoldBorder = Color(0xFF6E4E00)

val OnlineGreen = Color(0xFF00C985)
val VerifiedBlue = Color(0xFF38BDF8)
val DangerRed = Color(0xFFEF4444)
val HotOrange = Color(0xFFF97316)
