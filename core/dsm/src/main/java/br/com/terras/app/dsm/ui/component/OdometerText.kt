package br.com.terras.app.dsm.ui.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import kotlin.math.abs
import kotlin.math.floor

private val OdometerDrumEasing = CubicBezierEasing(0.22f, 1.0f, 0.36f, 1.0f)

@Composable
fun OdometerText(
    text: String,
    modifier: Modifier = Modifier,
    rawValue: Double? = null,
    animateChanges: Boolean = true,
    style: TextStyle = MaterialTheme.typography.bodyLarge,
    color: Color = MaterialTheme.colorScheme.onSurface,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontWeight: FontWeight? = null,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Ellipsis
) {
    val effectiveStyle = style.copy(
        fontFeatureSettings = "tnum",
        fontSize = if (fontSize != TextUnit.Unspecified) fontSize else style.fontSize,
        fontWeight = fontWeight ?: style.fontWeight
    )

    if (!animateChanges || text.none { it.isDigit() }) {
        Text(
            text = text,
            style = effectiveStyle,
            color = color,
            maxLines = 1,
            softWrap = false,
            overflow = overflow,
            textAlign = textAlign ?: TextAlign.Start,
            modifier = modifier
        )
        return
    }

    var previousRawValue by remember { mutableStateOf(rawValue) }
    var previousText by remember { mutableStateOf(text) }
    var lastDirectionUp by remember { mutableStateOf(true) }

    val rollUp = remember(text, rawValue) {
        when {
            rawValue != null && rawValue.isFinite() &&
                previousRawValue != null && previousRawValue!!.isFinite() &&
                rawValue != previousRawValue -> rawValue > previousRawValue!!
            text != previousText -> {
                val prevNum = parseNumericFallback(previousText)
                val currNum = parseNumericFallback(text)
                if (prevNum != null && currNum != null && currNum != prevNum) {
                    currNum > prevNum
                } else {
                    lastDirectionUp
                }
            }
            else -> lastDirectionUp
        }
    }

    SideEffect {
        previousRawValue = rawValue
        previousText = text
        lastDirectionUp = rollUp
    }

    val firstDigitIndex = text.indexOfFirst { it.isDigit() }
    val prefix = if (firstDigitIndex > 0) text.substring(0, firstDigitIndex) else ""
    val numericBody = if (firstDigitIndex >= 0) text.substring(firstDigitIndex) else text

    Row(
        modifier = modifier.clearAndSetSemantics { contentDescription = text },
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (prefix.isNotEmpty()) {
            key("odometer_prefix") {
                OdometerPrefixSlot(
                    prefix = prefix,
                    rollUp = rollUp,
                    textStyle = effectiveStyle,
                    color = color
                )
            }
        }

        numericBody.forEachIndexed { index, ch ->
            val slotFromRight = numericBody.length - 1 - index
            key("odometer_slot_$slotFromRight") {
                if (ch.isDigit()) {
                    OdometerDigitDrum(
                        digit = ch - '0',
                        rollUp = rollUp,
                        textStyle = effectiveStyle,
                        color = color
                    )
                } else {
                    OdometerSymbolSlot(
                        symbol = ch,
                        rollUp = rollUp,
                        textStyle = effectiveStyle,
                        color = color
                    )
                }
            }
        }
    }
}

@Composable
private fun OdometerDigitDrum(
    digit: Int,
    rollUp: Boolean,
    textStyle: TextStyle,
    color: Color
) {
    var lastDigit by remember { mutableIntStateOf(digit) }
    var targetSteps by remember { mutableIntStateOf(digit) }
    var lastDeltaMagnitude by remember { mutableIntStateOf(1) }
    val latestRollUp by rememberUpdatedState(rollUp)

    LaunchedEffect(digit) {
        if (digit != lastDigit) {
            val delta = if (latestRollUp) {
                (digit - lastDigit + 10) % 10
            } else {
                -((lastDigit - digit + 10) % 10)
            }
            lastDeltaMagnitude = abs(delta).coerceAtLeast(1)
            targetSteps += delta
            lastDigit = digit
        }
    }

    val durationMillis = (360 + lastDeltaMagnitude * 32).coerceIn(360, 620)
    val animatedStepState = animateFloatAsState(
        targetValue = targetSteps.toFloat(),
        animationSpec = tween(
            durationMillis = durationMillis,
            easing = OdometerDrumEasing
        ),
        label = "odometerDigitDrum"
    )

    val baseStep by remember {
        derivedStateOf { floor(animatedStepState.value).toInt() }
    }
    val isRolling by remember {
        derivedStateOf {
            abs(animatedStepState.value - targetSteps.toFloat()) > 0.001f ||
                (animatedStepState.value - floor(animatedStepState.value)) > 0.001f
        }
    }

    val currentDigit = ((baseStep % 10) + 10) % 10
    val nextDigit = (((baseStep + 1) % 10) + 10) % 10

    Box(
        modifier = Modifier.clipToBounds(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "0",
            style = textStyle,
            color = Color.Transparent,
            maxLines = 1,
            softWrap = false
        )

        Text(
            text = currentDigit.toString(),
            style = textStyle,
            color = color,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.graphicsLayer {
                val fraction = animatedStepState.value - floor(animatedStepState.value)
                translationY = -fraction * size.height
                alpha = 1f - 0.35f * fraction
                scaleY = 1f - 0.14f * fraction
            }
        )

        if (isRolling) {
            Text(
                text = nextDigit.toString(),
                style = textStyle,
                color = color,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier.graphicsLayer {
                    val fraction = animatedStepState.value - floor(animatedStepState.value)
                    val remaining = 1f - fraction
                    translationY = remaining * size.height
                    alpha = 1f - 0.35f * remaining
                    scaleY = 1f - 0.14f * remaining
                }
            )
        }
    }
}

@Composable
private fun OdometerPrefixSlot(
    prefix: String,
    rollUp: Boolean,
    textStyle: TextStyle,
    color: Color
) {
    AnimatedContent(
        targetState = prefix,
        transitionSpec = {
            val enterOffset: (Int) -> Int = { fullHeight -> if (rollUp) fullHeight else -fullHeight }
            val exitOffset: (Int) -> Int = { fullHeight -> if (rollUp) -fullHeight else fullHeight }
            (slideInVertically(tween(260, easing = OdometerDrumEasing), enterOffset) + fadeIn(tween(180)))
                .togetherWith(
                    slideOutVertically(tween(260, easing = OdometerDrumEasing), exitOffset) + fadeOut(tween(150))
                )
                .using(SizeTransform(clip = true))
        },
        label = "odometerPrefix"
    ) { value ->
        Text(
            text = value,
            style = textStyle,
            color = color,
            maxLines = 1,
            softWrap = false
        )
    }
}

@Composable
private fun OdometerSymbolSlot(
    symbol: Char,
    rollUp: Boolean,
    textStyle: TextStyle,
    color: Color
) {
    AnimatedContent(
        targetState = symbol,
        modifier = Modifier.clipToBounds(),
        transitionSpec = {
            val enterOffset: (Int) -> Int = { fullHeight -> if (rollUp) fullHeight else -fullHeight }
            val exitOffset: (Int) -> Int = { fullHeight -> if (rollUp) -fullHeight else fullHeight }
            (slideInVertically(tween(280, easing = OdometerDrumEasing), enterOffset) + fadeIn(tween(180)))
                .togetherWith(
                    slideOutVertically(tween(280, easing = OdometerDrumEasing), exitOffset) + fadeOut(tween(150))
                )
                .using(SizeTransform(clip = true))
        },
        label = "odometerSymbol"
    ) { ch ->
        Text(
            text = ch.toString(),
            style = textStyle,
            color = color,
            maxLines = 1,
            softWrap = false
        )
    }
}

private fun parseNumericFallback(text: String): Double? {
    val cleaned = buildString {
        text.forEach { ch ->
            when {
                ch.isDigit() || ch == '-' || ch == '.' -> append(ch)
                ch == ',' -> append('.')
            }
        }
    }
    return cleaned.toDoubleOrNull()
}
