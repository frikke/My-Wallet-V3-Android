package com.blockchain.home.presentation.quickactions

import androidx.lifecycle.viewModelScope
import com.blockchain.coincore.ActionState
import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.Coincore
import com.blockchain.coincore.FiatAccount
import com.blockchain.coincore.StateAwareAction
import com.blockchain.commonarch.presentation.mvi_v2.Intent
import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.commonarch.presentation.mvi_v2.ModelState
import com.blockchain.commonarch.presentation.mvi_v2.MviViewModel
import com.blockchain.commonarch.presentation.mvi_v2.NavigationEvent
import com.blockchain.commonarch.presentation.mvi_v2.ViewState
import com.blockchain.data.DataResource
import com.blockchain.fiatActions.fiatactions.FiatActionsUseCase
import com.blockchain.home.actions.QuickActionsService
import com.blockchain.home.presentation.R
import com.blockchain.nabu.api.getuser.domain.UserFeaturePermissionService
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.presentation.pulltorefresh.PullToRefreshUtils
import com.blockchain.utils.CurrentTimeProvider
import com.blockchain.walletmode.WalletMode
import com.blockchain.walletmode.WalletModeService
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx3.await
import timber.log.Timber

class QuickActionsViewModel(
    private val userFeaturePermissionService: UserFeaturePermissionService,
    private val walletModeService: WalletModeService,
    private val currencyPrefs: CurrencyPrefs,
    private val coincore: Coincore,
    private val quickActionsService: QuickActionsService,
    private val fiatActions: FiatActionsUseCase
) : MviViewModel<
    QuickActionsIntent,
    QuickActionsViewState,
    QuickActionsModelState,
    QuickActionsNavEvent,
    ModelConfigArgs.NoArgs>(
    QuickActionsModelState()
) {
    private var quickActionsJob: Job? = null
    private var moreActionsJob: Job? = null
    private var fiatActionJob: Job? = null

    override fun viewCreated(args: ModelConfigArgs.NoArgs) {}

    override fun reduce(state: QuickActionsModelState): QuickActionsViewState {
        return with(state) {
            QuickActionsViewState(
                actions = this.quickActions,
                moreActions = this.moreActions
            )
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override suspend fun handleIntent(modelState: QuickActionsModelState, intent: QuickActionsIntent) {
        when (intent) {
            is QuickActionsIntent.LoadActions -> when (intent.type) {
                ActionType.Quick -> {
                    quickActionsJob?.cancel()
                    quickActionsJob = viewModelScope.launch {
                        walletModeService.walletMode.onEach { wMode ->
                            updateState {
                                it.copy(
                                    quickActions = modelState.actionsForMode(wMode)
                                )
                            }
                        }.flatMapLatest { wMode ->
                            val actionsFlow = quickActionsService.availableQuickActionsForWalletMode(
                                walletMode = wMode,
                                freshnessStrategy = PullToRefreshUtils.freshnessStrategy(intent.forceRefresh)
                            ).map { actions ->
                                actions.map {
                                    it.toQuickActionItem()
                                }
                            }

                            val moreActionsAvailableFlow =
                                loadMoreActions(walletMode = wMode, forceRefresh = intent.forceRefresh)
                                    .onStart {
                                        emit(emptyList())
                                    }
                                    .map { list ->
                                        list.isNotEmpty()
                                    }

                            combine(actionsFlow, moreActionsAvailableFlow) { actions, moreActionsAvailable ->
                                if (moreActionsAvailable) {
                                    actions + QuickActionItem(
                                        title = R.string.common_more,
                                        action = QuickAction.More,
                                        enabled = true
                                    )
                                } else {
                                    actions
                                }
                            }.map { wMode to it }
                        }.collectLatest { (walletMode, actions) ->
                            Timber.d("Rendering Quick Actions for $walletMode with -> $actions")
                            updateState {
                                it.copy(
                                    quickActions = actions
                                )
                            }
                            modelState.cacheActions(actions, walletMode)
                        }
                    }
                }
                ActionType.More -> {
                    moreActionsJob?.cancel()
                    moreActionsJob = viewModelScope.launch {
                        walletModeService.walletMode.flatMapLatest { walletMode ->
                            loadMoreActions(walletMode = walletMode, forceRefresh = false)
                        }.collectLatest { actions ->
                            updateState {
                                it.copy(
                                    moreActions = actions
                                )
                            }
                        }
                    }
                }
            }
            is QuickActionsIntent.FiatAction -> {
                handleFiatAction(action = intent.action)
            }
            QuickActionsIntent.Refresh -> {
                updateState {
                    it.copy(lastFreshDataTime = CurrentTimeProvider.currentTimeMillis())
                }

                onIntent(QuickActionsIntent.LoadActions(ActionType.Quick, forceRefresh = true))
            }
        }
    }

    private fun loadMoreActions(
        walletMode: WalletMode,
        forceRefresh: Boolean
    ): Flow<List<MoreActionItem>> {
        return quickActionsService.moreActions(
            walletMode = walletMode,
            freshnessStrategy = PullToRefreshUtils.freshnessStrategy(forceRefresh)
        ).map { stateAwareActions ->
            Timber.d("Rendering More section with -> $stateAwareActions")
            stateAwareActions.map {
                it.toMoreActionItem()
            }
        }
    }

    private fun handleFiatAction(action: AssetAction) {
        fiatActionJob?.cancel()
        fiatActionJob = viewModelScope.launch {
            val account = coincore.allFiats()
                .map {
                    it.first {
                        it.currency.networkTicker == currencyPrefs.selectedFiatCurrency.networkTicker
                    } as FiatAccount
                }
                .await()

            when (action) {
                AssetAction.FiatDeposit -> fiatActions.deposit(
                    account = account,
                    action = action,
                    shouldLaunchBankLinkTransfer = false,
                    shouldSkipQuestionnaire = false
                )
                AssetAction.FiatWithdraw -> handleWithdraw(
                    account = account,
                    action = action
                )
                else -> {
                }
            }
        }
    }

    private suspend fun handleWithdraw(account: FiatAccount, action: AssetAction) {
        require(action == AssetAction.FiatWithdraw) { "action is not AssetAction.FiatWithdraw" }

        account.canWithdrawFunds()
            .collectLatest { dataResource ->
                when (dataResource) {
                    DataResource.Loading -> {
                        //                        updateState {
                        //                            it.copy(
                        //                                withdrawChecksLoading = true,
                        //                                actionError = FiatActionError.None
                        //                            )
                        //                        }
                    }
                    is DataResource.Data -> {
                        //                        updateState { it.copy(withdrawChecksLoading = false) }

                        dataResource.data.let { canWithdrawFunds ->
                            if (canWithdrawFunds) {
                                fiatActions.withdraw(
                                    account = account,
                                    action = action,
                                    shouldLaunchBankLinkTransfer = false,
                                    shouldSkipQuestionnaire = false
                                )
                            } else {
                                //                                updateState { it.copy(actionError = FiatActionError.WithdrawalInProgress) }
                                //                                startDismissErrorTimeout()
                            }
                        }
                    }
                    is DataResource.Error -> {
                        //                        updateState {
                        //                            it.copy(
                        //                                withdrawChecksLoading = false,
                        //                                actionError = FiatActionError.Unknown
                        //                            )
                        //                        }
                        //                        startDismissErrorTimeout()
                    }
                }
            }
    }
}

fun StateAwareAction.toQuickActionItem(): QuickActionItem {
    return when (this.action) {
        AssetAction.Buy -> QuickActionItem(
            title = R.string.common_buy,
            enabled = this.state == ActionState.Available,
            action = QuickAction.TxAction(AssetAction.Buy),
        )
        AssetAction.Sell -> QuickActionItem(
            title = R.string.common_sell,
            enabled = this.state == ActionState.Available,
            action = QuickAction.TxAction(AssetAction.Sell),
        )
        AssetAction.Swap -> QuickActionItem(
            title = R.string.common_swap,
            enabled = this.state == ActionState.Available,
            action = QuickAction.TxAction(AssetAction.Swap),
        )
        AssetAction.Receive -> QuickActionItem(
            title = R.string.common_receive,
            enabled = this.state == ActionState.Available,
            action = QuickAction.TxAction(AssetAction.Receive),
        )
        AssetAction.Send -> QuickActionItem(
            title = R.string.common_send,
            enabled = this.state == ActionState.Available,
            action = QuickAction.TxAction(AssetAction.Send),
        )
        // what should we do with these?
        else -> throw IllegalStateException(
            "Action ${this.action} not supported for quick action menu"
        )
    }
}

fun StateAwareAction.toMoreActionItem(): MoreActionItem {
    return when (this.action) {
        AssetAction.Send -> MoreActionItem(
            icon = R.drawable.ic_more_send,
            title = R.string.common_send,
            subtitle = R.string.transfer_to_other_wallets,
            action = QuickAction.TxAction(AssetAction.Send),
            enabled = this.state == ActionState.Available
        )
        AssetAction.FiatDeposit -> MoreActionItem(
            icon = R.drawable.ic_more_deposit,
            title = R.string.common_add_cash,
            subtitle = R.string.add_cash_from_your_bank_or_card,
            action = QuickAction.TxAction(AssetAction.FiatDeposit),
            enabled = this.state == ActionState.Available
        )
        AssetAction.FiatWithdraw -> MoreActionItem(
            icon = R.drawable.ic_more_withdraw,
            title = R.string.common_cash_out,
            subtitle = R.string.cash_out_bank,
            action = QuickAction.TxAction(AssetAction.FiatWithdraw),
            enabled = this.state == ActionState.Available
        )
        AssetAction.Buy -> MoreActionItem(
            icon = R.drawable.ic_activity_buy,
            title = R.string.common_buy,
            subtitle = R.string.buy_crypto,
            action = QuickAction.TxAction(AssetAction.Buy),
            enabled = this.state == ActionState.Available
        )
        AssetAction.Sell -> MoreActionItem(
            icon = R.drawable.ic_activity_sell,
            title = R.string.common_sell,
            subtitle = R.string.sell_crypto,
            action = QuickAction.TxAction(AssetAction.Sell),
            enabled = this.state == ActionState.Available
        )
        AssetAction.Swap -> MoreActionItem(
            icon = R.drawable.ic_activity_swap,
            title = R.string.common_swap,
            subtitle = R.string.swap_header_label,
            action = QuickAction.TxAction(AssetAction.Swap),
            enabled = this.state == ActionState.Available
        )
        AssetAction.Receive -> MoreActionItem(
            icon = R.drawable.ic_activity_receive,
            title = R.string.common_receive,
            subtitle = R.string.receive_to_your_wallet,
            action = QuickAction.TxAction(AssetAction.Receive),
            enabled = this.state == ActionState.Available
        )
        AssetAction.ViewActivity,
        AssetAction.ViewStatement,
        AssetAction.InterestDeposit,
        AssetAction.InterestWithdraw,
        AssetAction.Sign,
        AssetAction.StakingDeposit -> throw IllegalStateException(
            "Action ${this.action} not supported for more menu"
        )
    }
}

data class QuickActionsModelState(
    val quickActions: List<QuickActionItem> = emptyList(),
    val moreActions: List<MoreActionItem> = emptyList(),
    private val _actionsForMode: MutableMap<WalletMode, List<QuickActionItem>> = mutableMapOf(),
    val lastFreshDataTime: Long = 0
) : ModelState {
    fun actionsForMode(walletMode: WalletMode): List<QuickActionItem> =
        _actionsForMode[walletMode] ?: emptyList()

    fun cacheActions(actions: List<QuickActionItem>, walletMode: WalletMode) {
        _actionsForMode[walletMode] = actions
    }
}

data class QuickActionItem(
    val title: Int,
    val enabled: Boolean,
    val action: QuickAction
)

data class MoreActionItem(
    val icon: Int,
    val title: Int,
    val subtitle: Int,
    val action: QuickAction.TxAction,
    val enabled: Boolean
)

sealed interface QuickActionsIntent : Intent<QuickActionsModelState> {
    class LoadActions(
        val type: ActionType,
        val forceRefresh: Boolean = false
    ) : QuickActionsIntent

    data class FiatAction(
        val action: AssetAction,
    ) : QuickActionsIntent

    object Refresh : QuickActionsIntent {
        override fun isValidFor(modelState: QuickActionsModelState): Boolean {
            return PullToRefreshUtils.canRefresh(modelState.lastFreshDataTime)
        }
    }
}

sealed class QuickAction {
    data class TxAction(val assetAction: AssetAction) : QuickAction()
    object More : QuickAction()
}

data class QuickActionsViewState(
    val actions: List<QuickActionItem>,
    val moreActions: List<MoreActionItem>
) : ViewState

enum class ActionType {
    Quick, More
}

enum class QuickActionsNavEvent : NavigationEvent {
    Buy, Sell, Receive, Send, Swap
}
