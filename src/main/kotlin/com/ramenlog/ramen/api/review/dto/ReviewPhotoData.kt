package com.ramenlog.ramen.api.review.dto

import com.fasterxml.jackson.annotation.JsonProperty
import java.util.UUID

data class ReviewPhotoData (
    val id: UUID,

    @JsonProperty("image_url")
    val imageUrl: String,

    @JsonProperty("order_index")
    val orderIndex: Int
)