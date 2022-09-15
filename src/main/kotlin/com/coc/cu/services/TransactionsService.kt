package com.coc.cu.services

import com.coc.cu.domain.*
import com.coc.cu.entities.Account
import com.coc.cu.entities.Transaction
import com.coc.cu.repositories.AccountTransactionsRepository
import com.coc.cu.repositories.MemberAccountRepository
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class TransactionsService(var repository: AccountTransactionsRepository,var accountRepository: MemberAccountRepository, var objectMapper: ObjectMapper) {

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

    fun create(model: RawTransactionRequestDto): TransactionResponseDto? {
        val transactionTypeRef = object : TypeReference<Transaction>() {}
        var transaction = objectMapper.convertValue(model,transactionTypeRef)

        transaction.createdDate = LocalDate.now()
        transaction.account = accountRepository.findById(model.accountId!!).get()
        transaction = repository.save(transaction)




        val typeRef = object : TypeReference<TransactionResponseDto>() {}
        return objectMapper.convertValue(repository.findById(transaction.id!!).get(), typeRef)
    }

    fun getTotalSavings(memberId: Long): Double {
        return repository.getTotalSavings(memberId)
    }

    fun getTotalWithdrawals(memberId: Long): Double {
        return repository.getTotalWithdrawals(memberId)
    }
}