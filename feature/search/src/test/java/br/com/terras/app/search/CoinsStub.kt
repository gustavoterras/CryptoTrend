package br.com.terras.app.search

import br.com.terras.app.search.data.model.Coin
import br.com.terras.app.search.data.model.SearchResponse
import br.com.terras.app.search.domain.model.CoinVO

val mockCoinVOStub = CoinVO(
    id = "id",
    symbol = "SYMBOL",
    name = "name",
    thumb = "image"
)

val coinResponseStub = Coin(
    id = "id",
    symbol = "symbol",
    name = "name",
    thumb = "image"
)

val mockCoinsResponseStub = SearchResponse(coins = arrayListOf(coinResponseStub))

val mockCoinsVOListStub = listOf(mockCoinVOStub)