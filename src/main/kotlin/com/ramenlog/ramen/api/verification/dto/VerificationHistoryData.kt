package com.ramenlog.ramen.api.verification.dto

data class VerificationHistoryData (
    val verifications: List<VerificationHistoryItem>,

    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int
)