package com.ramenlog.ramen

import com.ramenlog.ramen.api.application.verification.VerificationService
import com.ramenlog.ramen.api.application.verification.VisitCompletion
import com.ramenlog.ramen.api.verification.dto.VerificationEnterRequest
import com.ramenlog.ramen.domain.restaurant.Restaurant
import com.ramenlog.ramen.domain.restaurant.RestaurantRepository
import com.ramenlog.ramen.domain.user.User
import com.ramenlog.ramen.domain.user.UserRepository
import com.ramenlog.ramen.domain.visit.VerificationStatus
import com.ramenlog.ramen.domain.visit.VisitHistoryRepository
import com.ramenlog.ramen.domain.visit.VisitVerification
import com.ramenlog.ramen.domain.visit.VisitVerificationRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.kotlin.argThat
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionTemplate
import java.time.LocalDateTime

/**
 * startVerification(새 인증 시작)이 "끝난 예전 인증 정리 + 새 인증 저장"을 한 트랜잭션으로 하는지 본다.
 * 진짜 Spring 빈(트랜잭션 프록시 포함)으로 돌려야 보이는 문제라 Mockito 단위 테스트가 아니라 H2로 확인한다.
 *
 * 예전 코드: startVerification에는 @Transactional이 없고, 안에서 this.autoCompleteVerification(@Transactional)을
 * 불러서 프록시를 안 거침 → save()가 저마다 바로 커밋 + 자동 완료 쪽은 방문 기록을 안 남김.
 */
@DataJpaTest(
    properties = [
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false",
        "verification.required-stay-seconds=1200",
        "verification.radius-meters=50",
    ]
)
// 스케줄러는 일부러 안 넣음 (@EnableScheduling 때문에 5초마다 돌면서 결과를 바꿀 수 있음)
@Import(VerificationService::class, VisitCompletion::class)
@Transactional(propagation = Propagation.NOT_SUPPORTED) // 실제 요청처럼 서비스가 스스로 커밋·롤백하게 둔다
class StartVerificationTransactionTest {

    @Autowired lateinit var verificationService: VerificationService
    @Autowired lateinit var txManager: PlatformTransactionManager
    @Autowired lateinit var userRepository: UserRepository
    @Autowired lateinit var restaurantRepository: RestaurantRepository
    @Autowired lateinit var historyRepository: VisitHistoryRepository
    @MockitoSpyBean lateinit var verificationRepository: VisitVerificationRepository

    private val tx by lazy { TransactionTemplate(txManager) }

    @AfterEach
    fun cleanUp() {
        historyRepository.deleteAll()
        verificationRepository.deleteAll()
        restaurantRepository.deleteAll()
        userRepository.deleteAll()
    }

    /** 30분 전에 들어와서 아직 PENDING인 예전 인증(가게 A)이 있는 사용자 + 새로 들어갈 가게 B */
    private fun setUpExpiredPending(): Triple<User, Restaurant, VisitVerification> {
        val user = userRepository.save(User(email = "tx@test.com", passwordHash = "x", nickname = "tester"))
        val shopA = restaurantRepository.save(Restaurant(kakaoPlaceId = "a", name = "가게A", address = "서울", latitude = 37.5665, longitude = 126.9780))
        val shopB = restaurantRepository.save(Restaurant(kakaoPlaceId = "b", name = "가게B", address = "서울", latitude = 37.5700, longitude = 126.9800))
        val old = verificationRepository.save(
            VisitVerification(user = user, restaurant = shopA, enteredAt = LocalDateTime.now().minusMinutes(30))
        )
        return Triple(user, shopB, old)
    }

    private fun enter(shop: Restaurant) = VerificationEnterRequest(
        restaurantId = shop.id!!, latitude = shop.latitude, longitude = shop.longitude,
    )

    private fun statusOf(v: VisitVerification) = tx.execute { verificationRepository.findById(v.id!!).get().status }

    @Test
    fun `새 인증을 시작하며 예전 인증을 자동 완료하면 방문 기록도 남는다`() {
        val (user, shopB, old) = setUpExpiredPending()

        verificationService.startVerification(user, enter(shopB))

        assertEquals(VerificationStatus.VERIFIED, statusOf(old))
        val history = tx.execute { historyRepository.findByUserIdAndRestaurantId(user.id!!, old.restaurant.id!!) }
        assertEquals(1, history?.visitCount, "예전 인증은 완료됐는데 방문 기록이 없음")
    }

    @Test
    fun `새 인증 저장이 실패하면 예전 인증 완료도 함께 되돌린다`() {
        val (user, shopB, old) = setUpExpiredPending()
        // 새 인증(id 없는 PENDING)을 저장할 때만 DB 오류가 났다고 가정
        doThrow(RuntimeException("DB 오류(테스트)"))
            .whenever(verificationRepository).save(argThat<VisitVerification> { id == null })

        assertThrows(RuntimeException::class.java) {
            verificationService.startVerification(user, enter(shopB))
        }

        assertEquals(VerificationStatus.PENDING, statusOf(old), "새 인증은 실패했는데 예전 인증 완료만 커밋됨")
    }
}
