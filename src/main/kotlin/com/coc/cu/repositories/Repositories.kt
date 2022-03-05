package com.coc.cu.repositories

import com.coc.cu.domain.AccountResponseDto
import com.coc.cu.entities.Member
import com.coc.cu.entities.MemberAccount
import com.coc.cu.entities.Transaction
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface AccountTransactionsRepository: CrudRepository<Transaction, Long> {
    fun findByAccountId(accountId: String?): List<Transaction>
}

@Repository
interface MembersRepository : CrudRepository<Member, Long> {

    @Query(
        value = "" +
                "SELECT " +
                    "DISTINCT member.* " +
                "FROM " +
                    "member LEFT JOIN MEMBER_ACCOUNT ON(MEMBER_ACCOUNT.member_id=member.id) " +
                "WHERE " +
                    "(( LOWER(?1) <> UPPER(?1) ) AND MEMBER_ACCOUNT.member_id LIKE '%' || ?1 || '%') OR " +
                    "(CAST(member.id AS CHAR) LIKE '%' || ?1 || '%' ) OR " +
                    "(LOWER(member.name) LIKE '%' || ?1 || '%' )" ,
        nativeQuery = true
    )
    fun findByQuery(query: String?): List<Member>
}


@Repository
interface MemberAccountRepository : CrudRepository<MemberAccount, String> {
    fun findByMemberId(id: Long?): List<MemberAccount>?
}