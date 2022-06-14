package piuk.blockchain.android.ui.settings.v2.account

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.annotation.StringRes
import com.blockchain.analytics.events.LaunchOrigin
import com.blockchain.commonarch.presentation.base.updateToolbar
import com.blockchain.commonarch.presentation.mvi.MviFragment
import com.blockchain.componentlib.alert.BlockchainSnackbar
import com.blockchain.componentlib.alert.SnackbarType
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.tag.TagType
import com.blockchain.componentlib.tag.TagViewState
import com.blockchain.componentlib.viewextensions.visibleIf
import com.blockchain.domain.referral.model.ReferralInfo
import com.blockchain.featureflag.FeatureFlag
import com.blockchain.koin.blockchainCardFeatureFlag
import com.blockchain.koin.scopedInject
import com.blockchain.walletconnect.domain.WalletConnectAnalytics
import info.blockchain.balance.FiatCurrency
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.plusAssign
import piuk.blockchain.android.BuildConfig
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.FragmentAccountBinding
import piuk.blockchain.android.simplebuy.sheets.CurrencySelectionSheet
import piuk.blockchain.android.ui.customviews.ErrorBottomDialog
import piuk.blockchain.android.ui.settings.SettingsAnalytics
import piuk.blockchain.android.ui.settings.v2.SettingsNavigator
import piuk.blockchain.android.ui.settings.v2.SettingsScreen
import piuk.blockchain.android.util.launchUrlInBrowser

class AccountFragment :
    MviFragment<AccountModel, AccountIntent, AccountState, FragmentAccountBinding>(),
    CurrencySelectionSheet.Host,
    SettingsScreen {

    override fun initBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentAccountBinding =
        FragmentAccountBinding.inflate(inflater, container, false)

    override fun navigator(): SettingsNavigator =
        (activity as? SettingsNavigator) ?: throw IllegalStateException(
            "Parent must implement SettingsNavigator"
        )

    override fun onBackPressed(): Boolean = true

    override val model: AccountModel by scopedInject()
    private val blockchainCardFF: FeatureFlag by scopedInject(blockchainCardFeatureFlag)

    private val compositeDisposable = CompositeDisposable()

    private lateinit var walletId: String

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        updateToolbar(
            toolbarTitle = getString(R.string.account_toolbar),
            menuItems = emptyList()
        )

        with(binding) {
            settingsLimits.apply {
                primaryText = getString(R.string.account_limits_title)
                secondaryText = getString(R.string.account_limits_subtitle)
                onClick = {
                    navigator().goToKycLimits()
                }
            }

            settingsWalletId.apply {
                primaryText = getString(R.string.account_wallet_id_title)
                secondaryText = getString(R.string.account_wallet_id_subtitle)
                endImageResource = ImageResource.Local(R.drawable.ic_copy, null)
                onClick = {
                    analytics.logEvent(SettingsAnalytics.WalletIdCopyClicked)

                    if (::walletId.isInitialized) {
                        val clipboard =
                            requireActivity().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("walletId", walletId)
                        clipboard.setPrimaryClip(clip)
                        BlockchainSnackbar.make(
                            binding.root,
                            getString(R.string.copied_to_clipboard),
                            type = SnackbarType.Success
                        ).show()
                        analytics.logEvent(SettingsAnalytics.WalletIdCopyCopied)
                    } else {
                        BlockchainSnackbar.make(
                            binding.root,
                            getString(R.string.account_wallet_id_copy_error),
                            type = SnackbarType.Error
                        ).show()
                    }
                }
            }

            settingsCurrency.apply {
                primaryText = getString(R.string.account_currency_title)
                onClick = {
                    model.process(AccountIntent.LoadFiatList)
                }
            }

            settingsExchange.apply {
                primaryText = getString(R.string.account_exchange_title)
                secondaryText = getString(R.string.account_exchange_launch)
                tags = listOf(TagViewState(getString(R.string.common_launch), TagType.InfoAlt()))
                onClick = {
                    showBottomSheet(
                        ExchangeConnectionSheet.newInstance(
                            ErrorBottomDialog.Content(
                                title = getString(R.string.account_exchange_connected_title),
                                ctaButtonText = R.string.account_exchange_connected_cta,
                                icon = R.drawable.ic_exchange_logo
                            ),
                            tags = emptyList(),
                            primaryCtaClick = {
                                requireActivity().launchUrlInBrowser(BuildConfig.EXCHANGE_LAUNCH_URL)
                            },
                        )
                    )
                }
            }

            settingsDebitCard.apply {
                primaryText = getString(R.string.blockchain_debit_card)
            }

            settingsWalletConnect.apply {

                primaryText = getString(R.string.account_wallet_connect)
                onClick = {
                    navigator().goToWalletConnect()
                    analytics.logEvent(
                        WalletConnectAnalytics.ConnectedDappsListClicked(
                            origin = LaunchOrigin.SETTINGS
                        )
                    )
                }
            }

            settingsAirdrops.apply {
                primaryText = getString(R.string.account_airdrops_title)
                onClick = {
                    navigator().goToAirdrops()
                }
            }
            settingsAddresses.apply {
                primaryText = getString(R.string.account_addresses_title)
                onClick = {
                    navigator().goToAddresses()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        model.process(AccountIntent.LoadAccountInformation)
        compositeDisposable += blockchainCardFF.enabled.onErrorReturn { false }.subscribe { enabled ->
            if (enabled) model.process(AccountIntent.LoadBCDebitCardInformation)
        }
    }

    override fun render(newState: AccountState) {
        if (newState.accountInformation != null) {
            renderWalletInformation(newState.accountInformation)
        }

        if (newState.viewToLaunch != ViewToLaunch.None) {
            renderViewToLaunch(newState)
        }

        renderDebitCardInformation(newState.blockchainCardOrderState)
        renderErrorState(newState.errorState)
        renderReferral(newState.referralInfo)
    }

    private fun renderDebitCardInformation(blockchainCardOrderState: BlockchainCardOrderState) =
        with(binding.settingsDebitCard) {
            when (blockchainCardOrderState) {
                is BlockchainCardOrderState.NotEligible -> {
                    // Do nothing
                }
                is BlockchainCardOrderState.Eligible -> {
                    visibility = VISIBLE
                    secondaryText = null
                    tags = listOf(TagViewState(getString(R.string.order_card), TagType.InfoAlt()))
                    onClick = {
                        navigator().goToOrderBlockchainCard(blockchainCardOrderState.cardProducts.first())
                    }
                }
                is BlockchainCardOrderState.Ordered -> {
                    visibility = VISIBLE
                    secondaryText = null
                    tags = null
                    onClick = {
                        navigator().goToManageBlockchainCard(blockchainCardOrderState.blockchainCard)
                    }
                }
            }
        }

    private fun renderReferral(referralInfo: ReferralInfo) {
        with(binding) {
            settingsReferAFriend.visibleIf { referralInfo is ReferralInfo.Data }
            settingsReferAFriendDiv.visibleIf { referralInfo is ReferralInfo.Data }
        }

        if (referralInfo is ReferralInfo.Data) {
            with(binding.settingsReferAFriend) {
                primaryText = getString(R.string.account_refer_a_friend)
                onClick = { navigator().goToReferralCode(referralInfo) }
            }
        }
    }

    private fun renderErrorState(error: AccountError) =
        when (error) {
            AccountError.ACCOUNT_INFO_FAIL -> {
                showErrorSnackbar(R.string.account_load_info_error)
            }
            AccountError.FIAT_LIST_FAIL -> {
                showErrorSnackbar(R.string.account_load_fiat_error)
            }
            AccountError.ACCOUNT_FIAT_UPDATE_FAIL -> {
                showErrorSnackbar(R.string.account_fiat_update_error)
            }
            AccountError.BLOCKCHAIN_CARD_LOAD_FAIL -> {
                showErrorSnackbar(R.string.account_load_bc_card_error)
            }
            AccountError.NONE -> {
                // do nothing
            }
        }

    private fun showErrorSnackbar(@StringRes message: Int) {
        BlockchainSnackbar.make(
            binding.root,
            getString(message),
            type = SnackbarType.Error
        ).show()
    }

    private fun renderWalletInformation(accountInformation: AccountInformation) {
        walletId = accountInformation.walletId

        binding.settingsCurrency.apply {
            secondaryText = accountInformation.userCurrency.nameWithSymbol()
        }
    }

    private fun renderViewToLaunch(newState: AccountState) {
        when (val view = newState.viewToLaunch) {
            is ViewToLaunch.CurrencySelection -> {
                showBottomSheet(
                    CurrencySelectionSheet.newInstance(
                        currencies = view.currencyList,
                        selectedCurrency = view.selectedCurrency,
                        currencySelectionType = CurrencySelectionSheet.Companion.CurrencySelectionType.DISPLAY_CURRENCY
                    )
                )
            }
            ViewToLaunch.None -> {
                // do nothing
            }
        }
        model.process(AccountIntent.ResetViewState)
    }

    override fun onCurrencyChanged(currency: FiatCurrency) {
        model.process(AccountIntent.UpdateFiatCurrency(currency))
    }

    override fun onSheetClosed() {
        // do nothing
    }

    companion object {
        fun newInstance() = AccountFragment()
    }
}
