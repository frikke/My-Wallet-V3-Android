package piuk.blockchain.android.ui.locks

import android.net.Uri
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.ViewGroup
import com.blockchain.core.payments.model.Withdrawals
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.DialogLocksInfoBinding
import piuk.blockchain.android.ui.base.SlidingModalBottomDialog
import piuk.blockchain.android.urllinks.TRADING_ACCOUNT_LOCKS
import piuk.blockchain.android.util.StringUtils

class LocksInfoBottomSheet : SlidingModalBottomDialog<DialogLocksInfoBinding>() {

    private val available: String by lazy {
        arguments?.getString(KEY_AVAILABLE, "").orEmpty()
    }

    private val withdrawals: Withdrawals by lazy {
        arguments?.getSerializable(KEY_LOCKS) as Withdrawals
    }

    override fun initBinding(inflater: LayoutInflater, container: ViewGroup?): DialogLocksInfoBinding =
        DialogLocksInfoBinding.inflate(inflater, container, false)

    override fun initControls(binding: DialogLocksInfoBinding) {
        with(binding) {
            text.apply {
                movementMethod = LinkMovementMethod.getInstance()
                text = setLearnMoreLink(R.string.withdrawal_details_text)
            }
            availableAmount.text = available
            onHoldAmount.text = withdrawals.onHoldTotalAmount.toStringWithSymbol()
            openDetails.setOnClickListener {
                startActivity(LocksDetailsActivity.newInstance(requireContext(), withdrawals))
            }
            close.setOnClickListener { dismiss() }
            okButton.setOnClickListener { dismiss() }
        }
    }

    private fun setLearnMoreLink(stringId: Int): CharSequence {
        val linksMap = mapOf<String, Uri>(
            "learn_more" to Uri.parse(TRADING_ACCOUNT_LOCKS)
        )
        return StringUtils.getStringWithMappedAnnotations(
            requireContext(),
            stringId,
            linksMap
        )
    }

    companion object {
        private const val KEY_AVAILABLE = "KEY_AVAILABLE"
        private const val KEY_LOCKS = "KEY_LOCKS"

        fun newInstance(
            available: String,
            withdrawals: Withdrawals
        ) = LocksInfoBottomSheet().apply {
            arguments = Bundle().apply {
                putString(KEY_AVAILABLE, available)
                putSerializable(KEY_LOCKS, withdrawals)
            }
        }
    }
}