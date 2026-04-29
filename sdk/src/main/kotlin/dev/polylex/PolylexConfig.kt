package dev.polylex

/**
 * Configuration for the Polylex SDK.
 *
 * @property cdnBaseUrl Base URL of the CDN hosting your translation JSON files.
 *   Polylex will fetch `${cdnBaseUrl}/messages_<locale>.json` on demand.
 *   Example: `"https://d1abc123.cloudfront.net/polylex"`.
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
    val cdnBaseUrl: String,
    val enableLogging: Boolean = false,
    val networkTimeoutMillis: Long = 10_000L,
    val maxRetryAttempts: Int = 3,
) {
    init {
        require(cdnBaseUrl.isNotBlank()) { "cdnBaseUrl must not be blank" }
        require(networkTimeoutMillis > 0) { "networkTimeoutMillis must be positive" }
        require(maxRetryAttempts in 1..10) { "maxRetryAttempts must be between 1 and 10" }
    }
}
