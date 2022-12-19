package com.coc.cu.repositories

import com.coc.cu.domain.TransactionSumsDto
import com.coc.cu.domain.TransactionType
import com.coc.cu.entities.Member
import com.coc.cu.entities.Account
import com.coc.cu.entities.Transaction
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.util.*

@Repository
interface AccountTransactionsRepository: JpaRepository<Transaction, Long> {
    fun findByAccountId(accountId: String?): List<Transaction>

    @Query(
        value = "SELECT * FROM TRANSACTION WHERE account_id IN (SELECT id FROM ACCOUNT WHERE member_id=?1) ORDER BY created_date DESC",
        nativeQuery = true
    )
    fun findAllByMemberId(memberId: Long, pageable: Pageable): List<Transaction>

    @Query(
        value = "SELECT ( " +
                "(SELECT COALESCE(SUM(CAST(COALESCE(AMOUNT,0) AS DECIMAL )),0) FROM TRANSACTION WHERE account_id = ?1 AND TYPE IN ('OPENING_BALANCE','SAVINGS','SAVINGS_CHEQUE')) " +
                " - " +
                "(SELECT COALESCE(SUM(CAST(COALESCE(AMOUNT,0) AS DECIMAL )),0) FROM TRANSACTION WHERE account_id = ?1 AND TYPE IN ('WITHDRAWAL','WITHDRAWAL_CHEQUE')) " +
                ")",
        nativeQuery = true
    )
    fun findBySavingsBalance(id: String?): Double

    @Query(
        value = "SELECT ( " +
                "(SELECT COALESCE(SUM(CAST(COALESCE(AMOUNT,0) AS DECIMAL )),0) FROM TRANSACTION WHERE account_id = ?1 AND TYPE IN ('OPENING_LOAN_BALANCE','LOAN','LOAN_CHEQUE')) " +
                " - " +
                "(SELECT COALESCE(SUM(CAST(COALESCE(AMOUNT,0) AS DECIMAL )),0) FROM TRANSACTION WHERE account_id = ?1 AND TYPE IN ('LOAN_REPAYMENT','LOAN_REPAYMENT_CHEQUE')) " +
                ") ",
        nativeQuery = true
    )
    fun findByLoanBalance(id: String?): Double

    @Query(
        value = "SELECT COALESCE(SUM(CAST(COALESCE(AMOUNT,0) AS DECIMAL )),0) FROM TRANSACTION WHERE account_id = (SELECT id FROM ACCOUNT WHERE member_id=?1 AND TYPE='SAVINGS') AND TYPE IN ('OPENING_BALANCE','SAVINGS','SAVINGS_CHEQUE')",
        nativeQuery = true
    )
    fun getTotalSavings(memberId: Long): Double

    @Query(
        value = "SELECT COALESCE(SUM(CAST(COALESCE(AMOUNT,0) AS DECIMAL )),0) FROM TRANSACTION WHERE account_id IN (SELECT id FROM ACCOUNT WHERE member_id=?1 AND TYPE='SAVINGS') AND TYPE IN ('WITHDRAWAL','WITHDRAWAL_CHEQUE')",
        nativeQuery = true
    )
    fun getTotalWithdrawals(memberId: Long): Double

    @Query(
        value = "SELECT COUNT(DISTINCT id) FROM TRANSACTION WHERE account_id IN (SELECT DISTINCT id FROM ACCOUNT WHERE member_id=?1)",
        nativeQuery = true
    )
    fun countByMemberId(id: Long?): Long

    @Query(
        value = "SELECT * FROM TRANSACTION WHERE account_id=?1 ORDER BY created_date DESC LIMIT 1",
        nativeQuery = true
    )
    fun lastByAccountId(accountId: String): Transaction?
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
                    "   (LOWER(MEMBER.name) LIKE '%' || ?1 || '%' ) " +
                    ")",
        nativeQuery = true
    )
    fun findByQuery(query: String?, pageRequest: Pageable): Page<Member>

    @Query(
        value = "UPDATE member SET transaction_count=(SELECT COUNT(id) FROM TRANSACTION WHERE account_id IN (SELECT id FROM ACCOUNT WHERE member_id=?1)) WHERE id=?1 RETURNING TRUE",
        nativeQuery = true
    )
    fun updateTransactionCount(id: Long?): Boolean

    @Query(
        value = "UPDATE member SET total_balance=(\n" +
                "\tSELECT\n" +
                "\t\tSUM(\n" +
                "\t\t\t(CASE \n" +
                "\t\t\t\t\tWHEN \"transaction\".\"type\" in ('WITHDRAWAL','WITHDRAWAL_CHEQUE') THEN -1 \n" +
                "\t\t\t\t\tWHEN \"transaction\".\"type\" in ('SAVINGS','SAVINGS_CHEQUE') THEN 1 \n" +
                "\t\t\t\t\tELSE 0 \n" +
                "\t\t\tEND) * amount\n" +
                "\t\t) AS total_balance \n" +
                "\tFROM \"transaction\" \n" +
                "\tWHERE account_id IN (SELECT id FROM ACCOUNT WHERE member_id=?1)\n" +
                ") WHERE id=?1 RETURNING TRUE\n" ,
        nativeQuery = true
    )
    fun updateTotalBalance(id: Long?): Boolean
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
        value = "SELECT COALESCE(SUM(CAST(COALESCE(AMOUNT,0) AS DECIMAL )),0) AS AMOUNT, TYPE FROM TRANSACTION WHERE CREATED_DATE BETWEEN ?1 AND ?2 GROUP BY TYPE",
        nativeQuery = true
    )
    fun getDashboardStatistics(startDate: LocalDate, endDate: LocalDate): List<TransactionSumsDto>

    @Query(
        value = "SELECT COALESCE(SUM(CAST(COALESCE(AMOUNT,0) AS DECIMAL )),0) FROM TRANSACTION WHERE TYPE IN (?1) AND CREATED_DATE BETWEEN ?2 AND ?3",
        nativeQuery = true
    )
    fun sumAmounts(transactionTypes: Array<String>, startDate: LocalDate, endDate: LocalDate): Double

    @Query(
        value = "SELECT COALESCE(SUM(CAST(COALESCE(AMOUNT,0) AS DECIMAL )),0) FROM TRANSACTION WHERE TYPE IN (?2) AND ACCOUNT_ID=?1 ",
        nativeQuery = true
    )
    fun sumAmounts(accountId: String, transactionTypes: Array<String>): Double

    @Query(
        value = "SELECT account.* FROM account WHERE account.type='LOAN' " +
                "AND (" +
                "   (SELECT COALESCE(SUM(CAST(COALESCE(AMOUNT,0) AS DECIMAL )),0) FROM TRANSACTION WHERE TYPE IN ('LOAN','LOAN_CHEQUE') AND ACCOUNT_ID=account.id) " +
                "   - " +
                "   (SELECT COALESCE(SUM(CAST(COALESCE(AMOUNT,0) AS DECIMAL )),0) FROM TRANSACTION WHERE TYPE IN ('LOAN_REPAYMENT','LOAN_REPAYMENT_CHEQUE') AND ACCOUNT_ID=account.id) " +
                ") > 0",
        nativeQuery = true
    )
    fun getDebtors(pageable: Pageable): List<Account>?


    @Query(
        value = "SELECT distinct \"account\".* FROM (\n" +
                "\tSELECT guarantors_id,account_guarantors.account_id,SUM(\n" +
                "\t\t(CASE \n" +
                "\t\t\tWHEN \"transaction\".\"type\" in ('LOAN_REPAYMENT','LOAN_REPAYMENT_CHEQUE') THEN -1 \n" +
                "\t\t\tWHEN \"transaction\".\"type\" in ('LOAN','LOAN_CHEQUE') THEN 1 \n" +
                "\t\t\tELSE 0 \n" +
                "\t\t END) * amount\n" +
                "\t) AS arrears \n" +
                "\tFROM \n" +
                "\t\taccount_guarantors \n" +
                "\t\tLEFT JOIN account ON(account.id=account_guarantors.account_id)\n" +
                "\t\tLEFT JOIN \"transaction\" ON(\"transaction\".account_id=account_guarantors.account_id) \n" +
                "\tGROUP BY account_guarantors.account_id, guarantors_id\n" +
                ") guarantor_arrears \n" +
                "LEFT JOIN account ON(account.id=guarantor_arrears.account_id)\n" +
                "LEFT JOIN \"member\" ON(\"member\".id=\"account\".member_id)\n" +
                "WHERE \n" +
                "arrears > 0 AND guarantors_id = ?1",
        nativeQuery = true
    )
    fun getGuarantorDebtorAccounts(memberId: Long): List<Account>?


}

