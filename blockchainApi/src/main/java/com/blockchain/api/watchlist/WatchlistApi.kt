package com.blockchain.api.watchlist

import com.blockchain.api.watchlist.data.WatchlistBody
import com.blockchain.api.watchlist.data.WatchlistListResponse
import com.blockchain.api.watchlist.data.WatchlistResponse
import com.blockchain.outcome.Outcome
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.HTTP
import retrofit2.http.PUT

internal interface WatchlistApi {
    @GET("watchlist")
    suspend fun getWatchlist(): Outcome<Exception, WatchlistListResponse>

    // @DELETE annotation does not support having a body, so we need to define it like this instead
    @HTTP(method = "DELETE", path = "watchlist", hasBody = true)
    suspend fun removeFromWatchlist(
        @Body body: WatchlistBody
    ): Outcome<Exception, Unit>

    @PUT("watchlist")
    suspend fun addToWatchlist(
        @Body body: WatchlistBody
    ): Outcome<Exception, WatchlistResponse>
}
