package com.ramenlog.ramen.domain.restaurant

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.GeneratedValue
import java.time.LocalDateTime
import java.util.UUID


@Entity
@Table(name = "restaurants")
open class Restaurant (
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,

    @Column(name = "kakao_place_id", nullable = false, unique = true, length = 255)
    var kakaoPlaceId: String,

    @Column(nullable = false, length = 255)
    var name: String,

    @Column(length = 500)
    var address: String,

    @Column(nullable = false)
    var latitude: Double,

    @Column(nullable = false)
    var longitude: Double,

    @Column(length = 50)
    var phone: String? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),




    )