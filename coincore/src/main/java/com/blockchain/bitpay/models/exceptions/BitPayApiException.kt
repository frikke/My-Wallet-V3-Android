package com.blockchain.bitpay.models.exceptions

import retrofit2.Response

class BitPayApiException private constructor(message: String) : Throwable(message) {

    var _httpErrorCode: Int = -1
    lateinit var _error: String

    companion object {
        fun fromResponseBody(response: Response<*>?): BitPayApiException {
            val bitpayErrorResponse = response?.errorBody()!!.string()
            val httpErrorCode = response.code()

            return BitPayApiException("$httpErrorCode: $bitpayErrorResponse")
                .apply {
                    _httpErrorCode = httpErrorCode
                    _error = bitpayErrorResponse
                }
        }
    }
}
