package com.blockchain.transactions.swap

import com.blockchain.coincore.AccountBalance
import com.blockchain.coincore.CryptoAccount
import com.blockchain.core.limits.TxLimits
import com.blockchain.data.DataResource
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.FiatCurrency
import info.blockchain.balance.Money
import kotlinx.coroutines.flow.Flow

interface SwapService {
    fun sourceAccounts(): Flow<DataResource<List<CryptoAccount>>>

    fun custodialSourceAccountsWithBalances(): Flow<DataResource<List<CryptoAccountWithBalance>>>

    suspend fun highestBalanceSourceAccount(): CryptoAccountWithBalance?

    /**
     * returns [TxLimits] which defines min and max
     * needs to be exchanged later to fiat if needed
     */
      fun limits(
        from: CryptoCurrency,
        to: CryptoCurrency,
        fiat: FiatCurrency
    ): Flow<DataResource<TxLimits>>
}