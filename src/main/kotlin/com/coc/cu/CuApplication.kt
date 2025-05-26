package com.coc.cu

import com.amazonaws.auth.AWSStaticCredentialsProvider
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.client.builder.AwsClientBuilder
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.coc.cu.repositories.AccountTransactionsRepository
import com.coc.cu.repositories.GuarantorRepository
import com.coc.cu.repositories.MemberAccountRepository
import com.coc.cu.services.TransactionsService
import com.coc.cu.services.UsersService
import com.coc.cu.utils.JwtUtils
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.provisioning.InMemoryUserDetailsManager
import org.springframework.web.client.RestTemplate
import javax.persistence.EntityManagerFactory


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
        usersService: UsersService,
        transactionsService: TransactionsService,
        accountTransactionsRepository: AccountTransactionsRepository,
        memberAccountRepository: MemberAccountRepository,
        guarantorRepository: GuarantorRepository,
        emf: EntityManagerFactory
    ) = CommandLineRunner {


        usersService.registerMembers()
        transactionsService.recordTransactions()
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


    

}


fun main(args: Array<String>) {
    runApplication<CuApplication>(*args)
}












