package com.coc.cu.controllers


import com.coc.cu.domain.UserResponseDto
import com.coc.cu.domain.models.ApiResponse
import com.coc.cu.services.UsersService
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*


@CrossOrigin(origins =  ["http://localhost:3000","https://coccu.sentigroup.com"])
@RequestMapping("/api/v1/users")
@RestController
class UsersController(val usersService: UsersService) {


    @GetMapping
        fun list(@RequestParam(name = "q", defaultValue = "") query: String): ApiResponse<List<UserResponseDto>> {
        return ApiResponse(usersService.list(query), HttpStatus.OK)
    }

    @GetMapping("/{id}")
    fun detail(@PathVariable id: Long): ApiResponse<UserResponseDto> {

        return ApiResponse(usersService.single(id), "Success", HttpStatus.OK)
    }


}