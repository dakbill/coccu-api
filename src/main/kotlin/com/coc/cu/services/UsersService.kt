package com.coc.cu.services

import com.coc.cu.domain.AccountResponseDto
import com.coc.cu.domain.MemberResponseDto
import com.coc.cu.entities.Account
import com.coc.cu.repositories.MemberAccountRepository
import com.coc.cu.repositories.MembersRepository
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Service


@Service
class UsersService(var repository: MembersRepository, var memberAccountRepository: MemberAccountRepository) {

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


}