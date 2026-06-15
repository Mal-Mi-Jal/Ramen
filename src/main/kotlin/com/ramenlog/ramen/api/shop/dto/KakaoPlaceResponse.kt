package com.ramenlog.ramen.api.shop.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class KakaoPlaceResponse (
    val documents: List<KakaoPlaceDocument>,
    val meta: KakaoMeta
)

data class KakaoMeta(
    @JsonProperty("is_end")
    val isEnd: Boolean
)

data class KakaoPlaceDocument (
    @JsonProperty("id")
    val id: String,

    @JsonProperty("place_name")
    val placeName: String,

    @JsonProperty("address_name")
    val addressName: String,

    @JsonProperty("road_address_name")
    val roadAddressName: String,

    val x: String,
    val y: String,

    @JsonProperty("phone")
    val phone: String?,

    @JsonProperty("distance")
    val distance: String?,

    @JsonProperty("category_name")
    val categoryName: String?,

    @JsonProperty("place_url")
    val placeUrl: String,
)