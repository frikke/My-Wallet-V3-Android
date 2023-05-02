package com.blockchain.transactions.swap

import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.AssetFilter
import com.blockchain.coincore.Coincore
import com.blockchain.coincore.CryptoAccount
import com.blockchain.coincore.SingleAccount
import com.blockchain.coincore.impl.CryptoNonCustodialAccount
import com.blockchain.coincore.impl.CustodialTradingAccount
import com.blockchain.core.limits.LimitsDataManager
import com.blockchain.core.limits.TxLimits
import com.blockchain.data.DataResource
import com.blockchain.data.dataOrElse
import com.blockchain.domain.paymentmethods.model.LegacyLimits
import com.blockchain.domain.transactions.TransferDirection
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.nabu.datamanagers.Product
import com.blockchain.nabu.datamanagers.repositories.swap.CustodialRepository
import com.blockchain.store.filterListData
import com.blockchain.store.flatMapData
import com.blockchain.store.mapData
import com.blockchain.store.mapListData
import com.blockchain.utils.toFlowDataResource
import com.blockchain.walletmode.WalletMode
import info.blockchain.balance.AssetCategory
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CurrencyPair
import info.blockchain.balance.FiatCurrency
import info.blockchain.balance.asFiatCurrencyOrThrow
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.kotlin.zipWith
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

// TODO (othman) ref swap stuff to this repo

internal class SwapRepository(
    private val coincore: Coincore,
    private val custodialRepository: CustodialRepository,
    private val limitsDataManager: LimitsDataManager,
    private val walletManager: CustodialWalletManager,
) : SwapService {

    private fun Flow<DataResource<List<CryptoAccount>>>.withBalance() = flatMapData {
        combine(
            it.map { account ->
                account.balance()
                    .distinctUntilChanged()
                    .map { balance ->
                        CryptoAccountWithBalance(
                            account = account,
                            balanceCrypto = balance.total,
                            balanceFiat = balance.totalFiat
                        )
                    }
            }
        ) { DataResource.Data(it.toList()) }
    }

    private fun sourceAccounts(): Flow<DataResource<List<CryptoAccount>>> {
        return coincore.walletsWithAction(action = AssetAction.Swap)
            .map {
                it.filterIsInstance<CryptoAccount>()
            }
            .zipWith(
                custodialRepository.getSwapAvailablePairs()
            )
            .map { (accounts, pairs) ->
                accounts.filter { account ->
                    account.isAvailableToSwapFrom(pairs)
                }
            }
            .toFlowDataResource()
    }

    override fun sourceAccountsWithBalances(): Flow<DataResource<List<CryptoAccountWithBalance>>> {
        return sourceAccounts()
            .withBalance()
    }

    override suspend fun highestBalanceSourceAccount(): CryptoAccountWithBalance? {
        return sourceAccountsWithBalances()
            .filterIsInstance<DataResource.Data<List<CryptoAccountWithBalance>>>()
            .map { it.data.maxBy { it.balanceCrypto.toBigDecimal() } }
            .firstOrNull()
    }

    override suspend fun targetTickersForMode(
        sourceTicker: String,
        mode: WalletMode
    ): List<String> {
        return targetAccountsForMode(sourceTicker = sourceTicker, mode = mode)
            .mapListData {
                it.currency.networkTicker
            }
            .mapData {
                it.distinct()
            }
            .filter { it is DataResource.Data<List<String>> }
            .firstOrNull()?.dataOrElse(emptyList()) ?: emptyList()
    }

    private fun targetAccountsForMode(
        sourceTicker: String,
        mode: WalletMode
    ): Flow<DataResource<List<CryptoAccount>>> {
        return custodialRepository.getSwapAvailablePairs()
            .flatMap { pairs ->
                val targetCurrencies = pairs.filter { it.source.networkTicker == sourceTicker }
                    .map { it.destination }

                val assets = targetCurrencies.map { coincore[it] }

                Single.just(assets).flattenAsObservable { it }
                    .flatMapSingle { asset ->
                        asset.accountGroup(AssetFilter.All).map { it.accounts }.switchIfEmpty(Single.just(emptyList()))
                    }
                    .reduce { t1, t2 ->
                        t1 + t2
                    }
                    .switchIfEmpty(Single.just(emptyList()))
                    .map {
                        when (mode) {
                            WalletMode.CUSTODIAL -> it.filterIsInstance<CustodialTradingAccount>()
                            WalletMode.NON_CUSTODIAL -> it.filterIsInstance<CryptoNonCustodialAccount>()
                        }
                    }
            }.toFlowDataResource()
    }

    override suspend fun bestTargetAccountForMode(
        sourceTicker: String,
        targetTicker: String,
        mode: WalletMode
    ): CryptoAccount? {
        // 1. get all target accounts of sourceTicker for mode
        // 2. filter only accounts of targetTicker
        // 3. get their balances
        // 4. ignore loading/error
        // 5. get highest balance
        // 6. return or null if nothing found
        return targetAccountsForMode(sourceTicker = sourceTicker, mode = mode)
            .filterListData { it.currency.networkTicker == targetTicker }
            .withBalance()
            .filterIsInstance<DataResource.Data<List<CryptoAccountWithBalance>>>()
            .map { it.data.maxBy { it.balanceCrypto.toBigDecimal() }.account }
            .firstOrNull()
    }

    override suspend fun isAccountValidForSource(
        account: CryptoAccount,
        sourceTicker: String,
        mode: WalletMode
    ): Boolean {
        return targetAccountsForMode(sourceTicker = sourceTicker, mode = mode)
            .filterIsInstance<DataResource.Data<List<CryptoAccount>>>()
            .map { it.data.contains(account) }
            .firstOrNull() ?: false
    }

    override fun accountsWithBalanceOfMode(
        sourceTicker: String,
        selectedAssetTicker: String,
        mode: WalletMode
    ): Flow<DataResource<List<CryptoAccountWithBalance>>> {
        return custodialRepository.getSwapAvailablePairs().flatMap { pairs ->
            val targetCurrencies = pairs
                .filter { it.source.networkTicker == sourceTicker }
                .map { it.destination }

            val assets = targetCurrencies.map { coincore[it] }

            Single.just(assets).flattenAsObservable { it }.flatMapSingle { asset ->
                asset.accountGroup(AssetFilter.All).map { it.accounts }.switchIfEmpty(Single.just(emptyList()))
            }.reduce { t1, t2 ->
                t1 + t2
            }.switchIfEmpty(Single.just(emptyList()))
                .map {
                    it.filter { it.currency.networkTicker == selectedAssetTicker }
                }
                .map {
                    when (mode) {
                        WalletMode.CUSTODIAL -> it.filterIsInstance<CustodialTradingAccount>()
                        WalletMode.NON_CUSTODIAL -> it.filterIsInstance<CryptoNonCustodialAccount>()
                    }
                }
        }.toFlowDataResource()
            .withBalance()
    }

    override fun limits( // todo support defi
        from: CryptoCurrency,
        to: CryptoCurrency,
        fiat: FiatCurrency
    ): Flow<DataResource<TxLimits>> {
        return limitsDataManager.getLimits(
            outputCurrency = from,
            sourceCurrency = from,
            targetCurrency = to,
            legacyLimits = walletManager.getProductTransferLimits(
                fiat.asFiatCurrencyOrThrow(),
                Product.TRADE,
                TransferDirection.INTERNAL
            ).map { it as LegacyLimits },
            sourceAccountType = TransferDirection.INTERNAL.sourceAccountType(), // todo support defi
            targetAccountType = TransferDirection.INTERNAL.targetAccountType() // todo support defi
        ).toFlowDataResource()
    }

    private fun CryptoAccount.isAvailableToSwapFrom(pairs: List<CurrencyPair>): Boolean =
        pairs.any { it.source == this.currency }
}

private fun TransferDirection.sourceAccountType(): AssetCategory {
    return when (this) {
        TransferDirection.FROM_USERKEY,
        TransferDirection.ON_CHAIN -> AssetCategory.NON_CUSTODIAL
        TransferDirection.INTERNAL,
        TransferDirection.TO_USERKEY -> AssetCategory.CUSTODIAL
    }
}

private fun TransferDirection.targetAccountType(): AssetCategory {
    return when (this) {
        TransferDirection.TO_USERKEY,
        TransferDirection.ON_CHAIN -> AssetCategory.NON_CUSTODIAL
        TransferDirection.INTERNAL,
        TransferDirection.FROM_USERKEY -> AssetCategory.CUSTODIAL
    }
}

/**
 * When wallet is in Universal mode, you can swap from Trading to Trading, from PK to PK and from PK to Trading
 * In any other case, swap is only allowed to same Type accounts
 */
private fun SingleAccount.isTargetAvailableForSwap(
    target: CryptoAccount
): Boolean =
    if (this is CustodialTradingAccount) target is CustodialTradingAccount else true
