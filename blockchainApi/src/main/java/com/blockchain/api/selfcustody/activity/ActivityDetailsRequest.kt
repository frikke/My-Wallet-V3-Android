package com.blockchain.api.selfcustody.activity

import com.blockchain.api.selfcustody.AuthInfo
import com.blockchain.domain.wallet.PubKeyStyle
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
    val pubKey: ActivityPubKeyInfo,
    @SerialName("localisationParams")
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

@Serializable
data class ActivityPubKeyInfo(
    @SerialName("pubKey")
    val pubKey: String,
    @SerialName("style")
    val style: PubKeyStyle,
    @SerialName("descriptor")
    val descriptor: String
)
