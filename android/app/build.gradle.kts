plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.paparazzi)
}

// The app only talks to the API Gateway. The Android emulator reaches the computer's localhost at 10.0.2.2;
// for a phone, build with -PnambedeApiUrl=http://<computer IP>:8202/api/
val apiBaseUrl = (findProperty("nambedeApiUrl") as String?) ?: "http://10.0.2.2:8202/api/"

android {
    namespace = "vn.edu.qnu.st.nambede"
    compileSdk = 35
    buildToolsVersion = "35.0.0"

    defaultConfig {
        applicationId = "vn.edu.qnu.st.nambede"
        // 26: java.time for due dates without desugaring
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        buildConfigField("String", "API_BASE_URL", "\"$apiBaseUrl\"")
    }

    // Release key from Gradle properties, e.g. in ~/.gradle/gradle.properties (never commit it):
    // nambedeKeystore=/path/key.jks, nambedeKeystorePassword, nambedeKeyAlias, nambedeKeyPassword
    val releaseKeystore = findProperty("nambedeKeystore") as String?
    signingConfigs {
        // Debug key kept in the repo (password "android", for development only): every machine, Docker and CI sign
        // debug builds with the same key, so a newer APK always installs over an older one
        getByName("debug") {
            storeFile = file("debug.keystore")
        }
        if (releaseKeystore != null) {
            create("release") {
                storeFile = file(releaseKeystore)
                storePassword = findProperty("nambedeKeystorePassword") as String?
                keyAlias = findProperty("nambedeKeyAlias") as String?
                keyPassword = findProperty("nambedeKeyPassword") as String?
            }
        }
    }

    buildTypes {
        release {
            // R8: drop unused code and resources, rules in proguard-rules.pro
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Without a release key the APK is signed with the debug key, so it can still be installed for a demo
            signingConfig = signingConfigs.getByName(if (releaseKeystore != null) "release" else "debug")
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
    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

tasks.withType<Test>().configureEach {
    // Same clock on every machine: screenshots and date tests show Vietnam time
    systemProperty("user.timezone", "Asia/Ho_Chi_Minh")
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
    implementation(libs.androidx.navigation.compose)
    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.okhttp)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.junit)
    testImplementation(libs.okhttp.mockwebserver)
    testImplementation(libs.kotlinx.coroutines.test)
    debugImplementation(libs.androidx.ui.tooling)
}
