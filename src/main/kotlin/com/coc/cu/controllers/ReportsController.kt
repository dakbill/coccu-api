package com.coc.cu.controllers

import com.coc.cu.domain.ClosingBooksResponseDto
import com.coc.cu.domain.models.ApiResponse
import com.coc.cu.services.AccountService
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController


@RequestMapping("/api/v1/reports")
@RestController
class ReportsController(var accountService: AccountService) {


    @GetMapping("/closing-books")
    fun getClosingBooks(): ApiResponse<ClosingBooksResponseDto> {
        val responseDto = ClosingBooksResponseDto()
        return ApiResponse(responseDto, HttpStatus.OK)
    }


    @GetMapping("/dashboard-metrics")
    fun getDashboardMetrics(): ApiResponse<ClosingBooksResponseDto> {
        val responseDto = ClosingBooksResponseDto()
        return ApiResponse(responseDto, HttpStatus.OK)
    }




}