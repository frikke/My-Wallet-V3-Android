package com.blockchain.earn.dashboard.viewmodel

import androidx.lifecycle.viewModelScope
import com.blockchain.coincore.Coincore
import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.commonarch.presentation.mvi_v2.MviViewModel
import com.blockchain.core.price.ExchangeRatesDataManager
import com.blockchain.earn.domain.service.StakingService
import com.blockchain.preferences.CurrencyPrefs
import info.blockchain.balance.Currency
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize

class EarnDashboardViewModel(
    private val coincore: Coincore,
    private val stakingService: StakingService,
    private val exchangeRatesDataManager: ExchangeRatesDataManager,
    private val currencyPrefs: CurrencyPrefs
) : MviViewModel<EarnDashboardIntent,
    EarnDashboardViewState,
    EarnDashboardModelState,
    EarnDashboardNavigationEvent,
    ModelConfigArgs.NoArgs
    >(initialState = EarnDashboardModelState()) {

    override fun viewCreated(args: ModelConfigArgs.NoArgs) {
        viewModelScope.launch {
        }
    }

    override fun reduce(state: EarnDashboardModelState): EarnDashboardViewState = state.run {
        EarnDashboardViewState(
            isLoading = isLoading,
            false,
            EarnDashboardError.None
        )
    }

    override suspend fun handleIntent(modelState: EarnDashboardModelState, intent: EarnDashboardIntent) {
        when (intent) {
            is EarnDashboardIntent.LoadData -> {
                // loadEarnInfo(intent.currency)
            }
        }
    }

    private suspend fun loadEarnInfo(currency: Currency) {
        updateState {
            it.copy(
                isLoading = true,
            )
        }
    }
}

@Parcelize
data class EarnDashboardArgs(
    val cryptoTicker: String,
) : ModelConfigArgs.ParcelableArgs
