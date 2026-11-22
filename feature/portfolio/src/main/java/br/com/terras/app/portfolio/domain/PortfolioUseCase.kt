package br.com.terras.app.portfolio.domain

import br.com.terras.app.network.portfolio.PortfolioLocalRepository
import br.com.terras.app.network.portfolio.PortfolioTransactionRecord
import br.com.terras.app.portfolio.data.PortfolioMarketRepository
import br.com.terras.app.portfolio.domain.model.PortfolioMarketQuote
import br.com.terras.app.portfolio.domain.model.PortfolioSnapshot
import br.com.terras.app.portfolio.domain.model.SelectableCoinVO
import br.com.terras.app.search.domain.SearchUseCase
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

interface PortfolioUseCase {
    fun observeTransactions(): Flow<List<PortfolioTransactionRecord>>
    fun buildSnapshot(
        transactions: List<PortfolioTransactionRecord>,
        quotes: Map<String, PortfolioMarketQuote>
    ): PortfolioSnapshot

    fun canApplyTimelineMutation(
        transactions: List<PortfolioTransactionRecord>,
        coinId: String,
        candidateTransaction: PortfolioTransactionRecord?,
        excludingTransactionId: Long? = null
    ): Boolean

    suspend fun getMarketQuotes(coinIds: List<String>): Result<Map<String, PortfolioMarketQuote>>
    suspend fun getTopSelectableCoins(): Result<List<SelectableCoinVO>>
    suspend fun searchSelectableCoins(query: String): Result<List<SelectableCoinVO>>
    suspend fun insertTransaction(record: PortfolioTransactionRecord): Long
    suspend fun updateTransaction(record: PortfolioTransactionRecord)
    suspend fun deleteTransaction(id: Long)
}

class PortfolioUseCaseImpl @Inject constructor(
    private val localRepository: PortfolioLocalRepository,
    private val marketRepository: PortfolioMarketRepository,
    private val searchUseCase: SearchUseCase,
    private val calculator: PortfolioCalculator
) : PortfolioUseCase {

    override fun observeTransactions(): Flow<List<PortfolioTransactionRecord>> =
        localRepository.observeTransactionsChronological()

    override fun buildSnapshot(
        transactions: List<PortfolioTransactionRecord>,
        quotes: Map<String, PortfolioMarketQuote>
    ): PortfolioSnapshot = calculator.calculate(transactions, quotes)

    override fun canApplyTimelineMutation(
        transactions: List<PortfolioTransactionRecord>,
        coinId: String,
        candidateTransaction: PortfolioTransactionRecord?,
        excludingTransactionId: Long?
    ): Boolean = calculator.availableQuantityBeforeOrWithReplacement(
        transactions = transactions,
        coinId = coinId,
        candidateTransaction = candidateTransaction,
        excludingTransactionId = excludingTransactionId
    )

    override suspend fun getMarketQuotes(coinIds: List<String>): Result<Map<String, PortfolioMarketQuote>> =
        marketRepository.getMarketQuotes(coinIds)

    override suspend fun getTopSelectableCoins(): Result<List<SelectableCoinVO>> {
        return marketRepository.getTopMarketCoins(limit = 25).map { quotes ->
            quotes.map { quote ->
                SelectableCoinVO(
                    id = quote.id,
                    symbol = quote.symbol,
                    name = quote.name,
                    imageUrl = quote.imageUrl,
                    currentPrice = quote.currentPrice
                )
            }
        }
    }

    override suspend fun searchSelectableCoins(query: String): Result<List<SelectableCoinVO>> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return Result.success(emptyList())
        return searchUseCase.searchCoins(trimmed).map { coins ->
            coins.map { coin ->
                SelectableCoinVO(
                    id = coin.id.trim().lowercase(),
                    symbol = coin.symbol.trim().uppercase(),
                    name = coin.name.trim(),
                    imageUrl = coin.thumb.trim(),
                    currentPrice = null
                )
            }
        }
    }

    override suspend fun insertTransaction(record: PortfolioTransactionRecord): Long =
        localRepository.insertTransaction(record)

    override suspend fun updateTransaction(record: PortfolioTransactionRecord) =
        localRepository.updateTransaction(record)

    override suspend fun deleteTransaction(id: Long) =
        localRepository.deleteTransaction(id)
}
