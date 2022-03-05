package com.coc.cu.domain


class AccountRequestDto {
    var memberId: Long? = null
    var type: String? = null

    var number: String? = null
}

class TransactionRequestDto(accountId: Long, amount: Float, type: TransactionType) {

}

class UserRequestDto {
}