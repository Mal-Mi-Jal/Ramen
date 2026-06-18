package com.ramenlog.ramen.api.review.dto

data class RestaurantReviewsData (
    val reviews: List<ReviewSummary>,

    val pagination: PaginationData
)