package br.com.terras.app.portfolio.data

import br.com.terras.app.portfolio.domain.model.PortfolioMarketQuote
import br.com.terras.app.portfolio.network.ApiServicePortfolio
import br.com.terras.app.portfolio.network.PortfolioMarketCoinDto
import javax.inject.Inject

interface PortfolioMarketRepository {
    suspend fun getMarketQuotes(coinIds: List<String>): Result<Map<String, PortfolioMarketQuote>>
    suspend fun getTopMarketCoins(limit: Int = 25): Result<List<PortfolioMarketQuote>>
}

class PortfolioMarketRepositoryImpl @Inject constructor(
    private val api: ApiServicePortfolio
) : PortfolioMarketRepository {

    override suspend fun getMarketQuotes(coinIds: List<String>): Result<Map<String, PortfolioMarketQuote>> {
        val cleanedIds = coinIds.map { it.trim().lowercase() }.filter { it.isNotEmpty() }.distinct()
        if (cleanedIds.isEmpty()) {
            return Result.success(emptyMap())
        }
        return try {
            val response = api.getMarketQuotesByIds(cleanedIds.joinToString(","))
            Result.success(response.mapNotNull { it.toDomainOrNull() }.associateBy { it.id })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getTopMarketCoins(limit: Int): Result<List<PortfolioMarketQuote>> {
        return try {
            val response = api.getTopMarketCoins(perPage = limit)
            Result.success(response.mapNotNull { it.toDomainOrNull() })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun PortfolioMarketCoinDto.toDomainOrNull(): PortfolioMarketQuote? {
        val price = currentPrice ?: return null
        return PortfolioMarketQuote(
            id = id.trim().lowercase(),
            symbol = symbol.trim().uppercase(),
            name = name.trim(),
            imageUrl = image.trim(),
            currentPrice = price,
            priceChangePercentage24h = priceChangePercentage24h ?: 0.0
        )
    }
}
