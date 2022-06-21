package com.coc.cu.repositories

import com.coc.cu.domain.TransactionSumsDto
import com.coc.cu.domain.TransactionType
import com.coc.cu.entities.Member
import com.coc.cu.entities.Account
import com.coc.cu.entities.Transaction
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.util.*

@Repository
interface AccountTransactionsRepository: CrudRepository<Transaction, Long> {
    fun findByAccountId(accountId: String?): List<Transaction>

    @Query(
        value = "SELECT * FROM TRANSACTION WHERE account_id IN (SELECT id FROM ACCOUNT WHERE member_id=?1)",
        nativeQuery = true
    )
    fun findAllByMemberId(memberId: Long): List<Transaction>

    @Query(
        value = "SELECT ( (SELECT COALESCE(SUM(AMOUNT),0) FROM TRANSACTION WHERE account_id = ?1 AND TYPE IN ('SAVINGS','SAVINGS_CHEQUE')) - SELECT COALESCE(SUM(AMOUNT),0) FROM TRANSACTION WHERE account_id = ?1 AND TYPE IN ('WITHDRAWAL','WITHDRAWAL_CHEQUE') )",
        nativeQuery = true
    )
    fun findBySavingsBalance(id: String?): Double

    @Query(
        value = "SELECT ( (SELECT COALESCE(SUM(AMOUNT),0) FROM TRANSACTION WHERE account_id = ?1 AND TYPE IN ('LOAN','LOAN_CHEQUE')) - SELECT COALESCE(SUM(AMOUNT),0) FROM TRANSACTION WHERE account_id = ?1 AND TYPE IN ('LOAN_REPAYMENT','LOAN_REPAYMENT_CHEQUE') ) ",
        nativeQuery = true
    )
    fun findByLoanBalance(id: String?): Double
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
    fun getDashboardStatistics(startDate: LocalDate, endDate: LocalDate): List<TransactionSumsDto>

    @Query(
        value = "SELECT COALESCE(SUM(AMOUNT),0) FROM TRANSACTION WHERE TYPE IN (?1) AND CREATED_DATE BETWEEN ?2 AND ?3",
        nativeQuery = true
    )
    fun sumAmounts(transactionTypes: Array<String>, startDate: LocalDate, endDate: LocalDate): Double
}

