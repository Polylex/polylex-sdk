package dev.polylex

import android.content.res.Resources
import dev.polylex.internal.Logger

/**
 * A [Resources] decorator that intercepts `getString(Int)` calls and checks the
 * Polylex cache before falling back to the platform resources.
 *
 * On any error — resource ID not found, translation missing, unexpected exception —
 * it falls back to the base `Resources` implementation. This class will never
 * throw from `getString()` in a way the caller didn't already have to handle.
 */
public class PolylexResources internal constructor(
    private val base: Resources,
) : Resources(base.assets, base.displayMetrics, base.configuration) {

    private companion object {
        const val TAG = "PolylexResources"
    }

    override fun getString(id: Int): String =
        try {
            val key = base.getResourceEntryName(id)
            Polylex.getString(key) ?: super.getString(id)
        } catch (e: NotFoundException) {
            Logger.e(TAG, "resource not found for id=$id", e)
            super.getString(id)
        } catch (e: Exception) {
            Logger.e(TAG, "unexpected error for id=$id — falling back to native", e)
            super.getString(id)
        }

    override fun getString(id: Int, vararg formatArgs: Any?): String =
        try {
            val key = base.getResourceEntryName(id)
            val raw = Polylex.getString(key)
            when {
                raw == null -> super.getString(id, *formatArgs)
                formatArgs.isEmpty() -> raw
                else -> runCatching { String.format(raw, *formatArgs) }
                    .getOrElse { super.getString(id, *formatArgs) }
            }
        } catch (e: NotFoundException) {
            Logger.e(TAG, "resource not found for id=$id", e)
            super.getString(id, *formatArgs)
        } catch (e: Exception) {
            Logger.e(TAG, "unexpected error for id=$id — falling back to native", e)
            super.getString(id, *formatArgs)
        }
}
