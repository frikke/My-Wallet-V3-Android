package com.blockchain.walletconnect.ui.sessionapproval

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import com.blockchain.commonarch.presentation.base.SlidingModalBottomDialog
import com.blockchain.componentlib.viewextensions.gone
import com.blockchain.walletconnect.R
import com.blockchain.walletconnect.databinding.SessionApprovalBottomSheetBinding
import com.blockchain.walletconnect.domain.WalletConnectSession
import com.bumptech.glide.Glide

class WCApproveSessionBottomSheet : SlidingModalBottomDialog<SessionApprovalBottomSheetBinding>() {

    private val session: WalletConnectSession by lazy {
        arguments?.getSerializable(SESSION_KEY) as WalletConnectSession
    }

    interface Host : SlidingModalBottomDialog.Host {
        fun onSessionApproved(session: WalletConnectSession)
        fun onSessionRejected(session: WalletConnectSession)
    }

    override val host: Host by lazy {
        super.host as? Host ?: throw IllegalStateException(
            "Host fragment is not a WCApproveSessionBottomSheet.Host"
        )
    }

    override fun initBinding(inflater: LayoutInflater, container: ViewGroup?): SessionApprovalBottomSheetBinding =
        SessionApprovalBottomSheetBinding.inflate(inflater, container, false)

    @SuppressLint("StringFormatInvalid")
    override fun initControls(binding: SessionApprovalBottomSheetBinding) {
        Glide.with(this).load(session.dAppInfo.peerMeta.uiIcon()).into(binding.icon)
        with(binding) {
            title.text = getString(R.string.dapp_wants_to_connect, session.dAppInfo.peerMeta.name)
            description.text = session.dAppInfo.peerMeta.url
            cancelButton.apply {
                text = getString(R.string.common_cancel)
                onClick = {
                    host.onSessionRejected(session)
                    dismiss()
                }
            }

            stateIndicator.gone()

            approveButton.apply {
                text = getString(R.string.common_confirm)
                onClick = {
                    host.onSessionApproved(session)
                    dismiss()
                }
            }
        }
    }

    companion object {
        private const val SESSION_KEY = "SESSION_KEY"
        fun newInstance(session: WalletConnectSession) =
            WCApproveSessionBottomSheet().apply {
                arguments = Bundle().also {
                    it.putSerializable(SESSION_KEY, session)
                }
            }
    }
}
