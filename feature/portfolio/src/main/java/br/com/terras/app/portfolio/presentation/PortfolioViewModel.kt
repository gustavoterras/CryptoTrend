package br.com.terras.app.portfolio.presentation

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.terras.app.common.formatCryptoQuantity
import br.com.terras.app.cryptotrend.core.dsm.R
import br.com.terras.app.network.portfolio.PortfolioTransactionRecord
import br.com.terras.app.network.portfolio.PortfolioTransactionType
import br.com.terras.app.portfolio.domain.PortfolioUseCase
import br.com.terras.app.portfolio.domain.model.PortfolioMarketQuote
import br.com.terras.app.portfolio.domain.model.PortfolioSnapshot
import br.com.terras.app.portfolio.domain.model.PortfolioTransactionVO
import br.com.terras.app.portfolio.domain.model.SelectableCoinVO
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class PortfolioViewModel @Inject constructor(
    private val useCase: PortfolioUseCase
) : ViewModel() {

    enum class PortfolioSubTab(
        val label: String,
        @StringRes val labelRes: Int
    ) {
        ASSETS("Ativos", R.string.portfolio_tab_assets),
        HISTORY("Histórico", R.string.portfolio_tab_history)
    }

    sealed interface PortfolioUiState {
        data object Loading : PortfolioUiState
        data object Empty : PortfolioUiState
        data class Content(
            val snapshot: PortfolioSnapshot,
            val activeTab: PortfolioSubTab,
            val selectedSliceId: String?,
            val isRefreshing: Boolean,
            val isOfflineFallback: Boolean
        ) : PortfolioUiState
    }

    data class TransactionEditorState(
        val isOpen: Boolean = false,
        val editingTransactionId: Long? = null,
        val isCoinLocked: Boolean = false,
        val selectedCoin: SelectableCoinVO? = null,
        val type: PortfolioTransactionType = PortfolioTransactionType.BUY,
        val quantityInput: String = "",
        val unitPriceInput: String = "",
        val timestamp: Long = System.currentTimeMillis(),
        val noteInput: String = "",
        val validationError: String? = null,
        val availableSellQuantity: Double = 0.0,
        val isFetchingPrice: Boolean = false,
        val isSaving: Boolean = false
    )

    data class CoinPickerState(
        val query: String = "",
        val topCoins: List<SelectableCoinVO> = emptyList(),
        val searchResults: List<SelectableCoinVO> = emptyList(),
        val isLoading: Boolean = false,
        val errorMessage: String? = null
    ) {
        val displayedCoins: List<SelectableCoinVO>
            get() = if (query.isBlank()) topCoins else searchResults
    }

    sealed interface PortfolioIntent {
        data object Refresh : PortfolioIntent
        data class SelectSubTab(val tab: PortfolioSubTab) : PortfolioIntent
        data class SelectAllocationSlice(val sliceId: String?) : PortfolioIntent
        data class OpenNewTransaction(
            val prefilledCoin: SelectableCoinVO? = null,
            val lockCoin: Boolean = false
        ) : PortfolioIntent
        data class OpenEditTransaction(val transaction: PortfolioTransactionVO) : PortfolioIntent
        data object DismissEditor : PortfolioIntent
        data class SelectEditorCoin(val coin: SelectableCoinVO) : PortfolioIntent
        data object ClearSelectedEditorCoin : PortfolioIntent
        data class UpdateEditorType(val type: PortfolioTransactionType) : PortfolioIntent
        data class UpdateQuantityInput(val value: String) : PortfolioIntent
        data class UpdateUnitPriceInput(val value: String) : PortfolioIntent
        data object UseCurrentMarketPrice : PortfolioIntent
        data class UpdateTimestamp(val timestamp: Long) : PortfolioIntent
        data class UpdateNoteInput(val value: String) : PortfolioIntent
        data class UpdateCoinSearchQuery(val query: String) : PortfolioIntent
        data object SaveTransaction : PortfolioIntent
        data class RequestDeleteTransaction(val transaction: PortfolioTransactionVO) : PortfolioIntent
        data object DismissDeleteDialog : PortfolioIntent
        data object ConfirmDeleteTransaction : PortfolioIntent
        data object ClearDeleteError : PortfolioIntent
    }

    private val _transactions = MutableStateFlow<List<PortfolioTransactionRecord>?>(null)
    private val _quotes = MutableStateFlow<Map<String, PortfolioMarketQuote>>(emptyMap())
    private val _activeTab = MutableStateFlow(PortfolioSubTab.ASSETS)
    private val _selectedSliceId = MutableStateFlow<String?>(null)
    private val _isRefreshing = MutableStateFlow(false)
    private val _isOfflineFallback = MutableStateFlow(false)

    private val _editorState = MutableStateFlow(TransactionEditorState())
    val editorState: StateFlow<TransactionEditorState> = _editorState.asStateFlow()

    private val _coinPickerState = MutableStateFlow(CoinPickerState())
    val coinPickerState: StateFlow<CoinPickerState> = _coinPickerState.asStateFlow()

    private val _pendingDeleteTransaction = MutableStateFlow<PortfolioTransactionVO?>(null)
    val pendingDeleteTransaction: StateFlow<PortfolioTransactionVO?> = _pendingDeleteTransaction.asStateFlow()

    private val _deleteErrorMessage = MutableStateFlow<String?>(null)
    val deleteErrorMessage: StateFlow<String?> = _deleteErrorMessage.asStateFlow()

    private val isPortuguese: Boolean
        get() = Locale.getDefault().language.equals("pt", ignoreCase = true)

    private var lastFetchedCoinIds: Set<String> = emptySet()
    private var quotesJob: Job? = null
    private var pickerSearchJob: Job? = null

    private data class UiMeta(
        val activeTab: PortfolioSubTab,
        val selectedSliceId: String?,
        val isRefreshing: Boolean,
        val isOfflineFallback: Boolean
    )

    private val uiMetaFlow = combine(
        _activeTab,
        _selectedSliceId,
        _isRefreshing,
        _isOfflineFallback
    ) { tab, sliceId, refreshing, offline ->
        UiMeta(
            activeTab = tab,
            selectedSliceId = sliceId,
            isRefreshing = refreshing,
            isOfflineFallback = offline
        )
    }

    val uiState: StateFlow<PortfolioUiState> = combine(
        _transactions,
        _quotes,
        uiMetaFlow
    ) { transactions, quotes, meta ->
        if (transactions == null) {
            PortfolioUiState.Loading
        } else if (transactions.isEmpty()) {
            PortfolioUiState.Empty
        } else {
            val snapshot = useCase.buildSnapshot(transactions, quotes)
            PortfolioUiState.Content(
                snapshot = snapshot,
                activeTab = meta.activeTab,
                selectedSliceId = meta.selectedSliceId,
                isRefreshing = meta.isRefreshing,
                isOfflineFallback = meta.isOfflineFallback
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = PortfolioUiState.Loading
    )

    init {
        observeLocalTransactions()
    }

    private fun observeLocalTransactions() {
        viewModelScope.launch {
            useCase.observeTransactions().collect { records ->
                _transactions.value = records
                val coinIds = records.map { it.coinId.trim().lowercase() }
                    .filter { it.isNotEmpty() }
                    .toSet()

                if (coinIds.isEmpty()) {
                    lastFetchedCoinIds = emptySet()
                    _isOfflineFallback.value = false
                } else if (!lastFetchedCoinIds.containsAll(coinIds)) {
                    refreshMarketQuotes(coinIds.toList(), showRefreshingIndicator = false)
                }
            }
        }
    }

    fun handleIntent(intent: PortfolioIntent) {
        when (intent) {
            is PortfolioIntent.Refresh -> {
                val ids = _transactions.value
                    ?.map { it.coinId.trim().lowercase() }
                    ?.filter { it.isNotEmpty() }
                    ?.distinct()
                    .orEmpty()
                if (ids.isNotEmpty()) {
                    refreshMarketQuotes(ids, showRefreshingIndicator = true)
                }
            }
            is PortfolioIntent.SelectSubTab -> {
                _activeTab.value = intent.tab
            }
            is PortfolioIntent.SelectAllocationSlice -> {
                _selectedSliceId.value = intent.sliceId
            }
            is PortfolioIntent.OpenNewTransaction -> openNewTransaction(
                prefilledCoin = intent.prefilledCoin,
                lockCoin = intent.lockCoin
            )
            is PortfolioIntent.OpenEditTransaction -> openEditTransaction(intent.transaction)
            is PortfolioIntent.DismissEditor -> {
                pickerSearchJob?.cancel()
                _editorState.value = TransactionEditorState()
            }
            is PortfolioIntent.SelectEditorCoin -> onSelectEditorCoin(intent.coin)
            is PortfolioIntent.ClearSelectedEditorCoin -> {
                _editorState.update {
                    it.copy(
                        selectedCoin = null,
                        validationError = null,
                        availableSellQuantity = 0.0
                    )
                }
            }
            is PortfolioIntent.UpdateEditorType -> {
                _editorState.update {
                    it.copy(
                        type = intent.type,
                        validationError = null
                    )
                }
                refreshAvailableSellQuantity()
            }
            is PortfolioIntent.UpdateQuantityInput -> {
                _editorState.update {
                    it.copy(
                        quantityInput = intent.value,
                        validationError = null
                    )
                }
            }
            is PortfolioIntent.UpdateUnitPriceInput -> {
                _editorState.update {
                    it.copy(
                        unitPriceInput = intent.value,
                        validationError = null
                    )
                }
            }
            is PortfolioIntent.UseCurrentMarketPrice -> applyCurrentMarketPrice()
            is PortfolioIntent.UpdateTimestamp -> {
                _editorState.update {
                    it.copy(
                        timestamp = intent.timestamp,
                        validationError = null
                    )
                }
            }
            is PortfolioIntent.UpdateNoteInput -> {
                _editorState.update {
                    it.copy(noteInput = intent.value)
                }
            }
            is PortfolioIntent.UpdateCoinSearchQuery -> onCoinSearchQueryChanged(intent.query)
            is PortfolioIntent.SaveTransaction -> saveTransaction()
            is PortfolioIntent.RequestDeleteTransaction -> {
                _deleteErrorMessage.value = null
                _pendingDeleteTransaction.value = intent.transaction
            }
            is PortfolioIntent.DismissDeleteDialog -> {
                _pendingDeleteTransaction.value = null
            }
            is PortfolioIntent.ConfirmDeleteTransaction -> confirmDeleteTransaction()
            is PortfolioIntent.ClearDeleteError -> {
                _deleteErrorMessage.value = null
            }
        }
    }

    private fun refreshMarketQuotes(coinIds: List<String>, showRefreshingIndicator: Boolean) {
        quotesJob?.cancel()
        quotesJob = viewModelScope.launch {
            if (showRefreshingIndicator) {
                _isRefreshing.value = true
            }
            useCase.getMarketQuotes(coinIds)
                .onSuccess { fetched ->
                    _quotes.update { existing -> existing + fetched }
                    lastFetchedCoinIds = lastFetchedCoinIds + coinIds.toSet()
                    _isOfflineFallback.value = false
                }
                .onFailure {
                    _isOfflineFallback.value = true
                }
            _isRefreshing.value = false
        }
    }

    private fun openNewTransaction(
        prefilledCoin: SelectableCoinVO?,
        lockCoin: Boolean
    ) {
        val currentPrice = prefilledCoin?.currentPrice
            ?: prefilledCoin?.id?.let { _quotes.value[it.lowercase()]?.currentPrice }
        val resolvedCoin = prefilledCoin?.copy(currentPrice = currentPrice)

        _editorState.value = TransactionEditorState(
            isOpen = true,
            editingTransactionId = null,
            isCoinLocked = lockCoin,
            selectedCoin = resolvedCoin,
            type = PortfolioTransactionType.BUY,
            quantityInput = "",
            unitPriceInput = currentPrice?.let(::formatInputNumber).orEmpty(),
            timestamp = System.currentTimeMillis(),
            noteInput = "",
            availableSellQuantity = resolvedCoin?.let { availableQuantityForCoin(it.id, null) } ?: 0.0
        )

        if (!lockCoin) {
            loadTopCoinsIfNeeded()
        }
        if (resolvedCoin != null && resolvedCoin.currentPrice == null) {
            fetchSelectedCoinPrice(resolvedCoin)
        }
    }

    private fun openEditTransaction(transaction: PortfolioTransactionVO) {
        val quote = _quotes.value[transaction.coinId.lowercase()]
        val selectedCoin = SelectableCoinVO(
            id = transaction.coinId,
            symbol = transaction.coinSymbol,
            name = transaction.coinName,
            imageUrl = transaction.coinImageUrl,
            currentPrice = quote?.currentPrice
        )
        _editorState.value = TransactionEditorState(
            isOpen = true,
            editingTransactionId = transaction.id,
            isCoinLocked = true,
            selectedCoin = selectedCoin,
            type = transaction.type,
            quantityInput = formatInputNumber(transaction.quantity),
            unitPriceInput = formatInputNumber(transaction.unitPrice),
            timestamp = transaction.timestamp,
            noteInput = transaction.note,
            availableSellQuantity = availableQuantityForCoin(transaction.coinId, transaction.id)
        )

        if (quote == null) {
            fetchSelectedCoinPrice(selectedCoin)
        }
    }

    private fun onSelectEditorCoin(coin: SelectableCoinVO) {
        val cachedPrice = coin.currentPrice ?: _quotes.value[coin.id.lowercase()]?.currentPrice
        val updatedCoin = coin.copy(currentPrice = cachedPrice)
        _editorState.update { current ->
            current.copy(
                selectedCoin = updatedCoin,
                unitPriceInput = if (current.unitPriceInput.isBlank() && cachedPrice != null) {
                    formatInputNumber(cachedPrice)
                } else {
                    current.unitPriceInput
                },
                validationError = null,
                availableSellQuantity = availableQuantityForCoin(
                    coinId = updatedCoin.id,
                    excludingTransactionId = current.editingTransactionId
                )
            )
        }
        if (cachedPrice == null) {
            fetchSelectedCoinPrice(updatedCoin)
        }
    }

    private fun fetchSelectedCoinPrice(coin: SelectableCoinVO) {
        viewModelScope.launch {
            _editorState.update { it.copy(isFetchingPrice = true) }
            useCase.getMarketQuotes(listOf(coin.id))
                .onSuccess { quotesMap ->
                    _quotes.update { it + quotesMap }
                    val fetched = quotesMap[coin.id.lowercase()]
                    _editorState.update { current ->
                        if (current.selectedCoin?.id == coin.id) {
                            val updatedCoin = current.selectedCoin.copy(
                                currentPrice = fetched?.currentPrice ?: current.selectedCoin.currentPrice,
                                imageUrl = fetched?.imageUrl?.takeIf { it.isNotBlank() }
                                    ?: current.selectedCoin.imageUrl
                            )
                            current.copy(
                                selectedCoin = updatedCoin,
                                unitPriceInput = if (current.unitPriceInput.isBlank() && fetched != null) {
                                    formatInputNumber(fetched.currentPrice)
                                } else {
                                    current.unitPriceInput
                                },
                                isFetchingPrice = false
                            )
                        } else {
                            current.copy(isFetchingPrice = false)
                        }
                    }
                }
                .onFailure {
                    _editorState.update { it.copy(isFetchingPrice = false) }
                }
        }
    }

    private fun applyCurrentMarketPrice() {
        val coin = _editorState.value.selectedCoin ?: return
        val cached = coin.currentPrice ?: _quotes.value[coin.id.lowercase()]?.currentPrice
        if (cached != null) {
            _editorState.update {
                it.copy(
                    unitPriceInput = formatInputNumber(cached),
                    validationError = null
                )
            }
            return
        }

        viewModelScope.launch {
            _editorState.update { it.copy(isFetchingPrice = true) }
            useCase.getMarketQuotes(listOf(coin.id))
                .onSuccess { quotesMap ->
                    _quotes.update { it + quotesMap }
                    val price = quotesMap[coin.id.lowercase()]?.currentPrice
                    _editorState.update { current ->
                        current.copy(
                            selectedCoin = current.selectedCoin?.copy(currentPrice = price),
                            unitPriceInput = price?.let(::formatInputNumber) ?: current.unitPriceInput,
                            isFetchingPrice = false
                        )
                    }
                }
                .onFailure {
                    _editorState.update {
                        it.copy(
                            isFetchingPrice = false,
                            validationError = if (isPortuguese) {
                                "Não foi possível obter a cotação atual."
                            } else {
                                "Unable to fetch the current quote."
                            }
                        )
                    }
                }
        }
    }

    private fun loadTopCoinsIfNeeded() {
        if (_coinPickerState.value.topCoins.isNotEmpty()) return
        viewModelScope.launch {
            _coinPickerState.update { it.copy(isLoading = true, errorMessage = null) }
            useCase.getTopSelectableCoins()
                .onSuccess { list ->
                    _coinPickerState.update {
                        it.copy(
                            topCoins = list,
                            isLoading = false,
                            errorMessage = null
                        )
                    }
                }
                .onFailure {
                    _coinPickerState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = if (isPortuguese) {
                                "Não foi possível carregar a lista de moedas."
                            } else {
                                "Unable to load cryptocurrency list."
                            }
                        )
                    }
                }
        }
    }

    private fun onCoinSearchQueryChanged(query: String) {
        _coinPickerState.update {
            it.copy(query = query, errorMessage = null)
        }
        pickerSearchJob?.cancel()
        val trimmed = query.trim()
        if (trimmed.isEmpty()) {
            _coinPickerState.update { it.copy(searchResults = emptyList(), isLoading = false) }
            loadTopCoinsIfNeeded()
            return
        }

        pickerSearchJob = viewModelScope.launch {
            delay(350)
            _coinPickerState.update { it.copy(isLoading = true, errorMessage = null) }
            useCase.searchSelectableCoins(trimmed)
                .onSuccess { results ->
                    _coinPickerState.update {
                        it.copy(
                            searchResults = results,
                            isLoading = false,
                            errorMessage = null
                        )
                    }
                }
                .onFailure {
                    _coinPickerState.update {
                        it.copy(
                            searchResults = emptyList(),
                            isLoading = false,
                            errorMessage = if (isPortuguese) {
                                "Erro ao buscar moedas. Verifique sua conexão."
                            } else {
                                "Error searching coins. Check your connection."
                            }
                        )
                    }
                }
        }
    }

    private fun refreshAvailableSellQuantity() {
        val state = _editorState.value
        val coin = state.selectedCoin ?: return
        _editorState.update {
            it.copy(
                availableSellQuantity = availableQuantityForCoin(coin.id, state.editingTransactionId)
            )
        }
    }

    private fun availableQuantityForCoin(
        coinId: String,
        excludingTransactionId: Long?
    ): Double {
        val records = _transactions.value.orEmpty()
        val filtered = records.filterNot {
            excludingTransactionId != null && it.id == excludingTransactionId
        }
        return useCase.buildSnapshot(filtered, emptyMap())
            .availableQuantitiesByCoin[coinId.trim().lowercase()]
            ?: 0.0
    }

    private fun saveTransaction() {
        val state = _editorState.value
        if (state.isSaving) return

        val coin = state.selectedCoin
        if (coin == null) {
            _editorState.update {
                it.copy(
                    validationError = if (isPortuguese) {
                        "Selecione uma criptomoeda."
                    } else {
                        "Select a cryptocurrency."
                    }
                )
            }
            return
        }

        val quantity = parseFlexibleDouble(state.quantityInput)
        if (quantity == null || quantity <= 0.0) {
            _editorState.update {
                it.copy(
                    validationError = if (isPortuguese) {
                        "Informe uma quantidade válida maior que zero."
                    } else {
                        "Enter a valid quantity greater than zero."
                    }
                )
            }
            return
        }

        val unitPrice = parseFlexibleDouble(state.unitPriceInput)
        if (unitPrice == null || unitPrice <= 0.0) {
            _editorState.update {
                it.copy(
                    validationError = if (isPortuguese) {
                        "Informe um preço unitário válido maior que zero."
                    } else {
                        "Enter a valid unit price greater than zero."
                    }
                )
            }
            return
        }

        val candidate = PortfolioTransactionRecord(
            id = state.editingTransactionId ?: 0L,
            coinId = coin.id,
            coinSymbol = coin.symbol,
            coinName = coin.name,
            coinImageUrl = coin.imageUrl,
            type = state.type,
            quantity = quantity,
            unitPrice = unitPrice,
            timestamp = state.timestamp,
            note = state.noteInput.trim()
        )

        val currentRecords = _transactions.value.orEmpty()
        val isTimelineValid = useCase.canApplyTimelineMutation(
            transactions = currentRecords,
            coinId = coin.id,
            candidateTransaction = candidate,
            excludingTransactionId = state.editingTransactionId
        )

        if (!isTimelineValid) {
            val available = availableQuantityForCoin(coin.id, state.editingTransactionId)
            _editorState.update {
                it.copy(
                    validationError = if (state.type == PortfolioTransactionType.SELL) {
                        if (isPortuguese) {
                            "Saldo insuficiente para venda. Disponível: ${available.formatCryptoQuantity()} ${coin.symbol}."
                        } else {
                            "Insufficient balance to sell. Available: ${available.formatCryptoQuantity()} ${coin.symbol}."
                        }
                    } else {
                        if (isPortuguese) {
                            "A alteração deixaria uma venda posterior sem saldo suficiente."
                        } else {
                            "This change would leave a later sell without sufficient balance."
                        }
                    }
                )
            }
            return
        }

        viewModelScope.launch {
            _editorState.update { it.copy(isSaving = true, validationError = null) }
            try {
                if (state.editingTransactionId == null) {
                    useCase.insertTransaction(candidate)
                } else {
                    useCase.updateTransaction(candidate)
                }
                _editorState.value = TransactionEditorState()
            } catch (e: Exception) {
                _editorState.update {
                    it.copy(
                        isSaving = false,
                        validationError = if (isPortuguese) {
                            "Não foi possível salvar a operação localmente."
                        } else {
                            "Unable to save transaction locally."
                        }
                    )
                }
            }
        }
    }

    private fun confirmDeleteTransaction() {
        val pending = _pendingDeleteTransaction.value ?: return
        val currentRecords = _transactions.value.orEmpty()
        val isTimelineValid = useCase.canApplyTimelineMutation(
            transactions = currentRecords,
            coinId = pending.coinId,
            candidateTransaction = null,
            excludingTransactionId = pending.id
        )

        if (!isTimelineValid) {
            _pendingDeleteTransaction.value = null
            _deleteErrorMessage.value = if (isPortuguese) {
                "Não é possível excluir esta compra porque há vendas posteriores de ${pending.coinSymbol} que dependem desse saldo."
            } else {
                "Cannot delete this buy because later ${pending.coinSymbol} sells depend on this balance."
            }
            return
        }

        viewModelScope.launch {
            useCase.deleteTransaction(pending.id)
            _pendingDeleteTransaction.value = null
        }
    }

    companion object {
        fun parseFlexibleDouble(raw: String): Double? {
            val cleaned = raw.trim().replace(" ", "")
            if (cleaned.isEmpty()) return null

            val lastComma = cleaned.lastIndexOf(',')
            val lastDot = cleaned.lastIndexOf('.')

            val normalized = when {
                lastComma >= 0 && lastDot >= 0 -> {
                    if (lastComma > lastDot) {
                        cleaned.replace(".", "").replace(",", ".")
                    } else {
                        cleaned.replace(",", "")
                    }
                }
                lastComma >= 0 -> cleaned.replace(",", ".")
                else -> cleaned
            }

            return normalized.toDoubleOrNull()?.takeIf { it.isFinite() }
        }

        fun formatInputNumber(value: Double): String {
            return BigDecimal.valueOf(value)
                .stripTrailingZeros()
                .toPlainString()
        }
    }
}
