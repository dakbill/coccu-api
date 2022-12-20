package com.coc.cu.controllers

import com.coc.cu.domain.GuarantorResponseDto
import com.coc.cu.domain.RawTransactionRequestDto
import com.coc.cu.domain.TransactionResponseDto
import com.coc.cu.domain.models.ApiResponse
import com.coc.cu.entities.Member
import com.coc.cu.repositories.GuarantorRepository
import com.coc.cu.services.TransactionsService
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.domain.JpaSort
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.time.LocalDate


@RequestMapping("/api/v1/guarantors")
@RestController
class GuarantorsController(val guarantorRepository: GuarantorRepository, val objectMapper: ObjectMapper) {


    @PreAuthorize("isAuthenticated()")
    @DeleteMapping("/{id}")
    fun releaseFunds(@PathVariable id: Long): ApiResponse<GuarantorResponseDto> {
        val guarantor = guarantorRepository.findById(id).get()
        guarantor.fundReleased = true

        val guarantorTypeRef = object : TypeReference<GuarantorResponseDto>() {}
        return ApiResponse(
            objectMapper.convertValue(guarantorRepository.save(guarantor), guarantorTypeRef),
            "Success",
            HttpStatus.OK
        )
    }


}