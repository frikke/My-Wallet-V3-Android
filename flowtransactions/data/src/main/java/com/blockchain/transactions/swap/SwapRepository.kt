package com.blockchain.transactions.swap

import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.Coincore
import com.blockchain.coincore.CryptoAccount
import com.blockchain.data.DataResource
import com.blockchain.nabu.datamanagers.repositories.swap.CustodialRepository
import com.blockchain.transactions.swap.SwapService.CryptoAccountWithBalance
import com.blockchain.utils.toFlowDataResource
import info.blockchain.balance.CurrencyPair
import io.reactivex.rxjava3.kotlin.zipWith
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

internal class SwapRepository(
    private val coincore: Coincore,
    private val custodialRepository: CustodialRepository,
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

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun custodialSourceAccountsWithBalances(): Flow<List<DataResource<CryptoAccountWithBalance>>> {
        return sourceAccounts()
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
}
