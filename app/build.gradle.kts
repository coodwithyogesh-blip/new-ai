plugins { alias(libs.plugins.android.application); alias(libs.plugins.kotlin.compose) }

android {
    namespace = "com.jarvis.local"
    compileSdk = 36
    defaultConfig { applicationId = "com.jarvis.local"; minSdk = 24; targetSdk = 36; versionCode = 3; versionName = "3.0" }
    signingConfigs { create("jarvisRelease") {
        val keystorePath = System.getenv("JARVIS_KEYSTORE_PATH"); val storePassword = System.getenv("JARVIS_KEYSTORE_PASSWORD"); val keyAlias = System.getenv("JARVIS_KEY_ALIAS"); val keyPassword = System.getenv("JARVIS_KEY_PASSWORD")
        if (!keystorePath.isNullOrBlank() && !storePassword.isNullOrBlank() && !keyAlias.isNullOrBlank() && !keyPassword.isNullOrBlank()) { storeFile = file(keystorePath); this.storePassword = storePassword; this.keyAlias = keyAlias; this.keyPassword = keyPassword }
    } }
    buildTypes { release { isMinifyEnabled = false; signingConfig = signingConfigs.getByName("jarvisRelease"); proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro") } }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    buildFeatures { compose = true }
}

dependencies { implementation(platform(libs.androidx.compose.bom)); implementation(libs.androidx.activity.compose); implementation(libs.androidx.compose.material3); implementation(libs.androidx.compose.material.icons.extended); implementation(libs.androidx.compose.ui); implementation(libs.androidx.compose.ui.graphics); implementation(libs.androidx.compose.ui.tooling.preview); implementation(libs.androidx.core.ktx); implementation(libs.kotlinx.coroutines.android); debugImplementation(libs.androidx.compose.ui.tooling) }