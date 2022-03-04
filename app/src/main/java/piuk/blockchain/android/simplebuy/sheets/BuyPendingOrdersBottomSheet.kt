package piuk.blockchain.android.simplebuy.sheets

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import com.blockchain.commonarch.presentation.base.SlidingModalBottomDialog
import java.lang.IllegalArgumentException
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.LayoutPendingBuyOrdersBinding

class BuyPendingOrdersBottomSheet : SlidingModalBottomDialog<LayoutPendingBuyOrdersBinding>() {

    interface Host : SlidingModalBottomDialog.Host {
        fun startActivityRequested()
    }

    override val host: Host by lazy {
        super.host as? Host
            ?: throw IllegalStateException("Host fragment is not a BuyPendingOrdersBottomSheet.Host")
    }

    private val pendingBuys: Int by lazy {
        arguments?.getInt(PENDING_BUYS) ?: throw IllegalArgumentException("Pending buys aren't provided")
    }

    override fun initBinding(inflater: LayoutInflater, container: ViewGroup?): LayoutPendingBuyOrdersBinding =
        LayoutPendingBuyOrdersBinding.inflate(inflater, container, false)

    override fun initControls(binding: LayoutPendingBuyOrdersBinding) {
        with(binding) {
            description.text = getString(R.string.pending_buys_description, pendingBuys)
            ok.setOnClickListener { dismiss() }
            viewActivity.setOnClickListener {
                host.startActivityRequested()
                dismiss()
            }
        }
    }

    companion object {
        private const val PENDING_BUYS = "PENDING_BUYS"
        const val TAG = "BuyPendingOrdersBottomSheet"
        fun newInstance(pendingBuys: Int): BuyPendingOrdersBottomSheet =
            BuyPendingOrdersBottomSheet().apply {
                arguments = Bundle().also {
                    it.putInt(PENDING_BUYS, pendingBuys)
                }
            }
    }
}
