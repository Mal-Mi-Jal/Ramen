package com.ramenlog.ramen.api.user

import com.ramenlog.ramen.api.application.review.ReviewService
import com.ramenlog.ramen.api.application.user.UserService
import com.ramenlog.ramen.api.application.visit.VisitHistoryService
import com.ramenlog.ramen.api.review.dto.MyReviewsResponse
import com.ramenlog.ramen.api.user.dto.MyProfileResponse
import com.ramenlog.ramen.api.user.dto.UpdateProfileRequest
import com.ramenlog.ramen.api.visit.dto.VisitHistoryResponse
import com.ramenlog.ramen.domain.user.User
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/users")
class UserController (
    private val userService: UserService,
    private val visitHistoryService: VisitHistoryService,
    private val reviewService: ReviewService,
){
    @GetMapping("/me")
    fun getMyProfile(
        @AuthenticationPrincipal user: User
    ): ResponseEntity<MyProfileResponse> {

        val response =
            userService.getMyProfile(
                user.email
            )

        return ResponseEntity.ok(response)
    }

    @PatchMapping("/me")
    fun updateProfile(
        @AuthenticationPrincipal user: User,
        @RequestBody request: UpdateProfileRequest
    ): ResponseEntity<MyProfileResponse> {

        val response =
            userService.updateProfile(
                user.email,
                request
            )

        return ResponseEntity.ok(response)
    }

    @GetMapping("/me/visit-history")
    fun getMyVisitHistory(

        @AuthenticationPrincipal user: User,

        @RequestParam(defaultValue = "1")
        page: Int,

        @RequestParam(defaultValue = "20")
        size: Int

    ): ResponseEntity<VisitHistoryResponse> {

        val response =
            visitHistoryService.getMyVisitHistory(
                user,
                page,
                size
            )

        return ResponseEntity.ok(response)
    }

    @GetMapping("/me/reviews")
    fun getMyReviews(
        @AuthenticationPrincipal user: User,

        @RequestParam(defaultValue = "1")
        page: Int,

        @RequestParam(defaultValue = "20")
        size: Int

    ): ResponseEntity<MyReviewsResponse> {

        val response =
            reviewService.getMyReviews(
                user.id!!,
                page,
                size
            )

        return ResponseEntity.ok(response)
    }

}