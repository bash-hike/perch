import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "app.perch"
    compileSdk = 36

    defaultConfig {
        applicationId = "app.perch"
        // Android 15 and up: the design targets that release's edge-to-edge defaults.
        minSdk = 35
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
    }

    // Release signing comes from the environment (CI) or a git-ignored keystore.properties (local), never
    // from the repository. Without either, the release build falls back to the debug key so that
    // `assembleRelease` still works for sideloading.
    val keystoreProps = Properties().apply {
        rootProject.file("keystore.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) }
    }
    fun signing(name: String, env: String) = System.getenv(env) ?: keystoreProps.getProperty(name)

    signingConfigs {
        val file = signing("storeFile", "SIGNING_KEYSTORE_FILE")
        if (file != null) {
            create("release") {
                storeFile = rootProject.file(file)
                storePassword = signing("storePassword", "SIGNING_KEYSTORE_PASSWORD")
                keyAlias = signing("keyAlias", "SIGNING_KEY_ALIAS")
                keyPassword = signing("keyPassword", "SIGNING_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures { compose = true }
}

kotlin {
    compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation(platform("androidx.compose:compose-bom:2024.10.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.palette:palette-ktx:1.0.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    testImplementation("junit:junit:4.13.2")
}
