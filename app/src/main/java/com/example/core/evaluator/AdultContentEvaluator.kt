package com.example.core.evaluator

import java.net.URLDecoder
import java.util.Locale

/**
 * High-performance, on-device adult content domain and keyword evaluator.
 * Operates strictly locally with zero remote network calls, tracking, or telemetry.
 */
object AdultContentEvaluator {

    // Common Android Web Browser Packages
    val BROWSER_PACKAGES = setOf(
        "com.android.chrome",
        "org.mozilla.firefox",
        "com.sec.android.app.sbrowser", // Samsung Internet
        "com.microsoft.emmx", // Microsoft Edge
        "com.brave.browser",
        "com.opera.browser",
        "com.opera.mini.native",
        "com.opera.touch",
        "com.duckduckgo.mobile.android",
        "com.UCMobile.intl",
        "com.vivaldi.browser",
        "com.kiwibrowser.browser",
        "com.mi.globalbrowser",
        "com.coloros.browser",
        "org.torproject.torbrowser"
    )

    // Curated high-impact adult domain database (top adult domains, cam networks, and hubs)
    private val ADULT_DOMAINS = hashSetOf(
        "pornhub.com", "xvideos.com", "xnxx.com", "xhamster.com", "xhamster.desi",
        "xhamster.one", "redtube.com", "youporn.com", "chaturbate.com", "onlyfans.com",
        "stripchat.com", "cam4.com", "livejasmin.com", "bongacams.com", "camsoda.com",
        "myfreecams.com", "streamate.com", "brazzers.com", "bangbros.com", "spankbang.com",
        "tube8.com", "hentaihaven.xxx", "nhentai.net", "rule34.xxx", "gelbooru.com",
        "danbooru.donmai.us", "e-hentai.org", "tsumino.com", "literotica.com", "lushstories.com",
        "beeg.com", "eporner.com", "hqporner.com", "daftsex.com", "txxx.com", "porn555.com",
        "motherless.com", "heavy-r.com", "fapello.com", "coomer.party", "coomer.su",
        "kemono.party", "kemono.su", "erome.com", "bravoteens.com", "shemalez.com",
        "playboy.com", "penthouse.com", "hustler.com", "fapster.xxx", "pornone.com",
        "pornhat.com", "porndo.com", "vporn.com", "youjizz.com", "slutload.com", "tblop.com",
        "nuvid.com", "sunporno.com", "drtuber.com", "empflix.com", "cliphunter.com",
        "keezmovies.com", "tube9.com", "pornsharing.com", "pornrabbit.com", "pornhd.com",
        "porntube.com", "fux.com", "badjojo.com", "thumbzilla.com", "zbporn.com", "fuq.com",
        "javlibrary.com", "javhd.com", "javbus.com", "dmm.co.jp", "r18.com", "fanza.com",
        "missav.com", "jable.tv", "supjav.com", "netflav.com", "tktube.com", "redgifs.com",
        "porntrex.com", "spankwire.com", "gotporn.com", "tnaflix.com", "porn.com",
        "adultfriendfinder.com", "fetlife.com", "manyvids.com", "fansly.com", "thothub.to",
        "simpcity.su", "leakgirls.com", "leaklinks.com"
    )

    // Explicit root terms that indicate pornographic intent in URLs or search queries
    private val EXPLICIT_KEYWORDS = listOf(
        "porn", "porno", "pornography", "pornographic", "xxx", "xvideos", "xnxx",
        "xhamster", "pornhub", "redtube", "youporn", "chaturbate", "stripchat",
        "camsoda", "brazzers", "bangbros", "spankbang", "hentai", "rule34",
        "erotic video", "hardcore sex", "blowjob", "gangbang", "creampie", "milf sex",
        "anal sex", "incest porn", "threesome sex", "shemale porn", "trans porn",
        "camgirl live", "fetish porn", "nsfw video", "nude leak", "onlyfans leak",
        "fapello", "sex tape", "adult video", "adult webcam", "live sex cam",
        "nude cam", "erome leak", "naked girls", "naked women", "free porn",
        "adult tube", "sex video", "hardcore video"
    )

    // Search Engine hosts
    private val SEARCH_ENGINES = listOf(
        "google.", "bing.com", "duckduckgo.com", "yahoo.com", "yandex.", "ecosia.org"
    )

    // Incognito / Private browsing indicator strings across Android browser UI
    private val INCOGNITO_INDICATORS = listOf(
        "incognito", "inprivate", "private browsing", "private tab", "secret mode"
    )

    sealed class EvaluationResult {
        data object Clean : EvaluationResult()
        data class Blocked(val category: BlockCategory, val layer: String) : EvaluationResult()
    }

    enum class BlockCategory(val label: String) {
        ADULT_DOMAIN("18+ Adult Website"),
        EXPLICIT_SEARCH("Explicit Sexual Search"),
        VISIBLE_CONTEXT("Restricted 18+ Content"),
        INCOGNITO_RESTRICTED("Private/Incognito Browsing")
    }

    /**
     * Evaluates a URL, address bar text, or browser page title.
     */
    fun evaluateText(rawInput: String): EvaluationResult {
        if (rawInput.isBlank()) return EvaluationResult.Clean

        val clean = rawInput.trim().lowercase(Locale.ROOT)

        // 1. Check Incognito / InPrivate browsing titles or tab indicators
        for (indicator in INCOGNITO_INDICATORS) {
            if (clean.contains(indicator)) {
                return EvaluationResult.Blocked(
                    category = BlockCategory.INCOGNITO_RESTRICTED,
                    layer = "Incognito Protection Guard"
                )
            }
        }

        // 2. Decode URL if encoded
        val decoded = try {
            URLDecoder.decode(clean, "UTF-8")
        } catch (_: Exception) {
            clean
        }

        // 3. Extract hostname / domain if it looks like a URL
        val domain = extractDomain(decoded)
        if (domain.isNotEmpty()) {
            // Exact or subdomain match against known adult domains
            if (isAdultDomain(domain)) {
                return EvaluationResult.Blocked(
                    category = BlockCategory.ADULT_DOMAIN,
                    layer = "18+ Domain Shield"
                )
            }
        }

        // 4. Check for explicit keywords in search queries or URL paths
        for (keyword in EXPLICIT_KEYWORDS) {
            // Word boundary match
            val regex = Regex("(^|[^a-z0-9])${Regex.escape(keyword)}([^a-z0-9]|$)", RegexOption.IGNORE_CASE)
            if (regex.containsMatchIn(decoded)) {
                return EvaluationResult.Blocked(
                    category = BlockCategory.EXPLICIT_SEARCH,
                    layer = "Real-Time URL & Search Shield"
                )
            }
        }

        return EvaluationResult.Clean
    }

    /**
     * Conservative context scoring for non-browser apps and WebViews.
     * One ambiguous term is never enough. Inspected text stays in memory only.
     */
    fun evaluateVisibleContext(rawInput: String): EvaluationResult {
        if (rawInput.isBlank()) return EvaluationResult.Clean
        val clean = rawInput.lowercase(Locale.ROOT).take(12_000)

        val strongTerms = listOf(
            "porn", "pornography", "xxx", "adult video", "adult webcam",
            "nsfw video", "explicit video", "nude leak", "sex video"
        )
        if (strongTerms.any { clean.contains(it) }) {
            return EvaluationResult.Blocked(
                category = BlockCategory.VISIBLE_CONTEXT,
                layer = "On-Screen Context Shield"
            )
        }

        val contextTerms = listOf(
            "18+", "adult only", "explicit", "nsfw", "uncensored",
            "nude", "naked", "webcam", "adult content"
        )
        val score = contextTerms.count { clean.contains(it) }
        return if (score >= 3) {
            EvaluationResult.Blocked(
                category = BlockCategory.VISIBLE_CONTEXT,
                layer = "On-Screen Context Shield"
            )
        } else {
            EvaluationResult.Clean
        }
    }

    /**
     * Checks if a domain or any of its parent domains match the adult blocklist.
     */
    fun isAdultDomain(domain: String): Boolean {
        val lowerDomain = domain.lowercase(Locale.ROOT)
            .removePrefix("www.")
            .removePrefix("m.")
            .removePrefix("mobile.")

        if (ADULT_DOMAINS.contains(lowerDomain)) return true

        // Check parent domains across all part depths (e.g. video.s1.xvideos.com -> xvideos.com, dmm.co.jp)
        val parts = lowerDomain.split(".")
        if (parts.size >= 2) {
            for (i in 2..parts.size) {
                val candidate = parts.takeLast(i).joinToString(".")
                if (ADULT_DOMAINS.contains(candidate)) return true
            }
        }

        return false
    }

    /**
     * Extracts the host portion from a raw URL or address bar input.
     */
    fun extractDomain(raw: String): String {
        var str = raw.trim().lowercase(Locale.ROOT)
        if (str.startsWith("http://")) str = str.substring(7)
        if (str.startsWith("https://")) str = str.substring(8)

        // Take up to first slash, colon, or question mark
        val slashIndex = str.indexOf('/')
        if (slashIndex != -1) str = str.substring(0, slashIndex)

        val colonIndex = str.indexOf(':')
        if (colonIndex != -1) str = str.substring(0, colonIndex)

        val queryIndex = str.indexOf('?')
        if (queryIndex != -1) str = str.substring(0, queryIndex)

        return str.trim()
    }

    /**
     * Checks if an application package name belongs to a web browser or web content viewer.
     */
    fun isBrowserPackage(packageName: String): Boolean {
        val lower = packageName.lowercase(Locale.ROOT)
        return BROWSER_PACKAGES.contains(lower) ||
                lower.contains("browser") ||
                lower.contains("chrome") ||
                lower.contains("firefox") ||
                lower.contains("webview") ||
                lower.contains("searchbox")
    }

    /**
     * Checks if a domain is a known search engine.
     */
    fun isSearchEngine(domain: String): Boolean {
        val lower = domain.lowercase(Locale.ROOT)
        return SEARCH_ENGINES.any { lower.contains(it) }
    }
}
