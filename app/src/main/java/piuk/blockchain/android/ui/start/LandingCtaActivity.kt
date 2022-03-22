package piuk.blockchain.android.ui.start

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.blockchain.commonarch.presentation.base.BlockchainActivity
import com.blockchain.componentlib.databinding.ToolbarGeneralBinding
import com.blockchain.componentlib.navigation.NavigationBarButton
import com.blockchain.componentlib.theme.Blue600
import com.blockchain.koin.scopedInject
import com.blockchain.preferences.OnboardingPrefs
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.ActivityLandingCtaBinding
import piuk.blockchain.android.ui.createwallet.CreateWalletActivity
import piuk.blockchain.android.ui.login.LoginActivity

class LandingCtaActivity : BlockchainActivity() {
    override val alwaysDisableScreenshots: Boolean = false

    private val binding: ActivityLandingCtaBinding by lazy {
        ActivityLandingCtaBinding.inflate(layoutInflater)
    }

    override val toolbarBinding: ToolbarGeneralBinding? = null
    private val onboardingPrefs: OnboardingPrefs by scopedInject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        binding.toolbar.apply {
            startNavigationBarButton = NavigationBarButton.Icon(
                drawable = R.drawable.ic_close_circle_v2,
                color = null,
                onIconClick = {
                    onboardingPrefs.isLandingCtaDismissed = true
                    onBackPressed()
                }
            )
            endNavigationBarButtons = listOf(
                NavigationBarButton.Text(
                    text = getString(R.string.landing_cta_login),
                    color = Blue600,
                    onTextClick = {
                        analytics.logEvent(LandingAnalytics.LogInClicked)
                        onboardingPrefs.isLandingCtaDismissed = true
                        startActivity(Intent(this@LandingCtaActivity, LoginActivity::class.java))
                        finish()
                    }
                )
            )
        }

        binding.buttonCta.setOnClickListener {
            analytics.logEvent(LandingAnalytics.BuyCryptoCtaClicked)
            onboardingPrefs.isLandingCtaDismissed = true
            CreateWalletActivity.start(this)
            finish()
        }
    }

    override fun onBackPressed() {
        finish()
        overridePendingTransition(0, R.anim.slide_down_to_bottom)
    }

    companion object {
        @JvmStatic
        fun newIntent(context: Context): Intent = Intent(context, LandingCtaActivity::class.java)
    }
}
