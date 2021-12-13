package piuk.blockchain.android.ui.settings.v2

import android.content.Context
import android.content.Intent
import android.os.Bundle
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
import piuk.blockchain.android.urllinks.URL_BLOCKCHAIN_SUPPORT_PORTAL
import piuk.blockchain.android.util.calloutToExternalSupportLinkDlg
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

    override val toolbarBinding: ToolbarGeneralBinding?
        get() = null

    private val environmentConfig: EnvironmentConfig by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        setupToolbar()
        setupMenuItems()

        setHeader()

        supportFragmentManager.beginTransaction()
            .replace(R.id.content_frame, SettingsFragment.newInstance(), SettingsFragment::class.simpleName)
            .commitAllowingStateLoss()
    }

    override fun render(newState: SettingsState) {
        model.process(SettingsIntent.LoadContactSupportEligibility)
        newState.userInformation?.let {
            setupActiveSupportButton(it)
        }
    }

    private fun setupMenuItems() {
        with(binding) {
            seeProfile.apply {
                text = context.getString(R.string.settings_see_profile)
                onClick = {
                    startActivity(ProfileActivity.newIntent(context))
                }
            }

            settingsAddresses.apply {
                primaryText = getString(R.string.drawer_addresses)
                onClick = {
                    setResultIntent(SettingsAction.Addresses)
                }
                startImageResource = ImageResource.Local(R.drawable.ic_nav_wallet, null)
            }

            settingsExchange.apply {
                primaryText = getString(R.string.item_the_exchange)
                onClick = {
                    setResultIntent(SettingsAction.Exchange)
                }
                startImageResource = ImageResource.Local(R.drawable.ic_nav_the_exchange, null)
            }

            settingsAirdrops.apply {
                primaryText = getString(R.string.item_airdrops)
                onClick = {
                    setResultIntent(SettingsAction.Airdrops)
                }
                startImageResource = ImageResource.Local(R.drawable.ic_nav_airdrops, null)
            }

            settingsWebLogin.apply {
                primaryText = getString(R.string.web_wallet_log_in)
                onClick = {
                    setResultIntent(SettingsAction.WebLogin)
                }
                startImageResource = ImageResource.Local(R.drawable.ic_nav_web_login, null)
            }

            settingsLogout.apply {
                primaryText = getString(R.string.logout)
                onClick = {
                    setResultIntent(SettingsAction.Logout)
                }
                startImageResource = ImageResource.Local(R.drawable.ic_nav_logout, null)
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

    // TODO set image, tier icon
    private fun setHeader() {
        with(binding) {
            name.text = "Paco Martinez"
            email.text = "paco@gmail.com"
        }
    }

    private fun setResultIntent(action: SettingsAction) {
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
        binding.toolbar.apply {
            title = "Settings phase 2"
            onBackButtonClick = { onBackPressed() }
        }
        setupDefaultSupportButton()
    }

    private fun setupDefaultSupportButton() {
        binding.toolbar.endNavigationBarButtons = listOf(
            NavigationBarButton.Icon(R.drawable.ic_support_chat) {
                analytics.logEvent(AnalyticsEvents.Support)
                calloutToExternalSupportLinkDlg(this, URL_BLOCKCHAIN_SUPPORT_PORTAL)
            }
        )
    }

    private fun setupActiveSupportButton(userInformation: BasicProfileInfo) {
        binding.toolbar.endNavigationBarButtons = listOf(
            NavigationBarButton.Icon(R.drawable.ic_support_chat) {
                analytics.logEvent(AnalyticsEvents.Support)
                startActivity(ZendeskSubjectActivity.newInstance(this, userInformation))
            }
        )
    }

    override fun onDestroy() {
        model.clearDisposables()
        super.onDestroy()
    }

    companion object {
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
            Exchange,
            Airdrops,
            WebLogin,
            Logout
        }
    }
}
