package com.ramenlog.ramen.domain.review

import java.util.UUID

/**
 * 가게별 리뷰 집계 결과 (ReviewRepository.getReviewStatsByRestaurantIds 의 한 줄).
 * Spring Data JPA 인터페이스 프로젝션: 쿼리의 별칭(as ...)과 프로퍼티 이름이 같아야 한다.
 */
interface RestaurantReviewStats {
    val restaurantId: UUID
    val reviewCount: Long
    val averageRating: Double?
}
