package com.ramenlog.ramen.domain.user

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "users")
open class User(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,

    @Column(nullable = false, unique = true, length = 255)
    var email: String,

    @Column(name = "password_hash", nullable = false, length = 255)
    var passwordHash: String,

    @Column(nullable = false, unique = true, length = 100)
    var nickname: String,

    @Column(name = "profile_image_url", length = 500)
    var profileImageUrl: String? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "refresh_token")
    var refreshToken: String? = null
){
    fun updateRefreshToken(token : String?){
        this.refreshToken = token
        this.updatedAt = LocalDateTime.now()
    }

    fun updateProfile(newNickname: String, newProfileImageUrl: String?){
        this.nickname = newNickname
        this.profileImageUrl = newProfileImageUrl
        this.updatedAt = LocalDateTime.now()
    }

}