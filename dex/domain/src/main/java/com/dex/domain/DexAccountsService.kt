package com.dex.domain

import com.blockchain.outcome.Outcome
import kotlinx.coroutines.flow.Flow

interface DexAccountsService {
    fun sourceAccounts(): Flow<List<DexAccount>>
    fun destinationAccounts(): Flow<List<DexAccount>>
    suspend fun defSourceAccount(): DexAccount?
    suspend fun defDestinationAccount(): DexAccount?
    fun updatePersistedDestinationAccount(dexAccount: DexAccount)
}

interface DexQuotesService {
    suspend fun quote(
        dexQuoteParams: DexQuoteParams
    ): Outcome<Exception, DexQuote>
}
