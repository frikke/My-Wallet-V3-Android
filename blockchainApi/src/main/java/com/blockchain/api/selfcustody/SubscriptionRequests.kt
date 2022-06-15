package com.blockchain.api.selfcustody

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AddSubscriptionRequest(
    @SerialName("auth")
    val auth: AuthInfo,
    @SerialName("data")
    val data: List<SubscriptionInfo>
)

@Serializable
data class RemoveSubscriptionRequest(
    @SerialName("auth")
    val auth: AuthInfo,
    @SerialName("currency")
    val currency: String
)

@Serializable
data class GetSubscriptionsRequest(
    @SerialName("auth")
    val auth: AuthInfo
)

@Serializable
data class AuthInfo(
    @SerialName("guidHash")
    val guidHash: String,
    @SerialName("sharedKeyHash")
    val sharedKeyHash: String
)

@Serializable
data class SubscriptionInfo(
    @SerialName("currency")
    val currency: String,
    @SerialName("account")
    val accountInfo: AccountInfo,
    @SerialName("pubkeys")
    val pubkeys: List<PubKeyInfo>
)

@Serializable
data class AccountInfo(
    @SerialName("index")
    val index: Int,
    @SerialName("name")
    val name: String
)

@Serializable
data class PubKeyInfo(
    @SerialName("pubkey")
    val pubKey: String,
    @SerialName("style")
    val style: String = "SINGLE",
    @SerialName("descriptor")
    val descriptor: Int = 0
)
