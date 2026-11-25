package br.com.terras.app.network.cache

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "list_cache_entries",
    indices = [
        Index(value = ["category"]),
        Index(value = ["fetchedAtMillis"])
    ]
)
data class ListCacheEntity(
    @PrimaryKey val cacheKey: String,
    val category: String,
    val currency: String,
    val payloadJson: String,
    val fetchedAtMillis: Long,
    val priceUpdatedAtMillis: Long
)
