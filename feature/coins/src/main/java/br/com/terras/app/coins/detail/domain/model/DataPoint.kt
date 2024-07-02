package br.com.terras.app.coins.detail.domain.model

data class DataPoint(
    val date: String,
    val y: Double,
    val xLabel: String?,
    val yLabel: String?
)