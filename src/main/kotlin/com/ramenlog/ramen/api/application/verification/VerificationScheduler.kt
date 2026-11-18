package com.ramenlog.ramen.api.application.verification

import com.ramenlog.ramen.domain.visit.VerificationStatus
import com.ramenlog.ramen.domain.visit.VisitHistory
import com.ramenlog.ramen.domain.visit.VisitHistoryRepository
import com.ramenlog.ramen.domain.visit.VisitVerificationRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.LocalDateTime

@Component
class VerificationScheduler(
    private val visitVerificationRepository: VisitVerificationRepository,
    private val visitHistoryRepository: VisitHistoryRepository,

    @Value("\${verification.required-stay-seconds}")
    private val requiredStaySeconds: Int
) {
    // 💡 PENDING 인증들을 체크해서 시간 지난 것들을 자동으로 VERIFIED로 변경
    @Scheduled(fixedDelay = 5000)
    @Transactional
    fun autoCompleteExpiredVerifications() {
        val pendingVerifications =
            visitVerificationRepository.findAllByStatus(VerificationStatus.PENDING)

        val now = LocalDateTime.now()

        pendingVerifications.forEach { verification ->
            val elapsed = Duration.between(verification.enteredAt, now).seconds

            if (elapsed >= requiredStaySeconds) {
                // 서버가 여러 대면 같은 PENDING을 동시에 읽을 수 있다.
                // 조건부 UPDATE로 바꾸고, 내가 바꾼 경우(1행)에만 방문 기록을 올린다.
                val updated = visitVerificationRepository.completeIfPending(
                    id = verification.id!!,
                    verifiedAt = verification.enteredAt.plusSeconds(requiredStaySeconds.toLong()),
                    staySeconds = elapsed.toInt(),
                )
                if (updated == 0) return@forEach // 다른 서버가 먼저 완료함

                // 방문 기록 업데이트
                val history = visitHistoryRepository.findByUserIdAndRestaurantId(
                    verification.user.id!!,
                    verification.restaurant.id!!
                )
                if (history == null) {
                    visitHistoryRepository.save(
                        VisitHistory(
                            user = verification.user,
                            restaurant = verification.restaurant,
                            visitVerification = verification,
                            visitCount = 1,
                            lastVisitedAt = now
                        )
                    )
                } else {
                    history.visitCount += 1
                    history.lastVisitedAt = now
                    visitHistoryRepository.save(history)
                }
            }
        }
    }
}