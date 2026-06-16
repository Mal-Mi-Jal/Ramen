package com.ramenlog.ramen.api.application.verification

import com.ramenlog.ramen.api.verification.VerificationData
import com.ramenlog.ramen.api.verification.VerificationEnterRequest
import com.ramenlog.ramen.api.verification.VerificationEnterResponse
import com.ramenlog.ramen.api.common.AlreadyPendingException
import com.ramenlog.ramen.api.common.OutOfRangeException
import com.ramenlog.ramen.api.common.RestaurantNotFoundException
import com.ramenlog.ramen.api.verification.VerificationError
import com.ramenlog.ramen.api.verification.VerificationErrorData
import com.ramenlog.ramen.api.verification.VerificationVerifyData
import com.ramenlog.ramen.api.verification.VerificationVerifyRequest
import com.ramenlog.ramen.api.verification.VerificationVerifyResponse
import com.ramenlog.ramen.domain.restaurant.RestaurantRepository
import com.ramenlog.ramen.domain.user.User
import com.ramenlog.ramen.domain.visit.VerificationStatus
import com.ramenlog.ramen.domain.visit.VisitVerification
import com.ramenlog.ramen.domain.visit.VisitVerificationRepository
import com.ramenlog.ramen.util.LocationUtils
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.LocalDateTime
import java.util.UUID

@Service
class VerificationService (
    private val visitVerificationRepository: VisitVerificationRepository,
    private val restaurantRepository: RestaurantRepository,

    @Value("\${verification.required-stay-seconds}")
    private val requiredStaySeconds: Int
){
    fun startVerification(
        user: User,
        request: VerificationEnterRequest
    ): VerificationEnterResponse {

        val restaurant =
            restaurantRepository.findById(
                request.restaurantId
            )
                .orElseThrow {
                    RestaurantNotFoundException()
                }

        val distance =
            LocationUtils.calculateDistance(
                request.latitude,
                request.longitude,

                restaurant.latitude,
                restaurant.longitude
            )

        if (distance > 50) {
            throw OutOfRangeException()
        }

        val hasPending =
            visitVerificationRepository
                .existsByUserAndStatus(
                    user,
                    VerificationStatus.PENDING
                )

        if (hasPending) {
            throw AlreadyPendingException()
        }

        val now = LocalDateTime.now()

        val verification =
            visitVerificationRepository.save(

                VisitVerification(
                    user = user,

                    restaurant = restaurant,

                    enteredAt = now,

                    status = VerificationStatus.PENDING
                )
            )

        return VerificationEnterResponse(
            success = true,

            data = VerificationData(
                verificationId =
                    verification.id.toString(),

                status = verification.status.name.lowercase(),

                enteredAt =
                    verification.enteredAt,

                requiredStaySeconds = requiredStaySeconds
            ),

            message =
                "방문 인증이 시작되었습니다. ${requiredStaySeconds}초 이상 체류해주세요."
        )
    }

    @Transactional
    fun verifyVerification(
        verificationId: UUID,
        user: User,
        request: VerificationVerifyRequest
    ): VerificationVerifyResponse{

        val verification =
            visitVerificationRepository
                .findById(verificationId)
                .orElseThrow {
                    IllegalArgumentException(
                        "방문 인증 정보가 없습니다."
                    )
                }
        if (verification.user.id != user.id) {
            throw IllegalArgumentException(
                "본인 인증만 가능합니다."
            )
        }

        if (
            verification.status ==
            VerificationStatus.VERIFIED
        ) {
            throw IllegalArgumentException(
                "이미 인증 완료된 방문입니다."
            )
        }

        val distance =
            LocationUtils.calculateDistance(
                request.latitude,
                request.longitude,

                verification.restaurant.latitude,
                verification.restaurant.longitude
            )

        if (distance > 50) {
            throw OutOfRangeException()
        }

        val currentStaySeconds =
            Duration.between(
                verification.enteredAt,
                LocalDateTime.now()
            ).seconds

        if (currentStaySeconds < requiredStaySeconds) {

            return VerificationVerifyResponse(
                success = false,

                error = VerificationError(
                    code = "INSUFFICIENT_STAY_TIME",

                    message = "아직 체류 시간이 부족합니다.",

                    data =
                        VerificationErrorData(
                            currentStaySeconds =
                                currentStaySeconds,

                            requiredStaySeconds =
                                requiredStaySeconds.toLong(),

                            remainingSeconds =
                                requiredStaySeconds.toLong() -
                                        currentStaySeconds
                        )
                )
            )
        }
        verification.status =
            VerificationStatus.VERIFIED

        verification.verifiedAt =
            LocalDateTime.now()

        verification.stayDurationSeconds =
            currentStaySeconds.toInt()

        visitVerificationRepository.save(
            verification
        )

        return VerificationVerifyResponse(
            success = true,

            data =
                VerificationVerifyData(
                    verificationId =
                        verification.id.toString(),

                    status =
                        verification.status.name.lowercase(),

                    stayDurationSeconds =
                        currentStaySeconds,

                    verifiedAt =
                        verification.verifiedAt!!
                ),

            message =
                "방문 인증이 완료되었습니다! 리뷰를 남겨주세요."
        )
    }

}