package com.coc.cu.repositories

import com.coc.cu.domain.TransactionSumsDto
import com.coc.cu.entities.Member
import com.coc.cu.entities.Account
import com.coc.cu.entities.Transaction
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface AccountTransactionsRepository: CrudRepository<Transaction, Long> {
    fun findByAccountId(accountId: String?): List<Transaction>

    @Query(
        value = "SELECT * FROM TRANSACTION WHERE account_id IN (SELECT id FROM ACCOUNT WHERE member_id=?1)",
        nativeQuery = true
    )
    fun findAllByMemberId(memberId: Long): List<Transaction>
}

@Repository
interface MembersRepository : CrudRepository<Member, Long> {

    @Query(
        value = "" +
                "SELECT " +
                    "DISTINCT MEMBER.* " +
                "FROM " +
                    "MEMBER LEFT JOIN ACCOUNT ON(ACCOUNT.member_id=MEMBER.id) " +
                "WHERE " +
                    "( LENGTH(MEMBER.name) > 0 ) AND"+
                    "( " +
                    "   (( LOWER(?1) <> UPPER(?1) ) AND ACCOUNT.member_id LIKE '%' || ?1 || '%') OR " +
                    "   (CAST(MEMBER.id AS CHAR) LIKE '%' || ?1 || '%' ) OR " +
                    "   (LOWER(MEMBER.name) LIKE '%' || ?1 || '%' )" +
                    " )",
        nativeQuery = true
    )
    fun findByQuery(query: String?): List<Member>
}


@Repository
interface MemberAccountRepository : CrudRepository<Account, String> {
    fun findByMemberId(id: Long?): List<Account>?


    @Query(
        value = "SELECT SUM(AMOUNT) AS AMOUNT, TYPE FROM TRANSACTION WHERE CREATED_DATE BETWEEN ?1 AND ?2 GROUP BY TYPE",
        nativeQuery = true
    )
    fun getDashboardStatistics(startDate: Date, endDate: Date): List<TransactionSumsDto>
}

