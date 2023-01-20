package com.blockchain.earn.data.repository

import com.blockchain.api.staking.StakingApiService
import com.blockchain.api.staking.data.StakingBalanceDto
import com.blockchain.api.staking.data.StakingEligibilityDto
import com.blockchain.core.history.data.datasources.PaymentTransactionHistoryStore
import com.blockchain.core.price.historic.HistoricRateFetcher
import com.blockchain.data.DataResource
import com.blockchain.data.FreshnessStrategy
import com.blockchain.data.FreshnessStrategy.Companion.withKey
import com.blockchain.domain.eligibility.model.StakingEligibility
import com.blockchain.earn.data.dataresources.staking.StakingBalanceStore
import com.blockchain.earn.data.dataresources.staking.StakingEligibilityStore
import com.blockchain.earn.data.dataresources.staking.StakingLimitsStore
import com.blockchain.earn.data.dataresources.staking.StakingRatesStore
import com.blockchain.earn.domain.models.staking.EarnRewardsFrequency
import com.blockchain.earn.domain.models.staking.StakingAccountBalance
import com.blockchain.earn.domain.models.staking.StakingActivity
import com.blockchain.earn.domain.models.staking.StakingActivityAttributes
import com.blockchain.earn.domain.models.staking.StakingLimits
import com.blockchain.earn.domain.models.staking.StakingRates
import com.blockchain.earn.domain.models.staking.StakingState
import com.blockchain.earn.domain.models.staking.StakingTransactionBeneficiary
import com.blockchain.earn.domain.service.StakingService
import com.blockchain.nabu.models.responses.simplebuy.TransactionAttributesResponse
import com.blockchain.nabu.models.responses.simplebuy.TransactionResponse
import com.blockchain.outcome.fold
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
import info.blockchain.balance.Money
import info.blockchain.wallet.multiaddress.TransactionSummary
import io.reactivex.rxjava3.core.Single
import java.util.Date
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
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
    private val historicRateFetcher: HistoricRateFetcher
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
    ): Flow<DataResource<StakingRates>> =
        stakingRatesStore.stream(refreshStrategy).mapData { ratesMap ->
            ratesMap.rates[currency.networkTicker]?.let { rateData ->
                StakingRates(
                    rate = rateData.rate,
                    commission = rateData.commission
                )
            } ?: StakingRates(0.0, 0.0)
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
        flow {
            emitAll(
                stakingBalanceStore.stream(refreshStrategy)
                    .getDataOrThrow().map {
                        it.keys.map { assetTicker ->
                            assetCatalogue.fromNetworkTicker(assetTicker) as? AssetInfo
                                ?: throw IllegalStateException(
                                    "Failed mapping unknown asset $assetTicker for Staking"
                                )
                        }.toSet()
                    }
            )
        }

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
    ): Flow<DataResource<StakingEligibility>> =
        stakingEligibilityStore.stream(refreshStrategy).mapData { eligibilityMap ->
            eligibilityMap[currency.networkTicker]?.let { eligibility ->
                if (eligibility.isEligible) {
                    StakingEligibility.Eligible
                } else {
                    eligibility.reason.toIneligibilityReason()
                }
            } ?: StakingEligibility.Ineligible.default()
        }

    override fun getEligibilityForAssets(
        refreshStrategy: FreshnessStrategy
    ): Flow<DataResource<Map<AssetInfo, StakingEligibility>>> =
        stakingEligibilityStore.stream(refreshStrategy).mapData { eligibilityMap ->
            eligibilityMap.mapNotNull { (assetTicker, eligibilityDto) ->
                (assetCatalogue.fromNetworkTicker(assetTicker) as? AssetInfo)?.let { asset ->
                    asset to if (eligibilityDto.isEligible) {
                        StakingEligibility.Eligible
                    } else {
                        eligibilityDto.reason.toIneligibilityReason()
                    }
                }
            }.toMap()
        }

    override fun getStakingEligibility(
        refreshStrategy: FreshnessStrategy
    ): Flow<DataResource<StakingEligibility>> {
        return stakingEligibilityStore.stream(refreshStrategy).mapData { eligibilityMap ->
            if (eligibilityMap.values.any { it.isEligible }) {
                StakingEligibility.Eligible
            } else {
                StakingEligibility.Ineligible.default()
            }
        }
    }

    override fun getActivity(
        asset: AssetInfo,
        refreshStrategy: FreshnessStrategy
    ): Flow<DataResource<List<StakingActivity>>> {
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
                            transaction.toStakingActivity(asset, (money as? DataResource.Data)?.data)
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

    override suspend fun getAccountAddress(currency: Currency): DataResource<String> =
        stakingApi.getAccountAddress(currency.networkTicker).fold(
            onSuccess = {
                DataResource.Data(it.address)
            },
            onFailure = {
                DataResource.Error(it)
            }
        )

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

    private fun String.toRewardsFrequency(): EarnRewardsFrequency =
        when (this) {
            DAILY -> EarnRewardsFrequency.Daily
            WEEKLY -> EarnRewardsFrequency.Weekly
            MONTHLY -> EarnRewardsFrequency.Monthly
            else -> EarnRewardsFrequency.Unknown
        }

    private fun TransactionResponse.toStakingActivity(currency: Currency, fiatValue: Money?): StakingActivity =
        StakingActivity(
            value = CryptoValue.fromMinor(currency as AssetInfo, amountMinor.toBigInteger()),
            id = id,
            insertedAt = insertedAt.fromIso8601ToUtc()?.toLocalTime() ?: Date(),
            state = state.toStakingState(),
            type = type.toTransactionType(),
            extraAttributes = extraAttributes?.toDomain(),
            fiatValue = fiatValue
        )

    private fun String.toTransactionType() =
        when (this) {
            TransactionResponse.DEPOSIT -> TransactionSummary.TransactionType.DEPOSIT
            TransactionResponse.WITHDRAWAL -> TransactionSummary.TransactionType.WITHDRAW
            TransactionResponse.INTEREST_OUTGOING -> TransactionSummary.TransactionType.INTEREST_EARNED
            else -> TransactionSummary.TransactionType.UNKNOWN
        }

    private fun String.toStakingState(): StakingState =
        when (this) {
            TransactionResponse.FAILED -> StakingState.FAILED
            TransactionResponse.REJECTED -> StakingState.REJECTED
            TransactionResponse.PROCESSING -> StakingState.PROCESSING
            TransactionResponse.CREATED,
            TransactionResponse.COMPLETE -> StakingState.COMPLETE
            TransactionResponse.PENDING -> StakingState.PENDING
            TransactionResponse.MANUAL_REVIEW -> StakingState.MANUAL_REVIEW
            TransactionResponse.CLEARED -> StakingState.CLEARED
            TransactionResponse.REFUNDED -> StakingState.REFUNDED
            else -> StakingState.UNKNOWN
        }

    private fun TransactionAttributesResponse.toDomain() = StakingActivityAttributes(
        address = address,
        confirmations = confirmations,
        hash = hash,
        id = id,
        transactionHash = txHash,
        transferType = transferType,
        beneficiary = StakingTransactionBeneficiary(
            beneficiary?.accountRef,
            beneficiary?.user
        )
    )

    private fun StakingBalanceDto.toStakingBalance(currency: Currency): StakingAccountBalance =
        StakingAccountBalance(
            totalBalance = Money.fromMinor(currency, totalBalance.toBigInteger()),
            lockedBalance = Money.fromMinor(currency, lockedBalance.toBigInteger()),
            pendingDeposit = Money.fromMinor(currency, bondingDeposits.toBigInteger()),
            pendingWithdrawal = Money.fromMinor(currency, unbondingWithdrawals.toBigInteger()),
            totalRewards = Money.fromMinor(currency, totalRewards.toBigInteger())
        )

    private fun String.toIneligibilityReason(): StakingEligibility.Ineligible {
        return when {
            this.isEmpty() -> StakingEligibility.Ineligible.NONE
            this == StakingEligibilityDto.DEFAULT_REASON_NONE -> StakingEligibility.Ineligible.NONE
            this == StakingEligibilityDto.UNSUPPORTED_REGION -> StakingEligibility.Ineligible.REGION
            this == StakingEligibilityDto.INVALID_ADDRESS -> StakingEligibility.Ineligible.REGION
            this == StakingEligibilityDto.TIER_TOO_LOW -> StakingEligibility.Ineligible.KYC_TIER
            else -> StakingEligibility.Ineligible.OTHER
        }
    }

    companion object {
        private const val STAKING_PRODUCT_NAME = "STAKING"

        private const val DAILY = "Daily"
        private const val WEEKLY = "Weekly"
        private const val MONTHLY = "Monthly"
    }
}
