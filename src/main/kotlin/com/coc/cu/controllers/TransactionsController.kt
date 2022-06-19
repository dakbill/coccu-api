package com.coc.cu.controllers

import com.coc.cu.domain.RawTransactionRequestDto
import com.coc.cu.domain.TransactionRequestDto
import com.coc.cu.domain.TransactionResponseDto
import com.coc.cu.domain.models.ApiResponse
import com.coc.cu.entities.Transaction
import com.coc.cu.services.TransactionsService
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*



@RequestMapping("/api/v1/transactions")
@RestController
class TransactionsController(val transactionsService: TransactionsService, var objectMapper: ObjectMapper) {

    @PostMapping
    fun create(@RequestBody model: RawTransactionRequestDto): ApiResponse<TransactionResponseDto> {

        return ApiResponse(transactionsService.create(model), HttpStatus.OK)
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