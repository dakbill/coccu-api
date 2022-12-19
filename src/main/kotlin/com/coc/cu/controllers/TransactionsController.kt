package com.coc.cu.controllers

import com.coc.cu.domain.RawTransactionRequestDto
import com.coc.cu.domain.TransactionResponseDto
import com.coc.cu.domain.models.ApiResponse
import com.coc.cu.services.TransactionsService
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*


@RequestMapping("/api/v1/transactions")
@RestController
class TransactionsController(val transactionsService: TransactionsService) {

    @PreAuthorize("isAuthenticated()")
    @PostMapping
    fun create(@RequestBody model: RawTransactionRequestDto): ApiResponse<TransactionResponseDto> {

        return ApiResponse(transactionsService.create(model), HttpStatus.OK)
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping
    fun list(
        @RequestParam(name = "page", defaultValue = "0") page: Int,
        @RequestParam(name = "size", defaultValue = "10") size: Int,
        @RequestParam(required = false) memberId: Long?
    ): ApiResponse<List<TransactionResponseDto>> {

        return ApiResponse(transactionsService.list(memberId, PageRequest.of(page, size)), HttpStatus.OK)
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{id}")
    fun detail(@PathVariable id: Long): ApiResponse<TransactionResponseDto> {
        return ApiResponse(transactionsService.single(id), "Success", HttpStatus.OK)
    }


}