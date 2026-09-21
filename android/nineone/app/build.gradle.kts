// `java.util.Properties` cannot be spelled inline here: inside a Gradle Kotlin
// DSL script `java` resolves to the java extension, not the package.
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "com.whooc.nineone"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.whooc.nineone"
        minSdk = 24
        targetSdk = 35
        versionCode = 8
        versionName = "2.2.0"
        resourceConfigurations += listOf("zh", "en")
    }

    // Signing material is never committed and never baked into this file.
    // Point NINEONE_STORE_FILE at a keystore via local.properties (git-ignored)
    // or the environment. With nothing configured the release build falls back
    // to the debug key so that a fresh clone still produces something
    // installable — which is a different signature, so it cannot upgrade an
    // existing install; the warning below says so.
    val signingProps = Properties().apply {
        rootProject.file("local.properties").takeIf { it.exists() }
            ?.inputStream()?.use { load(it) }
    }
    fun signingSetting(key: String): String? =
        signingProps.getProperty(key) ?: System.getenv(key)

    val releaseKeystore = signingSetting("NINEONE_STORE_FILE")?.let { file(it) }
    val hasReleaseKey = releaseKeystore != null && releaseKeystore.exists()

    signingConfigs {
        if (hasReleaseKey) {
            create("release") {
                storeFile = releaseKeystore
                storePassword = signingSetting("NINEONE_STORE_PASSWORD")
                keyAlias = signingSetting("NINEONE_KEY_ALIAS") ?: "nineone"
                keyPassword = signingSetting("NINEONE_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            // Everything here is native — no reflection-driven JSON, so R8 is
            // safe and keeps the APK small (material-icons-extended alone is
            // tens of MB of unused classes).
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = if (hasReleaseKey) {
                signingConfigs.getByName("release")
            } else {
                logger.warn(
                    "NINEONE: no release keystore configured " +
                        "(set NINEONE_STORE_FILE in local.properties) — " +
                        "signing the release build with the DEBUG key. " +
                        "This APK cannot upgrade an install signed with the " +
                        "real release key."
                )
                signingConfigs.getByName("debug")
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf("-opt-in=kotlin.RequiresOptIn")
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }

    packaging {
        resources.excludes += setOf(
            "META-INF/*.kotlin_module",
            "META-INF/DEPENDENCIES",
            "META-INF/LICENSE*",
            "META-INF/NOTICE*",
            "META-INF/AL2.0",
            "META-INF/LGPL2.1"
        )
    }

    lint {
        abortOnError = false
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.09.02")
    implementation(composeBom)

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")
    implementation("androidx.navigation:navigation-compose:2.8.0")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    implementation("androidx.media3:media3-exoplayer:1.4.1")
    implementation("androidx.media3:media3-ui:1.4.1")
    implementation("androidx.media3:media3-datasource-okhttp:1.4.1")

    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("io.coil-kt:coil-compose:2.7.0")

    debugImplementation("androidx.compose.ui:ui-tooling")
}
