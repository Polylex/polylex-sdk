package dev.polylex.models

import kotlinx.serialization.Serializable

/**
 * A locale-scoped set of translations fetched from the CDN.
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
 * Used for version tracking (short-circuit when the live manifest version
 * matches what's already on disk) and diagnostics.
 */
@Serializable
public data class CacheMetadata(
    val locale: String,
    val lastUpdatedEpochMs: Long,
    val translationCount: Int,
    /** The manifest version this bundle was fetched under. */
    val manifestVersion: String,
    /** The exact URL the bundle was fetched from. Diagnostics only. */
    val sourceUrl: String,
)
