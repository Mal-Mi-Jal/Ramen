package com.ramenlog.ramen.domain.visit

import com.ramenlog.ramen.domain.user.User
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
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
}