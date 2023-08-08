package com.blockchain.earn.data.repository

import com.blockchain.api.earn.staking.StakingApiService
import com.blockchain.api.earn.staking.data.StakingBalanceDto
import com.blockchain.api.earn.staking.data.StakingDepositActivityDto
import com.blockchain.api.earn.staking.data.StakingWithdrawalActivityDto
import com.blockchain.core.history.data.datasources.PaymentTransactionHistoryStore
import com.blockchain.core.price.historic.HistoricRateFetcher
import com.blockchain.data.DataResource
import com.blockchain.data.FreshnessStrategy
import com.blockchain.data.FreshnessStrategy.Companion.withKey
import com.blockchain.data.filterNotLoading
import com.blockchain.data.flatMapData
import com.blockchain.data.getDataOrThrow
import com.blockchain.data.mapData
import com.blockchain.data.onErrorReturn
import com.blockchain.domain.eligibility.model.EarnRewardsEligibility
import com.blockchain.earn.data.dataresources.staking.StakingBalanceStore
import com.blockchain.earn.data.dataresources.staking.StakingBondingStore
import com.blockchain.earn.data.dataresources.staking.StakingEligibilityStore
import com.blockchain.earn.data.dataresources.staking.StakingLimitsStore
import com.blockchain.earn.data.dataresources.staking.StakingRatesStore
import com.blockchain.earn.data.mapper.toEarnRewardsActivity
import com.blockchain.earn.data.mapper.toIneligibilityReason
import com.blockchain.earn.domain.models.EarnRewardsActivity
import com.blockchain.earn.domain.models.EarnRewardsFrequency.Companion.toRewardsFrequency
import com.blockchain.earn.domain.models.StakingRewardsRates
import com.blockchain.earn.domain.models.staking.StakingAccountBalance
import com.blockchain.earn.domain.models.staking.StakingActivity
import com.blockchain.earn.domain.models.staking.StakingActivityType
import com.blockchain.earn.domain.models.staking.StakingLimits
import com.blockchain.earn.domain.service.StakingService
import com.blockchain.outcome.Outcome
import com.blockchain.outcome.fold
import com.blockchain.outcome.map
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.utils.fromIso8601ToUtc
import com.blockchain.utils.toLocalTime
import info.blockchain.balance.AssetCatalogue
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.Currency
import info.blockchain.balance.Money
import io.reactivex.rxjava3.core.Single
import java.util.Date
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.rx3.rxSingle

class StakingRepository(
    private val stakingRatesStore: StakingRatesStore,
    private val stakingEligibilityStore: StakingEligibilityStore,
    private val stakingBalanceStore: StakingBalanceStore,
    private val assetCatalogue: AssetCatalogue,
    private val paymentTransactionHistoryStore: PaymentTransactionHistoryStore,
    private val stakingLimitsStore: StakingLimitsStore,
    private val currencyPrefs: CurrencyPrefs,
    private val stakingApi: StakingApiService,
    private val historicRateFetcher: HistoricRateFetcher,
    private val stakingBondingStore: StakingBondingStore
) : StakingService {

    // we use the rates endpoint to determine whether the user has access to staking cryptos
    override fun getAvailabilityForAsset(
        currency: Currency,
        refreshStrategy: FreshnessStrategy
    ): Flow<DataResource<Boolean>> =
        stakingRatesStore.stream(refreshStrategy).mapData {
            it.rates.containsKey(currency.networkTicker)
        }

    override fun getRatesForAsset(
        currency: Currency,
        refreshStrategy: FreshnessStrategy
    ): Flow<DataResource<StakingRewardsRates>> =
        stakingRatesStore.stream(refreshStrategy).mapData { ratesMap ->
            ratesMap.rates[currency.networkTicker]?.let { rateData ->
                StakingRewardsRates(
                    rate = rateData.rate,
                    commission = rateData.commission
                )
            } ?: StakingRewardsRates(0.0, 0.0)
        }

    override fun getRatesForAllAssets(
        refreshStrategy: FreshnessStrategy
    ): Flow<DataResource<Map<AssetInfo, Double>>> =
        stakingRatesStore.stream(refreshStrategy).mapData { rates ->
            rates.rates.mapNotNull { (ticker, rateDto) ->
                (assetCatalogue.fromNetworkTicker(ticker) as? AssetInfo)?.let { asset ->
                    asset to rateDto.rate
                }
            }.toMap()
        }

    override fun getActiveAssets(refreshStrategy: FreshnessStrategy): Flow<Set<AssetInfo>> =
        stakingBalanceStore.stream(refreshStrategy).filterNotLoading().mapData {
            it.keys.mapNotNull { assetTicker ->
                assetCatalogue.fromNetworkTicker(assetTicker) as? AssetInfo
            }.toSet()
        }.onErrorReturn {
            emptySet()
        }.getDataOrThrow()

    override fun getBalanceForAsset(
        currency: Currency,
        refreshStrategy: FreshnessStrategy
    ): Flow<DataResource<StakingAccountBalance>> =
        stakingBalanceStore.stream(refreshStrategy).mapData { assetDataMap ->
            assetDataMap[currency.networkTicker]?.toStakingBalance(currency) ?: StakingAccountBalance.zeroBalance(
                currency
            )
        }

    override fun getBalanceForAllAssets(
        refreshStrategy: FreshnessStrategy
    ): Flow<DataResource<Map<AssetInfo, StakingAccountBalance>>> =
        stakingBalanceStore.stream(refreshStrategy).mapData { mapAssetTickerWithBalance ->
            mapAssetTickerWithBalance.mapNotNull { (assetTicker, balanceDto) ->
                (assetCatalogue.fromNetworkTicker(assetTicker) as? AssetInfo)?.let { asset ->
                    asset to balanceDto.toStakingBalance(asset)
                }
            }.toMap()
        }

    override fun getEligibilityForAsset(
        currency: Currency,
        refreshStrategy: FreshnessStrategy
    ): Flow<DataResource<EarnRewardsEligibility>> =
        stakingEligibilityStore.stream(refreshStrategy).mapData { eligibilityMap ->
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
        stakingEligibilityStore.stream(refreshStrategy).mapData { eligibilityMap ->
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

    override fun getStakingEligibility(
        refreshStrategy: FreshnessStrategy
    ): Flow<DataResource<EarnRewardsEligibility>> {
        return stakingEligibilityStore.stream(refreshStrategy).mapData { eligibilityMap ->
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
                    PaymentTransactionHistoryStore.Key(product = STAKING_PRODUCT_NAME, type = null)
                )
            )
            .mapData { stakingActivityResponse ->
                stakingActivityResponse.items
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
    ): Flow<DataResource<Map<AssetInfo, StakingLimits>>> =
        stakingLimitsStore.stream(refreshStrategy).mapData { stakingLimits ->
            stakingLimits.limits.entries.mapNotNull { (assetTicker, limits) ->
                assetCatalogue.assetInfoFromNetworkTicker(assetTicker)?.let { asset ->

                    val minDepositFiatValue = Money.fromMinor(
                        currencyPrefs.selectedFiatCurrency,
                        limits.minDepositValue.toBigInteger()
                    )

                    val stakingLimit = StakingLimits(
                        minDepositValue = minDepositFiatValue,
                        bondingDays = limits.bondingDays,
                        unbondingDays = limits.unbondingDays ?: 0,
                        withdrawalsDisabled = limits.disabledWithdrawals ?: false,
                        rewardsFrequency = limits.rewardFrequency.toRewardsFrequency()
                    )

                    Pair(asset, stakingLimit)
                }
            }.toMap()
        }

    override suspend fun getAccountAddress(currency: Currency): Outcome<Exception, String> =
        stakingApi.getAccountAddress(currency.networkTicker).map {
            it.address
        }

    override fun getAccountAddressRx(currency: Currency): Single<String> =
        rxSingle {
            stakingApi.getAccountAddress(currency.networkTicker).fold(
                onSuccess = {
                    it.address
                },
                onFailure = {
                    throw it
                }
            )
        }

    override fun getLimitsForAsset(
        asset: AssetInfo,
        refreshStrategy: FreshnessStrategy
    ): Flow<DataResource<StakingLimits>> =
        getLimitsForAllAssets(refreshStrategy).mapData { mapAssetWithLimits ->
            mapAssetWithLimits[asset] ?: throw NoSuchElementException("Unable to get limits for ${asset.networkTicker}")
        }

    override suspend fun getPendingActivity(
        currency: Currency,
        refreshStrategy: FreshnessStrategy
    ): Flow<DataResource<List<StakingActivity>>> =
        stakingBondingStore.stream(refreshStrategy.withKey(currency.networkTicker))
            .mapData {
                it.deposits.map { it.toStakingActivity(currency) } +
                    it.withdrawals.map { it.toStakingActivity(currency) }
            }

    private fun StakingBalanceDto.toStakingBalance(currency: Currency): StakingAccountBalance =
        StakingAccountBalance(
            totalBalance = Money.fromMinor(currency, totalBalance.toBigInteger()),
            lockedBalance = Money.fromMinor(currency, lockedBalance.toBigInteger()),
            pendingDeposit = Money.fromMinor(currency, bondingDeposits.toBigInteger()),
            pendingWithdrawal = Money.fromMinor(currency, unbondingWithdrawals.toBigInteger()),
            totalRewards = Money.fromMinor(currency, totalRewards.toBigInteger())
        )

    private fun StakingWithdrawalActivityDto.toStakingActivity(currency: Currency): StakingActivity =
        StakingActivity(
            product = product,
            currency = this.currency,
            amountCrypto = amount?.let { Money.fromMinor(currency, it.toBigInteger()) },
            startDate = unbondingStartDate?.let { it.fromIso8601ToUtc()?.toLocalTime() },
            expiryDate = unbondingExpiryDate?.let { it.fromIso8601ToUtc()?.toLocalTime() },
            durationDays = unbondingDays,
            type = StakingActivityType.Unbonding
        )

    private fun StakingDepositActivityDto.toStakingActivity(currency: Currency): StakingActivity =
        StakingActivity(
            product = product,
            currency = this.currency,
            amountCrypto = amount?.let { Money.fromMinor(currency, it.toBigInteger()) },
            startDate = bondingStartDate?.let { it.fromIso8601ToUtc()?.toLocalTime() },
            expiryDate = bondingExpiryDate?.let { it.fromIso8601ToUtc()?.toLocalTime() },
            durationDays = bondingDays,
            type = StakingActivityType.Bonding
        )

    companion object {
        private const val STAKING_PRODUCT_NAME = "STAKING"
    }
}
