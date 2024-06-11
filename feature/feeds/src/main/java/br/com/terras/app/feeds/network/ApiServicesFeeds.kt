package br.com.terras.app.feeds.network

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.url

class ApiServicesFeeds(
    private val baseUrl: String,
    private val client: HttpClient
) {

    companion object {
        private const val FEEDS_DATA_ROUTE = "v2/everything"
        private const val FEEDS_DATA_PARAMS_QUERY = "q"
        private const val FEEDS_DATA_PARAMS_DATE = "from"
        private const val FEEDS_DATA_PARAMS_LANGUAGE = "language"
    }

    suspend fun getFeeds(
        query: String,
        fromDate: String,
        language: String
    ) = client.get {
        url("$baseUrl$FEEDS_DATA_ROUTE")
        parameter(FEEDS_DATA_PARAMS_QUERY, query)
        parameter(FEEDS_DATA_PARAMS_DATE, fromDate)
        parameter(FEEDS_DATA_PARAMS_LANGUAGE, language)
    }
}