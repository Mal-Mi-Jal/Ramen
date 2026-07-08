package com.ramenlog.ramen.api.shop

import com.ramenlog.ramen.api.application.favorite.FavoriteService
import com.ramenlog.ramen.api.application.review.ReviewService
import com.ramenlog.ramen.api.application.shop.RestaurantService
import com.ramenlog.ramen.api.application.visit.VisitHistoryService
import com.ramenlog.ramen.api.favorite.dto.FavoriteToggleResponse
import com.ramenlog.ramen.api.review.dto.RestaurantReviewsResponse
import com.ramenlog.ramen.api.shop.dto.NearbyRestaurantsResponse
import com.ramenlog.ramen.api.shop.dto.RestaurantDetailResponse
import com.ramenlog.ramen.api.visit.dto.RestaurantVisitHistoryResponse
import com.ramenlog.ramen.domain.user.User
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/restaurants")
class RestaurantController (
    private val restaurantService: RestaurantService,
    private val reviewService: ReviewService,
    private val visitHistoryService: VisitHistoryService,
    private val favoriteService: FavoriteService
){

    @GetMapping("/nearby")
    fun searchNearbyShops(
        @RequestParam latitude: Double,
        @RequestParam longitude: Double
    ): ResponseEntity<NearbyRestaurantsResponse>{
        val response = restaurantService.searchNearbyRamenShops(latitude, longitude)

        return ResponseEntity.ok(response)
    }

    @GetMapping("/{restaurantId}")
    fun getRestaurantDetail(
        @PathVariable restaurantId: UUID
    ): ResponseEntity<RestaurantDetailResponse> {

        val response =
            restaurantService.getRestaurantDetail(
                restaurantId
            )

        return ResponseEntity.ok(response)
    }

    @GetMapping("/{restaurantId}/reviews")
    fun getRestaurantReviews(
        @PathVariable restaurantId: UUID,
        @RequestParam(defaultValue = "latest")
        sort: String,
        @RequestParam(defaultValue = "1")
        page: Int,
        @RequestParam(defaultValue = "20")
        size: Int
    ): ResponseEntity<RestaurantReviewsResponse>{
        val response = reviewService.getRestaurantReviews(
            restaurantId,
            sort,
            page,
            size
        )

        return ResponseEntity.ok(response)
    }

    @GetMapping(
        "/{restaurantId}/visit-history/me"
    )
    fun getRestaurantVisitHistory(
        @PathVariable restaurantId: UUID,
        @AuthenticationPrincipal user: User
    ): ResponseEntity<RestaurantVisitHistoryResponse> {

        val response =
            visitHistoryService
                .getRestaurantVisitHistory(
                    restaurantId,
                    user
                )

        return ResponseEntity.ok(
            response
        )
    }

    @GetMapping("/search")
    fun searchRestaurants(
        @RequestParam keyword:String,
        @RequestParam latitude: Double,
        @RequestParam longitude: Double
    ): ResponseEntity<NearbyRestaurantsResponse>{
        return ResponseEntity.ok(
            restaurantService.searchRestaurants(
                keyword,
                latitude,
                longitude
            )
        )
    }

    @PostMapping("/{restaurantId}/favorite")
    fun toggleFavorite(
        @AuthenticationPrincipal
        user: User,
        @PathVariable
        restaurantId: UUID
    ): FavoriteToggleResponse {
        return favoriteService.toggleFavorite(
            user,
            restaurantId
        )
    }


}