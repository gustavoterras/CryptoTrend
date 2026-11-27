package br.com.terras.app.dsm.ui.component

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
fun TypewriterSearchPlaceholder(
    text: String,
    isSearchOpen: Boolean,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    cursorColor: Color = MaterialTheme.colorScheme.scrim,
    style: TextStyle = LocalTextStyle.current,
    charDelayMillis: Long = 38L,
    initialDelayMillis: Long = 180L,
    showCursor: Boolean = true,
    testTag: String = "typewriter_search_placeholder"
) {
    if (text.isEmpty()) return

    var charIndex by remember(isSearchOpen, text) { mutableIntStateOf(0) }
    var cursorVisible by remember(isSearchOpen, text) { mutableStateOf(false) }

    LaunchedEffect(isSearchOpen, text) {
        if (!isSearchOpen) {
            charIndex = 0
            cursorVisible = false
            return@LaunchedEffect
        }
        charIndex = 0
        cursorVisible = true
        delay(initialDelayMillis)
        for (i in 1..text.length) {
            charIndex = i
            delay(charDelayMillis)
        }
        if (showCursor) {
            repeat(4) {
                cursorVisible = !cursorVisible
                delay(260L)
            }
            cursorVisible = false
        }
    }

    val visibleText = if (charIndex >= text.length) text else text.take(charIndex)

    Row(
        modifier = modifier
            .testTag(testTag)
            .semantics { contentDescription = text },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = visibleText,
            color = color,
            style = style,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (cursorVisible && showCursor) {
            Text(
                text = "|",
                color = cursorColor,
                style = style,
                fontWeight = FontWeight.Normal,
                modifier = Modifier.padding(start = 1.dp)
            )
        }
    }
}
