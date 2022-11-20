package com.coc.cu.controllers


import com.coc.cu.domain.MemberResponseDto
import com.coc.cu.domain.UserRequestDto
import com.coc.cu.domain.models.ApiResponse
import com.coc.cu.services.TransactionsService
import com.coc.cu.services.UsersService
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import org.springframework.web.client.RestTemplate


@RequestMapping("/api/v1/users")
@RestController
class UsersController(val usersService: UsersService,val transactionsService: TransactionsService,val restTemplate: RestTemplate) {

    @PreAuthorize("isAuthenticated()")
    @PostMapping
    fun create(@RequestBody model: UserRequestDto): ApiResponse<MemberResponseDto> {
        return ApiResponse(usersService.create(model), HttpStatus.OK)
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping
    fun list(@RequestParam(name = "q", defaultValue = "") query: String): ApiResponse<List<MemberResponseDto>> {
        return ApiResponse(usersService.list(query), HttpStatus.OK)
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{id}")
    fun detail(@PathVariable id: Long): ApiResponse<MemberResponseDto> {

        var memberResponseDto = usersService.single(id)
        memberResponseDto!!.totalSavings = transactionsService.getTotalSavings(id)
        memberResponseDto!!.totalWithdrawals = transactionsService.getTotalWithdrawals(id)
        memberResponseDto!!.totalBalance = memberResponseDto.totalSavings - memberResponseDto.totalWithdrawals;
        return ApiResponse(memberResponseDto, "Success", HttpStatus.OK)
    }

    @PreAuthorize("isAuthenticated()")
    @PutMapping("/{id}")
    fun update(@PathVariable id: Long, @ModelAttribute model: UserRequestDto): ApiResponse<MemberResponseDto> {

        return ApiResponse(usersService.update(id,model), "Success", HttpStatus.OK)
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/send-sms")
    fun sendSms(@RequestParam message: String,@RequestParam to: String,@RequestParam sender: String): ApiResponse<String> {

        val res = restTemplate.getForObject("https://apps.mnotify.net/smsapi?key=WsdWfqH7Kr6fyiXDgLS25Ju62&to=${to}&msg=${message}&sender_id=${sender}", String::class.java)
        return ApiResponse(res, "Success", HttpStatus.OK)
    }


}