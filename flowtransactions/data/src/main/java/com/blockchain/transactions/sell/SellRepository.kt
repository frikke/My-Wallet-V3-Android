package com.blockchain.transactions.sell

import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.Coincore
import com.blockchain.coincore.CryptoAccount
import com.blockchain.coincore.FiatAccount
import com.blockchain.core.buy.domain.SimpleBuyService
import com.blockchain.core.limits.CUSTODIAL_LIMITS_ACCOUNT
import com.blockchain.core.limits.LimitsDataManager
import com.blockchain.core.limits.NON_CUSTODIAL_LIMITS_ACCOUNT
import com.blockchain.core.limits.TxLimits
import com.blockchain.data.DataResource
import com.blockchain.data.combineDataResourceFlows
import com.blockchain.data.firstOutcome
import com.blockchain.data.flatMapData
import com.blockchain.domain.fiatcurrencies.FiatCurrenciesService
import com.blockchain.domain.paymentmethods.model.LegacyLimits
import com.blockchain.domain.transactions.TransferDirection
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.nabu.datamanagers.Product
import com.blockchain.outcome.Outcome
import com.blockchain.outcome.getOrDefault
import com.blockchain.outcome.map
import com.blockchain.transactions.common.CryptoAccountWithBalance
import com.blockchain.utils.awaitOutcome
import com.blockchain.utils.toFlowDataResource
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.CurrencyPair
import info.blockchain.balance.FiatCurrency
import info.blockchain.balance.FiatValue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

internal class SellRepository(
    private val coincore: Coincore,
    private val limitsDataManager: LimitsDataManager,
    private val walletManager: CustodialWalletManager,
    private val simpleBuyService: SimpleBuyService,
    private val fiatCurrenciesService: FiatCurrenciesService,
) : SellService {

    private fun Flow<DataResource<List<CryptoAccount>>>.withBalance() = flatMapData {
        combine(
            it.map { account ->
                account.balance()
                    .distinctUntilChanged()
                    .map { balance ->
                        CryptoAccountWithBalance(
                            account = account,
                            balanceCrypto = balance.total as CryptoValue,
                            balanceFiat = balance.totalFiat as FiatValue,
                        )
                    }
            }
        ) { DataResource.Data(it.toList()) }
    }

    private fun sourceAccounts(forFiatCurrency: FiatCurrency? = null): Flow<DataResource<List<CryptoAccount>>> {
        val accountsEnabledForSell = combineDataResourceFlows(
            pairsEnabledForSell(forFiatCurrency),
            coincore.walletsWithAction(action = AssetAction.Sell).toFlowDataResource(),
        ) { pairs, accounts ->
            accounts.filterIsInstance<CryptoAccount>().filter { account ->
                account.currency.networkTicker in pairs.map { it.source.networkTicker }
            }
        }
        return accountsEnabledForSell
    }

    private fun pairsEnabledForSell(forFiatCurrency: FiatCurrency? = null): Flow<DataResource<List<CurrencyPair>>> {
        val fiatCurrency = fiatCurrenciesService.selectedTradingCurrency
        val availableFiats = walletManager.getSupportedFundsFiats(fiatCurrency)
            .map { DataResource.Data(it) }
            .catch { DataResource.Error(it as Exception) }

        val supportedCryptoCurrencies = simpleBuyService.getSupportedBuySellCryptoCurrencies()

        return combineDataResourceFlows(
            availableFiats,
            supportedCryptoCurrencies,
        ) { fiats, supportedCryptos ->
            supportedCryptos
                .filter {
                    fiats.contains(it.destination) &&
                        (forFiatCurrency == null || it.destination == forFiatCurrency)
                }
        }
    }

    override fun sourceAccountsWithBalances(): Flow<DataResource<List<CryptoAccountWithBalance>>> {
        return sourceAccounts()
            .withBalance()
    }

    override suspend fun bestSourceAccountForTarget(targetAccount: FiatAccount): CryptoAccountWithBalance? {
        return sourceAccounts(targetAccount.currency)
            .withBalance()
            .firstOutcome()
            .map { it.maxByOrNull { it.balanceFiat.toBigDecimal() } }
            .getOrDefault(null)
    }

    override suspend fun highestBalanceSourceAccount(): CryptoAccountWithBalance? {
        return sourceAccountsWithBalances()
            .filterIsInstance<DataResource.Data<List<CryptoAccountWithBalance>>>()
            .map { it.data.maxBy { it.balanceCrypto.toBigDecimal() } }
            .firstOrNull()
    }

    override suspend fun bestTargetAccount(): FiatAccount? {
        val fiatCurrency = fiatCurrenciesService.selectedTradingCurrency
        return coincore.allFiats().awaitOutcome()
            .map { accounts ->
                accounts.firstOrNull { account -> account.currency == fiatCurrency } as FiatAccount?
            }
            .getOrDefault(null)
    }

    override suspend fun isTargetAccountValidForSource(
        sourceAccount: CryptoAccount,
        targetAccount: FiatAccount
    ): Boolean {
        return targetAccounts(sourceAccount).firstOutcome()
            .map { accounts -> accounts.any { account -> account.matches(targetAccount) } }
            .getOrDefault(false)
    }

    override fun targetAccounts(sourceAccount: CryptoAccount): Flow<DataResource<List<FiatAccount>>> {
        return combineDataResourceFlows(
            pairsEnabledForSell(),
            coincore.getTransactionTargets(sourceAccount, AssetAction.Sell).toFlowDataResource()
        ) { assets, accountList ->
            val fiatAccounts = accountList.filterIsInstance(FiatAccount::class.java)
                .filter { account ->
                    assets.any { it.source == sourceAccount.currency && account.currency == it.destination }
                }
            fiatAccounts
        }
    }

    override suspend fun limits(
        from: CryptoCurrency,
        to: FiatCurrency,
        fiat: FiatCurrency,
        direction: TransferDirection,
    ): Outcome<Exception, TxLimits> {
        return limitsDataManager.getLimits(
            outputCurrency = from,
            sourceCurrency = from,
            targetCurrency = to,
            legacyLimits = walletManager.getProductTransferLimits(
                fiat,
                Product.SELL,
                direction
            ).map { it as LegacyLimits },
            sourceAccountType = direction.sourceAccountType(),
            targetAccountType = CUSTODIAL_LIMITS_ACCOUNT,
        ).awaitOutcome()
    }
}

private fun TransferDirection.sourceAccountType(): String {
    return when (this) {
        TransferDirection.FROM_USERKEY,
        TransferDirection.ON_CHAIN -> NON_CUSTODIAL_LIMITS_ACCOUNT
        TransferDirection.INTERNAL,
        TransferDirection.TO_USERKEY -> CUSTODIAL_LIMITS_ACCOUNT
    }
}
