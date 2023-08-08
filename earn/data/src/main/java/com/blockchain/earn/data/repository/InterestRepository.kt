package com.blockchain.earn.data.repository

import com.blockchain.api.earn.AssetsWithEligibility
import com.blockchain.api.earn.EarnRewardsEligibilityDto
import com.blockchain.api.earn.IneligibleReason
import com.blockchain.api.earn.passive.InterestApiService
import com.blockchain.api.earn.passive.data.InterestAccountBalanceDto
import com.blockchain.api.earn.passive.data.InterestWithdrawalBodyDto
import com.blockchain.core.history.data.datasources.PaymentTransactionHistoryStore
import com.blockchain.core.price.historic.HistoricRateFetcher
import com.blockchain.data.DataResource
import com.blockchain.data.FreshnessStrategy
import com.blockchain.data.FreshnessStrategy.Companion.withKey
import com.blockchain.data.RefreshStrategy
import com.blockchain.data.asSingle
import com.blockchain.data.doOnData
import com.blockchain.data.filterNotLoading
import com.blockchain.data.flatMapData
import com.blockchain.data.getDataOrThrow
import com.blockchain.data.mapData
import com.blockchain.data.onErrorReturn
import com.blockchain.data.toObservable
import com.blockchain.domain.eligibility.model.EarnRewardsEligibility
import com.blockchain.earn.data.dataresources.interest.InterestAvailableAssetsStore
import com.blockchain.earn.data.dataresources.interest.InterestBalancesStore
import com.blockchain.earn.data.dataresources.interest.InterestEligibilityStore
import com.blockchain.earn.data.dataresources.interest.InterestLimitsStore
import com.blockchain.earn.data.dataresources.interest.InterestRateForAllStore
import com.blockchain.earn.data.dataresources.interest.InterestRateStore
import com.blockchain.earn.data.mapper.toEarnRewardsActivity
import com.blockchain.earn.domain.models.EarnRewardsActivity
import com.blockchain.earn.domain.models.interest.InterestAccountBalance
import com.blockchain.earn.domain.models.interest.InterestLimits
import com.blockchain.earn.domain.service.InterestService
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.utils.fromIso8601ToUtc
import com.blockchain.utils.toLocalTime
import info.blockchain.balance.AssetCatalogue
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.Currency
import info.blockchain.balance.Money
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import java.util.Calendar
import java.util.Date
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

internal class InterestRepository(
    private val assetCatalogue: AssetCatalogue,
    private val interestBalancesStore: InterestBalancesStore,
    private val interestEligibilityStore: InterestEligibilityStore,
    private val interestAvailableAssetsStore: InterestAvailableAssetsStore,
    private val interestLimitsStore: InterestLimitsStore,
    private val interestRateStore: InterestRateStore,
    private val interestAllRatesStore: InterestRateForAllStore,
    private val paymentTransactionHistoryStore: PaymentTransactionHistoryStore,
    private val currencyPrefs: CurrencyPrefs,
    private val interestApiService: InterestApiService,
    private val historicRateFetcher: HistoricRateFetcher
) : InterestService {

    // balances
    override fun getBalancesFlow(
        refreshStrategy: FreshnessStrategy
    ): Flow<DataResource<Map<AssetInfo, InterestAccountBalance>>> {
        return interestBalancesStore.stream(refreshStrategy)
            .mapData { mapAssetTickerWithBalance ->
                mapAssetTickerWithBalance.mapNotNull { (assetTicker, balanceDto) ->
                    (assetCatalogue.fromNetworkTicker(assetTicker) as? AssetInfo)?.let { asset ->
                        asset to balanceDto.toInterestBalance(asset)
                    }
                }.toMap()
            }.onErrorReturn {
                emptyMap()
            }
    }

    override fun getBalances(refreshStrategy: FreshnessStrategy): Observable<Map<AssetInfo, InterestAccountBalance>> {
        return getBalancesFlow(refreshStrategy)
            .toObservable()
            .onErrorReturn { emptyMap() }
    }

    override fun getBalanceFor(
        asset: AssetInfo,
        refreshStrategy: FreshnessStrategy
    ): Observable<InterestAccountBalance> {
        return getBalancesFlow(refreshStrategy)
            .toObservable()
            .onErrorReturn { emptyMap() }
            .map { it.getOrDefault(asset, zeroBalance(asset)) }
    }

    override fun getBalanceForFlow(
        asset: AssetInfo,
        refreshStrategy: FreshnessStrategy
    ): Flow<DataResource<InterestAccountBalance>> {
        return getBalancesFlow(refreshStrategy)
            .mapData { it.getOrDefault(asset, zeroBalance(asset)) }
    }

    override fun getActiveAssets(refreshStrategy: FreshnessStrategy): Flow<Set<AssetInfo>> {
        return getBalancesFlow(refreshStrategy)
            .getDataOrThrow()
            .map { it.keys }
    }

    // availability
    override fun getAvailableAssetsForInterest(): Single<List<AssetInfo>> {
        return getAvailableAssetsForInterestFlow()
            .toObservable().firstOrError()
    }

    private var interestAvailableAssets: List<AssetInfo> = emptyList()
    override fun getAvailableAssetsForInterestFlow(): Flow<DataResource<List<AssetInfo>>> {
        return if (interestAvailableAssets.isNotEmpty()) {
            flowOf(DataResource.Data(interestAvailableAssets))
        } else interestAvailableAssetsStore.stream(FreshnessStrategy.Cached(RefreshStrategy.RefreshIfStale))
            .mapData { response ->
                response.networkTickers.mapNotNull { networkTicker ->
                    assetCatalogue.assetInfoFromNetworkTicker(networkTicker)
                }
            }.doOnData {
                interestAvailableAssets = it
            }
    }

    override fun isAssetAvailableForInterest(asset: AssetInfo): Single<Boolean> {
        return getAvailableAssetsForInterest()
            .map { assets -> asset.networkTicker in assets.map { it.networkTicker } }
            .onErrorResumeNext { Single.just(false) }
    }

    override fun isAssetAvailableForInterestFlow(
        asset: AssetInfo
    ): Flow<DataResource<Boolean>> {
        return getAvailableAssetsForInterestFlow()
            .mapData { assets -> assets.contains(asset) }
    }

    // eligibility
    override fun getEligibilityForAssetsLegacy(): Single<Map<AssetInfo, EarnRewardsEligibility>> {
        return getEligibilityForAssets()
            .toObservable().firstOrError()
    }

    override fun getEligibilityForAssets(
        refreshStrategy: FreshnessStrategy
    ): Flow<DataResource<Map<AssetInfo, EarnRewardsEligibility>>> {
        fun String.toIneligibilityReason(): EarnRewardsEligibility.Ineligible {
            return when {
                this == EarnRewardsEligibilityDto.UNSUPPORTED_REGION -> EarnRewardsEligibility.Ineligible.REGION
                this == EarnRewardsEligibilityDto.INVALID_ADDRESS -> EarnRewardsEligibility.Ineligible.REGION
                this == EarnRewardsEligibilityDto.TIER_TOO_LOW -> EarnRewardsEligibility.Ineligible.KYC_TIER
                else -> EarnRewardsEligibility.Ineligible.OTHER
            }
        }

        return interestEligibilityStore.stream(refreshStrategy).mapData { mapAssetTicketWithEligibility ->
            assetCatalogue.supportedCustodialAssets.associateWith { asset ->
                val eligibilityDto = when (mapAssetTicketWithEligibility) {
                    is AssetsWithEligibility ->
                        mapAssetTicketWithEligibility.assets[asset.networkTicker.uppercase()]
                            ?: EarnRewardsEligibilityDto.default()

                    is IneligibleReason -> mapAssetTicketWithEligibility.eligibility
                }

                if (eligibilityDto.isEligible) {
                    EarnRewardsEligibility.Eligible
                } else {
                    eligibilityDto.reason.toIneligibilityReason()
                }
            }
        }
    }

    override fun getEligibilityForAsset(asset: AssetInfo): Single<EarnRewardsEligibility> {
        return getEligibilityForAssetsLegacy().map { mapAssetWithEligibility ->
            mapAssetWithEligibility[asset] ?: EarnRewardsEligibility.Ineligible.default()
        }
    }

    override fun getEligibilityForAssetFlow(
        asset: AssetInfo,
        refreshStrategy: FreshnessStrategy
    ): Flow<DataResource<EarnRewardsEligibility>> {
        return getEligibilityForAssets(refreshStrategy)
            .mapData { mapAssetWithEligibility ->
                mapAssetWithEligibility[asset] ?: EarnRewardsEligibility.Ineligible.default()
            }
    }

    // limits
    override fun getLimitsForAssets(): Single<Map<AssetInfo, InterestLimits>> {
        return getLimitsForAssetsFlow()
            .toObservable().firstOrError()
    }

    override fun getLimitsForAssetsFlow(
        refreshStrategy: FreshnessStrategy
    ): Flow<DataResource<Map<AssetInfo, InterestLimits>>> {
        return interestLimitsStore.stream(refreshStrategy).mapData { interestLimits ->
            interestLimits.limits.entries.mapNotNull { (assetTicker, limits) ->
                assetCatalogue.assetInfoFromNetworkTicker(assetTicker)?.let { asset ->

                    val calendar = Calendar.getInstance().apply {
                        set(Calendar.DAY_OF_MONTH, 1)
                        add(Calendar.MONTH, 1)
                    }

                    val minDepositFiatValue = Money.fromMinor(
                        currencyPrefs.selectedFiatCurrency,
                        limits.minDepositAmount.toBigInteger()
                    )
                    val maxWithdrawalFiatValue = Money.fromMinor(
                        currencyPrefs.selectedFiatCurrency,
                        limits.maxWithdrawalAmount.toBigInteger()
                    )

                    val interestLimit = InterestLimits(
                        interestLockUpDuration = limits.lockUpDuration,
                        nextInterestPayment = calendar.time,
                        minDepositFiatValue = minDepositFiatValue,
                        maxWithdrawalFiatValue = maxWithdrawalFiatValue
                    )

                    Pair(asset, interestLimit)
                }
            }.toMap()
        }
    }

    override fun getLimitsForAsset(asset: AssetInfo): Single<InterestLimits> {
        return getLimitsForAssets().map { mapAssetWithLimits ->
            mapAssetWithLimits[asset] ?: throw NoSuchElementException("Unable to get limits for ${asset.networkTicker}")
        }
    }

    override fun getLimitsForAssetFlow(
        asset: AssetInfo,
        refreshStrategy: FreshnessStrategy
    ): Flow<DataResource<InterestLimits>> {
        return getLimitsForAssetsFlow(refreshStrategy)
            .mapData { mapAssetWithLimits ->
                mapAssetWithLimits[asset]
                    ?: throw NoSuchElementException("Unable to get limits for ${asset.networkTicker}")
            }
    }

    // rate
    override fun getInterestRate(asset: AssetInfo): Single<Double> {
        return getInterestRateFlow(asset)
            .toObservable().firstOrError()
    }

    override fun getInterestRateFlow(asset: AssetInfo, refreshStrategy: FreshnessStrategy): Flow<DataResource<Double>> {
        return interestRateStore.stream(refreshStrategy.withKey(InterestRateStore.Key(asset.networkTicker)))
            .mapData { interestRateResponse -> interestRateResponse.rate }
    }

    override fun getAllInterestRates(
        refreshStrategy: FreshnessStrategy
    ): Flow<DataResource<Map<AssetInfo, Double>>> =
        interestAllRatesStore.stream(refreshStrategy).mapData { rates ->
            rates.rates.mapNotNull { (ticker, rateDto) ->
                (assetCatalogue.fromNetworkTicker(ticker) as? AssetInfo)?.let { asset ->
                    asset to rateDto.rate
                }
            }.toMap()
        }

    // address
    override fun getAddress(asset: AssetInfo): Single<String> {
        return interestApiService.getAddress(asset.networkTicker)
            .map { interestAddressResponse -> interestAddressResponse.address }
    }

    // activity
    override fun getActivity(asset: AssetInfo): Single<List<EarnRewardsActivity>> {
        return getActivityFlow(asset).asSingle()
    }

    override fun getActivityFlow(
        asset: AssetInfo,
        refreshStrategy: FreshnessStrategy
    ): Flow<DataResource<List<EarnRewardsActivity>>> {
        return paymentTransactionHistoryStore
            .stream(
                refreshStrategy.withKey(
                    PaymentTransactionHistoryStore.Key(product = INTEREST_PRODUCT_NAME, type = null)
                )
            )
            .mapData { interestActivityResponse ->
                interestActivityResponse.items
                    .filter { transaction ->
                        assetCatalogue.fromNetworkTicker(
                            transaction.amount.symbol
                        )?.networkTicker == asset.networkTicker
                    }
            }
            .flatMapData {
                if (it.isEmpty()) {
                    flowOf(DataResource.Data(emptyList()))
                } else {
                    val flows = it.map { transaction ->
                        historicRateFetcher.fetch(
                            asset,
                            currencyPrefs.selectedFiatCurrency,
                            (transaction.insertedAt.fromIso8601ToUtc()?.toLocalTime() ?: Date()).time,
                            CryptoValue.fromMinor(asset, transaction.amountMinor.toBigInteger())
                        ).filterNotLoading().map {
                            transaction to it
                        }
                    }
                    combine(flows) {
                        it.map { (transaction, money) ->
                            transaction.toEarnRewardsActivity(asset, (money as? DataResource.Data)?.data)
                        }
                    }.map {
                        DataResource.Data(it)
                    }
                }
            }
    }

    // withdrawal
    override fun withdraw(asset: AssetInfo, amount: Money, address: String): Completable {
        return interestApiService.performWithdrawal(
            body = InterestWithdrawalBodyDto(
                withdrawalAddress = address,
                amount = amount.toBigInteger().toString(),
                currency = asset.networkTicker
            )
        )
    }

    override fun markBalancesAsStale() {
        interestBalancesStore.markAsStale()
    }

    // ///////////////
    // EXTENSIONS
    // ///////////////

    private fun InterestAccountBalanceDto.toInterestBalance(asset: AssetInfo) =
        InterestAccountBalance(
            totalBalance = CryptoValue.fromMinor(asset, totalBalance.toBigInteger()),
            pendingInterest = CryptoValue.fromMinor(asset, pendingInterest.toBigInteger()),
            pendingDeposit = CryptoValue.fromMinor(asset, pendingDeposit.toBigInteger()),
            totalInterest = CryptoValue.fromMinor(asset, totalInterest.toBigInteger()),
            lockedBalance = CryptoValue.fromMinor(asset, lockedBalance.toBigInteger()),
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

    companion object {
        const val INTEREST_PRODUCT_NAME = "savings"
    }
}
