package piuk.blockchain.android.ui.dashboard.coinview.interstitials

import android.app.Dialog
import android.os.Bundle
import android.widget.FrameLayout
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import com.blockchain.analytics.Analytics
import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.BlockchainAccount
import com.blockchain.coincore.CryptoAccount
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.sheets.BottomSheetButton
import com.blockchain.componentlib.sheets.BottomSheetOneButton
import com.blockchain.componentlib.sheets.BottomSheetTwoButtons
import com.blockchain.componentlib.sheets.ButtonType
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import info.blockchain.balance.AssetInfo
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.util.getAccount
import piuk.blockchain.android.util.putAccount

class NoBalanceActionBottomSheet : BottomSheetDialogFragment() {

    interface Host {
        fun navigateToAction(
            action: AssetAction,
            selectedAccount: BlockchainAccount,
            assetInfo: AssetInfo
        )
    }

    val host: Host by lazy {
        activity as? Host
            ?: throw IllegalStateException("Host activity is not a NoBalanceActionBottomSheet.Host")
    }

    val analytics: Analytics by inject()

    private val assetAction by lazy {
        arguments?.getSerializable(ASSET_ACTION) as AssetAction
    }

    private val selectedAccount by lazy { arguments?.getAccount(SELECTED_ACCOUNT) as CryptoAccount }

    private val canBuy by lazy { arguments?.getBoolean(CAN_BUY) ?: false }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = BottomSheetDialog(requireActivity())

        val details = getNoBalanceExplainerDetails(selectedAccount, assetAction, canBuy)

        dialog.setContentView(
            ComposeView(requireContext()).apply {
                setContent {
                    ScreenContent(details)
                }
            }
        )

        dialog.setOnShowListener {
            val d = it as BottomSheetDialog
            val layout =
                d.findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet) as FrameLayout
            BottomSheetBehavior.from(layout).state = BottomSheetBehavior.STATE_EXPANDED
        }
        return dialog
    }

    @Composable
    private fun ScreenContent(details: NoBalanceExplainerDetails) {
        if (details.hasOnlyOneCta()) {
            BottomSheetOneButton(
                title = details.title,
                subtitle = details.description,
                headerImageResource = details.icon,
                onCloseClick = {
                    dismiss()
                },
                button = BottomSheetButton(
                    type = ButtonType.PRIMARY,
                    text = details.primaryButton.text,
                    onClick = {
                        details.primaryButton.onClick()
                        super.dismiss()
                    }
                ),
                shouldShowHeaderDivider = false
            )
        } else {
            require(details.secondaryButton != null)

            BottomSheetTwoButtons(
                title = details.title,
                subtitle = details.description,
                headerImageResource = details.icon,
                onCloseClick = {
                    dismiss()
                },
                button1 = BottomSheetButton(
                    type = ButtonType.PRIMARY,
                    text = details.primaryButton.text,
                    onClick = {
                        details.primaryButton.onClick()
                        super.dismiss()
                    }
                ),
                button2 = BottomSheetButton(
                    type = ButtonType.MINIMAL,
                    text = details.secondaryButton.text,
                    onClick = {
                        details.secondaryButton.onClick()
                        super.dismiss()
                    }
                ),
                shouldShowHeaderDivider = false
            )
        }
    }

    private fun getNoBalanceExplainerDetails(
        selectedAccount: CryptoAccount,
        assetAction: AssetAction,
        canBuy: Boolean
    ): NoBalanceExplainerDetails {
        val assetTicker = selectedAccount.currency.displayTicker
        val accountLabel = selectedAccount.label
        var icon = R.drawable.ic_tx_sent
        var actionName = ""
        when (assetAction) {
            AssetAction.Send -> {
                actionName = getString(R.string.common_send)
                icon = R.drawable.ic_tx_sent
            }
            AssetAction.Swap -> {
                actionName = getString(R.string.common_swap)
                icon = R.drawable.ic_tx_swap
            }
            AssetAction.Sell -> {
                actionName = getString(R.string.common_sell)
                icon = R.drawable.ic_tx_sell
            }
            AssetAction.InterestWithdraw,
            AssetAction.FiatWithdraw -> {
                actionName = getString(R.string.common_withdraw)
                icon = R.drawable.ic_tx_withdraw
            }
            AssetAction.InterestDeposit -> {
                actionName = getString(R.string.common_transfer)
                icon = R.drawable.ic_tx_deposit_arrow
            }
            AssetAction.ViewActivity,
            AssetAction.ViewStatement,
            AssetAction.Receive,
            AssetAction.Buy,
            AssetAction.FiatDeposit,
            AssetAction.Sign -> throw IllegalStateException("$assetAction cannot have a 0 balance error")
        }

        val sheetTitle = getString(R.string.coinview_no_balance_sheet_title, assetTicker, actionName)
        val sheetSubtitle =
            getString(R.string.coinview_no_balance_sheet_subtitle, assetTicker, accountLabel, actionName)
        val sheetIcon = ImageResource.LocalWithBackgroundAndExternalResources(
            icon, selectedAccount.currency.colour, selectedAccount.currency.colour
        )

        val buyButton = NoBalanceExplainerCta(
            text = getString(R.string.tx_title_buy, assetTicker),
            onClick = {
                host.navigateToAction(AssetAction.Buy, selectedAccount, selectedAccount.currency)
            }
        )
        val receiveButton = NoBalanceExplainerCta(
            text = getString(R.string.common_receive_to, assetTicker),
            onClick = {
                host.navigateToAction(AssetAction.Receive, selectedAccount, selectedAccount.currency)
            }
        )

        return NoBalanceExplainerDetails(
            title = sheetTitle,
            description = sheetSubtitle,
            icon = sheetIcon,
            primaryButton = if (canBuy) buyButton else receiveButton,
            secondaryButton = if (canBuy) receiveButton else null,
        )
    }

    private data class NoBalanceExplainerDetails(
        val title: String = "",
        val description: String = "",
        val icon: ImageResource = ImageResource.None,
        val primaryButton: NoBalanceExplainerCta,
        val secondaryButton: NoBalanceExplainerCta?
    )

    private data class NoBalanceExplainerCta(
        val text: String = "",
        val onClick: () -> Unit = {},
    )

    private fun NoBalanceExplainerDetails.hasOnlyOneCta() = secondaryButton == null

    companion object {
        private const val SELECTED_ACCOUNT = "selected_account"
        private const val ASSET_ACTION = "asset_action"
        private const val CAN_BUY = "can_buy"

        fun newInstance(
            selectedAccount: BlockchainAccount,
            action: AssetAction,
            canBuy: Boolean
        ): NoBalanceActionBottomSheet {
            return NoBalanceActionBottomSheet().apply {
                arguments = Bundle().apply {
                    putAccount(SELECTED_ACCOUNT, selectedAccount)
                    putSerializable(ASSET_ACTION, action)
                    putBoolean(CAN_BUY, canBuy)
                }
            }
        }
    }
}
