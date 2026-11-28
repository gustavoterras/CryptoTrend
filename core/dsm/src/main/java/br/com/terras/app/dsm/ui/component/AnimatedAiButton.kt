package br.com.terras.app.dsm.ui.component

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.terras.app.cryptotrend.core.dsm.R

private val AI_PALETTE = listOf(
    Color(0xFFA855F7), // Roxo (Vibrant Purple)
    Color(0xFFFF4081), // Rosa (Neon Pink)
    Color(0xFF00B0FF), // Azul (Vivid Sky Blue)
    Color(0xFF00E676), // Verde (Neon Emerald Green)
    Color(0xFFA855F7)  // Loop de volta suave para o Roxo
)

@Composable
fun AnimatedAiButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String = "animated_ai_button"
) {
    val infiniteTransition = rememberInfiniteTransition(label = "ai_button_anim")

    val scale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    // Ciclo contínuo de 0f a 4f para transição suave entre Roxo -> Rosa -> Azul -> Verde -> Roxo
    val colorProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "color_progress"
    )

    val segmentIndex = colorProgress.toInt().coerceIn(0, 3)
    val segmentFraction = (colorProgress - segmentIndex).coerceIn(0f, 1f)
    // Suaviza a transição entre cada cor
    val smoothFraction = FastOutSlowInEasing.transform(segmentFraction)
    val animatedColor = lerp(AI_PALETTE[segmentIndex], AI_PALETTE[segmentIndex + 1], smoothFraction)

    Row(
        modifier = modifier
            .heightIn(min = 36.dp)
            .clip(RoundedCornerShape(999.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .semantics { role = Role.Button }
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_ai_sparkle_24dp),
            contentDescription = null,
            tint = animatedColor,
            modifier = Modifier
                .size(15.dp)
                .scale(scale)
        )
        Text(
            text = "IA",
            style = androidx.compose.ui.text.TextStyle(
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = animatedColor
            ),
            modifier = Modifier.scale(scale)
        )
    }
}
