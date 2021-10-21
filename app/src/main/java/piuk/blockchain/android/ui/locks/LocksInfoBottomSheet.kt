package piuk.blockchain.android.ui.locks

import android.net.Uri
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.ViewGroup
import com.blockchain.core.payments.model.WithdrawalsLocks
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.DialogLocksInfoBinding
import piuk.blockchain.android.ui.base.SlidingModalBottomDialog
import piuk.blockchain.android.urllinks.TRADING_ACCOUNT_LOCKS
import piuk.blockchain.android.util.StringUtils

class LocksInfoBottomSheet : SlidingModalBottomDialog<DialogLocksInfoBinding>() {

    private val origin: OriginScreenLocks by lazy {
        arguments?.getSerializable(ORIGIN_SCREEN) as OriginScreenLocks
    }

    private val available: String by lazy {
        arguments?.getString(KEY_AVAILABLE, "").orEmpty()
    }

    private val withdrawalsLocks: WithdrawalsLocks by lazy {
        arguments?.getSerializable(KEY_LOCKS) as WithdrawalsLocks
    }

    override fun initBinding(inflater: LayoutInflater, container: ViewGroup?): DialogLocksInfoBinding =
        DialogLocksInfoBinding.inflate(inflater, container, false)

    override fun initControls(binding: DialogLocksInfoBinding) {
        setTitle(origin)

        with(binding) {
            text.apply {
                movementMethod = LinkMovementMethod.getInstance()
                text = setLearnMoreLink(R.string.withdrawal_details_text)
            }
            availableAmount.text = available
            onHoldAmount.text = withdrawalsLocks.onHoldTotalAmount.toStringWithSymbol()
            openDetails.setOnClickListener {
                startActivity(LocksDetailsActivity.newInstance(requireContext(), withdrawalsLocks))
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

    private fun setTitle(origin: OriginScreenLocks) {
        binding.title.text = when (origin) {
            OriginScreenLocks.DASHBOARD_SCREEN -> context?.getString(R.string.withdrawal_summary_on_hold)
            OriginScreenLocks.WITHDRAWAL_SCREEN -> context?.getString(R.string.withdrawal_summary_withdraw)
            OriginScreenLocks.SEND_SCREEN -> context?.getString(R.string.withdrawal_summary_send)
            else -> context?.getString(R.string.withdrawal_summary_on_hold)
        }
    }

    companion object {
        private const val KEY_AVAILABLE = "KEY_AVAILABLE"
        private const val KEY_LOCKS = "KEY_LOCKS"
        private const val ORIGIN_SCREEN = "ORIGIN_SCREEN"

        fun newInstance(
            originScreen: OriginScreenLocks,
            available: String,
            withdrawalsLocks: WithdrawalsLocks
        ) = LocksInfoBottomSheet().apply {
            arguments = Bundle().apply {
                putSerializable(ORIGIN_SCREEN, originScreen)
                putString(KEY_AVAILABLE, available)
                putSerializable(KEY_LOCKS, withdrawalsLocks)
            }
        }
    }

    enum class OriginScreenLocks {
        DASHBOARD_SCREEN,
        WITHDRAWAL_SCREEN,
        SEND_SCREEN
    }
}