package com.coc.cu.controllers

import com.coc.cu.domain.AccountRequestDto
import com.coc.cu.domain.AccountResponseDto
import com.coc.cu.domain.MemberResponseDto
import com.coc.cu.domain.UserRequestDto
import com.coc.cu.domain.models.ApiResponse
import com.coc.cu.services.AccountService
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*



@RequestMapping("/api/v1/accounts")
@RestController
class AccountsController(var accountService: AccountService) {


    @PostMapping
    fun create(@RequestBody model: AccountRequestDto): ApiResponse<AccountResponseDto> {
        return ApiResponse(accountService.create(model), HttpStatus.OK)
    }

    @GetMapping
    fun list(): ApiResponse<List<AccountResponseDto>> {
        return ApiResponse(accountService.list(), HttpStatus.OK)
    }

    @GetMapping("/{id}")
    fun detail(@PathVariable id: String): ApiResponse<AccountResponseDto> {
        return ApiResponse(accountService.single(id), "Success", HttpStatus.OK)
    }


}