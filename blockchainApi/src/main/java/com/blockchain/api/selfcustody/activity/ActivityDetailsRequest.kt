package com.blockchain.api.selfcustody.activity

import com.blockchain.api.selfcustody.AuthInfo
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ActivityDetailsRequest(
    @SerialName("auth")
    val auth: AuthInfo,
    @SerialName("txId")
    val txId: String,
    @SerialName("network")
    val network: String,
    @SerialName("pubKey")
    val pubKey: String,
    @SerialName("localisation")
    val params: LocalisationParams
)

@Serializable
data class LocalisationParams(
    @SerialName("timeZone")
    val timeZone: String,
    @SerialName("locales")
    val locales: String,
    @SerialName("fiatCurrency")
    val fiatCurrency: String
)
