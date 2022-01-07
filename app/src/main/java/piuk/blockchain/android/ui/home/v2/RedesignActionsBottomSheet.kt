package piuk.blockchain.android.ui.home.v2

import android.view.LayoutInflater
import android.view.ViewGroup
import com.blockchain.componentlib.button.Alignment
import com.blockchain.componentlib.image.ImageResource
import com.blockchain.koin.scopedInject
import com.blockchain.notifications.analytics.LaunchOrigin
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.BottomSheetRedesignActionsBinding
import piuk.blockchain.android.ui.base.SlidingModalBottomDialog
import piuk.blockchain.android.ui.base.mvi.MviBottomSheet

class RedesignActionsBottomSheet :
    MviBottomSheet<ActionsSheetModel, ActionsSheetIntent, ActionsSheetState, BottomSheetRedesignActionsBinding>() {

    override fun initBinding(inflater: LayoutInflater, container: ViewGroup?): BottomSheetRedesignActionsBinding =
        BottomSheetRedesignActionsBinding.inflate(inflater, container, false)

    override val model: ActionsSheetModel by scopedInject()

    interface Host : SlidingModalBottomDialog.Host {
        fun launchSwapScreen()
        fun launchBuy()
        fun launchSell()
        fun launchInterestDashboard(origin: LaunchOrigin)
        fun launchReceive()
        fun launchSend()
        fun launchTooManyPendingBuys(maxTransactions: Int)
    }

    override val host: Host by lazy {
        super.host as? Host ?: throw IllegalStateException(
            "Host fragment is not a RedesignActionsBottomSheet.Host"
        )
    }

    override fun initControls(binding: BottomSheetRedesignActionsBinding) {
        with(binding) {
            splitButtons.alpha = 0f
            model.process(ActionsSheetIntent.CheckCtaOrdering)

            splitButtons.apply {
                primaryButtonText = getString(R.string.common_buy)
                onPrimaryButtonClick = {
                    model.process(ActionsSheetIntent.CheckForPendingBuys)
                }
                secondaryButtonText = getString(R.string.common_sell)
                onSecondaryButtonClick = {
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
                    filterColorId = R.color.blue_600,
                    tintColorId = R.color.blue_400,
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
                    filterColorId = R.color.blue_600,
                    tintColorId = R.color.blue_400,
                    contentDescription = null
                )
            }
            receiveBtn.apply {
                primaryText = getString(R.string.common_receive)
                secondaryText = context.getString(R.string.action_sheet_receive_description)
                onClick = {
                    dismiss()
                    host.launchReceive()
                }
                startImageResource = ImageResource.LocalWithBackground(
                    id = R.drawable.ic_tx_receive,
                    filterColorId = R.color.blue_600,
                    tintColorId = R.color.blue_400,
                    contentDescription = null
                )
            }
            rewardsBtn.apply {
                primaryText = getString(R.string.common_rewards)
                secondaryText = context.getString(R.string.action_sheet_rewards_description)
                onClick = {
                    dismiss()
                    host.launchInterestDashboard(LaunchOrigin.NAVIGATION)
                }
                startImageResource = ImageResource.LocalWithBackground(
                    id = R.drawable.ic_tx_interest,
                    filterColorId = R.color.blue_600,
                    tintColorId = R.color.blue_400,
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
        fun newInstance() = RedesignActionsBottomSheet()
    }
}
