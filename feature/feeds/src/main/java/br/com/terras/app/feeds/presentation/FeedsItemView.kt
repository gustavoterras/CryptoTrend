package br.com.terras.app.feeds.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import br.com.terras.app.dsm.ui.annotation.ThemePreviews
import br.com.terras.app.dsm.ui.theme.DSMTheme
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun FeedsItemView(
    articleTitle: String,
    articleDescription: String,
    articleImage: String,
    articleSeeMore: String,
    navigationClick: () -> Unit
) {
    ConstraintLayout(
        modifier = Modifier
            .clickable { navigationClick() }
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(18.dp)),
    ) {

        val (banner, title, description, seeMore) = createRefs()

        GlideImage(model = articleImage,
            contentDescription = "banner",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
                .constrainAs(banner) {
                    top.linkTo(parent.top)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                })

        Text(text = articleTitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.constrainAs(title) {
                width = Dimension.fillToConstraints
                start.linkTo(parent.start, 18.dp)
                top.linkTo(banner.bottom, 9.dp)
                end.linkTo(parent.end, 9.dp)
            })

        Text(text = articleDescription,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.surface,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.constrainAs(description) {
                width = Dimension.fillToConstraints
                start.linkTo(parent.start, 18.dp)
                top.linkTo(title.bottom, 9.dp)
                end.linkTo(parent.end, 9.dp)
            })

        Text(text = articleSeeMore,
            textDecoration = TextDecoration.Underline,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.surfaceTint,
            textAlign = TextAlign.Center,
            modifier = Modifier.constrainAs(seeMore) {
                end.linkTo(description.end, 9.dp)
                top.linkTo(description.bottom, 9.dp)
                bottom.linkTo(parent.bottom, 18.dp)
            })
    }
}

@ThemePreviews
@Composable
private fun FeedsItemViewPreview() {
    DSMTheme {
        FeedsItemView(
            "Jasmy coin bate recorde – Criptomoeda IA para moldar o mercado de IoT",
            "JASMY é uma criptomoeda que se concentra nas necessidades modernas e de gestão de dados. Além disso, perante o seu roadmap e whitepaper, esta nova criptomoeda integra tecnologia blockchain com “Internet das Coisas” (IoT)....",
            "BTC",
            "Ver mais",
             {}
        )
    }
}