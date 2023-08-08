package com.blockchain.presentation.sheets

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import com.blockchain.analytics.Analytics
import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.BlockchainAccount
import com.blockchain.coincore.CryptoAccount
import com.blockchain.common.R
import com.blockchain.commonarch.presentation.base.ThemedBottomSheetFragment
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.sheets.BottomSheetButton
import com.blockchain.componentlib.sheets.BottomSheetOneButton
import com.blockchain.componentlib.sheets.BottomSheetTwoButtons
import com.blockchain.componentlib.sheets.ButtonType
import com.blockchain.presentation.extensions.getAccount
import com.blockchain.presentation.extensions.putAccount
import info.blockchain.balance.AssetInfo
import org.koin.android.ext.android.inject

class NoBalanceActionBottomSheet : ThemedBottomSheetFragment() {

    interface Host {
        fun navigateToAction(
            action: AssetAction,
            selectedAccount: BlockchainAccount,
            assetInfo: AssetInfo
        )
    }

    val host: Host by lazy {
        parentFragment as? Host ?: activity as? Host ?: throw IllegalStateException(
            "Host activity is not a NoBalanceActionBottomSheet.Host"
        )
    }

    val analytics: Analytics by inject()

    private val assetAction by lazy {
        arguments?.getSerializable(ASSET_ACTION) as AssetAction
    }

    private val selectedAccount by lazy { arguments?.getAccount(SELECTED_ACCOUNT) as CryptoAccount }

    private val canBuy by lazy { arguments?.getBoolean(CAN_BUY) ?: false }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val details = getNoBalanceExplainerDetails(selectedAccount, assetAction, canBuy)

        return ComposeView(requireContext()).apply {
            setContent {
                ScreenContent(details)
            }
        }
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
                actionName = getString(com.blockchain.stringResources.R.string.common_send)
                icon = R.drawable.ic_tx_sent
            }
            AssetAction.Swap -> {
                actionName = getString(com.blockchain.stringResources.R.string.common_swap)
                icon = R.drawable.ic_tx_swap
            }
            AssetAction.Sell -> {
                actionName = getString(com.blockchain.stringResources.R.string.common_sell)
                icon = R.drawable.ic_tx_sell
            }
            AssetAction.InterestWithdraw,
            AssetAction.ActiveRewardsWithdraw,
            AssetAction.StakingWithdraw,
            AssetAction.FiatWithdraw -> {
                actionName = getString(com.blockchain.stringResources.R.string.common_withdraw)
                icon = R.drawable.ic_tx_withdraw
            }
            AssetAction.InterestDeposit,
            AssetAction.StakingDeposit,
            AssetAction.ActiveRewardsDeposit -> {
                actionName = getString(com.blockchain.stringResources.R.string.common_transfer)
                icon = R.drawable.ic_tx_interest
            }
            AssetAction.ViewActivity,
            AssetAction.ViewStatement,
            AssetAction.Receive,
            AssetAction.Buy,
            AssetAction.FiatDeposit,
            AssetAction.Sign -> throw IllegalStateException("$assetAction cannot have a 0 balance error")
        }

        val sheetTitle = when (assetAction) {
            AssetAction.StakingDeposit,
            AssetAction.InterestDeposit -> getString(
                com.blockchain.stringResources.R.string.no_balance_sheet_earn_title,
                assetTicker
            )
            else -> getString(
                com.blockchain.stringResources.R.string.coinview_no_balance_sheet_title,
                assetTicker,
                actionName
            )
        }
        val sheetSubtitle = when (assetAction) {
            AssetAction.StakingDeposit,
            AssetAction.InterestDeposit,
            AssetAction.ActiveRewardsDeposit -> getString(
                com.blockchain.stringResources.R.string.no_balance_sheet_earn_subtitle,
                assetTicker
            )
            else -> getString(
                com.blockchain.stringResources.R.string.coinview_no_balance_sheet_subtitle,
                assetTicker,
                accountLabel,
                actionName
            )
        }

        val sheetIcon = ImageResource.LocalWithBackgroundAndExternalResources(
            icon,
            selectedAccount.currency.colour,
            selectedAccount.currency.colour
        )

        val buyButton = NoBalanceExplainerCta(
            text = getString(com.blockchain.stringResources.R.string.tx_title_buy, assetTicker),
            onClick = {
                host.navigateToAction(AssetAction.Buy, selectedAccount, selectedAccount.currency)
            }
        )
        val receiveButton = NoBalanceExplainerCta(
            text = getString(com.blockchain.stringResources.R.string.common_receive_to, assetTicker),
            onClick = {
                host.navigateToAction(AssetAction.Receive, selectedAccount, selectedAccount.currency)
            }
        )

        return NoBalanceExplainerDetails(
            title = sheetTitle,
            description = sheetSubtitle,
            icon = sheetIcon,
            primaryButton = if (canBuy) buyButton else receiveButton,
            secondaryButton = if (canBuy) receiveButton else null
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
        val onClick: () -> Unit = {}
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
