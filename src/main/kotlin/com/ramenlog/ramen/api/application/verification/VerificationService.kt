package com.ramenlog.ramen.api.application.verification

import com.ramenlog.ramen.api.verification.VerificationData
import com.ramenlog.ramen.api.verification.VerificationEnterRequest
import com.ramenlog.ramen.api.verification.VerificationEnterResponse
import com.ramenlog.ramen.api.common.AlreadyPendingException
import com.ramenlog.ramen.api.common.OutOfRangeException
import com.ramenlog.ramen.api.common.RestaurantNotFoundException
import com.ramenlog.ramen.domain.restaurant.RestaurantRepository
import com.ramenlog.ramen.domain.user.User
import com.ramenlog.ramen.domain.visit.VerificationStatus
import com.ramenlog.ramen.domain.visit.VisitVerification
import com.ramenlog.ramen.domain.visit.VisitVerificationRepository
import com.ramenlog.ramen.util.LocationUtils
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class VerificationService (
    private val visitVerificationRepository: VisitVerificationRepository,
    private val restaurantRepository: RestaurantRepository
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

                requiredStaySeconds = 1200
            ),

            message =
                "방문 인증이 시작되었습니다. 20분 이상 체류해주세요."
        )
    }


}