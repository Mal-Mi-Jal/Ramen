package com.ramenlog.ramen.api.review.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class UpdateReviewRequest(

    val rating: Int,

    val content: String?,

    @JsonProperty("revisit_intention")
    val revisitIntention: Boolean
)