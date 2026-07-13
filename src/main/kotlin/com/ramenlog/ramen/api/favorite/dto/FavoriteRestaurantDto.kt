package com.ramenlog.ramen.api.favorite.dto

import java.util.UUID

data class FavoriteRestaurantDto(

    val restaurantId: UUID,

    val restaurantName: String,

    val address: String,

    val thumbnail: String?,

    val averageRating: Double,

    val reviewCount: Int
)