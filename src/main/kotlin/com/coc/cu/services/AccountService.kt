package com.coc.cu.services

import com.coc.cu.domain.*
import com.coc.cu.entities.Account
import com.coc.cu.repositories.AccountTransactionsRepository
import com.coc.cu.repositories.MemberAccountRepository
import com.coc.cu.repositories.MembersRepository
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.Period
import java.time.ZoneId
import java.util.*
import java.util.stream.Collectors
import kotlin.math.roundToInt


@Service
class AccountService(
    var repository: MemberAccountRepository,
    var membersRepository: MembersRepository,
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

    fun getDashboardMetrics(startDate: LocalDate, endDate: LocalDate): DashboardResponseDto {
        val response = DashboardResponseDto()
        val transactionSumsDto = repository.getDashboardStatistics(startDate, endDate)
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


        val zoneId = ZoneId.of("GMT")
        val diff = endDate.atStartOfDay(zoneId).toEpochSecond()-startDate.atStartOfDay(zoneId).toEpochSecond()


        val step = if (diff < (24 * 60 * 60)) {
            Period.ofDays(1 / 24)
        } else if (diff < (24 * 60 * 60 * 16)) {
            Period.ofDays(1)
        } else if (diff < (24 * 60 * 60 * 30 * 3)) {
            Period.ofWeeks(1)
        } else if (diff < (24 * 60 * 60 * 30 * 24)) {
            Period.ofMonths(1)
        } else {
            Period.ofYears(1)
        }

        val dates: List<LocalDate> = startDate.datesUntil(endDate, step)
            .collect(Collectors.toList())

        response.chart =
            mapOf(
                "x" to dates.map { String.format("%s-%s-%s", it.year, it.monthValue, it.dayOfMonth) },
                "series" to mapOf(
                    TransactionType.SAVINGS.name to dates.map {
                        repository.sumAmounts(
                            arrayOf(
                                TransactionType.SAVINGS.name,
                                TransactionType.SAVINGS_CHEQUE.name
                            ), it.minus(step), it
                        )
                    },
                    TransactionType.WITHDRAWAL.name to dates.map {
                        repository.sumAmounts(
                            arrayOf(
                                TransactionType.WITHDRAWAL.name,
                                TransactionType.WITHDRAWAL_CHEQUE.name
                            ), it.minus(step), it
                        )
                    },
                    TransactionType.LOAN_REPAYMENT.name to dates.map {
                        repository.sumAmounts(
                            arrayOf(
                                TransactionType.LOAN_REPAYMENT.name,
                                TransactionType.LOAN_REPAYMENT_CHEQUE.name
                            ), it.minus(step), it
                        )
                    },
                    TransactionType.INTEREST_ON_LOAN.name to dates.map {
                        repository.sumAmounts(
                            arrayOf(
                                TransactionType.INTEREST_ON_LOAN.name,
                                TransactionType.INTEREST_ON_LOAN_CHEQUE.name
                            ), it.minus(step), it
                        )
                    },
                    TransactionType.LOAN.name to dates.map {
                        repository.sumAmounts(
                            arrayOf(
                                TransactionType.LOAN.name,
                                TransactionType.LOAN_CHEQUE.name
                            ), it.minus(step), it
                        )
                    },
                )
            )

        return response
    }

    fun getClosingBooksMetrics(dayInFocus: LocalDate): ClosingBooksResponseDto {
        val response = ClosingBooksResponseDto()
        val endOfDay = dayInFocus.atStartOfDay().plusSeconds((24 * 60 * 60)).toLocalDate()
//        val endOfDay = Date(dayInFocus.time + (24 * 60 * 60 * 1000))

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

        response.cashBalance = response.totalIn - response.totalOut
        return response
    }


    fun create(model: AccountRequestDto): AccountResponseDto? {
        val accountTypeRef = object : TypeReference<Account>() {}
        var account = objectMapper.convertValue(model, accountTypeRef)
        account.id = UUID.randomUUID().toString()

        account.member = membersRepository.findById(model.memberId!!).get()
        account = repository.save(account)


        val typeRef = object : TypeReference<AccountResponseDto>() {}
        return objectMapper.convertValue(account, typeRef)
    }
}