plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "dev.polylex.sample"
    compileSdk = 34

    defaultConfig {
        applicationId = "dev.polylex.sample"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "0.1.0"

        // Polylex manifest URL — overridable per build flavor / type per ADR-012.
        // Default points at a placeholder you'll replace with your real CDN URL
        // once the R2 bucket is set up (see polylex-action/docs/setup/cloudflare-r2.md).
        val defaultManifestUrl =
            project.findProperty("polylex.manifestUrl") as String?
                ?: "https://pub-REPLACE-ME.r2.dev/polylex/manifest.json"
        buildConfigField("String", "POLYLEX_MANIFEST_URL", "\"$defaultManifestUrl\"")
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation(project(":sdk"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons)

    debugImplementation(libs.compose.ui.tooling)

    implementation(libs.kotlinx.coroutines.android)
}
