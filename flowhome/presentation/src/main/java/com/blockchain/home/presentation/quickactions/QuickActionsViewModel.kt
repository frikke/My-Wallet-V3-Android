package com.blockchain.home.presentation.quickactions

import com.blockchain.coincore.AccountBalance
import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.Coincore
import com.blockchain.commonarch.presentation.mvi_v2.Intent
import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.commonarch.presentation.mvi_v2.ModelState
import com.blockchain.commonarch.presentation.mvi_v2.MviViewModel
import com.blockchain.commonarch.presentation.mvi_v2.NavigationEvent
import com.blockchain.commonarch.presentation.mvi_v2.ViewState
import com.blockchain.data.DataResource
import com.blockchain.data.FreshnessStrategy
import com.blockchain.data.onErrorReturn
import com.blockchain.home.presentation.R
import com.blockchain.nabu.Feature
import com.blockchain.nabu.api.getuser.domain.UserFeaturePermissionService
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.store.filterNotLoading
import com.blockchain.walletmode.WalletMode
import com.blockchain.walletmode.WalletModeService
import java.lang.IllegalStateException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.zip
import kotlinx.coroutines.rx3.asFlow

class QuickActionsViewModel(
    private val userFeaturePermissionService: UserFeaturePermissionService,
    private val walletModeService: WalletModeService,
    private val currencyPrefs: CurrencyPrefs,
    private val coincore: Coincore
) : MviViewModel<
    QuickActionsIntent,
    QuickActionsViewState,
    QuickActionsModelState,
    QuickActionsNavEvent,
    ModelConfigArgs.NoArgs>(
    QuickActionsModelState()
) {
    override fun viewCreated(args: ModelConfigArgs.NoArgs) {}

    override fun reduce(state: QuickActionsModelState): QuickActionsViewState {
        return with(state) {
            QuickActionsViewState(
                actions = this.actions
            )
        }
    }

    override suspend fun handleIntent(modelState: QuickActionsModelState, intent: QuickActionsIntent) {
        when (intent) {
            is QuickActionsIntent.ActionClicked -> handleActionForMode(intent.action)
            is QuickActionsIntent.LoadActions -> walletModeService.walletMode.onEach { wMode ->
                updateState {
                    it.copy(
                        walletMode = wMode,
                        actions = modelState.actionsForMode(wMode)
                    )
                }
            }.flatMapLatest { wMode ->
                if (wMode == WalletMode.NON_CUSTODIAL_ONLY)
                    actionsForDefi() else
                    actionsForBrokerage()
            }.collectLatest { actions ->
                updateState {
                    it.copy(
                        actions = actions
                    )
                }
            }
        }
    }

    private fun handleActionForMode(action: AssetAction) {
        when (action) {
            AssetAction.Send -> navigate(QuickActionsNavEvent.Send)
            AssetAction.Swap -> navigate(QuickActionsNavEvent.Swap)
            AssetAction.Sell -> navigate(QuickActionsNavEvent.Sell)
            AssetAction.Buy -> navigate(QuickActionsNavEvent.Buy)
            AssetAction.Receive -> navigate(QuickActionsNavEvent.Receive)
            AssetAction.ViewActivity,
            AssetAction.ViewStatement,
            AssetAction.InterestDeposit,
            AssetAction.FiatWithdraw,
            AssetAction.InterestWithdraw,
            AssetAction.FiatDeposit,
            AssetAction.StakingDeposit,
            AssetAction.Sign -> throw IllegalStateException("Action $action is not supported as a Quick action")
        }
    }

    private fun totalWalletModeBalance(walletMode: WalletMode) =
        coincore.activeWallets(walletMode).flatMapObservable {
            it.balanceRx
        }.asFlow().catch { emit(AccountBalance.zero(currencyPrefs.selectedFiatCurrency)) }

    private fun actionsForDefi(): Flow<List<QuickAction>> =
        totalWalletModeBalance(WalletMode.NON_CUSTODIAL_ONLY).zip(
            userFeaturePermissionService.isEligibleFor(
                Feature.Sell,
                FreshnessStrategy.Cached(false)
            ).filterNotLoading()
        ) { balance, sellEligible ->

            listOf(
                QuickAction(
                    title = R.string.common_swap,
                    icon = R.drawable.ic_swap,
                    action = AssetAction.Swap,
                    enabled = balance.total.isPositive
                ),
                QuickAction(
                    title = R.string.common_receive,
                    enabled = true,
                    icon = R.drawable.ic_receive,
                    action = AssetAction.Receive,
                ),
                QuickAction(
                    title = R.string.common_send,
                    enabled = balance.total.isPositive,
                    icon = R.drawable.ic_send,
                    action = AssetAction.Send,
                ),
                QuickAction(
                    title = R.string.common_sell,
                    enabled = balance.total.isPositive &&
                        (sellEligible as? DataResource.Data)?.data ?: false,
                    icon = R.drawable.ic_send,
                    action = AssetAction.Send,
                )

            )
        }

    private fun actionsForBrokerage(): Flow<List<QuickAction>> {
        val buyEnabledFlow =
            userFeaturePermissionService.isEligibleFor(Feature.Buy).filterNot { it is DataResource.Loading }
                .onErrorReturn { false }
                .map {
                    (it as? DataResource.Data<Boolean>)?.data ?: throw IllegalStateException("Data should be returned")
                }
        val sellEnabledFlow =
            userFeaturePermissionService.isEligibleFor(Feature.Sell).filterNot { it is DataResource.Loading }
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
            balanceFlow, buyEnabledFlow, sellEnabledFlow, swapEnabledFlow, receiveEnabledFlow
        ) { balance, buyEnabled, sellEnabled, swapEnabled, receiveEnabled ->
            listOf(
                QuickAction(
                    title = R.string.common_buy,
                    enabled = buyEnabled,
                    icon = R.drawable.ic_buy,
                    action = AssetAction.Buy,
                ),
                QuickAction(
                    title = R.string.common_sell,
                    enabled = sellEnabled && balance.total.isPositive,
                    icon = R.drawable.ic_sell,
                    action = AssetAction.Sell,
                ),
                QuickAction(
                    title = R.string.common_swap,
                    enabled = swapEnabled && balance.total.isPositive,
                    icon = R.drawable.ic_swap,
                    action = AssetAction.Swap,
                ),
                QuickAction(
                    title = R.string.common_send,
                    icon = R.drawable.ic_send,
                    enabled = balance.total.isPositive,
                    action = AssetAction.Send,
                ),
                QuickAction(
                    title = R.string.common_receive,
                    icon = R.drawable.ic_receive,
                    action = AssetAction.Receive,
                    enabled = receiveEnabled
                )
            )
        }
    }
}

data class QuickActionsModelState(
    val walletMode: WalletMode = WalletMode.UNIVERSAL,
    val actions: List<QuickAction> = emptyList(),
    private val _actionsForMode: MutableMap<WalletMode, List<QuickAction>> = mutableMapOf()
) : ModelState {
    init {
        _actionsForMode[walletMode] = actions
    }

    fun actionsForMode(walletMode: WalletMode): List<QuickAction> =
        _actionsForMode[walletMode] ?: emptyList()
}

data class QuickAction(
    val icon: Int,
    val title: Int,
    val enabled: Boolean,
    val action: AssetAction
)

sealed class QuickActionsIntent : Intent<QuickActionsModelState> {
    class ActionClicked(val action: AssetAction) : QuickActionsIntent()
    object LoadActions : QuickActionsIntent()
}

data class QuickActionsViewState(val actions: List<QuickAction>) : ViewState

enum class QuickActionsNavEvent : NavigationEvent {
    Buy, Sell, Receive, Send, Swap
}
