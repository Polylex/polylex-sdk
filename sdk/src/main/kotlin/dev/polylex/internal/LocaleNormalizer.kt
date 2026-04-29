package dev.polylex.internal

/**
 * Canonicalizes platform-supplied locale strings to the file-naming convention
 * Polylex uses on the CDN (lowercase, hyphen-separated).
 *
 * Examples:
 *   "zh-CN"    -> "zh-cn"
 *   "zh-Hans"  -> "zh-cn"
 *   "zh-Hant"  -> "zh-tw"
 *   "in"       -> "id"   (Android's legacy Indonesian code)
 *   "nb-NO"    -> "no"
 *   "pt-BR"    -> "pt"
 *   "es-MX"    -> "es-419"
 *   ""         -> "en"   (blank defaults to English)
 */
internal object LocaleNormalizer {

    private val explicitMap: Map<String, String> = mapOf(
        // Android legacy codes
        "zh-CN" to "zh-cn",
        "zh-TW" to "zh-tw",
        "in" to "id",
        "nb-NO" to "no",
        "nb" to "no",
        "nn" to "no",

        // iOS script-based codes
        "zh-Hans" to "zh-cn",
        "zh-Hant" to "zh-tw",
        "pt-PT" to "pt",
        "pt-BR" to "pt",

        // Latin American Spanish variants
        "es-MX" to "es-419",
        "es-AR" to "es-419",
        "es-CO" to "es-419",
        "es-419" to "es-419",
    )

    fun normalize(raw: String): String {
        if (raw.isBlank()) return "en"

        explicitMap[raw]?.let { return it }

        val lower = raw.lowercase()
        return when {
            lower.startsWith("zh-hans") -> "zh-cn"
            lower.startsWith("zh-hant") -> "zh-tw"
            lower.startsWith("pt-") -> "pt"
            lower.startsWith("es-") && lower != "es-419" -> "es"
            lower.startsWith("ar-") -> "ar"
            else -> lower
        }
    }
}
