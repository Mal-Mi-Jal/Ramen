package com.ramenlog.ramen.api.review.dto

import com.fasterxml.jackson.annotation.JsonProperty
import java.util.UUID

data class ReviewLikeResponse (
    val success: Boolean,

    val data: ReviewLikeData
)

data class ReviewLikeData (

    @JsonProperty("review_id")
    val reviewId: UUID,

    @JsonProperty("like_count")
    val likeCount: Long
)