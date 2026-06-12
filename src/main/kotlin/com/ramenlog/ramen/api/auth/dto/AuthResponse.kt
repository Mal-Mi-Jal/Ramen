package com.ramenlog.ramen.api.auth.dto

data class TokenData(
    val access_token: String,
    val refresh_token: String,
    val expires_in: Long
)

data class LoginResponse(
    val success: Boolean,
    val data: TokenData
)