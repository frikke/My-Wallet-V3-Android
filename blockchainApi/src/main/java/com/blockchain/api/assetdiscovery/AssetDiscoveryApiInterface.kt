package com.blockchain.api.assetdiscovery

import com.blockchain.api.assetdiscovery.data.DynamicCurrencyList
import io.reactivex.rxjava3.core.Single

import retrofit2.http.GET

internal interface AssetDiscoveryApiInterface {

    // You may assume these to be unique by "symbol"
    @GET("assets/currencies/coin")
    fun getCurrencies(): Single<DynamicCurrencyList>

    // You may assume these to be unique by "symbol"
    @GET("assets/currencies/fiat")
    fun getFiatCurrencies(): Single<DynamicCurrencyList>

    @GET("assets/currencies/erc20")
    fun getErc20Currencies(): Single<DynamicCurrencyList>

    @GET("assets/currencies/custodial")
    fun getCustodialCurrencies(): Single<DynamicCurrencyList>
}
