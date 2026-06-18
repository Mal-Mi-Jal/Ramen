package com.ramenlog.ramen.api.shop

import com.ramenlog.ramen.api.application.review.ReviewService
import com.ramenlog.ramen.api.application.shop.RamenShopService
import com.ramenlog.ramen.api.review.dto.RestaurantReviewsResponse
import com.ramenlog.ramen.api.shop.dto.KakaoPlaceResponse
import com.ramenlog.ramen.api.shop.dto.NearbyRestaurantsResponse
import com.ramenlog.ramen.api.shop.dto.RestaurantDetailResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/restaurants")
class RamenShopController (
    private val ramenShopService: RamenShopService,
    private val reviewService: ReviewService
){

    @GetMapping("/nearby")
    fun searchNearbyShops(
        @RequestParam latitude: String,
        @RequestParam longitude: String
    ): ResponseEntity<NearbyRestaurantsResponse>{
        val response = ramenShopService.searchNearbyRamenShops(latitude, longitude)

        return ResponseEntity.ok(response)
    }

    @GetMapping("/{restaurantId}")
    fun getRestaurantDetail(
        @PathVariable restaurantId: UUID
    ): ResponseEntity<RestaurantDetailResponse> {

        val response =
            ramenShopService.getRestaurantDetail(
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

}