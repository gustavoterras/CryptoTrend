package br.com.terras.app.coins.detail.domain.model

enum class OrderBookMultiplier(val factor: Int) {
    X1(1),
    X10(10),
    X50(50),
    X100(100),
    X1000(1000);

    val label: String get() = "${factor}x"
}

data class OrderBookMultiplierOptionVO(
    val multiplier: OrderBookMultiplier,
    val stepFormatted: String
)

data class OrderBookEntryVO(
    val price: Double,
    val priceFormatted: String,
    val size: Double,
    val sizeFormatted: String,
    val cumulativeTotal: Double,
    val depthPercentage: Float
)

data class OrderBookSnapshotVO(
    val pairSymbol: String,
    val bids: List<OrderBookEntryVO>,
    val asks: List<OrderBookEntryVO>,
    val spreadAbsolute: Double,
    val spreadAbsoluteFormatted: String,
    val spreadPercentageFormatted: String,
    val isLive: Boolean,
    val selectedMultiplier: OrderBookMultiplier = OrderBookMultiplier.X1,
    val selectedStepFormatted: String = "",
    val availableMultipliers: List<OrderBookMultiplierOptionVO> = emptyList()
)

sealed interface OrderBookUiState {
    data object Hidden : OrderBookUiState
    data class Collapsed(val pairSymbol: String) : OrderBookUiState
    data class Loading(val pairSymbol: String) : OrderBookUiState
    data class Visible(val snapshot: OrderBookSnapshotVO) : OrderBookUiState
}
