package com.ramenlog.ramen.domain.review

import com.fasterxml.jackson.annotation.JsonProperty
import com.ramenlog.ramen.api.review.dto.ReviewPhotoData
import com.ramenlog.ramen.api.review.dto.ReviewUser
import java.time.LocalDateTime
import java.util.UUID

data class ReviewDetailData (

    val id: UUID,

    val restaurant: ReviewRestaurant,

    val user: ReviewUser,

    val rating: Int,

    val content: String?,

    @JsonProperty("revisit_intention")
    val revisitIntention: Boolean,

    @JsonProperty("like_count")
    val likeCount: Long,

    @JsonProperty("comment_count")
    val commentCount: Long,

    val photos: List<ReviewPhotoData>,

    @JsonProperty("created_at")
    val createdAt: LocalDateTime,

    @JsonProperty("updated_at")
    val updatedAt: LocalDateTime
)