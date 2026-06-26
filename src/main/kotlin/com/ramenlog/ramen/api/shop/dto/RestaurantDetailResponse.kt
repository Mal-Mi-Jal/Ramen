package com.ramenlog.ramen.api.shop.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class RestaurantDetailResponse(
    val success: Boolean,
    val data: RestaurantDetailData
)

data class RestaurantDetailData(
    val id: String,

    @JsonProperty("kakao_place_id")
    val kakaoPlaceId: String,

    val name: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val phone: String?,

    @JsonProperty("review_count")
    val reviewCount: Int,

    @JsonProperty("average_rating")
    val averageRating: Double,
    @JsonProperty("revisit_rate")
    val revisitRate: Double
)
