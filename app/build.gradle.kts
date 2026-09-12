plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.compose.compiler)
    // NOTE: apollo plugin is added in Stage 7 once the GraphQL schema/queries exist under
    // src/main/graphql/miya — applying it earlier fails codegen with no schema present.
}

android {
    namespace = "com.hurtado.miya"
    // AGP 8.7.2 is only tested up to compileSdk 35 (the only platform installed locally is
    // API 37, which this AGP version can't resolve a component path for) — use 35 until AGP
    // is bumped alongside a newer platform.
    compileSdk = 35

    defaultConfig {
        applicationId = "com.hurtado.miya"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    // Mirrors the iOS Debug scheme's MIYA_SERVER_URL env var. Empty string in release (fixture
    // mode is debug-only, same rule as iOS RunMode.isFixtureMode). Resolved directly to a
    // literal here rather than via a second BuildConfig field, since AGP/javac's generated
    // BuildConfig.java initializes fields in declaration order and rejects a forward reference
    // between two `public static final` fields.
    val debugServerUrl = (project.findProperty("MIYA_SERVER_URL") as String?)
        ?: System.getenv("MIYA_SERVER_URL")
        ?: "https://192.168.1.183:8000"

    // Mirrors iOS's MiyaGoogleClientID (root Info.plist) — empty until a real Android OAuth
    // client id is registered in Google Cloud Console for this applicationId/signing cert. With
    // it empty, sign-in falls back to fixture mode (see di/AuthModule.kt) exactly like a missing
    // MIYA_SERVER_URL does for HomeClient.
    val googleClientId = (project.findProperty("MIYA_GOOGLE_CLIENT_ID") as String?)
        ?: System.getenv("MIYA_GOOGLE_CLIENT_ID")
        ?: ""
    // A plain custom scheme rather than iOS's reversed-client-id convention (an App Store URL
    // scheme requirement Android has no equivalent for) — must match the intent-filter data in
    // AndroidManifest.xml.
    val oauthRedirectUri = "com.hurtado.miya.oauth://oauth2redirect"

    buildTypes {
        debug {
            buildConfigField("String", "MIYA_SERVER_URL", "\"$debugServerUrl\"")
            buildConfigField("String", "MIYA_GOOGLE_CLIENT_ID", "\"$googleClientId\"")
            buildConfigField("String", "MIYA_OAUTH_REDIRECT_URI", "\"$oauthRedirectUri\"")
        }
        release {
            isMinifyEnabled = false
            buildConfigField("String", "MIYA_SERVER_URL", "\"\"")
            buildConfigField("String", "MIYA_GOOGLE_CLIENT_ID", "\"\"")
            buildConfigField("String", "MIYA_OAUTH_REDIRECT_URI", "\"$oauthRedirectUri\"")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.browser)
    implementation(libs.androidx.security.crypto)
    implementation(libs.androidx.datastore.preferences)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)

    implementation(libs.apollo.runtime)
    implementation(libs.okhttp)

    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)

    implementation(libs.media3.exoplayer)
    implementation(libs.media3.session)
    implementation(libs.media3.common)

    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
