package com.ramenlog.ramen.api.verification.dto

import com.fasterxml.jackson.annotation.JsonProperty
import java.util.UUID

data class VerificationEnterRequest(

    @JsonProperty("restaurant_id")
    val restaurantId: UUID,

    val latitude: Double,

    val longitude: Double
)