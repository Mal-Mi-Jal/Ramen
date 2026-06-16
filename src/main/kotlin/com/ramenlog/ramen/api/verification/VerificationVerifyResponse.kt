package com.ramenlog.ramen.api.verification

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDateTime

data class VerificationVerifyResponse(
    val success: Boolean,

    val data: VerificationVerifyData? = null,

    val error: VerificationError? = null,

    val message: String? = null
)

data class VerificationVerifyData(

    @JsonProperty("verification_id")
    val verificationId: String,

    val status: String,

    @JsonProperty("stay_duration_seconds")
    val stayDurationSeconds: Long,

    @JsonProperty("verified_at")
    val verifiedAt: LocalDateTime
)