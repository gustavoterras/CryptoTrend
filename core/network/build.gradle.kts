plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.jetbrains.kotlin.android)
}

android {
    namespace = "br.com.terras.app.cryptotrend.core.network"
    compileSdk = 34

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_18
        targetCompatibility = JavaVersion.VERSION_18
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
}