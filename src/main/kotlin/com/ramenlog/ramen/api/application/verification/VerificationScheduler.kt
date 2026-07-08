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
    // 💡 30초마다 PENDING 인증들을 체크해서 시간 지난 것들을 자동으로 VERIFIED로 변경
    @Scheduled(fixedDelay = 30000)
    @Transactional
    fun autoCompleteExpiredVerifications() {
        val pendingVerifications =
            visitVerificationRepository.findAllByStatus(VerificationStatus.PENDING)

        val now = LocalDateTime.now()

        pendingVerifications.forEach { verification ->
            val elapsed = Duration.between(verification.enteredAt, now).seconds

            if (elapsed >= requiredStaySeconds) {
                verification.status = VerificationStatus.VERIFIED
                verification.verifiedAt = verification.enteredAt.plusSeconds(requiredStaySeconds.toLong())
                verification.stayDurationSeconds = elapsed.toInt()
                visitVerificationRepository.save(verification)

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
                }
            }
        }
    }
}