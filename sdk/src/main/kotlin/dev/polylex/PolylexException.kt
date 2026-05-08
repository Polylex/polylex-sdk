package dev.polylex

/**
 * Errors that Polylex surfaces to callers of the public suspend API
 * (`refresh()`, `setActiveLocale()`). Non-suspend synchronous lookups
 * like [Polylex.getString] never throw — they fall back to bundled strings.
 */
public sealed class PolylexException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause) {

    /** The SDK was used before [Polylex.initialize] was called. */
    public class NotInitialized : PolylexException(
        "Polylex is not initialized. Call Polylex.initialize() in Application.onCreate() first.",
    )

    /** Network I/O failed after exhausting all retry attempts. */
    public class Network(
        message: String,
        cause: Throwable? = null,
    ) : PolylexException(message, cause)

    /** The CDN returned a response that could not be parsed as a translation bundle. */
    public class InvalidResponse(
        message: String,
        cause: Throwable? = null,
    ) : PolylexException(message, cause)

    /** Reading from or writing to the on-disk cache failed. */
    public class Cache(
        message: String,
        cause: Throwable? = null,
    ) : PolylexException(message, cause)
}
