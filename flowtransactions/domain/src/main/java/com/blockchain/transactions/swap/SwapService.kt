package com.blockchain.transactions.swap

import com.blockchain.coincore.AccountBalance
import com.blockchain.coincore.CryptoAccount
import com.blockchain.data.DataResource
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.FiatCurrency
import info.blockchain.balance.Money
import kotlinx.coroutines.flow.Flow

interface SwapService {
    fun sourceAccounts(): Flow<DataResource<List<CryptoAccount>>>

    fun custodialSourceAccountsWithBalances(): Flow<List<DataResource<CryptoAccountWithBalance>>>

    data class CryptoAccountWithBalance(
        val account: CryptoAccount,
        val balanceCrypto: Money,
        val balanceFiat: Money
    )
}