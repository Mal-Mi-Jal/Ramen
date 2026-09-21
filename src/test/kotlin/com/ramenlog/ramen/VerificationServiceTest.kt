package com.ramenlog.ramen.api.application.verification

import com.ramenlog.ramen.api.common.OutOfRangeException
import com.ramenlog.ramen.api.verification.dto.VerificationEnterRequest
import com.ramenlog.ramen.api.verification.dto.VerificationVerifyRequest
import com.ramenlog.ramen.domain.restaurant.Restaurant
import com.ramenlog.ramen.domain.restaurant.RestaurantRepository
import com.ramenlog.ramen.domain.user.User
import com.ramenlog.ramen.domain.visit.VerificationStatus
import com.ramenlog.ramen.domain.visit.VisitHistoryRepository
import com.ramenlog.ramen.domain.visit.VisitVerification
import com.ramenlog.ramen.domain.visit.VisitVerificationRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.BDDMockito.given
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import java.time.LocalDateTime
import java.util.Optional
import java.util.UUID

/**
 * 방문 인증 로직(GPS 반경 5000m, 체류시간 requiredStaySeconds) 경계값 테스트.
 */
@ExtendWith(MockitoExtension::class)
class VerificationServiceTest {

    @Mock
    lateinit var visitVerificationRepository: VisitVerificationRepository

    @Mock
    lateinit var restaurantRepository: RestaurantRepository

    @Mock
    lateinit var visitHistoryRepository: VisitHistoryRepository

    private lateinit var verificationService: VerificationService

    private val requiredStaySeconds = 30 // application.yml의 verification.required-stay-seconds와 동일하게

    private lateinit var user: User
    private lateinit var restaurant: Restaurant

    @BeforeEach
    fun setUp() {
        verificationService = VerificationService(
            visitVerificationRepository,
            restaurantRepository,
            requiredStaySeconds,
            visitHistoryRepository
        )
        user = User(id = UUID.randomUUID(), email = "test@ramenlog.com", passwordHash = "x", nickname = "말미잘")
        restaurant = Restaurant(
            id = UUID.randomUUID(),
            kakaoPlaceId = "test-place-id",
            name = "라멘집",
            address = "서울시 어딘가",
            latitude = 37.5665,
            longitude = 126.9780
        )

        // save()를 명시적으로 스텁 — inline-mock-maker가 제네릭 브릿지 메서드를
        // 스텁 없이 호출할 때 생기는 문제를 피하기 위함
        org.mockito.Mockito.lenient().`when`(visitVerificationRepository.save(any())).thenAnswer { it.arguments[0] }
        org.mockito.Mockito.lenient().`when`(visitHistoryRepository.save(any())).thenAnswer { it.arguments[0] }
    }

    // ---------- 반경(distance) 경계값 ----------

    @Test
    fun `반경 5000m를 초과하면 OutOfRangeException이 발생한다`() {
        given(restaurantRepository.findById(any())).willReturn(Optional.of(restaurant))
        // OutOfRangeException이 findByUserAndStatus 호출 전에 던져지므로 그 스텁은 불필요

        // 서울(37.5665, 126.9780) 기준으로 5000m를 훌쩍 넘는 좌표
        val request = VerificationEnterRequest(
            restaurantId = restaurant.id!!,
            latitude = 37.60,
            longitude = 127.05
        )

        assertThrows(OutOfRangeException::class.java) {
            verificationService.startVerification(user, request)
        }
    }

    @Test
    fun `반경 5000m 이내면 방문 인증이 정상 시작된다`() {
        given(restaurantRepository.findById(any())).willReturn(Optional.of(restaurant))
        given(visitVerificationRepository.findByUserAndStatus(any(), any())).willReturn(null)
        given(visitVerificationRepository.save(any())).willAnswer { it.arguments[0] }

        val request = VerificationEnterRequest(
            restaurantId = restaurant.id!!,
            latitude = restaurant.latitude,
            longitude = restaurant.longitude
        )

        val response = verificationService.startVerification(user, request)

        assertTrue(response.success)
        assertEquals("pending", response.data.status)
    }

    // ---------- 체류시간(stay duration) 경계값 ----------

    @Test
    fun `체류시간이 기준보다 부족하면 INSUFFICIENT_STAY_TIME 에러를 반환한다`() {
        val verification = VisitVerification(
            user = user,
            restaurant = restaurant,
            enteredAt = LocalDateTime.now().minusSeconds((requiredStaySeconds - 1).toLong()),
            status = VerificationStatus.PENDING
        )
        given(visitVerificationRepository.findById(any())).willReturn(Optional.of(verification))

        val request = VerificationVerifyRequest(
            latitude = restaurant.latitude,
            longitude = restaurant.longitude
        )

        val response = verificationService.verifyVerification(UUID.randomUUID(), user, request)

        assertFalse(response.success)
        assertEquals("INSUFFICIENT_STAY_TIME", response.error?.code)
    }

    @Test
    fun `체류시간이 기준을 넘기면 방문 인증이 완료 처리되고 방문 기록이 남는다`() {
        val verification = VisitVerification(
            user = user,
            restaurant = restaurant,
            enteredAt = LocalDateTime.now().minusSeconds((requiredStaySeconds + 1).toLong()),
            status = VerificationStatus.PENDING
        )
        given(visitVerificationRepository.findById(any())).willReturn(Optional.of(verification))
        given(visitHistoryRepository.findByUserIdAndRestaurantId(any(), any())).willReturn(null)

        val request = VerificationVerifyRequest(
            latitude = restaurant.latitude,
            longitude = restaurant.longitude
        )

        val response = verificationService.verifyVerification(UUID.randomUUID(), user, request)

        assertTrue(response.success)
        assertEquals("verified", response.data?.status)
        verify(visitHistoryRepository).save(any())
    }

    @Test
    fun `본인 것이 아닌 인증 건을 검증하려 하면 예외가 발생`() {
        val otherUser = User(email = "other@ramenlog.com", passwordHash = "x", nickname = "다른유저")
        val verification = VisitVerification(
            user = otherUser,
            restaurant = restaurant,
            enteredAt = LocalDateTime.now(),
            status = VerificationStatus.PENDING
        )
        given(visitVerificationRepository.findById(any())).willReturn(Optional.of(verification))

        val request = VerificationVerifyRequest(
            latitude = restaurant.latitude,
            longitude = restaurant.longitude
        )

        assertThrows(IllegalArgumentException::class.java) {
            verificationService.verifyVerification(UUID.randomUUID(), user, request)
        }
    }
}
