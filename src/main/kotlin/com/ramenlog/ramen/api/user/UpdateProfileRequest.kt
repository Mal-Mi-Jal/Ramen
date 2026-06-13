package com.ramenlog.ramen.api.user

data class UpdateProfileRequest (
    val nickname: String,
    val profile_image_url: String?
)