package com.example.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.UserProfile
import com.example.data.VipBadgeCatalogItem
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

data class VipGemPalette(
    val level: Int,
    val badgeTitle: String,
    val gemstoneName: String,
    val gemCore: Color,
    val gemMid: Color,
    val gemDeep: Color,
    val gemHighlight: Color,
    val frameLight: Color,
    val frameMid: Color,
    val frameDark: Color,
    val glowAura: Color,
    val secondaryGem: Color,
    val hasWings: Boolean,
    val wingPairs: Int,
    val hasRoyalCrown: Boolean,
    val isCrownSculptedGem: Boolean,
    val diamondStudCount: Int
)

fun getVipGemPalette(vipLevel: Int): VipGemPalette {
    return when (vipLevel.coerceIn(1, 10)) {
        1 -> VipGemPalette(
            level = 1,
            badgeTitle = "VIP 1 – Blue Sapphire",
            gemstoneName = "Blue Sapphire",
            gemCore = Color(0xFF42A5F5),
            gemMid = Color(0xFF1565C0),
            gemDeep = Color(0xFF0A2463),
            gemHighlight = Color(0xFFB3E5FC),
            frameLight = Color(0xFFF5F7FA),
            frameMid = Color(0xFFB0BEC5),
            frameDark = Color(0xFF546E7A),
            glowAura = Color(0xFF29B6F6),
            secondaryGem = Color(0xFF80D8FF),
            hasWings = false,
            wingPairs = 0,
            hasRoyalCrown = false,
            isCrownSculptedGem = false,
            diamondStudCount = 4
        )
        2 -> VipGemPalette(
            level = 2,
            badgeTitle = "VIP 2 – Emerald",
            gemstoneName = "Deep Green Emerald",
            gemCore = Color(0xFF00E676),
            gemMid = Color(0xFF00A152),
            gemDeep = Color(0xFF004D25),
            gemHighlight = Color(0xFFB9F6CA),
            frameLight = Color(0xFFFFF59D),
            frameMid = Color(0xFFFFB300),
            frameDark = Color(0xFF8D5500),
            glowAura = Color(0xFF00E676),
            secondaryGem = Color(0xFF69F0AE),
            hasWings = false,
            wingPairs = 0,
            hasRoyalCrown = true,
            isCrownSculptedGem = false,
            diamondStudCount = 6
        )
        3 -> VipGemPalette(
            level = 3,
            badgeTitle = "VIP 3 – Ruby",
            gemstoneName = "Deep Red Ruby",
            gemCore = Color(0xFFFF1744),
            gemMid = Color(0xFFC51162),
            gemDeep = Color(0xFF5D001E),
            gemHighlight = Color(0xFFFF8A80),
            frameLight = Color(0xFFFFE082),
            frameMid = Color(0xFFFFB800),
            frameDark = Color(0xFF7A4900),
            glowAura = Color(0xFFFF1744),
            secondaryGem = Color(0xFFFFFFFF),
            hasWings = true,
            wingPairs = 2,
            hasRoyalCrown = true,
            isCrownSculptedGem = false,
            diamondStudCount = 6
        )
        4 -> VipGemPalette(
            level = 4,
            badgeTitle = "VIP 4 – Amethyst",
            gemstoneName = "Purple Amethyst Crystal",
            gemCore = Color(0xFFD500F9),
            gemMid = Color(0xFF7B1FA2),
            gemDeep = Color(0xFF311B92),
            gemHighlight = Color(0xFFF3E5F5),
            frameLight = Color(0xFFECEFF1),
            frameMid = Color(0xFF90A4AE),
            frameDark = Color(0xFF37474F),
            glowAura = Color(0xFFE040FB),
            secondaryGem = Color(0xFF00E5FF),
            hasWings = false,
            wingPairs = 0,
            hasRoyalCrown = true,
            isCrownSculptedGem = true,
            diamondStudCount = 6
        )
        5 -> VipGemPalette(
            level = 5,
            badgeTitle = "VIP 5 – Pink Diamond",
            gemstoneName = "Large Pink Diamond",
            gemCore = Color(0xFFFF2E93),
            gemMid = Color(0xFFC2185B),
            gemDeep = Color(0xFF560027),
            gemHighlight = Color(0xFFFFC1E3),
            frameLight = Color(0xFFFFE4EC),
            frameMid = Color(0xFFF48FB1),
            frameDark = Color(0xFF883955),
            glowAura = Color(0xFFFF4081),
            secondaryGem = Color(0xFFFFFFFF),
            hasWings = true,
            wingPairs = 3,
            hasRoyalCrown = true,
            isCrownSculptedGem = false,
            diamondStudCount = 8
        )
        6 -> VipGemPalette(
            level = 6,
            badgeTitle = "VIP 6 – Black Diamond",
            gemstoneName = "Obsidian Black Diamond",
            gemCore = Color(0xFF546E7A),
            gemMid = Color(0xFF263238),
            gemDeep = Color(0xFF090D12),
            gemHighlight = Color(0xFFECEFF1),
            frameLight = Color(0xFFCFD8DC),
            frameMid = Color(0xFF455A64),
            frameDark = Color(0xFF151C21),
            glowAura = Color(0xFF7C4DFF),
            secondaryGem = Color(0xFF00E5FF),
            hasWings = true,
            wingPairs = 3,
            hasRoyalCrown = true,
            isCrownSculptedGem = true,
            diamondStudCount = 8
        )
        7 -> VipGemPalette(
            level = 7,
            badgeTitle = "VIP 7 – Imperial Topaz",
            gemstoneName = "Golden Imperial Topaz",
            gemCore = Color(0xFFFFEA00),
            gemMid = Color(0xFFFF8F00),
            gemDeep = Color(0xFF6D3600),
            gemHighlight = Color(0xFFFFFDE7),
            frameLight = Color(0xFFFFF59D),
            frameMid = Color(0xFFFFB300),
            frameDark = Color(0xFF7F4700),
            glowAura = Color(0xFFFFD600),
            secondaryGem = Color(0xFF2979FF),
            hasWings = true,
            wingPairs = 4,
            hasRoyalCrown = true,
            isCrownSculptedGem = false,
            diamondStudCount = 8
        )
        8 -> VipGemPalette(
            level = 8,
            badgeTitle = "VIP 8 – Aquamarine",
            gemstoneName = "Ice-Blue Aquamarine",
            gemCore = Color(0xFF00E5FF),
            gemMid = Color(0xFF0091EA),
            gemDeep = Color(0xFF002F6C),
            gemHighlight = Color(0xFFE0F7FA),
            frameLight = Color(0xFFFFFFFF),
            frameMid = Color(0xFFB0BEC5),
            frameDark = Color(0xFF455A64),
            glowAura = Color(0xFF18FFFF),
            secondaryGem = Color(0xFFB388FF),
            hasWings = true,
            wingPairs = 4,
            hasRoyalCrown = true,
            isCrownSculptedGem = false,
            diamondStudCount = 10
        )
        9 -> VipGemPalette(
            level = 9,
            badgeTitle = "VIP 9 – Rainbow Opal",
            gemstoneName = "Iridescent Rainbow Opal",
            gemCore = Color(0xFFFF52D0),
            gemMid = Color(0xFF7C4DFF),
            gemDeep = Color(0xFF00B0FF),
            gemHighlight = Color(0xFFFFFFFF),
            frameLight = Color(0xFFFFF59D),
            frameMid = Color(0xFFFFB800),
            frameDark = Color(0xFF8C5300),
            glowAura = Color(0xFFE040FB),
            secondaryGem = Color(0xFF00E676),
            hasWings = true,
            wingPairs = 5,
            hasRoyalCrown = true,
            isCrownSculptedGem = true,
            diamondStudCount = 10
        )
        else -> VipGemPalette(
            level = 10,
            badgeTitle = "VIP 10 – Eternal Diamond",
            gemstoneName = "Sovereign Eternal Diamond",
            gemCore = Color(0xFFFFFFFF),
            gemMid = Color(0xFF80D8FF),
            gemDeep = Color(0xFF3D5AFE),
            gemHighlight = Color(0xFFFFFFFF),
            frameLight = Color(0xFFFFF9C4),
            frameMid = Color(0xFFFFD700),
            frameDark = Color(0xFF9E6B00),
            glowAura = Color(0xFF00E5FF),
            secondaryGem = Color(0xFFFF2E93),
            hasWings = true,
            wingPairs = 5,
            hasRoyalCrown = true,
            isCrownSculptedGem = false,
            diamondStudCount = 12
        )
    }
}

/**
 * Pure transparent-background 3D Luxury Gemstone Badge renderer for VIP 1 through VIP 10.
 * Features:
 * - Zero solid rectangular or black background (pure alpha channel around the badge)
 * - Sculpted 3D metallic frames (Silver, Gold, Rose Gold, Platinum, Black Platinum)
 * - Winged royal shields & diamond-studded bezels (inspired by uploaded reference badges)
 * - Faceted 3D gemstones with dynamic light reflection, rotating aura rays, shine sweep,
 *   and floating crystal particles.
 */
@Composable
fun VipGemstoneBadgeCanvas(
    vipLevel: Int,
    modifier: Modifier = Modifier,
    animated: Boolean = true
) {
    val palette = remember(vipLevel) { getVipGemPalette(vipLevel) }
    val infiniteTransition = rememberInfiniteTransition(label = "vip_gem_badge_$vipLevel")

    val rotationDeg by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (animated) 9000 else 60000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotationDeg"
    )
    val shineSweep by infiniteTransition.animateFloat(
        initialValue = -1.1f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shineSweep"
    )
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val minDim = minOf(w, h)
        if (minDim <= 0f) return@Canvas
        val cx = w * 0.5f
        val cy = h * 0.52f
        val r = minDim * 0.34f * (if (animated) pulseScale else 1f)

        // 1. Soft transparent radial crystal glow behind the gemstone (no solid box)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    palette.glowAura.copy(alpha = 0.42f),
                    palette.gemCore.copy(alpha = 0.16f),
                    Color.Transparent
                ),
                center = Offset(cx, cy),
                radius = r * 1.45f
            ),
            radius = r * 1.45f,
            center = Offset(cx, cy)
        )

        // 2. Rotating crystal light rays for higher VIP tiers
        rotate(degrees = rotationDeg, pivot = Offset(cx, cy)) {
            val rayCount = 6 + (palette.level / 2) * 2
            for (i in 0 until rayCount) {
                val angle = (i * 360f / rayCount) * (PI.toFloat() / 180f)
                val innerR = r * 0.72f
                val outerR = r * (1.22f + (i % 2) * 0.14f)
                val rayColor = if (palette.level >= 9 && i % 2 == 1) {
                    Color.hsv((i * 360f / rayCount + rotationDeg) % 360f, 0.8f, 1f).copy(alpha = 0.36f)
                } else {
                    palette.glowAura.copy(alpha = 0.30f)
                }
                drawLine(
                    color = rayColor,
                    start = Offset(cx + cos(angle) * innerR, cy + sin(angle) * innerR),
                    end = Offset(cx + cos(angle) * outerR, cy + sin(angle) * outerR),
                    strokeWidth = (minDim * 0.025f).coerceAtLeast(1.2f),
                    cap = StrokeCap.Round
                )
            }
        }

        // 3. Sculpted 3D Metallic Wings (VIP 3, 5, 6, 7, 8, 9, 10)
        if (palette.hasWings && palette.wingPairs > 0) {
            drawRoyalMetallicWings(
                cx = cx,
                cy = cy,
                r = r,
                wingPairs = palette.wingPairs,
                frameLight = palette.frameLight,
                frameMid = palette.frameMid,
                frameDark = palette.frameDark,
                accentGem = palette.secondaryGem
            )
        }

        // 4. Outer 3D Faceted Metallic Bezel / Shield Frame
        drawBeveledCrestBase(
            cx = cx,
            cy = cy,
            r = r,
            vipLevel = palette.level,
            frameLight = palette.frameLight,
            frameMid = palette.frameMid,
            frameDark = palette.frameDark,
            gemDeep = palette.gemDeep,
            secondaryGem = palette.secondaryGem
        )

        // 5. Royal Crown above the Crest (VIP 2, 3, 4, 5, 6, 7, 8, 9, 10)
        if (palette.hasRoyalCrown) {
            drawSovereignCrown(
                cx = cx,
                topY = cy - r * 0.82f,
                crownWidth = r * 1.05f,
                crownHeight = r * 0.56f,
                frameLight = palette.frameLight,
                frameMid = palette.frameMid,
                frameDark = palette.frameDark,
                gemColor = palette.gemCore,
                secondaryGem = palette.secondaryGem,
                shineSweep = shineSweep
            )
        }

        // 6. Central 3D Faceted Gemstone / Crystal Crown Centerpiece
        if (palette.isCrownSculptedGem) {
            // Amethyst / Black Diamond / Rainbow Opal sculpted crystal crown inside bezel (like Reference 1)
            drawSculptedCrystalCrownCenterpiece(
                cx = cx,
                cy = cy + r * 0.04f,
                r = r * 0.72f,
                palette = palette,
                rotationDeg = rotationDeg,
                shineSweep = shineSweep
            )
        } else {
            // Multi-faceted 3D gemstone cut (Sapphire, Emerald, Ruby, Pink Diamond, Topaz Star, Aquamarine, Eternal Diamond)
            drawFaceted3DGemstoneCenterpiece(
                cx = cx,
                cy = cy + r * 0.04f,
                r = r * 0.68f,
                palette = palette,
                rotationDeg = rotationDeg,
                shineSweep = shineSweep
            )
        }

        // 7. Diamond Studs around the frame
        drawSurroundingDiamonds(
            cx = cx,
            cy = cy,
            r = r * 0.96f,
            count = palette.diamondStudCount,
            rotationDeg = rotationDeg,
            secondaryColor = palette.secondaryGem
        )

        // 8. Floating Crystal Particles & 4-Point Starbursts
        drawFloatingCrystalSparks(
            cx = cx,
            cy = cy,
            r = r,
            vipLevel = palette.level,
            rotationDeg = rotationDeg,
            glowColor = palette.gemHighlight,
            accentColor = palette.glowAura
        )
    }
}

private fun DrawScope.drawRoyalMetallicWings(
    cx: Float,
    cy: Float,
    r: Float,
    wingPairs: Int,
    frameLight: Color,
    frameMid: Color,
    frameDark: Color,
    accentGem: Color
) {
    val wingBrush = Brush.linearGradient(
        colors = listOf(frameLight, frameMid, frameDark, frameMid),
        start = Offset(cx - r * 1.45f, cy - r * 0.8f),
        end = Offset(cx + r * 1.45f, cy + r * 0.8f)
    )
    for (side in listOf(-1f, 1f)) {
        for (idx in 0 until wingPairs) {
            val verticalOffset = (idx - (wingPairs - 1) * 0.35f) * (r * 0.24f)
            val span = r * (1.38f - idx * 0.11f)
            val tipY = cy + verticalOffset - r * (0.34f - idx * 0.14f)
            val wingPath = Path().apply {
                moveTo(cx + side * r * 0.55f, cy + verticalOffset * 0.65f)
                lineTo(cx + side * span, tipY)
                lineTo(cx + side * (span * 0.86f), tipY + r * 0.22f)
                lineTo(cx + side * r * 0.50f, cy + verticalOffset * 0.65f + r * 0.18f)
                close()
            }
            drawPath(path = wingPath, brush = wingBrush)
            drawPath(
                path = wingPath,
                color = frameLight.copy(alpha = 0.75f),
                style = Stroke(width = (r * 0.035f).coerceAtLeast(1f), join = StrokeJoin.Round)
            )
        }
        // Lower tail fins with embedded crystal
        val finPath = Path().apply {
            moveTo(cx + side * r * 0.35f, cy + r * 0.70f)
            lineTo(cx + side * r * 0.78f, cy + r * 1.08f)
            lineTo(cx + side * r * 0.28f, cy + r * 0.96f)
            close()
        }
        drawPath(path = finPath, brush = wingBrush)
        drawDiamondSparkle(
            center = Offset(cx + side * r * 0.72f, cy + r * 0.58f),
            size = r * 0.18f,
            coreColor = Color.White,
            tintColor = accentGem
        )
    }
}

private fun DrawScope.drawBeveledCrestBase(
    cx: Float,
    cy: Float,
    r: Float,
    vipLevel: Int,
    frameLight: Color,
    frameMid: Color,
    frameDark: Color,
    gemDeep: Color,
    secondaryGem: Color
) {
    val metallicBrush = Brush.linearGradient(
        colors = listOf(frameLight, frameMid, frameDark, frameLight, frameMid),
        start = Offset(cx - r, cy - r),
        end = Offset(cx + r, cy + r)
    )
    val darkInnerBrush = Brush.radialGradient(
        colors = listOf(gemDeep.copy(alpha = 0.95f), Color(0xFF090814)),
        center = Offset(cx, cy),
        radius = r * 0.85f
    )

    if (vipLevel in listOf(3, 5, 7, 8, 10)) {
        // Hexagonal / Royal Shield Crest (matching Reference 2 & 3)
        val outerShield = createPolygonPath(cx, cy + r * 0.04f, r * 0.96f, sides = 6, rotationRad = (PI / 6.0).toFloat())
        val innerShield = createPolygonPath(cx, cy + r * 0.04f, r * 0.80f, sides = 6, rotationRad = (PI / 6.0).toFloat())
        drawPath(path = outerShield, brush = metallicBrush)
        drawPath(
            path = outerShield,
            color = frameLight.copy(alpha = 0.9f),
            style = Stroke(width = (r * 0.05f).coerceAtLeast(1.2f))
        )
        drawPath(path = innerShield, brush = darkInnerBrush)
        drawPath(
            path = innerShield,
            color = secondaryGem.copy(alpha = 0.7f),
            style = Stroke(width = (r * 0.035f).coerceAtLeast(1f))
        )
    } else {
        // Faceted Circular Royal Medallion (matching Reference 1 & 4)
        drawCircle(
            brush = metallicBrush,
            radius = r * 0.96f,
            center = Offset(cx, cy)
        )
        drawCircle(
            color = frameLight.copy(alpha = 0.88f),
            radius = r * 0.96f,
            center = Offset(cx, cy),
            style = Stroke(width = (r * 0.045f).coerceAtLeast(1.2f))
        )
        drawCircle(
            brush = darkInnerBrush,
            radius = r * 0.80f,
            center = Offset(cx, cy)
        )
        drawCircle(
            color = frameMid.copy(alpha = 0.75f),
            radius = r * 0.80f,
            center = Offset(cx, cy),
            style = Stroke(width = (r * 0.03f).coerceAtLeast(1f))
        )
    }
}

private fun DrawScope.drawSovereignCrown(
    cx: Float,
    topY: Float,
    crownWidth: Float,
    crownHeight: Float,
    frameLight: Color,
    frameMid: Color,
    frameDark: Color,
    gemColor: Color,
    secondaryGem: Color,
    shineSweep: Float
) {
    val halfW = crownWidth * 0.5f
    val baseBottomY = topY + crownHeight * 0.42f
    val peakTopY = topY - crownHeight * 0.45f

    val crownPath = Path().apply {
        moveTo(cx - halfW * 0.78f, baseBottomY)
        lineTo(cx - halfW * 0.95f, topY - crownHeight * 0.05f)
        lineTo(cx - halfW * 0.52f, topY + crownHeight * 0.08f)
        lineTo(cx - halfW * 0.34f, topY - crownHeight * 0.28f)
        lineTo(cx - halfW * 0.14f, topY + crownHeight * 0.04f)
        lineTo(cx, peakTopY)
        lineTo(cx + halfW * 0.14f, topY + crownHeight * 0.04f)
        lineTo(cx + halfW * 0.34f, topY - crownHeight * 0.28f)
        lineTo(cx + halfW * 0.52f, topY + crownHeight * 0.08f)
        lineTo(cx + halfW * 0.95f, topY - crownHeight * 0.05f)
        lineTo(cx + halfW * 0.78f, baseBottomY)
        close()
    }

    drawPath(
        path = crownPath,
        brush = Brush.linearGradient(
            colors = listOf(frameLight, frameMid, frameDark, frameLight),
            start = Offset(cx - halfW, peakTopY),
            end = Offset(cx + halfW, baseBottomY)
        )
    )
    drawPath(
        path = crownPath,
        color = frameLight,
        style = Stroke(width = (crownWidth * 0.03f).coerceAtLeast(1f), join = StrokeJoin.Round)
    )

    // Crown jewels on tips
    val tipOffsets = listOf(
        Offset(cx, peakTopY),
        Offset(cx - halfW * 0.34f, topY - crownHeight * 0.28f),
        Offset(cx + halfW * 0.34f, topY - crownHeight * 0.28f)
    )
    tipOffsets.forEachIndexed { idx, tip ->
        drawCircle(
            color = if (idx == 0) gemColor else secondaryGem,
            radius = (crownWidth * 0.055f).coerceAtLeast(1.5f),
            center = tip
        )
        drawCircle(
            color = Color.White.copy(alpha = 0.85f),
            radius = (crownWidth * 0.025f).coerceAtLeast(0.8f),
            center = Offset(tip.x - crownWidth * 0.015f, tip.y - crownWidth * 0.015f)
        )
    }
    // Animated shine reflection across the crown
    val sweepX = cx + shineSweep * halfW * 0.6f
    drawCircle(
        color = Color.White.copy(alpha = 0.35f),
        radius = (crownWidth * 0.08f).coerceAtLeast(1.5f),
        center = Offset(sweepX.coerceIn(cx - halfW * 0.6f, cx + halfW * 0.6f), topY)
    )
}

private fun DrawScope.drawSculptedCrystalCrownCenterpiece(
    cx: Float,
    cy: Float,
    r: Float,
    palette: VipGemPalette,
    rotationDeg: Float,
    shineSweep: Float
) {
    // 3D Drop Shadow & Caustic Light Transmission Beneath Crown Crystal
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color.Black.copy(alpha = 0.50f),
                palette.glowAura.copy(alpha = 0.30f),
                Color.Transparent
            ),
            center = Offset(cx, cy + r * 0.16f),
            radius = r * 0.96f
        ),
        radius = r * 0.96f,
        center = Offset(cx, cy + r * 0.16f)
    )

    // 5-Peak Multi-Faceted 3D Crystal Crown (VIP 4 Amethyst & VIP 9 Rainbow Opal)
    val baseY = cy + r * 0.50f
    val topCenterY = cy - r * 0.80f
    val isOpal = palette.level >= 9

    val crystalBrush = if (isOpal) {
        Brush.linearGradient(
            colors = listOf(
                Color(0xFFFFFFFF),
                Color(0xFFFF2E93),
                Color(0xFFE040FB),
                Color(0xFF00E5FF),
                Color(0xFF00E676),
                Color(0xFFFFEA00),
                Color(0xFF6A1B9A)
            ),
            start = Offset(cx - r * 0.85f, topCenterY),
            end = Offset(cx + r * 0.85f, baseY)
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                Color.White,
                palette.gemHighlight,
                palette.gemCore,
                palette.gemMid,
                palette.gemDeep
            ),
            start = Offset(cx - r * 0.65f, topCenterY),
            end = Offset(cx + r * 0.65f, baseY)
        )
    }

    val crownBody = Path().apply {
        moveTo(cx - r * 0.66f, baseY * 0.96f)
        lineTo(cx - r * 0.84f, cy - r * 0.18f)
        lineTo(cx - r * 0.48f, cy + r * 0.04f)
        lineTo(cx - r * 0.58f, cy - r * 0.54f)
        lineTo(cx - r * 0.25f, cy - r * 0.05f)
        lineTo(cx, topCenterY)
        lineTo(cx + r * 0.25f, cy - r * 0.05f)
        lineTo(cx + r * 0.58f, cy - r * 0.54f)
        lineTo(cx + r * 0.48f, cy + r * 0.04f)
        lineTo(cx + r * 0.84f, cy - r * 0.18f)
        lineTo(cx + r * 0.66f, baseY * 0.96f)
        close()
    }
    drawPath(path = crownBody, brush = crystalBrush)

    // Left-lit & Right-shaded 3D Crystal Spire Facets for true depth
    val centerLeftSpire = Path().apply {
        moveTo(cx, topCenterY)
        lineTo(cx - r * 0.25f, cy - r * 0.05f)
        lineTo(cx, baseY * 0.94f)
        close()
    }
    drawPath(
        path = centerLeftSpire,
        brush = Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.65f),
                palette.gemHighlight.copy(alpha = 0.42f),
                palette.gemCore.copy(alpha = 0.15f)
            ),
            start = Offset(cx - r * 0.25f, topCenterY),
            end = Offset(cx, baseY)
        )
    )

    val centerRightSpire = Path().apply {
        moveTo(cx, topCenterY)
        lineTo(cx + r * 0.25f, cy - r * 0.05f)
        lineTo(cx, baseY * 0.94f)
        close()
    }
    drawPath(
        path = centerRightSpire,
        brush = Brush.linearGradient(
            colors = listOf(
                palette.gemMid.copy(alpha = 0.45f),
                palette.gemDeep.copy(alpha = 0.72f)
            ),
            start = Offset(cx, topCenterY),
            end = Offset(cx + r * 0.25f, baseY)
        )
    )

    // Internal Pavilion / Culet V-Facets inside the crystal crown
    val innerTable = Path().apply {
        moveTo(cx, topCenterY + r * 0.24f)
        lineTo(cx + r * 0.20f, cy + r * 0.14f)
        lineTo(cx, baseY * 0.88f)
        lineTo(cx - r * 0.20f, cy + r * 0.14f)
        close()
    }
    drawPath(
        path = innerTable,
        brush = Brush.radialGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.85f),
                palette.gemHighlight.copy(alpha = 0.55f),
                Color.Transparent
            ),
            center = Offset(cx - r * 0.04f, cy + r * 0.02f),
            radius = r * 0.42f
        )
    )

    // Crisp 3D Facet Ridge Lines
    val facetStroke = Stroke(width = (r * 0.032f).coerceAtLeast(1f), cap = StrokeCap.Round, join = StrokeJoin.Round)
    drawLine(
        color = Color.White.copy(alpha = 0.90f),
        start = Offset(cx, topCenterY),
        end = Offset(cx, baseY * 0.95f),
        strokeWidth = facetStroke.width
    )
    drawLine(
        color = palette.gemHighlight.copy(alpha = 0.80f),
        start = Offset(cx - r * 0.58f, cy - r * 0.54f),
        end = Offset(cx - r * 0.32f, baseY * 0.95f),
        strokeWidth = facetStroke.width
    )
    drawLine(
        color = palette.gemHighlight.copy(alpha = 0.75f),
        start = Offset(cx + r * 0.58f, cy - r * 0.54f),
        end = Offset(cx + r * 0.32f, baseY * 0.95f),
        strokeWidth = facetStroke.width
    )
    drawLine(
        color = Color.White.copy(alpha = 0.65f),
        start = Offset(cx - r * 0.48f, cy + r * 0.04f),
        end = Offset(cx + r * 0.48f, cy + r * 0.04f),
        strokeWidth = facetStroke.width * 0.8f
    )
    drawPath(
        path = crownBody,
        color = Color.White.copy(alpha = 0.92f),
        style = facetStroke
    )

    // Multi-point diamond starburst finials on top of crystal peaks
    drawDiamondSparkle(
        center = Offset(cx, topCenterY - r * 0.05f),
        size = r * 0.25f,
        coreColor = Color.White,
        tintColor = palette.gemHighlight
    )
    drawDiamondSparkle(
        center = Offset(cx - r * 0.58f, cy - r * 0.54f),
        size = r * 0.16f,
        coreColor = Color.White,
        tintColor = palette.glowAura
    )
    drawDiamondSparkle(
        center = Offset(cx + r * 0.58f, cy - r * 0.54f),
        size = r * 0.16f,
        coreColor = Color.White,
        tintColor = palette.glowAura
    )

    // Animated diagonal specular sweep across crystal facets
    val sweepX = cx + shineSweep * r * 0.58f
    if (sweepX in (cx - r * 0.68f)..(cx + r * 0.68f)) {
        drawLine(
            color = Color.White.copy(alpha = 0.82f),
            start = Offset(sweepX - r * 0.14f, cy - r * 0.48f),
            end = Offset(sweepX + r * 0.14f, cy + r * 0.40f),
            strokeWidth = (r * 0.10f).coerceAtLeast(1.5f),
            cap = StrokeCap.Round
        )
    }
}

/**
 * Multi-Faceted 3D Ray-Refracted Gemstone Centerpiece Engine:
 * Renders realistic 3D precious gemstones with:
 * - 3D Drop Shadow & Caustic Internal Light Transmission Glow
 * - 3D Girdle Bevel Rim
 * - Deep Pavilion Culet Facets (visible through the transparent crystal)
 * - Star Facets & Upper Girdle Kite Facets (individually shaded by 3D facet normal vs. light angle)
 * - Chromatic Dispersion ("Diamond Fire" rainbow flashes)
 * - Raised Crystal Table with Fresnel Glass Specular Reflection & Starbursts
 */
private fun DrawScope.drawFaceted3DGemstoneCenterpiece(
    cx: Float,
    cy: Float,
    r: Float,
    palette: VipGemPalette,
    rotationDeg: Float,
    shineSweep: Float
) {
    // 1. 3D Drop Shadow + Caustic Gemstone Transmission Glow underneath
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color.Black.copy(alpha = 0.55f),
                palette.gemDeep.copy(alpha = 0.42f),
                palette.glowAura.copy(alpha = 0.25f),
                Color.Transparent
            ),
            center = Offset(cx, cy + r * 0.14f),
            radius = r * 0.98f
        ),
        radius = r * 0.98f,
        center = Offset(cx, cy + r * 0.14f)
    )

    // Special Case: VIP 7 Imperial Topaz 3D Faceted 5-Point Star Gemstone
    if (palette.level == 7) {
        val outerR = r * 0.88f
        val valleyR = r * 0.42f
        val starPath = createStarPath(cx, cy, outerRadius = outerR, innerRadius = valleyR, points = 5)
        drawPath(
            path = starPath,
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.White,
                    palette.gemHighlight,
                    palette.gemCore,
                    palette.gemMid,
                    palette.gemDeep
                ),
                center = Offset(cx - r * 0.16f, cy - r * 0.16f),
                radius = outerR * 1.1f
            )
        )
        // Draw 10 individual 3D triangular star facets (alternating lit & shaded faces)
        for (i in 0 until 5) {
            val tipAngle = (-90f + i * 72f) * (PI.toFloat() / 180f)
            val leftValleyAngle = (-90f + i * 72f - 36f) * (PI.toFloat() / 180f)
            val rightValleyAngle = (-90f + i * 72f + 36f) * (PI.toFloat() / 180f)

            val tip = Offset(cx + cos(tipAngle) * outerR, cy + sin(tipAngle) * outerR)
            val leftV = Offset(cx + cos(leftValleyAngle) * valleyR, cy + sin(leftValleyAngle) * valleyR)
            val rightV = Offset(cx + cos(rightValleyAngle) * valleyR, cy + sin(rightValleyAngle) * valleyR)

            val leftFacet = Path().apply {
                moveTo(cx, cy)
                lineTo(tip.x, tip.y)
                lineTo(leftV.x, leftV.y)
                close()
            }
            drawPath(
                path = leftFacet,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.65f),
                        palette.gemHighlight.copy(alpha = 0.45f)
                    ),
                    start = Offset(cx, cy),
                    end = tip
                )
            )

            val rightFacet = Path().apply {
                moveTo(cx, cy)
                lineTo(tip.x, tip.y)
                lineTo(rightV.x, rightV.y)
                close()
            }
            drawPath(
                path = rightFacet,
                brush = Brush.linearGradient(
                    colors = listOf(
                        palette.gemMid.copy(alpha = 0.55f),
                        palette.gemDeep.copy(alpha = 0.78f)
                    ),
                    start = Offset(cx, cy),
                    end = rightV
                )
            )
        }
        // Central Pentagon Table on the Star Gemstone
        val innerPentagon = createPolygonPath(cx, cy, r * 0.28f, sides = 5, rotationRad = (-PI / 2.0).toFloat())
        drawPath(
            path = innerPentagon,
            brush = Brush.radialGradient(
                colors = listOf(Color.White.copy(alpha = 0.92f), palette.gemHighlight.copy(alpha = 0.75f)),
                center = Offset(cx - r * 0.06f, cy - r * 0.06f),
                radius = r * 0.32f
            )
        )
        drawPath(
            path = starPath,
            color = Color.White.copy(alpha = 0.92f),
            style = Stroke(width = (r * 0.042f).coerceAtLeast(1.1f), join = StrokeJoin.Round)
        )
        drawPath(
            path = innerPentagon,
            color = Color.White.copy(alpha = 0.85f),
            style = Stroke(width = (r * 0.028f).coerceAtLeast(0.9f), join = StrokeJoin.Round)
        )
        drawDiamondSparkle(
            center = Offset(cx - r * 0.12f, cy - r * 0.14f),
            size = r * 0.34f,
            coreColor = Color.White,
            tintColor = palette.gemHighlight
        )
        return
    }

    // Determine gemstone cut geometry per VIP level
    val sides = when (palette.level) {
        1 -> 8  // VIP 1: Royal Cushion-Octagon Blue Sapphire
        2 -> 8  // VIP 2: Step-Cut Asscher Emerald
        3 -> 6  // VIP 3: Sovereign Hex-Trilliant Pigeon-Blood Ruby
        5 -> 10 // VIP 5: 57-Facet Radiant Pink Diamond
        6 -> 8  // VIP 6: Asscher-Cut Obsidian Black Diamond
        8 -> 8  // VIP 8: Lozenge-Cut Glacial Aquamarine
        10 -> 12 // VIP 10: 58-Facet Sovereign Eternal Diamond
        else -> 8
    }

    val girdleR = r * 0.90f
    val crownMidR = r * 0.68f
    val tableR = when (palette.level) {
        2, 6 -> r * 0.48f // Deeper step-cut table for Emerald & Black Diamond
        10 -> r * 0.46f   // High-dispersion Eternal Diamond table
        else -> r * 0.50f
    }
    val rotOffset = (PI / sides).toFloat()

    // 2. 3D Girdle Rim (outer bevel giving physical thickness to the cut stone)
    val girdleOuterPath = createPolygonPath(cx, cy + r * 0.03f, girdleR * 1.03f, sides = sides, rotationRad = rotOffset)
    drawPath(
        path = girdleOuterPath,
        brush = Brush.linearGradient(
            colors = listOf(palette.frameLight, palette.gemDeep, palette.frameDark),
            start = Offset(cx - girdleR, cy - girdleR),
            end = Offset(cx + girdleR, cy + girdleR)
        )
    )

    val outerGemPath = createPolygonPath(cx, cy, girdleR, sides = sides, rotationRad = rotOffset)

    // 3. Base Gemstone Body Fill with Internal Caustic Depth
    val baseBodyBrush = when (palette.level) {
        10 -> Brush.sweepGradient(
            colors = listOf(
                Color.White,
                Color(0xFF80D8FF),
                Color(0xFFEA80FC),
                Color.White,
                Color(0xFFFFF59D),
                Color(0xFF84FFFF),
                Color(0xFFFF80AB),
                Color.White
            ),
            center = Offset(cx, cy)
        )
        6 -> Brush.radialGradient(
            colors = listOf(
                Color(0xFF90A4AE),
                Color(0xFF37474F),
                Color(0xFF151C24),
                Color(0xFF05070B)
            ),
            center = Offset(cx - r * 0.22f, cy - r * 0.22f),
            radius = girdleR * 1.25f
        )
        else -> Brush.radialGradient(
            colors = listOf(
                palette.gemHighlight,
                palette.gemCore,
                palette.gemMid,
                palette.gemDeep
            ),
            center = Offset(cx - r * 0.20f, cy - r * 0.20f),
            radius = girdleR * 1.2f
        )
    }
    drawPath(path = outerGemPath, brush = baseBodyBrush)

    // 4. Deep Internal Pavilion Culet Facets (visible through the transparent crystal)
    val culetX = cx + shineSweep * r * 0.05f
    val culetY = cy + r * 0.05f
    for (i in 0 until sides) {
        val a1 = rotOffset + (i * 2.0 * PI / sides).toFloat()
        val a2 = rotOffset + ((i + 1) * 2.0 * PI / sides).toFloat()
        val midA = (a1 + a2) * 0.5f

        val p1 = Offset(cx + cos(a1) * tableR, cy + sin(a1) * tableR)
        val p2 = Offset(cx + cos(a2) * tableR, cy + sin(a2) * tableR)

        val pavilionTri = Path().apply {
            moveTo(culetX, culetY)
            lineTo(p1.x, p1.y)
            lineTo(p2.x, p2.y)
            close()
        }

        // Dynamic light angle per internal pavilion facet
        val lightDot = cos(midA + (rotationDeg * 0.02f)).toFloat()
        val pavColor = when {
            palette.level == 10 && i % 3 == 0 -> Color(0xFF00E5FF).copy(alpha = 0.42f)
            palette.level == 10 && i % 3 == 1 -> Color(0xFFFF4081).copy(alpha = 0.36f)
            lightDot > 0.25f -> Color.White.copy(alpha = 0.46f)
            lightDot > -0.25f -> palette.gemCore.copy(alpha = 0.45f)
            else -> palette.gemDeep.copy(alpha = 0.65f)
        }
        drawPath(path = pavilionTri, color = pavColor)
    }

    // 5. Crown Upper Girdle & Bezel Kite Facets (individually shaded around the perimeter)
    val lightDirRad = (-55f + shineSweep * 35f) * (PI.toFloat() / 180f)
    for (i in 0 until sides) {
        val a1 = rotOffset + (i * 2.0 * PI / sides).toFloat()
        val a2 = rotOffset + ((i + 1) * 2.0 * PI / sides).toFloat()
        val aMid = (a1 + a2) * 0.5f

        val g1 = Offset(cx + cos(a1) * girdleR, cy + sin(a1) * girdleR)
        val g2 = Offset(cx + cos(a2) * girdleR, cy + sin(a2) * girdleR)
        val gMid = Offset(cx + cos(aMid) * girdleR, cy + sin(aMid) * girdleR)

        val t1 = Offset(cx + cos(a1) * tableR, cy + sin(a1) * tableR)
        val t2 = Offset(cx + cos(a2) * tableR, cy + sin(a2) * tableR)
        val starPeak = Offset(cx + cos(aMid) * crownMidR, cy + sin(aMid) * crownMidR)

        // Compute how directly this crown facet faces the upper-left studio key light
        val alignment = cos(aMid - lightDirRad)

        // Star Facet (adjacent to the central table)
        val starFacetPath = Path().apply {
            moveTo(t1.x, t1.y)
            lineTo(starPeak.x, starPeak.y)
            lineTo(t2.x, t2.y)
            close()
        }
        val starFacetColor = when {
            alignment > 0.55f -> Color.White.copy(alpha = 0.82f)
            alignment > 0.0f -> palette.gemHighlight.copy(alpha = 0.62f)
            alignment > -0.5f -> palette.gemCore.copy(alpha = 0.50f)
            else -> palette.gemDeep.copy(alpha = 0.72f)
        }
        drawPath(path = starFacetPath, color = starFacetColor)

        // Left & Right Upper Girdle Facets
        val leftGirdleFacet = Path().apply {
            moveTo(t1.x, t1.y)
            lineTo(g1.x, g1.y)
            lineTo(gMid.x, gMid.y)
            lineTo(starPeak.x, starPeak.y)
            close()
        }
        val rightGirdleFacet = Path().apply {
            moveTo(t2.x, t2.y)
            lineTo(starPeak.x, starPeak.y)
            lineTo(gMid.x, gMid.y)
            lineTo(g2.x, g2.y)
            close()
        }

        val leftLit = cos(a1 - lightDirRad)
        val rightLit = cos(a2 - lightDirRad)

        // Prismatic "Diamond Fire" chromatic dispersion on high-tier diamonds
        val fireTintLeft = if ((palette.level == 10 || palette.level == 5 || palette.level == 8) && i % 4 == 0) {
            Color(0xFF00E5FF).copy(alpha = 0.48f)
        } else if (leftLit > 0.3f) {
            palette.gemHighlight.copy(alpha = 0.70f)
        } else {
            palette.gemMid.copy(alpha = 0.58f)
        }

        val fireTintRight = if ((palette.level == 10 || palette.level == 5 || palette.level == 6) && i % 4 == 2) {
            Color(0xFFFF4081).copy(alpha = 0.42f)
        } else if (rightLit > 0.2f) {
            Color.White.copy(alpha = 0.52f)
        } else {
            palette.gemDeep.copy(alpha = 0.78f)
        }

        drawPath(path = leftGirdleFacet, color = fireTintLeft)
        drawPath(path = rightGirdleFacet, color = fireTintRight)

        // Crisp 3D Facet Ridges
        val ridgeWidth = (r * 0.022f).coerceAtLeast(0.75f)
        drawLine(
            color = Color.White.copy(alpha = if (alignment > 0f) 0.85f else 0.38f),
            start = t1,
            end = g1,
            strokeWidth = ridgeWidth
        )
        drawLine(
            color = Color.White.copy(alpha = if (alignment > 0f) 0.72f else 0.30f),
            start = t1,
            end = starPeak,
            strokeWidth = ridgeWidth * 0.85f
        )
        drawLine(
            color = Color.White.copy(alpha = if (alignment > 0f) 0.72f else 0.30f),
            start = t2,
            end = starPeak,
            strokeWidth = ridgeWidth * 0.85f
        )
        drawLine(
            color = Color.White.copy(alpha = 0.50f),
            start = starPeak,
            end = gMid,
            strokeWidth = ridgeWidth * 0.8f
        )
    }

    // Step-Cut Concentric Asscher Emerald / Black Diamond Frame Lines (VIP 2 & VIP 6)
    if (palette.level == 2 || palette.level == 6) {
        val stepOuter = createPolygonPath(cx, cy, girdleR * 0.82f, sides = sides, rotationRad = rotOffset)
        val stepInner = createPolygonPath(cx, cy, girdleR * 0.66f, sides = sides, rotationRad = rotOffset)
        drawPath(
            path = stepOuter,
            color = Color.White.copy(alpha = 0.62f),
            style = Stroke(width = (r * 0.024f).coerceAtLeast(0.8f), join = StrokeJoin.Miter)
        )
        drawPath(
            path = stepInner,
            color = palette.gemHighlight.copy(alpha = 0.75f),
            style = Stroke(width = (r * 0.022f).coerceAtLeast(0.8f), join = StrokeJoin.Miter)
        )
    }

    // 6. Raised Top Crystal Table with Fresnel Glass Specular Reflection
    val tablePath = createPolygonPath(cx, cy, tableR, sides = sides, rotationRad = rotOffset)
    drawPath(
        path = tablePath,
        brush = Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.78f),
                palette.gemHighlight.copy(alpha = 0.45f),
                palette.gemCore.copy(alpha = 0.25f),
                palette.gemDeep.copy(alpha = 0.40f)
            ),
            start = Offset(cx - tableR, cy - tableR),
            end = Offset(cx + tableR, cy + tableR)
        )
    )

    // Upper-left Fresnel glass reflection polygon on the table surface
    val glossPolygon = createPolygonPath(
        cx = cx - tableR * 0.18f,
        cy = cy - tableR * 0.18f,
        radius = tableR * 0.52f,
        sides = sides,
        rotationRad = rotOffset
    )
    drawPath(
        path = glossPolygon,
        brush = Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.82f),
                Color.White.copy(alpha = 0.18f),
                Color.Transparent
            ),
            start = Offset(cx - tableR * 0.65f, cy - tableR * 0.65f),
            end = Offset(cx + tableR * 0.15f, cy + tableR * 0.15f)
        )
    )

    // Precision White-Gold / Crystal Rim Outlines
    drawPath(
        path = outerGemPath,
        color = Color.White.copy(alpha = 0.94f),
        style = Stroke(width = (r * 0.038f).coerceAtLeast(1.0f), join = StrokeJoin.Round)
    )
    drawPath(
        path = tablePath,
        color = Color.White.copy(alpha = 0.88f),
        style = Stroke(width = (r * 0.030f).coerceAtLeast(0.9f), join = StrokeJoin.Round)
    )

    // 7. Animated Diagonal Specular Sweep + Primary Starburst Glint on Top Facet Edge
    val sweepX = cx + shineSweep * r * 0.68f
    if (sweepX in (cx - r * 0.72f)..(cx + r * 0.72f)) {
        drawLine(
            color = Color.White.copy(alpha = 0.86f),
            start = Offset(sweepX - r * 0.18f, cy - r * 0.44f),
            end = Offset(sweepX + r * 0.18f, cy + r * 0.44f),
            strokeWidth = (r * 0.11f).coerceAtLeast(1.5f),
            cap = StrokeCap.Round
        )
    }

    // Primary 3D Facet Edge Starburst Glint
    drawDiamondSparkle(
        center = Offset(cx - tableR * 0.58f, cy - tableR * 0.58f),
        size = (r * 0.34f).coerceAtLeast(3.5f),
        coreColor = Color.White,
        tintColor = palette.gemHighlight
    )
    if (palette.level >= 5) {
        drawDiamondSparkle(
            center = Offset(cx + tableR * 0.62f, cy + tableR * 0.42f),
            size = (r * 0.24f).coerceAtLeast(2.5f),
            coreColor = Color.White,
            tintColor = palette.glowAura
        )
    }
}

/**
 * Standalone Reusable 3D Multi-Faceted Diamond Icon Composable.
 * Replaces flat 2D diamond vector icons across headers, VIP pills, profile cards, and modals.
 */
@Composable
fun Realistic3DDiamondIcon(
    vipLevel: Int = 10,
    modifier: Modifier = Modifier,
    size: Dp = 18.dp,
    animated: Boolean = true
) {
    val safeLevel = vipLevel.coerceIn(1, 10)
    val palette = remember(safeLevel) { getVipGemPalette(safeLevel) }
    val transition = rememberInfiniteTransition(label = "realistic_3d_diamond_$safeLevel")
    val shineSweep by transition.animateFloat(
        initialValue = -1.2f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "diamond_shine"
    )
    val rotDeg by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 9000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "diamond_rot"
    )

    Canvas(modifier = modifier.size(size)) {
        val cx = this.size.width * 0.5f
        val cy = this.size.height * 0.5f
        val radius = min(this.size.width, this.size.height) * 0.48f
        drawFaceted3DGemstoneCenterpiece(
            cx = cx,
            cy = cy,
            r = radius,
            palette = palette,
            rotationDeg = if (animated) rotDeg else 25f,
            shineSweep = if (animated) shineSweep else 0f
        )
    }
}

private fun DrawScope.drawSurroundingDiamonds(
    cx: Float,
    cy: Float,
    r: Float,
    count: Int,
    rotationDeg: Float,
    secondaryColor: Color
) {
    for (i in 0 until count) {
        val angleDeg = (i * 360f / count) + 45f
        val rad = angleDeg * (PI.toFloat() / 180f)
        val dx = cx + cos(rad) * r
        val dy = cy + sin(rad) * r
        val pulse = 0.75f + 0.25f * sin((rotationDeg * 2f + i * 45f) * (PI.toFloat() / 180f))
        drawDiamondSparkle(
            center = Offset(dx, dy),
            size = (r * 0.16f * pulse).coerceAtLeast(2f),
            coreColor = Color.White,
            tintColor = secondaryColor
        )
    }
}

private fun DrawScope.drawFloatingCrystalSparks(
    cx: Float,
    cy: Float,
    r: Float,
    vipLevel: Int,
    rotationDeg: Float,
    glowColor: Color,
    accentColor: Color
) {
    val sparkCount = 4 + vipLevel
    for (i in 0 until sparkCount) {
        val baseAngle = (i * 360f / sparkCount) + rotationDeg * 0.6f
        val rad = baseAngle * (PI.toFloat() / 180f)
        val dist = r * (1.12f + 0.18f * sin((rotationDeg * 1.5f + i * 60f) * (PI.toFloat() / 180f)))
        val sx = cx + cos(rad) * dist
        val sy = cy + sin(rad) * dist
        val sparkRadius = (r * 0.055f).coerceAtLeast(1.2f)
        drawCircle(
            color = if (i % 2 == 0) Color.White.copy(alpha = 0.9f) else glowColor.copy(alpha = 0.85f),
            radius = sparkRadius,
            center = Offset(sx, sy)
        )
        if (i % 3 == 0) {
            drawLine(
                color = Color.White.copy(alpha = 0.85f),
                start = Offset(sx - sparkRadius * 2.4f, sy),
                end = Offset(sx + sparkRadius * 2.4f, sy),
                strokeWidth = (sparkRadius * 0.7f).coerceAtLeast(0.8f),
                cap = StrokeCap.Round
            )
            drawLine(
                color = accentColor.copy(alpha = 0.85f),
                start = Offset(sx, sy - sparkRadius * 2.4f),
                end = Offset(sx, sy + sparkRadius * 2.4f),
                strokeWidth = (sparkRadius * 0.7f).coerceAtLeast(0.8f),
                cap = StrokeCap.Round
            )
        }
    }
}

private fun DrawScope.drawDiamondSparkle(
    center: Offset,
    size: Float,
    coreColor: Color,
    tintColor: Color
) {
    val half = size * 0.5f
    val path = Path().apply {
        moveTo(center.x, center.y - half)
        lineTo(center.x + half * 0.28f, center.y - half * 0.28f)
        lineTo(center.x + half, center.y)
        lineTo(center.x + half * 0.28f, center.y + half * 0.28f)
        lineTo(center.x, center.y + half)
        lineTo(center.x - half * 0.28f, center.y + half * 0.28f)
        lineTo(center.x - half, center.y)
        lineTo(center.x - half * 0.28f, center.y - half * 0.28f)
        close()
    }
    drawPath(
        path = path,
        brush = Brush.radialGradient(
            colors = listOf(coreColor, tintColor, Color.Transparent),
            center = center,
            radius = half * 1.1f
        )
    )
    drawCircle(
        color = Color.White,
        radius = (size * 0.14f).coerceAtLeast(0.8f),
        center = center
    )
}

private fun createPolygonPath(
    cx: Float,
    cy: Float,
    radius: Float,
    sides: Int,
    rotationRad: Float = 0f
): Path {
    val path = Path()
    for (i in 0 until sides) {
        val angle = rotationRad + (i * 2.0 * PI / sides).toFloat()
        val x = cx + cos(angle) * radius
        val y = cy + sin(angle) * radius
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    return path
}

private fun createStarPath(
    cx: Float,
    cy: Float,
    outerRadius: Float,
    innerRadius: Float,
    points: Int
): Path {
    val path = Path()
    val totalVertices = points * 2
    for (i in 0 until totalVertices) {
        val r = if (i % 2 == 0) outerRadius else innerRadius
        val angle = (-PI / 2.0 + i * PI / points).toFloat()
        val x = cx + cos(angle) * r
        val y = cy + sin(angle) * r
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    return path
}

/**
 * Compact inline VIP 1–10 Luxury Gemstone Badge rendered next to usernames across the app:
 * - User Profile
 * - Home Feed Posts
 * - Chatrooms (Host, Seats, Chat messages, Participants)
 * - Direct Messages
 * - Search / Online Users List
 * - Followers / Following Lists
 */
@Composable
fun VipInlineGemBadge(
    vipLevel: Int,
    modifier: Modifier = Modifier,
    gemSize: Dp = 22.dp,
    showLevelLabel: Boolean = true,
    showLabel: Boolean = showLevelLabel,
    onClick: (() -> Unit)? = null
) {
    val safeLevel = vipLevel.coerceIn(0, 10)
    if (safeLevel <= 0) return
    val palette = remember(safeLevel) { getVipGemPalette(safeLevel) }

    Row(
        modifier = modifier
            .then(
                if (onClick != null) {
                    Modifier.clickable { onClick() }
                } else {
                    Modifier
                }
            )
            .testTag("vip_inline_badge_$safeLevel"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        VipGemstoneBadgeCanvas(
            vipLevel = safeLevel,
            modifier = Modifier.size(gemSize),
            animated = true
        )
        if (showLabel) {
            Text(
                text = "VIP $safeLevel",
                color = Color.White,
                fontSize = 9.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                palette.gemDeep.copy(alpha = 0.92f),
                                palette.gemMid.copy(alpha = 0.82f)
                            )
                        )
                    )
                    .border(
                        width = 0.8.dp,
                        color = palette.glowAura.copy(alpha = 0.75f),
                        shape = RoundedCornerShape(50)
                    )
                    .padding(horizontal = 5.dp, vertical = 1.dp)
            )
        }
    }
}

/**
 * Complete 2-Column VIP 1 to VIP 10 Luxury Gemstone Badge Shop with:
 * - Active Badge Header Banner
 * - 2-Column grid of all 10 animated 3D Gemstone Badges
 * - Tap any badge to open a Large 3D Animated Preview Modal
 * - Buy / Equip button backed by real Firestore coin verification
 */
@Composable
fun VipLuxuryBadgeShopSection(
    catalog: List<VipBadgeCatalogItem>,
    userCoins: Long,
    ownedVipBadgeLevels: Set<Int>,
    activeVipBadgeLevel: Int,
    onPurchaseBadge: (Int) -> Unit,
    onEquipBadge: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val syntheticProfile = remember(userCoins, ownedVipBadgeLevels, activeVipBadgeLevel, catalog) {
        val activeTitle = catalog.firstOrNull { it.vipLevel == activeVipBadgeLevel }?.title ?: "VIP $activeVipBadgeLevel"
        UserProfile(
            coins = userCoins,
            vip = true,
            activeVipBadgeLevel = activeVipBadgeLevel.toLong(),
            activeVipBadgeName = activeTitle,
            ownedVipBadges = ownedVipBadgeLevels.map { it.toLong() }.sorted()
        )
    }
    VipLuxuryBadgeShopSection(
        currentUserProfile = syntheticProfile,
        badgeCatalog = catalog,
        onPurchaseBadge = onPurchaseBadge,
        onSelectActiveBadge = onEquipBadge,
        modifier = modifier
    )
}

@Composable
fun VipLuxuryBadgeShopSection(
    currentUserProfile: UserProfile?,
    badgeCatalog: List<VipBadgeCatalogItem>,
    onPurchaseBadge: (Int) -> Unit,
    onSelectActiveBadge: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var previewBadgeItem by remember { mutableStateOf<VipBadgeCatalogItem?>(null) }
    val ownedLevels = currentUserProfile?.ownedVipBadges ?: emptyList()
    val activeLevel = (currentUserProfile?.activeVipBadgeLevel ?: 0L).toInt()
    val userCoins = currentUserProfile?.coins ?: 0L
    val isLight = com.example.ui.theme.ThemeManager.isLightMode

    val headerCardBg = if (isLight) Color(0xFFFFFFFF) else Color(0xFF140F26)
    val badgeCardBg = if (isLight) Color(0xFFFFFFFF) else Color(0xFF110D20)
    val primaryTitleColor = if (isLight) Color(0xFF000000) else Color.White
    val secondarySubtitleColor = if (isLight) Color(0xFF4A4A5A) else Color(0xFFB8B2D1)
    val goldAccentColor = if (isLight) Color(0xFFB47800) else Color(0xFFFFD700)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("vip_luxury_badge_shop_section"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Luxury Header Card with Active VIP Badge Preview
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = headerCardBg,
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    1.2.dp,
                    Brush.horizontalGradient(
                        listOf(
                            Color(0xFFFFB900).copy(alpha = 0.75f),
                            Color(0xFFFF2E93).copy(alpha = 0.65f),
                            Color(0xFF00E5FF).copy(alpha = 0.75f)
                        )
                    ),
                    RoundedCornerShape(20.dp)
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier.size(72.dp),
                    contentAlignment = Alignment.Center
                ) {
                    VipGemstoneBadgeCanvas(
                        vipLevel = if (activeLevel in 1..10) activeLevel else 10,
                        modifier = Modifier.fillMaxSize(),
                        animated = true
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Realistic3DDiamondIcon(
                            vipLevel = if (activeLevel in 1..10) activeLevel else 10,
                            size = 18.dp
                        )
                        Text(
                            text = "3D LUXURY GEMSTONE VIP BADGES",
                            color = goldAccentColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = if (activeLevel in 1..10) {
                            "Equipped: ${currentUserProfile?.activeVipBadgeName?.ifBlank { "VIP $activeLevel" }}"
                        } else {
                            "VIP 1 to VIP 10 Multi-Faceted 3D Gemstone Collection"
                        },
                        color = primaryTitleColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "Owned: ${ownedLevels.size}/10 Badges • Balance: $userCoins Coins • Tap any badge for 3D Preview",
                        color = secondarySubtitleColor,
                        fontSize = 11.sp
                    )
                }
            }
        }

        // 2-Column Grid of all 10 VIP Badges
        val rows = badgeCatalog.chunked(2)
        rows.forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                rowItems.forEach { item ->
                    val isOwned = item.vipLevel.toLong() in ownedLevels
                    val isEquipped = activeLevel == item.vipLevel
                    val palette = remember(item.vipLevel) { getVipGemPalette(item.vipLevel) }

                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = badgeCardBg,
                        modifier = Modifier
                            .weight(1f)
                            .border(
                                width = if (isEquipped) 1.8.dp else 1.dp,
                                brush = Brush.linearGradient(
                                    colors = if (isEquipped) {
                                        listOf(Color(0xFFFFD700), palette.glowAura, Color(0xFFFF2E93))
                                    } else if (isLight) {
                                        listOf(
                                            palette.gemCore.copy(alpha = 0.55f),
                                            Color(0xFFE0E0EA)
                                        )
                                    } else {
                                        listOf(
                                            palette.glowAura.copy(alpha = 0.45f),
                                            Color.White.copy(alpha = 0.12f)
                                        )
                                    }
                                ),
                                shape = RoundedCornerShape(18.dp)
                            )
                            .clickable { previewBadgeItem = item }
                            .testTag("vip_badge_card_${item.vipLevel}")
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Top row: VIP level pill + Owned/Equipped status
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "VIP ${item.vipLevel}",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .background(palette.gemDeep.copy(alpha = 0.90f))
                                        .border(
                                            1.dp,
                                            palette.glowAura.copy(alpha = 0.65f),
                                            RoundedCornerShape(50)
                                        )
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                                Text(
                                    text = when {
                                        isEquipped -> "EQUIPPED"
                                        isOwned -> "OWNED"
                                        else -> "NOT OWNED"
                                    },
                                    color = when {
                                        isEquipped -> Color(0xFF00C853)
                                        isOwned -> goldAccentColor
                                        else -> secondarySubtitleColor
                                    },
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }

                            Spacer(Modifier.height(6.dp))

                            // Pure transparent animated 3D Gemstone Badge
                            Box(
                                modifier = Modifier
                                    .size(94.dp)
                                    .clickable { previewBadgeItem = item },
                                contentAlignment = Alignment.Center
                            ) {
                                VipGemstoneBadgeCanvas(
                                    vipLevel = item.vipLevel,
                                    modifier = Modifier.fillMaxSize(),
                                    animated = true
                                )
                            }

                            Spacer(Modifier.height(6.dp))

                            Text(
                                text = item.badgeName,
                                color = primaryTitleColor,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = item.gemstoneName,
                                color = if (isLight) palette.gemDeep else palette.glowAura,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center
                            )

                            Spacer(Modifier.height(4.dp))

                            Text(
                                text = "${item.priceCoins} Coins",
                                color = goldAccentColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(Modifier.height(8.dp))

                            Button(
                                onClick = {
                                    if (isOwned) {
                                        onSelectActiveBadge(item.vipLevel)
                                    } else {
                                        onPurchaseBadge(item.vipLevel)
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = when {
                                        isEquipped -> Color(0xFF00C853)
                                        isOwned -> Color(0xFF7C4DFF)
                                        else -> Color(0xFFFF2E93)
                                    }
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(36.dp)
                                    .testTag("vip_badge_action_btn_${item.vipLevel}")
                            ) {
                                Text(
                                    text = when {
                                        isEquipped -> "Equipped ✓"
                                        isOwned -> "Equip Badge"
                                        else -> "Buy • ${item.priceCoins}"
                                    },
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }
                }
                if (rowItems.size == 1) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }

    previewBadgeItem?.let { item ->
        val isOwned = item.vipLevel.toLong() in ownedLevels
        val isEquipped = activeLevel == item.vipLevel
        VipBadgeLargePreviewDialog(
            badgeItem = item,
            isOwned = isOwned,
            isEquipped = isEquipped,
            userCoins = userCoins,
            onDismiss = { previewBadgeItem = null },
            onBuyOrEquip = {
                if (isOwned) {
                    onSelectActiveBadge(item.vipLevel)
                } else {
                    onPurchaseBadge(item.vipLevel)
                }
                previewBadgeItem = null
            }
        )
    }
}

@Composable
fun VipBadgeLargePreviewDialog(
    badgeItem: VipBadgeCatalogItem,
    isOwned: Boolean,
    isEquipped: Boolean,
    userCoins: Long,
    onDismiss: () -> Unit,
    onBuyOrEquip: () -> Unit
) {
    val palette = remember(badgeItem.vipLevel) { getVipGemPalette(badgeItem.vipLevel) }
    val isLight = com.example.ui.theme.ThemeManager.isLightMode
    val modalBg = if (isLight) Color(0xFFFFFFFF) else Color(0xFF0E0B1B)
    val infoCardBg = if (isLight) Color(0xFFF8F8FC) else Color(0xFF17122B)
    val primaryText = if (isLight) Color(0xFF000000) else Color.White
    val secondaryText = if (isLight) Color(0xFF4A4A5A) else Color(0xFFB8B2D1)
    val goldAccent = if (isLight) Color(0xFFB47800) else Color(0xFFFFD700)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = modalBg,
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .border(
                    width = 1.5.dp,
                    brush = Brush.linearGradient(
                        listOf(Color(0xFFFFD700), palette.glowAura, Color(0xFFFF2E93))
                    ),
                    shape = RoundedCornerShape(28.dp)
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Realistic3DDiamondIcon(
                            vipLevel = badgeItem.vipLevel,
                            size = 18.dp
                        )
                        Text(
                            text = "3D LUXURY GEMSTONE PREVIEW",
                            color = goldAccent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close preview", tint = primaryText)
                    }
                }

                Spacer(Modifier.height(8.dp))

                // Large 220dp Transparent 3D Animated Badge Preview
                Box(
                    modifier = Modifier
                        .size(220.dp)
                        .testTag("vip_badge_large_3d_preview_${badgeItem.vipLevel}"),
                    contentAlignment = Alignment.Center
                ) {
                    VipGemstoneBadgeCanvas(
                        vipLevel = badgeItem.vipLevel,
                        modifier = Modifier.fillMaxSize(),
                        animated = true
                    )
                }

                Spacer(Modifier.height(8.dp))

                Text(
                    text = badgeItem.badgeName,
                    color = primaryText,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = badgeItem.gemstoneName,
                    color = if (isLight) palette.gemDeep else palette.glowAura,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(10.dp))

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = infoCardBg,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, com.example.ui.theme.NoiseBorder, RoundedCornerShape(14.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Frame & Setting: ${badgeItem.frameDescription}",
                            color = primaryText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "3D FX: ${badgeItem.effectDescription}",
                            color = secondaryText,
                            fontSize = 11.sp
                        )
                        Text(
                            text = "Displays beside your username on Profile, Posts, Chatrooms, Seats, Direct Messages & Search.",
                            color = if (isLight) Color(0xFF7C3AED) else Color(0xFF00E5FF),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Badge Price",
                            color = secondaryText,
                            fontSize = 11.sp
                        )
                        Text(
                            text = "${badgeItem.priceCoins} VIP Coins",
                            color = goldAccent,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Your Balance",
                            color = secondaryText,
                            fontSize = 11.sp
                        )
                        Text(
                            text = "$userCoins Coins",
                            color = primaryText,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))

                Button(
                    onClick = onBuyOrEquip,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = when {
                            isEquipped -> Color(0xFF00C853)
                            isOwned -> Color(0xFF7C4DFF)
                            else -> Color(0xFFFF2E93)
                        }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("vip_preview_modal_action_${badgeItem.vipLevel}")
                ) {
                    Text(
                        text = when {
                            isEquipped -> "Currently Equipped ✓"
                            isOwned -> "Equip ${badgeItem.badgeName}"
                            else -> "Purchase & Equip (${badgeItem.priceCoins} Coins)"
                        },
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }
    }
}

/**
 * VIP Animated Username & Profile Effect Composable.
 * - Normal users (vipLevel <= 0): Standard readable text without VIP animation.
 * - VIP 1: Subtle animated sapphire-cyan gradient text.
 * - VIP 2: Moving emerald-gold premium gradient border around animated text.
 * - VIP 3: Glowing ruby-gold animated text with luminous aura.
 * - VIP 4: Fire-style animated text effect with blazing orange-crimson-gold gradient.
 * - VIP 5: Multicolor animated rainbow-diamond gradient text.
 * - VIP 6–10: Sovereign crystal / imperial topaz / opal / eternal diamond animated text + shimmering pill border.
 */
@Composable
fun VipAnimatedUsername(
    username: String,
    vipLevel: Int,
    modifier: Modifier = Modifier,
    fontSize: androidx.compose.ui.unit.TextUnit = 14.sp,
    fontWeight: FontWeight = FontWeight.Bold,
    defaultColor: Color = com.example.ui.theme.NoiseWhite,
    prefixAt: Boolean = false,
    maxLines: Int = 1
) {
    val displayStr = if (prefixAt && !username.startsWith("@")) "@$username" else username
    val safeLevel = vipLevel.coerceIn(0, 10)
    if (safeLevel <= 0) {
        Text(
            text = displayStr,
            color = defaultColor,
            fontSize = fontSize,
            fontWeight = fontWeight,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis,
            modifier = modifier.testTag("vip_username_normal_$username")
        )
        return
    }

    val transition = rememberInfiniteTransition(label = "vip_username_anim_$safeLevel")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = (2600 - safeLevel * 120).coerceAtLeast(1100), easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "vip_text_phase"
    )
    val glowPulse by transition.animateFloat(
        initialValue = 0.65f,
            targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "vip_glow_pulse"
    )

    val textColors = when (safeLevel) {
        1 -> listOf(
            Color(0xFF60A5FA),
            Color(0xFF00E5FF),
            Color(0xFFA7F3D0),
            Color(0xFF60A5FA)
        )
        2 -> listOf(
            Color(0xFF34D399),
            Color(0xFFFFD700),
            Color(0xFF00E5FF),
            Color(0xFF34D399)
        )
        3 -> listOf(
            Color(0xFFFF4D6D),
            Color(0xFFFFB800),
            Color(0xFFFF8FA3),
            Color(0xFFFF4D6D)
        )
        4 -> listOf(
            Color(0xFFFF3D00),
            Color(0xFFFF9100),
            Color(0xFFFFEA00),
            Color(0xFFFF2E93),
            Color(0xFFFF3D00)
        )
        5 -> listOf(
            Color(0xFF00E5FF),
            Color(0xFF8B5CF6),
            Color(0xFFD946EF),
            Color(0xFFFFB800),
            Color(0xFF10B981),
            Color(0xFF00E5FF)
        )
        else -> {
            val pal = getVipGemPalette(safeLevel)
            listOf(
                pal.gemHighlight,
                Color(0xFFFFD700),
                pal.glowAura,
                Color(0xFFD946EF),
                pal.gemHighlight
            )
        }
    }

    val startX = -120f + phase * 360f
    val animatedTextBrush = Brush.linearGradient(
        colors = textColors,
        start = Offset(startX, 0f),
        end = Offset(startX + 180f, 40f)
    )

    val hasAnimatedBorder = safeLevel == 2 || safeLevel >= 6
    val hasGlowAura = safeLevel >= 3

    Box(
        modifier = modifier
            .then(
                if (hasAnimatedBorder) {
                    Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (com.example.ui.theme.ThemeManager.isLightMode) {
                                com.example.ui.theme.LightLavender
                            } else {
                                Color(0xFF140F28).copy(alpha = 0.55f)
                            }
                        )
                        .border(
                            width = 1.dp,
                            brush = Brush.linearGradient(
                                colors = textColors,
                                start = Offset(startX + 40f, 0f),
                                end = Offset(startX + 220f, 30f)
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 1.dp)
                } else if (hasGlowAura) {
                    Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(textColors.first().copy(alpha = 0.10f * glowPulse))
                        .padding(horizontal = 4.dp, vertical = 0.5.dp)
                } else {
                    Modifier
                }
            )
            .testTag("vip_animated_username_${safeLevel}_$username"),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            if (safeLevel == 4) {
                Text(
                    text = "🔥",
                    fontSize = (fontSize.value - 2f).coerceAtLeast(10f).sp
                )
            } else if (safeLevel >= 8) {
                Text(
                    text = "✨",
                    fontSize = (fontSize.value - 2f).coerceAtLeast(10f).sp
                )
            }
            Text(
                text = displayStr,
                style = androidx.compose.ui.text.TextStyle(
                    brush = animatedTextBrush,
                    fontSize = fontSize,
                    fontWeight = FontWeight.ExtraBold,
                    shadow = if (hasGlowAura) {
                        androidx.compose.ui.graphics.Shadow(
                            color = textColors.first().copy(alpha = 0.75f * glowPulse),
                            offset = Offset(0f, 0f),
                            blurRadius = if (safeLevel >= 4) 12f * glowPulse else 8f * glowPulse
                        )
                    } else null
                ),
                maxLines = maxLines,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

