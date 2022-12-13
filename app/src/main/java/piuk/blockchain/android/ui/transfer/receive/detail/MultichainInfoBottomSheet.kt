package piuk.blockchain.android.ui.transfer.receive.detail

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.style.TextAlign
import com.blockchain.coincore.CryptoAccount
import com.blockchain.commonarch.presentation.base.ComposeModalBottomDialog
import com.blockchain.componentlib.sheets.BottomSheetButton
import com.blockchain.componentlib.sheets.BottomSheetTwoButtons
import com.blockchain.componentlib.sheets.ButtonType
import com.blockchain.presentation.extensions.getAccount
import com.blockchain.presentation.extensions.putAccount
import piuk.blockchain.android.R
import piuk.blockchain.android.urllinks.MULTICHAIN_LEARN_MORE

class MultichainInfoBottomSheet : ComposeModalBottomDialog() {

    val account: CryptoAccount?
        get() = arguments?.getAccount(PARAM_ACCOUNT) as? CryptoAccount

    val networkName: String
        get() = arguments?.getString(NETWORK_NAME).orEmpty()

    override val makeSheetNonCollapsible: Boolean
        get() = false

    @Composable
    override fun Sheet() {
        BottomSheetTwoButtons(
            onCloseClick = { dismiss() },
            title = getString(
                R.string.receive_network_alert_title,
                account?.currency?.displayTicker,
                networkName
            ),
            headerImageResource = null,
            shouldShowHeaderDivider = false,
            subtitleAlign = TextAlign.Left,
            showTitleInHeader = true,
            subtitle = getString(
                R.string.receive_network_alert_subtitle,
                account?.currency?.displayTicker,
                networkName
            ),
            button1 = BottomSheetButton(
                type = ButtonType.MINIMAL,
                onClick = {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(MULTICHAIN_LEARN_MORE)))
                },
                text = getString(R.string.common_learn_more)
            ),
            button2 = BottomSheetButton(
                type = ButtonType.PRIMARY,
                onClick = {
                    dismiss()
                }, text = getString(R.string.common_ok)
            )
        )
    }

    companion object {
        private const val PARAM_ACCOUNT = "PARAM_ACCOUNT"
        private const val NETWORK_NAME = "NETWORK_NAME"

        fun newInstance(
            account: CryptoAccount,
            networkName: String
        ): MultichainInfoBottomSheet =
            MultichainInfoBottomSheet().apply {
                arguments = Bundle().apply {
                    putAccount(PARAM_ACCOUNT, account)
                    putString(NETWORK_NAME, networkName)
                }
            }
    }
}
