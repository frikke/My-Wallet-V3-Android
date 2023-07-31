package com.dex.domain

import com.blockchain.coincore.impl.CryptoNonCustodialAccount
import com.blockchain.outcome.Outcome
import info.blockchain.balance.CoinNetwork
import kotlinx.coroutines.flow.Flow

interface DexAccountsService {
    fun sourceAccounts(chainId: Int): Flow<List<DexAccount>>
    fun destinationAccounts(chainId: Int): Flow<List<DexAccount>>
    suspend fun defSourceAccount(coinNetwork: CoinNetwork): DexAccount
    suspend fun nativeNetworkAccount(coinNetwork: CoinNetwork): Outcome<Exception, CryptoNonCustodialAccount>
    suspend fun defDestinationAccount(chainId: Int, source: DexAccount): DexAccount?
    fun updatePersistedDestinationAccount(dexAccount: DexAccount)
}

interface DexQuotesService {
    suspend fun quote(
        dexQuoteParams: DexQuoteParams
    ): Outcome<DexTxError, DexQuote>
}
