package dev.polylex.models

import kotlinx.serialization.Serializable

/**
 * A locale-scoped set of translations returned from the CDN.
 *
 * @property locale The normalized locale code (e.g., `"hi"`, `"zh-cn"`, `"pt"`).
 * @property translations Flat key-value map: `"welcome_title" → "Welcome aboard"`.
 */
@Serializable
public data class TranslationBundle(
    val locale: String = "",
    val translations: Map<String, String> = emptyMap(),
) {
    public val isValid: Boolean get() = translations.isNotEmpty()
    public val size: Int get() = translations.size
}

/**
 * Metadata written alongside each cached translation bundle.
 * Used for diagnostics and cache-invalidation decisions.
 */
@Serializable
public data class CacheMetadata(
    val locale: String,
    val lastUpdatedEpochMs: Long,
    val translationCount: Int,
    val sourceUrl: String,
)
