package com.coc.cu.entities


import com.coc.cu.domain.AccountType
import com.coc.cu.domain.TransactionType
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.time.LocalDate
import javax.persistence.*


@Entity
@JsonIgnoreProperties(ignoreUnknown = true)
data class Account(
    @ManyToOne var member: Member?,
    @Enumerated(value = EnumType.STRING) var type: AccountType?,
    @Id var id: String? = null,
    @OneToMany var transactions: List<Transaction>? = arrayListOf()
)

@Entity
@JsonIgnoreProperties(ignoreUnknown = true)
class Member(
    var name: String? = null,
    var image: String? = null,
    var phone: String? = null,
    var email: String? = null,
    var address: String? = null,
    var city: String? = null,
    var firstOfKinName: String? = null,
    var firstOfKinPhone: String? = null,
    var firstOfKinEmail: String? = null,
    var secondOfKinName: String? = null,
    var secondOfKinPhone: String? = null,
    var secondOfKinEmail: String? = null,
    @OneToMany var accounts: List<Account>? = arrayListOf(),
    @Id @GeneratedValue(strategy = GenerationType.AUTO) var id: Long? = -1
)


@Entity
@JsonIgnoreProperties(ignoreUnknown = true)
data class Transaction(
    @Enumerated(value = EnumType.STRING) var type: TransactionType? = null,
    var amount: Float? = null,

    var createdDate: LocalDate? = null,

    var updatedDate: LocalDate? = null,

    @ManyToOne var account: Account? = null,

    @Id @GeneratedValue(strategy = GenerationType.AUTO) var id: Long? = -1
)

