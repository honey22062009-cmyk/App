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
        "com.chrome.beta",
        "com.chrome.dev",
        "com.chrome.canary",
        "org.mozilla.firefox",
        "org.mozilla.fenix",
        "com.sec.android.app.sbrowser", // Samsung Internet
        "com.microsoft.emmx", // Microsoft Edge
        "com.brave.browser",
        "com.opera.browser",
        "com.opera.mini.native",
        "com.opera.touch",
        "com.opera.gx",
        "com.opera.mini.native.beta",
        "com.opera.browser.beta",
        "org.mozilla.focus",
        "org.mozilla.klar",
        "com.duckduckgo.mobile.android",
        "com.ucmobile.intl",
        "com.vivaldi.browser",
        "com.kiwibrowser.browser",
        "com.mi.globalbrowser",
        "com.coloros.browser",
        "com.heytap.browser",
        "com.vivo.browser",
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

    // High-confidence terms score immediately. Ambiguous single words are
    // deliberately lower-weight to reduce false positives on health/education pages.
    private val HIGH_SIGNAL_TERMS = listOf(
        "porn", "porno", "pornography", "pornographic", "porn video", "free porn",
        "pornhub", "xvideos", "xnxx", "xhamster", "redtube", "youporn",
        "chaturbate", "stripchat", "camsoda", "brazzers", "bangbros", "spankbang",
        "hentai", "rule34", "sex tape", "nude leak", "onlyfans leak", "camgirl live",
        "fetish porn", "adult webcam", "live sex cam", "free adult video",
        "p0rn", "p0rno", "p0rnography", "s3x video", "n5fw video",
        "पोर्न", "अश्लील"
    )
    private val MEDIUM_SIGNAL_TERMS = listOf(
        "erotic video", "hardcore video", "adult video", "adult tube",
        "nude cam", "naked girls", "naked women", "erome leak",
        "blowjob", "gangbang", "creampie", "milf sex", "anal sex",
        "incest porn", "threesome sex", "chudai", "चुदाई"
    )
    private val AMBIGUOUS_TERMS = listOf(
        "sex", "sexual", "nudes", "nude", "naked", "erotic", "erotica", "nsfw", "sexy", "सेक्स"
    )
    private val SAFE_EDUCATIONAL_DOMAINS = setOf(
        "nhs.uk", "mayoclinic.org", "clevelandclinic.org", "kidshealth.org",
        "healthline.com", "medlineplus.gov", "plannedparenthood.org",
        "who.int", "cdc.gov", "ncbi.nlm.nih.gov", "medscape.com",
        "britannica.com", "khanacademy.org", "openstax.org"
    )

    private fun tokenPattern(terms: List<String>): Regex {
        val alternation = terms.joinToString("|") { Regex.escape(it) }
        return Regex("(^|[^\\p{L}\\p{N}])(?:$alternation)(?=$|[^\\p{L}\\p{N}])", RegexOption.IGNORE_CASE)
    }
    private val highSignalPattern = tokenPattern(HIGH_SIGNAL_TERMS)
    private val mediumSignalPattern = tokenPattern(MEDIUM_SIGNAL_TERMS)
    private val ambiguousPattern = tokenPattern(AMBIGUOUS_TERMS)

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

    private val explicitPatterns = EXPLICIT_KEYWORDS.map { keyword ->
        Regex("(^|[^\\p{L}\\p{N}])${Regex.escape(keyword)}([^\\p{L}\\p{N}]|$)", RegexOption.IGNORE_CASE)
    }

    /**
     * Evaluates an actual browser address or search value, never arbitrary page copy.
     * Ambiguous single words need multiple signals; trusted health/education domains
     * are exempt from ambiguous-word scoring but not from the explicit domain blocklist.
     */
    fun evaluateText(rawInput: String): EvaluationResult {
        if (rawInput.isBlank()) return EvaluationResult.Clean

        val clean = normalizeForMatching(rawInput)
        val decoded = try {
            URLDecoder.decode(URLDecoder.decode(clean, "UTF-8"), "UTF-8")
        } catch (_: Exception) {
            clean
        }
        val domain = extractDomain(decoded)
        if (domain.isNotEmpty() && isAdultDomain(domain)) {
            return EvaluationResult.Blocked(BlockCategory.ADULT_DOMAIN, "18+ Domain Shield")
        }

        val searchText = decoded.replace(Regex("[+_\\-]"), " ")
        if (highSignalPattern.containsMatchIn(searchText) ||
            mediumSignalPattern.containsMatchIn(searchText)) {
            return EvaluationResult.Blocked(BlockCategory.EXPLICIT_SEARCH, "Browser search guard")
        }

        val trustedDomain = SAFE_EDUCATIONAL_DOMAINS.any { safe ->
            domain == safe || domain.endsWith(".$safe")
        }
        if (!trustedDomain && ambiguousPattern.findAll(searchText).take(3).count() >= 3) {
            return EvaluationResult.Blocked(BlockCategory.EXPLICIT_SEARCH, "Multiple restricted search signals")
        }
        return EvaluationResult.Clean
    }

    fun normalizeForMatching(value: String): String =
        java.text.Normalizer.normalize(value.take(8192), java.text.Normalizer.Form.NFKC)
            .filterNot { it == '\u200B' || it == '\u200C' || it == '\u200D' ||
                it == '\u2060' || it == '\uFEFF' }
            .trim()
            .lowercase(Locale.ROOT)

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
        return BROWSER_PACKAGES.contains(lower) || lower.startsWith("com.opera.") ||
                lower.contains("browser") ||
                lower.contains("chrome") ||
                lower.contains("firefox") ||
                lower.contains("webview") ||
                lower.contains("searchbox")
    }

}
