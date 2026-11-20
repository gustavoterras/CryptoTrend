package br.com.terras.app.coins.detail.domain.model

data class CoinInsightVO(
    val title: String,
    val description: String,
    val relatedCoinIds: List<String> = emptyList(),
    val postedAt: String = ""
)
