package com.blockchain.walletconnect.ui.sessionapproval

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import com.blockchain.commonarch.presentation.base.SlidingModalBottomDialog
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.viewextensions.gone
import com.blockchain.walletconnect.R
import com.blockchain.walletconnect.databinding.SessionApprovalBottomSheetBinding
import com.blockchain.walletconnect.domain.WalletConnectSession
import com.blockchain.walletconnect.domain.WalletConnectV2SessionProposal
import com.blockchain.walletconnect.ui.networks.NetworkInfo
import com.bumptech.glide.Glide

class WCApproveSessionBottomSheet : SlidingModalBottomDialog<SessionApprovalBottomSheetBinding>() {

    private val session: WalletConnectSession? by lazy {
        arguments?.getSerializable(SESSION_KEY)?.let { it as WalletConnectSession }
    }

    private val sessionV2: WalletConnectV2SessionProposal? by lazy {
        arguments?.getSerializable(SESSION_V2_KEY)?.let { it as WalletConnectV2SessionProposal }
    }

    private val selectedNetwork: NetworkInfo by lazy {
        arguments?.getSerializable(NETWORK_KEY)?.let {
            it as? NetworkInfo
        } ?: NetworkInfo.defaultEvmNetworkInfo
    }

    interface Host : SlidingModalBottomDialog.Host {
        fun onSelectNetworkClicked(session: WalletConnectSession)
        fun onSessionApproved(session: WalletConnectSession)
        fun onSessionRejected(session: WalletConnectSession)

        fun onApproveV2Session()
        fun onRejectV2Session()
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
        with(binding) {
            sessionV2?.let { sessionV2 ->
                Glide.with(requireActivity()).load(sessionV2.dappLogoUrl).into(binding.icon)
                title.text = getString(R.string.dapp_wants_to_connect, sessionV2.dappName)
                description.text = sessionV2.dappDescription
                walletAndNetwork.gone()
                cancelButton.apply {
                    text = getString(R.string.common_cancel)
                    onClick = {
                        host.onRejectV2Session()
                        dismiss()
                    }
                }

                stateIndicator.gone()

                approveButton.apply {
                    text = getString(R.string.common_confirm)
                    onClick = {
                        host.onApproveV2Session()
                        dismiss()
                    }
                }
            } ?: session?.let { session ->
                Glide.with(requireActivity()).load(session.dAppInfo.peerMeta.uiIcon()).into(binding.icon)
                title.text = getString(R.string.dapp_wants_to_connect, session.dAppInfo.peerMeta.name)
                description.text = session.dAppInfo.peerMeta.url
                walletAndNetwork.apply {
                    primaryText = getString(R.string.common_network)
                    secondaryText = selectedNetwork.name
                    startImageResource = selectedNetwork.logo?.let { ImageResource.Remote(it) }
                        ?: ImageResource.Local(R.drawable.ic_default_asset_logo)
                    onClick = {
                        host.onSelectNetworkClicked(session)
                        dismiss()
                    }
                }

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
    }

    companion object {
        private const val SESSION_KEY = "SESSION_KEY"
        private const val SESSION_V2_KEY = "SESSION_V2_KEY"
        private const val NETWORK_KEY = "NETWORK_KEY"
        fun newInstance(session: WalletConnectSession) =
            WCApproveSessionBottomSheet().apply {
                arguments = Bundle().also {
                    it.putSerializable(SESSION_KEY, session)
                }
            }

        fun newInstance(session: WalletConnectSession, selectedNetwork: NetworkInfo) =
            WCApproveSessionBottomSheet().apply {
                arguments = Bundle().also {
                    it.putSerializable(SESSION_KEY, session)
                    it.putSerializable(NETWORK_KEY, selectedNetwork)
                }
            }

        fun newInstanceWalletConnectV2(walletConnectV2SessionProposal: WalletConnectV2SessionProposal) =
            WCApproveSessionBottomSheet().apply {
                arguments = Bundle().also {
                    it.putSerializable(SESSION_V2_KEY, walletConnectV2SessionProposal)
                }
            }
    }
}
