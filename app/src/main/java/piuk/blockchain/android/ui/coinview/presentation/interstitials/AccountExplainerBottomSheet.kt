package piuk.blockchain.android.ui.coinview.presentation.interstitials

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.compose.ui.platform.ComposeView
import com.blockchain.analytics.Analytics
import com.blockchain.analytics.events.LaunchOrigin
import com.blockchain.coincore.BlockchainAccount
import com.blockchain.coincore.CryptoAccount
import com.blockchain.coincore.EarnRewardsAccount
import com.blockchain.coincore.NonCustodialAccount
import com.blockchain.coincore.StateAwareAction
import com.blockchain.coincore.TradingAccount
import com.blockchain.commonarch.presentation.base.ThemedBottomSheetFragment
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.sheets.BottomSheetButton
import com.blockchain.componentlib.sheets.BottomSheetOneButton
import com.blockchain.componentlib.sheets.ButtonType
import com.blockchain.presentation.extensions.getAccount
import com.blockchain.presentation.extensions.putAccount
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.simplebuy.SimpleBuyAnalytics
import piuk.blockchain.android.ui.coinview.presentation.CoinViewAnalytics

class AccountExplainerBottomSheet : ThemedBottomSheetFragment() {

    interface Host {
        fun navigateToActionSheet(actions: Array<StateAwareAction>, account: BlockchainAccount)
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

    private val interestRate by lazy { arguments?.getDouble(INTEREST_RATE) as Double }
    private val stakingRate by lazy { arguments?.getDouble(STAKING_RATE) as Double }
    private val activeRewardsRate by lazy { arguments?.getDouble(ACTIVE_REWARDS_RATE) as Double }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        explainerViewedToAnalytics(selectedAccount, networkTicker)

        val account = getAccountExplainerDetails(selectedAccount, interestRate, stakingRate)

        return ComposeView(requireContext()).apply {
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
                            host.navigateToActionSheet(accountActions, selectedAccount)
                            explainerAcceptedToAnalytics(selectedAccount, networkTicker)
                            super.dismiss()
                        }
                    )
                )
            }
        }
    }

    private fun getAccountExplainerDetails(
        selectedAccount: CryptoAccount,
        interestRate: Double,
        stakingRate: Double
    ): AccountExplainerDetails =
        when (selectedAccount) {
            is TradingAccount -> {
                AccountExplainerDetails(
                    title = getString(com.blockchain.stringResources.R.string.explainer_custodial_title),
                    description = getString(com.blockchain.stringResources.R.string.explainer_custodial_description),
                    icon = R.drawable.ic_custodial_explainer,
                    buttonText = getString(com.blockchain.stringResources.R.string.common_i_understand)
                )
            }

            is NonCustodialAccount -> {
                AccountExplainerDetails(
                    title = getString(com.blockchain.stringResources.R.string.explainer_non_custodial_title),
                    description = getString(
                        com.blockchain.stringResources.R.string.explainer_non_custodial_description
                    ),
                    icon = R.drawable.ic_non_custodial_explainer,
                    buttonText = getString(com.blockchain.stringResources.R.string.common_i_understand)
                )
            }

            is EarnRewardsAccount.Interest -> {
                AccountExplainerDetails(
                    title = getString(com.blockchain.stringResources.R.string.explainer_rewards_title),
                    description = getString(
                        com.blockchain.stringResources.R.string.explainer_rewards_description,
                        interestRate.toString()
                    ),
                    icon = R.drawable.ic_rewards_explainer,
                    buttonText = getString(com.blockchain.stringResources.R.string.common_i_understand)
                )
            }

            is EarnRewardsAccount.Staking -> {
                AccountExplainerDetails(
                    title = getString(com.blockchain.stringResources.R.string.explainer_staking_title),
                    description = getString(
                        com.blockchain.stringResources.R.string.explainer_staking_description,
                        stakingRate.toString()
                    ),
                    icon = R.drawable.ic_staking_explainer,
                    buttonText = getString(com.blockchain.stringResources.R.string.common_i_understand)
                )
            }

            is EarnRewardsAccount.Active -> {
                AccountExplainerDetails(
                    title = getString(com.blockchain.stringResources.R.string.explainer_active_rewards_title),
                    description = getString(
                        com.blockchain.stringResources.R.string.explainer_active_rewards_description,
                        activeRewardsRate.toString()
                    ),
                    icon = R.drawable.ic_active_rewards_explainer,
                    buttonText = getString(com.blockchain.stringResources.R.string.common_i_understand)
                )
            }

            else -> AccountExplainerDetails()
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
            is EarnRewardsAccount.Interest -> CoinViewAnalytics.Companion.AccountType.REWARDS_ACCOUNT
            is EarnRewardsAccount.Staking -> CoinViewAnalytics.Companion.AccountType.STAKING_ACCOUNT
            // TODO(EARN): analytics active rewards?
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
        private const val STAKING_RATE = "staking_rate"
        private const val ACTIVE_REWARDS_RATE = "active_rewards_rate"
        private const val STATE_AWARE_ACTIONS = "state_aware_actions"

        fun newInstance(
            selectedAccount: BlockchainAccount,
            networkTicker: String,
            interestRate: Double,
            stakingRate: Double,
            activeRewardsRate: Double,
            stateAwareActions: Array<StateAwareAction>
        ): AccountExplainerBottomSheet {
            return AccountExplainerBottomSheet().apply {
                arguments = Bundle().apply {
                    putAccount(SELECTED_ACCOUNT, selectedAccount)
                    putString(NETWORK_TICKER, networkTicker)
                    putDouble(INTEREST_RATE, interestRate)
                    putDouble(STAKING_RATE, stakingRate)
                    putDouble(ACTIVE_REWARDS_RATE, activeRewardsRate)
                    putSerializable(STATE_AWARE_ACTIONS, stateAwareActions)
                }
            }
        }
    }
}
