package com.ramenlog.ramen.api.user

import com.fasterxml.jackson.annotation.JsonFormat
import com.ramenlog.ramen.domain.user.User
import java.time.LocalDateTime
import java.util.UUID

data class UserProfileData(
    val id: UUID,
    val email: String,
    val nickname: String,
    val profile_image_url: String?,

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    val created_at: LocalDateTime

)

// 💡 명세서 규격 {"success": true, "data": {...}} 을 맞추기 위한 포장 가방
data class MyProfileResponse(
    val success: Boolean,
    val data: UserProfileData
) {
    companion object {
        // 엔티티를 DTO로 편하게 변환하기 위한 팩토리 메서드
        fun from(user: User): MyProfileResponse {
            return MyProfileResponse(
                success = true,
                data = UserProfileData(
                    id = user.id!!,
                    email = user.email,
                    nickname = user.nickname,
                    profile_image_url = user.profileImageUrl,
                    created_at = user.createdAt
                )
            )
        }
    }
}