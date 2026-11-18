package com.ramenlog.ramen.api.auth.security

import io.jsonwebtoken.Claims
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets
import java.util.Date
import java.util.UUID

/** 토큰 종류. 토큰 안의 "type" 값으로 들어간다. */
enum class TokenType(val claimValue: String) {
    ACCESS("access"),
    REFRESH("refresh"),
}

@Component
class JwtProvider (
    @Value("\${jwt.secret}")
    private val secretKeyString: String
){
    private val key = Keys.hmacShaKeyFor(secretKeyString.toByteArray(StandardCharsets.UTF_8))

    // 토큰 만료 시간 (1일 = 24시간 * 60분 * 60초 * 1000밀리초)
    val accessTokenValidityInMilliseconds: Long = 24 * 60 * 60 * 1000

    // 14일
    val refreshTokenValidityInMilliseconds: Long = 14 * 24 * 60 * 60 * 1000

    fun createAccessToken(userId: UUID, email: String): String =
        createToken(userId, email, TokenType.ACCESS, accessTokenValidityInMilliseconds)

    fun createRefreshToken(userId: UUID, email: String): String =
        createToken(userId, email, TokenType.REFRESH, refreshTokenValidityInMilliseconds)

    private fun createToken(userId: UUID, email: String, type: TokenType, validityInMilliseconds: Long): String {
        val claims = Jwts.claims().setSubject(email)
        claims["userId"] = userId.toString() // 토큰 내부에 유저 고유 ID도 심어두기
        claims[TYPE_CLAIM] = type.claimValue // access / refresh 구분 (예전엔 만료 시간만 달랐음)

        val now = Date()
        val validity = Date(now.time + validityInMilliseconds)

        return Jwts.builder()
            .setClaims(claims)
            .setIssuedAt(now)
            .setExpiration(validity)
            .signWith(key, SignatureAlgorithm.HS256)
            .compact()
    }

    fun getExpiresInSeconds(): Long {
        return accessTokenValidityInMilliseconds / 1000
    }

    /** API 요청용: access 토큰일 때만 userId를 돌려준다 */
    fun getUserIdFromAccessToken(token: String): UUID =
        UUID.fromString(parse(token, TokenType.ACCESS)["userId"] as String)

    /** 로그아웃용: access 토큰일 때만 email을 돌려준다 */
    fun getEmailFromAccessToken(token: String): String =
        parse(token, TokenType.ACCESS).subject

    /** 재발급용: refresh 토큰일 때만 email을 돌려준다 */
    fun getEmailFromRefreshToken(token: String): String =
        parse(token, TokenType.REFRESH).subject

    /** 서명·만료를 확인하고, 토큰 종류가 기대한 것과 다르면 예외 */
    private fun parse(token: String, expected: TokenType): Claims {
        val claims = Jwts.parserBuilder()
            .setSigningKey(key)
            .build()
            .parseClaimsJws(token)
            .body
        if (claims[TYPE_CLAIM] != expected.claimValue) {
            throw JwtException("${expected.claimValue} 토큰이 아닙니다.")
        }
        return claims
    }

    companion object {
        const val TYPE_CLAIM = "type"
    }
}
