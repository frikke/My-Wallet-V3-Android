package piuk.blockchain.android.simplebuy.sheets

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import com.blockchain.commonarch.presentation.base.SlidingModalBottomDialog
import com.blockchain.presentation.koin.scopedInject
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.SimpleBuyCancelOrderBottomSheetBinding
import piuk.blockchain.android.simplebuy.SimpleBuyAnalytics
import piuk.blockchain.android.simplebuy.SimpleBuySyncFactory

class SimpleBuyCancelOrderBottomSheet : SlidingModalBottomDialog<SimpleBuyCancelOrderBottomSheetBinding>() {

    interface Host : SlidingModalBottomDialog.Host {
        fun cancelOrderConfirmAction(cancelOrder: Boolean, orderId: String?)
    }

    override val host: Host by lazy {
        super.host as? Host
            ?: throw IllegalStateException("Host fragment is not a SimpleBuyCancelOrderBottomSheet.Host")
    }

    private val stateFactory: SimpleBuySyncFactory by scopedInject()

    override fun initBinding(inflater: LayoutInflater, container: ViewGroup?): SimpleBuyCancelOrderBottomSheetBinding =
        SimpleBuyCancelOrderBottomSheetBinding.inflate(inflater, container, false)

    override fun initControls(binding: SimpleBuyCancelOrderBottomSheetBinding) {
        val state = stateFactory.currentState()
        val asset = state?.selectedCryptoAsset
        if (asset != null) {
            with(binding) {
                if (arguments.fromDashboard()) {
                    cancelOrder.text = getString(
                        com.blockchain.stringResources.R.string.cancel_order_do_cancel_dashboard
                    )
                    goBack.text = getString(com.blockchain.stringResources.R.string.cancel_order_go_back_dashboard)
                } else {
                    cancelOrder.text = getString(com.blockchain.stringResources.R.string.cancel_order_do_cancel)
                    goBack.text = getString(com.blockchain.stringResources.R.string.cancel_order_go_back)
                }

                cancelOrderToken.text = getString(
                    com.blockchain.stringResources.R.string.cancel_token_instruction,
                    asset.displayTicker
                )
                cancelOrder.onClick = {
                    analytics.logEvent(SimpleBuyAnalytics.BANK_DETAILS_CANCEL_CONFIRMED)
                    dismiss()
                    host.cancelOrderConfirmAction(true, state.id)
                }
                goBack.onClick = {
                    analytics.logEvent(SimpleBuyAnalytics.BANK_DETAILS_CANCEL_GO_BACK)
                    dismiss()
                    host.cancelOrderConfirmAction(false, null)
                }
            }
        } else {
            dismiss()
        }
    }

    companion object {
        private const val FROM_DASHBOARD = "from_dashboard"

        fun newInstance(fromDashboard: Boolean = false): SimpleBuyCancelOrderBottomSheet =
            SimpleBuyCancelOrderBottomSheet().apply {
                arguments = Bundle().also {
                    it.putBoolean(FROM_DASHBOARD, fromDashboard)
                }
            }

        private fun Bundle?.fromDashboard(): Boolean =
            this?.getBoolean(FROM_DASHBOARD, false) ?: false
    }
}
