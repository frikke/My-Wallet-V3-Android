package piuk.blockchain.android.ui.recurringbuy.onboarding

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import com.blockchain.analytics.Analytics
import com.blockchain.coincore.AssetAction
import com.blockchain.commonarch.presentation.base.BlockchainActivity
import com.blockchain.componentlib.navigation.ModeBackgroundColor
import com.blockchain.componentlib.viewextensions.visibleIf
import com.blockchain.home.presentation.navigation.AssetActionsNavigation
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

    override val statusbarColor: ModeBackgroundColor = ModeBackgroundColor.None

    private val binding: ActivityRecurringBuyOnBoardingBinding by lazy {
        ActivityRecurringBuyOnBoardingBinding.inflate(layoutInflater)
    }

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

        setupBackPress()

        val recurringBuyOnBoardingPagerAdapter =
            RecurringBuyOnBoardingPagerAdapter(this, createListOfRecurringBuyInfo())

        with(binding) {
            viewpager.adapter = recurringBuyOnBoardingPagerAdapter
            indicator.setViewPager(viewpager)
            recurringBuyCta.setOnClickListener {
                goToRecurringSetUpScreen()
                finish()
            }
            closeBtn.setOnClickListener { finish() }
        }
        setupViewPagerListener()
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
            headerText.visibleIf { isShown }
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
                    asset = asset
                )
            )
        } ?: assetActionsNavigation.navigate(AssetAction.Buy)
    }

    private fun createListOfRecurringBuyInfo(): List<RecurringBuyInfo> = listOf(
        RecurringBuyInfo(
            title1 = getString(R.string.recurring_buy_title_1_1),
            title2 = getString(R.string.recurring_buy_title_1_2)
        ),
        RecurringBuyInfo(
            title1 = getString(R.string.recurring_buy_title_2_1),
            title2 = getString(R.string.recurring_buy_title_2_2)
        ),
        RecurringBuyInfo(
            title1 = getString(R.string.recurring_buy_title_3_1),
            title2 = getString(R.string.recurring_buy_title_3_2)
        ),
        RecurringBuyInfo(
            title1 = getString(R.string.recurring_buy_title_4_1),
            title2 = getString(R.string.recurring_buy_title_4_2)
        ),
        RecurringBuyInfo(
            title1 = getString(R.string.recurring_buy_title_5_1),
            title2 = getString(R.string.recurring_buy_title_5_2),
            noteLink = R.string.recurring_buy_note
        )
    )

    private fun setupBackPress() {
        val backPressCallback = onBackPressedDispatcher.addCallback {
            binding.viewpager.currentItem = binding.viewpager.currentItem - 1
        }

        binding.viewpager.registerOnPageChangeCallback(object : OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)

                // when viewpager is not on the first page
                // enable custom backpress handling to return to previous page
                backPressCallback.isEnabled = position > 0
            }
        })
    }

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
