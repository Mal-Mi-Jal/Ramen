package com.ramenlog.ramen.api.review.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class PaginationData(

    val total: Long,

    val page: Int,

    val size: Int,

    @JsonProperty("total_pages")
    val totalPages: Int
)