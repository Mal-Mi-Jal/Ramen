package com.ramenlog.ramen.domain.review

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

interface ReviewPhotoRepository : JpaRepository<ReviewPhoto, UUID> {
    fun findByReviewIdOrderByOrderIndexAsc(reviewId: UUID): List<ReviewPhoto>

    @Transactional
    @Modifying
    fun deleteByReviewId(reviewId: UUID)
}