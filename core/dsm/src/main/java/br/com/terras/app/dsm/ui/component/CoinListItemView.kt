package br.com.terras.app.dsm.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Visibility.Companion.Gone
import androidx.constraintlayout.compose.Visibility.Companion.Invisible
import androidx.constraintlayout.compose.Visibility.Companion.Visible
import br.com.terras.app.dsm.ui.annotation.ThemePreviews
import br.com.terras.app.dsm.ui.theme.DSMTheme
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun CoinListItemView(
    coinId: String,
    coinLogo: String,
    coinName: String,
    coinSymbol: String,
    coinPosition: String? = null,
    coinPrice: String? = null,
    coinPriceChancePercentage: String? = null,
    trendColor: TrendColor = TrendColor.NONE,
    onItemClick: (String) -> Unit
) {
    ConstraintLayout(
        modifier = Modifier
            .clickable { onItemClick(coinId) }
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(18.dp))
    ) {
        val (avatar, position, nick, name, price, percentage) = createRefs()

        val guideCenterVertically = createGuidelineFromTop(0.5f)

        GlideImage(model = coinLogo,
            contentDescription = "Avatar",
            Modifier
                .width(35.dp)
                .height(35.dp)
                .constrainAs(avatar) {
                    top.linkTo(parent.top, 18.dp)
                    bottom.linkTo(parent.bottom, 18.dp)
                    start.linkTo(parent.start, 18.dp)
                }
        )

        Text(text = coinName,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.constrainAs(nick) {
                start.linkTo(avatar.end, 9.dp)
                bottom.linkTo(guideCenterVertically)
            }
        )

        Box(modifier = Modifier
            .background(MaterialTheme.colorScheme.secondary, RoundedCornerShape(3.dp))
            .padding(start = 3.dp, end = 3.dp)
            .constrainAs(position) {
                top.linkTo(guideCenterVertically)
                start.linkTo(avatar.end, 9.dp)
                visibility = if (coinPosition.isNullOrEmpty()) Invisible else Visible
            }) {
            Text(
                text = coinPosition.orEmpty(),
                style = MaterialTheme.typography.labelLarge,
                color = Color.White
            )
        }

        val spacingNull = if (coinPosition.isNullOrEmpty()) (-5).dp else 3.dp

        Text(text = coinSymbol,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.constrainAs(name) {
                centerVerticallyTo(position)
                start.linkTo(position.end, spacingNull)
            }
        )

        Text(text = coinPrice.orEmpty(),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.surface,
            textAlign = TextAlign.Center,
            modifier = Modifier.constrainAs(price) {
                end.linkTo(parent.end, 18.dp)
                bottom.linkTo(guideCenterVertically)
                visibility = if (coinPrice.isNullOrEmpty()) Gone else Visible
            }
        )


        Box(modifier = Modifier
            .background(trendColor.color, RoundedCornerShape(9.dp))
            .padding(start = 9.dp, end = 9.dp)
            .constrainAs(percentage) {
                end.linkTo(parent.end, 18.dp)
                top.linkTo(guideCenterVertically)
                visibility = if (coinPriceChancePercentage.isNullOrEmpty()) Gone else Visible
            }) {
            Text(
                text = coinPriceChancePercentage.orEmpty(),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.surface,
                textAlign = TextAlign.Center
            )
        }
    }
}

@ThemePreviews
@Composable
private fun PersonListItemPreview() {
    DSMTheme {
        CoinListItemView(
            "id",
            "1",
            "teste",
            "Bitcoin",
            "1",
            "$ 10,000.00",
            "10%",
            TrendColor.DOWN
        ) {}
    }
}