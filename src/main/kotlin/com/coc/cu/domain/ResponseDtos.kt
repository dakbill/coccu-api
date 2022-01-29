package com.coc.cu.domain


class AccountResponseDto {
    var id: String? = null
    var number: String? = null
    var type: AccountType? = null
    var user: UserResponseDto? = null
    var transactions: List<TransactionResponseDto>? = null

}

class TransactionResponseDto {
    var id: Long? = null
    var account: AccountResponseDto? = null
    var type: TransactionType? = null
    var amount: Float? = null
}

class UserResponseDto {
    var id: Long? = null
    var name: String? = null
    var phone: String? = null
    var accounts: List<AccountResponseDto>? = null
}