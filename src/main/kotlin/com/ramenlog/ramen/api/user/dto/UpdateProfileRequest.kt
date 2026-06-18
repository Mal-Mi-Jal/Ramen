package com.ramenlog.ramen.api.user.dto

data class UpdateProfileRequest (
    val nickname: String,
    val profile_image_url: String?
)