package br.com.terras.app.feeds.domain

import br.com.terras.app.feeds.data.model.Article
import br.com.terras.app.feeds.domain.model.ArticleVO
import javax.inject.Inject

interface FeedsMapper {
    fun toFeeds(response: List<Article>): List<ArticleVO>
}

class FeedsMapperImpl @Inject constructor() : FeedsMapper {
    override fun toFeeds(response: List<Article>): List<ArticleVO> {
        return response.map { article ->
            ArticleVO(
                author = article.author.orEmpty(),
                title = article.title.orEmpty(),
                description = article.description.orEmpty(),
                url = article.url.orEmpty(),
                urlToImage = article.urlToImage.orEmpty(),
                publishedAt = article.publishedAt.orEmpty(),
                content = article.content.orEmpty()
            )
        }
    }
}