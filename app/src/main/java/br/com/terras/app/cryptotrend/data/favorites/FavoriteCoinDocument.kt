package br.com.terras.app.cryptotrend.data.favorites

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@Keep
@IgnoreExtraProperties
data class FavoriteCoinDocument(
    @DocumentId val id: String = "",
    val userId: String = "",
    val coinId: String = "",
    val symbol: String = "",
    val name: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
