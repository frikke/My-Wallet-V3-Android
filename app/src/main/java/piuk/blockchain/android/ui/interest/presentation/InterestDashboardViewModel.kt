package piuk.blockchain.android.ui.interest.presentation

import androidx.lifecycle.viewModelScope
import com.blockchain.coincore.AssetFilter
import com.blockchain.coincore.impl.CryptoInterestAccount
import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.commonarch.presentation.mvi_v2.MviViewModel
import com.blockchain.core.kyc.domain.KycService
import com.blockchain.core.kyc.domain.model.KycTier
import com.blockchain.data.DataResource
import com.blockchain.data.FreshnessStrategy
import com.blockchain.extensions.exhaustive
import com.blockchain.outcome.doOnSuccess
import info.blockchain.balance.AssetInfo
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import piuk.blockchain.android.ui.interest.domain.usecase.GetAccountGroupUseCase
import piuk.blockchain.android.ui.interest.domain.usecase.GetInterestDashboardUseCase

class InterestDashboardViewModel(
    private val kycService: KycService,
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
                loadDashboard()
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

    /**
     * Check kyc state first
     * if kyc gold -> load interest dashboard
     * if not -> show upgrade kyc
     */
    private fun loadDashboard() {
        viewModelScope.launch {
            kycService.getTiers(FreshnessStrategy.Cached(forceRefresh = true))
                .collectLatest { dataResourceKyc ->
                    when (dataResourceKyc) {
                        is DataResource.Loading -> updateState {
                            it.copy(isLoadingData = true)
                        }

                        is DataResource.Data -> {
                            // if kyc gold - load interest data
                            // else - prompt kyc upgrade
                            if (dataResourceKyc.data.isApprovedFor(KycTier.GOLD)) {
                                loadInterestData()
                            } else {
                                updateState {
                                    it.copy(
                                        isLoadingData = false,
                                        isError = false,
                                        isKycGold = false
                                    )
                                }
                            }
                        }

                        is DataResource.Error -> updateState {
                            it.copy(
                                isLoadingData = false,
                                isError = true,
                            )
                        }
                    }
                }
        }
    }

    private suspend fun loadInterestData() {
        getInterestDashboardUseCase().collectLatest { dataResourceInterest ->
            when (dataResourceInterest) {
                is DataResource.Loading -> updateState {
                    it.copy(
                        isLoadingData = it.data.isEmpty(),
                        isError = false
                    )
                }

                is DataResource.Data -> updateState {
                    it.copy(
                        isLoadingData = false,
                        isError = false,
                        isKycGold = true,
                        data = dataResourceInterest.data
                    )
                }

                is DataResource.Error -> updateState {
                    it.copy(
                        isLoadingData = false,
                        isError = true,
                    )
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
