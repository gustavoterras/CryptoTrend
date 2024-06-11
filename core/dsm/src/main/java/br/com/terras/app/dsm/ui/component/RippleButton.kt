package br.com.terras.app.dsm.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import br.com.terras.app.dsm.ui.component.RippleButtonSize.LARGE
import br.com.terras.app.dsm.ui.component.RippleButtonSize.MEDIUM
import br.com.terras.app.dsm.ui.component.RippleButtonSize.SMALL

enum class RippleButtonSize {
    SMALL, MEDIUM, LARGE
}

@Composable
fun RippleButton(
    text: String,
    size: RippleButtonSize = LARGE,
    onClick: () -> Unit
) {

    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceTint),
        modifier = Modifier.getRippleButtonSize(size)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.surface
        )
    }
}

@Composable
private fun Modifier.getRippleButtonSize(size: RippleButtonSize): Modifier {
    val config = LocalConfiguration.current
    return when (size) {
        SMALL -> wrapContentSize()
        MEDIUM -> width(config.screenWidthDp.dp / 2)
        LARGE -> fillMaxWidth().padding(horizontal = 20.dp)
    }
}

@Preview
@Composable
private fun RippleButtonPreview() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        RippleButton(text = "Click me", SMALL) {}
        RippleButton(text = "Click me", MEDIUM) {}
        RippleButton(text = "Click me") {}
    }
}