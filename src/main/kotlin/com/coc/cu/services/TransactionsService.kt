package com.coc.cu.services

import com.coc.cu.domain.*
import com.coc.cu.entities.Guarantor
import com.coc.cu.entities.Transaction
import com.coc.cu.repositories.AccountTransactionsRepository
import com.coc.cu.repositories.GuarantorRepository
import com.coc.cu.repositories.MemberAccountRepository
import com.coc.cu.repositories.MembersRepository
import com.coc.cu.utils.JwtUtils
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import java.util.stream.Collectors
import kotlin.collections.ArrayList

@Service
class TransactionsService(
    var repository: AccountTransactionsRepository,
    var membersRepository: MembersRepository,
    var memberAccountRepository: MemberAccountRepository,
    var guarantorRepository: GuarantorRepository,
    var objectMapper: ObjectMapper,
    var restTemplate: RestTemplate,
    var jwtUtils: JwtUtils
) {

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
        query: String?,
        memberId: Long,
        accountId: String?,
        transactionTypes: Array<String>?,
        startDate: LocalDate,
        endDate: LocalDate,
        pageRequest: PageRequest
    ): Page<TransactionResponseDto> {
        val typeRef = object : TypeReference<List<TransactionResponseDto>>() {}


        var transactionsPage: Page<Transaction> = repository.findAllByMemberId(
            Optional.ofNullable(query).orElse(""),
            memberId,
            Optional.ofNullable(accountId).orElse(""),
            transactionTypes ?: arrayOf("EMPTY"),
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
        var transaction = objectMapper.convertValue(model, transactionTypeRef)

        transaction.createdDate = LocalDateTime.now()
        transaction.account = memberAccountRepository.findById(model.accountId!!).get()

        transaction = repository.save(transaction)

        if (!model.guarantors.isNullOrEmpty()) {
            val account = memberAccountRepository.findById(model.accountId!!).get()
            if (account.guarantors!!.isEmpty()) {
                account.guarantors = arrayListOf()
            }

            model.guarantors!!.stream().forEach { g ->
                val member = membersRepository.findById(
                    g.memberId!!
                ).get()

                account.guarantors!!.add(
                    guarantorRepository.save(
                        Guarantor(
                            member = member,
                            amount = g.amount,
                            createdDate = transaction.createdDate,
                        )
                    )
                )

                jwtUtils.sendSms(
                    "COCCU",
                    member.phone!!,
                    "Transaction: Loan Guarantor\nFor: ${account.member!!.name}\nAmount: ${g.amount}\nDate: ${
                        String.format(
                            "%s-%s-%s %d:%d",
                            transaction.createdDate!!.year,
                            transaction.createdDate!!.monthValue,
                            transaction.createdDate!!.dayOfMonth,
                            transaction.createdDate!!.hour,
                            transaction.createdDate!!.minute
                        )
                    }",
                    restTemplate
                )


            }



            memberAccountRepository.save(account)
        }

        if(model.interestRate != null){
            val account = memberAccountRepository.findById(model.accountId!!).get()
            account.interestRate = model.interestRate

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

    fun purge(id: Long): Boolean? {
        var deleted = false
        try {
            repository.deleteById(id)
            deleted = true
        } catch (e: Exception) {
            //ignored
        }

        return deleted
    }
}