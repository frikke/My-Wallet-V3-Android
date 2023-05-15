package piuk.blockchain.android.ui.settings

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import com.blockchain.analytics.events.AnalyticsEvents
import com.blockchain.api.services.ContactPreference
import com.blockchain.commonarch.presentation.base.BlockchainActivity
import com.blockchain.commonarch.presentation.base.addTransactionAnimation
import com.blockchain.componentlib.databinding.ToolbarGeneralBinding
import com.blockchain.componentlib.navigation.NavigationBarButton
import com.blockchain.core.kyc.domain.model.KycTier
import com.blockchain.domain.paymentmethods.model.LinkedPaymentMethod
import com.blockchain.domain.paymentmethods.model.PaymentLimits
import com.blockchain.home.presentation.navigation.SettingsDestination
import com.blockchain.nabu.BasicProfileInfo
import com.blockchain.walletconnect.ui.dapps.DappsListFragment
import piuk.blockchain.android.R
import piuk.blockchain.android.cards.CardDetailsActivity
import piuk.blockchain.android.databinding.ActivitySettingsBinding
import piuk.blockchain.android.support.SupportCentreActivity
import piuk.blockchain.android.ui.addresses.AddressesActivity
import piuk.blockchain.android.ui.airdrops.AirdropCentreActivity
import piuk.blockchain.android.ui.debug.FeatureFlagsHandlingActivity
import piuk.blockchain.android.ui.kyc.limits.KycLimitsActivity
import piuk.blockchain.android.ui.referral.presentation.ReferralSheet
import piuk.blockchain.android.ui.settings.account.AccountFragment
import piuk.blockchain.android.ui.settings.appprefs.LocalSettingsFragment
import piuk.blockchain.android.ui.settings.notificationpreferences.NotificationPreferencesAnalyticsEvents
import piuk.blockchain.android.ui.settings.notificationpreferences.NotificationPreferencesFragment
import piuk.blockchain.android.ui.settings.notificationpreferences.details.NotificationPreferenceDetailsFragment
import piuk.blockchain.android.ui.settings.profile.ProfileActivity
import piuk.blockchain.android.ui.settings.security.SecurityFragment
import piuk.blockchain.android.ui.settings.security.password.PasswordChangeFragment
import piuk.blockchain.android.ui.settings.security.pin.PinActivity
import timber.log.Timber

class SettingsActivity : BlockchainActivity(), SettingsNavigator, SettingsFragment.Host {

    private val binding: ActivitySettingsBinding by lazy {
        ActivitySettingsBinding.inflate(layoutInflater)
    }

    private var basicProfileInfo: BasicProfileInfo? = null
    private var tier: KycTier? = null

    override val alwaysDisableScreenshots: Boolean = true

    override val toolbarBinding: ToolbarGeneralBinding
        get() = binding.toolbar

    private val deeplinkToScreen by lazy {
        (intent.getSerializableExtra(DEEPLINK_TO_SCREEN) as? SettingsDestination) ?: SettingsDestination.Home
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        setupToolbar()

        val settingsHomeFragment = SettingsFragment.newInstance()
        supportFragmentManager.beginTransaction()
            .addTransactionAnimation()
            .replace(
                binding.settingsContentFrame.id,
                settingsHomeFragment
            )
            .commitNowAllowingStateLoss()

        when (deeplinkToScreen) {
            SettingsDestination.Home -> {
                // do nothing
            }
            SettingsDestination.Account -> goToAccount()
            SettingsDestination.Notifications -> goToNotifications()
            SettingsDestination.Security -> goToSecurity()
            SettingsDestination.General -> goToGeneralSettings()
            SettingsDestination.About -> goToAboutApp()
            SettingsDestination.CardLinking -> startActivity(CardDetailsActivity.newIntent(this))
            SettingsDestination.BankLinking -> settingsHomeFragment.onLinkBankSelected()
        }
    }

    private fun setupToolbar() {
        updateToolbar(
            toolbarTitle = getString(com.blockchain.stringResources.R.string.toolbar_settings),
            backAction = { onBackPressedDispatcher.onBackPressed() }
        )
        setupSupportButton()
    }

    private fun setupSupportButton() {
        updateToolbarMenuItems(
            listOf(
                NavigationBarButton.Icon(
                    drawable = R.drawable.ic_support_chat,
                    contentDescription = com.blockchain.stringResources.R.string.accessibility_support
                ) {
                    analytics.logEvent(AnalyticsEvents.Support)
                    startActivity(SupportCentreActivity.newIntent(this))
                }
            )
        )
    }

    override fun goToAboutApp() {
        replaceCurrentFragment(AboutAppFragment.newInstance())
    }

    override fun goToPasswordChange() {
        replaceCurrentFragment(PasswordChangeFragment.newInstance())
    }

    override fun goToPinChange() {
        startActivity(
            PinActivity.newIntent(
                context = this,
                startForResult = false,
                originScreen = PinActivity.Companion.OriginScreenToPin.CHANGE_PIN_SECURITY,
                addFlagsToClear = false
            )
        )
    }

    override fun updateBasicProfile(basicProfileInfo: BasicProfileInfo) {
        this.basicProfileInfo = basicProfileInfo
    }

    override fun updateTier(tier: KycTier) {
        this.tier = tier
    }

    override fun goToProfile() {
        if (basicProfileInfo != null && tier != null) {
            startActivity(ProfileActivity.newIntent(this, basicProfileInfo!!, tier!!))
        } else {
            Timber.e("initialization for basicProfileInfo and tier went wrong")
        }
    }

    override fun goToAccount() {
        replaceCurrentFragment(AccountFragment.newInstance())
    }

    override fun goToNotifications() {
        analytics.logEvent(NotificationPreferencesAnalyticsEvents.NotificationClicked)
        replaceCurrentFragment(NotificationPreferencesFragment.newInstance())
    }

    override fun goToNotificationPreferences() {
        replaceCurrentFragment(NotificationPreferencesFragment.newInstance())
    }

    override fun goToNotificationPreferencesDetails(preference: ContactPreference) {
        replaceCurrentFragment(NotificationPreferenceDetailsFragment.newInstance(preference))
    }

    override fun goToSecurity() {
        replaceCurrentFragment(SecurityFragment.newInstance())
    }

    override fun goToFeatureFlags() {
        startActivity(FeatureFlagsHandlingActivity.newIntent(this))
    }

    override fun goToSupportCentre() {
        startActivity(SupportCentreActivity.newIntent(this))
    }

    override fun goToAirdrops() {
        startActivity(AirdropCentreActivity.newIntent(this))
    }

    override fun goToAddresses() {
        startActivity(AddressesActivity.newIntent(this))
    }

    override fun goToKycLimits() {
        startActivity(KycLimitsActivity.newIntent(this))
    }

    override fun goToWalletConnect() {
        replaceCurrentFragment(DappsListFragment.newInstance())
    }

    override fun goToReferralCode() {
        showBottomSheet(ReferralSheet.newInstance())
    }

    override fun goToGeneralSettings() {
        replaceCurrentFragment(LocalSettingsFragment.newInstance())
    }

    private fun replaceCurrentFragment(newFragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .addTransactionAnimation()
            .add(
                binding.settingsContentFrame.id,
                newFragment,
                newFragment::class.simpleName
            ).addToBackStack(newFragment::class.simpleName)
            .commitAllowingStateLoss()
    }

    companion object {
        const val BASIC_INFO = "basic_info_user"
        const val USER_TIER = "user_tier"
        private const val DEEPLINK_TO_SCREEN = "DEEPLINK_TO_SCREEN"

        const val SETTINGS_RESULT_DATA = "SETTINGS_RESULT_DATA"

        enum class SettingsAction {
            Addresses,
            Airdrops,
            WebLogin,
            Logout
        }

        fun newIntent(
            context: Context,
            deeplinkToScreen: SettingsDestination = SettingsDestination.Home
        ): Intent =
            Intent(context, SettingsActivity::class.java).apply {
                putExtra(DEEPLINK_TO_SCREEN, deeplinkToScreen)
            }
    }
}

interface SettingsNavigator {
    fun goToAboutApp()
    fun goToProfile()
    fun goToAccount()
    fun goToNotifications()
    fun goToNotificationPreferences()
    fun goToSecurity()
    fun goToFeatureFlags()
    fun goToSupportCentre()
    fun goToAirdrops()
    fun goToAddresses()
    fun goToWalletConnect()
    fun goToKycLimits()
    fun goToPasswordChange()
    fun goToPinChange()
    fun goToNotificationPreferencesDetails(preference: ContactPreference)
    fun goToReferralCode()
    fun goToGeneralSettings()
}

interface SettingsScreen {
    fun navigator(): SettingsNavigator
}

data class BankItem(
    val bank: LinkedPaymentMethod.Bank,
    val canBeUsedToTransact: Boolean,
    val limits: PaymentLimits
)
