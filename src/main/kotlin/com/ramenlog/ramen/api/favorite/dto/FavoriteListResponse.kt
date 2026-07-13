package com.ramenlog.ramen.api.favorite.dto

data class FavoriteListResponse(

    val success: Boolean,

    val data: List<FavoriteRestaurantDto>

)