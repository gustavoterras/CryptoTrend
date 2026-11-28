package br.com.terras.app.cryptotrend.ai

import android.util.Log
import br.com.terras.app.common.ai.AiChartAnalysisReport
import br.com.terras.app.common.ai.AiChartAnalysisRequest
import br.com.terras.app.common.ai.AiPortfolioAnalysisReport
import br.com.terras.app.common.ai.AiPortfolioAnalysisRequest
import br.com.terras.app.common.ai.AiPortfolioHealth
import br.com.terras.app.common.ai.AiTrendBias
import br.com.terras.app.common.ai.CryptoAiAnalyzer
import com.google.firebase.Firebase
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "FirebaseCryptoAi"
private const val GEMINI_MODEL_NAME = "gemini-3.8-flash"
private const val AI_TIMEOUT_MS = 120_000L

@Singleton
class FirebaseCryptoAiAnalyzer @Inject constructor() : CryptoAiAnalyzer {

    private val chartSchema: Schema by lazy {
        Schema.obj(
            properties = mapOf(
                "headline" to Schema.string(description = "Concise one-line technical takeaway for the chart."),
                "trendBias" to Schema.enumeration(
                    values = listOf("BULLISH", "BEARISH", "NEUTRAL"),
                    description = "Dominant directional bias observed in the chart data."
                ),
                "summary" to Schema.string(description = "2-3 sentence technical synthesis of price structure, momentum, and volatility."),
                "supportLevels" to Schema.array(
                    items = Schema.string(),
                    description = "1 to 3 key support price levels formatted in the same currency as the input data."
                ),
                "resistanceLevels" to Schema.array(
                    items = Schema.string(),
                    description = "1 to 3 key resistance price levels formatted in the same currency as the input data."
                ),
                "keyObservations" to Schema.array(
                    items = Schema.string(),
                    description = "3 to 4 concise bullet insights grounded strictly in the provided chart and market metrics."
                ),
                "riskNote" to Schema.string(description = "Brief educational disclaimer and key invalidation/volatility risk note.")
            )
        )
    }

    private val portfolioSchema: Schema by lazy {
        Schema.obj(
            properties = mapOf(
                "headline" to Schema.string(description = "Concise one-line executive diagnosis of the portfolio."),
                "healthRating" to Schema.enumeration(
                    values = listOf("STRONG", "BALANCED", "CONCENTRATED", "HIGH_RISK"),
                    description = "Overall portfolio diversification and risk balance rating."
                ),
                "summary" to Schema.string(description = "2-3 sentence overview of consolidated performance, allocation balance, and PnL."),
                "strengths" to Schema.array(
                    items = Schema.string(),
                    description = "2 to 3 positive highlights grounded in the portfolio metrics."
                ),
                "risksAndConcentration" to Schema.array(
                    items = Schema.string(),
                    description = "2 to 3 risk, drawdown, or concentration observations grounded in the holdings."
                ),
                "actionableIdeas" to Schema.array(
                    items = Schema.string(),
                    description = "2 to 3 practical, non-prescriptive risk management or rebalancing considerations."
                ),
                "disclaimer" to Schema.string(description = "Short educational note clarifying this is analytical insight, not financial advice.")
            )
        )
    }

    private val chartModel: GenerativeModel by lazy {
        Firebase.ai.generativeModel(
            modelName = GEMINI_MODEL_NAME,
            generationConfig = generationConfig {
                temperature = 0.35f
                responseMimeType = "application/json"
                responseSchema = chartSchema
            }
        )
    }

    private val portfolioModel: GenerativeModel by lazy {
        Firebase.ai.generativeModel(
            modelName = GEMINI_MODEL_NAME,
            generationConfig = generationConfig {
                temperature = 0.35f
                responseMimeType = "application/json"
                responseSchema = portfolioSchema
            }
        )
    }

    override suspend fun analyzeChart(request: AiChartAnalysisRequest): AiChartAnalysisReport {
        val prompt = buildChartPrompt(request)
        val isPt = request.languageTag.startsWith("pt", ignoreCase = true)
        return try {
            val response = withTimeout(AI_TIMEOUT_MS) {
                retryWithBackoff {
                    chartModel.generateContent(prompt)
                }
            }
            val jsonText = response.text?.takeIf { it.isNotBlank() }
                ?: throw IllegalStateException(
                    if (isPt) "A IA retornou uma resposta vazia." else "The AI returned an empty response."
                )
            parseChartReport(jsonText, isPt)
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Chart AI analysis timed out", e)
            throw IllegalStateException(
                if (isPt) {
                    "A análise do gráfico expirou após 120 segundos. Tente novamente."
                } else {
                    "Chart analysis timed out after 120 seconds. Please try again."
                },
                e
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Chart AI analysis failed", e)
            val msg = e.message.orEmpty()
            val friendlyMsg = if (msg.contains("high demand", ignoreCase = true) || msg.contains("ServerException", ignoreCase = true) || msg.contains("503", ignoreCase = true) || msg.contains("unavailable", ignoreCase = true) || msg.contains("overloaded", ignoreCase = true)) {
                if (isPt) "O modelo de IA está experimentando alta demanda no momento. Por favor, tente novamente em alguns instantes."
                else "The AI model is currently experiencing high demand. Please try again in a few moments."
            } else {
                e.localizedMessage?.takeIf { it.isNotBlank() }
                    ?: if (isPt) "Falha ao gerar análise gráfica." else "Failed to generate chart analysis."
            }
            throw IllegalStateException(friendlyMsg, e)
        }
    }

    override suspend fun analyzePortfolio(request: AiPortfolioAnalysisRequest): AiPortfolioAnalysisReport {
        val prompt = buildPortfolioPrompt(request)
        val isPt = request.languageTag.startsWith("pt", ignoreCase = true)
        return try {
            val response = withTimeout(AI_TIMEOUT_MS) {
                retryWithBackoff {
                    portfolioModel.generateContent(prompt)
                }
            }
            val jsonText = response.text?.takeIf { it.isNotBlank() }
                ?: throw IllegalStateException(
                    if (isPt) "A IA retornou uma resposta vazia." else "The AI returned an empty response."
                )
            parsePortfolioReport(jsonText, isPt)
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Portfolio AI analysis timed out", e)
            throw IllegalStateException(
                if (isPt) {
                    "A análise do portfólio expirou após 120 segundos. Tente novamente."
                } else {
                    "Portfolio analysis timed out after 120 seconds. Please try again."
                },
                e
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Portfolio AI analysis failed", e)
            val msg = e.message.orEmpty()
            val friendlyMsg = if (msg.contains("high demand", ignoreCase = true) || msg.contains("ServerException", ignoreCase = true) || msg.contains("503", ignoreCase = true) || msg.contains("unavailable", ignoreCase = true) || msg.contains("overloaded", ignoreCase = true)) {
                if (isPt) "O modelo de IA está experimentando alta demanda no momento. Por favor, tente novamente em alguns instantes."
                else "The AI model is currently experiencing high demand. Please try again in a few moments."
            } else {
                e.localizedMessage?.takeIf { it.isNotBlank() }
                    ?: if (isPt) "Falha ao gerar análise do portfólio." else "Failed to generate portfolio analysis."
            }
            throw IllegalStateException(friendlyMsg, e)
        }
    }

    private fun buildChartPrompt(request: AiChartAnalysisRequest): String {
        val isPt = request.languageTag.startsWith("pt", ignoreCase = true)
        val responseLanguage = if (isPt) "Portuguese (Brazil)" else "English"
        val periodsText = if (request.periodChanges.isEmpty()) {
            "N/A"
        } else {
            request.periodChanges.joinToString(", ") { "${it.first}: ${it.second}" }
        }

        return buildString {
            appendLine("You are a disciplined cryptocurrency market technical analyst.")
            appendLine("Write all human-readable JSON string fields in $responseLanguage (keep trendBias strictly one of BULLISH, BEARISH, NEUTRAL).")
            appendLine("Use ONLY the numbers provided below. Never invent prices, indicators, or news not present in the payload.")
            appendLine("For supportLevels and resistanceLevels, derive realistic zones from the sampled price range, 24h high/low, and period extremes, formatted in the same currency notation as the input.")
            appendLine()
            appendLine("Asset: ${request.coinName} (${request.coinSymbol.uppercase(Locale.US)}) [${request.coinId}]")
            appendLine("Current Price: ${request.currentPriceFormatted}")
            appendLine("Chart Timeframe: ${request.timeframeLabel} (${request.chartTypeLabel}, ${request.chartPointsCount} points)")
            appendLine("24h Change: ${request.change24hFormatted} | 24h High: ${request.high24hFormatted} | 24h Low: ${request.low24hFormatted} | 24h Vol: ${request.volume24hFormatted}")
            appendLine("Multi-Period Changes: $periodsText")
            appendLine("Displayed Period Range: start=${request.periodStartPriceFormatted}, end=${request.periodEndPriceFormatted}, min=${request.periodMinPriceFormatted}, max=${request.periodMaxPriceFormatted}, netChange=${request.periodChangePercentFormatted}")
            appendLine("Sampled Price Series (chronological):")
            appendLine(request.sampledPointsSummary)
            if (!request.orderBookSummary.isNullOrBlank()) {
                appendLine("Live Order Book Snapshot: ${request.orderBookSummary}")
            }
            if (!request.portfolioPositionSummary.isNullOrBlank()) {
                appendLine("User Position in This Asset: ${request.portfolioPositionSummary}")
            }
        }
    }

    private fun buildPortfolioPrompt(request: AiPortfolioAnalysisRequest): String {
        val isPt = request.languageTag.startsWith("pt", ignoreCase = true)
        val responseLanguage = if (isPt) "Portuguese (Brazil)" else "English"

        return buildString {
            appendLine("You are an objective crypto portfolio risk and allocation analyst.")
            appendLine("Write all human-readable JSON string fields in $responseLanguage (keep healthRating strictly one of STRONG, BALANCED, CONCENTRATED, HIGH_RISK).")
            appendLine("Evaluate diversification, concentration risk, cost basis vs current valuation, unrealized/realized PnL, and recent activity using ONLY the data below.")
            appendLine("Do not give direct buy/sell commands; frame ideas as educational portfolio risk and rebalancing considerations.")
            appendLine()
            appendLine("Portfolio Totals:")
            appendLine("- Total Balance: ${request.totalBalanceFormatted}")
            appendLine("- Total Invested: ${request.totalInvestedFormatted}")
            appendLine("- Total P&L: ${request.totalPnLFormatted} (${request.unrealizedPnLPercentFormatted})")
            appendLine("- Realized P&L: ${request.realizedPnLFormatted}")
            appendLine("- 24h Portfolio Change: ${request.change24hFormatted}")
            appendLine("- Active Assets: ${request.assetCount} | Recorded Transactions: ${request.transactionCount} | Offline Quotes Fallback: ${request.isOfflineFallback}")
            appendLine()
            appendLine("Holdings Breakdown:")
            if (request.holdings.isEmpty()) {
                appendLine("- No open holdings (all positions currently closed).")
            } else {
                request.holdings.forEach { h ->
                    val allocStr = String.format(Locale.US, "%.1f%%", h.allocationPercent)
                    appendLine(
                        "- ${h.name} (${h.symbol}): allocation=$allocStr, currentValue=${h.currentValueFormatted}, " +
                            "invested=${h.investedFormatted}, avgBuy=${h.averageBuyPriceFormatted}, " +
                            "currentPrice=${h.currentPriceFormatted}, unrealizedPnL=${h.unrealizedPnLFormatted} (${h.unrealizedPnLPercentFormatted}), " +
                            "24h=${h.change24hPercentFormatted}"
                    )
                }
            }
            if (request.recentTransactionsSummary.isNotEmpty()) {
                appendLine()
                appendLine("Recent Transactions:")
                request.recentTransactionsSummary.forEach { tx ->
                    appendLine("- $tx")
                }
            }
        }
    }

    private fun parseChartReport(jsonText: String, isPt: Boolean): AiChartAnalysisReport {
        val obj = JSONObject(jsonText)
        return AiChartAnalysisReport(
            headline = obj.optString("headline").ifBlank {
                if (isPt) "Leitura técnica do gráfico" else "Technical chart overview"
            },
            trendBias = AiTrendBias.fromRaw(obj.optString("trendBias")),
            summary = obj.optString("summary").ifBlank {
                if (isPt) "Sem resumo disponível." else "No summary available."
            },
            supportLevels = obj.optJSONArray("supportLevels").toStringList(),
            resistanceLevels = obj.optJSONArray("resistanceLevels").toStringList(),
            keyObservations = obj.optJSONArray("keyObservations").toStringList(),
            riskNote = obj.optString("riskNote").ifBlank {
                if (isPt) {
                    "Análise informativa baseada nos dados exibidos; não constitui recomendação financeira."
                } else {
                    "Informational analysis based on displayed data; not financial advice."
                }
            }
        )
    }

    private fun parsePortfolioReport(jsonText: String, isPt: Boolean): AiPortfolioAnalysisReport {
        val obj = JSONObject(jsonText)
        return AiPortfolioAnalysisReport(
            headline = obj.optString("headline").ifBlank {
                if (isPt) "Diagnóstico do portfólio" else "Portfolio diagnosis"
            },
            healthRating = AiPortfolioHealth.fromRaw(obj.optString("healthRating")),
            summary = obj.optString("summary").ifBlank {
                if (isPt) "Sem resumo disponível." else "No summary available."
            },
            strengths = obj.optJSONArray("strengths").toStringList(),
            risksAndConcentration = obj.optJSONArray("risksAndConcentration").toStringList(),
            actionableIdeas = obj.optJSONArray("actionableIdeas").toStringList(),
            disclaimer = obj.optString("disclaimer").ifBlank {
                if (isPt) {
                    "Análise educacional gerada por IA; não substitui aconselhamento financeiro profissional."
                } else {
                    "AI-generated educational analysis; does not constitute professional financial advice."
                }
            }
        )
    }

    private fun JSONArray?.toStringList(): List<String> {
        if (this == null) return emptyList()
        return buildList(length()) {
            for (i in 0 until length()) {
                val value = optString(i).trim()
                if (value.isNotEmpty()) add(value)
            }
        }
    }

    private suspend fun <T> retryWithBackoff(
        times: Int = 3,
        initialDelay: Long = 1000L,
        factor: Double = 2.0,
        block: suspend () -> T
    ): T {
        var currentDelay = initialDelay
        repeat(times) { attempt ->
            try {
                return block()
            } catch (e: Exception) {
                val msg = e.message.orEmpty()
                val isTransient = msg.contains("high demand", ignoreCase = true) ||
                                  msg.contains("ServerException", ignoreCase = true) ||
                                  msg.contains("503", ignoreCase = true) ||
                                  msg.contains("unavailable", ignoreCase = true) ||
                                  msg.contains("overloaded", ignoreCase = true)
                if (isTransient && attempt < times - 1) {
                    Log.w(TAG, "Transient AI error on attempt ${attempt + 1}: $msg. Retrying in ${currentDelay}ms...", e)
                    kotlinx.coroutines.delay(currentDelay)
                    currentDelay = (currentDelay * factor).toLong()
                } else {
                    throw e
                }
            }
        }
        return block()
    }
}
