package com.coc.cu.services

import com.coc.cu.domain.AccountResponseDto
import com.coc.cu.domain.ClosingBooksResponseDto
import com.coc.cu.domain.DashboardResponseDto
import com.coc.cu.domain.TransactionType
import com.coc.cu.repositories.AccountTransactionsRepository
import com.coc.cu.repositories.MemberAccountRepository
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Service
import java.util.*

@Service
class AccountService(
    var repository: MemberAccountRepository,
    var transactionsRepository: AccountTransactionsRepository,
    var objectMapper: ObjectMapper
) {


    fun single(id: String): AccountResponseDto? {

        val typeRef = object : TypeReference<AccountResponseDto>() {}

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

    fun getDashboardMetrics(startDate: Date, endDate: Date): DashboardResponseDto {
        val response = DashboardResponseDto()
        val transactionSumsDto = repository.getDashboardStatistics(startDate,endDate)
        for (record in transactionSumsDto) {
            if (record.type == null) {
                continue
            }

            if (record.type.contains("SAVINGS")) {
                response.savings += record.amount
            } else if (record.type.contains("WITHDRAWAL")) {
                response.withdrawals += record.amount
            } else if (record.type.contains("LOAN_REPAYMENT")) {
                response.loanRepayments += record.amount
            } else if (record.type.matches(Regex.fromLiteral("^(LOAN|LOAN_CHEQUE)$"))) {
                response.loans += record.amount
            }
        }

        return response
    }

    fun getClosingBooksMetrics(dayInFocus: Date): ClosingBooksResponseDto {
        val response = ClosingBooksResponseDto()
        val transactionSumsDto = repository.getDashboardStatistics(dayInFocus, dayInFocus)
        for (record in transactionSumsDto) {
            if (record.type == null) {
                continue
            }

            if (record.type.equals(TransactionType.SAVINGS)) {
                response.totalSavings += record.amount
            } else if (record.type.equals(TransactionType.SAVINGS_CHEQUE)) {
                response.totalSavingsCheque += record.amount
            }
        }

        return response
    }
}