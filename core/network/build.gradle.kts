plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.dagger.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "br.com.terras.app.cryptotrend.core.network"
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

    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        getByName("release") {
            buildConfigField(
                "String",
                "BASE_API_URL",
                "\"https://api.coingecko.com/api/\""
            )

            buildConfigField(
                "String",
                "API_TOKEN",
                "\"CG-x8oLvVwbVPmkTizyqH6WrPWU\""
            )

            buildConfigField(
                "String",
                "BASE_API_URL_NEWS",
                "\"https://newsapi.org/\""
            )

            buildConfigField(
                "String",
                "API_TOKEN_NEWS",
                "\"51c3f7779ef94707a83682d131d0e48d\""
            )
        }

        getByName("debug") {
            buildConfigField(
                "String",
                "BASE_API_URL",
                "\"https://api.coingecko.com/api/\""
            )

            buildConfigField(
                "String",
                "API_TOKEN",
                "\"CG-x8oLvVwbVPmkTizyqH6WrPWU\""
            )

            buildConfigField(
                "String",
                "BASE_API_URL_NEWS",
                "\"https://newsapi.org/\""
            )

            buildConfigField(
                "String",
                "API_TOKEN_NEWS",
                "\"51c3f7779ef94707a83682d131d0e48d\""
            )
        }
    }
}

dependencies {
    api(libs.bundles.playground.network)
    ksp(libs.dagger.hilt.compiler)
}