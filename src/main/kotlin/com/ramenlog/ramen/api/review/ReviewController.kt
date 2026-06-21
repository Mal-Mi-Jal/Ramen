package com.ramenlog.ramen.api.review

import com.ramenlog.ramen.api.application.review.ReviewService
import com.ramenlog.ramen.api.review.dto.CreateCommentRequest
import com.ramenlog.ramen.api.review.dto.CreateCommentResponse
import com.ramenlog.ramen.api.review.dto.CreateReviewRequest
import com.ramenlog.ramen.api.review.dto.CreateReviewResponse
import com.ramenlog.ramen.api.review.dto.DeleteCommentResponse
import com.ramenlog.ramen.api.review.dto.DeleteReviewResponse
import com.ramenlog.ramen.api.review.dto.GetCommentsResponse
import com.ramenlog.ramen.api.review.dto.ReviewLikeResponse
import com.ramenlog.ramen.api.review.dto.UpdateCommentRequest
import com.ramenlog.ramen.api.review.dto.UpdateCommentResponse
import com.ramenlog.ramen.domain.review.ReviewDetailResponse
import com.ramenlog.ramen.api.review.dto.UpdateReviewRequest
import com.ramenlog.ramen.api.review.dto.UpdateReviewResponse
import com.ramenlog.ramen.domain.user.User
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/reviews")
class ReviewController(
    private val reviewService: ReviewService
) {

    @PostMapping
    fun createReview(
        @AuthenticationPrincipal user: User,
        @RequestBody request: CreateReviewRequest
    ): ResponseEntity<CreateReviewResponse> {

        val response =
            reviewService.createReview(
                user,
                request
            )

        return ResponseEntity.status(
            HttpStatus.CREATED
        ).body(response)
    }

    @GetMapping("/{reviewId}")
    fun getReviewDetail(
        @PathVariable reviewId: UUID
    ): ResponseEntity<ReviewDetailResponse> {

        val response =
            reviewService.getReviewDetail(
                reviewId
            )

        return ResponseEntity.ok(response)
    }

    @PatchMapping("/{reviewId}")
    fun updateReview(
        @PathVariable reviewId: UUID,

        @AuthenticationPrincipal user: User,

        @RequestBody request: UpdateReviewRequest
    ): ResponseEntity<UpdateReviewResponse> {

        val response =
            reviewService.updateReview(
                reviewId,
                user,
                request
            )

        return ResponseEntity.ok(response)
    }

    @DeleteMapping("/{reviewId}")
    fun deleteReview(
        @PathVariable reviewId: UUID,

        @AuthenticationPrincipal user: User
    ): ResponseEntity<DeleteReviewResponse> {

        val response =
            reviewService.deleteReview(
                reviewId,
                user
            )

        return ResponseEntity.ok(response)
    }

    @PostMapping("/{reviewId}/likes")
    fun likeReview(
        @PathVariable reviewId: UUID,
        @AuthenticationPrincipal user: User
    ): ResponseEntity<ReviewLikeResponse> {
        val response = reviewService.likeReview(
            reviewId,
            user
        )

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(response)
    }

    @DeleteMapping("/{reviewId}/likes")
    fun unlikeReview(
        @PathVariable reviewId: UUID,

        @AuthenticationPrincipal user: User
    ): ResponseEntity<ReviewLikeResponse> {

        val response =
            reviewService.unlikeReview(
                reviewId,
                user
            )

        return ResponseEntity.ok(response)
    }

    @PostMapping("/{reviewId}/comments")
    fun createComment(

        @PathVariable reviewId: UUID,

        @AuthenticationPrincipal user: User,

        @RequestBody request: CreateCommentRequest

    ): ResponseEntity<CreateCommentResponse> {

        val response =
            reviewService.createComment(
                reviewId,
                user,
                request
            )

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(response)
    }

    @GetMapping("/{reviewId}/comments")
    fun getComments(

        @PathVariable reviewId: UUID,

        @RequestParam(defaultValue = "1")
        page: Int,

        @RequestParam(defaultValue = "30")
        size: Int

    ): ResponseEntity<GetCommentsResponse> {

        val response =
            reviewService.getComments(
                reviewId,
                page,
                size
            )

        return ResponseEntity.ok(
            response
        )
    }

    @PatchMapping(
        "/{reviewId}/comments/{commentId}"
    )
    fun updateComment(

        @PathVariable reviewId: UUID,

        @PathVariable commentId: UUID,

        @AuthenticationPrincipal user: User,

        @RequestBody request: UpdateCommentRequest

    ): ResponseEntity<UpdateCommentResponse> {

        val response =
            reviewService.updateComment(
                reviewId,
                commentId,
                user,
                request
            )

        return ResponseEntity.ok(
            response
        )
    }

    @DeleteMapping(
        "/{reviewId}/comments/{commentId}"
    ) fun deleteComment(
        @PathVariable reviewId: UUID,
        @PathVariable commentId: UUID,
        @AuthenticationPrincipal user: User
    ) : ResponseEntity<DeleteCommentResponse> {
        val response = reviewService.deleteComment(
            reviewId,
            commentId,
            user
        )

        return ResponseEntity.ok(response)
    }
}