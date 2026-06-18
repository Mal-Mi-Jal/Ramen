package com.ramenlog.ramen.api.verification.dto

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDateTime

data class VerificationHistoryItem(

    @JsonProperty("verification_id")
    val verificationId: String,

    @JsonProperty("restaurant_id")
    val restaurantId: String,

    @JsonProperty("restaurant_name")
    val restaurantName: String,

    val status: String,

    @JsonProperty("entered_at")
    val enteredAt: LocalDateTime,

    @JsonProperty("verified_at")
    val verifiedAt: LocalDateTime?
)