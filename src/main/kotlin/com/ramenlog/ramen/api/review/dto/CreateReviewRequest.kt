package com.ramenlog.ramen.api.review.dto

import com.fasterxml.jackson.annotation.JsonProperty
import java.util.UUID

data class CreateReviewRequest (
    @JsonProperty("restaurant_id")
    val restaurantId: UUID,

    @JsonProperty("verification_id")
    val verificationId: UUID,

    val rating: Int,

    val content: String?,

    @JsonProperty("revisit_intention")
    val revisitIntention: Boolean,

    @JsonProperty("image_url")
    val imageUrl : List<String> = emptyList()

)