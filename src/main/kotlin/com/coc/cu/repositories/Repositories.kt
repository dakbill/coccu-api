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
        value = "SELECT * FROM TRANSACTION WHERE account_id IN (SELECT id FROM ACCOUNT WHERE member_id=?1) ORDER BY created_date DESC",
        nativeQuery = true
    )
    fun findAllByMemberId(memberId: Long): List<Transaction>

    @Query(
        value = "SELECT ( " +
                "(SELECT COALESCE(SUM(AMOUNT),0) FROM TRANSACTION WHERE account_id = ?1 AND TYPE IN ('OPENING_BALANCE','SAVINGS','SAVINGS_CHEQUE')) " +
                " - " +
                "(SELECT COALESCE(SUM(AMOUNT),0) FROM TRANSACTION WHERE account_id = ?1 AND TYPE IN ('WITHDRAWAL','WITHDRAWAL_CHEQUE')) " +
                ")",
        nativeQuery = true
    )
    fun findBySavingsBalance(id: String?): Double

    @Query(
        value = "SELECT ( " +
                "(SELECT COALESCE(SUM(AMOUNT),0) FROM TRANSACTION WHERE account_id = ?1 AND TYPE IN ('OPENING_LOAN_BALANCE','LOAN','LOAN_CHEQUE')) " +
                " - " +
                "(SELECT COALESCE(SUM(AMOUNT),0) FROM TRANSACTION WHERE account_id = ?1 AND TYPE IN ('LOAN_REPAYMENT','LOAN_REPAYMENT_CHEQUE')) " +
                ") ",
        nativeQuery = true
    )
    fun findByLoanBalance(id: String?): Double

    @Query(
        value = "SELECT COALESCE(SUM(AMOUNT),0) FROM TRANSACTION WHERE account_id = (SELECT id FROM ACCOUNT WHERE member_id=?1 AND TYPE='SAVINGS') AND TYPE IN ('OPENING_BALANCE','SAVINGS','SAVINGS_CHEQUE')",
        nativeQuery = true
    )
    fun getTotalSavings(memberId: Long): Double

    @Query(
        value = "SELECT COALESCE(SUM(AMOUNT),0) FROM TRANSACTION WHERE account_id IN (SELECT id FROM ACCOUNT WHERE member_id=?1 AND TYPE='SAVINGS') AND TYPE IN ('WITHDRAWAL','WITHDRAWAL_CHEQUE')",
        nativeQuery = true
    )
    fun getTotalWithdrawals(memberId: Long): Double
}

@Repository
interface MembersRepository : CrudRepository<Member, Long> {

    @Query(
        value = "" +
                "SELECT " +
                    "DISTINCT member.* " +
                "FROM " +
                    "member LEFT JOIN account ON(account.member_id=member.id) " +
                "WHERE " +
                    "( LENGTH(MEMBER.name) > 0 ) AND"+
                    "( " +
                    "   (( LOWER(?1) <> UPPER(?1) ) AND CAST(account.member_id AS CHAR) LIKE '%' || ?1 || '%') OR " +
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
        value = "SELECT COUNT(account.id) FROM account WHERE type IN (?2) AND member_id=?1",
        nativeQuery = true
    )
    fun countByMemberIdAndType(id: Long?, transactionTypes: Array<String>): Long


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

    @Query(
        value = "SELECT COALESCE(SUM(AMOUNT),0) FROM TRANSACTION WHERE TYPE IN (?2) AND ACCOUNT_ID=?1 ",
        nativeQuery = true
    )
    fun sumAmounts(accountId: String, transactionTypes: Array<String>): Double

    @Query(
        value = "SELECT account.* FROM account WHERE account.type='LOAN' " +
                "AND (" +
                "   (SELECT COALESCE(SUM(AMOUNT),0) FROM TRANSACTION WHERE TYPE IN ('LOAN','LOAN_CHEQUE') AND ACCOUNT_ID=account.id) " +
                "   - " +
                "   (SELECT COALESCE(SUM(AMOUNT),0) FROM TRANSACTION WHERE TYPE IN ('LOAN_REPAYMENT','LOAN_REPAYMENT_CHEQUE') AND ACCOUNT_ID=account.id) " +
                ") > 0",
        nativeQuery = true
    )
    fun getDebtors(): List<Account>?
}

