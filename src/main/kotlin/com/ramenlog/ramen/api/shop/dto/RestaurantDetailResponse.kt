package com.ramenlog.ramen.api.shop.dto

data class RestaurantDetailResponse(
    val success: Boolean,
    val data: RestaurantDetailData
)

data class RestaurantDetailData(
    val id: String,


    val kakaoPlaceId: String,
    val name: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val phone: String?,

    
    val reviewCount: Int,

    
    val averageRating: Double,
    
    val revisitRate: Double
)
