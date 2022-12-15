package com.blockchain.home.data

import com.blockchain.coincore.AssetFilter
import com.blockchain.coincore.Coincore
import com.blockchain.coincore.SingleAccount
import com.blockchain.data.DataResource
import com.blockchain.home.domain.HomeAccountsService
import com.blockchain.store.mapData
import com.blockchain.unifiedcryptowallet.domain.balances.UnifiedBalancesService
import com.blockchain.walletmode.WalletMode
import io.reactivex.rxjava3.core.Single
import java.lang.IllegalStateException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.rx3.await

class HomeAccountsRepository(
    private val coincore: Coincore,
    private val unifiedBalancesService: UnifiedBalancesService,
) : HomeAccountsService {
    override fun accounts(walletMode: WalletMode): Flow<DataResource<List<SingleAccount>>> {
        return when (walletMode) {
            WalletMode.CUSTODIAL_ONLY -> activeCustodialWallets()
            WalletMode.NON_CUSTODIAL_ONLY -> activeNonCustodialWallets()
            else -> throw IllegalStateException("Wallet mode is not supported")
        }
    }

    private fun activeNonCustodialWallets(): Flow<DataResource<List<SingleAccount>>> {
        val activeAssets = unifiedBalancesService.balances().mapData {
            it.map { balance ->
                coincore[balance.currency]
            }.toSet()
        }

        return activeAssets.mapData { assetsDataRes ->
            Single.just(assetsDataRes).flattenAsObservable { it }.flatMapMaybe { asset ->
                asset.accountGroup(AssetFilter.NonCustodial).map { grp -> grp.accounts }
            }.reduce { a, l -> a + l }.switchIfEmpty(Single.just(emptyList())).await()
        }
    }

    private fun activeCustodialWallets() = coincore.activeWalletsInMode(WalletMode.CUSTODIAL_ONLY).map { it.accounts }
        .map {
            DataResource.Data(it) as DataResource<List<SingleAccount>>
        }.onStart {
            emit(DataResource.Loading)
        }.catch {
            emit(DataResource.Error(it as Exception))
        }
}
