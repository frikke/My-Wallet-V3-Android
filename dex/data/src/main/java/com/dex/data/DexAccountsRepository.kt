package com.dex.data

import com.blockchain.coincore.AccountBalance
import com.blockchain.coincore.Coincore
import com.blockchain.coincore.CryptoAccount
import com.blockchain.core.price.ExchangeRatesDataManager
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.walletmode.WalletMode
import com.dex.domain.DexAccount
import com.dex.domain.DexAccountsService
import info.blockchain.balance.isLayer2Token
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.scan

class DexAccountsRepository(
    private val coincore: Coincore,
    private val currencyPrefs: CurrencyPrefs,
    private val exchangeRate: ExchangeRatesDataManager
) :
    DexAccountsService {
    override fun sourceAccounts(): Flow<List<DexAccount>> {
        return coincore.activeWalletsInMode(WalletMode.NON_CUSTODIAL).map {
            it.accounts
        }.map {
            it.filterIsInstance<CryptoAccount>()
                .filter { account -> account.currency.isLayer2Token }
        }.distinctUntilChanged { old, new ->
            old.map { it.currency.networkTicker } == new.map { it.currency.networkTicker }
        }.flatMapLatest { cryptoAccounts ->
            cryptoAccounts.map { account ->
                account.balance().map { balance -> account to balance }
            }.merge().scan(emptyMap<CryptoAccount, AccountBalance>()) { acc, (account, balance) ->
                acc
                    .filterKeys { it.currency.networkTicker != account.currency.networkTicker }
                    .plus(account to balance)
            }.filter {
                it.keys.map { it.currency.networkTicker }.toSet() == cryptoAccounts.map { it.currency.networkTicker }
                    .toSet()
            }.map { balancedaccounts ->
                balancedaccounts.map { (account, balance) ->
                    DexAccount(
                        account = account,
                        currency = account.currency,
                        balance = balance.total,
                        fiatBalance = balance.totalFiat,
                    )
                }
            }
        }
    }
}
