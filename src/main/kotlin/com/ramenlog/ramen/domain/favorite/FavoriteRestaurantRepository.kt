package com.ramenlog.ramen.domain.favorite

import com.ramenlog.ramen.domain.user.User
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface FavoriteRestaurantRepository : JpaRepository<FavoriteRestaurant, UUID> {
    fun existsByUserIdAndRestaurantId(
        userId: UUID,
        restaurantId: UUID
    ): Boolean

    fun findByUserIdOrderByCreatedAtDesc(
        userId: UUID
    ): List<FavoriteRestaurant>

    fun findByUserIdAndRestaurantId(
        userId: UUID,
        restaurantId: UUID
    ): FavoriteRestaurant?

    fun deleteByUserIdAndRestaurantId(
        userId: UUID,
        restaurantId: UUID
    )

    fun countByRestaurantId(
        restaurantId: UUID
    ): Long


    fun findByUser(user: User): List<FavoriteRestaurant>
}