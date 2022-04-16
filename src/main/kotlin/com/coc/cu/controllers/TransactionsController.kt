package com.coc.cu.controllers

import com.coc.cu.domain.TransactionResponseDto
import com.coc.cu.domain.models.ApiResponse
import com.coc.cu.services.TransactionsService
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*



@RequestMapping("/api/v1/transactions")
@RestController
class TransactionsController(val transactionsService: TransactionsService) {

    @PostMapping
    fun create(): ApiResponse<Array<String>> {
        return ApiResponse(arrayOf("1"), HttpStatus.OK)
    }

    @GetMapping
    fun list(@RequestParam memberId: Long): ApiResponse<List<TransactionResponseDto>> {
        return ApiResponse(transactionsService.list(memberId), HttpStatus.OK)
    }

    @GetMapping("/{id}")
    fun detail(@PathVariable id: Long): ApiResponse<TransactionResponseDto> {
        return ApiResponse(transactionsService.single(id), "Success", HttpStatus.OK)
    }


}