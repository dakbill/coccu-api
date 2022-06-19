package com.coc.cu.services

import com.coc.cu.domain.AccountResponseDto
import com.coc.cu.domain.AccountType
import com.coc.cu.domain.MemberResponseDto
import com.coc.cu.domain.UserRequestDto
import com.coc.cu.entities.Account
import com.coc.cu.entities.Member
import com.coc.cu.repositories.MemberAccountRepository
import com.coc.cu.repositories.MembersRepository
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.util.*


@Service
class UsersService(var repository: MembersRepository, var memberAccountRepository: MemberAccountRepository, val objectMapper: ObjectMapper) {

    fun single(id: Long): MemberResponseDto? {
        val objectMapper = ObjectMapper()
        val typeRef = object : TypeReference<MemberResponseDto>() {}

        var res = repository.findById(id)
        if (res.isPresent) {
            var userEntity = res.get()
            val accounts = memberAccountRepository.findByMemberId(userEntity.id)

            for (account in accounts!!) {
                account.member = null
            }

            userEntity.accounts = accounts

            return objectMapper.convertValue(userEntity, typeRef)
        }

        return null
    }

    fun list(query: String): List<MemberResponseDto>? {
        val objectMapper = ObjectMapper()
        val typeRef = object : TypeReference<List<MemberResponseDto>>() {}
        val accountsTypeRef = object : TypeReference<List<AccountResponseDto>>() {}

        val members = repository.findByQuery(query.lowercase())


        val users = objectMapper.convertValue(members, typeRef)
        for (user in users) {
            user.accounts = objectMapper.convertValue(memberAccountRepository.findByMemberId(user.id), accountsTypeRef)
        }

        return users
    }

    fun create(model: UserRequestDto): MemberResponseDto? {
        val memberTypeRef = object : TypeReference<Member>() {}
        var member = objectMapper.convertValue(model,memberTypeRef)
        member = repository.save(member)

        var savingsAccount =
            Account(member, AccountType.SAVINGS, String.format("%s-%s", AccountType.SAVINGS, member.id))
        memberAccountRepository.save(savingsAccount)



        val typeRef = object : TypeReference<MemberResponseDto>() {}

        return objectMapper.convertValue(repository.findById(member.id!!).get(), typeRef)
    }

    fun update(id: Long, model: UserRequestDto): MemberResponseDto? {
        var existing = repository.findById(id).get()

        
        existing.name = if(model.name.isNullOrBlank()) existing.name else model.name
        existing.image = if(model.image.isNullOrBlank()) existing.image else model.image
        existing.phone = if(model.phone.isNullOrBlank()) existing.phone else model.phone
        existing.email = if(model.email.isNullOrBlank()) existing.email else model.email
        existing.address = if(model.address.isNullOrBlank()) existing.address else model.address
        existing.city = if(model.city.isNullOrBlank()) existing.city else model.city
        existing.firstOfKinName = if(model.firstOfKinName.isNullOrBlank()) existing.firstOfKinName else model.firstOfKinName
        existing.firstOfKinPhone = if(model.firstOfKinPhone.isNullOrBlank()) existing.firstOfKinPhone else model.firstOfKinPhone
        existing.firstOfKinEmail = if(model.firstOfKinEmail.isNullOrBlank()) existing.firstOfKinEmail else model.firstOfKinEmail
        existing.secondOfKinName = if(model.secondOfKinName.isNullOrBlank()) existing.secondOfKinName else model.secondOfKinName
        existing.secondOfKinPhone = if(model.secondOfKinPhone.isNullOrBlank()) existing.secondOfKinPhone else model.secondOfKinPhone
        existing.secondOfKinEmail = if(model.secondOfKinEmail.isNullOrBlank()) existing.secondOfKinEmail else model.secondOfKinEmail


        existing = repository.save(existing)


        val typeRef = object : TypeReference<MemberResponseDto>() {}

        return objectMapper.convertValue(existing, typeRef)
    }


}