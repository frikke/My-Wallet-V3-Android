package com.blockchain.api.bitcoin.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class XpubDto(
    @SerialName("m")
    val address: String,
    @SerialName("path")
    val derivationPath: String
)
