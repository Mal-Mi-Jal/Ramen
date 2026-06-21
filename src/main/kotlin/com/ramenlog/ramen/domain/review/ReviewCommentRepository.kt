package com.ramenlog.ramen.domain.review

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface ReviewCommentRepository : JpaRepository<ReviewComment, UUID> {
    @EntityGraph(
        attributePaths = ["user"]
    )
    fun findByReviewId(
        reviewId: UUID,
        pageable: Pageable
    ): Page<ReviewComment>

    fun countByReviewId(reviewId: UUID): Long
}