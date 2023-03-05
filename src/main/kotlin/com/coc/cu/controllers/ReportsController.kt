package com.coc.cu.controllers

import com.coc.cu.domain.ClosingBooksResponseDto
import com.coc.cu.domain.DashboardResponseDto
import com.coc.cu.domain.TransactionType
import com.coc.cu.domain.models.ApiResponse
import com.coc.cu.services.AccountService
import com.coc.cu.services.UsersService
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate


@RequestMapping("/api/v1/reports")
@RestController
class ReportsController(var accountService: AccountService, var usersService: UsersService) {

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/closing-books")
    fun getClosingBooks(@DateTimeFormat(pattern = "yyyy-MM-dd") @RequestParam dayInFocus: LocalDate): ApiResponse<ClosingBooksResponseDto> {
        return ApiResponse(accountService.getClosingBooksMetrics(dayInFocus), HttpStatus.OK)
    }


    @PreAuthorize("isAuthenticated()")
    @GetMapping("/dashboard-metrics")
    fun getDashboardMetrics(
        @DateTimeFormat(pattern = "yyyy-MM-dd") @RequestParam startDate: LocalDate,
        @DateTimeFormat(pattern = "yyyy-MM-dd") @RequestParam endDate: LocalDate
    ): ApiResponse<DashboardResponseDto> {
        return ApiResponse(accountService.getDashboardMetrics(startDate, endDate), HttpStatus.OK)
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/member-metrics")
    fun getMembersMetrics(
        @DateTimeFormat(pattern = "yyyy-MM-dd") @RequestParam startDate: LocalDate,
        @DateTimeFormat(pattern = "yyyy-MM-dd") @RequestParam endDate: LocalDate
    ): ApiResponse<Map<String, Any>> {

        val savingsTransactionTypeArray = arrayOf(
            TransactionType.SAVINGS, TransactionType.SAVINGS_CHEQUE,
        )

        val sharesTransactionTypeArray = arrayOf(
            TransactionType.SHARES, TransactionType.SHARES_CHEQUE,
        )

        return ApiResponse(
            mapOf(
                "malesCount" to usersService.countByGender(startDate, endDate, "m"),
                "femalesCount" to usersService.countByGender(startDate, endDate, "f"),
                "groupsCount" to usersService.countByGender(startDate, endDate, "g"),

                "malesSavings" to usersService.sumTransactionsByGenderAndTransactionTypes(
                    startDate,
                    endDate,
                    "m",
                    savingsTransactionTypeArray
                ),
                "femalesSavings" to usersService.sumTransactionsByGenderAndTransactionTypes(
                    startDate,
                    endDate,
                    "f",
                    savingsTransactionTypeArray
                ),
                "groupsSavings" to usersService.sumTransactionsByGenderAndTransactionTypes(
                    startDate,
                    endDate,
                    "g",
                    savingsTransactionTypeArray
                ),

                "malesShares" to usersService.sumTransactionsByGenderAndTransactionTypes(
                    startDate,
                    endDate,
                    "m",
                    sharesTransactionTypeArray
                ),
                "femalesShares" to usersService.sumTransactionsByGenderAndTransactionTypes(
                    startDate,
                    endDate,
                    "f",
                    sharesTransactionTypeArray
                ),
                "groupsShares" to usersService.sumTransactionsByGenderAndTransactionTypes(
                    startDate,
                    endDate,
                    "g",
                    sharesTransactionTypeArray
                ),
            ),
            HttpStatus.OK
        )
    }


    @PreAuthorize("isAuthenticated()")
    @GetMapping("/borrowers-metrics")
    fun getBorrowersMetrics(
        @DateTimeFormat(pattern = "yyyy-MM-dd") @RequestParam startDate: LocalDate,
        @DateTimeFormat(pattern = "yyyy-MM-dd") @RequestParam endDate: LocalDate
    ): ApiResponse<Map<String, Any>> {
        return ApiResponse(
            mapOf(
                "outstandingLoansMalesCount" to accountService.countOutstandingLoansByGender(
                    startDate,
                    endDate,
                    "m",
                    "outstanding"
                ),
                "outstandingLoansFemalesCount" to accountService.countOutstandingLoansByGender(
                    startDate,
                    endDate,
                    "f",
                    "outstanding"
                ),
                "outstandingLoansGroupsCount" to accountService.countOutstandingLoansByGender(
                    startDate,
                    endDate,
                    "g",
                    "outstanding"
                ),

                "malesOutstandingLoans" to accountService.sumOutstandingLoansByGender(
                    startDate,
                    endDate,
                    "m",
                    "outstanding"
                ),
                "femalesOutstandingLoans" to accountService.sumOutstandingLoansByGender(
                    startDate,
                    endDate,
                    "f",
                    "outstanding"
                ),
                "groupsOutstandingLoans" to accountService.sumOutstandingLoansByGender(
                    startDate,
                    endDate,
                    "g",
                    "outstanding"
                ),

                "malesDelinquentBorrowersCount" to usersService.sumTransactionsByGenderAndTransactionTypes(
                    startDate,
                    endDate,
                    "m",
                    arrayOf(TransactionType.SAVINGS, TransactionType.SAVINGS_CHEQUE)
                ),
                "femalesDelinquentBorrowersCount" to usersService.sumTransactionsByGenderAndTransactionTypes(
                    startDate,
                    endDate,
                    "f",
                    arrayOf(TransactionType.SAVINGS, TransactionType.SAVINGS_CHEQUE)
                ),
                "groupsDelinquentBorrowersCount" to usersService.sumTransactionsByGenderAndTransactionTypes(
                    startDate,
                    endDate,
                    "g",
                    arrayOf(TransactionType.SAVINGS, TransactionType.SAVINGS_CHEQUE)
                ),


                "malesDelinquentBorrowersAmount" to usersService.sumTransactionsByGenderAndTransactionTypes(
                    startDate,
                    endDate,
                    "m",
                    arrayOf(TransactionType.SAVINGS, TransactionType.SAVINGS_CHEQUE)
                ),
                "femalesDelinquentBorrowersAmount" to usersService.sumTransactionsByGenderAndTransactionTypes(
                    startDate,
                    endDate,
                    "f",
                    arrayOf(TransactionType.SAVINGS, TransactionType.SAVINGS_CHEQUE)
                ),
                "groupsDelinquentBorrowersAmount" to usersService.sumTransactionsByGenderAndTransactionTypes(
                    startDate,
                    endDate,
                    "g",
                    arrayOf(TransactionType.SAVINGS, TransactionType.SAVINGS_CHEQUE)
                ),
            ),
            HttpStatus.OK
        )
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/loans-metrics")
    fun getLoansMetrics(
        @DateTimeFormat(pattern = "yyyy-MM-dd") @RequestParam startDate: LocalDate,
        @DateTimeFormat(pattern = "yyyy-MM-dd") @RequestParam endDate: LocalDate
    ): ApiResponse<Map<String, Any>> {
        return ApiResponse(
            mapOf(

                "outstandingLoansMalesCount" to usersService.countByGender(startDate, endDate, "m"),
                "outstandingLoansFemalesCount" to usersService.countByGender(startDate, endDate, "f"),
                "outstandingLoansGroupsCount" to usersService.countByGender(startDate, endDate, "g"),

                "malesOutstandingLoans" to usersService.sumTransactionsByGenderAndTransactionTypes(
                    startDate,
                    endDate,
                    "m",
                    arrayOf(TransactionType.SAVINGS, TransactionType.SAVINGS_CHEQUE)
                ),
                "femalesOutstandingLoans" to usersService.sumTransactionsByGenderAndTransactionTypes(
                    startDate,
                    endDate,
                    "f",
                    arrayOf(TransactionType.SAVINGS, TransactionType.SAVINGS_CHEQUE)
                ),
                "groupsOutstandingLoans" to usersService.sumTransactionsByGenderAndTransactionTypes(
                    startDate,
                    endDate,
                    "g",
                    arrayOf(TransactionType.SAVINGS, TransactionType.SAVINGS_CHEQUE)
                ),

                "malesDelinquentBorrowersCount" to usersService.sumTransactionsByGenderAndTransactionTypes(
                    startDate,
                    endDate,
                    "m",
                    arrayOf(TransactionType.SAVINGS, TransactionType.SAVINGS_CHEQUE)
                ),
                "femalesDelinquentBorrowersCount" to usersService.sumTransactionsByGenderAndTransactionTypes(
                    startDate,
                    endDate,
                    "f",
                    arrayOf(TransactionType.SAVINGS, TransactionType.SAVINGS_CHEQUE)
                ),
                "groupsDelinquentBorrowersCount" to usersService.sumTransactionsByGenderAndTransactionTypes(
                    startDate,
                    endDate,
                    "g",
                    arrayOf(TransactionType.SAVINGS, TransactionType.SAVINGS_CHEQUE)
                ),


                "malesDelinquentBorrowersAmount" to usersService.sumTransactionsByGenderAndTransactionTypes(
                    startDate,
                    endDate,
                    "m",
                    arrayOf(TransactionType.SAVINGS, TransactionType.SAVINGS_CHEQUE)
                ),
                "femalesDelinquentBorrowersAmount" to usersService.sumTransactionsByGenderAndTransactionTypes(
                    startDate,
                    endDate,
                    "f",
                    arrayOf(TransactionType.SAVINGS, TransactionType.SAVINGS_CHEQUE)
                ),
                "groupsDelinquentBorrowersAmount" to usersService.sumTransactionsByGenderAndTransactionTypes(
                    startDate,
                    endDate,
                    "g",
                    arrayOf(TransactionType.SAVINGS, TransactionType.SAVINGS_CHEQUE)
                ),
            ),
            HttpStatus.OK
        )
    }


}