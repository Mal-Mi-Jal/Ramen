package com.ramenlog.ramen.domain.review

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface ReviewRepository : JpaRepository<Review, UUID> {
    fun findByRestaurantId(restaurantId: UUID): List<Review>
}