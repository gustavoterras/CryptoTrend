plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.dagger.hilt)
    alias(libs.plugins.kover)
    alias(libs.plugins.ksp)
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(project(":core:dsm"))
    implementation(project(":core:common"))
    implementation(project(":core:navigation"))
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