package com.ramenlog.ramen.api.visit.dto

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDateTime
import java.util.UUID

data class RestaurantVisitHistoryResponse(

    val success: Boolean,

    val data: RestaurantVisitHistoryData
)

data class RestaurantVisitHistoryData(

    @JsonProperty("restaurant_id")
    val restaurantId: UUID,

    @JsonProperty("visit_count")
    val visitCount: Int,

    @JsonProperty("last_visited_at")
    val lastVisitedAt: LocalDateTime,

    @JsonProperty("first_visited_at")
    val firstVisitedAt: LocalDateTime
)