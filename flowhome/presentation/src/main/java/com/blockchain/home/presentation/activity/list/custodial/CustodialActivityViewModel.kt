package com.blockchain.home.presentation.activity.list.custodial

import androidx.lifecycle.viewModelScope
import com.blockchain.coincore.ActivitySummaryItem
import com.blockchain.coincore.CryptoActivitySummaryItem
import com.blockchain.coincore.CustodialTransaction
import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.commonarch.presentation.mvi_v2.MviViewModel
import com.blockchain.data.DataResource
import com.blockchain.data.FreshnessStrategy
import com.blockchain.data.RefreshStrategy
import com.blockchain.data.filter
import com.blockchain.data.map
import com.blockchain.data.updateDataWith
import com.blockchain.home.activity.CustodialActivityService
import com.blockchain.home.presentation.SectionSize
import com.blockchain.home.presentation.activity.common.ActivityComponent
import com.blockchain.home.presentation.activity.list.ActivityIntent
import com.blockchain.home.presentation.activity.list.ActivityModelState
import com.blockchain.home.presentation.activity.list.ActivityViewState
import com.blockchain.home.presentation.activity.list.TransactionGroup
import com.blockchain.home.presentation.activity.list.custodial.mappers.toActivityComponent
import com.blockchain.home.presentation.dashboard.HomeNavEvent
import com.blockchain.utils.CurrentTimeProvider
import com.blockchain.walletmode.WalletMode
import com.blockchain.walletmode.WalletModeService
import java.util.Calendar
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch

class CustodialActivityViewModel(
    private val custodialActivityService: CustodialActivityService,
    private val walletModeService: WalletModeService
) : MviViewModel<
    ActivityIntent<CustodialTransaction>,
    ActivityViewState,
    ActivityModelState<CustodialTransaction>,
    HomeNavEvent,
    ModelConfigArgs.NoArgs>(ActivityModelState(walletMode = WalletMode.CUSTODIAL)) {

    private var activityJob: Job? = null

    override fun viewCreated(args: ModelConfigArgs.NoArgs) {}

    override fun reduce(state: ActivityModelState<CustodialTransaction>): ActivityViewState = state.run {

        ActivityViewState(
            activity = state.activityItems
                .filter { activityItem ->
                    if (state.filterTerm.isEmpty()) {
                        true
                    } else {
                        activityItem.matches(state.filterTerm)
                    }
                }
                .map { activityItems ->
                    activityItems.reduceActivityPage()
                }
                .map { groupedComponents ->
                    when (val sectionSize = state.sectionSize) {
                        SectionSize.All -> {
                            groupedComponents
                        }
                        is SectionSize.Limited -> {
                            mapOf(
                                TransactionGroup.Combined to groupedComponents.values.flatten().take(sectionSize.size)
                            )
                        }
                    }
                },
            walletMode = state.walletMode
        )
    }

    private fun List<ActivitySummaryItem>.reduceActivityPage(): Map<TransactionGroup, List<ActivityComponent>> {
        // group by date (month/year)
        return this
            .groupBy { activity ->
                if (activity.stateIsFinalised) {
                    Calendar.getInstance().apply {
                        timeInMillis = activity.timeStampMs
                        // keep year/month to group with and reset everything else
                        set(Calendar.DAY_OF_MONTH, 1)
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }.let { date ->
                        TransactionGroup.Group.Date(date)
                    }
                } else {
                    TransactionGroup.Group.Pending
                }
            }
            // reduce to summary
            .mapValues { (_, activities) ->
                activities.map { it.toActivityComponent() }
            }
            .toMap()
            .toSortedMap(compareByDescending { it })
    }

    override suspend fun handleIntent(
        modelState: ActivityModelState<CustodialTransaction>,
        intent: ActivityIntent<CustodialTransaction>
    ) {
        when (intent) {
            is ActivityIntent.LoadActivity -> {
                updateState { it.copy(sectionSize = intent.sectionSize) }
                activityJob?.cancel()
                activityJob = viewModelScope.launch {
                    walletModeService.walletMode.flatMapLatest {
                        loadData(intent.freshnessStrategy, it)
                    }.collect { dataRes ->
                        updateState {
                            it.copy(activityItems = it.activityItems.updateDataWith(dataRes))
                        }
                    }
                }
            }

            is ActivityIntent.FilterSearch -> {
                updateState {
                    it.copy(filterTerm = intent.term)
                }
            }

            is ActivityIntent.Refresh -> {
                updateState {
                    it.copy(lastFreshDataTime = CurrentTimeProvider.currentTimeMillis())
                }
                walletModeService.walletMode.take(1).flatMapLatest {
                    loadData(FreshnessStrategy.Cached(RefreshStrategy.ForceRefresh), it)
                }.collect { dataRes ->
                    updateState {
                        it.copy(activityItems = it.activityItems.updateDataWith(dataRes))
                    }
                }
            }
        }
    }

    private fun loadData(freshnessStrategy: FreshnessStrategy, walletMode: WalletMode) =
        when (walletMode) {
            WalletMode.CUSTODIAL -> {
                custodialActivityService.getAllActivity(
                    freshnessStrategy
                )
            }
            WalletMode.NON_CUSTODIAL -> flowOf(DataResource.Data(emptyList()))
        }
}

private fun ActivitySummaryItem.matches(filterTerm: String): Boolean {
    return account.currency.networkTicker.contains(filterTerm, ignoreCase = true) ||

        account.currency.name.contains(filterTerm, ignoreCase = true) ||

        value.toStringWithSymbol().contains(filterTerm, ignoreCase = true) ||

        (this as? CryptoActivitySummaryItem)?.currency?.let { asset ->
            asset.networkTicker.contains(filterTerm, ignoreCase = true) ||
                asset.name.contains(filterTerm, ignoreCase = true)
        } ?: false
}
