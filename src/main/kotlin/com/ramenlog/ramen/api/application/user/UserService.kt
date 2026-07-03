package com.ramenlog.ramen.api.application.user

import com.ramenlog.ramen.api.review.dto.PendingReviewData
import com.ramenlog.ramen.api.review.dto.PendingReviewResponse
import com.ramenlog.ramen.api.user.dto.MyProfileResponse
import com.ramenlog.ramen.api.user.dto.UpdateProfileRequest
import com.ramenlog.ramen.domain.review.ReviewRepository
import com.ramenlog.ramen.domain.user.UserRepository
import com.ramenlog.ramen.domain.visit.VerificationStatus
import com.ramenlog.ramen.domain.visit.VisitHistoryRepository
import com.ramenlog.ramen.domain.visit.VisitVerificationRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.LocalDateTime

@Service
class UserService (
    private val userRepository: UserRepository,
    private val reviewRepository: ReviewRepository,
    private val visitHistoryRepository: VisitHistoryRepository,
    private val visitVerificationRepository: VisitVerificationRepository
){
    @Transactional(readOnly=true)
    fun getMyProfile(email: String): MyProfileResponse {

        val user = userRepository.findByEmail(email)
            ?: throw IllegalStateException("존재하지 않는 유저입니다.")

        val reviewCount =
            reviewRepository.countByUserId(
                user.id!!
            ).toInt()

        val visitCount =
            visitHistoryRepository.countByUserId(
                user.id!!
            ).toInt()

        val receivedLikeCount =
            reviewRepository.getTotalLikeCountByUserId(
                user.id!!
            )

        return MyProfileResponse.fromWithStats(
            user,
            reviewCount,
            visitCount,
            receivedLikeCount
        )
    }

    @Transactional
    fun updateProfile(email : String, request: UpdateProfileRequest): MyProfileResponse{
        val user = userRepository.findByEmail(email)
            ?: throw IllegalArgumentException("존재하지 않는 유저입니다.")

        if(user.nickname != request.nickname && userRepository.findByNickname(request.nickname) != null){
            throw IllegalArgumentException("이미 사용중인 닉네임입니다.")
        }

        user.updateProfile(request.nickname, request.profile_image_url)

        return MyProfileResponse.from(user)
    }

    @Transactional(readOnly = true)
    fun getPendingReviews(
        email: String
    ): PendingReviewResponse {

        val user =
            userRepository.findByEmail(email)
                ?: throw IllegalArgumentException("존재하지 않는 유저입니다.")

        val now =
            LocalDateTime.now()

        val pending =

            visitVerificationRepository
                .findByUserIdAndStatusOrderByVerifiedAtDesc(
                    user.id!!,
                    VerificationStatus.VERIFIED
                )

                .filter {

                    it.reviewedAt == null

                }

                .filter {

                    val verifiedAt =
                        it.verifiedAt ?: return@filter false

                    verifiedAt.isAfter(
                        now.minusDays(3)
                    )

                }

                .map {

                    PendingReviewData(

                        verification_id = it.id!!,

                        restaurant_id = it.restaurant.id!!,

                        restaurant_name = it.restaurant.name,

                        days_left =

                            Duration.between(
                                now,
                                it.verifiedAt!!
                                    .plusDays(3)
                            ).toDays()

                    )

                }

        return PendingReviewResponse(

            success = true,

            data = pending

        )

    }
}