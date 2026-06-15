package com.ramenlog.ramen.api.shop.dto

data class NearbyRestaurantsResponse(
    val success: Boolean,
    val data: RestaurantData
)

data class RestaurantData(
    val restaurants: List<RestaurantResponse>
)

data class RestaurantResponse(
    val id: String,
    val kakaoPlaceId: String,
    val name: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val phone: String?,
    val distanceMeters: Int?,
    val reviewCount: Int,
    val averageRating: Double
)