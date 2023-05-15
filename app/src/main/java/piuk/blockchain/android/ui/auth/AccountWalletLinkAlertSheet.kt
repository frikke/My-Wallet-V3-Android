package piuk.blockchain.android.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import com.blockchain.commonarch.presentation.base.SlidingModalBottomDialog
import com.blockchain.home.presentation.navigation.AccountWalletLinkAlertSheetHost
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.AccountWalletLinkAlertSheetBinding

class AccountWalletLinkAlertSheet : SlidingModalBottomDialog<AccountWalletLinkAlertSheetBinding>() {

    private val walletId: String by lazy {
        val missingWalletId =
            getString(com.blockchain.stringResources.R.string.account_wallet_mismatch_wallet_id_not_found)
        arguments?.getString(WALLET_ID, missingWalletId) ?: missingWalletId
    }

    override val host: AccountWalletLinkAlertSheetHost by lazy {
        super.host as? AccountWalletLinkAlertSheetHost ?: throw IllegalStateException(
            "Host fragment is not a AccountWalletLinkAlertSheet.Host"
        )
    }

    override fun initBinding(inflater: LayoutInflater, container: ViewGroup?): AccountWalletLinkAlertSheetBinding =
        AccountWalletLinkAlertSheetBinding.inflate(inflater, container, false)

    override fun initControls(binding: AccountWalletLinkAlertSheetBinding) {
        with(binding) {
            walletIdText.text = getString(
                com.blockchain.stringResources.R.string.account_wallet_mismatch_label,
                walletId
            )
            logoutButton.setOnClickListener { host.logout() }
            cancelButton.setOnClickListener { dismiss() }
        }
    }

    companion object {
        private const val WALLET_ID = "WALLET_ID"

        fun newInstance(walletId: String): AccountWalletLinkAlertSheet {
            return AccountWalletLinkAlertSheet().apply {
                arguments = Bundle().apply {
                    putString(WALLET_ID, walletId)
                }
            }
        }
    }
}
