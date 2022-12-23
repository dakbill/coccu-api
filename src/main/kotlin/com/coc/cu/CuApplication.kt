package com.coc.cu

import com.amazonaws.auth.AWSStaticCredentialsProvider
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.client.builder.AwsClientBuilder
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.coc.cu.domain.AccountType
import com.coc.cu.domain.TransactionType
import com.coc.cu.entities.Account
import com.coc.cu.entities.Guarantor
import com.coc.cu.entities.Member
import com.coc.cu.entities.Transaction
import com.coc.cu.repositories.AccountTransactionsRepository
import com.coc.cu.repositories.GuarantorRepository
import com.coc.cu.repositories.MemberAccountRepository
import com.coc.cu.repositories.MembersRepository
import com.coc.cu.utils.JwtUtils
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import org.apache.logging.log4j.util.Strings
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.core.io.ClassPathResource
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.provisioning.InMemoryUserDetailsManager
import org.springframework.web.client.RestTemplate
import java.io.BufferedReader
import java.io.InputStreamReader
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.regex.Matcher
import java.util.regex.Pattern
import java.util.stream.Collectors


@SpringBootApplication
class CuApplication {

    @Value("\${cloud.aws.credentials.accessKey}")
    private val accessKey: String? = null

    @Value("\${cloud.aws.credentials.secretKey}")
    private val secretKey: String? = null

    @Value("\${cloud.aws.endpoint}")
    private val endpointUrl: String? = null

    @Value("\${cloud.aws.region}")
    private val region: String? = null


    @Bean
    fun init(
        membersRepository: MembersRepository,
        accountTransactionsRepository: AccountTransactionsRepository,
        memberAccountRepository: MemberAccountRepository,
        guarantorRepository: GuarantorRepository
    ) = CommandLineRunner {
        registerMembers(membersRepository, memberAccountRepository)
        recordTransactions(
            accountTransactionsRepository,
            membersRepository,
            memberAccountRepository,
            guarantorRepository
        )
    }


    @Bean
    fun objectMapper(): ObjectMapper {
        var om = jacksonObjectMapper()
        om.findAndRegisterModules()

        return om
    }

    @Bean
    fun restTemplate(): RestTemplate {
        return RestTemplate()
    }

    @Bean
    fun passwordEncoder(): PasswordEncoder? {
        return BCryptPasswordEncoder()
    }

    @Bean
    fun userDetailsService(): UserDetailsService {
        val passwordEncoder = passwordEncoder()
        val users: User.UserBuilder = User.builder()
        val manager = InMemoryUserDetailsManager()
        manager.createUser(
            users.username("taichobill@gmail.com").password(passwordEncoder!!.encode("0541928449"))
                .roles("USER", "ADMIN")
                .build()
        )
        manager.createUser(
            users.username("eben.ashley@gmail.com").password(passwordEncoder!!.encode("0209980010"))
                .roles("USER", "ADMIN").build()
        )
        manager.createUser(
            users.username("teller").password(passwordEncoder!!.encode("cuateller")).roles("USER", "TELLER").build()
        )
        manager.createUser(
            users.username("bernardakuffo@hotmail.com").password(passwordEncoder!!.encode("0249853588"))
                .roles("USER", "ADMIN").build()
        )
        return manager
    }


    @Bean
    fun jwtUtils(): JwtUtils {
        return JwtUtils()
    }

    @Bean
    fun getClient(): AmazonS3? {

        val credentials = BasicAWSCredentials(accessKey, secretKey)
        return AmazonS3ClientBuilder
            .standard()
            .withCredentials(AWSStaticCredentialsProvider(credentials))
            .withEndpointConfiguration(AwsClientBuilder.EndpointConfiguration(endpointUrl, region))
            .build()
    }

    @Bean
    @Throws(Exception::class)
    fun authenticationManager(
        authenticationConfiguration: AuthenticationConfiguration
    ): AuthenticationManager? {
        return authenticationConfiguration.authenticationManager
    }


    fun registerMembers(repository: MembersRepository, memberAccountRepository: MemberAccountRepository) {
        val bufferedReader =
            BufferedReader((InputStreamReader(ClassPathResource("/fixtures/Transactions_Members.csv").inputStream)))
        val csvParser = CSVParser(bufferedReader, CSVFormat.DEFAULT.withFirstRecordAsHeader())


        for (record in csvParser.records) {

            val userId = record[0].toLong()
            var member = repository.findById(userId)
                .orElse(
                    Member(
                        id = userId,
                        name = record[1],
                        createdDate = LocalDateTime.now(),
                        totalBalance = 0.0,
                        phone = record[2]
                    )
                )
            member = repository.save(member)

            var accountNumber = member.id.toString()
            var account = memberAccountRepository.findById(accountNumber)
                .orElse(Account(member, AccountType.SAVINGS, accountNumber, createdDate = LocalDateTime.now()))
            memberAccountRepository.save(account)


        }
    }

    fun recordTransactions(
        repository: AccountTransactionsRepository,
        membersRepository: MembersRepository,
        memberAccountRepository: MemberAccountRepository,
        guarantorRepository: GuarantorRepository,
    ) {
        val bufferedReader =
            BufferedReader((InputStreamReader(ClassPathResource("/fixtures/Transactions_Transactions.csv").inputStream)))
        val csvParser = CSVParser(
            bufferedReader,
            CSVFormat.EXCEL.withFirstRecordAsHeader()
                .withHeader(
                    "Col1", "Col2", "Col3", "Col5", "Col6",
                    "Col7", "Col8", "Col9", "Col10"
                )
        )

        val transactionsCount = repository.count()

        for ((counter, record) in csvParser.records.withIndex()) {

            if ((counter + 1) <= transactionsCount) {
                continue
            }

            var t = TransactionType.values().filter { t -> t.name == record[3].trim().replace(" ", "_") }
            if (t.isEmpty()) {
                continue
            }

            var transaction = Transaction(
                createdDate = LocalDateTime.parse(
                    String.format("%s 00:00", record[5]),
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
                ),
                type = t.first()
            )


            if (record[4].isNotEmpty()) {
                transaction.amount = record[4]
                    .replace("GHS", "")
                    .replace(",", "")
                    .toFloat()
            }

            if (Strings.isNotEmpty(record[1])) {
                val memberId = record[1].toLong()
                var accountNumber = memberId.toString()
                val accountType =
                    if (transaction.type!!.name.contains("LOAN")) AccountType.LOAN else AccountType.SAVINGS


                if (transaction.type.toString().contains("LOAN")) {
                    var loanAccountsCount = memberAccountRepository.countByMemberIdAndType(
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


                var memberOptional = membersRepository.findById(record[1].toLong())
                if (!memberOptional.isPresent) continue

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
                            createdDate = transaction.createdDate
                        )
                    )

                if (record[9].toString().isNotEmpty()) {
                    val p: Pattern = Pattern.compile("\\[Guarantors.*\\)\\]", Pattern.MULTILINE)
                    val m: Matcher = p.matcher(record[9].toString())
                    if (m.find()) {

                        if (account.guarantors!!.isEmpty()) {
                            account.guarantors = arrayListOf()
                        }

                        m.group().split(Pattern.compile("(\\(|\\)|,|Guarantors\\:)")).stream()
                            .filter { s -> s.trim().matches(Pattern.compile("\\d+").toRegex()) }
                            .forEach { s ->
                                val member = membersRepository.findById(
                                    s.toLong()
                                ).get()
                                account.guarantors!!.add(
                                    guarantorRepository.save(
                                        Guarantor(
                                            member = member,
                                            amount = 0.0f,
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


            repository.save(transaction)

        }
    }

}


fun main(args: Array<String>) {
    runApplication<CuApplication>(*args)
}












