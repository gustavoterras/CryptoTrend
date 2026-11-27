package br.com.terras.app.database.portfolio

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "portfolio_transactions",
    indices = [
        Index(value = ["coinId"]),
        Index(value = ["timestamp"])
    ]
)
data class PortfolioTransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val coinId: String,
    val coinSymbol: String,
    val coinName: String,
    val coinImageUrl: String,
    val type: String,
    val quantity: Double,
    val unitPrice: Double,
    val timestamp: Long,
    val note: String = ""
)
