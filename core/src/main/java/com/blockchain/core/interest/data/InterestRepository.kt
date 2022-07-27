package com.blockchain.core.interest.data

import com.blockchain.api.services.InterestBalanceDetails
import com.blockchain.core.interest.domain.InterestService
import com.blockchain.core.interest.domain.model.InterestAccountBalance
import com.blockchain.nabu.Authenticator
import com.blockchain.nabu.service.NabuService
import com.blockchain.refreshstrategy.RefreshStrategy
import com.blockchain.store.getDataOrThrow
import com.blockchain.store.mapData
import com.blockchain.store.toStoreRequest
import info.blockchain.balance.AssetCatalogue
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.Currency
import info.blockchain.balance.Money
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.rx3.asObservable

internal class InterestRepository(
    private val assetCatalogue: AssetCatalogue,
    private val interestStore: InterestStore,
    private val nabuService: NabuService,
    private val authenticator: Authenticator
) : InterestService {

    private fun getBalancesFlow(refreshStrategy: RefreshStrategy): Flow<Map<AssetInfo, InterestAccountBalance>> {
        return interestStore.stream(refreshStrategy.toStoreRequest())
            .mapData { interestBalanceDetailList ->
                interestBalanceDetailList.mapNotNull { interestBalanceDetails ->
                    (assetCatalogue.fromNetworkTicker(interestBalanceDetails.assetTicker) as? AssetInfo)
                        ?.let { assetInfo -> assetInfo to interestBalanceDetails.toInterestBalance(assetInfo) }
                }.toMap()
            }
            .getDataOrThrow()
    }

    override fun getBalances(refreshStrategy: RefreshStrategy): Observable<Map<AssetInfo, InterestAccountBalance>> {
        return getBalancesFlow(refreshStrategy)
            .asObservable()
            .onErrorReturn { emptyMap() }
    }

    override fun getBalanceFor(asset: AssetInfo, refreshStrategy: RefreshStrategy): Observable<InterestAccountBalance> {
        return getBalancesFlow(refreshStrategy)
            .asObservable()
            .onErrorReturn { emptyMap() }
            .map { it.getOrDefault(asset, zeroBalance(asset)) }
    }

    override fun getActiveAssets(refreshStrategy: RefreshStrategy): Flow<Set<AssetInfo>> {
        return getBalancesFlow(refreshStrategy)
            .map { it.keys }
    }

    override fun getEnabledStatusForAllAssets(): Single<List<AssetInfo>> {
        return authenticator.authenticate { token ->
            nabuService.getInterestEnabled(token).map { instrumentsResponse ->
                instrumentsResponse.networkTickers.mapNotNull { networkTicker ->
                    assetCatalogue.assetInfoFromNetworkTicker(networkTicker)
                }
            }
        }
    }
}

private fun InterestBalanceDetails.toInterestBalance(asset: AssetInfo) =
    InterestAccountBalance(
        totalBalance = CryptoValue.fromMinor(asset, totalBalance),
        pendingInterest = CryptoValue.fromMinor(asset, pendingInterest),
        pendingDeposit = CryptoValue.fromMinor(asset, pendingDeposit),
        totalInterest = CryptoValue.fromMinor(asset, totalInterest),
        lockedBalance = CryptoValue.fromMinor(asset, lockedBalance),
        hasTransactions = true
    )

private fun zeroBalance(asset: Currency): InterestAccountBalance =
    InterestAccountBalance(
        totalBalance = Money.zero(asset),
        pendingInterest = Money.zero(asset),
        pendingDeposit = Money.zero(asset),
        totalInterest = Money.zero(asset),
        lockedBalance = Money.zero(asset)
    )
