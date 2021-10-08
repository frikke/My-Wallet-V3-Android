package com.blockchain.api.assetdiscovery

import com.blockchain.network.interceptor.Cacheable
import com.blockchain.api.assetdiscovery.data.DynamicCurrencyList
import io.reactivex.rxjava3.core.Single

import retrofit2.http.GET

internal interface AssetDiscoveryApiInterface {

    @Cacheable(maxAge = Cacheable.MAX_AGE_THREE_DAYS)
    @GET("assets/currencies/coin")
    fun getCurrencies(): Single<DynamicCurrencyList>

    @Cacheable(maxAge = Cacheable.MAX_AGE_THREE_DAYS)
    @GET("assets/currencies/fiat")
    fun getFiatCurrencies(): Single<DynamicCurrencyList>

    @Cacheable(maxAge = Cacheable.MAX_AGE_THREE_DAYS)
    @GET("assets/currencies/erc20")
    fun getErc20Currencies(): Single<DynamicCurrencyList>

    @Cacheable(maxAge = Cacheable.MAX_AGE_THREE_DAYS)
    @GET("assets/currencies/custodial")
    fun getCustodialCurrencies(): Single<DynamicCurrencyList>
}
