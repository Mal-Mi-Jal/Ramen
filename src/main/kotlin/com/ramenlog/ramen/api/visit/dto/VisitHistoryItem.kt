package com.ramenlog.ramen.api.visit.dto

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDateTime

data class VisitHistoryItem (
    val restaurant: VisitRestaurantInfo,

    @JsonProperty("visit_count")
    val visitCount: Int,

    @JsonProperty("last_visited_at")
    val lastVisitedAt: LocalDateTime
)