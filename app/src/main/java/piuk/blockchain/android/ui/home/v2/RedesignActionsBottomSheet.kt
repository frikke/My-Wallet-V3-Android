package piuk.blockchain.android.ui.home.v2

import android.view.LayoutInflater
import android.view.ViewGroup
import com.blockchain.coincore.CryptoAccount
import com.blockchain.notifications.analytics.LaunchOrigin
import info.blockchain.balance.AssetInfo
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.BottomSheetRedesignActionsBinding
import piuk.blockchain.android.ui.base.SlidingModalBottomDialog
import piuk.blockchain.android.ui.sell.BuySellFragment

class RedesignActionsBottomSheet : SlidingModalBottomDialog<BottomSheetRedesignActionsBinding>() {

    override fun initBinding(inflater: LayoutInflater, container: ViewGroup?): BottomSheetRedesignActionsBinding =
        BottomSheetRedesignActionsBinding.inflate(inflater, container, false)

    interface Host : SlidingModalBottomDialog.Host {
        fun launchSwap(sourceAccount: CryptoAccount?, targetAccount: CryptoAccount?)
        fun launchBuySell(
            viewType: BuySellFragment.BuySellViewType,
            asset: AssetInfo?
        )
        fun launchInterestDashboard(origin: LaunchOrigin)
        fun launchReceive()
        fun launchSend()
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
                    dismiss()
                    host.launchBuySell(BuySellFragment.BuySellViewType.TYPE_BUY, null)
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
            }
            sendBtn.apply {
                primaryText = getString(R.string.common_send)
                secondaryText = context.getString(R.string.action_sheet_send_description)
                onClick = {
                    dismiss()
                    host.launchSend()
                }
            }
            receiveBtn.apply {
                primaryText = getString(R.string.common_receive)
                context.getString(R.string.action_sheet_rewards_description)
                secondaryText = context.getString(R.string.action_sheet_receive_description)
                onClick = {
                    dismiss()
                    host.launchReceive()
                }
            }
            rewardsBtn.apply {
                primaryText = getString(R.string.common_rewards)
                secondaryText = context.getString(R.string.action_sheet_rewards_description)
                onClick = {
                    dismiss()
                    host.launchInterestDashboard(LaunchOrigin.NAVIGATION)
                }
            }
            addCashBtn.apply {
                primaryText = getString(R.string.common_deposit)
                secondaryText = context.getString(R.string.action_sheet_deposit_description)
                onClick = {
                    dismiss()
                    // TODO
                }
            }
            cashOutBtn.apply {
                primaryText = getString(R.string.common_withdraw)
                secondaryText = context.getString(R.string.action_sheet_deposit_description)
                onClick = {
                    dismiss()
                    // TODO
                }
            }
        }
    }

    companion object {
        fun newInstance() = RedesignActionsBottomSheet()
    }
}
