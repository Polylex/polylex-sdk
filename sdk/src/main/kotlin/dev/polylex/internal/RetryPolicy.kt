package dev.polylex.internal

import kotlinx.coroutines.delay
import java.io.IOException
import kotlin.math.min

/**
 * Config for exponential-backoff retries on transient network failures.
 *
 * @property maxAttempts Total attempts including the first try. Default: 3.
 * @property initialDelayMs Backoff delay before the second attempt. Default: 1s.
 * @property maxDelayMs Upper bound on backoff. Default: 5s.
 * @property multiplier Factor applied to delay between attempts. Default: 2.0.
 */
internal data class RetryPolicy(
    val maxAttempts: Int = 3,
    val initialDelayMs: Long = 1_000L,
    val maxDelayMs: Long = 5_000L,
    val multiplier: Double = 2.0,
)

/**
 * Run [block] up to [policy].maxAttempts times, backing off exponentially between
 * attempts. Only retries on [IOException] (and its subtypes like SocketTimeoutException,
 * UnknownHostException). Non-IO exceptions propagate immediately.
 */
internal suspend fun <T> withRetry(
    policy: RetryPolicy = RetryPolicy(),
    block: suspend (attempt: Int) -> T,
): T {
    var delayMs = policy.initialDelayMs
    var lastError: Throwable? = null

    repeat(policy.maxAttempts) { attempt ->
        try {
            return block(attempt + 1)
        } catch (e: IOException) {
            lastError = e
            Logger.w("RetryPolicy", "attempt ${attempt + 1}/${policy.maxAttempts} failed: ${e.message}")
            if (attempt == policy.maxAttempts - 1) throw e
            delay(delayMs)
            delayMs = min((delayMs * policy.multiplier).toLong(), policy.maxDelayMs)
        }
    }
    // Unreachable — loop always either returns or throws.
    throw lastError ?: IllegalStateException("withRetry exited without result")
}
