package com.blockchain.core.auth.metadata

import com.blockchain.core.auth.isValidGuid
import com.blockchain.serialization.JsonSerializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WalletCredentialsMetadata(
    @SerialName("guid")
    val guid: String,

    @SerialName("password")
    val password: String,

    @SerialName("sharedKey")
    val sharedKey: String
) : JsonSerializable {

    fun isValid() = guid.isValidGuid() && password.isNotEmpty() && sharedKey.isNotEmpty()
}
