package com.coc.cu.entities



import com.coc.cu.repositories.MembersRepository
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Component
import javax.persistence.PostPersist
import javax.persistence.PostRemove
import javax.persistence.PostUpdate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


@Component
class TransactionListener(@Lazy val membersRepository: MembersRepository) {

    companion object {
        var enabled: Boolean = true
    }

    @PostPersist
    fun postPersist(target: Transaction?) {
        if (!enabled) return
        CoroutineScope(Dispatchers.IO).launch {
            if(target?.account != null){
                try {
                    membersRepository.updateTransactionCount(target.account!!.member!!.id ?:0)
                    membersRepository.updateTotalBalance(target.account!!.member!!.id ?:0)
                } catch (e: Exception) {
                    // Ignored
                }
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