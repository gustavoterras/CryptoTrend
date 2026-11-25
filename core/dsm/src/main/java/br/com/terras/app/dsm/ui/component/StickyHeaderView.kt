package br.com.terras.app.dsm.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.com.terras.app.cryptotrend.core.dsm.R
import br.com.terras.app.dsm.ui.annotation.ThemePreviews
import br.com.terras.app.dsm.ui.theme.DSMTheme

@Composable
fun StickyHeaderView() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(color = MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .padding(horizontal = 18.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.scrim),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_show_chart_24dp),
                    contentDescription = "CryptoTrend",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }

            Text(
                text = "CryptoTrend",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.surface
            )
        }

        val authUiController = LocalAuthUiController.current
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .testTag("profile_button")
                .clickable(
                    enabled = authUiController != null,
                    role = Role.Button,
                    onClick = {
                        if (authUiController?.isSignedIn == true) {
                            authUiController.openProfileMenu()
                        } else {
                            authUiController?.requestSignIn?.invoke {}
                        }
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            if (authUiController?.isSignedIn == true) {
                val initial = authUiController.displayName
                    ?.trim()
                    ?.firstOrNull()
                    ?.uppercaseChar()
                    ?: 'G'
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = initial.toString(),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            } else {
                Icon(
                    painter = painterResource(id = R.drawable.person_24dp),
                    contentDescription = stringResource(R.string.auth_sign_in_with_google),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@ThemePreviews
@Composable
private fun StickyHeaderViewPreview() {
    DSMTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            StickyHeaderView()
        }
    }
}