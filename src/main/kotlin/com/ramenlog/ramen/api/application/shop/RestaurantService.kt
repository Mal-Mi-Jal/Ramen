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
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import java.util.UUID

@Service
class RestaurantService (
    @Value("\${kakao.api.key}")
    private val kakaoApiKey: String,
    private val restaurantRepository: RestaurantRepository
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

            RestaurantResponse(
                id = savedRestaurant.id.toString(),

                kakaoPlaceId = savedRestaurant.kakaoPlaceId,

                name = savedRestaurant.name,

                address = savedRestaurant.address,

                latitude = savedRestaurant.latitude,

                longitude = savedRestaurant.longitude,

                phone = savedRestaurant.phone,

                distanceMeters = place.distance?.toIntOrNull(),

                reviewCount = 0,

                averageRating = 0.0
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

                reviewCount = 0,

                averageRating = 0.0,

                revisitRate = 0.0
            )
        )
    }
}