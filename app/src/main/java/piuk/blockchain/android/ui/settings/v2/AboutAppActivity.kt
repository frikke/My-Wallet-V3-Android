package piuk.blockchain.android.ui.settings.v2

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import com.blockchain.commonarch.presentation.base.BlockchainActivity
import com.blockchain.componentlib.databinding.ToolbarGeneralBinding
import com.blockchain.nabu.BasicProfileInfo
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.ActivityAboutAppBinding
import piuk.blockchain.android.ui.airdrops.AirdropCentreActivity
import piuk.blockchain.android.ui.home.ZendeskSubjectActivity
import piuk.blockchain.android.ui.settings.SettingsAnalytics
import piuk.blockchain.android.ui.settings.v2.RedesignSettingsPhase2Activity.Companion.BASIC_INFO
import piuk.blockchain.android.urllinks.URL_PRIVACY_POLICY
import piuk.blockchain.android.urllinks.URL_TOS_POLICY
import timber.log.Timber

class AboutAppActivity : BlockchainActivity() {

    private val binding: ActivityAboutAppBinding by lazy {
        ActivityAboutAppBinding.inflate(layoutInflater)
    }

    override val alwaysDisableScreenshots: Boolean = true

    override val toolbarBinding: ToolbarGeneralBinding
        get() = binding.toolbar

    private val basicProfileInfo by lazy {
        intent.getSerializableExtra(BASIC_INFO) as BasicProfileInfo
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        setupToolbar()
        setMenuOptions()
    }

    private fun setMenuOptions() {
        with(binding) {
            supportOption.apply {
                primaryText = getString(R.string.about_app_contact_support)
                onClick = {
                    startActivity(ZendeskSubjectActivity.newInstance(context, basicProfileInfo))
                }
            }
            airdropsOption.apply {
                primaryText = getString(R.string.about_app_airdrops)
                onClick = { startActivity(AirdropCentreActivity.newIntent(context)) }
            }
            rateOption.apply {
                primaryText = getString(R.string.about_app_rate_app)
                onClick = { goToPlayStore() }
            }
            termsOption.apply {
                primaryText = getString(R.string.about_app_terms_service)
                onClick = { onTermsOfServiceClicked() }
            }
            privacyOption.apply {
                primaryText = getString(R.string.about_app_privacy_policy)
                onClick = { onPrivacyClicked() }
            }
        }
    }

    private fun setupToolbar() {
        updateToolbar(
            toolbarTitle = getString(R.string.about_app_toolbar),
            backAction = { onBackPressed() }
        )
    }

    private fun goToPlayStore() {
        val flags = Intent.FLAG_ACTIVITY_NO_HISTORY or
            Intent.FLAG_ACTIVITY_MULTIPLE_TASK or Intent.FLAG_ACTIVITY_NEW_DOCUMENT
        try {
            Intent(
                Intent.ACTION_VIEW,
                Uri.parse("market://details?id=$packageName")
            ).let {
                it.addFlags(flags)
                startActivity(it)
            }
        } catch (e: ActivityNotFoundException) {
            Timber.e(e, "Google Play Store not found")
        }
    }

    private fun onTermsOfServiceClicked() {
        analytics.logEvent(
            SettingsAnalytics.SettingsHyperlinkClicked(
                SettingsAnalytics.AnalyticsHyperlinkDestination.TERMS_OF_SERVICE
            )
        )
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(URL_TOS_POLICY)))
    }

    private fun onPrivacyClicked() {
        analytics.logEvent(
            SettingsAnalytics.SettingsHyperlinkClicked(
                SettingsAnalytics.AnalyticsHyperlinkDestination.PRIVACY_POLICY
            )
        )
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(URL_PRIVACY_POLICY)))
    }

    companion object {
        fun newIntent(context: Context, basicProfileInfo: BasicProfileInfo) =
            Intent(context, AboutAppActivity::class.java).apply {
                putExtra(BASIC_INFO, basicProfileInfo)
            }
    }
}
