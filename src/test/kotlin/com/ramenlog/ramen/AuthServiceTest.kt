package com.ramenlog.ramen.api.application.auth

import com.ramenlog.ramen.api.auth.dto.LoginRequest
import com.ramenlog.ramen.api.auth.security.JwtProvider
import com.ramenlog.ramen.domain.user.User
import com.ramenlog.ramen.domain.user.UserRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.BDDMockito.given
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.security.crypto.password.PasswordEncoder
import java.util.UUID

/**
 * 실제로 겪었던 버그(@Transactional(readOnly = true)인 login()에서
 * refreshToken을 저장하려다 실패했던 문제)에 대한 회귀 테스트.
 * login()에 readOnly = true가 다시 붙으면 이 테스트가 실패해야 정상.
 */
@ExtendWith(MockitoExtension::class)
class AuthServiceTest {

    @Mock
    lateinit var userRepository: UserRepository

    @Mock
    lateinit var jwtProvider: JwtProvider

    @Mock
    lateinit var passwordEncoder: PasswordEncoder

    @InjectMocks
    lateinit var authService: AuthService

    private lateinit var user: User

    private val accessTokenValidity = 3_600_000L
    private val refreshTokenValidity = 1_209_600_000L

    @BeforeEach
    fun setUp() {
        user = User(
            id = UUID.randomUUID(),
            email = "test@ramenlog.com",
            passwordHash = "encoded-password",
            nickname = "말미잘"
        )
    }

    @Test
    fun `로그인 성공 시 refreshToken이 DB에 저장된다`() {
        // given
        val request = LoginRequest(email = "test@ramenlog.com", password = "1234")

        given(userRepository.findByEmail(request.email)).willReturn(user)
        given(passwordEncoder.matches(request.password, user.passwordHash)).willReturn(true)
        given(jwtProvider.accessTokenValidityInMilliseconds).willReturn(accessTokenValidity)
        given(jwtProvider.refreshTokenValidityInMilliseconds).willReturn(refreshTokenValidity)
        given(jwtProvider.createToken(user.id!!, user.email, accessTokenValidity)).willReturn("access-token")
        given(jwtProvider.createToken(user.id!!, user.email, refreshTokenValidity)).willReturn("refresh-token")
        given(jwtProvider.getExpiresInSeconds()).willReturn(3600L)

        // when
        val result = authService.login(request)

        // then
        assertEquals("refresh-token", user.refreshToken)
        verify(userRepository).saveAndFlush(user)
        assertEquals("access-token", result.access_token)
        assertEquals("refresh-token", result.refresh_token)
    }

    @Test
    fun `비밀번호가 틀리면 예외를 던진다`() {
        val request = LoginRequest(email = "test@ramenlog.com", password = "wrong-password")

        given(userRepository.findByEmail(request.email)).willReturn(user)
        given(passwordEncoder.matches(request.password, user.passwordHash)).willReturn(false)

        org.junit.jupiter.api.assertThrows<IllegalArgumentException> {
            authService.login(request)
        }
    }

    @Test
    fun `존재하지 않는 이메일이면 예외를 던진다`() {
        val request = LoginRequest(email = "nobody@ramenlog.com", password = "1234")

        given(userRepository.findByEmail(request.email)).willReturn(null)

        org.junit.jupiter.api.assertThrows<IllegalArgumentException> {
            authService.login(request)
        }
    }
}
