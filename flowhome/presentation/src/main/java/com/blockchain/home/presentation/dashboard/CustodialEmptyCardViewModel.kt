package com.blockchain.home.presentation.dashboard

import androidx.lifecycle.viewModelScope
import com.blockchain.commonarch.presentation.mvi_v2.Intent
import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.commonarch.presentation.mvi_v2.ModelState
import com.blockchain.commonarch.presentation.mvi_v2.MviViewModel
import com.blockchain.commonarch.presentation.mvi_v2.NavigationEvent
import com.blockchain.commonarch.presentation.mvi_v2.ViewState
import com.blockchain.data.DataResource
import com.blockchain.data.FreshnessStrategy
import com.blockchain.data.RefreshStrategy
import com.blockchain.data.dataOrElse
import com.blockchain.data.map
import com.blockchain.domain.fiatcurrencies.FiatCurrenciesService
import com.blockchain.domain.onboarding.CompletableDashboardOnboardingStep
import com.blockchain.domain.onboarding.DashboardOnboardingStep
import com.blockchain.domain.onboarding.DashboardOnboardingStepState
import com.blockchain.domain.onboarding.OnBoardingStepsService
import com.blockchain.home.emptystate.CustodialEmptyCardService
import com.blockchain.nabu.Feature
import com.blockchain.nabu.FeatureAccess
import com.blockchain.nabu.api.getuser.domain.UserFeaturePermissionService
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.FiatCurrency
import info.blockchain.balance.Money
import kotlinx.coroutines.launch

class CustodialEmptyCardViewModel(
    private val fiatCurrenciesService: FiatCurrenciesService,
    private val onBoardingStepsService: OnBoardingStepsService,
    private val userFeaturePermissionService: UserFeaturePermissionService,
    private val custodialEmptyCardService: CustodialEmptyCardService
) : MviViewModel
<
    CustodialEmptyCardIntent,
    CustodialEmptyCardViewState,
    CustodialEmptyCardModelState,
    CustodialEmptyCardNavEvent,
    ModelConfigArgs.NoArgs
    >(initialState = CustodialEmptyCardModelState()) {
    override fun viewCreated(args: ModelConfigArgs.NoArgs) {
    }

    override fun reduce(state: CustodialEmptyCardModelState): CustodialEmptyCardViewState {
        return with(state) {
            CustodialEmptyCardViewState(
                steps = this.steps,
                tradingCurrency = fiatCurrenciesService.selectedTradingCurrency,
                amounts = buyAmounts,
                trendCurrency = CryptoCurrency.BTC,
                userCanBuy = buyAccess.map {
                    it is FeatureAccess.Granted
                }.dataOrElse(false)
            )
        }
    }

    override suspend fun handleIntent(modelState: CustodialEmptyCardModelState, intent: CustodialEmptyCardIntent) {
        when (intent) {
            CustodialEmptyCardIntent.LoadEmptyStateConfig -> {
                try {
                    val steps = onBoardingStepsService.onBoardingSteps()
                    updateState {
                        it.copy(
                            steps = steps
                        )
                    }
                } catch (e: Exception) {
                    updateState {
                        it.copy(
                            steps = DashboardOnboardingStep.values().map { step ->
                                CompletableDashboardOnboardingStep(step, DashboardOnboardingStepState.INCOMPLETE)
                            }
                        )
                    }
                }

                val emptyStateAmounts = custodialEmptyCardService.getEmptyStateBuyAmounts(
                    fiatCurrenciesService.selectedTradingCurrency
                )
                updateState {
                    it.copy(
                        buyAmounts = emptyStateAmounts
                    )
                }

                viewModelScope.launch {
                    userFeaturePermissionService.getAccessForFeature(
                        Feature.Buy, FreshnessStrategy.Cached(RefreshStrategy.ForceRefresh)
                    ).collect {
                        updateState { state ->
                            state.copy(
                                buyAccess = it
                            )
                        }
                    }
                }
            }
        }
    }
}

data class CustodialEmptyCardModelState(
    val buyAmounts: List<Money> = emptyList(),
    val buyAccess: DataResource<FeatureAccess> = DataResource.Loading,
    val steps: List<CompletableDashboardOnboardingStep> = DashboardOnboardingStep.values().map {
        CompletableDashboardOnboardingStep(it, DashboardOnboardingStepState.INCOMPLETE)
    }
) : ModelState

sealed class CustodialEmptyCardIntent : Intent<CustodialEmptyCardModelState> {
    object LoadEmptyStateConfig : CustodialEmptyCardIntent()
}

class CustodialEmptyCardViewState(
    /*val totalSteps: Int,
    val completedSteps: Int,*/
    val steps: List<CompletableDashboardOnboardingStep>,
    val amounts: List<Money>,
    val userCanBuy: Boolean,
    val tradingCurrency: FiatCurrency,
    val trendCurrency: CryptoCurrency
) : ViewState

class CustodialEmptyCardNavEvent : NavigationEvent
