package dev.polylex.internal

import org.junit.Assert.assertEquals
import org.junit.Test

class LocaleNormalizerTest {

    @Test
    fun `blank input defaults to en`() {
        assertEquals("en", LocaleNormalizer.normalize(""))
        assertEquals("en", LocaleNormalizer.normalize("   "))
    }

    @Test
    fun `android chinese codes are lowercased`() {
        assertEquals("zh-cn", LocaleNormalizer.normalize("zh-CN"))
        assertEquals("zh-tw", LocaleNormalizer.normalize("zh-TW"))
    }

    @Test
    fun `ios script codes collapse to region codes`() {
        assertEquals("zh-cn", LocaleNormalizer.normalize("zh-Hans"))
        assertEquals("zh-tw", LocaleNormalizer.normalize("zh-Hant"))
        assertEquals("zh-cn", LocaleNormalizer.normalize("zh-Hans-CN"))
    }

    @Test
    fun `portuguese variants collapse to pt`() {
        assertEquals("pt", LocaleNormalizer.normalize("pt-PT"))
        assertEquals("pt", LocaleNormalizer.normalize("pt-BR"))
        assertEquals("pt", LocaleNormalizer.normalize("pt-AO"))
    }

    @Test
    fun `latam spanish maps to es-419`() {
        assertEquals("es-419", LocaleNormalizer.normalize("es-MX"))
        assertEquals("es-419", LocaleNormalizer.normalize("es-AR"))
        assertEquals("es-419", LocaleNormalizer.normalize("es-CO"))
    }

    @Test
    fun `european spanish defaults to es`() {
        assertEquals("es", LocaleNormalizer.normalize("es-ES"))
        assertEquals("es", LocaleNormalizer.normalize("es-US"))
    }

    @Test
    fun `indonesian legacy code is rewritten`() {
        assertEquals("id", LocaleNormalizer.normalize("in"))
    }

    @Test
    fun `norwegian variants collapse to no`() {
        assertEquals("no", LocaleNormalizer.normalize("nb-NO"))
        assertEquals("no", LocaleNormalizer.normalize("nb"))
        assertEquals("no", LocaleNormalizer.normalize("nn"))
    }

    @Test
    fun `arabic regional variants collapse to ar`() {
        assertEquals("ar", LocaleNormalizer.normalize("ar-SA"))
        assertEquals("ar", LocaleNormalizer.normalize("ar-EG"))
    }

    @Test
    fun `already canonical codes pass through unchanged`() {
        assertEquals("en", LocaleNormalizer.normalize("en"))
        assertEquals("hi", LocaleNormalizer.normalize("hi"))
        assertEquals("ja", LocaleNormalizer.normalize("ja"))
        assertEquals("fr", LocaleNormalizer.normalize("fr"))
    }
}
