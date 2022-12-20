package com.blockchain.earn.staking.viewmodel

import androidx.lifecycle.viewModelScope
import com.blockchain.coincore.Coincore
import com.blockchain.coincore.toUserFiat
import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.commonarch.presentation.mvi_v2.MviViewModel
import com.blockchain.core.price.ExchangeRatesDataManager
import com.blockchain.data.DataResource
import com.blockchain.data.combineDataResources
import com.blockchain.domain.eligibility.model.StakingEligibility
import com.blockchain.earn.domain.models.staking.StakingAccountBalance
import com.blockchain.earn.domain.models.staking.StakingLimits
import com.blockchain.earn.domain.models.staking.StakingRates
import com.blockchain.earn.domain.service.StakingService
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.Currency
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize

class StakingSummaryViewModel(
    private val coincore: Coincore,
    private val stakingService: StakingService,
    private val exchangeRatesDataManager: ExchangeRatesDataManager
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
            commissionRate = stakingCommission,
            isWithdrawable = isWithdrawable,
            rewardsFrequency = state.frequency,
            canDeposit = canDeposit
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
            stakingService.getRatesForAsset(currency),
            stakingService.getEligibilityForAsset(currency)
        ) { balance, limits, rate, eligibility ->
            combineDataResources(balance, limits, rate, eligibility) { b, l, r, e ->
                StakingSummaryData(b, l, r, e)
            }
        }.collectLatest { summary ->
            when (summary) {
                is DataResource.Data -> updateState {
                    with(summary.data) {
                        it.copy(
                            errorState = StakingError.None,
                            isLoading = false,
                            balance = balance.totalBalance,
                            staked = balance.totalBalance
                                .minus(balance.pendingDeposit)
                                .minus(balance.pendingWithdrawal),
                            bonding = balance.pendingDeposit,
                            totalEarned = balance.totalRewards,
                            stakingRate = rates.rate,
                            stakingCommission = rates.commission,
                            frequency = limits.rewardsFrequency,
                            isWithdrawable = !limits.withdrawalsDisabled,
                            canDeposit = stakingEligibility is StakingEligibility.Eligible
                        )
                    }
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
    val rates: StakingRates,
    val stakingEligibility: StakingEligibility
)
