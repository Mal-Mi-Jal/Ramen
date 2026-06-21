package com.ramenlog.ramen.api.review.dto

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDateTime
import java.util.UUID

data class CommentItem (
    val id: UUID,

    val user: CommentUser,

    val content: String,

    @JsonProperty("created_at")
    val createdAt: LocalDateTime
)