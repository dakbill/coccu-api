package com.coc.cu

import antlr.StringUtils
import com.coc.cu.domain.AccountType
import com.coc.cu.domain.TransactionType
import com.coc.cu.entities.Member
import com.coc.cu.entities.MemberAccount
import com.coc.cu.entities.Transaction
import com.coc.cu.repositories.AccountTransactionsRepository
import com.coc.cu.repositories.MemberAccountRepository
import com.coc.cu.repositories.MembersRepository
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import org.apache.logging.log4j.util.Strings
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.core.io.ClassPathResource
import java.io.BufferedReader
import java.io.InputStreamReader


@SpringBootApplication
class CuApplication {

    @Bean
    fun init(
        membersRepository: MembersRepository,
        accountTransactionsRepository: AccountTransactionsRepository,
        memberAccountRepository: MemberAccountRepository
    ) = CommandLineRunner {
        registerMembers(membersRepository,memberAccountRepository)
        recordTransactions(accountTransactionsRepository, membersRepository, memberAccountRepository)
    }


    fun registerMembers(repository: MembersRepository,memberAccountRepository: MemberAccountRepository) {
        val bufferedReader = BufferedReader((InputStreamReader(ClassPathResource("/fixtures/Members.csv").inputStream)))
        val csvParser = CSVParser(bufferedReader, CSVFormat.DEFAULT.withFirstRecordAsHeader())


        for (record in csvParser.records) {
            var user = Member()
            user.name = record[1]
            user.id = record[0].toLong()
            user = repository.save(user)

            var accountNumber = user.id.toString()
            var accountOptional = memberAccountRepository.findById(accountNumber)
            if (accountOptional.isEmpty) {
                var account = MemberAccount(user, AccountType.SAVINGS, accountNumber)
                account = memberAccountRepository.save(account)
                memberAccountRepository.save(account)
            }
        }
    }

    fun recordTransactions(
        repository: AccountTransactionsRepository,
        membersRepository: MembersRepository,
        memberAccountRepository: MemberAccountRepository
    ) {
        val bufferedReader =
            BufferedReader((InputStreamReader(ClassPathResource("/fixtures/Transactions.csv").inputStream)))
        val csvParser = CSVParser(
            bufferedReader,
            CSVFormat.EXCEL.withFirstRecordAsHeader()
                .withHeader(
                    "Col1", "Col2", "Col3", "Col5", "Col6",
                    "Col7", "Col8", "Col9", "Col10"
                )
        )


        for (record in csvParser.records) {


            var transaction = Transaction()

            transaction.amount = record[4]
                .replace("GHS", "")
                .replace(",", "").toFloat()
            var t = TransactionType.values().filter { t -> t.name == record[3].replace(" ", "_") }
            if (t.isNotEmpty()) {
                transaction.type = t.first()

                if (Strings.isNotEmpty(record[1])) {
                    var accountNumber = record[1].toLong().toString()
                    var accountType = AccountType.SAVINGS
                    if (transaction.type!!.name.contains("LOAN")) {
                        accountNumber = String.format("LOAN-%s", accountNumber.toLong())
                        accountType = AccountType.LOAN
                    }

                    var account: MemberAccount? = null
                    var accountOptional = memberAccountRepository.findById(accountNumber)
                    if (accountOptional.isPresent) {
                        account = accountOptional.get()
                    } else if (record[1].isNotEmpty()) {
                        var memberOptional = membersRepository.findById(record[1].toLong())
                        if (memberOptional.isPresent) {
                            account = MemberAccount(memberOptional.get(), accountType, accountNumber)
                            account = memberAccountRepository.save(account)
                            memberAccountRepository.save(account)
                        }

                    }

                    transaction.account = account
                }



            }

            repository.save(transaction)
        }
    }

}

fun main(args: Array<String>) {
    runApplication<CuApplication>(*args)


}












