package com.ramenlog.ramen.domain.favorite

import com.ramenlog.ramen.domain.restaurant.Restaurant
import com.ramenlog.ramen.domain.user.User
import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(
    name = "favorite_restaurants",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_user_restaurant_favorite",
            columnNames = ["user_id", "restaurant_id"]
        )
    ]
)
class FavoriteRestaurant(

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "restaurant_id", nullable = false)
    val restaurant: Restaurant,

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()

)