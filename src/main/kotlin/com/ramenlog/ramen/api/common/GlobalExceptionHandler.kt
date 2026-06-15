package com.ramenlog.ramen.api.common

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {
    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgumentException(e: IllegalArgumentException): ResponseEntity<Map<String, Any>> {

        val errorResponse = mapOf(
            "success" to false,
            "message" to (e.message ?: "잘못된 요청입니다.")
        )

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse)
    }

    @ExceptionHandler(OutOfRangeException::class)
    fun handleOutOfRangeException(
        e: OutOfRangeException
    ): ResponseEntity<Map<String, Any>> {

        val errorResponse = mapOf(
            "success" to false,
            "code" to "OUT_OF_RANGE",
            "message" to "라멘집 반경 50m 밖에 있습니다."
        )

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(errorResponse)
    }

    @ExceptionHandler(AlreadyPendingException::class)
    fun handleAlreadyPendingException(
        e: AlreadyPendingException
    ): ResponseEntity<Map<String, Any>> {

        val errorResponse = mapOf(
            "success" to false,
            "code" to "ALREADY_PENDING",
            "message" to "이미 진행 중인 방문 인증이 존재합니다."
        )

        return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(errorResponse)
    }

    @ExceptionHandler(RestaurantNotFoundException::class)
    fun handleRestaurantNotFoundException(
        e: RestaurantNotFoundException
    ): ResponseEntity<Map<String, Any>> {

        val errorResponse = mapOf(
            "success" to false,
            "code" to "RESTAURANT_NOT_FOUND",
            "message" to "식당을 찾을 수 없습니다."
        )

        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(errorResponse)
    }
}