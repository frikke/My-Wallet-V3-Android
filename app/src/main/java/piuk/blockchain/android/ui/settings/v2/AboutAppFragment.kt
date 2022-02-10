package piuk.blockchain.android.ui.settings.v2

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.blockchain.commonarch.presentation.base.BlockchainActivity
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.FragmentAboutAppBinding
import piuk.blockchain.android.ui.base.updateToolbar
import piuk.blockchain.android.ui.settings.SettingsAnalytics
import piuk.blockchain.android.urllinks.URL_PRIVACY_POLICY
import piuk.blockchain.android.urllinks.URL_TOS_POLICY
import timber.log.Timber

class AboutAppFragment : Fragment(), SettingsScreen {

    private var _binding: FragmentAboutAppBinding? = null
    private val binding: FragmentAboutAppBinding
        get() = _binding!!

    override fun navigator(): SettingsNavigator =
        (activity as? SettingsNavigator) ?: throw IllegalStateException(
            "Parent must implement SettingsNavigator"
        )

    override fun onBackPressed(): Boolean = true

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAboutAppBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        updateToolbar(
            toolbarTitle = getString(R.string.about_app_toolbar),
            menuItems = emptyList()
        )

        initUi()
    }

    private fun initUi() {
        with(binding) {
            supportOption.apply {
                primaryText = getString(R.string.about_app_contact_support)
                onClick = {
                    navigator().goToSupportCentre()
                }
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

    private fun goToPlayStore() {
        val flags = Intent.FLAG_ACTIVITY_NO_HISTORY or
            Intent.FLAG_ACTIVITY_MULTIPLE_TASK or Intent.FLAG_ACTIVITY_NEW_DOCUMENT
        try {
            Intent(
                Intent.ACTION_VIEW,
                Uri.parse("market://details?id=${requireActivity().packageName}")
            ).let {
                it.addFlags(flags)
                startActivity(it)
            }
        } catch (e: ActivityNotFoundException) {
            Timber.e(e, "Google Play Store not found")
        }
    }

    private fun onTermsOfServiceClicked() {
        (requireActivity() as BlockchainActivity).analytics.logEvent(
            SettingsAnalytics.SettingsHyperlinkClicked(
                SettingsAnalytics.AnalyticsHyperlinkDestination.TERMS_OF_SERVICE
            )
        )
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(URL_TOS_POLICY)))
    }

    private fun onPrivacyClicked() {
        (requireActivity() as BlockchainActivity).analytics.logEvent(
            SettingsAnalytics.SettingsHyperlinkClicked(
                SettingsAnalytics.AnalyticsHyperlinkDestination.PRIVACY_POLICY
            )
        )
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(URL_PRIVACY_POLICY)))
    }

    companion object {
        fun newInstance(): AboutAppFragment = AboutAppFragment()
    }
}
