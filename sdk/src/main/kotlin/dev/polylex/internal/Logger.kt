package dev.polylex.internal

import android.util.Log

/**
 * Internal logger. No-op unless [PolylexConfig.enableLogging] is true.
 * Writes to `android.util.Log` with a consistent `Polylex/<tag>` format.
 */
internal object Logger {

    @Volatile
    internal var enabled: Boolean = false

    private const val ROOT_TAG = "Polylex"

    fun d(tag: String, message: String) {
        if (enabled) Log.d("$ROOT_TAG/$tag", message)
    }

    fun i(tag: String, message: String) {
        if (enabled) Log.i("$ROOT_TAG/$tag", message)
    }

    fun w(tag: String, message: String, throwable: Throwable? = null) {
        if (enabled) Log.w("$ROOT_TAG/$tag", message, throwable)
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        if (enabled) Log.e("$ROOT_TAG/$tag", message, throwable)
    }
}
