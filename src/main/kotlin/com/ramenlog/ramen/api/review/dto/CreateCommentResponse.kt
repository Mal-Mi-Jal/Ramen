package com.ramenlog.ramen.api.review.dto

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDateTime
import java.util.UUID

data class CreateCommentResponse (
    val success: Boolean,

    val data: CommentData
)

data class CommentData (

    val id : UUID,
    val user: CommentUser,
    val content: String,
    @JsonProperty("created_at")
    val createdAt : LocalDateTime
)

