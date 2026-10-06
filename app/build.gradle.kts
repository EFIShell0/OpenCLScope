plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}
android {
    namespace = "com.efishell.openclscope"
    compileSdk = 37
    val dbApi = providers.gradleProperty("openclscopeDatabaseApiUrl").orElse("").get().trimEnd('/')
    val dbWeb = providers.gradleProperty("openclscopeDatabaseWebUrl").orElse("").get().trimEnd('/')
    require(dbApi.isBlank() || Regex("https://[A-Za-z0-9.-]+(?::443)?").matches(dbApi)) { "Database API must be a fixed HTTPS origin" }
    require(dbWeb.isBlank() || Regex("https://[A-Za-z0-9.-]+(?::443)?(?:/[A-Za-z0-9_/-]+)?").matches(dbWeb)) { "Database web URL must be HTTPS" }
    val releaseRepo = providers.gradleProperty("openclscopeGitHubRepository").orElse("").get().trimEnd('/')
    require(releaseRepo.isBlank() || Regex("https://github[.]com/[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+").matches(releaseRepo)) { "OpenCLScope release origin must be one fixed GitHub HTTPS repository" }
    buildFeatures { buildConfig = true }
    defaultConfig {
        buildConfigField("String", "DATABASE_API_BASE_URL", "\"$dbApi\"")
        buildConfigField("String", "DATABASE_WEB_BASE_URL", "\"$dbWeb\"")
        buildConfigField("String", "RELEASE_GITHUB_REPOSITORY", "\"$releaseRepo\"")
        applicationId = "com.efishell.openclscope"
        minSdk = 31
        targetSdk = 37
        versionCode = 900
        versionName = "0.9.0"
        ndkVersion = "29.0.14206865"
        ndk { abiFilters += listOf("armeabi-v7a", "arm64-v8a", "x86_64") }
    }
    splits {
        abi {
            isEnable = true
            reset()
            include("armeabi-v7a", "arm64-v8a", "x86_64")
            isUniversalApk = true
        }
    }
    externalNativeBuild { cmake { path = file("src/main/cpp/CMakeLists.txt") } }
    packaging { jniLibs { useLegacyPackaging = true } }
    buildTypes {
        release {
            isDebuggable = false
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        debug { isDebuggable = true }
    }
}
dependencies {
    implementation("androidx.core:core-ktx:1.19.0")
    implementation("androidx.core:core-splashscreen:1.2.0")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.compose.ui:ui:1.12.0")
    implementation("androidx.compose.foundation:foundation:1.12.0")
    implementation("androidx.compose.animation:animation:1.12.0")
    implementation("androidx.compose.material3:material3:1.5.0-alpha27")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:5.5.0")
    implementation("com.google.zxing:core:3.5.4")
}
