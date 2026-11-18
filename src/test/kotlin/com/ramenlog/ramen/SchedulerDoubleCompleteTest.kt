package com.ramenlog.ramen

import com.ramenlog.ramen.api.application.verification.VerificationScheduler
import com.ramenlog.ramen.api.application.verification.VisitCompletion
import com.ramenlog.ramen.domain.restaurant.Restaurant
import com.ramenlog.ramen.domain.user.User
import com.ramenlog.ramen.domain.visit.VerificationStatus
import com.ramenlog.ramen.domain.visit.VisitHistoryRepository
import com.ramenlog.ramen.domain.visit.VisitVerification
import com.ramenlog.ramen.domain.visit.VisitVerificationRepository
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionTemplate
import java.time.LocalDateTime
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

/**
 * 서버 2대가 같은 PENDING 인증을 동시에 자동 완료하는 상황을 재현한다.
 *
 * 순서를 일부러 고정한다 (실제로도 5초마다 도는 스케줄러 두 개면 일어날 수 있는 순서):
 *   1. 서버 B가 PENDING 목록을 읽는다 (아직 아무도 완료 안 함)
 *   2. 서버 A가 목록을 읽고, 완료 + 방문 기록 1회 저장까지 커밋한다
 *   3. 서버 B가 이어서 자기가 읽어 둔 (이미 끝난) 인증을 또 완료한다
 * 고치기 전: B가 방문 기록을 한 번 더 올려서 visitCount = 2 (방문 1번인데 2번으로 기록)
 * 고친 뒤:  B의 조건부 UPDATE가 0행을 바꾸므로 B는 방문 기록을 건드리지 않음 → 1
 */
@DataJpaTest(
    properties = [
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false",
    ]
)
// 테스트 전체를 하나의 트랜잭션으로 묶지 않음: 두 "서버"가 각자 커밋해야 서로의 결과가 보인다
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class SchedulerDoubleCompleteTest {

    @Autowired lateinit var em: EntityManager
    @Autowired lateinit var txManager: PlatformTransactionManager
    @Autowired lateinit var verificationRepository: VisitVerificationRepository
    @Autowired lateinit var historyRepository: VisitHistoryRepository

    private val requiredStaySeconds = 1200

    /** 목록을 읽은 직후 신호를 보내고, 다른 서버가 커밋할 때까지 기다리는 저장소 (서버 B용) */
    class PausingRepository(
        private val real: VisitVerificationRepository,
        private val afterRead: CountDownLatch,
        private val resume: CountDownLatch,
    ) : VisitVerificationRepository by real {
        override fun findAllByStatus(status: VerificationStatus): List<VisitVerification> {
            val list = real.findAllByStatus(status)
            afterRead.countDown()
            check(resume.await(10, TimeUnit.SECONDS)) { "서버 A가 끝나지 않음" }
            return list
        }
    }

    @Test
    fun `서버 2대가 같은 인증을 완료해도 방문 기록은 1번만 올라간다`() {
        val tx = TransactionTemplate(txManager)
        val (userId, restaurantId) = tx.execute {
            val user = User(email = "double@test.com", passwordHash = "x", nickname = "tester")
            val shop = Restaurant(
                kakaoPlaceId = "place-double", name = "라멘집", address = "서울",
                latitude = 37.5665, longitude = 126.9780,
            )
            em.persist(user)
            em.persist(shop)
            // 30분 전에 들어옴 → 기준 20분이 지나서 자동 완료 대상
            em.persist(VisitVerification(user = user, restaurant = shop, enteredAt = LocalDateTime.now().minusMinutes(30)))
            user.id!! to shop.id!!
        }!!

        val bRead = CountDownLatch(1)
        val aCommitted = CountDownLatch(1)
        val serverA = VerificationScheduler(verificationRepository, VisitCompletion(verificationRepository, historyRepository, requiredStaySeconds))
        val serverB = VerificationScheduler(
            PausingRepository(verificationRepository, bRead, aCommitted),
            VisitCompletion(verificationRepository, historyRepository, requiredStaySeconds),
        )

        // 직접 만든 객체라 @Transactional 프록시가 없으므로, 실제 스케줄러처럼 한 번 실행 = 트랜잭션 하나로 감쌈
        var errorInB: Throwable? = null
        val threadB = thread(name = "server-B") {
            try {
                tx.executeWithoutResult { serverB.autoCompleteExpiredVerifications() }
            } catch (e: Throwable) {
                errorInB = e
            }
        }
        check(bRead.await(10, TimeUnit.SECONDS)) { "서버 B가 목록을 읽지 못함" }
        tx.executeWithoutResult { serverA.autoCompleteExpiredVerifications() }
        aCommitted.countDown()
        threadB.join(10_000)
        errorInB?.let { throw it }

        val visitCount = tx.execute {
            historyRepository.findByUserIdAndRestaurantId(userId, restaurantId)!!.visitCount
        }
        println("[측정] 서버 2대 실행 후 visitCount = $visitCount (방문 1번)")
        assertEquals(1, visitCount, "방문 1번인데 방문 기록이 두 번 올라감")

        val verified = tx.execute { verificationRepository.findAllByStatus(VerificationStatus.VERIFIED).size }
        assertEquals(1, verified)
    }
}
