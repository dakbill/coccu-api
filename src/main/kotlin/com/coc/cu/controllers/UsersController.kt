package com.coc.cu.controllers


import com.coc.cu.domain.MemberResponseDto
import com.coc.cu.domain.models.ApiResponse
import com.coc.cu.services.UsersService
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*


@RequestMapping("/api/v1/users")
@RestController
class UsersController(val usersService: UsersService) {


    @GetMapping
    fun list(@RequestParam(name = "q", defaultValue = "") query: String): ApiResponse<List<MemberResponseDto>> {
        return ApiResponse(usersService.list(query), HttpStatus.OK)
    }

    @GetMapping("/{id}")
    fun detail(@PathVariable id: Long): ApiResponse<MemberResponseDto> {

        return ApiResponse(usersService.single(id), "Success", HttpStatus.OK)
    }


}