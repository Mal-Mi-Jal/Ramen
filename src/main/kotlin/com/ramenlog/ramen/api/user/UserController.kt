package com.ramenlog.ramen.api.user

import com.ramenlog.ramen.api.application.user.UserService
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/users")
class UserController (
    private val userService: UserService
){
    @GetMapping("/me")
    fun getMyProfile(
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<MyProfileResponse> {

        val email = userDetails.username

        val response = userService.getMyProfile(email)

        return ResponseEntity.ok(response)
    }

    @PatchMapping("/me")
    fun updateProfile(
        @AuthenticationPrincipal userDetails: UserDetails,
        @RequestBody request: UpdateProfileRequest
    ): ResponseEntity<MyProfileResponse> {
        val email = userDetails.username
        val response = userService.updateProfile(email, request)

        return ResponseEntity.ok(response)
    }

}