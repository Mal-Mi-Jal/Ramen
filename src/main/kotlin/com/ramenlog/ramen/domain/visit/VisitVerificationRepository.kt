package com.ramenlog.ramen.domain.visit

import com.ramenlog.ramen.domain.user.User
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime
import java.util.UUID

interface VisitVerificationRepository : JpaRepository<VisitVerification, UUID>{

    fun findByUser(user: User, pageable: Pageable): Page<VisitVerification>

    fun findByUserAndStatus(user: User, status: VerificationStatus, pageable: Pageable): Page<VisitVerification>

    fun findByUserAndStatus(user: User, status: VerificationStatus): VisitVerification?

    fun findAllByStatus(status: VerificationStatus): List<VisitVerification>

    @EntityGraph(attributePaths = ["restaurant"])
    fun findByUserIdAndStatusOrderByVerifiedAtDesc(
        userId: UUID,
        status: VerificationStatus
    ): List<VisitVerification>

    @EntityGraph(attributePaths = ["restaurant"])
    fun findByUserIdAndStatusOrderByEnteredAtDesc(
        userId: UUID,
        status: VerificationStatus
    ): List<VisitVerification>

    fun findTopByUserOrderByEnteredAtDesc(
        user: User
    ): VisitVerification?

    /**
     * 아직 PENDING일 때만 VERIFIED로 바꾼다. 바뀐 행 수를 돌려준다 (1 = 내가 완료, 0 = 이미 누가 완료함).
     * 서버가 여러 대여도 DB가 이 행을 잠그고 WHERE를 다시 확인하므로 한 곳만 1을 받는다.
     */
    @Modifying
    @Query(
        """
        update VisitVerification v
           set v.status = com.ramenlog.ramen.domain.visit.VerificationStatus.VERIFIED,
               v.verifiedAt = :verifiedAt,
               v.stayDurationSeconds = :staySeconds
         where v.id = :id
           and v.status = com.ramenlog.ramen.domain.visit.VerificationStatus.PENDING
        """
    )
    fun completeIfPending(
        @Param("id") id: UUID,
        @Param("verifiedAt") verifiedAt: LocalDateTime,
        @Param("staySeconds") staySeconds: Int,
    ): Int
}
