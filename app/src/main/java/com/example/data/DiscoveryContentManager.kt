package com.example.data

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.snapshots
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

enum class ModerationDecision(val statusValue: String) {
    APPROVED("approved"),
    PENDING_REVIEW("pending_review"),
    REJECTED("rejected")
}

data class ModerationEvaluation(
    val decision: ModerationDecision,
    val safetyScore: Long,
    val reason: String,
    val trustedSourceVerified: Boolean,
    val copyrightVerified: Boolean
)

object DiscoverySafetyModerator {

    // Strictly prohibited terms (Adult, CSAM, Terrorism, Illegal Drugs, Self-Harm, Gore, Piracy, Scams, Fake News markers)
    private val strictlyProhibitedKeywords = setOf(
        "porn", "pornography", "xxx", "nsfw", "nude", "nudity", "onlyfans", "sex tape", "erotic",
        "escort", "camgirl", "stripper", "hentai", "adult video", "explicit sex",
        "csam", "child abuse", "pedophil",
        "terrorist", "isis", "al-qaeda", "jihadist", "bomb making", "extremist manifesto",
        "cocaine", "heroin", "fentanyl", "methamphetamine", "drug cartel", "buy weed online", "darknet market",
        "suicide challenge", "self-harm", "how to kill yourself", "blue whale challenge", "choking game",
        "beheading", "dismemberment", "snuff film", "brutal gore", "execution video",
        "torrent download", "camrip", "hdrip free download", "pirated movie", "cracked apk", "warez",
        "123movies", "putlocker", "fmovies", "thepiratebay", "yts.mx",
        "phishing", "ponzi scheme", "crypto giveaway scam", "free bitcoin generator", "carding tutorial",
        "hoax exposed", "deepfake porn", "racial slur", "white supremacy", "neo-nazi"
    )

    // Sensitive / uncertain keywords that require manual administrator review before publishing
    private val sensitiveReviewKeywords = setOf(
        "graphic warning", "viewer discretion", "unconfirmed rumor", "leaked footage",
        "disturbing footage", "unverified claim", "censored in", "banned video"
    )

    // Country-specific legal restriction rules (can be extended dynamically by Admin in Firestore)
    private val defaultCountryBlockedKeywords: Map<String, Set<String>> = mapOf(
        "IN" to setOf("online betting app", "offshore casino", "banned apps"),
        "SG" to setOf("vape shop", "e-cigarette sale", "unlicensed gambling"),
        "AE" to setOf("online casino", "sports betting odds", "dating site"),
        "SA" to setOf("online casino", "gambling bonus", "alcohol promo"),
        "DE" to setOf("unconstitutional symbol", "hate speech"),
        "GB" to setOf("unlicensed lottery", "extremist group")
    )

    val trustedDomains: Set<String> = setOf(
        "bbc.co.uk", "bbc.com", "reuters.com", "npr.org", "apnews.com", "pbs.org",
        "theguardian.com", "nytimes.com", "dw.com", "aljazeera.com", "cbsnews.com", "abcnews.go.com",
        "nasa.gov", "sciencedaily.com", "nature.com", "newscientist.com", "space.com", "phys.org",
        "techcrunch.com", "theverge.com", "arstechnica.com", "wired.com", "engadget.com", "cnet.com",
        "9to5google.com", "androidauthority.com", "gsmarena.com", "zdnet.com",
        "espn.com", "skysports.com", "cbssports.com", " sportingnews.com",
        "polygon.com", "ign.com", "gamespot.com", "eurogamer.net", "kotaku.com",
        "billboard.com", "pitchfork.com", "rollingstone.com", "nme.com", "consequence.net",
        "variety.com", "hollywoodreporter.com", "deadline.com", "indiewire.com", "collider.com",
        "screencrush.com", "bloody-disgusting.com", "fangoria.com",
        "vogue.com", "gq.com", "elle.com", "Harper'sBAZAAR.com".lowercase(), "fashionista.com",
        "edutopia.org", "ted.com", "smithsonianmag.com", "nationalgeographic.com",
        "goodhousekeeping.com", "lifehacker.com", "apartmenttherapy.com", "mindful.org",
        "youtube.com", "youtu.be", "news.google.com", "feeds.bbci.co.uk", "rss.nytimes.com", "feeds.npr.org",
        // Trusted Kerala & Indian Regional News Publishers (English & Malayalam)
        "thehindu.com", "indianexpress.com", "newindianexpress.com", "onmanorama.com", "manoramaonline.com",
        "mathrubhumi.com", "asianetnews.com", "twentyfournews.com", "kairalinewsonline.com",
        "deshabhimani.com", "madhyamam.com", "deepika.com", "keralakaumudi.com", "janmabhumi.in",
        "mediaoneonline.com", "reporterlive.com", "jaihindtv.in", "ndtv.com", "hindustantimes.com",
        "timesofindia.indiatimes.com", "indiatimes.com", "livemint.com", "moneycontrol.com",
        "mausam.imd.gov.in", "kerala.gov.in", "prd.kerala.gov.in", "sdma.kerala.gov.in"
    )

    fun extractDomain(rawUrl: String): String {
        return try {
            val host = URI(rawUrl.trim()).host?.lowercase(Locale.US).orEmpty()
            host.removePrefix("www.")
        } catch (_: Exception) {
            ""
        }
    }

    fun isTrustedSourceUrl(
        rawUrl: String,
        configuredSources: List<DiscoverySourceConfig> = emptyList(),
        blockedDomains: List<String> = emptyList()
    ): Boolean {
        val trimmed = rawUrl.trim()
        if (!trimmed.startsWith("https://", ignoreCase = true)) return false
        val domain = extractDomain(trimmed)
        if (domain.isBlank()) return false
        if (blockedDomains.any { blocked -> domain == blocked.lowercase() || domain.endsWith(".${blocked.lowercase()}") }) {
            return false
        }
        val customDomains = configuredSources
            .filter { it.enabled && it.trustedVerified }
            .mapNotNull { it.sourceDomain.lowercase().takeIf { d -> d.isNotBlank() } ?: extractDomain(it.feedUrl).takeIf { d -> d.isNotBlank() } }
            .toSet()

        val allTrusted = trustedDomains + customDomains
        return allTrusted.any { trusted ->
            domain == trusted || domain.endsWith(".$trusted")
        }
    }

    fun isPermittedInCountry(
        item: DiscoveryContentItem,
        userCountryCode: String,
        globalConfig: DiscoveryGlobalConfig? = null
    ): Boolean {
        val country = userCountryCode.trim().uppercase(Locale.US).ifBlank { "US" }
        if (country in item.blockedCountries.map { it.uppercase(Locale.US) }) return false
        if (item.allowedCountries.isNotEmpty() &&
            "ALL" !in item.allowedCountries.map { it.uppercase(Locale.US) } &&
            country !in item.allowedCountries.map { it.uppercase(Locale.US) }
        ) {
            return false
        }
        val combinedText = "${item.title} ${item.description} ${item.sourceDomain}".lowercase(Locale.US)
        val defaultCountryBlocks = defaultCountryBlockedKeywords[country].orEmpty()
        if (defaultCountryBlocks.any { kw -> combinedText.contains(kw) }) return false

        val adminCountryBlocks = globalConfig?.restrictedCountriesMap?.get(country).orEmpty()
        if (adminCountryBlocks.any { kw -> kw.isNotBlank() && combinedText.contains(kw.lowercase(Locale.US)) }) {
            return false
        }
        return true
    }

    fun evaluateContentSafety(
        title: String,
        description: String,
        originalUrl: String,
        thumbnailUrl: String,
        videoUrl: String,
        configuredSources: List<DiscoverySourceConfig> = emptyList(),
        globalConfig: DiscoveryGlobalConfig? = null
    ): ModerationEvaluation {
        val cleanTitle = title.trim()
        val cleanDesc = description.trim()
        if (cleanTitle.length < 6 || originalUrl.isBlank()) {
            return ModerationEvaluation(
                decision = ModerationDecision.REJECTED,
                safetyScore = 0L,
                reason = "Missing verifiable title or original source URL",
                trustedSourceVerified = false,
                copyrightVerified = false
            )
        }

        val blockedDomains = globalConfig?.blockedDomains.orEmpty()
        val sourceTrusted = isTrustedSourceUrl(originalUrl, configuredSources, blockedDomains)
        if (!sourceTrusted) {
            return ModerationEvaluation(
                decision = ModerationDecision.REJECTED,
                safetyScore = 10L,
                reason = "Untrusted or unverified publisher domain: ${extractDomain(originalUrl)}",
                trustedSourceVerified = false,
                copyrightVerified = false
            )
        }

        if (thumbnailUrl.isNotBlank() && !thumbnailUrl.startsWith("https://", ignoreCase = true)) {
            return ModerationEvaluation(
                decision = ModerationDecision.REJECTED,
                safetyScore = 20L,
                reason = "Insecure thumbnail URL (HTTPS required)",
                trustedSourceVerified = true,
                copyrightVerified = false
            )
        }

        val combinedText = "$cleanTitle $cleanDesc $originalUrl $thumbnailUrl $videoUrl".lowercase(Locale.US)

        for (prohibited in strictlyProhibitedKeywords) {
            if (combinedText.contains(prohibited)) {
                return ModerationEvaluation(
                    decision = ModerationDecision.REJECTED,
                    safetyScore = 0L,
                    reason = "Rejected by Safe & Positive Content Policy ($prohibited)",
                    trustedSourceVerified = true,
                    copyrightVerified = false
                )
            }
        }

        // Check copyright compliance for video links (only official embeds/publishers allowed)
        val copyrightOk = if (videoUrl.isNotBlank()) {
            val vDomain = extractDomain(videoUrl)
            vDomain in setOf("youtube.com", "youtu.be") || isTrustedSourceUrl(videoUrl, configuredSources, blockedDomains)
        } else {
            true
        }
        if (!copyrightOk) {
            return ModerationEvaluation(
                decision = ModerationDecision.REJECTED,
                safetyScore = 0L,
                reason = "Unauthorized video host — copyright verification failed",
                trustedSourceVerified = true,
                copyrightVerified = false
            )
        }

        for (sensitive in sensitiveReviewKeywords) {
            if (combinedText.contains(sensitive)) {
                return ModerationEvaluation(
                    decision = ModerationDecision.PENDING_REVIEW,
                    safetyScore = 65L,
                    reason = "Flagged for administrator review ($sensitive)",
                    trustedSourceVerified = true,
                    copyrightVerified = true
                )
            }
        }

        return ModerationEvaluation(
            decision = ModerationDecision.APPROVED,
            safetyScore = 100L,
            reason = "Verified Safe & Positive Content • Trusted Publisher",
            trustedSourceVerified = true,
            copyrightVerified = true
        )
    }
}

object DiscoveryContentManager {
    private const val TAG = "DiscoveryContentMgr"
    private const val COLLECTION_ITEMS = "discovery_items"
    private const val COLLECTION_SOURCES = "discovery_sources"
    private const val COLLECTION_CONFIG = "discovery_config"
    private const val COLLECTION_LOGS = "discovery_import_logs"
    private const val GLOBAL_CONFIG_DOC_ID = "global_config"

    private val lastCategorySyncAttemptMs = ConcurrentHashMap<String, Long>()
    private val activeCategorySyncLocks = ConcurrentHashMap.newKeySet<String>()

    fun defaultTrustedSources(): List<DiscoverySourceConfig> = listOf(
        DiscoverySourceConfig(
            id = "src_news_bbc",
            categoryKey = DiscoveryCategory.NEWS.key,
            sourceName = "BBC News Top Stories",
            sourceDomain = "bbc.co.uk",
            feedUrl = "https://feeds.bbci.co.uk/news/rss.xml",
            feedFormat = "rss",
            contentType = "article"
        ),
        DiscoverySourceConfig(
            id = "src_news_npr",
            categoryKey = DiscoveryCategory.NEWS.key,
            sourceName = "NPR News Headlines",
            sourceDomain = "npr.org",
            feedUrl = "https://feeds.npr.org/1001/rss.xml",
            feedFormat = "rss",
            contentType = "article"
        ),
        DiscoverySourceConfig(
            id = "src_world_bbc",
            categoryKey = DiscoveryCategory.WORLD_NEWS.key,
            sourceName = "BBC World News",
            sourceDomain = "bbc.co.uk",
            feedUrl = "https://feeds.bbci.co.uk/news/world/rss.xml",
            feedFormat = "rss",
            contentType = "article"
        ),
        DiscoverySourceConfig(
            id = "src_world_guardian",
            categoryKey = DiscoveryCategory.WORLD_NEWS.key,
            sourceName = "The Guardian World",
            sourceDomain = "theguardian.com",
            feedUrl = "https://www.theguardian.com/world/rss",
            feedFormat = "rss",
            contentType = "article"
        ),
        DiscoverySourceConfig(
            id = "src_entertainment_variety",
            categoryKey = DiscoveryCategory.ENTERTAINMENT.key,
            sourceName = "Variety Entertainment",
            sourceDomain = "variety.com",
            feedUrl = "https://variety.com/feed/",
            feedFormat = "rss",
            contentType = "article"
        ),
        DiscoverySourceConfig(
            id = "src_entertainment_bbc",
            categoryKey = DiscoveryCategory.ENTERTAINMENT.key,
            sourceName = "BBC Entertainment & Arts",
            sourceDomain = "bbc.co.uk",
            feedUrl = "https://feeds.bbci.co.uk/news/entertainment_and_arts/rss.xml",
            feedFormat = "rss",
            contentType = "article"
        ),
        DiscoverySourceConfig(
            id = "src_music_billboard",
            categoryKey = DiscoveryCategory.MUSIC.key,
            sourceName = "Billboard Music News",
            sourceDomain = "billboard.com",
            feedUrl = "https://www.billboard.com/feed/",
            feedFormat = "rss",
            contentType = "article"
        ),
        DiscoverySourceConfig(
            id = "src_music_nme",
            categoryKey = DiscoveryCategory.MUSIC.key,
            sourceName = "NME Music Releases",
            sourceDomain = "nme.com",
            feedUrl = "https://www.nme.com/news/music/feed",
            feedFormat = "rss",
            contentType = "article"
        ),
        DiscoverySourceConfig(
            id = "src_movies_collider",
            categoryKey = DiscoveryCategory.MOVIES.key,
            sourceName = "Collider Movie News & Trailers",
            sourceDomain = "collider.com",
            feedUrl = "https://collider.com/feed/",
            feedFormat = "rss",
            contentType = "article"
        ),
        DiscoverySourceConfig(
            id = "src_movies_deadline",
            categoryKey = DiscoveryCategory.MOVIES.key,
            sourceName = "Deadline Film Updates",
            sourceDomain = "deadline.com",
            feedUrl = "https://deadline.com/v/film/feed/",
            feedFormat = "rss",
            contentType = "article"
        ),
        DiscoverySourceConfig(
            id = "src_horror_bloodydisgusting",
            categoryKey = DiscoveryCategory.HORROR.key,
            sourceName = "Bloody Disgusting Horror Releases",
            sourceDomain = "bloody-disgusting.com",
            feedUrl = "https://bloody-disgusting.com/movies/feed/",
            feedFormat = "rss",
            contentType = "article"
        ),
        DiscoverySourceConfig(
            id = "src_gadgets_engadget",
            categoryKey = DiscoveryCategory.GADGETS.key,
            sourceName = "Engadget Gadget Launches & Reviews",
            sourceDomain = "engadget.com",
            feedUrl = "https://www.engadget.com/rss.xml",
            feedFormat = "rss",
            contentType = "article"
        ),
        DiscoverySourceConfig(
            id = "src_gadgets_verge",
            categoryKey = DiscoveryCategory.GADGETS.key,
            sourceName = "The Verge Gadgets & Hardware",
            sourceDomain = "theverge.com",
            feedUrl = "https://www.theverge.com/rss/index.xml",
            feedFormat = "atom",
            contentType = "article"
        ),
        DiscoverySourceConfig(
            id = "src_tech_techcrunch",
            categoryKey = DiscoveryCategory.TECHNOLOGY.key,
            sourceName = "TechCrunch Innovation",
            sourceDomain = "techcrunch.com",
            feedUrl = "https://techcrunch.com/feed/",
            feedFormat = "rss",
            contentType = "article"
        ),
        DiscoverySourceConfig(
            id = "src_tech_arstechnica",
            categoryKey = DiscoveryCategory.TECHNOLOGY.key,
            sourceName = "Ars Technica Technology",
            sourceDomain = "arstechnica.com",
            feedUrl = "https://feeds.arstechnica.com/arstechnica/index",
            feedFormat = "rss",
            contentType = "article"
        ),
        DiscoverySourceConfig(
            id = "src_sports_espn",
            categoryKey = DiscoveryCategory.SPORTS.key,
            sourceName = "ESPN Top Sports Headlines",
            sourceDomain = "espn.com",
            feedUrl = "https://www.espn.com/espn/rss/news",
            feedFormat = "rss",
            contentType = "article"
        ),
        DiscoverySourceConfig(
            id = "src_sports_bbc",
            categoryKey = DiscoveryCategory.SPORTS.key,
            sourceName = "BBC Sport Global",
            sourceDomain = "bbc.co.uk",
            feedUrl = "https://feeds.bbci.co.uk/sport/rss.xml",
            feedFormat = "rss",
            contentType = "article"
        ),
        DiscoverySourceConfig(
            id = "src_gaming_polygon",
            categoryKey = DiscoveryCategory.GAMING.key,
            sourceName = "Polygon Gaming News",
            sourceDomain = "polygon.com",
            feedUrl = "https://www.polygon.com/rss/index.xml",
            feedFormat = "atom",
            contentType = "article"
        ),
        DiscoverySourceConfig(
            id = "src_gaming_eurogamer",
            categoryKey = DiscoveryCategory.GAMING.key,
            sourceName = "Eurogamer Updates",
            sourceDomain = "eurogamer.net",
            feedUrl = "https://www.eurogamer.net/feed",
            feedFormat = "rss",
            contentType = "article"
        ),
        DiscoverySourceConfig(
            id = "src_comedy_consequence",
            categoryKey = DiscoveryCategory.COMEDY.key,
            sourceName = "Consequence Comedy & Pop Culture",
            sourceDomain = "consequence.net",
            feedUrl = "https://consequence.net/category/comedy/feed/",
            feedFormat = "rss",
            contentType = "article"
        ),
        DiscoverySourceConfig(
            id = "src_viral_ted",
            categoryKey = DiscoveryCategory.VIRAL_VIDEOS.key,
            sourceName = "TED Talks Official Video Feed",
            sourceDomain = "ted.com",
            feedUrl = "https://www.youtube.com/feeds/videos.xml?channel_id=UCAuUUnT6oDeKwE6v1NGQxug",
            feedFormat = "youtube_rss",
            contentType = "video"
        ),
        DiscoverySourceConfig(
            id = "src_viral_nasa_yt",
            categoryKey = DiscoveryCategory.VIRAL_VIDEOS.key,
            sourceName = "NASA Official Video Highlights",
            sourceDomain = "nasa.gov",
            feedUrl = "https://www.youtube.com/feeds/videos.xml?channel_id=UCLA_DiR1FfKNvjuUpBHmylQ",
            feedFormat = "youtube_rss",
            contentType = "video"
        ),
        DiscoverySourceConfig(
            id = "src_fashion_fashionista",
            categoryKey = DiscoveryCategory.FASHION.key,
            sourceName = "Vogue Style & Fashion",
            sourceDomain = "vogue.com",
            feedUrl = "https://www.vogue.com/feed/rss",
            feedFormat = "rss",
            contentType = "article"
        ),
        DiscoverySourceConfig(
            id = "src_education_edutopia",
            categoryKey = DiscoveryCategory.EDUCATION.key,
            sourceName = "Edutopia Learning & Education",
            sourceDomain = "edutopia.org",
            feedUrl = "https://www.edutopia.org/rss.xml",
            feedFormat = "rss",
            contentType = "article"
        ),
        DiscoverySourceConfig(
            id = "src_science_nasa",
            categoryKey = DiscoveryCategory.SCIENCE.key,
            sourceName = "NASA Breaking Science & Space",
            sourceDomain = "nasa.gov",
            feedUrl = "https://www.nasa.gov/news-release/feed/",
            feedFormat = "rss",
            contentType = "article"
        ),
        DiscoverySourceConfig(
            id = "src_science_sciencedaily",
            categoryKey = DiscoveryCategory.SCIENCE.key,
            sourceName = "ScienceDaily Discoveries",
            sourceDomain = "sciencedaily.com",
            feedUrl = "https://www.sciencedaily.com/rss/top/science.xml",
            feedFormat = "rss",
            contentType = "article"
        ),
        DiscoverySourceConfig(
            id = "src_lifestyle_lifehacker",
            categoryKey = DiscoveryCategory.LIFESTYLE.key,
            sourceName = "Lifehacker Smart Living & Wellness",
            sourceDomain = "lifehacker.com",
            feedUrl = "https://lifehacker.com/rss",
            feedFormat = "rss",
            contentType = "article"
        ),
        DiscoverySourceConfig(
            id = "src_trending_bbc",
            categoryKey = DiscoveryCategory.TRENDING.key,
            sourceName = "BBC Most Popular & Trending",
            sourceDomain = "bbc.co.uk",
            feedUrl = "https://feeds.bbci.co.uk/news/technology/rss.xml",
            feedFormat = "rss",
            contentType = "article"
        )
    ) + defaultKeralaTrustedSources()

    fun defaultKeralaTrustedSources(): List<DiscoverySourceConfig> = listOf(
        DiscoverySourceConfig(
            id = "src_kerala_latest_thehindu",
            categoryKey = KeralaNewsCategory.KERALA_LATEST.key,
            sourceName = "The Hindu – Kerala Latest",
            sourceDomain = "thehindu.com",
            feedUrl = "https://www.thehindu.com/news/national/kerala/feeder/default.rss",
            feedFormat = "rss",
            contentType = "article",
            language = "en",
            isKeralaSource = true
        ),
        DiscoverySourceConfig(
            id = "src_kerala_latest_indianexpress",
            categoryKey = KeralaNewsCategory.KERALA_LATEST.key,
            sourceName = "Indian Express – Kerala News",
            sourceDomain = "indianexpress.com",
            feedUrl = "https://indianexpress.com/section/india/kerala/feed/",
            feedFormat = "rss",
            contentType = "article",
            language = "en",
            isKeralaSource = true
        ),
        DiscoverySourceConfig(
            id = "src_kerala_latest_malayalam_gn",
            categoryKey = KeralaNewsCategory.KERALA_LATEST.key,
            sourceName = "കേരള വാർത്തകൾ (Malayalam Live Feed)",
            sourceDomain = "news.google.com",
            feedUrl = "https://news.google.com/rss/search?q=Kerala+when:1d&hl=ml&gl=IN&ceid=IN:ml",
            feedFormat = "rss",
            contentType = "article",
            language = "ml",
            isKeralaSource = true
        ),
        DiscoverySourceConfig(
            id = "src_kerala_breaking_en",
            categoryKey = KeralaNewsCategory.KERALA_BREAKING.key,
            sourceName = "Kerala Breaking News Live",
            sourceDomain = "news.google.com",
            feedUrl = "https://news.google.com/rss/search?q=Kerala+Breaking+News+when:1d&hl=en-IN&gl=IN&ceid=IN:en",
            feedFormat = "rss",
            contentType = "article",
            language = "en",
            isKeralaSource = true
        ),
        DiscoverySourceConfig(
            id = "src_kerala_breaking_ml",
            categoryKey = KeralaNewsCategory.KERALA_BREAKING.key,
            sourceName = "കേരള ബ്രേക്കിംഗ് ന്യൂസ് (Malayalam)",
            sourceDomain = "news.google.com",
            feedUrl = "https://news.google.com/rss/search?q=%E0%B4%95%E0%B5%87%E0%B4%B0%E0%B4%B3%E0%B4%82+when:1d&hl=ml&gl=IN&ceid=IN:ml",
            feedFormat = "rss",
            contentType = "article",
            language = "ml",
            isKeralaSource = true
        ),
        DiscoverySourceConfig(
            id = "src_kerala_politics_en",
            categoryKey = KeralaNewsCategory.KERALA_POLITICS.key,
            sourceName = "Kerala Politics & Assembly Desk",
            sourceDomain = "news.google.com",
            feedUrl = "https://news.google.com/rss/search?q=Kerala+Politics+Government+Assembly+when:2d&hl=en-IN&gl=IN&ceid=IN:en",
            feedFormat = "rss",
            contentType = "article",
            language = "en",
            isKeralaSource = true
        ),
        DiscoverySourceConfig(
            id = "src_kerala_politics_ml",
            categoryKey = KeralaNewsCategory.KERALA_POLITICS.key,
            sourceName = "കേരള രാഷ്ട്രീയം (Politics Malayalam)",
            sourceDomain = "news.google.com",
            feedUrl = "https://news.google.com/rss/search?q=Kerala+Politics&hl=ml&gl=IN&ceid=IN:ml",
            feedFormat = "rss",
            contentType = "article",
            language = "ml",
            isKeralaSource = true
        ),
        DiscoverySourceConfig(
            id = "src_kerala_education_en",
            categoryKey = KeralaNewsCategory.KERALA_EDUCATION.key,
            sourceName = "Kerala Education, Universities & Exams",
            sourceDomain = "news.google.com",
            feedUrl = "https://news.google.com/rss/search?q=Kerala+Education+School+University+Exam+when:3d&hl=en-IN&gl=IN&ceid=IN:en",
            feedFormat = "rss",
            contentType = "article",
            language = "en",
            isKeralaSource = true
        ),
        DiscoverySourceConfig(
            id = "src_kerala_technology_en",
            categoryKey = KeralaNewsCategory.KERALA_TECH.key,
            sourceName = "Kerala IT, Technopark & Infopark News",
            sourceDomain = "news.google.com",
            feedUrl = "https://news.google.com/rss/search?q=Kerala+Technology+Technopark+Infopark+Startup+when:7d&hl=en-IN&gl=IN&ceid=IN:en",
            feedFormat = "rss",
            contentType = "article",
            language = "en",
            isKeralaSource = true
        ),
        DiscoverySourceConfig(
            id = "src_kerala_sports_en",
            categoryKey = KeralaNewsCategory.KERALA_SPORTS.key,
            sourceName = "Kerala Sports, Blasters & Athletics",
            sourceDomain = "news.google.com",
            feedUrl = "https://news.google.com/rss/search?q=Kerala+Sports+Blasters+Football+Cricket+when:3d&hl=en-IN&gl=IN&ceid=IN:en",
            feedFormat = "rss",
            contentType = "article",
            language = "en",
            isKeralaSource = true
        ),
        DiscoverySourceConfig(
            id = "src_kerala_entertainment_en",
            categoryKey = KeralaNewsCategory.KERALA_ENTERTAINMENT.key,
            sourceName = "Mollywood & Malayalam Cinema News",
            sourceDomain = "news.google.com",
            feedUrl = "https://news.google.com/rss/search?q=Malayalam+Cinema+Movie+Mollywood+Kerala+when:3d&hl=en-IN&gl=IN&ceid=IN:en",
            feedFormat = "rss",
            contentType = "article",
            language = "en",
            isKeralaSource = true
        ),
        DiscoverySourceConfig(
            id = "src_kerala_entertainment_ml",
            categoryKey = KeralaNewsCategory.KERALA_ENTERTAINMENT.key,
            sourceName = "മലയാള സിനിമ വാർത്തകൾ (Malayalam Cinema)",
            sourceDomain = "news.google.com",
            feedUrl = "https://news.google.com/rss/search?q=Malayalam+Cinema&hl=ml&gl=IN&ceid=IN:ml",
            feedFormat = "rss",
            contentType = "article",
            language = "ml",
            isKeralaSource = true
        ),
        DiscoverySourceConfig(
            id = "src_kerala_business_en",
            categoryKey = KeralaNewsCategory.KERALA_BUSINESS.key,
            sourceName = "Kerala Business, Economy & Gold Rate",
            sourceDomain = "news.google.com",
            feedUrl = "https://news.google.com/rss/search?q=Kerala+Business+Economy+Kochi+Port+Tourism+when:3d&hl=en-IN&gl=IN&ceid=IN:en",
            feedFormat = "rss",
            contentType = "article",
            language = "en",
            isKeralaSource = true
        ),
        DiscoverySourceConfig(
            id = "src_kerala_district_en",
            categoryKey = KeralaNewsCategory.KERALA_DISTRICT.key,
            sourceName = "Kerala 14 Districts Live Desk",
            sourceDomain = "news.google.com",
            feedUrl = "https://news.google.com/rss/search?q=Thiruvananthapuram+OR+Kochi+OR+Kozhikode+OR+Thrissur+OR+Kannur+OR+Kollam+OR+Alappuzha+OR+Kottayam+OR+Palakkad+OR+Malappuram+OR+Wayanad+OR+Idukki+OR+Pathanamthitta+OR+Kasaragod+when:2d&hl=en-IN&gl=IN&ceid=IN:en",
            feedFormat = "rss",
            contentType = "article",
            language = "en",
            isKeralaSource = true
        ),
        DiscoverySourceConfig(
            id = "src_kerala_district_ml",
            categoryKey = KeralaNewsCategory.KERALA_DISTRICT.key,
            sourceName = "ജില്ലാ വാർത്തകൾ (Kerala Districts Malayalam)",
            sourceDomain = "news.google.com",
            feedUrl = "https://news.google.com/rss/search?q=Thiruvananthapuram+OR+Kochi+OR+Kozhikode+OR+Thrissur+OR+Kannur&hl=ml&gl=IN&ceid=IN:ml",
            feedFormat = "rss",
            contentType = "article",
            language = "ml",
            isKeralaSource = true
        ),
        DiscoverySourceConfig(
            id = "src_kerala_weather_en",
            categoryKey = KeralaNewsCategory.KERALA_WEATHER.key,
            sourceName = "Kerala Weather, IMD Rain Alerts & Public Info",
            sourceDomain = "news.google.com",
            feedUrl = "https://news.google.com/rss/search?q=Kerala+Weather+Rain+Alert+IMD+Monsoon+when:3d&hl=en-IN&gl=IN&ceid=IN:en",
            feedFormat = "rss",
            contentType = "article",
            language = "en",
            isKeralaSource = true
        ),
        DiscoverySourceConfig(
            id = "src_kerala_weather_ml",
            categoryKey = KeralaNewsCategory.KERALA_WEATHER.key,
            sourceName = "കേരള കാലാവസ്ഥ & അറിയിപ്പുകൾ (Weather Malayalam)",
            sourceDomain = "news.google.com",
            feedUrl = "https://news.google.com/rss/search?q=Kerala+Weather+Rain&hl=ml&gl=IN&ceid=IN:ml",
            feedFormat = "rss",
            contentType = "article",
            language = "ml",
            isKeralaSource = true
        )
    )

    private val keralaDistrictKeywordMap: Map<String, List<String>> = mapOf(
        "Thiruvananthapuram" to listOf("thiruvananthapuram", "trivandrum", "kovalam", "varkala", "neyyattinkara", "attukal", "technopark", "തിരുവനന്തപുരം"),
        "Kollam" to listOf("kollam", "quilon", "punalur", "karunagappally", "paravur", "ashtamudi", "കൊല്ലം"),
        "Pathanamthitta" to listOf("pathanamthitta", "sabarimala", "thiruvalla", "adoor", "ranni", "konni", "പത്തനംതിട്ട", "ശബരിമല"),
        "Alappuzha" to listOf("alappuzha", "alleppey", "cherthala", "kayamkulam", "chengannur", "kuttanad", "ambalapuzha", "ആലപ്പുഴ", "കുട്ടനാട്"),
        "Kottayam" to listOf("kottayam", "pala", "changanassery", "kumarakom", "vaikom", "ettumanoor", "കോട്ടയം", "പാലാ"),
        "Idukki" to listOf("idukki", "munnar", "thekkady", "thodupuzha", "kattappana", "wagamon", "ഇടുക്കി", "മൂന്നാർ"),
        "Ernakulam" to listOf("ernakulam", "kochi", "cochin", "aluva", "angamaly", "muvattupuzha", "kakkanad", "infopark", "perumbavoor", "എറണാകുളം", "കൊച്ചി"),
        "Thrissur" to listOf("thrissur", "trichur", "guruvayur", "chalakudy", "irinjalakuda", "kodungallur", "kunnamkulam", "തൃശ്ശൂർ", "തൃശൂർ", "ഗുരുവായൂർ"),
        "Palakkad" to listOf("palakkad", "palghat", "ottapalam", "shoranur", "chittur", "mannarkkad", "walayar", "പാലക്കാട്"),
        "Malappuram" to listOf("malappuram", "manjeri", "tirur", "perinthalmanna", "ponnani", "nilambur", "kottakkal", "karipur", "മലപ്പുറം"),
        "Kozhikode" to listOf("kozhikode", "calicut", "vadakara", "koyilandy", "thamarassery", "beypore", "കോഴിക്കോട്"),
        "Wayanad" to listOf("wayanad", "kalpetta", "sulthan bathery", "mananthavady", "chooralmala", "meppadi", "വയനാട്", "കൽപ്പറ്റ"),
        "Kannur" to listOf("kannur", "cannanore", "thalassery", "payyanur", "taliparamba", "mattannur", "കണ്ണൂർ", "തലശ്ശേരി"),
        "Kasaragod" to listOf("kasaragod", "kasargod", "kanhangad", "bekal", "manjeshwaram", "nileshwaram", "കാസർകോട്", "കാസറഗോഡ്")
    )

    fun detectKeralaDistrict(title: String, description: String): String {
        val combined = "$title $description".lowercase(Locale.US)
        for ((district, keywords) in keralaDistrictKeywordMap) {
            if (keywords.any { kw -> combined.contains(kw) }) {
                return district
            }
        }
        return "All Districts"
    }

    fun detectArticleLanguage(title: String, description: String, fallbackLanguage: String = "en"): String {
        val hasMalayalamUnicode = title.any { it in '\u0D00'..'\u0D7F' } || description.any { it in '\u0D00'..'\u0D7F' }
        if (hasMalayalamUnicode) return "ml"
        return fallbackLanguage.ifBlank { "en" }.lowercase(Locale.US)
    }

    fun detectBreakingNews(title: String, categoryKey: String): Boolean {
        if (categoryKey == KeralaNewsCategory.KERALA_BREAKING.key) return true
        val lower = title.lowercase(Locale.US)
        return lower.contains("breaking") ||
            lower.contains("just in") ||
            lower.contains("urgent") ||
            lower.contains("red alert") ||
            lower.contains("orange alert") ||
            lower.contains("flash:") ||
            title.contains("ബ്രേക്കിംഗ്") ||
            title.contains("അടിയന്തര") ||
            title.contains("മുന്നറിയിപ്പ്")
    }

    fun observeDiscoveryGlobalConfig(db: FirebaseFirestore): Flow<DiscoveryGlobalConfig> {
        return db.collection(COLLECTION_CONFIG)
            .document(GLOBAL_CONFIG_DOC_ID)
            .snapshots()
            .map { doc ->
                doc.toObject(DiscoveryGlobalConfig::class.java)?.copy(id = doc.id)
                    ?: DiscoveryGlobalConfig(id = GLOBAL_CONFIG_DOC_ID)
            }
            .catch { emit(DiscoveryGlobalConfig(id = GLOBAL_CONFIG_DOC_ID)) }
    }

    fun observeDiscoverySources(db: FirebaseFirestore): Flow<List<DiscoverySourceConfig>> {
        return db.collection(COLLECTION_SOURCES)
            .whereEqualTo("appScope", "noise_live_public")
            .snapshots()
            .map { snap ->
                val remote = snap.documents.mapNotNull { doc ->
                    doc.toObject(DiscoverySourceConfig::class.java)?.copy(id = doc.id)
                }
                val defaultsById = defaultTrustedSources().associateBy { it.id }
                val remoteIds = remote.map { it.id }.toSet()
                val merged = remote + defaultsById.values.filter { it.id !in remoteIds }
                merged.sortedWith(compareBy({ it.categoryKey }, { it.sourceName }))
            }
            .catch { emit(defaultTrustedSources()) }
    }

    fun observeDiscoveryImportLogs(db: FirebaseFirestore): Flow<List<DiscoveryImportLog>> {
        return db.collection(COLLECTION_LOGS)
            .whereEqualTo("appScope", "noise_live_public")
            .snapshots()
            .map { snap ->
                snap.documents.mapNotNull { doc ->
                    doc.toObject(DiscoveryImportLog::class.java)?.copy(id = doc.id)
                }.sortedByDescending { it.timestampMillis }.take(40)
            }
            .catch { emit(emptyList()) }
    }

    fun observeDiscoveryItems(
        db: FirebaseFirestore,
        userCountryCode: String = Locale.getDefault().country.ifBlank { "US" }
    ): Flow<List<DiscoveryContentItem>> {
        return db.collection(COLLECTION_ITEMS)
            .whereEqualTo("appScope", "noise_live_public")
            .snapshots()
            .map { snap ->
                snap.documents.mapNotNull { doc ->
                    doc.toObject(DiscoveryContentItem::class.java)?.copy(id = doc.id)
                }.filter { item ->
                    item.moderationStatus == ModerationDecision.APPROVED.statusValue &&
                        item.trustedSourceVerified &&
                        item.copyrightVerified &&
                        item.title.isNotBlank() &&
                        item.originalUrl.isNotBlank() &&
                        DiscoverySafetyModerator.isPermittedInCountry(item, userCountryCode)
                }.sortedByDescending { it.publishedAtMillis }
            }
            .catch { e ->
                Log.w(TAG, "Error observing discovery items: ${e.message}")
                emit(emptyList())
            }
    }

    fun observeAllDiscoveryItemsForAdmin(db: FirebaseFirestore): Flow<List<DiscoveryContentItem>> {
        return db.collection(COLLECTION_ITEMS)
            .whereEqualTo("appScope", "noise_live_public")
            .snapshots()
            .map { snap ->
                snap.documents.mapNotNull { doc ->
                    doc.toObject(DiscoveryContentItem::class.java)?.copy(id = doc.id)
                }.sortedByDescending { it.importedAtMillis }
            }
            .catch { emit(emptyList()) }
    }

    suspend fun syncCategoryIfNeeded(
        db: FirebaseFirestore,
        category: DiscoveryCategory,
        forceRefresh: Boolean = false,
        userCountryCode: String = Locale.getDefault().country.ifBlank { "US" }
    ): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val uid = Firebase.auth.currentUser?.uid ?: return@runCatching 0
            val lockKey = category.key
            if (!activeCategorySyncLocks.add(lockKey)) {
                return@runCatching 0
            }
            try {
                val nowMs = System.currentTimeMillis()
                val globalConfig = runCatching {
                    db.collection(COLLECTION_CONFIG).document(GLOBAL_CONFIG_DOC_ID).get().await()
                        .toObject(DiscoveryGlobalConfig::class.java)
                }.getOrNull() ?: DiscoveryGlobalConfig(id = GLOBAL_CONFIG_DOC_ID)

                if (category.key in globalConfig.disabledCategories) {
                    return@runCatching 0
                }

                val intervalMs = (globalConfig.refreshIntervalMinutes.coerceIn(15L, 30L)) * 60_000L
                val lastAttempt = lastCategorySyncAttemptMs[lockKey] ?: 0L
                if (!forceRefresh && (nowMs - lastAttempt) < 90_000L) {
                    return@runCatching 0
                }

                // Step 1 & 2: Check whether fresh content is already available in Firestore
                if (!forceRefresh) {
                    val existingSnap = runCatching {
                        db.collection(COLLECTION_ITEMS)
                            .whereEqualTo("appScope", "noise_live_public")
                            .get()
                            .await()
                    }.getOrNull()
                    val matchingFresh = existingSnap?.documents?.mapNotNull { doc ->
                        doc.toObject(DiscoveryContentItem::class.java)
                    }?.filter { item ->
                        item.moderationStatus == ModerationDecision.APPROVED.statusValue &&
                            (category == DiscoveryCategory.FOR_YOU ||
                                category == DiscoveryCategory.TRENDING ||
                                item.categoryKey == category.key) &&
                            (nowMs - item.importedAtMillis) < intervalMs
                    }.orEmpty()

                    if (matchingFresh.size >= 4) {
                        lastCategorySyncAttemptMs[lockKey] = nowMs
                        return@runCatching 0
                    }
                }

                lastCategorySyncAttemptMs[lockKey] = nowMs

                // Step 3 & 4: Resolve configured sources for this category
                val remoteSources = runCatching {
                    db.collection(COLLECTION_SOURCES)
                        .whereEqualTo("appScope", "noise_live_public")
                        .get()
                        .await()
                        .documents
                        .mapNotNull { it.toObject(DiscoverySourceConfig::class.java)?.copy(id = it.id) }
                }.getOrDefault(emptyList())

                val defaultMap = defaultTrustedSources().associateBy { it.id }
                val remoteIds = remoteSources.map { it.id }.toSet()
                val allSources = (remoteSources + defaultMap.values.filter { it.id !in remoteIds })
                    .filter { it.enabled && it.trustedVerified }

                val targetSources = when (category) {
                    DiscoveryCategory.FOR_YOU -> {
                        allSources.filter {
                            it.categoryKey in setOf(
                                DiscoveryCategory.NEWS.key,
                                DiscoveryCategory.TECHNOLOGY.key,
                                DiscoveryCategory.ENTERTAINMENT.key,
                                DiscoveryCategory.SCIENCE.key,
                                DiscoveryCategory.SPORTS.key,
                                DiscoveryCategory.MUSIC.key
                            )
                        }.take(5)
                    }
                    DiscoveryCategory.TRENDING -> {
                        allSources.filter {
                            it.categoryKey in setOf(
                                DiscoveryCategory.TRENDING.key,
                                DiscoveryCategory.NEWS.key,
                                DiscoveryCategory.VIRAL_VIDEOS.key,
                                DiscoveryCategory.TECHNOLOGY.key,
                                DiscoveryCategory.GAMING.key
                            )
                        }.take(5)
                    }
                    else -> allSources.filter { it.categoryKey == category.key }
                }

                var totalSaved = 0
                for (source in targetSources) {
                    val fetchResult = fetchAndImportSingleSource(
                        db = db,
                        source = source,
                        allConfiguredSources = allSources,
                        globalConfig = globalConfig,
                        userCountryCode = userCountryCode,
                        actorUid = uid
                    )
                    totalSaved += fetchResult
                }
                totalSaved
            } finally {
                activeCategorySyncLocks.remove(lockKey)
            }
        }
    }

    private suspend fun fetchAndImportSingleSource(
        db: FirebaseFirestore,
        source: DiscoverySourceConfig,
        allConfiguredSources: List<DiscoverySourceConfig>,
        globalConfig: DiscoveryGlobalConfig,
        userCountryCode: String,
        actorUid: String
    ): Int {
        val nowMs = System.currentTimeMillis()
        var fetchedCount = 0L
        var savedCount = 0L
        var duplicatesSkipped = 0L
        var safetyRejected = 0L
        var errorMsg = ""

        try {
            val rawXml = downloadFeedString(source.feedUrl)
            val parsedEntries = parseSyndicationFeed(rawXml, source)
            fetchedCount = parsedEntries.size.toLong()

            for (rawEntry in parsedEntries.take(12)) {
                val docId = buildDeterministicDiscoveryId(rawEntry.originalUrl)
                val evaluation = DiscoverySafetyModerator.evaluateContentSafety(
                    title = rawEntry.title,
                    description = rawEntry.description,
                    originalUrl = rawEntry.originalUrl,
                    thumbnailUrl = rawEntry.thumbnailUrl,
                    videoUrl = rawEntry.videoEmbedUrl,
                    configuredSources = allConfiguredSources,
                    globalConfig = globalConfig
                )

                if (evaluation.decision == ModerationDecision.REJECTED) {
                    safetyRejected++
                    continue
                }

                val candidateItem = rawEntry.copy(
                    id = docId,
                    moderationStatus = evaluation.decision.statusValue,
                    moderationNotes = evaluation.reason,
                    safetyScore = evaluation.safetyScore,
                    trustedSourceVerified = evaluation.trustedSourceVerified,
                    copyrightVerified = evaluation.copyrightVerified,
                    importedAtMillis = nowMs
                )

                if (!DiscoverySafetyModerator.isPermittedInCountry(candidateItem, userCountryCode, globalConfig)) {
                    safetyRejected++
                    continue
                }

                val docRef = db.collection(COLLECTION_ITEMS).document(docId)
                val existingSnap = runCatching { docRef.get().await() }.getOrNull()
                if (existingSnap != null && existingSnap.exists()) {
                    // Refresh timestamp so cache stays warm without overwriting likes/saves or admin moderation status
                    val existingStatus = existingSnap.getString("moderationStatus").orEmpty()
                    if (existingStatus == "hidden" || existingStatus == "rejected") {
                        duplicatesSkipped++
                        continue
                    }
                    runCatching {
                        docRef.update(
                            mapOf(
                                "importedAtMillis" to nowMs,
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        ).await()
                    }
                    duplicatesSkipped++
                    continue
                }

                val payload = mapOf(
                    "appScope" to "noise_live_public",
                    "categoryKey" to candidateItem.categoryKey,
                    "categoryLabel" to candidateItem.categoryLabel,
                    "title" to candidateItem.title,
                    "description" to candidateItem.description,
                    "thumbnailUrl" to candidateItem.thumbnailUrl,
                    "sourceName" to candidateItem.sourceName,
                    "sourceDomain" to candidateItem.sourceDomain,
                    "originalUrl" to candidateItem.originalUrl,
                    "contentType" to candidateItem.contentType,
                    "videoEmbedUrl" to candidateItem.videoEmbedUrl,
                    "youtubeVideoId" to candidateItem.youtubeVideoId,
                    "publishedAtMillis" to candidateItem.publishedAtMillis,
                    "publishedDateLabel" to candidateItem.publishedDateLabel,
                    "importedAtMillis" to nowMs,
                    "moderationStatus" to candidateItem.moderationStatus,
                    "moderationNotes" to candidateItem.moderationNotes,
                    "safetyScore" to candidateItem.safetyScore,
                    "trustedSourceVerified" to candidateItem.trustedSourceVerified,
                    "copyrightVerified" to candidateItem.copyrightVerified,
                    "allowedCountries" to candidateItem.allowedCountries,
                    "blockedCountries" to candidateItem.blockedCountries,
                    "language" to candidateItem.language,
                    "district" to candidateItem.district,
                    "isKeralaNews" to candidateItem.isKeralaNews,
                    "isBreakingNews" to candidateItem.isBreakingNews,
                    "likesCount" to 0L,
                    "savesCount" to 0L,
                    "sharesCount" to 0L,
                    "viewsCount" to 0L,
                    "likedByUids" to emptyList<String>(),
                    "savedByUids" to emptyList<String>(),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                runCatching {
                    docRef.set(payload).await()
                    savedCount++
                }.onFailure { e ->
                    Log.w(TAG, "Failed saving discovery item $docId: ${e.message}")
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            errorMsg = e.localizedMessage ?: e.javaClass.simpleName
            Log.w(TAG, "Feed import failed for ${source.sourceName}: $errorMsg")
        }

        // Write import log if actor is signed in
        if (actorUid.isNotBlank()) {
            val logId = "log_${nowMs}_${source.id.takeLast(8)}"
            val logPayload = mapOf(
                "appScope" to "noise_live_public",
                "categoryKey" to source.categoryKey,
                "sourceName" to source.sourceName,
                "feedUrl" to source.feedUrl,
                "status" to if (errorMsg.isNotBlank()) "error" else "success",
                "itemsFetched" to fetchedCount,
                "itemsSaved" to savedCount,
                "duplicatesSkipped" to duplicatesSkipped,
                "safetyRejected" to safetyRejected,
                "errorMessage" to errorMsg,
                "timestampMillis" to nowMs,
                "createdAt" to FieldValue.serverTimestamp()
            )
            runCatching {
                db.collection(COLLECTION_LOGS).document(logId).set(logPayload).await()
            }
        }

        return savedCount.toInt()
    }

    suspend fun syncKeralaNewsIfNeeded(
        db: FirebaseFirestore,
        keralaCategory: KeralaNewsCategory,
        forceRefresh: Boolean = false,
        userCountryCode: String = Locale.getDefault().country.ifBlank { "IN" }
    ): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val uid = Firebase.auth.currentUser?.uid ?: return@runCatching 0
            val lockKey = "kerala_${keralaCategory.key}"
            if (!activeCategorySyncLocks.add(lockKey)) {
                return@runCatching 0
            }
            try {
                val nowMs = System.currentTimeMillis()
                val globalConfig = runCatching {
                    db.collection(COLLECTION_CONFIG).document(GLOBAL_CONFIG_DOC_ID).get().await()
                        .toObject(DiscoveryGlobalConfig::class.java)
                }.getOrNull() ?: DiscoveryGlobalConfig(id = GLOBAL_CONFIG_DOC_ID)

                if (keralaCategory.key in globalConfig.disabledKeralaCategories) {
                    return@runCatching 0
                }

                val intervalMs = (globalConfig.refreshIntervalMinutes.coerceIn(15L, 30L)) * 60_000L
                val lastAttempt = lastCategorySyncAttemptMs[lockKey] ?: 0L
                if (!forceRefresh && (nowMs - lastAttempt) < 60_000L) {
                    return@runCatching 0
                }

                if (!forceRefresh) {
                    val existingSnap = runCatching {
                        db.collection(COLLECTION_ITEMS)
                            .whereEqualTo("appScope", "noise_live_public")
                            .get()
                            .await()
                    }.getOrNull()
                    val matchingFresh = existingSnap?.documents?.mapNotNull { doc ->
                        doc.toObject(DiscoveryContentItem::class.java)
                    }?.filter { item ->
                        item.moderationStatus == ModerationDecision.APPROVED.statusValue &&
                            item.categoryKey == keralaCategory.key &&
                            (nowMs - item.importedAtMillis) < intervalMs
                    }.orEmpty()

                    if (matchingFresh.size >= 4) {
                        lastCategorySyncAttemptMs[lockKey] = nowMs
                        return@runCatching 0
                    }
                }

                lastCategorySyncAttemptMs[lockKey] = nowMs

                val remoteSources = runCatching {
                    db.collection(COLLECTION_SOURCES)
                        .whereEqualTo("appScope", "noise_live_public")
                        .get()
                        .await()
                        .documents
                        .mapNotNull { it.toObject(DiscoverySourceConfig::class.java)?.copy(id = it.id) }
                }.getOrDefault(emptyList())

                val defaultMap = defaultTrustedSources().associateBy { it.id }
                val remoteIds = remoteSources.map { it.id }.toSet()
                val allSources = (remoteSources + defaultMap.values.filter { it.id !in remoteIds })
                    .filter { it.enabled && it.trustedVerified }

                val targetSources = allSources.filter { it.categoryKey == keralaCategory.key }
                    .ifEmpty {
                        defaultKeralaTrustedSources().filter { it.categoryKey == keralaCategory.key }
                    }

                var totalSaved = 0
                for (source in targetSources) {
                    val saved = fetchAndImportSingleSource(
                        db = db,
                        source = source,
                        allConfiguredSources = allSources,
                        globalConfig = globalConfig,
                        userCountryCode = userCountryCode,
                        actorUid = uid
                    )
                    totalSaved += saved
                }
                totalSaved
            } finally {
                activeCategorySyncLocks.remove(lockKey)
            }
        }
    }

    private fun downloadFeedString(feedUrl: String): String {
        val url = URL(feedUrl)
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 9_000
            readTimeout = 9_000
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", "NoiseLiveDiscoveryBot/2.0 (+https://noiselive.app)")
            setRequestProperty("Accept", "application/rss+xml, application/atom+xml, application/xml, text/xml, */*")
        }
        try {
            val status = conn.responseCode
            if (status !in 200..299) {
                throw IllegalStateException("HTTP $status from ${url.host}")
            }
            BufferedReader(InputStreamReader(conn.inputStream, Charsets.UTF_8)).use { reader ->
                val sb = StringBuilder()
                var line: String? = reader.readLine()
                while (line != null && sb.length < 450_000) {
                    sb.append(line).append('\n')
                    line = reader.readLine()
                }
                return sb.toString()
            }
        } finally {
            conn.disconnect()
        }
    }

    internal fun parseSyndicationFeed(
        rawXml: String,
        source: DiscoverySourceConfig
    ): List<DiscoveryContentItem> {
        if (rawXml.isBlank()) return emptyList()
        val isKerala = source.isKeralaSource || source.categoryKey.startsWith("kerala_", ignoreCase = true)
        val resolvedCategoryKey: String
        val resolvedCategoryLabel: String
        if (isKerala) {
            val keralaCat = KeralaNewsCategory.fromKey(source.categoryKey)
            resolvedCategoryKey = keralaCat.key
            resolvedCategoryLabel = keralaCat.label
        } else {
            val categoryEnum = DiscoveryCategory.fromKey(source.categoryKey)
            resolvedCategoryKey = categoryEnum.key
            resolvedCategoryLabel = categoryEnum.label
        }
        val items = mutableListOf<DiscoveryContentItem>()
        val seenTitleKeys = mutableSetOf<String>()

        // Match RSS <item>...</item> or Atom <entry>...</entry>
        val blocks = Regex("<(item|entry)\\b[^>]*>([\\s\\S]*?)</\\1>", RegexOption.IGNORE_CASE)
            .findAll(rawXml)
            .map { it.groupValues[2] }
            .toList()

        for (block in blocks) {
            val rawTitle = extractXmlTagText(block, "title")
            val cleanTitle = cleanHtmlAndEntities(rawTitle).take(220)
            if (cleanTitle.length < 6) continue

            // Deduplicate by normalized headline within the same feed
            val normalizedHeadlineKey = cleanTitle.lowercase(Locale.US).replace(Regex("[^a-z0-9\\u0D00-\\u0D7F]"), "")
            if (normalizedHeadlineKey.length >= 8 && !seenTitleKeys.add(normalizedHeadlineKey)) {
                continue
            }

            val link = extractFeedLink(block)
            if (!link.startsWith("https://", ignoreCase = true)) continue

            // Extract <source> tag if present (e.g. Google News RSS includes <source url="https://www.mathrubhumi.com">Mathrubhumi</source>)
            val inlineSourceName = cleanHtmlAndEntities(extractXmlTagText(block, "source")).trim()
            val inlineSourceUrl = Regex("<source\\b[^>]*url=[\"']([^\"']+)[\"']", RegexOption.IGNORE_CASE)
                .find(block)?.groupValues?.getOrNull(1).orEmpty().trim()

            val rawDesc = extractXmlTagText(block, "description")
                .ifBlank { extractXmlTagText(block, "summary") }
                .ifBlank { extractXmlTagText(block, "media:description") }
                .ifBlank { extractXmlTagText(block, "content:encoded") }
                .ifBlank { extractXmlTagText(block, "content") }

            val cleanDesc = cleanHtmlAndEntities(rawDesc)
                .replace(Regex("\\s+"), " ")
                .trim()
                .take(320)
                .ifBlank { "Tap to read the full verified update from ${inlineSourceName.ifBlank { source.sourceName }}." }

            val ytVideoId = extractXmlTagText(block, "yt:videoId")
                .ifBlank { extractYouTubeIdFromLink(link) }

            val thumbUrl = extractFeedThumbnail(block, rawDesc, ytVideoId)
            val pubRaw = extractXmlTagText(block, "pubDate")
                .ifBlank { extractXmlTagText(block, "published") }
                .ifBlank { extractXmlTagText(block, "updated") }
                .ifBlank { extractXmlTagText(block, "dc:date") }

            val pubMillis = parseSyndicationDateMillis(pubRaw)
            val pubLabel = formatRelativeDateLabel(pubMillis)

            val isVideo = source.contentType == "video" || ytVideoId.isNotBlank()
            val videoEmbed = if (ytVideoId.isNotBlank()) {
                "https://www.youtube.com/embed/$ytVideoId"
            } else {
                ""
            }

            val domain = DiscoverySafetyModerator.extractDomain(inlineSourceUrl)
                .ifBlank { DiscoverySafetyModerator.extractDomain(link) }
                .ifBlank { source.sourceDomain }
            val publisherName = inlineSourceName.ifBlank { source.sourceName }
            val detectedLang = detectArticleLanguage(cleanTitle, cleanDesc, source.language)
            val detectedDistrict = if (isKerala) detectKeralaDistrict(cleanTitle, cleanDesc) else "All Districts"
            val breaking = if (isKerala) detectBreakingNews(cleanTitle, resolvedCategoryKey) else false

            items.add(
                DiscoveryContentItem(
                    id = buildDeterministicDiscoveryId(link),
                    categoryKey = resolvedCategoryKey,
                    categoryLabel = resolvedCategoryLabel,
                    title = cleanTitle,
                    description = cleanDesc,
                    thumbnailUrl = thumbUrl,
                    sourceName = publisherName,
                    sourceDomain = domain,
                    originalUrl = link,
                    contentType = if (isVideo) "video" else "article",
                    videoEmbedUrl = videoEmbed,
                    youtubeVideoId = ytVideoId,
                    publishedAtMillis = pubMillis,
                    publishedDateLabel = pubLabel,
                    importedAtMillis = System.currentTimeMillis(),
                    language = detectedLang,
                    district = detectedDistrict,
                    isKeralaNews = isKerala,
                    isBreakingNews = breaking
                )
            )
        }
        return items
    }

    private fun extractYouTubeIdFromLink(rawUrl: String): String {
        val trimmed = rawUrl.trim()
        if (trimmed.isBlank()) return ""
        val watchMatch = Regex("[?&]v=([a-zA-Z0-9_-]{11})").find(trimmed)?.groupValues?.getOrNull(1)
        if (!watchMatch.isNullOrBlank()) return watchMatch
        val shortMatch = Regex("youtu\\.be/([a-zA-Z0-9_-]{11})").find(trimmed)?.groupValues?.getOrNull(1)
        if (!shortMatch.isNullOrBlank()) return shortMatch
        val embedMatch = Regex("youtube\\.com/embed/([a-zA-Z0-9_-]{11})").find(trimmed)?.groupValues?.getOrNull(1)
        if (!embedMatch.isNullOrBlank()) return embedMatch
        return ""
    }

    private fun extractXmlTagText(xmlBlock: String, tagName: String): String {
        val escaped = Regex.escape(tagName)
        val regex = Regex("<$escaped\\b[^>]*>([\\s\\S]*?)</$escaped>", RegexOption.IGNORE_CASE)
        val match = regex.find(xmlBlock)?.groupValues?.getOrNull(1).orEmpty().trim()
        return match
            .removePrefix("<![CDATA[")
            .removeSuffix("]]>")
            .trim()
    }

    private fun extractFeedLink(xmlBlock: String): String {
        // Atom <link rel="alternate" href="https://..." /> or <link href="https://..." />
        val atomAlt = Regex("<link\\b[^>]*rel=[\"']alternate[\"'][^>]*href=[\"']([^\"']+)[\"']", RegexOption.IGNORE_CASE)
            .find(xmlBlock)?.groupValues?.getOrNull(1).orEmpty()
        if (atomAlt.startsWith("http", ignoreCase = true)) return cleanHtmlAndEntities(atomAlt)

        val atomHref = Regex("<link\\b[^>]*href=[\"']([^\"']+)[\"']", RegexOption.IGNORE_CASE)
            .find(xmlBlock)?.groupValues?.getOrNull(1).orEmpty()
        if (atomHref.startsWith("http", ignoreCase = true)) return cleanHtmlAndEntities(atomHref)

        val rssLink = extractXmlTagText(xmlBlock, "link")
        if (rssLink.startsWith("http", ignoreCase = true)) return cleanHtmlAndEntities(rssLink)

        val guidLink = extractXmlTagText(xmlBlock, "guid")
        if (guidLink.startsWith("http", ignoreCase = true)) return cleanHtmlAndEntities(guidLink)

        return ""
    }

    private fun extractFeedThumbnail(xmlBlock: String, rawDesc: String, ytVideoId: String): String {
        if (ytVideoId.isNotBlank()) {
            return "https://i.ytimg.com/vi/$ytVideoId/hqdefault.jpg"
        }
        // 1. <media:thumbnail url="..." />
        val mediaThumb = Regex("<media:thumbnail\\b[^>]*url=[\"']([^\"']+)[\"']", RegexOption.IGNORE_CASE)
            .find(xmlBlock)?.groupValues?.getOrNull(1).orEmpty()
        if (mediaThumb.startsWith("https://", ignoreCase = true)) return cleanHtmlAndEntities(mediaThumb)

        // 2. <media:content url="..." medium="image" /> or image extension
        val mediaContent = Regex("<media:content\\b[^>]*url=[\"']([^\"']+)[\"']", RegexOption.IGNORE_CASE)
            .find(xmlBlock)?.groupValues?.getOrNull(1).orEmpty()
        if (mediaContent.startsWith("https://", ignoreCase = true)) return cleanHtmlAndEntities(mediaContent)

        // 3. <enclosure url="..." type="image/..." />
        val enclosure = Regex("<enclosure\\b[^>]*url=[\"']([^\"']+)[\"']", RegexOption.IGNORE_CASE)
            .find(xmlBlock)?.groupValues?.getOrNull(1).orEmpty()
        if (enclosure.startsWith("https://", ignoreCase = true) &&
            !enclosure.endsWith(".mp3", ignoreCase = true)
        ) {
            return cleanHtmlAndEntities(enclosure)
        }

        // 4. <img src="https://..." /> inside description/content
        val imgSrc = Regex("<img\\b[^>]*src=[\"'](https://[^\"']+)[\"']", RegexOption.IGNORE_CASE)
            .find("$xmlBlock $rawDesc")?.groupValues?.getOrNull(1).orEmpty()
        if (imgSrc.startsWith("https://", ignoreCase = true)) return cleanHtmlAndEntities(imgSrc)

        return ""
    }

    internal fun cleanHtmlAndEntities(raw: String): String {
        if (raw.isBlank()) return ""
        val withoutCdata = raw
            .replace("<![CDATA[", "")
            .replace("]]>", "")
        val withoutTags = withoutCdata
            .replace(Regex("<script\\b[^>]*>[\\s\\S]*?</script>", RegexOption.IGNORE_CASE), " ")
            .replace(Regex("<style\\b[^>]*>[\\s\\S]*?</style>", RegexOption.IGNORE_CASE), " ")
            .replace(Regex("<[^>]+>"), " ")
        return withoutTags
            .replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&apos;", "'")
            .replace("&#8217;", "’")
            .replace("&#8216;", "‘")
            .replace("&#8220;", "“")
            .replace("&#8221;", "”")
            .replace("&#8211;", "–")
            .replace("&#8212;", "—")
            .replace("&#038;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&nbsp;", " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun parseSyndicationDateMillis(rawDate: String): Long {
        val cleaned = rawDate.trim()
        if (cleaned.isBlank()) return System.currentTimeMillis()
        val formats = listOf(
            "EEE, dd MMM yyyy HH:mm:ss Z",
            "EEE, dd MMM yyyy HH:mm:ss zzz",
            "dd MMM yyyy HH:mm:ss Z",
            "yyyy-MM-dd'T'HH:mm:ssXXX",
            "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd HH:mm:ss"
        )
        for (pattern in formats) {
            try {
                val sdf = SimpleDateFormat(pattern, Locale.US)
                val parsed = sdf.parse(cleaned)?.time
                if (parsed != null && parsed > 0L) {
                    return parsed.coerceAtMost(System.currentTimeMillis())
                }
            } catch (_: Exception) {
                // try next
            }
        }
        return System.currentTimeMillis()
    }

    fun formatRelativeDateLabel(publishedAtMillis: Long, nowMillis: Long = System.currentTimeMillis()): String {
        if (publishedAtMillis <= 0L) return "Just now"
        val diffMs = (nowMillis - publishedAtMillis).coerceAtLeast(0L)
        val minutes = diffMs / 60_000L
        val hours = diffMs / 3_600_000L
        val days = diffMs / 86_400_000L
        return when {
            minutes < 2L -> "Just now"
            minutes < 60L -> "${minutes}m ago"
            hours < 24L -> "${hours}h ago"
            days < 7L -> "${days}d ago"
            else -> SimpleDateFormat("MMM d, yyyy", Locale.US).format(Date(publishedAtMillis))
        }
    }

    fun buildDeterministicDiscoveryId(canonicalUrl: String): String {
        val normalized = canonicalUrl.trim().lowercase(Locale.US)
            .removeSuffix("/")
        val digest = MessageDigest.getInstance("SHA-256").digest(normalized.toByteArray(Charsets.UTF_8))
        val hex = digest.take(12).joinToString("") { "%02x".format(it) }
        return "disc_$hex"
    }

    suspend fun toggleLikeDiscoveryItem(db: FirebaseFirestore, item: DiscoveryContentItem): Result<Boolean> {
        return runCatching {
            val uid = Firebase.auth.currentUser?.uid ?: error("Must be signed in")
            val docRef = db.collection(COLLECTION_ITEMS).document(item.id)
            val alreadyLiked = uid in item.likedByUids
            if (alreadyLiked) {
                docRef.update(
                    mapOf(
                        "likedByUids" to FieldValue.arrayRemove(uid),
                        "likesCount" to (item.likesCount - 1L).coerceAtLeast(0L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
                false
            } else {
                docRef.update(
                    mapOf(
                        "likedByUids" to FieldValue.arrayUnion(uid),
                        "likesCount" to (item.likesCount + 1L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
                true
            }
        }
    }

    suspend fun toggleSaveDiscoveryItem(db: FirebaseFirestore, item: DiscoveryContentItem): Result<Boolean> {
        return runCatching {
            val uid = Firebase.auth.currentUser?.uid ?: error("Must be signed in")
            val docRef = db.collection(COLLECTION_ITEMS).document(item.id)
            val alreadySaved = uid in item.savedByUids
            if (alreadySaved) {
                docRef.update(
                    mapOf(
                        "savedByUids" to FieldValue.arrayRemove(uid),
                        "savesCount" to (item.savesCount - 1L).coerceAtLeast(0L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
                false
            } else {
                docRef.update(
                    mapOf(
                        "savedByUids" to FieldValue.arrayUnion(uid),
                        "savesCount" to (item.savesCount + 1L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
                true
            }
        }
    }

    suspend fun recordDiscoveryItemShare(db: FirebaseFirestore, itemId: String) {
        if (itemId.isBlank() || Firebase.auth.currentUser == null) return
        runCatching {
            db.collection(COLLECTION_ITEMS).document(itemId).update(
                mapOf(
                    "sharesCount" to FieldValue.increment(1L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }
    }

    suspend fun recordDiscoveryItemView(db: FirebaseFirestore, itemId: String) {
        if (itemId.isBlank() || Firebase.auth.currentUser == null) return
        runCatching {
            db.collection(COLLECTION_ITEMS).document(itemId).update(
                mapOf(
                    "viewsCount" to FieldValue.increment(1L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }
    }

    // ADMIN CONTROL OPERATIONS
    fun isUserDiscoveryAdmin(userProfile: UserProfile?, globalConfig: DiscoveryGlobalConfig? = null): Boolean {
        val uid = Firebase.auth.currentUser?.uid.orEmpty()
        val email = Firebase.auth.currentUser?.email.orEmpty().lowercase(Locale.US)
        if (email == "sebinmr556@gmail.com") return true
        if (userProfile?.username?.equals("sebin_mr", ignoreCase = true) == true) return true
        if (globalConfig != null) {
            if (uid.isNotBlank() && uid in globalConfig.adminUids) return true
            if (email.isNotBlank() && email in globalConfig.adminEmails.map { it.lowercase(Locale.US) }) return true
        }
        return false
    }

    suspend fun setCategoryEnabled(
        db: FirebaseFirestore,
        categoryKey: String,
        enabled: Boolean
    ): Result<Unit> = runCatching {
        val docRef = db.collection(COLLECTION_CONFIG).document(GLOBAL_CONFIG_DOC_ID)
        val snap = docRef.get().await()
        val current = snap.toObject(DiscoveryGlobalConfig::class.java) ?: DiscoveryGlobalConfig()
        val nextDisabled = if (enabled) {
            current.disabledCategories - categoryKey
        } else {
            (current.disabledCategories + categoryKey).distinct()
        }
        val nextEnabled = DiscoveryCategory.entries.map { it.key }.filter { it !in nextDisabled }
        docRef.set(
            mapOf(
                "appScope" to "noise_live_public",
                "enabledCategories" to nextEnabled,
                "disabledCategories" to nextDisabled,
                "updatedAt" to FieldValue.serverTimestamp()
            ),
            SetOptions.merge()
        ).await()
    }

    suspend fun updateRefreshIntervalMinutes(
        db: FirebaseFirestore,
        intervalMinutes: Long
    ): Result<Unit> = runCatching {
        val clamped = intervalMinutes.coerceIn(15L, 30L)
        db.collection(COLLECTION_CONFIG).document(GLOBAL_CONFIG_DOC_ID).set(
            mapOf(
                "appScope" to "noise_live_public",
                "refreshIntervalMinutes" to clamped,
                "updatedAt" to FieldValue.serverTimestamp()
            ),
            SetOptions.merge()
        ).await()
    }

    suspend fun addCustomDiscoverySource(
        db: FirebaseFirestore,
        categoryKey: String,
        sourceName: String,
        feedUrl: String,
        contentType: String = "article"
    ): Result<DiscoverySourceConfig> = runCatching {
        val cleanUrl = feedUrl.trim()
        require(cleanUrl.startsWith("https://", ignoreCase = true)) {
            "Content source URL must use secure HTTPS"
        }
        val domain = DiscoverySafetyModerator.extractDomain(cleanUrl)
        require(domain.isNotBlank()) { "Invalid source domain" }
        val sourceId = "src_${categoryKey}_${buildDeterministicDiscoveryId(cleanUrl).removePrefix("disc_")}"
        val format = if (cleanUrl.contains("youtube.com/feeds/videos.xml")) "youtube_rss" else "rss"
        val source = DiscoverySourceConfig(
            id = sourceId,
            categoryKey = categoryKey,
            sourceName = sourceName.trim().ifBlank { domain },
            sourceDomain = domain,
            feedUrl = cleanUrl,
            feedFormat = format,
            contentType = contentType,
            enabled = true,
            trustedVerified = true,
            lastStatus = "Added by Admin"
        )
        db.collection(COLLECTION_SOURCES).document(sourceId).set(
            mapOf(
                "appScope" to "noise_live_public",
                "categoryKey" to source.categoryKey,
                "sourceName" to source.sourceName,
                "sourceDomain" to source.sourceDomain,
                "feedUrl" to source.feedUrl,
                "feedFormat" to source.feedFormat,
                "contentType" to source.contentType,
                "enabled" to true,
                "trustedVerified" to true,
                "lastFetchedAtMillis" to 0L,
                "lastStatus" to "Added by Admin",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        source
    }

    suspend fun toggleDiscoverySourceEnabled(
        db: FirebaseFirestore,
        source: DiscoverySourceConfig,
        enabled: Boolean
    ): Result<Unit> = runCatching {
        db.collection(COLLECTION_SOURCES).document(source.id).set(
            mapOf(
                "appScope" to "noise_live_public",
                "categoryKey" to source.categoryKey,
                "sourceName" to source.sourceName,
                "sourceDomain" to source.sourceDomain,
                "feedUrl" to source.feedUrl,
                "feedFormat" to source.feedFormat,
                "contentType" to source.contentType,
                "enabled" to enabled,
                "trustedVerified" to source.trustedVerified,
                "lastStatus" to if (enabled) "Enabled by Admin" else "Disabled by Admin",
                "updatedAt" to FieldValue.serverTimestamp()
            ),
            SetOptions.merge()
        ).await()
    }

    suspend fun deleteDiscoverySource(
        db: FirebaseFirestore,
        sourceId: String
    ): Result<Unit> = runCatching {
        db.collection(COLLECTION_SOURCES).document(sourceId).delete().await()
    }

    suspend fun moderateDiscoveryItem(
        db: FirebaseFirestore,
        itemId: String,
        newStatus: String, // "approved", "hidden", "rejected"
        notes: String
    ): Result<Unit> = runCatching {
        db.collection(COLLECTION_ITEMS).document(itemId).update(
            mapOf(
                "moderationStatus" to newStatus,
                "moderationNotes" to notes,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
    }

    suspend fun deleteDiscoveryItem(
        db: FirebaseFirestore,
        itemId: String,
        blockDomain: String = ""
    ): Result<Unit> = runCatching {
        db.collection(COLLECTION_ITEMS).document(itemId).delete().await()
        if (blockDomain.isNotBlank()) {
            db.collection(COLLECTION_CONFIG).document(GLOBAL_CONFIG_DOC_ID).set(
                mapOf(
                    "appScope" to "noise_live_public",
                    "blockedDomains" to FieldValue.arrayUnion(blockDomain.lowercase(Locale.US)),
                    "updatedAt" to FieldValue.serverTimestamp()
                ),
                SetOptions.merge()
            ).await()
        }
    }

    suspend fun setKeralaCategoryEnabled(
        db: FirebaseFirestore,
        keralaCategoryKey: String,
        enabled: Boolean
    ): Result<Unit> = runCatching {
        val docRef = db.collection(COLLECTION_CONFIG).document(GLOBAL_CONFIG_DOC_ID)
        val snap = docRef.get().await()
        val current = snap.toObject(DiscoveryGlobalConfig::class.java) ?: DiscoveryGlobalConfig()
        val nextDisabled = if (enabled) {
            current.disabledKeralaCategories - keralaCategoryKey
        } else {
            (current.disabledKeralaCategories + keralaCategoryKey).distinct()
        }
        val nextEnabled = KeralaNewsCategory.entries.map { it.key }.filter { it !in nextDisabled }
        docRef.set(
            mapOf(
                "appScope" to "noise_live_public",
                "enabledKeralaCategories" to nextEnabled,
                "disabledKeralaCategories" to nextDisabled,
                "updatedAt" to FieldValue.serverTimestamp()
            ),
            SetOptions.merge()
        ).await()
    }

    suspend fun updateKeralaAndReelsConfig(
        db: FirebaseFirestore,
        keralaNewsNotificationsEnabled: Boolean? = null,
        trendingReelsNotificationsEnabled: Boolean? = null,
        trendingAlgorithmMode: String? = null
    ): Result<Unit> = runCatching {
        val updates = mutableMapOf<String, Any>(
            "appScope" to "noise_live_public",
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (keralaNewsNotificationsEnabled != null) {
            updates["keralaNewsNotificationsEnabled"] = keralaNewsNotificationsEnabled
        }
        if (trendingReelsNotificationsEnabled != null) {
            updates["trendingReelsNotificationsEnabled"] = trendingReelsNotificationsEnabled
        }
        if (!trendingAlgorithmMode.isNullOrBlank()) {
            updates["trendingAlgorithmMode"] = trendingAlgorithmMode
        }
        db.collection(COLLECTION_CONFIG).document(GLOBAL_CONFIG_DOC_ID)
            .set(updates, SetOptions.merge())
            .await()
    }
}
