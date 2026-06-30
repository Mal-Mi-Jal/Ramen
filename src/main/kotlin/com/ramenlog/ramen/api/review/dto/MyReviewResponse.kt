package com.ramenlog.ramen.api.review.dto

data class MyReviewsData (
    val reviews: List<MyReviewSummary>,

    val pagination: PaginationData
)

data class MyReviewsResponse (
    val success: Boolean,

    val data: MyReviewsData
)