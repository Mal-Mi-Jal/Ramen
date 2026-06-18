package com.ramenlog.ramen.domain.review

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface ReviewRepository : JpaRepository<Review, UUID> {
    fun findByRestaurantId(restaurantId: UUID): List<Review>

    fun existsByVerificationId(verificationId: UUID): Boolean

    fun findByRestaurantId(
        restaurantId: UUID,
        pageable: Pageable
    ): Page<Review>
}