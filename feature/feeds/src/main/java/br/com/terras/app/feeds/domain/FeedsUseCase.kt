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
}

class FeedsUseCaseImpl @Inject constructor(
    private val repository: FeedsRepository,
    private val mapper: FeedsMapper
) : FeedsUseCase {

    override suspend fun getFeeds(
        query: String
    ): Result<List<ArticleVO>> {
        val locale = Locale.getDefault()
        return repository.getFeeds(
            query = query,
            fromDate = SimpleDateFormat(YYYY_MM_DD, locale).format(
                Calendar.getInstance().apply {
                    set(Calendar.DAY_OF_MONTH, -7)
                }.time,
            ),
            language = locale.language
        ).map { result ->
            mapper.toFeeds(result.articles)
        }
    }
}