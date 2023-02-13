package com.blockchain.home.presentation.quickactions

import androidx.lifecycle.viewModelScope
import com.blockchain.coincore.ActionState
import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.Coincore
import com.blockchain.coincore.FiatAccount
import com.blockchain.coincore.NullFiatAccount
import com.blockchain.coincore.StateAwareAction
import com.blockchain.commonarch.presentation.mvi_v2.Intent
import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.commonarch.presentation.mvi_v2.ModelState
import com.blockchain.commonarch.presentation.mvi_v2.MviViewModel
import com.blockchain.commonarch.presentation.mvi_v2.NavigationEvent
import com.blockchain.commonarch.presentation.mvi_v2.ViewState
import com.blockchain.data.DataResource
import com.blockchain.data.FreshnessStrategy
import com.blockchain.domain.fiatcurrencies.FiatCurrenciesService
import com.blockchain.featureflag.FeatureFlag
import com.blockchain.fiatActions.fiatactions.FiatActionsUseCase
import com.blockchain.home.actions.QuickActionsService
import com.blockchain.home.presentation.R
import com.blockchain.presentation.pulltorefresh.PullToRefresh
import com.blockchain.utils.CurrentTimeProvider
import com.blockchain.walletmode.WalletMode
import com.blockchain.walletmode.WalletModeService
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx3.await

class QuickActionsViewModel(
    private val walletModeService: WalletModeService,
    private val fiatCurrenciesService: FiatCurrenciesService,
    private val coincore: Coincore,
    private val dexFeatureFlag: FeatureFlag,
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
    private var fiatActionJob: Job? = null

    override fun viewCreated(args: ModelConfigArgs.NoArgs) {}
    override fun reduce(state: QuickActionsModelState): QuickActionsViewState = state.run {
        state.maxQuickActionsOnScreen?.let { maxQuickActionsOnScreen ->
            // leave space for More action
            val maxQuickActions = maxQuickActionsOnScreen - 1

            val quickActionItemsCount = maxQuickActions.coerceAtMost(
                state.quickActions.filter { it.state == ActionState.Available }.size
            )

            val quickActions = if (state.quickActions.size > quickActionItemsCount)
                state.quickActions.subList(0, quickActionItemsCount).map { it.toQuickActionItem() }.plus(
                    QuickActionItem(
                        title = R.string.common_more,
                        action = QuickAction.More,
                        enabled = true
                    )
                )
            else state.quickActions.map { it.toQuickActionItem() }

            val moreActions = if (state.quickActions.size > quickActionItemsCount) {
                state.quickActions.subList(quickActionItemsCount, state.quickActions.size)
                    .map { it.toMoreActionItem() }
            } else emptyList()

            QuickActionsViewState(
                actions = quickActions,
                moreActions = moreActions
            )
        } ?: QuickActionsViewState(
            actions = emptyList(),
            moreActions = emptyList()
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override suspend fun handleIntent(modelState: QuickActionsModelState, intent: QuickActionsIntent) {
        when (intent) {
            is QuickActionsIntent.LoadActions -> {
                updateState {
                    it.copy(maxQuickActionsOnScreen = intent.maxQuickActionsOnScreen)
                }

                walletModeService.walletMode.onEach { wMode ->
                    updateState {
                        it.copy(
                            walletMode = wMode
                        )
                    }
                }.flatMapLatest { wMode ->
                    quickActionsService.availableQuickActionsForWalletMode(wMode)
                        .map { actions ->
                            actions to wMode
                        }
                }.collectLatest { (actions, _) ->
                    updateState {
                        it.copy(
                            quickActions = actions
                        )
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
                walletModeService.walletMode.take(1).flatMapLatest {
                    quickActionsService.availableQuickActionsForWalletMode(
                        walletMode = it,
                        freshnessStrategy = FreshnessStrategy.Fresh
                    )
                }.collectLatest { actions ->
                    updateState {
                        it.copy(
                            quickActions = actions
                        )
                    }
                }
            }
            is QuickActionsIntent.ActionClicked -> {
                navigate(intent.action.navigationEvent())
            }
        }
    }

    private suspend fun QuickActionItem.navigationEvent(): QuickActionsNavEvent {
        check(modelState.walletMode != null)
        val assetAction = (action as? QuickAction.TxAction)?.assetAction ?: return QuickActionsNavEvent.More
        return when (assetAction) {
            AssetAction.Send -> QuickActionsNavEvent.Send
            AssetAction.Swap -> {
                if (dexFeatureFlag.coEnabled() && modelState.walletMode == WalletMode.NON_CUSTODIAL) {
                    QuickActionsNavEvent.DexOrSwapOption
                } else {
                    QuickActionsNavEvent.Swap
                }
            }
            AssetAction.Sell -> QuickActionsNavEvent.Sell
            AssetAction.Buy -> QuickActionsNavEvent.Buy
            AssetAction.FiatWithdraw -> QuickActionsNavEvent.FiatWithdraw
            AssetAction.Receive -> QuickActionsNavEvent.Receive
            AssetAction.FiatDeposit -> QuickActionsNavEvent.FiatDeposit
            AssetAction.InterestDeposit,
            AssetAction.InterestWithdraw,
            AssetAction.ViewActivity,
            AssetAction.Sign,
            AssetAction.ViewStatement,
            AssetAction.StakingDeposit,
            AssetAction.ActiveRewardsDeposit -> throw IllegalStateException("Action not supported")
        }
    }

    private fun handleFiatAction(action: AssetAction) {
        fiatActionJob?.cancel()
        fiatActionJob = viewModelScope.launch {
            val account = coincore.allFiats()
                .map {
                    (
                        it.firstOrNull { acc ->
                            acc.currency.networkTicker == fiatCurrenciesService.selectedTradingCurrency.networkTicker
                        } ?: NullFiatAccount
                        ) as FiatAccount
                }
                .await()

            when {
                account == NullFiatAccount -> fiatActions.noEligibleAccount(
                    fiatCurrenciesService.selectedTradingCurrency
                )
                action == AssetAction.FiatDeposit -> fiatActions.deposit(
                    account = account,
                    action = action,
                    shouldLaunchBankLinkTransfer = false,
                    shouldSkipQuestionnaire = false
                )
                action == AssetAction.FiatWithdraw -> handleWithdraw(
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
                    }
                    is DataResource.Data -> {
                        //                       updateState { it.copy(withdrawChecksLoading = false) }

                        dataResource.data.let { canWithdrawFunds ->
                            if (canWithdrawFunds) {
                                fiatActions.withdraw(
                                    account = account,
                                    action = action,
                                    shouldLaunchBankLinkTransfer = false,
                                    shouldSkipQuestionnaire = false
                                )
                            }
                        }
                    }
                    is DataResource.Error -> {
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
        AssetAction.FiatDeposit -> QuickActionItem(
            title = R.string.common_add_cash,
            enabled = this.state == ActionState.Available,
            action = QuickAction.TxAction(AssetAction.FiatDeposit),
        )
        AssetAction.FiatWithdraw -> QuickActionItem(
            title = R.string.common_cash_out,
            enabled = this.state == ActionState.Available,
            action = QuickAction.TxAction(AssetAction.FiatWithdraw),
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
        AssetAction.StakingDeposit,
        AssetAction.ActiveRewardsDeposit -> throw IllegalStateException(
            "Action ${this.action} not supported for more menu"
        )
    }
}

data class QuickActionsModelState(
    val quickActions: List<StateAwareAction> = emptyList(),
    val maxQuickActionsOnScreen: Int? = null,
    val walletMode: WalletMode? = null,
    val lastFreshDataTime: Long = 0,
) : ModelState

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
    data class LoadActions(val maxQuickActionsOnScreen: Int) : QuickActionsIntent
    object Refresh : QuickActionsIntent {
        override fun isValidFor(modelState: QuickActionsModelState): Boolean {
            return PullToRefresh.canRefresh(modelState.lastFreshDataTime)
        }
    }

    data class ActionClicked(val action: QuickActionItem) : QuickActionsIntent

    data class FiatAction(
        val action: AssetAction,
    ) : QuickActionsIntent
}

sealed class QuickAction {
    data class TxAction(val assetAction: AssetAction) : QuickAction()
    object More : QuickAction()
}

data class QuickActionsViewState(
    val actions: List<QuickActionItem>,
    val moreActions: List<MoreActionItem>
) : ViewState

enum class QuickActionsNavEvent : NavigationEvent {
    Buy, Sell, Receive, Send, Swap, DexOrSwapOption, More, FiatDeposit, FiatWithdraw
}
