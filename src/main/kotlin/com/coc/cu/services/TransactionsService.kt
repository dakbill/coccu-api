package com.coc.cu.services

import com.coc.cu.domain.*
import com.coc.cu.entities.Transaction
import com.coc.cu.repositories.AccountTransactionsRepository
import com.coc.cu.repositories.MemberAccountRepository
import com.coc.cu.repositories.MembersRepository
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import java.util.stream.Collectors

@Service
class TransactionsService(var repository: AccountTransactionsRepository, var membersRepository: MembersRepository, var memberAccountRepository: MemberAccountRepository, var objectMapper: ObjectMapper) {

    fun single(id: Long): TransactionResponseDto? {
        val typeRef = object : TypeReference<TransactionResponseDto>() {}


        var res = repository.findById(id)
        if (res.isPresent) {
            var transactionEntity = res.get()

            return objectMapper.convertValue(transactionEntity, typeRef)
        }


        return null
    }

    fun list(
        memberId: Long,
        accountId: String?,
        transactionType: String?,
        startDate: LocalDate,
        endDate: LocalDate,
        pageRequest: PageRequest
    ): Page<TransactionResponseDto> {
        val typeRef = object : TypeReference<List<TransactionResponseDto>>() {}


        var transactionsPage: Page<Transaction> = repository.findAllByMemberId(
            memberId,
            Optional.ofNullable(accountId).orElse(""),
            Optional.ofNullable(transactionType).orElse(""),
            startDate,
            endDate,
            pageRequest
        )

        return PageImpl(
            objectMapper.convertValue(transactionsPage.content, typeRef),
            pageRequest,
            transactionsPage.totalElements
        )
    }

    fun create(model: RawTransactionRequestDto): TransactionResponseDto? {
        val transactionTypeRef = object : TypeReference<Transaction>() {}
        var transaction = objectMapper.convertValue(model,transactionTypeRef)

        transaction.createdDate = LocalDateTime.now()
        transaction.account = memberAccountRepository.findById(model.accountId!!).get()
        transaction = repository.save(transaction)

        if (!model.guarantors.isNullOrEmpty()) {
            val account = memberAccountRepository.findById(model.accountId!!).get()
            val members = model.guarantors!!.stream().map { s ->
                membersRepository.findById(
                    s.toLong()
                ).get()
            }
                .collect(Collectors.toList())

            account.guarantors = members

            memberAccountRepository.save(account)
        }




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