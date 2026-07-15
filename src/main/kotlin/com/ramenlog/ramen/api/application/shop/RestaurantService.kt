package com.ramenlog.ramen.api.application.shop

import com.ramenlog.ramen.api.shop.dto.KakaoPlaceDocument
import com.ramenlog.ramen.api.shop.dto.KakaoPlaceResponse
import com.ramenlog.ramen.api.shop.dto.NearbyRestaurantsResponse
import com.ramenlog.ramen.api.shop.dto.RestaurantData
import com.ramenlog.ramen.api.shop.dto.RestaurantDetailData
import com.ramenlog.ramen.api.shop.dto.RestaurantDetailResponse
import com.ramenlog.ramen.api.shop.dto.RestaurantResponse
import com.ramenlog.ramen.domain.favorite.FavoriteRestaurantRepository
import com.ramenlog.ramen.domain.restaurant.Restaurant
import com.ramenlog.ramen.domain.restaurant.RestaurantRepository
import com.ramenlog.ramen.domain.review.ReviewRepository
import com.ramenlog.ramen.domain.user.User
import com.ramenlog.ramen.util.LocationUtils
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import java.util.UUID

@Service
class RestaurantService (
    @Value("\${kakao.api.key}")
    private val kakaoApiKey: String,
    private val restaurantRepository: RestaurantRepository,
    private val reviewRepository: ReviewRepository,
    private val favoriteRestaurantRepository: FavoriteRestaurantRepository
){
    private val webClient = WebClient.builder()
        .baseUrl("https://dapi.kakao.com")
        .build()

    fun searchNearbyRamenShops(latitude: Double, longitude: Double): NearbyRestaurantsResponse {
        println("========== GPS ==========")
        println("현재 위치 : $latitude , $longitude")
        println("=========================")
        val searchKeywords = listOf("라멘", "라면", "ramen", "일본라면")
        val allDocuments = mutableListOf<KakaoPlaceDocument>()

        for (keyword in searchKeywords) {
            for (page in 1..3) {
                val response = webClient.get()
                    .uri { uriBuilder ->
                        uriBuilder
                            .path("/v2/local/search/keyword.json")
                            .queryParam("query", keyword)
                            .queryParam("x", longitude.toString())
                            .queryParam("y", latitude.toString())
                            .queryParam("radius", 20000)
                            .queryParam("sort", "distance")
                            .queryParam("page", page)
                            .build()
                    }
                    .header("Authorization", "KakaoAK ${kakaoApiKey.trim()}")
                    .retrieve()
                    .bodyToMono(KakaoPlaceResponse::class.java)
                    .block()

                if (response == null) continue
                println("키워드: $keyword, 페이지: $page, 결과: ${response.documents.size}개")
                response.documents.forEach { println("  ${it.placeName} | ${it.categoryName}") }
                allDocuments.addAll(response.documents)
                if (response.meta.isEnd) break
            }
        }

        println("전체 문서 수: ${allDocuments.size}")
        println("중복 제거 후: ${allDocuments.distinctBy { it.id }.size}")

        val filteredDocuments = allDocuments
            .filter {
                val category = it.categoryName ?: ""
                category.contains("일본식라면") ||
                        category.contains("라멘") ||
                        (category.contains("일식") && it.placeName.contains("라멘", ignoreCase = true)) ||
                        (category.contains("일식") && it.placeName.contains("라면", ignoreCase = true))
            }
            .distinctBy { it.id }

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

            val distance =
                LocationUtils.calculateDistance(
                    latitude,
                    longitude,
                    savedRestaurant.latitude,
                    savedRestaurant.longitude
                ).toInt()

            println("${savedRestaurant.name} -> ${distance}m")

            RestaurantResponse(
                id = savedRestaurant.id.toString(),

                kakaoPlaceId = savedRestaurant.kakaoPlaceId,

                name = savedRestaurant.name,

                address = savedRestaurant.address,

                latitude = savedRestaurant.latitude,

                longitude = savedRestaurant.longitude,

                phone = savedRestaurant.phone,

                distanceMeters = distance,

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

    private fun currentUserOrNull(): User? {

        val authentication =
            SecurityContextHolder.getContext().authentication

        if (
            authentication == null ||
            !authentication.isAuthenticated
        ) {
            return null
        }

        return authentication.principal as? User
    }

    fun getRestaurantDetail(
        restaurantId: UUID
    ): RestaurantDetailResponse {
        val user = currentUserOrNull()

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

        val isFavorite =
            if(user == null){
                false
            }else{
                favoriteRestaurantRepository.existsByUserIdAndRestaurantId(
                    user.id!!,
                    restaurant.id!!
                )
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

                revisitRate = revisitRate,

                isFavorite = isFavorite
            )
        )
    }

    fun searchRestaurants(
        keyword: String,
        latitude: Double,
        longitude: Double
    ): NearbyRestaurantsResponse {

        if (keyword.trim().length < 2) {
            return NearbyRestaurantsResponse(
                success = true,
                data = RestaurantData(
                    restaurants = emptyList()
                )
            )
        }

        val dbRestaurants =
            restaurantRepository.findByNameContainingIgnoreCase(keyword)

        val dbResults = dbRestaurants.map { restaurant ->

            val reviewCount =
                reviewRepository.countByRestaurantId(
                    restaurant.id!!
                )

            val averageRating =
                reviewRepository.getAverageRating(
                    restaurant.id!!
                ) ?: 0.0

            val distance =
                LocationUtils.calculateDistance(
                    latitude,
                    longitude,
                    restaurant.latitude,
                    restaurant.longitude
                ).toInt()

            RestaurantResponse(
                id = restaurant.id.toString(),
                kakaoPlaceId = restaurant.kakaoPlaceId,
                name = restaurant.name,
                address = restaurant.address,
                latitude = restaurant.latitude,
                longitude = restaurant.longitude,
                phone = restaurant.phone,
                distanceMeters = distance,
                reviewCount = reviewCount.toInt(),
                averageRating = averageRating
            )
        }

        // 💡 이미 "라멘"이 포함된 검색어는 중복 키워드를 만들지 않음 (불필요한 API 호출 방지)
        val keywords = if (keyword.contains("라멘")) {
            listOf(keyword)
        } else {
            listOf(keyword, "$keyword 라멘", "$keyword 일본라멘")
        }

        val allDocuments = mutableListOf<KakaoPlaceDocument>()

        for (searchKeyword in keywords) {

            for (page in 1..3) {

                val response = webClient.get()
                    .uri { uriBuilder ->
                        uriBuilder
                            .path("/v2/local/search/keyword.json")
                            .queryParam("query", searchKeyword)
                            .queryParam("x", longitude)
                            .queryParam("y", latitude)
                            .queryParam("sort", "distance")
                            .queryParam("page", page)
                            .build()
                    }
                    .header("Authorization", "KakaoAK ${kakaoApiKey.trim()}")
                    .retrieve()
                    .bodyToMono(KakaoPlaceResponse::class.java)
                    .block()

                if (response != null) {
                    allDocuments.addAll(response.documents)

                    if (response.meta.isEnd) {
                        break
                    }
                }
            }
        }

        val restaurants = allDocuments

            // 중복 제거
            .distinctBy { it.id }

            // 라멘 관련 업종만 (카테고리 경로의 마지막 분류로 정확히 일치 비교)
            .filter {
                val lastCategory = it.categoryName?.split(">")?.lastOrNull()?.trim()
                lastCategory == "일본식라면" &&
                        (
                                it.placeName.contains(keyword, ignoreCase = true) ||
                                        it.placeName.replace(" ", "")
                                            .contains(keyword.replace(" ", ""), ignoreCase = true)
                                )
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

                val distance =
                    LocationUtils.calculateDistance(
                        latitude,
                        longitude,
                        savedRestaurant.latitude,
                        savedRestaurant.longitude
                    ).toInt()

                RestaurantResponse(
                    id = savedRestaurant.id.toString(),
                    kakaoPlaceId = savedRestaurant.kakaoPlaceId,
                    name = savedRestaurant.name,
                    address = savedRestaurant.address,
                    latitude = savedRestaurant.latitude,
                    longitude = savedRestaurant.longitude,
                    phone = savedRestaurant.phone,
                    distanceMeters = distance,
                    reviewCount = reviewCount.toInt(),
                    averageRating = averageRating
                )
            }

        val mergedRestaurants =
            (dbResults + restaurants)
                .distinctBy { it.kakaoPlaceId }
                .sortedWith(
                    compareByDescending<RestaurantResponse> {
                        it.name.contains(keyword, ignoreCase = true)
                    }.thenBy {
                        it.distanceMeters ?: Int.MAX_VALUE
                    }
                )

        return NearbyRestaurantsResponse(
            success = true,
            data = RestaurantData(
                restaurants = mergedRestaurants
            )
        )
    }


}