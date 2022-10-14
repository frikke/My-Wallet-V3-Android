package com.blockchain.core.settings

import info.blockchain.wallet.api.data.Settings
import io.reactivex.rxjava3.core.Single
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okhttp3.ResponseBody
import retrofit2.Response

fun Response<ResponseBody>.handleError(): Single<Settings> =
    errorBody()?.let { errorBody ->
        val errorResponse = Json.decodeFromString<SavePhoneNumberError>(errorBody.string().toString())
        if (!errorResponse.success) {
            Single.error(
                if (errorResponse.error.contains(PHONE_INVALID)) {
                    InvalidPhoneNumber()
                } else {
                    Exception()
                }
            )
        } else {
            Single.error(Exception())
        }
    } ?: kotlin.run {
        Single.just(Settings())
    }

private const val PHONE_INVALID = "Invalid SMS Number"

@Serializable
data class SavePhoneNumberError(
    @SerialName("success") val success: Boolean,
    @SerialName("error") val error: String
)

class InvalidPhoneNumber : Exception()
