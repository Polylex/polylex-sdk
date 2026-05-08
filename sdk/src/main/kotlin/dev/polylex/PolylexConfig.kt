package dev.polylex

/**
 * Configuration for the Polylex SDK.
 *
 * @property manifestUrl Full URL to your `manifest.json`. The SDK fetches this on
 *   every refresh to learn (a) the current release [version] and (b) the base URL
 *   to fetch per-locale `messages_<locale>.json` from.
 *
 *   Recommend setting a short TTL (e.g., 60s) on this file in your CDN so new
 *   releases propagate quickly. Translation files themselves can be cached
 *   indefinitely because the version is part of their URL path.
 *
 *   Example: `"https://cdn.example.com/polylex/manifest.json"`.
 *
 * @property enableLogging When `true`, Polylex emits debug logs via `android.util.Log`.
 *   Default: `false`. Recommended: set to `BuildConfig.DEBUG`.
 *
 * @property networkTimeoutMillis Per-request timeout. Default: 10 seconds.
 *
 * @property maxRetryAttempts Max retry attempts per network call. Default: 3.
 *   Retries use exponential backoff starting at 1s, capped at 5s.
 */
public data class PolylexConfig(
    val manifestUrl: String,
    val enableLogging: Boolean = false,
    val networkTimeoutMillis: Long = 10_000L,
    val maxRetryAttempts: Int = 3,
) {
    init {
        require(manifestUrl.isNotBlank()) { "manifestUrl must not be blank" }
        require(networkTimeoutMillis > 0) { "networkTimeoutMillis must be positive" }
        require(maxRetryAttempts in 1..10) { "maxRetryAttempts must be between 1 and 10" }
    }
}
