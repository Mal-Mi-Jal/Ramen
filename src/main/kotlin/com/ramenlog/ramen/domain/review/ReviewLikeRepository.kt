package com.ramenlog.ramen.domain.review

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.transaction.annotation.Transactional
import java.util.Optional
import java.util.UUID

interface ReviewLikeRepository : JpaRepository<ReviewLike, UUID> {
    fun existsByReviewIdAndUserId(reviewId: UUID, userId: UUID): Boolean

    fun findByReviewIdAndUserId(reviewId: UUID, userId: UUID): ReviewLike?

    fun countByReviewId(reviewId: UUID): Long

    @Transactional
    @Modifying
    fun deleteByReviewId(reviewId: UUID)
}