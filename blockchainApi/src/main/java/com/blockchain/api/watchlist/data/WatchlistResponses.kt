package com.blockchain.api.watchlist.data

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
class WatchlistListResponse(
    @SerialName("assets")
    val assets: List<WatchlistResponse>
)

@Serializable
class WatchlistResponse(
    @SerialName("asset")
    val asset: String,
    @SerialName("insertedAt")
    val insertedAt: String,
    @SerialName("updatedAt")
    val updatedAt: String,
    @SerialName("tags")
    val tags: List<TagResponse>
)

@Serializable
class TagResponse(
    @SerialName("tag")
    val tag: String,
    @SerialName("insertedAt")
    val insertedAt: String
)
