package dev.polylex.internal

import dev.polylex.PolylexException
import dev.polylex.models.Manifest
import dev.polylex.models.TranslationBundle
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import java.io.IOException

/**
 * Performs the two network operations the SDK needs:
 *   1. [fetchManifest] — hit the customer's stable manifest URL, parse the JSON.
 *   2. [fetchBundle] — hit `{translationsBaseUrl}/messages_<locale>.json`, parse the flat map.
 *
 * Both operations use exponential-backoff retry on [IOException].
 */
internal class RemoteDatasource(
    timeoutMillis: Long,
    maxRetryAttempts: Int,
) {
    private val retryPolicy = RetryPolicy(maxAttempts = maxRetryAttempts)

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val client = HttpClient(OkHttp) {
        expectSuccess = false
        install(ContentNegotiation) {
            json(json)
        }
        install(HttpTimeout) {
            requestTimeoutMillis = timeoutMillis
            connectTimeoutMillis = timeoutMillis
            socketTimeoutMillis = timeoutMillis
        }
    }

    private companion object {
        const val TAG = "RemoteDatasource"
    }

    /**
     * Fetch the manifest from [manifestUrl]. Sends `Cache-Control: no-cache` so
     * intermediate caches are bypassed — freshness here is load-bearing.
     */
    suspend fun fetchManifest(manifestUrl: String): Manifest {
        return withRetry(retryPolicy) { attempt ->
            Logger.d(TAG, "manifest fetch attempt $attempt: $manifestUrl")
            val response = safeGet(manifestUrl, freshnessSensitive = true)
            when {
                response.status.isSuccess() -> parseManifest(response, manifestUrl)
                response.status == HttpStatusCode.NotFound ->
                    throw PolylexException.Network("manifest not found at $manifestUrl (HTTP 404)")
                else ->
                    throw PolylexException.Network("CDN returned ${response.status} for $manifestUrl")
            }
        }
    }

    /**
     * Fetch the translation bundle for [locale] from `{translationsBaseUrl}/messages_<locale>.json`.
     * Translation files are content-addressed by version in the path, so intermediate caches
     * are fine to trust here.
     */
    suspend fun fetchBundle(translationsBaseUrl: String, locale: String): TranslationBundle {
        val normalized = LocaleNormalizer.normalize(locale)
        val url = "${translationsBaseUrl.trimEnd('/')}/messages_$normalized.json"

        return withRetry(retryPolicy) { attempt ->
            Logger.d(TAG, "bundle fetch attempt $attempt: $url")
            val response = safeGet(url, freshnessSensitive = false)
            when {
                response.status.isSuccess() -> parseBundle(response, normalized, url)
                response.status == HttpStatusCode.NotFound ->
                    throw PolylexException.Network("no translations for locale=$locale at $url (HTTP 404)")
                else ->
                    throw PolylexException.Network("CDN returned ${response.status} for $url")
            }
        }
    }

    private suspend fun safeGet(url: String, freshnessSensitive: Boolean): HttpResponse {
        return try {
            client.get(url) {
                if (freshnessSensitive) {
                    header("Cache-Control", "no-cache")
                    header("Pragma", "no-cache")
                }
            }
        } catch (e: IOException) {
            throw e // let withRetry handle it
        } catch (e: Exception) {
            throw PolylexException.Network("unexpected error fetching $url", e)
        }
    }

    private suspend fun parseManifest(response: HttpResponse, sourceUrl: String): Manifest {
        val body = response.bodyAsText()
        val manifest = try {
            json.decodeFromString(Manifest.serializer(), body)
        } catch (e: Exception) {
            throw PolylexException.InvalidResponse("failed to parse manifest from $sourceUrl", e)
        }
        if (!manifest.isValid) {
            throw PolylexException.InvalidResponse(
                "manifest from $sourceUrl is missing required fields (version, translations_base_url)",
            )
        }
        return manifest
    }

    private suspend fun parseBundle(
        response: HttpResponse,
        locale: String,
        sourceUrl: String,
    ): TranslationBundle {
        val body = response.bodyAsText()
        return try {
            val map = json.decodeFromString(
                MapSerializer(String.serializer(), String.serializer()),
                body,
            )
            TranslationBundle(locale = locale, translations = map)
        } catch (e: Exception) {
            throw PolylexException.InvalidResponse(
                "failed to parse translation bundle for locale=$locale from $sourceUrl",
                e,
            )
        }
    }

    fun close() {
        client.close()
    }
}
