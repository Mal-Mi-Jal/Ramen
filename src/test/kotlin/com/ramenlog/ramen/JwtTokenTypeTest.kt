package com.ramenlog.ramen

import com.ramenlog.ramen.api.auth.security.JwtAuthenticationFilter
import com.ramenlog.ramen.api.auth.security.JwtProvider
import com.ramenlog.ramen.domain.user.User
import com.ramenlog.ramen.domain.user.UserRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.security.Keys
import java.util.Date
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.springframework.mock.web.MockFilterChain
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.core.context.SecurityContextHolder
import java.util.Optional
import java.util.UUID

/**
 * 진짜 JwtProvider로 토큰을 만들어 진짜 필터에 넣어 본다 (Spring 컨텍스트·DB 없음).
 * 문제: access(24시간)와 refresh(14일) 토큰이 만료 시간 말고는 똑같아서,
 *      refresh 토큰을 Authorization 헤더에 넣어도 필터가 로그인 상태로 만들어 줬다.
 */
class JwtTokenTypeTest {

    private val jwtProvider = JwtProvider(SECRET)
    private val user = User(id = UUID.randomUUID(), email = "jwt@test.com", passwordHash = "x", nickname = "tester")
    private val userRepository: UserRepository = mock {
        on { findById(any()) } doReturn Optional.of(user)
    }
    private val filter = JwtAuthenticationFilter(jwtProvider, userRepository)

    @AfterEach
    fun clear() = SecurityContextHolder.clearContext()

    /** Bearer 토큰을 넣어 필터를 한 번 통과시키고, 로그인된 사용자(없으면 null)를 돌려준다 */
    private fun authenticatedUserWith(token: String): Any? {
        val request = MockHttpServletRequest().apply { addHeader("Authorization", "Bearer $token") }
        filter.doFilter(request, MockHttpServletResponse(), MockFilterChain())
        return SecurityContextHolder.getContext().authentication?.principal
    }

    @Test
    fun `access 토큰은 필터를 통과한다`() {
        val access = jwtProvider.createAccessToken(user.id!!, user.email)
        assertEquals(user, authenticatedUserWith(access))
    }

    @Test
    fun `refresh 토큰을 access 자리에 넣으면 로그인되지 않는다`() {
        val refresh = jwtProvider.createRefreshToken(user.id!!, user.email)
        assertNull(authenticatedUserWith(refresh), "refresh 토큰으로 API에 로그인됨")
    }

    @Test
    fun `재발급에는 refresh 토큰만 쓸 수 있다`() {
        val access = jwtProvider.createAccessToken(user.id!!, user.email)
        val refresh = jwtProvider.createRefreshToken(user.id!!, user.email)
        assertEquals(user.email, jwtProvider.getEmailFromRefreshToken(refresh))
        assertThrows(JwtException::class.java) { jwtProvider.getEmailFromRefreshToken(access) }
    }

    @Test
    fun `type이 없는 예전 토큰은 거부된다`() {
        val old = Jwts.builder()
            .setSubject(user.email)
            .claim("userId", user.id.toString())
            .setExpiration(Date(System.currentTimeMillis() + 60_000))
            .signWith(Keys.hmacShaKeyFor(SECRET.toByteArray()), SignatureAlgorithm.HS256)
            .compact()
        assertNull(authenticatedUserWith(old))
    }

    companion object {
        const val SECRET = "test-secret-key-for-unit-test-only-32bytes!!"
    }
}
