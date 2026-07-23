plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.indianservers.aiexplorer"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.indianservers.aiexplorer"
        minSdk = 31
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(project(":arengine"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.google.mlkit.text.recognition)
    implementation(libs.google.mlkit.digital.ink)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
}

listOf(
    "validatePhase1Foundation",
    "validatePhase2UnifiedStudio",
    "validatePhysicsFormulaDimensions",
    "validateChemistryFormulaDimensions",
    "validateScientificUnits",
    "validatePhase3Release"
).forEach { taskName ->
    tasks.register(taskName) {
        group = "verification"
        description = when (taskName) {
            "validatePhase1Foundation" -> "Runs the Phase 1 persistence, recovery, correctness, and interoperability contract suite."
            "validatePhase2UnifiedStudio" -> "Runs the Phase 2 linked algebra, graph, table, geometry, CAS, and persistence contract suite."
            else -> "Runs the verified Phase 3 scientific and release contract suite."
        }
        dependsOn("testDebugUnitTest")
    }
}
