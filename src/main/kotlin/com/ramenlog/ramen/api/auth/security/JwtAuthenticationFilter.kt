package com.ramenlog.ramen.api.auth.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.User
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component // 💡 스프링이 이 필터를 관리하도록 컴포넌트로 등록!
class JwtAuthenticationFilter(
    private val jwtProvider: JwtProvider
) : OncePerRequestFilter() { // 💡 매 요청마다 딱 '한 번'만 실행되는 시큐리티 필터 상속

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
                // 3. 💡 [중요] 토큰이 유효한지 검증하고, 토큰 내부에서 이메일을 추출합니다!
                // (아까 우리가 만든 getEmailFromToken 함수를 활용해요)
                val email = jwtProvider.getEmailFromToken(token)

                // 4. 스프링 시큐리티가 "오, 이 유저 인증됐네!"라고 인식할 수 있도록 인증 티켓을 만듭니다.
                val userDetails = User.withUsername(email).password("").authorities(emptyList()).build()
                val authentication = UsernamePasswordAuthenticationToken(userDetails, null, userDetails.authorities)

                // 5. 🎯 시큐리티 전용 보관함(Context)에 이 인증 티켓을 쏙 넣어둡니다.
                // 이렇게 넣어두면 나중에 컨트롤러에서 "지금 로그인한 사람 누구야?" 하고 꺼내 쓸 수 있어요!
                SecurityContextHolder.getContext().authentication = authentication

            } catch (e: Exception) {
                // 토큰이 만료되었거나 위조되었다면 인증 처리를 하지 않고 조용히 넘어갑니다.
                // (어차피 다음 시큐리티 설정에서 문을 잠가버릴 거니까요!)
                logger.error("JWT 토큰 검증 실패: ${e.message}")
            }
        }

        // 6. 다음 보안관(필터)으로 요청을 넘겨줍니다. "너도 검사해봐~"
        filterChain.doFilter(request, response)
    }
}