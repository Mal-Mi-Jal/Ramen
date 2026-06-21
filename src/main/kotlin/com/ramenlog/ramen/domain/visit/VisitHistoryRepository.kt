package com.ramenlog.ramen.domain.visit

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface VisitHistoryRepository : JpaRepository<VisitHistory, UUID> {
    @EntityGraph(attributePaths = ["restaurant"])
    fun findByUserId(
        userId: UUID,
        pageable: Pageable
    ): Page<VisitHistory>

    fun findByUserIdAndRestaurantId(userId: UUID, restaurantId: UUID): VisitHistory?
}