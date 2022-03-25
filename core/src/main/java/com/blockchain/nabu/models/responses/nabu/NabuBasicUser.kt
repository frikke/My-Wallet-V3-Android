package com.blockchain.nabu.models.responses.nabu

import com.squareup.moshi.Json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NabuBasicUser(
    @SerialName("firstName")
    @field:Json(name = "firstName")
    val firstName: String,

    @SerialName("lastName")
    @field:Json(name = "lastName")
    val lastName: String,

    @SerialName("dob")
    @field:Json(name = "dob")
    val dateOfBirth: String
)
