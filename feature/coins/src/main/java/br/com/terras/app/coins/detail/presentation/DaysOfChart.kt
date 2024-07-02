package br.com.terras.app.coins.detail.presentation

enum class DaysOfChart(val text: String, val value: Int) {
    DAY("24H", 1),
    WEEK("7D", 7),
    MONTH("30D", 30),
    TREE_MONTHS("90D", 90),
    YEAR("1Y", 365)
}