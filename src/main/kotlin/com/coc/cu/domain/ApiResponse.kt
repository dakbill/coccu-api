package com.coc.cu.domain.models

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import java.io.Serializable


class ApiResponse<T> : ResponseEntity<CustomBody<T>> {


    constructor(data: T?, status: HttpStatus) : super(CustomBody<T>(status, data), status)

    constructor(data: T?, message: String, status: HttpStatus) : super(CustomBody<T>(status, message, data), status)

    constructor(
        data: T?,
        message: String,
        status: HttpStatus,
        errors: List<Map<String, String>>?
    ) : super(CustomBody<T>(status, message, data, errors), status)


    constructor(
        data: T?,
        message: String,
        status: HttpStatus,
        page: Int,
        size: Int,
        total: Long,
    ) : super(CustomBody<T>(status, message, data, page, size, total), status)




}

class Pager(var page: Int, var size: Int, var total: Long) : Serializable {
}

class CustomBody<T>(data: T?) : Serializable {

    var code: Int = 2000

    var message: String = "Success"

    var data: T? = data

    var pager: Pager? = null

    var errors: List<Map<String, String>>? = null


    constructor(status: HttpStatus, data: T?) : this(data) {
        this.code = status.value() * 10
        this.data = data
    }

    constructor(status: HttpStatus, message: String, data: T?) : this(data) {
        this.code = status.value() * 10
        this.data = data
        this.message = message
    }

    constructor(status: HttpStatus, message: String, data: T?, page: Int, size: Int, total: Long) : this(data) {
        this.code = status.value() * 10
        this.data = data
        this.message = message
        this.pager = Pager(page, size, total)
    }

    constructor(status: HttpStatus, message: String, data: T?, errors: List<Map<String, String>>?) : this(data) {
        this.code = status.value() * 10
        this.data = data
        this.message = message
        this.errors = errors
    }


}