package com.coc.cu

import com.amazonaws.auth.AWSStaticCredentialsProvider
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.client.builder.AwsClientBuilder
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.coc.cu.domain.AccountType
import com.coc.cu.domain.TransactionType
import com.coc.cu.entities.Account
import com.coc.cu.entities.Member
import com.coc.cu.entities.Transaction
import com.coc.cu.repositories.AccountTransactionsRepository
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
import java.time.LocalDate
import java.time.format.DateTimeFormatter


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
        memberAccountRepository: MemberAccountRepository
    ) = CommandLineRunner {
        registerMembers(membersRepository, memberAccountRepository)
        recordTransactions(accountTransactionsRepository, membersRepository, memberAccountRepository)
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
        manager.createUser(users.username("taichobill@gmail.com").password(passwordEncoder!!.encode("password")).roles("USER", "ADMIN").build())
        manager.createUser(users.username("eben.ashley@gmail.com").password(passwordEncoder!!.encode("password")).roles("USER", "ADMIN").build())
        manager.createUser(users.username("admin").password(passwordEncoder!!.encode("admin")).roles("USER", "ADMIN").build())
        manager.createUser(users.username("bernardakuffo@hotmail.com").password(passwordEncoder!!.encode("password")).roles("USER", "ADMIN").build())
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
            var user = Member()
            user.name = record[1]
            user.id = record[0].toLong()
            user.phone = record[2]
            user = repository.save(user)

            var accountNumber = user.id.toString()
            var accountOptional = memberAccountRepository.findById(accountNumber)
            if (accountOptional.isEmpty) {
                var account = Account(user, AccountType.SAVINGS, accountNumber)
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
            BufferedReader((InputStreamReader(ClassPathResource("/fixtures/Transactions_Transactions.csv").inputStream)))
        val csvParser = CSVParser(
            bufferedReader,
            CSVFormat.EXCEL.withFirstRecordAsHeader()
                .withHeader(
                    "Col1", "Col2", "Col3", "Col5", "Col6",
                    "Col7", "Col8", "Col9", "Col10"
                )
        )

        val transactionsCount =  repository.count()

        for ((counter, record) in csvParser.records.withIndex()) {

            if ((counter + 1) <= transactionsCount) {
                continue
            }

            var transaction = Transaction()

            if (record[4].isNotEmpty()) {
                transaction.amount = record[4]
                    .replace("GHS", "")
                    .replace(",", "").toFloat()
            }

            transaction.createdDate = LocalDate.parse(record[5], DateTimeFormatter.ISO_DATE)
            var t = TransactionType.values().filter { t -> t.name == record[3].trim().replace(" ", "_") }
            if (t.isNotEmpty()) {
                transaction.type = t.first()


                if (Strings.isNotEmpty(record[1])) {
                    var accountNumber = record[1].toLong().toString()
                    var accountType = AccountType.SAVINGS
                    if (transaction.type!!.name.contains("LOAN")) {
                        accountNumber = String.format("LOAN-%s", accountNumber.toLong())
                        accountType = AccountType.LOAN
                    }

                    var account: Account? = null
                    var accountOptional = memberAccountRepository.findById(accountNumber)
                    var createdDate =
                        LocalDate.parse(record[5].trim(), DateTimeFormatter.ISO_DATE)
                    if (accountOptional.isPresent) {
                        account = accountOptional.get()
                        if ((account.createdDate == null || account.createdDate!!.isAfter(createdDate)) && record[5].isNotEmpty()) {
                            account.createdDate = createdDate
                            account = memberAccountRepository.save(account)
                        }


                    } else if (record[1].isNotEmpty()) {
                        var memberOptional = membersRepository.findById(record[1].toLong())
                        if (memberOptional.isPresent) {
                            account = Account(memberOptional.get(), accountType, accountNumber)
                            if (record[5].isNotEmpty()) {
                                account.createdDate = createdDate
                            }

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












