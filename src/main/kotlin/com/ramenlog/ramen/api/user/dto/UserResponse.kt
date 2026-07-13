package com.ramenlog.ramen.api.user.dto

import com.fasterxml.jackson.annotation.JsonFormat
import com.ramenlog.ramen.domain.user.User
import java.time.LocalDateTime
import java.util.UUID

data class UserProfileData(
    val id: UUID,
    val email: String,
    val nickname: String,
    val profile_image_url: String?,

    val review_count: Int,
    val visit_count: Int,
    val received_like_count: Int,

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    val created_at: LocalDateTime

)

data class MyProfileResponse(
    val success: Boolean,
    val data: UserProfileData
) {
    companion object {

        fun from(user: User): MyProfileResponse {
            return MyProfileResponse(
                success = true,
                data = UserProfileData(
                    id = user.id!!,
                    email = user.email,
                    nickname = user.nickname,
                    profile_image_url = user.profileImageUrl,

                    review_count = 0,
                    visit_count = 0,
                    received_like_count = 0,

                    created_at = user.createdAt
                )
            )
        }

        fun fromWithStats(
            user: User,
            reviewCount: Int,
            visitCount: Int,
            receivedLikeCount: Int
        ): MyProfileResponse {

            return MyProfileResponse(
                success = true,
                data = UserProfileData(
                    id = user.id!!,
                    email = user.email,
                    nickname = user.nickname,
                    profile_image_url = user.profileImageUrl,

                    review_count = reviewCount,
                    visit_count = visitCount,
                    received_like_count = receivedLikeCount,

                    created_at = user.createdAt
                )
            )
        }
    }
}