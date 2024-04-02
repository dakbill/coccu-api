package com.coc.cu.services

import com.coc.cu.domain.*
import com.coc.cu.entities.Account
import com.coc.cu.repositories.AccountTransactionsRepository
import com.coc.cu.repositories.MemberAccountRepository
import com.coc.cu.repositories.MembersRepository
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Period
import java.time.ZoneId
import java.time.format.DateTimeFormatter


@Service
class AccountService(
    var repository: MemberAccountRepository,
    var membersRepository: MembersRepository,
    var transactionsRepository: AccountTransactionsRepository,
    var objectMapper: ObjectMapper
) {


    fun single(id: String): AccountResponseDto? {

        val typeRef = object : TypeReference<AccountResponseDto>() {}

        val res = repository.findById(id)
        if (res.isPresent) {
            val accountEntity = res.get()

            accountEntity.transactions = transactionsRepository.findByAccountId(accountEntity.id)
            return objectMapper.convertValue(accountEntity, typeRef)
        }


        return null
    }

    fun list(): List<AccountResponseDto>? {
        val typeRef = object : TypeReference<List<AccountResponseDto>>() {}


        return objectMapper.convertValue(repository.findAll(), typeRef)
    }

    fun getDebtors(query: String, type: String?, pageRequest: PageRequest): Page<AccountResponseDto> {


        val accountsPage = when (type) {
            "outstanding" -> {
                repository.getOutstandingDebtors(query, pageRequest)
            }
            "defaulting" -> {
                repository.getDefaultingDebtors(query, pageRequest)
            }
            else -> {
                repository.getDebtors(query, pageRequest)
            }
        }

        val typeRef = object : TypeReference<List<AccountResponseDto>>() {}
        val response = objectMapper.convertValue(accountsPage.content, typeRef)

        return PageImpl(response, pageRequest, accountsPage.totalElements)
    }



    fun getNegativeBalanceAccounts(query: String, pageRequest: PageRequest): Page<AccountResponseDto> {
        val accountsPage = repository.getNegativeBalanceAccounts(query, pageRequest)
        val typeRef = object : TypeReference<List<AccountResponseDto>>() {}
        val response = objectMapper.convertValue(accountsPage.content, typeRef)


        return PageImpl(response, pageRequest, accountsPage.totalElements)
    }

    fun getGuarantorDebtorAccounts(memberId: Long): List<Map<String,Any>> {
        return repository.getGuarantorDebtorAccounts(memberId)
    }

    fun getDashboardMetrics(startDate: LocalDate, endDate: LocalDate): DashboardResponseDto {
        val response = DashboardResponseDto()
        val transactionSumsDto = repository.getDashboardStatistics(startDate, endDate)
        for (record in transactionSumsDto) {

            if (arrayOf("SAVINGS", "SAVINGS_CHEQUE").contains(record.type)) {
                response.savings += record.amount
            } else if (arrayOf("WITHDRAWAL", "WITHDRAWAL_CHEQUE").contains(record.type)) {
                response.withdrawals += record.amount
            } else if (arrayOf("LOAN_REPAYMENT", "LOAN_REPAYMENT_CHEQUE").contains(record.type)) {
                response.loanRepayments += record.amount
            } else if (arrayOf("LOAN", "LOAN_CHEQUE").contains(record.type)) {
                response.loans += record.amount
            } else if (arrayOf("INTEREST_ON_LOAN", "INTEREST_ON_LOAN_CHEQUE").contains(record.type)) {
                response.interestOnLoan += record.amount
            } else if (arrayOf("CARD").contains(record.type)) {
                response.cardsAndProcessingFee += record.amount
            } else if (arrayOf("STATIONERY","STATIONERY_CHEQUE","TRANSPORT","INCENTIVE_TO_PERSONNEL","INCENTIVE_TO_PERSONNEL_CHEQUE").contains(record.type)) {
                response.expenditure += record.amount
            }
        }

        val zoneId = ZoneId.of("GMT")
        val diff = endDate.atStartOfDay(zoneId).toEpochSecond() - startDate.atStartOfDay(zoneId).toEpochSecond()


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


        val dates: List<LocalDate> =
            generateSequence(startDate) { it.plus(step) }.takeWhile { it.isBefore(endDate) }.toList()


        response.chart =
            mapOf(
                "x" to dates.map { it.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) },
                "series" to mapOf(
                    TransactionType.SAVINGS.name to dates.map {
                        repository.sumAmounts(
                            arrayOf(
                                TransactionType.SAVINGS.name,
                                TransactionType.SAVINGS_CHEQUE.name
                            ), it, it.plus(step)
                        )
                    },
                    TransactionType.WITHDRAWAL.name to dates.map {
                        repository.sumAmounts(
                            arrayOf(
                                TransactionType.WITHDRAWAL.name,
                                TransactionType.WITHDRAWAL_CHEQUE.name
                            ), it, it.plus(step)
                        )
                    },
                    TransactionType.LOAN_REPAYMENT.name to dates.map {
                        repository.sumAmounts(
                            arrayOf(
                                TransactionType.LOAN_REPAYMENT.name,
                                TransactionType.LOAN_REPAYMENT_CHEQUE.name
                            ), it, it.plus(step)
                        )
                    },
                    TransactionType.INTEREST_ON_LOAN.name to dates.map {
                        repository.sumAmounts(
                            arrayOf(
                                TransactionType.INTEREST_ON_LOAN.name,
                                TransactionType.INTEREST_ON_LOAN_CHEQUE.name
                            ), it, it.plus(step)
                        )
                    },
                    TransactionType.LOAN.name to dates.map {
                        repository.sumAmounts(
                            arrayOf(
                                TransactionType.LOAN.name,
                                TransactionType.LOAN_CHEQUE.name
                            ), it, it.plus(step)
                        )
                    },
                    "MONIES_TO_BANK" to dates.map { this.getTotalMoniesToBank(it, it.plus(step)) },
                )
            )

        return response
    }

    fun getClosingBooksMetrics(dayInFocus: LocalDate): ClosingBooksResponseDto {
        val response = ClosingBooksResponseDto()
        val endOfDay = dayInFocus.atStartOfDay().plusSeconds(((24 * 60 * 60).toLong())).toLocalDate()

        val transactionSumsDto = repository.getDashboardStatistics(dayInFocus, endOfDay)
        for (record in transactionSumsDto) {

            if (record.type == TransactionType.SAVINGS.name) {
                response.totalSavings += record.amount
                response.totalIn += record.amount
            } else if (record.type == TransactionType.SAVINGS_CHEQUE.name) {
                response.totalSavingsCheque += record.amount
                response.totalInCheque += record.amount
            } else if (record.type == TransactionType.OPENING_BALANCE.name) {
                response.openingBalance += record.amount
                response.totalIn += record.amount
            } else if (record.type == TransactionType.CARD.name) {
                response.cards += record.amount
                response.totalIn += record.amount
            } else if (record.type == TransactionType.WITHDRAWAL.name) {
                response.totalWithdrawals += record.amount
                response.totalOut += record.amount
            } else if (record.type == TransactionType.WITHDRAWAL_CHEQUE.name) {
                response.totalWithdrawalsCheque += record.amount
                response.totalOutCheque += record.amount
            } else if (arrayOf(
                    TransactionType.INCENTIVE_TO_PERSONNEL.name,
                    TransactionType.STATIONERY.name,
                    TransactionType.TRANSPORT.name
                ).contains(
                    record.type
                )
            ) {
                response.totalAdminExpenses += record.amount
                response.totalOut += record.amount
            } else if (arrayOf(
                    TransactionType.INCENTIVE_TO_PERSONNEL_CHEQUE.name,
                    TransactionType.STATIONERY_CHEQUE.name
                ).contains(record.type)
            ) {
                response.totalAdminExpensesCheque += record.amount
                response.totalOutCheque += record.amount
            } else if (record.type == TransactionType.LOAN.name) {
                response.totalLoans += record.amount
                response.totalOut += record.amount
            } else if (record.type == TransactionType.LOAN_CHEQUE.name) {
                response.totalLoansCheque += record.amount
                response.totalOutCheque += record.amount
            } else if (record.type == TransactionType.LOAN_REPAYMENT.name) {
                response.totalLoanRepayments += record.amount
                response.totalIn += record.amount
            } else if (record.type == TransactionType.LOAN_REPAYMENT_CHEQUE.name) {
                response.totalLoanRepaymentsCheque += record.amount
                response.totalInCheque += record.amount
            } else if (record.type == TransactionType.INTEREST_ON_LOAN_CHEQUE.name) {
                response.totalInterestRepaymentsCheque += record.amount
                response.totalOutCheque += record.amount
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
        response.moneyToBank = response.cashBalance + response.totalInCheque
        return response
    }

    fun getTotalMoniesToBank(startDate: LocalDate, endDate: LocalDate): Double {
        val startOfWeekStartDate = startDate.with(DayOfWeek.SUNDAY)


        return generateSequence(startOfWeekStartDate) { it.with(DayOfWeek.SUNDAY).plus(Period.ofWeeks(1)) }
            .takeWhile { it.isBefore(endDate) || it.isEqual(endDate) }
            .map {
                this.getClosingBooksMetrics(it).moneyToBank
            }
            .sum()
    }


    fun create(model: AccountRequestDto): AccountResponseDto? {
        val accountTypeRef = object : TypeReference<Account>() {}
        var account = objectMapper.convertValue(model, accountTypeRef)
        val loanAccountsCount = repository.countByMemberIdAndType(
            model.memberId,
            arrayOf(AccountType.LOAN.name)
        )
        account.id = String.format(
            "LOAN-%s-%s", model.memberId, loanAccountsCount + 1
        )

        account.member = membersRepository.findById(model.memberId!!).get()
        account = repository.save(account)


        val typeRef = object : TypeReference<AccountResponseDto>() {}
        return objectMapper.convertValue(account, typeRef)
    }


    fun countOutstandingLoansByGender(startDate: LocalDate, endDate: LocalDate, gender: String, type: String): Int {

        val count = when (type) {
            "outstanding" -> {
                repository.countOutstandingLoansByGender(startDate, endDate, gender)
            }
            "defaulting" -> {
                0
            }
            else -> 0
        }

        return count
    }

    fun sumOutstandingLoansByGender(startDate: LocalDate, endDate: LocalDate, gender: String, type: String?): Float {


        val sum = when (type) {
            "outstanding" -> {
                repository.sumOutstandingLoansByGender(startDate, endDate, gender)
            }
            "defaulting" -> {
                0f
            }
            else -> 0.0f
        }

        return sum
    }
}