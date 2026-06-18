package com.ramenlog.ramen.domain.review

import com.ramenlog.ramen.domain.restaurant.Restaurant
import com.ramenlog.ramen.domain.user.User
import com.ramenlog.ramen.domain.visit.VisitVerification
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "reviews")
open class Review (
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "restaurant_id", nullable = false)
    val restaurant: Restaurant,

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "verification_id", nullable = false, unique = true)
    val verification: VisitVerification,

    @Column(nullable = false)
    var rating: Int,

    @Column(columnDefinition = "TEXT")
    var content: String? = null,

    @Column(name = "revisit_intention", nullable = false)
    var revisitIntention: Boolean,

    @Column(name = "like_count", nullable = false)
    var likeCount: Int = 0,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at")
    var updatedAt: LocalDateTime = LocalDateTime.now()

){
    fun update(
        rating: Int,
        content: String?,
        revisitIntention: Boolean
    ) {
        this.rating = rating
        this.content = content
        this.revisitIntention = revisitIntention
        this.updatedAt = LocalDateTime.now()
    }
}