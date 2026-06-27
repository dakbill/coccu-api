package com.coc.cu.services

import com.coc.cu.domain.AccountType
import com.coc.cu.domain.RawTransactionRequestDto
import com.coc.cu.domain.TransactionResponseDto
import com.coc.cu.domain.TransactionType
import com.coc.cu.entities.Account
import com.coc.cu.entities.Guarantor
import com.coc.cu.entities.Member
import com.coc.cu.entities.Transaction
import com.coc.cu.entities.TransactionListener
import com.coc.cu.repositories.AccountTransactionsRepository
import com.coc.cu.repositories.GuarantorRepository
import com.coc.cu.repositories.MemberAccountRepository
import com.coc.cu.repositories.MembersRepository
import com.coc.cu.utils.GoogleSheetUtils
import com.coc.cu.utils.JwtUtils
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.auth.oauth2.GoogleCredentials
import org.springframework.core.io.ResourceLoader
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.regex.Matcher
import java.util.regex.Pattern
import javax.persistence.EntityManager
import javax.persistence.EntityManagerFactory
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate


@Service
class TransactionsService(
    var repository: AccountTransactionsRepository,
    var membersRepository: MembersRepository,
    var memberAccountRepository: MemberAccountRepository,
    var guarantorRepository: GuarantorRepository,
    var objectMapper: ObjectMapper,
    var restTemplate: RestTemplate,
    var jwtUtils: JwtUtils,
    var emf: EntityManagerFactory,
    var em: EntityManager,
    var transactionManager: PlatformTransactionManager,
    var resourceLoader: ResourceLoader
) {

    fun single(id: Long): TransactionResponseDto? {
        val typeRef = object : TypeReference<TransactionResponseDto>() {}


        val res = repository.findById(id)
        if (res.isPresent) {
            val transactionEntity = res.get()

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



        val transactionsPage: Page<Transaction> = repository.findAllByMemberId(
            if (query == "_") "" else (query ?: ""),
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


        if (model.date != null) {
            transaction.createdDate = model.date!!.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()
        } else {
            transaction.createdDate = LocalDateTime.now()
        }


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

        if (model.interestRate != null) {
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
            val memberId = single(id)?.account?.member?.id
            repository.deleteById(id)
            deleted = true

            if (memberId != null) {
                membersRepository.updateTotalBalance(memberId)
            }
        } catch (e: Exception) {
            //ignored
        }

        return deleted
    }


    fun recordTransactions() {

        val reader = GoogleSheetUtils()
        val spreadsheetId = "17fuYsDWkBkv4aLaVI3ZwXdNAc2Pam52-jFqcN6N6FjI"
        val range = "Transactions!A2:J10000"
        val serviceAccount = resourceLoader.getResource("classpath:credentials.json").inputStream
        val data = reader.readSheet(GoogleCredentials.fromStream(serviceAccount), spreadsheetId, range)

        // Disable entity listener temporarily for bulk sync to prevent thread spawning
        TransactionListener.enabled = false

        try {
            val transactionTemplate = TransactionTemplate(transactionManager)
            transactionTemplate.execute {
                // Truncate existing transactions and accounts
                em.createNativeQuery("truncate transaction cascade").executeUpdate()
                em.createNativeQuery("truncate account cascade").executeUpdate()
                em.createNativeQuery("truncate guarantor cascade").executeUpdate()

                // Load all existing members from the database
                val membersMap = membersRepository.findAll().associateBy { it.id!! }.toMutableMap()
                val accountsMap = mutableMapOf<String, Account>()
                val lastTransactionMap = mutableMapOf<String, Transaction>()
                val loanAccountsCountMap = mutableMapOf<Long, Long>()

                val transactionsToSave = mutableListOf<Transaction>()
                val modifiedMembers = mutableSetOf<Member>()

                for (record in data) {
                    try {
                        if (record.isEmpty()) break

                        val transactionType = TransactionType.values().find {
                            it.name == record[3].toString().trim().replace(" ", "_").replace("PERSONEL", "PERSONNEL")
                        }

                        if (transactionType == null) {
                            println("Unknown transaction type: $record")
                            continue
                        }

                        val transaction = Transaction(
                            createdDate = LocalDateTime.parse(
                                "${record[5]} 00:00", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
                            ),
                            type = transactionType,
                            amount = record[4].toString().replace("GHS", "").replace(",", "").toFloatOrNull(),
                            comment = record.getOrNull(9)?.toString()
                        )

                        val memberId = record[1].toString().toLongOrNull() ?: continue

                        val accountType = if (transaction.type!!.name.contains("LOAN")) AccountType.LOAN else AccountType.SAVINGS

                        val accountNumber = resolveAccountNumber(memberId, transaction.type!!, loanAccountsCountMap, lastTransactionMap)

                        val member = membersMap[memberId] ?: Member(id = memberId, createdDate = transaction.createdDate).apply {
                            membersMap[memberId] = this
                            modifiedMembers.add(this)
                        }

                        if (member.createdDate == null || member.createdDate!!.isAfter(transaction.createdDate)) {
                            member.createdDate = transaction.createdDate
                            modifiedMembers.add(member)
                        }

                        val account = accountsMap[accountNumber] ?: Account(member, accountType, accountNumber, createdDate = transaction.createdDate, balance = 0.0).apply {
                            accountsMap[accountNumber] = this
                        }

                        if (account.createdDate == null || account.createdDate!!.isAfter(transaction.createdDate)) {
                            account.createdDate = transaction.createdDate
                        }

                        // Calculate balance in memory
                        val amount = transaction.amount ?: 0.0f
                        val currentBalance = account.balance ?: 0.0
                        account.balance = when (account.type) {
                            AccountType.SAVINGS -> {
                                if (arrayOf(TransactionType.OPENING_BALANCE, TransactionType.SAVINGS, TransactionType.SAVINGS_CHEQUE).contains(transaction.type)) {
                                    currentBalance + amount
                                } else if (arrayOf(TransactionType.WITHDRAWAL, TransactionType.WITHDRAWAL_CHEQUE).contains(transaction.type)) {
                                    currentBalance - amount
                                } else {
                                    currentBalance
                                }
                            }
                            AccountType.LOAN -> {
                                if (arrayOf(TransactionType.OPENING_LOAN_BALANCE, TransactionType.LOAN, TransactionType.LOAN_CHEQUE).contains(transaction.type)) {
                                    currentBalance + amount
                                } else if (arrayOf(TransactionType.LOAN_REPAYMENT, TransactionType.LOAN_REPAYMENT_CHEQUE).contains(transaction.type)) {
                                    currentBalance - amount
                                } else {
                                    currentBalance
                                }
                            }
                            else -> { // admin/expenses
                                if (arrayOf(TransactionType.STATIONERY, TransactionType.STATIONERY_CHEQUE, TransactionType.TRANSPORT, TransactionType.INCENTIVE_TO_PERSONNEL, TransactionType.INCENTIVE_TO_PERSONNEL_CHEQUE).contains(transaction.type)) {
                                    currentBalance + amount
                                } else if (transaction.type == TransactionType.WITHDRAWAL) {
                                    currentBalance - amount
                                } else {
                                    currentBalance
                                }
                            }
                        }

                        transaction.account = account
                        transactionsToSave.add(transaction)
                        lastTransactionMap[accountNumber] = transaction

                    } catch (ex: Exception) {
                        println(record)
                        ex.printStackTrace()
                    }
                }

                // Bulk save all entities
                membersRepository.saveAll(modifiedMembers)
                memberAccountRepository.saveAll(accountsMap.values)
                repository.saveAll(transactionsToSave)

                // Bulk update total balances and transaction counts for all members
                membersRepository.updateTotalBalance(0)
                membersRepository.updateAllTransactionCounts()
            }
        } finally {
            TransactionListener.enabled = true
        }
    }


    private fun resolveAccountNumber(
        memberId: Long,
        transactionType: TransactionType,
        loanAccountsCountMap: MutableMap<Long, Long>,
        lastTransactionMap: Map<String, Transaction>
    ): String {
        if (!transactionType.name.contains("LOAN")) return memberId.toString()

        var loanAccountsCount = loanAccountsCountMap[memberId] ?: 0L
        if (loanAccountsCount == 0L) {
            loanAccountsCount = 1L
            loanAccountsCountMap[memberId] = 1L
        }
        val lastLoanAccountNumber = "LOAN-$memberId-$loanAccountsCount"
        val lastTransaction = lastTransactionMap[lastLoanAccountNumber]


//        println(lastLoanAccountNumber + " - " + transactionType.name + " - " + lastTransaction?.account?.balance)
        return if (
            lastTransaction == null
            || (lastTransaction.account?.balance == null)
            || (arrayOf(
                TransactionType.LOAN,
                TransactionType.LOAN_CHEQUE,

                ).contains(lastTransaction.type) && arrayOf(
                TransactionType.LOAN,
                TransactionType.LOAN_CHEQUE,
            ).contains(transactionType))
            || (
                    lastTransaction.account?.balance != 0.0
                            &&
                            !arrayOf(
                                TransactionType.LOAN.name,
                                TransactionType.LOAN_CHEQUE.name
                            ).contains(transactionType.name)
                    )
            || (
                    lastTransaction.account?.balance == 0.0
                            &&
                            arrayOf(
                                TransactionType.INTEREST_ON_LOAN.name,
                                TransactionType.INTEREST_ON_LOAN_CHEQUE.name
                            ).contains(transactionType.name)
                    )
        ) {
            lastLoanAccountNumber
        } else {
            val nextCount = loanAccountsCount + 1
            loanAccountsCountMap[memberId] = nextCount
            "LOAN-$memberId-$nextCount"
        }
    }

    private fun updateAccountBalance(account: Account) {
        account.balance = when (account.type) {
            AccountType.SAVINGS -> repository.findBySavingsBalance(account.id)
            AccountType.LOAN -> repository.findByLoanBalance(account.id)
            else -> repository.findByAdminExpensesBalance(account.id)
        }
        memberAccountRepository.save(account)
    }


}