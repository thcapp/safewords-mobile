plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.thc.safewords"
    compileSdk = 35

    // Strip Firebase telemetry transport pulled in transitively by MLKit.
    // We never use it; declaring its INTERNET permission would contradict our
    // "zero network" Data Safety claim.
    configurations.all {
        exclude(group = "com.google.android.datatransport")
        exclude(group = "com.google.firebase", module = "firebase-encoders")
        exclude(group = "com.google.firebase", module = "firebase-encoders-json")
    }

    defaultConfig {
        applicationId = "app.thc.safewords"
        minSdk = 26
        targetSdk = 35
        versionCode = 15
        versionName = "1.3.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            // Reads from ~/.gradle/safewords.properties OR project gradle.properties OR env vars
            val keystorePath = (findProperty("safewords.keystorePath") as? String)
                ?: System.getenv("SAFEWORDS_KEYSTORE_PATH")
                ?: "${System.getProperty("user.home")}/keystores/safewords-release.jks"
            val keystorePassword = (findProperty("safewords.keystorePassword") as? String)
                ?: System.getenv("SAFEWORDS_KEYSTORE_PASSWORD")
                ?: ""
            val keyAlias = (findProperty("safewords.keyAlias") as? String)
                ?: System.getenv("SAFEWORDS_KEY_ALIAS")
                ?: "safewords"
            val keyPassword = (findProperty("safewords.keyPassword") as? String)
                ?: System.getenv("SAFEWORDS_KEY_PASSWORD")
                ?: keystorePassword

            storeFile = file(keystorePath)
            storePassword = keystorePassword
            this.keyAlias = keyAlias
            this.keyPassword = keyPassword
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        compose = true
    }
}

// Copy /shared/{wordlists/bip39-english.txt, recovery-vectors.json} into the
// app test resources before tests run so Bip39Test can load them via classpath.
val sharedDir = file("../../../shared")
val testResourcesDir = file("src/test/resources")

val copySharedToTestResources by tasks.registering(Copy::class) {
    from(File(sharedDir, "wordlists/bip39-english.txt"))
    from(File(sharedDir, "recovery-vectors.json"))
    from(File(sharedDir, "primitive-vectors.json"))
    from(File(sharedDir, "migration-vectors.json"))
    from(File(projectDir, "src/main/assets/wordlists/adjectives.json"))
    from(File(projectDir, "src/main/assets/wordlists/nouns.json"))
    into(testResourcesDir)
    // src/main/assets/wordlists/* lives under the same root that
    // copySharedToAssets writes into — declare the dependency so gradle
    // orders them correctly.
    mustRunAfter("copySharedToAssets")
}

// /shared/safety-card-copy.json is read by CardRenderer at runtime, so it
// needs to land in the app's assets bundle (not just test resources).
val mainAssetsDir = file("src/main/assets")

val copySharedToAssets by tasks.registering(Copy::class) {
    from(File(sharedDir, "safety-card-copy.json"))
    into(mainAssetsDir)
}

tasks.matching { it.name.startsWith("merge") && it.name.contains("Assets") }.configureEach {
    dependsOn(copySharedToAssets)
}
tasks.matching { it.name.startsWith("process") && it.name.contains("Assets") }.configureEach {
    dependsOn(copySharedToAssets)
}

tasks.withType<Test>().configureEach {
    dependsOn(copySharedToTestResources)
}

// processDebugUnitTestJavaRes also reads from src/test/resources; make sure it
// runs after the copy task. Same for any other tests in other variants.
tasks.matching { it.name.startsWith("process") && it.name.contains("UnitTestJavaRes") }.configureEach {
    dependsOn(copySharedToTestResources)
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)

    implementation(libs.androidx.navigation.compose)

    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.mlkit.barcode.scanning)

    implementation(libs.zxing.core)

    implementation(libs.androidx.security.crypto)
    implementation(libs.androidx.biometric)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.print)

    implementation(libs.androidx.glance)
    implementation(libs.androidx.glance.material3)
    implementation(libs.androidx.work.runtime)

    implementation(libs.gson)

    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
