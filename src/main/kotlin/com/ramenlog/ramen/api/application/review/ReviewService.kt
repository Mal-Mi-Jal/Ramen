package com.ramenlog.ramen.api.application.review

import com.ramenlog.ramen.api.common.AlreadyLikedException
import com.ramenlog.ramen.api.common.AlreadyReviewedException
import com.ramenlog.ramen.api.common.CannotLikeOwnReviewException
import com.ramenlog.ramen.api.common.EmptyCommentException
import com.ramenlog.ramen.api.common.InvalidRatingException
import com.ramenlog.ramen.api.common.RestaurantNotFoundException
import com.ramenlog.ramen.api.common.ReviewCommentNotFoundException
import com.ramenlog.ramen.api.common.ReviewLikeNotFoundException
import com.ramenlog.ramen.api.common.ReviewNotFoundException
import com.ramenlog.ramen.api.common.VerificationRequiredException
import com.ramenlog.ramen.api.review.dto.CommentData
import com.ramenlog.ramen.api.review.dto.CommentItem
import com.ramenlog.ramen.api.review.dto.CommentUser
import com.ramenlog.ramen.api.review.dto.CommentsData
import com.ramenlog.ramen.api.review.dto.CreateCommentRequest
import com.ramenlog.ramen.api.review.dto.CreateCommentResponse
import com.ramenlog.ramen.api.review.dto.CreateReviewRequest
import com.ramenlog.ramen.api.review.dto.CreateReviewResponse
import com.ramenlog.ramen.api.review.dto.DeleteCommentResponse
import com.ramenlog.ramen.api.review.dto.DeleteReviewResponse
import com.ramenlog.ramen.api.review.dto.GetCommentsResponse
import com.ramenlog.ramen.api.review.dto.MyReviewRestaurant
import com.ramenlog.ramen.api.review.dto.MyReviewSummary
import com.ramenlog.ramen.api.review.dto.MyReviewsData
import com.ramenlog.ramen.api.review.dto.MyReviewsResponse
import com.ramenlog.ramen.api.review.dto.PaginationData
import com.ramenlog.ramen.api.review.dto.RestaurantReviewsData
import com.ramenlog.ramen.api.review.dto.RestaurantReviewsResponse
import com.ramenlog.ramen.api.review.dto.ReviewData
import com.ramenlog.ramen.api.review.dto.ReviewLikeData
import com.ramenlog.ramen.api.review.dto.ReviewLikeResponse
import com.ramenlog.ramen.api.review.dto.ReviewPhotoData
import com.ramenlog.ramen.api.review.dto.ReviewSummary
import com.ramenlog.ramen.api.review.dto.ReviewUser
import com.ramenlog.ramen.api.review.dto.UpdateCommentRequest
import com.ramenlog.ramen.api.review.dto.UpdateCommentResponse
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
import com.ramenlog.ramen.domain.review.ReviewComment
import com.ramenlog.ramen.domain.review.ReviewLike
import com.ramenlog.ramen.domain.user.User
import com.ramenlog.ramen.domain.visit.VerificationStatus
import com.ramenlog.ramen.domain.visit.VisitVerificationRepository
import org.hibernate.sql.Delete
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
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

        // 이미 이 인증으로 리뷰를 작성한 경우
        if (verification.reviewedAt != null) {
            throw AlreadyReviewedException()
        }

        val verifiedAt =
            verification.verifiedAt
                ?: throw VerificationRequiredException()

        if (
            verifiedAt.isBefore(
                LocalDateTime.now().minusDays(3)
            )
        ) {
            throw IllegalArgumentException(
                "리뷰 작성 가능 기간이 지났습니다."
            )
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

        verification.reviewedAt =
            LocalDateTime.now()

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
                        "like_count"
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

        if (review.user.id != user.id) {
            throw IllegalArgumentException(
                "본인 리뷰만 삭제할 수 있습니다."
            )
        }

        reviewCommentRepository
            .deleteByReviewId(reviewId)

        reviewLikeRepository
            .deleteByReviewId(reviewId)

        reviewPhotoRepository
            .deleteByReviewId(reviewId)

        reviewRepository.delete(review)

        return DeleteReviewResponse(
            success = true,
            message = "리뷰가 삭제되었습니다."
        )
    }

    @Transactional
    fun likeReview(
        reviewId: UUID,
        user: User
    ): ReviewLikeResponse {
        val review = reviewRepository.findById(reviewId)
            .orElseThrow{
                ReviewNotFoundException()
            }

        if (review.user.id == user.id){
            throw CannotLikeOwnReviewException()
        }

        if (reviewLikeRepository.existsByReviewIdAndUserId(
                review.id!!, user.id!!)
            )
        {
            throw AlreadyLikedException()
        }

        reviewLikeRepository.save(
            ReviewLike(
                review = review,
                user = user
            )
        )

        val likeCount =
            reviewLikeRepository.countByReviewId(
                review.id!!
            )

        return ReviewLikeResponse(
            success = true,

            data = ReviewLikeData(
                reviewId = reviewId,
                likeCount = likeCount
            )
        )

    }

    @Transactional
    fun unlikeReview(
        reviewId: UUID,
        user: User
    ): ReviewLikeResponse {

        val review =
            reviewRepository.findById(reviewId)
                .orElseThrow {
                    ReviewNotFoundException()
                }

        val reviewLike =
            reviewLikeRepository
                .findByReviewIdAndUserId(
                    reviewId,
                    user.id!!
                )
                ?: throw ReviewLikeNotFoundException()

        reviewLikeRepository.delete(
            reviewLike
        )

        val likeCount =
            reviewLikeRepository.countByReviewId(
                reviewId
            )

        return ReviewLikeResponse(
            success = true,

            data = ReviewLikeData(
                reviewId = reviewId,
                likeCount = likeCount
            )
        )
    }

    @Transactional
    fun createComment(
        reviewId: UUID,
        user: User,
        request: CreateCommentRequest
    ): CreateCommentResponse {
        if (request.content.isBlank()) {
            throw EmptyCommentException()
        }

        val review =
            reviewRepository.findById(reviewId)
                .orElseThrow {
                    ReviewNotFoundException()
                }

        val comment =
            reviewCommentRepository.save(

                ReviewComment(
                    review = review,
                    user = user,
                    content = request.content
                )
            )

        return CreateCommentResponse(
            success = true,

            data = CommentData(
                id = comment.id!!,

                user = CommentUser(
                    id = user.id!!,

                    nickname = user.nickname,

                    profileImageUrl =
                        user.profileImageUrl
                ),

                content = comment.content,

                createdAt =
                    comment.createdAt
            )
        )
    }

    @Transactional(readOnly = true)
    fun getComments(
        reviewId: UUID,
        page: Int,
        size: Int
    ): GetCommentsResponse {

        reviewRepository.findById(reviewId)
            .orElseThrow {
                ReviewNotFoundException()
            }

        val pageable =
            PageRequest.of(
                page - 1,
                size,
                Sort.by("createdAt").ascending()
            )

        val comments =
            reviewCommentRepository.findByReviewId(
                reviewId,
                pageable
            )

        val commentItems =
            comments.content.map { comment ->

                CommentItem(
                    id = comment.id!!,

                    user =
                        CommentUser(
                            id = comment.user.id!!,

                            nickname =
                                comment.user.nickname,

                            profileImageUrl =
                                comment.user.profileImageUrl
                        ),

                    content =
                        comment.content,

                    createdAt =
                        comment.createdAt
                )
            }

        return GetCommentsResponse(
            success = true,

            data =
                CommentsData(
                    comments =
                        commentItems,

                    pagination =
                        PaginationData(
                            total =
                                comments.totalElements,

                            page = page,

                            size = size,

                            totalPages =
                                comments.totalPages
                        )
                )
        )
    }

    @Transactional
    fun updateComment(
        reviewId: UUID,
        commentId: UUID,
        user: User,
        request: UpdateCommentRequest
    ): UpdateCommentResponse {

        val comment =
            reviewCommentRepository.findById(commentId)
                .orElseThrow {
                    ReviewCommentNotFoundException()
                }

        if (comment.review.id != reviewId) {
            throw IllegalArgumentException(
                "해당 리뷰의 댓글이 아닙니다."
            )
        }

        if (comment.user.id != user.id) {
            throw IllegalArgumentException(
                "본인 댓글만 수정할 수 있습니다."
            )
        }

        if (request.content.isBlank()) {
            throw IllegalArgumentException(
                "댓글 내용을 입력해주세요."
            )
        }

        comment.updateContent(
            request.content
        )

        return UpdateCommentResponse(
            success = true,
            message = "댓글이 수정되었습니다."
        )
    }

    @Transactional
    fun deleteComment(
        reviewId: UUID,
        commentId: UUID,
        user: User
    ): DeleteCommentResponse {

        val comment =
            reviewCommentRepository.findById(commentId)
                .orElseThrow {
                    ReviewCommentNotFoundException()
                }

        if (comment.review.id != reviewId) {
            throw IllegalArgumentException(
                "해당 리뷰의 댓글이 아닙니다."
            )
        }

        if (comment.user.id != user.id) {
            throw IllegalArgumentException(
                "본인 댓글만 삭제할 수 있습니다."
            )
        }

        reviewCommentRepository.delete(comment)

        return DeleteCommentResponse(
            success = true,
            message = "댓글이 삭제되었습니다."
        )
    }

    @Transactional(readOnly = true)
    fun getMyReviews(
        userId: UUID,
        page: Int,
        size: Int
    ): MyReviewsResponse {
        val pageable =
            PageRequest.of(
                page - 1,
                size,
                Sort.by(Sort.Direction.DESC, "createdAt")
            )

        val reviewPage =
            reviewRepository.findByUserId(
                userId,
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

                MyReviewSummary(
                    id = review.id!!,

                    restaurant =
                        MyReviewRestaurant(
                            id =
                                review.restaurant.id!!,

                            name =
                                review.restaurant.name,

                            address =
                                review.restaurant.address
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

        return MyReviewsResponse(
            success = true,

            data =
                MyReviewsData(
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



}