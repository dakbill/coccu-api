package com.coc.cu.services

import com.coc.cu.domain.*
import com.coc.cu.entities.Account
import com.coc.cu.entities.Member
import com.coc.cu.repositories.AccountTransactionsRepository
import com.coc.cu.repositories.MemberAccountRepository
import com.coc.cu.repositories.MembersRepository
import com.coc.cu.utils.GoogleSheetUtils
import com.coc.cu.utils.JwtUtils
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.auth.oauth2.GoogleCredentials
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import org.apache.logging.log4j.util.Strings
import org.springframework.core.io.ClassPathResource
import org.springframework.core.io.ResourceLoader
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import java.io.BufferedReader
import java.io.InputStreamReader
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.stream.Collectors
import javax.persistence.EntityManager
import javax.persistence.EntityManagerFactory


@Service
class UsersService(
    val repository: MembersRepository,
    val memberAccountRepository: MemberAccountRepository,
    val transactionsRepository: AccountTransactionsRepository,
    val objectMapper: ObjectMapper,
    val restTemplate: RestTemplate,
    val storageService: StorageService,
    val authenticationManager: AuthenticationManager,
    val jwtUtils: JwtUtils,
    var emf: EntityManagerFactory,
    val resourceLoader: ResourceLoader
) {

    fun single(id: Long): MemberResponseDto? {
        val typeRef = object : TypeReference<MemberResponseDto>() {}

        val res = repository.findById(id)
        if (res.isPresent) {
            val userEntity = res.get()

            val accountTypeRef = object : TypeReference<List<AccountResponseDto>>() {}
            val accounts =
                objectMapper.convertValue(memberAccountRepository.findByMemberId(userEntity.id), accountTypeRef)

            for (account in accounts!!) {
                if (account.type == AccountType.SAVINGS) {
                    account.balance = transactionsRepository.findBySavingsBalance(account.id)
                } else {
                    account.balance = transactionsRepository.findByLoanBalance(account.id)
                }

            }

            val member = objectMapper.convertValue(userEntity, typeRef)
            member.accounts = accounts

            return member
        }

        return null
    }

    fun list(query: String, pageRequest: PageRequest): Page<MemberResponseDto> {
        val typeRef = object : TypeReference<List<MemberResponseDto>>() {}
        val accountsTypeRef = object : TypeReference<List<AccountResponseDto>>() {}

        val members = repository.findByQuery(query.lowercase(), pageRequest)
        val users = objectMapper.convertValue(members.content, typeRef)

        users.stream().forEach { user ->
            run {
                val accounts = memberAccountRepository.findByMemberId(user.id)
                user.accounts = objectMapper.convertValue(accounts, accountsTypeRef)
            }
        }

        return PageImpl(users, pageRequest, members.totalElements)
    }

    fun listSummaries(
        query: String,
        startDate: LocalDate?,
        endDate: LocalDate?,
        pageRequest: PageRequest
    ): Page<MemberSummariesResponseDto> {
        val accountsTypeRef = object : TypeReference<List<AccountResponseDto>>() {}

        val members = repository.findSummariesByQuery(
            query.lowercase(),
            startDate ?: LocalDate.now(),
            endDate ?: LocalDate.now(),
            pageRequest
        )


        return PageImpl(members.content, pageRequest, members.totalElements)
    }

    fun create(model: UserRequestDto): MemberResponseDto? {
        val memberTypeRef = object : TypeReference<Member>() {}
        var member = objectMapper.convertValue(model, memberTypeRef)
        if(model.memberId != null){
            member.id = model.memberId
        }
        member = repository.save(member)

        val savingsAccount =
            Account(member, AccountType.SAVINGS, String.format("%s-%s", AccountType.SAVINGS, member.id))
        memberAccountRepository.save(savingsAccount)


        val typeRef = object : TypeReference<MemberResponseDto>() {}

        return objectMapper.convertValue(repository.findById(member.id!!).get(), typeRef)
    }

    fun update(id: Long, model: UserRequestDto): MemberResponseDto? {
        var existing = repository.findById(id).get()

        if (model.imageFile != null) {
            model.image = storageService.uploadMultipartFile(model.imageFile!!);
        }

        existing.name = if (model.name.isNullOrBlank()) existing.name else model.name
        existing.image = if (model.image.isNullOrBlank()) existing.image else model.image
        existing.phone = if (model.phone.isNullOrBlank()) existing.phone else model.phone
        existing.email = if (model.email.isNullOrBlank()) existing.email else model.email
        existing.address = if (model.address.isNullOrBlank()) existing.address else model.address
        existing.city = if (model.city.isNullOrBlank()) existing.city else model.city
        existing.firstOfKinName =
            if (model.firstOfKinName.isNullOrBlank()) existing.firstOfKinName else model.firstOfKinName
        existing.firstOfKinPhone =
            if (model.firstOfKinPhone.isNullOrBlank()) existing.firstOfKinPhone else model.firstOfKinPhone
        existing.firstOfKinEmail =
            if (model.firstOfKinEmail.isNullOrBlank()) existing.firstOfKinEmail else model.firstOfKinEmail
        existing.secondOfKinName =
            if (model.secondOfKinName.isNullOrBlank()) existing.secondOfKinName else model.secondOfKinName
        existing.secondOfKinPhone =
            if (model.secondOfKinPhone.isNullOrBlank()) existing.secondOfKinPhone else model.secondOfKinPhone
        existing.secondOfKinEmail =
            if (model.secondOfKinEmail.isNullOrBlank()) existing.secondOfKinEmail else model.secondOfKinEmail


        existing = repository.save(existing)


        val typeRef = object : TypeReference<MemberResponseDto>() {}

        return objectMapper.convertValue(existing, typeRef)
    }

    fun exchangeSocialToken(model: ExchangeTokenRequestDto): AuthResponseDto? {
        val response = AuthResponseDto()

        if (model.authProvider == AuthProvider.GOOGLE) {
            val profileUrl =
                String.format("https://www.googleapis.com/oauth2/v1/tokeninfo?access_token=%s", model.token)

            val res = restTemplate.getForObject(profileUrl,GoogleTokenInfoResponseDto::class.java)

            if (res != null) {
                val authentication = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken(res.email, "password")
                )

                val userDetails = authentication.principal as UserDetails

                val member = MemberResponseDto()
                member.email = res.email


                response.member = member
                response.bearerToken = jwtUtils.generateJwtToken(authentication)
                response.authorities = userDetails.authorities.stream()
                    .map { item -> item.authority }
                    .collect(Collectors.toList())
            }


        }



        return response
    }

    fun login(model: LoginRequestDto): AuthResponseDto? {
        val authentication = authenticationManager.authenticate(
            UsernamePasswordAuthenticationToken(model.username, model.password)
        )

        val userDetails = authentication.principal as UserDetails



        val response = AuthResponseDto()
        response.member = MemberResponseDto()
        response.member
        response.bearerToken = jwtUtils.generateJwtToken(authentication)
        response.authorities = userDetails.authorities.stream()
            .map { item -> item.authority }
            .collect(Collectors.toList())

        return response
    }

    fun countByGender(startDate: LocalDate, endDate: LocalDate, gender: String): Long {
        return repository.countByGender(
            startDate,
            endDate,
            gender
        )
    }

    fun sumTransactionsByGenderAndTransactionTypes(
        startDate: LocalDate,
        endDate: LocalDate,
        gender: String,
        transactionTypes: Array<TransactionType>
    ): Long {
        return repository.sumBalanceByGender(
            startDate,
            endDate,
            gender,
            transactionTypes.map { transactionType -> transactionType.name }.toTypedArray()
        )
    }

    fun resetMemberIdSequence(): Long? {
        return repository.resetMemberIdSequence()
    }

    fun updateTotalBalance(memberId: Long) {
        repository.updateTotalBalance(memberId)
    }



    fun registerMembers() {
        val reader = GoogleSheetUtils()

        val spreadsheetId = "17fuYsDWkBkv4aLaVI3ZwXdNAc2Pam52-jFqcN6N6FjI"
        val range = "Members!A2:M1000"
        val resource = resourceLoader.getResource("classpath:credentials.json")
        val serviceAccount = resource.inputStream

        val data = reader.readSheet(GoogleCredentials.fromStream(serviceAccount), spreadsheetId, range)

        val em: EntityManager = emf.createEntityManager()
        em.transaction.begin()
        em.createNativeQuery("truncate member cascade").executeUpdate()
        em.transaction.commit()

        repository.resetMemberIdSequence()


        for (record in data) {
            if (record.isEmpty()) {
                return
            }

            if (record.size < 2) {
                continue
            }


            val userId = record[0].toString().toLong()

            val member = repository.findById(userId).orElseGet {
                Member(
                    id = userId,
                    name = record[1].toString(),
                    createdDate = LocalDateTime.now(),
                    totalBalance = 0.0,
                    availableBalance = 0.0,
                    gender = if (record.size > 3) record[3].toString() else null,
                    phone = if (record.size > 2) record[2].toString() else null
                )
            }.let {
                repository.save(it)
            }


            val accountNumber = member.id.toString()
            val account = memberAccountRepository.findById(accountNumber).orElseGet {
                Account(member, AccountType.SAVINGS, accountNumber, createdDate = LocalDateTime.now())
            }
            memberAccountRepository.save(account)
        }

        repository.resetMemberIdSequence()

        em.transaction.begin()
        em.createNativeQuery("UPDATE member SET image='https://lh3.googleusercontent.com/d/1zPFy3jwXMpSsAwdn6nayCUayv9HhzOvj' WHERE id='390'").executeUpdate()
        em.createNativeQuery("UPDATE member SET image='https://lh3.googleusercontent.com/d/1ZoiayYoGAPt-t2GAJtgXX9ibdvW-FUfp' WHERE id='218'").executeUpdate()
        em.createNativeQuery("UPDATE member SET image='https://lh3.googleusercontent.com/d/1ZN4yRY8cGcSq_nXoyBQoOcKIkCi2avPF' WHERE id='030'").executeUpdate()
        em.createNativeQuery("UPDATE member SET image='https://lh3.googleusercontent.com/d/1Zf19seAZpruFmAD3aVAyFIP7dcnaCoSM' WHERE id='319'").executeUpdate()
        em.createNativeQuery("UPDATE member SET image='https://lh3.googleusercontent.com/d/1YxwtDuq6VnH5CkOh9isBLlzxFrDdHGuZ' WHERE id='439'").executeUpdate()
        em.createNativeQuery("UPDATE member SET image='https://lh3.googleusercontent.com/d/1YW9X9uEEfs3722lCyMJda1T88UjzZL3w' WHERE id='321'").executeUpdate()
        em.createNativeQuery("UPDATE member SET image='https://lh3.googleusercontent.com/d/1yV5d8Flyu29-D-CvFvdimbfafza2skUk' WHERE id='027'").executeUpdate()
        em.createNativeQuery("UPDATE member SET image='https://lh3.googleusercontent.com/d/1yoqlib19lXrTpZvHKFku0_fHOl_82bSg' WHERE id='150'").executeUpdate()
        em.createNativeQuery("UPDATE member SET image='https://lh3.googleusercontent.com/d/1y8KFCym4aBjQ5uJo2I2bnLUry_6MckC3' WHERE id='597'").executeUpdate()
        em.createNativeQuery("UPDATE member SET image='https://lh3.googleusercontent.com/d/1Y7KE1d1JLBJHH5_ad4rNCjXVVJHLWXGd' WHERE id='030'").executeUpdate()
        em.createNativeQuery("UPDATE member SET image='https://lh3.googleusercontent.com/d/1XwC0wxM_zP6aC_dlQn0A8-O1fYH33xaU' WHERE id='469'").executeUpdate()
        em.createNativeQuery("UPDATE member SET image='https://lh3.googleusercontent.com/d/1xPc4AzDvxAYcQddnQlr7Iqu9CGEHMZ14' WHERE id='555'").executeUpdate()
        em.createNativeQuery("UPDATE member SET image='https://lh3.googleusercontent.com/d/1XcCFmT_Gl914adyfBc84ApNruac8geTu' WHERE id='325'").executeUpdate()
        em.createNativeQuery("UPDATE member SET image='https://lh3.googleusercontent.com/d/1X7YeIkXO2WWRD-dzilggKv15t0Ys0QL1' WHERE id='407'").executeUpdate()
        em.createNativeQuery("UPDATE member SET image='https://lh3.googleusercontent.com/d/1wWjR5zN1ZnmkuQWZYHilVImcnPtsGy-5' WHERE id='488'").executeUpdate()
        em.createNativeQuery("UPDATE member SET image='https://lh3.googleusercontent.com/d/1wO4TCBTf4eDR4YPLJ62TGsteUON7q8FT' WHERE id='412'").executeUpdate()
        em.createNativeQuery("UPDATE member SET image='https://lh3.googleusercontent.com/d/1WNaGfcm_WoIvjEYKuJN6TUOvfuMksLWR' WHERE id='508'").executeUpdate()
        em.createNativeQuery("UPDATE member SET image='https://lh3.googleusercontent.com/d/1WFLYuutYi4CZ1BzHuUhWP4hPfsd95qv5' WHERE id='587'").executeUpdate()
        em.createNativeQuery("UPDATE member SET image='https://lh3.googleusercontent.com/d/1vyrzVNIVNRALiKguLo_yrYWCo3YE2ajn' WHERE id='169'").executeUpdate()
        em.createNativeQuery("UPDATE member SET image='https://lh3.googleusercontent.com/d/1Vnu1TSIcDnwbVE0YMeuLkEw9Q1KaGMbn' WHERE id='427'").executeUpdate()
        em.createNativeQuery("UPDATE member SET image='https://lh3.googleusercontent.com/d/1vBV4f1Y_K_IEwFQEdKzivuwEPc_ZYnKN' WHERE id='375'").executeUpdate()
        em.createNativeQuery("UPDATE member SET image='https://lh3.googleusercontent.com/d/1V7VfNCqBcii2eUEpJ-_wBA3Q-2KBrN21' WHERE id='231'").executeUpdate()
        em.createNativeQuery("UPDATE member SET image='https://lh3.googleusercontent.com/d/1UUOJDx-ww0vHjUPuMSZiJ_g84F4-qJLc' WHERE id='158'").executeUpdate()
        em.createNativeQuery("UPDATE member SET image='https://lh3.googleusercontent.com/d/1ugmB-UQZnF-k-d4lnPApyf-3HM-_HoGO' WHERE id='094'").executeUpdate()
        em.createNativeQuery("UPDATE member SET image='https://lh3.googleusercontent.com/d/1u-FzSWsX1Wc6Yiz0gqfQOsz-g8-pJsaD' WHERE id='552'").executeUpdate()
        em.createNativeQuery("UPDATE member SET image='https://lh3.googleusercontent.com/d/1TWP2dpfdvqqYXrasV_5lwt0fYzFY_SJz' WHERE id='533'").executeUpdate()
        em.createNativeQuery("UPDATE member SET image='https://lh3.googleusercontent.com/d/1tQQezmCeUSG9yLWiYfQA8TulLwcK4l4v' WHERE id='605'").executeUpdate()
        em.createNativeQuery("UPDATE member SET image='https://lh3.googleusercontent.com/d/1T1af4lnGepU4QVCi-aMKpt6iisBe9Ul9' WHERE id='484'").executeUpdate()
        em.createNativeQuery("UPDATE member SET image='https://lh3.googleusercontent.com/d/1sN200sV1OBcrXrNWzSlmTtjBU6LdhWp-' WHERE id='483'").executeUpdate()
        em.createNativeQuery("UPDATE member SET image='https://lh3.googleusercontent.com/d/1Shj4fyaC8Rl4T3TVJsFpMxog-FhypZdb' WHERE id='549'").executeUpdate()
        em.createNativeQuery("UPDATE member SET image='https://lh3.googleusercontent.com/d/1rVCI-rX-xjqouQE-2YQj8JYOhbWqaRKq' WHERE id='435'").executeUpdate()
        em.createNativeQuery("UPDATE member SET image='https://lh3.googleusercontent.com/d/1Rf1AxILKawTsIrWjhvCEAd7QOeCjO67c' WHERE id='057'").executeUpdate()
        em.createNativeQuery("UPDATE member SET image='https://lh3.googleusercontent.com/d/1RAcgzl-tRvDlBR1Wg3MNfNMoyKqFaDso' WHERE id='366'").executeUpdate()
        em.createNativeQuery("UPDATE member SET image='https://lh3.googleusercontent.com/d/1R6qP1hGtLuF9P4ITy-HY2b2LSKuhPnrM' WHERE id='161'").executeUpdate()
        em.createNativeQuery("UPDATE member SET image='https://lh3.googleusercontent.com/d/1QRcLkVxFHLDEsOUuSj7eHycae8ANuBM6' WHERE id='145'").executeUpdate()
        em.createNativeQuery("UPDATE member SET image='https://lh3.googleusercontent.com/d/1qLTCJ4dM578sq5I4TXXp8upzHkZUwCJC' WHERE id='591'").executeUpdate()
        em.createNativeQuery("UPDATE member SET image='https://lh3.googleusercontent.com/d/1qK4AoptdOWe8JBQ4CsXonGIC-J3jJkkE' WHERE id='433'").executeUpdate()
        em.createNativeQuery("UPDATE member SET image='https://lh3.googleusercontent.com/d/1QJX1O3iFZe_Iftnv_zwHXqjll-lxVfIK' WHERE id='153'").executeUpdate()
        em.createNativeQuery("UPDATE member SET image='https://lh3.googleusercontent.com/d/1qfyD8NLKCgotKVvYe8GZAD8fGgyyR05P' WHERE id='019'").executeUpdate()
        em.createNativeQuery("UPDATE member SET image='https://lh3.googleusercontent.com/d/1OqLubu3Iq3jdBJgiLvLG8dr_j4cKsxWG' WHERE id='590'").executeUpdate()
        em.createNativeQuery("UPDATE member SET image='https://lh3.googleusercontent.com/d/1olbb2hWEefHj2M2OSG2keaCJe_4vZXlB' WHERE id='005'").executeUpdate()
        em.createNativeQuery("UPDATE member SET image='https://lh3.googleusercontent.com/d/1OCbgKLRopMGBfMWfs5ltMLoEXvo4g5fo' WHERE id='582'").executeUpdate()
        em.createNativeQuery("UPDATE member SET image='https://lh3.googleusercontent.com/d/1nPPu93j_mdmjfkZYbFqcTh4GWmDAKtdS' WHERE id='579'").executeUpdate()
        em.createNativeQuery("UPDATE member SET image='https://lh3.googleusercontent.com/d/1n9NQQO5PIvADYW3pc6ponvRDx2d7tegX' WHERE id='154'").executeUpdate()
        em.createNativeQuery("UPDATE member SET image='https://lh3.googleusercontent.com/d/1mJTAYYShGSU2pSUk8_DrYfivStalO4Mi' WHERE id='348'").executeUpdate()
        em.createNativeQuery("UPDATE member SET image='https://lh3.googleusercontent.com/d/1mDyRJ_lObZyOLOusDDrZhQKuSUKsr8Lh' WHERE id='307'").executeUpdate()
        em.createNativeQuery("UPDATE member SET image='https://lh3.googleusercontent.com/d/1m-HoinLScK_7ZDrI5GVexDhW7HuGkyPW' WHERE id='558'").executeUpdate()
        em.createNativeQuery("UPDATE member SET image='https://lh3.googleusercontent.com/d/1ls-xE5tUxF3ozaKvuS5CCf_W4gOamPAc' WHERE id='018'").executeUpdate()
        em.createNativeQuery("UPDATE member SET image='https://lh3.googleusercontent.com/d/1lGAHOWAFIKcPzWTWjQQh9u213L1xwOrT' WHERE id='179'").executeUpdate()
        em.createNativeQuery("UPDATE member SET image='https://lh3.googleusercontent.com/d/1jp2rsV6uj_A0zRagB1SabTrHH8Z2nnXT' WHERE id='413'").executeUpdate()
        em.createNativeQuery("UPDATE member SET image='https://lh3.googleusercontent.com/d/1jMUHSnigjyHfUjaZRqCEz8H3oxJ7JwId' WHERE id='481'").executeUpdate()
        em.createNativeQuery("UPDATE member SET image='https://lh3.googleusercontent.com/d/1JIXbFjPanYk_csU67rgYB8ygpE7Is5j0' WHERE id='398'").executeUpdate()
        em.createNativeQuery("UPDATE member SET image='https://lh3.googleusercontent.com/d/1IVYYhOO98iT65Bqp8JzS2fxnwMobyH36' WHERE id='070'").executeUpdate()
        em.createNativeQuery("UPDATE member SET image='https://lh3.googleusercontent.com/d/1iuXEhCJshg0TSQN021hPLnyP2spVtaiD' WHERE id='547'").executeUpdate()
        em.createNativeQuery("UPDATE member SET image='https://lh3.googleusercontent.com/d/1iUemEgpjvcr5uzGyzFm8n3wEMnY-g_UD' WHERE id='168'").executeUpdate()
        em.createNativeQuery("UPDATE member SET image='https://lh3.googleusercontent.com/d/1IqxSnINNRLuRmKcspUc7ydoMTAstbsIb' WHERE id='119'").executeUpdate()
        em.createNativeQuery("UPDATE member SET image='https://lh3.googleusercontent.com/d/1ioPNZNNJJoyd-ljYJ_QspzMB0CVlblqA' WHERE id='122'").executeUpdate()
        em.createNativeQuery("UPDATE member SET image='https://lh3.googleusercontent.com/d/1I5f6evF5QCyI7TTyYWttcCnDpYB7efLs' WHERE id='502'").executeUpdate()
        em.createNativeQuery("UPDATE member SET image='https://lh3.googleusercontent.com/d/1i2MHP3LIXo907bvPJfY7WyS2ntdnNqtC' WHERE id='515'").executeUpdate()
        em.createNativeQuery("UPDATE member SET image='https://lh3.googleusercontent.com/d/1HK6k0DJD8YQiOXbi5UGmLDa5fuZkbdb5' WHERE id='400'").executeUpdate()
        em.createNativeQuery("UPDATE member SET image='https://lh3.googleusercontent.com/d/1H_i7eST3A15b-8gzc7gzc3WFTmYgYepP' WHERE id='221'").executeUpdate()
        em.createNativeQuery("UPDATE member SET image='https://lh3.googleusercontent.com/d/1GE0uLnMidQK39sdB2wJvYxTEqiKKIWYA' WHERE id='330'").executeUpdate()
        em.createNativeQuery("UPDATE member SET image='https://lh3.googleusercontent.com/d/1fnw52AQDGMSXcB2Y-jQQtMTr8r37XkaN' WHERE id='429'").executeUpdate()
        em.createNativeQuery("UPDATE member SET image='https://lh3.googleusercontent.com/d/1F_TgRZZGRXirlY9NWYu8xxz3QVHpKscG' WHERE id='130'").executeUpdate()
        em.createNativeQuery("UPDATE member SET image='https://lh3.googleusercontent.com/d/1EzW5oHVTfRZKdl90GKcBfdKQk7itsZBZ' WHERE id='136'").executeUpdate()
        em.createNativeQuery("UPDATE member SET image='https://lh3.googleusercontent.com/d/1EUkxnxKQNBd1Ml7ChAr2lN6uxGHRZc5n' WHERE id='504'").executeUpdate()
        em.createNativeQuery("UPDATE member SET image='https://lh3.googleusercontent.com/d/1eRDBupjLgYoY0G4vl4eGynIyy8twDlhF' WHERE id='559'").executeUpdate()
        em.createNativeQuery("UPDATE member SET image='https://lh3.googleusercontent.com/d/1eg6d8FIa6l-EJju43kaib50MwQLRa-WR' WHERE id='530'").executeUpdate()
        em.createNativeQuery("UPDATE member SET image='https://lh3.googleusercontent.com/d/1DYr2zdvRykytSpZuznvL_i1UH8a-ZQdZ' WHERE id='004'").executeUpdate()
        em.createNativeQuery("UPDATE member SET image='https://lh3.googleusercontent.com/d/1DWmm04DerDw_TG0bFf_a1wIW7b3ckQM3' WHERE id='025'").executeUpdate()
        em.createNativeQuery("UPDATE member SET image='https://lh3.googleusercontent.com/d/1dPhuW0hKMbAMt6YO74fg-HY8ctd7jkGZ' WHERE id='163'").executeUpdate()
        em.createNativeQuery("UPDATE member SET image='https://lh3.googleusercontent.com/d/1dMSxo6LiJfHR8q0hhGyN-9NHrZsSK85O' WHERE id='070'").executeUpdate()
        em.createNativeQuery("UPDATE member SET image='https://lh3.googleusercontent.com/d/1dj6pUhJ_itTa06kalsZ_G2DhKqhzwFjx' WHERE id='498'").executeUpdate()
        em.createNativeQuery("UPDATE member SET image='https://lh3.googleusercontent.com/d/1cMNrNQ7sAvUSACLojilv9SI8_9fy62Zx' WHERE id='418'").executeUpdate()
        em.createNativeQuery("UPDATE member SET image='https://lh3.googleusercontent.com/d/1CDIYc4vzfBeFjAOJLPg1Z6LawRzLGz0b' WHERE id='500'").executeUpdate()
        em.createNativeQuery("UPDATE member SET image='https://lh3.googleusercontent.com/d/1CaXrHcOyhnEgLs804Kj_WXRFLBe7YVgB' WHERE id='534'").executeUpdate()
        em.createNativeQuery("UPDATE member SET image='https://lh3.googleusercontent.com/d/1bVa-t2Bu_WvB35t8Je2fvxAU3sHmk2u8' WHERE id='016'").executeUpdate()
        em.createNativeQuery("UPDATE member SET image='https://lh3.googleusercontent.com/d/1BixdikT39cOhshvLBV7W_jniAyZgrsKn' WHERE id='365'").executeUpdate()
        em.createNativeQuery("UPDATE member SET image='https://lh3.googleusercontent.com/d/1bcL4Q82Lwm3uKl4CrGUZEJj63BCcU9_z' WHERE id='313'").executeUpdate()
        em.createNativeQuery("UPDATE member SET image='https://lh3.googleusercontent.com/d/1ahCO9x8-TKcSUi3B9YVoJlLcM_kTPaqo' WHERE id='570'").executeUpdate()
        em.createNativeQuery("UPDATE member SET image='https://lh3.googleusercontent.com/d/1ABo-g1m-7w7IZBH6zoY7TYnpIA_d-XRh' WHERE id='071'").executeUpdate()
        em.createNativeQuery("UPDATE member SET image='https://lh3.googleusercontent.com/d/19hVqgTwlqlXmyZsHuI1vtAztt_ny8Jk1' WHERE id='471'").executeUpdate()
        em.createNativeQuery("UPDATE member SET image='https://lh3.googleusercontent.com/d/19GTOpm9N1K_bPx-f5inJHkn1jjeun-Ga' WHERE id='135'").executeUpdate()
        em.createNativeQuery("UPDATE member SET image='https://lh3.googleusercontent.com/d/19FyaTbobxC2JFbLHJTLh0rIQNBPPyxFR' WHERE id='451'").executeUpdate()
        em.createNativeQuery("UPDATE member SET image='https://lh3.googleusercontent.com/d/18j3Hqy-h7HwCTY15vcKUoxhXq99GWsSq' WHERE id='146'").executeUpdate()
        em.createNativeQuery("UPDATE member SET image='https://lh3.googleusercontent.com/d/17xfUJtbX-LOjY688KBwxIHmE9JnFIgI0' WHERE id='585'").executeUpdate()
        em.createNativeQuery("UPDATE member SET image='https://lh3.googleusercontent.com/d/17SEHNeVem1GzxvmQIYethqLq7mD1X7Dp' WHERE id='571'").executeUpdate()
        em.createNativeQuery("UPDATE member SET image='https://lh3.googleusercontent.com/d/16svMTHrPM3fRi0B4bzIPRcKjZlX5KUGa' WHERE id='134'").executeUpdate()
        em.createNativeQuery("UPDATE member SET image='https://lh3.googleusercontent.com/d/15UpsD9Y5uj1R328OGR4nLy8HmgUFGsSZ' WHERE id='008'").executeUpdate()
        em.createNativeQuery("UPDATE member SET image='https://lh3.googleusercontent.com/d/13qk9OSFz1UPdpAKRgK_-rp1XztcP2QO9' WHERE id='499'").executeUpdate()
        em.createNativeQuery("UPDATE member SET image='https://lh3.googleusercontent.com/d/11vIODqJsfd2LCgo68ppoKa3BFYZN2e8-' WHERE id='044'").executeUpdate()
        em.createNativeQuery("UPDATE member SET image='https://lh3.googleusercontent.com/d/11UptiPiAeMI2TTf3cv_9_J9RTv2jd3E4' WHERE id='336'").executeUpdate()
        em.createNativeQuery("UPDATE member SET image='https://lh3.googleusercontent.com/d/11Bn5bvU-9SbuhT0Z5l80ERjvG2PoFR0_' WHERE id='311'").executeUpdate()
        em.createNativeQuery("UPDATE member SET image='https://lh3.googleusercontent.com/d/10HqWmULsa-Oq3qoqDshGgcK34rx9uGKU' WHERE id='339'").executeUpdate()
        em.createNativeQuery("UPDATE member SET image='https://lh3.googleusercontent.com/d/106ZGDwm8UxsU0hSbEpuRcqQa6tjzMRzi' WHERE id='368'").executeUpdate()
        em.createNativeQuery("UPDATE member SET image='https://lh3.googleusercontent.com/d/1-WNYRytE1hQNJH2XHCiNS21Ty83MwMHG' WHERE id='093'").executeUpdate()
        em.createNativeQuery("UPDATE member SET image='https://lh3.googleusercontent.com/d/1-3RbckyQHkrAUMAAqX7JF-nKReLFhHxK' WHERE id='584'").executeUpdate()
        em.createNativeQuery("UPDATE member SET image='https://lh3.googleusercontent.com/d/1_8RIS_DcAwEQ3tqvNpeLLgF6joW5u66Q' WHERE id='450'").executeUpdate()
        em.transaction.commit()
    }


}