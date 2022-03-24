package com.blockchain.nabu.models.responses.nabu

import com.blockchain.serialization.JsonSerializable
import kotlinx.serialization.Serializable

@Serializable
internal data class TierUpdateJson(
    val selectedTier: Int
) : JsonSerializable
