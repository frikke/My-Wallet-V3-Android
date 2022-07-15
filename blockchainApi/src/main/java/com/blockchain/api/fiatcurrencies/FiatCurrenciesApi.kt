package com.blockchain.api.fiatcurrencies

import com.blockchain.api.adapters.ApiError
import com.blockchain.api.fiatcurrencies.data.SetSelectedTradingCurrencyRequest
import com.blockchain.outcome.Outcome
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.PUT

interface FiatCurrenciesApi {

    @PUT("users/current/currency")
    suspend fun setSelectedTradingCurrency(
        @Header("authorization") authorization: String,
        @Body request: SetSelectedTradingCurrencyRequest
    ): Outcome<ApiError, Unit>
}
