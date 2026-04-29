package dev.polylex

import android.content.Context
import dev.polylex.internal.Logger
import dev.polylex.models.TranslationBundle
import kotlinx.atomicfu.atomic

/**
 * Polylex — the public entry point for the SDK.
 *
 * Integration is two steps:
 *
 * 1. Initialize in your [android.app.Application]:
 * ```
 * Polylex.initialize(
 *     config = PolylexConfig(cdnBaseUrl = "https://cdn.example.com/polylex"),
 *     context = applicationContext,
 * )
 * ```
 *
 * 2. Wrap activity context in your `BaseActivity`:
 * ```
 * override fun attachBaseContext(newBase: Context) {
 *     super.attachBaseContext(PolylexContextWrapper.wrap(newBase))
 * }
 * ```
 *
 * After that, every existing `getString(R.string.key)` call resolves against the
 * Polylex cache first, falling back to the bundled `strings.xml` on any miss.
 */
public object Polylex {

    private const val TAG = "Polylex"

    private val initialized = atomic(false)
    private val currentTranslations = atomic<Map<String, String>>(emptyMap())
    private val activeLocale = atomic<String?>(null)

    private var config: PolylexConfig? = null
    private var appContext: Context? = null

    /**
     * Initialize the SDK. Safe to call multiple times — subsequent calls are no-ops.
     *
     * @throws IllegalStateException if [context] is not an application context.
     */
    @JvmStatic
    public fun initialize(config: PolylexConfig, context: Context) {
        if (!initialized.compareAndSet(expect = false, update = true)) {
            Logger.w(TAG, "initialize() called more than once — ignoring subsequent calls")
            return
        }
        val applicationContext = context.applicationContext
            ?: error("context.applicationContext must not be null")

        Logger.enabled = config.enableLogging
        this.config = config
        this.appContext = applicationContext

        Logger.i(TAG, "initialized with cdn=${config.cdnBaseUrl}")
    }

    /**
     * Look up a translation by key. Returns `null` if not present in the active locale's bundle.
     * Never throws — callers should fall back to the platform's bundled string on null.
     */
    @JvmStatic
    public fun getString(key: String): String? {
        if (!initialized.value) return null
        return currentTranslations.value[key]
    }

    /**
     * Look up a translation and apply `String.format` with [formatArgs]. Returns the
     * unformatted string if no format args are supplied, `null` if the key is absent.
     */
    @JvmStatic
    public fun getString(key: String, vararg formatArgs: Any?): String? {
        val raw = getString(key) ?: return null
        if (formatArgs.isEmpty()) return raw
        return runCatching { String.format(raw, *formatArgs) }.getOrElse { raw }
    }

    /**
     * Snapshot of the currently active locale's translations.
     * Primarily exposed for testing and diagnostics.
     */
    @JvmStatic
    public fun activeLocale(): String? = activeLocale.value

    internal fun commit(bundle: TranslationBundle) {
        if (!bundle.isValid) {
            Logger.w(TAG, "commit() ignoring invalid bundle for locale=${bundle.locale}")
            return
        }
        currentTranslations.value = bundle.translations
        activeLocale.value = bundle.locale
        Logger.i(TAG, "committed ${bundle.size} translations for locale=${bundle.locale}")
    }

    internal fun isInitialized(): Boolean = initialized.value

    internal fun requireContext(): Context = appContext
        ?: error("Polylex is not initialized. Call Polylex.initialize() in Application.onCreate().")

    internal fun requireConfig(): PolylexConfig = config
        ?: error("Polylex is not initialized. Call Polylex.initialize() in Application.onCreate().")
}
