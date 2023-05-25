package com.blockchain.transactions.sell

import com.blockchain.coincore.CryptoAccount
import com.blockchain.coincore.FiatAccount
import com.blockchain.core.limits.TxLimits
import com.blockchain.data.DataResource
import com.blockchain.domain.transactions.TransferDirection
import com.blockchain.outcome.Outcome
import com.blockchain.transactions.common.CryptoAccountWithBalance
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.FiatCurrency
import kotlinx.coroutines.flow.Flow

interface SellService {
    fun sourceAccountsWithBalances(): Flow<DataResource<List<CryptoAccountWithBalance>>>

    suspend fun highestBalanceSourceAccount(): CryptoAccountWithBalance?

    suspend fun bestSourceAccountForTarget(targetAccount: FiatAccount): CryptoAccountWithBalance?

    suspend fun bestTargetAccount(): FiatAccount?

    suspend fun isTargetAccountValidForSource(
        sourceAccount: CryptoAccount,
        targetAccount: FiatAccount
    ): Boolean

    fun targetAccounts(sourceAccount: CryptoAccount): Flow<DataResource<List<FiatAccount>>>

    /**
     * returns [TxLimits] which defines min and max
     * needs to be exchanged later to fiat if needed
     */
    suspend fun limits(
        from: CryptoCurrency,
        to: FiatCurrency,
        fiat: FiatCurrency,
        direction: TransferDirection,
    ): Outcome<Exception, TxLimits>
}
