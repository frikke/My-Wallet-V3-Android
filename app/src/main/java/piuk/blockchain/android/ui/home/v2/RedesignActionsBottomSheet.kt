package piuk.blockchain.android.ui.home.v2

import android.view.LayoutInflater
import android.view.ViewGroup
import com.blockchain.coincore.CryptoAccount
import com.blockchain.componentlib.image.ImageResource
import com.blockchain.koin.scopedInject
import com.blockchain.notifications.analytics.LaunchOrigin
import info.blockchain.balance.AssetInfo
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.BottomSheetRedesignActionsBinding
import piuk.blockchain.android.ui.base.SlidingModalBottomDialog
import piuk.blockchain.android.ui.base.mvi.MviBottomSheet
import piuk.blockchain.android.ui.sell.BuySellFragment

class RedesignActionsBottomSheet :
    MviBottomSheet<ActionsSheetModel, ActionsSheetIntent, ActionsSheetState, BottomSheetRedesignActionsBinding>() {

    override fun initBinding(inflater: LayoutInflater, container: ViewGroup?): BottomSheetRedesignActionsBinding =
        BottomSheetRedesignActionsBinding.inflate(inflater, container, false)

    override val model: ActionsSheetModel by scopedInject()

    interface Host : SlidingModalBottomDialog.Host {
        fun launchSwap(sourceAccount: CryptoAccount?, targetAccount: CryptoAccount?)
        fun launchBuySell(
            viewType: BuySellFragment.BuySellViewType,
            asset: AssetInfo?
        )

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
            splitButtons.apply {
                startButtonText = getString(R.string.common_buy)
                onStartButtonClick = {
                    model.process(ActionsSheetIntent.CheckForPendingBuys)
                }
                endButtonText = getString(R.string.common_sell)
                onEndButtonClick = {
                    dismiss()
                    host.launchBuySell(BuySellFragment.BuySellViewType.TYPE_SELL, null)
                }
            }
            swapBtn.apply {
                primaryText = getString(R.string.common_swap)
                secondaryText = context.getString(R.string.action_sheet_swap_description)
                onClick = {
                    dismiss()
                    host.launchSwap(null, null)
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
                host.launchBuySell(BuySellFragment.BuySellViewType.TYPE_BUY, null)
                dismiss()
            }
            FlowToLaunch.None -> {
                // do nothing
            }
        }
    }

    companion object {
        fun newInstance() = RedesignActionsBottomSheet()
    }
}
