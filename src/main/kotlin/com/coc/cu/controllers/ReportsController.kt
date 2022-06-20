package com.coc.cu.controllers

import com.coc.cu.domain.ClosingBooksResponseDto
import com.coc.cu.domain.DashboardResponseDto
import com.coc.cu.domain.models.ApiResponse
import com.coc.cu.services.AccountService
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.util.*


@RequestMapping("/api/v1/reports")
@RestController
class ReportsController(var accountService: AccountService) {


    @GetMapping("/closing-books")
    fun getClosingBooks(@DateTimeFormat(pattern = "yyyy-MM-dd") @RequestParam dayInFocus: LocalDate): ApiResponse<ClosingBooksResponseDto> {
        return ApiResponse(accountService.getClosingBooksMetrics(dayInFocus), HttpStatus.OK)
    }


    @GetMapping("/dashboard-metrics")
    fun getDashboardMetrics(
        @DateTimeFormat(pattern = "yyyy-MM-dd") @RequestParam startDate: LocalDate,
        @DateTimeFormat(pattern = "yyyy-MM-dd") @RequestParam endDate: LocalDate
    ): ApiResponse<DashboardResponseDto> {
        return ApiResponse(accountService.getDashboardMetrics(startDate, endDate), HttpStatus.OK)
    }




}