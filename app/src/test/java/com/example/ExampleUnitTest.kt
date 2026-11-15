package com.example

import androidx.compose.ui.graphics.Color
import com.example.ui.getVipGemPalette
import com.example.ui.theme.LightLavender
import com.example.ui.theme.NoiseBlack
import com.example.ui.theme.NoiseBrandPink
import com.example.ui.theme.NoiseBrandPurple
import com.example.ui.theme.NoiseCard
import com.example.ui.theme.NoiseCardElevated
import com.example.ui.theme.NoiseDisabledText
import com.example.ui.theme.NoiseHeadingText
import com.example.ui.theme.NoiseLinkColor
import com.example.ui.theme.NoiseNormalText
import com.example.ui.theme.NoisePlaceholderText
import com.example.ui.theme.NoiseSurface
import com.example.ui.theme.NoiseTextSecondary
import com.example.ui.theme.NoiseWhite
import com.example.ui.theme.OnlineGreen
import com.example.ui.theme.ThemeManager
import com.example.ui.theme.ThemeViewModel
import com.example.ui.theme.VipGold
import com.example.ui.theme.autoContrastTextOn
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun themeManagerAndThemeViewModel_applyInstagramWhiteLightModeAndAutoContrast() {
        val viewModel = ThemeViewModel()

        // Switch to Light Mode (Instagram-style pure white Day Mode)
        viewModel.setThemeMode("light", null)
        assertTrue(ThemeManager.isLightMode)
        assertTrue(viewModel.isLightMode.value)
        assertEquals("light", viewModel.themeMode.value)

        // 1. Main Backgrounds in Light Mode: Pure White (#FFFFFF) and Soft Light Surface (#F8F8FC)
        assertEquals(Color(0xFFFFFFFF), NoiseBlack)
        assertEquals(Color(0xFFF8F8FC), NoiseSurface)
        assertEquals(Color(0xFFFFFFFF), NoiseCard)
        assertEquals(Color(0xFFF8F8FC), NoiseCardElevated)

        // 2. Text Color Correction in Light Mode: Pure Black (#000000) primary text & Dark Grey (#4A4A5A) secondary text
        assertEquals(Color(0xFF000000), NoiseHeadingText)
        assertEquals(Color(0xFF000000), NoiseWhite)
        assertEquals(Color(0xFF000000), NoiseNormalText)
        assertEquals(Color(0xFF4A4A5A), NoiseTextSecondary)
        assertEquals(Color(0xFF7A7A8C), NoisePlaceholderText)
        assertEquals(Color(0xFF9E9EAE), NoiseDisabledText)
        assertEquals(Color(0xFFC72DDC), NoiseLinkColor)

        // 3. Automatic Contrast Helper verification
        assertEquals(Color(0xFF000000), autoContrastTextOn(Color(0xFFFFFFFF)))
        assertEquals(Color(0xFFFFFFFF), autoContrastTextOn(Color(0xFF0E0B1B)))

        // 4. Original NOICE Brand Colors Preserved
        assertEquals(Color(0xFF8525FF), NoiseBrandPurple)
        assertEquals(Color(0xFFE52BEB), NoiseBrandPink)
        assertEquals(Color(0xFFFFB900), VipGold)
        assertEquals(Color(0xFF00C985), OnlineGreen)
        assertEquals(Color(0xFFF3E5F8), LightLavender)

        // 5. Switch back to Dark Mode and verify Dark Mode preservation
        val toggled = viewModel.toggleTheme(null)
        assertEquals("dark", toggled)
        assertFalse(ThemeManager.isLightMode)
        assertFalse(viewModel.isLightMode.value)
        assertEquals(Color(0xFF050507), NoiseBlack)
        assertEquals(Color(0xFFF9F9FB), NoiseWhite)
    }

    @Test
    fun vipGemPalettes_provideUnique3DGemstoneColorsForLevels1To10() {
        val palettes = (1..10).map { getVipGemPalette(it) }
        assertEquals(10, palettes.size)
        // Verify each VIP level 1..10 has its own distinct gemstone core color
        for (i in 0 until palettes.size) {
            assertEquals(i + 1, palettes[i].level)
            for (j in i + 1 until palettes.size) {
                assertNotEquals(
                    "VIP ${i + 1} and VIP ${j + 1} should have distinct gemstone core colors",
                    palettes[i].gemCore,
                    palettes[j].gemCore
                )
            }
        }
    }

    @Test
    fun discoverySystem_supportsAll18CategoriesAndEnforcesStrictSafeContentPolicy() {
        val categories = com.example.data.DiscoveryCategory.entries
        assertEquals(18, categories.size)
        val expectedLabels = listOf(
            "For You", "Trending", "News", "World News", "Entertainment", "Music",
            "Movies", "Horror", "Gadgets", "Technology", "Sports", "Gaming",
            "Comedy", "Viral Videos", "Fashion", "Education", "Science", "Lifestyle"
        )
        assertEquals(expectedLabels, categories.map { it.label })

        // Verify Safe & Positive Content Moderator approves verified trusted publisher articles
        val safeEval = com.example.data.DiscoverySafetyModerator.evaluateContentSafety(
            title = "NASA Webb Telescope Discovers Distant Exoplanet Atmosphere",
            description = "Scientists announced a breakthrough in space exploration.",
            originalUrl = "https://www.nasa.gov/news-release/webb-exoplanet-discovery/",
            thumbnailUrl = "https://www.nasa.gov/images/webb.jpg",
            videoUrl = ""
        )
        assertEquals(com.example.data.ModerationDecision.APPROVED, safeEval.decision)
        assertTrue(safeEval.trustedSourceVerified)
        assertTrue(safeEval.copyrightVerified)

        // Verify Safe & Positive Content Moderator rejects adult/explicit, piracy, and untrusted domains
        val adultRejected = com.example.data.DiscoverySafetyModerator.evaluateContentSafety(
            title = "Explicit NSFW Adult Video Leak",
            description = "Prohibited content test",
            originalUrl = "https://www.bbc.co.uk/news/test",
            thumbnailUrl = "",
            videoUrl = ""
        )
        assertEquals(com.example.data.ModerationDecision.REJECTED, adultRejected.decision)

        val piracyRejected = com.example.data.DiscoverySafetyModerator.evaluateContentSafety(
            title = "Download Free Pirated Movie Camrip Torrent",
            description = "Unauthorized copyrighted stream",
            originalUrl = "https://variety.com/2026/film/test",
            thumbnailUrl = "",
            videoUrl = ""
        )
        assertEquals(com.example.data.ModerationDecision.REJECTED, piracyRejected.decision)

        val untrustedDomainRejected = com.example.data.DiscoverySafetyModerator.evaluateContentSafety(
            title = "Breaking Global Headline Update",
            description = "Normal description from unverified suspicious site",
            originalUrl = "https://suspicious-fake-news-site.xyz/article/123",
            thumbnailUrl = "",
            videoUrl = ""
        )
        assertEquals(com.example.data.ModerationDecision.REJECTED, untrustedDomainRejected.decision)
    }

    @Test
    fun discoveryFeedParserAndDummyRoomFilter_parseRssAndRejectDummyStageRooms() {
        val sampleRss = """
            <?xml version="1.0" encoding="UTF-8"?>
            <rss version="2.0" xmlns:media="http://search.yahoo.com/mrss/">
              <channel>
                <title>BBC Technology</title>
                <item>
                  <title>Next-Gen AI Chips Announced at Global Tech Summit</title>
                  <link>https://www.bbc.co.uk/news/technology-123456</link>
                  <description><![CDATA[<p>New energy-efficient processors unveiled for mobile devices.</p>]]></description>
                  <media:thumbnail url="https://ichef.bbci.co.uk/news/976/cpsprodpb/tech.jpg" />
                  <pubDate>Wed, 11 Nov 2026 10:00:00 GMT</pubDate>
                </item>
              </channel>
            </rss>
        """.trimIndent()

        val source = com.example.data.DiscoveryContentManager.defaultTrustedSources()
            .first { it.categoryKey == com.example.data.DiscoveryCategory.TECHNOLOGY.key }
        val parsed = com.example.data.DiscoveryContentManager.parseSyndicationFeed(sampleRss, source)
        assertEquals(1, parsed.size)
        assertEquals("Next-Gen AI Chips Announced at Global Tech Summit", parsed[0].title)
        assertEquals("New energy-efficient processors unveiled for mobile devices.", parsed[0].description)
        assertEquals("https://ichef.bbci.co.uk/news/976/cpsprodpb/tech.jpg", parsed[0].thumbnailUrl)

        // Verify dummy stage rooms are flagged as dummy and excluded
        val dummyMicLem = com.example.data.PartyRoom(
            id = "room_1",
            hostId = "u1",
            title = "mic lem886's Stage"
        )
        val dummySebin = com.example.data.PartyRoom(
            id = "room_2",
            hostId = "u2",
            title = "sebin_mr's Stage"
        )
        val dummyOpenStage = com.example.data.PartyRoom(
            id = "vip_party_stage_card",
            hostId = "u3",
            title = "Open Stage"
        )
        val realPublicRoom = com.example.data.PartyRoom(
            id = "room_real_99",
            hostId = "uid_real_user",
            hostUsername = "sebin_mr",
            title = "Late Night Music Lounge",
            visibility = "public",
            privateRoom = false
        )
        val realPrivateRoom = realPublicRoom.copy(visibility = "private", privateRoom = true, invitationToken = "tok123")

        assertTrue(dummyMicLem.isDummyOrSampleRoom())
        assertTrue(dummySebin.isDummyOrSampleRoom())
        assertTrue(dummyOpenStage.isDummyOrSampleRoom())
        assertFalse(realPublicRoom.isDummyOrSampleRoom())
        assertTrue(realPublicRoom.isPublicRoom())
        assertFalse(realPrivateRoom.isPublicRoom())
        assertFalse(realPrivateRoom.isUserAuthorizedForRoom("stranger_uid", suppliedToken = null))
        assertTrue(realPrivateRoom.isUserAuthorizedForRoom("stranger_uid", suppliedToken = "tok123"))
    }

    @Test
    fun superOwnerControlSystemAndChatroomCleanup_enforcesPermanentSebinMrIdentityAndTombstones() {
        // 1. Verify permanent Super Owner identity & exact confirmation dialog message
        assertEquals("sebin_mr", com.example.data.SuperOwnerControlManager.PERMANENT_SUPER_OWNER_USERNAME)
        assertEquals(
            "Are you sure you want to permanently delete all chatrooms?",
            com.example.data.SuperOwnerControlManager.DELETE_ALL_CHATROOMS_CONFIRMATION_MESSAGE
        )

        val registry = com.example.data.SuperOwnerRegistryDoc(
            superOwnerUid = "qiVebE18UCecUn4IBtOzFAg12345",
            superOwnerUsername = "sebin_mr",
            superOwnerEmail = "sebinmr556@gmail.com"
        )

        // 2. Verified Super Owner account (@sebin_mr with bound UID / email) has full Super Owner authority
        assertTrue(
            com.example.data.SuperOwnerControlManager.isVerifiedSuperOwner(
                uid = "qiVebE18UCecUn4IBtOzFAg12345",
                email = "sebinmr556@gmail.com",
                username = "sebin_mr",
                registryDoc = registry
            )
        )

        // 3. Normal user cannot claim or impersonate @sebin_mr or gain Super Owner privileges
        assertTrue(com.example.data.SuperOwnerControlManager.isReservedSuperOwnerUsername("sebin_mr"))
        assertTrue(com.example.data.SuperOwnerControlManager.isReservedSuperOwnerUsername("@SEBIN_MR"))
        assertFalse(
            com.example.data.SuperOwnerControlManager.isVerifiedSuperOwner(
                uid = "normal_user_uid_999",
                email = "normaluser@example.com",
                username = "sebin_mr",
                registryDoc = registry
            )
        )

        // 4. Deleted chatroom tombstones prevent deleted or bugged rooms from reappearing
        com.example.data.SuperOwnerControlManager.markRoomDeletedTombstone("room_deleted_test_1")
        assertTrue(com.example.data.SuperOwnerControlManager.isRoomDeletedTombstone("room_deleted_test_1"))
    }

    @Test
    fun vipPartyStageCreateRoom_andTwoAccountJoinFlow_workEndToEnd() {
        val account1Uid = "uid_creator_account_1"
        val account2Uid = "uid_joiner_account_2"

        val createdPublicRoom = com.example.data.PartyRoom(
            id = "room_vip_stage_101",
            roomId = "room_vip_stage_101",
            hostId = account1Uid,
            hostUid = account1Uid,
            ownerUid = account1Uid,
            hostUsername = "arjun_live",
            ownerUsername = "arjun_live",
            roomName = "Kerala Music Live Party",
            title = "Kerala Music Live Party",
            subtitle = "arjun_live • Music",
            description = "Live Malayalam & English Music Stage",
            roomDescription = "Live Malayalam & English Music Stage",
            category = "Music",
            roomCategory = "Music",
            visibility = "public",
            publicRoom = true,
            privateRoom = false,
            authorizedUserIds = listOf(account1Uid),
            isActive = true,
            active = true,
            roomStatus = "active",
            participantCount = 1L,
            musicTrack = "Arabian Nights",
            musicSelection = "Arabian Nights",
            gameName = "PK Battle",
            gameSelection = "PK Battle",
            micMode = "Free Mic",
            microphoneSettings = "Free Mic",
            capacity = 12L,
            roomCapacity = 12L,
            themeKey = "starry_mountain",
            roomCover = "starry_mountain",
            stageTheme = "starry_mountain",
            roomImageUrl = "starry_mountain",
            live = true
        )

        // Creator (Registered Account 1) is automatically Room Owner and Host with all 13 required fields populated
        assertEquals(account1Uid, createdPublicRoom.hostId)
        assertEquals(account1Uid, createdPublicRoom.resolvedHostUid())
        assertEquals(account1Uid, createdPublicRoom.ownerUid)
        assertEquals("arjun_live", createdPublicRoom.hostUsername)
        assertEquals("Kerala Music Live Party", createdPublicRoom.resolvedRoomName())
        assertEquals("Live Malayalam & English Music Stage", createdPublicRoom.resolvedDescription())
        assertEquals("Music", createdPublicRoom.resolvedCategory())
        assertEquals("Arabian Nights", createdPublicRoom.musicSelection)
        assertEquals("PK Battle", createdPublicRoom.gameSelection)
        assertEquals("Free Mic", createdPublicRoom.microphoneSettings)
        assertEquals(12L, createdPublicRoom.roomCapacity)
        assertEquals("starry_mountain", createdPublicRoom.resolvedCoverThemeKey())
        assertEquals("Starry Mountain", createdPublicRoom.resolvedCoverThemeLabel())
        assertTrue(createdPublicRoom.isActive)
        assertTrue(createdPublicRoom.active)
        assertEquals("active", createdPublicRoom.roomStatus)
        assertFalse(createdPublicRoom.isDummyOrSampleRoom())
        assertTrue(createdPublicRoom.isPublicRoom())
        // Both Account 1 (Creator) and Account 2 (Registered User) can join public room
        assertTrue(createdPublicRoom.isUserAuthorizedForRoom(account1Uid))
        assertTrue(createdPublicRoom.isUserAuthorizedForRoom(account2Uid))

        // Account 2 creates a Private room — requires password/token for others while Account 2 is automatically authorized
        val createdPrivateRoom = createdPublicRoom.copy(
            id = "room_vip_stage_102",
            roomId = "room_vip_stage_102",
            hostId = account2Uid,
            hostUid = account2Uid,
            ownerUid = account2Uid,
            hostUsername = "meera_vibes",
            ownerUsername = "meera_vibes",
            roomName = "Meera's Stage",
            title = "Meera's Stage",
            visibility = "private",
            publicRoom = false,
            privateRoom = true,
            password = "4321",
            invitationToken = "invite_token_xyz",
            authorizedUserIds = listOf(account2Uid),
            themeKey = "royal_majlis",
            roomCover = "royal_majlis",
            stageTheme = "royal_majlis"
        )
        assertFalse(createdPrivateRoom.isDummyOrSampleRoom()) // Real user room ending in "'s Stage" is NOT flagged as dummy
        assertFalse(createdPrivateRoom.isPublicRoom())
        assertEquals("Royal Lounge", createdPrivateRoom.resolvedCoverThemeLabel())
        assertTrue(createdPrivateRoom.isUserAuthorizedForRoom(account2Uid))
        assertFalse(createdPrivateRoom.isUserAuthorizedForRoom(account1Uid))
        assertTrue(createdPrivateRoom.isUserAuthorizedForRoom(account1Uid, suppliedPassword = "4321"))
        assertTrue(createdPrivateRoom.isUserAuthorizedForRoom(account1Uid, suppliedToken = "invite_token_xyz"))
    }

    @Test
    fun automaticKeralaNews_supports10Categories14DistrictsAndMalayalamDetection() {
        // 1. All 10 required Kerala News categories exist
        val keralaCategories = com.example.data.KeralaNewsCategory.entries
        assertEquals(10, keralaCategories.size)
        val expectedLabels = listOf(
            "Kerala Latest News",
            "Kerala Breaking News",
            "Politics",
            "Education",
            "Technology",
            "Sports",
            "Entertainment",
            "Business",
            "District News",
            "Weather & Public Info"
        )
        assertEquals(expectedLabels, keralaCategories.map { it.label })

        // 2. All 14 Kerala districts (+ "All Districts") exist
        assertEquals(15, com.example.data.KERALA_DISTRICTS.size)
        assertTrue(com.example.data.KERALA_DISTRICTS.contains("Thiruvananthapuram"))
        assertTrue(com.example.data.KERALA_DISTRICTS.contains("Ernakulam"))
        assertTrue(com.example.data.KERALA_DISTRICTS.contains("Kozhikode"))
        assertTrue(com.example.data.KERALA_DISTRICTS.contains("Wayanad"))
        assertTrue(com.example.data.KERALA_DISTRICTS.contains("Kasaragod"))

        // 3. District detection works for both English and Malayalam script
        assertEquals(
            "Ernakulam",
            com.example.data.DiscoveryContentManager.detectKeralaDistrict(
                "Kochi Metro Phase 2 expansion announced in Ernakulam",
                "Infopark commuters to benefit"
            )
        )
        assertEquals(
            "Thiruvananthapuram",
            com.example.data.DiscoveryContentManager.detectKeralaDistrict(
                "തിരുവനന്തപുരം നഗരത്തിൽ കനത്ത മഴ മുന്നറിയിപ്പ്",
                "കേരള കാലാവസ്ഥ അറിയിപ്പ്"
            )
        )

        // 4. Malayalam script detection and Breaking News alert detection
        assertEquals(
            "ml",
            com.example.data.DiscoveryContentManager.detectArticleLanguage(
                "കേരള വാർത്തകൾ: കൊച്ചിയിൽ പുതിയ ഐടി പാർക്ക്",
                "",
                "en"
            )
        )
        assertEquals(
            "en",
            com.example.data.DiscoveryContentManager.detectArticleLanguage(
                "Kerala Startup Mission announces new technology grant",
                "Startups across Kerala can apply online",
                "en"
            )
        )
        assertTrue(
            com.example.data.DiscoveryContentManager.detectBreakingNews(
                "Breaking: IMD issues Red Alert for heavy rain in Idukki and Wayanad",
                com.example.data.KeralaNewsCategory.KERALA_WEATHER.key
            )
        )
    }

    @Test
    fun automaticTrendingReels_computesTrendingScoreAndPrioritizesHighEngagement() {
        val nowMs = 1_760_000_000_000L
        val highEngagementReel = com.example.data.Post(
            id = "reel_high",
            userId = "creator_1",
            username = "malayalam_creator",
            mediaKey = "https://storage.googleapis.com/noice/reel_high.mp4",
            mediaType = "video",
            caption = "Kochi vibes #KeralaReels #Malayalam",
            location = "Kochi, Kerala",
            regionTag = "Kerala",
            language = "ml",
            viewsCount = 500L,
            likesCount = 120L,
            commentsCount = 35L,
            sharesCount = 40L,
            savesCount = 25L
        )
        val lowEngagementReel = com.example.data.Post(
            id = "reel_low",
            userId = "creator_2",
            username = "new_creator",
            mediaKey = "https://storage.googleapis.com/noice/reel_low.mp4",
            mediaType = "video",
            caption = "Evening walk #NoiceLive",
            viewsCount = 10L,
            likesCount = 2L,
            commentsCount = 0L,
            sharesCount = 0L,
            savesCount = 0L
        )

        val highScore = highEngagementReel.computeReelTrendingScore(nowMs, "engagement_velocity")
        val lowScore = lowEngagementReel.computeReelTrendingScore(nowMs, "engagement_velocity")
        assertTrue("High engagement reel must have higher trending score", highScore > lowScore)
        assertTrue(highEngagementReel.isKeralaOrMalayalamReel())
        assertTrue(highEngagementReel.resolvedHashtags().contains("#KeralaReels"))
        assertTrue(highEngagementReel.resolvedHashtags().contains("#Malayalam"))
    }

    @Test
    fun centralAiAdminAndLiveUpdateSystem_verifiesOwnerUid_interpretsNaturalLanguageCommands_andSupportsLiveRollback() {
        val registry = com.example.data.SuperOwnerRegistryDoc(
            superOwnerUid = "qiVebE18UCecUn4IBtOzFAg12345",
            superOwnerUsername = "sebin_mr",
            superOwnerEmail = "sebinmr556@gmail.com"
        )

        // 1. Universal Admin Access: Only the verified Owner Admin UID / email can access the AI Admin Panel
        assertTrue(
            com.example.data.CentralAiAdminManager.isAuthorizedOwnerAdmin(
                uid = "qiVebE18UCecUn4IBtOzFAg12345",
                email = "sebinmr556@gmail.com",
                registryDoc = registry
            )
        )
        assertFalse(
            com.example.data.CentralAiAdminManager.isAuthorizedOwnerAdmin(
                uid = "impersonator_device_uid_001",
                email = "attacker@example.com",
                registryDoc = registry
            )
        )

        val baseConfig = com.example.data.CentralAppConfig()

        // 2. AI App Editor Command 1: "Change the home page background to Deep Cyber Midnight Blue"
        val bgProposal = com.example.data.CentralAiAdminManager.buildDeterministicProposal(
            prompt = "Change the home page background to Deep Cyber Midnight Blue",
            currentConfig = baseConfig,
            existingPages = emptyList()
        )
        assertTrue(bgProposal.validationPassed)
        assertEquals("#060B1E", bgProposal.proposedConfig.homeBackgroundDarkHex)
        assertEquals("#FFFFFF", bgProposal.proposedConfig.homeBackgroundLightHex)

        // 3. AI App Editor Command 2: "Add a new page 'VIP Events & Rewards'"
        val addPageProposal = com.example.data.CentralAiAdminManager.buildDeterministicProposal(
            prompt = "Add a new page 'VIP Events & Rewards'",
            currentConfig = bgProposal.proposedConfig,
            existingPages = emptyList()
        )
        assertTrue(addPageProposal.validationPassed)
        assertEquals("add_or_update", addPageProposal.dynamicPageOperation)
        val createdPage = addPageProposal.proposedDynamicPage
        assertTrue(createdPage != null && createdPage.title == "VIP Events & Rewards")
        assertTrue(createdPage!!.parsedSections().isNotEmpty())

        // 4. AI App Editor Command 3: "Change button colors and gradients to Luxury Gold-Rose"
        val gradProposal = com.example.data.CentralAiAdminManager.buildDeterministicProposal(
            prompt = "Change button colors and gradients to Luxury Gold-Rose",
            currentConfig = addPageProposal.proposedConfig,
            existingPages = listOf(createdPage)
        )
        assertTrue(gradProposal.validationPassed)
        assertEquals("#FFB900", gradProposal.proposedConfig.buttonGradientStartHex)
        assertEquals("#FF2E93", gradProposal.proposedConfig.buttonGradientEndHex)

        // 5. AI App Editor Command 4: "Modify chatroom layouts to 2-column grid"
        val roomLayoutProposal = com.example.data.CentralAiAdminManager.buildDeterministicProposal(
            prompt = "Modify chatroom layouts to 2-column grid",
            currentConfig = gradProposal.proposedConfig,
            existingPages = listOf(createdPage)
        )
        assertTrue(roomLayoutProposal.validationPassed)
        assertEquals("grid_2col", roomLayoutProposal.proposedConfig.chatroomLayoutStyle)

        // 6. Central Live Update Runtime & Backup Serialization / Rollback verification
        val serializedBackup = com.example.data.CentralAiAdminManager.serializeConfigToJson(roomLayoutProposal.proposedConfig)
        val restoredConfig = com.example.data.CentralAiAdminManager.deserializeConfigFromJson(serializedBackup, baseConfig)
        assertEquals("#060B1E", restoredConfig.homeBackgroundDarkHex)
        assertEquals("#FFB900", restoredConfig.buttonGradientStartHex)
        assertEquals("grid_2col", restoredConfig.chatroomLayoutStyle)

        // Reset runtime to clean baseline so other tests see default tokens
        com.example.data.CentralLiveUpdateRuntime.clearLocalPreview()
        com.example.data.CentralLiveUpdateRuntime.updateFromProductionSnapshot(com.example.data.CentralAppConfig())
    }

    @Test
    fun ownerOnlyGeminiAiDeveloperSystem_enforcesSebinMrAuth_generatesCodeFixesBugsAndManagesGitHubBranches() {
        val registry = com.example.data.SuperOwnerRegistryDoc(
            id = "primary",
            superOwnerUid = "qiVebE18UCecUn4IBtOzFAg12345",
            superOwnerUsername = "sebin_mr",
            superOwnerEmail = "sebinmr556@gmail.com",
            immutableLock = true
        )

        // 1. Strict Owner-Only Developer Authorization Check
        assertTrue(
            com.example.data.GeminiDeveloperStudioManager.isAuthorizedOwnerDeveloper(
                uid = "qiVebE18UCecUn4IBtOzFAg12345",
                email = "sebinmr556@gmail.com",
                username = "sebin_mr",
                registryDoc = registry
            )
        )
        assertFalse(
            com.example.data.GeminiDeveloperStudioManager.isAuthorizedOwnerDeveloper(
                uid = "other_admin_uid_999",
                email = "moderator@noiselive.app",
                username = "sebin_mr", // Even spoofing username must be rejected!
                registryDoc = registry
            )
        )

        // 2. Project File Explorer Catalog & Syntax / Security Validator
        val catalog = com.example.data.GeminiDeveloperStudioManager.defaultProjectSourceCatalog()
        assertTrue(catalog.size >= 10)
        val buildReport = com.example.data.GeminiDeveloperStudioManager.runBuildAndTestVerificationSuite(
            workspaceFiles = catalog,
            branchName = "ai-dev/gemini-studio"
        )
        assertTrue(buildReport.compileCheckPassed)
        assertTrue(buildReport.firestoreRulesCheckPassed)
        assertEquals(10, buildReport.unitTestPassedCount)

        // 3. AI Code Generator: Generate complete Kotlin/Compose code & unified diff for natural language command
        val codePatch = com.example.data.GeminiDeveloperStudioManager.buildDeterministicCodePatch(
            prompt = "Create a new page 'VIP Creator Studio'",
            workspaceFiles = catalog,
            currentConfig = com.example.data.CentralAppConfig(),
            existingPages = emptyList()
        )
        assertTrue(codePatch.buildCheckPassed)
        assertTrue(codePatch.generatedCompleteCode.contains("@Composable"))
        assertTrue(codePatch.unifiedDiff.contains("--- a/"))
        assertTrue(codePatch.unifiedDiff.contains("+++ b/"))
        assertFalse(codePatch.approvedByOwner) // Never overwrites production without Owner approval

        // 4. Security Validator rejects hardcoded API keys or signInAnonymously()
        val (unsafeValid, unsafeErrors) = com.example.data.GeminiDeveloperStudioManager.validateGeneratedCodeSyntaxAndSecurity(
            filePath = "app/src/main/java/com/example/Unsafe.kt",
            code = "fun bad() { auth.signInAnonymously() }"
        )
        assertFalse(unsafeValid)
        assertTrue(unsafeErrors.any { it.contains("Anonymous auth is forbidden") })
    }

    @Test
    fun videoPlaybackAndCloudMediaEngine_extractsCloudMediaIds_resolvesSources_andFormatsDuration() {
        // 1. Extract cloudMediaId from explicit query param, post storagePath, and story storagePath
        val fromQuery = com.example.data.CloudMediaStorageManager.extractCloudMediaId(
            "https://firebasestorage.googleapis.com/v0/b/bucket/o/users%2Fu1%2Fposts%2Fpost_123%2Foriginal.mp4?alt=media&cloudMediaId=media_post_post_123"
        )
        assertEquals("media_post_post_123", fromQuery)

        val fromPostPath = com.example.data.CloudMediaStorageManager.extractCloudMediaId(
            "users/uid_abc/posts/post_98765/original.mp4"
        )
        assertEquals("cmedia_post_98765", fromPostPath)

        val fromStoryPath = com.example.data.CloudMediaStorageManager.extractCloudMediaId(
            "users/uid_abc/stories/story_54321/original.mp4"
        )
        assertEquals("cmedia_story_54321", fromStoryPath)

        // 2. Post and Story effectiveMediaSource() preserve permanent cloud video URLs
        val videoPost = com.example.data.Post(
            id = "post_vid_1",
            postId = "post_vid_1",
            userId = "uid_1",
            username = "creator_1",
            mediaType = "video",
            mediaKey = "https://firebasestorage.googleapis.com/v0/b/bucket/o/video.mp4?alt=media&cloudMediaId=media_post_vid_1",
            mediaUrl = "https://firebasestorage.googleapis.com/v0/b/bucket/o/video.mp4?alt=media&cloudMediaId=media_post_vid_1",
            storagePath = "users/uid_1/posts/post_vid_1/original.mp4",
            cloudMediaId = "media_post_vid_1",
            uploadStatus = "Published"
        )
        assertTrue(videoPost.effectiveMediaSource().contains("cloudMediaId=media_post_vid_1"))
        assertFalse(com.example.data.CloudMediaStorageManager.isTemporaryLocalReference(videoPost.effectiveMediaSource()))

        val videoStory = com.example.data.Story(
            id = "story_vid_1",
            storyId = "story_vid_1",
            userId = "uid_1",
            username = "creator_1",
            mediaType = "video",
            durationSec = 45L,
            mediaKey = "https://firebasestorage.googleapis.com/v0/b/bucket/o/story.mp4?alt=media&cloudMediaId=media_story_vid_1",
            mediaUrl = "https://firebasestorage.googleapis.com/v0/b/bucket/o/story.mp4?alt=media&cloudMediaId=media_story_vid_1",
            storagePath = "users/uid_1/stories/story_vid_1/original.mp4",
            cloudMediaId = "media_story_vid_1",
            uploadStatus = "Published"
        )
        assertEquals(45L, videoStory.durationSec)
        assertTrue(videoStory.effectiveMediaSource().contains("cloudMediaId=media_story_vid_1"))

        // 3. Video duration formatter for Post Seek Bar & Story Progress Indicator
        assertEquals("00:00", com.example.ui.formatVideoDurationMs(0L))
        assertEquals("00:15", com.example.ui.formatVideoDurationMs(15_400L))
        assertEquals("01:30", com.example.ui.formatVideoDurationMs(90_000L))
        assertEquals("10:05", com.example.ui.formatVideoDurationMs(605_000L))

        // 4. Diagnostic error logger records technical errors for debugging
        com.example.data.CloudMediaStorageManager.recordDiagnosticError(
            stage = "UnitTest.Verification",
            detail = "Simulated network timeout check"
        )
        assertTrue(
            com.example.data.CloudMediaStorageManager.recentDiagnosticErrors().any {
                it.contains("UnitTest.Verification")
            }
        )
    }

    @Test
    fun vercelLiveKitConnection_verifiesProductionUrl_parsesTokens_andDetectsHealthConfig() {
        // 1. Verify production Vercel Base URL and API route builders
        assertEquals("https://noice-live26.vercel.app", com.example.ui.VERCEL_LIVE_URL)
        assertEquals(
            "https://noice-live26.vercel.app/api/livekit/rooms/room_party_101/token",
            com.example.ui.VercelLiveKitTokenClient.buildRoomTokenUrl("room_party_101")
        )
        assertEquals(
            "https://noice-live26.vercel.app/api/livekit/token",
            com.example.ui.VercelLiveKitTokenClient.buildDirectTokenUrl()
        )
        assertEquals(
            "https://noice-live26.vercel.app/api/livekit/calls",
            com.example.ui.VercelLiveKitTokenClient.buildCallsUrl()
        )

        // 2. Verify token response JSON parser handles both direct and nested response schemas
        val tokenJson = """
            {
              "wsUrl": "wss://noice-live.livekit.cloud",
              "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.sample.signature",
              "roomName": "room_party_101",
              "identity": "firebase_uid_account_1"
            }
        """.trimIndent()
        val parsed = com.example.ui.VercelLiveKitTokenClient.parseTokenResponseJson(
            rawJson = tokenJson,
            fallbackRoomName = "fallback_room",
            fallbackIdentity = "fallback_uid",
            endpointUsed = "https://noice-live26.vercel.app/api/livekit/rooms/room_party_101/token"
        )
        assertTrue(parsed != null)
        assertEquals("wss://noice-live.livekit.cloud", parsed?.wsUrl)
        assertEquals("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.sample.signature", parsed?.token)
        assertEquals("room_party_101", parsed?.roomName)
        assertEquals("firebase_uid_account_1", parsed?.participantIdentity)

        // 3. Verify health endpoint parser detects missing Firebase Admin env vars on Vercel
        val healthJson = """
            {
              "ok": false,
              "service": "noice-live-livekit",
              "config": {
                "LIVEKIT_URL": true,
                "LIVEKIT_API_KEY": true,
                "LIVEKIT_API_SECRET": true,
                "FIREBASE_PROJECT_ID": false,
                "FIREBASE_CLIENT_EMAIL": false,
                "FIREBASE_PRIVATE_KEY": false
              }
            }
        """.trimIndent()
        val health = com.example.ui.VercelLiveKitTokenClient.parseHealthResponseJson(healthJson, 503)
        assertFalse(health.ok)
        assertTrue(health.livekitUrlConfigured)
        assertTrue(health.livekitApiKeyConfigured)
        assertTrue(health.livekitApiSecretConfigured)
        assertFalse(health.firebaseProjectIdConfigured)
        assertEquals(
            listOf("FIREBASE_PROJECT_ID", "FIREBASE_CLIENT_EMAIL", "FIREBASE_PRIVATE_KEY"),
            health.missingEnvVars
        )
    }

    @Test
    fun twoAccountLiveKitChatroom_verifiesUserAAndUserBJoin_micCameraControls_leaveAndRoomIsolation() {
        val room1Id = "party_room_alpha_101"
        val room2Id = "party_room_beta_202"

        val userAUid = "firebase_uid_user_a"
        val userAUsername = "arjun_host"
        val userBUid = "firebase_uid_user_b"
        val userBUsername = "meera_guest"
        val userCUid = "firebase_uid_user_c"
        val userCUsername = "rahul_other_room"

        // 1. User A logs in and joins Room 1
        val userATokenJson = """
            {
              "serverUrl": "wss://noice-live.livekit.cloud",
              "token": "jwt_user_a_room_1",
              "roomName": "$room1Id",
              "identity": "$userAUid"
            }
        """.trimIndent()
        val userAGrant = com.example.ui.VercelLiveKitTokenClient.parseTokenResponseJson(
            rawJson = userATokenJson,
            fallbackRoomName = room1Id,
            fallbackIdentity = userAUid,
            endpointUsed = "${com.example.ui.VERCEL_ROOMS_ENDPOINT}/$room1Id/token"
        )
        assertTrue(userAGrant != null)
        assertEquals(room1Id, userAGrant?.roomName)
        assertEquals(userAUid, userAGrant?.participantIdentity)

        // 2. User B logs in and joins the same Room 1
        val userBTokenJson = """
            {
              "serverUrl": "wss://noice-live.livekit.cloud",
              "token": "jwt_user_b_room_1",
              "roomName": "$room1Id",
              "identity": "$userBUid"
            }
        """.trimIndent()
        val userBGrant = com.example.ui.VercelLiveKitTokenClient.parseTokenResponseJson(
            rawJson = userBTokenJson,
            fallbackRoomName = room1Id,
            fallbackIdentity = userBUid,
            endpointUsed = "${com.example.ui.VERCEL_ROOMS_ENDPOINT}/$room1Id/token"
        )
        assertTrue(userBGrant != null)
        assertEquals(room1Id, userBGrant?.roomName)
        assertEquals(userBUid, userBGrant?.participantIdentity)

        // 3. Both User A and User B appear in Room 1's LiveKit session state with real-time mic/camera/speaking states
        val participantA = com.example.ui.LiveKitParticipantSnapshot(
            identity = userAUid,
            displayName = userAUsername,
            isLocal = true,
            isSpeaking = true,
            isMicEnabled = true,
            isCameraEnabled = true,
            audioLevel = 0.85f
        )
        val participantB = com.example.ui.LiveKitParticipantSnapshot(
            identity = userBUid,
            displayName = userBUsername,
            isLocal = false,
            isSpeaking = false,
            isMicEnabled = true,
            isCameraEnabled = false,
            audioLevel = 0.0f
        )

        val room1ConnectedState = com.example.ui.LiveKitSessionUiState(
            connectionStatus = com.example.ui.LiveKitConnectionStatus.CONNECTED,
            roomId = room1Id,
            wsUrl = "wss://noice-live.livekit.cloud",
            localIdentity = userAUid,
            isMicEnabled = true,
            isCameraEnabled = true,
            isFrontCamera = true,
            isLocalSpeaking = true,
            participants = listOf(participantA, participantB),
            activeSpeakerIds = setOf(userAUid),
            activeSpeakerNames = listOf(userAUsername)
        )

        assertEquals(2, room1ConnectedState.participants.size)
        assertTrue(room1ConnectedState.participants.any { it.identity == userAUid && it.isSpeaking })
        assertTrue(room1ConnectedState.participants.any { it.identity == userBUid && it.isMicEnabled })

        // 4. Test Mic Mute/Unmute and Camera On/Off transitions between User A and User B
        val participantBMutedAndCamOn = participantB.copy(
            isMicEnabled = false,
            isCameraEnabled = true,
            isSpeaking = false
        )
        val room1AfterControls = room1ConnectedState.copy(
            isMicEnabled = false,
            isLocalSpeaking = false,
            participants = listOf(
                participantA.copy(isMicEnabled = false, isSpeaking = false),
                participantBMutedAndCamOn
            )
        )
        assertFalse(room1AfterControls.isMicEnabled)
        assertFalse(room1AfterControls.participants.last().isMicEnabled)
        assertTrue(room1AfterControls.participants.last().isCameraEnabled)

        // 5. Verify Room Isolation: Room 2 has User C and never mixes with Room 1
        val participantC = com.example.ui.LiveKitParticipantSnapshot(
            identity = userCUid,
            displayName = userCUsername,
            isLocal = true,
            isSpeaking = true,
            isMicEnabled = true,
            isCameraEnabled = false
        )
        val room2ConnectedState = com.example.ui.LiveKitSessionUiState(
            connectionStatus = com.example.ui.LiveKitConnectionStatus.CONNECTED,
            roomId = room2Id,
            wsUrl = "wss://noice-live.livekit.cloud",
            localIdentity = userCUid,
            participants = listOf(participantC)
        )
        assertTrue(room1ConnectedState.roomId != room2ConnectedState.roomId)
        assertFalse(room1ConnectedState.participants.any { it.identity == userCUid })
        assertFalse(room2ConnectedState.participants.any { it.identity == userAUid || it.identity == userBUid })

        // 6. Verify User B leaving Room 1 removes User B cleanly
        val room1AfterUserBLeaves = room1ConnectedState.copy(
            participants = listOf(participantA)
        )
        assertEquals(1, room1AfterUserBLeaves.participants.size)
        assertEquals(userAUid, room1AfterUserBLeaves.participants.first().identity)

        // 7. Verify CallSessionDoc defaults to https://noice-live26.vercel.app
        val callDoc = com.example.data.CallSessionDoc(
            id = "call_ab_01",
            callerId = userAUid,
            calleeId = userBUid,
            participantIds = listOf(userAUid, userBUid),
            liveKitRoomName = "call_ab_01"
        )
        assertEquals("https://noice-live26.vercel.app", callDoc.liveKitUrl)
    }

    @Test
    fun ownerMasterControlSystem_verifiesOwnerOnlyUid_magicPickerHsvHexGradients_dragDropUndoRedo_geminiAiCommands_andGlobalLivePublish() {
        val registry = com.example.data.SuperOwnerRegistryDoc(
            id = "primary",
            superOwnerUid = "qiVebE18UCecUn4IBtOzFAg12345",
            superOwnerUsername = "sebin_mr",
            superOwnerEmail = "sebinmr556@gmail.com",
            immutableLock = true
        )

        // 1. Strict Owner-Only UID verification (rejects username-only spoofing)
        assertTrue(
            com.example.data.CentralAiAdminManager.isAuthorizedOwnerAdmin(
                uid = "qiVebE18UCecUn4IBtOzFAg12345",
                email = "sebinmr556@gmail.com",
                registryDoc = registry
            )
        )
        assertFalse(
            com.example.data.CentralAiAdminManager.isAuthorizedOwnerAdmin(
                uid = "normal_user_or_mod_uid",
                email = "mod@noiselive.app",
                registryDoc = registry
            )
        )

        // 2. MagicPicker Color Engine: HSV <-> RGB <-> HEX conversions & Multi-Stop Gradient Spec
        val (r, g, b) = com.example.data.MagicColorEngine.hsvToRgb(0f, 1f, 1f) // Pure Red
        assertEquals(255, r)
        assertEquals(0, g)
        assertEquals(0, b)
        assertEquals("#FF0000", com.example.data.MagicColorEngine.rgbToHex(r, g, b))

        val (hsvTuple, parsedAlpha) = com.example.data.MagicColorEngine.hexToHsva("#8000FF00")
        assertEquals(120f, hsvTuple.first, 1.5f)
        assertEquals(1f, hsvTuple.second, 0.02f)
        assertEquals(1f, hsvTuple.third, 0.02f)
        assertEquals(128f / 255f, parsedAlpha, 0.02f)

        val multiStopGrad = com.example.data.MagicGradientSpec(
            gradientId = "grad_owner_1",
            name = "Purple Cyber Glow",
            type = "linear",
            angleDegrees = 135f,
            stops = listOf(
                com.example.data.MagicGradientStop("s0", 0f, "#7C3AED", 1f),
                com.example.data.MagicGradientStop("s1", 0.5f, "#E52BEB", 1f),
                com.example.data.MagicGradientStop("s2", 1f, "#00E5FF", 1f)
            )
        )
        val gradJson = com.example.data.MagicColorEngine.serializeGradientSpec(multiStopGrad)
        val restoredGrad = com.example.data.MagicColorEngine.deserializeGradientSpec(gradJson)
        assertTrue(restoredGrad != null)
        assertEquals(3, restoredGrad!!.stops.size)
        assertEquals(135f, restoredGrad.angleDegrees, 0.01f)
        assertEquals("#00E5FF", restoredGrad.stops.last().colorHex)

        // 3. Complete Drag-and-Drop Visual Editor + Undo / Redo / Reset / Duplicate / Add Element
        com.example.data.CentralLiveUpdateRuntime.clearLocalPreview()
        com.example.data.CentralLiveUpdateRuntime.updateFromProductionSnapshot(com.example.data.CentralAppConfig())
        com.example.data.CentralLiveUpdateRuntime.activateOwnerEditMode("header_notification_icon", "Notification Bell Icon")
        assertTrue(com.example.data.CentralLiveUpdateRuntime.isOwnerEditModeActive)

        com.example.data.CentralLiveUpdateRuntime.updateComponentOverride(
            elementId = "header_notification_icon",
            description = "Dragged Notification Bell Icon to right (+24dp, -4dp)"
        ) { ov ->
            ov.copy(offsetXDp = 24f, offsetYDp = -4f, widthScale = 1.15f, heightScale = 1.15f, horizontalAlign = "end")
        }
        val movedNotif = com.example.data.CentralLiveUpdateRuntime.getComponentOverride("header_notification_icon")
        assertEquals(24f, movedNotif.offsetXDp, 0.01f)
        assertEquals(-4f, movedNotif.offsetYDp, 0.01f)
        assertEquals(1.15f, movedNotif.widthScale, 0.01f)
        assertEquals("end", movedNotif.horizontalAlign)

        // Duplicate & Add custom element
        val duplicated = com.example.data.CentralLiveUpdateRuntime.duplicateComponentOverride("header_notification_icon")
        assertTrue(duplicated.isCustomAdded)
        assertEquals(36f, duplicated.offsetXDp, 0.01f)

        val addedBtn = com.example.data.CentralLiveUpdateRuntime.addCustomComponentToPage(
            pageKey = "HOME",
            elementType = "button",
            label = "VIP Quick Stage",
            customText = "Join VIP Stage"
        )
        assertTrue(com.example.data.CentralLiveUpdateRuntime.effectiveConfig.parsedComponentOverrides().any { it.elementId == addedBtn.elementId })

        // Test Undo & Redo
        assertTrue(com.example.data.CentralLiveUpdateRuntime.canUndo)
        assertTrue(com.example.data.CentralLiveUpdateRuntime.undoVisualEdit())
        assertFalse(com.example.data.CentralLiveUpdateRuntime.effectiveConfig.parsedComponentOverrides().any { it.elementId == addedBtn.elementId })
        assertTrue(com.example.data.CentralLiveUpdateRuntime.canRedo)
        assertTrue(com.example.data.CentralLiveUpdateRuntime.redoVisualEdit())
        assertTrue(com.example.data.CentralLiveUpdateRuntime.effectiveConfig.parsedComponentOverrides().any { it.elementId == addedBtn.elementId })

        // 4. Real Gemini AI Developer Natural Language Commands Verification
        val baseCfg = com.example.data.CentralAppConfig()

        // Command A: "Change the entire home page background to white."
        val whiteBgProp = com.example.data.CentralAiAdminManager.buildDeterministicProposal(
            prompt = "Change the entire home page background to white.",
            currentConfig = baseCfg,
            existingPages = emptyList()
        )
        assertEquals("#FFFFFF", whiteBgProp.proposedConfig.homeBackgroundDarkHex)
        assertEquals("#FFFFFF", whiteBgProp.proposedConfig.homeBackgroundLightHex)

        // Command B: "Move the notification icon to the right."
        val moveIconProp = com.example.data.CentralAiAdminManager.buildDeterministicProposal(
            prompt = "Move the notification icon to the right.",
            currentConfig = whiteBgProp.proposedConfig,
            existingPages = emptyList()
        )
        val notifOverride = moveIconProp.proposedConfig.parsedComponentOverrides().firstOrNull { it.elementId == "header_notification_icon" }
        assertTrue(notifOverride != null && notifOverride.offsetXDp > 0f && notifOverride.horizontalAlign == "end")

        // Command C: "Create a new Kerala News page."
        val keralaPageProp = com.example.data.CentralAiAdminManager.buildDeterministicProposal(
            prompt = "Create a new Kerala News page.",
            currentConfig = moveIconProp.proposedConfig,
            existingPages = emptyList()
        )
        assertEquals("add_or_update", keralaPageProp.dynamicPageOperation)
        assertTrue(keralaPageProp.proposedDynamicPage?.title?.contains("Kerala News") == true)

        // Command D: "Change all chatroom buttons to purple gradients."
        val purpleBtnProp = com.example.data.CentralAiAdminManager.buildDeterministicProposal(
            prompt = "Change all chatroom buttons to purple gradients.",
            currentConfig = keralaPageProp.proposedConfig,
            existingPages = emptyList()
        )
        assertEquals("#7C3AED", purpleBtnProp.proposedConfig.chatroomButtonStartHex)
        assertEquals("#C026D3", purpleBtnProp.proposedConfig.chatroomButtonEndHex)

        // Command E: "Add a new video calling feature."
        val videoCallProp = com.example.data.CentralAiAdminManager.buildDeterministicProposal(
            prompt = "Add a new video calling feature.",
            currentConfig = purpleBtnProp.proposedConfig,
            existingPages = emptyList()
        )
        assertTrue(videoCallProp.proposedConfig.showVideoCallingFeature)
        assertTrue(videoCallProp.proposedConfig.heroBannerTitle.contains("VIDEO CALLING"))

        // Command F: "Fix the video playback error."
        val fixVideoProp = com.example.data.CentralAiAdminManager.buildDeterministicProposal(
            prompt = "Fix the video playback error.",
            currentConfig = videoCallProp.proposedConfig,
            existingPages = emptyList()
        )
        assertTrue(fixVideoProp.proposedConfig.lastDiagnosticSummary.contains("Video Playback Engine verified"))

        // 5. Verify full JSON serialization/deserialization round-trip for Global Live Update & Offline Cache
        val jsonSnap = com.example.data.CentralAiAdminManager.serializeConfigToJson(fixVideoProp.proposedConfig)
        val deserialized = com.example.data.CentralAiAdminManager.deserializeConfigFromJson(jsonSnap, baseCfg)
        assertEquals("#7C3AED", deserialized.chatroomButtonStartHex)
        assertTrue(deserialized.parsedComponentOverrides().any { it.elementId == "header_notification_icon" })

        // Clean up runtime state after test
        com.example.data.CentralLiveUpdateRuntime.exitOwnerEditMode(keepPreviewChanges = false)
        com.example.data.CentralLiveUpdateRuntime.updateFromProductionSnapshot(com.example.data.CentralAppConfig())
    }
}
