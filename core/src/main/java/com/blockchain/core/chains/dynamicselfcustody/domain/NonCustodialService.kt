package com.blockchain.core.chains.dynamicselfcustody.domain

import com.blockchain.api.adapters.ApiError
import com.blockchain.api.selfcustody.BuildTxResponse
import com.blockchain.api.selfcustody.PushTxResponse
import com.blockchain.core.chains.dynamicselfcustody.domain.model.NonCustodialAccountBalance
import com.blockchain.core.chains.dynamicselfcustody.domain.model.NonCustodialDerivedAddress
import com.blockchain.core.chains.dynamicselfcustody.domain.model.NonCustodialTxHistoryItem
import com.blockchain.core.chains.dynamicselfcustody.domain.model.TransactionSignature
import com.blockchain.outcome.Outcome
import com.blockchain.store.StoreRequest
import info.blockchain.balance.AssetInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.JsonObject

interface NonCustodialService {

    suspend fun authenticate(): Outcome<ApiError, Boolean>

    suspend fun subscribe(currency: String, label: String, addresses: List<String>): Outcome<ApiError, Boolean>

    suspend fun unsubscribe(currency: String): Outcome<ApiError, Boolean>

    fun getSubscriptions(
        request: StoreRequest = StoreRequest.Cached(forceRefresh = true)
    ): Flow<List<String>>

    suspend fun getBalances(currencies: List<String>): Outcome<ApiError, List<NonCustodialAccountBalance>>

    suspend fun getAddresses(currencies: List<String>): Outcome<ApiError, List<NonCustodialDerivedAddress>>

    suspend fun getTransactionHistory(
        currency: String,
        contractAddress: String?
    ): Outcome<ApiError, List<NonCustodialTxHistoryItem>>

    suspend fun buildTransaction(
        currency: String,
        accountIndex: Int = 0,
        type: String,
        transactionTarget: String,
        amount: String,
        fee: String,
        memo: String = "",
        feeCurrency: String = currency
    ): Outcome<ApiError, BuildTxResponse>

    fun getFeeCurrencyFor(asset: AssetInfo): AssetInfo

    suspend fun pushTransaction(
        currency: String,
        rawTx: JsonObject,
        signatures: List<TransactionSignature>
    ): Outcome<ApiError, PushTxResponse>
}
