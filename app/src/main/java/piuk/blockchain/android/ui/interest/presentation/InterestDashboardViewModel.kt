package piuk.blockchain.android.ui.interest.presentation

import androidx.lifecycle.viewModelScope
import com.blockchain.coincore.AssetFilter
import com.blockchain.coincore.impl.CryptoInterestAccount
import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.commonarch.presentation.mvi_v2.MviViewModel
import com.blockchain.extensions.exhaustive
import com.blockchain.nabu.models.responses.nabu.KycTierLevel
import com.blockchain.outcome.doOnFailure
import com.blockchain.outcome.doOnSuccess
import info.blockchain.balance.AssetInfo
import kotlinx.coroutines.launch
import piuk.blockchain.android.ui.interest.domain.model.InterestDashboard
import piuk.blockchain.android.ui.interest.domain.usecase.GetAccountGroupUseCase
import piuk.blockchain.android.ui.interest.domain.usecase.GetAssetsInterestUseCase
import piuk.blockchain.android.ui.interest.domain.usecase.GetInterestDashboardUseCase
import timber.log.Timber

class InterestDashboardViewModel(
    private val getAssetsInterestUseCase: GetAssetsInterestUseCase,
    private val getInterestDashboardUseCase: GetInterestDashboardUseCase,
    private val getAccountGroupUseCase: GetAccountGroupUseCase
) : MviViewModel<InterestDashboardIntents,
    InterestDashboardViewState,
    InterestDashboardModelState,
    InterestDashboardNavigationEvent,
    ModelConfigArgs.NoArgs>(InterestDashboardModelState()) {

    override fun viewCreated(args: ModelConfigArgs.NoArgs) {
    }

    override suspend fun handleIntent(modelState: InterestDashboardModelState, intent: InterestDashboardIntents) {
        when (intent) {
            InterestDashboardIntents.LoadDashboard -> {
                loadInterestDashboard()
            }

            is InterestDashboardIntents.FilterData -> {
                filterData(intent.filter)
            }

            is InterestDashboardIntents.InterestItemClicked -> {
                handleInterestItemClicked(cryptoCurrency = intent.cryptoCurrency, hasBalance = intent.hasBalance)
            }

            InterestDashboardIntents.StartKyc -> {
                navigate(InterestDashboardNavigationEvent.StartKyc)
            }
        }.exhaustive
    }

    override fun reduce(state: InterestDashboardModelState): InterestDashboardViewState {
        return InterestDashboardViewState(
            isLoading = state.isLoadingData,
            isError = state.isError,
            isKycGold = state.isKycGold,
            data = state.data.run {
                if (state.filter.isEmpty().not()) {
                    filter {
                        it.assetInfo.displayTicker.contains(state.filter, ignoreCase = true) ||
                            it.assetInfo.name.contains(state.filter, ignoreCase = true)
                    }
                } else this
            }
        )
    }

    private fun loadInterestDashboard() {
        viewModelScope.launch {
            updateState { it.copy(isLoadingData = true) }

            getInterestDashboardUseCase().let { result ->
                result.doOnSuccess { interestDetail ->
                    loadAssets(interestDetail)
                }.doOnFailure { error ->
                    updateState { it.copy(isLoadingData = false, isError = true) }
                    Timber.e("Error loading interest summary details $error")
                }
            }
        }
    }

    private fun loadAssets(interestDashboard: InterestDashboard) {
        viewModelScope.launch {

            getAssetsInterestUseCase(interestDashboard.enabledAssets).let { result ->
                result.doOnSuccess { assetInterestInfoList ->
                    updateState {
                        it.copy(
                            isLoadingData = false,
                            isError = false,
                            isKycGold = interestDashboard.tiers.isApprovedFor(KycTierLevel.GOLD),
                            data = assetInterestInfoList,
                        )
                    }
                }.doOnFailure { error ->
                    updateState { it.copy(isLoadingData = false, isError = true) }
                    Timber.e("Error loading interest info list $error")
                }
            }
        }
    }

    private fun filterData(filter: String) {
        updateState { it.copy(filter = filter) }
    }

    private fun handleInterestItemClicked(cryptoCurrency: AssetInfo, hasBalance: Boolean) {
        viewModelScope.launch {
            getAccountGroupUseCase(cryptoCurrency = cryptoCurrency, filter = AssetFilter.Interest).let { result ->
                result.doOnSuccess {
                    val interestAccount = it.accounts.first() as CryptoInterestAccount
                    navigate(
                        if (hasBalance) {
                            InterestDashboardNavigationEvent.InterestSummary(interestAccount)
                        } else {
                            InterestDashboardNavigationEvent.InterestDeposit(interestAccount)
                        }
                    )
                }
            }
        }
    }
}
