package com.blockchain.api.watchlist

import com.blockchain.api.watchlist.model.WatchlistBody
import com.blockchain.api.watchlist.model.WatchlistDto
import com.blockchain.api.watchlist.model.WatchlistItemDto
import com.blockchain.outcome.Outcome
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.HTTP
import retrofit2.http.PUT

internal interface WatchlistApi {
    @GET("watchlist")
    suspend fun getWatchlist(): Outcome<Exception, WatchlistDto>

    // @DELETE annotation does not support having a body, so we need to define it like this instead
    @HTTP(method = "DELETE", path = "watchlist", hasBody = true)
    suspend fun removeFromWatchlist(
        @Body body: WatchlistBody
    ): Outcome<Exception, Unit>

    @PUT("watchlist")
    suspend fun addToWatchlist(
        @Body body: WatchlistBody
    ): Outcome<Exception, WatchlistItemDto>
}
