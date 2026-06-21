package com.ramenlog.ramen.api.application.auth

import com.ramenlog.ramen.api.auth.dto.LoginRequest
import com.ramenlog.ramen.api.auth.dto.SignupRequest
import com.ramenlog.ramen.api.auth.dto.TokenData
import com.ramenlog.ramen.api.auth.security.JwtProvider
import com.ramenlog.ramen.domain.user.User
import com.ramenlog.ramen.domain.user.UserRepository
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AuthService (
    private val userRepository: UserRepository,
    private val jwtProvider: JwtProvider,
    private val passwordEncoder: PasswordEncoder
){

    @Transactional
    fun signup(request: SignupRequest){
        if(userRepository.findByEmail(request.email) != null){
            throw IllegalArgumentException("이미 사용중인 이메일입니다.")
        }

        if(userRepository.findByNickname(request.nickname) != null){
            throw IllegalArgumentException("이미 사용중인 닉네임입니다.")
        }

        val newUser = User(
            email = request.email,
            passwordHash = passwordEncoder.encode(request.password)!!,
            nickname = request.nickname
        )

        userRepository.save(newUser)
    }

    @Transactional(readOnly = true)
    fun login(request: LoginRequest): TokenData {
        val user = userRepository.findByEmail(request.email)
            ?: throw IllegalArgumentException("이메일 또는 비밀번호가 일치하지 않습니다.")

        if (!passwordEncoder.matches(request.password, user.passwordHash)) {
            throw IllegalArgumentException("이메일 또는 비밀번호가 일치하지 않습니다.")
        }

        val accessToken = jwtProvider.createToken(user.id!!, user.email, jwtProvider.accessTokenValidityInMilliseconds)
        // 2. 리프레시 토큰 굽기 (14일짜리)
        val refreshToken = jwtProvider.createToken(user.id!!, user.email, jwtProvider.refreshTokenValidityInMilliseconds)

        user.updateRefreshToken(refreshToken)
        userRepository.saveAndFlush(user)

        // 3. 가방에 이쁘게 담아서 던지기
        return TokenData(
            access_token = accessToken,
            refresh_token = refreshToken,
            expires_in = jwtProvider.getExpiresInSeconds()
        )
    }

    @Transactional
    fun logout(accessToken: String){
        val email = jwtProvider.getEmailFromToken(accessToken)

        val user = userRepository.findByEmail(email)
            ?: throw IllegalArgumentException("존재하지 않는 유저입니다.")

        if (user.refreshToken == null) {
            throw IllegalArgumentException("유효하지 않거나 이미 로그아웃된 토큰입니다.")
        }

        user.updateRefreshToken(null)
        userRepository.saveAndFlush(user)
        // 나중에 Redis 블랙리스트로 구현할 예정

        println("${user.nickname}님 로그아웃 완료!")
    }
}