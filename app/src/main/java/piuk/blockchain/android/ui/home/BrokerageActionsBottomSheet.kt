package piuk.blockchain.android.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import com.blockchain.analytics.events.LaunchOrigin
import com.blockchain.commonarch.presentation.base.SlidingModalBottomDialog
import com.blockchain.commonarch.presentation.mvi.MviBottomSheet
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.button.Alignment
import com.blockchain.componentlib.theme.Blue400
import com.blockchain.componentlib.theme.Blue600
import com.blockchain.componentlib.viewextensions.visibleIf
import com.blockchain.domain.common.model.BuySellViewType
import com.blockchain.presentation.koin.scopedInject
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.BottomSheetRedesignActionsBinding
import piuk.blockchain.android.simplebuy.BuySellClicked
import piuk.blockchain.android.ui.home.models.ActionsSheetIntent
import piuk.blockchain.android.ui.home.models.ActionsSheetModel
import piuk.blockchain.android.ui.home.models.ActionsSheetState
import piuk.blockchain.android.ui.home.models.FlowToLaunch
import piuk.blockchain.android.ui.home.models.SplitButtonCtaOrdering

interface ActionBottomSheetHost : SlidingModalBottomDialog.Host {
    fun launchSwapScreen()
    fun launchBuy()
    fun launchBuyForDefi()
    fun launchSell()
    fun launchInterestDashboard(origin: LaunchOrigin)
    fun launchReceive(cryptoTicker: String?)
    fun launchSend()
    fun launchTooManyPendingBuys(maxTransactions: Int)
}

class BrokerageActionsBottomSheet :
    MviBottomSheet<ActionsSheetModel, ActionsSheetIntent, ActionsSheetState, BottomSheetRedesignActionsBinding>() {

    override fun initBinding(inflater: LayoutInflater, container: ViewGroup?): BottomSheetRedesignActionsBinding =
        BottomSheetRedesignActionsBinding.inflate(inflater, container, false)

    override val model: ActionsSheetModel by scopedInject()

    override val host: ActionBottomSheetHost by lazy {
        super.host as? ActionBottomSheetHost ?: throw IllegalStateException(
            "Host fragment is not a ActionBottomSheetHost"
        )
    }

    private val isEarnEnabled by lazy {
        arguments?.getBoolean(EARN_ENABLED_ARG, false) ?: false
    }

    override fun initControls(binding: BottomSheetRedesignActionsBinding) {
        analytics.logEvent(WalletClientAnalytics.WalletFABViewed)
        with(binding) {
            splitButtons.alpha = 0f
            model.process(ActionsSheetIntent.CheckCtaOrdering)

            splitButtons.apply {
                primaryButtonText = getString(R.string.common_buy)
                onPrimaryButtonClick = {
                    analytics.logEvent(
                        BuySellClicked(
                            origin = LaunchOrigin.FAB,
                            type = BuySellViewType.TYPE_BUY
                        )
                    )
                    model.process(ActionsSheetIntent.CheckForPendingBuys)
                }
                secondaryButtonText = getString(R.string.common_sell)
                onSecondaryButtonClick = {
                    analytics.logEvent(
                        BuySellClicked(
                            origin = LaunchOrigin.FAB,
                            type = BuySellViewType.TYPE_SELL
                        )
                    )
                    dismiss()
                    host.launchSell()
                }
            }
            swapBtn.apply {
                primaryText = getString(R.string.common_swap)
                secondaryText = context.getString(R.string.action_sheet_swap_description)
                onClick = {
                    dismiss()
                    host.launchSwapScreen()
                }
                startImageResource = ImageResource.LocalWithBackground(
                    id = R.drawable.ic_tx_swap,
                    iconColor = Blue600,
                    backgroundColor = Blue400,
                    contentDescription = null
                )
            }
            sendBtn.apply {
                primaryText = getString(R.string.common_send)
                secondaryText = context.getString(R.string.action_sheet_send_description)
                onClick = {
                    dismiss()
                    host.launchSend()
                }
                startImageResource = ImageResource.LocalWithBackground(
                    id = R.drawable.ic_tx_sent,
                    iconColor = Blue600,
                    backgroundColor = Blue400,
                    contentDescription = null
                )
            }
            receiveBtn.apply {
                primaryText = getString(R.string.common_receive)
                secondaryText = context.getString(R.string.action_sheet_receive_description)
                onClick = {
                    dismiss()
                    host.launchReceive(cryptoTicker = null)
                }
                startImageResource = ImageResource.LocalWithBackground(
                    id = R.drawable.ic_tx_receive,
                    iconColor = Blue600,
                    backgroundColor = Blue400,
                    contentDescription = null
                )
            }

            rewardsBtn.apply {
                visibleIf { !isEarnEnabled }
                primaryText = getString(R.string.common_rewards)
                secondaryText = context.getString(R.string.action_sheet_rewards_description)
                onClick = {
                    dismiss()
                    host.launchInterestDashboard(LaunchOrigin.NAVIGATION)
                }
                startImageResource = ImageResource.LocalWithBackground(
                    id = R.drawable.ic_tx_interest,
                    iconColor = Blue600,
                    backgroundColor = Blue400,
                    contentDescription = null
                )
            }
        }
    }

    override fun render(newState: ActionsSheetState) {
        when (val flow = newState.flowToLaunch) {
            is FlowToLaunch.TooManyPendingBuys -> {
                host.launchTooManyPendingBuys(flow.maxTransactions)
                dismiss()
            }
            FlowToLaunch.BuyFlow -> {
                host.launchBuy()
                dismiss()
            }
            FlowToLaunch.None -> {
                // do nothing
            }
        }

        with(binding.splitButtons) {
            when (newState.splitButtonCtaOrdering) {
                SplitButtonCtaOrdering.UNINITIALISED -> {
                    // do nothing
                }
                SplitButtonCtaOrdering.BUY_END -> {
                    primaryButtonAlignment = Alignment.END
                    animate().alpha(1f)
                }
                SplitButtonCtaOrdering.BUY_START -> {
                    primaryButtonAlignment = Alignment.START
                    animate().alpha(1f)
                }
            }
        }
    }

    override fun getTheme() = R.style.RedesignBottomSheetDialog

    companion object {
        private const val EARN_ENABLED_ARG = "EARN_ENABLED_ARG"
        fun newInstance(isEarnOnNavBarEnabled: Boolean) = BrokerageActionsBottomSheet().apply {
            arguments = Bundle().apply {
                putBoolean(EARN_ENABLED_ARG, isEarnOnNavBarEnabled)
            }
        }
    }
}
