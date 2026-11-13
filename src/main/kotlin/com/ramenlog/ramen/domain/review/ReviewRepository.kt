package com.ramenlog.ramen.domain.review

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.UUID

interface ReviewRepository : JpaRepository<Review, UUID> {
    fun findByRestaurantId(
        restaurantId: UUID,
        pageable: Pageable
    ): Page<Review>

    fun countByRestaurantId(
        restaurantId: UUID
    ): Long

    fun countByRestaurantIdAndRevisitIntentionTrue(
        restaurantId: UUID
    ): Long

    @Query("""
        select avg(r.rating)
        from Review r
        where r.restaurant.id = :restaurantId
    """)
    fun getAverageRating(
        restaurantId: UUID
    ): Double?

    // 여러 가게의 리뷰 수와 평균 별점을 쿼리 한 번으로 모아 온다 (리뷰가 없는 가게는 결과에 없음)
    @Query("""
        select r.restaurant.id as restaurantId,
               count(r) as reviewCount,
               avg(r.rating) as averageRating
        from Review r
        where r.restaurant.id in :restaurantIds
        group by r.restaurant.id
    """)
    fun getReviewStatsByRestaurantIds(
        restaurantIds: Collection<UUID>
    ): List<RestaurantReviewStats>

    fun existsByVerificationId(verificationId: UUID): Boolean

    fun findByUserId(
        userId: UUID,
        pageable: Pageable
    ): Page<Review>

    fun countByUserId(
        userId: UUID
    ): Long

    @Query("""
        select coalesce(sum(r.likeCount), 0)
        from Review r
        where r.user.id = :userId
    """)
    fun getTotalLikeCountByUserId(userId: UUID): Int
}