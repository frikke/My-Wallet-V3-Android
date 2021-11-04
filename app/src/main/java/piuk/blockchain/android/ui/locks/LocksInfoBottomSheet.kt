package piuk.blockchain.android.ui.locks

import android.net.Uri
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.ViewGroup
import com.blockchain.core.payments.model.FundsLocks
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

    private val fundsLocks: FundsLocks by lazy {
        arguments?.getSerializable(KEY_LOCKS) as FundsLocks
    }

    override fun initBinding(inflater: LayoutInflater, container: ViewGroup?): DialogLocksInfoBinding =
        DialogLocksInfoBinding.inflate(inflater, container, false)

    override fun initControls(binding: DialogLocksInfoBinding) {
        displayAvailable(origin)

        with(binding) {
            text.apply {
                movementMethod = LinkMovementMethod.getInstance()
                text = setLearnMoreLink(R.string.funds_locked_summary_text)
            }
            availableAmount.text = available
            title.text = getString(
                R.string.funds_locked_summary_on_hold,
                fundsLocks.onHoldTotalAmount.toStringWithSymbol()
            )
            seeDetails.setOnClickListener {
                LocksDetailsActivity.start(requireContext(), fundsLocks)
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

    private fun displayAvailable(origin: OriginScreenLocks) {
        when (origin) {
            OriginScreenLocks.ENTER_AMOUNT_SEND_SCREEN -> {
                binding.availableTitle.text = getString(R.string.funds_locked_summary_available_send)
            }
            OriginScreenLocks.ENTER_AMOUNT_WITHDRAW_SCREEN -> {
                binding.availableTitle.text = getString(R.string.funds_locked_summary_available_withdraw)
            }
        }
    }

    companion object {
        private const val KEY_AVAILABLE = "KEY_AVAILABLE"
        private const val KEY_LOCKS = "KEY_LOCKS"
        private const val ORIGIN_SCREEN = "ORIGIN_SCREEN"

        fun newInstance(
            originScreen: OriginScreenLocks,
            available: String,
            fundsLocks: FundsLocks
        ) = LocksInfoBottomSheet().apply {
            arguments = Bundle().apply {
                putSerializable(ORIGIN_SCREEN, originScreen)
                putString(KEY_AVAILABLE, available)
                putSerializable(KEY_LOCKS, fundsLocks)
            }
        }
    }

    enum class OriginScreenLocks {
        ENTER_AMOUNT_SEND_SCREEN,
        ENTER_AMOUNT_WITHDRAW_SCREEN
    }
}
