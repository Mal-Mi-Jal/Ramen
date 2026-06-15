package com.ramenlog.ramen.domain.visit

import com.ramenlog.ramen.domain.user.User
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface VisitVerificationRepository : JpaRepository<VisitVerification, UUID>{
    fun findByUserIdAndRestaurantIdAndStatus(userId: UUID, restaurantId: UUID, status: String): VisitVerification?

    fun existsByUserAndStatus(user: User, status: VerificationStatus): Boolean
    fun findByIdAndUser(id: UUID, user: User): VisitVerification?
}