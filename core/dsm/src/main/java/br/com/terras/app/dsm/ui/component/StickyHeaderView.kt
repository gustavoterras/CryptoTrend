package br.com.terras.app.dsm.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.constraintlayout.compose.Visibility
import br.com.terras.app.cryptotrend.core.dsm.R
import br.com.terras.app.dsm.ui.annotation.ThemePreviews
import br.com.terras.app.dsm.ui.theme.DSMTheme

@Composable
fun StickyHeaderView(
    shouldShowSearchBar: Boolean = false,
    onValueChangeListener: ((String) -> Unit)? = null
) {

    var searchText by rememberSaveable { mutableStateOf("") }

    ConstraintLayout(
        modifier = Modifier
            .background(color = MaterialTheme.colorScheme.background)
    ) {
        val (title, avatar, menu, space, search) = createRefs()

        Box(modifier = Modifier
            .background(MaterialTheme.colorScheme.primary, CircleShape)
            .height(42.dp)
            .width(42.dp)
            .constrainAs(avatar) {
                start.linkTo(parent.start, 18.dp)
                top.linkTo(parent.top, 36.dp)
                bottom.linkTo(space.top)
            }) {
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
                    centerHorizontallyTo(parent)
                    centerVerticallyTo(avatar)
                },
            textAlign = TextAlign.Center
        )

        Box(modifier = Modifier
            .background(MaterialTheme.colorScheme.primary, CircleShape)
            .height(42.dp)
            .width(42.dp)
            .wrapContentSize()
            .constrainAs(menu) {
                end.linkTo(parent.end, 18.dp)
                top.linkTo(parent.top, 36.dp)
            }) {
            Icon(
                painterResource(id = R.drawable.settings_24),
                contentDescription = "menu",
                modifier = Modifier.align(Alignment.Center)
            )
        }

        Spacer(modifier = Modifier
            .fillMaxWidth()
            .height(0.dp)
            .constrainAs(space) {
                top.linkTo(avatar.bottom, 9.dp)
                start.linkTo(parent.start)
                end.linkTo(parent.end)
            })

        SearchBar(
            searchText, modifier = Modifier
                .padding(horizontal = 18.dp)
                .constrainAs(search) {
                    top.linkTo(space.bottom)
                    width = Dimension.fillToConstraints
                    bottom.linkTo(parent.bottom, 9.dp)
                    visibility = if (shouldShowSearchBar) Visibility.Visible else Visibility.Gone
                }, onValueChange = {
                searchText = it
                onValueChangeListener?.invoke(searchText)
            }
        )
    }
}

@Composable
fun SearchBar(
    search: String, modifier: Modifier = Modifier, onValueChange: (String) -> Unit
) {

    Box(
        modifier = modifier.clip(RoundedCornerShape(18.dp))
    ) {
        TextField(value = search,
            singleLine = true,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.primary,
                unfocusedContainerColor = MaterialTheme.colorScheme.primary,
                focusedIndicatorColor = MaterialTheme.colorScheme.secondary,
                unfocusedIndicatorColor = MaterialTheme.colorScheme.primary,
                focusedTextColor = MaterialTheme.colorScheme.surface,
                unfocusedTextColor = MaterialTheme.colorScheme.primary,
            ),
            leadingIcon = {
                Icon(
                    painterResource(id = R.drawable.search_24dp), contentDescription = "search"
                )
            },
            placeholder = {
                Text(
                    text = "Procurar criptomoedas        ",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            })
    }
}


@ThemePreviews
@Composable
private fun PersonListItemPreview() {
    DSMTheme {
        Surface {
            StickyHeaderView(shouldShowSearchBar = true)
        }
    }
}