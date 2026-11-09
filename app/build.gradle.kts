plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.dagger.hilt)
    alias(libs.plugins.serialization)
    alias(libs.plugins.kover)
    alias(libs.plugins.ksp)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.appdistribution)
}

android {
    namespace = "br.com.terras.app.cryptotrend"
    compileSdk = 34

    defaultConfig {
        applicationId = "br.com.terras.app.cryptotrend"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        create("debugConfig") {
            storeFile = file("${rootDir}/debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            signingConfig = signingConfigs.getByName("debugConfig")
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
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    firebaseAppDistribution {
        appId = "1:942204810323:android:ef2eb8ae55a51e16fdc2a6"
        artifactType = "APK"
        val customGroups = findProperty("groups")?.toString()
        if (!customGroups.isNullOrBlank()) {
            groups = customGroups
        }
    }
}

dependencies {
    implementation(project(":core:dsm"))
    implementation(project(":core:common"))
    implementation(project(":core:navigation"))
    implementation(project(":core:network"))
    implementation(project(":feature:favorites"))
    implementation(project(":feature:feeds"))
    implementation(project(":feature:coins"))
    implementation(project(":feature:search"))

    kover(project(":core:common"))
    "kover"(project(":feature:favorites"))
    kover(project(":feature:feeds"))
    kover(project(":feature:coins"))
    kover(project(":feature:search"))

    implementation(libs.dagger.hilt.core)
    ksp(libs.dagger.hilt.compiler)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}

kover.reports {
    filters {
        excludes {
            classes = listOf(
                "*Module*",
                "*Api*",
                "*Activity*",
                "*Compose*",
                "*Graph*",
            )
            annotatedBy(
                "androidx.compose.runtime.Composable",
                "androidx.compose.ui.tooling.preview.Preview",
                "dagger.hilt.android.HiltAndroidApp"
            )
        }
    }

    verify {
        rule {
            bound {
//                minValue.set(80)
            }
        }
    }
}