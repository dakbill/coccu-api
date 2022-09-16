package com.coc.cu.domain

import org.springframework.web.multipart.MultipartFile
import java.util.*


class AccountRequestDto {
    var memberId: Long? = null
    var type: String? = null
}



class TransactionRequestDto(accountId: Long, amount: Float, type: TransactionType) {
    var accountId: String? = null
    var type: String? = null
    var comment: String? = null
    var amount: Float? = null
    var date: Date? = null
}

class RawTransactionRequestDto {
    var accountId: String? = null
    var type: String? = null
    var comment: String? = null
    var amount: Float? = null
    var date: Date? = null
}


class LoginRequestDto{
    val username: String? = null
    val password: String? = null
}

class ExchangeTokenRequestDto{
    val authProvider: AuthProvider? = null
    val token: String? = null
}

class UserRequestDto {
    var image: String? = null
    var imageFile: MultipartFile? = null
    var name: String? = null
    var phone: String? = null
    var email: String? = null
    var address: String? = null
    var city: String? = null
    var firstOfKinName: String? = null
    var firstOfKinPhone: String? = null
    var firstOfKinEmail: String? = null
    var secondOfKinName: String? = null
    var secondOfKinPhone: String? = null
    var secondOfKinEmail: String? = null
}