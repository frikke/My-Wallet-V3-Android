package com.blockchain.transactions.swap

import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.Coincore
import com.blockchain.coincore.CryptoAccount
import com.blockchain.core.limits.LimitsDataManager
import com.blockchain.core.limits.TxLimits
import com.blockchain.data.DataResource
import com.blockchain.domain.paymentmethods.model.LegacyLimits
import com.blockchain.domain.transactions.TransferDirection
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.nabu.datamanagers.Product
import com.blockchain.nabu.datamanagers.repositories.swap.CustodialRepository
import com.blockchain.store.flatMapData
import com.blockchain.utils.toFlowDataResource
import info.blockchain.balance.AssetCategory
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CurrencyPair
import info.blockchain.balance.FiatCurrency
import info.blockchain.balance.asFiatCurrencyOrThrow
import io.reactivex.rxjava3.kotlin.zipWith
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

internal class SwapRepository(
    private val coincore: Coincore,
    private val custodialRepository: CustodialRepository,
    private val limitsDataManager: LimitsDataManager,
    private val walletManager: CustodialWalletManager,
) : SwapService {

    override fun sourceAccounts(): Flow<DataResource<List<CryptoAccount>>> {
        return coincore.walletsWithAction(
            action = AssetAction.Swap
        ).zipWith(
            custodialRepository.getSwapAvailablePairs()
        ).map { (accounts, pairs) ->
            accounts.filter { account ->
                (account as? CryptoAccount)?.isAvailableToSwapFrom(pairs) ?: false
            }
        }.map {
            it.map { account -> account as CryptoAccount }
        }.toFlowDataResource()
    }

    override fun custodialSourceAccountsWithBalances(): Flow<DataResource<List<CryptoAccountWithBalance>>> {
        return sourceAccounts()
            .flatMapData {
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
    }

    override suspend fun highestBalanceSourceAccount(): CryptoAccountWithBalance? {
        return custodialSourceAccountsWithBalances()
            .filterIsInstance<DataResource.Data<List<CryptoAccountWithBalance>>>()
            .map {
                it.data.forEach {
                    println("----- iiiiii ${it.account.currency.displayTicker} ---- ${it.balanceCrypto.toBigDecimal()}")
                    println("-----")
                }
                it
            }
            .map { it.data.maxBy { it.balanceCrypto.toBigDecimal() } }
            .firstOrNull()
    }

    override fun limits(
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
            sourceAccountType = TransferDirection.INTERNAL.sourceAccountType(),
            targetAccountType = TransferDirection.INTERNAL.targetAccountType()
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
