package com.blockchain.api.selfcustody

import com.blockchain.domain.wallet.PubKeyStyle
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ActivityRequest(
    @SerialName("auth")
    val auth: AuthInfo,
    @SerialName("params")
    val params: ActivityRequestParams,
    @SerialName("nextPage")
    val nextPage: String?
)

@Serializable
data class ActivityRequestParams(
    @SerialName("timezoneIana")
    val timezone: String,
    @SerialName("fiatCurrency")
    val fiatCurrency: String,
    @SerialName("acceptLanguage")
    val acceptLanguage: String,
    @SerialName("network")
    val networkTicker: String,
    @SerialName("pubKeyInfo")
    val pubKeyInfo: ActivityPubKeyInfo
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
