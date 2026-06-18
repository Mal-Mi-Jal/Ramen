package com.ramenlog.ramen.api.review.dto

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDateTime
import java.util.UUID

data class ReviewSummary (
    val id: UUID,

    val user: ReviewUser,

    val rating: Int,

    val content: String?,

    @JsonProperty("revisit_intention")
    val revisitIntention: Boolean,

    @JsonProperty("like_count")
    val likeCount: Long,

    val photos: List<ReviewPhotoData>,

    @JsonProperty("comment_count")
    val commentCount: Long,

    @JsonProperty("created_at")
    val createdAt: LocalDateTime
)