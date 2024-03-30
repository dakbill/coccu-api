package com.coc.cu.services

import com.coc.cu.domain.*
import com.coc.cu.entities.Account
import com.coc.cu.entities.Member
import com.coc.cu.repositories.AccountTransactionsRepository
import com.coc.cu.repositories.MemberAccountRepository
import com.coc.cu.repositories.MembersRepository
import com.coc.cu.utils.JwtUtils
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import java.time.LocalDate
import java.util.stream.Collectors


@Service
class UsersService(
    val repository: MembersRepository,
    val memberAccountRepository: MemberAccountRepository,
    val transactionsRepository: AccountTransactionsRepository,
    val objectMapper: ObjectMapper,
    val restTemplate: RestTemplate,
    val storageService: StorageService,
    val authenticationManager: AuthenticationManager,
    val jwtUtils: JwtUtils
) {

    fun single(id: Long): MemberResponseDto? {
        val typeRef = object : TypeReference<MemberResponseDto>() {}

        var res = repository.findById(id)
        if (res.isPresent) {
            var userEntity = res.get()

            val accountTypeRef = object : TypeReference<List<AccountResponseDto>>() {}
            val accounts =
                objectMapper.convertValue(memberAccountRepository.findByMemberId(userEntity.id), accountTypeRef)

            for (account in accounts!!) {
                if (account.type == AccountType.SAVINGS) {
                    account.balance = transactionsRepository.findBySavingsBalance(account.id)
                } else {
                    account.balance = transactionsRepository.findByLoanBalance(account.id)
                }

            }

            val member = objectMapper.convertValue(userEntity, typeRef)
            member.accounts = accounts

            return member
        }

        return null
    }

    fun list(query: String, pageRequest: PageRequest): Page<MemberResponseDto> {
        val typeRef = object : TypeReference<List<MemberResponseDto>>() {}
        val accountsTypeRef = object : TypeReference<List<AccountResponseDto>>() {}

        val members = repository.findByQuery(query.lowercase(), pageRequest)
        val users = objectMapper.convertValue(members.content, typeRef)

        users.stream().forEach { user ->
            run {
                val accounts = memberAccountRepository.findByMemberId(user.id)
                user.accounts = objectMapper.convertValue(accounts, accountsTypeRef)
            }
        }

        return PageImpl(users, pageRequest, members.totalElements)
    }

    fun create(model: UserRequestDto): MemberResponseDto? {
        val memberTypeRef = object : TypeReference<Member>() {}
        var member = objectMapper.convertValue(model, memberTypeRef)
        if(model.memberId!! > 0){
            member.id = model.memberId
        }
        member = repository.save(member)

        var savingsAccount =
            Account(member, AccountType.SAVINGS, String.format("%s-%s", AccountType.SAVINGS, member.id))
        memberAccountRepository.save(savingsAccount)


        val typeRef = object : TypeReference<MemberResponseDto>() {}

        return objectMapper.convertValue(repository.findById(member.id!!).get(), typeRef)
    }

    fun update(id: Long, model: UserRequestDto): MemberResponseDto? {
        var existing = repository.findById(id).get()

        if (model.imageFile != null) {
            model.image = storageService.uploadMultipartFile(model.imageFile!!);
        }

        existing.name = if (model.name.isNullOrBlank()) existing.name else model.name
        existing.image = if (model.image.isNullOrBlank()) existing.image else model.image
        existing.phone = if (model.phone.isNullOrBlank()) existing.phone else model.phone
        existing.email = if (model.email.isNullOrBlank()) existing.email else model.email
        existing.address = if (model.address.isNullOrBlank()) existing.address else model.address
        existing.city = if (model.city.isNullOrBlank()) existing.city else model.city
        existing.firstOfKinName =
            if (model.firstOfKinName.isNullOrBlank()) existing.firstOfKinName else model.firstOfKinName
        existing.firstOfKinPhone =
            if (model.firstOfKinPhone.isNullOrBlank()) existing.firstOfKinPhone else model.firstOfKinPhone
        existing.firstOfKinEmail =
            if (model.firstOfKinEmail.isNullOrBlank()) existing.firstOfKinEmail else model.firstOfKinEmail
        existing.secondOfKinName =
            if (model.secondOfKinName.isNullOrBlank()) existing.secondOfKinName else model.secondOfKinName
        existing.secondOfKinPhone =
            if (model.secondOfKinPhone.isNullOrBlank()) existing.secondOfKinPhone else model.secondOfKinPhone
        existing.secondOfKinEmail =
            if (model.secondOfKinEmail.isNullOrBlank()) existing.secondOfKinEmail else model.secondOfKinEmail


        existing = repository.save(existing)


        val typeRef = object : TypeReference<MemberResponseDto>() {}

        return objectMapper.convertValue(existing, typeRef)
    }

    fun exchangeSocialToken(model: ExchangeTokenRequestDto): AuthResponseDto? {
        val response = AuthResponseDto()

        if (model.authProvider == AuthProvider.GOOGLE) {
            val profileUrl =
                String.format("https://www.googleapis.com/oauth2/v1/tokeninfo?access_token=%s", model.token)

            val res = restTemplate.getForObject(profileUrl,GoogleTokenInfoResponseDto::class.java)

            if (res != null) {
                val authentication = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken(res.email, "password")
                )

                val userDetails = authentication.principal as UserDetails

                val member = MemberResponseDto()
                member.email = res.email


                response.member = member
                response.bearerToken = jwtUtils.generateJwtToken(authentication)
                response.authorities = userDetails.authorities.stream()
                    .map { item -> item.authority }
                    .collect(Collectors.toList())
            }


        }



        return response
    }

    fun login(model: LoginRequestDto): AuthResponseDto? {
        val authentication = authenticationManager.authenticate(
            UsernamePasswordAuthenticationToken(model.username, model.password)
        )

        val userDetails = authentication.principal as UserDetails



        val response = AuthResponseDto()
        response.member = MemberResponseDto()
        response.member
        response.bearerToken = jwtUtils.generateJwtToken(authentication)
        response.authorities = userDetails.authorities.stream()
            .map { item -> item.authority }
            .collect(Collectors.toList())

        return response
    }

    fun countByGender(startDate: LocalDate, endDate: LocalDate, gender: String): Long {
        return repository.countByGender(
            startDate,
            endDate,
            gender
        )
    }

    fun sumTransactionsByGenderAndTransactionTypes(
        startDate: LocalDate,
        endDate: LocalDate,
        gender: String,
        transactionTypes: Array<TransactionType>
    ): Long {
        return repository.sumBalanceByGender(
            startDate,
            endDate,
            gender,
            transactionTypes.map { transactionType -> transactionType.name }.toTypedArray()
        )
    }

}