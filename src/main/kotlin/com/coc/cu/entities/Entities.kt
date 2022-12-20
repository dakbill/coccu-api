package com.coc.cu.entities


import com.coc.cu.domain.AccountType
import com.coc.cu.domain.TransactionType
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.time.LocalDateTime
import javax.persistence.*


@Entity
@JsonIgnoreProperties(ignoreUnknown = true)
data class Account(
    @ManyToOne var member: Member?,
    @Enumerated(value = EnumType.STRING) var type: AccountType?,
    @Id var id: String? = null,
    var createdDate: LocalDateTime? = null,
    var updatedDate: LocalDateTime? = null,
    @OneToMany var transactions: List<Transaction>? = arrayListOf(),
    @OneToMany var guarantors: List<Guarantor>? = arrayListOf()
)

@Entity
@JsonIgnoreProperties(ignoreUnknown = true)
data class Guarantor(
    @Id @GeneratedValue(strategy = GenerationType.AUTO) var id: Long? = null,
    @ManyToOne var member: Member?,
    var amount: Float? = null,
    var fundReleased: Boolean = false,
    var createdDate: LocalDateTime? = null,
    var updatedDate: LocalDateTime? = null
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
    var totalBalance: Double = 0.00,
    var transactionCount: Long = 0,
    var firstOfKinName: String? = null,
    var firstOfKinPhone: String? = null,
    var firstOfKinEmail: String? = null,
    var secondOfKinName: String? = null,
    var secondOfKinPhone: String? = null,
    var secondOfKinEmail: String? = null,
    var createdDate: LocalDateTime? = null,
    var updatedDate: LocalDateTime? = null,
    @OneToMany var accounts: List<Account>? = arrayListOf(),
    @Id @GeneratedValue(strategy = GenerationType.AUTO) var id: Long? = -1
)


@Entity
@EntityListeners(TransactionListener::class)
@JsonIgnoreProperties(ignoreUnknown = true)
data class Transaction(
    @Enumerated(value = EnumType.STRING) var type: TransactionType? = null,
    var amount: Float? = null,

    var comment: String? = null,

    var createdDate: LocalDateTime? = null,

    var updatedDate: LocalDateTime? = null,

    @ManyToOne var account: Account? = null,

    @Id @GeneratedValue(strategy = GenerationType.AUTO) var id: Long? = -1
)

