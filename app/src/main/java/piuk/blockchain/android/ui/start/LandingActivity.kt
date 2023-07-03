package piuk.blockchain.android.ui.start

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.method.LinkMovementMethod
import android.widget.Button
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.Lifecycle
import com.blockchain.componentlib.alert.BlockchainSnackbar
import com.blockchain.componentlib.alert.SnackbarType
import com.blockchain.componentlib.carousel.CarouselViewType
import com.blockchain.componentlib.price.PriceView
import com.blockchain.componentlib.utils.openUrl
import com.blockchain.componentlib.viewextensions.visible
import com.blockchain.presentation.koin.scopedInject
import java.util.Timer
import java.util.TimerTask
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.ActivityLandingOnboardingBinding
import piuk.blockchain.android.ui.base.MvpActivity
import piuk.blockchain.android.ui.createwallet.CreateWalletActivity
import piuk.blockchain.android.ui.login.LoginAnalytics
import piuk.blockchain.android.ui.recover.AccountRecoveryActivity
import piuk.blockchain.android.urllinks.WALLET_STATUS_URL
import piuk.blockchain.android.util.StringUtils

class LandingActivity : MvpActivity<LandingView, LandingPresenter>(), LandingView {

    override val presenter: LandingPresenter by scopedInject()
    override val view: LandingView = this

    private val binding: ActivityLandingOnboardingBinding by lazy {
        ActivityLandingOnboardingBinding.inflate(layoutInflater)
    }

    private lateinit var timer: Timer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        with(binding) {
            presenter.checkForRooted()

            btnCreate.apply { 
                text = getString(com.blockchain.stringResources.R.string.landing_create_wallet)
                onClick = { launchCreateWalletActivity() }
            }

            val onboardingList: List<CarouselViewType> = listOf(
                CarouselViewType.ValueProp(
                    com.blockchain.componentlib.R.drawable.carousel_brokerage,
                    this@LandingActivity.getString(com.blockchain.stringResources.R.string.landing_value_prop_one)
                ),
                CarouselViewType.ValueProp(
                    com.blockchain.componentlib.R.drawable.carousel_rewards,
                    this@LandingActivity.getString(com.blockchain.stringResources.R.string.landing_value_prop_two_1)
                ),
                CarouselViewType.ValueProp(
                    com.blockchain.componentlib.R.drawable.carousel_security,
                    this@LandingActivity.getString(com.blockchain.stringResources.R.string.landing_value_prop_three)
                ),
                CarouselViewType.PriceList(
                    this@LandingActivity.getString(com.blockchain.stringResources.R.string.landing_value_prop_four),
                    this@LandingActivity.getString(com.blockchain.stringResources.R.string.landing_live_prices)
                )
            )

            carousel.apply {
                submitList(onboardingList)
                setCarouselIndicator(carouselIndicators)

                setOnPricesAlphaChangeListener {
                    this@with.background.alpha = it
                }
            }

            val handler = Handler(Looper.getMainLooper())
            val update = Runnable {
                if (this@LandingActivity.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                    if (carousel.selectedPosition < onboardingList.size) {
                        carousel.setPage(carousel.selectedPosition++)
                    } else {
                        timer.cancel()
                        carousel.setPage(0)
                        presenter.checkShouldShowLandingCta()
                    }
                } else {
                    timer.cancel()
                }
            }

            timer = Timer().apply {
                schedule(
                    object : TimerTask() {
                        override fun run() {
                            handler.post(update)
                        }
                    },
                    CAROUSEL_PAGE_TIME,
                    CAROUSEL_PAGE_TIME
                )
            }
        }

        if (intent.getBooleanExtra(REDIRECT_TO_LOGIN, false)) {
            launchSSOLoginActivity()
        }
    }

    override fun onStart() {
        super.onStart()
        setupSSOControls(
            binding.btnLoginRestore.rightButton,
            binding.btnLoginRestore.leftButton
        )
    }

    private fun launchSSOAccountRecoveryFlow() =
        startActivity(Intent(this, AccountRecoveryActivity::class.java))

    private fun setupSSOControls(loginButton: Button, recoverButton: Button) {
        loginButton.setOnClickListener {
            launchSSOLoginActivity()
        }
        setupRecoverButton(recoverButton)
    }

    private fun setupRecoverButton(recoverButton: Button) {
        recoverButton.apply {
            text = getString(com.blockchain.stringResources.R.string.restore_wallet_cta)
            setOnClickListener { launchSSOAccountRecoveryFlow() }
        }
    }

    private fun launchCreateWalletActivity() {
        CreateWalletActivity.start(this)
    }

    private fun launchSSOLoginActivity() {
        analytics.logEvent(LoginAnalytics.LoginClicked())
        startActivity(Intent(this, piuk.blockchain.android.ui.login.LoginActivity::class.java))
    }

    override fun showLandingCta() {
        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(LandingCtaActivity.newIntent(this))
            overridePendingTransition(R.anim.slide_up_from_bottom, 0)
        }, 500)
    }

    override fun showIsRootedWarning() =
        showAlert(
            AlertDialog.Builder(this, com.blockchain.componentlib.R.style.AlertDialogStyle)
                .setMessage(com.blockchain.stringResources.R.string.device_rooted)
                .setCancelable(false)
                .setPositiveButton(com.blockchain.stringResources.R.string.dialog_continue) { _, _ -> clearAlert() }
                .create()
        )

    override fun showApiOutageMessage() {
        with(binding.tagWarning){
            visible()
            text = getString(com.blockchain.stringResources.R.string.wallet_issue_message_clear)
            onClick = {
                openUrl(WALLET_STATUS_URL)
            }
        }
    }

    override fun showSnackbar(message: String, type: SnackbarType) =
        BlockchainSnackbar.make(binding.root, message, type = type).show()

    override fun onLoadPrices(prices: List<PriceView.Price>) {
        binding.carousel.onLoadPrices(prices)
    }

    companion object {
        const val REDIRECT_TO_LOGIN = "REDIRECT_TO_LOGIN"

        @JvmStatic
        fun start(context: Context, redirectToLogin: Boolean = false) {
            Intent(context, LandingActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
                putExtra(REDIRECT_TO_LOGIN, redirectToLogin)
                context.startActivity(this)
            }
        }

        private const val CAROUSEL_PAGE_TIME = 2500L
    }
}
