package br.com.terras.app.dsm.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import br.com.terras.app.cryptotrend.core.dsm.R

val avenirNextFontFamily = FontFamily(
    Font(R.font.avenir_next_ultra_light, FontWeight.ExtraLight, FontStyle.Normal),
    Font(R.font.avenir_next_light, FontWeight.Light, FontStyle.Normal),
    Font(R.font.avenir_next_regular, FontWeight.Normal, FontStyle.Normal),
    Font(R.font.avenir_next_medium, FontWeight.Medium, FontStyle.Normal),
    Font(R.font.avenir_next_bold, FontWeight.Bold, FontStyle.Normal),
    Font(R.font.avenir_next_italic, FontWeight.Normal, style = FontStyle.Italic)
)

val typography = Typography(
    headlineLarge = TextStyle(
        fontFamily = avenirNextFontFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 28.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = avenirNextFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 26.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = avenirNextFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp
    ),
    titleLarge = TextStyle(
        fontFamily = avenirNextFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 18.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = avenirNextFontFamily,
        fontWeight = FontWeight.ExtraLight,
        fontSize = 16.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = avenirNextFontFamily,
        fontWeight = FontWeight.Light,
        fontSize = 14.sp
    ),
    labelLarge = TextStyle(
        fontFamily = avenirNextFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp
    ),
    labelMedium = TextStyle(
        fontFamily = avenirNextFontFamily,
        fontWeight = FontWeight.Light,
        fontSize = 12.sp
    ),
    labelSmall = TextStyle(
        fontFamily = avenirNextFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 10.sp
    )
)