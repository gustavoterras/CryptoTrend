package br.com.terras.app.dsm.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.terras.app.cryptotrend.core.dsm.R
import br.com.terras.app.dsm.ui.annotation.ThemePreviews
import br.com.terras.app.dsm.ui.theme.DSMTheme

enum class MarketSortColumn {
    NAME,
    VOLUME,
    GRAPH,
    CHANGE_24H
}

enum class SortDirection {
    ASC,
    DESC
}

data class MarketSortState(
    val column: MarketSortColumn? = null,
    val direction: SortDirection = SortDirection.DESC
) {
    fun toggle(target: MarketSortColumn): MarketSortState {
        val defaultDirection = when (target) {
            MarketSortColumn.NAME -> SortDirection.ASC
            MarketSortColumn.VOLUME,
            MarketSortColumn.GRAPH,
            MarketSortColumn.CHANGE_24H -> SortDirection.DESC
        }
        return when {
            column != target -> MarketSortState(column = target, direction = defaultDirection)
            direction == defaultDirection -> MarketSortState(
                column = target,
                direction = if (defaultDirection == SortDirection.ASC) SortDirection.DESC else SortDirection.ASC
            )
            else -> MarketSortState(column = null, direction = SortDirection.DESC)
        }
    }
}

fun <T> List<T>.sortWithMarketState(
    sortState: MarketSortState,
    nameSelector: (T) -> String,
    volumeSelector: (T) -> Double,
    priceSelector: (T) -> Double,
    change24hSelector: (T) -> Double
): List<T> {
    val activeColumn = sortState.column ?: return this
    val comparator = when (activeColumn) {
        MarketSortColumn.NAME -> compareBy<T> { nameSelector(it).lowercase() }
        MarketSortColumn.VOLUME -> compareBy<T> { volumeSelector(it) }
        MarketSortColumn.GRAPH -> compareBy<T> { priceSelector(it) }
        MarketSortColumn.CHANGE_24H -> compareBy<T> { change24hSelector(it) }
    }
    return when (sortState.direction) {
        SortDirection.ASC -> sortedWith(comparator)
        SortDirection.DESC -> sortedWith(comparator.reversed())
    }
}

@Composable
fun MarketSortBar(
    sortState: MarketSortState,
    onSortClick: (MarketSortColumn) -> Unit,
    modifier: Modifier = Modifier,
    hasFavoriteColumn: Boolean = false
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = if (hasFavoriteColumn) 16.dp else 18.dp,
                end = if (hasFavoriteColumn) 6.dp else 18.dp,
                top = 6.dp,
                bottom = 6.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1.45f),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SortHeaderItem(
                label = stringResource(R.string.sort_name),
                column = MarketSortColumn.NAME,
                sortState = sortState,
                onClick = { onSortClick(MarketSortColumn.NAME) },
                modifier = Modifier.testTag("sort_header_name")
            )
            SortHeaderItem(
                label = stringResource(R.string.sort_vol),
                column = MarketSortColumn.VOLUME,
                sortState = sortState,
                onClick = { onSortClick(MarketSortColumn.VOLUME) },
                modifier = Modifier.testTag("sort_header_volume")
            )
        }

        Box(
            modifier = Modifier.width(if (hasFavoriteColumn) 64.dp else 74.dp),
            contentAlignment = Alignment.Center
        ) {
            SortHeaderItem(
                label = stringResource(R.string.sort_chart),
                column = MarketSortColumn.GRAPH,
                sortState = sortState,
                onClick = { onSortClick(MarketSortColumn.GRAPH) },
                modifier = Modifier.testTag("sort_header_graph")
            )
        }

        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SortHeaderItem(
                label = stringResource(R.string.sort_change_24h),
                column = MarketSortColumn.CHANGE_24H,
                sortState = sortState,
                onClick = { onSortClick(MarketSortColumn.CHANGE_24H) },
                modifier = Modifier.testTag("sort_header_change")
            )
        }

        if (hasFavoriteColumn) {
            Spacer(modifier = Modifier.width(48.dp))
        }
    }
}

@Composable
private fun SortHeaderItem(
    label: String,
    column: MarketSortColumn,
    sortState: MarketSortState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isActive = sortState.column == column
    val isAscending = isActive && sortState.direction == SortDirection.ASC
    val contentColor = if (isActive) {
        MaterialTheme.colorScheme.scrim
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Row(
        modifier = modifier
            .heightIn(min = 36.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(1.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontSize = 12.sp,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Icon(
            imageVector = Icons.Filled.ArrowDropDown,
            contentDescription = if (isActive) {
                if (isAscending) {
                    stringResource(R.string.sort_asc_cd, label)
                } else {
                    stringResource(R.string.sort_desc_cd, label)
                }
            } else {
                stringResource(R.string.sort_by_cd, label)
            },
            tint = contentColor,
            modifier = Modifier
                .size(16.dp)
                .graphicsLayer {
                    rotationZ = if (isAscending) 180f else 0f
                }
        )
    }
}

@ThemePreviews
@Composable
private fun MarketSortBarPreview() {
    DSMTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            MarketSortBar(
                sortState = MarketSortState(MarketSortColumn.VOLUME, SortDirection.DESC),
                onSortClick = {}
            )
        }
    }
}
