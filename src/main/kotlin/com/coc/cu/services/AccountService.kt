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
            } else if (Regex("^(LOAN|LOAN_CHEQUE)$").matches(record.type)) {
                response.loans += record.amount
            }
        }

        return response
    }

    fun getClosingBooksMetrics(dayInFocus: Date): ClosingBooksResponseDto {
        val response = ClosingBooksResponseDto()
        val endOfDay = Date(dayInFocus.time + (24 * 60 * 60 * 1000))

        val transactionSumsDto = repository.getDashboardStatistics(dayInFocus, endOfDay)
        for (record in transactionSumsDto) {
            if (record.type == null) {
                continue
            }

            if (record.type.equals(TransactionType.SAVINGS.name)) {
                response.totalSavings += record.amount
                response.totalIn += record.amount
            } else if (record.type.equals(TransactionType.SAVINGS_CHEQUE.name)) {
                response.totalSavingsCheque += record.amount
                response.totalInCheque += record.amount
            } else if (record.type.equals(TransactionType.OPENING_BALANCE.name)) {
                response.openingBalance += record.amount
                response.totalIn += record.amount
            } else if (record.type.equals(TransactionType.CARD.name)) {
                response.cards += record.amount
                response.totalIn += record.amount
            } else if (record.type.equals(TransactionType.WITHDRAWAL.name)) {
                response.totalWithdrawals += record.amount
                response.totalOut += record.amount
            } else if (record.type.equals(TransactionType.WITHDRAWAL_CHEQUE.name)) {
                response.totalWithdrawalsCheque += record.amount
                response.totalOutCheque += record.amount
            } else if (arrayOf(TransactionType.INCENTIVE_TO_PERSONEL.name, TransactionType.STATIONERY.name).contains(
                    record.type
                )
            ) {
                response.totalAdminExpenses += record.amount
                response.totalOut += record.amount
            } else if (arrayOf(
                    TransactionType.INCENTIVE_TO_PERSONEL_CHEQUE.name,
                    TransactionType.STATIONERY_CHEQUE.name
                ).contains(record.type)
            ) {
                response.totalAdminExpensesCheque += record.amount
                response.totalOutCheque += record.amount
            } else if (record.type.equals(TransactionType.LOAN.name)) {
                response.totalLoans += record.amount
                response.totalOut += record.amount
            } else if (record.type.equals(TransactionType.LOAN_CHEQUE.name)) {
                response.totalLoansCheque += record.amount
                response.totalOutCheque += record.amount
            } else if (record.type.equals(TransactionType.LOAN_REPAYMENT.name)) {
                response.totalLoanRepayments += record.amount
                response.totalIn += record.amount
            } else if (record.type.equals(TransactionType.LOAN_REPAYMENT_CHEQUE.name)) {
                response.totalLoanRepaymentsCheque += record.amount
                response.totalInCheque += record.amount
            } else if (arrayOf(
                    TransactionType.INTEREST_ON_LOAN.name,
                ).contains(
                    record.type
                )
            ) {
                response.totalInterestRepayments += record.amount
                response.totalIn += record.amount
            }
        }

        return response
    }
}