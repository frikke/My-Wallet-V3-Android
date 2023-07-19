package piuk.blockchain.android.ui.start

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.blockchain.commonarch.presentation.base.BlockchainActivity
import com.blockchain.componentlib.databinding.ToolbarGeneralBinding
import com.blockchain.componentlib.navigation.NavigationBarButton
import com.blockchain.preferences.OnboardingPrefs
import com.blockchain.presentation.koin.scopedInject
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
            startNavigationButton = NavigationBarButton.Icon(
                drawable = com.blockchain.common.R.drawable.ic_close_circle_v2,
                contentDescription = com.blockchain.stringResources.R.string.accessibility_close,
                color = null,
                onIconClick = {
                    onboardingPrefs.isLandingCtaDismissed = true
                    onBackPressedDispatcher.onBackPressed()
                }
            )
            endNavigationBarButtons = listOf(
                NavigationBarButton.TextWithColorInt(
                    text = getString(com.blockchain.stringResources.R.string.landing_cta_login),
                    colorId = com.blockchain.componentlib.R.color.primary,
                    onTextClick = {
                        analytics.logEvent(LandingAnalytics.LogInClicked)
                        onboardingPrefs.isLandingCtaDismissed = true
                        startActivity(Intent(this@LandingCtaActivity, LoginActivity::class.java))
                        finish()
                    }
                )
            )
        }

        binding.buttonCta.apply {
            text = getString(com.blockchain.stringResources.R.string.common_buy_crypto)
            onClick = {
                analytics.logEvent(LandingAnalytics.BuyCryptoCtaClicked)
                onboardingPrefs.isLandingCtaDismissed = true
                CreateWalletActivity.start(this@LandingCtaActivity)
                finish()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        if (isFinishing) overridePendingTransition(0, R.anim.slide_down_to_bottom)
    }

    companion object {
        @JvmStatic
        fun newIntent(context: Context): Intent = Intent(context, LandingCtaActivity::class.java)
    }
}
