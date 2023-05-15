package com.dex.domain

import com.blockchain.data.DataResource
import com.blockchain.outcome.Outcome
import com.dex.domain.models.DexChain
import kotlinx.coroutines.flow.Flow

interface DexAccountsService {
    fun sourceAccounts(): Flow<List<DexAccount>>
    fun destinationAccounts(): Flow<List<DexAccount>>
    suspend fun defSourceAccount(chainId: Int): DexAccount?
    suspend fun defDestinationAccount(chainId: Int, sourceTicker: String): DexAccount?
    fun updatePersistedDestinationAccount(dexAccount: DexAccount)
}

interface DexQuotesService {
    suspend fun quote(
        dexQuoteParams: DexQuoteParams
    ): Outcome<DexTxError, DexQuote>
}
