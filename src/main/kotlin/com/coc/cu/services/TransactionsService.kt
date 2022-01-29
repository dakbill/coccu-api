package com.coc.cu.services

import com.coc.cu.domain.AccountResponseDto
import com.coc.cu.domain.TransactionResponseDto
import com.coc.cu.repositories.AccountTransactionsRepository
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Service

@Service
class TransactionsService(repository: AccountTransactionsRepository) {
    var repository: AccountTransactionsRepository = repository


    fun single(id: Long): TransactionResponseDto? {
        val objectMapper = ObjectMapper()
        val typeRef = object : TypeReference<TransactionResponseDto>() {}


        var res = repository.findById(id)
        if (res.isPresent) {
            var transactionEntity = res.get()

            return objectMapper.convertValue(transactionEntity, typeRef)
        }


        return null
    }

    fun list(): List<TransactionResponseDto>? {
        val objectMapper = ObjectMapper()
        val typeRef = object : TypeReference<List<TransactionResponseDto>>() {}

        return objectMapper.convertValue(repository.findAll(), typeRef)
    }
}