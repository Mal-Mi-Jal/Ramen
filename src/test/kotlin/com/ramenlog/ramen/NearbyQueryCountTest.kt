package com.ramenlog.ramen

import com.ramenlog.ramen.api.application.shop.RestaurantService
import com.ramenlog.ramen.domain.favorite.FavoriteRestaurantRepository
import com.ramenlog.ramen.domain.restaurant.Restaurant
import com.ramenlog.ramen.domain.restaurant.RestaurantRepository
import com.ramenlog.ramen.domain.review.Review
import com.ramenlog.ramen.domain.review.ReviewRepository
import com.ramenlog.ramen.domain.user.User
import com.ramenlog.ramen.domain.visit.VisitVerification
import jakarta.persistence.EntityManager
import org.hibernate.SessionFactory
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import java.time.LocalDateTime

/**
 * 주변 라멘집 조회(searchNearbyRamenShops)가 DB에 쿼리를 몇 번 보내는지 센다.
 * 메모리 DB(H2)에 가게 31곳(20km 안)과 가게마다 리뷰 2개를 넣고,
 * Hibernate 통계(prepareStatementCount)로 실행된 SQL 수를 잰다.
 */
@DataJpaTest(
    properties = [
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false",
        "spring.jpa.properties.hibernate.generate_statistics=true",
    ]
)
class NearbyQueryCountTest {

    @Autowired lateinit var em: EntityManager
    @Autowired lateinit var restaurantRepository: RestaurantRepository
    @Autowired lateinit var reviewRepository: ReviewRepository
    @Autowired lateinit var favoriteRestaurantRepository: FavoriteRestaurantRepository

    // 서울시청 근처 기준점
    private val baseLat = 37.5665
    private val baseLng = 126.9780

    @Test
    fun `주변 가게 조회 쿼리 수와 리뷰 집계 값`() {
        val user = User(email = "a@test.com", passwordHash = "x", nickname = "tester")
        em.persist(user)

        // 31곳: i번째 가게는 북쪽으로 i * 100m 쯤 (모두 20km 안) → 가까운 30곳만 응답에 들어감
        repeat(31) { i ->
            val r = Restaurant(
                kakaoPlaceId = "place-$i",
                name = "라멘집 $i",
                address = "서울",
                latitude = baseLat + i * 0.0009,
                longitude = baseLng,
            )
            em.persist(r)
            // 3번 가게는 리뷰가 없음 → GROUP BY 결과에 안 나오므로 0개, 0.0으로 나와야 함
            if (i == NO_REVIEW_SHOP) return@repeat
            // 가게마다 리뷰 2개: 별점 (i % 5) + 1 과 5 → 평균 = ((i % 5) + 1 + 5) / 2
            listOf((i % 5) + 1, 5).forEachIndexed { k, rating ->
                val v = VisitVerification(
                    user = user,
                    restaurant = r,
                    enteredAt = LocalDateTime.of(2026, 1, 1, 12, 0).plusMinutes((i * 10 + k).toLong()),
                )
                em.persist(v)
                em.persist(
                    Review(
                        user = user, restaurant = r, verification = v,
                        rating = rating, revisitIntention = true,
                    )
                )
            }
        }
        em.flush()
        em.clear() // 1차 캐시를 비워서 실제 조회처럼 DB에 다시 묻게 함

        val stats = em.entityManagerFactory.unwrap(SessionFactory::class.java).statistics
        stats.clear()

        val service = RestaurantService(
            "test-key", restaurantRepository, reviewRepository, favoriteRestaurantRepository
        )
        val result = service.searchNearbyRamenShops(baseLat, baseLng).data.restaurants

        val queries = stats.prepareStatementCount
        println("[측정] 주변 가게 조회 쿼리 수 = $queries (응답 가게 ${result.size}곳)")

        // 값이 바뀌지 않았는지 확인 (고치기 전과 후 모두 같아야 함)
        assertEquals(30, result.size)
        assertEquals("라멘집 0", result.first().name)
        result.forEach { r ->
            val i = r.name.removePrefix("라멘집 ").toInt()
            if (i == NO_REVIEW_SHOP) {
                assertEquals(0, r.reviewCount, "리뷰 없는 가게 리뷰 수")
                assertEquals(0.0, r.averageRating, "리뷰 없는 가게 평균 별점")
                return@forEach
            }
            assertEquals(2, r.reviewCount, "${r.name} 리뷰 수")
            assertEquals(((i % 5) + 1 + 5) / 2.0, r.averageRating, 1e-9, "${r.name} 평균 별점")
        }

        assertEquals(EXPECTED_QUERIES, queries, "주변 가게 조회 쿼리 수")
    }

    companion object {
        // 고치기 전: findAll 1 + 30곳 × (리뷰 수 1 + 평균 1) = 61
        // 고친 뒤: findAll 1 + 리뷰 집계(IN + GROUP BY) 1 = 2
        const val EXPECTED_QUERIES = 2L
        const val NO_REVIEW_SHOP = 3
    }
}
