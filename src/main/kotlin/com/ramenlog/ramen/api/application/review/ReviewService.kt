package com.ramenlog.ramen.api.application.review

import com.ramenlog.ramen.api.common.AlreadyReviewedException
import com.ramenlog.ramen.api.common.InvalidRatingException
import com.ramenlog.ramen.api.common.RestaurantNotFoundException
import com.ramenlog.ramen.api.common.ReviewNotFoundException
import com.ramenlog.ramen.api.common.VerificationRequiredException
import com.ramenlog.ramen.api.review.dto.CreateReviewRequest
import com.ramenlog.ramen.api.review.dto.CreateReviewResponse
import com.ramenlog.ramen.api.review.dto.DeleteReviewResponse
import com.ramenlog.ramen.api.review.dto.PaginationData
import com.ramenlog.ramen.api.review.dto.RestaurantReviewsData
import com.ramenlog.ramen.api.review.dto.RestaurantReviewsResponse
import com.ramenlog.ramen.api.review.dto.ReviewData
import com.ramenlog.ramen.api.review.dto.ReviewPhotoData
import com.ramenlog.ramen.api.review.dto.ReviewSummary
import com.ramenlog.ramen.api.review.dto.ReviewUser
import com.ramenlog.ramen.domain.restaurant.RestaurantRepository
import com.ramenlog.ramen.domain.review.Review
import com.ramenlog.ramen.domain.review.ReviewCommentRepository
import com.ramenlog.ramen.domain.review.ReviewDetailData
import com.ramenlog.ramen.domain.review.ReviewDetailResponse
import com.ramenlog.ramen.domain.review.ReviewLikeRepository
import com.ramenlog.ramen.domain.review.ReviewPhotoRepository
import com.ramenlog.ramen.domain.review.ReviewRepository
import com.ramenlog.ramen.domain.review.ReviewRestaurant
import com.ramenlog.ramen.api.review.dto.UpdateReviewData
import com.ramenlog.ramen.api.review.dto.UpdateReviewRequest
import com.ramenlog.ramen.api.review.dto.UpdateReviewResponse
import com.ramenlog.ramen.domain.user.User
import com.ramenlog.ramen.domain.visit.VerificationStatus
import com.ramenlog.ramen.domain.visit.VisitVerificationRepository
import org.hibernate.sql.Delete
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class ReviewService (
    private val reviewRepository: ReviewRepository,
    private val restaurantRepository: RestaurantRepository,
    private val visitVerificationRepository: VisitVerificationRepository,
    private val reviewPhotoRepository: ReviewPhotoRepository,
    private val reviewLikeRepository: ReviewLikeRepository,
    private val reviewCommentRepository: ReviewCommentRepository
){
    @Transactional
    fun createReview(
        user: User,
        request: CreateReviewRequest
    ): CreateReviewResponse {

        val restaurant =
            restaurantRepository.findById(
                request.restaurantId
            )
                .orElseThrow {
                    RestaurantNotFoundException()
                }

        val verification =
            visitVerificationRepository.findById(
                request.verificationId
            )
                .orElseThrow {
                    VerificationRequiredException()
                }

        if (verification.user.id != user.id) {
            throw VerificationRequiredException()
        }

        if (
            verification.restaurant.id !=
            request.restaurantId
        ) {
            throw IllegalArgumentException(
                "식당 정보가 일치하지 않습니다."
            )
        }

        if (
            verification.status !=
            VerificationStatus.VERIFIED
        ) {
            throw VerificationRequiredException()
        }

        if (
            reviewRepository.existsByVerificationId(
                request.verificationId
            )
        ) {
            throw AlreadyReviewedException()
        }

        if (request.rating !in 1..5) {
            throw InvalidRatingException()
        }

        val review =
            reviewRepository.save(
                Review(
                    user = user,

                    restaurant = restaurant,

                    verification = verification,

                    rating = request.rating,

                    content = request.content,

                    revisitIntention =
                        request.revisitIntention
                )
            )

        return CreateReviewResponse(
            success = true,

            data =
                ReviewData(
                    reviewId = review.id!!,

                    restaurantId =
                        restaurant.id!!,

                    restaurantName =
                        restaurant.name,

                    rating =
                        review.rating,

                    content =
                        review.content,

                    revisitIntention =
                        review.revisitIntention,

                    createdAt =
                        review.createdAt
                )
        )
    }

    @Transactional(readOnly = true)
    fun getRestaurantReviews(
        restaurantId: UUID,
        sort: String,
        page: Int,
        size: Int
    ): RestaurantReviewsResponse{
        val sortOption =
            when (sort) {
                "latest" ->
                    Sort.by(
                        Sort.Direction.DESC,
                        "createdAt"
                    )

                "rating_desc" ->
                    Sort.by(
                        Sort.Direction.DESC,
                        "rating"
                    )

                "likes" ->
                    Sort.by(
                        Sort.Direction.DESC,
                        "createdAt" // todo: likes count
                    )

                else ->
                    Sort.by(
                        Sort.Direction.DESC,
                        "createdAt"
                    )
            }
        val pageable =
            PageRequest.of(
                page - 1,
                size,
                sortOption
            )

        val reviewPage =
            reviewRepository.findByRestaurantId(
                restaurantId,
                pageable
            )

        val reviews =
            reviewPage.content.map { review ->

                val photos =
                    reviewPhotoRepository
                        .findByReviewIdOrderByOrderIndexAsc(
                            review.id!!
                        )
                        .map { photo ->

                            ReviewPhotoData(
                                id = photo.id!!,

                                imageUrl =
                                    photo.imageUrl,

                                orderIndex =
                                    photo.orderIndex
                            )
                        }

                val likeCount =
                    reviewLikeRepository
                        .countByReviewId(
                            review.id!!
                        )

                val commentCount =
                    reviewCommentRepository
                        .countByReviewId(
                            review.id!!
                        )

                ReviewSummary(
                    id = review.id!!,

                    user =
                        ReviewUser(
                            id =
                                review.user.id!!,

                            nickname =
                                review.user.nickname,

                            profileImageUrl =
                                review.user.profileImageUrl
                        ),

                    rating =
                        review.rating,

                    content =
                        review.content,

                    revisitIntention =
                        review.revisitIntention,

                    likeCount =
                        likeCount,

                    photos =
                        photos,

                    commentCount =
                        commentCount,

                    createdAt =
                        review.createdAt
                )
            }

        return RestaurantReviewsResponse(
            success = true,

            data =
                RestaurantReviewsData(
                    reviews = reviews,

                    pagination =
                        PaginationData(
                            total =
                                reviewPage.totalElements,

                            page = page,

                            size = size,

                            totalPages =
                                reviewPage.totalPages
                        )
                )
        )
    }

    @Transactional(readOnly = true)
    fun getReviewDetail(
        reviewId: UUID
    ): ReviewDetailResponse{
        val review =
            reviewRepository.findById(reviewId)
                .orElseThrow {
                    ReviewNotFoundException()
                }

        val photos =
            reviewPhotoRepository
                .findByReviewIdOrderByOrderIndexAsc(
                    review.id!!
                )

        val likeCount =
            reviewLikeRepository
                .countByReviewId(
                    review.id!!
                )

        val commentCount =
            reviewCommentRepository
                .countByReviewId(
                    review.id!!
                )

        return ReviewDetailResponse(
            success = true,
            data = ReviewDetailData(
                id = review.id!!,

                restaurant =
                    ReviewRestaurant(
                        id =
                            review.restaurant.id!!,

                        name =
                            review.restaurant.name
                    ),

                user =
                    ReviewUser(
                        id =
                            review.user.id!!,

                        nickname =
                            review.user.nickname,

                        profileImageUrl =
                            review.user.profileImageUrl
                    ),

                rating =
                    review.rating,

                content =
                    review.content,

                revisitIntention =
                    review.revisitIntention,

                likeCount =
                    likeCount,

                commentCount =
                    commentCount,

                photos =
                    photos.map { photo ->

                        ReviewPhotoData(
                            id =
                                photo.id!!,

                            imageUrl =
                                photo.imageUrl,

                            orderIndex =
                                photo.orderIndex
                        )
                    },

                createdAt =
                    review.createdAt,

                updatedAt =
                    review.updatedAt
            )
        )
    }

    @Transactional
    fun updateReview(
        reviewId: UUID,
        user: User,
        request: UpdateReviewRequest
    ): UpdateReviewResponse{

        val review =
            reviewRepository.findById(reviewId)
                .orElseThrow {
                    ReviewNotFoundException()
                }

        if (review.user.id != user.id) {
            throw IllegalArgumentException(
                "본인 리뷰만 수정할 수 있습니다."
            )
        }

        if (request.rating !in 1..5) {
            throw IllegalArgumentException(
                "별점은 1~5 사이여야 합니다."
            )
        }

        review.update(
            rating = request.rating,
            content = request.content,
            revisitIntention = request.revisitIntention
        )

        reviewRepository.save(review)

        return UpdateReviewResponse(
            success = true,

            data =
                UpdateReviewData(
                    reviewId = review.id!!,

                    rating = review.rating,

                    content = review.content,

                    revisitIntention =
                        review.revisitIntention,

                    updatedAt =
                        review.updatedAt
                )
        )
    }

    @Transactional
    fun deleteReview(
        reviewId: UUID,
        user: User
    ): DeleteReviewResponse {
        val review =
            reviewRepository.findById(reviewId)
                .orElseThrow {
                    ReviewNotFoundException()
                }

        if (review.user.id != user.id){
            throw IllegalArgumentException("본인 리뷰만 삭제할 수 있습니다.")
        }

        reviewRepository.delete(review)

        return DeleteReviewResponse(
            success = true,

            message = "리뷰가 삭제되었습니다."
        )
    }


}