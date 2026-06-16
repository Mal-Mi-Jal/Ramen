package com.ramenlog.ramen.api.verification

import com.fasterxml.jackson.annotation.JsonProperty

data class VerificationError(

    val code: String,

    val message: String,

    val data: VerificationErrorData
)

data class VerificationErrorData(

    @JsonProperty("current_stay_seconds")
    val currentStaySeconds: Long,

    @JsonProperty("required_stay_seconds")
    val requiredStaySeconds: Long,

    @JsonProperty("remaining_seconds")
    val remainingSeconds: Long
)