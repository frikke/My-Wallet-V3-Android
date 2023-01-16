package com.blockchain.home.presentation.quickactions

import androidx.lifecycle.viewModelScope
import com.blockchain.coincore.AccountBalance
import com.blockchain.coincore.ActionState
import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.Coincore
import com.blockchain.coincore.FiatAccount
import com.blockchain.commonarch.presentation.mvi_v2.Intent
import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.commonarch.presentation.mvi_v2.ModelState
import com.blockchain.commonarch.presentation.mvi_v2.MviViewModel
import com.blockchain.commonarch.presentation.mvi_v2.NavigationEvent
import com.blockchain.commonarch.presentation.mvi_v2.ViewState
import com.blockchain.data.DataResource
import com.blockchain.data.RefreshStrategy
import com.blockchain.data.onErrorReturn
import com.blockchain.fiatActions.fiatactions.FiatActionsUseCase
import com.blockchain.home.actions.QuickActionsService
import com.blockchain.home.presentation.R
import com.blockchain.nabu.Feature
import com.blockchain.nabu.api.getuser.domain.UserFeaturePermissionService
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.presentation.pulltorefresh.canRefresh
import com.blockchain.presentation.pulltorefresh.ptrFreshnessStrategy
import com.blockchain.store.filterNotLoading
import com.blockchain.utils.CurrentTimeProvider
import com.blockchain.walletmode.WalletMode
import com.blockchain.walletmode.WalletModeService
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.zip
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx3.asFlow
import kotlinx.coroutines.rx3.await

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
                            if (wMode == WalletMode.NON_CUSTODIAL) {
                                actionsForDefi(intent.forceRefresh)
                            } else {
                                actionsForBrokerage(intent.forceRefresh)
                            }.map { wMode to it }
                        }.collectLatest { (walletMode, actions) ->
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
                        walletModeService.walletMode.flatMapLatest {
                            loadMoreActions()
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
            QuickActionsIntent.RefreshRequested -> {
                updateState {
                    it.copy(lastFreshDataTime = CurrentTimeProvider.currentTimeMillis())
                }

                onIntent(QuickActionsIntent.LoadActions(ActionType.Quick, forceRefresh = true))
            }
        }
    }

    private fun loadMoreActions(): Flow<List<MoreActionItem>> {
        return quickActionsService.moreActions().map { assetAwareActions ->
            assetAwareActions.map {
                when (it.action) {
                    AssetAction.Send -> MoreActionItem(
                        icon = R.drawable.ic_more_send,
                        title = R.string.common_send,
                        subtitle = R.string.transfer_to_other_wallets,
                        action = QuickAction.TxAction(AssetAction.Send),
                        enabled = it.state == ActionState.Available
                    )
                    AssetAction.FiatDeposit -> MoreActionItem(
                        icon = R.drawable.ic_more_deposit,
                        title = R.string.common_deposit,
                        subtitle = R.string.add_cash_from_your_bank_or_card,
                        action = QuickAction.TxAction(AssetAction.FiatDeposit),
                        enabled = it.state == ActionState.Available
                    )
                    AssetAction.FiatWithdraw -> MoreActionItem(
                        icon = R.drawable.ic_more_withdraw,
                        title = R.string.common_withdraw,
                        subtitle = R.string.cash_out_bank,
                        action = QuickAction.TxAction(AssetAction.FiatWithdraw),
                        enabled = it.state == ActionState.Available
                    )
                    AssetAction.ViewActivity,
                    AssetAction.ViewStatement,
                    AssetAction.Swap,
                    AssetAction.Sell,
                    AssetAction.Buy,
                    AssetAction.Receive,
                    AssetAction.InterestDeposit,
                    AssetAction.InterestWithdraw,
                    AssetAction.Sign,
                    AssetAction.StakingDeposit -> throw IllegalStateException(
                        "Action not supported for more menu"
                    )
                }
            }
        }
    }

    private fun totalWalletModeBalance(walletMode: WalletMode, forceRefresh: Boolean) =
        coincore.activeWalletsInModeRx(
            walletMode = walletMode,
            freshnessStrategy = ptrFreshnessStrategy(
                shouldGetFresh = forceRefresh,
                cacheStrategy = RefreshStrategy.ForceRefresh
            )
        ).flatMap {
            it.balanceRx()
        }.asFlow()
            .catch {
                println("--------- catch")
                it.printStackTrace()
                emit(AccountBalance.zero(currencyPrefs.selectedFiatCurrency))
            }.onStart {
                println("--------- onStart")
                AccountBalance.zero(currencyPrefs.selectedFiatCurrency)
            }
            .onEach {
                println("--------- onEach")
            }

    private fun actionsForDefi(forceRefresh: Boolean): Flow<List<QuickActionItem>> =
        totalWalletModeBalance(WalletMode.NON_CUSTODIAL, forceRefresh)
            .zip(
                userFeaturePermissionService.isEligibleFor(
                    Feature.Sell,
                    ptrFreshnessStrategy(shouldGetFresh = forceRefresh)
                ).filterNotLoading()
            ) { balance, sellEligible ->
                println("--------- balance ${balance.total.toStringWithSymbol()}")

                listOf(
                    QuickActionItem(
                        title = R.string.common_swap,
                        action = QuickAction.TxAction(AssetAction.Swap),
                        enabled = balance.total.isPositive
                    ),
                    QuickActionItem(
                        title = R.string.common_receive,
                        enabled = true,
                        action = QuickAction.TxAction(AssetAction.Receive),
                    ),
                    QuickActionItem(
                        title = R.string.common_send,
                        enabled = balance.total.isPositive,
                        action = QuickAction.TxAction(AssetAction.Send),
                    ),
                    QuickActionItem(
                        title = R.string.common_sell,
                        enabled = balance.total.isPositive &&
                            (sellEligible as? DataResource.Data)?.data ?: false,
                        action = QuickAction.TxAction(AssetAction.Sell),
                    )

                )
            }

    private fun actionsForBrokerage(forceRefresh: Boolean): Flow<List<QuickActionItem>> {
        val buyEnabledFlow =
            userFeaturePermissionService.isEligibleFor(
                feature = Feature.Buy,
                freshnessStrategy = ptrFreshnessStrategy(shouldGetFresh = forceRefresh)
            ).filterNot { it is DataResource.Loading }
                .onErrorReturn { false }
                .map {
                    (it as? DataResource.Data<Boolean>)?.data ?: throw IllegalStateException("Data should be returned")
                }
        val sellEnabledFlow =
            userFeaturePermissionService.isEligibleFor(
                feature = Feature.DepositFiat,
                freshnessStrategy = ptrFreshnessStrategy(shouldGetFresh = forceRefresh)
            ).filterNot { it is DataResource.Loading }
                .onErrorReturn { false }
                .map {
                    (it as? DataResource.Data<Boolean>)?.data ?: throw IllegalStateException("Data should be returned")
                }
        val swapEnabledFlow =
            userFeaturePermissionService.isEligibleFor(
                feature = Feature.Swap,
                freshnessStrategy = ptrFreshnessStrategy(shouldGetFresh = forceRefresh)
            ).filterNot { it is DataResource.Loading }
                .onErrorReturn { false }
                .map {
                    (it as? DataResource.Data<Boolean>)?.data ?: throw IllegalStateException("Data should be returned")
                }
        val receiveEnabledFlow =
            userFeaturePermissionService.isEligibleFor(
                feature = Feature.DepositCrypto,
                freshnessStrategy = ptrFreshnessStrategy(shouldGetFresh = forceRefresh)
            ).filterNot { it is DataResource.Loading }
                .onErrorReturn { false }
                .map {
                    (it as? DataResource.Data<Boolean>)?.data ?: throw IllegalStateException("Data should be returned")
                }
        val balanceFlow = totalWalletModeBalance(WalletMode.CUSTODIAL, forceRefresh).map { it.totalFiat.isPositive }
        val more = loadMoreActions().onStart { emit(emptyList()) }.map { list -> list.any { it.enabled } }

        return combine(
            balanceFlow,
            buyEnabledFlow,
            sellEnabledFlow,
            swapEnabledFlow,
            receiveEnabledFlow,
            more
        ) { config ->
            val balanceIsPositive = config[0]
            val buyEnabled = config[1]
            val sellEnabled = config[2]
            val swapEnabled = config[3]
            val receiveEnabled = config[4]
            val moreEnabled = config[5]
            listOf(
                QuickActionItem(
                    title = R.string.common_buy,
                    enabled = buyEnabled,
                    action = QuickAction.TxAction(AssetAction.Buy),
                ),
                QuickActionItem(
                    title = R.string.common_sell,
                    enabled = sellEnabled && balanceIsPositive,
                    action = QuickAction.TxAction(AssetAction.Sell),
                ),
                QuickActionItem(
                    title = R.string.common_swap,
                    enabled = swapEnabled && balanceIsPositive,
                    action = QuickAction.TxAction(AssetAction.Swap),
                ),
                QuickActionItem(
                    title = R.string.common_receive,
                    enabled = receiveEnabled,
                    action = QuickAction.TxAction(AssetAction.Receive),
                ),
                QuickActionItem(
                    title = R.string.common_more,
                    action = QuickAction.More,
                    enabled = moreEnabled
                )
            )
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

    object RefreshRequested : QuickActionsIntent {
        override fun isValidFor(modelState: QuickActionsModelState): Boolean {
            return canRefresh(modelState.lastFreshDataTime)
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
