package piuk.blockchain.android.ui.swap

import android.view.LayoutInflater
import android.view.ViewGroup
import com.blockchain.componentlib.image.ImageResource
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.DialogSheetSwapEmptyStateBinding
import piuk.blockchain.android.ui.base.SlidingModalBottomDialog

class NoAccountsToSwapFromBottomSheet : SlidingModalBottomDialog<DialogSheetSwapEmptyStateBinding> () {
    interface Host : SlidingModalBottomDialog.Host {
        fun receiveClicked()
        fun buyClicked()
    }

    override val host: Host
        get() = super.host as? Host ?: throw IllegalStateException(
            "Host fragment is not a NoAccountsToSwapFromBottomSheet.Host"
        )

    override fun initBinding(inflater: LayoutInflater, container: ViewGroup?): DialogSheetSwapEmptyStateBinding =
        DialogSheetSwapEmptyStateBinding.inflate(inflater, container, false)

    override fun initControls(binding: DialogSheetSwapEmptyStateBinding) {
        initDoubleButtons(binding)
    }

    private fun initDoubleButtons(binding: DialogSheetSwapEmptyStateBinding) {
        binding.receiveBuyButton.apply {
            onPrimaryButtonClick = {
                host.receiveClicked()
                dismiss()
            }
            primaryButtonText = getString(R.string.common_receive)
            startButtonIcon = ImageResource.Local(
                id = R.drawable.ic_qr_code,
                contentDescription = null
            )
            onSecondaryButtonClick = {
                host.buyClicked()
                dismiss()
            }
            secondaryButtonText = getString(R.string.buy_now)
            endButtonIcon = ImageResource.Local(
                id = R.drawable.ic_bank_details,
                contentDescription = null
            )
        }
    }

    companion object {
        fun newInstance(): NoAccountsToSwapFromBottomSheet = NoAccountsToSwapFromBottomSheet()
    }
}
