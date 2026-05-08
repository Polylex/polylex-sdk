package dev.polylex.internal

import dev.polylex.PolylexException
import dev.polylex.models.CacheMetadata
import dev.polylex.models.Manifest
import dev.polylex.models.TranslationBundle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import java.io.File

/**
 * On-disk JSON cache for translation bundles, per-locale metadata, and the last
 * seen manifest. One file per artifact:
 *   - messages_<locale>.json         → [TranslationBundle]
 *   - cache_metadata_<locale>.json   → [CacheMetadata]
 *   - manifest.json                  → [Manifest]
 *
 * Writes are atomic (temp file + rename). All I/O runs on [Dispatchers.IO].
 * A single [Mutex] serializes access to the cache dir — contention is rare,
 * so per-file locks aren't worth the complexity.
 */
internal class DiskCache(
    private val cacheDir: File,
) {
    private val mutex = Mutex()

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    private companion object {
        const val TAG = "DiskCache"
        const val MANIFEST_FILE = "manifest.json"
    }

    // ---- Translation bundle ------------------------------------------------

    suspend fun saveBundle(locale: String, bundle: TranslationBundle) {
        writeJson(bundleFileName(locale), bundle, TranslationBundle.serializer(), "bundle($locale)")
    }

    suspend fun loadBundle(locale: String): TranslationBundle? =
        readJson(bundleFileName(locale), TranslationBundle.serializer())

    // ---- Metadata ----------------------------------------------------------

    suspend fun saveMetadata(metadata: CacheMetadata) {
        writeJson(
            metadataFileName(metadata.locale),
            metadata,
            CacheMetadata.serializer(),
            "metadata(${metadata.locale})",
        )
    }

    suspend fun loadMetadata(locale: String): CacheMetadata? =
        readJson(metadataFileName(locale), CacheMetadata.serializer())

    // ---- Manifest ----------------------------------------------------------

    suspend fun saveManifest(manifest: Manifest) {
        writeJson(MANIFEST_FILE, manifest, Manifest.serializer(), "manifest")
    }

    suspend fun loadManifest(): Manifest? =
        readJson(MANIFEST_FILE, Manifest.serializer())

    // ---- Clear -------------------------------------------------------------

    suspend fun clear(locale: String) {
        mutex.withLock {
            withContext(Dispatchers.IO) {
                runCatching { File(cacheDir, bundleFileName(locale)).delete() }
                runCatching { File(cacheDir, metadataFileName(locale)).delete() }
            }
        }
    }

    suspend fun clearAll() {
        mutex.withLock {
            withContext(Dispatchers.IO) {
                cacheDir.listFiles()?.forEach { runCatching { it.delete() } }
            }
        }
    }

    // ---- Internals ---------------------------------------------------------

    private suspend fun <T> writeJson(
        fileName: String,
        value: T,
        serializer: KSerializer<T>,
        label: String,
    ) {
        mutex.withLock {
            withContext(Dispatchers.IO) {
                ensureCacheDir()
                val target = File(cacheDir, fileName)
                val temp = File(cacheDir, "$fileName.tmp")
                try {
                    temp.writeText(json.encodeToString(serializer, value))
                    if (!temp.renameTo(target)) {
                        temp.copyTo(target, overwrite = true)
                        temp.delete()
                    }
                    Logger.d(TAG, "wrote $label -> $fileName")
                } catch (e: Exception) {
                    temp.delete()
                    throw PolylexException.Cache("failed to write $label", e)
                }
            }
        }
    }

    private suspend fun <T> readJson(fileName: String, serializer: KSerializer<T>): T? {
        return mutex.withLock {
            withContext(Dispatchers.IO) {
                val file = File(cacheDir, fileName)
                if (!file.exists()) return@withContext null
                try {
                    json.decodeFromString(serializer, file.readText())
                } catch (e: Exception) {
                    Logger.w(TAG, "corrupt cache file $fileName — deleting", e)
                    runCatching { file.delete() }
                    null
                }
            }
        }
    }

    private fun ensureCacheDir() {
        if (!cacheDir.exists() && !cacheDir.mkdirs()) {
            throw PolylexException.Cache("could not create cache dir: ${cacheDir.absolutePath}")
        }
    }

    private fun bundleFileName(locale: String): String {
        val normalized = LocaleNormalizer.normalize(locale)
        return "messages_$normalized.json"
    }

    private fun metadataFileName(locale: String): String {
        val normalized = LocaleNormalizer.normalize(locale)
        return "cache_metadata_$normalized.json"
    }
}
