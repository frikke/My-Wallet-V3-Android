package com.blockchain.api.payments.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CardTokenIdResponse(
    @SerialName("card_token_id")
    val cardTokenId: String,
    @SerialName("vgs_vault_id")
    val vgsVaultId: String
)
