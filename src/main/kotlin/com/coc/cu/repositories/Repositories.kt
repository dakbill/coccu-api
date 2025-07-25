package com.coc.cu.repositories

import com.coc.cu.domain.MemberSummariesResponseDto
import com.coc.cu.domain.TransactionSumsDto
import com.coc.cu.entities.Account
import com.coc.cu.entities.Guarantor
import com.coc.cu.entities.Member
import com.coc.cu.entities.Transaction
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Repository
interface AccountTransactionsRepository: JpaRepository<Transaction, Long> {
    fun findByAccountId(accountId: String?): List<Transaction>

    @Query(
        value = "SELECT * FROM TRANSACTION WHERE " +
                "  ( (LENGTH(?1)=0) OR ( SELECT LOWER(name) LIKE CONCAT('%', ?1, '%') FROM member WHERE id=(SELECT member_id FROM ACCOUNT WHERE id=TRANSACTION.account_id LIMIT 1) )   )   " +
                " AND ( (?2=0) OR (account_id IN (SELECT id FROM ACCOUNT WHERE member_id=?2)) )   " +
                " AND  ( (LENGTH(?3)=0) OR (?3 = account_id ) )   " +
                " AND  ( ('EMPTY' IN (?4) ) OR ( type IN (?4) ) )   " +
                " AND  ( created_date BETWEEN CAST(?5 AS DATE) AND CAST(?6 AS DATE) )   ",
        countQuery = "SELECT COUNT(TRANSACTION.id) FROM TRANSACTION WHERE " +
                     " ( (?2=0) OR (account_id IN (SELECT id FROM ACCOUNT WHERE member_id=?2)) )   " +
                     " AND  ( (LENGTH(?3)=0) OR (?3 = account_id ) )   " +
                     " AND  ( created_date BETWEEN CAST(?5 AS DATE) AND CAST(?6 AS DATE) )   ",
        nativeQuery = true
    )
    fun findAllByMemberId(
        query: String,
        memberId: Long,
        accountId: String,
        transactionTypes: Array<String>,
        startDate: LocalDate,
        endDate: LocalDate,
        pageable: Pageable
    ): Page<Transaction>

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
        value = "SELECT ( " +
                    "(SELECT COALESCE(SUM(CAST(COALESCE(AMOUNT,0) AS DECIMAL )),0) FROM TRANSACTION WHERE account_id = ?1 AND TYPE IN ('STATIONERY','STATIONERY_CHEQUE','TRANSPORT','INCENTIVE_TO_PERSONNEL','INCENTIVE_TO_PERSONNEL_CHEQUE')) " +
                    " - " +
                    "(SELECT COALESCE(SUM(CAST(COALESCE(AMOUNT,0) AS DECIMAL )),0) FROM TRANSACTION WHERE account_id = ?1 AND TYPE IN ('WITHDRAWAL')) " +
                ") ",
        nativeQuery = true
    )
    fun findByAdminExpensesBalance(id: String?): Double

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
        value = """
        SELECT DISTINCT member.* 
        FROM member 
        LEFT JOIN account ON account.member_id = member.id 
        WHERE LENGTH(member.name) > 0 
          AND (
              (LOWER(?1) <> UPPER(?1) AND CAST(account.member_id AS CHAR) LIKE '%' || ?1 || '%') OR 
              (CAST(member.id AS CHAR) LIKE '%' || ?1 || '%') OR 
              (LOWER(member.name) LIKE '%' || ?1 || '%')
          )
    """,
        countQuery = """
        SELECT COUNT(DISTINCT member.id) 
        FROM member 
        LEFT JOIN account ON account.member_id = member.id 
        WHERE LENGTH(member.name) > 0 
          AND (
              (LOWER(?1) <> UPPER(?1) AND CAST(account.member_id AS CHAR) LIKE '%' || ?1 || '%') OR 
              (CAST(member.id AS CHAR) LIKE '%' || ?1 || '%') OR 
              (LOWER(member.name) LIKE '%' || ?1 || '%')
          )
    """,
        nativeQuery = true
    )
    fun findByQuery(query: String?, pageRequest: Pageable): Page<Member>


    @Query(
        value = "" +
                "SELECT " +
                "DISTINCT member.* ," +
                "(SUM( CASE WHEN transaction.type IN ('SAVINGS','SAVINGS_CHEQUE','OPENING_BALANCE') THEN transaction.amount ELSE 0 END)) AS savings," +
                "(SUM( CASE WHEN transaction.type IN ('WITHDRAWAL','WITHDRAWAL_CHEQUE') THEN transaction.amount ELSE 0 END)) AS withdrawals " +
                "FROM member " +
                "LEFT JOIN account ON(account.member_id=member.id) " +
                "LEFT JOIN transaction ON(transaction.account_id=account.id AND transaction.created_date BETWEEN CAST(?2 AS DATE) AND CAST(?3 AS DATE) ) " +
                "WHERE " +
                "( LENGTH(MEMBER.name) > 0 ) AND"+
                "( " +
                "   (( LOWER(?1) <> UPPER(?1) ) AND CAST(account.member_id AS CHAR) LIKE '%' || ?1 || '%') OR " +
                "   (CAST(MEMBER.id AS CHAR) LIKE '%' || ?1 || '%' ) OR " +
                "   (LOWER(MEMBER.name) LIKE '%' || ?1 || '%' ) " +
                ") GROUP BY member.id",

        countQuery = "" +
                "SELECT " +
                "COUNT(DISTINCT member.id) " +
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
    fun findSummariesByQuery(
        query: String?,
        startDate: LocalDate?,
        endDate: LocalDate?,
        pageRequest: Pageable
    ): Page<MemberSummariesResponseDto>

    @Modifying
    @Transactional
    @Query(
        value = "UPDATE member SET transaction_count=(SELECT COUNT(id) FROM TRANSACTION WHERE account_id IN (SELECT id FROM ACCOUNT WHERE member_id=member.id)) WHERE id=?1",
        nativeQuery = true
    )
    fun updateTransactionCount(id: Long): Int

    @Modifying
    @Transactional
    @Query(
        value = """
            UPDATE member 
            SET total_balance = (
                SELECT SUM(
                    CASE 
                        WHEN transaction.type IN ('WITHDRAWAL', 'WITHDRAWAL_CHEQUE') THEN -1 
                        WHEN transaction.type IN ('OPENING_BALANCE', 'SAVINGS', 'SAVINGS_CHEQUE') THEN 1 
                        ELSE 0 
                    END * amount
                ) 
                FROM transaction 
                WHERE account_id IN (SELECT id FROM account WHERE member_id = member.id)
            ) 
            WHERE (id = ?1 OR ?1 = 0) 
        """,
        nativeQuery = true
    )
    fun updateTotalBalance(id: Long): Int


    @Query(
        value = "SELECT COUNT(id) FROM member WHERE created_date BETWEEN ?1 AND ?2 AND gender=?3",
        nativeQuery = true
    )
    fun countByGender(startDate: LocalDate, endDate: LocalDate,gender: String): Long

    @Query(
        value = "SELECT COALESCE(SUM(amount),0) FROM transaction WHERE created_date BETWEEN ?1 AND ?2 AND type IN (?4) AND account_id IN (SELECT account.id FROM account WHERE member_id IN (SELECT id FROM member WHERE gender=?3) )",
        nativeQuery = true
    )
    fun sumBalanceByGender(startDate: LocalDate, endDate: LocalDate, gender: String, transactionTypes: Array<String>): Long

    @Query(
        value = "SELECT 1",
//        value = "SELECT setval('member_id_seq',(SELECT max(id) FROM member))",
        nativeQuery = true
    )
    fun resetMemberIdSequence(): Long?
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
        value = "SELECT " +
                "   account.* FROM account LEFT JOIN member ON(member.id=member_id) " +
                "WHERE " +
                "   account.type='LOAN' AND account.balance > 0 AND (LOWER(MEMBER.name) LIKE '%' || ?1 || '%' )",
        countQuery = "SELECT " +
                "   COUNT(account.id) FROM account LEFT JOIN member ON(member.id=member_id) " +
                "WHERE " +
                "   account.type='LOAN' AND account.balance > 0 AND (LOWER(MEMBER.name) LIKE '%' || ?1 || '%' )",
        nativeQuery = true
    )
    fun getDebtors(query: String, pageable: Pageable): Page<Account>


    @Query(
        value = "SELECT " +
                "   account.* FROM account LEFT JOIN member ON(member.id=member_id) " +
                "WHERE " +
                "   account.type='LOAN' AND account.balance > 0 AND (LOWER(MEMBER.name) LIKE '%' || ?1 || '%' )",
        countQuery = "SELECT " +
                "   COUNT(account.id) FROM account LEFT JOIN member ON(member.id=member_id) " +
                "WHERE " +
                "    account.type='LOAN' AND account.balance > 0 AND (LOWER(MEMBER.name) LIKE '%' || ?1 || '%' )",
        nativeQuery = true
    )
    fun getOutstandingDebtors(query: String, pageable: Pageable): Page<Account>

    @Query(
        value = "SELECT " +
                "   COUNT(account.id) FROM account RIGHT JOIN member ON(member.id=member_id AND member.gender = ?3) " +
                "WHERE " +
                "    account.type='LOAN' AND account.balance > 0 AND account.created_date BETWEEN ?1 AND ?2",
        nativeQuery = true
    )
    fun countOutstandingLoansByGender(startDate: LocalDate, endDate: LocalDate, gender: String): Int


    @Query(
        value = "SELECT " +
                "   COALESCE(SUM(account.balance),0) FROM account RIGHT JOIN member ON(member.id=member_id AND member.gender = ?3) " +
                "WHERE " +
                "    account.type='LOAN' AND account.balance > 0 AND account.created_date BETWEEN ?1 AND ?2",
        nativeQuery = true
    )
    fun sumOutstandingLoansByGender(startDate: LocalDate, endDate: LocalDate, gender: String): Float




//    (loan repayments) > ((principal/12) * months to date)
    @Query(
        value = "SELECT " +
                "   account.* FROM account LEFT JOIN member ON(member.id=member_id) " +
                "WHERE " +
                "   account.type='LOAN' AND account.balance > 0 " +
                "   AND (" +
                "           (SELECT COALESCE(SUM(amount),0) FROM transaction WHERE account_id=account.id AND \"type\" IN ('LOAN_REPAYMENT','LOAN_REPAYMENT_CHEQUE')) " +
                "           <  " +
                "           ((SELECT COALESCE(SUM(amount)/12.0,0) FROM transaction WHERE account_id=account.id AND \"type\" IN ('LOAN','LOAN_CHEQUE')) * LEAST(EXTRACT(year FROM age(NOW(),account.created_date))*12 + EXTRACT(month FROM age(NOW(),account.created_date)),12) ) " +
                "   ) " +
                "   AND (LOWER(MEMBER.name) LIKE '%' || ?1 || '%' )",
        countQuery = "SELECT " +
                "   COUNT(account.id) FROM account LEFT JOIN member ON(member.id=member_id) " +
                "WHERE " +
                "   account.type='LOAN' AND account.balance > 0 " +
                "   AND (" +
                "           (SELECT COALESCE(SUM(amount),0) FROM transaction WHERE account_id=account.id AND \"type\" IN ('LOAN_REPAYMENT','LOAN_REPAYMENT_CHEQUE')) " +
                "           <  " +
                "           ((SELECT COALESCE(SUM(amount)/12.0,0) FROM transaction WHERE account_id=account.id AND \"type\" IN ('LOAN','LOAN_CHEQUE')) * LEAST(EXTRACT(year FROM age(NOW(),account.created_date))*12 + EXTRACT(month FROM age(NOW(),account.created_date)),12) ) " +
                "   ) " +
                "   AND (LOWER(MEMBER.name) LIKE '%' || ?1 || '%' )",
        nativeQuery = true
    )
    fun getDefaultingDebtors(query: String, pageable: Pageable): Page<Account>


    @Query(
        value = "SELECT account.* FROM account LEFT JOIN member ON(member.id=member_id) WHERE account.balance < 0 " +
                "AND (LOWER(MEMBER.name) LIKE '%' || ?1 || '%' )",
        countQuery = "SELECT COUNT(account.id) FROM account LEFT JOIN member ON(member.id=member_id) WHERE account.balance < 0 " +
                "AND (LOWER(MEMBER.name) LIKE '%' || ?1 || '%' )",
        nativeQuery = true
    )
    fun getNegativeBalanceAccounts(query: String, pageable: Pageable): Page<Account>


    @Query(
        value = "SELECT \n" +
                "\tDISTINCT account_guarantors.* \n" +
                "FROM \n" +
                "\taccount_guarantors INNER JOIN guarantor ON(guarantor.id=account_guarantors.guarantors_id)\n" +
                "\tRIGHT JOIN account ON(account.id=account_guarantors.account_id)\n" +
                "\tLEFT JOIN (\n" +
                "\t\tSELECT transaction.account_id,SUM(\n" +
                "\t\t\t(CASE \n" +
                "\t\t\t\tWHEN \"transaction\".\"type\" in ('LOAN_REPAYMENT','LOAN_REPAYMENT_CHEQUE') THEN -1 \n" +
                "\t\t\t\tWHEN \"transaction\".\"type\" in ('LOAN','LOAN_CHEQUE') THEN 1 \n" +
                "\t\t\t\tELSE 0 \n" +
                "\t\t\t END) * \"transaction\".amount\n" +
                "\t\t) AS arrears \n" +
                "\t\tFROM \n" +
                "\t\t\t\"transaction\"\n" +
                "\t\tGROUP BY transaction.account_id\n" +
                "\t) guarantor_arrears ON(guarantor_arrears.account_id=account.id)\n" +
                "WHERE\n" +
                "\t\n" +
                "\tguarantor.member_id=?1 \n" +
                "\tAND guarantor_arrears.arrears > 0\n" +
                "\tAND guarantor.fund_released = FALSE",
        nativeQuery = true
    )
    fun getGuarantorDebtorAccounts(memberId: Long): List<Map<String,Any>>

    @Query(
        value = "SELECT COALESCE(COALESCE(total_balance,0)-(SELECT COALESCE(SUM(guarantor.amount),0) FROM guarantor RIGHT JOIN member ON(member.id=guarantor.member_id ) WHERE guarantor.member_id=?1 AND NOT guarantor.fund_released),0) FROM member WHERE id=?1",
        nativeQuery = true
    )
    fun getAvailableBalance(memberId: Long?): Double


}


@Repository
interface GuarantorRepository : CrudRepository<Guarantor, Long> {}





