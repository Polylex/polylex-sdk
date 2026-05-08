package dev.polylex.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Manifest document hosted at the customer's stable [PolylexConfig.manifestUrl].
 *
 * The manifest is the contract between the customer's CI/CD (which writes it on
 * every translation deploy) and the SDK (which reads it on every refresh).
 *
 * The SDK uses it to answer two questions:
 *   1. What version of translations is currently "live"?
 *   2. Where do I fetch the per-locale JSON files?
 *
 * Recommended CDN cache policy:
 *   - `manifest.json`: short TTL (e.g., 60s) — must be fresh to unlock new versions
 *   - `{translations_base_url}/messages_*.json`: long TTL (e.g., 30 days) — immutable
 *     because the version identifier is part of the URL path
 *
 * Customers who don't want timestamped deploys can still write a manifest with a
 * fixed [version] and [translationsBaseUrl] pointing to a static path.
 */
@Serializable
public data class Manifest(
    /** Manifest schema version. Current: `1`. */
    @SerialName("schema_version")
    val schemaVersion: Int = 1,

    /**
     * Identifier for this release. SDKs compare against the cached `version` to
     * decide whether to re-fetch. Format is customer-defined — typical choices:
     * ISO 8601 timestamp (`"2026-04-28T10-30-00Z"`), commit SHA, or semver.
     */
    val version: String,

    /**
     * Base URL that the SDK will append `/messages_<locale>.json` to when fetching
     * a translation bundle. Typically ends with something like
     * `/polylex/<version>/translations`.
     */
    @SerialName("translations_base_url")
    val translationsBaseUrl: String,

    /** Locales available in this release. Informational; the SDK requests by locale name. */
    val locales: List<String> = emptyList(),

    /** ISO 8601 timestamp of when this manifest was written. Informational. */
    @SerialName("generated_at")
    val generatedAt: String? = null,

    /** Commit SHA of the source code that produced this release. Informational. */
    @SerialName("source_commit")
    val sourceCommit: String? = null,
) {
    public val isValid: Boolean
        get() = version.isNotBlank() && translationsBaseUrl.isNotBlank()
}
