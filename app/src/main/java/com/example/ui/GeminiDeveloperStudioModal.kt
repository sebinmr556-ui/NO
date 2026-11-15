package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.AiCodePatchProposal
import com.example.data.AiDeveloperChatMessageDoc
import com.example.data.AppPerformanceSnapshot
import com.example.data.BugScanFindingDoc
import com.example.data.CentralAppConfig
import com.example.data.CentralConfigBackupDoc
import com.example.data.DeveloperBuildTestReport
import com.example.data.DeveloperDeploymentDoc
import com.example.data.GeminiDeveloperStudioManager
import com.example.data.GitHubBranchDoc
import com.example.data.GitHubCommitDoc
import com.example.data.GitHubIntegrationConfigDoc
import com.example.data.ProjectSourceFileDoc
import com.example.data.SuperOwnerControlManager
import com.example.data.SuperOwnerRegistryDoc
import com.example.data.UserProfile
import com.example.ui.theme.DangerRed
import com.example.ui.theme.NoisePurple
import com.example.ui.theme.OnlineGreen
import com.example.ui.theme.VipGold
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Professional Dark IDE Palette for the Owner-Only Developer Studio
private val DevStudioBg = Color(0xFF07070D)
private val DevStudioSurface = Color(0xFF10111A)
private val DevStudioCard = Color(0xFF161826)
private val DevStudioCodeBg = Color(0xFF0B0C14)
private val DevStudioBorder = Color(0xFF282B40)
private val DevStudioCyan = Color(0xFF00E5FF)
private val DevStudioGreen = Color(0xFF00E676)
private val DevStudioTextSecondary = Color(0xFFA0A4B8)

@Composable
fun GeminiDeveloperStudioModal(
    currentUserProfile: UserProfile,
    registryDoc: SuperOwnerRegistryDoc,
    productionConfig: CentralAppConfig,
    workspaceFiles: List<ProjectSourceFileDoc>,
    chatMessages: List<AiDeveloperChatMessageDoc>,
    currentCodeProposal: AiCodePatchProposal?,
    bugFindings: List<BugScanFindingDoc>,
    buildTestReport: DeveloperBuildTestReport,
    githubConfig: GitHubIntegrationConfigDoc,
    githubBranches: List<GitHubBranchDoc>,
    githubCommits: List<GitHubCommitDoc>,
    deployments: List<DeveloperDeploymentDoc>,
    configBackups: List<CentralConfigBackupDoc>,
    isBusy: Boolean,
    statusMessage: String?,
    onSendChatPrompt: (String) -> Unit,
    onGenerateCodePatch: (String) -> Unit,
    onApproveAndDeployCodePatch: (AiCodePatchProposal) -> Unit,
    onRunBugScanner: () -> Unit,
    onApproveAndDeployBugFix: (BugScanFindingDoc) -> Unit,
    onSaveEditedFileToDevBranch: (String, String, String, String, String) -> Unit,
    onRunBuildAndTests: (String) -> Unit,
    onSaveGitHubConfig: (String, String, String, String, String) -> Unit,
    onRollbackCommit: (GitHubCommitDoc) -> Unit,
    onRollbackConfigBackup: (CentralConfigBackupDoc) -> Unit,
    onOpenCentralAiLivePanel: () -> Unit,
    onDismiss: () -> Unit
) {
    // 1. Strict Owner-Only Verification Gate
    val isVerifiedOwner = SuperOwnerControlManager.isCurrentUserSuperOwner(currentUserProfile, registryDoc)
    if (!isVerifiedOwner) {
        onDismiss()
        return
    }

    var selectedTab by remember { mutableIntStateOf(0) }
    var selectedFile by remember(workspaceFiles) {
        mutableStateOf(workspaceFiles.firstOrNull() ?: GeminiDeveloperStudioManager.defaultProjectSourceCatalog().first())
    }
    var editorCodeBuffer by remember(selectedFile.filePath, selectedFile.content) {
        mutableStateOf(selectedFile.content)
    }
    var editorCommitMsg by remember { mutableStateOf("Update source via Gemini AI Developer Studio") }
    var editorTargetBranch by remember(githubConfig.activeDevBranch) {
        mutableStateOf(githubConfig.activeDevBranch.ifBlank { "ai-dev/gemini-studio" })
    }

    var chatInput by remember { mutableStateOf("") }
    var generatorPrompt by remember { mutableStateOf("") }
    var pendingDeployProposal by remember { mutableStateOf<AiCodePatchProposal?>(null) }
    var pendingBugFixDeploy by remember { mutableStateOf<BugScanFindingDoc?>(null) }
    var perfSnapshot by remember { mutableStateOf(GeminiDeveloperStudioManager.captureAppPerformanceSnapshot()) }

    // GitHub settings state
    var ghOwner by remember(githubConfig.repositoryOwner) { mutableStateOf(githubConfig.repositoryOwner) }
    var ghRepo by remember(githubConfig.repositoryName) { mutableStateOf(githubConfig.repositoryName) }
    var ghDefaultBranch by remember(githubConfig.defaultBranch) { mutableStateOf(githubConfig.defaultBranch) }
    var ghDevBranch by remember(githubConfig.activeDevBranch) { mutableStateOf(githubConfig.activeDevBranch) }
    var ghTokenInput by remember { mutableStateOf("") }

    val quickDevCommands = remember {
        listOf(
            "Create a new page 'VIP Creator Studio'",
            "Change home page design to Cosmic Violet Stage",
            "Change all app colors to Luxury Gold & Neon Rose",
            "Add new chatroom features with 8-seat speaker pulse",
            "Create a new notification system with Gold VIP banners",
            "Fix messaging bugs & verify 4-stage DM delivery ticks",
            "Add new games tournament banner",
            "Improve profile pages with VIP 3D Gemstone showcase",
            "Change menu design to glass floating",
            "Check Firebase integration & harden security rules"
        )
    }

    val dashboardTabs = listOf(
        "💬 Gemini AI Chat",
        "⚡ AI Code Generator",
        "🛠️ Auto Bug Fixer (${bugFindings.size})",
        "📁 File Explorer (${workspaceFiles.size})",
        "📝 Code Editor",
        "🧪 Build & Test",
        "🚨 Error Reports",
        "🐙 GitHub Repo",
        "🚀 Deployments (${deployments.size})",
        "⏪ Rollback",
        "📊 Performance"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .testTag("gemini_developer_studio_modal"),
            color = DevStudioBg
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top Developer Studio Header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0xFF0F172A), Color(0xFF3B0764), Color(0xFF831843))
                            )
                        )
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Code,
                                    contentDescription = "Gemini AI Developer Studio",
                                    tint = DevStudioCyan,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "NOICE LIVE • GEMINI AI DEVELOPER STUDIO",
                                    color = Color.White,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .background(VipGold.copy(alpha = 0.2f))
                                        .border(1.dp, VipGold, RoundedCornerShape(50))
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "OWNER ONLY (@sebin_mr)",
                                        color = VipGold,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 9.sp
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = "UID Verified (${currentUserProfile.id.take(18)}...) • Branch: ${githubConfig.activeDevBranch} • Live ${productionConfig.versionLabel}",
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.Black.copy(alpha = 0.35f))
                                    .border(1.dp, DevStudioCyan, RoundedCornerShape(12.dp))
                                    .clickable { onOpenCentralAiLivePanel() }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                                    .testTag("switch_to_central_live_admin_button")
                            ) {
                                Text(
                                    text = "LIVE CUSTOMIZER",
                                    color = DevStudioCyan,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 10.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.4f))
                                    .testTag("close_gemini_developer_studio_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close Developer Studio",
                                    tint = Color.White
                                )
                            }
                        }
                    }
                }

                // Status Feedback Bar
                if (!statusMessage.isNullOrBlank()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF064E3B))
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = DevStudioGreen,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = statusMessage,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }

                // 11-Module Developer Navigation Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DevStudioSurface)
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    dashboardTabs.forEachIndexed { index, label ->
                        val selected = selectedTab == index
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(18.dp))
                                .background(
                                    if (selected) {
                                        Brush.horizontalGradient(listOf( Color(0xFF6D28D9), Color(0xFFDB2777)))
                                    } else {
                                        Brush.horizontalGradient(listOf(DevStudioCard, DevStudioCard))
                                    }
                                )
                                .border(
                                    1.dp,
                                    if (selected) DevStudioCyan else DevStudioBorder,
                                    RoundedCornerShape(18.dp)
                                )
                                .clickable { selectedTab = index }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                                .testTag("dev_studio_tab_$index")
                        ) {
                            Text(
                                text = label,
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                // Module Content
                when (selectedTab) {
                    // =========================================================
                    // TAB 0: GEMINI AI CHAT
                    // =========================================================
                    0 -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(14.dp)
                        ) {
                            // Security & Architecture Context Banner
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(DevStudioCard)
                                    .border(1.dp, DevStudioBorder, RoundedCornerShape(14.dp))
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = VipGold,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Gemini AI Developer has full visibility into all ${workspaceFiles.size} NOICE LIVE modules, Firestore rules, and live configs. Ask Gemini to build features, fix bugs, or inspect code.",
                                    color = DevStudioTextSecondary,
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            LazyColumn(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                if (chatMessages.isEmpty()) {
                                    item {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(16.dp))
                                                .background(DevStudioCard)
                                                .border(1.dp, DevStudioCyan.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                                                .padding(16.dp)
                                        ) {
                                            Text(
                                                text = "👋 Welcome @sebin_mr to the Gemini AI Developer Studio",
                                                color = VipGold,
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 14.sp
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = "Try any command below or type your own instruction:",
                                                color = DevStudioTextSecondary,
                                                fontSize = 12.sp
                                            )
                                            Spacer(modifier = Modifier.height(10.dp))
                                            quickDevCommands.take(5).forEach { cmd ->
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(vertical = 3.dp)
                                                        .clip(RoundedCornerShape(10.dp))
                                                        .background(DevStudioCodeBg)
                                                        .border(1.dp, DevStudioBorder, RoundedCornerShape(10.dp))
                                                        .clickable { chatInput = cmd }
                                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                                                ) {
                                                    Text(
                                                        text = "▸ $cmd",
                                                        color = DevStudioCyan,
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                items(chatMessages, key = { it.id }) { msg ->
                                    val isOwnerMsg = msg.senderRole == "owner"
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(
                                                if (isOwnerMsg) Color(0xFF2E1065) else DevStudioCard
                                            )
                                            .border(
                                                1.dp,
                                                if (isOwnerMsg) NoisePurple else DevStudioCyan.copy(alpha = 0.5f),
                                                RoundedCornerShape(16.dp)
                                            )
                                            .padding(14.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = if (isOwnerMsg) "👑 @sebin_mr (Owner)" else "🤖 ${msg.senderName}",
                                                color = if (isOwnerMsg) VipGold else DevStudioCyan,
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 11.sp
                                            )
                                            Text(
                                                text = formatDevTimestamp(msg.timestampMillis),
                                                color = DevStudioTextSecondary,
                                                fontSize = 10.sp
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = msg.messageText,
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            lineHeight = 18.sp
                                        )
                                        if (msg.generatedCodeSnippet.isNotBlank()) {
                                            Spacer(modifier = Modifier.height(10.dp))
                                            Text(
                                                text = "GENERATED COMPLETE SOURCE (${msg.generatedCodeFilePath}):",
                                                color = VipGold,
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 10.sp
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            CodeBlockViewer(code = msg.generatedCodeSnippet)
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                OutlinedButton(
                                                    onClick = {
                                                        val targetFile = workspaceFiles.firstOrNull { it.filePath == msg.generatedCodeFilePath }
                                                        if (targetFile != null) {
                                                            selectedFile = targetFile
                                                        }
                                                        editorCodeBuffer = msg.generatedCodeSnippet
                                                        selectedTab = 4 // Open in Code Editor
                                                    },
                                                    shape = RoundedCornerShape(10.dp)
                                                ) {
                                                    Text("Open in Code Editor", color = DevStudioCyan, fontSize = 11.sp)
                                                }
                                                Button(
                                                    onClick = { selectedTab = 1 },
                                                    colors = ButtonDefaults.buttonColors(containerColor = NoisePurple),
                                                    shape = RoundedCornerShape(10.dp)
                                                ) {
                                                    Text("Inspect Diff & Deploy", color = Color.White, fontSize = 11.sp)
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Chat Input Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = chatInput,
                                    onValueChange = { chatInput = it },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("dev_studio_chat_input"),
                                    placeholder = {
                                        Text("Ask Gemini AI Developer to generate code, add pages, or fix bugs...", fontSize = 12.sp)
                                    },
                                    maxLines = 3,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = DevStudioCyan,
                                        unfocusedBorderColor = DevStudioBorder
                                    )
                                )
                                Button(
                                    onClick = {
                                        if (chatInput.isNotBlank() && !isBusy) {
                                            val sendText = chatInput
                                            chatInput = ""
                                            onSendChatPrompt(sendText)
                                        }
                                    },
                                    enabled = chatInput.isNotBlank() && !isBusy,
                                    modifier = Modifier
                                        .height(52.dp)
                                        .testTag("dev_studio_chat_send_button"),
                                    colors = ButtonDefaults.buttonColors(containerColor = NoisePurple),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    if (isBusy) {
                                        CircularProgressIndicator(
                                            color = Color.White,
                                            strokeWidth = 2.dp,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    } else {
                                        Text("SEND", color = Color.White, fontWeight = FontWeight.ExtraBold)
                                    }
                                }
                            }
                        }
                    }

                    // =========================================================
                    // TAB 1: AI CODE GENERATOR & APP DEVELOPMENT
                    // =========================================================
                    1 -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            item {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(DevStudioCard)
                                        .border(1.dp, DevStudioBorder, RoundedCornerShape(16.dp))
                                        .padding(14.dp)
                                ) {
                                    Text(
                                        text = "⚡ AI App Development & Complete Code Generator",
                                        color = VipGold,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 14.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Type any feature or UI command. Gemini generates complete Kotlin/Compose code, runs syntax & security checks, computes a unified diff, and prepares a branch commit + live deployment awaiting your approval.",
                                        color = DevStudioTextSecondary,
                                        fontSize = 11.sp
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        quickDevCommands.forEachIndexed { idx, cmd ->
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(14.dp))
                                                    .background(DevStudioCodeBg)
                                                    .border(1.dp, DevStudioCyan.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
                                                    .clickable { generatorPrompt = cmd }
                                                    .padding(horizontal = 12.dp, vertical = 7.dp)
                                                    .testTag("dev_quick_command_$idx")
                                            ) {
                                                Text(
                                                    text = cmd,
                                                    color = Color.White,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(10.dp))
                                    OutlinedTextField(
                                        value = generatorPrompt,
                                        onValueChange = { generatorPrompt = it },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("dev_code_generator_input"),
                                        label = { Text("Development Instruction for Gemini AI") },
                                        placeholder = { Text("e.g., Create a new page, Change home page design, Add new chatroom features...") },
                                        minLines = 3,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White,
                                            focusedBorderColor = DevStudioCyan,
                                            unfocusedBorderColor = DevStudioBorder
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Button(
                                        onClick = {
                                            if (generatorPrompt.isNotBlank() && !isBusy) {
                                                onGenerateCodePatch(generatorPrompt)
                                            }
                                        },
                                        enabled = generatorPrompt.isNotBlank() && !isBusy,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(48.dp)
                                            .testTag("dev_generate_code_patch_button"),
                                        colors = ButtonDefaults.buttonColors(containerColor = NoisePurple),
                                        shape = RoundedCornerShape(14.dp)
                                    ) {
                                        if (isBusy) {
                                            CircularProgressIndicator(
                                                color = Color.White,
                                                strokeWidth = 2.dp,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Gemini Generating Code & Running Checks...", color = Color.White, fontWeight = FontWeight.ExtraBold)
                                        } else {
                                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = VipGold)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("GENERATE COMPLETE CODE & DIFF", color = Color.White, fontWeight = FontWeight.ExtraBold)
                                        }
                                    }
                                }
                            }

                            if (currentCodeProposal != null) {
                                item {
                                    CodePatchProposalCard(
                                        proposal = currentCodeProposal,
                                        isBusy = isBusy,
                                        onOpenInEditor = {
                                            editorCodeBuffer = currentCodeProposal.generatedCompleteCode
                                            selectedTab = 4
                                        },
                                        onApproveAndDeploy = {
                                            pendingDeployProposal = currentCodeProposal
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // =========================================================
                    // TAB 2: AUTOMATIC BUG FIXER (8-STEP DEBUGGING PIPELINE)
                    // =========================================================
                    2 -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            item {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(DevStudioCard)
                                        .border(1.dp, DevStudioBorder, RoundedCornerShape(16.dp))
                                        .padding(14.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "🛠️ Automatic Bug Fixer (8-Step AI Pipeline)",
                                                color = VipGold,
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 14.sp
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "1. Scan Source -> 2. Identify Errors -> 3. Explain Problem -> 4. Generate Fix -> 5. Run Tests -> 6. Verify -> 7. Show Diff -> 8. Owner Approval & Deploy.",
                                                color = DevStudioTextSecondary,
                                                fontSize = 11.sp
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Button(
                                            onClick = onRunBugScanner,
                                            enabled = !isBusy,
                                            colors = ButtonDefaults.buttonColors(containerColor = NoisePurple),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.testTag("run_auto_bug_scanner_button")
                                        ) {
                                            Icon(Icons.Default.BugReport, contentDescription = null, tint = VipGold, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("SCAN PROJECT", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 11.sp)
                                        }
                                    }
                                }
                            }

                            items(bugFindings, key = { it.id }) { finding ->
                                BugFindingCard(
                                    finding = finding,
                                    isBusy = isBusy,
                                    onApproveAndDeploy = { pendingBugFixDeploy = finding }
                                )
                            }
                        }
                    }

                    // =========================================================
                    // TAB 3: PROJECT FILE EXPLORER
                    // =========================================================
                    3 -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            item {
                                Text(
                                    text = "📁 NOICE LIVE AUTHORIZED PROJECT SOURCE TREE (${workspaceFiles.size} MODULES)",
                                    color = VipGold,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 13.sp
                                )
                            }
                            items(workspaceFiles, key = { it.filePath }) { file ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(DevStudioCard)
                                        .border(
                                            1.dp,
                                            if (selectedFile.filePath == file.filePath) DevStudioCyan else DevStudioBorder,
                                            RoundedCornerShape(14.dp)
                                        )
                                        .clickable {
                                            selectedFile = file
                                            editorCodeBuffer = file.content
                                            selectedTab = 4
                                        }
                                        .padding(14.dp)
                                        .testTag("file_explorer_item_${file.id}"),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = if (file.filePath.endsWith(".kt")) Icons.Default.Code else Icons.Default.Description,
                                            contentDescription = null,
                                            tint = DevStudioCyan,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = file.filePath,
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp
                                            )
                                            Text(
                                                text = "${file.category.uppercase()} • ${file.lineCount} lines • Branch: ${file.branchName}",
                                                color = DevStudioTextSecondary,
                                                fontSize = 10.sp
                                            )
                                        }
                                    }
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(NoisePurple.copy(alpha = 0.25f))
                                            .padding(horizontal = 10.dp, vertical = 5.dp)
                                    ) {
                                        Text(
                                            text = "EDIT SOURCE",
                                            color = DevStudioCyan,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // =========================================================
                    // TAB 4: CODE EDITOR
                    // =========================================================
                    4 -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            item {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(DevStudioCard)
                                        .border(1.dp, DevStudioBorder, RoundedCornerShape(16.dp))
                                        .padding(14.dp)
                                ) {
                                    Text(
                                        text = "📝 Editing: ${selectedFile.filePath}",
                                        color = DevStudioCyan,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 13.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Branch: $editorTargetBranch • Changes are committed to a separate development branch and verified before merging.",
                                        color = DevStudioTextSecondary,
                                        fontSize = 11.sp
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    OutlinedTextField(
                                        value = editorCodeBuffer,
                                        onValueChange = { editorCodeBuffer = it },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("dev_studio_code_editor_input"),
                                        label = { Text("Kotlin / Compose / Rules Source Code") },
                                        minLines = 12,
                                        maxLines = 22,
                                        textStyle = androidx.compose.ui.text.TextStyle(
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 11.sp,
                                            color = Color(0xFFE2E8F0)
                                        ),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedContainerColor = DevStudioCodeBg,
                                            unfocusedContainerColor = DevStudioCodeBg,
                                            focusedBorderColor = DevStudioCyan,
                                            unfocusedBorderColor = DevStudioBorder
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = editorTargetBranch,
                                            onValueChange = { editorTargetBranch = it },
                                            modifier = Modifier.weight(1f),
                                            label = { Text("Target Dev Branch") },
                                            singleLine = true,
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedTextColor = Color.White,
                                                unfocusedTextColor = Color.White
                                            )
                                        )
                                        OutlinedTextField(
                                            value = editorCommitMsg,
                                            onValueChange = { editorCommitMsg = it },
                                            modifier = Modifier.weight(1.4f),
                                            label = { Text("Commit Message") },
                                            singleLine = true,
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedTextColor = Color.White,
                                                unfocusedTextColor = Color.White
                                            )
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        OutlinedButton(
                                            onClick = { editorCodeBuffer = selectedFile.originalContent },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Text("Reset to Original", color = Color.White, fontSize = 11.sp)
                                        }
                                        Button(
                                            onClick = {
                                                onSaveEditedFileToDevBranch(
                                                    editorTargetBranch,
                                                    selectedFile.filePath,
                                                    editorCodeBuffer,
                                                    selectedFile.content,
                                                    editorCommitMsg
                                                )
                                            },
                                            modifier = Modifier
                                                .weight(1.5f)
                                                .testTag("commit_to_dev_branch_button"),
                                            colors = ButtonDefaults.buttonColors(containerColor = NoisePurple),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Icon(Icons.Default.CloudUpload, contentDescription = null, tint = VipGold, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("COMMIT TO DEV BRANCH", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 11.sp)
                                        }
                                    }
                                }
                            }

                            if (editorCodeBuffer != selectedFile.content) {
                                item {
                                    val liveDiff = remember(selectedFile.filePath, selectedFile.content, editorCodeBuffer) {
                                        GeminiDeveloperStudioManager.generateUnifiedDiff(
                                            selectedFile.filePath,
                                            selectedFile.content,
                                            editorCodeBuffer
                                        )
                                    }
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(DevStudioCard)
                                            .border(1.dp, VipGold.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                                            .padding(14.dp)
                                    ) {
                                        Text(
                                            text = "UNIFIED CODE DIFF PREVIEW",
                                            color = VipGold,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 12.sp
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        CodeBlockViewer(code = liveDiff)
                                    }
                                }
                            }
                        }
                    }

                    // =========================================================
                    // TAB 5: BUILD AND TEST RUNNER
                    // =========================================================
                    5 -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            item {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(DevStudioCard)
                                        .border(1.dp, DevStudioBorder, RoundedCornerShape(16.dp))
                                        .padding(14.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "🧪 Build & Test Verification Suite",
                                                color = VipGold,
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 14.sp
                                            )
                                            Text(
                                                text = "Passed: ${buildTestReport.unitTestPassedCount}/${buildTestReport.unitTestTotalCount} Suites • Branch: ${buildTestReport.branchName}",
                                                color = DevStudioGreen,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp
                                            )
                                        }
                                        Button(
                                            onClick = { onRunBuildAndTests(githubConfig.activeDevBranch) },
                                            colors = ButtonDefaults.buttonColors(containerColor = NoisePurple),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.testTag("run_build_and_tests_button")
                                        ) {
                                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = VipGold)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("RUN BUILD & TESTS", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 11.sp)
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(10.dp))
                                    CodeBlockViewer(code = buildTestReport.consoleOutput)
                                }
                            }
                        }
                    }

                    // =========================================================
                    // TAB 6: ERROR REPORTS & FIREBASE DIAGNOSTICS
                    // =========================================================
                    6 -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            item {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(DevStudioCard)
                                        .border(1.dp, DevStudioGreen.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                                        .padding(14.dp)
                                ) {
                                    Text(
                                        text = "🚨 Firebase Integration, Auth & Database Health Monitor",
                                        color = VipGold,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 14.sp
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    val checks = listOf(
                                        "✓ Firebase Authentication: Google Sign-In via CredentialManager active (UID: ${currentUserProfile.id.take(16)}...)",
                                        "✓ Super Owner Authority Lock: Bound to @sebin_mr (sebinmr556@gmail.com) in /super_owner_registry/primary",
                                        "✓ Firestore Named Database Instance: Connected via R.string.firestore_database_id",
                                        "✓ Firebase AI Logic (gemini-3.8-flash): App Check token verified • Zero API keys exposed in client APK",
                                        "✓ Firestore Security Rules: isSuperOwner() enforced on /central_app_config, /central_dynamic_pages & /developer_studio_*",
                                        "✓ Active Diagnostic Summary: ${productionConfig.lastDiagnosticSummary}"
                                    )
                                    checks.forEach { line ->
                                        Text(
                                            text = line,
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            modifier = Modifier.padding(vertical = 3.dp)
                                        )
                                    }
                                }
                            }

                            items(bugFindings, key = { "err_${it.id}" }) { finding ->
                                BugFindingCard(
                                    finding = finding,
                                    isBusy = isBusy,
                                    onApproveAndDeploy = { pendingBugFixDeploy = finding }
                                )
                            }
                        }
                    }

                    // =========================================================
                    // TAB 7: GITHUB CONNECTION & BRANCH / DIFF MANAGER
                    // =========================================================
                    7 -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            item {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(DevStudioCard)
                                        .border(1.dp, DevStudioBorder, RoundedCornerShape(16.dp))
                                        .padding(14.dp)
                                ) {
                                    Text(
                                        text = "🐙 GitHub Repository Connection (REST API v3 & Dev Branches)",
                                        color = VipGold,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 14.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = githubConfig.lastSyncStatus,
                                        color = DevStudioGreen,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedTextField(
                                            value = ghOwner,
                                            onValueChange = { ghOwner = it },
                                            modifier = Modifier.weight(1f),
                                            label = { Text("GitHub Owner/Org") },
                                            singleLine = true,
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedTextColor = Color.White,
                                                unfocusedTextColor = Color.White
                                            )
                                        )
                                        OutlinedTextField(
                                            value = ghRepo,
                                            onValueChange = { ghRepo = it },
                                            modifier = Modifier.weight(1f),
                                            label = { Text("Repository Name") },
                                            singleLine = true,
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedTextColor = Color.White,
                                                unfocusedTextColor = Color.White
                                            )
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedTextField(
                                            value = ghDefaultBranch,
                                            onValueChange = { ghDefaultBranch = it },
                                            modifier = Modifier.weight(1f),
                                            label = { Text("Production Branch") },
                                            singleLine = true,
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedTextColor = Color.White,
                                                unfocusedTextColor = Color.White
                                            )
                                        )
                                        OutlinedTextField(
                                            value = ghDevBranch,
                                            onValueChange = { ghDevBranch = it },
                                            modifier = Modifier.weight(1f),
                                            label = { Text("AI Development Branch") },
                                            singleLine = true,
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedTextColor = Color.White,
                                                unfocusedTextColor = Color.White
                                            )
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    OutlinedTextField(
                                        value = ghTokenInput,
                                        onValueChange = { ghTokenInput = it },
                                        modifier = Modifier.fillMaxWidth(),
                                        label = {
                                            Text(
                                                if (githubConfig.hasPersonalAccessTokenConfigured) {
                                                    "GitHub PAT Configured (${githubConfig.maskedTokenPreview}) • Enter new to rotate"
                                                } else {
                                                    "Optional GitHub Personal Access Token (stored in Owner-only vault)"
                                                }
                                            )
                                        },
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Button(
                                        onClick = {
                                            onSaveGitHubConfig(
                                                ghOwner,
                                                ghRepo,
                                                ghDefaultBranch,
                                                ghDevBranch,
                                                ghTokenInput
                                            )
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("save_github_config_button"),
                                        colors = ButtonDefaults.buttonColors(containerColor = NoisePurple),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("CONNECT & SYNC GITHUB REPOSITORY", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                                    }
                                }
                            }

                            item {
                                Text(
                                    text = "ACTIVE BRANCHES (${githubBranches.size})",
                                    color = VipGold,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 12.sp
                                )
                            }

                            items(githubBranches, key = { it.id }) { branch ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(DevStudioCard)
                                        .border(1.dp, DevStudioBorder, RoundedCornerShape(14.dp))
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "🌿 ${branch.branchName} (HEAD: ${branch.headCommitSha})",
                                            color = DevStudioCyan,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 12.sp
                                        )
                                        Text(
                                            text = branch.lastCommitMessage,
                                            color = Color.White,
                                            fontSize = 11.sp
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(DevStudioGreen.copy(alpha = 0.2f))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = branch.status.uppercase(),
                                            color = DevStudioGreen,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                            }

                            if (githubCommits.isNotEmpty()) {
                                item {
                                    Text(
                                        text = "RECENT COMMITS & CODE DIFFS (${githubCommits.size})",
                                        color = VipGold,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 12.sp
                                    )
                                }
                                items(githubCommits, key = { it.id }) { commit ->
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(DevStudioCard)
                                            .border(1.dp, DevStudioBorder, RoundedCornerShape(14.dp))
                                            .padding(12.dp)
                                    ) {
                                        Text(
                                            text = "[${commit.commitSha}] ${commit.commitMessage}",
                                            color = Color.White,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 12.sp
                                        )
                                        Text(
                                            text = "Branch: ${commit.branchName} • File: ${commit.filePath} • Merged: ${if (commit.mergedToMain) "YES" else "PENDING"}",
                                            color = DevStudioTextSecondary,
                                            fontSize = 10.sp
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        CodeBlockViewer(code = commit.unifiedDiff)
                                    }
                                }
                            }
                        }
                    }

                    // =========================================================
                    // TAB 8: DEPLOYMENT HISTORY (HOT LIVE + NATIVE APK/AAB)
                    // =========================================================
                    8 -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            item {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(DevStudioCard)
                                        .border(1.dp, DevStudioBorder, RoundedCornerShape(16.dp))
                                        .padding(14.dp)
                                ) {
                                    Text(
                                        text = "🚀 Live App Updates & Native APK/AAB Deployment Pipeline",
                                        color = VipGold,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 14.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "• UI settings, themes, gradients, menus, chatroom layouts & dynamic pages deploy immediately via Firestore real-time snapshots without reinstalling.\n• Native Android code updates generate a versioned APK/AAB release entry.",
                                        color = DevStudioTextSecondary,
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            items(deployments, key = { it.id }) { deploy ->
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(DevStudioCard)
                                        .border(1.dp, DevStudioCyan.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                                        .padding(14.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${deploy.deploymentVersion} • ${deploy.title}",
                                            color = Color.White,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 13.sp,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(
                                                    if (deploy.requiresUserAppUpdate) VipGold.copy(alpha = 0.2f)
                                                    else DevStudioGreen.copy(alpha = 0.2f)
                                                )
                                                .padding(horizontal = 8.dp, vertical = 3.dp)
                                        ) {
                                            Text(
                                                text = deploy.deploymentType,
                                                color = if (deploy.requiresUserAppUpdate) VipGold else DevStudioGreen,
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 9.sp
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = deploy.changelog,
                                        color = DevStudioTextSecondary,
                                        fontSize = 11.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Commit: ${deploy.commitSha} • Branch: ${deploy.branchName} • ${formatDevTimestamp(deploy.deployedAtMillis)}",
                                        color = DevStudioCyan,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                    }

                    // =========================================================
                    // TAB 9: ROLLBACK CHANGES (CODE COMMITS & LIVE CONFIGS)
                    // =========================================================
                    9 -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            item {
                                Text(
                                    text = "⏪ 1-Tap Rollback Center (Source Commits & Live App Configs)",
                                    color = VipGold,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 14.sp
                                )
                            }

                            if (githubCommits.isNotEmpty()) {
                                item {
                                    Text(
                                        text = "SOURCE CODE COMMIT ROLLBACKS:",
                                        color = DevStudioCyan,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 11.sp
                                    )
                                }
                                items(githubCommits, key = { "rb_commit_${it.id}" }) { commit ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(DevStudioCard)
                                            .border(1.dp, DevStudioBorder, RoundedCornerShape(14.dp))
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "[${commit.commitSha}] ${commit.filePath.substringAfterLast('/')}",
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp
                                            )
                                            Text(
                                                text = commit.commitMessage,
                                                color = DevStudioTextSecondary,
                                                fontSize = 11.sp
                                            )
                                        }
                                        Button(
                                            onClick = { onRollbackCommit(commit) },
                                            colors = ButtonDefaults.buttonColors(containerColor = DangerRed),
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Icon(Icons.Default.Restore, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("ROLLBACK CODE", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 10.sp)
                                        }
                                    }
                                }
                            }

                            if (configBackups.isNotEmpty()) {
                                item {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "LIVE PRODUCTION CONFIG BACKUP ROLLBACKS:",
                                        color = DevStudioCyan,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 11.sp
                                    )
                                }
                                items(configBackups, key = { "rb_cfg_${it.id}" }) { backup ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(DevStudioCard)
                                            .border(1.dp, DevStudioBorder, RoundedCornerShape(14.dp))
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = backup.backupLabel,
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp
                                            )
                                            Text(
                                                text = backup.reason,
                                                color = DevStudioTextSecondary,
                                                fontSize = 11.sp
                                            )
                                        }
                                        Button(
                                            onClick = { onRollbackConfigBackup(backup) },
                                            colors = ButtonDefaults.buttonColors(containerColor = NoisePurple),
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Text("RESTORE LIVE", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 10.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // =========================================================
                    // TAB 10: APP PERFORMANCE MONITOR
                    // =========================================================
                    10 -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            item {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(DevStudioCard)
                                        .border(1.dp, DevStudioCyan.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                                        .padding(16.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Speed, contentDescription = null, tint = DevStudioCyan)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "📊 Real-Time App Performance Monitor",
                                                color = VipGold,
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 14.sp
                                            )
                                        }
                                        IconButton(
                                            onClick = {
                                                perfSnapshot = GeminiDeveloperStudioManager.captureAppPerformanceSnapshot()
                                            }
                                        ) {
                                            Icon(Icons.Default.Refresh, contentDescription = "Refresh Telemetry", tint = DevStudioCyan)
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        PerfMetricCard(
                                            title = "JVM Memory Heap",
                                            value = "${perfSnapshot.usedHeapMb}MB / ${perfSnapshot.maxHeapMb}MB",
                                            subtitle = "${perfSnapshot.heapUtilizationPercent}% Utilized",
                                            modifier = Modifier.weight(1f)
                                        )
                                        PerfMetricCard(
                                            title = "UI Render Rate",
                                            value = "${perfSnapshot.estimatedUiFps} FPS",
                                            subtitle = "${perfSnapshot.frameTimeMs}ms / frame",
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        PerfMetricCard(
                                            title = "Firestore Streams",
                                            value = "${perfSnapshot.activeFirestoreListeners} Active",
                                            subtitle = "Real-time snapshots()",
                                            modifier = Modifier.weight(1f)
                                        )
                                        PerfMetricCard(
                                            title = "Gemini AI Latency",
                                            value = "${perfSnapshot.firebaseAiLatencyMs} ms",
                                            subtitle = "gemini-3.8-flash",
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))
                                    perfSnapshot.recommendations.forEach { rec ->
                                        Text(
                                            text = rec,
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            modifier = Modifier.padding(vertical = 3.dp)
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

    // Owner Approval Confirmation Dialog before merging & deploying AI Code Patch
    val currentPendingProposal = pendingDeployProposal
    if (currentPendingProposal != null) {
        AlertDialog(
            onDismissRequest = { pendingDeployProposal = null },
            containerColor = DevStudioCard,
            title = {
                Text(
                    text = "Approve, Merge & Deploy AI Code Changes?",
                    color = VipGold,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Target File: ${currentPendingProposal.targetFilePath}",
                        color = DevStudioCyan,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                    Text(
                        text = currentPendingProposal.explanation,
                        color = Color.White,
                        fontSize = 12.sp
                    )
                    Text(
                        text = "• Commits code to branch '${currentPendingProposal.targetBranch}' and merges to 'main'.\n• Automatically creates a pre-deploy backup snapshot.\n• Broadcasts live UI/feature updates to all connected NOICE LIVE users.",
                        color = DevStudioTextSecondary,
                        fontSize = 11.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val toDeploy = currentPendingProposal
                        pendingDeployProposal = null
                        onApproveAndDeployCodePatch(toDeploy)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DevStudioGreen),
                    modifier = Modifier.testTag("confirm_approve_deploy_code_patch_button")
                ) {
                    Text("APPROVE & DEPLOY NOW", color = Color.Black, fontWeight = FontWeight.ExtraBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeployProposal = null }) {
                    Text("Cancel", color = Color.White)
                }
            }
        )
    }

    // Owner Approval Confirmation Dialog before deploying a Bug Fix
    val currentPendingBug = pendingBugFixDeploy
    if (currentPendingBug != null) {
        AlertDialog(
            onDismissRequest = { pendingBugFixDeploy = null },
            containerColor = DevStudioCard,
            title = {
                Text(
                    text = "Approve & Deploy Verified Bug Fix?",
                    color = VipGold,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = currentPendingBug.title,
                        color = DevStudioCyan,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                    Text(
                        text = currentPendingBug.problemExplanation,
                        color = Color.White,
                        fontSize = 12.sp
                    )
                    Text(
                        text = currentPendingBug.testVerificationSummary,
                        color = DevStudioGreen,
                        fontSize = 11.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val bug = currentPendingBug
                        pendingBugFixDeploy = null
                        onApproveAndDeployBugFix(bug)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DevStudioGreen),
                    modifier = Modifier.testTag("confirm_approve_bug_fix_button")
                ) {
                    Text("APPROVE & DEPLOY FIX", color = Color.Black, fontWeight = FontWeight.ExtraBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingBugFixDeploy = null }) {
                    Text("Cancel", color = Color.White)
                }
            }
        )
    }
}

@Composable
private fun CodePatchProposalCard(
    proposal: AiCodePatchProposal,
    isBusy: Boolean,
    onOpenInEditor: () -> Unit,
    onApproveAndDeploy: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(DevStudioCard)
            .border(1.5.dp, DevStudioCyan, RoundedCornerShape(18.dp))
            .padding(16.dp)
            .testTag("ai_code_patch_proposal_card")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "✨ GENERATED CODE PATCH (${proposal.featureCategory.uppercase()})",
                color = VipGold,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 13.sp
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(if (proposal.buildCheckPassed) DevStudioGreen.copy(alpha = 0.2f) else DangerRed.copy(alpha = 0.2f))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = if (proposal.buildCheckPassed) "BUILD & TESTS PASSED" else "CHECK REQUIRED",
                    color = if (proposal.buildCheckPassed) DevStudioGreen else DangerRed,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 9.sp
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Target File: ${proposal.targetFilePath} • Branch: ${proposal.targetBranch}",
            color = DevStudioCyan,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = proposal.explanation,
            color = Color.White,
            fontSize = 12.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        proposal.verificationReport.forEach { check ->
            Text(
                text = check,
                color = DevStudioGreen,
                fontSize = 11.sp
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "COMPLETE GENERATED SOURCE CODE:",
            color = VipGold,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 11.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        CodeBlockViewer(code = proposal.generatedCompleteCode)
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "UNIFIED GIT DIFF:",
            color = VipGold,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 11.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        CodeBlockViewer(code = proposal.unifiedDiff)
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = onOpenInEditor,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Edit in Code Editor", color = DevStudioCyan, fontWeight = FontWeight.Bold, fontSize = 11.sp)
            }
            Button(
                onClick = onApproveAndDeploy,
                enabled = !isBusy && proposal.buildCheckPassed,
                modifier = Modifier
                    .weight(1.4f)
                    .testTag("approve_and_deploy_code_patch_button"),
                colors = ButtonDefaults.buttonColors(containerColor = DevStudioGreen),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "APPROVE, MERGE & DEPLOY",
                    color = Color.Black,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
private fun BugFindingCard(
    finding: BugScanFindingDoc,
    isBusy: Boolean,
    onApproveAndDeploy: () -> Unit
) {
    val deployed = finding.status == "approved_deployed"
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DevStudioCard)
            .border(
                1.dp,
                if (deployed) DevStudioGreen else VipGold.copy(alpha = 0.7f),
                RoundedCornerShape(16.dp)
            )
            .padding(14.dp)
            .testTag("bug_finding_card_${finding.id}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "[${finding.severity}] ${finding.title}",
                color = if (deployed) DevStudioGreen else VipGold,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 13.sp,
                modifier = Modifier.weight(1f)
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (deployed) DevStudioGreen.copy(alpha = 0.2f) else NoisePurple.copy(alpha = 0.3f))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = if (deployed) "DEPLOYED ✓" else "VERIFIED FIX READY",
                    color = if (deployed) DevStudioGreen else Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 9.sp
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "File: ${finding.filePath} (Line ${finding.lineNumber})",
            color = DevStudioCyan,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Problem Explanation: ${finding.problemExplanation}",
            color = Color.White,
            fontSize = 12.sp
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = finding.testVerificationSummary,
            color = DevStudioGreen,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        CodeBlockViewer(code = finding.unifiedDiff)
        if (!deployed) {
            Spacer(modifier = Modifier.height(10.dp))
            Button(
                onClick = onApproveAndDeploy,
                enabled = !isBusy,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("approve_bug_fix_${finding.id}"),
                colors = ButtonDefaults.buttonColors(containerColor = DevStudioGreen),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "APPROVE & DEPLOY VERIFIED FIX",
                    color = Color.Black,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun CodeBlockViewer(code: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DevStudioCodeBg)
            .border(1.dp, DevStudioBorder, RoundedCornerShape(12.dp))
            .horizontalScroll(rememberScrollState())
            .padding(12.dp)
    ) {
        Text(
            text = code,
            color = Color(0xFFE2E8F0),
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            lineHeight = 16.sp
        )
    }
}

@Composable
private fun PerfMetricCard(
    title: String,
    value: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(DevStudioCodeBg)
            .border(1.dp, DevStudioBorder, RoundedCornerShape(14.dp))
            .padding(12.dp)
    ) {
        Text(text = title, color = DevStudioTextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = value, color = DevStudioCyan, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = subtitle, color = DevStudioGreen, fontSize = 10.sp)
    }
}

private fun formatDevTimestamp(millis: Long): String {
    if (millis <= 0L) return "Just now"
    return runCatching {
        SimpleDateFormat("MMM d, HH:mm:ss", Locale.US).format(Date(millis))
    }.getOrDefault("Just now")
}
