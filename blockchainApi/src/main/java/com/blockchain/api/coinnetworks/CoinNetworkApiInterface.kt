package com.blockchain.api.coinnetworks

import com.blockchain.api.coinnetworks.data.CoinNetworkResponse
import com.blockchain.outcome.Outcome
import retrofit2.http.GET

interface CoinNetworkApiInterface {
    // When calling this method remember to filter out NetworkType.NOT_SUPPORTED
    @GET("network-config/")
    suspend fun getCoinNetworks(): Outcome<Exception, CoinNetworkResponse>
}
