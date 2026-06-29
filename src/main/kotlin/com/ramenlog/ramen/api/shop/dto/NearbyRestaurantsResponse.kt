package com.ramenlog.ramen.api.shop.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class NearbyRestaurantsResponse(
    val success: Boolean,
    val data: RestaurantData
)

data class RestaurantData(
    val restaurants: List<RestaurantResponse>
)

data class RestaurantResponse(
    val id: String,

    @JsonProperty("kakao_place_id")
    val kakaoPlaceId: String,
    val name: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val phone: String?,

    @JsonProperty("distance_meters")
    val distanceMeters: Int?,
    @JsonProperty("review_count")
    val reviewCount: Int,
    @JsonProperty("average_rating")
    val averageRating: Double
)