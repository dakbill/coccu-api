package com.coc.cu.entities


import com.coc.cu.domain.AccountType
import com.coc.cu.domain.TransactionType
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer
import java.time.LocalDate
import java.util.*
import javax.persistence.*

@Entity
class Member(
    var name: String? = null,
    @OneToMany var accounts: List<MemberAccount>? = arrayListOf(),
    @Id @GeneratedValue(strategy = GenerationType.AUTO) var id: Long? = -1
)


@Entity
data class MemberAccount(
    @ManyToOne var member: Member,
    @Enumerated(value = EnumType.STRING) var type: AccountType?,
    @Id var id: String? = null,
    @OneToMany var transactions: List<Transaction>? = arrayListOf()
)

@Entity
data class Transaction(
    @Enumerated(value = EnumType.STRING) var type: TransactionType? = null,
    var amount: Float? = null,

    var createdDate: LocalDate? = null,


    var updatedDate: LocalDate? = null,

    @JsonIgnore @ManyToOne var account: MemberAccount? = null,
    @Id @GeneratedValue(strategy = GenerationType.AUTO) var id: Long? = -1
)

