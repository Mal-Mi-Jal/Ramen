package com.ramenlog.ramen.api.application.user

import com.ramenlog.ramen.api.user.MyProfileResponse
import com.ramenlog.ramen.api.user.UpdateProfileRequest
import com.ramenlog.ramen.domain.user.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserService (
    private val userRepository: UserRepository
){
    @Transactional(readOnly=true)
    fun getMyProfile(email : String) : MyProfileResponse{
        val user = userRepository.findByEmail(email)
            ?: throw IllegalStateException("존재하지 않는 유저입니다.")

        return MyProfileResponse.from(user)
    }

    @Transactional
    fun updateProfile(email : String, request: UpdateProfileRequest): MyProfileResponse{
        val user = userRepository.findByEmail(email)
            ?: throw IllegalArgumentException("존재하지 않는 유저입니다.")

        if(user.nickname != request.nickname && userRepository.findByNickname(request.nickname) != null){
            throw IllegalArgumentException("이미 사용중인 닉네임입니다.")
        }

        user.updateProfile(request.nickname, request.profile_image_url)

        return MyProfileResponse.from(user)
    }
}