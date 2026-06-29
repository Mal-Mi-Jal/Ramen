package com.ramenlog.ramen.api.application.shop

import com.ramenlog.ramen.api.shop.dto.KakaoPlaceDocument
import com.ramenlog.ramen.api.shop.dto.KakaoPlaceResponse
import com.ramenlog.ramen.api.shop.dto.NearbyRestaurantsResponse
import com.ramenlog.ramen.api.shop.dto.RestaurantData
import com.ramenlog.ramen.api.shop.dto.RestaurantDetailData
import com.ramenlog.ramen.api.shop.dto.RestaurantDetailResponse
import com.ramenlog.ramen.api.shop.dto.RestaurantResponse
import com.ramenlog.ramen.domain.restaurant.Restaurant
import com.ramenlog.ramen.domain.restaurant.RestaurantRepository
import com.ramenlog.ramen.domain.review.ReviewRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import java.util.UUID

@Service
class RestaurantService (
    @Value("\${kakao.api.key}")
    private val kakaoApiKey: String,
    private val restaurantRepository: RestaurantRepository,
    private val reviewRepository: ReviewRepository
){
    private val webClient = WebClient.builder()
        .baseUrl("https://dapi.kakao.com")
        .build()

    fun searchNearbyRamenShops(latitude: Double, longitude: Double): NearbyRestaurantsResponse {
        val allDocuments = mutableListOf<KakaoPlaceDocument>()

        for(page in 1..3){

            val response = webClient.get()
                .uri { uriBuilder ->
                    uriBuilder
                        .path("/v2/local/search/keyword.json")
                        .queryParam("query", "라멘")
                        .queryParam("x", longitude.toString())
                        .queryParam("y", latitude.toString())
                        .queryParam("radius", 20000)
                        .queryParam("sort", "distance")
                        .queryParam("page" , page)
                        .build()
                }
                .header("Authorization", "KakaoAK ${kakaoApiKey.trim()}")
                .retrieve()
                .bodyToMono(KakaoPlaceResponse::class.java)
                .block()

            if(response == null) continue

            allDocuments.addAll(response.documents)

            if(response.meta.isEnd) {
                break
            }
        }

        val filteredDocuments = allDocuments
            .filter {
                it.categoryName?.contains("일본식라면") == true
            }
            .distinctBy { it.placeName }

        val restaurants = filteredDocuments.map { place ->

            val savedRestaurant =
                restaurantRepository.findByKakaoPlaceId(place.id)

                    ?: restaurantRepository.save(
                        Restaurant(
                            kakaoPlaceId = place.id,

                            name = place.placeName,

                            address =
                                if (place.roadAddressName.isNotBlank())
                                    place.roadAddressName
                                else
                                    place.addressName,

                            latitude = place.y.toDouble(),

                            longitude = place.x.toDouble(),

                            phone = place.phone
                        )
                    )

            val reviewCount =
                reviewRepository.countByRestaurantId(
                    savedRestaurant.id!!
                )

            val averageRating =
                reviewRepository.getAverageRating(
                    savedRestaurant.id!!
                ) ?: 0.0

            RestaurantResponse(
                id = savedRestaurant.id.toString(),

                kakaoPlaceId = savedRestaurant.kakaoPlaceId,

                name = savedRestaurant.name,

                address = savedRestaurant.address,

                latitude = savedRestaurant.latitude,

                longitude = savedRestaurant.longitude,

                phone = savedRestaurant.phone,

                distanceMeters = place.distance?.toIntOrNull(),

                reviewCount = reviewCount.toInt(),

                averageRating = averageRating
            )
        }

        return NearbyRestaurantsResponse(
            success = true,
            data = RestaurantData(
                restaurants = restaurants
            )
        )
    }

    fun getRestaurantDetail(
        restaurantId: UUID
    ): RestaurantDetailResponse {

        val restaurant =
            restaurantRepository.findById(restaurantId)
                .orElseThrow {
                    IllegalArgumentException("식당을 찾을 수 없습니다.")
                }

        val reviewCount =
            reviewRepository.countByRestaurantId(
                restaurant.id!!
            )

        val averageRating =
            reviewRepository.getAverageRating(
                restaurant.id!!
            ) ?: 0.0

        val revisitCount =
            reviewRepository
                .countByRestaurantIdAndRevisitIntentionTrue(
                    restaurant.id!!
                )

        val revisitRate =
            if (reviewCount == 0L) {
                0.0
            } else {
                revisitCount.toDouble() /
                        reviewCount.toDouble() * 100
            }

        return RestaurantDetailResponse(
            success = true,

            data = RestaurantDetailData(
                id = restaurant.id.toString(),

                kakaoPlaceId = restaurant.kakaoPlaceId,

                name = restaurant.name,

                address = restaurant.address,

                latitude = restaurant.latitude,

                longitude = restaurant.longitude,

                phone = restaurant.phone,

                reviewCount = reviewCount.toInt(),

                averageRating = averageRating,

                revisitRate = revisitRate
            )
        )
    }

    fun searchRestaurants(
        keyword: String
    ): NearbyRestaurantsResponse {

        val keywords = listOf(
            keyword,
            "$keyword 라멘",
            "$keyword 일본라멘"
        )

        val allDocuments = mutableListOf<KakaoPlaceDocument>()

        for (searchKeyword in keywords) {

            val response = webClient.get()
                .uri { uriBuilder ->
                    uriBuilder
                        .path("/v2/local/search/keyword.json")
                        .queryParam("query", searchKeyword)
                        .queryParam("page", 1)
                        .build()
                }
                .header("Authorization", "KakaoAK ${kakaoApiKey.trim()}")
                .retrieve()
                .bodyToMono(KakaoPlaceResponse::class.java)
                .block()

            if (response != null) {
                allDocuments.addAll(response.documents)
            }
        }

        val restaurants = allDocuments

            // 중복 제거
            .distinctBy { it.id }

            // 라멘 관련 업종만
            .filter {

                it.categoryName?.contains("일본식라면") == true ||

                        it.categoryName?.contains("일식") == true

            }

            .map { place ->

                val savedRestaurant =
                    restaurantRepository.findByKakaoPlaceId(place.id)

                        ?: restaurantRepository.save(
                            Restaurant(
                                kakaoPlaceId = place.id,
                                name = place.placeName,
                                address =
                                    if (place.roadAddressName.isNotBlank())
                                        place.roadAddressName
                                    else
                                        place.addressName,
                                latitude = place.y.toDouble(),
                                longitude = place.x.toDouble(),
                                phone = place.phone
                            )
                        )

                val reviewCount =
                    reviewRepository.countByRestaurantId(
                        savedRestaurant.id!!
                    )

                val averageRating =
                    reviewRepository.getAverageRating(
                        savedRestaurant.id!!
                    ) ?: 0.0

                RestaurantResponse(
                    id = savedRestaurant.id.toString(),
                    kakaoPlaceId = savedRestaurant.kakaoPlaceId,
                    name = savedRestaurant.name,
                    address = savedRestaurant.address,
                    latitude = savedRestaurant.latitude,
                    longitude = savedRestaurant.longitude,
                    phone = savedRestaurant.phone,
                    distanceMeters = null,
                    reviewCount = reviewCount.toInt(),
                    averageRating = averageRating
                )
            }

        return NearbyRestaurantsResponse(
            success = true,
            data = RestaurantData(
                restaurants = restaurants
            )
        )
    }
}