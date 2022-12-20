package com.blockchain.api.services

import com.blockchain.api.watchlist.WatchlistApi
import com.blockchain.api.watchlist.model.WatchlistBody
import com.blockchain.api.watchlist.model.WatchlistDto
import com.blockchain.outcome.Outcome

class WatchlistApiService internal constructor(
    private val api: WatchlistApi
) {

    suspend fun getWatchlist(): Outcome<Exception, WatchlistDto> =
        api.getWatchlist()

    suspend fun addToWatchlist(assetTicker: String) =
        api.addToWatchlist(
            body = WatchlistBody(
                asset = assetTicker,
                tags = listOf(FAVOURITE_TAG)
            )
        )

    suspend fun removeFromWatchlist(assetTicker: String) =
        api.removeFromWatchlist(
            body = WatchlistBody(
                asset = assetTicker,
                tags = listOf(FAVOURITE_TAG)
            )
        )

    companion object {
        const val FAVOURITE_TAG = "Favourite"
    }
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
