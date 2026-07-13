package com.ramenlog.ramen.api.review.dto

import java.util.UUID

data class PendingReviewResponse(

    val success: Boolean,

    val data: List<PendingReviewData>

)

data class PendingReviewData(

    val verification_id: UUID,

    val restaurant_id: UUID,

    val restaurant_name: String,

    val days_left: Long

)