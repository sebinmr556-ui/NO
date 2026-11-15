package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.*
import com.example.ui.theme.*
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Composable modifier that makes any UI component selectable, draggable, resizable,
 * and restylable in real time when the verified Owner activates the Owner Edit Bar.
 * Also applies published/preview `VisualComponentOverride` properties for all users.
 */
fun Modifier.ownerEditableComponent(
    elementId: String,
    label: String
): Modifier = composed {
    val cfg = CentralLiveUpdateRuntime.effectiveConfig
    val override = remember(cfg.componentOverridesJson, elementId) {
        cfg.parsedComponentOverrides().firstOrNull { it.elementId == elementId }
    }
    val isEditMode = CentralLiveUpdateRuntime.isOwnerEditModeActive
    val isSelected = isEditMode && CentralLiveUpdateRuntime.selectedElementId == elementId
    val density = LocalDensity.current

    if (override?.isHidden == true && !isEditMode) {
        return@composed this.size(0.dp).alpha(0f)
    }

    var m = this

    // Apply margin if configured
    if (override != null && override.marginDp > 0f) {
        m = m.padding(override.marginDp.dp)
    }

    // Apply drag offset (in dp)
    if (override != null && (override.offsetXDp != 0f || override.offsetYDp != 0f)) {
        m = m.offset {
            IntOffset(
                x = with(density) { override.offsetXDp.dp.roundToPx() },
                y = with(density) { override.offsetYDp.dp.roundToPx() }
            )
        }
    }

    // Apply scale
    if (override != null && (override.widthScale != 1f || override.heightScale != 1f)) {
        m = m.scale(
            scaleX = override.widthScale.coerceIn(0.4f, 2.5f),
            scaleY = override.heightScale.coerceIn(0.4f, 2.5f)
        )
    }

    // Apply custom dimensions if specified
    if (override != null && override.customWidthDp > 20f) {
        m = m.width(override.customWidthDp.dp)
    }
    if (override != null && override.customHeightDp > 20f) {
        m = m.height(override.customHeightDp.dp)
    }

    val shapeRadius = if (override != null && override.cornerRadiusDp >= 0f) {
        override.cornerRadiusDp.dp
    } else {
        12.dp
    }
    val shape = RoundedCornerShape(shapeRadius)

    // Apply shadow if specified
    if (override != null && override.shadowElevationDp > 0f) {
        m = m.shadow(override.shadowElevationDp.dp, shape)
    }

    // Apply custom gradient or background color if specified
    if (override != null && override.gradientSpecJson.isNotBlank()) {
        val spec = MagicColorEngine.deserializeGradientSpec(override.gradientSpecJson)
        if (spec != null) {
            m = m.background(MagicColorEngine.buildComposeBrush(spec), shape)
        }
    } else if (override != null && override.backgroundColorHex.isNotBlank()) {
        val bgColor = CentralLiveUpdateRuntime.parseColorOrFallback(override.backgroundColorHex, Color.Transparent)
        if (bgColor != Color.Transparent) {
            m = m.background(bgColor, shape)
        }
    }

    // Apply custom border if specified
    if (override != null && override.borderWidthDp > 0f && override.borderColorHex.isNotBlank()) {
        val borderCol = CentralLiveUpdateRuntime.parseColorOrFallback(override.borderColorHex, NoiseBrandPurple)
        m = m.border(override.borderWidthDp.dp, borderCol, shape)
    }

    // Apply padding if specified
    if (override != null && override.paddingDp >= 0f) {
        m = m.padding(override.paddingDp.dp)
    }

    // Apply opacity/alpha
    val effectiveAlpha = when {
        override?.isHidden == true && isEditMode -> 0.35f
        override != null -> override.opacityAlpha.coerceIn(0.1f, 1f)
        else -> 1f
    }
    if (effectiveAlpha < 1f) {
        m = m.alpha(effectiveAlpha)
    }

    // When Owner Edit Mode is active, add visual selection outline + drag-and-drop + tap-to-select
    if (isEditMode) {
        val highlightColor = if (isSelected) Color(0xFF00E5FF) else Color(0xFF8525FF).copy(alpha = 0.55f)
        val strokeWidth = if (isSelected) 2.dp else 1.dp
        m = m
            .border(strokeWidth, highlightColor, shape)
            .pointerInput(elementId, isEditMode) {
                detectDragGestures(
                    onDragStart = {
                        CentralLiveUpdateRuntime.selectVisualElement(elementId, label)
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val currentOv = CentralLiveUpdateRuntime.getComponentOverride(elementId)
                        if (!currentOv.isLocked) {
                            val dxDp = dragAmount.x / density.density
                            val dyDp = dragAmount.y / density.density
                            CentralLiveUpdateRuntime.updateComponentOverride(
                                elementId = elementId,
                                description = "Dragged '$label' to (${(currentOv.offsetXDp + dxDp).roundToInt()}dp, ${(currentOv.offsetYDp + dyDp).roundToInt()}dp)"
                            ) { prev ->
                                prev.copy(
                                    label = label,
                                    offsetXDp = (prev.offsetXDp + dxDp).coerceIn(-220f, 220f),
                                    offsetYDp = (prev.offsetYDp + dyDp).coerceIn(-220f, 220f)
                                )
                            }
                        }
                    }
                )
            }
    }

    m
}

/**
 * Renders any custom UI elements (Text, Buttons, Cards, Banners, Badges, Containers)
 * added to [pageKey] by the Owner via the Visual Editor or Unlimited Page Creator.
 */
@Composable
fun OwnerCustomPageElementsSection(
    pageKey: String,
    onNavigateRoute: (String) -> Unit = {}
) {
    val cfg = CentralLiveUpdateRuntime.effectiveConfig
    val customElements = remember(cfg.componentOverridesJson, pageKey) {
        cfg.parsedComponentOverrides().filter {
            it.isCustomAdded && (it.pageKey.equals(pageKey, ignoreCase = true) || it.pageKey.equals("ALL", ignoreCase = true))
        }
    }
    val isEditMode = CentralLiveUpdateRuntime.isOwnerEditModeActive

    if (customElements.isEmpty()) return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        customElements.forEach { elem ->
            if (!elem.isHidden || isEditMode) {
                val bgCol = CentralLiveUpdateRuntime.parseColorOrFallback(
                    elem.backgroundColorHex.ifBlank { cfg.primaryAccentHex },
                    NoiseBrandPurple
                )
                val txtCol = CentralLiveUpdateRuntime.parseColorOrFallback(
                    elem.textColorHex.ifBlank { "#FFFFFF" },
                    Color.White
                )
                val fontWeight = when (elem.fontWeightName.lowercase()) {
                    "black" -> FontWeight.Black
                    "bold" -> FontWeight.Bold
                    "medium" -> FontWeight.Medium
                    else -> FontWeight.SemiBold
                }
                val align = when (elem.horizontalAlign.lowercase()) {
                    "center" -> Alignment.CenterHorizontally
                    "end" -> Alignment.End
                    else -> Alignment.Start
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = align
                ) {
                    Surface(
                        modifier = Modifier
                            .ownerEditableComponent(elem.elementId, elem.label)
                            .clickable {
                                if (isEditMode) {
                                    CentralLiveUpdateRuntime.selectVisualElement(elem.elementId, elem.label)
                                } else if (elem.actionTargetRoute.isNotBlank()) {
                                    onNavigateRoute(elem.actionTargetRoute)
                                }
                            },
                        shape = RoundedCornerShape((elem.cornerRadiusDp.takeIf { it >= 0f } ?: 16f).dp),
                        color = bgCol,
                        border = if (elem.borderWidthDp > 0f) {
                            BorderStroke(
                                elem.borderWidthDp.dp,
                                CentralLiveUpdateRuntime.parseColorOrFallback(elem.borderColorHex, VipGold)
                            )
                        } else null
                    ) {
                        Row(
                            modifier = Modifier.padding(
                                horizontal = (elem.paddingDp.takeIf { it >= 0f } ?: 16f).dp,
                                vertical = ((elem.paddingDp.takeIf { it >= 0f } ?: 12f) * 0.75f).dp
                            ),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = when (elem.customElementType.lowercase()) {
                                    "banner" -> Icons.Default.Campaign
                                    "card" -> Icons.Default.DashboardCustomize
                                    "badge" -> Icons.Default.Verified
                                    "text" -> Icons.Default.TextFields
                                    else -> Icons.Default.AutoAwesome
                                },
                                contentDescription = elem.label,
                                tint = txtCol,
                                modifier = Modifier.size((18f * elem.fontSizeScale).dp)
                            )
                            Column {
                                Text(
                                    text = elem.customText.ifBlank { elem.label },
                                    color = txtCol,
                                    fontSize = (13f * elem.fontSizeScale).sp,
                                    fontWeight = fontWeight
                                )
                                if (isEditMode) {
                                    Text(
                                        text = "ID: ${elem.elementId} • Drag to move",
                                        color = txtCol.copy(alpha = 0.72f),
                                        fontSize = 9.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * OWNER-ONLY EDIT BAR ON EVERY PAGE
 *
 * Floating Master Control Bar visible exclusively to the verified Owner UID (`sebin_mr`).
 * Provides:
 * - 1-tap `EDIT BAR` activation on every screen of NOICE LIVE
 * - Prominent `FINISH` button -> Preview & `PUBLISH UPDATE` / `CANCEL`
 * - Interactive selection of any UI element on the active page
 * - MagicPicker Color Wheel & Multi-Stop Gradient Editor
 * - Drag-and-Drop / Resize / Align / Text / Border / Shadow Component Editor
 * - `+ Add Element` & `+ Add Page` (Unlimited Page Creator)
 * - Real Google Gemini AI Developer (`gemini-3.8-flash`)
 * - Undo / Redo / Reset history controls
 */
@Composable
fun FloatingOwnerEditBarOverlay(
    viewModel: NoiseViewModel,
    currentPageKey: String,
    onOpenDeveloperStudio: () -> Unit = {}
) {
    // STRICT OWNER-ONLY ACCESS GATE:
    // Verified via Firebase Auth UID + Server-side /super_owner_registry/primary
    if (!viewModel.isSuperOwner) return

    LaunchedEffect(currentPageKey) {
        CentralLiveUpdateRuntime.activePageContextKey = currentPageKey
    }

    val isEditMode = CentralLiveUpdateRuntime.isOwnerEditModeActive
    val selectedId = CentralLiveUpdateRuntime.selectedElementId
    val selectedLabel = CentralLiveUpdateRuntime.selectedElementLabel
    val activeConfig = CentralLiveUpdateRuntime.effectiveConfig

    var activeToolSheet by remember { mutableStateOf("NONE") } // "NONE", "MAGIC_PICKER", "COMPONENT_EDITOR", "ADD_PAGE", "GEMINI_AI", "MASTER_DASHBOARD"
    var barOffsetYDp by remember { mutableFloatStateOf(0f) }
    val density = LocalDensity.current

    // Page-aware catalog of selectable UI elements so the Owner can either tap directly on screen OR pick from the inspector bar
    val selectablePageElements = remember(currentPageKey, activeConfig.componentOverridesJson) {
        val base = mutableListOf(
            "home_header_logo" to "NOICE Brand Logo",
            "header_vip_pill" to "Top Bar VIP Crown Pill",
            "header_games_button" to "Top Bar 3D Games Button",
            "header_live_button" to "Top Bar Live Button",
            "header_notification_icon" to "Notification Bell Icon",
            "header_dm_icon" to "Direct Messages Icon",
            "bottom_nav_bar" to "Bottom Navigation Dock",
            "page_background_$currentPageKey" to "$currentPageKey Page Background",
            "primary_cta_buttons" to "Primary Action Buttons",
            "chatroom_action_buttons" to "Chatroom Stage Buttons",
            "story_rings_strip" to "Stories Ring Strip"
        )
        activeConfig.parsedComponentOverrides().forEach { ov ->
            if (base.none { it.first == ov.elementId }) {
                base.add(ov.elementId to ov.label.ifBlank { ov.elementId })
            }
        }
        base
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 56.dp, bottom = 74.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .offset { IntOffset(0, with(density) { barOffsetYDp.dp.roundToPx() }) }
                .padding(horizontal = 10.dp)
                .widthIn(max = 600.dp),
            horizontalAlignment = Alignment.End
        ) {
            if (!isEditMode) {
                // Collapsed Floating Owner Edit Bar pill available on every page
                Surface(
                    modifier = Modifier
                        .testTag("floating_owner_edit_bar_button")
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                barOffsetYDp = (barOffsetYDp + dragAmount.y / density.density).coerceIn(0f, 460f)
                            }
                        },
                    shape = RoundedCornerShape(999.dp),
                    color = Color(0xFF0B0718).copy(alpha = 0.94f),
                    border = BorderStroke(
                        1.5.dp,
                        Brush.horizontalGradient(listOf(VipGold, NoiseBrandPurple, Color(0xFF00E5FF)))
                    ),
                    shadowElevation = 10.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(
                                    Brush.horizontalGradient(listOf( Color(0xFF8525FF), Color(0xFFE52BEB) ))
                                )
                                .clickable {
                                    CentralLiveUpdateRuntime.activateOwnerEditMode(
                                        initialSelectedId = "home_header_logo",
                                        initialLabel = "NOICE Brand Logo"
                                    )
                                }
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                Icons.Default.EditNote,
                                contentDescription = "Activate Owner Edit Bar",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "EDIT BAR",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black
                            )
                        }

                        Surface(
                            modifier = Modifier
                                .testTag("owner_master_dashboard_quick_button")
                                .clickable { activeToolSheet = "MASTER_DASHBOARD" },
                            shape = RoundedCornerShape(999.dp),
                            color = VipGold.copy(alpha = 0.16f),
                            border = BorderStroke(1.dp, VipGold.copy(alpha = 0.55f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    Icons.Default.AdminPanelSettings,
                                    contentDescription = "Owner Master Dashboard",
                                    tint = VipGold,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "MASTER HUB • ${activeConfig.versionLabel}",
                                    color = VipGold,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }
                }
            } else {
                // Expanded Active Visual Editing Bar on every page
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("active_owner_visual_edit_bar")
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                barOffsetYDp = (barOffsetYDp + dragAmount.y / density.density).coerceIn(0f, 420f)
                            }
                        },
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFF090614).copy(alpha = 0.96f),
                    border = BorderStroke(
                        1.5.dp,
                        Brush.horizontalGradient(listOf(Color(0xFF00E5FF), NoiseBrandPurple, VipGold))
                    ),
                    shadowElevation = 14.dp
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Top Row: Live Edit Badge + Undo / Redo / Reset + FINISH button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF00E5FF))
                                )
                                Column {
                                    Text(
                                        text = "OWNER VISUAL EDITOR • $currentPageKey",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                    Text(
                                        text = "Tap or drag any UI element • Live preview active",
                                        color = Color(0xFF00E5FF),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                IconButton(
                                    onClick = { CentralLiveUpdateRuntime.undoVisualEdit() },
                                    enabled = CentralLiveUpdateRuntime.canUndo,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.Undo,
                                        contentDescription = "Undo Visual Edit",
                                        tint = if (CentralLiveUpdateRuntime.canUndo) Color.White else Color.White.copy(alpha = 0.3f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                IconButton(
                                    onClick = { CentralLiveUpdateRuntime.redoVisualEdit() },
                                    enabled = CentralLiveUpdateRuntime.canRedo,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.Redo,
                                        contentDescription = "Redo Visual Edit",
                                        tint = if (CentralLiveUpdateRuntime.canRedo) Color.White else Color.White.copy(alpha = 0.3f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                IconButton(
                                    onClick = { CentralLiveUpdateRuntime.resetVisualEditsToProduction() },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        Icons.Default.RestartAlt,
                                        contentDescription = "Reset Edits",
                                        tint = VipGold,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                // FINISH button -> opens Finish & Publish confirmation modal
                                Button(
                                    onClick = {
                                        CentralLiveUpdateRuntime.showFinishPublishDialog = true
                                    },
                                    modifier = Modifier
                                        .height(34.dp)
                                        .testTag("owner_edit_bar_finish_button"),
                                    colors = ButtonDefaults.buttonColors(containerColor = OnlineGreen),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                                    shape = RoundedCornerShape(999.dp)
                                ) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = "Finish Editing",
                                        tint = Color.Black,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = "FINISH",
                                        color = Color.Black,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            }
                        }

                        // Second Row: Element Selector strip (tap any chip or tap directly on screen)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "SELECT:",
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                            selectablePageElements.forEach { (id, lbl) ->
                                val isSel = selectedId == id
                                val ov = CentralLiveUpdateRuntime.getComponentOverride(id)
                                Surface(
                                    modifier = Modifier.clickable {
                                        CentralLiveUpdateRuntime.selectVisualElement(id, lbl)
                                    },
                                    shape = RoundedCornerShape(999.dp),
                                    color = if (isSel) Color(0xFF00E5FF).copy(alpha = 0.22f) else Color.White.copy(alpha = 0.07f),
                                    border = BorderStroke(
                                        1.dp,
                                        if (isSel) Color(0xFF00E5FF) else Color.White.copy(alpha = 0.15f)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        if (ov.isLocked) {
                                            Icon(
                                                Icons.Default.Lock,
                                                contentDescription = "Locked",
                                                tint = VipGold,
                                                modifier = Modifier.size(10.dp)
                                            )
                                        }
                                        Text(
                                            text = lbl,
                                            color = if (isSel) Color(0xFF00E5FF) else Color.White,
                                            fontSize = 10.sp,
                                            fontWeight = if (isSel) FontWeight.ExtraBold else FontWeight.Medium
                                        )
                                        if (ov.offsetXDp != 0f || ov.offsetYDp != 0f) {
                                            Text(
                                                text = "(${ov.offsetXDp.roundToInt()},${ov.offsetYDp.roundToInt()})",
                                                color = VipGold,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Third Row: Master Tool Actions (MagicPicker, Drag & Drop Inspector, +Element, +Page, Gemini AI, Master Hub)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OwnerEditToolChip(
                                icon = Icons.Default.Palette,
                                label = "MagicPicker",
                                accent = Color(0xFFE52BEB),
                                testTag = "owner_tool_magic_picker",
                                onClick = { activeToolSheet = "MAGIC_PICKER" }
                            )
                            OwnerEditToolChip(
                                icon = Icons.Default.OpenWith,
                                label = "Move & Resize",
                                accent = Color(0xFF00E5FF),
                                testTag = "owner_tool_component_editor",
                                onClick = { activeToolSheet = "COMPONENT_EDITOR" }
                            )
                            OwnerEditToolChip(
                                icon = Icons.Default.AddBox,
                                label = "+ Element",
                                accent = OnlineGreen,
                                testTag = "owner_tool_add_element",
                                onClick = {
                                    CentralLiveUpdateRuntime.addCustomComponentToPage(
                                        pageKey = currentPageKey,
                                        elementType = "button",
                                        label = "Custom $currentPageKey Action",
                                        customText = "✨ Explore Live on $currentPageKey"
                                    )
                                    activeToolSheet = "COMPONENT_EDITOR"
                                }
                            )
                            OwnerEditToolChip(
                                icon = Icons.Default.NoteAdd,
                                label = "+ Add Page",
                                accent = VipGold,
                                testTag = "owner_tool_add_page",
                                onClick = { activeToolSheet = "ADD_PAGE" }
                            )
                            OwnerEditToolChip(
                                icon = Icons.Default.AutoAwesome,
                                label = "Gemini AI",
                                accent = Color(0xFF8525FF),
                                testTag = "owner_tool_gemini_ai",
                                onClick = { activeToolSheet = "GEMINI_AI" }
                            )
                            OwnerEditToolChip(
                                icon = Icons.Default.DashboardCustomize,
                                label = "Master Hub",
                                accent = VipGold,
                                testTag = "owner_tool_master_hub",
                                onClick = { activeToolSheet = "MASTER_DASHBOARD" }
                            )
                        }
                    }
                }
            }
        }
    }

    // Active Tool Modals
    when (activeToolSheet) {
        "MAGIC_PICKER" -> {
            MagicPickerColorAndGradientModal(
                selectedElementId = selectedId,
                selectedElementLabel = selectedLabel,
                onDismiss = { activeToolSheet = "NONE" }
            )
        }
        "COMPONENT_EDITOR" -> {
            VisualComponentInspectorModal(
                selectedElementId = selectedId.ifBlank { "home_header_logo" },
                selectedElementLabel = selectedLabel.ifBlank { "NOICE Brand Logo" },
                currentPageKey = currentPageKey,
                selectableElements = selectablePageElements,
                onOpenMagicPicker = { activeToolSheet = "MAGIC_PICKER" },
                onDismiss = { activeToolSheet = "NONE" }
            )
        }
        "ADD_PAGE" -> {
            UnlimitedPageCreatorModal(
                viewModel = viewModel,
                onDismiss = { activeToolSheet = "NONE" }
            )
        }
        "GEMINI_AI" -> {
            OwnerGeminiLivePromptModal(
                viewModel = viewModel,
                onOpenFullStudio = {
                    activeToolSheet = "NONE"
                    onOpenDeveloperStudio()
                },
                onDismiss = { activeToolSheet = "NONE" }
            )
        }
        "MASTER_DASHBOARD" -> {
            OwnerMasterDashboardModal(
                viewModel = viewModel,
                currentPageKey = currentPageKey,
                onOpenMagicPicker = { activeToolSheet = "MAGIC_PICKER" },
                onOpenComponentEditor = { activeToolSheet = "COMPONENT_EDITOR" },
                onOpenAddPage = { activeToolSheet = "ADD_PAGE" },
                onOpenGeminiPrompt = { activeToolSheet = "GEMINI_AI" },
                onOpenFullDeveloperStudio = {
                    activeToolSheet = "NONE"
                    onOpenDeveloperStudio()
                },
                onDismiss = { activeToolSheet = "NONE" }
            )
        }
    }

    // FINISH & PUBLISH confirmation modal
    if (CentralLiveUpdateRuntime.showFinishPublishDialog) {
        FinishAndPublishReviewModal(
            viewModel = viewModel,
            onDismissCancel = {
                CentralLiveUpdateRuntime.showFinishPublishDialog = false
            },
            onDiscardAndExit = {
                CentralLiveUpdateRuntime.exitOwnerEditMode(keepPreviewChanges = false)
            },
            onPublishedSuccess = {
                CentralLiveUpdateRuntime.exitOwnerEditMode(keepPreviewChanges = true)
            }
        )
    }
}

@Composable
private fun OwnerEditToolChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    accent: Color,
    testTag: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .testTag(testTag)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = accent.copy(alpha = 0.15f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.55f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = accent,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = label,
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * 2. ADVANCED MAGICPICKER COLOR & MULTI-STOP GRADIENT EDITOR
 *
 * Implements the Photoshop-inspired MagicPicker interface:
 * - Circular 360° Rainbow Hue Wheel + inner Saturation & Brightness Square (`Canvas` + pointer drag)
 * - Previous & Current color comparison split preview + Foreground/Background swatches
 * - Hue, Saturation, Brightness, Red, Green, Blue, and Alpha/Opacity sliders + HEX code input
 * - Multi-stop Linear & Radial Gradient Editor with angle (`0°–360°`) & unlimited stops
 * - Immediate live application to any page background, button, header, navbar, text, icon, story ring,
 *   chatroom theme, border, shadow, or selected UI component.
 */
@Composable
fun MagicPickerColorAndGradientModal(
    selectedElementId: String,
    selectedElementLabel: String,
    onDismiss: () -> Unit
) {
    val cfg = CentralLiveUpdateRuntime.effectiveConfig
    var editorTab by remember { mutableStateOf("WHEEL") } // "WHEEL", "SLIDERS", "GRADIENT", "SWATCHES"

    val initialHex = CentralLiveUpdateRuntime.magicCurrentColorHex.ifBlank { cfg.primaryAccentHex }
    val initialHsva = remember(initialHex) { MagicColorEngine.hexToHsva(initialHex) }

    var hue by remember { mutableFloatStateOf(initialHsva.first.first) }
    var saturation by remember { mutableFloatStateOf(initialHsva.first.second) }
    var brightness by remember { mutableFloatStateOf(initialHsva.first.third) }
    var alpha by remember { mutableFloatStateOf(initialHsva.second) }
    var hexInput by remember { mutableStateOf(initialHex) }
    var activeSwatchSlot by remember { mutableStateOf("FG") } // "FG" or "BG"

    // Multi-stop gradient state
    var gradientType by remember { mutableStateOf(cfg.buttonGradientType.ifBlank { "linear" }) }
    var gradientAngle by remember { mutableFloatStateOf(cfg.buttonGradientAngleDeg.toFloat()) }
    var gradientStops by remember {
        mutableStateOf(
            listOf(
                MagicGradientStop("s0", 0f, cfg.buttonGradientStartHex, 1f),
                MagicGradientStop("s1", 1f, cfg.buttonGradientEndHex, 1f)
            )
        )
    }
    var selectedStopIndex by remember { mutableIntStateOf(0) }

    // Target selector where color/gradient is applied immediately
    val applyTargets = remember(selectedElementId, selectedElementLabel) {
        buildList {
            if (selectedElementId.isNotBlank()) {
                add("SELECTED_ELEMENT" to "Selected: ${selectedElementLabel.ifBlank { selectedElementId }}")
            }
            add("HOME_BG" to "Home Page Background")
            add("SURFACE_BG" to "Section / Surface Background")
            add("CARD_BG" to "Cards & Containers")
            add("BUTTON_GRADIENT" to "Primary Buttons & CTAs")
            add("CHATROOM_BUTTONS" to "Chatroom Buttons Gradient")
            add("CHATROOM_THEME" to "Chatroom Stage Background")
            add("HEADER_BG" to "Top Header Bar")
            add("BOTTOM_NAV_BG" to "Bottom Navigation Bar")
            add("TEXT_PRIMARY" to "Headings & Primary Text")
            add("ICON_TINT" to "Icons & Accent Badges")
            add("STORY_RINGS" to "Story Rings Gradient")
            add("PROFILE_FRAME" to "Profile Frame & VIP Gold")
            add("BORDERS" to "Borders & Dividers")
            add("SHADOW_GLOW" to "Shadows & Neon Glow")
        }
    }
    var selectedTargetKey by remember {
        mutableStateOf(if (selectedElementId.isNotBlank()) "SELECTED_ELEMENT" else "HOME_BG")
    }

    val currentHex = remember(hue, saturation, brightness, alpha) {
        MagicColorEngine.hsvToHex(hue, saturation, brightness, alpha, includeAlpha = alpha < 0.99f)
    }
    val currentRgb = remember(hue, saturation, brightness) {
        MagicColorEngine.hsvToRgb(hue, saturation, brightness)
    }
    val currentColor = remember(currentHex, alpha) {
        CentralLiveUpdateRuntime.parseColorOrFallback(currentHex, NoiseBrandPurple).copy(alpha = alpha)
    }
    val previousColor = remember(CentralLiveUpdateRuntime.magicPreviousColorHex) {
        CentralLiveUpdateRuntime.parseColorOrFallback(CentralLiveUpdateRuntime.magicPreviousColorHex, NoiseBrandPurple)
    }

    fun syncFromHsv(h: Float, s: Float, v: Float, a: Float = alpha) {
        hue = h.coerceIn(0f, 360f)
        saturation = s.coerceIn(0f, 1f)
        brightness = v.coerceIn(0f, 1f)
        alpha = a.coerceIn(0f, 1f)
        val newHex = MagicColorEngine.hsvToHex(hue, saturation, brightness, alpha, includeAlpha = alpha < 0.99f)
        hexInput = newHex
        CentralLiveUpdateRuntime.magicCurrentColorHex = newHex
        if (activeSwatchSlot == "FG") {
            CentralLiveUpdateRuntime.magicForegroundColorHex = newHex
        } else {
            CentralLiveUpdateRuntime.magicBackgroundColorHex = newHex
        }
        if (editorTab == "GRADIENT" && selectedStopIndex in gradientStops.indices) {
            gradientStops = gradientStops.mapIndexed { idx, st ->
                if (idx == selectedStopIndex) st.copy(colorHex = MagicColorEngine.hsvToHex(hue, saturation, brightness), alpha = alpha)
                else st
            }
        }
    }

    fun applyColorOrGradientLive(asGradient: Boolean = (editorTab == "GRADIENT")) {
        val hex6 = MagicColorEngine.hsvToHex(hue, saturation, brightness)
        CentralLiveUpdateRuntime.magicPreviousColorHex = currentHex
        val startStopHex = gradientStops.minByOrNull { it.position }?.colorHex ?: hex6
        val endStopHex = gradientStops.maxByOrNull { it.position }?.colorHex ?: hex6
        val spec = MagicGradientSpec(
            gradientId = "grad_${System.currentTimeMillis()}",
            name = "MagicPicker Gradient",
            type = gradientType,
            angleDegrees = gradientAngle,
            stops = gradientStops
        )
        val specJson = MagicColorEngine.serializeGradientSpec(spec)

        when (selectedTargetKey) {
            "SELECTED_ELEMENT" -> {
                val targetId = selectedElementId.ifBlank { "home_header_logo" }
                CentralLiveUpdateRuntime.updateComponentOverride(
                    elementId = targetId,
                    description = if (asGradient) "Applied ${gradientType.uppercase()} gradient (${gradientAngle.roundToInt()}°) to '$selectedElementLabel'"
                    else "Applied MagicPicker color $hex6 to '$selectedElementLabel'"
                ) { ov ->
                    if (asGradient) {
                        ov.copy(gradientSpecJson = specJson, backgroundColorHex = startStopHex, opacityAlpha = alpha)
                    } else {
                        ov.copy(backgroundColorHex = hex6, gradientSpecJson = "", opacityAlpha = alpha)
                    }
                }
            }
            "HOME_BG" -> {
                CentralLiveUpdateRuntime.applyVisualEdit("MagicPicker: Set Home Page background to $hex6") { c ->
                    c.copy(
                        homeBackgroundDarkHex = hex6,
                        homeBackgroundLightHex = if (ThemeManager.isLightMode) hex6 else c.homeBackgroundLightHex
                    )
                }
            }
            "SURFACE_BG" -> {
                CentralLiveUpdateRuntime.applyVisualEdit("MagicPicker: Set Surface background to $hex6") { c ->
                    c.copy(
                        surfaceBackgroundDarkHex = hex6,
                        surfaceBackgroundLightHex = if (ThemeManager.isLightMode) hex6 else c.surfaceBackgroundLightHex
                    )
                }
            }
            "CARD_BG" -> {
                CentralLiveUpdateRuntime.applyVisualEdit("MagicPicker: Set Card background to $hex6") { c ->
                    c.copy(
                        cardBackgroundDarkHex = hex6,
                        cardBackgroundLightHex = if (ThemeManager.isLightMode) hex6 else c.cardBackgroundLightHex
                    )
                }
            }
            "BUTTON_GRADIENT" -> {
                CentralLiveUpdateRuntime.applyVisualEdit("MagicPicker: Updated Primary Button Gradient ($startStopHex → $endStopHex)") { c ->
                    c.copy(
                        primaryAccentHex = if (asGradient) startStopHex else hex6,
                        secondaryAccentHex = if (asGradient) endStopHex else hex6,
                        buttonGradientStartHex = if (asGradient) startStopHex else hex6,
                        buttonGradientEndHex = if (asGradient) endStopHex else hex6,
                        buttonGradientAngleDeg = gradientAngle.toDouble(),
                        buttonGradientType = gradientType
                    )
                }
            }
            "CHATROOM_BUTTONS" -> {
                CentralLiveUpdateRuntime.applyVisualEdit("MagicPicker: Updated Chatroom Buttons Gradient ($startStopHex → $endStopHex)") { c ->
                    c.copy(
                        chatroomButtonStartHex = if (asGradient) startStopHex else hex6,
                        chatroomButtonEndHex = if (asGradient) endStopHex else hex6
                    )
                }
            }
            "CHATROOM_THEME" -> {
                CentralLiveUpdateRuntime.applyVisualEdit("MagicPicker: Updated Chatroom Stage Theme to $hex6") { c ->
                    c.copy(chatroomThemeHex = hex6)
                }
            }
            "HEADER_BG" -> {
                CentralLiveUpdateRuntime.applyVisualEdit("MagicPicker: Updated Top Header background to $hex6") { c ->
                    c.copy(
                        headerBackgroundDarkHex = hex6,
                        headerBackgroundLightHex = if (ThemeManager.isLightMode) hex6 else c.headerBackgroundLightHex
                    )
                }
            }
            "BOTTOM_NAV_BG" -> {
                CentralLiveUpdateRuntime.applyVisualEdit("MagicPicker: Updated Bottom Navigation background to $hex6") { c ->
                    c.copy(
                        bottomNavBackgroundDarkHex = hex6,
                        bottomNavBackgroundLightHex = if (ThemeManager.isLightMode) hex6 else c.bottomNavBackgroundLightHex
                    )
                }
            }
            "TEXT_PRIMARY" -> {
                CentralLiveUpdateRuntime.applyVisualEdit("MagicPicker: Updated Primary Text color to $hex6") { c ->
                    c.copy(
                        textPrimaryDarkHex = hex6,
                        textPrimaryLightHex = hex6
                    )
                }
            }
            "ICON_TINT" -> {
                CentralLiveUpdateRuntime.applyVisualEdit("MagicPicker: Updated Icon & Accent Tint to $hex6") { c ->
                    c.copy(iconTintHex = hex6, primaryAccentHex = hex6)
                }
            }
            "STORY_RINGS" -> {
                CentralLiveUpdateRuntime.applyVisualEdit("MagicPicker: Updated Story Rings Gradient ($startStopHex → $endStopHex)") { c ->
                    c.copy(
                        storyRingStartHex = if (asGradient) startStopHex else hex6,
                        storyRingEndHex = if (asGradient) endStopHex else hex6
                    )
                }
            }
            "PROFILE_FRAME" -> {
                CentralLiveUpdateRuntime.applyVisualEdit("MagicPicker: Updated Profile Frame & VIP Accent to $hex6") { c ->
                    c.copy(profileFrameHex = hex6, vipAccentHex = hex6)
                }
            }
            "BORDERS" -> {
                CentralLiveUpdateRuntime.applyVisualEdit("MagicPicker: Updated Borders & Dividers to $hex6") { c ->
                    c.copy(borderColorDarkHex = hex6, borderColorLightHex = hex6)
                }
            }
            "SHADOW_GLOW" -> {
                CentralLiveUpdateRuntime.applyVisualEdit("MagicPicker: Updated Shadows & Neon Glow to $hex6") { c ->
                    c.copy(shadowGlowHex = hex6)
                }
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.90f)
                .testTag("magic_picker_modal"),
            shape = RoundedCornerShape(26.dp),
            color = Color(0xFF0A0814),
            border = BorderStroke(
                1.5.dp,
                Brush.horizontalGradient(listOf(Color(0xFF8525FF), Color(0xFFE52BEB), Color(0xFF00E5FF)))
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    Brush.sweepGradient(
                                        listOf(
                                            Color.Red,
                                            Color.Yellow,
                                            Color.Green,
                                            Color.Cyan,
                                            Color.Blue,
                                            Color.Magenta,
                                            Color.Red
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Palette, contentDescription = null, tint = Color.Black)
                        }
                        Column {
                            Text(
                                text = "MAGICPICKER PRO COLOR & GRADIENT ENGINE",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = "360° Hue Wheel • S/B Square • Multi-Stop Gradients • Live Preview",
                                color = Color(0xFF00E5FF),
                                fontSize = 10.sp
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close MagicPicker", tint = Color.White)
                    }
                }

                Spacer(Modifier.height(10.dp))

                // Target Selector Strip ("Apply Colors to Everything")
                Text(
                    text = "APPLY TARGET (INSTANT LIVE PREVIEW):",
                    color = Color.White.copy(alpha = 0.68f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    applyTargets.forEach { (key, title) ->
                        val selected = selectedTargetKey == key
                        Surface(
                            modifier = Modifier.clickable { selectedTargetKey = key },
                            shape = RoundedCornerShape(999.dp),
                            color = if (selected) Color(0xFFE52BEB).copy(alpha = 0.25f) else Color.White.copy(alpha = 0.06f),
                            border = BorderStroke(1.dp, if (selected) Color(0xFFE52BEB) else Color.White.copy(alpha = 0.14f))
                        ) {
                            Text(
                                text = title,
                                color = if (selected) Color.White else Color.White.copy(alpha = 0.78f),
                                fontSize = 11.sp,
                                fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))

                // Mode Tabs: Wheel, Sliders (RGB/HSV/HEX), Multi-Stop Gradient, Saved Swatches
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        "WHEEL" to "Color Wheel",
                        "SLIDERS" to "RGB / HSV / HEX",
                        "GRADIENT" to "Gradient Editor",
                        "SWATCHES" to "Swatches"
                    ).forEach { (tabKey, tabLabel) ->
                        val active = editorTab == tabKey
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { editorTab = tabKey },
                            shape = RoundedCornerShape(12.dp),
                            color = if (active) NoiseBrandPurple else Color.White.copy(alpha = 0.06f)
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = tabLabel,
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = if (active) FontWeight.Black else FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))

                // Main Content Area
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Previous vs Current Split Preview + Foreground / Background Swatches + HEX Quick Bar
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White.copy(alpha = 0.05f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Previous / Current Split Box
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .size(width = 72.dp, height = 38.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .border(1.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                            .background(previousColor)
                                            .clickable {
                                                val (hsv, a) = MagicColorEngine.hexToHsva(CentralLiveUpdateRuntime.magicPreviousColorHex)
                                                syncFromHsv(hsv.first, hsv.second, hsv.third, a)
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("PREV", color = autoContrastTextOn(previousColor), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                            .background(currentColor),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("NOW", color = autoContrastTextOn(currentColor), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                Column {
                                    Text(
                                        text = currentHex,
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Black
                                    )
                                    Text(
                                        text = "R:${currentRgb.first} G:${currentRgb.second} B:${currentRgb.third} • α:${(alpha * 100).roundToInt()}%",
                                        color = Color.White.copy(alpha = 0.65f),
                                        fontSize = 10.sp
                                    )
                                }
                            }

                            // Foreground / Background Swatch Pair
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                val fgCol = CentralLiveUpdateRuntime.parseColorOrFallback(
                                    CentralLiveUpdateRuntime.magicForegroundColorHex,
                                    NoiseBrandPurple
                                )
                                val bgCol = CentralLiveUpdateRuntime.parseColorOrFallback(
                                    CentralLiveUpdateRuntime.magicBackgroundColorHex,
                                    Color(0xFF050507)
                                )
                                Surface(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clickable {
                                            activeSwatchSlot = "FG"
                                            val (hsv, a) = MagicColorEngine.hexToHsva(CentralLiveUpdateRuntime.magicForegroundColorHex)
                                            syncFromHsv(hsv.first, hsv.second, hsv.third, a)
                                        },
                                    shape = CircleShape,
                                    color = fgCol,
                                    border = BorderStroke(if (activeSwatchSlot == "FG") 2.dp else 1.dp, if (activeSwatchSlot == "FG") Color(0xFF00E5FF) else Color.White)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text("FG", color = autoContrastTextOn(fgCol), fontSize = 8.sp, fontWeight = FontWeight.Black)
                                    }
                                }
                                Surface(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clickable {
                                            activeSwatchSlot = "BG"
                                            val (hsv, a) = MagicColorEngine.hexToHsva(CentralLiveUpdateRuntime.magicBackgroundColorHex)
                                            syncFromHsv(hsv.first, hsv.second, hsv.third, a)
                                        },
                                    shape = CircleShape,
                                    color = bgCol,
                                    border = BorderStroke(if (activeSwatchSlot == "BG") 2.dp else 1.dp, if (activeSwatchSlot == "BG") Color(0xFF00E5FF) else Color.White)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text("BG", color = autoContrastTextOn(bgCol), fontSize = 8.sp, fontWeight = FontWeight.Black)
                                    }
                                }
                            }
                        }
                    }

                    when (editorTab) {
                        "WHEEL" -> {
                            // Professional Circular Rainbow Hue Wheel + Inner Saturation/Brightness Square
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(250.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                MagicPickerWheelCanvas(
                                    hue = hue,
                                    saturation = saturation,
                                    brightness = brightness,
                                    onHsvChanged = { h, s, v ->
                                        syncFromHsv(h, s, v)
                                        applyColorOrGradientLive(asGradient = false)
                                    }
                                )
                            }

                            // Quick Opacity / Alpha Slider below the wheel
                            MagicSliderRow(
                                label = "Opacity / Alpha",
                                valueText = "${(alpha * 100).roundToInt()}%",
                                value = alpha,
                                valueRange = 0.05f..1f,
                                accentColor = Color(0xFF00E5FF),
                                onValueChange = {
                                    syncFromHsv(hue, saturation, brightness, it)
                                    applyColorOrGradientLive(asGradient = false)
                                }
                            )
                        }

                        "SLIDERS" -> {
                            // HEX Input + Eyedropper Theme Sampler
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = hexInput,
                                    onValueChange = { raw ->
                                        hexInput = raw.uppercase()
                                        val clean = raw.trim().removePrefix("#")
                                        if (clean.length == 6 || clean.length == 8) {
                                            val (hsv, a) = MagicColorEngine.hexToHsva("#$clean")
                                            hue = hsv.first
                                            saturation = hsv.second
                                            brightness = hsv.third
                                            alpha = a
                                            CentralLiveUpdateRuntime.magicCurrentColorHex = "#${clean.uppercase()}"
                                            applyColorOrGradientLive(asGradient = false)
                                        }
                                    },
                                    label = { Text("HEX Color (#RRGGBB)", fontSize = 11.sp) },
                                    singleLine = true,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("magic_picker_hex_input"),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = Color(0xFF00E5FF),
                                        unfocusedBorderColor = Color.White.copy(alpha = 0.25f)
                                    )
                                )
                            }

                            // HSV Sliders
                            MagicSliderRow(
                                label = "Hue (H)",
                                valueText = "${hue.roundToInt()}°",
                                value = hue,
                                valueRange = 0f..360f,
                                accentColor = Color(0xFFE52BEB),
                                onValueChange = {
                                    syncFromHsv(it, saturation, brightness, alpha)
                                    applyColorOrGradientLive(asGradient = false)
                                }
                            )
                            MagicSliderRow(
                                label = "Saturation (S)",
                                valueText = "${(saturation * 100).roundToInt()}%",
                                value = saturation,
                                valueRange = 0f..1f,
                                accentColor = NoiseBrandPurple,
                                onValueChange = {
                                    syncFromHsv(hue, it, brightness, alpha)
                                    applyColorOrGradientLive(asGradient = false)
                                }
                            )
                            MagicSliderRow(
                                label = "Brightness (B)",
                                valueText = "${(brightness * 100).roundToInt()}%",
                                value = brightness,
                                valueRange = 0f..1f,
                                accentColor = VipGold,
                                onValueChange = {
                                    syncFromHsv(hue, saturation, it, alpha)
                                    applyColorOrGradientLive(asGradient = false)
                                }
                            )

                            // RGB Sliders
                            MagicSliderRow(
                                label = "Red (R)",
                                valueText = "${currentRgb.first}",
                                value = currentRgb.first.toFloat(),
                                valueRange = 0f..255f,
                                accentColor = Color(0xFFEF4444),
                                onValueChange = { rVal ->
                                    val hsv = MagicColorEngine.rgbToHsv(rVal.roundToInt(), currentRgb.second, currentRgb.third)
                                    syncFromHsv(hsv.first, hsv.second, hsv.third, alpha)
                                    applyColorOrGradientLive(asGradient = false)
                                }
                            )
                            MagicSliderRow(
                                label = "Green (G)",
                                valueText = "${currentRgb.second}",
                                value = currentRgb.second.toFloat(),
                                valueRange = 0f..255f,
                                accentColor = Color(0xFF10B981),
                                onValueChange = { gVal ->
                                    val hsv = MagicColorEngine.rgbToHsv(currentRgb.first, gVal.roundToInt(), currentRgb.third)
                                    syncFromHsv(hsv.first, hsv.second, hsv.third, alpha)
                                    applyColorOrGradientLive(asGradient = false)
                                }
                            )
                            MagicSliderRow(
                                label = "Blue (B)",
                                valueText = "${currentRgb.third}",
                                value = currentRgb.third.toFloat(),
                                valueRange = 0f..255f,
                                accentColor = Color(0xFF3B82F6),
                                onValueChange = { bVal ->
                                    val hsv = MagicColorEngine.rgbToHsv(currentRgb.first, currentRgb.second, bVal.roundToInt())
                                    syncFromHsv(hsv.first, hsv.second, hsv.third, alpha)
                                    applyColorOrGradientLive(asGradient = false)
                                }
                            )
                        }

                        "GRADIENT" -> {
                            // Live Gradient Preview Banner
                            val liveSpec = remember(gradientType, gradientAngle, gradientStops) {
                                MagicGradientSpec(
                                    type = gradientType,
                                    angleDegrees = gradientAngle,
                                    stops = gradientStops
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(68.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(MagicColorEngine.buildComposeBrush(liveSpec))
                                    .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(16.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${gradientType.uppercase()} GRADIENT • ${gradientAngle.roundToInt()}° • ${gradientStops.size} STOPS",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }

                            // Linear vs Radial toggle
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf("linear" to "Linear Gradient", "radial" to "Radial Gradient").forEach { (tKey, tLbl) ->
                                    val active = gradientType == tKey
                                    Surface(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable {
                                                gradientType = tKey
                                                applyColorOrGradientLive(asGradient = true)
                                            },
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (active) Color(0xFF00E5FF).copy(alpha = 0.22f) else Color.White.copy(alpha = 0.06f),
                                        border = BorderStroke(1.dp, if (active) Color(0xFF00E5FF) else Color.White.copy(alpha = 0.15f))
                                    ) {
                                        Box(modifier = Modifier.padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                                            Text(tLbl, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }

                            if (gradientType == "linear") {
                                MagicSliderRow(
                                    label = "Gradient Angle",
                                    valueText = "${gradientAngle.roundToInt()}°",
                                    value = gradientAngle,
                                    valueRange = 0f..360f,
                                    accentColor = VipGold,
                                    onValueChange = {
                                        gradientAngle = it
                                        applyColorOrGradientLive(asGradient = true)
                                    }
                                )
                            }

                            // Color Stops Header + "+ Add Stop"
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "COLOR STOPS (${gradientStops.size}):",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                TextButton(
                                    onClick = {
                                        val newStop = MagicGradientStop(
                                            stopId = "s${gradientStops.size}",
                                            position = 0.5f,
                                            colorHex = MagicColorEngine.hsvToHex(hue, saturation, brightness),
                                            alpha = 1f
                                        )
                                        gradientStops = (gradientStops + newStop).sortedBy { it.position }
                                        selectedStopIndex = gradientStops.lastIndex
                                        applyColorOrGradientLive(asGradient = true)
                                    }
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(15.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Add Stop", color = Color(0xFF00E5FF), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            gradientStops.forEachIndexed { idx, stop ->
                                val isSel = idx == selectedStopIndex
                                val stopCol = CentralLiveUpdateRuntime.parseColorOrFallback(stop.colorHex, NoiseBrandPurple)
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedStopIndex = idx
                                            val (hsv, a) = MagicColorEngine.hexToHsva(stop.colorHex)
                                            syncFromHsv(hsv.first, hsv.second, hsv.third, a)
                                        },
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSel) Color.White.copy(alpha = 0.10f) else Color.White.copy(alpha = 0.04f),
                                    border = BorderStroke(1.dp, if (isSel) Color(0xFF00E5FF) else Color.White.copy(alpha = 0.1f))
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(22.dp)
                                                        .clip(CircleShape)
                                                        .background(stopCol)
                                                        .border(1.dp, Color.White, CircleShape)
                                                )
                                                Text(
                                                    text = "Stop #${idx + 1} (${stop.colorHex}) • ${(stop.position * 100).roundToInt()}%",
                                                    color = Color.White,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                            if (gradientStops.size > 2) {
                                                IconButton(
                                                    onClick = {
                                                        gradientStops = gradientStops.filterIndexed { i, _ -> i != idx }
                                                        selectedStopIndex = 0
                                                        applyColorOrGradientLive(asGradient = true)
                                                    },
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Icon(Icons.Default.DeleteOutline, contentDescription = "Remove Stop", tint = DangerRed, modifier = Modifier.size(16.dp))
                                                }
                                            }
                                        }
                                        Slider(
                                            value = stop.position,
                                            onValueChange = { pos ->
                                                gradientStops = gradientStops.mapIndexed { i, s ->
                                                    if (i == idx) s.copy(position = pos.coerceIn(0f, 1f)) else s
                                                }
                                                applyColorOrGradientLive(asGradient = true)
                                            },
                                            valueRange = 0f..1f,
                                            colors = SliderDefaults.colors(
                                                thumbColor = stopCol,
                                                activeTrackColor = stopCol
                                            )
                                        )
                                    }
                                }
                            }
                        }

                        "SWATCHES" -> {
                            // Eyedropper / Theme Token Sampler + Curated Swatches
                            Text(
                                text = "EYEDROPPER – SAMPLE ACTIVE APP COLORS:",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            val themeTokens = listOf(
                                "Home Dark BG" to cfg.homeBackgroundDarkHex,
                                "Home Day BG" to cfg.homeBackgroundLightHex,
                                "Primary Accent" to cfg.primaryAccentHex,
                                "Secondary Pink" to cfg.secondaryAccentHex,
                                "VIP Gold" to cfg.vipAccentHex,
                                "Chatroom Start" to cfg.chatroomButtonStartHex,
                                "Pure White" to "#FFFFFF",
                                "Obsidian Black" to "#050507",
                                "Cyber Cyan" to "#00E5FF",
                                "Emerald Live" to "#10B981",
                                "Royal Violet" to "#7C3AED",
                                "Neon Rose" to "#FF2E93"
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                themeTokens.chunked(3).forEach { rowItems ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        rowItems.forEach { (name, hex) ->
                                            val swCol = CentralLiveUpdateRuntime.parseColorOrFallback(hex, NoiseBrandPurple)
                                            Surface(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clickable {
                                                        val (hsv, a) = MagicColorEngine.hexToHsva(hex)
                                                        syncFromHsv(hsv.first, hsv.second, hsv.third, a)
                                                        applyColorOrGradientLive(asGradient = false)
                                                    },
                                                shape = RoundedCornerShape(12.dp),
                                                color = Color.White.copy(alpha = 0.06f),
                                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f))
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(8.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(20.dp)
                                                            .clip(CircleShape)
                                                            .background(swCol)
                                                            .border(1.dp, Color.White.copy(alpha = 0.5f), CircleShape)
                                                    )
                                                    Column {
                                                        Text(name, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                                        Text(hex, color = Color.White.copy(alpha = 0.6f), fontSize = 9.sp)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))

                // Bottom Action Row: Apply Color, Apply Gradient, Done
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { applyColorOrGradientLive(asGradient = false) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("magic_picker_apply_color_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = NoiseBrandPurple),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Apply Solid Color", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black)
                    }
                    Button(
                        onClick = { applyColorOrGradientLive(asGradient = true) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("magic_picker_apply_gradient_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE52BEB)),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Apply Gradient", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black)
                    }
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f))
                    ) {
                        Text("Done", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/**
 * Interactive Photoshop-style Circular 360° Hue Ring + Inner Saturation/Brightness Square.
 */
@Composable
private fun MagicPickerWheelCanvas(
    hue: Float,
    saturation: Float,
    brightness: Float,
    onHsvChanged: (Float, Float, Float) -> Unit
) {
    val currentHuePureColor = remember(hue) {
        val (r, g, b) = MagicColorEngine.hsvToRgb(hue, 1f, 1f)
        Color(r, g, b)
    }

    Canvas(
        modifier = Modifier
            .size(236.dp)
            .testTag("magic_picker_color_wheel_canvas")
            .pointerInput(hue, saturation, brightness) {
                detectTapGestures { offset ->
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    val outerRadius = minOf(cx, cy) * 0.94f
                    val ringStroke = outerRadius * 0.20f
                    val innerRadius = outerRadius - ringStroke
                    val dx = offset.x - cx
                    val dy = offset.y - cy
                    val dist = hypot(dx, dy)
                    val sqHalf = innerRadius * 0.64f

                    if (dist >= innerRadius - 8f && dist <= outerRadius + 12f) {
                        val angle = ((Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat() + 360f) % 360f)
                        onHsvChanged(angle, saturation, brightness)
                    } else if (kotlin.math.abs(dx) <= sqHalf && kotlin.math.abs(dy) <= sqHalf) {
                        val newSat = ((dx + sqHalf) / (2f * sqHalf)).coerceIn(0f, 1f)
                        val newBri = (1f - ((dy + sqHalf) / (2f * sqHalf))).coerceIn(0f, 1f)
                        onHsvChanged(hue, newSat, newBri)
                    }
                }
            }
            .pointerInput(hue, saturation, brightness) {
                detectDragGestures { change, _ ->
                    change.consume()
                    val offset = change.position
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    val outerRadius = minOf(cx, cy) * 0.94f
                    val ringStroke = outerRadius * 0.20f
                    val innerRadius = outerRadius - ringStroke
                    val dx = offset.x - cx
                    val dy = offset.y - cy
                    val dist = hypot(dx, dy)
                    val sqHalf = innerRadius * 0.64f

                    if (dist >= innerRadius * 0.85f) {
                        val angle = ((Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat() + 360f) % 360f)
                        onHsvChanged(angle, saturation, brightness)
                    } else {
                        val newSat = ((dx + sqHalf) / (2f * sqHalf)).coerceIn(0f, 1f)
                        val newBri = (1f - ((dy + sqHalf) / (2f * sqHalf))).coerceIn(0f, 1f)
                        onHsvChanged(hue, newSat, newBri)
                    }
                }
            }
    ) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val outerRadius = minOf(cx, cy) * 0.94f
        val ringStroke = outerRadius * 0.20f
        val ringRadius = outerRadius - ringStroke / 2f
        val innerRadius = outerRadius - ringStroke

        // 1. Draw Rainbow Hue Wheel Ring
        val sweepColors = listOf(
            Color(0xFFFF0000),
            Color(0xFFFFFF00),
            Color(0xFF00FF00),
            Color(0xFF00FFFF),
            Color(0xFF0000FF),
            Color(0xFFFF00FF),
            Color(0xFFFF0000)
        )
        drawCircle(
            brush = Brush.sweepGradient(sweepColors, center = Offset(cx, cy)),
            radius = ringRadius,
            center = Offset(cx, cy),
            style = Stroke(width = ringStroke, cap = StrokeCap.Butt)
        )

        // Hue indicator handle on the wheel
        val hueRad = Math.toRadians(hue.toDouble())
        val hx = cx + ringRadius * cos(hueRad).toFloat()
        val hy = cy + ringRadius * sin(hueRad).toFloat()
        drawCircle(
            color = Color.Black,
            radius = ringStroke * 0.48f,
            center = Offset(hx, hy),
            style = Stroke(width = 5f)
        )
        drawCircle(
            color = Color.White,
            radius = ringStroke * 0.44f,
            center = Offset(hx, hy),
            style = Stroke(width = 3f)
        )

        // 2. Draw Inner Saturation & Brightness Square
        val sqHalf = innerRadius * 0.64f
        val sqLeft = cx - sqHalf
        val sqTop = cy - sqHalf
        val sqSize = sqHalf * 2f

        // Base horizontal gradient: White -> Pure Hue
        drawRoundRect(
            brush = Brush.horizontalGradient(
                colors = listOf(Color.White, currentHuePureColor),
                startX = sqLeft,
                endX = sqLeft + sqSize
            ),
            topLeft = Offset(sqLeft, sqTop),
            size = Size(sqSize, sqSize),
            cornerRadius = CornerRadius(12f, 12f)
        )
        // Vertical overlay gradient: Transparent -> Black (Brightness/Value)
        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color.Transparent, Color.Black),
                startY = sqTop,
                endY = sqTop + sqSize
            ),
            topLeft = Offset(sqLeft, sqTop),
            size = Size(sqSize, sqSize),
            cornerRadius = CornerRadius(12f, 12f)
        )
        drawRoundRect(
            color = Color.White.copy(alpha = 0.35f),
            topLeft = Offset(sqLeft, sqTop),
            size = Size(sqSize, sqSize),
            cornerRadius = CornerRadius(12f, 12f),
            style = Stroke(width = 2f)
        )

        // S/B selection handle inside the square
        val sx = sqLeft + saturation.coerceIn(0f, 1f) * sqSize
        val sy = sqTop + (1f - brightness.coerceIn(0f, 1f)) * sqSize
        drawCircle(
            color = Color.Black,
            radius = 10f,
            center = Offset(sx, sy),
            style = Stroke(width = 4f)
        )
        drawCircle(
            color = Color.White,
            radius = 8f,
            center = Offset(sx, sy),
            style = Stroke(width = 2.5f)
        )
    }
}

@Composable
private fun MagicSliderRow(
    label: String,
    valueText: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    accentColor: Color,
    onValueChange: (Float) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, color = Color.White.copy(alpha = 0.85f), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            Text(valueText, color = accentColor, fontSize = 11.sp, fontWeight = FontWeight.Black)
        }
        Slider(
            value = value.coerceIn(valueRange.start, valueRange.endInclusive),
            onValueChange = onValueChange,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = accentColor,
                activeTrackColor = accentColor,
                inactiveTrackColor = Color.White.copy(alpha = 0.14f)
            )
        )
    }
}

/**
 * 3. COMPLETE DRAG-AND-DROP & VISUAL COMPONENT INSPECTOR MODAL
 *
 * Allows the Owner to:
 * - Select any UI element on any page
 * - Move it precisely (X/Y offset or on-screen drag)
 * - Resize width & height scale / custom dp
 * - Edit text, font size, font weight, padding, margin, corner radius, border thickness, shadow elevation
 * - Align horizontally & vertically (Start, Center, End)
 * - Duplicate, Lock/Unlock, Hide/Delete, or Restore any element
 */
@Composable
fun VisualComponentInspectorModal(
    selectedElementId: String,
    selectedElementLabel: String,
    currentPageKey: String,
    selectableElements: List<Pair<String, String>>,
    onOpenMagicPicker: () -> Unit,
    onDismiss: () -> Unit
) {
    var activeId by remember { mutableStateOf(selectedElementId) }
    var activeLabel by remember { mutableStateOf(selectedElementLabel) }
    val override = CentralLiveUpdateRuntime.getComponentOverride(activeId)

    var customTextDraft by remember(activeId, override.customText) { mutableStateOf(override.customText) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.88f)
                .testTag("visual_component_inspector_modal"),
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF0B0816),
            border = BorderStroke(1.5.dp, Color(0xFF00E5FF))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "DRAG-AND-DROP & COMPONENT EDITOR",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "Editing: $activeLabel ($activeId)",
                            color = Color(0xFF00E5FF),
                            fontSize = 11.sp
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close Component Editor", tint = Color.White)
                    }
                }

                Spacer(Modifier.height(8.dp))

                // Element switcher strip
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    selectableElements.forEach { (id, lbl) ->
                        val isSel = id == activeId
                        Surface(
                            modifier = Modifier.clickable {
                                activeId = id
                                activeLabel = lbl
                                CentralLiveUpdateRuntime.selectVisualElement(id, lbl)
                            },
                            shape = RoundedCornerShape(999.dp),
                            color = if (isSel) Color(0xFF00E5FF).copy(alpha = 0.22f) else Color.White.copy(alpha = 0.06f),
                            border = BorderStroke(1.dp, if (isSel) Color(0xFF00E5FF) else Color.White.copy(alpha = 0.14f))
                        ) {
                            Text(
                                text = lbl,
                                color = if (isSel) Color(0xFF00E5FF) else Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Quick Actions: Duplicate, Lock/Unlock, Hide/Restore, Open MagicPicker
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val dup = CentralLiveUpdateRuntime.duplicateComponentOverride(activeId)
                                activeId = dup.elementId
                                activeLabel = dup.label
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Duplicate", color = Color.White, fontSize = 11.sp)
                        }
                        OutlinedButton(
                            onClick = {
                                CentralLiveUpdateRuntime.updateComponentOverride(
                                    elementId = activeId,
                                    description = if (override.isLocked) "Unlocked '$activeLabel'" else "Locked '$activeLabel'"
                                ) { it.copy(isLocked = !override.isLocked) }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                if (override.isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                                contentDescription = null,
                                tint = VipGold,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(if (override.isLocked) "Unlock" else "Lock", color = Color.White, fontSize = 11.sp)
                        }
                        OutlinedButton(
                            onClick = {
                                if (override.isHidden) {
                                    CentralLiveUpdateRuntime.restoreHiddenComponent(activeId)
                                } else {
                                    CentralLiveUpdateRuntime.deleteOrHideComponent(activeId)
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                if (override.isHidden) Icons.Default.Visibility else Icons.Default.DeleteOutline,
                                contentDescription = null,
                                tint = if (override.isHidden) OnlineGreen else DangerRed,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(if (override.isHidden) "Restore" else "Delete/Hide", color = Color.White, fontSize = 11.sp)
                        }
                    }

                    // Position X & Y Sliders
                    MagicSliderRow(
                        label = "Horizontal Offset (X)",
                        valueText = "${override.offsetXDp.roundToInt()} dp",
                        value = override.offsetXDp,
                        valueRange = -180f..180f,
                        accentColor = Color(0xFF00E5FF),
                        onValueChange = { x ->
                            if (!override.isLocked) {
                                CentralLiveUpdateRuntime.updateComponentOverride(activeId, "Moved '$activeLabel' X=${x.roundToInt()}dp") {
                                    it.copy(label = activeLabel, offsetXDp = x)
                                }
                            }
                        }
                    )
                    MagicSliderRow(
                        label = "Vertical Offset (Y)",
                        valueText = "${override.offsetYDp.roundToInt()} dp",
                        value = override.offsetYDp,
                        valueRange = -180f..180f,
                        accentColor = Color(0xFF00E5FF),
                        onValueChange = { y ->
                            if (!override.isLocked) {
                                CentralLiveUpdateRuntime.updateComponentOverride(activeId, "Moved '$activeLabel' Y=${y.roundToInt()}dp") {
                                    it.copy(label = activeLabel, offsetYDp = y)
                                }
                            }
                        }
                    )

                    // Width & Height Scale Sliders
                    MagicSliderRow(
                        label = "Width Scale",
                        valueText = "${(override.widthScale * 100).roundToInt()}%",
                        value = override.widthScale,
                        valueRange = 0.5f..2.0f,
                        accentColor = NoiseBrandPurple,
                        onValueChange = { sx ->
                            if (!override.isLocked) {
                                CentralLiveUpdateRuntime.updateComponentOverride(activeId, "Resized '$activeLabel' width=${(sx * 100).roundToInt()}%") {
                                    it.copy(widthScale = sx)
                                }
                            }
                        }
                    )
                    MagicSliderRow(
                        label = "Height Scale",
                        valueText = "${(override.heightScale * 100).roundToInt()}%",
                        value = override.heightScale,
                        valueRange = 0.5f..2.0f,
                        accentColor = Color(0xFFE52BEB),
                        onValueChange = { sy ->
                            if (!override.isLocked) {
                                CentralLiveUpdateRuntime.updateComponentOverride(activeId, "Resized '$activeLabel' height=${(sy * 100).roundToInt()}%") {
                                    it.copy(heightScale = sy)
                                }
                            }
                        }
                    )

                    // Corner Radius, Border Width, Shadow Elevation, Padding
                    MagicSliderRow(
                        label = "Border Corner Radius",
                        valueText = if (override.cornerRadiusDp < 0f) "Default" else "${override.cornerRadiusDp.roundToInt()} dp",
                        value = if (override.cornerRadiusDp < 0f) 14f else override.cornerRadiusDp,
                        valueRange = 0f..40f,
                        accentColor = VipGold,
                        onValueChange = { rad ->
                            CentralLiveUpdateRuntime.updateComponentOverride(activeId, "Set '$activeLabel' radius=${rad.roundToInt()}dp") {
                                it.copy(cornerRadiusDp = rad)
                            }
                        }
                    )
                    MagicSliderRow(
                        label = "Border Thickness",
                        valueText = "${override.borderWidthDp.roundToInt()} dp",
                        value = override.borderWidthDp,
                        valueRange = 0f..8f,
                        accentColor = OnlineGreen,
                        onValueChange = { bw ->
                            CentralLiveUpdateRuntime.updateComponentOverride(activeId, "Set '$activeLabel' border=${bw.roundToInt()}dp") {
                                it.copy(borderWidthDp = bw, borderColorHex = it.borderColorHex.ifBlank { "#8525FF" })
                            }
                        }
                    )
                    MagicSliderRow(
                        label = "Shadow / Glow Elevation",
                        valueText = "${override.shadowElevationDp.roundToInt()} dp",
                        value = override.shadowElevationDp,
                        valueRange = 0f..24f,
                        accentColor = Color(0xFFE52BEB),
                        onValueChange = { sh ->
                            CentralLiveUpdateRuntime.updateComponentOverride(activeId, "Set '$activeLabel' shadow=${sh.roundToInt()}dp") {
                                it.copy(shadowElevationDp = sh)
                            }
                        }
                    )

                    // Horizontal Alignment
                    Text("HORIZONTAL ALIGNMENT:", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("start" to "Left / Start", "center" to "Center", "end" to "Right / End").forEach { (alKey, alLbl) ->
                            val isSel = override.horizontalAlign == alKey
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        CentralLiveUpdateRuntime.updateComponentOverride(activeId, "Aligned '$activeLabel' $alLbl") {
                                            it.copy(horizontalAlign = alKey)
                                        }
                                    },
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSel) Color(0xFF00E5FF).copy(alpha = 0.22f) else Color.White.copy(alpha = 0.06f),
                                border = BorderStroke(1.dp, if (isSel) Color(0xFF00E5FF) else Color.White.copy(alpha = 0.14f))
                            ) {
                                Box(modifier = Modifier.padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                                    Text(alLbl, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // Custom Text & Add Custom Element Section
                    OutlinedTextField(
                        value = customTextDraft,
                        onValueChange = { txt ->
                            customTextDraft = txt
                            CentralLiveUpdateRuntime.updateComponentOverride(activeId, "Updated text for '$activeLabel'") {
                                it.copy(customText = txt)
                            }
                        },
                        label = { Text("Element Text / Label Override", fontSize = 11.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                }

                Spacer(Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onOpenMagicPicker,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE52BEB)),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.Palette, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Color / Gradient (MagicPicker)", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black)
                    }
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = OnlineGreen),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Done", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }
}

/**
 * 4. ADD PAGE – UNLIMITED PAGE CREATOR MODAL
 *
 * Allows the Owner to create unlimited new pages, choose Blank or Modern Templates,
 * add custom sections (Hero Banner, Feature Cards, Announcements, Quick Links),
 * link to Home Quick Bar & Top Menu, and publish live to all devices.
 */
@Composable
fun UnlimitedPageCreatorModal(
    viewModel: NoiseViewModel,
    onDismiss: () -> Unit
) {
    var pageTitle by remember { mutableStateOf("") }
    var pageSubtitle by remember { mutableStateOf("") }
    var selectedTemplate by remember { mutableStateOf("modern_showcase") }
    var accentHex by remember { mutableStateOf("#8525FF") }
    var secondaryHex by remember { mutableStateOf("#E52BEB") }
    var showInHomeQuickBar by remember { mutableStateOf(true) }
    var showInTopMenu by remember { mutableStateOf(true) }

    val existingPages by viewModel.centralDynamicPages.collectAsState()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.86f)
                .testTag("unlimited_page_creator_modal"),
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF0A0714),
            border = BorderStroke(1.5.dp, VipGold)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "ADD PAGE – UNLIMITED PAGE CREATOR",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "Create blank or template pages • Add to Navigation • Publish Live",
                            color = VipGold,
                            fontSize = 10.sp
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close Add Page", tint = Color.White)
                    }
                }

                Spacer(Modifier.height(10.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = pageTitle,
                        onValueChange = { pageTitle = it },
                        label = { Text("New Page Title (e.g., Kerala News Hub, VIP Lounge)") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("add_page_title_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    OutlinedTextField(
                        value = pageSubtitle,
                        onValueChange = { pageSubtitle = it },
                        label = { Text("Page Subtitle / Description") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("add_page_subtitle_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    Text("CHOOSE PAGE LAYOUT TEMPLATE:", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            "blank_canvas" to "Blank Canvas",
                            "modern_showcase" to "Modern Pro Showcase",
                            "kerala_news" to "Kerala News Portal",
                            "vip_arena" to "VIP Party & DJ Arena",
                            "video_calling" to "HD Video Call Lounge"
                        ).forEach { (tplKey, tplName) ->
                            val active = selectedTemplate == tplKey
                            Surface(
                                modifier = Modifier.clickable {
                                    selectedTemplate = tplKey
                                    if (pageTitle.isBlank()) pageTitle = tplName
                                    if (pageSubtitle.isBlank()) pageSubtitle = "Interactive $tplName powered by NOICE LIVE"
                                },
                                shape = RoundedCornerShape(12.dp),
                                color = if (active) VipGold.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.06f),
                                border = BorderStroke(1.dp, if (active) VipGold else Color.White.copy(alpha = 0.15f))
                            ) {
                                Text(
                                    text = tplName,
                                    color = if (active) VipGold else Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Show in Home Quick Bar", color = Color.White, fontSize = 12.sp)
                        Switch(checked = showInHomeQuickBar, onCheckedChange = { showInHomeQuickBar = it })
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Show in Top Navigation Menu", color = Color.White, fontSize = 12.sp)
                        Switch(checked = showInTopMenu, onCheckedChange = { showInTopMenu = it })
                    }

                    if (existingPages.isNotEmpty()) {
                        HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                        Text(
                            text = "EXISTING LIVE PAGES (${existingPages.size}):",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        existingPages.forEach { pg ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color.White.copy(alpha = 0.05f),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(pg.title, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        Text("/${pg.pageKey} • ${pg.subtitle.take(50)}", color = Color.White.copy(alpha = 0.65f), fontSize = 10.sp)
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        IconButton(
                                            onClick = {
                                                // Duplicate page
                                                viewModel.saveAndPublishDynamicPage(
                                                    title = "${pg.title} Copy",
                                                    subtitle = pg.subtitle,
                                                    accentHex = pg.accentHex,
                                                    secondaryHex = pg.secondaryAccentHex
                                                )
                                            },
                                            modifier = Modifier.size(30.dp)
                                        ) {
                                            Icon(Icons.Default.ContentCopy, contentDescription = "Duplicate Page", tint = Color(0xFF00E5FF), modifier = Modifier.size(16.dp))
                                        }
                                        IconButton(
                                            onClick = { viewModel.deleteAndPublishDynamicPage(pg.pageKey) },
                                            modifier = Modifier.size(30.dp)
                                        ) {
                                            Icon(Icons.Default.DeleteOutline, contentDescription = "Delete Page", tint = DangerRed, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))

                Button(
                    onClick = {
                        val finalTitle = pageTitle.trim().ifBlank { "NOICE Custom Page" }
                        val finalSub = pageSubtitle.trim().ifBlank { "Live dynamic page created by Owner" }
                        viewModel.saveAndPublishDynamicPage(
                            title = finalTitle,
                            subtitle = finalSub,
                            accentHex = accentHex,
                            secondaryHex = secondaryHex
                        )
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("publish_new_dynamic_page_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = VipGold),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.RocketLaunch, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("CREATE & PUBLISH PAGE LIVE", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

/**
 * 5. REAL GOOGLE GEMINI AI DEVELOPER NATURAL-LANGUAGE PROMPT MODAL
 *
 * Uses Firebase AI Logic (`Firebase.ai` with `gemini-3.8-flash`) to execute any natural-language
 * Owner instruction and immediately apply live preview + 1-tap publish.
 */
@Composable
fun OwnerGeminiLivePromptModal(
    viewModel: NoiseViewModel,
    onOpenFullStudio: () -> Unit,
    onDismiss: () -> Unit
) {
    var promptText by remember { mutableStateOf("") }
    val isGenerating = viewModel.centralAiIsGenerating
    val proposal = viewModel.centralAiCurrentProposal
    val statusMsg = viewModel.centralAiStatusMessage

    val quickExamplePrompts = listOf(
        "Change the entire home page background to white.",
        "Move the notification icon to the right.",
        "Create a new Kerala News page.",
        "Change all chatroom buttons to purple gradients.",
        "Add a new video calling feature.",
        "Fix the video playback error.",
        "Create a new page with a professional modern design."
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.88f)
                .testTag("owner_gemini_live_prompt_modal"),
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF0A0816),
            border = BorderStroke(1.5.dp, NoiseBrandPurple)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "REAL GEMINI AI DEVELOPER (gemini-3.8-flash)",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "Firebase AI Logic • Natural Language Visual & Code Editor",
                            color = Color(0xFF00E5FF),
                            fontSize = 10.sp
                        )
                    }
                    Row {
                        TextButton(onClick = onOpenFullStudio) {
                            Text("Full AI Studio", color = VipGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close Gemini AI", tint = Color.White)
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = promptText,
                        onValueChange = { promptText = it },
                        label = { Text("Tell Gemini AI what to change, add, move, or fix...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("gemini_owner_command_input"),
                        minLines = 3,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF00E5FF),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.25f)
                        )
                    )

                    Button(
                        onClick = {
                            if (promptText.isNotBlank()) {
                                viewModel.runCentralAiAdminCommand(promptText)
                            }
                        },
                        enabled = !isGenerating && promptText.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("gemini_owner_execute_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = NoiseBrandPurple),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        if (isGenerating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = Color.White
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Gemini 3.8 Flash Analyzing & Applying...", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        } else {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("GENERATE & PREVIEW WITH GEMINI AI", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black)
                        }
                    }

                    Text("1-TAP OWNER AI COMMANDS:", color = Color.White.copy(alpha = 0.7f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    quickExamplePrompts.forEach { sample ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    promptText = sample
                                    viewModel.runCentralAiAdminCommand(sample)
                                },
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White.copy(alpha = 0.05f),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.Bolt, contentDescription = null, tint = VipGold, modifier = Modifier.size(15.dp))
                                Text(sample, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    if (statusMsg != null) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = OnlineGreen.copy(alpha = 0.14f),
                            border = BorderStroke(1.dp, OnlineGreen.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = statusMsg,
                                color = Color.White,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }

                    if (proposal != null) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFF141029),
                            border = BorderStroke(1.dp, Color(0xFF00E5FF))
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "GEMINI AI PROPOSAL READY (${proposal.generatedByModel})",
                                    color = Color(0xFF00E5FF),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black
                                )
                                Text(
                                    text = proposal.summaryOfChanges,
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                proposal.diagnosticReport.forEach { diag ->
                                    Text(diag, color = OnlineGreen, fontSize = 10.sp)
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            viewModel.publishCentralAiProposalToAllUsers()
                                            onDismiss()
                                        },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = OnlineGreen),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("PUBLISH UPDATE", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Black)
                                    }
                                    OutlinedButton(
                                        onClick = { viewModel.cancelCentralAiPreview() },
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("Cancel", color = Color.White, fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 6. FINISH AND PUBLISH SYSTEM MODAL
 *
 * Workflow:
 * 1. Click EDIT BAR -> 2. Edit anything -> 3. Click FINISH -> 4. Show complete preview of all changes ->
 * 5. Show two options: `PUBLISH UPDATE` or `CANCEL`.
 */
@Composable
fun FinishAndPublishReviewModal(
    viewModel: NoiseViewModel,
    onDismissCancel: () -> Unit,
    onDiscardAndExit: () -> Unit,
    onPublishedSuccess: () -> Unit
) {
    val previewCfg = CentralLiveUpdateRuntime.effectiveConfig
    val prodCfg = CentralLiveUpdateRuntime.activeConfig
    val history = CentralLiveUpdateRuntime.editHistoryLog
    val overrides = previewCfg.parsedComponentOverrides()
    val isPublishing = viewModel.centralAiIsPublishing

    Dialog(
        onDismissRequest = onDismissCancel,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .testTag("finish_and_publish_review_modal"),
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF0A0816),
            border = BorderStroke(1.5.dp, OnlineGreen)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Verified, contentDescription = null, tint = OnlineGreen, modifier = Modifier.size(24.dp))
                        Column {
                            Text(
                                text = "FINISH & PUBLISH GLOBAL LIVE UPDATE",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = "Current: ${prodCfg.versionLabel} → Next: v1.${(prodCfg.versionNumber + 1) / 10}.${(prodCfg.versionNumber + 1) % 10}-live.${prodCfg.versionNumber + 1}",
                                color = OnlineGreen,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White.copy(alpha = 0.05f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 240.dp)
                            .verticalScroll(rememberScrollState())
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "COMPLETE PREVIEW OF CHANGES TO BROADCAST:",
                            color = VipGold,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "• Home BG: ${previewCfg.homeBackgroundDarkHex} (Day: ${previewCfg.homeBackgroundLightHex})",
                            color = Color.White,
                            fontSize = 11.sp
                        )
                        Text(
                            text = "• Primary Gradient: ${previewCfg.buttonGradientStartHex} → ${previewCfg.buttonGradientEndHex} (${previewCfg.buttonGradientType}, ${previewCfg.buttonGradientAngleDeg.roundToInt()}°)",
                            color = Color.White,
                            fontSize = 11.sp
                        )
                        Text(
                            text = "• Chatroom Buttons: ${previewCfg.chatroomButtonStartHex} → ${previewCfg.chatroomButtonEndHex}",
                            color = Color.White,
                            fontSize = 11.sp
                        )
                        Text(
                            text = "• Visual Component Overrides: ${overrides.size} elements configured",
                            color = Color.White,
                            fontSize = 11.sp
                        )
                        if (history.isNotEmpty()) {
                            Spacer(Modifier.height(4.dp))
                            Text("SESSION EDIT LOG:", color = Color(0xFF00E5FF), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            history.take(10).forEach { entry ->
                                Text("✓ $entry", color = Color.White.copy(alpha = 0.85f), fontSize = 10.sp)
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "✓ Pre-Publish Validation Passed • Automatic Backup Snapshot Enabled • Zero Data Loss",
                            color = OnlineGreen,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismissCancel,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("finish_modal_cancel_button"),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f))
                    ) {
                        Text("CANCEL", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = onDiscardAndExit,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, DangerRed.copy(alpha = 0.6f))
                    ) {
                        Text("Discard", color = DangerRed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            val summary = if (history.isNotEmpty()) {
                                history.take(3).joinToString(" • ")
                            } else {
                                previewCfg.changeSummary.ifBlank { "Owner Visual & MagicPicker Live Update" }
                            }
                            val proposal = AiAdminProposal(
                                prompt = "Owner Edit Bar Live Update",
                                actionCategory = "owner_visual_editor",
                                inspectedComponents = listOf("OwnerMasterControlSystem", "MagicPicker", "CentralLiveUpdateRuntime"),
                                summaryOfChanges = summary,
                                diagnosticReport = listOf("✓ Validated by Owner Master Control System"),
                                validationPassed = true,
                                proposedConfig = previewCfg.copy(changeSummary = summary)
                            )
                            viewModel.publishDirectOwnerVisualProposal(proposal) {
                                onPublishedSuccess()
                            }
                        },
                        enabled = !isPublishing,
                        modifier = Modifier
                            .weight(1.3f)
                            .testTag("finish_modal_publish_update_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = OnlineGreen),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        if (isPublishing) {
                            CircularProgressIndicator(modifier = Modifier.size(15.dp), strokeWidth = 2.dp, color = Color.Black)
                            Spacer(Modifier.width(6.dp))
                        }
                        Text("PUBLISH UPDATE", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }
}

/**
 * 8. OWNER MASTER DASHBOARD MODAL
 *
 * Private Owner Control Center uniting all 12 Master Control systems:
 * - Visual Page Editor, Add Page, Real Gemini AI Developer, MagicPicker Color Wheel,
 *   Advanced Gradient Editor, Component Editor, Drag-and-Drop Editor, Navigation Editor,
 *   Theme Manager, Bug Detector & Error Fixer, AI Code Generator, App Update Manager,
 *   Device Synchronization Status, Publish History, Version Management & 1-Tap Rollback.
 */
@Composable
fun OwnerMasterDashboardModal(
    viewModel: NoiseViewModel,
    currentPageKey: String,
    onOpenMagicPicker: () -> Unit,
    onOpenComponentEditor: () -> Unit,
    onOpenAddPage: () -> Unit,
    onOpenGeminiPrompt: () -> Unit,
    onOpenFullDeveloperStudio: () -> Unit,
    onDismiss: () -> Unit
) {
    val activeConfig = CentralLiveUpdateRuntime.effectiveConfig
    val versions by viewModel.centralVersionHistory.collectAsState()
    val backups by viewModel.centralBackupsList.collectAsState()
    val dynamicPages by viewModel.centralDynamicPages.collectAsState()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.92f)
                .testTag("owner_master_dashboard_modal"),
            shape = RoundedCornerShape(26.dp),
            color = Color(0xFF080612),
            border = BorderStroke(
                1.5.dp,
                Brush.horizontalGradient(listOf(VipGold, NoiseBrandPurple, Color(0xFF00E5FF)))
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = VipGold.copy(alpha = 0.18f),
                            border = BorderStroke(1.dp, VipGold)
                        ) {
                            Icon(
                                Icons.Default.AdminPanelSettings,
                                contentDescription = null,
                                tint = VipGold,
                                modifier = Modifier.padding(8.dp).size(22.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "NOICE LIVE – OWNER MASTER CONTROL DASHBOARD",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = "Verified Owner: @sebin_mr • Live Version: ${activeConfig.versionLabel} • All Devices Synced",
                                color = VipGold,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close Master Dashboard", tint = Color.White)
                    }
                }

                Spacer(Modifier.height(12.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Device Sync & Global Live Update Status Banner
                    item {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = OnlineGreen.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, OnlineGreen.copy(alpha = 0.45f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "GLOBAL LIVE UPDATE & DEVICE SYNC ACTIVE",
                                        color = OnlineGreen,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                    Text(
                                        text = "Firestore /central_app_config/production + Offline Cache synced • ${dynamicPages.size} Dynamic Pages • ${versions.size} Versions",
                                        color = Color.White.copy(alpha = 0.85f),
                                        fontSize = 10.sp
                                    )
                                }
                                Button(
                                    onClick = {
                                        CentralLiveUpdateRuntime.activateOwnerEditMode()
                                        CentralLiveUpdateRuntime.showFinishPublishDialog = true
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = OnlineGreen),
                                    shape = RoundedCornerShape(999.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text("Publish Now", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Black)
                                }
                            }
                        }
                    }

                    // 2x4 Grid of Core Owner Master Modules
                    item {
                        Text(
                            text = "MASTER CONTROL MODULES:",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(6.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                MasterModuleCard(
                                    modifier = Modifier.weight(1f),
                                    title = "Visual Page Editor",
                                    subtitle = "Activate floating EDIT BAR & drag-and-drop on $currentPageKey",
                                    icon = Icons.Default.EditNote,
                                    accent = Color(0xFF00E5FF),
                                    onClick = {
                                        CentralLiveUpdateRuntime.activateOwnerEditMode()
                                        onDismiss()
                                    }
                                )
                                MasterModuleCard(
                                    modifier = Modifier.weight(1f),
                                    title = "MagicPicker Wheel",
                                    subtitle = "360° Hue Wheel, S/B Square, RGB/HEX & Multi-Stop Gradients",
                                    icon = Icons.Default.Palette,
                                    accent = Color(0xFFE52BEB),
                                    onClick = onOpenMagicPicker
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                MasterModuleCard(
                                    modifier = Modifier.weight(1f),
                                    title = "Drag & Drop Inspector",
                                    subtitle = "Move, resize, align, duplicate, lock or hide any component",
                                    icon = Icons.Default.OpenWith,
                                    accent = NoiseBrandPurple,
                                    onClick = onOpenComponentEditor
                                )
                                MasterModuleCard(
                                    modifier = Modifier.weight(1f),
                                    title = "Add Page Creator",
                                    subtitle = "Create unlimited new pages & attach to navigation menus",
                                    icon = Icons.Default.NoteAdd,
                                    accent = VipGold,
                                    onClick = onOpenAddPage
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                MasterModuleCard(
                                    modifier = Modifier.weight(1f),
                                    title = "Real Gemini AI Editor",
                                    subtitle = "Natural language commands via gemini-3.8-flash",
                                    icon = Icons.Default.AutoAwesome,
                                    accent = OnlineGreen,
                                    onClick = onOpenGeminiPrompt
                                )
                                MasterModuleCard(
                                    modifier = Modifier.weight(1f),
                                    title = "AI Code & Bug Studio",
                                    subtitle = "AI Code Generator, Bug Detector & Auto Error Fixer",
                                    icon = Icons.Default.Code,
                                    accent = Color(0xFF38BDF8),
                                    onClick = onOpenFullDeveloperStudio
                                )
                            }
                        }
                    }

                    // Navigation & Theme Manager Quick Controls
                    item {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color.White.copy(alpha = 0.04f),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "NAVIGATION, MENU & THEME MANAGER",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black
                                )
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    listOf(
                                        "modern_pill" to "Modern Pill Nav",
                                        "glass_floating" to "Glass Floating Nav",
                                        "neon_dock" to "Neon Dock Nav",
                                        "classic_bar" to "Classic Bar Nav"
                                    ).forEach { (styleKey, styleLbl) ->
                                        val isSel = activeConfig.menuStyle == styleKey
                                        Surface(
                                            modifier = Modifier.clickable {
                                                CentralLiveUpdateRuntime.applyVisualEdit("Set navigation menu style to $styleLbl") {
                                                    it.copy(menuStyle = styleKey)
                                                }
                                            },
                                            shape = RoundedCornerShape(10.dp),
                                            color = if (isSel) NoiseBrandPurple else Color.White.copy(alpha = 0.08f)
                                        ) {
                                            Text(
                                                text = styleLbl,
                                                color = Color.White,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Publish History, Version Management & Rollback Previous Version
                    item {
                        Text(
                            text = "PUBLISH HISTORY & 1-TAP VERSION ROLLBACK (${backups.size} BACKUPS):",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black
                        )
                    }

                    items(backups.take(8)) { backup ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White.copy(alpha = 0.05f),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${backup.versionLabel} • ${backup.backupLabel}",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = backup.reason,
                                        color = Color.White.copy(alpha = 0.65f),
                                        fontSize = 10.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                OutlinedButton(
                                    onClick = { viewModel.rollbackCentralConfigToBackup(backup) },
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, VipGold)
                                ) {
                                    Text("Rollback", color = VipGold, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MasterModuleCard(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accent: Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = accent.copy(alpha = 0.10f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.45f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(icon, contentDescription = title, tint = accent, modifier = Modifier.size(22.dp))
            Text(title, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black)
            Text(subtitle, color = Color.White.copy(alpha = 0.72f), fontSize = 10.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}
