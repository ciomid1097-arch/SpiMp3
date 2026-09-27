import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

// Release signing is read from keystore.properties (git-ignored).
//
// A release build MUST be signed with the real upload key. Previously a missing
// keystore.properties silently fell back to the debug key, which produced
// debug-signed "release" APKs that Myket (and Play) reject. The fallback is now
// opt-in via -Pspimp3.signWithDebugKey=true and only serves local experiments.
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) {
        keystorePropertiesFile.inputStream().use { load(it) }
    }
}
val hasReleaseKeystore = keystoreProperties.getProperty("storeFile") != null
val signWithDebugKey = (project.findProperty("spimp3.signWithDebugKey") as String?) == "true"

if (!hasReleaseKeystore && !signWithDebugKey) {
    // Only fail when a release artifact is actually requested, so that
    // assembleDebug / test / lint keep working on a fresh clone without a key.
    logger.lifecycle(
        "[spimp3] No keystore.properties found — release builds will FAIL. " +
            "Copy store/keystore.properties.example to keystore.properties, or pass " +
            "-Pspimp3.signWithDebugKey=true for a throwaway debug-signed build."
    )
} else if (!hasReleaseKeystore) {
    logger.warn("[spimp3] Signing release with the DEBUG key (spimp3.signWithDebugKey=true). Never ship this APK.")
}

android {
    namespace = "com.spimp3.app"
    compileSdk = 37

    // The project lives under a non-ASCII Windows path (E:\موزیک پلیر\...), which
    // corrupts the unit-test worker classpath. When SPIMP3_OUT_DIR is set, all
    // build output goes to that ASCII directory instead (a junction to ./out).
    if (System.getenv("SPIMP3_OUT_DIR") != null) {
        layout.buildDirectory.set(File(System.getenv("SPIMP3_OUT_DIR")!!))
    }

    defaultConfig {
        applicationId = "com.spimp3.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 5
        versionName = "1.1.2"
        buildConfigField("int", "VERSION_CODE", "5")
        buildConfigField("String", "VERSION_NAME", "\"1.1.2\"")
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        if (hasReleaseKeystore) {
            create("release") {
                storeFile = rootProject.file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = when {
                hasReleaseKeystore -> signingConfigs.getByName("release")
                signWithDebugKey -> signingConfigs.getByName("debug")
                else -> throw GradleException(
                    "Refusing to build a release APK without a signing key.\n" +
                        "keystore.properties was not found at ${keystorePropertiesFile.absolutePath}.\n" +
                        "Create it (see store/keystore.properties.example) so the APK is signed with the " +
                        "real release key — app stores reject debug-signed APKs.\n" +
                        "For a throwaway local build pass -Pspimp3.signWithDebugKey=true."
                )
            }
        }
    }

    // Google Play delivers an App Bundle (.aab), split per device density and
    // language, so a single universal APK is no longer the upload format.
    bundle {
        language { enableSplit = false }
        density { enableSplit = true }
        abi { enableSplit = true }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }

    lint {
        // The app is a Media3 media-session app: MediaSession, MediaLibraryService
        // and their notification provider are the APIs the library marks @UnstableApi.
        // That is the intended usage here, so the opt-in check would only add noise.
        disable += "UnsafeOptInUsageError"
        warningsAsErrors = false
        abortOnError = true
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/LICENSE*"
        }
    }
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll(
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            "-opt-in=androidx.compose.foundation.ExperimentalFoundationApi",
            "-opt-in=androidx.compose.animation.ExperimentalAnimationApi",
            "-opt-in=androidx.media3.common.util.UnstableApi",
        )
    }
}

dependencies {
    coreLibraryDesugaring(libs.desugar.jdk.libs)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.datastore.preferences)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.foundation)
    implementation(libs.compose.animation)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons)
    debugImplementation(libs.compose.ui.tooling)

    implementation(libs.media3.exoplayer)
    implementation(libs.media3.session)
    implementation(libs.media3.common)
    implementation(libs.media3.datasource)

    implementation(libs.coil.compose)
    implementation(libs.coil.core)

    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.jaudiotagger)
    implementation(libs.kotlinx.coroutines.guava)

    testImplementation(libs.junit.jupiter)
    testImplementation(libs.kotlin.test)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
