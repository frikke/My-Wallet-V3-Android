package com.blockchain.walletconnect.ui.sessionapproval

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import com.blockchain.commonarch.presentation.base.SlidingModalBottomDialog
import com.blockchain.componentlib.viewextensions.gone
import com.blockchain.componentlib.viewextensions.goneIf
import com.blockchain.walletconnect.R
import com.blockchain.walletconnect.databinding.SessionApprovalBottomSheetBinding
import com.blockchain.walletconnect.domain.WalletConnectSession
import com.bumptech.glide.Glide

class WCSessionUpdatedBottomSheet : SlidingModalBottomDialog<SessionApprovalBottomSheetBinding>() {

    private val session: WalletConnectSession by lazy {
        arguments?.getSerializable(SESSION_KEY) as WalletConnectSession
    }

    private val approved: Boolean by lazy {
        arguments?.getBoolean(APPROVED_KEY) ?: throw IllegalStateException("Undefined approval state")
    }

    override fun initBinding(inflater: LayoutInflater, container: ViewGroup?): SessionApprovalBottomSheetBinding =
        SessionApprovalBottomSheetBinding.inflate(inflater, container, false)

    @SuppressLint("StringFormatInvalid")
    override fun initControls(binding: SessionApprovalBottomSheetBinding) {
        Glide.with(this).load(session.dAppInfo.peerMeta.uiIcon()).into(binding.icon)
        with(binding) {
            title.text = getString(
                if (approved)
                    R.string.dapp_is_now_approved else R.string.dapp_is_now_rejected,
                session.dAppInfo.peerMeta.name
            )
            description.goneIf { approved }
            description.text = if (!approved) getString(R.string.go_back_to_your_browser) else ""

            cancelButton.gone()

            stateIndicator.setImageResource(
                if (approved) R.drawable.ic_wc_success_icon else R.drawable.ic_wc_rejected_icon
            )

            approveButton.apply {
                text = getString(R.string.common_ok)
                onClick = {
                    dismiss()
                }
            }
        }
    }

    companion object {
        private const val SESSION_KEY = "SESSION_KEY"
        private const val APPROVED_KEY = "APPROVED_KEY"
        fun newInstance(session: WalletConnectSession, approved: Boolean) =
            WCSessionUpdatedBottomSheet().apply {
                arguments = Bundle().also {
                    it.putSerializable(SESSION_KEY, session)
                    it.putSerializable(APPROVED_KEY, approved)
                }
            }
    }
}
