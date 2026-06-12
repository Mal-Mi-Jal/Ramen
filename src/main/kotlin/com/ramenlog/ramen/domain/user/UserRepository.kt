package com.ramenlog.ramen.domain.user

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID


interface UserRepository : JpaRepository<User, UUID>{
    fun findByEmail(email: String) : User?

    fun findByNickname(nickname: String) : User?

    fun findByRefreshToken(token : String): User?
}