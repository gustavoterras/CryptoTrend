package br.com.terras.app.coins.list.presentation

import br.com.terras.app.coins.list.domain.model.FearAndGreedSentiment
import br.com.terras.app.coins.list.domain.model.GlobalMarketVO
import br.com.terras.app.dsm.ui.component.TrendColor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GlobalMarketBalloonContentTest {

    private val sampleMarket = GlobalMarketVO(
        fearAndGreedValue = 22,
        fearAndGreedSentiment = FearAndGreedSentiment.EXTREME_FEAR,
        fearAndGreedLabel = "Medo Extremo",
        hasFearAndGreed = true,
        totalMarketCap = "R$ 16,42 tri",
        marketCapChangePercentage = "+2,4%",
        trendColor = TrendColor.UP,
        totalVolume24h = "R$ 612,80 bi",
        btcDominance = "58,0%",
        ethDominance = "16,5%",
        activeCryptocurrencies = "15.420",
        marketsCount = "1.120",
        btcDominancePercent = 58.0,
        btcDominanceScore = 58,
        altcoinsDominanceScore = 42,
        hasBtcDominance = true
    )

    @Test
    fun `fear and greed balloon includes extreme fear sentiment and explanation in pt and en`() {
        val pt = buildHeaderBalloonContent(GlobalHeaderSection.FEAR_GREED, sampleMarket, isEnglish = false)
        assertEquals("Medo & Ganância", pt.title)
        assertEquals("Medo Extremo · 22/100", pt.badgeText)
        assertTrue(pt.explanation.contains("Medo Extremo"))
        assertTrue(pt.explanation.contains("22/100"))

        val en = buildHeaderBalloonContent(GlobalHeaderSection.FEAR_GREED, sampleMarket, isEnglish = true)
        assertEquals("Fear & Greed", en.title)
        assertEquals("Extreme Fear · 22/100", en.badgeText)
        assertTrue(en.explanation.contains("Extreme Fear"))
        assertTrue(en.explanation.contains("22/100"))
    }

    @Test
    fun `altcoin season balloon includes btc vs alts split and explanation in pt and en`() {
        val pt = buildHeaderBalloonContent(GlobalHeaderSection.ALTCOIN_SEASON, sampleMarket, isEnglish = false)
        assertEquals("Altcoin Season", pt.title)
        assertEquals("58/100", pt.badgeText)
        assertNotNull(pt.splitHighlight)
        assertEquals(58, pt.splitHighlight?.btcScore)
        assertEquals(42, pt.splitHighlight?.altsScore)
        assertTrue(pt.explanation.contains("58%"))
        assertTrue(pt.explanation.contains("42%"))

        val altcoinFavored = sampleMarket.copy(btcDominanceScore = 40, altcoinsDominanceScore = 60)
        val en = buildHeaderBalloonContent(GlobalHeaderSection.ALTCOIN_SEASON, altcoinFavored, isEnglish = true)
        assertEquals("40/100", en.badgeText)
        assertTrue(en.explanation.contains("40%"))
        assertTrue(en.explanation.contains("60%"))
        assertTrue(en.explanation.contains("Altcoin Season"))
    }

    @Test
    fun `market cap balloon includes total cap, 24h change, and explanation in pt and en`() {
        val pt = buildHeaderBalloonContent(GlobalHeaderSection.MARKET_CAP, sampleMarket, isEnglish = false)
        assertEquals("Capitalização Global", pt.title)
        assertTrue(pt.badgeText.contains("R$ 16,42 tri"))
        assertTrue(pt.badgeText.contains("+2,4%"))
        assertNull(pt.splitHighlight)
        assertTrue(pt.explanation.contains("R$ 16,42 tri"))
        assertTrue(pt.explanation.contains("+2,4%"))

        val enData = sampleMarket.copy(totalMarketCap = "$3.12T", marketCapChangePercentage = "+2.4%")
        val en = buildHeaderBalloonContent(GlobalHeaderSection.MARKET_CAP, enData, isEnglish = true)
        assertEquals("Global Market Cap", en.title)
        assertTrue(en.explanation.contains("$3.12T"))
        assertTrue(en.explanation.contains("+2.4%"))
    }

    @Test
    fun `volume balloon includes 24h volume and explanation in pt and en`() {
        val pt = buildHeaderBalloonContent(GlobalHeaderSection.VOLUME, sampleMarket, isEnglish = false)
        assertEquals("Volume Global em 24h", pt.title)
        assertEquals("R$ 612,80 bi", pt.badgeText)
        assertTrue(pt.explanation.contains("R$ 612,80 bi"))

        val enData = sampleMarket.copy(totalVolume24h = "$112.50B")
        val en = buildHeaderBalloonContent(GlobalHeaderSection.VOLUME, enData, isEnglish = true)
        assertEquals("24h Global Volume", en.title)
        assertEquals("$112.50B", en.badgeText)
        assertTrue(en.explanation.contains("$112.50B"))
    }
}
