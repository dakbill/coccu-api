package com.coc.cu.domain.models

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import java.io.Serializable


class ApiResponse<T> : ResponseEntity<CustomBody<T>> {


    constructor(data: T?, status: HttpStatus) : super(CustomBody<T>(status, data), status) {

    }

    constructor(data: T?, message: String, status: HttpStatus) : super(CustomBody<T>(status, message, data),  status) {

    }


}

class CustomBody<T>(data: T?) : Serializable {

    var code: Int = 2000

    var message: String = "Success"

    var data: T? = data


    constructor(status: HttpStatus, data: T?) : this(data) {
        this.code = status.value() * 10
        this.data = data
    }

    constructor(status: HttpStatus, message: String, data: T?) : this(data) {
        this.code = status.value() * 10
        this.data = data
        this.message = message
    }


}