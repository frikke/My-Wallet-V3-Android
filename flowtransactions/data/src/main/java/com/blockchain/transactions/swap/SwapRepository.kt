package com.blockchain.transactions.swap

import com.blockchain.coincore.AccountBalance
import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.Coincore
import com.blockchain.coincore.CryptoAccount
import com.blockchain.coincore.SingleAccount
import com.blockchain.coincore.toUserFiat
import com.blockchain.data.DataResource
import com.blockchain.data.flatMap
import com.blockchain.data.map
import com.blockchain.nabu.datamanagers.repositories.swap.CustodialRepository
import com.blockchain.store.flatMapData
import com.blockchain.store.mapData
import com.blockchain.store.mapListData
import com.blockchain.transactions.swap.SwapService.CryptoAccountWithBalance
import com.blockchain.utils.toFlowDataResource
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CurrencyPair
import info.blockchain.balance.FiatCurrency
import io.reactivex.rxjava3.kotlin.zipWith
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.transform

internal class SwapRepository(
    private val coincore: Coincore,
    private val custodialRepository: CustodialRepository,
) : SwapService {

    override fun custodialSourceAccounts(): Flow<DataResource<List<CryptoAccount>>> {
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

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun custodialSourceAccountsWithBalances(): Flow<List<DataResource<CryptoAccountWithBalance>>> {
        return custodialSourceAccounts()
            .filterIsInstance<DataResource.Data<List<CryptoAccount>>>()
            .flatMapLatest { cryptoAccountsData ->
                combine(cryptoAccountsData.data.map { account ->
                    account.balance()
                        .distinctUntilChanged()
                        .map { balance ->
                            DataResource.Data(
                                CryptoAccountWithBalance(
                                    account = account,
                                    balanceCrypto = balance.total,
                                    balanceFiat = balance.totalFiat
                                )
                            )
                        }
                }) {
                    it.toList()
                }
            }
    }




    private fun CryptoAccount.isAvailableToSwapFrom(pairs: List<CurrencyPair>): Boolean =
        pairs.any { it.source == this.currency }

    override suspend fun fiatToCrypto(
        fiatValue: String,
        fiatCurrency: FiatCurrency,
        cryptoCurrency: CryptoCurrency
    ): String {
        return (fiatValue.toDouble() / 2).toString()
    }

    override suspend fun cryptoToFiat(
        cryptoValue: String,
        fiatCurrency: FiatCurrency,
        cryptoCurrency: CryptoCurrency
    ): String {
        return (cryptoValue.toDouble() * 2).toString()
    }
}