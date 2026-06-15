package com.ramenlog.ramen.api.verification

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDateTime

data class VerificationEnterResponse(
    val success: Boolean,
    val data: VerificationData,
    val message: String
)

data class VerificationData(

    @JsonProperty("verification_id")
    val verificationId: String,

    val status: String,

    @JsonProperty("entered_at")
    val enteredAt: LocalDateTime,

    @JsonProperty("required_stay_seconds")
    val requiredStaySeconds: Int
)