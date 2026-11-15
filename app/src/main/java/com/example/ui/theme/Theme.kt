package com.example.ui.theme

import android.app.Activity
import android.content.Context
import android.os.Build
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.StateFlow

/**
 * ViewModel centralizing Light Mode (Day Mode) and Dark Mode state and transitions
 * via [ThemeManager], exposing reactive StateFlows for Compose screens.
 */
class ThemeViewModel : ViewModel() {
    val themeMode: StateFlow<String> = ThemeManager.themeModeFlow
    val isLightMode: StateFlow<Boolean> = ThemeManager.isLightModeFlow

    fun initTheme(context: Context) {
        ThemeManager.initFromPrefs(context)
    }

    fun setThemeMode(mode: String, context: Context? = null) {
        ThemeManager.applyThemeMode(mode, context)
    }

    fun toggleTheme(context: Context? = null): String {
        return ThemeManager.toggleThemeMode(context)
    }
}

private val NoiseLightColorScheme = lightColorScheme(
    primary = NoiseBrandPurple,
    onPrimary = Color.White,
    primaryContainer = LightLavender,
    onPrimaryContainer = Color(0xFF171525),
    secondary = NoiseBrandPink,
    onSecondary = Color.White,
    secondaryContainer = LightLavender,
    onSecondaryContainer = Color(0xFF171525),
    tertiary = VipGold,
    onTertiary = Color(0xFF171525),
    background = Color(0xFFFFFFFF),
    onBackground = Color(0xFF171525),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF252333),
    surfaceVariant = Color(0xFFF8F8FC),
    onSurfaceVariant = Color(0xFF626274),
    outline = Color(0xFFE2E2EC),
    outlineVariant = Color(0xFFEAEAF2)
)

private val NoiseDarkColorScheme = darkColorScheme(
    primary = NoiseBrandPurple,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF1E1430),
    onPrimaryContainer = Color(0xFFF9F9FB),
    secondary = NoiseBrandPink,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF281238),
    onSecondaryContainer = Color(0xFFF9F9FB),
    tertiary = VipGold,
    onTertiary = Color(0xFF050507),
    background = Color(0xFF050507),
    onBackground = Color(0xFFF9F9FB),
    surface = Color(0xFF0D0D12),
    onSurface = Color(0xFFF2F2F7),
    surfaceVariant = Color(0xFF14141C),
    onSurfaceVariant = Color(0xFF9E9EB0),
    outline = Color(0xFF262636),
    outlineVariant = Color(0xFF1E1E2C)
)

/**
 * Merges an optional Material 3 dynamic color scheme with the mandatory NOICE LIVE
 * brand and Day/Night surface & text color specifications.
 */
fun buildNoiseColorScheme(
    context: Context,
    isLight: Boolean,
    dynamicColor: Boolean = true
): ColorScheme {
    val base = if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (isLight) dynamicLightColorScheme(context) else dynamicDarkColorScheme(context)
    } else {
        if (isLight) NoiseLightColorScheme else NoiseDarkColorScheme
    }

    return if (isLight) {
        base.copy(
            primary = NoiseBrandPurple,
            onPrimary = Color.White,
            primaryContainer = LightLavender,
            onPrimaryContainer = Color(0xFF171525),
            secondary = NoiseBrandPink,
            onSecondary = Color.White,
            secondaryContainer = LightLavender,
            onSecondaryContainer = Color(0xFF171525),
            tertiary = VipGold,
            onTertiary = Color(0xFF171525),
            background = Color(0xFFFFFFFF),
            onBackground = Color(0xFF171525),
            surface = Color(0xFFFFFFFF),
            onSurface = Color(0xFF252333),
            surfaceVariant = Color(0xFFF8F8FC),
            onSurfaceVariant = Color(0xFF626274),
            outline = Color(0xFFE2E2EC),
            outlineVariant = Color(0xFFEAEAF2)
        )
    } else {
        base.copy(
            primary = NoiseBrandPurple,
            onPrimary = Color.White,
            primaryContainer = Color(0xFF1E1430),
            onPrimaryContainer = Color(0xFFF9F9FB),
            secondary = NoiseBrandPink,
            onSecondary = Color.White,
            secondaryContainer = Color(0xFF281238),
            onSecondaryContainer = Color(0xFFF9F9FB),
            tertiary = VipGold,
            onTertiary = Color(0xFF050507),
            background = Color(0xFF050507),
            onBackground = Color(0xFFF9F9FB),
            surface = Color(0xFF0D0D12),
            onSurface = Color(0xFFF2F2F7),
            surfaceVariant = Color(0xFF14141C),
            onSurfaceVariant = Color(0xFF9E9EB0),
            outline = Color(0xFF262636),
            outlineVariant = Color(0xFF1E1E2C)
        )
    }
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = !ThemeManager.isLightMode,
    dynamicColor: Boolean = true,
    themeViewModel: ThemeViewModel? = null,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val isLight = !darkTheme
    val colorScheme = buildNoiseColorScheme(
        context = context,
        isLight = isLight,
        dynamicColor = dynamicColor
    )

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = isLight
                controller.isAppearanceLightNavigationBars = isLight
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
