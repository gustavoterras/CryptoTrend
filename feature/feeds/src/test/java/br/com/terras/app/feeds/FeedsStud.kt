package br.com.terras.app.feeds

import br.com.terras.app.common.YYYY_MM_DD
import br.com.terras.app.feeds.data.model.Article
import br.com.terras.app.feeds.data.model.FeedsResponse
import br.com.terras.app.feeds.domain.model.ArticleVO
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

val mockArticleStub = Article(
    author = "author",
    title = "title",
    description = "description",
    url = "url",
    urlToImage = "urlToImage",
    publishedAt = "publishedAt",
    content = "content"
)

val mockArticleVOStub = ArticleVO(
    author = "author",
    title = "title",
    description = "description",
    url = "url",
    urlToImage = "urlToImage",
    publishedAt = "publishedAt",
    content = "content"
)

val mockFeedsResponseStub = FeedsResponse(
    articles = listOf(
        mockArticleStub
    )
)

val mockArticlesVOStub = listOf(mockArticleVOStub)

fun getDateFrom(): String = SimpleDateFormat(YYYY_MM_DD, Locale.getDefault()).format(
    Calendar.getInstance().apply {
        set(Calendar.DAY_OF_MONTH, -7)
    }.time
)