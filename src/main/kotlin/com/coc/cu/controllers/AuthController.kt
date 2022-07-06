package com.coc.cu.controllers

import com.coc.cu.domain.AuthResponseDto
import com.coc.cu.domain.ExchangeTokenRequestDto
import com.coc.cu.domain.models.ApiResponse
import com.coc.cu.services.UsersService
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*



@RequestMapping("/api/v1/auth")
@RestController
class AuthController(val usersService: UsersService) {


    @PostMapping("/exchange-token")
    fun exchangeSocialToken(@RequestBody model: ExchangeTokenRequestDto): ApiResponse<AuthResponseDto> {

        return ApiResponse(usersService.exchangeSocialToken(model), "Success", HttpStatus.OK)
    }


}