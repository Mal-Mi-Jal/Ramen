package com.ramenlog.ramen.api.verification.dto

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDateTime
import java.util.UUID

data class VerificationCurrentResponse (
    val success : Boolean,

    val data : CurrentVerificationData?
)

data class CurrentVerificationData (
    @JsonProperty("verification_id")
    val verificationId: UUID,

    @JsonProperty("restaurant_id")
    val restaurantId: UUID,

    val status: String,

    @JsonProperty("remaining_seconds")
    val remainingSeconds: Long,

    @JsonProperty("entered_at")
    val enteredAt: LocalDateTime
)