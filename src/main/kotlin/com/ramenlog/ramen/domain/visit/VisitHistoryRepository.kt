package com.ramenlog.ramen.domain.visit

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface VisitHistoryRepository : JpaRepository<VisitHistory, UUID> {
    fun findByUserId(userId: UUID): List<VisitHistory>

    fun findByUserIdAndRestaurantId(userId: UUID, restaurantId: UUID): VisitHistory?
}