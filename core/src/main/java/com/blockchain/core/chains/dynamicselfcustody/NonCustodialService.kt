package com.blockchain.core.chains.dynamicselfcustody

import com.blockchain.core.chains.dynamicselfcustody.domain.model.NonCustodialAccountBalance
import com.blockchain.core.chains.dynamicselfcustody.domain.model.NonCustodialDerivedAddress
import com.blockchain.core.chains.dynamicselfcustody.domain.model.NonCustodialTxHistoryItem
import com.blockchain.outcome.Outcome

interface NonCustodialService {

    suspend fun authenticate(): Outcome<Exception, Boolean>

    suspend fun subscribe(currency: String, label: String, addresses: List<String>): Outcome<Exception, Boolean>

    suspend fun unsubscribe(currency: String): Outcome<Exception, Boolean>

    suspend fun getSubscriptions(): Outcome<Exception, List<String>>

    suspend fun getBalances(currencies: List<String>): Outcome<Exception, List<NonCustodialAccountBalance>>

    suspend fun getAddresses(currencies: List<String>): Outcome<Exception, List<NonCustodialDerivedAddress>>

    suspend fun getTransactionHistory(
        currency: String,
        contractAddress: String?
    ): Outcome<Exception, List<NonCustodialTxHistoryItem>>
}
