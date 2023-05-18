package com.blockchain.earn.activeRewards.viewmodel

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
import com.blockchain.earn.domain.models.ActiveRewardsRates
import com.blockchain.earn.domain.models.active.ActiveRewardsAccountBalance
import com.blockchain.earn.domain.models.active.ActiveRewardsLimits
import com.blockchain.earn.domain.service.ActiveRewardsService
import com.blockchain.featureflag.FeatureFlag
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.utils.asFlow
import com.blockchain.utils.combineMore
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.Currency
import info.blockchain.balance.ExchangeRate
import info.blockchain.balance.FiatCurrency
import info.blockchain.balance.Money
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx3.asFlow
import kotlinx.parcelize.Parcelize

class ActiveRewardsSummaryViewModel(
    private val coincore: Coincore,
    private val activeRewardsService: ActiveRewardsService,
    private val exchangeRatesDataManager: ExchangeRatesDataManager,
    private val currencyPrefs: CurrencyPrefs,
    private val activeRewardsWithdrawalsFF: FeatureFlag
) : MviViewModel<
    ActiveRewardsSummaryIntent,
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

    override fun ActiveRewardsSummaryModelState.reduce() = ActiveRewardsSummaryViewState(
        account = account,
        tradingAccount = tradingAccount,
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
        rewardsFrequency = rewardsFrequency,
        isWithdrawable = isWithdrawable,
        canDeposit = canDeposit,
        canWithdraw = canWithdraw,
        assetFiatPrice = assetFiatPrice,
        hasOngoingWithdrawals = hasOngoingWithdrawals
    )

    override suspend fun handleIntent(modelState: ActiveRewardsSummaryModelState, intent: ActiveRewardsSummaryIntent) {
        when (intent) {
            is ActiveRewardsSummaryIntent.LoadData -> loadActiveRewardsDetails(intent.currency)
            is ActiveRewardsSummaryIntent.ActiveRewardsSummaryLoadError -> updateState {
                copy(
                    errorState = ActiveRewardsError.UnknownAsset(intent.assetTicker)
                )
            }
        }
    }

    private suspend fun loadActiveRewardsDetails(currency: Currency) {
        updateState {
            copy(
                isLoading = true
            )
        }

        val withdrawalsEnabled = activeRewardsWithdrawalsFF.coEnabled()

        combineMore(
            coincore[currency].accountGroup(AssetFilter.ActiveRewards).toObservable().map {
                it.accounts.first()
            }.asFlow(),
            coincore[currency].accountGroup(AssetFilter.Trading).toObservable().map {
                it.accounts.first() as CustodialTradingAccount
            }.asFlow(),
            activeRewardsService.getBalanceForAsset(currency),
            activeRewardsService.getLimitsForAsset(currency as AssetInfo),
            activeRewardsService.getRatesForAsset(currency),
            exchangeRatesDataManager.exchangeRate(FiatCurrency.Dollars, currencyPrefs.selectedFiatCurrency),
            activeRewardsService.getEligibilityForAsset(currency),
            coincore[currency].getPricesWith24hDelta(),
            activeRewardsService.hasOngoingWithdrawals(currency)
        ) { account,
            tradingAccount,
            balance,
            limits,
            rate,
            exchangeRate,
            eligibility,
            priceData,
            hasOngoingWithdrawals ->
            combineDataResources(
                DataResource.Data(account),
                DataResource.Data(tradingAccount),
                balance,
                limits,
                rate,
                exchangeRate,
                eligibility,
                priceData,
                hasOngoingWithdrawals
            ) { a, t, b, l, r, er, e, p, h ->
                ActiveRewardsSummaryData(a, t, b, l, r, er, e, p.currentRate.price, h)
            }
        }.collectLatest { summary ->
            when (summary) {
                is DataResource.Data -> updateState {
                    with(summary.data) {
                        copy(
                            account = account,
                            tradingAccount = tradingAccount,
                            errorState = ActiveRewardsError.None,
                            isLoading = false,
                            balance = balance.totalBalance,
                            assetFiatPrice = assetFiatPrice,
                            totalEarned = balance.totalRewards,
                            totalSubscribed = balance.earningBalance,
                            totalOnHold = balance.bondingDeposits,
                            activeRewardsRate = rates.rate,
                            triggerPrice = rates.triggerPrice
                                .takeIf { currencyPrefs.selectedFiatCurrency == it.currency }
                                ?: exchangeRate.convert(rates.triggerPrice),
                            rewardsFrequency = limits.rewardsFrequency,
                            canWithdraw = balance.earningBalance.isPositive &&
                                withdrawalsEnabled &&
                                hasOngoingWithdrawals.not(),
                            hasOngoingWithdrawals = hasOngoingWithdrawals,
                            canDeposit = eligibility is EarnRewardsEligibility.Eligible
                        )
                    }
                }

                is DataResource.Error -> updateState {
                    copy(isLoading = false, errorState = ActiveRewardsError.Other)
                }

                DataResource.Loading -> updateState { copy(isLoading = true) }
            }
        }
    }
}

@Parcelize
data class ActiveRewardsSummaryArgs(
    val cryptoTicker: String
) : ModelConfigArgs.ParcelableArgs

private data class ActiveRewardsSummaryData(
    val account: BlockchainAccount,
    val tradingAccount: CustodialTradingAccount,
    val balance: ActiveRewardsAccountBalance,
    val limits: ActiveRewardsLimits,
    val rates: ActiveRewardsRates,
    val exchangeRate: ExchangeRate,
    val eligibility: EarnRewardsEligibility,
    val assetFiatPrice: Money,
    val hasOngoingWithdrawals: Boolean
)
