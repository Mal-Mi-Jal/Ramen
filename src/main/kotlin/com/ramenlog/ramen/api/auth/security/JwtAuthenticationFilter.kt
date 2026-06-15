package com.ramenlog.ramen.api.auth.security

import com.ramenlog.ramen.domain.user.UserRepository
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.User
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class JwtAuthenticationFilter(
    private val jwtProvider: JwtProvider,
    private val userRepository: UserRepository
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        // 1. HTTP 요청 헤더에서 "Authorization" 값을 꺼내옵니다.
        val authHeader = request.getHeader("Authorization")

        // 2. 헤더에 Bearer 토큰이 정상적으로 실려있는지 확인합니다.
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            val token = authHeader.substringAfter("Bearer ")

            try {
                val userId =
                    jwtProvider.getUserIdFromToken(token)

                val user =
                    userRepository.findById(userId)
                        .orElseThrow {
                            IllegalArgumentException("사용자를 찾을 수 없습니다.")
                        }

                val authentication = UsernamePasswordAuthenticationToken(
                    user,
                    null,
                    emptyList()
                )

                SecurityContextHolder.getContext().authentication = authentication

            } catch (e: Exception) {
                logger.error("JWT 토큰 검증 실패: ${e.message}")
            }
        }

        filterChain.doFilter(request, response)
    }
}