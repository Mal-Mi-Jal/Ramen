package com.ramenlog.ramen.api.application.verification

import com.ramenlog.ramen.domain.visit.VerificationStatus
import com.ramenlog.ramen.domain.visit.VisitVerificationRepository
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Component
class VerificationScheduler(
    private val visitVerificationRepository: VisitVerificationRepository,
    private val visitCompletion: VisitCompletion,
) {
    // PENDING 인증들을 체크해서 시간 지난 것들을 자동으로 VERIFIED로 변경 (+ 방문 기록)
    @Scheduled(fixedDelay = 5000)
    @Transactional
    fun autoCompleteExpiredVerifications() {
        val now = LocalDateTime.now()
        visitVerificationRepository.findAllByStatus(VerificationStatus.PENDING)
            .forEach { visitCompletion.completeIfExpired(it, now) }
    }
}
