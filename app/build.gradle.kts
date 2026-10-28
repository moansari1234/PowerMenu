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

  // One private key signs every published build, so the in-app updater can install new versions over
  // old ones (Android rejects an update signed with a different key). keystore/release.p12 is
  // AES-256 encrypted; its random password lives only in the SIGNING_PASSWORD Actions secret (and
  // the owner's password vault). Without it (local builds) the standard Android debug key is used.
  val signingPassword = System.getenv("SIGNING_PASSWORD")?.takeIf { it.isNotBlank() }
  signingConfigs {
    if (signingPassword != null) create("app") {
      storeFile = rootProject.file("keystore/release.p12")
      storeType = "pkcs12"
      storePassword = signingPassword
      keyAlias = "upload"
      keyPassword = signingPassword
    }
  }
  val appSigning = signingConfigs.findByName("app") ?: signingConfigs.getByName("debug")

  buildTypes {
    debug {
      // Installs next to the release build instead of replacing it
      applicationIdSuffix = ".debug"
      signingConfig = appSigning
    }
    release {
      isMinifyEnabled = true
      isShrinkResources = true
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      signingConfig = appSigning
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
