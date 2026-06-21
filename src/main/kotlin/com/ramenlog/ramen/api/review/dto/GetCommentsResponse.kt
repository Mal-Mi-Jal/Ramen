package com.ramenlog.ramen.api.review.dto

data class GetCommentsResponse (
    val success: Boolean,

    val data: CommentsData
)

data class CommentsData (
    val comments: List<CommentItem>,

    val pagination: PaginationData
)