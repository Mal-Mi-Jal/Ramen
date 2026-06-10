package com.ramenlog.ramen.domain.restaurant

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface RestaurantRepository : JpaRepository<Restaurant, UUID> {
    fun findByKakaoPlaceId(kakaoPlaceId: String) : Restaurant?
}