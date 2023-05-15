package com.blockchain.earn.interest.viewmodel

import androidx.lifecycle.viewModelScope
import com.blockchain.coincore.AssetFilter
import com.blockchain.coincore.BlockchainAccount
import com.blockchain.coincore.Coincore
import com.blockchain.coincore.toUserFiat
import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.commonarch.presentation.mvi_v2.MviViewModel
import com.blockchain.core.price.ExchangeRatesDataManager
import com.blockchain.data.DataResource
import com.blockchain.data.combineDataResources
import com.blockchain.domain.eligibility.model.EarnRewardsEligibility
import com.blockchain.earn.domain.models.EarnRewardsFrequency
import com.blockchain.earn.domain.models.interest.InterestAccountBalance
import com.blockchain.earn.domain.models.interest.InterestLimits
import com.blockchain.earn.domain.service.InterestService
import com.blockchain.utils.secondsToDays
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.Currency
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx3.asFlow
import kotlinx.parcelize.Parcelize

class InterestSummaryViewModel(
    private val coincore: Coincore,
    private val interestService: InterestService,
    private val exchangeRatesDataManager: ExchangeRatesDataManager
) : MviViewModel<
    InterestSummaryIntent,
    InterestSummaryViewState,
    InterestSummaryModelState,
    InterestSummaryNavigationEvent,
    InterestSummaryArgs
    >(initialState = InterestSummaryModelState()) {

    override fun viewCreated(args: InterestSummaryArgs) {
        viewModelScope.launch {
            coincore[args.cryptoTicker]?.let {
                onIntent(InterestSummaryIntent.LoadData(it.currency))
            } ?: onIntent(InterestSummaryIntent.InterestSummaryLoadError(args.cryptoTicker))
        }
    }

    override fun reduce(state: InterestSummaryModelState): InterestSummaryViewState = state.run {
        InterestSummaryViewState(
            account = account,
            isLoading = isLoading,
            errorState = errorState,
            balanceCrypto = balance,
            balanceFiat = balance?.toUserFiat(exchangeRatesDataManager),
            totalEarnedCrypto = totalEarned,
            totalEarnedFiat = totalEarned?.toUserFiat(exchangeRatesDataManager),
            pendingInterestCrypto = pendingInterest,
            pendingInterestFiat = pendingInterest?.toUserFiat(exchangeRatesDataManager),
            interestRate = interestRate,
            interestCommission = interestCommission, // TODO delete this if not available
            earnFrequency = earnFrequency,
            nextPaymentDate = nextPaymentDate,
            initialHoldPeriod = initialHoldPeriod,
            canWithdraw = canWithdraw,
            canDeposit = canDeposit
        )
    }

    override suspend fun handleIntent(modelState: InterestSummaryModelState, intent: InterestSummaryIntent) {
        when (intent) {
            is InterestSummaryIntent.LoadData -> loadInterestDetails(intent.currency)
            is InterestSummaryIntent.InterestSummaryLoadError -> updateState {
                it.copy(
                    errorState = InterestError.UnknownAsset(intent.assetTicker)
                )
            }
        }
    }

    private suspend fun loadInterestDetails(currency: Currency) {
        updateState {
            it.copy(
                isLoading = true
            )
        }
        combine(
            coincore[currency].accountGroup(AssetFilter.Interest).toObservable().map {
                it.accounts.first()
            }.asFlow(),
            interestService.getBalanceForFlow(currency as AssetInfo),
            interestService.getLimitsForAssetFlow(currency),
            interestService.getInterestRateFlow(currency),
            interestService.getEligibilityForAssetFlow(currency)
        ) { account, balance, limits, rate, eligibility ->
            combineDataResources(DataResource.Data(account), balance, limits, rate, eligibility) { a, b, l, r, e ->
                InterestSummaryData(a, b, l, r, e)
            }
        }.collectLatest { summary ->
            when (summary) {
                is DataResource.Data -> updateState {
                    with(summary.data) {
                        it.copy(
                            account = account,
                            errorState = InterestError.None,
                            isLoading = false,
                            balance = balance.totalBalance,
                            totalEarned = balance.totalInterest,
                            pendingInterest = balance.pendingInterest,
                            interestRate = rate,
                            earnFrequency = EarnRewardsFrequency.Monthly,
                            nextPaymentDate = limits.nextInterestPayment,
                            initialHoldPeriod = limits.interestLockUpDuration.secondsToDays(),
                            canWithdraw = limits.maxWithdrawalFiatValue.isPositive,
                            canDeposit = eligibility is EarnRewardsEligibility.Eligible
                        )
                    }
                }
                is DataResource.Error -> updateState {
                    it.copy(isLoading = false, errorState = InterestError.Other)
                }
                DataResource.Loading -> updateState { it.copy(isLoading = true) }
            }
        }
    }
}

@Parcelize
data class InterestSummaryArgs(
    val cryptoTicker: String
) : ModelConfigArgs.ParcelableArgs

private data class InterestSummaryData(
    val account: BlockchainAccount,
    val balance: InterestAccountBalance,
    val limits: InterestLimits,
    val rate: Double,
    val eligibility: EarnRewardsEligibility
)
