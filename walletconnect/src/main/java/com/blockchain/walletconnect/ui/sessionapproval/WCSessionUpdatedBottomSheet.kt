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

    private val session: WalletConnectSession? by lazy {
        arguments?.getSerializable(SESSION_KEY)?.let { it as WalletConnectSession }
    }

    private val sessionV2DappName: String? by lazy {
        arguments?.getString(SESSION_V2_DAPP_NAME_KEY)
    }

    private val sessionV2DappLogoURL: String? by lazy {
        arguments?.getString(SESSION_V2_DAPP_LOGO_URL_KEY)
    }

    private val approved: Boolean by lazy {
        arguments?.getBoolean(APPROVED_KEY) ?: throw IllegalStateException("Undefined approval state")
    }

    private val networkSupported: Boolean by lazy {
        arguments?.getBoolean(SESSION_SUPPORTED_KEY) ?: true
    }

    override fun initBinding(inflater: LayoutInflater, container: ViewGroup?): SessionApprovalBottomSheetBinding =
        SessionApprovalBottomSheetBinding.inflate(inflater, container, false)

    @SuppressLint("StringFormatInvalid")
    override fun initControls(binding: SessionApprovalBottomSheetBinding) {
        sessionV2DappLogoURL?.let {
            Glide.with(this).load(it).into(binding.icon)
        } ?: Glide.with(this).load(session?.dAppInfo?.peerMeta?.uiIcon()).into(binding.icon)
        with(binding) {
            walletAndNetwork.gone()

            title.text = getString(
                if (approved)
                    com.blockchain.stringResources.R.string.dapp_is_now_approved else
                    com.blockchain.stringResources.R.string.dapp_is_now_rejected,
                sessionV2DappName ?: session?.dAppInfo?.peerMeta?.name
            )
            description.goneIf { approved }
            description.text =
                if (!networkSupported)
                    getString(com.blockchain.stringResources.R.string.dapp_network_not_supported)
                else if (!approved)
                    getString(com.blockchain.stringResources.R.string.go_back_to_your_browser)
                else ""

            cancelButton.gone()

            stateIndicator.setImageResource(
                if (approved) R.drawable.ic_wc_success_icon else R.drawable.ic_wc_rejected_icon
            )

            approveButton.apply {
                text = getString(com.blockchain.stringResources.R.string.common_ok)
                onClick = {
                    dismiss()
                }
            }
        }
    }

    companion object {
        private const val SESSION_KEY = "SESSION_KEY"
        private const val APPROVED_KEY = "APPROVED_KEY"
        private const val SESSION_SUPPORTED_KEY = "SESSION_SUPPORTED_KEY"
        private const val SESSION_V2_DAPP_NAME_KEY = "SESSION_V2_DAPP_NAME_KEY"
        private const val SESSION_V2_DAPP_LOGO_URL_KEY = "SESSION_V2_DAPP_LOGO_URL_KEY"
        fun newInstance(session: WalletConnectSession, approved: Boolean) =
            WCSessionUpdatedBottomSheet().apply {
                arguments = Bundle().also {
                    it.putSerializable(SESSION_KEY, session)
                    it.putSerializable(APPROVED_KEY, approved)
                }
            }

        fun newInstanceV2SessionApproved(dappName: String, dappLogoUrl: String) =
            WCSessionUpdatedBottomSheet().apply {
                arguments = Bundle().also {
                    it.putSerializable(APPROVED_KEY, true)
                    it.putSerializable(SESSION_V2_DAPP_NAME_KEY, dappName)
                    it.putSerializable(SESSION_V2_DAPP_LOGO_URL_KEY, dappLogoUrl)
                }
            }

        fun newInstanceV2SessionRejected(dappName: String, dappLogoUrl: String) =
            WCSessionUpdatedBottomSheet().apply {
                arguments = Bundle().also {
                    it.putSerializable(APPROVED_KEY, false)
                    it.putSerializable(SESSION_V2_DAPP_NAME_KEY, dappName)
                    it.putSerializable(SESSION_V2_DAPP_LOGO_URL_KEY, dappLogoUrl)
                }
            }

        fun newInstanceV2SessionNotSupported(dappName: String, dappLogoUrl: String) =
            WCSessionUpdatedBottomSheet().apply {
                arguments = Bundle().also {
                    it.putSerializable(APPROVED_KEY, false)
                    it.putSerializable(SESSION_SUPPORTED_KEY, false)
                    it.putSerializable(SESSION_V2_DAPP_NAME_KEY, dappName)
                    it.putSerializable(SESSION_V2_DAPP_LOGO_URL_KEY, dappLogoUrl)
                }
            }
    }
}
