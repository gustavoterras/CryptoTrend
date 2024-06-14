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
        sourceCompatibility = JavaVersion.VERSION_18
        targetCompatibility = JavaVersion.VERSION_18
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(project(":core:dsm"))
    implementation(project(":core:network"))
    implementation(project(":core:common"))
    implementation(libs.dagger.hilt.core)
    testImplementation("org.junit.jupiter:junit-jupiter:5.8.1")
    ksp(libs.dagger.hilt.compiler)
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    testImplementation(libs.coroutines)
    testImplementation(libs.core.testing)
    androidTestImplementation(libs.androidx.junit)
}