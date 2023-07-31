package piuk.blockchain.android.ui.recurringbuy.onboarding

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import com.blockchain.chrome.navigation.AssetActionsNavigation
import com.blockchain.commonarch.presentation.base.BlockchainActivity
import com.blockchain.componentlib.databinding.ToolbarGeneralBinding
import com.blockchain.componentlib.viewextensions.invisibleIf
import com.blockchain.home.presentation.recurringbuy.RecurringBuysAnalyticsEvents
import com.blockchain.koin.payloadScope
import com.blockchain.utils.unsafeLazy
import info.blockchain.balance.AssetCatalogue
import info.blockchain.balance.AssetInfo
import org.koin.android.ext.android.inject
import org.koin.core.parameter.parametersOf
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.ActivityRecurringBuyOnBoardingBinding
import piuk.blockchain.android.simplebuy.SimpleBuyActivity
import piuk.blockchain.android.ui.recurringbuy.RecurringBuyAnalytics

class RecurringBuyOnboardingActivity : BlockchainActivity() {

    override val alwaysDisableScreenshots: Boolean = false

    private val binding: ActivityRecurringBuyOnBoardingBinding by lazy {
        ActivityRecurringBuyOnBoardingBinding.inflate(layoutInflater)
    }

    override val toolbarBinding: ToolbarGeneralBinding
        get() = binding.toolbar

    private val assetCatalogue: AssetCatalogue by inject()

    private val asset: AssetInfo? by unsafeLazy {
        intent?.getStringExtra(ASSET)?.let {
            assetCatalogue.assetInfoFromNetworkTicker(it)
        }
    }

    private val assetActionsNavigation: AssetActionsNavigation = payloadScope.get {
        parametersOf(
            this
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showFullScreen()
        setContentView(binding.root)

        updateToolbarBackAction {
            onBackPressedDispatcher.onBackPressed()
        }
        updateToolbarTitle(getString(com.blockchain.stringResources.R.string.recurring_buy_toolbar))
        updateToolbarBackground()

        val recurringBuyOnBoardingPagerAdapter =
            RecurringBuyOnBoardingPagerAdapter(this, createListOfRecurringBuyInfo())

        with(binding) {
            viewpager.adapter = recurringBuyOnBoardingPagerAdapter
            indicator.setViewPager(viewpager)
            recurringBuyCta.apply {
                text = getString(com.blockchain.stringResources.R.string.recurring_buy_cta_1)
                onClick = {
                    goToRecurringSetUpScreen()
                    finish()
                }
            }
        }
        setupViewPagerListener()

        analytics.logEvent(RecurringBuysAnalyticsEvents.OnboardingViewed)
    }

    private fun showFullScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(true)
        } else {
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN
        }
    }

    private fun showHeader(isShown: Boolean) {
        binding.apply {
            headerText.invisibleIf { !isShown }
        }
    }

    private fun setupViewPagerListener() {
        binding.viewpager.registerOnPageChangeCallback(object : OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                showHeader(position == 0)
                analytics.logEvent(RecurringBuyAnalytics.RecurringBuyInfoViewed(position))
                playLottieAnimationInterval(position)
            }
        })
    }

    private fun playLottieAnimationInterval(position: Int) {
        val minFrames = FRAMES_PER_SCREEN.times(position)
        val maxFrames = minFrames + FRAMES_PER_SCREEN
        binding.lottieAnimation.apply {
            setMinFrame(minFrames)
            setMaxFrame(maxFrames)
            playAnimation()
        }
    }

    private fun goToRecurringSetUpScreen() {
        asset?.let {
            startActivity(
                SimpleBuyActivity.newIntent(
                    context = this,
                    asset = asset,
                    fromRecurringBuy = true
                )
            )
        } ?: assetActionsNavigation.buyCryptoWithRecurringBuy()
    }

    private fun createListOfRecurringBuyInfo(): List<RecurringBuyInfo> = listOf(
        RecurringBuyInfo(
            title1 = getString(com.blockchain.stringResources.R.string.recurring_buy_title_1_1),
            title2 = getString(com.blockchain.stringResources.R.string.recurring_buy_title_1_2)
        ),
        RecurringBuyInfo(
            title1 = getString(com.blockchain.stringResources.R.string.recurring_buy_title_2_1),
            title2 = getString(com.blockchain.stringResources.R.string.recurring_buy_title_2_2)
        ),
        RecurringBuyInfo(
            title1 = getString(com.blockchain.stringResources.R.string.recurring_buy_title_3_1),
            title2 = getString(com.blockchain.stringResources.R.string.recurring_buy_title_3_2)
        ),
        RecurringBuyInfo(
            title1 = getString(com.blockchain.stringResources.R.string.recurring_buy_title_4_1),
            title2 = getString(com.blockchain.stringResources.R.string.recurring_buy_title_4_2)
        ),
        RecurringBuyInfo(
            title1 = getString(com.blockchain.stringResources.R.string.recurring_buy_title_5_1),
            title2 = getString(com.blockchain.stringResources.R.string.recurring_buy_title_5_2),
            noteLink = com.blockchain.stringResources.R.string.recurring_buy_note
        )
    )

    companion object {
        private const val FRAMES_PER_SCREEN = 60
        private const val ASSET = "ASSET"

        fun newIntent(
            context: Context,
            assetTicker: String?
        ): Intent = Intent(context, RecurringBuyOnboardingActivity::class.java).apply {
            putExtra(ASSET, assetTicker)
        }
    }
}
