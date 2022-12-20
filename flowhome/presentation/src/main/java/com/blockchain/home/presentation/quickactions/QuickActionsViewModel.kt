package com.blockchain.home.presentation.quickactions

import com.blockchain.coincore.AssetAction
import com.blockchain.commonarch.presentation.mvi_v2.Intent
import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.commonarch.presentation.mvi_v2.ModelState
import com.blockchain.commonarch.presentation.mvi_v2.MviViewModel
import com.blockchain.commonarch.presentation.mvi_v2.NavigationEvent
import com.blockchain.commonarch.presentation.mvi_v2.ViewState
import com.blockchain.data.DataResource
import com.blockchain.data.onErrorReturn
import com.blockchain.home.presentation.R
import com.blockchain.nabu.Feature
import com.blockchain.nabu.api.getuser.domain.UserFeaturePermissionService
import com.blockchain.walletmode.WalletMode
import com.blockchain.walletmode.WalletModeService
import java.lang.IllegalStateException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

class QuickActionsViewModel(
    private val userFeaturePermissionService: UserFeaturePermissionService,
    private val walletModeService: WalletModeService
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
                        walletMode = wMode
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

    private fun actionsForDefi(): Flow<List<QuickAction>> =
        flowOf(
            listOfNotNull(
                QuickAction(
                    title = R.string.common_swap,
                    icon = R.drawable.ic_swap,
                    action = AssetAction.Swap,
                ),
                QuickAction(
                    title = R.string.common_send,
                    icon = R.drawable.ic_send,
                    action = AssetAction.Send,
                ),
                QuickAction(
                    title = R.string.common_receive,
                    icon = R.drawable.ic_receive,
                    action = AssetAction.Receive,
                )
            )
        )

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

        return combine(
            buyEnabledFlow, sellEnabledFlow, swapEnabledFlow, receiveEnabledFlow
        ) { buyEnabled, sellEnabled, swapEnabled, receiveEnabled ->
            listOfNotNull(
                QuickAction(
                    title = R.string.common_buy,
                    icon = R.drawable.ic_buy,
                    action = AssetAction.Buy,
                ).takeIf { buyEnabled },
                QuickAction(
                    title = R.string.common_sell,
                    icon = R.drawable.ic_sell,
                    action = AssetAction.Sell,
                ).takeIf { sellEnabled },
                QuickAction(
                    title = R.string.common_swap,
                    icon = R.drawable.ic_swap,
                    action = AssetAction.Swap,
                ).takeIf { swapEnabled },
                QuickAction(
                    title = R.string.common_send,
                    icon = R.drawable.ic_send,
                    action = AssetAction.Send,
                ),
                QuickAction(
                    title = R.string.common_receive,
                    icon = R.drawable.ic_receive,
                    action = AssetAction.Receive,
                ).takeIf { receiveEnabled }
            )
        }
    }
}

data class QuickActionsModelState(
    val walletMode: WalletMode = WalletMode.UNIVERSAL,
    val actions: List<QuickAction> = emptyList()
) : ModelState

data class QuickAction(
    val icon: Int,
    val title: Int,
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
