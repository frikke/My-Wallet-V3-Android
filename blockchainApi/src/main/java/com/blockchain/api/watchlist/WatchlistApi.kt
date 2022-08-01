package com.blockchain.api.watchlist

import com.blockchain.api.adapters.ApiException
import com.blockchain.api.watchlist.data.WatchlistBody
import com.blockchain.api.watchlist.data.WatchlistListResponse
import com.blockchain.api.watchlist.data.WatchlistResponse
import com.blockchain.outcome.Outcome
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.HTTP
import retrofit2.http.Header
import retrofit2.http.PUT

internal interface WatchlistApi {
    @GET("watchlist")
    suspend fun getWatchlist(
        @Header("authorization") authorization: String,
    ): Outcome<ApiException, WatchlistListResponse>

    // @DELETE annotation does not support having a body, so we need to define it like this instead
    @HTTP(method = "DELETE", path = "watchlist", hasBody = true)
    suspend fun removeFromWatchlist(
        @Header("authorization") authorization: String,
        @Body body: WatchlistBody
    ): Outcome<ApiException, Unit>

    @PUT("watchlist")
    suspend fun addToWatchlist(
        @Header("authorization") authorization: String,
        @Body body: WatchlistBody
    ): Outcome<ApiException, WatchlistResponse>
}
