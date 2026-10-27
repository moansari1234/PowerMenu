plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
}

android {
  namespace = "com.example"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

  defaultConfig {
    applicationId = "com.aistudio.powermenu.qzpxrt"
    minSdk = 24
    targetSdk = 36
    // Release CI passes -PappVersionName=1.2.3 from the git tag; versionCode is derived so it always grows.
    val appVersion = (project.findProperty("appVersionName") as String?) ?: "1.0.0"
    versionName = appVersion
    versionCode = appVersion.split(".").map { it.toIntOrNull() ?: 0 }.let { v ->
      v.getOrElse(0) { 0 } * 10000 + v.getOrElse(1) { 0 } * 100 + v.getOrElse(2) { 0 }
    }
    buildConfigField("String", "UPDATE_REPO", "\"moansari1234/PowerMenu\"")
  }

  // One stable key signs every build, so the in-app updater can install new versions over old ones
  // (Android rejects an update signed with a different key). CI can swap in a private key through
  // KEYSTORE_PATH / STORE_PASSWORD / KEY_ALIAS / KEY_PASSWORD; otherwise the committed sideload key is used.
  val envKeystore = System.getenv("KEYSTORE_PATH")?.takeIf { it.isNotBlank() }
  signingConfigs {
    create("sideload") {
      storeFile = if (envKeystore != null) file(envKeystore) else rootProject.file("keystore/sideload.p12")
      storePassword = if (envKeystore != null) System.getenv("STORE_PASSWORD") else "sideload"
      keyAlias = if (envKeystore != null) System.getenv("KEY_ALIAS") ?: "upload" else "sideload"
      keyPassword = if (envKeystore != null) System.getenv("KEY_PASSWORD") else "sideload"
      storeType = if (envKeystore != null && !envKeystore.endsWith(".p12")) "jks" else "pkcs12"
    }
  }

  buildTypes {
    debug {
      // Installs next to the release build instead of replacing it
      applicationIdSuffix = ".debug"
      signingConfig = signingConfigs.getByName("sideload")
    }
    release {
      isMinifyEnabled = true
      isShrinkResources = true
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      signingConfig = signingConfigs.getByName("sideload")
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  buildFeatures {
    compose = true
    buildConfig = true
  }
}

// Only what the app uses. The AI Studio scaffold also pulled in Firebase AI, Retrofit, OkHttp,
// Moshi and Room; none were used, and they added network permissions and ~10 MB to the APK.
dependencies {
  implementation(platform(libs.androidx.compose.bom))
  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
  debugImplementation(libs.androidx.compose.ui.tooling)
}
