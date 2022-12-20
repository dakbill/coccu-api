package com.coc.cu.controllers

import com.coc.cu.domain.RawTransactionRequestDto
import com.coc.cu.domain.TransactionResponseDto
import com.coc.cu.domain.models.ApiResponse
import com.coc.cu.services.TransactionsService
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.domain.JpaSort
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.time.LocalDate


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
        @RequestParam(name = "transactionType", required = false) transactionType: String?,
        @DateTimeFormat(pattern = "yyyy-MM-dd") @RequestParam(required = false) startDate: LocalDate,
        @DateTimeFormat(pattern = "yyyy-MM-dd") @RequestParam(required = false) endDate: LocalDate,
        @RequestParam(name = "exportToExcel", defaultValue = "false") exportToExcel: Boolean,
        @RequestParam(name = "page", defaultValue = "0") page: Int,
        @RequestParam(name = "size", defaultValue = "10") size: Int,
        @RequestParam(required = false, defaultValue = "0") memberId: Long,
        @RequestParam(required = false) accountId: String?,
        @RequestParam(name = "properties", required = false) properties: Array<String>?,
        @RequestParam(name = "direction", required = false) direction: Array<String>?,
    ): ApiResponse<List<TransactionResponseDto>> {

        val sortMaps = mapOf(
            "(name)" to "(" +
                        "SELECT member.name FROM " +
                            "member LEFT JOIN account ON(member.id=account.member_id) " +
                        "WHERE account.id=transaction.account_id" +
                    ")",
        )

        var sort: Sort = JpaSort.unsafe(Sort.Direction.ASC, sortMaps.getOrDefault("(name)","(name)"))
        if (properties != null && properties.isNotEmpty()) {

            sort = JpaSort.unsafe(Sort.Direction.valueOf(direction!![0].uppercase()), sortMaps.getOrDefault(properties[0],properties[0]))
            properties.forEachIndexed { index, property ->
                run {
                    if (index > 0) {
                        sort.and(JpaSort.unsafe(Sort.Direction.valueOf(direction[index].uppercase()), sortMaps.getOrDefault(property,property)))
                    }
                }
            }
        }


        val transactionsPage =
            transactionsService.list(
                memberId,
                accountId,
                transactionType,
                startDate,
                endDate,
                PageRequest.of(if (exportToExcel) 0 else page, if (exportToExcel) Int.MAX_VALUE else size, sort)
            )

        return ApiResponse(transactionsPage.content, "OK", HttpStatus.OK, page, size, transactionsPage.totalElements)
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{id}")
    fun detail(@PathVariable id: Long): ApiResponse<TransactionResponseDto> {
        return ApiResponse(transactionsService.single(id), "Success", HttpStatus.OK)
    }


}