package br.com.terras.app.search.domain

import br.com.terras.app.search.data.model.SearchResponse
import br.com.terras.app.search.domain.model.CoinVO
import javax.inject.Inject

interface SearchMapper {
    fun toSearch(response: SearchResponse): List<CoinVO>
}

class SearchMapperImpl @Inject constructor() : SearchMapper {
    override fun toSearch(response: SearchResponse): List<CoinVO> {
        return response.coins.map {
            CoinVO(
                id = it.id,
                symbol = it.symbol.uppercase(),
                name = it.name,
                thumb = it.thumb
            )
        }
    }
}