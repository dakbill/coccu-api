package com.coc.cu.controllers


import com.coc.cu.domain.MemberResponseDto
import com.coc.cu.domain.UserRequestDto
import com.coc.cu.domain.models.ApiResponse
import com.coc.cu.entities.Account
import com.coc.cu.entities.Member
import com.coc.cu.entities.Transaction
import com.coc.cu.services.UsersService
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*


@RequestMapping("/api/v1/users")
@RestController
class UsersController(val usersService: UsersService, var objectMapper: ObjectMapper) {


    @PostMapping
    fun create(@RequestBody model: UserRequestDto): ApiResponse<MemberResponseDto> {
        return ApiResponse(usersService.create(model), HttpStatus.OK)
    }

    @GetMapping
    fun list(@RequestParam(name = "q", defaultValue = "") query: String): ApiResponse<List<MemberResponseDto>> {
        return ApiResponse(usersService.list(query), HttpStatus.OK)
    }

    @GetMapping("/{id}")
    fun detail(@PathVariable id: Long): ApiResponse<MemberResponseDto> {

        return ApiResponse(usersService.single(id), "Success", HttpStatus.OK)
    }

    @PutMapping("/{id}")
    fun update(@PathVariable id: Long, @RequestBody model: UserRequestDto): ApiResponse<MemberResponseDto> {

        return ApiResponse(usersService.update(id,model), "Success", HttpStatus.OK)
    }


}