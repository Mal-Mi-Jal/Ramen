package com.ramenlog.ramen.domain.visit

import com.ramenlog.ramen.domain.restaurant.Restaurant
import com.ramenlog.ramen.domain.user.User
import jakarta.persistence.Column
import jakarta.persistence.Entity
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
    name = "visit_history",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_user_restaurant_history",
            columnNames = ["user_id", "restaurant_id"]
        )
    ]
)
open class VisitHistory (
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "restaurant_id", nullable = false)
    val restaurant: Restaurant,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "verification_id", nullable = false)
    val visitVerification: VisitVerification,

    @Column(name = "visit_count", nullable = false)
    var visitCount: Int = 1,

    @Column(name = "last_visited_at", nullable = false)
    var lastVisitedAt: LocalDateTime,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()

)