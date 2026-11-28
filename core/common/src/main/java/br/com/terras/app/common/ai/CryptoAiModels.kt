package br.com.terras.app.common.ai

sealed interface AiAnalysisUiState<out T> {
    data object Idle : AiAnalysisUiState<Nothing>
    data object Loading : AiAnalysisUiState<Nothing>
    data class Success<T>(val data: T) : AiAnalysisUiState<T>
    data class Error(val message: String) : AiAnalysisUiState<Nothing>
}

enum class AiTrendBias {
    BULLISH,
    BEARISH,
    NEUTRAL;

    companion object {
        fun fromRaw(raw: String): AiTrendBias = when (raw.trim().uppercase()) {
            "BULLISH", "ALTA" -> BULLISH
            "BEARISH", "BAIXA" -> BEARISH
            else -> NEUTRAL
        }
    }
}

enum class AiPortfolioHealth {
    STRONG,
    BALANCED,
    CONCENTRATED,
    HIGH_RISK;

    companion object {
        fun fromRaw(raw: String): AiPortfolioHealth = when (raw.trim().uppercase()) {
            "STRONG", "FORTE", "SAUDAVEL" -> STRONG
            "BALANCED", "EQUILIBRADO" -> BALANCED
            "CONCENTRATED", "CONCENTRADO" -> CONCENTRATED
            "HIGH_RISK", "ALTO_RISCO" -> HIGH_RISK
            else -> BALANCED
        }
    }
}

data class AiChartAnalysisRequest(
    val coinId: String,
    val coinName: String,
    val coinSymbol: String,
    val currentPriceFormatted: String,
    val rawPrice: Double,
    val timeframeLabel: String,
    val chartTypeLabel: String,
    val change24hFormatted: String,
    val high24hFormatted: String,
    val low24hFormatted: String,
    val volume24hFormatted: String,
    val periodChanges: List<Pair<String, String>>,
    val chartPointsCount: Int,
    val periodStartPriceFormatted: String,
    val periodEndPriceFormatted: String,
    val periodMinPriceFormatted: String,
    val periodMaxPriceFormatted: String,
    val periodChangePercentFormatted: String,
    val sampledPointsSummary: String,
    val orderBookSummary: String? = null,
    val portfolioPositionSummary: String? = null,
    val languageTag: String = "pt"
)

data class AiChartAnalysisReport(
    val headline: String,
    val trendBias: AiTrendBias,
    val summary: String,
    val supportLevels: List<String>,
    val resistanceLevels: List<String>,
    val keyObservations: List<String>,
    val riskNote: String
)

data class AiPortfolioHoldingInput(
    val symbol: String,
    val name: String,
    val allocationPercent: Double,
    val investedFormatted: String,
    val currentValueFormatted: String,
    val averageBuyPriceFormatted: String,
    val currentPriceFormatted: String,
    val unrealizedPnLFormatted: String,
    val unrealizedPnLPercentFormatted: String,
    val change24hPercentFormatted: String
)

data class AiPortfolioAnalysisRequest(
    val totalBalanceFormatted: String,
    val totalInvestedFormatted: String,
    val totalPnLFormatted: String,
    val unrealizedPnLPercentFormatted: String,
    val realizedPnLFormatted: String,
    val change24hFormatted: String,
    val assetCount: Int,
    val transactionCount: Int,
    val isOfflineFallback: Boolean,
    val holdings: List<AiPortfolioHoldingInput>,
    val recentTransactionsSummary: List<String>,
    val languageTag: String = "pt"
)

data class AiPortfolioAnalysisReport(
    val headline: String,
    val healthRating: AiPortfolioHealth,
    val summary: String,
    val strengths: List<String>,
    val risksAndConcentration: List<String>,
    val actionableIdeas: List<String>,
    val disclaimer: String
)

interface CryptoAiAnalyzer {
    suspend fun analyzeChart(request: AiChartAnalysisRequest): AiChartAnalysisReport
    suspend fun analyzePortfolio(request: AiPortfolioAnalysisRequest): AiPortfolioAnalysisReport
}
