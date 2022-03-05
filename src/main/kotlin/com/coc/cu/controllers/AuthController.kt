package com.coc.cu.controllers

import  com.coc.cu.domain.models.ApiResponse
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*



@RequestMapping("/api/v1/auth")
@RestController
class AuthController {

    @PreAuthorize("permitAll()")
    @PostMapping("/exchange-token")
    fun exchangeSocialToken(): ApiResponse<String> {
        return ApiResponse(null, "Success", HttpStatus.OK)
    }


}