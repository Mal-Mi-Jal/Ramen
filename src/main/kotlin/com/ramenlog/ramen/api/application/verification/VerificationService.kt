package com.ramenlog.ramen.api.application.verification

import com.ramenlog.ramen.api.verification.dto.VerificationData
import com.ramenlog.ramen.api.verification.dto.VerificationEnterRequest
import com.ramenlog.ramen.api.verification.dto.VerificationEnterResponse
import com.ramenlog.ramen.api.common.AlreadyPendingException
import com.ramenlog.ramen.api.common.OutOfRangeException
import com.ramenlog.ramen.api.common.RestaurantNotFoundException
import com.ramenlog.ramen.api.verification.dto.CurrentVerificationData
import com.ramenlog.ramen.api.verification.dto.VerificationCurrentResponse
import com.ramenlog.ramen.api.verification.dto.VerificationError
import com.ramenlog.ramen.api.verification.dto.VerificationErrorData
import com.ramenlog.ramen.api.verification.dto.VerificationHistoryData
import com.ramenlog.ramen.api.verification.dto.VerificationHistoryItem
import com.ramenlog.ramen.api.verification.dto.VerificationHistoryResponse
import com.ramenlog.ramen.api.verification.dto.VerificationVerifyData
import com.ramenlog.ramen.api.verification.dto.VerificationVerifyRequest
import com.ramenlog.ramen.api.verification.dto.VerificationVerifyResponse
import com.ramenlog.ramen.domain.restaurant.RestaurantRepository
import com.ramenlog.ramen.domain.user.User
import com.ramenlog.ramen.domain.visit.VerificationStatus
import com.ramenlog.ramen.domain.visit.VisitHistory
import com.ramenlog.ramen.domain.visit.VisitHistoryRepository
import com.ramenlog.ramen.domain.visit.VisitVerification
import com.ramenlog.ramen.domain.visit.VisitVerificationRepository
import com.ramenlog.ramen.util.LocationUtils
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.LocalDateTime
import java.util.UUID
import kotlin.text.toLong

@Service
class VerificationService (
    private val visitVerificationRepository: VisitVerificationRepository,
    private val restaurantRepository: RestaurantRepository,

    @Value("\${verification.required-stay-seconds}")
    private val requiredStaySeconds: Int,
    private val visitHistoryRepository: VisitHistoryRepository
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

        if (distance > 5000){   // test용 5000
            throw OutOfRangeException()
        }

        var existing =
            visitVerificationRepository
                .findByUserAndStatus(
                    user,
                    VerificationStatus.PENDING
                )

        if (existing != null) {

            autoCompleteVerification(existing)

            existing =
                visitVerificationRepository
                    .findByUserAndStatus(
                        user,
                        VerificationStatus.PENDING
                    )
        }

        if (existing != null) {

            return VerificationEnterResponse(

                success = true,

                data = VerificationData(

                    verificationId =
                        existing.id.toString(),

                    status =
                        existing.status.name.lowercase(),

                    enteredAt =
                        existing.enteredAt,

                    requiredStaySeconds =
                        requiredStaySeconds
                ),

                message = "이미 진행 중인 방문 인증입니다."
            )
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

        if (distance > 5000) { // test용 5000
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
                                maxOf(
                                    0,
                                    requiredStaySeconds.toLong() - currentStaySeconds
                                )
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

        val history =
            visitHistoryRepository
                .findByUserIdAndRestaurantId(
                    user.id!!,
                    verification.restaurant.id!!
                )

        if (history == null) {

            visitHistoryRepository.save(

                VisitHistory(
                    user = user,

                    restaurant =
                        verification.restaurant,

                    visitVerification =
                        verification,

                    visitCount = 1,

                    lastVisitedAt =
                        LocalDateTime.now()
                )
            )
        } else {

            history.visitCount += 1

            history.lastVisitedAt =
                LocalDateTime.now()
        }

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

    @Transactional(readOnly = true)
    fun getMyVerifications(
        user: User,
        status: String?,
        page: Int,
        size: Int
    ): VerificationHistoryResponse {

        val pageable =
            PageRequest.of(page - 1, size)

        val verificationPage =

            if (status != null) {

                visitVerificationRepository
                    .findByUserAndStatus(
                        user,
                        VerificationStatus.valueOf(
                            status.uppercase()
                        ),
                        pageable
                    )

            } else {

                visitVerificationRepository
                    .findByUser(
                        user,
                        pageable
                    )
            }

        val items =
            verificationPage.content.map {

                VerificationHistoryItem(

                    verificationId =
                        it.id.toString(),

                    restaurantId =
                        it.restaurant.id.toString(),

                    restaurantName =
                        it.restaurant.name,

                    status =
                        it.status.name.lowercase(),

                    enteredAt =
                        it.enteredAt,

                    verifiedAt =
                        it.verifiedAt
                )
            }

        return VerificationHistoryResponse(

            success = true,

            data =
                VerificationHistoryData(

                    verifications = items,

                    page = page,

                    size = size,

                    totalElements =
                        verificationPage.totalElements,

                    totalPages =
                        verificationPage.totalPages
                )
        )
    }

    @Transactional(readOnly = true)
    fun getCurrentVerification(
        user: User
    ): VerificationCurrentResponse {

        val verification =
            visitVerificationRepository
                .findByUserAndStatus(
                    user,
                    VerificationStatus.PENDING
                )
                ?: return VerificationCurrentResponse(
                    success = true,
                    data = null
                )

        val elapsed =
            Duration.between(
                verification.enteredAt,
                LocalDateTime.now()
            ).seconds

        val remaining =
            maxOf(
                0,
                requiredStaySeconds - elapsed
            )

        return VerificationCurrentResponse(

            success = true,

            data =
                CurrentVerificationData(

                    verificationId =
                        verification.id!!,

                    restaurantId =
                        verification.restaurant.id!!,

                    status =
                        verification.status.name.lowercase(),

                    remainingSeconds =
                        remaining,

                    enteredAt =
                        verification.enteredAt
                )
        )
    }

    private fun autoCompleteVerification(
        verification: VisitVerification
    ): VisitVerification {

        if (
            verification.status == VerificationStatus.PENDING
        ) {

            val elapsed =
                Duration.between(
                    verification.enteredAt,
                    LocalDateTime.now()
                ).seconds

            if (elapsed >= requiredStaySeconds) {

                verification.status =
                    VerificationStatus.VERIFIED

                verification.verifiedAt =
                    verification.enteredAt.plusSeconds(
                        requiredStaySeconds.toLong()
                    )

                verification.stayDurationSeconds =
                    elapsed.toInt()

                visitVerificationRepository.save(
                    verification
                )
            }
        }

        return verification
    }
}

