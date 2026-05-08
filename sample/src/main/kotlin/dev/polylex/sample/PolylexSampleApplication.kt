package dev.polylex.sample

import android.app.Application
import dev.polylex.Polylex
import dev.polylex.PolylexConfig

/**
 * One-time SDK initialization. Polylex reads `BuildConfig.POLYLEX_MANIFEST_URL`
 * which is set at build time from a Gradle property — see [build.gradle.kts].
 *
 * Per ADR-012 (tracks are a build-time concern), staging vs prod is selected
 * by setting different manifest URLs in different build flavors / types.
 */
class PolylexSampleApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        Polylex.initialize(
            config = PolylexConfig(
                manifestUrl = BuildConfig.POLYLEX_MANIFEST_URL,
                enableLogging = BuildConfig.DEBUG,
            ),
            context = applicationContext,
        )
    }
}
