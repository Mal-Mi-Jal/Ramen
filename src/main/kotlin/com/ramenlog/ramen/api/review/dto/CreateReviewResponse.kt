package com.ramenlog.ramen.api.review.dto

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDateTime
import java.util.UUID

data class CreateReviewResponse (
    val success: Boolean,
    val data: ReviewData
)

data class ReviewData(
    @JsonProperty("review_id")
    val reviewId: UUID,

    @JsonProperty("restaurant_id")
    val restaurantId: UUID,

    @JsonProperty("restaurant_name")
    val restaurantName: String,

    val rating: Int,

    val content: String?,

    @JsonProperty("revisit_intention")
    val revisitIntention: Boolean,

    @JsonProperty("created_at")
    val createdAt: LocalDateTime

)

