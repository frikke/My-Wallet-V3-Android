package piuk.blockchain.android.ui.settings.v2

import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutManager
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.compose.ui.text.buildAnnotatedString
import androidx.core.content.ContextCompat
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieDrawable.RESTART
import com.blockchain.componentlib.button.MinimalButtonView
import com.blockchain.componentlib.image.ImageResource
import com.blockchain.componentlib.navigation.NavigationBarButton
import com.blockchain.componentlib.tablerow.BalanceTableRowView
import com.blockchain.componentlib.tablerow.DefaultTableRowView
import com.blockchain.componentlib.tag.TagType
import com.blockchain.componentlib.tag.TagViewState
import com.blockchain.koin.scopedInject
import com.blockchain.nabu.BasicProfileInfo
import com.blockchain.nabu.Tier
import com.blockchain.nabu.datamanagers.PaymentMethod
import com.blockchain.nabu.datamanagers.custodialwalletimpl.PaymentMethodType
import com.blockchain.notifications.analytics.AnalyticsEvents
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.cards.CardDetailsActivity
import piuk.blockchain.android.cards.RemoveCardBottomSheet
import piuk.blockchain.android.cards.icon
import piuk.blockchain.android.databinding.ActivityRedesignPhase2SettingsBinding
import piuk.blockchain.android.databinding.ToolbarGeneralBinding
import piuk.blockchain.android.simplebuy.RemoveLinkedBankBottomSheet
import piuk.blockchain.android.simplebuy.RemovePaymentMethodBottomSheetHost
import piuk.blockchain.android.simplebuy.SimpleBuyAnalytics
import piuk.blockchain.android.simplebuy.linkBankEventWithCurrency
import piuk.blockchain.android.ui.FeatureFlagsHandlingActivity
import piuk.blockchain.android.ui.base.mvi.MviActivity
import piuk.blockchain.android.ui.base.mvi.MviFragment
import piuk.blockchain.android.ui.customviews.ToastCustom
import piuk.blockchain.android.ui.customviews.toast
import piuk.blockchain.android.ui.dashboard.sheets.WireTransferAccountDetailsBottomSheet
import piuk.blockchain.android.ui.home.ZendeskSubjectActivity
import piuk.blockchain.android.ui.linkbank.BankAuthActivity
import piuk.blockchain.android.ui.linkbank.BankAuthSource
import piuk.blockchain.android.ui.settings.SettingsFragment
import piuk.blockchain.android.ui.settings.v2.account.AccountActivity
import piuk.blockchain.android.ui.settings.v2.notifications.NotificationsActivity
import piuk.blockchain.android.ui.settings.v2.profile.ProfileActivity
import piuk.blockchain.android.urllinks.URL_BLOCKCHAIN_SUPPORT_PORTAL
import piuk.blockchain.android.util.AndroidUtils
import piuk.blockchain.android.util.calloutToExternalSupportLinkDlg
import piuk.blockchain.android.util.gone
import piuk.blockchain.android.util.visible
import piuk.blockchain.android.util.visibleIf
import piuk.blockchain.androidcore.data.api.EnvironmentConfig

class RedesignSettingsPhase2Activity :
    MviActivity<SettingsModel, SettingsIntent, SettingsState, ActivityRedesignPhase2SettingsBinding>(),
    AddPaymentMethodsBottomSheet.Host,
    RemovePaymentMethodBottomSheetHost {

    override val model: SettingsModel by scopedInject()

    override fun initBinding(): ActivityRedesignPhase2SettingsBinding =
        ActivityRedesignPhase2SettingsBinding.inflate(layoutInflater)

    override val alwaysDisableScreenshots: Boolean = true

    override val toolbarBinding: ToolbarGeneralBinding
        get() = binding.toolbar

    private val environmentConfig: EnvironmentConfig by inject()

    private val onCardAddedResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == RESULT_OK) {
            model.process(SettingsIntent.LoadPaymentMethods)
        }
    }

    private val onBankTransferAddedResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == RESULT_OK) {
                model.process(SettingsIntent.LoadPaymentMethods)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        setupToolbar()
    }

    override fun onResume() {
        super.onResume()
        model.process(SettingsIntent.LoadHeaderInformation)
        model.process(SettingsIntent.LoadPaymentMethods)
    }

    override fun render(newState: SettingsState) {
        setupMenuItems(newState.basicProfileInfo)

        newState.basicProfileInfo?.let { userInfo ->
            if (newState.tier.isSupportChatEnabled()) {
                setupActiveSupportButton(userInfo)
            }
            setInfoHeader(userInfo, newState.tier)
        } ?: setupEmptyHeader()

        showUserTierIcon(newState.tier)

        if (newState.viewToLaunch != ViewToLaunch.None) {
            renderView(newState)
        }

        if (newState.hasWalletUnpaired) {
            analytics.logEvent(AnalyticsEvents.Logout)
            if (AndroidUtils.is25orHigher()) {
                getSystemService(ShortcutManager::class.java).removeAllDynamicShortcuts()
            }
        }

        if (newState.paymentMethodInfo != null) {
            binding.paymentsContainer.removeAllViews()

            addPaymentMethods(
                paymentMethodInfo = newState.paymentMethodInfo,
                totalLinkedPaymentMethods = newState.paymentMethodInfo.linkedBanks.count() +
                    newState.paymentMethodInfo.linkedCards.count(),
                isUserGold = newState.tier == Tier.GOLD
            )
        } else {
            with(binding.paymentsContainer) {
                removeAllViews()
                addView(
                    LottieAnimationView(this@RedesignSettingsPhase2Activity).apply {
                        imageAssetsFolder = LOTTIE_LOADER_PATH
                        setAnimation(LOTTIE_LOADER_PATH)
                        repeatMode = RESTART
                        playAnimation()
                    },
                    LinearLayoutCompat.LayoutParams(
                        resources.getDimensionPixelOffset(R.dimen.xlarge_margin),
                        resources.getDimensionPixelOffset(R.dimen.xlarge_margin)
                    ).apply {
                        gravity = Gravity.CENTER
                    }
                )
            }
        }

        if (newState.error != SettingsError.NONE) {
            renderError(newState.error)
        }
    }

    private fun renderView(newState: SettingsState) {
        when (newState.viewToLaunch) {
            ViewToLaunch.Profile -> startActivity(
                newState.basicProfileInfo?.let {
                    ProfileActivity.newIntent(this, it)
                }
            )
            is ViewToLaunch.BankAccount -> {
                val fiatCurrency = newState.viewToLaunch.currency
                WireTransferAccountDetailsBottomSheet.newInstance(fiatCurrency)
                    .show(supportFragmentManager, MviFragment.BOTTOM_SHEET)
                analytics.logEvent(
                    linkBankEventWithCurrency(SimpleBuyAnalytics.WIRE_TRANSFER_CLICKED, fiatCurrency.networkTicker)
                )
            }
            is ViewToLaunch.BankTransfer -> {
                onBankTransferAddedResult.launch(
                    BankAuthActivity.newInstance(
                        newState.viewToLaunch.linkBankTransfer,
                        BankAuthSource.SETTINGS,
                        this
                    )
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
            SettingsError.PAYMENT_METHODS_LOAD_FAIL -> {
                // TODO error state here? maybe show retry - check with design
            }
            SettingsError.BANK_LINK_START_FAIL -> {
                toast(R.string.failed_to_link_bank, ToastCustom.TYPE_ERROR)
            }
            SettingsError.UNPAIR_FAILED -> {
                toast(R.string.settings_logout_error, ToastCustom.TYPE_ERROR)
            }
            SettingsError.NONE -> {
                // do nothing
            }
        }
        model.process(SettingsIntent.ResetErrorState)
    }

    private fun addPaymentMethods(
        paymentMethodInfo: PaymentMethods,
        totalLinkedPaymentMethods: Int,
        isUserGold: Boolean
    ) {
        when {
            paymentMethodInfo.canLinkPaymentMethods() -> {
                with(binding.paymentsContainer) {
                    if (totalLinkedPaymentMethods > 0) {
                        addBanks(paymentMethodInfo)
                        addCards(paymentMethodInfo)
                        addView(
                            MinimalButtonView(this@RedesignSettingsPhase2Activity).apply {
                                text = getString(R.string.add_payment_method)
                                onClick = {
                                    showPaymentMethodsBottomSheet(
                                        canAddCard = paymentMethodInfo.eligiblePaymentMethods
                                            .getPaymentMethodEligibility(PaymentMethodType.PAYMENT_CARD),
                                        canAddBankTransfer = paymentMethodInfo.eligiblePaymentMethods
                                            .getPaymentMethodEligibility(PaymentMethodType.BANK_TRANSFER),
                                        canAddBankAccount = paymentMethodInfo.eligiblePaymentMethods
                                            .getPaymentMethodEligibility(PaymentMethodType.BANK_ACCOUNT)
                                    )
                                }
                            },
                            LinearLayoutCompat.LayoutParams(
                                MATCH_PARENT,
                                WRAP_CONTENT,
                            ).apply {
                                marginStart = resources.getDimensionPixelOffset(R.dimen.standard_margin)
                                marginEnd = resources.getDimensionPixelOffset(R.dimen.standard_margin)
                            }
                        )
                    } else {
                        addView(
                            DefaultTableRowView(this@RedesignSettingsPhase2Activity).apply {
                                primaryText = getString(R.string.settings_title_no_payments)
                                secondaryText = getString(R.string.settings_subtitle_no_payments)
                                onClick = {
                                    showPaymentMethodsBottomSheet(
                                        canAddCard = paymentMethodInfo.eligiblePaymentMethods
                                            .getPaymentMethodEligibility(PaymentMethodType.PAYMENT_CARD),
                                        canAddBankTransfer = paymentMethodInfo.eligiblePaymentMethods
                                            .getPaymentMethodEligibility(PaymentMethodType.BANK_TRANSFER),
                                        canAddBankAccount = paymentMethodInfo.eligiblePaymentMethods
                                            .getPaymentMethodEligibility(PaymentMethodType.BANK_ACCOUNT)
                                    )
                                }
                                startImageResource = ImageResource.Local(R.drawable.ic_payment_card, null)
                            }
                        )
                    }
                }
            }
            else -> {
                if (totalLinkedPaymentMethods > 0) {
                    with(binding.paymentsContainer) {
                        addBanks(paymentMethodInfo)
                        addCards(paymentMethodInfo)
                    }
                } else {
                    with(binding) {
                        if (isUserGold) {
                            paymentsContainer.gone()
                            headerPayments.gone()
                        } else {
                            // TODO show KYC for silver -> gold UI - missing design
                        }
                    }
                }
            }
        }
    }

    private fun Map<PaymentMethodType, Boolean>.getPaymentMethodEligibility(key: PaymentMethodType): Boolean =
        this[key] ?: false

    private fun LinearLayoutCompat.addCards(paymentMethodInfo: PaymentMethods) {
        paymentMethodInfo.linkedCards.forEach { card ->
            addView(
                BalanceTableRowView(this@RedesignSettingsPhase2Activity).apply {
                    alpha = 0f
                    titleStart = buildAnnotatedString { append(card.uiLabel()) }
                    titleEnd = buildAnnotatedString { append(card.dottedEndDigits()) }
                    startImageResource = ImageResource.Local(
                        (card as? PaymentMethod.Card)?.cardType?.icon()
                            ?: R.drawable.ic_payment_card,
                        null
                    )
                    bodyStart = buildAnnotatedString {
                        append(
                            getString(
                                R.string.common_spaced_strings, card.limits.max.toStringWithSymbol(),
                                getString(R.string.deposit_enter_amount_limit_title)
                            )
                        )
                    }
                    bodyEnd = buildAnnotatedString {
                        append(
                            getString(R.string.card_expiry_date, card.expireDate.formatted())
                        )
                    }
                    onClick = {
                        showBottomSheet(RemoveCardBottomSheet.newInstance(card))
                    }
                    animate().alpha(1f)
                }
            )
        }
    }

    private fun LinearLayoutCompat.addBanks(paymentMethodInfo: PaymentMethods) {
        paymentMethodInfo.linkedBanks.forEach { bank ->
            addView(
                DefaultTableRowView(this@RedesignSettingsPhase2Activity).apply {
                    alpha = 0f
                    primaryText = bank.name
                    startImageResource = ImageResource.Remote(url = bank.iconUrl, null)
                    secondaryText = bank.account
                    endTag = if (bank.canBeUsedToTransact) null else
                        TagViewState(
                            getString(R.string.common_unavailable), TagType.Error()
                        )
                    onClick = {
                        showBottomSheet(RemoveLinkedBankBottomSheet.newInstance(bank))
                    }
                    animate().alpha(1f)
                }
            )
        }
    }

    private fun showUserTierIcon(tier: Tier) {
        binding.iconUser.setImageResource(
            when (tier) {
                Tier.GOLD -> R.drawable.ic_icon_gold
                Tier.SILVER -> R.drawable.ic_icon_silver
                else -> 0
            }
        )
    }

    private fun setupMenuItems(basicProfileInfo: BasicProfileInfo?) {
        with(binding) {
            seeProfile.apply {
                text = context.getString(R.string.settings_see_profile)
                onClick = {
                    startActivity(ProfileActivity.newIntent(context, basicProfileInfo))
                }
            }

            headerPayments.title = getString(R.string.settings_label_payments)
            headerSettings.title = getString(R.string.settings_label_settings)

            accountGroup.apply {
                primaryText = getString(R.string.settings_title_account)
                secondaryText = getString(R.string.settings_subtitle_account)
                onClick = {
                    startActivity(AccountActivity.newIntent(this@RedesignSettingsPhase2Activity))
                }
            }

            notificationsGroup.apply {
                primaryText = getString(R.string.settings_notifications_title)
                secondaryText = getString(R.string.settings_notifications_subtitle)
                onClick = {
                    startActivity(NotificationsActivity.newIntent(this@RedesignSettingsPhase2Activity))
                }
            }

            securityGroup.apply {
                primaryText = getString(R.string.settings_title_security)
                secondaryText = getString(R.string.settings_subtitle_security)
                onClick = {
                    // TODO open
                }
            }

            aboutAppGroup.apply {
                primaryText = getString(R.string.settings_title_about_app)
                secondaryText = getString(R.string.settings_subtitle_about_app)
                onClick = {
                    basicProfileInfo?.let {
                        startActivity(AboutAppActivity.newIntent(context, it))
                    }
                }
            }

            signOutBtn.apply {
                text = getString(R.string.settings_sign_out)
                onClick = { showLogoutDialog() }
            }

            settingsDebug.apply {
                visibleIf { environmentConfig.isRunningInDebugMode() }
                primaryText = getString(R.string.item_debug_menu)
                onClick = {
                    startActivity(FeatureFlagsHandlingActivity.newIntent(context))
                }
                startImageResource = ImageResource.Local(R.drawable.ic_nav_debug_swap, null)
            }
        }
    }

    private fun showLogoutDialog() {
        AlertDialog.Builder(this, R.style.AlertDialogStyle)
            .setTitle(R.string.logout_wallet)
            .setMessage(R.string.ask_you_sure_logout)
            .setPositiveButton(R.string.btn_logout) { _, _ -> model.process(SettingsIntent.Logout) }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun showPaymentMethodsBottomSheet(
        canAddCard: Boolean,
        canAddBankTransfer: Boolean,
        canAddBankAccount: Boolean
    ) {
        showBottomSheet(
            AddPaymentMethodsBottomSheet.newInstance(
                canAddCard = canAddCard,
                canAddBankTransfer = canAddBankTransfer,
                canAddBankAccount = canAddBankAccount
            )
        )
    }

    override fun onAddCardSelected() {
        analytics.logEvent(SimpleBuyAnalytics.SETTINGS_ADD_CARD)
        onCardAddedResult.launch(CardDetailsActivity.newIntent(this))
    }

    override fun onAddBankTransferSelected() {
        model.process(SettingsIntent.AddBankTransferSelected)
    }

    override fun onAddBankAccountSelected() {
        model.process(SettingsIntent.AddBankAccountSelected)
    }

    override fun onCardRemoved(cardId: String) {
        model.process(SettingsIntent.OnCardRemoved(cardId))
    }

    override fun onLinkedBankRemoved(bankId: String) {
        model.process(SettingsIntent.OnBankRemoved(bankId))
    }

    override fun onSheetClosed() {
        // do nothing
    }

    private fun setInfoHeader(userInformation: BasicProfileInfo, tier: Tier) {
        if (tier == Tier.BRONZE) {
            setUserTier0Info(userInformation.email)
        } else {
            setUserInfo(userInformation)
        }
    }

    private fun Tier.isSupportChatEnabled(): Boolean = this == Tier.GOLD

    private fun setUserInfo(userInformation: BasicProfileInfo) {
        with(binding) {
            name.text = getString(
                R.string.common_spaced_strings, userInformation.firstName, userInformation.lastName
            )
            name.animate().alpha(1f)
            email.text = userInformation.email
            email.animate().alpha(1f)
            userInitials.background = ContextCompat.getDrawable(
                this@RedesignSettingsPhase2Activity,
                R.drawable.bkgd_profile_circle
            )
            userInitials.text = getString(
                R.string.settings_initials,
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
                this@RedesignSettingsPhase2Activity,
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
                this@RedesignSettingsPhase2Activity,
                R.drawable.bkgd_profile_circle_empty
            )
            name.alpha = 0f
            email.alpha = 0f
            seeProfile.gone()
        }
    }

    private fun setupToolbar() {
        updateToolbar(
            toolbarTitle = getString(R.string.toolbar_settings),
            backAction = { onBackPressed() }
        )
        setupDefaultSupportButton()
    }

    private fun setupDefaultSupportButton() {
        updateToolbarMenuItems(
            listOf(
                NavigationBarButton.Icon(R.drawable.ic_support_chat) {
                    analytics.logEvent(AnalyticsEvents.Support)
                    calloutToExternalSupportLinkDlg(this, URL_BLOCKCHAIN_SUPPORT_PORTAL)
                }
            )
        )
    }

    private fun setupActiveSupportButton(userInformation: BasicProfileInfo) {
        updateToolbarMenuItems(
            listOf(
                NavigationBarButton.Icon(R.drawable.ic_support_chat) {
                    analytics.logEvent(AnalyticsEvents.Support)
                    startActivity(ZendeskSubjectActivity.newInstance(this, userInformation))
                }
            )
        )
    }

    companion object {
        private const val LOTTIE_LOADER_PATH = "lottie/loader.json"
        const val BASIC_INFO = "basic_info_user"

        fun newIntent(context: Context): Intent =
            Intent(context, RedesignSettingsPhase2Activity::class.java)

        fun newIntentFor2FA(context: Context) =
            Intent(context, RedesignSettingsPhase2Activity::class.java).apply {
                Bundle().apply {
                    this.putBoolean(SettingsFragment.EXTRA_SHOW_TWO_FA_DIALOG, true)
                }
            }
    }
}

private fun Date.formatted() =
    SimpleDateFormat("MM/yyyy", Locale.getDefault()).format(this)
