package com.blockchain.core.chains.dynamicselfcustody

import com.blockchain.api.adapters.ApiException
import com.blockchain.core.chains.dynamicselfcustody.domain.model.NonCustodialAccountBalance
import com.blockchain.core.chains.dynamicselfcustody.domain.model.NonCustodialDerivedAddress
import com.blockchain.core.chains.dynamicselfcustody.domain.model.NonCustodialTxHistoryItem
import com.blockchain.outcome.Outcome

interface NonCustodialService {

    suspend fun authenticate(): Outcome<ApiException, Boolean>

    suspend fun subscribe(currency: String, label: String, addresses: List<String>): Outcome<ApiException, Boolean>

    suspend fun unsubscribe(currency: String): Outcome<ApiException, Boolean>

    suspend fun getSubscriptions(): Outcome<ApiException, List<String>>

    suspend fun getBalances(currencies: List<String>): Outcome<ApiException, List<NonCustodialAccountBalance>>

    suspend fun getAddresses(currencies: List<String>): Outcome<ApiException, List<NonCustodialDerivedAddress>>

    suspend fun getTransactionHistory(
        currency: String,
        contractAddress: String?
    ): Outcome<ApiException, List<NonCustodialTxHistoryItem>>
}
