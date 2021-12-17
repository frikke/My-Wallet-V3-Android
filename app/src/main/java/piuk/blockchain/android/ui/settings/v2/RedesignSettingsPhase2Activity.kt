package piuk.blockchain.android.ui.settings.v2

import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutManager
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import com.blockchain.componentlib.image.ImageResource
import com.blockchain.componentlib.navigation.NavigationBarButton
import com.blockchain.koin.scopedInject
import com.blockchain.nabu.BasicProfileInfo
import com.blockchain.notifications.analytics.AnalyticsEvents
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.ActivityRedesignPhase2SettingsBinding
import piuk.blockchain.android.databinding.ToolbarGeneralBinding
import piuk.blockchain.android.ui.FeatureFlagsHandlingActivity
import piuk.blockchain.android.ui.base.mvi.MviActivity
import piuk.blockchain.android.ui.home.ZendeskSubjectActivity
import piuk.blockchain.android.ui.settings.SettingsFragment
import piuk.blockchain.android.ui.settings.v2.profile.ProfileActivity
import piuk.blockchain.android.urllinks.URL_BLOCKCHAIN_SUPPORT_PORTAL
import piuk.blockchain.android.util.AndroidUtils
import piuk.blockchain.android.util.calloutToExternalSupportLinkDlg
import piuk.blockchain.android.util.gone
import piuk.blockchain.android.util.visibleIf
import piuk.blockchain.androidcore.data.api.EnvironmentConfig

class RedesignSettingsPhase2Activity :
    MviActivity<SettingsModel,
        SettingsIntent,
        SettingsState,
        ActivityRedesignPhase2SettingsBinding>() {

    override val model: SettingsModel by scopedInject()

    override fun initBinding(): ActivityRedesignPhase2SettingsBinding =
        ActivityRedesignPhase2SettingsBinding.inflate(layoutInflater)

    override val alwaysDisableScreenshots: Boolean = true

    override val toolbarBinding: ToolbarGeneralBinding
        get() = binding.toolbar

    private val environmentConfig: EnvironmentConfig by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        setupToolbar()
    }

    override fun onResume() {
        super.onResume()
        model.process(SettingsIntent.LoadInitialInformation)
    }

    override fun render(newState: SettingsState) {
        setupMenuItems(newState.basicProfileInfo)

        newState.basicProfileInfo?.let { userInfo ->
            if (newState.isSupportChatEnabled) {
                setupActiveSupportButton(userInfo)
            }

            setUserInfoHeader(userInfo)
        } ?: {
            // TODO what do we show if we don't know have any profile info
        }

        if (newState.viewToLaunch != ViewToLaunch.None) {
            when (newState.viewToLaunch) {
                ViewToLaunch.Profile -> startActivity(
                    newState.basicProfileInfo?.let {
                        ProfileActivity.newIntent(
                            this@RedesignSettingsPhase2Activity, it
                        )
                    }
                )
                ViewToLaunch.None -> {
                    // do nothing
                }
            }
            model.process(SettingsIntent.ResetViewState)
        }

        if (newState.hasWalletUnpaired) {
            analytics.logEvent(AnalyticsEvents.Logout)
            if (AndroidUtils.is25orHigher()) {
                getSystemService(ShortcutManager::class.java).removeAllDynamicShortcuts()
            }
        }
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

            payments.apply {
                primaryText = getString(R.string.settings_title_no_payments)
                secondaryText = getString(R.string.settings_subtitle_no_payments)
                onClick = {
                    // TODO open bottomsheet
                }
                startImageResource = ImageResource.Local(R.drawable.ic_payment_card, null)
            }

            accountGroup.apply {
                primaryText = getString(R.string.settings_title_account)
                secondaryText = getString(R.string.settings_subtitle_account)
                onClick = {
                    // TODO open
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

            settingsAddresses.apply {
                primaryText = getString(R.string.drawer_addresses)
                onClick = {
                    setResultIntent(SettingsAction.Addresses)
                }
                startImageResource = ImageResource.Local(R.drawable.ic_nav_wallet, null)
            }

            settingsWebLogin.apply {
                primaryText = getString(R.string.web_wallet_log_in)
                onClick = {
                    setResultIntent(SettingsAction.WebLogin)
                }
                startImageResource = ImageResource.Local(R.drawable.ic_nav_web_login, null)
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
            .setPositiveButton(R.string.btn_logout) { _, _ -> model.process(SettingsIntent.UnpairWallet) }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    // TODO extract methods so there is not duplicates in SimpleBiy
/*    private fun showAvailableToAddPaymentMethods() =
        showPaymentMethodsBottomSheet(
            paymentOptions = lastState?.paymentOptions ?: PaymentOptions(),
            state = SimpleBuyCryptoFragment.PaymentMethodsChooserState.AVAILABLE_TO_ADD
        )

    private fun showPaymentMethodsBottomSheet(paymentOptions: PaymentOptions, state: SimpleBuyCryptoFragment.PaymentMethodsChooserState) {
        showBottomSheet(
            PaymentMethodChooserBottomSheet.newInstance(
                when (state) {
                    SimpleBuyCryptoFragment.PaymentMethodsChooserState.AVAILABLE_TO_PAY ->
                        paymentOptions.availablePaymentMethods
                            .filter { method ->
                                method.canUsedForPaying()
                            }
                    SimpleBuyCryptoFragment.PaymentMethodsChooserState.AVAILABLE_TO_ADD ->
                        paymentOptions.availablePaymentMethods
                            .filter { method ->
                                method.canBeAdded()
                            }
                }
            )
        )
   } */

    private fun setUserInfoHeader(userInformation: BasicProfileInfo) {
        with(binding) {
            if (userInformation.email.isNotEmpty()) {
                if (userInformation.firstName == userInformation.email &&
                    userInformation.lastName == userInformation.email
                ) {
                    name.text = userInformation.email
                    name.animate().alpha(1f)
                    email.gone()
                } else {
                    name.text = getString(
                        R.string.common_spaced_strings, userInformation.firstName, userInformation.lastName
                    )
                    name.animate().alpha(1f)
                    email.text = userInformation.email
                    email.animate().alpha(1f)
                }
            } else {
                // TODO can user email ever be empty?
            }
        }
    }

    private fun setResultIntent(action: RedesignSettingsPhase2Activity.Companion.SettingsAction) {
        setResult(
            RESULT_OK,
            Intent().apply {
                putExtra(SETTINGS_RESULT_DATA, action)
            }
        )
        finish()
    }

    override fun onBackPressed() {
        setResult(RESULT_CANCELED)
        finish()
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
        const val BASIC_INFO = "basic_info_user"

        fun newIntent(context: Context): Intent =
            Intent(context, RedesignSettingsPhase2Activity::class.java)

        fun newIntentFor2FA(context: Context) =
            Intent(context, RedesignSettingsPhase2Activity::class.java).apply {
                Bundle().apply {
                    this.putBoolean(SettingsFragment.EXTRA_SHOW_TWO_FA_DIALOG, true)
                }
            }

        const val SETTINGS_RESULT_DATA = "SETTINGS_RESULT_DATA"

        enum class SettingsAction {
            Addresses,
            WebLogin
        }
    }
}
