package com.ramenlog.ramen.domain.review

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.UUID

interface ReviewRepository : JpaRepository<Review, UUID> {
    fun findByRestaurantId(
        restaurantId: UUID,
        pageable: Pageable
    ): Page<Review>

    fun countByRestaurantId(
        restaurantId: UUID
    ): Long

    fun countByRestaurantIdAndRevisitIntentionTrue(
        restaurantId: UUID
    ): Long

    @Query("""
        select avg(r.rating)
        from Review r
        where r.restaurant.id = :restaurantId
    """)
    fun getAverageRating(
        restaurantId: UUID
    ): Double?

    fun existsByVerificationId(verificationId: UUID): Boolean
}