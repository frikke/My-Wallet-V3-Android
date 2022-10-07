package com.blockchain.api.watchlist.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class WatchlistBody(
    @SerialName("asset")
    val asset: String,
    @SerialName("tags")
    val tags: List<String>
)

@Serializable
class WatchlistDto(
    @SerialName("assets")
    val items: List<WatchlistItemDto>
)

@Serializable
class WatchlistItemDto(
    @SerialName("asset")
    val asset: String,
    @SerialName("insertedAt")
    val insertedAt: String,
    @SerialName("updatedAt")
    val updatedAt: String,
    @SerialName("tags")
    val tags: List<WatchlistTagDto>
)

@Serializable
class WatchlistTagDto(
    @SerialName("tag")
    val tag: String,
    @SerialName("insertedAt")
    val insertedAt: String
)
