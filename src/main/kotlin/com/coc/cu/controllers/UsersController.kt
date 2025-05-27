package com.coc.cu.controllers


import com.coc.cu.domain.*
import com.coc.cu.domain.models.ApiResponse
import com.coc.cu.repositories.AccountTransactionsRepository
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
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import org.springframework.web.client.RestTemplate
import java.math.BigInteger
import java.time.LocalDate


@RequestMapping("/api/v1/users")
@RestController
class UsersController(
    val usersService: UsersService,
    val accountService: AccountService,
    val memberAccountRepository: MemberAccountRepository,
    val guarantorRepository: GuarantorRepository,
    val transactionsRepository: AccountTransactionsRepository,
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

        val membersPage = usersService.list(
            query,
            PageRequest.of(
                if (exportToExcel || getGuarantorDebtorAccounts) 0 else page,
                if (exportToExcel || getGuarantorDebtorAccounts) Int.MAX_VALUE else size,
                sort
            )
        )
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
    @GetMapping("/summaries")
    fun summaries(
        @RequestParam(name = "exportToExcel", defaultValue = "false") exportToExcel: Boolean,
        @RequestParam(name = "page", defaultValue = "0") page: Int,
        @RequestParam(name = "size", defaultValue = "10") size: Int,
        @RequestParam(name = "q", defaultValue = "") query: String,
        @DateTimeFormat(pattern = "yyyy-MM-dd") @RequestParam(required = false) startDate: LocalDate?,
        @DateTimeFormat(pattern = "yyyy-MM-dd") @RequestParam(required = false) endDate: LocalDate?,
        @RequestParam(name = "properties", required = false) properties: Array<String>?,
        @RequestParam(name = "direction", required = false) direction: Array<String>?,

        ): ApiResponse<List<MemberSummariesResponseDto>> {

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

        val membersPage = usersService.listSummaries(
            query,
            startDate,
            endDate,
            PageRequest.of(
                if (exportToExcel) 0 else page,
                if (exportToExcel) Int.MAX_VALUE else size,
                sort
            )
        )


        return ApiResponse(membersPage.content, "OK", HttpStatus.OK, page, size, membersPage.totalElements)
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{id}")
    fun detail(@PathVariable id: Long): ApiResponse<MemberResponseDto> {

        var memberResponseDto = usersService.single(id)
        memberResponseDto!!.totalSavings = transactionsService.getTotalSavings(id)
        memberResponseDto!!.totalWithdrawals = transactionsService.getTotalWithdrawals(id)

        val accountTypeRef = object : TypeReference<MinimalAccountResponseDto>() {}
        val guarantorTypeRef = object : TypeReference<GuarantorResponseDto>() {}

        memberResponseDto.availableBalance = memberAccountRepository.getAvailableBalance(memberResponseDto.id)
        memberResponseDto.guarantorDebtorAccounts =
            accountService.getGuarantorDebtorAccounts(memberResponseDto.id!!).map {

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

                guarantorAccountResponseDto.account!!.balance = transactionsRepository.findByLoanBalance(account.id)

                guarantorAccountResponseDto

            }
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


    @PreAuthorize("isAuthenticated()")
    @GetMapping("/reset-member-id-sequence")
    fun resetMemberIdSequence(): ApiResponse<Long> {
        return ApiResponse(usersService.resetMemberIdSequence(), "Success", HttpStatus.OK)
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/reset-member-balances")
    fun resetMemberBalances(@RequestParam(name = "memberId", defaultValue = "0") memberId: Long): ApiResponse<Boolean> {
        usersService.updateTotalBalance(memberId)
        return ApiResponse(true, "Success", HttpStatus.OK)
    }

}