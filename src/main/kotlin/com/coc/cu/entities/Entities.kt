package com.coc.cu.entities


import com.coc.cu.domain.AccountType
import com.coc.cu.domain.TransactionType
import com.fasterxml.jackson.annotation.JsonBackReference
import com.fasterxml.jackson.annotation.JsonFilter
import com.fasterxml.jackson.annotation.JsonManagedReference
import java.time.LocalDate
import javax.persistence.*


@Entity
data class Account(
    @ManyToOne var member: Member?,
    @Enumerated(value = EnumType.STRING) var type: AccountType?,
    @Id var id: String? = null,
    @OneToMany var transactions: List<Transaction>? = arrayListOf()
)

@Entity
class Member(
    var name: String? = null,
    @OneToMany var accounts: List<Account>? = arrayListOf(),
    @Id @GeneratedValue(strategy = GenerationType.AUTO) var id: Long? = -1
)


@Entity
data class Transaction(
    @Enumerated(value = EnumType.STRING) var type: TransactionType? = null,
    var amount: Float? = null,

    var createdDate: LocalDate? = null,

    var updatedDate: LocalDate? = null,

    @ManyToOne var account: Account? = null,

    @Id @GeneratedValue(strategy = GenerationType.AUTO) var id: Long? = -1
)

