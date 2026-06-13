package com.ramenlog.ramen.api.auth

import com.ramenlog.ramen.api.application.auth.AuthService
import com.ramenlog.ramen.api.auth.dto.LoginRequest
import com.ramenlog.ramen.api.auth.dto.LoginResponse
import com.ramenlog.ramen.api.auth.dto.SignupRequest
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/auth")
class AuthController (
    private val authService: AuthService
){

    @PostMapping("/signup")
    fun signup(@RequestBody request: SignupRequest): ResponseEntity<Map<String, Any>> {
        authService.signup(request)

        val response = mapOf(
            "success" to true,
            "message" to "회원가입이 완료되었습니다."
        )
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    @PostMapping("/login")
    fun login(@Valid @RequestBody request: LoginRequest): ResponseEntity<LoginResponse>{
        val tokenData = authService.login(request)

        val response = LoginResponse(
            success = true,
            data = tokenData
        )

        return ResponseEntity.ok(response)

    }

    @PostMapping("/logout")
    fun logout(
        @RequestHeader("Authorization") authHeader: String
    ): ResponseEntity<Map<String, Any>> {
        val token = authHeader.substringAfter("Bearer ")

        authService.logout(token)

        val response = mapOf(
            "success" to true,
            "message" to "로그아웃 되었습니다."
        )
        return ResponseEntity.ok(response)
    }
}