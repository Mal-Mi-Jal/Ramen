package com.ramenlog.ramen.api.verification

import com.ramenlog.ramen.api.application.verification.VerificationService
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/verifications")
class VerificationController (
    private val verificationService: VerificationService
){



}