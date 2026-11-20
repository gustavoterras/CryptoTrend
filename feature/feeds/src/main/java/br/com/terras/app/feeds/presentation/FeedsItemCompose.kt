package br.com.terras.app.feeds.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
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
fun FeedsItemCompose(
    articleTitle: String,
    articleDescription: String,
    articleImage: String,
    articleSeeMore: String,
    navigationClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { navigationClick() }
    ) {
        ConstraintLayout(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 14.dp)
        ) {
            val (banner, title, description, seeMore) = createRefs()

            GlideImage(
                model = articleImage,
                contentDescription = "banner",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(190.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .constrainAs(banner) {
                        top.linkTo(parent.top)
                        start.linkTo(parent.start)
                        end.linkTo(parent.end)
                    }
            )

            Text(
                text = articleTitle,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.constrainAs(title) {
                    width = Dimension.fillToConstraints
                    start.linkTo(parent.start)
                    top.linkTo(banner.bottom, 12.dp)
                    end.linkTo(parent.end)
                }
            )

            Text(
                text = articleDescription,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.constrainAs(description) {
                    width = Dimension.fillToConstraints
                    start.linkTo(parent.start)
                    top.linkTo(title.bottom, 6.dp)
                    end.linkTo(parent.end)
                }
            )

            Text(
                text = articleSeeMore,
                textDecoration = TextDecoration.Underline,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.surfaceTint,
                textAlign = TextAlign.Center,
                modifier = Modifier.constrainAs(seeMore) {
                    end.linkTo(parent.end)
                    top.linkTo(description.bottom, 10.dp)
                    bottom.linkTo(parent.bottom)
                }
            )
        }
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 18.dp),
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant
        )
    }
}

@ThemePreviews
@Composable
private fun FeedsItemComposePreview() {
    DSMTheme {
        FeedsItemCompose(
            "Jasmy coin bate recorde – Criptomoeda IA para moldar o mercado de IoT",
            "JASMY é uma criptomoeda que se concentra nas necessidades modernas e de gestão de dados. Além disso, perante o seu roadmap e whitepaper, esta nova criptomoeda integra tecnologia blockchain com “Internet das Coisas” (IoT)....",
            "BTC",
            "Ver mais",
             {}
        )
    }
}