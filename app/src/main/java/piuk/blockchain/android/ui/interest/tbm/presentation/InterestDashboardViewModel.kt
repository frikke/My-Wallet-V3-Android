package piuk.blockchain.android.ui.interest.tbm.presentation

import androidx.lifecycle.viewModelScope
import com.blockchain.coincore.AssetFilter
import com.blockchain.coincore.impl.CryptoInterestAccount
import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.commonarch.presentation.mvi_v2.MviViewModel
import com.blockchain.nabu.models.responses.nabu.KycTierLevel
import info.blockchain.balance.AssetInfo
import kotlinx.coroutines.launch
import piuk.blockchain.android.ui.interest.tbm.domain.model.InterestDetail
import piuk.blockchain.android.ui.interest.tbm.domain.usecase.GetAccountGroupUseCase
import piuk.blockchain.android.ui.interest.tbm.domain.usecase.GetAssetInterestInfoUseCase
import piuk.blockchain.android.ui.interest.tbm.domain.usecase.GetInterestDetailUseCase
import piuk.blockchain.android.ui.interest.tbm.presentation.adapter.InterestDashboardItem
import timber.log.Timber

class InterestDashboardViewModel(
    private val getAssetInterestInfoUseCase: GetAssetInterestInfoUseCase,
    private val getInterestDetailUseCase: GetInterestDetailUseCase,
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
            InterestDashboardIntents.LoadData -> {
                loadInterestDetail()
            }

            is InterestDashboardIntents.InterestItemClicked -> {
                handleInterestItemClicked(cryptoCurrency = intent.cryptoCurrency, hasBalance = intent.hasBalance)
            }
        }
    }

    override fun reduce(state: InterestDashboardModelState): InterestDashboardViewState {
        return InterestDashboardViewState(
            isLoading = state.isLoadingData,
            isError = state.isError,
            isKycGold = state.isKycGold,
            data = state.data
        )
    }

    private fun loadInterestDetail() {
        viewModelScope.launch {
            updateState { it.copy(isLoadingData = true) }

            getInterestDetailUseCase().let { result ->
                result.getOrNull()?.let { interestDetail ->
                    loadAssets(interestDetail)
                } ?: kotlin.run {
                    updateState { it.copy(isLoadingData = false, isError = true) }
                    Timber.e("Error loading interest summary details ${result.exceptionOrNull()}")
                }
            }
        }
    }

    private fun loadAssets(interestDetail: InterestDetail) {
        viewModelScope.launch {

            val isKycGold = interestDetail.tiers.isApprovedFor(KycTierLevel.GOLD)

            val initialData = mutableListOf<InterestDashboardItem>()
            if (isKycGold.not()) initialData.add(InterestDashboardItem.InterestIdentityVerificationItem)

            getAssetInterestInfoUseCase(interestDetail.enabledAssets).let { result ->
                result.getOrNull()?.let { assetInterestInfoList ->
                    updateState {
                        it.copy(
                            isLoadingData = false,
                            isError = false,
                            isKycGold = isKycGold,
                            data = initialData +
                                assetInterestInfoList.map { InterestDashboardItem.InterestAssetInfoItem(it) }
                        )
                    }
                } ?: kotlin.run {
                    updateState { it.copy(isLoadingData = false, isError = true) }
                    Timber.e("Error loading interest info list ${result.exceptionOrNull()}")
                }
            }
        }
    }

    private fun handleInterestItemClicked(cryptoCurrency: AssetInfo, hasBalance: Boolean) {
        viewModelScope.launch {
            getAccountGroupUseCase(cryptoCurrency = cryptoCurrency, filter = AssetFilter.Interest).let { result ->
                result.getOrNull()?.let {
                    val interestAccount = it.accounts.first() as CryptoInterestAccount
                    navigate(
                        if (hasBalance) {
                            InterestDashboardNavigationEvent.NavigateToInterestSummarySheet(interestAccount)
                        } else {
                            InterestDashboardNavigationEvent.NavigateToTransactionFlow(interestAccount)
                        }
                    )
                }
            }
        }
    }
}
