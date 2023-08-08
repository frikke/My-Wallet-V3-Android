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
import com.blockchain.core.kyc.domain.KycService
import com.blockchain.core.kyc.domain.model.KycTier
import com.blockchain.core.kyc.domain.model.KycTierState
import com.blockchain.data.DataResource
import com.blockchain.data.FreshnessStrategy
import com.blockchain.data.dataOrElse
import com.blockchain.data.filterNotLoading
import com.blockchain.data.map
import com.blockchain.data.mapData
import com.blockchain.domain.fiatcurrencies.FiatCurrenciesService
import com.blockchain.featureflag.FeatureFlag
import com.blockchain.fiatActions.fiatactions.FiatActionsUseCase
import com.blockchain.home.actions.QuickActionsService
import com.blockchain.home.handhold.HandholdService
import com.blockchain.home.handhold.isMandatory
import com.blockchain.home.presentation.R
import com.blockchain.nabu.Feature
import com.blockchain.nabu.api.getuser.domain.UserFeaturePermissionService
import com.blockchain.outcome.getOrNull
import com.blockchain.presentation.pulltorefresh.PullToRefresh
import com.blockchain.utils.CurrentTimeProvider
import com.blockchain.utils.awaitOutcome
import com.blockchain.walletmode.WalletMode
import com.blockchain.walletmode.WalletModeService
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class QuickActionsViewModel(
    private val fiatCurrenciesService: FiatCurrenciesService,
    private val coincore: Coincore,
    private val dexFeatureFlag: FeatureFlag,
    private val userFeaturePermissionService: UserFeaturePermissionService,
    private val quickActionsService: QuickActionsService,
    private val fiatActions: FiatActionsUseCase,
    private val dispatcher: CoroutineDispatcher,
    private val handholdService: HandholdService,
    private val walletModeService: WalletModeService,
    private val kycService: KycService,
) : MviViewModel<
    QuickActionsIntent,
    QuickActionsViewState,
    QuickActionsModelState,
    QuickActionsNavEvent,
    ModelConfigArgs.NoArgs
    >(
    QuickActionsModelState()
) {
    private var loadActionsJob: Job? = null
    private var fiatActionJob: Job? = null

    override fun viewCreated(args: ModelConfigArgs.NoArgs) {}

    override fun QuickActionsModelState.reduce(): QuickActionsViewState {
        return maxQuickActionsOnScreen?.let { maxQuickActionsOnScreen ->
            val quickActionItemsCount = if (quickActions.size <= maxQuickActionsOnScreen) {
                maxQuickActionsOnScreen
            } else {
                // (maxQuickActionsOnScreen - 1) to leave space for More action
                (maxQuickActionsOnScreen - 1).coerceAtMost(
                    quickActions.filter { it.state == ActionState.Available }.size
                )
            }

            val quickActionItems = if (quickActions.size > quickActionItemsCount) {
                quickActions.subList(0, quickActionItemsCount).map { it.toQuickActionItem() }.plus(
                    QuickActionItem(
                        title = com.blockchain.stringResources.R.string.common_more,
                        action = QuickAction.More,
                        enabled = true
                    )
                )
            } else {
                quickActions.map { it.toQuickActionItem() }
            }

            val moreActions = if (quickActions.size > quickActionItemsCount) {
                quickActions.subList(quickActionItemsCount, quickActions.size)
                    .map { it.toMoreActionItem() }
            } else {
                emptyList()
            }

            QuickActionsViewState(
                actions = quickActionItems,
                moreActions = moreActions
            )
        } ?: QuickActionsViewState(
            actions = emptyList(),
            moreActions = emptyList()
        )
    }

    override suspend fun handleIntent(modelState: QuickActionsModelState, intent: QuickActionsIntent) {
        when (intent) {
            is QuickActionsIntent.LoadActions -> {

                val isDefiOnly = onlyDefiAvailable()

                updateState {
                    copy(maxQuickActionsOnScreen = intent.maxQuickActionsOnScreen)
                }

                updateState {
                    copy(
                        walletMode = intent.walletMode
                    )
                }

                loadActions(intent.walletMode, isDefiOnly)
                loadDexState()
                loadKycState()
            }

            is QuickActionsIntent.FiatAction -> {
                handleFiatAction(action = intent.action)
            }

            QuickActionsIntent.Refresh -> {
                check(modelState.walletMode != null)

                updateState {
                    copy(lastFreshDataTime = CurrentTimeProvider.currentTimeMillis())
                }

                quickActionsService.availableQuickActionsForWalletMode(
                    walletMode = modelState.walletMode,
                    freshnessStrategy = FreshnessStrategy.Fresh
                ).collectLatest { actions ->
                    updateState {
                        copy(
                            quickActions = actions
                        )
                    }
                }
            }

            is QuickActionsIntent.ActionClicked -> {
                val navEvent = intent.action.navigationEvent()

                if (navEvent.needsToCompleteKycFirst()) {
                    navigate(QuickActionsNavEvent.KycVerificationPrompt)
                } else {
                    navigate(navEvent)
                }
            }
        }
    }

    private fun QuickActionsNavEvent.needsToCompleteKycFirst(): Boolean {
        return when (this) {
            QuickActionsNavEvent.Swap,
            QuickActionsNavEvent.FiatWithdraw,
            QuickActionsNavEvent.Sell,
            QuickActionsNavEvent.Buy -> {
                !modelState.isKycCompleted
            }
            QuickActionsNavEvent.Receive,
            QuickActionsNavEvent.Send,
            QuickActionsNavEvent.DexOrSwapOption,
            QuickActionsNavEvent.More,
            QuickActionsNavEvent.KycVerificationPrompt -> false
        }
    }

    private suspend fun loadDexState() {
        viewModelScope.launch(dispatcher) {
            userFeaturePermissionService.isEligibleFor(Feature.Dex).filterNotLoading().collectLatest {
                updateState {
                    copy(
                        dexEligible = it.dataOrElse(false)
                    )
                }
            }
        }

        viewModelScope.launch(dispatcher) {
            val enabled = dexFeatureFlag.coEnabled()
            updateState {
                copy(
                    dexFFEnabled = enabled
                )
            }
        }
    }

    private suspend fun loadKycState() {
        viewModelScope.launch(dispatcher) {
            kycService.stateFor(tierLevel = KycTier.GOLD)
                .mapData { goldState ->
                    goldState == KycTierState.Verified
                }
                .collectLatest {
                    updateState {
                        copy(
                            isKycCompleted = it.dataOrElse(false)
                        )
                    }
                }
        }
    }

    private fun loadActions(walletMode: WalletMode, isDefiOnly: Boolean) {
        loadActionsJob?.cancel()
        loadActionsJob = viewModelScope.launch(dispatcher) {

            val isHandholdVisibleFlow = when (walletMode) {
                WalletMode.CUSTODIAL -> handholdService.handholdTasksStatus().mapData {
                    it.any { it.task.isMandatory() && !it.isComplete }
                }

                WalletMode.NON_CUSTODIAL -> flowOf(DataResource.Data(false))
            }

            combine(
                isHandholdVisibleFlow.filterNotLoading(),
                quickActionsService.availableQuickActionsForWalletMode(walletMode)
            ) { isHandholdVisible, actions ->
                isHandholdVisible.dataOrElse(false) to actions
            }.collectLatest { (isHandholdVisible, actions) ->
                updateState {
                    copy(
                        quickActions = if (isHandholdVisible) {
                            // show only available actions
                            actions.filter { it.state == ActionState.Available }
                        } else {
                            // if we are in defi only mode, the user can't sell
                            if (isDefiOnly) actions.filter { it.action != AssetAction.Sell }
                            else actions
                        }
                    )
                }
            }
        }
    }

    private fun QuickActionItem.navigationEvent(): QuickActionsNavEvent {
        check(modelState.walletMode != null)
        val assetAction = (action as? QuickAction.TxAction)?.assetAction ?: return QuickActionsNavEvent.More
        return when (assetAction) {
            AssetAction.Send -> QuickActionsNavEvent.Send
            AssetAction.Swap -> {
                if (modelState.canUseDex()) {
                    QuickActionsNavEvent.DexOrSwapOption
                } else {
                    QuickActionsNavEvent.Swap
                }
            }

            AssetAction.Sell -> QuickActionsNavEvent.Sell
            AssetAction.Buy -> QuickActionsNavEvent.Buy
            AssetAction.FiatWithdraw -> QuickActionsNavEvent.FiatWithdraw
            AssetAction.Receive -> QuickActionsNavEvent.Receive
            AssetAction.FiatDeposit,
            AssetAction.InterestDeposit,
            AssetAction.InterestWithdraw,
            AssetAction.ViewActivity,
            AssetAction.Sign,
            AssetAction.ViewStatement,
            AssetAction.StakingDeposit,
            AssetAction.StakingWithdraw,
            AssetAction.ActiveRewardsDeposit,
            AssetAction.ActiveRewardsWithdraw -> throw IllegalStateException("Action not supported")
        }
    }

    private fun handleFiatAction(action: AssetAction) {
        fiatActionJob?.cancel()
        fiatActionJob = viewModelScope.launch(dispatcher) {
            val accountOutcome = coincore.allFiats().map {
                (
                    it.firstOrNull { acc ->
                        acc.currency.networkTicker == fiatCurrenciesService.selectedTradingCurrency.networkTicker
                    } ?: NullFiatAccount
                    ) as FiatAccount
            }
                .awaitOutcome()
            val account = accountOutcome.getOrNull() ?: return@launch

            when {
                account == NullFiatAccount -> fiatActions.noEligibleAccount(
                    fiatCurrenciesService.selectedTradingCurrency
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
                        //                       updateState { copy(withdrawChecksLoading = false) }

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

    private suspend fun onlyDefiAvailable(): Boolean {
        val modes = walletModeService.availableModes()
        return modes.size == 1 && modes.first() == WalletMode.NON_CUSTODIAL
    }
}

fun StateAwareAction.toQuickActionItem(): QuickActionItem {
    return when (this.action) {
        AssetAction.Buy -> QuickActionItem(
            title = com.blockchain.stringResources.R.string.common_buy,
            enabled = this.state == ActionState.Available,
            action = QuickAction.TxAction(AssetAction.Buy)
        )

        AssetAction.Sell -> QuickActionItem(
            title = com.blockchain.stringResources.R.string.common_sell,
            enabled = this.state == ActionState.Available,
            action = QuickAction.TxAction(AssetAction.Sell)
        )

        AssetAction.Swap -> QuickActionItem(
            title = com.blockchain.stringResources.R.string.common_swap,
            enabled = this.state == ActionState.Available,
            action = QuickAction.TxAction(AssetAction.Swap)
        )

        AssetAction.Receive -> QuickActionItem(
            title = com.blockchain.stringResources.R.string.common_deposit,
            enabled = this.state == ActionState.Available,
            action = QuickAction.TxAction(AssetAction.Receive)
        )

        AssetAction.Send -> QuickActionItem(
            title = com.blockchain.stringResources.R.string.common_send,
            enabled = this.state == ActionState.Available,
            action = QuickAction.TxAction(AssetAction.Send)
        )

        AssetAction.FiatWithdraw -> QuickActionItem(
            title = com.blockchain.stringResources.R.string.common_cash_out,
            enabled = this.state == ActionState.Available,
            action = QuickAction.TxAction(AssetAction.FiatWithdraw)
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
            title = com.blockchain.stringResources.R.string.common_send,
            subtitle = com.blockchain.stringResources.R.string.transfer_to_other_wallets,
            action = QuickAction.TxAction(AssetAction.Send),
            enabled = this.state == ActionState.Available
        )

        AssetAction.FiatWithdraw -> MoreActionItem(
            icon = R.drawable.ic_more_withdraw,
            title = com.blockchain.stringResources.R.string.common_cash_out,
            subtitle = com.blockchain.stringResources.R.string.cash_out_bank,
            action = QuickAction.TxAction(AssetAction.FiatWithdraw),
            enabled = this.state == ActionState.Available
        )

        AssetAction.Buy -> MoreActionItem(
            icon = R.drawable.ic_activity_buy,
            title = com.blockchain.stringResources.R.string.common_buy,
            subtitle = com.blockchain.stringResources.R.string.buy_crypto,
            action = QuickAction.TxAction(AssetAction.Buy),
            enabled = this.state == ActionState.Available
        )

        AssetAction.Sell -> MoreActionItem(
            icon = R.drawable.ic_activity_sell,
            title = com.blockchain.stringResources.R.string.common_sell,
            subtitle = com.blockchain.stringResources.R.string.sell_crypto,
            action = QuickAction.TxAction(AssetAction.Sell),
            enabled = this.state == ActionState.Available
        )

        AssetAction.Swap -> MoreActionItem(
            icon = R.drawable.ic_activity_swap,
            title = com.blockchain.stringResources.R.string.common_swap,
            subtitle = com.blockchain.stringResources.R.string.swap_header_label,
            action = QuickAction.TxAction(AssetAction.Swap),
            enabled = this.state == ActionState.Available
        )

        AssetAction.Receive -> MoreActionItem(
            icon = R.drawable.ic_activity_receive,
            title = com.blockchain.stringResources.R.string.common_receive,
            subtitle = com.blockchain.stringResources.R.string.receive_to_your_wallet,
            action = QuickAction.TxAction(AssetAction.Receive),
            enabled = this.state == ActionState.Available
        )

        AssetAction.FiatDeposit,
        AssetAction.ViewActivity,
        AssetAction.ViewStatement,
        AssetAction.InterestDeposit,
        AssetAction.InterestWithdraw,
        AssetAction.Sign,
        AssetAction.StakingDeposit,
        AssetAction.StakingWithdraw,
        AssetAction.ActiveRewardsWithdraw,
        AssetAction.ActiveRewardsDeposit -> throw IllegalStateException(
            "Action ${this.action} not supported for more menu"
        )
    }
}

data class QuickActionsModelState(
    val quickActions: List<StateAwareAction> = emptyList(),
    val maxQuickActionsOnScreen: Int? = null,
    private val dexFFEnabled: Boolean = false,
    private val dexEligible: Boolean = false,
    val walletMode: WalletMode? = null,
    val lastFreshDataTime: Long = 0,
    val isKycCompleted: Boolean = false
) : ModelState {
    fun canUseDex() =
        dexFFEnabled && dexEligible && walletMode == WalletMode.NON_CUSTODIAL
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
    data class LoadActions(
        val walletMode: WalletMode,
        val maxQuickActionsOnScreen: Int
    ) : QuickActionsIntent

    object Refresh : QuickActionsIntent {
        override fun isValidFor(modelState: QuickActionsModelState): Boolean {
            return modelState.walletMode != null && PullToRefresh.canRefresh(modelState.lastFreshDataTime)
        }
    }

    data class ActionClicked(val action: QuickActionItem) : QuickActionsIntent

    data class FiatAction(
        val action: AssetAction
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
    Buy, Sell, Receive, Send, Swap, DexOrSwapOption, More, FiatWithdraw, KycVerificationPrompt
}
