package com.coc.cu.entities


import com.coc.cu.domain.AccountType
import com.coc.cu.domain.TransactionType
import com.fasterxml.jackson.annotation.JsonIgnore
import javax.persistence.*

@Entity
class Member(
    var name: String? = null,
    @OneToMany var accounts: List<MemberAccount>? = arrayListOf(),
    @Id @GeneratedValue(strategy = GenerationType.AUTO) var id: Long? = -1
)


@Entity
data class MemberAccount(
    @ManyToOne var user: Member,
    @Enumerated(value = EnumType.STRING) var type: AccountType?,
    @Id var id: String? = null,
    @OneToMany var transactions: List<Transaction>? = arrayListOf()
)

@Entity
data class Transaction(
    @Enumerated(value = EnumType.STRING) var type: TransactionType? = null,
    var amount: Float? = null,
    @JsonIgnore @ManyToOne var account: MemberAccount? = null,
    @Id @GeneratedValue(strategy = GenerationType.AUTO) var id: Long? = -1
)