package com.ramenlog.ramen.domain.visit

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface VisitVerificationRepository : JpaRepository<VisitVerification, UUID>{
    fun findByUserIdAndRestaurantIdAndStatus(userId: UUID, restaurantId: UUID, status: String): VisitVerification?
}