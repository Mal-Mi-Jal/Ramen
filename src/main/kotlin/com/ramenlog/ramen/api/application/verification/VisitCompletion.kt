package com.ramenlog.ramen.api.application.verification

import com.ramenlog.ramen.domain.visit.VerificationStatus
import com.ramenlog.ramen.domain.visit.VisitHistory
import com.ramenlog.ramen.domain.visit.VisitHistoryRepository
import com.ramenlog.ramen.domain.visit.VisitVerification
import com.ramenlog.ramen.domain.visit.VisitVerificationRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.LocalDateTime

/**
 * 체류 시간이 지난 PENDING 인증을 자동 완료하는 단 한 곳.
 * 예전엔 스케줄러와 VerificationService에 같은 로직이 따로 있었고, 서비스 쪽은 방문 기록을 안 남겼다.
 * 별도 빈이라 다른 클래스에서 부르면 항상 프록시를 거친다 (자기 호출 문제 없음).
 */
@Component
class VisitCompletion(
    private val visitVerificationRepository: VisitVerificationRepository,
    private val visitHistoryRepository: VisitHistoryRepository,

    @Value("\${verification.required-stay-seconds}")
    private val requiredStaySeconds: Int,
) {
    /** 완료했으면 true. 아직 시간이 안 됐거나, 이미 다른 곳(다른 서버)에서 완료했으면 false. */
    @Transactional // 부르는 쪽에 트랜잭션이 있으면 거기에 합류, 없으면 여기서 새로 시작
    fun completeIfExpired(verification: VisitVerification, now: LocalDateTime = LocalDateTime.now()): Boolean {
        if (verification.status != VerificationStatus.PENDING) return false

        val elapsed = Duration.between(verification.enteredAt, now).seconds
        if (elapsed < requiredStaySeconds) return false

        val verifiedAt = verification.enteredAt.plusSeconds(requiredStaySeconds.toLong())
        // 조건부 UPDATE: 아직 PENDING일 때만 바뀜. 0이면 다른 서버가 먼저 완료한 것
        val updated = visitVerificationRepository.completeIfPending(verification.id!!, verifiedAt, elapsed.toInt())
        if (updated == 0) return false

        // 메모리 속 객체도 DB와 같게 맞춰 둠 (부르는 쪽이 이 객체를 계속 쓰므로)
        verification.status = VerificationStatus.VERIFIED
        verification.verifiedAt = verifiedAt
        verification.stayDurationSeconds = elapsed.toInt()

        val history = visitHistoryRepository.findByUserIdAndRestaurantId(
            verification.user.id!!, verification.restaurant.id!!
        )
        if (history == null) {
            visitHistoryRepository.save(
                VisitHistory(
                    user = verification.user,
                    restaurant = verification.restaurant,
                    visitVerification = verification,
                    visitCount = 1,
                    lastVisitedAt = now,
                )
            )
        } else {
            history.visitCount += 1
            history.lastVisitedAt = now
            visitHistoryRepository.save(history)
        }
        return true
    }
}
