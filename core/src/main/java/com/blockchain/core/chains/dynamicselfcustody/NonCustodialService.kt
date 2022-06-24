package com.blockchain.core.chains.dynamicselfcustody

import com.blockchain.api.adapters.ApiError
import com.blockchain.outcome.Outcome

interface NonCustodialService {

    suspend fun authenticate(): Outcome<ApiError, Boolean>

    suspend fun subscribe(currency: String, label: String, addresses: List<String>): Outcome<ApiError, Boolean>

    suspend fun unsubscribe(currency: String): Outcome<ApiError, Boolean>

    suspend fun getSubscriptions(): Outcome<ApiError, List<String>>

    suspend fun getBalances(currencies: List<String>): Outcome<ApiError, List<NonCustodialAccountBalance>>

    suspend fun getAddresses(currencies: List<String>): Outcome<ApiError, List<NonCustodialDerivedAddress>>

    suspend fun getTransactionHistory(
        currency: String,
        contractAddress: String?
    ): Outcome<ApiError, List<NonCustodialTxHistoryItem>>
}
