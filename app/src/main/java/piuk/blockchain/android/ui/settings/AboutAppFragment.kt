package piuk.blockchain.android.ui.settings

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import com.blockchain.commonarch.presentation.base.BlockchainActivity
import com.blockchain.commonarch.presentation.base.updateToolbar
import com.blockchain.componentlib.viewextensions.visible
import com.blockchain.featureflag.FeatureFlag
import com.blockchain.koin.darkModeFeatureFlag
import org.koin.android.ext.android.inject
import piuk.blockchain.android.databinding.FragmentAboutAppBinding
import piuk.blockchain.android.rating.presentaion.AppRatingFragment
import piuk.blockchain.android.rating.presentaion.AppRatingTriggerSource
import piuk.blockchain.android.urllinks.URL_PRIVACY_POLICY
import piuk.blockchain.android.urllinks.URL_TOS_POLICY

class AboutAppFragment : Fragment(), SettingsScreen {

    private var _binding: FragmentAboutAppBinding? = null
    private val binding: FragmentAboutAppBinding
        get() = _binding!!

    override fun navigator(): SettingsNavigator =
        (activity as? SettingsNavigator) ?: throw IllegalStateException(
            "Parent must implement SettingsNavigator"
        )

    private val darkModeFF: FeatureFlag by inject(darkModeFeatureFlag)

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
            toolbarTitle = getString(com.blockchain.stringResources.R.string.about_app_toolbar),
            menuItems = emptyList()
        )

        initUi()
    }

    private fun initUi() {
        with(binding) {
            supportOption.apply {
                primaryText = getString(com.blockchain.stringResources.R.string.about_app_contact_support)
                onClick = {
                    navigator().goToSupportCentre()
                }
            }

            rateOption.apply {
                primaryText = getString(com.blockchain.stringResources.R.string.about_app_rate_app)
                onClick = {
                    showAppRating()
                }
            }
            termsOption.apply {
                primaryText = getString(com.blockchain.stringResources.R.string.about_app_terms_service)
                onClick = { onTermsOfServiceClicked() }
            }
            privacyOption.apply {
                primaryText = getString(com.blockchain.stringResources.R.string.about_app_privacy_policy)
                onClick = { onPrivacyClicked() }
            }

            darkModeFF.enabled
                .onErrorReturn { false }
                .subscribe { enabled ->
                    if (enabled) {
                        light.visible()
                        light.setOnClickListener {
                            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                        }

                        dark.visible()
                        dark.setOnClickListener {
                            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                        }
                    }
                }
        }
    }

    private fun showAppRating() {
        AppRatingFragment.newInstance(AppRatingTriggerSource.SETTINGS)
            .show(childFragmentManager, AppRatingFragment.TAG)
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
