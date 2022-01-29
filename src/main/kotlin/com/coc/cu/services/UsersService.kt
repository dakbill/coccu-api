package com.coc.cu.services

import com.coc.cu.domain.AccountResponseDto
import com.coc.cu.domain.UserResponseDto
import com.coc.cu.entities.Member
import com.coc.cu.repositories.MemberAccountRepository
import com.coc.cu.repositories.MembersRepository
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Service


@Service
class UsersService(var repository: MembersRepository, var memberAccountRepository: MemberAccountRepository) {

    fun single(id: Long): UserResponseDto? {
        val objectMapper = ObjectMapper()
        val typeRef = object : TypeReference<UserResponseDto>() {}

        var res = repository.findById(id)
        if (res.isPresent) {
            var userEntity = res.get()

            return objectMapper.convertValue(userEntity, typeRef)
        }


        return null
    }

    fun list(query: String): List<UserResponseDto>? {
        val objectMapper = ObjectMapper()
        val typeRef = object : TypeReference<List<UserResponseDto>>() {}
        val accountsTypeRef = object : TypeReference<List<AccountResponseDto>>() {}

        val members = repository.findByQuery(query.lowercase())


        val users = objectMapper.convertValue(members, typeRef)
        for (user in users) {
            user.accounts = objectMapper.convertValue(memberAccountRepository.findByUserId(user.id), accountsTypeRef)
        }

        return users
    }


}