package com.blockchain.api.selfcustody.activity

import com.blockchain.api.selfcustody.AuthInfo
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ActivityRequest(
    @SerialName("auth")
    val auth: AuthInfo,
    @SerialName("params")
    val params: ActivityRequestParams,
    @SerialName("action")
    val action: String,
    @SerialName("channel")
    val channel: String
)

@Serializable
data class ActivityRequestParams(
    @SerialName("timeZone")
    val timeZone: String,
    @SerialName("fiatCurrency")
    val fiatCurrency: String,
    @SerialName("locales")
    val locales: String
)
