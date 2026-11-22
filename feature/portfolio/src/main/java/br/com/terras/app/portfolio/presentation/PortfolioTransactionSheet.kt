package br.com.terras.app.portfolio.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import br.com.terras.app.common.formatAxisMoney
import br.com.terras.app.common.formatCryptoQuantity
import br.com.terras.app.common.formatMoney
import br.com.terras.app.common.toFormatDate
import br.com.terras.app.dsm.ui.component.TrendColor
import br.com.terras.app.network.portfolio.PortfolioTransactionType
import br.com.terras.app.portfolio.domain.model.SelectableCoinVO
import br.com.terras.app.portfolio.presentation.PortfolioViewModel.CoinPickerState
import br.com.terras.app.portfolio.presentation.PortfolioViewModel.PortfolioIntent
import br.com.terras.app.portfolio.presentation.PortfolioViewModel.TransactionEditorState
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import java.time.Instant
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PortfolioTransactionSheet(
    editorState: TransactionEditorState,
    pickerState: CoinPickerState,
    onIntent: (PortfolioIntent) -> Unit
) {
    if (!editorState.isOpen) return

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showDatePicker by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = { onIntent(PortfolioIntent.DismissEditor) },
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        val selectedCoin = editorState.selectedCoin

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = when {
                            selectedCoin == null -> "Escolher criptomoeda"
                            editorState.editingTransactionId != null -> "Editar operação"
                            else -> "Nova operação"
                        },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.surface
                    )
                    Text(
                        text = if (selectedCoin == null) {
                            "Pesquise ou selecione um ativo para registrar"
                        } else {
                            "${selectedCoin.name} (${selectedCoin.symbol})"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = { onIntent(PortfolioIntent.DismissEditor) },
                    modifier = Modifier.testTag("tx_sheet_close_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Fechar formulário",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (selectedCoin == null) {
                CoinPickerContent(
                    pickerState = pickerState,
                    onQueryChange = { onIntent(PortfolioIntent.UpdateCoinSearchQuery(it)) },
                    onSelectCoin = { onIntent(PortfolioIntent.SelectEditorCoin(it)) }
                )
            } else {
                TransactionFormContent(
                    editorState = editorState,
                    coin = selectedCoin,
                    onIntent = onIntent,
                    onOpenDatePicker = { showDatePicker = true }
                )
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = editorState.timestamp
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            colors = DatePickerDefaults.colors(
                containerColor = MaterialTheme.colorScheme.background
            ),
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { utcMidnightMillis ->
                            val selectedLocalDate = Instant.ofEpochMilli(utcMidnightMillis)
                                .atZone(ZoneOffset.UTC)
                                .toLocalDate()
                            val currentLocalTime = Instant.ofEpochMilli(editorState.timestamp)
                                .atZone(java.time.ZoneId.systemDefault())
                                .toLocalTime()
                            val updatedMillis = selectedLocalDate
                                .atTime(currentLocalTime)
                                .atZone(java.time.ZoneId.systemDefault())
                                .toInstant()
                                .toEpochMilli()
                            onIntent(PortfolioIntent.UpdateTimestamp(updatedMillis))
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("Confirmar", color = MaterialTheme.colorScheme.scrim)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancelar", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
private fun CoinPickerContent(
    pickerState: CoinPickerState,
    onQueryChange: (String) -> Unit,
    onSelectCoin: (SelectableCoinVO) -> Unit
) {
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = MaterialTheme.colorScheme.scrim,
        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
        focusedTextColor = MaterialTheme.colorScheme.surface,
        unfocusedTextColor = MaterialTheme.colorScheme.surface,
        cursorColor = MaterialTheme.colorScheme.scrim
    )

    OutlinedTextField(
        value = pickerState.query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("tx_coin_search_input"),
        placeholder = {
            Text(
                text = "Buscar por nome ou símbolo (ex: BTC, SOL)",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = "Buscar criptomoeda",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        trailingIcon = {
            if (pickerState.query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Limpar busca",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        colors = fieldColors,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search)
    )

    Spacer(modifier = Modifier.height(12.dp))

    when {
        pickerState.isLoading && pickerState.displayedCoins.isEmpty() -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.scrim,
                    modifier = Modifier.size(30.dp)
                )
            }
        }
        pickerState.errorMessage != null && pickerState.displayedCoins.isEmpty() -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = pickerState.errorMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        pickerState.displayedCoins.isEmpty() -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Nenhuma criptomoeda encontrada.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        else -> {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 360.dp),
                contentPadding = PaddingValues(bottom = 8.dp)
            ) {
                items(pickerState.displayedCoins, key = { it.id }) { coin ->
                    PickerCoinRow(
                        coin = coin,
                        onClick = { onSelectCoin(coin) }
                    )
                    HorizontalDivider(
                        thickness = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
private fun PickerCoinRow(
    coin: SelectableCoinVO,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 56.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 10.dp)
            .testTag("tx_picker_coin_${coin.id}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            CoinAvatar(imageUrl = coin.imageUrl, symbol = coin.symbol)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = coin.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.surface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = coin.symbol,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (coin.currentPrice != null) {
            Text(
                text = coin.currentPrice.formatAxisMoney(),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.surface
            )
        }
    }
}

@Composable
private fun TransactionFormContent(
    editorState: TransactionEditorState,
    coin: SelectableCoinVO,
    onIntent: (PortfolioIntent) -> Unit,
    onOpenDatePicker: () -> Unit
) {
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = MaterialTheme.colorScheme.scrim,
        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
        focusedTextColor = MaterialTheme.colorScheme.surface,
        unfocusedTextColor = MaterialTheme.colorScheme.surface,
        focusedLabelColor = MaterialTheme.colorScheme.scrim,
        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
        cursorColor = MaterialTheme.colorScheme.scrim
    )

    val parsedQty = PortfolioViewModel.parseFlexibleDouble(editorState.quantityInput) ?: 0.0
    val parsedPrice = PortfolioViewModel.parseFlexibleDouble(editorState.unitPriceInput) ?: 0.0
    val estimatedTotal = if (parsedQty > 0.0 && parsedPrice > 0.0) parsedQty * parsedPrice else 0.0

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CoinAvatar(imageUrl = coin.imageUrl, symbol = coin.symbol)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = coin.name,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.surface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        val subtitle = coin.currentPrice?.let {
                            "Preço atual: ${it.formatAxisMoney()}"
                        } ?: coin.symbol
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (!editorState.isCoinLocked) {
                    TextButton(
                        onClick = { onIntent(PortfolioIntent.ClearSelectedEditorCoin) },
                        modifier = Modifier.testTag("tx_change_coin_btn")
                    ) {
                        Text(
                            text = "Trocar",
                            color = MaterialTheme.colorScheme.scrim,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
            HorizontalDivider(
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant
            )
        }

        // BUY / SELL segmented toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OperationTypeChip(
                label = "Compra",
                selected = editorState.type == PortfolioTransactionType.BUY,
                selectedContainer = TrendColor.UP.color,
                selectedContent = TrendColor.UP.onColor,
                onClick = { onIntent(PortfolioIntent.UpdateEditorType(PortfolioTransactionType.BUY)) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("tx_type_buy")
            )
            OperationTypeChip(
                label = "Venda",
                selected = editorState.type == PortfolioTransactionType.SELL,
                selectedContainer = TrendColor.DOWN.color,
                selectedContent = TrendColor.DOWN.onColor,
                onClick = { onIntent(PortfolioIntent.UpdateEditorType(PortfolioTransactionType.SELL)) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("tx_type_sell")
            )
        }

        if (editorState.type == PortfolioTransactionType.SELL) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Saldo disponível: ${editorState.availableSellQuantity.formatCryptoQuantity()} ${coin.symbol}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (editorState.availableSellQuantity > 0.0) {
                    TextButton(
                        onClick = {
                            onIntent(
                                PortfolioIntent.UpdateQuantityInput(
                                    PortfolioViewModel.formatInputNumber(editorState.availableSellQuantity)
                                )
                            )
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.testTag("tx_sell_max_btn")
                    ) {
                        Text(
                            text = "Vender tudo",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.scrim
                        )
                    }
                }
            }
        }

        OutlinedTextField(
            value = editorState.quantityInput,
            onValueChange = { onIntent(PortfolioIntent.UpdateQuantityInput(it)) },
            label = { Text("Quantidade (${coin.symbol})") },
            placeholder = { Text("Ex: 0.25") },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = fieldColors,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Decimal,
                imeAction = ImeAction.Next
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("tx_quantity_input")
        )

        OutlinedTextField(
            value = editorState.unitPriceInput,
            onValueChange = { onIntent(PortfolioIntent.UpdateUnitPriceInput(it)) },
            label = { Text("Preço unitário") },
            placeholder = { Text("Ex: 350000.00") },
            trailingIcon = {
                IconButton(
                    onClick = { onIntent(PortfolioIntent.UseCurrentMarketPrice) },
                    enabled = !editorState.isFetchingPrice,
                    modifier = Modifier.testTag("tx_use_current_price_btn")
                ) {
                    if (editorState.isFetchingPrice) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.scrim
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = "Atualizar com preço atual",
                            tint = MaterialTheme.colorScheme.scrim
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = fieldColors,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Decimal,
                imeAction = ImeAction.Next
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("tx_unit_price_input")
        )

        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = editorState.timestamp.toFormatDate(),
                onValueChange = {},
                readOnly = true,
                label = { Text("Data da operação") },
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Filled.DateRange,
                        contentDescription = "Selecionar data da operação",
                        tint = MaterialTheme.colorScheme.scrim
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = fieldColors,
                modifier = Modifier.fillMaxWidth()
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable(
                        role = Role.Button,
                        onClickLabel = "Selecionar data da operação",
                        onClick = onOpenDatePicker
                    )
                    .testTag("tx_date_picker_btn")
            )
        }

        OutlinedTextField(
            value = editorState.noteInput,
            onValueChange = { onIntent(PortfolioIntent.UpdateNoteInput(it)) },
            label = { Text("Observação (opcional)") },
            placeholder = { Text("Ex: Compra recorrente / Meta LP") },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = fieldColors,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("tx_note_input")
        )

        if (estimatedTotal > 0.0) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Total da operação",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = estimatedTotal.formatMoney(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.surface
                )
            }
        }

        editorState.validationError?.let { errorMsg ->
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = TrendColor.DOWN.color,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("tx_validation_error")
            ) {
                Text(
                    text = errorMsg,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = TrendColor.DOWN.onColor,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                )
            }
        }

        Button(
            onClick = { onIntent(PortfolioIntent.SaveTransaction) },
            enabled = !editorState.isSaving,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.scrim,
                contentColor = Color.White
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("tx_save_btn")
        ) {
            if (editorState.isSaving) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = Color.White
                )
            } else {
                Text(
                    text = if (editorState.editingTransactionId != null) {
                        "Salvar alterações"
                    } else {
                        "Confirmar operação"
                    },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun OperationTypeChip(
    label: String,
    selected: Boolean,
    selectedContainer: Color,
    selectedContent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (selected) selectedContainer else MaterialTheme.colorScheme.tertiary,
        border = BorderStroke(
            width = 1.dp,
            color = if (selected) selectedContent.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(role = Role.RadioButton, onClick = onClick)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) selectedContent else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun CoinAvatar(
    imageUrl: String,
    symbol: String,
    modifier: Modifier = Modifier,
    sizeDp: Int = 38
) {
    if (imageUrl.isNotBlank()) {
        GlideImage(
            model = imageUrl,
            contentDescription = symbol,
            contentScale = ContentScale.Crop,
            modifier = modifier
                .size(sizeDp.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.tertiary)
        )
    } else {
        Box(
            modifier = modifier
                .size(sizeDp.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = symbol.take(3).ifBlank { "?" },
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}
