plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.serialization)
    alias(libs.plugins.dagger.hilt)
    alias(libs.plugins.kover)
    alias(libs.plugins.ksp)
}

android {
    namespace = "br.com.terras.app.cryptotrend.feature.feeds"
    compileSdk = 34

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildTypes {
        release {
            buildConfigField("String", "BASE_API_URL_NEWS", "\"https://newsapi.org/\"")
            buildConfigField("String", "API_TOKEN_NEWS", "\"51c3f7779ef94707a83682d131d0e48d\"")
        }
        debug {
            buildConfigField("String", "BASE_API_URL_NEWS", "\"https://newsapi.org/\"")
            buildConfigField("String", "API_TOKEN_NEWS", "\"51c3f7779ef94707a83682d131d0e48d\"")
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(project(":core:dsm"))
    implementation(libs.lottie.compose)
    implementation(project(":core:network"))
    implementation(project(":core:database"))
    implementation(project(":core:common"))
    implementation(libs.dagger.hilt.core)
    ksp(libs.dagger.hilt.compiler)
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    testImplementation(libs.coroutines)
    testImplementation(libs.core.testing)
    androidTestImplementation(libs.androidx.junit)
}