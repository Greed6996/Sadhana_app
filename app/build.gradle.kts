import java.security.KeyStore

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
}

android {
    namespace = "com.sadhana.card"
    compileSdk = 35

    val envVersionCode = System.getenv("VERSION_CODE")?.toIntOrNull()
        ?: (project.findProperty("versionCode") as? String)?.toIntOrNull()
        ?: 27
    val envVersionName = System.getenv("VERSION_NAME")
        ?: (project.findProperty("versionName") as? String)
        ?: "1.0.27"

    defaultConfig {
        applicationId = "com.sadhana.card"
        minSdk = 26
        targetSdk = 35
        versionCode = envVersionCode
        versionName = envVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    val releaseKeystorePath = System.getenv("RELEASE_KEYSTORE_PATH")
    val releaseKeystorePassword = System.getenv("RELEASE_KEYSTORE_PASSWORD")
    val releaseKeyAlias = System.getenv("RELEASE_KEY_ALIAS") ?: "sadhana-release"
    val releaseKeyPassword = System.getenv("RELEASE_KEY_PASSWORD") ?: releaseKeystorePassword

    signingConfigs {
        create("release") {
            if (!releaseKeystorePath.isNullOrBlank()) {
                val ksFile = file(releaseKeystorePath)
                if (ksFile.exists()) {
                    storeFile = ksFile
                    storePassword = releaseKeystorePassword
                    keyAlias = releaseKeyAlias
                    keyPassword = releaseKeyPassword
                }
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
        debug {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

val validateReleaseSigning = tasks.register("validateReleaseSigning") {
    description = "Validates production release signing credentials and keystore integrity."
    doLast {
        val keystorePath = System.getenv("RELEASE_KEYSTORE_PATH")
        val keystorePassword = System.getenv("RELEASE_KEYSTORE_PASSWORD")
        val keyAlias = System.getenv("RELEASE_KEY_ALIAS") ?: "sadhana-release"
        val keyPassword = System.getenv("RELEASE_KEY_PASSWORD") ?: keystorePassword

        if (keystorePath.isNullOrBlank()) {
            throw GradleException(
                "❌ [FATAL] Release build aborted: RELEASE_KEYSTORE_PATH environment variable is not set.\n" +
                "Production release signing credentials are strictly required."
            )
        }
        val ksFile = file(keystorePath)
        if (!ksFile.exists() || !ksFile.isFile) {
            throw GradleException(
                "❌ [FATAL] Release build aborted: Keystore file does not exist at: $keystorePath"
            )
        }
        if (keystorePassword.isNullOrBlank()) {
            throw GradleException(
                "❌ [FATAL] Release build aborted: RELEASE_KEYSTORE_PASSWORD environment variable is not set."
            )
        }
        if (keyAlias.isNullOrBlank()) {
            throw GradleException(
                "❌ [FATAL] Release build aborted: RELEASE_KEY_ALIAS environment variable is not set."
            )
        }

        try {
            val keyStore = KeyStore.getInstance("PKCS12")
            ksFile.inputStream().use { stream ->
                keyStore.load(stream, keystorePassword.toCharArray())
            }
            if (!keyStore.containsAlias(keyAlias)) {
                throw GradleException(
                    "❌ [FATAL] Release build aborted: Alias '$keyAlias' was not found in keystore '${ksFile.name}'."
                )
            }
            val key = keyStore.getKey(keyAlias, keyPassword.toCharArray())
            if (key == null) {
                throw GradleException(
                    "❌ [FATAL] Release build aborted: Could not recover key for alias '$keyAlias' (password mismatch or missing key entry)."
                )
            }
        } catch (e: GradleException) {
            throw e
        } catch (e: Exception) {
            throw GradleException(
                "❌ [FATAL] Release build aborted: Keystore validation error: ${e.message}",
                e
            )
        }
    }
}

tasks.matching { it.name == "packageRelease" }.configureEach {
    dependsOn(validateReleaseSigning)
}

dependencies {
    // Clean Architecture: Domain module
    implementation(project(":domain"))

    // Kotlin & Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    // AndroidX Core & Lifecycle
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.6")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")
    implementation("androidx.activity:activity-compose:1.9.3")

    // Jetpack Compose & Material 3
    val composeBom = platform("androidx.compose:compose-bom:2024.10.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui-text-google-fonts:1.7.4")

    // Navigation Compose
    implementation("androidx.navigation:navigation-compose:2.8.3")

    // Room Database + KSP
    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")

    // DataStore Preferences
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // Hilt Dependency Injection
    implementation("com.google.dagger:hilt-android:2.52")
    ksp("com.google.dagger:hilt-android-compiler:2.52")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    // Baseline Profile Installer
    implementation("androidx.profileinstaller:profileinstaller:1.4.1")

    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
