package com.dex.domain

import com.blockchain.outcome.Outcome
import kotlinx.coroutines.flow.Flow

interface DexAccountsService {
    fun sourceAccounts(chainId: Int): Flow<List<DexAccount>>
    fun destinationAccounts(chainId: Int): Flow<List<DexAccount>>
    suspend fun defSourceAccount(chainId: Int): DexAccount?
    suspend fun defDestinationAccount(chainId: Int, source: DexAccount): DexAccount?
    fun updatePersistedDestinationAccount(dexAccount: DexAccount)
}

interface DexQuotesService {
    suspend fun quote(
        dexQuoteParams: DexQuoteParams
    ): Outcome<DexTxError, DexQuote>
}
