package com.ramenlog.ramen.api.common

class CannotLikeOwnReviewException : RuntimeException("본인 리뷰는 추천할 수 없습니다.") {
}