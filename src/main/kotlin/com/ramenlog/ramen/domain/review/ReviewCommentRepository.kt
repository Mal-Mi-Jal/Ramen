package com.ramenlog.ramen.domain.review

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface ReviewCommentRepository : JpaRepository<ReviewComment, UUID> {
    fun findByReviewId(reviewId: UUID): List<ReviewComment>

    fun countByReviewId(reviewId: UUID): Long
}