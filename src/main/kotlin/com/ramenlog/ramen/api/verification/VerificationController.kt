package com.ramenlog.ramen.api.verification

import com.ramenlog.ramen.api.application.verification.VerificationService
import com.ramenlog.ramen.domain.user.User
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/verifications")
class VerificationController (
    private val verificationService: VerificationService
){

    @PostMapping("/enter")
    fun startVerification(
        @AuthenticationPrincipal user: User,
        @RequestBody request: VerificationEnterRequest
    ) : ResponseEntity<VerificationEnterResponse>{
        val response = verificationService.startVerification(
            user,
            request
        )

        return ResponseEntity.status(HttpStatus.CREATED)
            .body(response)
    }

    @PostMapping("/{verificationId}/verify")
    fun verify(
        @PathVariable verificationId: UUID,
        @AuthenticationPrincipal user: User,
        @RequestBody request: VerificationVerifyRequest
    ): ResponseEntity<VerificationVerifyResponse>{

        val response = verificationService.verifyVerification(
            verificationId,
            user,
            request
        )
        return ResponseEntity.ok(response)
    }



}