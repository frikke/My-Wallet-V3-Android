package piuk.blockchain.android.ui.brokerage.sell

import androidx.lifecycle.viewModelScope
import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.Coincore
import com.blockchain.coincore.CryptoAccount
import com.blockchain.coincore.SingleAccountList
import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.commonarch.presentation.mvi_v2.MviViewModel
import com.blockchain.core.sell.domain.SellService
import com.blockchain.data.DataResource
import com.blockchain.data.map
import com.blockchain.featureflag.FeatureFlag
import com.blockchain.preferences.LocalSettingsPrefs
import com.blockchain.utils.zipObservables
import info.blockchain.balance.AssetInfo
import io.reactivex.rxjava3.core.Observable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx3.asFlow
import piuk.blockchain.android.ui.transfer.AccountsSorting

class SellViewModel(
    private val sellService: SellService,
    private val coincore: Coincore,
    private val accountsSorting: AccountsSorting,
    private val localSettingsPrefs: LocalSettingsPrefs,
    private val hideDustFlag: FeatureFlag
) : MviViewModel<
    SellIntent,
    SellViewState,
    SellModelState,
    SellNavigation,
    ModelConfigArgs.NoArgs
    >(SellModelState()) {

    override fun viewCreated(args: ModelConfigArgs.NoArgs) {
        viewModelScope.launch {
            sellService.loadSellAssets().collectLatest { data ->
                updateState {
                    it.copy(
                        sellEligibility = data,
                        shouldShowLoading = false
                    )
                }
            }
        }
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

    override suspend fun handleIntent(modelState: SellModelState, intent: SellIntent) {
        when (intent) {
            is SellIntent.CheckSellEligibility -> {
                sellService.loadSellAssets().first()
            }
            is SellIntent.LoadSupportedAccounts -> {
                viewModelScope.launch {
                    loadSupportedWallets(
                        intent = intent,
                        shouldFilterDust = hideDustFlag.coEnabled() && localSettingsPrefs.hideSmallBalancesEnabled
                    ).collectLatest { list ->
                        updateState {
                            it.copy(supportedAccountList = DataResource.Data(list))
                        }
                    }
                }
            }
            is SellIntent.FilterAccounts -> updateState {
                it.copy(searchTerm = intent.searchTerm)
            }
        }
    }

    private fun loadSupportedWallets(
        intent: SellIntent.LoadSupportedAccounts,
        shouldFilterDust: Boolean
    ): Flow<List<CryptoAccount>> =
        coincore.walletsWithActions(
            actions = setOf(AssetAction.Sell),
            sorter = accountsSorting.sorter()
        ).toObservable().flatMap { accountList ->
            accountList
                .filterUnsupportedPairs(intent.supportedAssets).let {
                    if (shouldFilterDust) {
                        it.filterDustBalances()
                    } else {
                        Observable.just(it.filterIsInstance<CryptoAccount>())
                    }
                }
        }.asFlow()

    private fun SingleAccountList.filterDustBalances(): Observable<List<CryptoAccount>> =
        map { account ->
            account.balanceRx
        }.zipObservables().map {
            this.mapIndexedNotNull { index, singleAccount ->
                if (!it[index].totalFiat.isDust()) {
                    singleAccount
                } else {
                    null
                }
            }.filterIsInstance<CryptoAccount>()
        }

    private fun SingleAccountList.filterUnsupportedPairs(supportedAssets: List<AssetInfo>) =
        this.filter { account ->
            supportedAssets.contains(account.currency)
        }

    private fun List<CryptoAccount>.filterList(searchTerm: String) =
        this.filter { account ->
            account.currency.name.contains(searchTerm, true) ||
                account.currency.networkTicker.contains(searchTerm, true) ||
                account.label.contains(searchTerm, true)
        }
}
