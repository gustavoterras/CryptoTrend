package br.com.terras.app.coins.detail.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CoinInsightResponse(
    @SerialName("title") val title: String = "",
    @SerialName("description") val description: String = "",
    @SerialName("related_coin_ids") val relatedCoinIds: List<String> = emptyList(),
    @SerialName("posted_at") val postedAt: String = ""
)
