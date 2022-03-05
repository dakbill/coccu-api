package com.coc.cu.services

import com.coc.cu.domain.AccountResponseDto
import com.coc.cu.domain.TransactionResponseDto
import com.coc.cu.repositories.AccountTransactionsRepository
import com.coc.cu.repositories.MemberAccountRepository
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Service

@Service
class AccountService(
    var repository: MemberAccountRepository,
    var transactionsRepository: AccountTransactionsRepository,
    var objectMapper: ObjectMapper
) {


    fun single(id: String): AccountResponseDto? {

        val typeRef = object : TypeReference<AccountResponseDto>() {}
        val transactionRef = object : TypeReference<TransactionResponseDto>() {}

        var res = repository.findById(id)
        if (res.isPresent) {
            var accountEntity = res.get()

            accountEntity.transactions = transactionsRepository.findByAccountId(accountEntity.id)
            return objectMapper.convertValue(accountEntity, typeRef)
        }


        return null
    }

    fun list(): List<AccountResponseDto>? {
        val typeRef = object : TypeReference<List<AccountResponseDto>>() {}


        return objectMapper.convertValue(repository.findAll(), typeRef)
    }
}