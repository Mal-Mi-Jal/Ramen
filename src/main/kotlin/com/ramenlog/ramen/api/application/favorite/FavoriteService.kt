package com.ramenlog.ramen.api.application.favorite

import com.ramenlog.ramen.api.common.RestaurantNotFoundException
import com.ramenlog.ramen.api.favorite.dto.FavoriteListResponse
import com.ramenlog.ramen.api.favorite.dto.FavoriteRestaurantDto
import com.ramenlog.ramen.api.favorite.dto.FavoriteToggleData
import com.ramenlog.ramen.api.favorite.dto.FavoriteToggleResponse
import com.ramenlog.ramen.domain.favorite.FavoriteRestaurant
import com.ramenlog.ramen.domain.favorite.FavoriteRestaurantRepository
import com.ramenlog.ramen.domain.restaurant.RestaurantRepository
import com.ramenlog.ramen.domain.review.ReviewRepository
import com.ramenlog.ramen.domain.user.User
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class FavoriteService (
    private val favoriteRestaurantRepository: FavoriteRestaurantRepository,
    private val restaurantRepository: RestaurantRepository,
    private val reviewRepository: ReviewRepository
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
            favoriteRestaurantRepository
                .findByUserIdAndRestaurantId(
                    user.id!!,
                    restaurantId
                )

        val isFavorite =

            if (favorite == null) {
                favoriteRestaurantRepository.save(
                    FavoriteRestaurant(
                        user = user,
                        restaurant = restaurant
                    )
                )
                true
            } else {
                favoriteRestaurantRepository.delete(favorite)
                false
            }

        val favoriteCount =
            favoriteRestaurantRepository
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
    fun getMyFavorites(
        user: User
    ): FavoriteListResponse {

        val favorites =
            favoriteRestaurantRepository
                .findByUser(user)

        val data = favorites.map { favorite ->

            FavoriteRestaurantDto(

                restaurantId =
                    favorite.restaurant.id!!,

                restaurantName =
                    favorite.restaurant.name,

                address =
                    favorite.restaurant.address,

                thumbnail = null,

                averageRating =
                    reviewRepository
                        .getAverageRating(
                            favorite.restaurant.id!!
                        ) ?: 0.0,

                reviewCount =
                    reviewRepository
                        .countByRestaurantId(
                            favorite.restaurant.id!!
                        ).toInt()

            )

        }

        return FavoriteListResponse(

            success = true,

            data = data

        )
    }


}