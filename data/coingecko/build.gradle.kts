plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.dagger.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kover)
}

android {
    namespace = "br.com.terras.app.cryptotrend.data.coingecko"
    compileSdk = 34

    defaultConfig {
        minSdk = 26
    }

    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        release {
            buildConfigField("String", "BASE_API_URL", "\"https://api.coingecko.com/api/\"")
            buildConfigField("String", "API_TOKEN", "\"CG-x8oLvVwbVPmkTizyqH6WrPWU\"")
        }
        debug {
            buildConfigField("String", "BASE_API_URL", "\"https://api.coingecko.com/api/\"")
            buildConfigField("String", "API_TOKEN", "\"CG-x8oLvVwbVPmkTizyqH6WrPWU\"")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation(project(":core:network"))
    implementation(libs.dagger.hilt.core)
    ksp(libs.dagger.hilt.compiler)
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.coroutines)
}
