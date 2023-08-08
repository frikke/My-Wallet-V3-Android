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
import com.blockchain.earn.domain.models.StakingRewardsRates
import com.blockchain.earn.domain.models.staking.StakingAccountBalance
import com.blockchain.earn.domain.models.staking.StakingActivity
import com.blockchain.earn.domain.models.staking.StakingActivityType
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
) : MviViewModel<
    StakingSummaryIntent,
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

    override fun StakingSummaryModelState.reduce() = StakingSummaryViewState(
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
        earnFrequency = frequency,
        canDeposit = canDeposit,
        canWithdraw = canWithdraw,
        pendingActivity = pendingActivity.reduce(),
        unbondingDays = unbondingDays
    )

    private fun List<StakingActivity>.reduce(): List<StakingActivityViewState> {
        return sortedBy { it.expiryDate }
            .map {
                StakingActivityViewState(
                    currency = it.currency,
                    amountCrypto = it.amountCrypto?.let { amount ->
                        "${it.type.amountSign}${amount.toStringWithSymbol()}"
                    } ?: "",
                    amountFiat = it.amountCrypto?.let { amount ->
                        "${it.type.amountSign}${amount.toUserFiat(exchangeRatesDataManager).toStringWithSymbol()}"
                    } ?: "",
                    startDate = it.startDate?.toFormattedDate() ?: "",
                    expiryDate = it.expiryDate?.toFormattedDate() ?: "",
                    timestamp = it.startDate,
                    durationDays = it.durationDays,
                    type = it.type
                )
            }
    }

    override suspend fun handleIntent(modelState: StakingSummaryModelState, intent: StakingSummaryIntent) {
        when (intent) {
            is StakingSummaryIntent.LoadData -> loadStakingDetails(intent.currency)
            is StakingSummaryIntent.StakingSummaryLoadError -> updateState {
                copy(
                    errorState = StakingError.UnknownAsset(intent.assetTicker)
                )
            }
        }
    }

    private suspend fun loadStakingDetails(currency: Currency) {
        updateState {
            copy(
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
            stakingService.getPendingActivity(currency)
        ) { account, tradingAccount, balance, limits, rate, eligibility, activity ->
            combineDataResources(
                DataResource.Data(account),
                DataResource.Data(tradingAccount),
                balance,
                limits,
                rate,
                eligibility,
                activity
            ) { accountData, tradingAccountData, balanceData, limitsData,
                ratesData, eligibilityData, pendingActivity ->
                StakingSummaryData(
                    account = accountData,
                    tradingAccount = tradingAccountData,
                    balance = balanceData,
                    limits = limitsData,
                    rates = ratesData,
                    eligibility = eligibilityData,
                    pendingActivity = pendingActivity
                )
            }
        }.collectLatest { summary ->
            when (summary) {
                is DataResource.Data -> updateState {
                    with(summary.data) {
                        copy(
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
                            pendingActivity = pendingActivity,
                            unbondingDays = limits.unbondingDays
                        )
                    }
                }

                is DataResource.Error -> updateState {
                    copy(isLoading = false, errorState = StakingError.Other)
                }

                DataResource.Loading -> updateState { copy(isLoading = true) }
            }
        }
    }
}

@Parcelize
data class StakingSummaryArgs(
    val cryptoTicker: String
) : ModelConfigArgs.ParcelableArgs

private data class StakingSummaryData(
    val account: BlockchainAccount,
    val tradingAccount: CustodialTradingAccount,
    val balance: StakingAccountBalance,
    val limits: StakingLimits,
    val rates: StakingRewardsRates,
    val eligibility: EarnRewardsEligibility,
    val pendingActivity: List<StakingActivity>
)

private val StakingActivityType.amountSign: String
    get() = when (this) {
        StakingActivityType.Bonding -> ""
        StakingActivityType.Unbonding -> "-"
    }
