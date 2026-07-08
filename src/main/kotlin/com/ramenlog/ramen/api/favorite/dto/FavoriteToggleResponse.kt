package com.ramenlog.ramen.api.favorite.dto

data class FavoriteToggleResponse(

    val success: Boolean,

    val data: FavoriteToggleData

)

data class FavoriteToggleData(

    val isFavorite: Boolean,

    val favoriteCount: Long

)