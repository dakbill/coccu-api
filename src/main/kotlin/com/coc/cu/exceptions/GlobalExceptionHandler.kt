package com.coc.cu.exceptions

import com.coc.cu.domain.models.ApiResponse
import org.springframework.http.HttpStatus
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.context.request.WebRequest

@ControllerAdvice
class GlobalExceptionHandler {

    @ResponseStatus(value = HttpStatus.FORBIDDEN)
    @ExceptionHandler(BadCredentialsException::class)
    fun handleForbiddenException(ex: BadCredentialsException, request: WebRequest?): ApiResponse<Any?>? {
        val errors: List<Map<String, String>> = listOf(mapOf("field" to "error", "message" to ex.message!!))
        return ApiResponse(
            null,
            ex.message!!,
            HttpStatus.FORBIDDEN,
            errors
        )
    }
}