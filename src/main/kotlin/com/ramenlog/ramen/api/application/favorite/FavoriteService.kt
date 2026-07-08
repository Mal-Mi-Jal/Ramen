package com.ramenlog.ramen.api.application.favorite

import com.ramenlog.ramen.api.common.RestaurantNotFoundException
import com.ramenlog.ramen.api.favorite.dto.FavoriteToggleData
import com.ramenlog.ramen.api.favorite.dto.FavoriteToggleResponse
import com.ramenlog.ramen.domain.favorite.FavoriteRestaurant
import com.ramenlog.ramen.domain.favorite.FavoriteRestaurantRepository
import com.ramenlog.ramen.domain.restaurant.RestaurantRepository
import com.ramenlog.ramen.domain.user.User
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class FavoriteService (
    private val favoriteRepository : FavoriteRestaurantRepository,
    private val restaurantRepository : RestaurantRepository
){
    @Transactional
    fun toggleFavorite(
        user: User,
        restaurantId: UUID
    ): FavoriteToggleResponse {

        val restaurant =
            restaurantRepository
                .findById(restaurantId)
                .orElseThrow {
                    RestaurantNotFoundException()
                }

        val favorite =
            favoriteRepository
                .findByUserIdAndRestaurantId(
                    user.id!!,
                    restaurantId
                )

        val isFavorite =

            if (favorite == null) {
                favoriteRepository.save(
                    FavoriteRestaurant(
                        user = user,
                        restaurant = restaurant
                    )
                )
                true
            } else {
                favoriteRepository.delete(favorite)
                false
            }

        val favoriteCount =
            favoriteRepository
                .countByRestaurantId(
                    restaurantId

                )
        return FavoriteToggleResponse(
            success = true,
            data = FavoriteToggleData(
                isFavorite = isFavorite,
                favoriteCount = favoriteCount
            )
        )
    }

    @Transactional(readOnly = true)
    fun isFavorite(
        user: User,
        restaurantId: UUID
    ): Boolean {

        return favoriteRepository
            .existsByUserIdAndRestaurantId(
                user.id!!,
                restaurantId
            )
    }

    @Transactional(readOnly = true)
    fun getMyFavorites(
        user: User
    ): List<FavoriteRestaurant> {

        return favoriteRepository
            .findByUserIdOrderByCreatedAtDesc(
                user.id!!
            )
    }


}