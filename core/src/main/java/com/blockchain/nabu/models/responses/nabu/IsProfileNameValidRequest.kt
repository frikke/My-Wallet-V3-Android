package com.blockchain.nabu.models.responses.nabu

import kotlinx.serialization.Serializable

@Serializable
class IsProfileNameValidRequest(
    val firstName: String,
    val lastName: String
)
