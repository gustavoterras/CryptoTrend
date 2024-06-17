package br.com.terras.app.coins.detail.presentation

import android.annotation.SuppressLint
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import br.com.terras.app.dsm.ui.annotation.ThemePreviews
import br.com.terras.app.dsm.ui.component.RippleButton
import br.com.terras.app.dsm.ui.theme.DSMTheme

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun DetailActivityView() {

    var showDialog by remember { mutableStateOf(true) }

    Scaffold {

//        val coinsListState by viewModel.coins.collectAsState()

//        when (coinsListState) {
//            is Loading -> CoinListItemShimmerView()
//            is Success -> OnCoinsListSuccess((coinsListState as Success).coinsList)
//            is Error -> OnCoinsListError(
//                showDialog
//            ) { showDialog = false }
//        }

        OnCoinsListSuccess()
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun OnCoinsListSuccess() {

}

@Composable
private fun OnCoinsListError(
    showDialog: Boolean,
    onDismissRequest: () -> Unit
) {
    if (showDialog.not()) return

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(text = "Alerta") },
        text = { Text(text = "Erro ao carregar os dados, tente novamente mais tarde.") },
        confirmButton = {
            RippleButton(text = "Entendi", onClick = onDismissRequest)
        }
    )
}

@ThemePreviews
@Composable
private fun DetailActivityViewPreview() {
    DSMTheme {
        Surface {
            DetailActivityView()
        }
    }
}