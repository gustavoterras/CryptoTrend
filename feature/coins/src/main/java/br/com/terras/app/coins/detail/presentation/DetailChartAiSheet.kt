package br.com.terras.app.coins.detail.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.com.terras.app.common.ai.AiAnalysisUiState
import br.com.terras.app.common.ai.AiChartAnalysisReport
import br.com.terras.app.common.ai.AiTrendBias
import br.com.terras.app.cryptotrend.core.dsm.R
import br.com.terras.app.dsm.ui.component.AiAnalysisBottomSheet
import br.com.terras.app.dsm.ui.component.AiAnalysisErrorContent
import br.com.terras.app.dsm.ui.component.AiAnalysisLoadingContent
import br.com.terras.app.dsm.ui.component.AiBulletSection
import br.com.terras.app.dsm.ui.component.AiPriceLevelsCard
import br.com.terras.app.dsm.ui.component.AiRefreshButton
import br.com.terras.app.dsm.ui.component.AiStatusBadge
import br.com.terras.app.dsm.ui.component.TrendColor
import java.util.Locale

@Composable
fun DetailChartAiSheet(
    coinName: String,
    coinSymbol: String,
    timeframeLabel: String,
    state: AiAnalysisUiState<AiChartAnalysisReport>,
    onRetry: () -> Unit,
    onDismiss: () -> Unit
) {
    AiAnalysisBottomSheet(
        title = stringResource(R.string.ai_chart_sheet_title),
        subtitle = stringResource(
            R.string.ai_chart_sheet_subtitle,
            coinName,
            coinSymbol.uppercase(Locale.US),
            timeframeLabel
        ),
        onDismiss = onDismiss,
        sheetTestTag = "detail_ai_chart_sheet"
    ) {
        when (state) {
            is AiAnalysisUiState.Idle,
            is AiAnalysisUiState.Loading -> {
                AiAnalysisLoadingContent(
                    statusMessage = stringResource(
                        R.string.ai_chart_loading,
                        coinSymbol.uppercase(Locale.US),
                        timeframeLabel
                    )
                )
            }
            is AiAnalysisUiState.Error -> {
                AiAnalysisErrorContent(
                    message = state.message,
                    onRetry = onRetry,
                    retryTestTag = "detail_ai_chart_retry_button"
                )
            }
            is AiAnalysisUiState.Success -> {
                val report = state.data
                val biasLabel = when (report.trendBias) {
                    AiTrendBias.BULLISH -> stringResource(R.string.ai_chart_bias_bullish)
                    AiTrendBias.BEARISH -> stringResource(R.string.ai_chart_bias_bearish)
                    AiTrendBias.NEUTRAL -> stringResource(R.string.ai_chart_bias_neutral)
                }
                val biasTrend = when (report.trendBias) {
                    AiTrendBias.BULLISH -> TrendColor.UP
                    AiTrendBias.BEARISH -> TrendColor.DOWN
                    AiTrendBias.NEUTRAL -> TrendColor.FLAT
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AiStatusBadge(
                        label = biasLabel,
                        containerColor = biasTrend.textColor.copy(alpha = 0.14f),
                        contentColor = biasTrend.textColor
                    )

                    Text(
                        text = report.headline,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.surface
                    )

                    Text(
                        text = report.summary,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f)
                    )

                    AiPriceLevelsCard(
                        title = stringResource(R.string.ai_chart_support_levels),
                        levels = report.supportLevels,
                        accentColor = TrendColor.UP.textColor
                    )

                    AiPriceLevelsCard(
                        title = stringResource(R.string.ai_chart_resistance_levels),
                        levels = report.resistanceLevels,
                        accentColor = TrendColor.DOWN.textColor
                    )

                    AiBulletSection(
                        title = stringResource(R.string.ai_chart_observations),
                        items = report.keyObservations,
                        bulletColor = MaterialTheme.colorScheme.scrim
                    )

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.ai_chart_risk_note),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = report.riskNote,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    AiRefreshButton(
                        label = stringResource(R.string.ai_reanalyze),
                        onClick = onRetry,
                        testTag = "detail_ai_chart_reanalyze_button"
                    )
                }
            }
        }
    }
}
