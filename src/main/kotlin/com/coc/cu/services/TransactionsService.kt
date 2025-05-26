package com.coc.cu.services

import com.coc.cu.domain.AccountType
import com.coc.cu.domain.RawTransactionRequestDto
import com.coc.cu.domain.TransactionResponseDto
import com.coc.cu.domain.TransactionType
import com.coc.cu.entities.Account
import com.coc.cu.entities.Guarantor
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
import org.apache.logging.log4j.util.Strings
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
        em.transaction.begin();
        em.createNativeQuery("truncate transaction cascade").executeUpdate()

        val reader = GoogleSheetUtils()

        val spreadsheetId = "17fuYsDWkBkv4aLaVI3ZwXdNAc2Pam52-jFqcN6N6FjI"
        val range = "Transactions!A2:J8000"
        val resource = resourceLoader.getResource("classpath:credentials.json")
        val serviceAccount = resource.inputStream


        val data = reader.readSheet(GoogleCredentials.fromStream(serviceAccount), spreadsheetId, range)


        for(record in data) {
            try {


                if (record.isEmpty()) {
                    return
                }


                val t = TransactionType.values().filter { t ->
                    t.name == record[3].toString().trim().replace(" ", "_").replace("PERSONEL", "PERSONNEL")
                }
                if (t.isNotEmpty()) {

                    val transaction = Transaction(
                        createdDate = LocalDateTime.parse(
                            String.format("%s 00:00", record[5]),
                            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
                        ),
                        type = t.first(),
                    )

                    if (record.size >= 10) {
                        transaction.comment = record[9].toString()
                    }


                    if (record[4].toString().isNotEmpty()) {
                        transaction.amount = record[4]
                            .toString()
                            .replace("GHS", "")
                            .replace(",", "")
                            .toFloat()
                    }

                    if (Strings.isNotEmpty(record[1].toString())) {
                        val memberId = record[1].toString().toLong()
                        var accountNumber = memberId.toString()
                        val accountType =
                            if (transaction.type!!.name.contains("LOAN")) AccountType.LOAN else AccountType.SAVINGS


                        if (transaction.type.toString().contains("LOAN")) {
                            val loanAccountsCount = memberAccountRepository.countByMemberIdAndType(
                                memberId,
                                arrayOf(AccountType.LOAN.name)
                            )

                            val lastLoanAccountNumber = "LOAN-${memberId}-${loanAccountsCount}"
                            val lastTransaction = repository.lastByAccountId(lastLoanAccountNumber)


                            accountNumber =
                                if (
                                    (arrayOf(
                                        TransactionType.LOAN_CHEQUE,
                                        TransactionType.LOAN
                                    ).contains(transaction.type) || loanAccountsCount == 0L)
                                    && !(lastTransaction != null && arrayOf(
                                        TransactionType.LOAN_CHEQUE,
                                        TransactionType.LOAN
                                    ).contains(lastTransaction.type))
                                ) {
                                    "LOAN-${memberId}-${loanAccountsCount + 1}"
                                } else {
                                    lastLoanAccountNumber
                                }

                        }


                        val memberOptional = membersRepository.findById(record[1].toString().toLong())
                        if (memberOptional.isPresent) {
                            //update member creation date if transaction is older
                            val member = memberOptional.get()
                            if (member.createdDate!!.isAfter(transaction.createdDate)) {
                                member.createdDate = transaction.createdDate
                                membersRepository.save(member)
                            }

                            var account = memberAccountRepository.findById(accountNumber)
                                .orElse(
                                    Account(
                                        member = member,
                                        type = accountType,
                                        id = accountNumber,
                                        interestRate = if (AccountType.LOAN != accountType) null else
                                            if (transaction.createdDate!!.year <= 2022) 0.18f else 0.2f,
                                        createdDate = transaction.createdDate
                                    )
                                )

                            if (record.size >= 10 && record[9].toString().isNotEmpty()) {
                                val p: Pattern = Pattern.compile("\\[Guarantors.*\\)]", Pattern.MULTILINE)
                                val m: Matcher = p.matcher(record[9].toString())
                                if (m.find()) {

                                    if (account.guarantors!!.isEmpty()) {
                                        account.guarantors = arrayListOf()
                                    }

                                    m.group().split(Pattern.compile("(\\(|\\)|,|Guarantors:)")).stream()
                                        .filter { s -> s.trim().matches(Pattern.compile("(\\d|:)+").toRegex()) }
                                        .forEach { s ->
                                            val (guarantorMemberId, amount) = s.split(":")

                                            val guarantorMember = membersRepository.findById(
                                                guarantorMemberId.toLong()
                                            ).get()
                                            account.guarantors!!.add(
                                                guarantorRepository.save(
                                                    Guarantor(
                                                        member = guarantorMember,
                                                        amount = amount.toFloat(),
                                                        createdDate = transaction.createdDate,
                                                    )
                                                )
                                            )


                                        }


                                    account = memberAccountRepository.save(account)
                                }
                            }

                            if (account.createdDate!!.isAfter(transaction.createdDate)) {
                                account.createdDate = transaction.createdDate
                            }



                            transaction.account = memberAccountRepository.save(account)
                        }


                    }


                    repository.save(transaction)

                    val account = transaction.account
                    if(account!=null){
                        if (account.type == AccountType.SAVINGS) {
                            account.balance = repository.findBySavingsBalance(account.id)
                        } else if (account.type == AccountType.LOAN) {
                            account.balance = repository.findByLoanBalance(account.id)
                        } else if (account.member!!.name!!.lowercase()
                                .contains("admin") || account.member!!.name!!.lowercase()
                                .contains("expenses")
                        ) {
                            account.balance = repository.findByAdminExpensesBalance(account.id)
                        }
                        memberAccountRepository.save(account)
                    }

                }


            } catch (ex: Exception) {
                ex.printStackTrace()
            }

        }


        em.createNativeQuery("UPDATE \"transaction\" SET account_id='LOAN-200-1' WHERE account_id='LOAN-200-2' AND amount=100 AND CAST(created_date AS DATE)='2015-08-15' AND type='LOAN_REPAYMENT'")
            .executeUpdate()
        em.createNativeQuery("UPDATE \"transaction\" SET account_id='LOAN-406-1' WHERE account_id='LOAN-406-2' AND amount=300 AND CAST(created_date AS DATE)='2015-02-08' AND type='LOAN'")
            .executeUpdate()
        em.createNativeQuery("UPDATE \"transaction\" SET account_id='LOAN-546-1' WHERE account_id='LOAN-546-2' AND amount=1000 AND CAST(created_date AS DATE)='2022-11-20' AND type='LOAN_REPAYMENT'")
            .executeUpdate()
        em.createNativeQuery("DELETE FROM \"account\" WHERE id='LOAN-406-2'").executeUpdate()

        em.createNativeQuery("UPDATE \"account\" SET balance=(SELECT ( (SELECT COALESCE(SUM(CAST(COALESCE(AMOUNT,0) AS DECIMAL )),0) FROM TRANSACTION WHERE account_id = 'LOAN-200-2' AND TYPE IN ('OPENING_LOAN_BALANCE','LOAN','LOAN_CHEQUE'))  - (SELECT COALESCE(SUM(CAST(COALESCE(AMOUNT,0) AS DECIMAL )),0) FROM TRANSACTION WHERE account_id = 'LOAN-200-2' AND TYPE IN ('LOAN_REPAYMENT','LOAN_REPAYMENT_CHEQUE')) ) ) WHERE id='LOAN-200-2'")
            .executeUpdate()
        em.createNativeQuery("UPDATE \"account\" SET balance=(SELECT ( (SELECT COALESCE(SUM(CAST(COALESCE(AMOUNT,0) AS DECIMAL )),0) FROM TRANSACTION WHERE account_id = 'LOAN-406-1' AND TYPE IN ('OPENING_LOAN_BALANCE','LOAN','LOAN_CHEQUE'))  - (SELECT COALESCE(SUM(CAST(COALESCE(AMOUNT,0) AS DECIMAL )),0) FROM TRANSACTION WHERE account_id = 'LOAN-406-1' AND TYPE IN ('LOAN_REPAYMENT','LOAN_REPAYMENT_CHEQUE')) ) ) WHERE id='LOAN-406-1'")
            .executeUpdate()
        em.createNativeQuery("UPDATE \"account\" SET balance=(SELECT ( (SELECT COALESCE(SUM(CAST(COALESCE(AMOUNT,0) AS DECIMAL )),0) FROM TRANSACTION WHERE account_id = 'LOAN-546-1' AND TYPE IN ('OPENING_LOAN_BALANCE','LOAN','LOAN_CHEQUE'))  - (SELECT COALESCE(SUM(CAST(COALESCE(AMOUNT,0) AS DECIMAL )),0) FROM TRANSACTION WHERE account_id = 'LOAN-546-1' AND TYPE IN ('LOAN_REPAYMENT','LOAN_REPAYMENT_CHEQUE')) ) ) WHERE id='LOAN-546-1'")
            .executeUpdate()

        em.transaction.commit();

        membersRepository.updateTotalBalance(0)
        membersRepository.resetMemberIdSequence()
    }
}