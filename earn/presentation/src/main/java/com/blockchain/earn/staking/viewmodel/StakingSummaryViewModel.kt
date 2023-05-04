package com.blockchain.earn.staking.viewmodel

import androidx.lifecycle.viewModelScope
import com.blockchain.coincore.AssetFilter
import com.blockchain.coincore.BlockchainAccount
import com.blockchain.coincore.Coincore
import com.blockchain.coincore.impl.CustodialTradingAccount
import com.blockchain.coincore.toUserFiat
import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.commonarch.presentation.mvi_v2.MviViewModel
import com.blockchain.core.price.ExchangeRatesDataManager
import com.blockchain.data.DataResource
import com.blockchain.data.combineDataResources
import com.blockchain.domain.eligibility.model.EarnRewardsEligibility
import com.blockchain.earn.domain.models.EarnWithdrawal
import com.blockchain.earn.domain.models.StakingRewardsRates
import com.blockchain.earn.domain.models.staking.StakingAccountBalance
import com.blockchain.earn.domain.models.staking.StakingLimits
import com.blockchain.earn.domain.service.StakingService
import com.blockchain.featureflag.FeatureFlag
import com.blockchain.utils.combineMore
import com.blockchain.utils.toFormattedDate
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.Currency
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx3.asFlow
import kotlinx.parcelize.Parcelize

class StakingSummaryViewModel(
    private val coincore: Coincore,
    private val stakingService: StakingService,
    private val exchangeRatesDataManager: ExchangeRatesDataManager,
    private val stakingWithdrawalsFF: FeatureFlag
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
            account = account,
            tradingAccount = tradingAccount,
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
            earnFrequency = state.frequency,
            canDeposit = canDeposit,
            canWithdraw = canWithdraw,
            pendingWithdrawals = reducePendingWithdrawals(pendingWithdrawals),
            unbondingDays = unbondingDays
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
                isLoading = true
            )
        }

        val withdrawalsEnabled = stakingWithdrawalsFF.coEnabled()

        combineMore(
            coincore[currency].accountGroup(AssetFilter.Staking).toObservable().map {
                it.accounts.first()
            }.asFlow(),
            coincore[currency].accountGroup(AssetFilter.Trading).toObservable().map {
                it.accounts.first() as CustodialTradingAccount
            }.asFlow(),
            stakingService.getBalanceForAsset(currency),
            stakingService.getLimitsForAsset(currency as AssetInfo),
            stakingService.getRatesForAsset(currency),
            stakingService.getEligibilityForAsset(currency),
            stakingService.getOngoingWithdrawals(currency)
        ) { account, tradingAccount, balance, limits, rate, eligibility, withdrawals ->
            combineDataResources(
                DataResource.Data(account),
                DataResource.Data(tradingAccount),
                balance,
                limits,
                rate,
                eligibility,
                withdrawals
            ) { a, t, b, l, r, e, w ->
                StakingSummaryData(a, t, b, l, r, e, w)
            }
        }.collectLatest { summary ->
            when (summary) {
                is DataResource.Data -> updateState {
                    with(summary.data) {
                        it.copy(
                            account = account,
                            tradingAccount = tradingAccount,
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
                            canDeposit = eligibility is EarnRewardsEligibility.Eligible,
                            canWithdraw = balance.availableBalance.isPositive &&
                                withdrawalsEnabled &&
                                !limits.withdrawalsDisabled,
                            pendingWithdrawals = pendingWithdrawals,
                            unbondingDays = limits.unbondingDays
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

    private fun reducePendingWithdrawals(pendingWithdrawals: List<EarnWithdrawal>): List<EarnWithdrawalUiElement> =
        pendingWithdrawals.map {
            EarnWithdrawalUiElement(
                currency = it.currency,
                amountCrypto = it.amountCrypto?.let { amount ->
                    "-${amount.toStringWithSymbol()}"
                } ?: "",
                amountFiat = it.amountCrypto?.let { amount ->
                    "-${amount.toUserFiat(exchangeRatesDataManager).toStringWithSymbol()}"
                } ?: "",
                unbondingStartDate = it.unbondingStartDate?.toFormattedDate() ?: "",
                unbondingExpiryDate = it.unbondingExpiryDate?.toFormattedDate() ?: "",
                withdrawalTimestamp = it.unbondingStartDate
            )
        }
}

@Parcelize
data class StakingSummaryArgs(
    val cryptoTicker: String,
) : ModelConfigArgs.ParcelableArgs

private data class StakingSummaryData(
    val account: BlockchainAccount,
    val tradingAccount: CustodialTradingAccount,
    val balance: StakingAccountBalance,
    val limits: StakingLimits,
    val rates: StakingRewardsRates,
    val eligibility: EarnRewardsEligibility,
    val pendingWithdrawals: List<EarnWithdrawal>
)
