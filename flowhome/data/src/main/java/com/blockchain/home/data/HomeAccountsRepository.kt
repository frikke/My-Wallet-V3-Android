package com.blockchain.home.data

import com.blockchain.coincore.AssetFilter
import com.blockchain.coincore.Coincore
import com.blockchain.coincore.SingleAccount
import com.blockchain.data.DataResource
import com.blockchain.data.FreshnessStrategy
import com.blockchain.data.mapData
import com.blockchain.data.onErrorReturn
import com.blockchain.home.domain.HomeAccountsService
import com.blockchain.unifiedcryptowallet.domain.balances.FailedNetworkState
import com.blockchain.unifiedcryptowallet.domain.balances.UnifiedBalancesService
import com.blockchain.walletmode.WalletMode
import io.reactivex.rxjava3.core.Single
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.rx3.await

class HomeAccountsRepository(
    private val coincore: Coincore,
    private val unifiedBalancesService: UnifiedBalancesService
) : HomeAccountsService {

    override fun failedNetworks(
        walletMode: WalletMode,
        freshnessStrategy: FreshnessStrategy
    ): Flow<DataResource<FailedNetworkState>> {
        return when (walletMode) {
            WalletMode.CUSTODIAL -> flowOf(DataResource.Data(FailedNetworkState.None))
            WalletMode.NON_CUSTODIAL -> unifiedBalancesService.failedNetworks(freshnessStrategy = freshnessStrategy)
        }
    }

    override fun accounts(
        walletMode: WalletMode,
        freshnessStrategy: FreshnessStrategy
    ): Flow<DataResource<List<SingleAccount>>> {
        return when (walletMode) {
            WalletMode.CUSTODIAL -> activeCustodialWallets(freshnessStrategy)
            WalletMode.NON_CUSTODIAL -> activeNonCustodialWallets(freshnessStrategy)
        }
    }

    private fun activeNonCustodialWallets(
        freshnessStrategy: FreshnessStrategy
    ): Flow<DataResource<List<SingleAccount>>> {
        val activeAssets = unifiedBalancesService.balances(freshnessStrategy = freshnessStrategy).mapData {
            it.map { balance ->
                coincore[balance.currency]
            }.toSet()
        }

        return activeAssets.mapData { assetsDataRes ->
            Single.just(assetsDataRes).flattenAsObservable { it }.flatMapMaybe { asset ->
                asset.accountGroup(AssetFilter.NonCustodial).map { grp -> grp.accounts }
            }.reduce { a, l -> a + l }.switchIfEmpty(Single.just(emptyList())).await()
        }.onErrorReturn {
            emptyList()
        }
    }

    private fun activeCustodialWallets(freshnessStrategy: FreshnessStrategy) =
        coincore.activeWalletsInMode(WalletMode.CUSTODIAL, freshnessStrategy).map { it.accounts }
            .map {
                DataResource.Data(it)
            }.onErrorReturn {
                emptyList()
            }
}
