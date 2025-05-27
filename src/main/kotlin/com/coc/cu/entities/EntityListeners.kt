package com.coc.cu.entities



import com.coc.cu.repositories.MembersRepository
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Component
import javax.persistence.PostPersist
import javax.persistence.PostRemove
import javax.persistence.PostUpdate
import kotlin.concurrent.thread


@Component
class TransactionListener(@Lazy val membersRepository: MembersRepository) {

    @PostPersist
    fun postPersist(target: Transaction?) {
        thread(start = true) {
            if(target?.account != null){
                membersRepository.updateTransactionCount(target.account!!.member!!.id ?:0)
                membersRepository.updateTotalBalance(target.account!!.member!!.id ?:0)
            }

        }
    }

    @PostUpdate
    fun postUpdate(target: Transaction?) {

    }

    @PostRemove
    fun postDelete(target: Transaction?) {

    }

}