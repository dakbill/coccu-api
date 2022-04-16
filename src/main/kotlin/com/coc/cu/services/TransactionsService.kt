package com.coc.cu.services

import com.coc.cu.domain.TransactionResponseDto
import com.coc.cu.repositories.AccountTransactionsRepository
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Service

@Service
class TransactionsService(var repository: AccountTransactionsRepository, var objectMapper: ObjectMapper) {

    fun single(id: Long): TransactionResponseDto? {
        val typeRef = object : TypeReference<TransactionResponseDto>() {}


        var res = repository.findById(id)
        if (res.isPresent) {
            var transactionEntity = res.get()

            return objectMapper.convertValue(transactionEntity, typeRef)
        }


        return null
    }

    fun list(memberId: Long): List<TransactionResponseDto>? {
        val typeRef = object : TypeReference<List<TransactionResponseDto>>() {}
        var transactions = repository.findAllByMemberId(memberId)


        return objectMapper.convertValue(transactions, typeRef)
    }
}