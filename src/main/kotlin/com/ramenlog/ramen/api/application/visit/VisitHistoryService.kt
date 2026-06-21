package com.ramenlog.ramen.api.application.visit

import com.ramenlog.ramen.api.review.dto.PaginationData
import com.ramenlog.ramen.api.visit.dto.RestaurantVisitHistoryData
import com.ramenlog.ramen.api.visit.dto.RestaurantVisitHistoryResponse
import com.ramenlog.ramen.api.visit.dto.VisitHistoryData
import com.ramenlog.ramen.api.visit.dto.VisitHistoryItem
import com.ramenlog.ramen.api.visit.dto.VisitHistoryResponse
import com.ramenlog.ramen.api.visit.dto.VisitRestaurantInfo
import com.ramenlog.ramen.domain.user.User
import com.ramenlog.ramen.domain.visit.VisitHistoryRepository
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class VisitHistoryService (
    private val visitHistoryRepository: VisitHistoryRepository
){
    @Transactional(readOnly = true)
    fun getMyVisitHistory(
        user: User,
        page: Int,
        size: Int
    ): VisitHistoryResponse {

        val pageable =
            PageRequest.of(
                page - 1,
                size,
                Sort.by("lastVisitedAt").descending()
            )

        val histories =
            visitHistoryRepository.findByUserId(
                user.id!!,
                pageable
            )

        val items =
            histories.content.map { history ->

                VisitHistoryItem(

                    restaurant =
                        VisitRestaurantInfo(
                            id = history.restaurant.id!!,
                            name = history.restaurant.name,
                            address = history.restaurant.address
                        ),

                    visitCount =
                        history.visitCount,

                    lastVisitedAt =
                        history.lastVisitedAt
                )
            }

        return VisitHistoryResponse(

            success = true,

            data =
                VisitHistoryData(

                    visitHistory = items,

                    pagination =
                        PaginationData(
                            total = histories.totalElements,
                            page = page,
                            size = size,
                            totalPages = histories.totalPages
                        )
                )
        )
    }

    @Transactional(readOnly = true)
    fun getRestaurantVisitHistory(
        restaurantId: UUID,
        user: User
    ): RestaurantVisitHistoryResponse {

        val history =
            visitHistoryRepository
                .findByUserIdAndRestaurantId(
                    user.id!!,
                    restaurantId
                )
                ?: throw IllegalArgumentException(
                    "방문 기록이 없습니다."
                )

        return RestaurantVisitHistoryResponse(

            success = true,

            data =
                RestaurantVisitHistoryData(

                    restaurantId =
                        restaurantId,

                    visitCount =
                        history.visitCount,

                    lastVisitedAt =
                        history.lastVisitedAt,

                    firstVisitedAt =
                        history.createdAt
                )
        )
    }
}