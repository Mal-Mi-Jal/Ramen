package com.ramenlog.ramen.api.review.dto

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDateTime
import java.util.UUID

data class UpdateReviewResponse(

    val success: Boolean,

    val data: UpdateReviewData
)

data class UpdateReviewData(

    @JsonProperty("review_id")
    val reviewId: UUID,

    val rating: Int,

    val content: String?,

    @JsonProperty("revisit_intention")
    val revisitIntention: Boolean,

    @JsonProperty("updated_at")
    val updatedAt: LocalDateTime
)