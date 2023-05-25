package com.blockchain.transactions.swap

import com.blockchain.coincore.CryptoAccount
import com.blockchain.coincore.impl.CryptoNonCustodialAccount
import com.blockchain.coincore.impl.CustodialTradingAccount
import com.blockchain.core.limits.TxLimits
import com.blockchain.data.DataResource
import com.blockchain.domain.transactions.TransferDirection
import com.blockchain.outcome.Outcome
import com.blockchain.transactions.common.CryptoAccountWithBalance
import com.blockchain.walletmode.WalletMode
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.FiatCurrency
import kotlinx.coroutines.flow.Flow

interface SwapService {
    fun sourceAccountsWithBalances(): Flow<DataResource<List<CryptoAccountWithBalance>>>

    suspend fun highestBalanceSourceAccount(): CryptoAccountWithBalance?

    suspend fun targetTickersForMode(
        sourceTicker: String,
        mode: WalletMode
    ): List<String>

    /**
     * returns the highest balance account of [targetTicker]
     * that is eligible for swap with [sourceTicker]
     *
     * [mode] defines which account type: [CustodialTradingAccount] or [CryptoNonCustodialAccount]
     *
     * needed for defi accounts like BTC or BCH where we can have multiple accounts
     *
     * quick tip: open coinview for btc or bch on defi and see how many accounts
     */
    suspend fun bestTargetAccountForMode(
        sourceTicker: String,
        targetTicker: String,
        mode: WalletMode
    ): CryptoAccount?

    /**
     * check if [targetAccount] is a valid target for [sourceTicker]
     */
    suspend fun isTargetAccountValidForSource(
        targetAccount: CryptoAccount,
        sourceTicker: String,
        mode: WalletMode
    ): Boolean

    /**
     * returns the [selectedAssetTicker] target accounts of [sourceTicker]
     */
    fun targetAccountsWithBalanceOfMode(
        sourceTicker: String,
        selectedAssetTicker: String,
        mode: WalletMode
    ): Flow<DataResource<List<CryptoAccountWithBalance>>>

    /**
     * returns [TxLimits] which defines min and max
     * needs to be exchanged later to fiat if needed
     */
    suspend fun limits(
        from: CryptoCurrency,
        to: CryptoCurrency,
        fiat: FiatCurrency,
        direction: TransferDirection,
    ): Outcome<Exception, TxLimits>
}
