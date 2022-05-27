package piuk.blockchain.android.ui.dashboard.coinview.interstitials

import android.app.Dialog
import android.os.Bundle
import android.widget.FrameLayout
import androidx.annotation.DrawableRes
import androidx.compose.ui.platform.ComposeView
import com.blockchain.analytics.Analytics
import com.blockchain.analytics.events.LaunchOrigin
import com.blockchain.coincore.BlockchainAccount
import com.blockchain.coincore.CryptoAccount
import com.blockchain.coincore.InterestAccount
import com.blockchain.coincore.NonCustodialAccount
import com.blockchain.coincore.StateAwareAction
import com.blockchain.coincore.TradingAccount
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.sheets.BottomSheetButton
import com.blockchain.componentlib.sheets.BottomSheetOneButton
import com.blockchain.componentlib.sheets.ButtonType
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.simplebuy.SimpleBuyAnalytics
import piuk.blockchain.android.ui.dashboard.coinview.CoinViewAnalytics
import piuk.blockchain.android.util.getAccount
import piuk.blockchain.android.util.putAccount

class AccountExplainerBottomSheet : BottomSheetDialogFragment() {

    interface Host {
        fun navigateToActionSheet(actions: Array<StateAwareAction>)
    }

    val host: Host by lazy {
        activity as? Host
            ?: throw IllegalStateException("Host activity is not a AccountExplainerBottomSheet.Host")
    }

    val analytics: Analytics by inject()

    private val accountActions by lazy {
        arguments?.getSerializable(STATE_AWARE_ACTIONS) as Array<StateAwareAction>
    }

    private val selectedAccount by lazy { arguments?.getAccount(SELECTED_ACCOUNT) as CryptoAccount }
    private val networkTicker by lazy { arguments?.getString(NETWORK_TICKER).orEmpty() }

    // TODO get the highest interest rate from all available before opening this sheet
    private val interestRate by lazy { arguments?.getDouble(INTEREST_RATE) as Double }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = BottomSheetDialog(requireActivity())

        explainerViewedToAnalytics(selectedAccount, networkTicker)
        val account = getAccountExplainerDetails(selectedAccount, interestRate)

        dialog.setContentView(
            ComposeView(requireContext()).apply {
                setContent {
                    BottomSheetOneButton(
                        title = account.title,
                        subtitle = account.description,
                        headerImageResource = ImageResource.Local(account.icon),
                        onCloseClick = {
                            dismiss()
                        },
                        button = BottomSheetButton(
                            type = ButtonType.PRIMARY,
                            text = account.buttonText,
                            onClick = {
                                host.navigateToActionSheet(accountActions)
                                explainerAcceptedToAnalytics(selectedAccount, networkTicker)
                                super.dismiss()
                            }
                        ),
                        shouldShowHeaderDivider = false
                    )
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

    private fun getAccountExplainerDetails(
        selectedAccount: CryptoAccount,
        interestRate: Double
    ): AccountExplainerDetails =
        when (selectedAccount) {
            is TradingAccount -> {
                AccountExplainerDetails(
                    title = getString(R.string.explainer_custodial_title),
                    description = getString(R.string.explainer_custodial_description),
                    icon = R.drawable.ic_custodial_explainer,
                    buttonText = getString(R.string.explainer_button)
                )
            }
            is NonCustodialAccount -> {
                AccountExplainerDetails(
                    title = getString(R.string.explainer_non_custodial_title),
                    description = getString(R.string.explainer_non_custodial_description),
                    icon = R.drawable.ic_non_custodial_explainer,
                    buttonText = getString(R.string.explainer_button)
                )
            }
            is InterestAccount -> {
                AccountExplainerDetails(
                    title = getString(R.string.explainer_rewards_title),
                    description = getString(R.string.explainer_rewards_description, interestRate.toString()),
                    icon = R.drawable.ic_rewards_explainer,
                    buttonText = getString(R.string.explainer_button)
                )
            }
            else -> AccountExplainerDetails()
            // TODO uncomment when exchange account is supported
            // AccountExplainerDetails(
            //     title = getString(R.string.explainer_exchange_title),
            //     description = getString(R.string.explainer_exchange_description),
            //     icon = R.drawable.ic_exchange_logo,
            //     buttonText = getString(R.string.explainer_exchange_button)
            // )
        }

    private fun explainerViewedToAnalytics(selectedAccount: CryptoAccount, networkTicker: String) {
        analytics.logEvent(
            CoinViewAnalytics.ExplainerViewed(
                origin = LaunchOrigin.COIN_VIEW,
                currency = networkTicker,
                accountType = getAccountTypeForAnalytics(selectedAccount)
            )
        )
        // We were sending this in the old CustodyWalletIntroSheet
        if (selectedAccount is TradingAccount) {
            analytics.logEvent(SimpleBuyAnalytics.CUSTODY_WALLET_CARD_SHOWN)
        }
    }

    private fun explainerAcceptedToAnalytics(selectedAccount: CryptoAccount, networkTicker: String) {
        analytics.logEvent(
            CoinViewAnalytics.ExplainerAccepted(
                origin = LaunchOrigin.COIN_VIEW,
                currency = networkTicker,
                accountType = getAccountTypeForAnalytics(selectedAccount)
            )
        )
        // We were sending this in the old CustodyWalletIntroSheet
        if (selectedAccount is TradingAccount) {
            analytics.logEvent(SimpleBuyAnalytics.CUSTODY_WALLET_CARD_CLICKED)
        }
    }

    private fun getAccountTypeForAnalytics(selectedAccount: CryptoAccount): CoinViewAnalytics.Companion.AccountType =
        when (selectedAccount) {
            is NonCustodialAccount -> CoinViewAnalytics.Companion.AccountType.USERKEY
            is TradingAccount -> CoinViewAnalytics.Companion.AccountType.CUSTODIAL
            is InterestAccount -> CoinViewAnalytics.Companion.AccountType.REWARDS_ACCOUNT
            else -> CoinViewAnalytics.Companion.AccountType.EXCHANGE_ACCOUNT
        }

    data class AccountExplainerDetails(
        val title: String = "",
        val description: String = "",
        @DrawableRes val icon: Int = 0,
        val buttonText: String = ""
    )

    companion object {
        private const val SELECTED_ACCOUNT = "selected_account"
        private const val NETWORK_TICKER = "network_ticker"
        private const val INTEREST_RATE = "interest_rate"
        private const val STATE_AWARE_ACTIONS = "state_aware_actions"

        fun newInstance(
            selectedAccount: BlockchainAccount,
            networkTicker: String,
            interestRate: Double,
            stateAwareActions: Array<StateAwareAction>,
        ): AccountExplainerBottomSheet {
            return AccountExplainerBottomSheet().apply {
                arguments = Bundle().apply {
                    putAccount(SELECTED_ACCOUNT, selectedAccount)
                    putString(NETWORK_TICKER, networkTicker)
                    putDouble(INTEREST_RATE, interestRate)
                    putSerializable(STATE_AWARE_ACTIONS, stateAwareActions)
                }
            }
        }
    }
}
