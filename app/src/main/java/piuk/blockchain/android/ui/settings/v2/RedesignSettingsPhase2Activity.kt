package piuk.blockchain.android.ui.settings.v2

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import com.blockchain.commonarch.presentation.base.BlockchainActivity
import com.blockchain.commonarch.presentation.base.FlowFragment
import com.blockchain.componentlib.databinding.ToolbarGeneralBinding
import com.blockchain.componentlib.navigation.NavigationBarButton
import com.blockchain.nabu.BasicProfileInfo
import com.blockchain.nabu.Tier
import com.blockchain.notifications.analytics.AnalyticsEvents
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.ActivityRedesignPhase2SettingsBinding
import piuk.blockchain.android.support.SupportCentreActivity
import piuk.blockchain.android.ui.airdrops.AirdropCentreActivity
import piuk.blockchain.android.ui.base.addAnimationTransaction
import piuk.blockchain.android.ui.debug.FeatureFlagsHandlingActivity
import piuk.blockchain.android.ui.kyc.limits.KycLimitsActivity
import piuk.blockchain.android.ui.settings.v2.account.AccountFragment
import piuk.blockchain.android.ui.settings.v2.notifications.NotificationsFragment
import piuk.blockchain.android.ui.settings.v2.profile.ProfileActivity
import piuk.blockchain.android.ui.settings.v2.security.SecurityFragment
import piuk.blockchain.android.ui.settings.v2.security.password.PasswordChangeFragment
import piuk.blockchain.android.ui.thepit.PitPermissionsActivity

class RedesignSettingsPhase2Activity : BlockchainActivity(), SettingsNavigator {

    private val binding: ActivityRedesignPhase2SettingsBinding by lazy {
        ActivityRedesignPhase2SettingsBinding.inflate(layoutInflater)
    }

    override val alwaysDisableScreenshots: Boolean = true

    override val toolbarBinding: ToolbarGeneralBinding
        get() = binding.toolbar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        setupToolbar()

        supportFragmentManager.beginTransaction()
            .addAnimationTransaction()
            .add(
                binding.settingsContentFrame.id, RedesignSettingsFragment.newInstance()
            )
            .commitNowAllowingStateLoss()
    }

    private fun setupToolbar() {
        updateToolbar(
            toolbarTitle = getString(R.string.toolbar_settings),
            backAction = { onBackPressed() }
        )
        setupSupportButton()
    }

    private fun setupSupportButton() {
        updateToolbarMenuItems(
            listOf(
                NavigationBarButton.Icon(R.drawable.ic_support_chat) {
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

    override fun goToProfile(basicProfileInfo: BasicProfileInfo, tier: Tier) {
        startActivity(ProfileActivity.newIntent(this, basicProfileInfo, tier))
    }

    override fun goToAccount() {
        replaceCurrentFragment(AccountFragment.newInstance())
    }

    override fun goToNotifications() {
        replaceCurrentFragment(NotificationsFragment.newInstance())
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

    override fun goToExchange() {
        PitPermissionsActivity.start(this, "")
    }

    override fun goToKycLimits() {
        startActivity(KycLimitsActivity.newIntent(this))
    }

    private fun replaceCurrentFragment(newFragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .addAnimationTransaction()
            .replace(
                binding.settingsContentFrame.id, newFragment, newFragment::class.simpleName
            ).addToBackStack(newFragment::class.simpleName)
            .commitAllowingStateLoss()
    }

    companion object {
        const val BASIC_INFO = "basic_info_user"
        const val USER_TIER = "user_tier"

        fun newIntent(context: Context): Intent =
            Intent(context, RedesignSettingsPhase2Activity::class.java)
    }
}

interface SettingsNavigator {
    fun goToAboutApp()
    fun goToProfile(basicProfileInfo: BasicProfileInfo, tier: Tier)
    fun goToAccount()
    fun goToNotifications()
    fun goToSecurity()
    fun goToFeatureFlags()
    fun goToSupportCentre()
    fun goToAirdrops()
    fun goToExchange()
    fun goToKycLimits()
    fun goToPasswordChange()
}

interface SettingsScreen : FlowFragment {
    fun navigator(): SettingsNavigator
}
