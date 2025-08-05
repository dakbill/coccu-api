package com.coc.cu.controllers


import com.coc.cu.domain.models.ApiResponse
import com.coc.cu.services.TransactionsService
import com.coc.cu.services.UsersService
import com.coc.cu.utils.JwtUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController


@RequestMapping("/api/v1/utils")
@RestController
class UtilsController(
    val usersService: UsersService,
    val transactionsService: TransactionsService,
) {
    val logger: Logger = LoggerFactory.getLogger(UtilsController::class.java)


    @GetMapping("/google-sheet-sync")
    suspend fun googleSheetSync(): ApiResponse<Boolean> = withContext(Dispatchers.IO) {
        val job = launch {
            try {
                logger.info("Starting Google Sheet sync")
                usersService.registerMembers()
                logger.info("Users registration completed")

                transactionsService.recordTransactions()
                logger.info("Transactions recording completed")

                logger.info("Google Sheet sync completed successfully")
            } catch (e: Exception) {
                logger.error("Google Sheet sync failed", e)
                // Consider notifying admin or updating status somewhere
            }
        }



        ApiResponse(true, "Sync process started", HttpStatus.ACCEPTED)
    }


    

}