package br.com.terras.app.database.favorites

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorite_coins")
data class FavoriteCoinEntity(
    @PrimaryKey val id: String,
    val addedAt: Long = System.currentTimeMillis()
)
