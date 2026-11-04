plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

android {
    namespace = "org.jameslick.foresight"
    compileSdk = 36

    defaultConfig {
        applicationId = "org.jameslick.foresight"
        minSdk = 26          // Android 8.0: covers mid-range Galaxy phones the team owns
        targetSdk = 36
        versionCode = 2
        versionName = "0.1.0-phase1"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true; buildConfig = true }
    androidResources {
        // The .tflite must stay uncompressed so it can be memory-mapped straight from the APK.
        noCompress += "tflite"
    }
    lint {
        // Privacy (§6): fail the build on anything that could leak data or allow backup.
        warningsAsErrors = false
        abortOnError = true
        error += listOf("AllowBackup", "HardcodedText", "SetTextI18n")
        checkDependencies = true
    }
    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")   // versioned DB schemas, reviewed in PRs
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.animation)
    implementation(libs.androidx.navigation.compose)

    // On-device inference (spec §2.1): LiteRT. CPU/XNNPACK in Phase 1; GPU delegate evaluated in Phase 2.
    implementation(libs.litert)
    implementation(libs.litert.gpu)

    // Encrypted local storage (spec §2.1, §6): Room + SQLCipher
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.sqlite.ktx)
    implementation(libs.sqlcipher.android)

    implementation(libs.kotlinx.serialization.json)   // model card + de-identified export
    implementation(libs.zip4j)                        // password-protected clinic export (§3.7)
    implementation("com.google.zxing:core:3.5.3")     // QR code on the referral card (§3.1 step 8)

    testImplementation(libs.junit)
    testImplementation("ar.com.hjg:pngj:2.1.0")   // pure-Java PNG decoder for the preprocessing equivalence test (android.jar has no javax.imageio)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
