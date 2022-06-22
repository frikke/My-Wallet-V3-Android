package com.blockchain.core.interest.data

import com.blockchain.api.services.InterestBalanceDetails
import com.blockchain.core.interest.InterestAccountBalance
import com.blockchain.core.interest.data.store.InterestDataSource
import com.blockchain.core.interest.domain.InterestStoreService
import com.blockchain.store.asObservable
import com.blockchain.store.mapData
import info.blockchain.balance.AssetCatalogue
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.Currency
import info.blockchain.balance.Money
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single

internal class InterestStoreRepository(
    private val assetCatalogue: AssetCatalogue,
    private val interestDataSource: InterestDataSource
) : InterestStoreService {

    private fun getBalances(refresh: Boolean): Observable<Map<AssetInfo, InterestAccountBalance>> {
        return interestDataSource.stream(refresh)
            .mapData { interestBalanceDetailList ->
                interestBalanceDetailList.mapNotNull { interestBalanceDetails ->
                    (assetCatalogue.fromNetworkTicker(interestBalanceDetails.assetTicker) as? AssetInfo)
                        ?.let { assetInfo -> assetInfo to interestBalanceDetails.toInterestBalance(assetInfo) }
                }.toMap()
            }
            .asObservable { it }
            .onErrorReturn { emptyMap() }
    }

    override fun getBalances(): Observable<Map<AssetInfo, InterestAccountBalance>> {
        return getBalances(refresh = true)
    }

    override fun getBalanceFor(asset: AssetInfo): Observable<InterestAccountBalance> {
        return getBalances(refresh = true)
            .map { it.getOrDefault(asset, zeroBalance(asset)) }
    }

    override fun getActiveAssets(): Single<Set<AssetInfo>> {
        return getBalances(refresh = false).map { it.keys }.firstElement().toSingle()
    }

    override fun invalidate() {
        interestDataSource.invalidate()
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
