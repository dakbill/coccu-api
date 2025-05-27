package com.coc.cu.services

import com.coc.cu.domain.AccountType
import com.coc.cu.domain.RawTransactionRequestDto
import com.coc.cu.domain.TransactionResponseDto
import com.coc.cu.domain.TransactionType
import com.coc.cu.entities.Account
import com.coc.cu.entities.Guarantor
import com.coc.cu.entities.Member
import com.coc.cu.entities.Transaction
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
        val em: EntityManager = emf.createEntityManager()
        em.transaction.begin()
        em.createNativeQuery("truncate transaction cascade").executeUpdate()
        em.transaction.commit()

        val reader = GoogleSheetUtils()
        val spreadsheetId = "17fuYsDWkBkv4aLaVI3ZwXdNAc2Pam52-jFqcN6N6FjI"
        val range = "Transactions!A2:J8000"
        val serviceAccount = resourceLoader.getResource("classpath:credentials.json").inputStream
        val data = reader.readSheet(GoogleCredentials.fromStream(serviceAccount), spreadsheetId, range)

        data.forEach { record ->
            try {
                if (record.isEmpty()) return


                val transactionType = TransactionType.values().find {
                    it.name == record[3].toString().trim().replace(" ", "_").replace("PERSONEL", "PERSONNEL")
                }

                if  (transactionType == null) {
                    println("Unknown transaction type: $record")
                    return@forEach
                }

                val transaction = Transaction(
                    createdDate = LocalDateTime.parse(
                        "${record[5]} 00:00", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
                    ),
                    type = transactionType,
                    amount = record[4].toString().replace("GHS", "").replace(",", "").toFloatOrNull(),
                    comment = record.getOrNull(9)?.toString()
                )

                val memberId = record[1].toString().toLongOrNull() ?: return@forEach
                val accountType = if (transaction.type!!.name.contains("LOAN")) AccountType.LOAN else AccountType.SAVINGS
                val accountNumber = resolveAccountNumber(memberId, transaction.type!!)


                val member = membersRepository.findById(memberId).orElseGet {
                    membersRepository.save(Member(id = memberId, createdDate = transaction.createdDate))
                }.apply {
                    if (createdDate!!.isAfter(transaction.createdDate)) {
                        createdDate = transaction.createdDate
                        membersRepository.save(this)
                    }

                }


                val account = memberAccountRepository.findById(accountNumber).orElse(
                    memberAccountRepository.save(Account(member, accountType, accountNumber, createdDate = transaction.createdDate))
                ).apply {
                    if (createdDate!!.isAfter(transaction.createdDate)){
                        createdDate = transaction.createdDate
                        memberAccountRepository.save(this)
                    }
                }



                transaction.account = account
                repository.save(transaction)

                updateAccountBalance(account)
            } catch (ex: Exception) {
                println(record)
                ex.printStackTrace()
            }
        }

//        em.transaction.begin()
//        applyPostProcessing(em)
//        em.transaction.commit()
        membersRepository.updateTotalBalance(0)
        membersRepository.resetMemberIdSequence()
    }

    private fun resolveAccountNumber(memberId: Long, transactionType: TransactionType): String {
        if (!transactionType.name.contains("LOAN")) return memberId.toString()

        val loanAccountsCount = memberAccountRepository.countByMemberIdAndType(memberId, arrayOf(AccountType.LOAN.name))
        val lastLoanAccountNumber = "LOAN-$memberId-$loanAccountsCount"
        val lastTransaction = repository.lastByAccountId(lastLoanAccountNumber)

        return if (lastTransaction == null || !arrayOf(TransactionType.LOAN, TransactionType.LOAN_CHEQUE).contains(lastTransaction.type)) {
            "LOAN-$memberId-${loanAccountsCount + 1}"
        } else {
            lastLoanAccountNumber
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

    private fun applyPostProcessing(em: EntityManager) {
        em.createNativeQuery("UPDATE \"transaction\" SET account_id='LOAN-200-1' WHERE account_id='LOAN-200-2' AND amount=100 AND CAST(created_date AS DATE)='2015-08-15' AND type='LOAN_REPAYMENT'").executeUpdate()
        em.createNativeQuery("UPDATE \"transaction\" SET account_id='LOAN-546-1' WHERE account_id='LOAN-546-2' AND amount=1000 AND CAST(created_date AS DATE)='2022-11-20' AND type='LOAN_REPAYMENT'").executeUpdate()
//        em.createNativeQuery("DELETE FROM \"account\" WHERE id='LOAN-406-2'").executeUpdate()
    }
}