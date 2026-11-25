package br.com.terras.app.coins.list.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import br.com.terras.app.coins.list.domain.model.FearAndGreedSentiment
import br.com.terras.app.coins.list.domain.model.GlobalMarketVO
import kotlin.math.roundToInt

internal enum class GlobalHeaderSection {
    FEAR_GREED,
    ALTCOIN_SEASON,
    MARKET_CAP,
    VOLUME
}

internal enum class BalloonBadgeTone {
    SENTIMENT,
    ALTCOIN_SEASON,
    TREND,
    VOLUME
}

internal data class BalloonSplitHighlight(
    val btcScore: Int,
    val altsScore: Int,
    val hasValue: Boolean
)

internal data class HeaderBalloonContent(
    val title: String,
    val badgeText: String,
    val badgeTone: BalloonBadgeTone,
    val splitHighlight: BalloonSplitHighlight? = null,
    val explanation: String
)

internal fun buildHeaderBalloonContent(
    section: GlobalHeaderSection,
    data: GlobalMarketVO,
    isEnglish: Boolean
): HeaderBalloonContent = when (section) {
    GlobalHeaderSection.FEAR_GREED -> {
        val sentimentTitle = localizedFearGreedLabel(data.fearAndGreedSentiment, isEnglish)
        val badge = if (data.hasFearAndGreed) {
            "$sentimentTitle · ${data.fearAndGreedValue}/100"
        } else {
            if (isEnglish) "Unavailable" else "Indisponível"
        }
        val explanation = if (!data.hasFearAndGreed) {
            if (isEnglish) {
                "Measures overall crypto market sentiment from 0 (Extreme Fear) to 100 (Extreme Greed) based on volatility, momentum, and investor behavior."
            } else {
                "Mede o sentimento geral do mercado cripto de 0 (Medo Extremo) a 100 (Ganância Extrema) com base em volatilidade, momentum e comportamento dos investidores."
            }
        } else {
            fearGreedExplanation(data.fearAndGreedSentiment, data.fearAndGreedValue, isEnglish)
        }
        HeaderBalloonContent(
            title = if (isEnglish) "Fear & Greed" else "Medo & Ganância",
            badgeText = badge,
            badgeTone = BalloonBadgeTone.SENTIMENT,
            explanation = explanation
        )
    }

    GlobalHeaderSection.ALTCOIN_SEASON -> {
        val badge = if (data.hasBtcDominance) "${data.btcDominanceScore}/100" else "--"
        val explanation = if (!data.hasBtcDominance) {
            if (isEnglish) {
                "Compares Bitcoin's share of total crypto market capitalization against all altcoins combined."
            } else {
                "Compara a participação do Bitcoin na capitalização total do mercado cripto em relação às demais criptomoedas."
            }
        } else if (isEnglish) {
            val context = when {
                data.btcDominanceScore >= 55 ->
                    "Since BTC concentrates the larger share of capital, market momentum currently favors Bitcoin over altcoins."
                data.btcDominanceScore <= 45 ->
                    "Lower BTC dominance signals capital rotating into altcoins, characterizing an Altcoin Season."
                else ->
                    "Capital distribution between Bitcoin and altcoins is currently balanced."
            }
            "Compares Bitcoin's market dominance (${data.btcDominanceScore}%) against the rest of the market (${data.altcoinsDominanceScore}%). $context"
        } else {
            val context = when {
                data.btcDominanceScore >= 55 ->
                    "Como o BTC concentra a maior fatia do capital, o momento favorece o Bitcoin frente às altcoins."
                data.btcDominanceScore <= 45 ->
                    "A menor dominância do BTC indica rotação de capital para altcoins, caracterizando uma Altcoin Season."
                else ->
                    "A distribuição de capital entre Bitcoin e altcoins está equilibrada no momento."
            }
            "Compara a dominância do Bitcoin (${data.btcDominanceScore}%) contra o restante do mercado (${data.altcoinsDominanceScore}%). $context"
        }
        HeaderBalloonContent(
            title = "Altcoin Season",
            badgeText = badge,
            badgeTone = BalloonBadgeTone.ALTCOIN_SEASON,
            splitHighlight = BalloonSplitHighlight(
                btcScore = data.btcDominanceScore,
                altsScore = data.altcoinsDominanceScore,
                hasValue = data.hasBtcDominance
            ),
            explanation = explanation
        )
    }

    GlobalHeaderSection.MARKET_CAP -> {
        val title = if (isEnglish) "Global Market Cap" else "Capitalização Global"
        val badge = if (isEnglish) {
            "${data.totalMarketCap} (${data.marketCapChangePercentage} 24h)"
        } else {
            "${data.totalMarketCap} (${data.marketCapChangePercentage} 24h)"
        }
        val explanation = if (isEnglish) {
            "Combined market value of all cryptocurrencies in circulation (${data.totalMarketCap}). The ${data.marketCapChangePercentage} 24h change reflects net capital inflows or outflows across the sector."
        } else {
            "Soma do valor de mercado de todas as criptomoedas em circulação (${data.totalMarketCap}). A variação de ${data.marketCapChangePercentage} nas últimas 24h indica entrada ou saída líquida de capital no setor."
        }
        HeaderBalloonContent(
            title = title,
            badgeText = badge,
            badgeTone = BalloonBadgeTone.TREND,
            explanation = explanation
        )
    }

    GlobalHeaderSection.VOLUME -> {
        val title = if (isEnglish) "24h Global Volume" else "Volume Global em 24h"
        val explanation = if (isEnglish) {
            "Total value traded across global cryptocurrency markets over the last 24 hours (${data.totalVolume24h}). It measures market liquidity and trading intensity."
        } else {
            "Total negociado em todas as criptomoedas nas últimas 24 horas (${data.totalVolume24h}). Reflete o nível de liquidez e a intensidade de compra e venda no mercado."
        }
        HeaderBalloonContent(
            title = title,
            badgeText = data.totalVolume24h,
            badgeTone = BalloonBadgeTone.VOLUME,
            explanation = explanation
        )
    }
}

private fun localizedFearGreedLabel(
    sentiment: FearAndGreedSentiment,
    isEnglish: Boolean
): String = if (isEnglish) {
    when (sentiment) {
        FearAndGreedSentiment.EXTREME_FEAR -> "Extreme Fear"
        FearAndGreedSentiment.FEAR -> "Fear"
        FearAndGreedSentiment.NEUTRAL -> "Neutral"
        FearAndGreedSentiment.GREED -> "Greed"
        FearAndGreedSentiment.EXTREME_GREED -> "Extreme Greed"
    }
} else {
    when (sentiment) {
        FearAndGreedSentiment.EXTREME_FEAR -> "Medo Extremo"
        FearAndGreedSentiment.FEAR -> "Medo"
        FearAndGreedSentiment.NEUTRAL -> "Neutro"
        FearAndGreedSentiment.GREED -> "Ganância"
        FearAndGreedSentiment.EXTREME_GREED -> "Ganância Extrema"
    }
}

private fun fearGreedExplanation(
    sentiment: FearAndGreedSentiment,
    value: Int,
    isEnglish: Boolean
): String = if (isEnglish) {
    when (sentiment) {
        FearAndGreedSentiment.EXTREME_FEAR ->
            "The index is in Extreme Fear ($value/100) due to strong selling pressure, elevated volatility, and cautious investor sentiment. Historically, it can point to oversold conditions."
        FearAndGreedSentiment.FEAR ->
            "The index reads Fear ($value/100), reflecting investor caution and lower risk appetite following recent negative price swings."
        FearAndGreedSentiment.NEUTRAL ->
            "The index is Neutral ($value/100), showing balanced buying and selling pressure without dominant euphoria or panic."
        FearAndGreedSentiment.GREED ->
            "The index signals Greed ($value/100), driven by strong buying inflows, upward momentum, and rising investor optimism."
        FearAndGreedSentiment.EXTREME_GREED ->
            "The index reached Extreme Greed ($value/100) amid high euphoria, intense buying volume, and rapid rallies. This can indicate an overheated market."
    }
} else {
    when (sentiment) {
        FearAndGreedSentiment.EXTREME_FEAR ->
            "O índice está em Medo Extremo ($value/100) devido à forte pressão vendedora, alta volatilidade e cautela dos investidores. Historicamente, pode sinalizar ativos sobrevendidos."
        FearAndGreedSentiment.FEAR ->
            "O índice marca Medo ($value/100), refletindo postura defensiva e menor apetite por risco após quedas ou oscilações recentes."
        FearAndGreedSentiment.NEUTRAL ->
            "O índice está Neutro ($value/100), mostrando equilíbrio entre força compradora e vendedora, sem euforia ou pânico predominantes."
        FearAndGreedSentiment.GREED ->
            "O índice aponta Ganância ($value/100), impulsionado por fluxo comprador consistente, impulso de alta e otimismo crescente."
        FearAndGreedSentiment.EXTREME_GREED ->
            "O índice atingiu Ganância Extrema ($value/100) com forte euforia, alto volume comprador e valorização acelerada. Pode indicar mercado superaquecido."
    }
}

@Composable
internal fun GlobalMarketInfoBalloon(
    section: GlobalHeaderSection,
    data: GlobalMarketVO,
    isEnglish: Boolean,
    headerWidthPx: Int,
    headerHeightPx: Int,
    anchorCenterXPx: Float,
    onDismiss: () -> Unit
) {
    if (headerWidthPx <= 0 || headerHeightPx <= 0) return

    val density = LocalDensity.current
    val isDark = isSystemInDarkTheme()
    val content = remember(section, data, isEnglish) {
        buildHeaderBalloonContent(section, data, isEnglish)
    }

    val balloonWidthDp = with(density) {
        (headerWidthPx.toDp() - 16.dp).coerceIn(220.dp, 316.dp)
    }
    val balloonWidthPx = with(density) { balloonWidthDp.roundToPx() }
    val marginPx = with(density) { 8.dp.roundToPx() }
    val maxLeftPx = (headerWidthPx - balloonWidthPx - marginPx).coerceAtLeast(marginPx)
    val popupLeftPx = (anchorCenterXPx - balloonWidthPx / 2f).roundToInt().coerceIn(marginPx, maxLeftPx)
    val caretCenterInPopupPx = (anchorCenterXPx - popupLeftPx)
        .coerceIn(28f, (balloonWidthPx - 28).toFloat().coerceAtLeast(28f))

    val cornerRadiusPx = with(density) { 14.dp.toPx() }
    val caretWidthPx = with(density) { 16.dp.toPx() }
    val caretHeightDp = 8.dp
    val caretHeightPx = with(density) { caretHeightDp.toPx() }
    val yOffsetPx = headerHeightPx + with(density) { 2.dp.roundToPx() }

    val shape = remember(balloonWidthPx, caretCenterInPopupPx) {
        GenericShape { size, _ ->
            val w = size.width
            val h = size.height
            val top = caretHeightPx
            val r = cornerRadiusPx.coerceAtMost((w / 2f).coerceAtMost((h - top) / 2f))
            val halfCaret = caretWidthPx / 2f
            val clampedCenter = caretCenterInPopupPx.coerceIn(
                r + halfCaret,
                (w - r - halfCaret).coerceAtLeast(r + halfCaret)
            )

            moveTo(r, top)
            lineTo(clampedCenter - halfCaret, top)
            lineTo(clampedCenter, 0f)
            lineTo(clampedCenter + halfCaret, top)
            lineTo(w - r, top)
            quadraticTo(w, top, w, top + r)
            lineTo(w, h - r)
            quadraticTo(w, h, w - r, h)
            lineTo(r, h)
            quadraticTo(0f, h, 0f, h - r)
            lineTo(0f, top + r)
            quadraticTo(0f, top, r, top)
            close()
        }
    }

    val btcColor = if (isDark) Color(0xFFFFB020) else Color(0xFFE68400)
    val altsColor = MaterialTheme.colorScheme.scrim
    val badgeColor = when (content.badgeTone) {
        BalloonBadgeTone.SENTIMENT -> if (data.hasFearAndGreed) {
            sentimentColor(data.fearAndGreedSentiment, isDark)
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        }
        BalloonBadgeTone.ALTCOIN_SEASON -> btcColor
        BalloonBadgeTone.TREND -> data.trendColor.textColor
        BalloonBadgeTone.VOLUME -> MaterialTheme.colorScheme.scrim
    }

    Popup(
        alignment = Alignment.TopStart,
        offset = IntOffset(popupLeftPx, yOffsetPx),
        onDismissRequest = onDismiss,
        properties = PopupProperties(
            focusable = true,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Surface(
            modifier = Modifier
                .width(balloonWidthDp)
                .testTag("global_market_balloon"),
            shape = shape,
            color = MaterialTheme.colorScheme.primary,
            shadowElevation = 10.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = 14.dp,
                        end = 10.dp,
                        top = caretHeightDp + 12.dp,
                        bottom = 14.dp
                    )
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Text(
                            text = content.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.testTag("global_market_balloon_title")
                        )

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(badgeColor.copy(alpha = 0.14f))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = content.badgeText,
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = badgeColor,
                                modifier = Modifier.testTag("global_market_balloon_badge")
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .clickable(
                                role = Role.Button,
                                onClickLabel = if (isEnglish) "Close explanation" else "Fechar explicação",
                                onClick = onDismiss
                            )
                            .testTag("global_market_balloon_close"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = if (isEnglish) "Close" else "Fechar",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                content.splitHighlight?.let { split ->
                    if (split.hasValue) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = buildAnnotatedString {
                                withStyle(SpanStyle(color = btcColor, fontWeight = FontWeight.Bold)) {
                                    append("₿ ${split.btcScore}%")
                                }
                                withStyle(SpanStyle(color = MaterialTheme.colorScheme.onSurfaceVariant)) {
                                    append("  /  ")
                                }
                                withStyle(SpanStyle(color = altsColor, fontWeight = FontWeight.Bold)) {
                                    append("Altcoins ${split.altsScore}%")
                                }
                            },
                            fontSize = 12.sp,
                            modifier = Modifier.testTag("global_market_balloon_split")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = content.explanation,
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag("global_market_balloon_description")
                )
            }
        }
    }
}
