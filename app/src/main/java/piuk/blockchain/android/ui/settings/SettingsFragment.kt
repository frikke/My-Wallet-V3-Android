package piuk.blockchain.android.ui.settings

import android.content.pm.ShortcutManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.blockchain.analytics.events.AnalyticsEvents
import com.blockchain.commonarch.presentation.base.updateToolbar
import com.blockchain.commonarch.presentation.mvi.MviFragment
import com.blockchain.componentlib.alert.BlockchainSnackbar
import com.blockchain.componentlib.alert.SnackbarType
import com.blockchain.componentlib.basic.ComposeColors
import com.blockchain.componentlib.basic.ComposeGravities
import com.blockchain.componentlib.basic.ComposeTypographies
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.icons.Chat
import com.blockchain.componentlib.icons.Icons
import com.blockchain.componentlib.navigation.NavigationBarButton
import com.blockchain.componentlib.theme.AppColors
import com.blockchain.componentlib.viewextensions.gone
import com.blockchain.componentlib.viewextensions.goneIf
import com.blockchain.componentlib.viewextensions.visible
import com.blockchain.componentlib.viewextensions.visibleIf
import com.blockchain.core.kyc.domain.model.KycTier
import com.blockchain.domain.paymentmethods.model.BankAuthSource
import com.blockchain.domain.paymentmethods.model.PaymentMethodType
import com.blockchain.domain.referral.model.ReferralInfo
import com.blockchain.enviroment.EnvironmentConfig
import com.blockchain.featureflag.FeatureFlag
import com.blockchain.koin.darkModeFeatureFlag
import com.blockchain.nabu.BasicProfileInfo
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.presentation.koin.scopedInject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx3.await
import org.koin.android.ext.android.inject
import piuk.blockchain.android.BuildConfig
import piuk.blockchain.android.R
import piuk.blockchain.android.cards.CardDetailsActivity
import piuk.blockchain.android.cards.RemoveCardBottomSheet
import piuk.blockchain.android.cards.mapper.icon
import piuk.blockchain.android.databinding.FragmentRedesignSettingsBinding
import piuk.blockchain.android.domain.usecases.LinkAccess
import piuk.blockchain.android.simplebuy.SimpleBuyAnalytics
import piuk.blockchain.android.simplebuy.linkBankEventWithCurrency
import piuk.blockchain.android.simplebuy.sheets.RemoveLinkedBankBottomSheet
import piuk.blockchain.android.ui.base.ErrorButtonCopies
import piuk.blockchain.android.ui.base.ErrorDialogData
import piuk.blockchain.android.ui.base.ErrorSlidingBottomDialog
import piuk.blockchain.android.ui.dashboard.sheets.WireTransferAccountDetailsBottomSheet
import piuk.blockchain.android.ui.linkbank.BankAuthActivity
import piuk.blockchain.android.ui.linkbank.alias.BankAliasLinkContract
import piuk.blockchain.android.ui.referral.presentation.Origin
import piuk.blockchain.android.ui.referral.presentation.ReferralAnalyticsEvents
import piuk.blockchain.android.ui.settings.sheets.AddPaymentMethodsBottomSheet
import piuk.blockchain.android.ui.settings.sheets.ThemeBottomSheet
import piuk.blockchain.android.ui.settings.sheets.textResource
import piuk.blockchain.android.util.AndroidUtils

class SettingsFragment :
    MviFragment<SettingsModel, SettingsIntent, SettingsState, FragmentRedesignSettingsBinding>(),
    AddPaymentMethodsBottomSheet.Host,
    RemoveCardBottomSheet.Host,
    RemoveLinkedBankBottomSheet.Host,
    SettingsScreen,
    ErrorSlidingBottomDialog.Host {

    interface Host {
        fun updateBasicProfile(basicProfileInfo: BasicProfileInfo)
        fun updateTier(tier: KycTier)
    }

    val host: Host by lazy {
        activity as? Host
            ?: throw IllegalStateException("Host activity is not a SettingsFragment.Host")
    }

    private val onCardAddedResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == AppCompatActivity.RESULT_OK) {
            model.process(SettingsIntent.LoadPaymentMethods)
        }
    }

    private val onBankTransferAddedResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == AppCompatActivity.RESULT_OK) {
                model.process(SettingsIntent.LoadPaymentMethods)
            }
        }

    private val environmentConfig: EnvironmentConfig by inject()
    private val currencyPrefs: CurrencyPrefs by inject()

    override val model: SettingsModel by scopedInject()

    private val bankAliasLinkLauncher = registerForActivityResult(BankAliasLinkContract()) {}

    private val darkModeFF: FeatureFlag by inject(darkModeFeatureFlag)

    override fun initBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentRedesignSettingsBinding =
        FragmentRedesignSettingsBinding.inflate(inflater, container, false)

    override fun navigator(): SettingsNavigator =
        (activity as? SettingsNavigator) ?: throw IllegalStateException(
            "Parent must implement SettingsNavigator"
        )

    private val settingsPaymentMethodsAdapter =
        SettingsPaymentMethodsAdapter(onPaymentMethodClicked = { paymentMethod ->
            when (paymentMethod) {
                is CardSettingsPaymentMethod -> showBottomSheet(
                    RemoveCardBottomSheet.newInstance(
                        paymentMethodsCache.linkedCards.first { it.cardId == paymentMethod.id }
                    )
                )

                is BankSettingsPaymentMethod -> showBottomSheet(
                    RemoveLinkedBankBottomSheet.newInstance(
                        paymentMethodsCache.linkedBanks.first { it.bank.id == paymentMethod.id }.bank
                    )
                )
            }
        })

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.appVersion.text = getString(
            com.blockchain.stringResources.R.string.app_version,
            BuildConfig.VERSION_NAME,
            BuildConfig.VERSION_CODE
        )
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        binding.legalEntity.text =
            getString(com.blockchain.stringResources.R.string.legal_entity_copyright, currentYear)
        binding.paymentMethodsList.apply {
            adapter = settingsPaymentMethodsAdapter
            layoutManager = LinearLayoutManager(context)
        }
        renderSettingsItems()
    }

    private fun renderSettingsItems() {
        with(binding) {
            seeProfile.apply {
                text = context.getString(com.blockchain.stringResources.R.string.settings_see_profile)
            }

            headerPayments.title = getString(com.blockchain.stringResources.R.string.settings_label_payments)
            headerSettings.title = getString(com.blockchain.stringResources.R.string.settings_label_settings)

            accountGroup.apply {
                primaryText = getString(com.blockchain.stringResources.R.string.settings_title_account)
                secondaryText = getString(com.blockchain.stringResources.R.string.settings_subtitle_account)
                onClick = {
                    navigator().goToAccount()
                }
            }

            notificationsGroup.apply {
                primaryText = getString(com.blockchain.stringResources.R.string.settings_notifications_title)
                secondaryText = getString(com.blockchain.stringResources.R.string.settings_notifications_subtitle)
                onClick = {
                    navigator().goToNotifications()
                }
            }

            securityGroup.apply {
                primaryText = getString(com.blockchain.stringResources.R.string.settings_title_security)
                secondaryText = getString(com.blockchain.stringResources.R.string.settings_subtitle_security)
                onClick = {
                    navigator().goToSecurity()
                }
            }

            aboutAppGroup.apply {
                primaryText = getString(com.blockchain.stringResources.R.string.settings_title_about_app)
                secondaryText = getString(com.blockchain.stringResources.R.string.settings_subtitle_about_app)
                onClick = {
                    navigator().goToAboutApp()
                }
            }

            signOutBtn.apply {
                text = getString(com.blockchain.stringResources.R.string.settings_sign_out)
                onClick = { showLogoutDialog() }
            }

            settingsDebug.apply {
                visibleIf { environmentConfig.isRunningInDebugMode() }
                primaryText = getString(com.blockchain.stringResources.R.string.item_debug_menu)
                onClick = {
                    navigator().goToFeatureFlags()
                }
                startImageResource = ImageResource.Local(R.drawable.ic_nav_debug_swap, null)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        model.process(SettingsIntent.LoadTheme)
        model.process(SettingsIntent.LoadHeaderInformation)
        model.process(SettingsIntent.LoadPaymentMethods)

        lifecycleScope.launch {
            darkModeFF.enabled
                .onErrorReturn { false }
                .await()
                .let { enabled ->
                    with(binding) {
                        if (enabled) {
                            themeGroup.visible()
                            dividerTheme.visible()
                        } else {
                            themeGroup.gone()
                            dividerTheme.gone()
                        }
                    }
                }
        }

        updateToolbar(
            toolbarTitle = getString(com.blockchain.stringResources.R.string.toolbar_settings),
            menuItems = listOf(
                NavigationBarButton.IconResource(
                    image = Icons.Filled.Chat,
                    onIconClick = {
                        analytics.logEvent(AnalyticsEvents.Support)
                        navigator().goToSupportCentre()
                    }
                )
            )
        )
    }

    override fun render(newState: SettingsState) {
        configProfileOnClick(newState.basicProfileInfo)
        host.updateTier(newState.tier)

        newState.basicProfileInfo?.let { userInfo ->
            setInfoHeader(userInfo, newState.tier)
            host.updateBasicProfile(userInfo)
        } ?: setupEmptyHeader()

        showUserTierIcon(newState.tier)

        if (newState.viewToLaunch != ViewToLaunch.None) {
            renderView(newState)
        }

        if (newState.hasWalletUnpaired) {
            analytics.logEvent(AnalyticsEvents.Logout)
            if (AndroidUtils.is25orHigher()) {
                requireActivity().getSystemService(ShortcutManager::class.java).removeAllDynamicShortcuts()
            }
        }

        binding.payments.visibleIf { newState.paymentMethodInfo != null }
        newState.paymentMethodInfo?.let {
            handlePaymentMethods(
                isUserGold = newState.tier == KycTier.GOLD,
                canPayWithBind = newState.canPayWithBind,
                paymentMethods = it
            )
        }

        with(binding.referralBtn) {
            if (newState.referralInfo is ReferralInfo.Data) {
                visible()
                onClick = {
                    analytics.logEvent(ReferralAnalyticsEvents.ReferralProgramClicked(Origin.Profile))
                    navigator().goToReferralCode()
                }

                textColor = { AppColors.titleSecondary }

                newState.referralInfo.announcementInfo?.let { announcementInfo ->
                    isCloseable = false
                    title = announcementInfo.title
                    subtitle = announcementInfo.message
                    if (announcementInfo.backgroundUrl.isNotEmpty()) {
                        backgroundResource = ImageResource.Remote(announcementInfo.backgroundUrl)
                    }

                    if (announcementInfo.iconUrl.isNotEmpty()) {
                        iconResource = ImageResource.Remote(announcementInfo.iconUrl)
                    }
                } ?: run {
                    // keep old functionality here if no data returned
                    title = getString(com.blockchain.stringResources.R.string.referral_program)
                    subtitle = newState.referralInfo.rewardTitle
                    backgroundResource = ImageResource.Local(R.drawable.rounded_blue_button_bkg)
                }
            }
        }

        with(binding) {
            with(generalGroup) {
                primaryText = getString(com.blockchain.stringResources.R.string.common_general)
                secondaryText = getString(com.blockchain.stringResources.R.string.settings_general_description)
                onClick = {
                    navigator().goToGeneralSettings()
                }
                visible()
            }
            dividerGeneral.visible()

            themeGroup.apply {
                primaryText = getString(com.blockchain.stringResources.R.string.settings_theme_title)
                secondaryText = getString(
                    com.blockchain.stringResources.R.string.settings_theme_subtitle,
                    newState.theme?.let { theme ->
                        getString(theme.textResource())
                    } ?: "--"
                )
                onClick = {
                    showThemeBottomSheet()
                }
            }
        }

        if (newState.error != SettingsError.None) {
            renderError(newState.error)
        }
    }

    private fun handlePaymentMethods(
        isUserGold: Boolean,
        canPayWithBind: Boolean,
        paymentMethods: PaymentMethods
    ) {
        val totalLinkedPaymentMethods = paymentMethods.linkedBanks.count() +
            paymentMethods.linkedCards.count()

        val availablePaymentMethodTypes = paymentMethods.availablePaymentMethodTypes
        val linkAccessMap = availablePaymentMethodTypes.associate { it.type to it.linkAccess }

        val hidePaymentsSection = totalLinkedPaymentMethods == 0 &&
            availablePaymentMethodTypes.none { it.linkAccess == LinkAccess.GRANTED }

        binding.payments.goneIf(hidePaymentsSection)
        if (hidePaymentsSection) {
            return
        }
        if (availablePaymentMethodTypes.isNotEmpty()) {
            renderStateWithAvailableToPaymentTypes(
                paymentMethods = paymentMethods,
                canPayWithBind = canPayWithBind,
                linkAccessMap = linkAccessMap
            )
        } else if (totalLinkedPaymentMethods > 0) {
            renderPaymentMethodsList(paymentMethods)
            // if user is Gold and has no payment methods or can not add a payment methods then remove the ui
        } else if (isUserGold) {
            binding.paymentMethodsList.gone()
            binding.headerPayments.gone()
        }
    }

    private fun renderStateWithAvailableToPaymentTypes(
        paymentMethods: PaymentMethods,
        canPayWithBind: Boolean,
        linkAccessMap: Map<PaymentMethodType, LinkAccess>
    ) {
        val totalLinkedPaymentMethods = paymentMethods.linkedBanks.count() +
            paymentMethods.linkedCards.count()
        if (totalLinkedPaymentMethods > 0) {
            renderPaymentMethodsList(paymentMethods = paymentMethods)
            val canLinkNewMethods =
                paymentMethods.availablePaymentMethodTypes.any { it.linkAccess == LinkAccess.GRANTED }
            binding.addPaymentMethod.apply {
                text = getString(com.blockchain.stringResources.R.string.add_payment_method)
                onClick = {
                    if (canPayWithBind) {
                        bankAliasLinkLauncher.launch(
                            currencyPrefs.selectedFiatCurrency.networkTicker
                        )
                    } else {
                        showPaymentMethodsBottomSheet(
                            canAddCard =
                            linkAccessMap[PaymentMethodType.PAYMENT_CARD] == LinkAccess.GRANTED,
                            canLinkBank =
                            linkAccessMap[PaymentMethodType.BANK_TRANSFER] == LinkAccess.GRANTED,
                            canWireTransfer =
                            linkAccessMap[PaymentMethodType.BANK_ACCOUNT] == LinkAccess.GRANTED
                        )
                    }
                }
                visibleIf { canLinkNewMethods }
            }
        } else {
            binding.addPaymentMethod.gone()
            binding.addPaymentMethodRow.apply {
                visible()
                primaryText = getString(com.blockchain.stringResources.R.string.settings_title_no_payments)
                secondaryText = if (canPayWithBind) {
                    getString(com.blockchain.stringResources.R.string.add_a_bank_account)
                } else {
                    getString(com.blockchain.stringResources.R.string.settings_subtitle_no_payments)
                }
                startImageResource = ImageResource.Local(R.drawable.ic_payment_card, null)
                onClick = {
                    if (canPayWithBind) {
                        bankAliasLinkLauncher.launch(currencyPrefs.selectedFiatCurrency.networkTicker)
                    } else {
                        showPaymentMethodsBottomSheet(
                            canAddCard =
                            linkAccessMap[PaymentMethodType.PAYMENT_CARD] == LinkAccess.GRANTED,
                            canLinkBank =
                            linkAccessMap[PaymentMethodType.BANK_TRANSFER] == LinkAccess.GRANTED,
                            canWireTransfer =
                            linkAccessMap[PaymentMethodType.BANK_ACCOUNT] == LinkAccess.GRANTED
                        )
                    }
                }
            }
        }
    }

    private fun renderPaymentMethodsList(paymentMethods: PaymentMethods) {
        val settingsPaymentMethods = paymentMethods.linkedCards.map { card ->
            CardSettingsPaymentMethod(
                id = card.cardId,
                title = card.uiLabel(),
                iconRes = card.cardType.icon(),
                subtitle = getString(
                    com.blockchain.stringResources.R.string.common_spaced_strings,
                    card.limits.max.toStringWithSymbol(),
                    getString(com.blockchain.stringResources.R.string.deposit_enter_amount_limit_title)
                ),
                titleEnd = card.dottedEndDigits(),
                bodyEnd = getString(
                    com.blockchain.stringResources.R.string.card_expiry_date,
                    card.expireDate.formatted()
                ),
                cardRejectionState = card.cardRejectionState
            )
        } + paymentMethods.linkedBanks.map {
            BankSettingsPaymentMethod(
                id = it.bank.id,
                title = it.bank.name,
                iconUrl = it.bank.iconUrl,
                subtitle = getString(
                    com.blockchain.stringResources.R.string.common_spaced_strings,
                    it.limits.max.toStringWithSymbol(),
                    getString(com.blockchain.stringResources.R.string.deposit_enter_amount_limit_title)
                ),
                titleEnd = getString(
                    com.blockchain.stringResources.R.string.dotted_suffixed_string,
                    it.bank.accountEnding
                ),
                bodyEnd = it.bank.accountType,
                canBeUsedToTransact = it.canBeUsedToTransact
            )
        }
        settingsPaymentMethodsAdapter.items = settingsPaymentMethods
        this.paymentMethodsCache = paymentMethods
    }

    private var paymentMethodsCache = PaymentMethods(emptyList(), emptyList(), emptyList())

    private fun renderView(newState: SettingsState) {
        when (newState.viewToLaunch) {
            ViewToLaunch.Profile ->
                newState.basicProfileInfo?.let {
                    navigator().goToProfile()
                }

            is ViewToLaunch.BankTransfer -> {
                onBankTransferAddedResult.launch(
                    BankAuthActivity.newInstance(
                        newState.viewToLaunch.linkBankTransfer,
                        BankAuthSource.SETTINGS,
                        requireContext()
                    )
                )
            }

            is ViewToLaunch.WireTransfer -> {
                val fiatCurrency = newState.viewToLaunch.currency
                WireTransferAccountDetailsBottomSheet.newInstance(fiatCurrency)
                    .show(childFragmentManager, BOTTOM_SHEET)
                analytics.logEvent(
                    linkBankEventWithCurrency(SimpleBuyAnalytics.WIRE_TRANSFER_CLICKED, fiatCurrency.networkTicker)
                )
            }

            ViewToLaunch.None -> {
                // do nothing
            }
        }
        model.process(SettingsIntent.ResetViewState)
    }

    private fun renderError(errorState: SettingsError) {
        when (errorState) {
            SettingsError.PaymentMethodsLoadFail -> {
                // TODO error state here? maybe show retry - check with design
            }

            SettingsError.BankLinkStartFail -> {
                BlockchainSnackbar.make(
                    binding.root,
                    getString(com.blockchain.stringResources.R.string.failed_to_link_bank),
                    type = SnackbarType.Error
                ).show()
            }

            is SettingsError.BankLinkMaxAccountsReached -> {
                showBottomSheet(
                    ErrorSlidingBottomDialog.newInstance(
                        ErrorDialogData(
                            title = getString(com.blockchain.stringResources.R.string.bank_linking_max_accounts_title),
                            description = getString(
                                com.blockchain.stringResources.R.string.bank_linking_max_accounts_subtitle
                            ),
                            error = errorState.toString(),
                            nabuApiException = errorState.error,
                            errorButtonCopies = ErrorButtonCopies(
                                primaryButtonText = getString(com.blockchain.stringResources.R.string.common_ok)
                            ),
                            analyticsCategories = emptyList()
                        )
                    )
                )
            }

            is SettingsError.BankLinkMaxAttemptsReached -> {
                showBottomSheet(
                    ErrorSlidingBottomDialog.newInstance(
                        ErrorDialogData(
                            title = getString(com.blockchain.stringResources.R.string.bank_linking_max_attempts_title),
                            description = getString(
                                com.blockchain.stringResources.R.string.bank_linking_max_attempts_subtitle
                            ),
                            error = errorState.toString(),
                            nabuApiException = errorState.error,
                            errorButtonCopies = ErrorButtonCopies(
                                primaryButtonText = getString(com.blockchain.stringResources.R.string.common_ok)
                            ),
                            analyticsCategories = emptyList()
                        )
                    )
                )
            }

            SettingsError.UnpairFailed -> {
                BlockchainSnackbar.make(
                    binding.root,
                    getString(com.blockchain.stringResources.R.string.settings_logout_error),
                    type = SnackbarType.Error
                ).show()
            }

            SettingsError.None -> {
                // do nothing
            }
        }
        model.process(SettingsIntent.ResetErrorState)
    }

    private fun showUserTierIcon(tier: KycTier) {
        binding.iconUser.setImageResource(
            when (tier) {
                KycTier.GOLD -> R.drawable.bkgd_profile_icon_gold
                KycTier.SILVER -> R.drawable.bkgd_profile_icon_silver
                else -> 0
            }
        )
    }

    private fun configProfileOnClick(basicProfileInfo: BasicProfileInfo?) {
        with(binding) {
            seeProfile.apply {
                onClick = {
                    basicProfileInfo?.let {
                        navigator().goToProfile()
                    }
                }
            }
        }
    }

    private fun showLogoutDialog() {
        AlertDialog.Builder(requireContext(), com.blockchain.componentlib.R.style.AlertDialogStyle)
            .setTitle(com.blockchain.stringResources.R.string.settings_signout_wallet)
            .setMessage(com.blockchain.stringResources.R.string.settings_ask_you_sure_signout)
            .setPositiveButton(com.blockchain.stringResources.R.string.settings_btn_signout) { _, _ ->
                model.process(
                    SettingsIntent.Logout
                )
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun showThemeBottomSheet() {
        showBottomSheet(
            ThemeBottomSheet.newInstance()
        )
    }

    private fun showPaymentMethodsBottomSheet(
        canAddCard: Boolean,
        canLinkBank: Boolean,
        canWireTransfer: Boolean
    ) {
        showBottomSheet(
            AddPaymentMethodsBottomSheet.newInstance(
                canAddCard = canAddCard,
                canLinkBank = canLinkBank,
                canWireTransfer = canWireTransfer
            )
        )
    }

    override fun onAddCardSelected() {
        analytics.logEvent(SimpleBuyAnalytics.SETTINGS_ADD_CARD)
        onCardAddedResult.launch(CardDetailsActivity.newIntent(requireContext()))
    }

    override fun onLinkBankSelected() {
        model.process(SettingsIntent.AddLinkBankSelected)
    }

    override fun onWireTransferSelected() {
        model.process(SettingsIntent.WireTransferSelected)
    }

    override fun onCardRemoved(cardId: String) {
        model.process(SettingsIntent.OnCardRemoved(cardId))
    }

    override fun onLinkedBankRemoved(bankId: String) {
        model.process(SettingsIntent.OnBankRemoved(bankId))
    }

    override fun onErrorPrimaryCta() {
        // do nothing
    }

    override fun onErrorSecondaryCta() {
        // do nothing
    }

    override fun onErrorTertiaryCta() {
        // do nothing
    }

    override fun onSheetClosed() {
        // do nothing
    }

    private fun setInfoHeader(userInformation: BasicProfileInfo, tier: KycTier) {
        if (tier == KycTier.BRONZE) {
            setUserTier0Info(userInformation.email)
        } else {
            setUserInfo(userInformation)
        }
    }

    private fun setUserInfo(userInformation: BasicProfileInfo) {
        with(binding) {
            name.text = getString(
                com.blockchain.stringResources.R.string.common_spaced_strings,
                userInformation.firstName,
                userInformation.lastName
            )
            name.animate().alpha(1f)
            email.apply {
                text = userInformation.email
                style = ComposeTypographies.Body1
                textColor = ComposeColors.Body
                this.animate().alpha(1f)
                gravity = ComposeGravities.Centre
            }
            userInitials.background = ContextCompat.getDrawable(
                requireContext(),
                R.drawable.bkgd_profile_circle
            )
            userInitials.text = getString(
                com.blockchain.stringResources.R.string.settings_initials,
                userInformation.firstName.first().uppercase(),
                userInformation.lastName.first().uppercase()
            )
            seeProfile.visible()
            iconUser.animate().alpha(1f)
        }
    }

    private fun setUserTier0Info(emailAddress: String) {
        with(binding) {
            name.text = emailAddress
            userInitials.background = ContextCompat.getDrawable(
                requireContext(),
                R.drawable.bkgd_profile_circle_empty
            )
            name.animate().alpha(1f)
            email.gone()
            seeProfile.visible()
        }
    }

    // Network calls fail or lack of connectivity UI
    private fun setupEmptyHeader() {
        with(binding) {
            userInitials.background = ContextCompat.getDrawable(
                requireContext(),
                R.drawable.bkgd_profile_circle_empty
            )
            name.alpha = 0f
            email.alpha = 0f
            seeProfile.gone()
        }
    }

    private fun Date.formatted() =
        SimpleDateFormat("MM/yyyy", Locale.getDefault()).format(this)

    companion object {
        private const val LOTTIE_LOADER_PATH = "lottie/loader.json"

        fun newInstance(): SettingsFragment = SettingsFragment()
    }
}
