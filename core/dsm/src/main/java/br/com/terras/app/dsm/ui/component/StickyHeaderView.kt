package br.com.terras.app.dsm.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import br.com.terras.app.cryptotrend.core.dsm.R
import br.com.terras.app.dsm.ui.annotation.ThemePreviews
import br.com.terras.app.dsm.ui.theme.DSMTheme

@Composable
fun StickyHeaderView() {
    ConstraintLayout(modifier = Modifier.padding(start = 18.dp, end = 18.dp)) {
        val (title, avatar, menu) = createRefs()

        Box(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.primary, CircleShape)
                .height(42.dp)
                .width(42.dp)
                .constrainAs(avatar) {
                    centerVerticallyTo(parent)
                    start.linkTo(parent.start)
                }
        ) {
            Icon(
                painterResource(id = R.drawable.person_24dp),
                contentDescription = "avatar",
                modifier = Modifier.align(Alignment.Center)
            )
        }

        Text(
            text = "CryptoTrend",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .wrapContentSize()
                .constrainAs(title) {
                    centerVerticallyTo(parent)
                    centerHorizontallyTo(parent)
                },
            textAlign = TextAlign.Center
        )

        Box(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.primary, CircleShape)
                .height(42.dp)
                .width(42.dp)
                .wrapContentSize()
                .constrainAs(menu) {
                    centerVerticallyTo(parent)
                    end.linkTo(parent.end)
                }
        ) {
            Icon(
                painterResource(id = R.drawable.settings_24),
                contentDescription = "menu",
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}

@ThemePreviews
@Composable
private fun PersonListItemPreview() {
    DSMTheme {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp),
            color = MaterialTheme.colorScheme.background
        ) {
            StickyHeaderView()
        }
    }
}