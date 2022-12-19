package com.blockchain.home.presentation.quickactions

import androidx.lifecycle.viewModelScope
import com.blockchain.coincore.AccountBalance
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
import com.blockchain.data.FreshnessStrategy
import com.blockchain.data.onErrorReturn
import com.blockchain.fiatActions.fiatactions.FiatActionsUseCase
import com.blockchain.home.presentation.R
import com.blockchain.home.presentation.fiat.actions.hasAvailableAction
import com.blockchain.nabu.Feature
import com.blockchain.nabu.api.getuser.domain.UserFeaturePermissionService
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.store.filterNotLoading
import com.blockchain.utils.asFlow
import com.blockchain.walletmode.WalletMode
import com.blockchain.walletmode.WalletModeService
import io.reactivex.rxjava3.core.Observable
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.zip
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx3.asFlow
import kotlinx.coroutines.rx3.await

class QuickActionsViewModel(
    private val userFeaturePermissionService: UserFeaturePermissionService,
    private val walletModeService: WalletModeService,
    private val currencyPrefs: CurrencyPrefs,
    private val coincore: Coincore,
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
            //    is QuickActionsIntent.ActionClicked -> handleActionForMode(intent.action)
            is QuickActionsIntent.LoadActions -> when (intent.type) {
                ActionType.Quick -> walletModeService.walletMode.onEach { wMode ->
                    updateState {
                        it.copy(
                            walletMode = wMode,
                            quickActions = modelState.actionsForMode(wMode)
                        )
                    }
                }.flatMapLatest { wMode ->
                    if (wMode == WalletMode.NON_CUSTODIAL_ONLY)
                        actionsForDefi() else
                        actionsForBrokerage()
                }.collectLatest { actions ->
                    updateState {
                        it.copy(
                            quickActions = actions
                        )
                    }
                }
                ActionType.More -> walletModeService.walletMode.onEach { wMode ->
                    updateState {
                        it.copy(
                            walletMode = wMode
                        )
                    }
                }.flatMapLatest {
                    loadMoreActions()
                }.collectLatest { actions ->
                    updateState {
                        it.copy(
                            moreActions = actions
                        )
                    }
                }
            }
            is QuickActionsIntent.FiatAction -> {
                handleFiatAction(action = intent.action)
            }
        }
    }

    private fun loadMoreActions(): Flow<List<MoreActionItem>> {
        val custodialBalance = totalWalletModeBalance(WalletMode.CUSTODIAL_ONLY)
        val hasFiatBalance =
            coincore.activeWallets(WalletMode.CUSTODIAL_ONLY).map { it.accounts.filterIsInstance<FiatAccount>() }
                .flatMapObservable {
                    if (it.isEmpty()) {
                        Observable.just(false)
                    } else
                        it[0].balanceRx.map { balance ->
                            balance.total.isPositive
                        }
                }.asFlow()

        val depositFiatFeature =
            userFeaturePermissionService.isEligibleFor(Feature.DepositFiat).filterNot { it is DataResource.Loading }
                .onErrorReturn { false }
                .map {
                    (it as? DataResource.Data<Boolean>)?.data ?: throw IllegalStateException("Data should be returned")
                }
        val withdrawFiatFeature =
            userFeaturePermissionService.isEligibleFor(Feature.WithdrawFiat).filterNot { it is DataResource.Loading }
                .onErrorReturn { false }
                .map {
                    (it as? DataResource.Data<Boolean>)?.data ?: throw IllegalStateException("Data should be returned")
                }

        val stateAwareActions = coincore.allFiats()
            .map {
                it.first { it.currency.networkTicker == currencyPrefs.selectedFiatCurrency.networkTicker }
                    as FiatAccount
            }.flatMap {
                it.stateAwareActions
            }
            .asFlow()

        return combine(
            custodialBalance,
            depositFiatFeature,
            withdrawFiatFeature,
            hasFiatBalance,
            stateAwareActions
        ) { balance, depositEnabled, withdrawEnabled, hasAnyFiatBalance, actions ->
            listOf(
                MoreActionItem(
                    icon = R.drawable.ic_more_send,
                    title = R.string.common_send,
                    subtitle = R.string.transfer_to_other_wallets,
                    action = QuickAction.TxAction(AssetAction.Send),
                    enabled = balance.total.isPositive
                ),
                MoreActionItem(
                    icon = R.drawable.ic_more_deposit,
                    title = R.string.common_deposit,
                    subtitle = R.string.add_cash_from_your_bank_or_card,
                    action = QuickAction.TxAction(AssetAction.FiatDeposit),
                    enabled = depositEnabled && hasAnyFiatBalance && actions.hasAvailableAction(AssetAction.FiatDeposit)
                ),
                MoreActionItem(
                    icon = R.drawable.ic_more_withdraw,
                    title = R.string.common_withdraw,
                    subtitle = R.string.cash_out_bank,
                    action = QuickAction.TxAction(AssetAction.FiatWithdraw),
                    enabled = withdrawEnabled && actions.hasAvailableAction(AssetAction.FiatWithdraw)
                ),
            )
        }
    }

    private fun totalWalletModeBalance(walletMode: WalletMode) =
        coincore.activeWallets(walletMode).flatMapObservable {
            it.balanceRx
        }.asFlow().catch { emit(AccountBalance.zero(currencyPrefs.selectedFiatCurrency)) }

    private fun actionsForDefi(): Flow<List<QuickActionItem>> =
        totalWalletModeBalance(WalletMode.NON_CUSTODIAL_ONLY).zip(
            userFeaturePermissionService.isEligibleFor(
                Feature.Sell,
                FreshnessStrategy.Cached(false)
            ).filterNotLoading()
        ) { balance, sellEligible ->

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

    private fun actionsForBrokerage(): Flow<List<QuickActionItem>> {
        val buyEnabledFlow =
            userFeaturePermissionService.isEligibleFor(Feature.Buy).filterNot { it is DataResource.Loading }
                .onErrorReturn { false }
                .map {
                    (it as? DataResource.Data<Boolean>)?.data ?: throw IllegalStateException("Data should be returned")
                }
        val sellEnabledFlow =
            userFeaturePermissionService.isEligibleFor(Feature.DepositFiat).filterNot { it is DataResource.Loading }
                .onErrorReturn { false }
                .map {
                    (it as? DataResource.Data<Boolean>)?.data ?: throw IllegalStateException("Data should be returned")
                }
        val swapEnabledFlow =
            userFeaturePermissionService.isEligibleFor(Feature.Swap).filterNot { it is DataResource.Loading }
                .onErrorReturn { false }
                .map {
                    (it as? DataResource.Data<Boolean>)?.data ?: throw IllegalStateException("Data should be returned")
                }
        val receiveEnabledFlow =
            userFeaturePermissionService.isEligibleFor(Feature.DepositCrypto).filterNot { it is DataResource.Loading }
                .onErrorReturn { false }
                .map {
                    (it as? DataResource.Data<Boolean>)?.data ?: throw IllegalStateException("Data should be returned")
                }
        val balanceFlow = totalWalletModeBalance(WalletMode.CUSTODIAL_ONLY)

        return combine(
            balanceFlow,
            buyEnabledFlow,
            sellEnabledFlow,
            swapEnabledFlow,
            receiveEnabledFlow
        ) { balance, buyEnabled, sellEnabled, swapEnabled, receiveEnabled ->
            listOf(
                QuickActionItem(
                    title = R.string.common_buy,
                    enabled = buyEnabled,
                    action = QuickAction.TxAction(AssetAction.Buy),
                ),
                QuickActionItem(
                    title = R.string.common_sell,
                    enabled = sellEnabled && balance.total.isPositive,
                    action = QuickAction.TxAction(AssetAction.Sell),
                ),
                QuickActionItem(
                    title = R.string.common_swap,
                    enabled = swapEnabled && balance.total.isPositive,
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
                    enabled = true
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
    val walletMode: WalletMode = WalletMode.UNIVERSAL,
    val quickActions: List<QuickActionItem> = emptyList(),
    val moreActions: List<MoreActionItem> = emptyList(),
    private val _actionsForMode: MutableMap<WalletMode, List<QuickActionItem>> = mutableMapOf()
) : ModelState {
    init {
        _actionsForMode[walletMode] = quickActions
    }

    fun actionsForMode(walletMode: WalletMode): List<QuickActionItem> =
        _actionsForMode[walletMode] ?: emptyList()
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
    // class ActionClicked(val action: QuickAction) : QuickActionsIntent()
    class LoadActions(val type: ActionType) : QuickActionsIntent

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

enum class ActionType {
    Quick, More
}

enum class QuickActionsNavEvent : NavigationEvent {
    Buy, Sell, Receive, Send, Swap
}
