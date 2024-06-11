package br.com.terras.app.dsm.ui.component

import androidx.compose.foundation.background
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import br.com.terras.app.dsm.ui.annotation.ThemePreviews
import br.com.terras.app.dsm.ui.theme.DSMTheme
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun CoinListItemView(
    coinPosition: String,
    coinLogo: String,
    coinName: String,
    coinSymbol: String,
    coinPrice: String,
    coinPriceChancePercentage: String,
    trendColor: TrendColor
) {
    ConstraintLayout(
        modifier = Modifier
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
                })

        Text(text = coinName,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.constrainAs(nick) {
                start.linkTo(avatar.end, 9.dp)
                bottom.linkTo(guideCenterVertically)
            })

        Box(modifier = Modifier
            .background(MaterialTheme.colorScheme.secondary, RoundedCornerShape(3.dp))
            .padding(start = 3.dp, end = 3.dp)
            .constrainAs(position) {
                top.linkTo(guideCenterVertically)
                start.linkTo(avatar.end, 9.dp)
            }) {
            Text(
                text = coinPosition,
                style = MaterialTheme.typography.labelLarge,
                color = Color.White,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Text(text = coinSymbol,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.surface,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.constrainAs(name) {
                centerVerticallyTo(position)
                start.linkTo(position.end, 3.dp)
            })

        Text(text = coinPrice,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.surface,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.constrainAs(price) {
                end.linkTo(parent.end, 18.dp)
                bottom.linkTo(guideCenterVertically)
            })

        Box(modifier = Modifier
            .background(trendColor.color, RoundedCornerShape(9.dp))
            .padding(start = 9.dp, end = 9.dp)
            .constrainAs(percentage) {
                end.linkTo(parent.end, 18.dp)
                top.linkTo(guideCenterVertically)
            }) {
            Text(
                text = coinPriceChancePercentage,
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
            "1",
            "teste",
            "Bitcoin",
            "BTC",
            "$ 10,000.00",
            "10%",
            TrendColor.DOWN
        )
    }
}