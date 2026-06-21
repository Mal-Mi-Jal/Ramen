package com.ramenlog.ramen.api.auth.security

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets
import java.util.Date
import java.util.UUID

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

        // 💡 유저 정보를 바탕으로 JWT 토큰 글자를 생성해주는 마법의 메서드!
        fun createToken(userId: UUID, email: String, validityInMilliseconds: Long): String {
            val claims = Jwts.claims().setSubject(email)
            claims["userId"] = userId.toString() // 토큰 내부에 유저 고유 ID도 심어두기!

            val now = Date()
            val validity = Date(now.time + validityInMilliseconds)

            return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(validity)
                .signWith(key, SignatureAlgorithm.HS256) // HS256 알고리즘으로 암호화 지장 찍기
                .compact()
        }

        fun getExpiresInSeconds(): Long {
            return accessTokenValidityInMilliseconds / 1000
        }

        fun getEmailFromToken(token: String): String {
            return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .body
                .subject // 👈 로그인할 때 심었던 email이 나옵니다!
        }

        fun getUserIdFromToken(
            token: String
        ): UUID {

            val claims =
                Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .body

            return UUID.fromString(
                claims["userId"] as String
            )
        }
    }