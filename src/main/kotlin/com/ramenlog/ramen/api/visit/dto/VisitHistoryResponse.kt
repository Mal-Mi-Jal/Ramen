package com.ramenlog.ramen.api.visit.dto

import com.fasterxml.jackson.annotation.JsonProperty
import com.ramenlog.ramen.api.review.dto.PaginationData

data class VisitHistoryResponse (
    val success: Boolean,

    val data: VisitHistoryData
)

data class VisitHistoryData (

    @JsonProperty("visit_history")
    val visitHistory: List<VisitHistoryItem>,

    val pagination: PaginationData
)