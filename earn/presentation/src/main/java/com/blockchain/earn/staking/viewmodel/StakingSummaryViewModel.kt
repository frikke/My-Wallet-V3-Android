package com.blockchain.earn.staking.viewmodel

import androidx.lifecycle.viewModelScope
import com.blockchain.coincore.Coincore
import com.blockchain.coincore.toUserFiat
import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.commonarch.presentation.mvi_v2.MviViewModel
import com.blockchain.core.price.ExchangeRatesDataManager
import com.blockchain.data.DataResource
import com.blockchain.data.combineDataResources
import com.blockchain.earn.domain.models.staking.StakingAccountBalance
import com.blockchain.earn.domain.models.staking.StakingLimits
import com.blockchain.earn.domain.service.StakingService
import com.blockchain.preferences.CurrencyPrefs
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.Currency
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize

class StakingSummaryViewModel(
    private val coincore: Coincore,
    private val stakingService: StakingService,
    private val exchangeRatesDataManager: ExchangeRatesDataManager,
    private val currencyPrefs: CurrencyPrefs
) : MviViewModel<StakingSummaryIntent,
    StakingSummaryViewState,
    StakingSummaryModelState,
    StakingSummaryNavigationEvent,
    StakingSummaryArgs
    >(initialState = StakingSummaryModelState()) {

    override fun viewCreated(args: StakingSummaryArgs) {
        viewModelScope.launch {
            coincore[args.cryptoTicker]?.let {
                onIntent(StakingSummaryIntent.LoadData(it.currency))
            } ?: onIntent(StakingSummaryIntent.StakingSummaryLoadError(args.cryptoTicker))
        }
    }

    override fun reduce(state: StakingSummaryModelState): StakingSummaryViewState = state.run {
        StakingSummaryViewState(
            currency = currency,
            isLoading = isLoading,
            errorState = errorState,
            balanceCrypto = balance,
            balanceFiat = balance?.toUserFiat(exchangeRatesDataManager),
            stakedCrypto = staked,
            stakedFiat = staked?.toUserFiat(exchangeRatesDataManager),
            bondingCrypto = bonding,
            bondingFiat = bonding?.toUserFiat(exchangeRatesDataManager),
            earnedCrypto = totalEarned,
            earnedFiat = totalEarned?.toUserFiat(exchangeRatesDataManager),
            stakingRate = stakingRate,
            isWithdrawable = isWithdrawable,
            rewardsFrequency = state.frequency
        )
    }

    override suspend fun handleIntent(modelState: StakingSummaryModelState, intent: StakingSummaryIntent) {
        when (intent) {
            is StakingSummaryIntent.LoadData -> loadStakingDetails(intent.currency)
            is StakingSummaryIntent.StakingSummaryLoadError -> updateState {
                it.copy(
                    errorState = StakingError.UnknownAsset(intent.assetTicker)
                )
            }
        }
    }

    private suspend fun loadStakingDetails(currency: Currency) {
        updateState {
            it.copy(
                isLoading = true,
                currency = currency
            )
        }

        combine(
            stakingService.getBalanceForAsset(currency),
            stakingService.getLimitsForAsset(currency as AssetInfo),
            stakingService.getRateForAsset(currency)
        ) { balance, limits, rate ->
            combineDataResources(balance, limits, rate) { b, l, r ->
                StakingSummaryData(b, l, r)
            }
        }.collectLatest { summary ->
            when (summary) {
                is DataResource.Data -> updateState {
                    it.copy(
                        errorState = StakingError.None,
                        isLoading = false,
                        balance = summary.data.balance.totalBalance,
                        staked = summary.data.balance.totalBalance
                            .minus(summary.data.balance.pendingDeposit)
                            .minus(summary.data.balance.pendingWithdrawal),
                        bonding = summary.data.balance.pendingDeposit,
                        totalEarned = summary.data.balance.totalRewards,
                        stakingRate = summary.data.rate,
                        frequency = summary.data.limits.rewardsFrequency,
                        isWithdrawable = !summary.data.limits.withdrawalsDisabled
                    )
                }
                is DataResource.Error -> updateState {
                    it.copy(isLoading = false, errorState = StakingError.Other)
                }
                DataResource.Loading -> updateState { it.copy(isLoading = true) }
            }
        }
    }
}

@Parcelize
data class StakingSummaryArgs(
    val cryptoTicker: String,
) : ModelConfigArgs.ParcelableArgs

private data class StakingSummaryData(
    val balance: StakingAccountBalance,
    val limits: StakingLimits,
    val rate: Double
)
