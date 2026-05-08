package dev.polylex.internal

import dev.polylex.models.CacheMetadata
import dev.polylex.models.Manifest
import dev.polylex.models.TranslationBundle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files

class DiskCacheTest {

    private lateinit var tempDir: File
    private lateinit var cache: DiskCache

    @Before
    fun setUp() {
        tempDir = Files.createTempDirectory("polylex-test").toFile()
        cache = DiskCache(tempDir)
    }

    @After
    fun tearDown() {
        tempDir.deleteRecursively()
    }

    @Test
    fun `round-trip save and load bundle`() = runTest {
        val bundle = TranslationBundle(
            locale = "hi",
            translations = mapOf("welcome_title" to "नमस्ते", "cta_done" to "हो गया"),
        )
        cache.saveBundle("hi", bundle)
        val loaded = cache.loadBundle("hi")
        assertNotNull(loaded)
        assertEquals(2, loaded!!.size)
        assertEquals("नमस्ते", loaded.translations["welcome_title"])
    }

    @Test
    fun `loadBundle returns null when file does not exist`() = runTest {
        assertNull(cache.loadBundle("ja"))
    }

    @Test
    fun `loadBundle returns null and deletes corrupt file`() = runTest {
        val file = File(tempDir, "messages_hi.json")
        file.writeText("{not valid json at all")
        assertTrue(file.exists())

        val loaded = cache.loadBundle("hi")
        assertNull(loaded)
        assertFalse("corrupt file should be deleted", file.exists())
    }

    @Test
    fun `round-trip metadata includes manifest version`() = runTest {
        val meta = CacheMetadata(
            locale = "ja",
            lastUpdatedEpochMs = 1_700_000_000_000L,
            translationCount = 42,
            manifestVersion = "2026-04-28T10-30-00Z",
            sourceUrl = "https://cdn.example.com/polylex/2026-04-28T10-30-00Z/translations/messages_ja.json",
        )
        cache.saveMetadata(meta)
        val loaded = cache.loadMetadata("ja")
        assertEquals(meta, loaded)
    }

    @Test
    fun `round-trip manifest`() = runTest {
        val manifest = Manifest(
            schemaVersion = 1,
            version = "2026-04-28T10-30-00Z",
            translationsBaseUrl = "https://cdn.example.com/polylex/2026-04-28T10-30-00Z/translations",
            locales = listOf("en", "hi", "ja"),
            generatedAt = "2026-04-28T10:30:15Z",
            sourceCommit = "a1b2c3d",
        )
        cache.saveManifest(manifest)
        assertEquals(manifest, cache.loadManifest())
    }

    @Test
    fun `loadManifest returns null when missing`() = runTest {
        assertNull(cache.loadManifest())
    }

    @Test
    fun `clear removes bundle and metadata for a single locale`() = runTest {
        cache.saveBundle("hi", TranslationBundle(locale = "hi", translations = mapOf("a" to "क")))
        cache.saveBundle("ja", TranslationBundle(locale = "ja", translations = mapOf("a" to "あ")))
        cache.saveMetadata(
            CacheMetadata(
                locale = "hi",
                lastUpdatedEpochMs = 1L,
                translationCount = 1,
                manifestVersion = "v1",
                sourceUrl = "",
            ),
        )

        cache.clear("hi")

        assertNull(cache.loadBundle("hi"))
        assertNull(cache.loadMetadata("hi"))
        assertNotNull(cache.loadBundle("ja")) // untouched
    }

    @Test
    fun `locale normalization applies to file names`() = runTest {
        val bundle = TranslationBundle(locale = "zh-cn", translations = mapOf("hi" to "你好"))
        cache.saveBundle("zh-CN", bundle)
        assertTrue(File(tempDir, "messages_zh-cn.json").exists())

        assertNotNull(cache.loadBundle("zh-CN"))
        assertNotNull(cache.loadBundle("zh-Hans"))
    }
}
