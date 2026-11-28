package br.com.terras.app.dsm.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.LottieProperty
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.airbnb.lottie.compose.rememberLottieDynamicProperties
import com.airbnb.lottie.compose.rememberLottieDynamicProperty
import br.com.terras.app.cryptotrend.core.dsm.R

@Composable
fun AnimatedLottieButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String = "animated_lottie_button"
) {
    val composition by rememberLottieComposition(
        LottieCompositionSpec.RawRes(R.raw.crypto_refresh_animation)
    )
    val tintColorArgb = MaterialTheme.colorScheme.onTertiary.toArgb()
    val dynamicProperties = rememberLottieDynamicProperties(
        rememberLottieDynamicProperty(
            property = LottieProperty.COLOR,
            value = tintColorArgb,
            keyPath = arrayOf("**")
        )
    )

    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.tertiary,
        modifier = modifier
            .size(40.dp)
            .testTag(testTag)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            LottieAnimation(
                composition = composition,
                iterations = LottieConstants.IterateForever,
                speed = 1.2f,
                dynamicProperties = dynamicProperties,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
