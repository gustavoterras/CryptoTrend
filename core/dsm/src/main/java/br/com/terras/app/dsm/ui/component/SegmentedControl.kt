package br.com.terras.app.dsm.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import br.com.terras.app.dsm.ui.annotation.ThemePreviews
import br.com.terras.app.dsm.ui.theme.DSMTheme

@Composable
fun SegmentedControl(
    items: List<String>,
    defaultSelectedItemIndex: Int = 0,
    useFixedWidth: Boolean = false,
    equalWidth: Boolean = false,
    itemWidth: Dp = 60.dp,
    itemHeight: Dp = 30.dp,
    cornerRadius: Dp = 8.dp,
    itemSpacing: Dp = 6.dp,
    chipHorizontalPadding: Dp = 10.dp,
    chipVerticalPadding: Dp = 7.dp,
    textStyle: TextStyle = MaterialTheme.typography.labelMedium,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.scrim,
    onItemSelection: (selectedItemIndex: Int) -> Unit
) {
    val selectedIndex = remember(defaultSelectedItemIndex) { mutableIntStateOf(defaultSelectedItemIndex) }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(itemSpacing),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items.forEachIndexed { index, item ->
            val isSelected = selectedIndex.intValue == index
            val itemModifier = when {
                useFixedWidth -> Modifier
                    .width(itemWidth)
                    .height(itemHeight)
                equalWidth -> Modifier
                    .weight(1f)
                    .defaultMinSize(minHeight = itemHeight)
                else -> Modifier
                    .defaultMinSize(minWidth = 48.dp, minHeight = itemHeight)
            }

            val selectedTextColor = if (color.luminance() > 0.4f) Color(0xFF052E16) else Color.White

            Box(
                modifier = itemModifier
                    .clip(RoundedCornerShape(cornerRadius))
                    .background(
                        if (isSelected) color else MaterialTheme.colorScheme.background
                    )
                    .clickable {
                        selectedIndex.intValue = index
                        onItemSelection(index)
                    }
                    .padding(horizontal = chipHorizontalPadding, vertical = chipVerticalPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = item,
                    style = textStyle,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) selectedTextColor else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}

@ThemePreviews
@Composable
private fun SegmentedControlPreview() {
    val periods = listOf("1D", "7D", "30D", "90D", "1Y")
    DSMTheme {
        Surface(color = MaterialTheme.colorScheme.primary) {
            SegmentedControl(
                items = periods,
                defaultSelectedItemIndex = 0,
                equalWidth = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {}
        }
    }
}