package com.blockchain.api.selfcustody

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class AddressesResponse(
    @SerialName("results")
    val addressEntries: List<AddressEntry>
)

@Serializable
data class AddressEntry(
    @SerialName("addresses")
    val addresses: List<DerivedAddress>,
    @SerialName("account")
    val accountInfo: AccountInfo
)

@Serializable
data class DerivedAddress(
    @SerialName("pubKey")
    val pubKey: String,
    @SerialName("address")
    val address: String,
    @SerialName("includesMemo")
    val includesMemo: Boolean,
    @SerialName("format")
    val format: String,
    @SerialName("default")
    val default: Boolean
)
