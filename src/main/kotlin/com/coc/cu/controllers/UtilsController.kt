package com.coc.cu.controllers


import com.coc.cu.domain.*
import com.coc.cu.domain.models.ApiResponse
import com.coc.cu.services.TransactionsService
import com.coc.cu.services.UsersService
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*



@RequestMapping("/api/v1/utils")
@RestController
class UtilsController(
    val usersService: UsersService,
    val transactionsService: TransactionsService,
) {

   

    @GetMapping("/google-sheet-sync")
    fun googleSheetSync(): ApiResponse<Boolean> {

        usersService.registerMembers()
        transactionsService.recordTransactions()

        return ApiResponse(true, "OK", HttpStatus.OK)
    }


    

}