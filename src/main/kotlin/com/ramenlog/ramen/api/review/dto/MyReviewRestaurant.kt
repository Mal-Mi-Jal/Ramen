package com.ramenlog.ramen.api.review.dto

import java.util.UUID

data class MyReviewRestaurant (
    val id: UUID,

    val name: String,

    val address: String?
)