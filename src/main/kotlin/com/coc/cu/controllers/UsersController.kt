package com.coc.cu.controllers


import com.coc.cu.domain.*
import com.coc.cu.domain.models.ApiResponse
import com.coc.cu.repositories.GuarantorRepository
import com.coc.cu.repositories.MemberAccountRepository
import com.coc.cu.services.AccountService
import com.coc.cu.services.TransactionsService
import com.coc.cu.services.UsersService
import com.coc.cu.utils.JwtUtils
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.domain.JpaSort
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import org.springframework.web.client.RestTemplate
import java.math.BigInteger


@RequestMapping("/api/v1/users")
@RestController
class UsersController(
    val usersService: UsersService,
    val accountService: AccountService,
    val memberAccountRepository: MemberAccountRepository,
    val guarantorRepository: GuarantorRepository,
    val objectMapper: ObjectMapper,
    val transactionsService: TransactionsService,
    val restTemplate: RestTemplate,
    val jwtUtils: JwtUtils
) {

    @PreAuthorize("isAuthenticated()")
    @PostMapping
    fun create(@RequestBody model: UserRequestDto): ApiResponse<MemberResponseDto> {
        return ApiResponse(usersService.create(model), HttpStatus.OK)
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping
    fun list(
        @RequestParam(name = "exportToExcel", defaultValue = "false") exportToExcel: Boolean,
        @RequestParam(name = "page", defaultValue = "0") page: Int,
        @RequestParam(name = "size", defaultValue = "10") size: Int,
        @RequestParam(name = "q", defaultValue = "") query: String,
        @RequestParam(name = "getGuarantorDebtorAccounts", defaultValue = "false") getGuarantorDebtorAccounts: Boolean,
        @RequestParam(name = "properties", required = false) properties: Array<String>?,
        @RequestParam(name = "direction", required = false) direction: Array<String>?,

        ): ApiResponse<List<MemberResponseDto>> {

        var sort: Sort = JpaSort.unsafe(Sort.Direction.ASC, "(name)")
        if (properties != null && properties.isNotEmpty()) {

            sort = JpaSort.unsafe(Sort.Direction.valueOf(direction!![0].uppercase()), properties[0])
            properties.forEachIndexed { index, property ->
                run {
                    if (index > 0) {
                        sort.and(JpaSort.unsafe(Sort.Direction.valueOf(direction[index].uppercase()), property))
                    }
                }
            }
        }

        val membersPage = usersService.list(query, PageRequest.of(if (exportToExcel) 0 else page, if (exportToExcel) Int.MAX_VALUE else size, sort))
        if (getGuarantorDebtorAccounts) {
            val accountTypeRef = object : TypeReference<MinimalAccountResponseDto>() {}
            val guarantorTypeRef = object : TypeReference<GuarantorResponseDto>() {}
            membersPage.content!!.stream().forEach { m ->
                run {
                    m.availableBalance = memberAccountRepository.getAvailableBalance(m.id)
                    m.guarantorDebtorAccounts =
                        accountService.getGuarantorDebtorAccounts(m.id!!).map {

                            val guarantorAccountResponseDto = GuarantorAccountResponseDto(
                            )

                            val account = memberAccountRepository.findById(it["account_id"] as String).get()
                            guarantorAccountResponseDto.account = objectMapper.convertValue(
                                account,
                                accountTypeRef
                            )

                            val guarantor = guarantorRepository.findById((it["guarantors_id"] as BigInteger).toLong()).get()
                            guarantorAccountResponseDto.guarantor = objectMapper.convertValue(
                                guarantor,
                                guarantorTypeRef
                            )

                            guarantorAccountResponseDto

                        }

                }
            }
        }

        return ApiResponse(membersPage.content, "OK", HttpStatus.OK, page, size, membersPage.totalElements)
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{id}")
    fun detail(@PathVariable id: Long): ApiResponse<MemberResponseDto> {

        var memberResponseDto = usersService.single(id)
        memberResponseDto!!.totalSavings = transactionsService.getTotalSavings(id)
        memberResponseDto!!.totalWithdrawals = transactionsService.getTotalWithdrawals(id)
        return ApiResponse(memberResponseDto, "Success", HttpStatus.OK)
    }

    @PreAuthorize("isAuthenticated()")
    @PutMapping("/{id}")
    fun update(@PathVariable id: Long, @ModelAttribute model: UserRequestDto): ApiResponse<MemberResponseDto> {

        return ApiResponse(usersService.update(id, model), "Success", HttpStatus.OK)
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/send-sms")
    fun sendSms(
        @RequestParam message: String,
        @RequestParam to: String,
        @RequestParam sender: String
    ): ApiResponse<String> {

        val res = jwtUtils.sendSms(sender,to, message, restTemplate)
        return ApiResponse(res, "Success", HttpStatus.OK)
    }


}