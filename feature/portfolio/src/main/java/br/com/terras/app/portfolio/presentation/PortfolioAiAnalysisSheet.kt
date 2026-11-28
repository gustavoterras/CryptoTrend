package br.com.terras.app.portfolio.presentation

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.com.terras.app.common.ai.AiAnalysisUiState
import br.com.terras.app.common.ai.AiPortfolioAnalysisReport
import br.com.terras.app.common.ai.AiPortfolioHealth
import br.com.terras.app.cryptotrend.core.dsm.R
import br.com.terras.app.dsm.ui.component.AiAnalysisBottomSheet
import br.com.terras.app.dsm.ui.component.AiAnalysisErrorContent
import br.com.terras.app.dsm.ui.component.AiAnalysisLoadingContent
import br.com.terras.app.dsm.ui.component.AiBulletSection
import br.com.terras.app.dsm.ui.component.AiRefreshButton
import br.com.terras.app.dsm.ui.component.AiStatusBadge
import br.com.terras.app.dsm.ui.component.TrendColor

@Composable
fun PortfolioAiAnalysisSheet(
    assetCount: Int,
    totalBalanceFormatted: String,
    state: AiAnalysisUiState<AiPortfolioAnalysisReport>,
    onRetry: () -> Unit,
    onDismiss: () -> Unit
) {
    AiAnalysisBottomSheet(
        title = stringResource(R.string.ai_portfolio_sheet_title),
        subtitle = stringResource(
            R.string.ai_portfolio_sheet_subtitle,
            assetCount,
            totalBalanceFormatted
        ),
        onDismiss = onDismiss,
        sheetTestTag = "portfolio_ai_sheet"
    ) {
        when (state) {
            is AiAnalysisUiState.Idle,
            is AiAnalysisUiState.Loading -> {
                AiAnalysisLoadingContent(
                    statusMessage = stringResource(R.string.ai_portfolio_loading)
                )
            }
            is AiAnalysisUiState.Error -> {
                AiAnalysisErrorContent(
                    message = state.message,
                    onRetry = onRetry,
                    retryTestTag = "portfolio_ai_retry_button"
                )
            }
            is AiAnalysisUiState.Success -> {
                val report = state.data
                val (healthLabel, healthColor) = when (report.healthRating) {
                    AiPortfolioHealth.STRONG ->
                        stringResource(R.string.ai_portfolio_health_strong) to TrendColor.UP.textColor
                    AiPortfolioHealth.BALANCED ->
                        stringResource(R.string.ai_portfolio_health_balanced) to MaterialTheme.colorScheme.scrim
                    AiPortfolioHealth.CONCENTRATED ->
                        stringResource(R.string.ai_portfolio_health_concentrated) to MaterialTheme.colorScheme.inversePrimary
                    AiPortfolioHealth.HIGH_RISK ->
                        stringResource(R.string.ai_portfolio_health_high_risk) to TrendColor.DOWN.textColor
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AiStatusBadge(
                        label = healthLabel,
                        containerColor = healthColor.copy(alpha = 0.14f),
                        contentColor = healthColor
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

                    AiBulletSection(
                        title = stringResource(R.string.ai_portfolio_strengths),
                        items = report.strengths,
                        bulletColor = TrendColor.UP.textColor
                    )

                    AiBulletSection(
                        title = stringResource(R.string.ai_portfolio_risks),
                        items = report.risksAndConcentration,
                        bulletColor = TrendColor.DOWN.textColor
                    )

                    AiBulletSection(
                        title = stringResource(R.string.ai_portfolio_ideas),
                        items = report.actionableIdeas,
                        bulletColor = MaterialTheme.colorScheme.scrim
                    )

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = report.disclaimer,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(12.dp)
                        )
                    }

                    AiRefreshButton(
                        label = stringResource(R.string.ai_reanalyze),
                        onClick = onRetry,
                        testTag = "portfolio_ai_reanalyze_button"
                    )
                }
            }
        }
    }
}
