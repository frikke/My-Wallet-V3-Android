package piuk.blockchain.android.ui.brokerage.sell

import com.blockchain.coincore.CryptoAccount
import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.commonarch.presentation.mvi_v2.MviViewModel
import com.blockchain.core.sell.domain.SellEligibility
import com.blockchain.data.DataResource
import com.blockchain.data.FreshnessStrategy
import com.blockchain.data.RefreshStrategy
import com.blockchain.data.map
import com.blockchain.walletmode.WalletModeService
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flatMapLatest

class SellViewModel(
    private val sellRepository: SellRepository,
    private val walletModeService: WalletModeService
) : MviViewModel<
    SellIntent,
    SellViewState,
    SellModelState,
    SellNavigation,
    ModelConfigArgs.NoArgs
    >(SellModelState()) {

    override fun viewCreated(args: ModelConfigArgs.NoArgs) {
    }

    override fun reduce(state: SellModelState): SellViewState =
        SellViewState(
            sellEligibility = state.sellEligibility,
            showLoader = state.shouldShowLoading,
            supportedAccountList = state.supportedAccountList.map { list ->
                if (state.searchTerm.isNotEmpty()) {
                    list.filterList(searchTerm = state.searchTerm)
                } else {
                    list
                }
            }
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    override suspend fun handleIntent(modelState: SellModelState, intent: SellIntent) {
        when (intent) {
            is SellIntent.CheckSellEligibility -> {
                sellRepository.sellEligibility(
                    freshnessStrategy = FreshnessStrategy.Cached(RefreshStrategy.ForceRefresh)
                ).collectLatest { data ->
                    if (data is DataResource.Data || modelState.sellEligibility !is DataResource.Data) {
                        updateState {
                            it.copy(
                                sellEligibility = data
                            )
                        }
                    }
                    if (data is DataResource.Data) {
                        (data.data as? SellEligibility.Eligible)?.let { sellEligibility ->
                            onIntent(SellIntent.LoadSupportedAccounts(sellEligibility.sellAssets))
                        }
                    }
                }
            }
            is SellIntent.LoadSupportedAccounts -> {
                walletModeService.walletMode.flatMapLatest {
                    sellRepository.sellSupportedAssets(
                        availableAssets = intent.supportedAssets,
                        walletMode = it
                    )
                }.collect { list ->
                    if (
                        list is DataResource.Data &&
                        modelState.supportedAccountList is DataResource.Data &&
                        shouldUpdateList(list, modelState.supportedAccountList)
                    ) {
                        updateState {
                            it.copy(supportedAccountList = list)
                        }
                    } else if (
                        modelState.supportedAccountList !is DataResource.Data
                    ) {
                        updateState {
                            it.copy(supportedAccountList = list)
                        }
                    }
                }
            }
            is SellIntent.FilterAccounts -> updateState {
                it.copy(searchTerm = intent.searchTerm)
            }
        }
    }

    private fun shouldUpdateList(
        list: DataResource.Data<List<CryptoAccount>>,
        supportedAccountList: DataResource.Data<List<CryptoAccount>>
    ): Boolean {
        val currentList = supportedAccountList.data
        val newList = list.data
        if (newList.size != currentList.size) {
            return true
        }
        newList.forEach {
            if (it !in currentList) {
                return true
            }
        }
        return newList.any { it !in currentList }
    }

    private fun List<CryptoAccount>.filterList(searchTerm: String) =
        this.filter { account ->
            account.currency.name.contains(searchTerm, true) ||
                account.currency.networkTicker.contains(searchTerm, true) ||
                account.label.contains(searchTerm, true)
        }
}
