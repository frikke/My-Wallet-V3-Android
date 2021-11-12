package piuk.blockchain.android.ui.home.v2

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import piuk.blockchain.android.databinding.BottomSheetRedesignActionsBinding
import piuk.blockchain.android.ui.base.SlidingModalBottomDialog
import piuk.blockchain.android.ui.sell.BuySellFragment

class RedesignActionsBottomSheet : SlidingModalBottomDialog<BottomSheetRedesignActionsBinding>() {

    override fun initBinding(inflater: LayoutInflater, container: ViewGroup?): BottomSheetRedesignActionsBinding =
        BottomSheetRedesignActionsBinding.inflate(inflater, container, false)

    override fun initControls(binding: BottomSheetRedesignActionsBinding) {
        with(binding) {
            splitButtons.apply {
                startButtonText = "Buy"
                onStartButtonClick = {
                    dismiss()
                    // TODO move to Host
                    (activity as RedesignMainActivity).launchBuySell(BuySellFragment.BuySellViewType.TYPE_BUY)
                }
                endButtonText = "Sell"
                onEndButtonClick = {
                    dismiss()
                    // TODO move to Host
                    (activity as RedesignMainActivity).launchBuySell(BuySellFragment.BuySellViewType.TYPE_SELL)
                }
            }
            swap.apply {
                // TODO move to strings once we have design
                text = "Swap"
                onClick = {
//                    showFragment(
//                        childFragmentManager,
//                        SwapFragment.newInstance()
//                    )
                }
            }
            send.apply {
                text = "Send"
                onClick = {
//                    showFragment(
//                        childFragmentManager,
//                        TransferSendFragment.newInstance()
//                    )
                }
            }
            receive.apply {
                text = "Receive"
                onClick = {
//                    showFragment(
//                        childFragmentManager,
//                        ReceiveFragment.newInstance()
//                    )
                }
            }
            deposit.apply {
                text = "Deposit"
                onClick = {
                }
            }
            withdraw.apply {
                text = "Withdraw"
                onClick = {
                }
            }
        }
    }

    companion object {
        fun newInstance() = RedesignActionsBottomSheet().apply {
            arguments = Bundle().apply {
            }
        }
    }
}
