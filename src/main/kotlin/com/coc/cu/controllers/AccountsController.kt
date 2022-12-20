package com.coc.cu.controllers

import com.coc.cu.domain.AccountRequestDto
import com.coc.cu.domain.AccountResponseDto
import com.coc.cu.domain.models.ApiResponse
import com.coc.cu.services.AccountService
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.domain.JpaSort
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*


@RequestMapping("/api/v1/accounts")
@RestController
class AccountsController(var accountService: AccountService) {


    @PreAuthorize("isAuthenticated()")
    @PostMapping
    fun create(@RequestBody model: AccountRequestDto): ApiResponse<AccountResponseDto> {
        return ApiResponse(accountService.create(model), HttpStatus.OK)
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping
    fun list(): ApiResponse<List<AccountResponseDto>> {
        return ApiResponse(accountService.list(), HttpStatus.OK)
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{id}")
    fun detail(@PathVariable id: String): ApiResponse<AccountResponseDto> {
        return ApiResponse(accountService.single(id), "Success", HttpStatus.OK)
    }


    @PreAuthorize("isAuthenticated()")
    @GetMapping("/debtors")
    fun getDebtors(
        @RequestParam(name = "exportToExcel", defaultValue = "false") exportToExcel: Boolean,
        @RequestParam(name = "q", defaultValue = "_") query: String,
        @RequestParam(name = "page", defaultValue = "0") page: Int,
        @RequestParam(name = "size", defaultValue = "10") size: Int,
        @RequestParam(name = "properties", required = false) properties: Array<String>?,
        @RequestParam(name = "direction", required = false) direction: Array<String>?,

        ): ApiResponse<List<AccountResponseDto>> {

        val sortMaps = mapOf(
            "(name)" to "(member.name)",
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

        val debtorsPage = accountService.getDebtors(query, PageRequest.of(if (exportToExcel) 0 else page, if (exportToExcel) Int.MAX_VALUE else size, sort))
        return ApiResponse(debtorsPage.content, "OK", HttpStatus.OK, page, size, debtorsPage.totalElements)
    }


}