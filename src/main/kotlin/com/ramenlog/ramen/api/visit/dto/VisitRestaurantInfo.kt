package com.ramenlog.ramen.api.visit.dto

import java.util.UUID

data class VisitRestaurantInfo (
    val id: UUID,
    val name: String,
    val address: String
)