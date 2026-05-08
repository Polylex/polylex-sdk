package dev.polylex

import android.content.Context
import dev.polylex.internal.DiskCache
import dev.polylex.internal.LocaleNormalizer
import dev.polylex.internal.Logger
import dev.polylex.internal.RemoteDatasource
import dev.polylex.internal.Repository
import dev.polylex.models.TranslationBundle
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale

/**
 * Polylex — public entry point for the SDK.
 *
 * Lifecycle:
 *  1. [initialize] in `Application.onCreate`
 *  2. Wrap activity contexts via [PolylexContextWrapper]
 *  3. Call [refresh] (or [setActiveLocale] for explicit switches) from a
 *     coroutine — typically on app start and on user language change.
 *
 * Synchronous string lookups ([getString]) never throw. Async operations
 * throw [PolylexException] subclasses on unrecoverable failures.
 */
public object Polylex {

    private const val TAG = "Polylex"
    private const val CACHE_SUBDIR = "polylex"

    private val initialized = atomic(false)
    private val currentTranslations = atomic<Map<String, String>>(emptyMap())
    private val activeLocale = atomic<String?>(null)

    private var config: PolylexConfig? = null
    private var appContext: Context? = null
    private var repository: Repository? = null

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    /**
     * Initialize the SDK. Idempotent — subsequent calls are ignored with a warning.
     * Starts a background load of any previously cached translations for the
     * device locale so the in-memory cache is warm before the first `refresh()`.
     */
    @JvmStatic
    public fun initialize(config: PolylexConfig, context: Context) {
        if (!initialized.compareAndSet(expect = false, update = true)) {
            Logger.w(TAG, "initialize() called more than once — ignoring")
            return
        }
        val app = context.applicationContext
            ?: error("context.applicationContext must not be null")

        Logger.enabled = config.enableLogging
        this.config = config
        this.appContext = app

        val cacheDir = File(app.cacheDir, CACHE_SUBDIR)
        val diskCache = DiskCache(cacheDir)
        val remote = RemoteDatasource(
            timeoutMillis = config.networkTimeoutMillis,
            maxRetryAttempts = config.maxRetryAttempts,
        )
        val repo = Repository(
            manifestUrl = config.manifestUrl,
            diskCache = diskCache,
            remote = remote,
        )
        this.repository = repo

        Logger.i(TAG, "initialized: manifest=${config.manifestUrl}, cache=${cacheDir.absolutePath}")

        scope.launch { warmCacheFromDisk(repo) }
    }

    /**
     * Fetch the latest manifest, then fetch + commit translations for [locale]
     * (defaulting to the device locale). If the live manifest version matches
     * what's already on disk for this locale, the bundle download is skipped.
     *
     * Returns `true` if translations were committed successfully (either freshly
     * downloaded or reused from cache). Throws [PolylexException] on unrecoverable failure.
     */
    @JvmStatic
    public suspend fun refresh(locale: String? = null): Boolean {
        val repo = requireRepository()
        val target = locale?.takeIf { it.isNotBlank() } ?: deviceLocale()
        val bundle = repo.fetch(target)
        commit(bundle)
        return bundle.isValid
    }

    /**
     * Force a fresh download bypassing the manifest-version short-circuit. Use
     * this when you explicitly want to re-pull translations even if the version
     * hasn't changed (e.g., after a cache-corruption recovery).
     */
    @JvmStatic
    public suspend fun forceRefresh(locale: String? = null): Boolean {
        val repo = requireRepository()
        val target = locale?.takeIf { it.isNotBlank() } ?: deviceLocale()
        val bundle = repo.refresh(target)
        commit(bundle)
        return bundle.isValid
    }

    /**
     * Change the active locale. Fetches translations for [locale] first; commits
     * only if the fetch succeeds. Use this for user-initiated language changes
     * to guarantee the new language is available before swapping the UI.
     */
    @JvmStatic
    public suspend fun setActiveLocale(locale: String): Boolean {
        require(locale.isNotBlank()) { "locale must not be blank" }
        val repo = requireRepository()
        val bundle = repo.fetch(locale)
        commit(bundle)
        return bundle.isValid
    }

    /** Synchronous lookup. Returns `null` if the key is absent. Never throws. */
    @JvmStatic
    public fun getString(key: String): String? {
        if (!initialized.value) return null
        return currentTranslations.value[key]
    }

    /** Synchronous lookup with `String.format` applied. Never throws. */
    @JvmStatic
    public fun getString(key: String, vararg formatArgs: Any?): String? {
        val raw = getString(key) ?: return null
        if (formatArgs.isEmpty()) return raw
        return runCatching { String.format(raw, *formatArgs) }.getOrElse { raw }
    }

    /** The locale currently live in the in-memory cache. */
    @JvmStatic
    public fun activeLocale(): String? = activeLocale.value

    /** Number of translations currently loaded. Primarily for diagnostics. */
    @JvmStatic
    public fun translationCount(): Int = currentTranslations.value.size

    // -----------------------------------------------------------------------
    // Internal
    // -----------------------------------------------------------------------

    private fun commit(bundle: TranslationBundle) {
        if (!bundle.isValid) {
            Logger.w(TAG, "commit: ignoring invalid bundle for locale=${bundle.locale}")
            return
        }
        currentTranslations.value = bundle.translations
        activeLocale.value = bundle.locale
        Logger.i(TAG, "committed ${bundle.size} translations for locale=${bundle.locale}")
    }

    private suspend fun warmCacheFromDisk(repo: Repository) {
        val locale = deviceLocale()
        try {
            val cached = repo.loadFromDisk(locale)
            if (cached != null) {
                commit(cached)
            } else {
                Logger.d(TAG, "no cache warm-up for locale=$locale (empty or missing)")
            }
        } catch (e: Exception) {
            Logger.w(TAG, "cache warm-up failed: ${e.message}", e)
        }
    }

    private fun deviceLocale(): String {
        val locale = Locale.getDefault()
        val raw = if (locale.country.isNotEmpty()) "${locale.language}-${locale.country}" else locale.language
        return LocaleNormalizer.normalize(raw)
    }

    private fun requireRepository(): Repository = repository
        ?: throw PolylexException.NotInitialized()

    internal fun requireContext(): Context = appContext
        ?: throw PolylexException.NotInitialized()

    internal fun isInitialized(): Boolean = initialized.value
}
