package com.ramenlog.ramen.api.review.dto

import com.fasterxml.jackson.annotation.JsonProperty
import java.util.UUID

data class ReviewUser (
    val id: UUID,

    val nickname: String,

    @JsonProperty("profile_image_url")
    val profileImageUrl: String?
)