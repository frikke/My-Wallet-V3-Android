package com.blockchain.api.paymentmethods.models

import com.blockchain.api.NabuUxErrorResponse
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AliasInfoResponse(
    @SerialName("id")
    val id: String,
    @SerialName("address")
    val address: String?,
    @SerialName("agent")
    val agent: Agent?,
    @SerialName("currency")
    val currency: String?,
    @SerialName("state")
    val state: String?,
    @SerialName("name")
    val name: String?,
    @SerialName("whitelisted")
    val whitelisted: Boolean,
    @SerialName("fiat")
    val fiat: Boolean,
    @SerialName("ux")
    val ux: NabuUxErrorResponse?
) {

    @Serializable
    data class Agent(
        @SerialName("account")
        val account: String?,
        @SerialName("address")
        val address: String?,
        @SerialName("label")
        val label: String?,
        @SerialName("holderDocument")
        val holderDocument: String?,
        @SerialName("name")
        val name: String?,
        @SerialName("bankName")
        val bankName: String?,
        @SerialName("accountType")
        val accountType: String?
    )
}

@Serializable
data class AliasInfoRequestBody(
    val currency: String,
    val address: String
)

@Serializable
data class LinkWithAliasRequestBody(
    val beneficiaryId: String
)
