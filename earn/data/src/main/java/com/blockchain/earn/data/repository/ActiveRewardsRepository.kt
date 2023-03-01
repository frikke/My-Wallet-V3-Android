package com.blockchain.earn.data.repository

import com.blockchain.api.earn.active.ActiveRewardsApiService
import com.blockchain.api.earn.active.data.ActiveRewardsBalanceDto
import com.blockchain.core.history.data.datasources.PaymentTransactionHistoryStore
import com.blockchain.core.price.historic.HistoricRateFetcher
import com.blockchain.data.DataResource
import com.blockchain.data.FreshnessStrategy
import com.blockchain.data.FreshnessStrategy.Companion.withKey
import com.blockchain.domain.eligibility.model.EarnRewardsEligibility
import com.blockchain.earn.data.dataresources.active.ActiveRewardsBalanceStore
import com.blockchain.earn.data.dataresources.active.ActiveRewardsEligibilityStore
import com.blockchain.earn.data.dataresources.active.ActiveRewardsLimitsStore
import com.blockchain.earn.data.dataresources.active.ActiveRewardsRatesStore
import com.blockchain.earn.data.mapper.toEarnRewardsActivity
import com.blockchain.earn.data.mapper.toIneligibilityReason
import com.blockchain.earn.domain.models.ActiveRewardsRates
import com.blockchain.earn.domain.models.EarnRewardsActivity
import com.blockchain.earn.domain.models.EarnRewardsFrequency.Companion.toRewardsFrequency
import com.blockchain.earn.domain.models.active.ActiveRewardsAccountBalance
import com.blockchain.earn.domain.models.active.ActiveRewardsLimits
import com.blockchain.earn.domain.service.ActiveRewardsService
import com.blockchain.featureflag.FeatureFlag
import com.blockchain.outcome.Outcome
import com.blockchain.outcome.map
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.store.filterNotLoading
import com.blockchain.store.flatMapData
import com.blockchain.store.getDataOrThrow
import com.blockchain.store.mapData
import com.blockchain.utils.fromIso8601ToUtc
import com.blockchain.utils.toLocalTime
import info.blockchain.balance.AssetCatalogue
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.Currency
import info.blockchain.balance.FiatCurrency
import info.blockchain.balance.Money
import java.util.Date
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

class ActiveRewardsRepository(
    private val activeRewardsRateStore: ActiveRewardsRatesStore,
    private val activeRewardsEligibilityStore: ActiveRewardsEligibilityStore,
    private val activeRewardsBalanceStore: ActiveRewardsBalanceStore,
    private val assetCatalogue: AssetCatalogue,
    private val activeRewardsFeatureFlag: FeatureFlag,
    private val paymentTransactionHistoryStore: PaymentTransactionHistoryStore,
    private val activeRewardsLimitsStore: ActiveRewardsLimitsStore,
    private val currencyPrefs: CurrencyPrefs,
    private val activeRewardsApi: ActiveRewardsApiService,
    private val historicRateFetcher: HistoricRateFetcher,
) : ActiveRewardsService {

    // we use the rates endpoint to determine whether the user has access to staking cryptos
    override fun getAvailabilityForAsset(
        currency: Currency,
        refreshStrategy: FreshnessStrategy
    ): Flow<DataResource<Boolean>> =
        activeRewardsRateStore.stream(refreshStrategy).mapData {
            it.rates.containsKey(currency.networkTicker)
        }

    override fun getRatesForAsset(
        currency: Currency,
        refreshStrategy: FreshnessStrategy
    ): Flow<DataResource<ActiveRewardsRates>> =
        activeRewardsRateStore.stream(refreshStrategy).mapData { ratesMap ->
            ratesMap.rates[currency.networkTicker]?.let { rateData ->
                ActiveRewardsRates(
                    rate = rateData.rate,
                    commission = rateData.commission,
                    triggerPrice = Money.fromMinor(
                        FiatCurrency.Dollars,
                        rateData.triggerPrice.toBigInteger()
                    )
                )
            } ?: ActiveRewardsRates(0.0, 0.0, Money.zero(currency))
        }

    override fun getRatesForAllAssets(
        refreshStrategy: FreshnessStrategy
    ): Flow<DataResource<Map<AssetInfo, Double>>> =
        activeRewardsRateStore.stream(refreshStrategy).mapData { rates ->
            rates.rates.mapNotNull { (ticker, rateDto) ->
                (assetCatalogue.fromNetworkTicker(ticker) as? AssetInfo)?.let { asset ->
                    asset to rateDto.rate
                }
            }.toMap()
        }

    override fun getActiveAssets(refreshStrategy: FreshnessStrategy): Flow<Set<AssetInfo>> =
        flow {
            if (activeRewardsFeatureFlag.coEnabled()) {
                emitAll(
                    activeRewardsBalanceStore.stream(refreshStrategy)
                        .getDataOrThrow().map {
                            it.keys.map { assetTicker ->
                                assetCatalogue.fromNetworkTicker(assetTicker) as? AssetInfo
                                    ?: throw IllegalStateException(
                                        "Failed mapping unknown asset $assetTicker for Staking"
                                    )
                            }.toSet()
                        }
                )
            } else {
                emit(emptySet())
            }
        }

    override fun getBalanceForAsset(
        currency: Currency,
        refreshStrategy: FreshnessStrategy
    ): Flow<DataResource<ActiveRewardsAccountBalance>> =
        activeRewardsBalanceStore.stream(refreshStrategy).mapData { assetDataMap ->
            assetDataMap[currency.networkTicker]?.toBalance(currency) ?: ActiveRewardsAccountBalance.zeroBalance(
                currency
            )
        }

    override fun getBalanceForAllAssets(
        refreshStrategy: FreshnessStrategy
    ): Flow<DataResource<Map<AssetInfo, ActiveRewardsAccountBalance>>> =
        activeRewardsBalanceStore.stream(refreshStrategy).mapData { mapAssetTickerWithBalance ->
            mapAssetTickerWithBalance.mapNotNull { (assetTicker, balanceDto) ->
                (assetCatalogue.fromNetworkTicker(assetTicker) as? AssetInfo)?.let { asset ->
                    asset to balanceDto.toBalance(asset)
                }
            }.toMap()
        }

    override fun getEligibilityForAsset(
        currency: Currency,
        refreshStrategy: FreshnessStrategy
    ): Flow<DataResource<EarnRewardsEligibility>> =
        activeRewardsEligibilityStore.stream(refreshStrategy).mapData { eligibilityMap ->
            eligibilityMap[currency.networkTicker]?.let { eligibility ->
                if (eligibility.isEligible) {
                    EarnRewardsEligibility.Eligible
                } else {
                    eligibility.reason.toIneligibilityReason()
                }
            } ?: EarnRewardsEligibility.Ineligible.default()
        }

    override fun getEligibilityForAssets(
        refreshStrategy: FreshnessStrategy
    ): Flow<DataResource<Map<AssetInfo, EarnRewardsEligibility>>> =
        activeRewardsEligibilityStore.stream(refreshStrategy).mapData { eligibilityMap ->
            eligibilityMap.mapNotNull { (assetTicker, eligibilityDto) ->
                (assetCatalogue.fromNetworkTicker(assetTicker) as? AssetInfo)?.let { asset ->
                    asset to if (eligibilityDto.isEligible) {
                        EarnRewardsEligibility.Eligible
                    } else {
                        eligibilityDto.reason.toIneligibilityReason()
                    }
                }
            }.toMap()
        }

    override fun getEarnRewardsEligibility(
        refreshStrategy: FreshnessStrategy
    ): Flow<DataResource<EarnRewardsEligibility>> {
        return activeRewardsEligibilityStore.stream(refreshStrategy).mapData { eligibilityMap ->
            if (eligibilityMap.values.any { it.isEligible }) {
                EarnRewardsEligibility.Eligible
            } else {
                EarnRewardsEligibility.Ineligible.default()
            }
        }
    }

    override fun getActivity(
        asset: AssetInfo,
        refreshStrategy: FreshnessStrategy
    ): Flow<DataResource<List<EarnRewardsActivity>>> {
        return paymentTransactionHistoryStore
            .stream(
                refreshStrategy.withKey(
                    PaymentTransactionHistoryStore.Key(product = AR_PRODUCT_NAME, type = null)
                )
            )
            .mapData { arActivityResponse ->
                arActivityResponse.items
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

    override fun getLimitsForAllAssets(
        refreshStrategy: FreshnessStrategy
    ): Flow<DataResource<Map<AssetInfo, ActiveRewardsLimits>>> =
        activeRewardsLimitsStore.stream(refreshStrategy).mapData { arLimits ->
            arLimits.limits.entries.mapNotNull { (assetTicker, limits) ->
                assetCatalogue.assetInfoFromNetworkTicker(assetTicker)?.let { asset ->

                    val minDepositFiatValue = Money.fromMinor(
                        currencyPrefs.selectedFiatCurrency,
                        limits.minDepositValue.toBigInteger()
                    )

                    val arLimit = ActiveRewardsLimits(
                        minDepositValue = minDepositFiatValue,
                        bondingDays = limits.bondingDays,
                        unbondingDays = limits.unbondingDays ?: 0,
                        withdrawalsDisabled = limits.disabledWithdrawals ?: false,
                        rewardsFrequency = limits.rewardFrequency.toRewardsFrequency()
                    )

                    Pair(asset, arLimit)
                }
            }.toMap()
        }

    override suspend fun getAccountAddress(currency: Currency): Outcome<Exception, String> =
        activeRewardsApi.getAccountAddress(currency.networkTicker).map { it.address }

    override fun getLimitsForAsset(
        asset: AssetInfo,
        refreshStrategy: FreshnessStrategy
    ): Flow<DataResource<ActiveRewardsLimits>> =
        getLimitsForAllAssets(refreshStrategy).mapData { mapAssetWithLimits ->
            mapAssetWithLimits[asset] ?: throw NoSuchElementException("Unable to get limits for ${asset.networkTicker}")
        }

    private fun ActiveRewardsBalanceDto.toBalance(currency: Currency): ActiveRewardsAccountBalance =
        ActiveRewardsAccountBalance(
            totalBalance = Money.fromMinor(currency, totalBalance.toBigInteger()),
            lockedBalance = Money.fromMinor(currency, lockedBalance.toBigInteger()),
            pendingDeposit = Money.fromMinor(currency, bondingDeposits.toBigInteger()),
            pendingWithdrawal = Money.fromMinor(currency, unbondingWithdrawals.toBigInteger()),
            totalRewards = Money.fromMinor(currency, totalRewards.toBigInteger()),
            earningBalance = Money.fromMinor(currency, earningBalance.toBigInteger()),
            bondingDeposits = Money.fromMinor(currency, bondingDeposits.toBigInteger()),
        )

    companion object {
        private const val AR_PRODUCT_NAME = "EARN_CC1W"

        private const val DAILY = "Daily"
        private const val WEEKLY = "Weekly"
        private const val MONTHLY = "Monthly"
    }
}
