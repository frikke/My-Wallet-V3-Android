package piuk.blockchain.android.ui.settings.v2

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.blockchain.commonarch.presentation.base.BlockchainActivity
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.databinding.ToolbarGeneralBinding
import com.blockchain.componentlib.navigation.NavigationBarButton
import com.blockchain.componentlib.viewextensions.visibleIf
import com.blockchain.enviroment.EnvironmentConfig
import com.blockchain.koin.scopedInject
import com.blockchain.nabu.UserIdentity
import com.blockchain.notifications.analytics.AnalyticsEvents
import io.reactivex.rxjava3.disposables.CompositeDisposable
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.ActivityRedesignSettingsBinding
import piuk.blockchain.android.support.SupportCentreActivity
import piuk.blockchain.android.ui.debug.FeatureFlagsHandlingActivity
import piuk.blockchain.android.ui.settings.SettingsFragment

class SettingsActivity : BlockchainActivity() {

    private val binding: ActivityRedesignSettingsBinding by lazy {
        ActivityRedesignSettingsBinding.inflate(layoutInflater)
    }

    private val compositeDisposable = CompositeDisposable()
    private val userIdentity: UserIdentity by scopedInject()
    private val environmentConfig: EnvironmentConfig by inject()

    override val alwaysDisableScreenshots: Boolean = true

    override val toolbarBinding: ToolbarGeneralBinding
        get() = binding.toolbar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        setupToolbar()
        setupMenuItems()

        supportFragmentManager.beginTransaction()
            .replace(R.id.content_frame, SettingsFragment.newInstance(), SettingsFragment::class.simpleName)
            .commitAllowingStateLoss()
    }

    private fun setupMenuItems() {
        with(binding) {
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
        updateToolbarMenuItems(
            listOf(
                NavigationBarButton.Icon(R.drawable.ic_support_chat) {
                    analytics.logEvent(AnalyticsEvents.Support)
                    startActivity(SupportCentreActivity.newIntent(this))
                }
            )
        )
        updateToolbarTitle(getString(R.string.toolbar_settings))
        updateToolbarBackAction { onBackPressed() }
    }

    companion object {
        fun newIntent(context: Context): Intent =
            Intent(context, SettingsActivity::class.java)

        fun newIntentFor2FA(context: Context) =
            Intent(context, SettingsActivity::class.java).apply {
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
