package dev.polylex.internal

import dev.polylex.PolylexException
import dev.polylex.models.CacheMetadata
import dev.polylex.models.Manifest
import dev.polylex.models.TranslationBundle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Coordinates the three tiers (memory → disk → network) around a manifest-first flow:
 *
 * ```
 * fetch(locale):
 *   1. Fetch the manifest (cheap, short TTL)
 *   2. Compare manifest.version against cached_metadata.manifestVersion for this locale
 *   3. If same: return disk cache (no bundle download)
 *   4. If different or nothing cached: download messages_<locale>.json,
 *      persist with the new manifestVersion, return it
 *   5. If the manifest fetch fails, fall back to the disk cache for this locale
 * ```
 *
 * `refresh(locale)` forces a fresh manifest + bundle fetch, bypassing version comparison.
 */
internal class Repository(
    private val manifestUrl: String,
    private val diskCache: DiskCache,
    private val remote: RemoteDatasource,
) {
    private companion object {
        const val TAG = "Repository"
    }

    private val backgroundScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    /**
     * Normal cache-aware fetch. Short-circuits the bundle download when the
     * manifest reports a version we already have on disk for this locale.
     */
    suspend fun fetch(locale: String): TranslationBundle {
        return try {
            val manifest = remote.fetchManifest(manifestUrl)
            diskCache.saveManifest(manifest)

            val cachedMetadata = diskCache.loadMetadata(locale)
            val cachedBundle = diskCache.loadBundle(locale)

            if (cachedMetadata != null &&
                cachedBundle != null &&
                cachedBundle.isValid &&
                cachedMetadata.manifestVersion == manifest.version
            ) {
                Logger.i(TAG, "cache hit (version=${manifest.version}) for locale=$locale")
                return cachedBundle
            }

            Logger.i(
                TAG,
                "cache miss for locale=$locale " +
                    "(cached=${cachedMetadata?.manifestVersion}, live=${manifest.version})",
            )
            fetchBundleAndPersist(manifest, locale)
        } catch (e: PolylexException) {
            // Manifest or bundle fetch failed — try stale disk cache
            Logger.w(TAG, "fetch failed for locale=$locale: ${e.message}")
            diskCache.loadBundle(locale)?.takeIf { it.isValid }?.let { stale ->
                Logger.i(TAG, "falling back to stale cache for locale=$locale")
                return stale
            }
            throw e
        }
    }

    /** Force a fresh manifest + bundle download, ignoring version match. */
    suspend fun refresh(locale: String): TranslationBundle {
        return try {
            val manifest = remote.fetchManifest(manifestUrl)
            diskCache.saveManifest(manifest)
            fetchBundleAndPersist(manifest, locale)
        } catch (e: PolylexException) {
            Logger.w(TAG, "refresh failed for locale=$locale: ${e.message}")
            diskCache.loadBundle(locale)?.takeIf { it.isValid }?.let { stale ->
                Logger.i(TAG, "falling back to stale cache for locale=$locale")
                return stale
            }
            throw e
        }
    }

    /**
     * Load whatever is already on disk for [locale] without any network call.
     * Used during `initialize()` to warm the in-memory cache before the first
     * successful refresh completes.
     */
    suspend fun loadFromDisk(locale: String): TranslationBundle? =
        diskCache.loadBundle(locale)?.takeIf { it.isValid }

    private suspend fun fetchBundleAndPersist(
        manifest: Manifest,
        locale: String,
    ): TranslationBundle {
        val bundle = remote.fetchBundle(manifest.translationsBaseUrl, locale)
        diskCache.saveBundle(locale, bundle)
        diskCache.saveMetadata(
            CacheMetadata(
                locale = bundle.locale,
                lastUpdatedEpochMs = System.currentTimeMillis(),
                translationCount = bundle.size,
                manifestVersion = manifest.version,
                sourceUrl = "${manifest.translationsBaseUrl.trimEnd('/')}/messages_${bundle.locale}.json",
            ),
        )
        return bundle
    }

    fun close() {
        remote.close()
    }
}
