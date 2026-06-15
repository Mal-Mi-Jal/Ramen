package com.ramenlog.ramen.domain.visit

import com.ramenlog.ramen.domain.restaurant.Restaurant
import com.ramenlog.ramen.domain.user.User
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.LocalDateTime
import java.util.UUID


@Entity
@Table(
    name = "visit_verifications",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_user_restaurant_visit",
            columnNames = ["user_id", "restaurant_id", "entered_at"]
        )
    ]
)
open class VisitVerification (
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "restaurant_id", nullable = false)
    val restaurant: Restaurant,

    @Column(name = "entered_at", nullable = false)
    var enteredAt: LocalDateTime,

    @Column(name = "verified_at")
    var verifiedAt: LocalDateTime? = null,

    @Column(name = "stay_duration_seconds")
    var stayDurationSeconds: Int? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: VerificationStatus =
        VerificationStatus.PENDING,

    @Column(name = "verification_type", nullable = false, length = 50)
    var verificationType: String = "gps", // 나중에 receipt

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()


)
