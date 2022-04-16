package com.coc.cu.domain

import com.fasterxml.jackson.annotation.JsonFilter
import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer
import java.time.LocalDate


class AccountResponseDto {
    var id: String? = null
    var number: String? = null
    var type: AccountType? = null
    var member: MemberResponseDto? = null
    var transactions: List<TransactionResponseDto>? = null

}


class TransactionResponseDto {
    var id: Long? = null
    var account: AccountResponseDto? = null
    var type: TransactionType? = null
    var amount: Float? = null


    @JsonDeserialize(using = LocalDateDeserializer::class)
    @JsonSerialize(using = LocalDateSerializer::class)
    @JsonFormat(pattern="yyyy-MM-dd")
    var createdDate: LocalDate? = null

    @JsonDeserialize(using = LocalDateDeserializer::class)
    @JsonSerialize(using = LocalDateSerializer::class)
    @JsonFormat(pattern="yyyy-MM-dd")
    var updatedDate: LocalDate? = null
}


class MemberResponseDto {
    var id: Long? = null
    var name: String? = null
    var phone: String? = null
    var accounts: List<AccountResponseDto>? = null
}




class ClosingBooksResponseDto {
    var openingBalance: Double = 0.0
    var totalSales: Double = 0.0
    var totalCheckSavings: Double = 0.0
    var totalCashSavings: Double = 0.0
    var totalCashWithdrawals: Double = 0.0
    var totalCheckWithdrawals: Double = 0.0
    var totalLoanRepayments: Double = 0.0
    var totalAdminExpenses: Double = 0.0
    var totalInterestRepayments: Double = 0.0
    var totalCashLoans: Double = 0.0
    var totalCheckLoans: Double = 0.0
    var totalLoanRepaymentsPlusInterest: Double = 0.0
    var totalIn: Double = 0.0
    var totalOut: Double = 0.0
    var cashBalance: Double = 0.0
    var totalBalance: Double = 0.0
}