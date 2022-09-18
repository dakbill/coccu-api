package com.coc.cu.domain

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer
import lombok.Data
import java.time.LocalDateTime


class AccountResponseDto {
    var id: String? = null
    var number: String? = null
    var balance: Double = 0.0
    var type: AccountType? = null
    var member: MemberResponseDto? = null
    var transactions: List<TransactionResponseDto>? = null

    @JsonDeserialize(using = LocalDateTimeDeserializer::class)
    @JsonSerialize(using = LocalDateTimeSerializer::class)
    @JsonFormat(pattern="yyyy-MM-dd hh:mm:ss")
    var createdDate: LocalDateTime? = null

    @JsonDeserialize(using = LocalDateTimeDeserializer::class)
    @JsonSerialize(using = LocalDateTimeSerializer::class)
    @JsonFormat(pattern="yyyy-MM-dd hh:mm:ss")
    var updatedDate: LocalDateTime? = null

}


class TransactionResponseDto {
    var id: Long? = null
    var account: AccountResponseDto? = null
    var type: TransactionType? = null
    var amount: Float? = null
    var comment: String? = null

    @JsonDeserialize(using = LocalDateTimeDeserializer::class)
    @JsonSerialize(using = LocalDateTimeSerializer::class)
    @JsonFormat(pattern="yyyy-MM-dd hh:mm:ss")
    var createdDate: LocalDateTime? = null

    @JsonDeserialize(using = LocalDateTimeDeserializer::class)
    @JsonSerialize(using = LocalDateTimeSerializer::class)
    @JsonFormat(pattern="yyyy-MM-dd hh:mm:ss")
    var updatedDate: LocalDateTime? = null
}


class MemberResponseDto {
    var id: Long? = null
    var name: String? = null
    var phone: String? = null
    var accounts: List<AccountResponseDto>? = null


    var image: String? = null
    var email: String? = null
    var address: String? = null
    var city: String? = null
    var firstOfKinName: String? = null
    var firstOfKinPhone: String? = null
    var firstOfKinEmail: String? = null
    var secondOfKinName: String? = null
    var secondOfKinPhone: String? = null
    var secondOfKinEmail: String? = null

    var totalSavings: Double = 0.0
    var totalWithdrawals: Double = 0.0
    var totalBalance: Double = 0.0
    var transactionCount: Long = 0

    @JsonDeserialize(using = LocalDateTimeDeserializer::class)
    @JsonSerialize(using = LocalDateTimeSerializer::class)
    @JsonFormat(pattern="yyyy-MM-dd hh:mm:ss")
    var createdDate: LocalDateTime? = null

    @JsonDeserialize(using = LocalDateTimeDeserializer::class)
    @JsonSerialize(using = LocalDateTimeSerializer::class)
    @JsonFormat(pattern="yyyy-MM-dd hh:mm:ss")
    var updatedDate: LocalDateTime? = null
}

class AuthResponseDto {
    var member: MemberResponseDto? = null
    var bearerToken: String? = null
    var authorities: List<String>? = null
}

data class GoogleTokenInfoResponseDto(
    var email: String,
    @JsonProperty("verified_email") var isVerified: Boolean,
    @JsonProperty("user_id") var userId: String
)

class ClosingBooksResponseDto {
    var openingBalance: Double = 0.0
    var cards: Double = 0.0
    var totalSavingsCheque: Double = 0.0
    var totalSavings: Double = 0.0
    var totalWithdrawalsCheque: Double = 0.0
    var totalWithdrawals: Double = 0.0
    var totalAdminExpensesCheque: Double = 0.0
    var totalAdminExpenses: Double = 0.0
    var totalLoansCheque: Double = 0.0
    var totalLoans: Double = 0.0
    var totalLoanRepaymentsCheque: Double = 0.0
    var totalLoanRepayments: Double = 0.0
    var totalInterestRepayments: Double = 0.0
    var totalInCheque: Double = 0.0
    var totalOutCheque: Double = 0.0
    var totalIn: Double = 0.0
    var totalOut: Double = 0.0
    var cashBalance: Double = 0.0
    var moneyToBank: Double = 0.0



}


@Data
class DashboardResponseDto {
    var savings: Double = 0.0
    var withdrawals: Double = 0.0
    var loans: Double = 0.0
    var loanRepayments: Double = 0.0
    var chart: Map<String,Any>? = null
}

@Data
interface TransactionSumsDto {
    val type: String
    val amount: Double
}