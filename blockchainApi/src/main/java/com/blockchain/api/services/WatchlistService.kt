package com.blockchain.api.services

import com.blockchain.api.adapters.ApiException
import com.blockchain.api.watchlist.WatchlistApi
import com.blockchain.api.watchlist.data.WatchlistBody
import com.blockchain.api.watchlist.data.WatchlistListResponse
import com.blockchain.outcome.Outcome

class WatchlistService internal constructor(
    private val api: WatchlistApi
) {

    suspend fun getWatchlist(authHeader: String): Outcome<ApiException, WatchlistListResponse> =
        api.getWatchlist(authHeader)

    suspend fun addToWatchlist(authHeader: String, assetTicker: String, tags: List<AssetTag>) =
        api.addToWatchlist(
            authorization = authHeader,
            body = WatchlistBody(
                asset = assetTicker,
                tags = tags.map { it.tagName }
            )
        )

    suspend fun removeFromWatchlist(authHeader: String, assetTicker: String, tags: List<AssetTag>) =
        api.removeFromWatchlist(
            authorization = authHeader,
            body = WatchlistBody(
                asset = assetTicker,
                tags = tags.map { it.tagName }
            )
        )
}

enum class AssetTag(val tagName: String = "Favourite") {
    Favourite,
    Unknown;

    companion object {

        fun fromString(name: String) =
            when (name) {
                "Favourite" -> Favourite
                else -> Unknown
            }
    }
}
