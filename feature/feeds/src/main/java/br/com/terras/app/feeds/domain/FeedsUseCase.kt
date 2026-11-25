package br.com.terras.app.feeds.domain

import br.com.terras.app.common.YYYY_MM_DD
import br.com.terras.app.feeds.data.FeedsRepository
import br.com.terras.app.feeds.domain.model.ArticleVO
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

interface FeedsUseCase {
    suspend fun getFeeds(query: String): Result<List<ArticleVO>>

    suspend fun getCachedFeeds(query: String): List<ArticleVO>? = null
    suspend fun getFeeds(query: String, forceRefresh: Boolean): Result<List<ArticleVO>> =
        getFeeds(query)
}

class FeedsUseCaseImpl @Inject constructor(
    private val repository: FeedsRepository,
    private val mapper: FeedsMapper
) : FeedsUseCase {

    override suspend fun getFeeds(query: String): Result<List<ArticleVO>> =
        getFeeds(query = query, forceRefresh = false)

    override suspend fun getCachedFeeds(query: String): List<ArticleVO>? {
        val (fromDate, language) = currentDateAndLanguage()
        val cached = repository.getCachedFeeds(
            query = query,
            fromDate = fromDate,
            language = language
        ) ?: return null
        return mapper.toFeeds(cached.articles)
    }

    override suspend fun getFeeds(query: String, forceRefresh: Boolean): Result<List<ArticleVO>> {
        val (fromDate, language) = currentDateAndLanguage()
        val result = if (forceRefresh) {
            repository.getFeeds(
                query = query,
                fromDate = fromDate,
                language = language,
                forceRefresh = true
            )
        } else {
            repository.getFeeds(
                query = query,
                fromDate = fromDate,
                language = language
            )
        }
        return result.map { response ->
            mapper.toFeeds(response.articles)
        }
    }

    private fun currentDateAndLanguage(): Pair<String, String> {
        val locale = Locale.getDefault()
        val fromDate = SimpleDateFormat(YYYY_MM_DD, locale).format(
            Calendar.getInstance().apply {
                set(Calendar.DAY_OF_MONTH, -7)
            }.time
        )
        return fromDate to locale.language
    }
}