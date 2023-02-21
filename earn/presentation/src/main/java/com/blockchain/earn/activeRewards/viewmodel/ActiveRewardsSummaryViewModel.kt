package com.blockchain.earn.activeRewards.viewmodel

import androidx.lifecycle.viewModelScope
import com.blockchain.coincore.AssetFilter
import com.blockchain.coincore.Coincore
import com.blockchain.coincore.EarnRewardsAccount
import com.blockchain.coincore.toUserFiat
import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.commonarch.presentation.mvi_v2.MviViewModel
import com.blockchain.core.price.ExchangeRatesDataManager
import com.blockchain.data.DataResource
import com.blockchain.data.combineDataResources
import com.blockchain.domain.eligibility.model.EarnRewardsEligibility
import com.blockchain.earn.domain.models.ActiveRewardsRates
import com.blockchain.earn.domain.models.active.ActiveRewardsAccountBalance
import com.blockchain.earn.domain.models.active.ActiveRewardsLimits
import com.blockchain.earn.domain.service.ActiveRewardsService
import com.blockchain.utils.asFlow
import com.blockchain.utils.combineMore
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.Currency
import info.blockchain.balance.Money
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx3.asFlow
import kotlinx.parcelize.Parcelize

class ActiveRewardsSummaryViewModel(
    private val coincore: Coincore,
    private val activeRewardsService: ActiveRewardsService,
    private val exchangeRatesDataManager: ExchangeRatesDataManager,
) : MviViewModel<ActiveRewardsSummaryIntent,
    ActiveRewardsSummaryViewState,
    ActiveRewardsSummaryModelState,
    ActiveRewardsSummaryNavigationEvent,
    ActiveRewardsSummaryArgs
    >(initialState = ActiveRewardsSummaryModelState()) {

    override fun viewCreated(args: ActiveRewardsSummaryArgs) {
        viewModelScope.launch {
            coincore[args.cryptoTicker]?.let {
                onIntent(ActiveRewardsSummaryIntent.LoadData(it.currency))
            } ?: onIntent(ActiveRewardsSummaryIntent.ActiveRewardsSummaryLoadError(args.cryptoTicker))
        }
    }

    override fun reduce(state: ActiveRewardsSummaryModelState): ActiveRewardsSummaryViewState = state.run {
        ActiveRewardsSummaryViewState(
            account = account,
            isLoading = isLoading,
            errorState = errorState,
            balanceCrypto = balance,
            balanceFiat = balance?.toUserFiat(exchangeRatesDataManager),
            totalEarnedCrypto = totalEarned,
            totalEarnedFiat = totalEarned?.toUserFiat(exchangeRatesDataManager),
            totalSubscribedCrypto = totalSubscribed,
            totalSubscribedFiat = totalSubscribed?.toUserFiat(exchangeRatesDataManager),
            totalOnHoldCrypto = totalOnHold,
            totalOnHoldFiat = totalOnHold?.toUserFiat(exchangeRatesDataManager),
            activeRewardsRate = activeRewardsRate,
            triggerPrice = triggerPrice,
            rewardsFrequency = state.rewardsFrequency,
            isWithdrawable = isWithdrawable,
            canDeposit = canDeposit,
            assetFiatPrice = assetFiatPrice,
        )
    }

    override suspend fun handleIntent(modelState: ActiveRewardsSummaryModelState, intent: ActiveRewardsSummaryIntent) {
        when (intent) {
            is ActiveRewardsSummaryIntent.LoadData -> loadActiveRewardsDetails(intent.currency)
            is ActiveRewardsSummaryIntent.ActiveRewardsSummaryLoadError -> updateState {
                it.copy(
                    errorState = ActiveRewardsError.UnknownAsset(intent.assetTicker)
                )
            }
        }
    }

    private suspend fun loadActiveRewardsDetails(currency: Currency) {
        updateState {
            it.copy(
                isLoading = true
            )
        }
        combineMore(
            coincore[currency].accountGroup(AssetFilter.ActiveRewards).toObservable().map {
                it.accounts.first() as EarnRewardsAccount.Active
            }.asFlow(),
            activeRewardsService.getBalanceForAsset(currency),
            activeRewardsService.getLimitsForAsset(currency as AssetInfo),
            activeRewardsService.getRatesForAsset(currency),
            activeRewardsService.getEligibilityForAsset(currency),
            coincore[currency].getPricesWith24hDelta()
        ) { account, balance, limits, rate, eligibility, priceData ->
            combineDataResources(
                DataResource.Data(account),
                balance,
                limits,
                rate,
                eligibility,
                priceData
            ) { a, b, l, r, e, p ->
                ActiveRewardsSummaryData(a, b, l, r, e, p.currentRate.price)
            }
        }.collectLatest { summary ->
            when (summary) {
                is DataResource.Data -> updateState {
                    with(summary.data) {
                        it.copy(
                            account = account,
                            errorState = ActiveRewardsError.None,
                            isLoading = false,
                            balance = balance.totalBalance,
                            assetFiatPrice = assetFiatPrice,
                            totalEarned = balance.totalRewards,
                            totalSubscribed = balance.earningBalance,
                            totalOnHold = balance.bondingDeposits,
                            activeRewardsRate = rates.rate,
                            triggerPrice = rates.triggerPrice,
                            rewardsFrequency = limits.rewardsFrequency,
                            canDeposit = eligibility is EarnRewardsEligibility.Eligible
                        )
                    }
                }
                is DataResource.Error -> updateState {
                    it.copy(isLoading = false, errorState = ActiveRewardsError.Other)
                }
                DataResource.Loading -> updateState { it.copy(isLoading = true) }
            }
        }
    }
}

@Parcelize
data class ActiveRewardsSummaryArgs(
    val cryptoTicker: String,
) : ModelConfigArgs.ParcelableArgs

private data class ActiveRewardsSummaryData(
    val account: EarnRewardsAccount.Active,
    val balance: ActiveRewardsAccountBalance,
    val limits: ActiveRewardsLimits,
    val rates: ActiveRewardsRates,
    val eligibility: EarnRewardsEligibility,
    val assetFiatPrice: Money
)
